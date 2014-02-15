package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which imports project data from text files.
 * 
 * Sample execution:
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
	 * Instantiates a {@link MapProjectDataImportMojo} from the specified parameters.
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

		try {

			getLog().info("Starting Import Metadata");

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

			File specialistsFile = new File(inputDir, "mapspecialists.txt");
			BufferedReader specialistsReader =
					new BufferedReader(new FileReader(specialistsFile));

			File leadsFile = new File(inputDir, "mapleads.txt");
			BufferedReader leadsReader =
					new BufferedReader(new FileReader(leadsFile));

			File advicesFile = new File(inputDir, "mapadvices.txt");
			BufferedReader advicesReader =
					new BufferedReader(new FileReader(advicesFile));

			File principlesFile = new File(inputDir, "mapprinciples.txt");
			BufferedReader principlesReader =
					new BufferedReader(new FileReader(principlesFile));

			File projectsFile = new File(inputDir, "mapprojects.txt");
			BufferedReader projectsReader =
					new BufferedReader(new FileReader(projectsFile));

			MappingService mappingService = new MappingServiceJpa();

			// Add Specialists and Leads
			String line = "";
			while ((line = leadsReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapLeadJpa mapLead = new MapLeadJpa();
				mapLead.setName(st.nextToken());
				mapLead.setUserName(st.nextToken());
				mapLead.setEmail(st.nextToken());
				mappingService.addMapLead(mapLead);
			}

			while ((line = specialistsReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
				mapSpecialist.setName(st.nextToken());
				mapSpecialist.setUserName(st.nextToken());
				mapSpecialist.setEmail(st.nextToken());
				mappingService.addMapSpecialist(mapSpecialist);
			}

			// Add map advices
			while ((line = advicesReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapAdviceJpa mapAdvice = new MapAdviceJpa();
				mapAdvice.setName(st.nextToken());
				mapAdvice.setDetail(st.nextToken());
				mappingService.addMapAdvice(mapAdvice);
			}

			// Add map principles
			while ((line = principlesReader.readLine()) != null) {
				String[] fields = line.split("\\|");
				MapPrincipleJpa mapPrinciple = new MapPrincipleJpa();
				mapPrinciple.setName(fields[0]);
				mapPrinciple.setPrincipleId(fields[1]);
				mapPrinciple.setSectionRef(fields[2]);
				mapPrinciple.setDetail(fields[3]);
				mappingService.addMapPrinciple(mapPrinciple);
			}

			// Add map projects
			while ((line = projectsReader.readLine()) != null) {
				String[] fields = line.split("\t");
				MapProjectJpa mapProject = new MapProjectJpa();
				mapProject.setName(fields[0]);
				mapProject.setRefSetId(fields[1]);
				mapProject.setRefSetName(fields[2]);
				mapProject.setObjectId(fields[3]);
				mapProject.setSourceTerminology(fields[4]);
				mapProject.setSourceTerminologyVersion(fields[5]);
				mapProject.setDestinationTerminology(fields[6]);
				mapProject.setDestinationTerminologyVersion(fields[7]);
				mapProject.setBlockStructure(fields[8].equals("true") ? true : false);
				mapProject.setGroupStructure(fields[9].equals("true") ? true : false);
				mapProject.setPublished(fields[10].equals("true") ? true : false);
				mapProject.setMapRelationStyle(fields[11]);
				mapProject.setMapPrincipleSourceDocument(fields[12]);
				mapProject.setRuleBased(fields[14].equals("true") ? true : false);
				
				String mapAdvices = fields[15];
				if (!mapAdvices.equals("")) {
					for (String advice : mapAdvices.split(",")) {
						for (MapAdvice ml : mappingService.getMapAdvices()) {
							if (ml.getName().equals(advice))
								mapProject.addMapAdvice(ml);
						}
					}
				}

				String mapPrinciples = fields[16];
				if (!mapPrinciples.equals("")) {
					for (String principle : mapPrinciples.split(",")) {
						for (MapPrinciple ml : mappingService.getMapPrinciples()) {
							if (ml.getPrincipleId().equals(principle)) {
								mapProject.addMapPrinciple(ml);
							}
						}
					}
				}

				String mapLeads = fields[17];
				for (String lead : mapLeads.split(",")) {
					for (MapLead ml : mappingService.getMapLeads()) {
						if (ml.getUserName().equals(lead))
							mapProject.addMapLead(ml);
					}
				}

				String mapSpecialists = fields[18];
				for (String specialist : mapSpecialists.split(",")) {
					for (MapSpecialist ml : mappingService.getMapSpecialists()) {
						if (ml.getUserName().equals(specialist))
							mapProject.addMapSpecialist(ml);
					}
				}
				mappingService.addMapProject(mapProject);
			}

			mappingService.close();

			specialistsReader.close();
			leadsReader.close();
			advicesReader.close();
			principlesReader.close();
			projectsReader.close();

		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}
}
