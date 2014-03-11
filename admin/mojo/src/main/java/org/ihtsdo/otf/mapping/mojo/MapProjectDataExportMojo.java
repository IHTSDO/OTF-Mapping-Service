package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapUserList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which exports project data to text files.
 * 
 * Sample execution;
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <dependencies>
 *         <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-export-config</artifactId>
 *           <version>${project.version}</version>
 *           <scope>system</scope>
 *           <systemPath>${project.build.directory}/mapping-admin-export-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>export-project-data</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>export-project-data</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal export-project-data
 * 
 * @phase package
 */
public class MapProjectDataExportMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;

	/**
	 * Instantiates a {@link MapProjectDataExportMojo} from the specified parameters.
	 * 
	 */
	public MapProjectDataExportMojo() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		getLog().info("Starting exporting metadata ...");

		try {


			FileInputStream propertiesInputStream = null;

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);

			// set the output directory
			String outputDirString = properties.getProperty("export.output.dir");
			propertiesInputStream.close();
			File outputDir = new File(outputDirString);
			if (!outputDir.exists()) {
				throw new MojoFailureException(
						"Specified export.output.dir directory does not exist: "
								+ outputDirString);
			}

			File usersFile = new File(outputDir, "mapusers.txt");
			// if file doesn't exist, then create it
			if (!usersFile.exists()) {
				usersFile.createNewFile();
			}
			BufferedWriter usersWriter =
					new BufferedWriter(new FileWriter(usersFile.getAbsoluteFile()));

		
			File advicesFile = new File(outputDir, "mapadvices.txt");
			// if file doesn't exist, then create it
			if (!advicesFile.exists()) {
				advicesFile.createNewFile();
			}
			BufferedWriter advicesWriter =
					new BufferedWriter(new FileWriter(advicesFile.getAbsoluteFile()));

			File principlesFile = new File(outputDir, "mapprinciples.txt");
			// if file doesn't exist, then create it
			if (!principlesFile.exists()) {
				principlesFile.createNewFile();
			}
			BufferedWriter principlesWriter =
					new BufferedWriter(new FileWriter(principlesFile.getAbsoluteFile()));

			File projectsFile = new File(outputDir, "mapprojects.txt");
			// if file doesn't exist, then create it
			if (!projectsFile.exists()) {
				projectsFile.createNewFile();
			}
			BufferedWriter projectsWriter =
					new BufferedWriter(new FileWriter(projectsFile.getAbsoluteFile()));

			File agerangesFile = new File(outputDir, "mapageranges.txt");
			
			// if file doesn't exist, then create it
			if (!agerangesFile.exists()) {
				agerangesFile.createNewFile();
			}
			BufferedWriter agerangesWriter =
					new BufferedWriter(new FileWriter(agerangesFile.getAbsoluteFile()));


			
			File scopeIncludesFile = new File(outputDir, "scopeIncludes.txt");
			// if file doesn't exist, then create it
			if (!scopeIncludesFile.exists()) {
				scopeIncludesFile.createNewFile();
			}
			BufferedWriter scopeIncludesWriter =
					new BufferedWriter(new FileWriter(scopeIncludesFile.getAbsoluteFile()));

			File scopeExcludesFile = new File(outputDir, "scopeExcludes.txt");
			// if file doesn't exist, then create it
			if (!scopeExcludesFile.exists()) {
				scopeExcludesFile.createNewFile();
			}
			BufferedWriter scopeExcludesWriter =
					new BufferedWriter(new FileWriter(scopeExcludesFile.getAbsoluteFile()));
			
			// export to mapspecialists.txt
			MappingService mappingService = new MappingServiceJpa();
			MapUserList mapUsers = new MapUserList();
			mapUsers.setMapUsers(mappingService.getMapUsers());
			mapUsers.sortMapUsers();
			for (MapUser ms : mapUsers.getMapUsers()) {
				usersWriter.write(ms.getName() + "\t" + ms.getUserName() + "\t"
						+ ms.getEmail() + "\n");
			}

			// export to mapadvices.txt
			for (MapAdvice ma : mappingService.getMapAdvices()) {
				advicesWriter.write(ma.getName() + "\t" + ma.getDetail() + "\n");
			}

			// export to mapprinciples.txt
			for (MapPrinciple ma : mappingService.getMapPrinciples()) {
				String detail = ma.getDetail();
				detail = detail.replace("\n", "<br>").replace("\r", "<br>");
				principlesWriter.write(ma.getName() + "|" + ma.getPrincipleId() + "|"
						+ ma.getSectionRef() + "|" + detail + "\n");
			}
			
			// export to mapprojects.txt
			for (MapProject mpr : mappingService.getMapProjects()) {
				StringBuffer mapAdvices = new StringBuffer();
				for (MapAdvice ma : mpr.getMapAdvices()) {
					mapAdvices.append(ma.getName()).append(",");
				}
				if (mapAdvices.length() > 1)
					mapAdvices.deleteCharAt(mapAdvices.length() - 1);

				StringBuffer mapPrinciples = new StringBuffer();
				for (MapPrinciple ma : mpr.getMapPrinciples()) {
					mapPrinciples.append(ma.getPrincipleId()).append(",");
				}
				if (mapPrinciples.length() > 1)
					mapPrinciples.deleteCharAt(mapPrinciples.length() - 1);

				StringBuffer mprMapLeads = new StringBuffer();
				for (MapUser ma : mpr.getMapLeads()) {
					mprMapLeads.append(ma.getUserName()).append(",");
				}
				if (mprMapLeads.length() > 1)
					mprMapLeads.deleteCharAt(mprMapLeads.length() - 1);

				StringBuffer mprMapSpecialists = new StringBuffer();
				for (MapUser ma : mpr.getMapSpecialists()) {
					mprMapSpecialists.append(ma.getUserName()).append(",");
				}
				if (mprMapSpecialists.length() > 1)
					mprMapSpecialists.deleteCharAt(mprMapSpecialists.length() - 1);
				
				projectsWriter.write(mpr.getName() + "\t" + mpr.getRefSetId() + "\t"
						+ mpr.getRefSetName() + "\t"
						+ mpr.getSourceTerminology() + "\t"
						+ mpr.getSourceTerminologyVersion() + "\t"
						+ mpr.getDestinationTerminology() + "\t"
						+ mpr.getDestinationTerminologyVersion() + "\t"
						+ mpr.isBlockStructure() + "\t" + mpr.isGroupStructure() + "\t"
						+ mpr.isPublished() + "\t" 
						+ mpr.getMapRelationStyle() + "\t"
						+ mpr.getMapPrincipleSourceDocument() + "\t"
						+ mpr.isRuleBased() + "\t"
						+ mpr.getMapRefsetPattern() + "\t"
						+ mapAdvices + "\t" 
						+ mapPrinciples + "\t"
						+ mprMapLeads + "\t" 
						+ mprMapSpecialists + "\t"
						+ mpr.isScopeDescendantsFlag() + "\t" 
						+ mpr.isScopeExcludedDescendantsFlag() + "\n");
				
				
				// add to mapagerangesbyproject.txt
				for (MapAgeRange ar : mpr.getPresetAgeRanges()) {
					agerangesWriter.write(
							mpr.getRefSetId() + "|" + ar.getName() + "|" 
						+   ar.getLowerValue() + "|" + ar.getLowerUnits() + "|"	+ (ar.getLowerInclusive() == true ? "true" : "false") + "|"	
						+   ar.getUpperValue() + "|" + ar.getUpperUnits() + "|" + (ar.getUpperInclusive() == true ? "true" : "false") + "\n");		
					}
			}
			
			for (MapProject mpr : mappingService.getMapProjects()) {
				for (String concept : mpr.getScopeConcepts()) {
					scopeIncludesWriter.write(mpr.getRefSetId() + "\t" + concept + "\n");
				}
			}
			
			for (MapProject mpr : mappingService.getMapProjects()) {
				for (String concept : mpr.getScopeExcludedConcepts()) {
					scopeExcludesWriter.write(mpr.getRefSetId() + "\t" + concept + "\n");
				}
			}
			
			mappingService.close();

			getLog().info("done ...");
			usersWriter.close();
			advicesWriter.close();
			principlesWriter.close();
			projectsWriter.close();
			scopeIncludesWriter.close();
			scopeExcludesWriter.close();
			agerangesWriter.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
