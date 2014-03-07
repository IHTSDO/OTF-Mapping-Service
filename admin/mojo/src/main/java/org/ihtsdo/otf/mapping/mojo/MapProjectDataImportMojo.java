package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
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

			File principlesFile = new File(inputDir, "mapprinciples.txt");
			BufferedReader principlesReader =
					new BufferedReader(new FileReader(principlesFile));

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
			
			getLog().info("  " + Integer.toString(mappingService.getMapUsers().size()) + " users added.");

			
			// Add map advices
			getLog().info("Adding advices...");
			
			while ((line = advicesReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapAdviceJpa mapAdvice = new MapAdviceJpa();
				mapAdvice.setName(st.nextToken());
				mapAdvice.setDetail(st.nextToken());
				mappingService.addMapAdvice(mapAdvice);
			}
			
			getLog().info("  " + Integer.toString(mappingService.getMapAdvices().size()) + " advices added.");


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
			getLog().info("  " + Integer.toString(mappingService.getMapPrinciples().size()) + " principles added.");


			// Add map projects
			getLog().info("Adding projects...");
			while ((line = projectsReader.readLine()) != null) {
				String[] fields = line.split("\t");
				MapProjectJpa mapProject = new MapProjectJpa();
				mapProject.setName(fields[0]);
				mapProject.setRefSetId(fields[1]);
				mapProject.setRefSetName(fields[2]);
				mapProject.setSourceTerminology(fields[3]);
				mapProject.setSourceTerminologyVersion(fields[4]);
				mapProject.setDestinationTerminology(fields[5]);
				mapProject.setDestinationTerminologyVersion(fields[6]);
				mapProject.setBlockStructure(fields[7].equals("true") ? true : false);
				mapProject.setGroupStructure(fields[8].equals("true") ? true : false);
				mapProject.setPublished(fields[9].equals("true") ? true : false);
				mapProject.setMapRelationStyle(fields[10]);
				mapProject.setMapPrincipleSourceDocument(fields[11]);
				mapProject.setRuleBased(fields[12].equals("true") ? true : false);
				mapProject.setMapRefsetPattern(fields[13]);

				String mapAdvices = fields[14];
				if (!mapAdvices.equals("")) {
					for (String advice : mapAdvices.split(",")) {
						for (MapAdvice ml : mappingService.getMapAdvices()) {
							if (ml.getName().equals(advice))
								mapProject.addMapAdvice(ml);
						}
					}
				}

				String mapPrinciples = fields[15];
				if (!mapPrinciples.equals("")) {
					for (String principle : mapPrinciples.split(",")) {
						for (MapPrinciple ml : mappingService.getMapPrinciples()) {
							if (ml.getPrincipleId().equals(principle)) {
								mapProject.addMapPrinciple(ml);
							}
						}
					}
				}

				String mapLeads = fields[16];
				for (String lead : mapLeads.split(",")) {
					for (MapUser ml : mappingService.getMapUsers()) {
						if (ml.getUserName().equals(lead))
							mapProject.addMapLead(ml);
					}
				}

				String mapSpecialists = fields[17];
				for (String specialist : mapSpecialists.split(",")) {

					for (MapUser ml : mappingService.getMapUsers()) {
						if (ml.getUserName().equals(specialist))
							mapProject.addMapSpecialist(ml);
					}
				}

				mapProject.setScopeDescendantsFlag(fields[18].equals("true") ? true
						: false);
				mapProject.setScopeExcludedDescendantsFlag(fields[19].equals("true")
						? true : false);


				/*
				 * String rulePresetAgeRangesStr = fields[19]; Set<String>
				 * rulePresetAgeRanges = new HashSet<String>(); for (String preset :
				 * rulePresetAgeRangesStr.split(",")) { rulePresetAgeRanges.add(preset);
				 * } mapProject.setRulePresetAgeRanges(rulePresetAgeRanges);
				 */

				mappingService.addMapProject(mapProject);
			}
			
			getLog().info("  " + Integer.toString(mappingService.getMapProjects().size()) + " projects added.");


			// Concepts In Scope Assignment
			
			// hashmap of project id -> Set of concept terminology ids
			Map<Long, Set<String>> projectToConceptsInScope = new HashMap<Long, Set<String>>();
			
			// cycle over the includes file
			
			getLog().info("Adding scope includes...");
			
			while ((line = scopeIncludesReader.readLine()) != null) {
				
				String[] fields = line.split("\\t");
				
				// retrieve the project id associated with this refSetId
				Long projectId = mappingService.getMapProjectByRefSetId(fields[0]).getId();
	
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
				conceptsIncludedInScopeCount += projectToConceptsInScope.get(projectId).size();
			}
			
			getLog().info("  " + Integer.toString(conceptsIncludedInScopeCount) + " included concepts added for " + Integer.toString(projectToConceptsInScope.keySet().size()) + " projects.");

			
			// Concepts Excluded From Scope Assignmnet
			
			getLog().info("Adding scope excludes...");
			
			// map of project ids -> set of concept terminology Ids
			Map<Long, Set<String>> projectToConceptsExcludedFromScope = new HashMap<Long, Set<String>>();			
		
			// cycle over the exclude file
			while ((line = scopeExcludesReader.readLine()) != null) {
	
				String[] fields = line.split("\\t");
				
				// retrieve the project id associated with this refSetId
				Long projectId = mappingService.getMapProjectByRefSetId(fields[0]).getId();
				
				// if project in set, add this new set of concept ids
				if (projectToConceptsExcludedFromScope.containsKey(projectId))
					projectToConceptsExcludedFromScope.get(projectId).add(fields[1]);
				
				// otherwise, insert project into hash set with the excluded concept list
				else {
					Set<String> conceptsExcludedFromScope = new HashSet<String>();
					conceptsExcludedFromScope.add(fields[1]);
					projectToConceptsExcludedFromScope.put(projectId, conceptsExcludedFromScope);
				}
			}
			
			// cycle over detected project excludes and update map projects
			int conceptsExcludedFromScopeCount = 0;
			for (Long projectId : projectToConceptsExcludedFromScope.keySet()) {
				MapProject mapProject = mappingService.getMapProject(new Long(projectId));
				mapProject.setScopeExcludedConcepts(projectToConceptsExcludedFromScope.get(projectId));
				mappingService.updateMapProject(mapProject);
				conceptsExcludedFromScopeCount += mapProject.getScopeExcludedConcepts().size();
			}
			
			getLog().info("  " + Integer.toString(conceptsExcludedFromScopeCount) + " excluded concepts added for " + Integer.toString(projectToConceptsExcludedFromScope.keySet().size()) + " projects.");


			getLog().info("done ...");
			mappingService.close();
			usersReader.close();
			advicesReader.close();
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
