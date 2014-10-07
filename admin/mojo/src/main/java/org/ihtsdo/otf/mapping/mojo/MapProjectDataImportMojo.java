package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
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

      // get all project .xml files
      FilenameFilter projectFilter = new FilenameFilter() {
  			public boolean accept(File dir, String name) {
  				String lowercaseName = name.toLowerCase();
  				if (lowercaseName.endsWith(".xml") && lowercaseName.startsWith("Project")) {
  					return true;
  				} else {
  					return false;
  				}
  			}
  		};
      File [] projectFiles = inputDir.listFiles(projectFilter);
      
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();
      
      
			// read project .xml files one at a time
			for (File projectFile : projectFiles) {
				BufferedReader projectReader =
						new BufferedReader(new FileReader(projectFile));

				// Unmarshal from XML
				JAXBContext context = JAXBContext.newInstance(MapProjectJpa.class);
				Unmarshaller unmarshaller = context.createUnmarshaller();
				MapProject project =
						(MapProjectJpa) unmarshaller.unmarshal(new StreamSource(
								new StringReader(projectReader.readLine())));

				
				Set<MapAdvice> advices = project.getMapAdvices();
				List<MapAdvice> currentAdvices =
						mappingService.getMapAdvices().getMapAdvices();
				for (MapAdvice advice : advices) {
					if (!currentAdvices.contains(advice)) {
						System.out.println("ready to add " + advice);
						// mappingService.addMapAdvice(advice);
					}
				}

				Set<MapPrinciple> principles = project.getMapPrinciples();
				List<MapPrinciple> currentPrinciples =
						mappingService.getMapPrinciples().getMapPrinciples();
				for (MapPrinciple principle : principles) {
					if (!currentPrinciples.contains(principle)) {
						System.out.println("ready to add " + principle);
						// mappingService.addMapPrinciple(principle);
					}
				}

				Set<MapRelation> relations = project.getMapRelations();
				List<MapRelation> currentRelations =
						mappingService.getMapRelations().getMapRelations();
				for (MapRelation relation : relations) {
					if (!currentRelations.contains(relation)) {
						System.out.println("ready to add " + relation);
						// mappingService.addMapRelation(relation);
					}
				}

				Set<MapAgeRange> ageRanges = project.getPresetAgeRanges();
				List<MapAgeRange> currentAgeRanges =
						mappingService.getMapAgeRanges().getMapAgeRanges();
				for (MapAgeRange mapAgeRange : ageRanges) {
					if (!currentAgeRanges.contains(mapAgeRange)) {
						System.out.println("ready to add " + mapAgeRange);
						// mappingService.addMapAgeRange(mapAgeRange);
					}
				}

				Set<MapUser> users = project.getMapLeads();
				users.addAll(project.getMapSpecialists());
				users.addAll(project.getMapAdministrators());
				List<MapUser> currentUsers = mappingService.getMapUsers().getMapUsers();
				for (MapUser user : users) {
					if (!currentUsers.contains(user)) {
						System.out.println("ready to add " + user);
						//mappingService.addMapUser(user);
					}
				}

				project.setId(null);
				mappingService.addMapProject(project);
				
			}
      
			// get all scope includes files
      FilenameFilter scopeIncludesFilter = new FilenameFilter() {
  			public boolean accept(File dir, String name) {
  				String lowercaseName = name.toLowerCase();
  				if (lowercaseName.endsWith("Scope.txt") && lowercaseName.startsWith("Project")) {
  					return true;
  				} else {
  					return false;
  				}
  			}
  		};
      File [] scopeIncludesFiles = inputDir.listFiles(scopeIncludesFilter);
      
      // read in the scope includes file for each project
      for (File scopeIncludesFile : scopeIncludesFiles) {
      	BufferedReader scopeIncludesReader =
            new BufferedReader(new FileReader(scopeIncludesFile));
      	
        String line = "";
        // hashmap of project id -> Set of concept terminology ids
        Set<String> conceptsInScope = new HashSet<>();
        
        // retrieve the project id 
        String projectId = scopeIncludesFile.getName().substring(
        		"Project".length(), scopeIncludesFile.getName().indexOf("Scope.txt"));
        
        while ((line = scopeIncludesReader.readLine()) != null) {
                   
          MapProject project = mappingService.getMapProject(new Long(projectId));

          Concept c = contentService.getConcept(line.trim(), project.getSourceTerminology(), 
          		project.getSourceTerminologyVersion());
          if (c == null) {
            getLog().warn("Scope Includes concept + " + line.trim() + " is not in the data.");
          } else {
  					conceptsInScope.add(line.trim());
  				}
        }

        // set the map project scope concepts and update the project
        MapProject mapProject = mappingService.getMapProject(new Long(projectId));
        mapProject.setScopeConcepts(conceptsInScope);
        mappingService.updateMapProject(mapProject);
       

        getLog().info(
            "  " + Integer.toString(conceptsInScope.size())
                + " included concepts added for "
                + mapProject.getName()
                + " projects.");

      	scopeIncludesReader.close();
      }

      
      FilenameFilter scopeExcludesFilter = new FilenameFilter() {
  			public boolean accept(File dir, String name) {
  				String lowercaseName = name.toLowerCase();
  				if (lowercaseName.endsWith("ScopeExcludes.txt") && lowercaseName.startsWith("Project")) {
  					return true;
  				} else {
  					return false;
  				}
  			}
  		};
      File [] scopeExcludesFiles = inputDir.listFiles(scopeExcludesFilter);
      for (File scopeExcludesFile : scopeExcludesFiles) {
        /*BufferedReader scopeExcludesReader =
          new BufferedReader(new FileReader(scopeExcludesFile));
        scopeExcludesReader.close();*/
      }

      
      
/* 
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
        mapProject.setGroupStructure(fields[i++].toLowerCase().equals("true")
            ? true : false);
        mapProject.setPublished(fields[i++].toLowerCase().equals("true") ? true
            : false);
        mapProject.setMapRelationStyle(fields[i++]);
        if (fields[i++].equals(WorkflowType.CONFLICT_PROJECT.toString())) 
        	mapProject.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
        else if (fields[i++].equals(WorkflowType.REVIEW_PROJECT.toString())) 
        	mapProject.setWorkflowType(WorkflowType.REVIEW_PROJECT);

        mapProject.setMapPrincipleSourceDocument(fields[i++]);
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
                  .getTotalCount()) + " projects added.");*/



      getLog().info("done ...");
      mappingService.close();
      contentService.close();
      

    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
