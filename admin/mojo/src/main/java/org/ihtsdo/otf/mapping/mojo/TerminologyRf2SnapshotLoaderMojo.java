package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.FileSorter;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.AttributeValueRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.LanguageRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleRefSetMemberJpa;

import com.google.common.io.Files;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <dependencies>
 *         <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-loader-config</artifactId>
 *           <version>${project.version}</version>
 *          <scope>system</scope>
 *            <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>load-rf2-snapshot</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-rf2-snapshot</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *             <terminology>SNOMEDCT</terminology>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-rf2-snapshot
 * 
 * @phase package
 */
public class TerminologyRf2SnapshotLoaderMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;

	/**
	 * Name of terminology to be loaded.
	 * @parameter
	 * @required
	 */
	private String terminology;

	/** The date format. */
	private final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	// buffered readers for sorted files
	/** The extended_map_refsets_by_concept. */
	private BufferedReader conceptsByConcept, descriptionsByDescription,
			relationshipsBySourceConcept, languageRefsetsByDescription,
			attributeRefsetsByDescription, simpleRefsetsByConcept,
			simpleMapRefsetsByConcept, complexMapRefsetsByConcept,
			extendedMapRefsetsByConcept;

	/** The version. */
	private String version = null;

	/** the defaultPreferredNames values. */
	private Long dpnTypeId;

	/** The dpn ref set id. */
	private Long dpnRefSetId;

	/** The dpn acceptability id. */
	private Long dpnAcceptabilityId;

	/** The manager. */
	private EntityManager manager;

	/** hash sets for retrieving concepts. */
	private Map<String, Concept> conceptCache = new HashMap<>(); // used to

	/** hash set for storing default preferred names. */
	Map<Long, String> defaultPreferredNames = new HashMap<Long, String>();

	/** counter for objects created, reset in each load section */
	int objectCt; //

	/** the number of objects to create before committing. */
	int commitCt = 200;

	/** The memory max. */
	long memoryMax = 0L;

	/** The memory free. */
	long memoryFree = 0L;

	/** The memory total. */
	long memoryTotal = 0L;

	/**
	 * Instantiates a {@link TerminologyRf2SnapshotLoaderMojo} from the specified
	 * parameters.
	 * 
	 */
	public TerminologyRf2SnapshotLoaderMojo() {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		FileInputStream propertiesInputStream = null;
		try {

			// Track system level information
			long startTimeOrig = System.nanoTime();

			getLog().info("Start loading RF2 data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);

			// set the input directory
			String coreInputDirString =
					properties.getProperty("loader." + terminology + ".input.data");
			File coreInputDir = new File(coreInputDirString);
			if (!coreInputDir.exists()) {
				throw new MojoFailureException("Specified loader." + terminology
						+ ".input.data directory does not exist: " + coreInputDirString);
			}

			// set the parameters for determining defaultPreferredNames
			dpnTypeId =
					Long.valueOf(properties
							.getProperty("loader.defaultPreferredNames.typeId"));
			dpnRefSetId =
					Long.valueOf(properties
							.getProperty("loader.defaultPreferredNames.refSetId"));
			dpnAcceptabilityId =
					Long.valueOf(properties
							.getProperty("loader.defaultPreferredNames.acceptabilityId"));

			// output relevant properties/settings to console
			getLog().info("Default preferred name settings:");
			getLog().info(" typeId:          " + dpnTypeId);
			getLog().info(" refSetId:        " + dpnRefSetId);
			getLog().info(" acceptabilityId: " + dpnAcceptabilityId);
			getLog().info(
					"Commit settings: Objects committed in blocks of "
							+ Integer.toString(commitCt));

			// close the Properties file
			propertiesInputStream.close();

			// create Entitymanager
			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			Runtime runtime = Runtime.getRuntime();
			getLog().info(String.valueOf(runtime.totalMemory()) + " total memory");
			getLog().info(String.valueOf(runtime.freeMemory()) + " free memory");
			getLog().info(String.valueOf(runtime.maxMemory()) + " max memory");

			SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a"); // format for
			// logging

			// Prepare sorted input files
			File sortedFileDir = new File(coreInputDir, "/RF2-sorted-temp/");

			getLog().info("Preparing input files...");
			long startTime = System.nanoTime();
			prepareSortedFiles(coreInputDir, sortedFileDir);
			getLog()
					.info(
							"    File preparation complete in " + getElapsedTime(startTime)
									+ "s");

			EntityTransaction tx = manager.getTransaction();
			try {

				// load Concepts
				if (conceptsByConcept != null) {

					getLog().info("    Loading Concepts...");

					startTime = System.nanoTime();
					loadConcepts();
					getLog().info(
							"      " + Integer.toString(objectCt) + " Concepts loaded in "
									+ getElapsedTime(startTime) + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load Descriptions and Language Ref Set Members
				if (descriptionsByDescription != null
						&& languageRefsetsByDescription != null) {
					getLog().info("    Loading Descriptions and LanguageRefSets...");
					startTime = System.nanoTime();
					//
					loadDescriptionsAndLanguageRefSets();
					getLog().info(
							"      "
									+ " Descriptions and language ref set members loaded in "
									+ getElapsedTime(startTime) + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");

					// set default preferred names
					getLog().info(" Setting default preferred names for all concepts...");
					startTime = System.nanoTime();
					tx.begin();
					Iterator<Concept> conceptIterator = conceptCache.values().iterator();
					objectCt = 0;
					int ct = 0;
					int skippedCt = 0;
					while (conceptIterator.hasNext()) {
						Concept cachedConcept = conceptIterator.next();

						Concept dbConcept = manager.find(ConceptJpa.class, cachedConcept.getId());
						dbConcept.getDescriptions();
						dbConcept.getRelationships();
						if (defaultPreferredNames.get(dbConcept.getId()) != null) {
							dbConcept.setDefaultPreferredName(defaultPreferredNames.get(dbConcept
									.getId()));
						} else {
							dbConcept.setDefaultPreferredName("No default preferred name found");
							skippedCt++;
						}

						manager.merge(dbConcept);

						if (++ct % 50000 == 0) {
							getLog().info(Integer.toString(ct));
						}

						if (++objectCt % commitCt == 0) {
							tx.commit();
							manager.clear();
							tx.begin();
						}
					}

					tx.commit();
					manager.clear();
					getLog().info(
							"      " + "Names set in " + getElapsedTime(startTime).toString()
									+ "s");
					getLog().info(
							"      " + Integer.toString(skippedCt)
									+ " concepts had no default preferred name");
					defaultPreferredNames.clear();

				}

				// load Relationships
				if (relationshipsBySourceConcept != null) {
					getLog().info("    Loading Relationships...");
					startTime = System.nanoTime();

					loadRelationships();

					getLog().info(
							"      " + Integer.toString(objectCt) + " Concepts loaded in "
									+ getElapsedTime(startTime) + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load Simple RefSets (Content)
				if (simpleRefsetsByConcept != null) {
					getLog().info("    Loading Simple RefSets...");
					startTime = System.nanoTime();

					loadSimpleRefSets();

					getLog().info(
							"      " + Integer.toString(objectCt)
									+ " Simple Refsets loaded in " + getElapsedTime(startTime)
									+ "s" + " (Ended at " + ft.format(new Date()) + ")");
				}

				// load SimpleMapRefSets
				if (simpleMapRefsetsByConcept != null) {
					getLog().info("    Loading SimpleMap RefSets...");
					startTime = System.nanoTime();

					loadSimpleMapRefSets();
					getLog().info(
							"      " + Integer.toString(objectCt)
									+ " SimpleMap RefSets loaded in " + getElapsedTime(startTime)
									+ "s" + " (Ended at " + ft.format(new Date()) + ")");
				}

				// load ComplexMapRefSets
				if (complexMapRefsetsByConcept != null) {
					getLog().info("    Loading ComplexMap RefSets...");
					startTime = System.nanoTime();

					loadComplexMapRefSets();
					getLog().info(
							"      " + Integer.toString(objectCt)
									+ " ComplexMap RefSets loaded in "
									+ getElapsedTime(startTime) + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load ExtendedMapRefSets
				if (extendedMapRefsetsByConcept != null) {
					getLog().info("    Loading ExtendedMap RefSets...");
					startTime = System.nanoTime();

					loadExtendedMapRefSets();

					getLog().info(
							"      " + Integer.toString(objectCt)
									+ " ExtendedMap RefSets loaded in "
									+ getElapsedTime(startTime) + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load AttributeValue RefSets (Content)
				if (attributeRefsetsByDescription != null) {
					getLog().info("    Loading AttributeValue RefSets...");
					startTime = System.nanoTime();

					loadAttributeValueRefSets();
					getLog().info(
							"      " + Integer.toString(objectCt)
									+ " AttributeValue RefSets loaded in "
									+ getElapsedTime(startTime).toString() + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				getLog().info(
						"    Total elapsed time for run: "
								+ getTotalElapsedTimeStr(startTimeOrig));
				getLog().info("done ...");

			} catch (Exception e) {
				e.printStackTrace();

				throw e;
			}

			// Clean-up
			manager.close();
			factory.close();
			closeAllSortedFiles();

		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		} finally {
			try {
				propertiesInputStream.close();
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	// Used for debugging/efficiency monitoring
	/**
	 * Returns the elapsed time.
	 * 
	 * @return the elapsed time
	 */
	@SuppressWarnings("boxing")
	private static Long getElapsedTime(long time) {
		return (System.nanoTime() - time) / 1000000000;
	}

	/**
	 * Returns the total elapsed time str.
	 * 
	 * @return the total elapsed time str
	 */
	@SuppressWarnings("boxing")
	private static String getTotalElapsedTimeStr(long time) {

		Long resultnum = (System.nanoTime() - time) / 1000000000;
		String result = resultnum.toString() + "s";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "m";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "h";
		return result;

	}

	/**
	 * Returns the last modified.
	 * 
	 * @param directory the directory
	 * @return the last modified
	 */
	private long getLastModified(File directory) {
		File[] files = directory.listFiles();
		long lastModified = 0;

		for (int j = 0; j < files.length; j++) {
			if (files[j].isDirectory()) {
				long tempLastModified = getLastModified(files[j]);
				if (lastModified < tempLastModified) {
					lastModified = tempLastModified;
				}
			} else if (lastModified < files[j].lastModified()) {
				lastModified = files[j].lastModified();
			}
		}

		return lastModified;
	}

	/**
	 * File management for sorted files.
	 * 
	 * @throws Exception the exception
	 */
	// *************************************************************************************
	// //
	//
	// *************************************************************************************
	// //
	private void prepareSortedFiles(File coreInputDir, File outputDir)
		throws Exception {

		// *********************** //
		// Sort files if required //
		// *********************** //

		// if sorted files do not exist OR sorted files are older than last modified
		// input file, sort
		if (!outputDir.exists()
				|| getLastModified(outputDir) < getLastModified(coreInputDir)) {

			// log reason for sort
			if (!outputDir.exists()) {
				getLog().info("     No sorted files exist -- sorting RF2 files");
			} else if (getLastModified(outputDir) < getLastModified(coreInputDir)) {
				getLog().info(
						"     Sorted files older than input files -- sorting RF2 files");
			}

			// delete any existing temporary files
			FileSorter.deleteSortedFiles(outputDir);

			// test whether file/folder still exists (i.e. delete error)
			if (outputDir.exists()) {
				throw new MojoFailureException(
						"Could not delete existing sorted files folder "
								+ outputDir.toString());
			}

			// attempt to make sorted files directory
			if (outputDir.mkdir()) {
				getLog().info(
						" Creating new sorted files folder " + outputDir.toString());
			} else {
				throw new MojoFailureException(
						" Could not create temporary sorted file folder "
								+ outputDir.toString());
			}

			// sort files
			sortRf2Files(coreInputDir, outputDir);
		} else {
			getLog().info(
					"    Sorted files exist and are up to date.  No sorting required");
		}

	}

	/**
	 * Sorts all files by concept or referencedComponentId.
	 * 
	 * @throws Exception the exception
	 */
	private void sortRf2Files(File coreInputDir, File outputDir) throws Exception {

		//
		// Set files
		//
		File coreRelInputFile = null;
		File coreStatedRelInputFile = null;
		File coreConceptInputFile = null;
		File coreDescriptionInputFile = null;
		File coreSimpleRefsetInputFile = null;
		File coreAssociationReferenceInputFile = null;
		File coreAttributeValueInputFile = null;
		File coreComplexMapInputFile = null;
		File coreExtendedMapInputFile = null;
		File coreSimpleMapInputFile = null;
		File coreLanguageInputFile = null;
		File coreIdentifierInputFile = null;
		File coreTextDefinitionInputFile = null;

		// CORE
		File coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
		getLog().info(
				"  Core Input Dir = " + coreTerminologyInputDir.toString() + " "
						+ coreTerminologyInputDir.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Relationship_")) {
				if (coreRelInputFile != null)
					throw new MojoFailureException("Multiple Relationships Files!");
				coreRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Rel Input File = " + coreRelInputFile.toString() + " "
						+ coreRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_StatedRelationship_")) {
				if (coreStatedRelInputFile != null)
					throw new MojoFailureException("Multiple Stated Relationships Files!");
				coreStatedRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Stated Rel Input File = " + coreStatedRelInputFile.toString()
						+ " " + coreStatedRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Concept_")) {
				if (coreConceptInputFile != null)
					throw new MojoFailureException("Multiple Concept Files!");
				coreConceptInputFile = f;
			}
		}
		getLog().info(
				"  Core Concept Input File = " + coreConceptInputFile.toString() + " "
						+ coreConceptInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Description_")) {
				if (coreDescriptionInputFile != null)
					throw new MojoFailureException("Multiple Description Files!");
				coreDescriptionInputFile = f;
			}
		}
		getLog().info(
				"  Core Description Input File = "
						+ coreDescriptionInputFile.toString() + " "
						+ coreDescriptionInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Identifier_")) {
				if (coreIdentifierInputFile != null)
					throw new MojoFailureException("Multiple Identifier Files!");
				coreIdentifierInputFile = f;
			}
		}
		getLog().info(
				"  Core Identifier Input File = " + coreIdentifierInputFile.toString()
						+ " " + coreIdentifierInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_TextDefinition_")) {
				if (coreTextDefinitionInputFile != null)
					throw new MojoFailureException("Multiple TextDefinition Files!");
				coreTextDefinitionInputFile = f;
			}
		}
		if (coreTextDefinitionInputFile != null) {
			getLog().info(
					"  Core Text Definition Input File = "
							+ coreTextDefinitionInputFile.toString() + " "
							+ coreTextDefinitionInputFile.exists());
		}

		File coreRefsetInputDir = new File(coreInputDir, "/Refset/");
		File coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
		getLog().info(
				"  Core Input Dir = " + coreContentInputDir.toString() + " "
						+ coreContentInputDir.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Simple")) {
				if (coreSimpleRefsetInputFile != null)
					throw new MojoFailureException("Multiple Simple Refset Files!");
				coreSimpleRefsetInputFile = f;
			}
		}
		getLog().info(
				"  Core Simple Refset Input File = "
						+ coreSimpleRefsetInputFile.toString() + " "
						+ coreSimpleRefsetInputFile.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("AssociationReference")) {
				if (coreAssociationReferenceInputFile != null)
					throw new MojoFailureException(
							"Multiple Association Reference Files!");
				coreAssociationReferenceInputFile = f;
			}
		}
		getLog().info(
				"  Core Association Reference Input File = "
						+ coreAssociationReferenceInputFile.toString() + " "
						+ coreAssociationReferenceInputFile.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("AttributeValue")) {
				if (coreAttributeValueInputFile != null)
					throw new MojoFailureException("Multiple Attribute Value Files!");
				coreAttributeValueInputFile = f;
			}
		}
		getLog().info(
				"  Core Attribute Value Input File = "
						+ coreAttributeValueInputFile.toString() + " "
						+ coreAttributeValueInputFile.exists());

		File coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
		getLog().info(
				"  Core Crossmap Input Dir = " + coreCrossmapInputDir.toString() + " "
						+ coreCrossmapInputDir.exists());

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("ComplexMap")) {
				if (coreComplexMapInputFile != null)
					throw new MojoFailureException("Multiple Complex Map Files!");
				coreComplexMapInputFile = f;
			}
		}
		if (coreComplexMapInputFile != null) {
			getLog().info(
					"  Core Complex Map Input File = "
							+ coreComplexMapInputFile.toString() + " "
							+ coreComplexMapInputFile.exists());
		}

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("ExtendedMap")) {
				if (coreExtendedMapInputFile != null)
					throw new MojoFailureException("Multiple Extended Map Files!");
				coreExtendedMapInputFile = f;
			}
		}
		if (coreComplexMapInputFile != null) {
			getLog().info(
					"  Core Complex Map Input File = "
							+ coreComplexMapInputFile.toString() + " "
							+ coreComplexMapInputFile.exists());
		}

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("SimpleMap")) {
				if (coreSimpleMapInputFile != null)
					throw new MojoFailureException("Multiple Simple Map Files!");
				coreSimpleMapInputFile = f;
			}
		}
		getLog().info(
				"  Core Simple Map Input File = " + coreSimpleMapInputFile.toString()
						+ " " + coreSimpleMapInputFile.exists());

		File coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
		getLog().info(
				"  Core Language Input Dir = " + coreLanguageInputDir.toString() + " "
						+ coreLanguageInputDir.exists());

		for (File f : coreLanguageInputDir.listFiles()) {
			if (f.getName().contains("Language")) {
				if (coreLanguageInputFile != null)
					throw new MojoFailureException("Multiple Language Files!");
				coreLanguageInputFile = f;
			}
		}
		getLog().info(
				"  Core Language Input File = " + coreLanguageInputFile.toString()
						+ " " + coreLanguageInputFile.exists());

		File coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
		getLog().info(
				"  Core Metadata Input Dir = " + coreMetadataInputDir.toString() + " "
						+ coreMetadataInputDir.exists());

		//
		// Initialize files
		//

		File concepts_by_concept_file =
				new File(outputDir, "concepts_by_concept.sort");
		File descriptions_by_description_file =
				new File(outputDir, "descriptions_by_description.sort");
		File descriptions_core_by_description_file =
				new File(outputDir, "descriptions_core_by_description.sort");
		File descriptions_text_by_description_file =
				new File(outputDir, "descriptions_text_by_description.sort");
		File relationships_by_source_concept_file =
				new File(outputDir, "relationship_by_source_concept.sort");
		File relationships_by_dest_concept_file =
				new File(outputDir, "relationship_by_dest_concept.sort");
		File language_refsets_by_description_file =
				new File(outputDir, "language_refsets_by_description.sort");
		File attribute_refsets_by_concept_file =
				new File(outputDir, "attribute_refsets_by_concept.sort");
		File simple_refsets_by_concept_file =
				new File(outputDir, "simple_refsets_by_concept.sort");
		File simple_map_refsets_by_concept_file =
				new File(outputDir, "simple_map_refsets_by_concept.sort");
		File complex_map_refsets_by_concept_file =
				new File(outputDir, "complex_map_refsets_by_concept.sort");
		File extended_map_refsets_by_concept_file =
				new File(outputDir, "extended_map_refsets_by_concept.sort");

		// Concepts
		conceptsByConcept =
				new BufferedReader(new FileReader(concepts_by_concept_file));

		// Relationships by source concept
		relationshipsBySourceConcept =
				new BufferedReader(new FileReader(relationships_by_source_concept_file));

		// Descriptions by description id
		descriptionsByDescription =
				new BufferedReader(new FileReader(descriptions_by_description_file));

		// Language RefSets by description id
		languageRefsetsByDescription =
				new BufferedReader(new FileReader(language_refsets_by_description_file));

		// ******************************************************* //
		// Component RefSet Members //
		// ******************************************************* //

		// Attribute Value
		attributeRefsetsByDescription =
				new BufferedReader(new FileReader(attribute_refsets_by_concept_file));

		// Simple
		simpleRefsetsByConcept =
				new BufferedReader(new FileReader(simple_refsets_by_concept_file));

		// Simple Map
		simpleMapRefsetsByConcept =
				new BufferedReader(new FileReader(simple_map_refsets_by_concept_file));

		// Complex map
		complexMapRefsetsByConcept =
				new BufferedReader(new FileReader(complex_map_refsets_by_concept_file));

		// Extended map
		extendedMapRefsetsByConcept =
				new BufferedReader(new FileReader(extended_map_refsets_by_concept_file));

		// ******************************************************* //
		// Log file
		// ******************************************************* //

		// ****************//
		// Components //
		// ****************//

		sortRf2File(coreConceptInputFile, concepts_by_concept_file, 0);

		// core descriptions by description
		sortRf2File(coreDescriptionInputFile,
				descriptions_core_by_description_file, 0);

		// if text descriptions file exists, sort and merge
		if (coreTextDefinitionInputFile != null) {

			// sort the text definition file
			sortRf2File(coreTextDefinitionInputFile,
					descriptions_text_by_description_file, 0);

			// merge the two description files
			getLog().info("        Merging description files...");
			File merged_descriptions =
					mergeSortedFiles(descriptions_text_by_description_file,
							descriptions_core_by_description_file, new Comparator<String>() {
								@Override
								public int compare(String s1, String s2) {
									String v1[] = s1.split("\t");
									String v2[] = s2.split("\t");
									return v1[0].compareTo(v2[0]);
								}
							}, outputDir, ""); // header line

			// rename the temporary file
			Files.move(merged_descriptions, descriptions_by_description_file);

		} else {
			// copy the core descriptions file
			Files.copy(descriptions_core_by_description_file,
					descriptions_by_description_file);
		}

		sortRf2File(coreRelInputFile, relationships_by_source_concept_file, 4);
		sortRf2File(coreRelInputFile, relationships_by_dest_concept_file, 5);

		// ****************//
		// RefSets //
		// ****************//
		sortRf2File(coreAttributeValueInputFile, attribute_refsets_by_concept_file,
				5);
		sortRf2File(coreSimpleRefsetInputFile, simple_refsets_by_concept_file, 5);
		sortRf2File(coreSimpleMapInputFile, simple_map_refsets_by_concept_file, 5);
		sortRf2File(coreComplexMapInputFile, complex_map_refsets_by_concept_file, 5);
		sortRf2File(coreExtendedMapInputFile, extended_map_refsets_by_concept_file,
				5);
		sortRf2File(coreLanguageInputFile, language_refsets_by_description_file, 5);

		//
		// Determine version
		//
		int index = coreConceptInputFile.getName().indexOf(".txt");
		version = coreConceptInputFile.getName().substring(index - 8, index);
		getLog().info("Version " + version);

	}

	/**
	 * Helper function for sorting an individual file with colum comparator.
	 * 
	 * @param file_in the input file to be sorted
	 * @param file_out the resulting sorted file
	 * @param sort_column the column ([0, 1, ...] to compare by
	 * @throws Exception the exception
	 */
	private void sortRf2File(File file_in, File file_out, final int sort_column)
		throws Exception {

		Comparator<String> comp;

		comp = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t");
				return v1[sort_column].compareTo(v2[sort_column]);
			}
		};

		getLog().info(
				" Sorting " + file_in.toString() + "  into " + file_out.toString()
						+ " by column " + Integer.toString(sort_column));
		FileSorter.sortFile(file_in.toString(), file_out.toString(), comp);

	}

	// /////////////////////////////
	// / OLDER SORT FUNCTIONS
	// /////////////////////////////

	/**
	 * Sort the specified file using the specified {@link Comparator} and
	 * optionally sort uniquely.
	 * 
	 * @param filename the file to sort
	 * @param fileout the destination sorted file
	 * @param comp the {@link Comparator}
	 * @throws Exception the exception
	 */

	public void sort(String filename, String fileout, Comparator<String> comp)
		throws Exception {

		//
		// Vars
		//
		List<String> lines = null;
		List<File> files1 = new ArrayList<File>();
		List<File> files2 = new ArrayList<File>();
		String line;
		final File orig_file = new File(filename).getAbsoluteFile();
		final File dest_file = new File(fileout).getAbsoluteFile();
		final File sortdir = new File(dest_file.getParent());

		//
		// Open file
		//
		final BufferedReader in = new BufferedReader(new FileReader(orig_file));
		//

		// Break input file into files with max size of 16MB and then sort it
		//

		int size_so_far = 0;
		final int segment_size = 32 * 1024 * 1024;

		// get and save header
		String header_line = in.readLine(); //

		while ((line = in.readLine()) != null) {

			if (size_so_far == 0) {

				lines = new ArrayList<String>(10000);

			}

			lines.add(line);
			size_so_far += line.length();

			if (size_so_far > segment_size) {

				sortHelper(lines.toArray(new String[0]), files1, comp, sortdir);

				size_so_far = 0;
			}
		}

		//
		// If there are left-over lines, create final tmp file
		//
		if (lines != null && lines.size() != 0 && size_so_far <= segment_size) {
			sortHelper(lines.toArray(new String[0]), files1, comp, sortdir);
		}

		//
		// Calculations for pm
		//
		int total_files = files1.size();
		int tmp = total_files;

		while (tmp > 1) {

			tmp = (int) Math.ceil(tmp / 2.0);
			total_files += tmp;

		}
		//
		// Merge sorted files
		//
		tmp = 0;

		// if one file, do not sort
		while (files1.size() > 1) {

			// cycle over files two at a time
			for (int i = 0; i < files1.size(); i += 2) {

				if (files1.size() == i + 1) {
					files2.add(files1.get(i));
					break;
				} else {

					final File f =
							mergeSortedFiles(files1.get(i), files1.get(i + 1), comp, sortdir,
									header_line);

					files2.add(f);

					// files1.get(i).delete();
					// files1.get(i + 1).delete();

					// TODO add test to check line-by-line comparator
					if (!FileSorter.checkSortedFile(f, comp)) {
						getLog().info("FAILED SORT CHECK: " + f.getName());
					} else
						getLog().info("Passed sort check: " + f.getName());
				}
			}

			files1 = new ArrayList<File>(files2);
			files2.clear();
		}

		// rename file

		if (files1.size() > 0) {
			files1.get(0).renameTo(dest_file);
		}

		// if no files, create an empty file
		else {
			dest_file.createNewFile();
		}

		// close input file
		in.close();

	}

	/**
	 * Helper function to perform sort operations.
	 * 
	 * @param lines the lines to sort
	 * @param all_tmp_files the list of files
	 * @param comp the comparator
	 * @param sortdir the sort dir
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sortHelper(String[] lines, List<File> all_tmp_files,
		Comparator<String> comp, File sortdir) throws IOException {

		//
		// Create temp file
		//
		final File f = File.createTempFile("t+~", ".tmp", sortdir);

		//
		// Sort data for this segment
		//
		Arrays.sort(lines, comp);

		//
		// Write lines to file f
		//
		final BufferedWriter out = new BufferedWriter(new FileWriter(f));

		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i];
			out.write(line);
			out.newLine();
			// out.flush();

		}

		out.flush();
		out.close();
		all_tmp_files.add(f);
	}

	/**
	 * Merge-sort two files.
	 * 
	 * @param files1 the first set of files
	 * @param files2 the second set of files
	 * @param comp the comparator
	 * @param dir the sort dir
	 * @param header_line the header_line
	 * @return the sorted {@link File}
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private File mergeSortedFiles(File files1, File files2,
		Comparator<String> comp, File dir, String header_line) throws IOException {
		final BufferedReader in1 = new BufferedReader(new FileReader(files1));

		final BufferedReader in2 = new BufferedReader(new FileReader(files2));

		final File out_file = File.createTempFile("t+~", ".tmp", dir);

		final BufferedWriter out = new BufferedWriter(new FileWriter(out_file));

		getLog().info(
				"Merging files: " + files1.getName() + " - " + files2.getName()
						+ " into " + out_file.getName());

		String line1 = in1.readLine();
		String line2 = in2.readLine();
		String line = null;

		if (!header_line.isEmpty()) {
			line = header_line;
			out.write(line);
			out.newLine();
		}

		while (line1 != null || line2 != null) {

			// System.out.println("Comparing: ");
			// System.out.println("     " + line1);
			// System.out.println("     " + line2);

			if (line1 == null) {
				line = line2;
				line2 = in2.readLine();

			} else if (line2 == null) {

				line = line1;
				line1 = in1.readLine();

			} else if (comp.compare(line1, line2) < 0) {

				line = line1;
				line1 = in1.readLine();

			} else {

				line = line2;
				line2 = in2.readLine();

			}

			// if a header line, do not write
			if (!line.startsWith("id")) {

				out.write(line);
				out.newLine();
			}
		}

		out.flush();
		out.close();
		in1.close();
		in2.close();

		return out_file;

	}

	/**
	 * Returns the concept.
	 * 
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the concept
	 * @throws Exception the exception
	 */
	private Concept getConcept(String terminologyId, String terminology,
		String terminologyVersion) throws Exception {

		if (conceptCache.containsKey(terminologyId + terminology
				+ terminologyVersion)) {

			// uses hibernate first-level cache
			return conceptCache.get(terminologyId + terminology + terminologyVersion);
		}

		Query query =
				manager
						.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		// Try to retrieve the single expected result
		// If zero or more than one result are returned, log error and set
		// result to null

		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			conceptCache.put(terminologyId + terminology + terminologyVersion, c);

			return c;

		} catch (NoResultException e) {
			// Log and return null if there are no releases
			getLog().info(
					"Concept query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology + ", terminologyVersion = "
							+ terminologyVersion + " returned no results!");
			return null;
		}

	}

	/**
	 * Closes all sorted temporary files.
	 * 
	 * @throws Exception if something goes wrong
	 */
	private void closeAllSortedFiles() throws Exception {
		if (conceptsByConcept != null) {
			conceptsByConcept.close();
		}
		if (descriptionsByDescription != null) {
			descriptionsByDescription.close();
		}
		if (relationshipsBySourceConcept != null) {
			relationshipsBySourceConcept.close();
		}
		if (languageRefsetsByDescription != null) {
			languageRefsetsByDescription.close();
		}
		if (attributeRefsetsByDescription != null) {
			attributeRefsetsByDescription.close();
		}
		if (simpleRefsetsByConcept != null) {
			simpleRefsetsByConcept.close();
		}
		if (simpleMapRefsetsByConcept != null) {
			simpleMapRefsetsByConcept.close();
		}
		if (complexMapRefsetsByConcept != null) {
			complexMapRefsetsByConcept.close();
		}
		if (extendedMapRefsetsByConcept != null) {
			extendedMapRefsetsByConcept.close();
		}
	}

	/**
	 * Load concepts.
	 * 
	 * @throws Exception the exception
	 */
	private void loadConcepts() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = conceptsByConcept.readLine()) != null) {

			String fields[] = line.split("\t");
			Concept concept = new ConceptJpa();

			if (!fields[0].equals("id")) { // header
				concept.setTerminologyId(fields[0]);
				concept.setEffectiveTime(dt.parse(fields[1]));
				concept.setActive(fields[2].equals("1") ? true : false);
				concept.setModuleId(Long.valueOf(fields[3]));
				concept.setDefinitionStatusId(Long.valueOf(fields[4]));
				concept.setTerminology(terminology);
				concept.setTerminologyVersion(version);
				concept.setDefaultPreferredName("null");

				getLog().debug(
						"  Add concept " + concept.getTerminologyId() + " "
								+ concept.getDefaultPreferredName());
				manager.persist(concept);

				conceptCache.put(new String(fields[0] + concept.getTerminology()
						+ concept.getTerminologyVersion()), concept);

				// memory debugging
				if (objectCt % commitCt == 0) {
					Runtime runtime = Runtime.getRuntime();
					if (memoryTotal < runtime.totalMemory()) {
						memoryTotal = runtime.totalMemory();
					}
					if (memoryFree < runtime.freeMemory()) {
						memoryTotal = runtime.freeMemory();
					}
					if (memoryMax < runtime.maxMemory()) {
						memoryTotal = runtime.maxMemory();
					}
				}

				// regularly commit at intervals
				if (++objectCt % commitCt == 0) {

					tx.commit();
					manager.clear();
					tx.begin();
				}
			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();

		// print memory information
		getLog().info("MEMORY USAGE:");
		getLog().info(" Total: " + memoryTotal);
		getLog().info(" Free:  " + memoryFree);
		getLog().info(" Max:   " + memoryMax);

	}

	/**
	 * Load relationships.
	 * 
	 * @throws Exception the exception
	 */
	private void loadRelationships() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = relationshipsBySourceConcept.readLine()) != null) {

			String fields[] = line.split("\t");
			Relationship relationship = new RelationshipJpa();

			if (!fields[0].equals("id")) { // header
				relationship.setTerminologyId(fields[0]);
				relationship.setEffectiveTime(dt.parse(fields[1]));
				relationship.setActive(fields[2].equals("1") ? true : false); // active
				relationship.setModuleId(Long.valueOf(fields[3])); // moduleId

				relationship.setRelationshipGroup(Integer.valueOf(fields[6])); // relationshipGroup
				relationship.setTypeId(Long.valueOf(fields[7])); // typeId
				relationship.setCharacteristicTypeId(Long.valueOf(fields[8])); // characteristicTypeId
				relationship.setTerminology(terminology);
				relationship.setTerminologyVersion(version);
				relationship.setModifierId(Long.valueOf(fields[9]));

				Concept sourceConcept =
						getConcept(fields[4], relationship.getTerminology(),
								relationship.getTerminologyVersion());
				Concept destinationConcept =
						getConcept(fields[5], relationship.getTerminology(),
								relationship.getTerminologyVersion());

				if (sourceConcept != null && destinationConcept != null) {
					relationship.setSourceConcept(sourceConcept);
					relationship.setDestinationConcept(destinationConcept);

					manager.persist(relationship);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					if (sourceConcept == null) {
						getLog().info(
								"Relationship " + relationship.getTerminologyId()
										+ " references non-existent source concept " + fields[4]);
					}
					if (destinationConcept == null) {
						getLog().info(
								"Relationship " + relationship.getTerminologyId()
										+ " references non-existent destination concept "
										+ fields[5]);
					}

				}
			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();
	}

	/**
	 * Load descriptions.
	 * 
	 * @throws Exception the exception
	 */
	private void loadDescriptionsAndLanguageRefSets() throws Exception {

		Concept concept;
		Description description;
		LanguageRefSetMember language;
		int n_desc = 0; // counter for descriptions
		int n_lang = 0; // counter for language ref set members
		int n_skip = 0; // counter for number of language ref set members skipped

		// load and persist first description
		description = getNextDescription();

		// load first language ref set member
		language = getNextLanguage();

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		// cycle over descriptions
		while (!description.getTerminologyId().equals("-1")) { // getNextDescription
																														// sets this to -1
																														// if null line

			// if current language ref set references a lexicographically "lower"
			// String terminologyId, SKIP: description is not in data set
			while (language.getDescription().getTerminologyId()
					.compareTo(description.getTerminologyId()) < 0
					&& !language.getTerminologyId().equals("-1")) {

				getLog().info(
						"     " + "Language Ref Set " + language.getTerminologyId()
								+ " references non-existent description "
								+ language.getDescription().getTerminologyId());
				language = getNextLanguage();
				n_skip++;
			}

			// cycle over language ref sets until new description id found or end of
			// language ref sets found
			while (language.getDescription().getTerminologyId()
					.equals(description.getTerminologyId())
					&& !language.getTerminologyId().equals("-1")) {

				// set the description
				language.setDescription(description);
				description.addLanguageRefSetMember(language);
				n_lang++;

				// System.out.println("      " + Integer.toString(n_lang) + " - " +
				// language.getDescription().getTerminologyId() + " - " +
				// description.getTerminologyId() + " - " +
				// language.getTerminologyId());

				// check if this language refset and description form the
				// defaultPreferredName
				if (description.isActive() && description.getTypeId().equals(dpnTypeId)
						&& new Long(language.getRefSetId()).equals(dpnRefSetId)
						&& language.getAcceptabilityId().equals(dpnAcceptabilityId)) {

					// retrieve the concept for this description
					concept = description.getConcept();
					if (defaultPreferredNames.get(concept.getId()) != null) {
						getLog().info(
								"Multiple default preferred names for concept "
										+ concept.getTerminologyId());
						getLog().info(
								"  " + "Existing: "
										+ defaultPreferredNames.get(concept.getId()));
						getLog().info("  " + "Replaced: " + description.getTerm());
					}
					defaultPreferredNames.put(concept.getId(), description.getTerm());

				}

				// / get the next language ref set member
				language = getNextLanguage();
			}

			// persist the description
			manager.persist(description);

			// get the next description
			description = getNextDescription();

			// increment description count
			n_desc++;

			if (n_desc % 100000 == 0) {
				getLog().info("-> descriptions: " + Integer.toString(n_desc));
			}

			// memory debugging
			if (n_desc % commitCt == 0) {
				Runtime runtime = Runtime.getRuntime();
				if (memoryTotal < runtime.totalMemory()) {
					memoryTotal = runtime.totalMemory();
				}
				if (memoryFree < runtime.freeMemory()) {
					memoryTotal = runtime.freeMemory();
				}
				if (memoryMax < runtime.maxMemory()) {
					memoryTotal = runtime.maxMemory();
				}
			}

			// regularly commit at intervals
			if (n_desc % commitCt == 0) {
				tx.commit();
				manager.clear();
				tx.begin();
			}

		}

		// commit any remaining objects
		tx.commit();
		manager.clear();

		getLog().info("      " + n_desc + " descriptions loaded");
		getLog().info("      " + n_lang + " language ref sets loaded");
		getLog().info(
				"      " + n_skip + " language ref sets skipped (no description)");

		// print memory information
		getLog().info("MEMORY USAGE:");
		getLog().info(" Total: " + memoryTotal);
		getLog().info(" Free:  " + memoryFree);
		getLog().info(" Max:   " + memoryMax);
	}

	/**
	 * Returns the next description.
	 * 
	 * @return the next description
	 * @throws Exception the exception
	 */
	private Description getNextDescription() throws Exception {

		String line, fields[];
		Description description = new DescriptionJpa();
		description.setTerminologyId("-1");

		if ((line = descriptionsByDescription.readLine()) != null) {

			line = line.replace("\r", "");
			fields = line.split("\t");

			if (!fields[0].equals("id")) { // header

				description.setTerminologyId(fields[0]);
				description.setEffectiveTime(dt.parse(fields[1]));
				description.setActive(fields[2].equals("1") ? true : false);
				description.setModuleId(Long.valueOf(fields[3]));

				description.setLanguageCode(fields[5]);
				description.setTypeId(Long.valueOf(fields[6]));
				description.setTerm(fields[7]);
				description.setCaseSignificanceId(Long.valueOf(fields[8]));
				description.setTerminology(terminology);
				description.setTerminologyVersion(version);

				// set concept from cache
				Concept concept =
						getConcept(fields[4], description.getTerminology(),
								description.getTerminologyVersion());

				if (concept != null) {
					description.setConcept(concept);
				} else {
					getLog().info(
							"Description " + description.getTerminologyId()
									+ " references non-existent concept " + fields[4]);
				}
				// otherwise get next line
			} else {
				description = getNextDescription();
			}
		}

		return description;
	}

	/**
	 * Utility function to return the next line of language ref set files in
	 * object form.
	 * 
	 * @return a partial language ref set member (lacks full description)
	 * @throws Exception the exception
	 */
	private LanguageRefSetMember getNextLanguage() throws Exception {

		String line, fields[];
		LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
		languageRefSetMember.setTerminologyId("-1");

		// if non-null
		if ((line = languageRefsetsByDescription.readLine()) != null) {

			// for (int j = 0; j < line.length(); j++)
			// System.out.print(line.charAt(j));

			line = line.replace("\r", "");

			fields = line.split("\t");

			if (!fields[0].equals("id")) { // header line

				// Universal RefSet attributes
				languageRefSetMember.setTerminologyId(fields[0]);
				languageRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				languageRefSetMember.setActive(fields[2].equals("1") ? true : false);
				languageRefSetMember.setModuleId(Long.valueOf(fields[3]));
				languageRefSetMember.setRefSetId(fields[4]);

				// Language unique attributes
				languageRefSetMember.setAcceptabilityId(Long.valueOf(fields[6]));

				// Terminology attributes
				languageRefSetMember.setTerminology(terminology);
				languageRefSetMember.setTerminologyVersion(version);

				// Set a dummy description with terminology id only
				Description description = new DescriptionJpa();
				description.setTerminologyId(fields[5]);
				languageRefSetMember.setDescription(description);

				// if header line, get next record
			} else {
				languageRefSetMember = getNextLanguage();
			}

			// if null, set a dummy description value to avoid null-pointer exceptions
			// in main loop
		} else {
			Description description = new DescriptionJpa();
			description.setTerminologyId("-1");
			languageRefSetMember.setDescription(description);
		}

		return languageRefSetMember;
	}

	/**
	 * Load AttributeRefSets (Content).
	 * 
	 * @throws Exception the exception
	 */

	@SuppressWarnings("boxing")
	private void loadAttributeValueRefSets() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = attributeRefsetsByDescription.readLine()) != null) {

			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			AttributeValueRefSetMember attributeValueRefSetMember =
					new AttributeValueRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				attributeValueRefSetMember.setTerminologyId(fields[0]);
				attributeValueRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				attributeValueRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				attributeValueRefSetMember.setModuleId(Long.valueOf(fields[3]));
				attributeValueRefSetMember.setRefSetId(fields[4]);

				// AttributeValueRefSetMember unique attributes
				attributeValueRefSetMember.setValueId(Long.valueOf(fields[6]));

				// Terminology attributes
				attributeValueRefSetMember.setTerminology(terminology);
				attributeValueRefSetMember.setTerminologyVersion(version);

				// Retrieve concept -- firstToken is referencedComponentId
				Concept concept =
						getConcept(fields[5], attributeValueRefSetMember.getTerminology(),
								attributeValueRefSetMember.getTerminologyVersion());

				if (concept != null) {

					attributeValueRefSetMember.setConcept(concept);
					manager.persist(attributeValueRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					getLog().info(
							"attributeValueRefSetMember "
									+ attributeValueRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();
	}

	/**
	 * Load SimpleRefSets (Content).
	 * 
	 * @throws Exception the exception
	 */

	private void loadSimpleRefSets() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = simpleRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			SimpleRefSetMember simpleRefSetMember = new SimpleRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				simpleRefSetMember.setTerminologyId(fields[0]);
				simpleRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleRefSetMember.setActive(fields[2].equals("1") ? true : false);
				simpleRefSetMember.setModuleId(Long.valueOf(fields[3]));
				simpleRefSetMember.setRefSetId(fields[4]);

				// SimpleRefSetMember unique attributes
				// NONE

				// Terminology attributes
				simpleRefSetMember.setTerminology(terminology);
				simpleRefSetMember.setTerminologyVersion(version);

				// Retrieve Concept -- firstToken is referencedComonentId
				Concept concept =
						getConcept(fields[5], simpleRefSetMember.getTerminology(),
								simpleRefSetMember.getTerminologyVersion());

				if (concept != null) {
					simpleRefSetMember.setConcept(concept);
					manager.persist(simpleRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					getLog().info(
							"simpleRefSetMember " + simpleRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();
	}

	/**
	 * Load SimpleMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */
	private void loadSimpleMapRefSets() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = simpleMapRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			SimpleMapRefSetMember simpleMapRefSetMember =
					new SimpleMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				simpleMapRefSetMember.setTerminologyId(fields[0]);
				simpleMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
				simpleMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				simpleMapRefSetMember.setRefSetId(fields[4]);

				// SimpleMap unique attributes
				simpleMapRefSetMember.setMapTarget(fields[6]);

				// Terminology attributes
				simpleMapRefSetMember.setTerminology(terminology);
				simpleMapRefSetMember.setTerminologyVersion(version);

				// Retrieve concept -- firstToken is referencedComponentId
				Concept concept =
						getConcept(fields[5], simpleMapRefSetMember.getTerminology(),
								simpleMapRefSetMember.getTerminologyVersion());

				if (concept != null) {
					simpleMapRefSetMember.setConcept(concept);
					manager.persist(simpleMapRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					getLog().info(
							"simpleMapRefSetMember "
									+ simpleMapRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();
	}

	/**
	 * Load ComplexMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */
	private void loadComplexMapRefSets() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = complexMapRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember =
					new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
				complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[11]));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(0); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(terminology);
				complexMapRefSetMember.setTerminologyVersion(version);

				// set Concept
				Concept concept =
						getConcept(fields[5], complexMapRefSetMember.getTerminology(),
								complexMapRefSetMember.getTerminologyVersion());

				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					manager.persist(complexMapRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					getLog().info(
							"complexMapRefSetMember "
									+ complexMapRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}

			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();

	}

	/**
	 * Load ExtendedMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */

	// NOTE: ExtendedMap RefSets are loaded into ComplexMapRefSetMember
	// where mapRelationId = mapCategoryId
	private void loadExtendedMapRefSets() throws Exception {

		String line = "";
		objectCt = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = extendedMapRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember =
					new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
				complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[12]));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(terminology);
				complexMapRefSetMember.setTerminologyVersion(version);

				// set Concept
				Concept concept =
						getConcept(fields[5], complexMapRefSetMember.getTerminology(),
								complexMapRefSetMember.getTerminologyVersion());

				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					manager.persist(complexMapRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						tx.commit();
						manager.clear();
						tx.begin();
					}
				} else {
					getLog().info(
							"complexMapRefSetMember "
									+ complexMapRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}

			}
		}

		// commit any remaining objects
		tx.commit();
		manager.clear();

	}
}
