package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceList;
import org.ihtsdo.otf.mapping.jpa.MapLeadList;
import org.ihtsdo.otf.mapping.jpa.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which updates the db to sync it with the model via JPA.
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
 * @goal export-metadata
 * 
 * @phase process-resources
 */
public class ExportMetadataMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;
	
	

	/**
	 * Instantiates a {@link ExportMetadataMojo} from the specified parameters.
	 * 
	 */
	public ExportMetadataMojo() {
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
			 		
			getLog().info("Starting Export Metadata");
			
			FileInputStream propertiesInputStream = null;


				// load Properties file
				Properties properties = new Properties();
				propertiesInputStream = new FileInputStream(propertiesFile);
				properties.load(propertiesInputStream);

				// set the output directory
				String outputDirString = properties
						.getProperty("export.output.dir");
				propertiesInputStream.close();
				File outputDir = new File(outputDirString);
				if (!outputDir.exists()) {
					throw new MojoFailureException(
							"Specified export.output.dir directory does not exist: "
									+ outputDirString);
				}

	
			File specialistsFile = new File(outputDir, "mapSpecialists.txt");
			// if file doesn't exist, then create it
			if (!specialistsFile.exists()) {
				specialistsFile.createNewFile();
			}
			BufferedWriter specialistsWriter = new BufferedWriter(new FileWriter(specialistsFile.getAbsoluteFile()));

			File leadsFile = new File(outputDir, "mapleads.txt");
			// if file doesn't exist, then create it
			if (!leadsFile.exists()) {
				leadsFile.createNewFile();
			}
			BufferedWriter leadsWriter = new BufferedWriter(new FileWriter(leadsFile.getAbsoluteFile()));

			File advicesFile = new File(outputDir, "mapadvices.txt");
			// if file doesn't exist, then create it
			if (!advicesFile.exists()) {
				advicesFile.createNewFile();
			}
			BufferedWriter advicesWriter = new BufferedWriter(new FileWriter(advicesFile.getAbsoluteFile()));
			
			// export to mapspecialists.txt
			MappingService mappingService = new MappingServiceJpa();
			MapSpecialistList mapSpecialists = new MapSpecialistList();
			mapSpecialists.setMapSpecialists(mappingService.getMapSpecialists());
			mapSpecialists.sortMapSpecialists();
			for (MapSpecialist ms : mapSpecialists.getMapSpecialists()) {
				specialistsWriter.write(ms.getName() + "\t" + ms.getUserName() + "\t" +
						ms.getEmail() + "\n");
			}
			
			// export to mapleads.txt
			MapLeadList mapLeads = new MapLeadList();
			mapLeads.setMapLeads(mappingService.getMapLeads());
			mapLeads.sortMapLeads();	
			for (MapLead ml : mapLeads.getMapLeads()) {
				leadsWriter.write(ml.getName() + "\t" + ml.getUserName() + "\t" +
						ml.getEmail() + "\n");
			}
			
			//export to mapAdvices.txt
			MapProjectList mapProjects = new MapProjectList();
			mapProjects.setMapProjects(mappingService.getMapProjects());
			MapAdviceList mapAdvices = new MapAdviceList();
			for (MapProject mp : mapProjects.getMapProjects()) {
				Set<MapAdvice> mapAdviceSet = mp.getMapAdvices();
				for (MapAdvice ma : mapAdviceSet) {
					mapAdvices.addMapAdvice(ma);
				  advicesWriter.write(ma.getName() + "\t" + ma.getDetail() + "\t" +
				  		ma.getId() + "\t" + mp.getName() + "\n");
				}
			}
			mappingService.close();		
			
			

			getLog().info("...done");
			specialistsWriter.close();
			leadsWriter.close();
			advicesWriter.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
