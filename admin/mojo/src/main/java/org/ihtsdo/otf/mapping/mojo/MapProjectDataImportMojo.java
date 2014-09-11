package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
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
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

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
 *       <executions>
 *         <execution>
 *           <id>import-project-data</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>import-project-data</goal>
 *           </goals>
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
  @SuppressWarnings("resource")
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting importing metadata ...");
    try {

      String configFileName = System.getProperty("run.config");
      getLog().info("  run.config = " + configFileName);
      Properties config = new Properties();
      FileReader in = new FileReader(new File(configFileName)); 
      config.load(in);
      in.close();
      getLog().info("  properties = " + config);

      // set the input directory
      String inputDirString = config.getProperty("import.input.dir");
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
        String role = st.nextToken();       
        mapUser.setApplicationRole(MapUserRole.valueOf(role));
        mappingService.addMapUser(mapUser);
        getLog().info("  " + mapUser.getUserName());
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
        getLog().info("  " + mapAdvice.getName());
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
        getLog().info("  " + mapRelation.getName());
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
        getLog().info("  " + mapPrinciple.getPrincipleId());
      }
      getLog().info(
          "  "
              + Integer.toString(mappingService.getMapPrinciples()
                  .getTotalCount()) + " principles added.");

      // Add map age ranges
      getLog().info("Adding project age ranges...");

      // Instantiate sets for checking uniqueness and saving project-specific
      // information
      Set<MapAgeRange> ageRanges = new HashSet<>();
      Map<String, Set<MapAgeRange>> projectAgeRanges =
          new HashMap<>();

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
          newAgeRanges = new HashSet<>();
          newAgeRanges.add(newAgeRange);
        }

        // replace the list
        projectAgeRanges.put(refSetId, newAgeRanges);
        getLog().info("  " + mapAgeRange.toString());
      }
      getLog().info(
          "  " + Integer.toString(mappingService.getMapAgeRanges().getCount())
              + " ageranges added from "
              + Integer.toString(projectAgeRanges.size()) + " map projects.");

      // Add map projects
      getLog().info("Adding projects...");

      MetadataService metadataService = new MetadataServiceJpa();
      while ((line = projectsReader.readLine()) != null) {
    	  
    	int i = 0; 
    	  
        String[] fields = line.split("\t");
        MapProjectJpa mapProject = new MapProjectJpa();
        mapProject.setName(fields[i++]);
        mapProject.setRefSetId(fields[i++]);
        mapProject.setPublished(fields[i++].equals("true") ? true : false);
        mapProject.setSourceTerminology(fields[i++]);
        // Override setting from the file and use the current version in the DB.
        String terminologyVersion = metadataService.getTerminologyLatestVersions().get(mapProject.getSourceTerminology());
        if (terminologyVersion == null) {
          throw new Exception("Unexpected failure to find current version of " + mapProject.getSourceTerminology());
        }
        i++; // increment the counter
        mapProject.setSourceTerminologyVersion(terminologyVersion);
        mapProject.setDestinationTerminology(fields[i++]);
        // Override setting from the file and use the current version in the DB.
        terminologyVersion = metadataService.getTerminologyLatestVersions().get(mapProject.getDestinationTerminology());
        if (terminologyVersion == null) {
          throw new Exception("Unexpected failure to find current version of " + mapProject.getDestinationTerminology());
        }
        i++; // increment the counter
        mapProject.setDestinationTerminologyVersion(terminologyVersion);
        mapProject.setBlockStructure(fields[i++].toLowerCase().equals("true")
            ? true : false);
        mapProject.setGroupStructure(fields[i++].toLowerCase().equals("true")
            ? true : false);
        mapProject.setPublished(fields[i++].toLowerCase().equals("true") ? true
            : false);
        mapProject.setMapRelationStyle(fields[i++]);
        if (fields[i++].equals(WorkflowType.CONFLICT_PROJECT.toString())) 
        	mapProject.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
        else if (fields[i++].equals(WorkflowType.REVIEW_PROJECT.toString())) 
        	mapProject.setWorkflowType(WorkflowType.REVIEW_PROJECT);

        mapProject.setMapPrincipleSourceDocumentName(fields[i++]);
        mapProject.setRuleBased(fields[i++].toLowerCase().equals("true") ? true
            : false);
        mapProject.setMapRefsetPattern(fields[i++]);
        mapProject.setProjectSpecificAlgorithmHandlerClass(fields[i++]);
        getLog().info("  " + mapProject.getRefSetId());
        getLog().info("  " + mapProject.getName());

        String mapAdvices = fields[i++].replaceAll("\"", "");
        if (!mapAdvices.equals("")) {
          for (String advice : mapAdvices.split(";")) {
            for (MapAdvice ml : mappingService.getMapAdvices().getIterable()) {
              if (ml.getName().equals(advice))
                mapProject.addMapAdvice(ml);
            }
          }
        }

        String mapRelations = fields[i++].replaceAll("\"", "");
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

        String mapPrinciples = fields[i++].replaceAll("\"", "");
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

        String mapLeads = fields[i++].replaceAll("\"", "");
        for (String lead : mapLeads.split(",")) {
          for (MapUser ml : mappingService.getMapUsers().getIterable()) {
            if (ml.getUserName().equals(lead))
              mapProject.addMapLead(ml);
          }
        }

        String mapSpecialists = fields[i++].replaceAll("\"", "");
        for (String specialist : mapSpecialists.split(",")) {

          for (MapUser ml : mappingService.getMapUsers().getIterable()) {
            if (ml.getUserName().toLowerCase().equals(specialist))
              mapProject.addMapSpecialist(ml);
          }
        }
        
        String mapAdministrators = fields[i++].replaceAll("\"", "");
        for (String administrator : mapAdministrators.split(",")) {

          for (MapUser ma : mappingService.getMapUsers().getIterable()) {
            if (ma.getUserName().toLowerCase().equals(administrator))
              mapProject.addMapAdministrator(ma);
          }
        }

        mapProject.setScopeDescendantsFlag(fields[i++].toLowerCase().equals(
            "true") ? true : false);
        mapProject.setScopeExcludedDescendantsFlag(fields[i++].toLowerCase()
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

      metadataService.close();

      getLog().info(
          "  "
              + Integer.toString(mappingService.getMapProjects()
                  .getTotalCount()) + " projects added.");

      // Concepts In Scope Assignment

      // hashmap of project id -> Set of concept terminology ids
      Map<Long, Set<String>> projectToConceptsInScope =
          new HashMap<>();

      // cycle over the includes file

      getLog().info("Adding scope includes...");

      ContentService contentService = new ContentServiceJpa();
      while ((line = scopeIncludesReader.readLine()) != null) {

        String[] fields = line.split("\\t");

        // retrieve the project id associated with this refSetId
        Long projectId =
            mappingService.getMapProjectForRefSetId(fields[0]).getId();
        MapProject project = mappingService.getMapProject(projectId);

        Concept c = contentService.getConcept(fields[1], project.getSourceTerminology(), 
        		project.getSourceTerminologyVersion());
        if (c == null) {
          getLog().warn("Scope Includes concept + " + fields[1] + " is not in the data.");
        } else {
					// if project already added, add list of concepts
					if (projectToConceptsInScope.containsKey(projectId))
						projectToConceptsInScope.get(projectId).add(fields[1]);

					// otherwise add project with list of concepts
					else {
						Set<String> conceptsInScope = new HashSet<>();
						conceptsInScope.add(fields[1]);
						projectToConceptsInScope.put(projectId, conceptsInScope);
					}
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
          new HashMap<>();

      // cycle over the exclude file
      while ((line = scopeExcludesReader.readLine()) != null) {

        String[] fields = line.split("\\t");

        // retrieve the project id associated with this refSetId
        Long projectId =
            mappingService.getMapProjectForRefSetId(fields[0]).getId();
        MapProject project = mappingService.getMapProject(projectId);

        Concept c = contentService.getConcept(fields[1], project.getSourceTerminology(), 
        		project.getSourceTerminologyVersion());
        
        if (c == null)
          getLog().warn("Scope Excludes concept + " + fields[1] + " is not in the data.");
        else {
					// if project in set, add this new set of concept ids
					if (projectToConceptsExcludedFromScope.containsKey(projectId))
						projectToConceptsExcludedFromScope.get(projectId).add(fields[1]);

					// otherwise, insert project into hash set with the excluded concept
					// list
					else {
						Set<String> conceptsExcludedFromScope = new HashSet<>();
						conceptsExcludedFromScope.add(fields[1]);
						projectToConceptsExcludedFromScope.put(projectId,
								conceptsExcludedFromScope);
					}
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
      contentService.close();
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
