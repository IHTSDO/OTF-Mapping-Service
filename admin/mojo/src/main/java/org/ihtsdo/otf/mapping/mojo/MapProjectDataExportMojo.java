package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Comparator;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which exports project data to text files.
 * 
 * Sample execution;
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>export-project-data</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>export-project-data</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal export-project-data
 * 
 * @phase package
 */
public class MapProjectDataExportMojo extends AbstractMojo {

  /**
   * Instantiates a {@link MapProjectDataExportMojo} from the specified
   * parameters.
   * 
   */
  public MapProjectDataExportMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting exporting metadata ...");

    try {

      String configFileName = System.getProperty("run.config");
      getLog().info("  run.config = " + configFileName);
      Properties config = new Properties();
      FileReader in = new FileReader(new File(configFileName)); 
      config.load(in);
      in.close();
      getLog().info("  properties = " + config);

      // set the output directory
      String outputDirString = config.getProperty("export.output.dir");

      File outputDir = new File(outputDirString);
      if (!outputDir.exists()) {
        throw new MojoFailureException(
            "Specified export.output.dir directory does not exist: "
                + outputDirString);
      }

      File usersFile = new File(outputDir, "mapusers.txt");
      // if file doesn't exist, then create it
      if (!usersFile.exists()) {
        usersFile.createNewFile();
      }
      BufferedWriter usersWriter =
          new BufferedWriter(new FileWriter(usersFile.getAbsoluteFile()));

      File advicesFile = new File(outputDir, "mapadvices.txt");
      // if file doesn't exist, then create it
      if (!advicesFile.exists()) {
        advicesFile.createNewFile();
      }
      BufferedWriter advicesWriter =
          new BufferedWriter(new FileWriter(advicesFile.getAbsoluteFile()));

      File relationsFile = new File(outputDir, "maprelations.txt");
      // if file doesn't exist, then create it
      if (!relationsFile.exists()) {
        relationsFile.createNewFile();
      }
      BufferedWriter relationsWriter =
          new BufferedWriter(new FileWriter(relationsFile.getAbsoluteFile()));

      File principlesFile = new File(outputDir, "mapprinciples.txt");
      // if file doesn't exist, then create it
      if (!principlesFile.exists()) {
        principlesFile.createNewFile();
      }
      BufferedWriter principlesWriter =
          new BufferedWriter(new FileWriter(principlesFile.getAbsoluteFile()));

      File projectsFile = new File(outputDir, "mapprojects.txt");
      // if file doesn't exist, then create it
      if (!projectsFile.exists()) {
        projectsFile.createNewFile();
      }
      BufferedWriter projectsWriter =
          new BufferedWriter(new FileWriter(projectsFile.getAbsoluteFile()));

      File agerangesFile = new File(outputDir, "mapageranges.txt");

      // if file doesn't exist, then create it
      if (!agerangesFile.exists()) {
        agerangesFile.createNewFile();
      }
      BufferedWriter agerangesWriter =
          new BufferedWriter(new FileWriter(agerangesFile.getAbsoluteFile()));

      File scopeIncludesFile = new File(outputDir, "scopeIncludes.txt");
      // if file doesn't exist, then create it
      if (!scopeIncludesFile.exists()) {
        scopeIncludesFile.createNewFile();
      }
      BufferedWriter scopeIncludesWriter =
          new BufferedWriter(
              new FileWriter(scopeIncludesFile.getAbsoluteFile()));

      File scopeExcludesFile = new File(outputDir, "scopeExcludes.txt");
      // if file doesn't exist, then create it
      if (!scopeExcludesFile.exists()) {
        scopeExcludesFile.createNewFile();
      }
      BufferedWriter scopeExcludesWriter =
          new BufferedWriter(
              new FileWriter(scopeExcludesFile.getAbsoluteFile()));

      // export to mapspecialists.txt
      MappingService mappingService = new MappingServiceJpa();
      MapUserListJpa mapUsers = new MapUserListJpa();
      mapUsers.setMapUsers(mappingService.getMapUsers().getMapUsers());
      // Sort by name
      mapUsers.sortBy(new Comparator<MapUser>() {
        @Override
        public int compare(MapUser o1, MapUser o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      for (MapUser ms : mapUsers.getMapUsers()) {
        usersWriter.write(ms.getName() + "\t" + ms.getUserName() + "\t"
            + ms.getEmail() + "\n");
      }

      // export to mapadvices.txt
      for (MapAdvice ma : mappingService.getMapAdvices().getMapAdvices()) {
        advicesWriter.write(ma.getName() + "\t" + ma.getDetail() + "\t"
            + ma.isAllowableForNullTarget() + "\t" + ma.isComputed() + "\n");
      }

      // export to maprelations.txt
      for (MapRelation ma : mappingService.getMapRelations().getMapRelations()) {
        relationsWriter.write(ma.getTerminologyId() + "\t"
            + ma.getAbbreviation() + "\t" + ma.getName() + "\t"
            + ma.isAllowableForNullTarget() + "\t" + ma.isComputed() + "\n");
      }

      // export to mapprinciples.txt
      for (MapPrinciple ma : mappingService.getMapPrinciples()
          .getMapPrinciples()) {
        String detail = ma.getDetail();
        detail = detail.replace("\n", "<br>").replace("\r", "<br>");
        principlesWriter.write(ma.getName() + "|" + ma.getPrincipleId() + "|"
            + ma.getSectionRef() + "|" + detail + "\n");
      }

      // export to mapprojects.txt
      for (MapProject mpr : mappingService.getMapProjects().getMapProjects()) {
        StringBuffer mapAdvices = new StringBuffer();
        for (MapAdvice ma : mpr.getMapAdvices()) {
          mapAdvices.append(ma.getName()).append(";");
        }
        if (mapAdvices.length() > 1)
          mapAdvices.deleteCharAt(mapAdvices.length() - 1);

        StringBuffer mapRelations = new StringBuffer();
        for (MapRelation ma : mpr.getMapRelations()) {
          mapRelations.append(ma.getTerminologyId()).append(",");
        }
        if (mapRelations.length() > 1)
          mapRelations.deleteCharAt(mapRelations.length() - 1);

        StringBuffer mapPrinciples = new StringBuffer();
        for (MapPrinciple ma : mpr.getMapPrinciples()) {
          mapPrinciples.append(ma.getPrincipleId()).append(",");
        }
        if (mapPrinciples.length() > 1)
          mapPrinciples.deleteCharAt(mapPrinciples.length() - 1);

        StringBuffer mprMapLeads = new StringBuffer();
        for (MapUser ma : mpr.getMapLeads()) {
          mprMapLeads.append(ma.getUserName()).append(",");
        }
        if (mprMapLeads.length() > 1)
          mprMapLeads.deleteCharAt(mprMapLeads.length() - 1);

        StringBuffer mprMapSpecialists = new StringBuffer();
        for (MapUser ma : mpr.getMapSpecialists()) {
          mprMapSpecialists.append(ma.getUserName()).append(",");
        }
        if (mprMapSpecialists.length() > 1)
          mprMapSpecialists.deleteCharAt(mprMapSpecialists.length() - 1);

        projectsWriter.write(mpr.getName() + "\t" + mpr.getRefSetId() + "\t"
            + mpr.isPublished() + "\t" + mpr.getSourceTerminology() + "\t"
            + mpr.getSourceTerminologyVersion() + "\t"
            + mpr.getDestinationTerminology() + "\t"
            + mpr.getDestinationTerminologyVersion() + "\t"
            + mpr.isBlockStructure() + "\t" + mpr.isGroupStructure() + "\t"
            + mpr.isPublished() + "\t" + mpr.getMapRelationStyle() + "\t"
            + mpr.getMapPrincipleSourceDocument() + "\t" + mpr.isRuleBased()
            + "\t" + mpr.getMapRefsetPattern() + "\t"
            + mpr.getProjectSpecificAlgorithmHandlerClass() + "\t" + mapAdvices
            + "\t" + mapRelations + "\t" + mapPrinciples + "\t" + mprMapLeads
            + "\t" + mprMapSpecialists + "\t" + mpr.isScopeDescendantsFlag()
            + "\t" + mpr.isScopeExcludedDescendantsFlag() + "\n");

        // add to mapageranges.txt
        for (MapAgeRange ar : mpr.getPresetAgeRanges()) {
          agerangesWriter.write(mpr.getRefSetId() + "|" + ar.getName() + "|"
              + ar.getLowerValue() + "|" + ar.getLowerUnits() + "|"
              + (ar.getLowerInclusive() == true ? "true" : "false") + "|"
              + ar.getUpperValue() + "|" + ar.getUpperUnits() + "|"
              + (ar.getUpperInclusive() == true ? "true" : "false") + "\n");
        }
      }

      for (MapProject mpr : mappingService.getMapProjects().getMapProjects()) {
        for (String concept : mpr.getScopeConcepts()) {
          scopeIncludesWriter.write(mpr.getRefSetId() + "\t" + concept + "\n");
        }
      }

      for (MapProject mpr : mappingService.getMapProjects().getMapProjects()) {
        for (String concept : mpr.getScopeExcludedConcepts()) {
          scopeExcludesWriter.write(mpr.getRefSetId() + "\t" + concept + "\n");
        }
      }

      mappingService.close();

      getLog().info("done ...");
      usersWriter.close();
      advicesWriter.close();
      principlesWriter.close();
      relationsWriter.close();
      projectsWriter.close();
      scopeIncludesWriter.close();
      scopeExcludesWriter.close();
      agerangesWriter.close();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

}
