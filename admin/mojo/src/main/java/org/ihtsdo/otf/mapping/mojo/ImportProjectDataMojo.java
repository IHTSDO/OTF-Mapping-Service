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
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which imports project data from text files.
 * 
 * <pre>
          <plugin>
            <groupId>org.ihtsdo.otf.mapping</groupId>
            <artifactId>mapping-admin-mojo</artifactId>
            <version>${project.version}</version>
            <dependencies>
              <dependency>
                <groupId>org.ihtsdo.otf.mapping</groupId>
                <artifactId>mapping-admin-import-config</artifactId>
                <version>${project.version}</version>
                <scope>system</scope>
                <systemPath>${project.build.directory}/mapping-admin-import-${project.version}.jar</systemPath>
              </dependency>
            </dependencies>
            <executions>
              <execution>
                <id>import-project-data</id>
                <phase>package</phase>
                <goals>
                  <goal>import-project-data</goal>
                </goals>
                <configuration>
                  <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
                </configuration>
              </execution>
            </executions>
          </plugin>
 * </pre>
 * 
 * @goal import-project-data
 * 
 * @phase process-resources
 */
public class ImportProjectDataMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;


	/**
	 * Instantiates a {@link ImportProjectDataMojo} from the specified parameters.
	 * 
	 */
	public ImportProjectDataMojo() {
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
				StringTokenizer st = new StringTokenizer(line, "|");
				MapPrincipleJpa mapPrinciple = new MapPrincipleJpa();
				mapPrinciple.setName(st.nextToken());
				mapPrinciple.setDetail(st.nextToken());
				mappingService.addMapPrinciple(mapPrinciple);
			}
			
			mappingService.close();
			
			specialistsReader.close();
			leadsReader.close();
			advicesReader.close();
			principlesReader.close();
			
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}
}
