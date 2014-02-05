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
 *       <executions>
 *         <execution>
 *           <id>load-rf2-snapshot</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-rf2-snapshot</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-sources/org/ihtsdo</propertiesFile>
 *             <terminology>SNOMEDCT</terminology>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-rf2-snapshot
 * 
 * @phase process-resources
 */
public class RF2SnapshotLoaderMojo extends AbstractMojo {

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

	/** String for core input directory. */
	private String coreInputDirString;

	/** Core input directory. */
	private File coreInputDir;

	/** The core rel input file. */
	private File coreRelInputFile = null;

	/** The core stated rel input file. */
	private File coreStatedRelInputFile = null;

	/** The core concept input file. */
	private File coreConceptInputFile = null;

	/** The core description input file. */
	private File coreDescriptionInputFile = null;

	/** The core simple refset input file. */
	private File coreSimpleRefsetInputFile = null;

	/** The core association reference input file. */
	private File coreAssociationReferenceInputFile = null;

	/** The core attribute value input file. */
	private File coreAttributeValueInputFile = null;

	/** The core complex map input file. */
	private File coreComplexMapInputFile = null;

	/** The core complex map input file. */
	private File coreExtendedMapInputFile = null;

	/** The core simple map input file. */
	private File coreSimpleMapInputFile = null;

	/** The core language input file. */
	private File coreLanguageInputFile = null;

	/** The core identifier input file. */
	private File coreIdentifierInputFile = null;

	/** The core text definition input file. */
	private File coreTextDefinitionInputFile = null;

	/** The log file. */
	private File log_file = null;

	/** The core rel input. */
	private BufferedReader coreRelInput = null;

	/** The core stated rel input. */
	private BufferedReader coreStatedRelInput = null;

	/** The core concept input. */
	private BufferedReader coreConceptInput = null;

	/** The core desc input. */
	private BufferedReader coreDescInput = null;

	/** The core simple refset input. */
	private BufferedReader coreSimpleRefsetInput = null;

	/** The core association reference input. */
	private BufferedReader coreAssociationReferenceInput = null;

	/** The core attribute value input. */
	private BufferedReader coreAttributeValueInput = null;

	/** The core complex map input. */
	private BufferedReader coreComplexMapInput = null;

	/** The core extended map input. */
	private BufferedReader coreExtendedMapInput = null;

	/** The core simple map input. */
	private BufferedReader coreSimpleMapInput = null;

	/** The core language input. */
	private BufferedReader coreLanguageInput = null;

	/** The core identifier input. */
	private BufferedReader coreIdentifierInput = null;

	/** The core text definition input. */
	private BufferedReader coreTextDefinitionInput = null;

	/** The log buffered writer. */
	private BufferedWriter log_file_out = null;

	/** The core refset input dir. */
	private File coreRefsetInputDir = null;

	/** The core terminology input dir. */
	private File coreTerminologyInputDir = null;

	/** The core content input dir. */
	private File coreContentInputDir = null;

	/** The core crossmap input dir. */
	private File coreCrossmapInputDir = null;

	/** The core language input dir. */
	private File coreLanguageInputDir = null;

	/** The core metadata input dir. */
	private File coreMetadataInputDir = null;

	// directory for sorted files
	/** The sorted_files. */
	private File sorted_files;

	// filenames for sorted files
	/** The extended_map_refsets_by_concept_file. */
	private File concepts_by_concept_file, descriptions_by_description_file,
			descriptions_core_by_description_file,
			descriptions_text_by_description_file,
			relationships_by_source_concept_file, relationships_by_dest_concept_file,
			language_refsets_by_description_file, attribute_refsets_by_concept_file,
			simple_refsets_by_concept_file, simple_map_refsets_by_concept_file,
			complex_map_refsets_by_concept_file,
			extended_map_refsets_by_concept_file;

	// buffered readers for sorted files
	/** The extended_map_refsets_by_concept. */
	private BufferedReader concepts_by_concept, descriptions_by_description,
			relationships_by_source_concept, language_refsets_by_description,
			attribute_refsets_by_concept, simple_refsets_by_concept,
			simple_map_refsets_by_concept, complex_map_refsets_by_concept,
			extended_map_refsets_by_concept;

	/** The date format. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The version. */
	private String version = "";

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

	// speed
	// Concept
	// assignment
	// to
	// ConceptRefSetMembers

	/** hash set for storing default preferred names. */
	Map<Long, String> defaultPreferredNames = new HashMap<Long, String>();

	/** Efficiency testing and committing parameters. */
	private long startTime, startTimeOrig;

	/** The n_objects. */
	int n_objects; // counter for objects created, reset in each load section

	/** The n_commit. */
	int n_commit = 200; // the number of objects to create before committing

	/** The runtime. */
	Runtime runtime;

	/** The memory max. */
	Long memoryMax = new Long(0);

	/** The memory free. */
	Long memoryFree = new Long(0);

	/** The memory total. */
	Long memoryTotal = new Long(0);

	/**
	 * Instantiates a {@link RF2SnapshotLoaderMojo} from the specified parameters.
	 * 
	 */
	public RF2SnapshotLoaderMojo() {
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
			runtime = Runtime.getRuntime();
			startTimeOrig = System.nanoTime();

			getLog().info("Start loading RF2 data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);

			// set the input directory
			coreInputDirString = properties.getProperty("loader.SNOMEDCT.input.data");
			coreInputDir = new File(coreInputDirString);
			if (!coreInputDir.exists()) {
				throw new MojoFailureException(
						"Specified loader.SNOMEDCT.input.data directory does not exist: "
								+ coreInputDirString);
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
							+ Integer.toString(n_commit));

			// close the Properties file
			propertiesInputStream.close();

			// create Entitymanager
			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			getLog().info(String.valueOf(runtime.totalMemory()) + " total memory");
			getLog().info(String.valueOf(runtime.freeMemory()) + " free memory");
			getLog().info(String.valueOf(runtime.maxMemory()) + " max memory");

			// Preparation
			openInputFiles();
			determineVersion();
			SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a"); // format for
																																// logging

			// Prepare sorted input files
			sorted_files = new File(coreInputDir, "/RF2-sorted-temp/");

			getLog().info("Preparing input files...");
			startTime = System.nanoTime();
			prepareSortedFiles();
			getLog().info(
					"    File preparation complete in " + getElapsedTime() + "s");

			closeAllInputFiles();

			EntityTransaction tx = manager.getTransaction();
			try {

				// load Concepts
				if (concepts_by_concept != null) {

					getLog().info("    Loading Concepts...");

					startTime = System.nanoTime();
					loadConcepts();
					getLog().info(
							"      " + Integer.toString(n_objects) + " Concepts loaded in "
									+ getElapsedTime() + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load Descriptions and Language Ref Set Members
				if (descriptions_by_description != null
						&& language_refsets_by_description != null) {
					getLog().info("    Loading Descriptions and LanguageRefSets...");
					startTime = System.nanoTime();
					//
					loadDescriptionsAndLanguageRefSets();
					getLog().info(
							"      "
									+ " Descriptions and language ref set members loaded in "
									+ getElapsedTime() + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");

					// set default preferred names
					getLog().info(" Setting default preferred names for all concepts...");
					startTime = System.nanoTime();
					tx.begin();
					Iterator<Concept> concept_iter = conceptCache.values().iterator();
					n_objects = 0;
					int n_count = 0;
					int n_skipped = 0;
					while (concept_iter.hasNext()) {
						Concept c_cached = concept_iter.next();

						Concept c_db = manager.find(ConceptJpa.class, c_cached.getId());
						if (defaultPreferredNames.get(c_db.getId()) != null) {
							c_db.setDefaultPreferredName(defaultPreferredNames.get(c_db
									.getId()));
						} else {
							c_db.setDefaultPreferredName("No default preferred name found");
							n_skipped++;
						}

						manager.merge(c_db);

						if (++n_count % 50000 == 0) {
							getLog().info(Integer.toString(n_count));
						}

						if (++n_objects % n_commit == 0) {
							manager.flush();
							manager.clear();
							tx.commit();
							tx.begin();
						}
					}
					tx.commit();
					getLog().info(
							"      " + "Names set in " + getElapsedTime().toString() + "s");
					getLog().info(
							"      " + Integer.toString(n_skipped)
									+ " concepts had no default preferred name");
					defaultPreferredNames.clear();

				}

				// load Relationships
				if (relationships_by_source_concept != null) {
					getLog().info("    Loading Relationships...");
					startTime = System.nanoTime();

					loadRelationships();

					getLog().info(
							"      " + Integer.toString(n_objects) + " Concepts loaded in "
									+ getElapsedTime() + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				// load Simple RefSets (Content)
				if (simple_refsets_by_concept != null) {
					getLog().info("    Loading Simple RefSets...");
					startTime = System.nanoTime();

					loadSimpleRefSets();

					getLog().info(
							"      " + Integer.toString(n_objects)
									+ " Simple Refsets loaded in " + getElapsedTime() + "s"
									+ " (Ended at " + ft.format(new Date()) + ")");
				}

				// load SimpleMapRefSets
				if (simple_map_refsets_by_concept != null) {
					getLog().info("    Loading SimpleMap RefSets...");
					startTime = System.nanoTime();

					loadSimpleMapRefSets();
					getLog().info(
							"      " + Integer.toString(n_objects)
									+ " SimpleMap RefSets loaded in " + getElapsedTime() + "s"
									+ " (Ended at " + ft.format(new Date()) + ")");
				}

				// load ComplexMapRefSets
				if (complex_map_refsets_by_concept != null) {
					getLog().info("    Loading ComplexMap RefSets...");
					startTime = System.nanoTime();

					loadComplexMapRefSets();
					getLog().info(
							"      " + Integer.toString(n_objects)
									+ " ComplexMap RefSets loaded in " + getElapsedTime() + "s"
									+ " (Ended at " + ft.format(new Date()) + ")");
				}

				// load ExtendedMapRefSets
				if (extended_map_refsets_by_concept != null) {
					getLog().info("    Loading ExtendedMap RefSets...");
					startTime = System.nanoTime();

					loadExtendedMapRefSets();

					getLog().info(
							"      " + Integer.toString(n_objects)
									+ " ExtendedMap RefSets loaded in " + getElapsedTime() + "s"
									+ " (Ended at " + ft.format(new Date()) + ")");
				}

				// load AttributeValue RefSets (Content)
				if (attribute_refsets_by_concept != null) {
					getLog().info("    Loading AttributeValue RefSets...");
					startTime = System.nanoTime();

					loadAttributeValueRefSets();
					getLog().info(
							"      " + Integer.toString(n_objects)
									+ " AttributeValue RefSets loaded in "
									+ getElapsedTime().toString() + "s" + " (Ended at "
									+ ft.format(new Date()) + ")");
				}

				getLog().info(
						"    Total elapsed time for run: " + getTotalElapsedTimeStr());
				getLog().info("done ...");

			} catch (Exception e) {
				e.printStackTrace();

				throw e;
			}

			// Clean-up
			manager.close();
			factory.close();
			closeAllSortedFiles();
			log_file_out.close();
			// deleteSortedFiles(sorted_files);

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
	private Long getElapsedTime() {
		return (System.nanoTime() - startTime) / 1000000000;
	}

	/**
	 * Returns the total elapsed time str.
	 * 
	 * @return the total elapsed time str
	 */
	@SuppressWarnings("boxing")
	private String getTotalElapsedTimeStr() {

		Long resultnum = (System.nanoTime() - startTimeOrig) / 1000000000;
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
	 * File management for sorted files; calls sort_RF2_Files.
	 * 
	 * @throws Exception the exception
	 */
	// *************************************************************************************
	// //
	//
	// *************************************************************************************
	// //
	private void prepareSortedFiles() throws Exception {

		// ******************** //
		// Initialize files //
		// ******************** //

		concepts_by_concept_file =
				new File(sorted_files, "concepts_by_concept.sort");
		descriptions_by_description_file =
				new File(sorted_files, "descriptions_by_description.sort");
		descriptions_core_by_description_file =
				new File(sorted_files, "descriptions_core_by_description.sort");
		descriptions_text_by_description_file =
				new File(sorted_files, "descriptions_text_by_description.sort");
		relationships_by_source_concept_file =
				new File(sorted_files, "relationship_by_source_concept.sort");
		relationships_by_dest_concept_file =
				new File(sorted_files, "relationship_by_dest_concept.sort");
		language_refsets_by_description_file =
				new File(sorted_files, "language_refsets_by_description.sort");
		attribute_refsets_by_concept_file =
				new File(sorted_files, "attribute_refsets_by_concept.sort");
		simple_refsets_by_concept_file =
				new File(sorted_files, "simple_refsets_by_concept.sort");
		simple_map_refsets_by_concept_file =
				new File(sorted_files, "simple_map_refsets_by_concept.sort");
		complex_map_refsets_by_concept_file =
				new File(sorted_files, "complex_map_refsets_by_concept.sort");
		extended_map_refsets_by_concept_file =
				new File(sorted_files, "extended_map_refsets_by_concept.sort");
		log_file = new File(sorted_files, "RF2SnapshotLoader_log.txt");

		// *********************** //
		// Sort files if required //
		// *********************** //

		// if sorted files do not exist OR sorted files are older than last modified
		// input file, sort
		if (!sorted_files.exists()
				|| getLastModified(sorted_files) < getLastModified(coreInputDir)) {

			// log reason for sort
			if (!sorted_files.exists()) {
				getLog().info("     No sorted files exist -- sorting RF2 files");
			} else if (getLastModified(sorted_files) < getLastModified(coreInputDir)) {
				getLog().info(
						"     Sorted files older than input files -- sorting RF2 files");
			}

			// delete any existing temporary files
			FileSorter.deleteSortedFiles(sorted_files);

			// test whether file/folder still exists (i.e. delete error)
			if (sorted_files.exists()) {
				throw new MojoFailureException(
						"Could not delete existing sorted files folder "
								+ sorted_files.toString());
			}

			// attempt to make sorted files directory
			if (sorted_files.mkdir()) {
				getLog().info(
						" Creating new sorted files folder " + sorted_files.toString());
			} else {
				throw new MojoFailureException(
						" Could not create temporary sorted file folder "
								+ sorted_files.toString());
			}

			// sort files
			sortRf2Files();
		} else {
			getLog().info(
					"    Sorted files exist and are up to date.  No sorting required");
		}

		// ************************* //
		// Test and open files //
		// ************************* //

		// Concepts
		concepts_by_concept =
				new BufferedReader(new FileReader(concepts_by_concept_file));

		// Relationships by source concept
		relationships_by_source_concept =
				new BufferedReader(new FileReader(relationships_by_source_concept_file));

		// Descriptions by description id
		descriptions_by_description =
				new BufferedReader(new FileReader(descriptions_by_description_file));

		// Language RefSets by description id
		language_refsets_by_description =
				new BufferedReader(new FileReader(language_refsets_by_description_file));

		// ******************************************************* //
		// Component RefSet Members //
		// ******************************************************* //

		// Attribute Value
		attribute_refsets_by_concept =
				new BufferedReader(new FileReader(attribute_refsets_by_concept_file));

		// Simple
		simple_refsets_by_concept =
				new BufferedReader(new FileReader(simple_refsets_by_concept_file));

		// Simple Map
		simple_map_refsets_by_concept =
				new BufferedReader(new FileReader(simple_map_refsets_by_concept_file));

		// Complex map
		complex_map_refsets_by_concept =
				new BufferedReader(new FileReader(complex_map_refsets_by_concept_file));

		// Extended map
		extended_map_refsets_by_concept =
				new BufferedReader(new FileReader(extended_map_refsets_by_concept_file));

		// ******************************************************* //
		// Log file
		// ******************************************************* //

		log_file_out = new BufferedWriter(new FileWriter(log_file));
	}

	/*
	 * private void sort_RF2_files() throws Exception { // core descriptions by
	 * description sort_RF2_file(coreDescriptionInputFile,
	 * descriptions_core_by_description_file, 0); }
	 */

	/**
	 * Sorts all files by concept or referencedComponentId.
	 * 
	 * @throws Exception the exception
	 */
	private void sortRf2Files() throws Exception {

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
							}, sorted_files, ""); // header line

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
		sortRf2File(coreAttributeValueInputFile,
				attribute_refsets_by_concept_file, 5);
		sortRf2File(coreSimpleRefsetInputFile, simple_refsets_by_concept_file, 5);
		sortRf2File(coreSimpleMapInputFile, simple_map_refsets_by_concept_file, 5);
		sortRf2File(coreComplexMapInputFile, complex_map_refsets_by_concept_file,
				5);
		sortRf2File(coreExtendedMapInputFile,
				extended_map_refsets_by_concept_file, 5);
		sortRf2File(coreLanguageInputFile, language_refsets_by_description_file,
				5);

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

	/*
	*//**
	 * Returns the description.
	 * 
	 * @return the description
	 * @throws Exception the exception
	 */
	/*
	 * private Description getDescription(String terminologyId, String
	 * terminology, String terminologyVersion) throws Exception {
	 * 
	 * if (descriptionCache.containsKey(terminologyId + terminology +
	 * terminologyVersion)) { // uses hibernate first-level cache return
	 * manager.find( DescriptionJpa.class, descriptionCache.get(terminologyId +
	 * terminology + terminologyVersion)); }
	 * 
	 * Query query = manager .createQuery(
	 * "select d from DescriptionJpa d where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology"
	 * );
	 * 
	 * try { query.setParameter("terminologyId", terminologyId);
	 * query.setParameter("terminology", terminology);
	 * query.setParameter("terminologyVersion", terminologyVersion);
	 * 
	 * Description d = (Description) query.getSingleResult();
	 * 
	 * descriptionCache.put(terminologyId + terminology + terminologyVersion,
	 * d.getId());
	 * 
	 * return d;
	 * 
	 * } catch (NoResultException e) { // Log and return null if there are no
	 * releases getLog().info( "Description query for terminologyId = " +
	 * terminologyId + ", terminology = " + terminology +
	 * ", terminologyVersion = " + terminologyVersion + " returned no results!");
	 * return null; } }
	 */
	/**
	 * Opens input files.
	 * 
	 * @throws Exception if something goes wrong
	 */
	private void openInputFiles() throws Exception {

		// CORE
		coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
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
		coreRefsetInputDir = new File(coreInputDir, "/Refset/");
		coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
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

		coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
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

		coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
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

		coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
		getLog().info(
				"  Core Metadata Input Dir = " + coreMetadataInputDir.toString() + " "
						+ coreMetadataInputDir.exists());

		coreRelInput = new BufferedReader(new FileReader(coreRelInputFile));
		coreStatedRelInput =
				new BufferedReader(new FileReader(coreStatedRelInputFile));
		coreConceptInput = new BufferedReader(new FileReader(coreConceptInputFile));
		coreDescInput =
				new BufferedReader(new FileReader(coreDescriptionInputFile));
		coreSimpleRefsetInput =
				new BufferedReader(new FileReader(coreSimpleRefsetInputFile));
		coreAssociationReferenceInput =
				new BufferedReader(new FileReader(coreAssociationReferenceInputFile));
		coreAttributeValueInput =
				new BufferedReader(new FileReader(coreAttributeValueInputFile));
		if (coreComplexMapInputFile != null)
			coreComplexMapInput =
					new BufferedReader(new FileReader(coreComplexMapInputFile));
		if (coreExtendedMapInputFile != null)
			coreExtendedMapInput =
					new BufferedReader(new FileReader(coreExtendedMapInputFile));
		coreSimpleMapInput =
				new BufferedReader(new FileReader(coreSimpleMapInputFile));
		coreLanguageInput =
				new BufferedReader(new FileReader(coreLanguageInputFile));
		coreIdentifierInput =
				new BufferedReader(new FileReader(coreIdentifierInputFile));
		if (coreTextDefinitionInputFile != null)
			coreTextDefinitionInput =
					new BufferedReader(new FileReader(coreTextDefinitionInputFile));
	}

	/**
	 * Sets and logs the version based on Concept input filename.
	 */
	private void determineVersion() {
		int index = coreConceptInputFile.getName().indexOf(".txt");
		version = coreConceptInputFile.getName().substring(index - 8, index);
		getLog().info("Version " + version);
	}

	/**
	 * Closes all files.
	 * 
	 * @throws Exception if something goes wrong
	 */
	private void closeAllInputFiles() throws Exception {
		coreRelInput.close();
		coreStatedRelInput.close();
		coreConceptInput.close();
		coreDescInput.close();
		coreSimpleRefsetInput.close();
		coreAssociationReferenceInput.close();
		coreAttributeValueInput.close();
		if (coreComplexMapInput != null)
			coreComplexMapInput.close();
		if (coreExtendedMapInput != null)
			coreComplexMapInput.close();
		coreSimpleMapInput.close();
		coreLanguageInput.close();
		coreIdentifierInput.close();
		if (coreTextDefinitionInput != null)
			coreTextDefinitionInput.close();
	}

	/**
	 * Closes all sorted temporary files.
	 * 
	 * @throws Exception if something goes wrong
	 */
	private void closeAllSortedFiles() throws Exception {
		if (concepts_by_concept != null) {
			concepts_by_concept.close();
		}
		if (descriptions_by_description != null) {
			descriptions_by_description.close();
		}
		if (relationships_by_source_concept != null) {
			relationships_by_source_concept.close();
		}
		if (language_refsets_by_description != null) {
			language_refsets_by_description.close();
		}
		if (attribute_refsets_by_concept != null) {
			attribute_refsets_by_concept.close();
		}
		if (simple_refsets_by_concept != null) {
			simple_refsets_by_concept.close();
		}
		if (simple_map_refsets_by_concept != null) {
			simple_map_refsets_by_concept.close();
		}
		if (complex_map_refsets_by_concept != null) {
			complex_map_refsets_by_concept.close();
		}
		if (extended_map_refsets_by_concept != null) {
			extended_map_refsets_by_concept.close();
		}
	}

	/**
	 * Load concepts.
	 * 
	 * @throws Exception the exception
	 */
	private void loadConcepts() throws Exception {

		String line = "";
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = concepts_by_concept.readLine()) != null) {

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

				manager.persist(concept);

				conceptCache.put(new String(fields[0] + concept.getTerminology()
						+ concept.getTerminologyVersion()), concept);

				// memory debugging
				if (n_objects % n_commit == 0) {
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
				if (++n_objects % n_commit == 0) {

					manager.flush();
					manager.clear();
					tx.commit();
					tx.begin();
				}
			}
		}

		// commit any remaining objects
		tx.commit();

		// print memory information
		getLog().info("MEMORY USAGE:");
		getLog().info(" Total: " + memoryTotal.toString());
		getLog().info(" Free:  " + memoryFree.toString());
		getLog().info(" Max:   " + memoryMax.toString());

	}

	/**
	 * Load relationships.
	 * 
	 * @throws Exception the exception
	 */
	private void loadRelationships() throws Exception {

		String line = "";
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = relationships_by_source_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					if (sourceConcept == null) {
						log_file_out
								.write("Relationship " + relationship.getTerminologyId()
										+ " references non-existent source concept " + fields[4]
										+ "\n");
						log_file_out.newLine();
					}
					if (destinationConcept == null) {
						log_file_out.write("Relationship "
								+ relationship.getTerminologyId()
								+ " references non-existent destination concept " + fields[5]
								+ "\n");
						log_file_out.newLine();
					}

				}
			}
		}

		// commit any remaining objects
		tx.commit();
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

				log_file_out.write("     " + "Language Ref Set "
						+ language.getTerminologyId()
						+ " references non-existent description "
						+ language.getDescription().getTerminologyId() + "\n");
				log_file_out.newLine();
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
						log_file_out.write("Multiple default preferred names for concept "
								+ concept.getTerminologyId());
						log_file_out.newLine();
						log_file_out.write("  " + "Existing: "
								+ defaultPreferredNames.get(concept.getId()));
						log_file_out.newLine();
						log_file_out.write("  " + "Replaced: " + description.getTerm());
						log_file_out.newLine();

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
			if (n_desc % n_commit == 0) {
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
			if (n_desc % n_commit == 0) {
				manager.flush();
				manager.clear();
				tx.commit();
				tx.begin();
			}

		}

		// commit any remaining objects
		tx.commit();

		getLog().info("      " + n_desc + " descriptions loaded");
		getLog().info("      " + n_lang + " language ref sets loaded");
		getLog().info(
				"      " + n_skip + " language ref sets skipped (no description)");

		// print memory information
		getLog().info("MEMORY USAGE:");
		getLog().info(" Total: " + memoryTotal.toString());
		getLog().info(" Free:  " + memoryFree.toString());
		getLog().info(" Max:   " + memoryMax.toString());
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

		if ((line = descriptions_by_description.readLine()) != null) {

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
					log_file_out.write("Description " + description.getTerminologyId()
							+ " references non-existent concept " + fields[4] + "\n");
					log_file_out.newLine();
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
		if ((line = language_refsets_by_description.readLine()) != null) {

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
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = attribute_refsets_by_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					log_file_out.write("attributeValueRefSetMember "
							+ attributeValueRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5] + "\n");
					log_file_out.newLine();
				}
			}
		}

		// commit any remaining objects
		tx.commit();
	}

	/**
	 * Load SimpleRefSets (Content).
	 * 
	 * @throws Exception the exception
	 */

	private void loadSimpleRefSets() throws Exception {

		String line = "";
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = simple_refsets_by_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					log_file_out.write("simpleRefSetMember "
							+ simpleRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5] + "\n");
					log_file_out.newLine();
				}
			}
		}

		// commit any remaining objects
		tx.commit();
	}

	/**
	 * Load SimpleMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */
	private void loadSimpleMapRefSets() throws Exception {

		String line = "";
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = simple_map_refsets_by_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					log_file_out.write("simpleMapRefSetMember "
							+ simpleMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5] + "\n");
					log_file_out.newLine();
				}
			}
		}

		// commit any remaining objects
		tx.commit();
	}

	/**
	 * Load ComplexMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */
	private void loadComplexMapRefSets() throws Exception {

		String line = "";
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = complex_map_refsets_by_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					log_file_out.write("complexMapRefSetMember "
							+ complexMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5] + "\n");
					log_file_out.newLine();
				}

			}
		}

		// commit any remaining objects
		tx.commit();

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
		n_objects = 0;

		// begin transcation
		EntityTransaction tx = manager.getTransaction();
		tx.begin();

		while ((line = extended_map_refsets_by_concept.readLine()) != null) {

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
					if (++n_objects % n_commit == 0) {
						manager.flush();
						manager.clear();
						tx.commit();
						tx.begin();
					}
				} else {
					log_file_out.write("complexMapRefSetMember "
							+ complexMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5] + "\n");
					log_file_out.newLine();
				}

			}
		}

		// commit any remaining objects
		tx.commit();

	}
}
