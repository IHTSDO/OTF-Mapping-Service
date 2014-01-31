package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapLeadList;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which exports project data to text files.
 * 
 * <pre>
          <plugin>
            <groupId>org.ihtsdo.otf.mapping</groupId>
            <artifactId>mapping-admin-mojo</artifactId>
            <version>${project.version}</version>
            <dependencies>
              <dependency>
                <groupId>org.ihtsdo.otf.mapping</groupId>
                <artifactId>mapping-admin-export-config</artifactId>
                <version>${project.version}</version>
                <scope>system</scope>
                <systemPath>${project.build.directory}/mapping-admin-export-${project.version}.jar</systemPath>
              </dependency>
            </dependencies>
            <executions>
              <execution>
                <id>export-project-data</id>
                <phase>package</phase>
                <goals>
                  <goal>export-project-data</goal>
                </goals>
                <configuration>
                  <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
                </configuration>
              </execution>
            </executions>
          </plugin>
 * </pre>
 * 
 * @goal export-project-data
 * 
 * @phase process-resources
 */
public class ExportProjectDataMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;
	
	

	/**
	 * Instantiates a {@link ExportProjectDataMojo} from the specified parameters.
	 * 
	 */
	public ExportProjectDataMojo() {
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

	
			File specialistsFile = new File(outputDir, "mapspecialists.txt");
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
			
			File principlesFile = new File(outputDir, "mapprinciples.txt");
			// if file doesn't exist, then create it
			if (!principlesFile.exists()) {
				principlesFile.createNewFile();
			}
			BufferedWriter principlesWriter = new BufferedWriter(new FileWriter(principlesFile.getAbsoluteFile()));
			
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
			

			
			//export to mapadvices.txt and mapprinciples.txt
			for (MapAdvice ma : mappingService.getMapAdvices()) {
				advicesWriter.write(ma.getName() + "\t" + ma.getDetail() + "\n");
			}
		  for (MapPrinciple ma : mappingService.getMapPrinciples()) {
		  	String detail = ma.getDetail();
		  	detail = detail.replace("\n", "<br>").replace("\r", "<br>");
			  principlesWriter.write(ma.getName() + "|" + detail + "\n");
		  }				
			mappingService.close();		
			
			
			getLog().info("...done");
			specialistsWriter.close();
			leadsWriter.close();
			advicesWriter.close();
			principlesWriter.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
