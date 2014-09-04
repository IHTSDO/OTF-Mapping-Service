package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.FileSorter;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads a sampling unpublished complex maps.
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
 *           <id>load-rf2-complex-map-sample</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-rf2-complex-map-sample</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-rf2-complex-map-sample
 * @phase package
 */
public class MapRecordRf2ComplexMapSampleLoaderMojo extends AbstractMojo {

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException
	 *             the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting loading sample of complex map data ...");

		try {

			String configFileName = System.getProperty("run.config");
			getLog().info("  run.config = " + configFileName);
			Properties config = new Properties();
			FileReader in = new FileReader(new File(configFileName));
			config.load(in);
			in.close();
			getLog().info("  properties = " + config);

			// set the input directory
			String inputFile = config
					.getProperty("loader.complexmap.rf2.sample.input.data");
			if (inputFile == null) {
				throw new MojoFailureException(
						"Input file directory loader.complexmap.rf2.sample.input.user must be specified");
			}
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader.complexmap.rf2.sample.input.data directory does not exist: "
								+ inputFile);
			}

			// set the input directory
			String loaderUserName = config
					.getProperty("loader.complexmap.rf2.sample.input.user");

			if (loaderUserName == null) {

				throw new MojoFailureException(
						"User name loader.complexmap.rf2.sample.input.user must be specified");
			}

			// try to parse the decimal percentage value, fail gracefully on
			// errors
			float samplingRate;
			try {

				// TODO: Add loader....owner parameter

				samplingRate = Float
						.parseFloat(config
								.getProperty("loader.complexmap.rf2.sample.percentage"));
			} catch (Exception e) {
				throw new MojoFailureException(
						"Sampling percentage loader.complexmap.rf2.sample.percentage could not be loaded from config file");
			}
			Logger.getLogger(this.getClass()).info("  inputFile: " + inputFile);
			Logger.getLogger(this.getClass()).info(
					"  sampingPercentage: " + samplingRate);

			// Bail if input file is not set
			if (inputFile == null) {
				throw new MojoExecutionException(
						"Failed to set sampling percentage from property file parameter");
			}

			// Bail if samping percentage is not set
			if (Float.isNaN(samplingRate)) {
				throw new MojoExecutionException(
						"Failed to set input file from property file parameter");
			}

			// Bail if sampling percentage not in range (0, 1]
			if (samplingRate <= 0 || samplingRate > 1) {
				throw new MojoExecutionException(
						"Sampling percentage must be greater than zero and less than or equal to one");
			}

			// sort input file
			getLog().info(
					"  Sorting the file into "
							+ System.getProperty("java.io.tmpdir"));
			File outputFile = File.createTempFile("ttt", ".sort", new File(
					System.getProperty("java.io.tmpdir")));
			outputFile.delete();
			// Sort file according to unix sort
			// -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11
			// -k 12,12 -k 13,13
			FileSorter.sortFile(inputFile, outputFile.getPath(),
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
			getLog().info("  Done sorting the file ");

			// Set up map of refSetIds that we may encounter
			MappingService mappingService = new MappingServiceJpa();
			Map<String, MapProject> mapProjectMap = new HashMap<>();
			for (MapProject project : mappingService.getMapProjects()
					.getIterable()) {
				mapProjectMap.put(project.getRefSetId(), project);
			}
			
			MapUser loaderUser = mappingService.getMapUser(loaderUserName);
			
			if (loaderUser == null) 
				throw new MojoFailureException("Could not retrieve user object for name " + loaderUserName);

			// load complexMapRefSetMembers from extendedMap file
			// TODO All non-sampled ones should be PUBLISHED
			Map<String, List<ComplexMapRefSetMember>> complexMapRefSetMemberMap = loadExtendedMapRefSets(
					outputFile, mapProjectMap);

			// Call mapping service to create records as we go along
			for (String refSetId : complexMapRefSetMemberMap.keySet()) {
				mappingService.createMapRecordsForMapProject(
						mapProjectMap.get(refSetId).getId(),
						complexMapRefSetMemberMap.get(refSetId),
						WorkflowStatus.REVIEW_NEEDED, samplingRate);

				for (MapRecord mr : mappingService.getMapRecordsForMapProject(
						mapProjectMap.get(refSetId).getId()).getIterable()) {

					// TODO Set the user here
					mr.setOwner(loaderUser);
				}
			}

			getLog().info("done ...");
			// clean-up
			mappingService.close();
			// outputFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Loading of Sampled Unpublished RF2 Complex Maps failed.",
					e);
		}

	}

	/**
	 * Load extended map ref sets from the file.
	 * 
	 * @return
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private Map<String, List<ComplexMapRefSetMember>> loadExtendedMapRefSets(
			File complexMapFile, Map<String, MapProject> mapProjectMap)
			throws Exception {

		// Open reader and service
		BufferedReader complexMapReader = new BufferedReader(new FileReader(
				complexMapFile));
		ContentService contentService = new ContentServiceJpa();

		// Set up sets for any map records we encounter
		String line = null;
		Map<String, List<ComplexMapRefSetMember>> complexMapRefSetMemberMap = new HashMap<>();
		for (MapProject mapProject : mapProjectMap.values()) {
			complexMapRefSetMemberMap.put(mapProject.getRefSetId(),
					new ArrayList<ComplexMapRefSetMember>());
		}

		Random random = new Random();

		int refSetMemberCt = 0;
		int refSetMemberSkippedCt = 0;

		final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");
		while ((line = complexMapReader.readLine()) != null) {
			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// ComplexMap attributes
				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1"));
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				final String refSetId = fields[4];
				complexMapRefSetMember.setRefSetId(refSetId);
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
				complexMapRefSetMember.setMapPriority(Integer
						.parseInt(fields[7]));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long
						.valueOf(fields[12]));

				// BLOCK is unused
				complexMapRefSetMember.setMapBlock(0); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(mapProjectMap.get(
						refSetId).getSourceTerminology());
				complexMapRefSetMember.setTerminologyVersion(mapProjectMap.get(
						refSetId).getSourceTerminologyVersion());

				// set Concept
				Concept concept = contentService.getConcept(
						fields[5], // referencedComponentId
						mapProjectMap.get(refSetId).getSourceTerminology(),
						mapProjectMap.get(refSetId)
								.getSourceTerminologyVersion());

				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					// don't persist, non-published shouldn't be in the db
					complexMapRefSetMemberMap.get(refSetId).add(
							complexMapRefSetMember);
				} else {
					complexMapReader.close();
					throw new IllegalStateException("complexMapRefSetMember "
							+ complexMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5]);
				}

				refSetMemberCt++;
			} else {
				refSetMemberSkippedCt++;
			}
		}

		complexMapReader.close();

		// Remove any map projects for which we did not encounter any records
		for (MapProject mapProject : mapProjectMap.values()) {
			if (complexMapRefSetMemberMap.get(mapProject.getRefSetId()).size() == 0) {
				complexMapRefSetMemberMap.remove(mapProject.getRefSetId());
			}
		}

		return complexMapRefSetMemberMap;
	}
}