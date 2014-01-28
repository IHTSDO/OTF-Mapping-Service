package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which imports metadata from text files.
 * 
 * <pre>
 *   <plugin>
 *      <groupId>org.ihtsdo.otf.mapping</groupId>
 *      <artifactId>mapping-admin-mojo</artifactId>
 *      <version>${project.version}</version>
 *      <dependencies>
 *        <dependency>
 *          <groupId>org.ihtsdo.otf.mapping</groupId>
 *          <artifactId>mapping-admin-sampledata-config</artifactId>
 *          <version>${project.version}</version>
 *          <scope>system</scope>
 *          <systemPath>${project.build.directory}/mapping-admin-sampledata-${project.version}.jar</systemPath>
 *        </dependency>
 *      </dependencies>
 *      <executions>
 *        <execution>
 *          <id>sampledata</id>
 *          <phase>package</phase>
 *          <goals>
 *            <goal>sampledata</goal>
 *          </goals>
 *          <configuration>
 *            <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *          </configuration>
 *        </execution>
 *      </executions>
 *    </plugin>
 * </pre>
 * 
 * @goal import-metadata
 * 
 * @phase process-resources
 */
public class ImportMetadataMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;

	/** The manager. */
	private EntityManager manager;

	/**
	 * Instantiates a {@link ImportMetadataMojo} from the specified parameters.
	 * 
	 */
	public ImportMetadataMojo() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {

		EntityManagerFactory factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();

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

			File specialistsFile = new File(inputDir, "mapSpecialists.txt");
			BufferedReader specialistsReader =
					new BufferedReader(new FileReader(specialistsFile));

			File leadsFile = new File(inputDir, "mapleads.txt");
			BufferedReader leadsReader =
					new BufferedReader(new FileReader(leadsFile));

			File advicesFile = new File(inputDir, "mapadvices.txt");
			BufferedReader advicesReader =
					new BufferedReader(new FileReader(advicesFile));

			List<MapProject> advices = new ArrayList<>();
			List<MapSpecialist> specialists = new ArrayList<>();
			List<MapLead> leads = new ArrayList<>();

			// Add Specialists and Leads
			String line = "";
			while ((line = leadsReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapLeadJpa mapLead = new MapLeadJpa();
				mapLead.setName(st.nextToken());
				mapLead.setUserName(st.nextToken());
				mapLead.setEmail(st.nextToken());
				leads.add(mapLead);
			}

			while ((line = specialistsReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
				mapSpecialist.setName(st.nextToken());
				mapSpecialist.setUserName(st.nextToken());
				mapSpecialist.setEmail(st.nextToken());
				specialists.add(mapSpecialist);
			}

			tx.begin();
			for (MapSpecialist m : specialists) {
				Logger.getLogger(this.getClass()).info(
						"Adding map specialist " + m.getName());
				manager.persist(m);
			}

			for (MapLead m : leads) {
				Logger.getLogger(this.getClass())
						.info("Adding map lead " + m.getName());
				manager.persist(m);
			}
			tx.commit();

			// Add map advices
			Map<String, Set<MapAdvice>> mapProjectToMapAdvices = new HashMap<String, Set<MapAdvice>>();
			while ((line = advicesReader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				MapAdviceJpa mapAdvice = new MapAdviceJpa();
				mapAdvice.setName(st.nextToken());
				mapAdvice.setDetail(st.nextToken());
				String mapProject = st.nextToken();
				if (mapProjectToMapAdvices.containsKey(mapProject)) {
				  Set<MapAdvice> mapAdvices = mapProjectToMapAdvices.get(mapProject);
				  if (!mapAdvices.contains(mapAdvice))
				  	mapAdvices.add(mapAdvice);
				} else {  // no prior advices with given mapProject
					Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
					mapAdvices.add(mapAdvice);
					mapProjectToMapAdvices.put(mapProject, mapAdvices);
				}
			}
			// for each mapProject installed, check if there are mapAdvices to add to it
			MappingService mappingService = new MappingServiceJpa();
			for (MapProject mapProject : mappingService.getMapProjects()) {
			  Set<MapAdvice> mapAdvices = mapProjectToMapAdvices.get(mapProject.getName());
				mapProject.setMapAdvices(mapAdvices);	
			}
			
			
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}
}
