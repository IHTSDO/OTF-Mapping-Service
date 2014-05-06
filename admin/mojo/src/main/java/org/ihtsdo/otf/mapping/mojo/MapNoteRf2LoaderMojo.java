package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads map notes.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <dependencies>
 *         <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-loader-config</artifactId>
 *           <version>${project.version}</version>
 *           <scope>system</scope>
 *           <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>load-map-notes</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-map-notes</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-map-notes
 * @phase package
 */
public class MapNoteRf2LoaderMojo extends AbstractMojo {

  /**
   * Properties file.
   * 
   * @parameter 
   *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
   * @required
   */
  private File propertiesFile;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Start loading map notes data ...");

    FileInputStream propertiesInputStream = null;
    BufferedReader mapNoteReader = null;
    try {

      // load Properties file
      Properties properties = new Properties();
      propertiesInputStream = new FileInputStream(propertiesFile);
      properties.load(propertiesInputStream);
      propertiesInputStream.close();

      // Set date format for parsing "effectiveTime"
      final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

      // set the input directory
      String inputFile = properties.getProperty("loader.mapnotes.input.data");
      if (!new File(inputFile).exists()) {
        throw new MojoFailureException(
            "Specified loader.mapnotes.input.data directory does not exist: "
                + inputFile);
      }
      getLog().info("  inputFile: " + inputFile);

      // Instantiate service and transaction
      getLog().info("  Instantiate mapping service and setup transactions");
      MappingService mappingService = new MappingServiceJpa();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();

      // Look up map projects
      getLog().info("  Lookup map projects");
      List<MapProject> mapProjects =
          mappingService.getMapProjects().getMapProjects();

      // Iterate through the file
      mapNoteReader = new BufferedReader(new FileReader(new File(inputFile)));
      MapUser loaderUser = mappingService.getMapUser("loader");
      String line = null;
      int ct = 0;
      while ((line = mapNoteReader.readLine()) != null) {
        ct++;

        // parse fields and create object
        // id effectiveTime active moduleId refSetId referencedComponentId
        // fullySpecifiedName annotation
        line = line.replace("\r", "");
        String fields[] = line.split("\t");
        MapNote mapNote = new MapNoteJpa();

        // Skip header
        if (fields[0].equals("id")) {
          continue;
        }

        // Set fields
        mapNote.setUser(loaderUser);
        mapNote.setTimestamp(dt.parse(fields[1]));
        String note = fields[7];
        note = note.trim();
        if (note.length() > 4000) {
          final String truncatedMsg = "...[truncated]";
          note = note.substring(0, 4000 - truncatedMsg.length()) + truncatedMsg;
        }
        mapNote.setNote(note);

        List<MapRecord> mapRecords =
            mappingService.getMapRecordsForConcept(fields[5]).getMapRecords();

        // Verify matching map records were found, otherwise fail
        if (mapRecords != null && mapRecords.size() > 0) {

          // Iterate through records
          for (MapRecord mapRecord : mapRecords) {

            // Find matching map project
            for (MapProject mapProject : mapProjects) {

              // find matching refset id
              if (mapProject.getRefSetId().equals(fields[4])) {
                getLog().debug(
                    mapNote.getNote().length() + " "
                        + "    Adding note to record "
                        + mapProject.getRefSetId() + ", "
                        + mapRecord.getConceptId() + " = " + mapNote.getNote());
                mapRecord.addMapNote(mapNote);
                mappingService.updateMapRecord(mapRecord);

                if (++ct % 500 == 0) {
                  getLog().info("      " + ct + " notes processed");
                }
              }
            }
          }
        } else {
          throw new IllegalStateException(
              "Map note references non-existent concept " + fields[5]);
        }
      }
      getLog().info("  " + ct + " map notes inserted");
      getLog().info("done ...");
      mappingService.commit();
      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Loading of Unpublished RF2 Complex Maps failed.", e);
    } finally {
      try {
        mapNoteReader.close();
      } catch (IOException e1) {
        // do nothing
      }
      try {
        propertiesInputStream.close();
      } catch (IOException e) {
        // do nothing
      }
    }

  }
}