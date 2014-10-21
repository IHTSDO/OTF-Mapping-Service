package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution in pom.xml:
 * 
 * <pre>
 *     <profile>
 *       <id>Release</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>release</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>release</goal>
 *                 </goals>
 *                 <configuration>
 *                   <refSetId>450993002</refSetId>
 *                   <outputDirName>/tmp</outputDirName>
 *                   <effectiveTime>20150131</effectiveTime>
 *                   <moduleId>900000000000207008</moduleId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile>
 * </pre>
 *
 * Sample execution of a pom.xml with this configuration:
 * 
 * <pre>
 * % mvn -PRelease -Drun.config=/home/ihtsdo/config/config.properties \
 *       -Drefset.id=450993002 -Doutput.dir=/tmp -Dtime=20150131 \
 *       -Dmodule.id=900000000000207008 install
 * </pre>
 * 
 * @goal release
 * @phase package
 */
public class ReleaseProcessingMojo extends AbstractMojo {

	/**
	 * The refSet id
	 * 
	 * @parameter refSetId
	 */
	private String refSetId = null;

	/**
	 * The refSet id
	 * 
	 * @parameter outputDirName
	 */
	private String outputDirName = null;

	/**
	 * The effective time of release
	 * 
	 * @parameter effectiveTime
	 */
	private String effectiveTime = null;

	/**
	 * The module id.
	 * 
	 * @parameter moduleId
	 */
	private String moduleId = null;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Processing release for ref set ids: " + refSetId);

		if (refSetId == null) {
			throw new MojoExecutionException("You must specify a refSetId.");
		}

		if (refSetId == null) {
			throw new MojoExecutionException(
					"You must specify an output file directory.");
		}

		File outputDir = new File(outputDirName);
		if (!outputDir.isDirectory())
			throw new MojoExecutionException("Output file directory ("
					+ outputDirName + ") could not be found.");

		if (effectiveTime == null)
			throw new MojoExecutionException("You must specify a release time");

		if (moduleId == null)
			throw new MojoExecutionException("You must specify a module id");

		try {

			MappingService mappingService = new MappingServiceJpa();
			Set<MapProject> mapProjects = new HashSet<>();

			for (MapProject mapProject : mappingService.getMapProjects()
					.getIterable()) {
				for (String id : refSetId.split(",")) {
					if (mapProject.getRefSetId().equals(id)) {
						mapProjects.add(mapProject);
					}
				}
			}

			DateFormat df = new SimpleDateFormat("yyyyMMdd");

			// Perform the release processing
			for (MapProject mapProject : mapProjects) {

				// add check for scope concepts contained in the map record set

				// FOR TESTING ONLY
				Set<MapRecord> mapRecords = new HashSet<>();

				boolean testRun = false;

				if (!testRun) {

					// RETRIEVE MAP RECORDS HERE
					mapRecords.addAll(mappingService
							.getPublishedAndReadyForPublicationMapRecordsForMapProject(
									mapProject.getId(), null).getMapRecords());
				}

				if (testRun) {

					/*
					 * String conceptIds[] = { "10000006", "10001005",
					 * "1001000119102", "10041001", "10050004",
					 * "100581000119102", "10061007", "10065003", "10070005" };
					 */

					/*String conceptIds[] = { "42434002" };

					for (String conceptId : conceptIds) {
						MapRecord mr = mappingService
								.getMapRecordForProjectAndConcept(
										mapProject.getId(), conceptId);
						mapRecords.add(mr);
					}*/
				}

				getLog().info(
						"Processing release for " + mapProject.getName() + ", "
								+ mapProject.getId() + ", with " + mapRecords.size() + " records to publish");

				// ensure output directory name has a terminating /
				if (!outputDirName.endsWith("/"))
					outputDirName += "/";
				
				//der2_iisssccRefset_ExtendedMapSnapshot_INT_20140731

				String machineReadableOutputFileName = outputDirName + "der2_"
						
						+ mapProject.getDestinationTerminology() +
						mapProject.getMapRefsetPattern() + "_INT_" +
						effectiveTime + ".txt";
				
				//tls_${project}HumanReadableMap_INT_${releaseVersion}.tsv
				
				String humanReadableOutputFileName = outputDirName + "tls_"
						+ mapProject.getDestinationTerminology() + "HumanReadableMap_INT_"
						+ mapProject.getSourceTerminologyVersion() + "_"
						+ effectiveTime + ".tsv";

				getLog().info("  Machine-readable release file:  " + machineReadableOutputFileName);
				getLog().info("  Human-readable release file:    " + humanReadableOutputFileName);
				
				ReleaseHandler releaseHandler = new ReleaseHandlerJpa();		
				releaseHandler.processRelease(mapProject, machineReadableOutputFileName, humanReadableOutputFileName, mapRecords, effectiveTime, moduleId );

			}

			getLog().info("done ...");
			mappingService.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Performing release processing failed.", e);
		}

	}

}
