package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which imports project data from text files.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *      <dependencies>
 *          <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-import-config</artifactId>
 *           <version>${project.version}</version>
 *           <scope>system</scope>
 *           <systemPath>${project.build.directory}/mapping-admin-import-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>import-project-data</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>import-project-data</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal import-project-data
 * 
 * @phase package
 */
public class MapProjectDataImportMojo extends AbstractMojo {

  /**
   * Properties file.
   * 
   * @parameter 
   *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
   * @required
   */
  private File propertiesFile;

  /**
   * Instantiates a {@link MapProjectDataImportMojo} from the specified
   * parameters.
   * 
   */
  public MapProjectDataImportMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting importing metadata ...");
    try {

      FileInputStream propertiesInputStream = null;

      // load Properties file
      Properties properties = new Properties();
      propertiesInputStream = new FileInputStream(propertiesFile);
      properties.load(propertiesInputStream);

      // set the input directory
      String inputDirString = properties.getProperty("import.input.dir");
      propertiesInputStream.close();
      File inputDir = new File(inputDirString);
      if (!inputDir.exists()) {
        throw new MojoFailureException(
            "Specified import.input.dir directory does not exist: "
                + inputDirString);
      }

      File usersFile = new File(inputDir, "mapusers.txt");
      BufferedReader usersReader =
          new BufferedReader(new FileReader(usersFile));

      File advicesFile = new File(inputDir, "mapadvices.txt");
      BufferedReader advicesReader =
          new BufferedReader(new FileReader(advicesFile));

      File relationsFile = new File(inputDir, "maprelations.txt");
      BufferedReader relationsReader =
          new BufferedReader(new FileReader(relationsFile));

      File principlesFile = new File(inputDir, "mapprinciples.txt");
      BufferedReader principlesReader =
          new BufferedReader(new FileReader(principlesFile));

      File agerangesFile = new File(inputDir, "mapageranges.txt");
      BufferedReader agerangesReader =
          new BufferedReader(new FileReader(agerangesFile));

      File projectsFile = new File(inputDir, "mapprojects.txt");
      BufferedReader projectsReader =
          new BufferedReader(new FileReader(projectsFile));

      File scopeExcludesFile = new File(inputDir, "scopeExcludes.txt");
      BufferedReader scopeExcludesReader =
          new BufferedReader(new FileReader(scopeExcludesFile));

      File scopeIncludesFile = new File(inputDir, "scopeIncludes.txt");
      BufferedReader scopeIncludesReader =
          new BufferedReader(new FileReader(scopeIncludesFile));

      MappingService mappingService = new MappingServiceJpa();

      // Add Users
      getLog().info("Adding users...");

      String line = "";
      while ((line = usersReader.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, "\t");
        MapUser mapUser = new MapUserJpa();
        mapUser.setName(st.nextToken());
        mapUser.setUserName(st.nextToken());
        mapUser.setEmail(st.nextToken());
        mappingService.addMapUser(mapUser);
      }

      getLog().info(
          "  " + Integer.toString(mappingService.getMapUsers().getTotalCount())
              + " users added.");

      // Add map advices
      getLog().info("Adding advices...");

      while ((line = advicesReader.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, "\t");
        MapAdviceJpa mapAdvice = new MapAdviceJpa();
        mapAdvice.setName(st.nextToken());
        mapAdvice.setDetail(st.nextToken());
        mapAdvice.setAllowableForNullTarget(st.nextToken().toLowerCase()
            .equals("true") ? true : false);
        mapAdvice.setComputed(st.nextToken().toLowerCase().equals("true")
            ? true : false);
        mappingService.addMapAdvice(mapAdvice);
      }

      getLog().info(
          "  "
              + Integer
                  .toString(mappingService.getMapAdvices().getTotalCount())
              + " advices added.");

      // Add map relations
      getLog().info("Adding relations...");

      while ((line = relationsReader.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line, "\t");
        MapRelationJpa mapRelation = new MapRelationJpa();
        mapRelation.setTerminologyId(st.nextToken());
        mapRelation.setAbbreviation(st.nextToken());
        mapRelation.setName(st.nextToken());
        mapRelation.setAllowableForNullTarget(st.nextToken().toLowerCase()
            .equals("true") ? true : false);
        mapRelation.setComputed(st.nextToken().toLowerCase().equals("true")
            ? true : false);
        mappingService.addMapRelation(mapRelation);
      }

      getLog().info(
          "  "
              + Integer.toString(mappingService.getMapRelations()
                  .getTotalCount()) + " relations added.");

      // Add map principles
      getLog().info("Adding principles...");

      while ((line = principlesReader.readLine()) != null) {
        String[] fields = line.split("\\|");
        MapPrincipleJpa mapPrinciple = new MapPrincipleJpa();
        mapPrinciple.setName(fields[0]);
        mapPrinciple.setPrincipleId(fields[1]);
        mapPrinciple.setSectionRef(fields[2]);
        mapPrinciple.setDetail(fields[3]);
        mappingService.addMapPrinciple(mapPrinciple);
      }
      getLog().info(
          "  "
              + Integer.toString(mappingService.getMapPrinciples()
                  .getTotalCount()) + " principles added.");

      // Add map age ranges
      getLog().info("Adding project age ranges...");

      // Instantiate sets for checking uniqueness and saving project-specific
      // information
      Set<MapAgeRange> ageRanges = new HashSet<MapAgeRange>();
      Map<String, Set<MapAgeRange>> projectAgeRanges =
          new HashMap<String, Set<MapAgeRange>>();

      // cycle through the project age ranges
      while ((line = agerangesReader.readLine()) != null) {
        String[] fields = line.split("\\|", -1); // make sure to account for
                                                 // empty fields

        // get the ref set id
        String refSetId = fields[0];

        // construct the age range
        MapAgeRangeJpa mapAgeRange = new MapAgeRangeJpa();
        mapAgeRange.setName(fields[1]);
        mapAgeRange.setLowerValue(fields[2].equals("")
            || fields[2].equals("null") ? null : new Integer(fields[2]));
        mapAgeRange.setLowerUnits(fields[3]);
        mapAgeRange.setLowerInclusive(fields[4].toLowerCase().equals("true")
            ? true : false);
        mapAgeRange.setUpperValue(fields[5].equals("")
            || fields[5].equals("null") ? null : new Integer(fields[5]));
        mapAgeRange.setUpperUnits(fields[6]);
        mapAgeRange.setUpperInclusive(fields[7].toLowerCase().equals("true")
            ? true : false);

        // if this age range is not in hash set, add it
        MapAgeRange newAgeRange = null;
        if (!ageRanges.contains(mapAgeRange)) {
          newAgeRange = mappingService.addMapAgeRange(mapAgeRange);
          ageRanges.add(newAgeRange);

          // otherwise find this age range by hash value
        } else {
          Iterator<MapAgeRange> iter = ageRanges.iterator();
          while (iter.hasNext()) {
            MapAgeRange hashedAgeRange = iter.next();
            if (hashedAgeRange.equals(mapAgeRange)) {
              newAgeRange = hashedAgeRange;
            }
          }
        }

        // add this age range to this refset id hashset
        Set<MapAgeRange> newAgeRanges = projectAgeRanges.get(refSetId);

        // if a list exists for this refset id, add to list
        if (newAgeRanges != null) {
          newAgeRanges.add(newAgeRange);

          // otherwise instantiate a new list
        } else {
          newAgeRanges = new HashSet<MapAgeRange>();
          newAgeRanges.add(newAgeRange);
        }

        // replace the list
        projectAgeRanges.put(refSetId, newAgeRanges);

      }
      getLog().info(
          "  " + Integer.toString(mappingService.getMapAgeRanges().size())
              + " ageranges added from "
              + Integer.toString(projectAgeRanges.size()) + " map projects.");

      // Add map projects
      getLog().info("Adding projects...");

      while ((line = projectsReader.readLine()) != null) {
        String[] fields = line.split("\t");
        MapProjectJpa mapProject = new MapProjectJpa();
        mapProject.setName(fields[0]);
        mapProject.setRefSetId(fields[1]);
        mapProject.setPublished(fields[2].equals("true") ? true : false);
        mapProject.setSourceTerminology(fields[3]);
        mapProject.setSourceTerminologyVersion(fields[4]);
        mapProject.setDestinationTerminology(fields[5]);
        mapProject.setDestinationTerminologyVersion(fields[6]);
        mapProject.setBlockStructure(fields[7].toLowerCase().equals("true")
            ? true : false);
        mapProject.setGroupStructure(fields[8].toLowerCase().equals("true")
            ? true : false);
        mapProject.setPublished(fields[9].toLowerCase().equals("true") ? true
            : false);
        mapProject.setMapRelationStyle(fields[10]);
        mapProject.setMapPrincipleSourceDocument(fields[11]);
        mapProject.setRuleBased(fields[12].toLowerCase().equals("true") ? true
            : false);
        mapProject.setMapRefsetPattern(fields[13]);
        mapProject.setProjectSpecificAlgorithmHandlerClass(fields[14]);

        String mapAdvices = fields[15].replaceAll("\"", "");
        if (!mapAdvices.equals("")) {
          for (String advice : mapAdvices.split(",")) {
            for (MapAdvice ml : mappingService.getMapAdvices().getIterable()) {
              if (ml.getName().equals(advice))
                mapProject.addMapAdvice(ml);
            }
          }
        }

        String mapRelations = fields[16].replaceAll("\"", "");
        if (!mapRelations.equals("")) {
          for (String terminologyId : mapRelations.split(",")) {
            for (MapRelation ml : mappingService.getMapRelations()
                .getIterable()) {
              if (ml.getTerminologyId().equals(terminologyId)) {
                mapProject.addMapRelation(ml);
              }
            }
          }
        }

        String mapPrinciples = fields[17].replaceAll("\"", "");
        if (!mapPrinciples.equals("")) {
          for (String principle : mapPrinciples.split(",")) {
            for (MapPrinciple ml : mappingService.getMapPrinciples()
                .getIterable()) {
              if (ml.getPrincipleId().equals(principle)) {
                mapProject.addMapPrinciple(ml);
              }
            }
          }
        }

        String mapLeads = fields[18].replaceAll("\"", "");
        for (String lead : mapLeads.split(",")) {
          for (MapUser ml : mappingService.getMapUsers().getIterable()) {
            if (ml.getUserName().equals(lead))
              mapProject.addMapLead(ml);
          }
        }

        String mapSpecialists = fields[19].replaceAll("\"", "");
        for (String specialist : mapSpecialists.split(",")) {

          for (MapUser ml : mappingService.getMapUsers().getIterable()) {
            if (ml.getUserName().toLowerCase().equals(specialist))
              mapProject.addMapSpecialist(ml);
          }
        }

        mapProject.setScopeDescendantsFlag(fields[20].toLowerCase().equals(
            "true") ? true : false);
        mapProject.setScopeExcludedDescendantsFlag(fields[21].toLowerCase()
            .equals("true") ? true : false);

        // add the preset age ranges
        if (projectAgeRanges.containsKey(mapProject.getRefSetId())) {
          mapProject.setPresetAgeRanges(projectAgeRanges.get(mapProject
              .getRefSetId()));
        }
        
        // look up the refset concept name
        ContentService contentService = new ContentServiceJpa();
        Concept refSetConcept = contentService.getConcept(
        		mapProject.getRefSetId(),
        		mapProject.getSourceTerminology(),
        		mapProject.getSourceTerminologyVersion());
        
        // throw an exception if the concept does not exist
        if (refSetConcept == null) {
        	throw new Exception("Project import could not retrieve ref set name");
        }    
        mapProject.setRefSetName(refSetConcept.getDefaultPreferredName());

        mappingService.addMapProject(mapProject);
      }

      getLog().info(
          "  "
              + Integer.toString(mappingService.getMapProjects()
                  .getTotalCount()) + " projects added.");

      // Concepts In Scope Assignment

      // hashmap of project id -> Set of concept terminology ids
      Map<Long, Set<String>> projectToConceptsInScope =
          new HashMap<Long, Set<String>>();

      // cycle over the includes file

      getLog().info("Adding scope includes...");

      while ((line = scopeIncludesReader.readLine()) != null) {

        String[] fields = line.split("\\t");

        // retrieve the project id associated with this refSetId
        Long projectId =
            mappingService.getMapProjectByRefSetId(fields[0]).getId();

        // if project already added, add list of concepts
        if (projectToConceptsInScope.containsKey(projectId))
          projectToConceptsInScope.get(projectId).add(fields[1]);

        // otherwise add project with list of concepts
        else {
          Set<String> conceptsInScope = new HashSet<String>();
          conceptsInScope.add(fields[1]);
          projectToConceptsInScope.put(projectId, conceptsInScope);
        }
      }

      // set the map project scope concepts and update the project
      int conceptsIncludedInScopeCount = 0;
      for (Long projectId : projectToConceptsInScope.keySet()) {
        MapProject mapProject = mappingService.getMapProject(projectId);
        mapProject.setScopeConcepts(projectToConceptsInScope.get(projectId));
        mappingService.updateMapProject(mapProject);
        conceptsIncludedInScopeCount +=
            projectToConceptsInScope.get(projectId).size();
      }

      getLog().info(
          "  " + Integer.toString(conceptsIncludedInScopeCount)
              + " included concepts added for "
              + Integer.toString(projectToConceptsInScope.keySet().size())
              + " projects.");

      // Concepts Excluded From Scope Assignmnet

      getLog().info("Adding scope excludes...");

      // map of project ids -> set of concept terminology Ids
      Map<Long, Set<String>> projectToConceptsExcludedFromScope =
          new HashMap<Long, Set<String>>();

      // cycle over the exclude file
      while ((line = scopeExcludesReader.readLine()) != null) {

        String[] fields = line.split("\\t");

        // retrieve the project id associated with this refSetId
        Long projectId =
            mappingService.getMapProjectByRefSetId(fields[0]).getId();

        // if project in set, add this new set of concept ids
        if (projectToConceptsExcludedFromScope.containsKey(projectId))
          projectToConceptsExcludedFromScope.get(projectId).add(fields[1]);

        // otherwise, insert project into hash set with the excluded concept
        // list
        else {
          Set<String> conceptsExcludedFromScope = new HashSet<String>();
          conceptsExcludedFromScope.add(fields[1]);
          projectToConceptsExcludedFromScope.put(projectId,
              conceptsExcludedFromScope);
        }
      }

      // cycle over detected project excludes and update map projects
      int conceptsExcludedFromScopeCount = 0;
      for (Long projectId : projectToConceptsExcludedFromScope.keySet()) {
        MapProject mapProject =
            mappingService.getMapProject(new Long(projectId));
        mapProject.setScopeExcludedConcepts(projectToConceptsExcludedFromScope
            .get(projectId));
        mappingService.updateMapProject(mapProject);
        conceptsExcludedFromScopeCount +=
            mapProject.getScopeExcludedConcepts().size();
      }

      getLog().info(
          "  "
              + Integer.toString(conceptsExcludedFromScopeCount)
              + " excluded concepts added for "
              + Integer.toString(projectToConceptsExcludedFromScope.keySet()
                  .size()) + " projects.");

      getLog().info("done ...");
      mappingService.close();
      agerangesReader.close();
      usersReader.close();
      advicesReader.close();
      relationsReader.close();
      principlesReader.close();
      projectsReader.close();
      scopeIncludesReader.close();
      scopeExcludesReader.close();

    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
