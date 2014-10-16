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
  			@Override
        public boolean accept(File dir, String name) {
  				String lowercaseName = name.toLowerCase();
  				if (lowercaseName.endsWith(".xml") && lowercaseName.startsWith("project")) {
  					return true;
  				} else {
  					return false;
  				}
  			}
  		};
      File [] projectFiles = inputDir.listFiles(projectFilter);
      
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();
      
      JAXBContext context = JAXBContext.newInstance(MapProjectJpa.class);
	  Unmarshaller unmarshaller = context.createUnmarshaller();
		
      
			// read project .xml files one at a time
			for (File projectFile : projectFiles) {
				BufferedReader projectReader =
						new BufferedReader(new FileReader(projectFile));

				// Unmarshal from XML
				MapProject project =
						(MapProjectJpa) unmarshaller.unmarshal(new StreamSource(
								new StringReader(projectReader.readLine())));
				projectReader.close();
				
				Set<MapAdvice> advices = project.getMapAdvices();
				List<MapAdvice> currentAdvices =
						mappingService.getMapAdvices().getMapAdvices();
				for (MapAdvice advice : advices) {
					if (!currentAdvices.contains(advice)) {
						// System.out.println("ready to add " + advice);
						advice.setId(null);
						mappingService.addMapAdvice(advice);
					}
				}

				Set<MapPrinciple> principles = project.getMapPrinciples();
				List<MapPrinciple> currentPrinciples =
						mappingService.getMapPrinciples().getMapPrinciples();
				for (MapPrinciple principle : principles) {
					if (!currentPrinciples.contains(principle)) {
						// System.out.println("ready to add " + principle);
						principle.setId(null);
						mappingService.addMapPrinciple(principle);
					}
				}

				Set<MapRelation> relations = project.getMapRelations();
				List<MapRelation> currentRelations =
						mappingService.getMapRelations().getMapRelations();
				for (MapRelation relation : relations) {
					if (!currentRelations.contains(relation)) {
						// System.out.println("ready to add " + relation);
						relation.setId(null);
						mappingService.addMapRelation(relation);
					}
				}

				Set<MapAgeRange> ageRanges = project.getPresetAgeRanges();
				List<MapAgeRange> currentAgeRanges =
						mappingService.getMapAgeRanges().getMapAgeRanges();
				for (MapAgeRange mapAgeRange : ageRanges) {
					if (!currentAgeRanges.contains(mapAgeRange)) {
						// System.out.println("ready to add " + mapAgeRange);
						mapAgeRange.setId(null);
						mappingService.addMapAgeRange(mapAgeRange);
					}
				}

				Set<MapUser> leads = project.getMapLeads();
				Set<MapUser> specialists = project.getMapSpecialists();
				Set<MapUser> admins = project.getMapAdministrators();
				List<MapUser> currentUsers = mappingService.getMapUsers().getMapUsers();
				for (MapUser user : leads) {
					if (!currentUsers.contains(user)) {
						// System.out.println("ready to add " + user);
						user.setId(null);
						mappingService.addMapUser(user);
					}
				}
				for (MapUser user : specialists) {
					if (!currentUsers.contains(user)) {
						// System.out.println("ready to add " + user);
						user.setId(null);
						mappingService.addMapUser(user);
					}
				}
				for (MapUser user : admins) {
					if (!currentUsers.contains(user)) {
						// System.out.println("ready to add " + user);
						user.setId(null);
						mappingService.addMapUser(user);
					}
				}

				// copy project
				MapProject bareProject = new MapProjectJpa(project);
				
				// clear copied project of all collections
				bareProject.setMapAdministrators(new HashSet<MapUser>());
				bareProject.setMapAdvices(new HashSet<MapAdvice>());
				bareProject.setMapLeads(new HashSet<MapUser>());
				bareProject.setMapSpecialists(new HashSet<MapUser>());
				bareProject.setMapPrinciples(new HashSet<MapPrinciple>());
				bareProject.setMapRelations(new HashSet<MapRelation>());
				bareProject.setId(null);
				
				// add the blank project
				mappingService.addMapProject(bareProject);
				
				// go back and cycle over one at a time and add them
				for (MapUser specialist : project.getMapSpecialists()) {
					MapUser user = mappingService.getMapUser(specialist.getUserName());
					bareProject.addMapSpecialist(user);
				}
				mappingService.updateMapProject(bareProject);
				for (MapRelation relation : project.getMapRelations()) {
					for (MapRelation rel : mappingService.getMapRelations().getMapRelations()) {
						if (rel.equals(relation)) {
							bareProject.addMapRelation(rel);
						}
					}
				}
				mappingService.updateMapProject(bareProject);
				for (MapPrinciple principle : project.getMapPrinciples()) {
					for (MapPrinciple pcpl : mappingService.getMapPrinciples().getMapPrinciples()) {
						if (pcpl.equals(principle)) {
							bareProject.addMapPrinciple(pcpl);
						}
					}
				}
				mappingService.updateMapProject(bareProject);
				for (MapAdvice advice : project.getMapAdvices()) {
					for (MapAdvice avc : mappingService.getMapAdvices().getMapAdvices()) {
						if (avc.equals(advice)) {
							bareProject.addMapAdvice(avc);
						}
					}
				}
				mappingService.updateMapProject(bareProject);	
				for (MapUser lead : project.getMapLeads()) {
					MapUser user = mappingService.getMapUser(lead.getUserName());
					bareProject.addMapLead(user);
				}
				mappingService.updateMapProject(bareProject);
				for (MapUser administrator : project.getMapAdministrators()) {
					MapUser user = mappingService.getMapUser(administrator.getUserName());
					bareProject.addMapAdministrator(user);
				}
				mappingService.updateMapProject(bareProject);

				// add scope concepts to project from Project*Scope.txt file
				BufferedReader scopeIncludesReader = new BufferedReader(
						new FileReader(new File(projectFile.getAbsolutePath().replace(".xml", "Scope.txt"))));

				String line = "";
				// hashmap of project id -> Set of concept terminology ids
				Set<String> conceptsInScope = new HashSet<>();

				while ((line = scopeIncludesReader.readLine()) != null) {

					Concept c = contentService.getConcept(line.trim(),
							project.getSourceTerminology(),
							project.getSourceTerminologyVersion());
					if (c == null) {
						getLog().warn(
								"Scope Includes concept + " + line.trim()
										+ " is not in the data.");
					} else {
						conceptsInScope.add(line.trim());
					}
				}

				// set the map project scope concepts and update the project
				bareProject.setScopeConcepts(conceptsInScope);
				mappingService.updateMapProject(bareProject);

				getLog().info(
						"  " + Integer.toString(conceptsInScope.size())
								+ " included concepts added for "
								+ bareProject.getName() + " projects.");

				scopeIncludesReader.close();

				// add scope concepts to project from Project*ScopeExcludes.txt file
				BufferedReader scopeExcludesReader = new BufferedReader(
						new FileReader(new File(projectFile.getAbsolutePath().replace(".xml", "ScopeExcludes.txt"))));

				while ((line = scopeExcludesReader.readLine()) != null) {

					Concept c = contentService.getConcept(line.trim(),
							project.getSourceTerminology(),
							project.getSourceTerminologyVersion());
					if (c == null) {
						getLog().warn(
								"Scope Excludes concept + " + line.trim()
										+ " is not in the data.");
					} else {
						conceptsInScope.add(line.trim());
					}
				}

				// set the map project scope concepts and update the project
				bareProject.setScopeConcepts(conceptsInScope);
				mappingService.updateMapProject(bareProject);

				getLog().info(
						"  " + Integer.toString(conceptsInScope.size())
								+ " excluded concepts added for "
								+ bareProject.getName() + " projects.");

				scopeExcludesReader.close();
			}

			getLog().info("done ...");
			mappingService.close();
			contentService.close();

    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
