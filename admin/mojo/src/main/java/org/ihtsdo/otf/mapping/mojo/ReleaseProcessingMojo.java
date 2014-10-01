package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.FileSorter;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
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
 *                   <refSetId>${refset.id}</refSetId>
 *                   <outputDirName>$(output.dir)</outputDirName>
 *                   <effectiveTime>${time}</effectiveTime>
 *                   <moduleId>${module.id}</moduleId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile>
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
				// Set<String> conceptIds = new HashSet<>();
				Set<MapRecord> mapRecords = new HashSet<>();

				// POPULATE CONCEPT SET HERE
				// conceptIds.add("276008");

				// RETRIEVE MAP RECORDS HERE
				// for (String terminologyId : conceptIds) {
				mapRecords.addAll(mappingService
						.getPublishedMapRecordsForMapProject(
								mapProject.getId(), null).getMapRecords());
				// mappingService.getMapRecordForProjectAndConcept(mapProject.getId(),
				// terminologyId));
				// }

				getLog().info(
						"Processing release for " + mapProject.getName() + ", "
								+ mapProject.getId());

				// ensure output directory name has a terminating /
				if (!outputDirName.endsWith("/"))
					outputDirName += "/";

				String releaseFileName = outputDirName + "release_"
						+ mapProject.getSourceTerminology() + "_"
						+ mapProject.getSourceTerminologyVersion() + "_"
						+ mapProject.getDestinationTerminology() + "_"
						+ mapProject.getDestinationTerminologyVersion() + "_"
						+ df.format(new Date()) + ".txt";

				getLog().info("  Release file:  " + releaseFileName);

				mappingService.processRelease(mapProject, releaseFileName,
						mapRecords, effectiveTime, moduleId);

				// sort the file into a temporary file
				FileSorter.sortFile(releaseFileName, releaseFileName + ".tmp",
						new Comparator<String>() {

							@Override
							public int compare(String o1, String o2) {
								String[] fields1 = o1.split("\t");
								String[] fields2 = o2.split("\t");
								long i = fields1[4].compareTo(fields2[4]);
								if (i != 0) {
									return (int) i;
								} else {
									i = (Long.parseLong(fields1[5]) - Long
											.parseLong(fields2[5]));
									if (i != 0) {
										return (int) i;
									} else {
										i = Long.parseLong(fields1[6])
												- Long.parseLong(fields2[6]);
										if (i != 0) {
											return (int) i;
										} else {
											i = Long.parseLong(fields1[7])
													- Long.parseLong(fields2[7]);
											if (i != 0) {
												return (int) i;
											} else {
												i = (fields1[0] + fields1[1]
														+ fields1[2] + fields1[3])
														.compareTo(fields1[0]
																+ fields1[1]
																+ fields1[2]
																+ fields1[3]);
												if (i != 0) {
													return (int) i;
												} else {
													i = fields1[8]
															.compareTo(fields2[8]);
													if (i != 0) {
														return (int) i;
													} else {
														i = fields1[9]
																.compareTo(fields2[9]);
														if (i != 0) {
															return (int) i;
														} else {
															i = fields1[10]
																	.compareTo(fields2[10]);
															if (i != 0) {
																return (int) i;
															} else {
																i = fields1[11]
																		.compareTo(fields2[11]);
																if (i != 0) {
																	return (int) i;
																} else {
																	i = fields1[12]
																			.compareTo(fields2[12]);
																	if (i != 0) {
																		return (int) i;
																	} else {
																		return 0;
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						});

				// delete the original file and move the tmp (sorted) file into the original file's place
				File file = new File(releaseFileName);
				file.delete();			
				file = new File(releaseFileName + ".tmp");
				file.renameTo(new File(releaseFileName));
				
				getLog().info("  Done sorting the file ");

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
