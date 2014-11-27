package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

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
 *       <executions>
 *         <execution>
 *           <id>load-map-notes</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-map-notes</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-map-notes
 * @phase package
 */
public class MapNoteRf2LoaderMojo extends AbstractMojo {

  /** The commit count. */
  private final static int commitCt = 500;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Start loading map notes data ...");

    BufferedReader mapNoteReader = null;
    try {

      Properties config = ConfigUtility.getConfigProperties();

      // Set date format for parsing "effectiveTime"
      final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

      // set the input directory
      String inputFile = config.getProperty("loader.mapnotes.input.data");
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

      // Look up map projects amd ,a[ recprds
      getLog().info("  Lookup map projects");
      List<MapProject> mapProjects =
          mappingService.getMapProjects().getMapProjects();
      // refsetId -> conceptId -> Set<MapRecord>s
      Map<String, Map<String, Set<MapRecord>>> mapProjectMap = new HashMap<>();
      for (MapProject mapProject : mapProjects) {
        Map<String, Set<MapRecord>> mapRecordsMap = new HashMap<>();
        for (MapRecord mapRecord : mappingService.getMapRecordsForMapProject(
            mapProject.getId()).getMapRecords()) {
          if (!mapRecordsMap.containsKey(mapRecord.getConceptId())) {
            Set<MapRecord> mapRecordSet = new HashSet<>();
            mapRecordsMap.put(mapRecord.getConceptId(), mapRecordSet);
          }
          Set<MapRecord> mapRecordSet =
              mapRecordsMap.get(mapRecord.getConceptId());
          mapRecordSet.add(mapRecord);
        }
        mapProjectMap.put(mapProject.getRefSetId(), mapRecordsMap);
      }

      // Iterate through the file
      mapNoteReader = new BufferedReader(new FileReader(new File(inputFile)));
      MapUser loaderUser = mappingService.getMapUser("loader");
      String line = null;
      int ct = 0;
      while ((line = mapNoteReader.readLine()) != null) {

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

        Set<MapRecord> mapRecords = mapProjectMap.get(fields[4]).get(fields[5]);

        // Verify matching map records were found, otherwise fail
        if (mapRecords != null && mapRecords.size() > 0) {

          // Iterate through records and add note to each one
          // Note, if there are multiple records in the workflow, they all get
          // the note
          for (MapRecord mapRecord : mapRecords) {
            getLog().debug(
                mapNote.getNote().length() + " " + "    Adding note "
                    + fields[4] + ", " + mapRecord.getConceptId() + " = "
                    + mapNote.getNote());
            mapRecord.addMapNote(mapNote);
            mappingService.updateMapRecord(mapRecord);

            if (++ct % commitCt == 0) {
              getLog().info("      commit = " + ct);
              mappingService.commit();
              mappingService.beginTransaction();
            }
          }
        } else {
          getLog().info(
              "Map note references non-existent concept/project " + fields[5]
                  + "/" + fields[4]);
        }
      }
      getLog().info("    count = " + ct);
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
    }

  }
}