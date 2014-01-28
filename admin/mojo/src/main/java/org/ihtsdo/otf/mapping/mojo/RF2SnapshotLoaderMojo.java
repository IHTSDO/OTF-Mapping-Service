/**
 * Copyright (c) 2012 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.HashMap;
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

	/** String for core input directory */
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
	
	/** The log file */
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
	
	/** The log buffered writer */
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
	private File sorted_files;

	// filenames for sorted files
	private File concepts_by_concept_file, descriptions_by_concept_file,
			descriptions_by_description_file,
			relationships_by_source_concept_file,
			relationships_by_dest_concept_file,
			language_refsets_by_description_file,
			attribute_refsets_by_concept_file, simple_refsets_by_concept_file,
			simple_map_refsets_by_concept_file,
			complex_map_refsets_by_concept_file,
			extended_map_refsets_by_concept_file;

	// buffered readers for sorted files
	private BufferedReader concepts_by_concept, descriptions_by_description,
			relationships_by_source_concept, language_refsets_by_description,
			attribute_refsets_by_concept, simple_refsets_by_concept,
			simple_map_refsets_by_concept, complex_map_refsets_by_concept,
			extended_map_refsets_by_concept;

	// hashmap used for determioning defaultPreferredNames during file loads
	Map<String, String> defaultPreferredNames = new HashMap<String, String>();

	/** The date format. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The version. */
	private String version = "";

	/** the defaultPreferredNames values */
	private Long dpnTypeId;
	private Long dpnRefSetId;
	private Long dpnAcceptabilityId;

	/** The manager. */
	private EntityManager manager;

	/** hash sets for faster loading */
	private Map<String, Concept> conceptCache = new HashMap<>(); // used to
																	// speed
																	// Concept
																	// assignment
																	// to
																	// ConceptRefSetMembers
	private Map<String, Long> descriptionCache = new HashMap<>(); // speeds
																	// Description
																	// assignment
																	// to
																	// DescriptionRefSetMembers

	/** Efficiency testing */
	private long startTime, startTimeOrig;
	int i;

	/**
	 * Instantiates a {@link RF2SnapshotLoaderMojo} from the specified
	 * parameters.
	 * 
	 */
	public RF2SnapshotLoaderMojo() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		FileInputStream propertiesInputStream = null;
		try {

			startTimeOrig = System.nanoTime();

			getLog().info("Start loading RF2 data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);

			// set the input directory
			coreInputDirString = properties
					.getProperty("loader.SNOMEDCT.input.data");
			coreInputDir = new File(coreInputDirString);
			if (!coreInputDir.exists()) {
				throw new MojoFailureException(
						"Specified loader.SNOMEDCT.input.data directory does not exist: "
								+ coreInputDirString);
			}

			// set the parameters for determining defaultPreferredNames
			dpnTypeId = Long.valueOf(properties
					.getProperty("loader.defaultPreferredNames.typeId"));
			dpnRefSetId = Long.valueOf(properties
					.getProperty("loader.defaultPreferredNames.refSetId"));
			dpnAcceptabilityId = Long
					.valueOf(properties
							.getProperty("loader.defaultPreferredNames.acceptabilityId"));

			// close the Properties file
			propertiesInputStream.close();

			// create Entitymanager
			EntityManagerFactory factory = Persistence
					.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			// Preparation
			openInputFiles();
			getVersion();

			// Prepare sorted input files
			sorted_files = new File(coreInputDir, "/RF2-sorted-temp/");
			getLog().info("Sorting Files");
			startTime = System.nanoTime();
			prepareSortedFiles();
			getLog().info("    Files sorted in " + getElapsedTime() + "s");

			closeAllInputFiles();

			EntityTransaction tx = manager.getTransaction();
			try {

				// load Concepts
				if (concepts_by_concept != null) {
					startTime = System.nanoTime();
					getLog().info("    Loading Concepts...");
					tx.begin();
					loadConcepts();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i) + " Concepts loaded in "
									+ getElapsedTime() + "s");
				}

				// List<Number> revNumbers =
				// reader.getRevisions(ConceptJpa.class, testconcept.getId());
				// getLog().info("concept: " + testconcept.getTerminologyId() +
				// " - Versions: " + revNumbers.toString());

				// load Descriptions
				if (descriptions_by_description != null) {
					getLog().info("    Loading Descriptions...");
					startTime = System.nanoTime();
					tx.begin();
					loadDescriptions();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i) + " Descriptions loaded in "
									+ getElapsedTime().toString() + "s");
				}

				// load Language RefSet (Language)
				if (language_refsets_by_description != null) {
					getLog().info("    Loading Language RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadLanguageRefSets();
					tx.commit();
					getLog().info(
							"      "+ Integer.toString(i)
									+ " Language RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}
		
				// description cache no longer required, clear
				descriptionCache.clear();



				// load Relationships
				if (relationships_by_source_concept != null) {
					getLog().info("    Loading Relationships...");
					startTime = System.nanoTime();
					tx.begin();
					loadRelationships();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i) + " Relationships loaded in "
									+ getElapsedTime().toString() + "s");
				}
				

				// load Simple RefSets (Content)
				if (simple_refsets_by_concept != null) {
					getLog().info("    Loading Simple RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadSimpleRefSets();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i) + " Simple RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}

				// load SimpleMapRefSets
				if (simple_map_refsets_by_concept != null) {
					getLog().info("    Loading SimpleMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadSimpleMapRefSets();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i)
									+ " SimpleMap RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}

				// load ComplexMapRefSets
				if (complex_map_refsets_by_concept != null) {
					getLog().info("    Loading ComplexMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadComplexMapRefSets();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i)
									+ " ComplexMap RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}

				// load ExtendedMapRefSets
				if (extended_map_refsets_by_concept != null) {
					getLog().info("    Loading ExtendedMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadExtendedMapRefSets();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i)
									+ " ExtendedMap RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}

				// load AttributeValue RefSets (Content)
				if (attribute_refsets_by_concept != null) {
					getLog().info("    Loading AttributeValue RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadAttributeValueRefSets();
					tx.commit();
					getLog().info(
							"      " + Integer.toString(i)
									+ " AttributeValue RefSets loaded in "
									+ getElapsedTime().toString() + "s");
				}

				getLog().info(
						"    Total elapsed time for run: "
								+ getTotalElapsedTimeStr());
				getLog().info("done ...");

			} catch (Exception e) {
				tx.rollback();
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

	/**
	 * 
	 * @param file
	 */
	public static void deleteSortedFiles(File file) {

		// Check if file is directory/folder
		if (file.isDirectory()) {
			// Get all files in the folder
			File[] files = file.listFiles();

			for (int i = 0; i < files.length; i++) {
				// Delete each file in the folder
				deleteSortedFiles(files[i]);
			}
			// Delete the folder
			file.delete();
		} else {
			// Delete the file if it is not a folder
			file.delete();
		}
	}

	// Used for debugging/efficiency monitoring
	private Long getElapsedTime() {
		return (System.nanoTime() - startTime) / 1000000000;
	}

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
	 * File management for sorted files; calls sort_RF2_Files
	 * 
	 * @throws Exception
	 *             the exception
	 */
	// *************************************************************************************
	// //
	//
	// *************************************************************************************
	// //
	private void prepareSortedFiles() throws Exception {

		// ******************** //
		// Initial file set up //
		// ******************** //

		// delete any existing temporary files
		deleteSortedFiles(sorted_files);

		// test whether file/folder still exists (i.e. delete error)
		if (sorted_files.exists()) {
			throw new MojoFailureException(
					"Could not delete existing sorted files folder "
							+ sorted_files.toString());
		}

		// attempt to make sorted files directory
		if (sorted_files.mkdir()) {
			getLog().info(
					" Creating new sorted files folder "
							+ sorted_files.toString());
		} else {
			throw new MojoFailureException(
					" Could not create temporary sorted file folder "
							+ sorted_files.toString());
		}

		concepts_by_concept_file = new File(sorted_files,
				"concepts_by_concept.sort");
		descriptions_by_concept_file = new File(sorted_files,
				"descriptions_by_concept.sort");
		descriptions_by_description_file = new File(sorted_files,
				"descriptions_by_description.sort");
		relationships_by_source_concept_file = new File(sorted_files,
				"relationship_by_source_concept.sort");
		relationships_by_dest_concept_file = new File(sorted_files,
				"relationship_by_dest_concept.sort");
		language_refsets_by_description_file = new File(sorted_files,
				"language_refsets_by_description.sort");
		attribute_refsets_by_concept_file = new File(sorted_files,
				"attribute_refsets_by_concept.sort");
		simple_refsets_by_concept_file = new File(sorted_files,
				"simple_refsets_by_concept.sort");
		simple_map_refsets_by_concept_file = new File(sorted_files,
				"simple_map_refsets_by_concept.sort");
		complex_map_refsets_by_concept_file = new File(sorted_files,
				"complex_map_refsets_by_concept.sort");
		extended_map_refsets_by_concept_file = new File(sorted_files,
				"extended_map_refsets_by_concept.sort");
		log_file = new File(sorted_files, "log_file.txt");

		// **************** //
		// Initial sorting //
		// **************** //

		sort_RF2_files();

		// ************************* //
		// Test and open files 		 //
		// ************************* //

		// Concepts
		concepts_by_concept = new BufferedReader(new FileReader(
				concepts_by_concept_file));

		// Relationships by source concept
		relationships_by_source_concept = new BufferedReader(new FileReader(
				relationships_by_source_concept_file));

		descriptions_by_description = new BufferedReader(new FileReader(
				descriptions_by_description_file));

		language_refsets_by_description = new BufferedReader(new FileReader(
				language_refsets_by_description_file));

		// ******************************************************* //
		// Component RefSet Members //
		// ******************************************************* //

		// Attribute Value
		attribute_refsets_by_concept = new BufferedReader(new FileReader(
				attribute_refsets_by_concept_file));

		// Simple
		simple_refsets_by_concept = new BufferedReader(new FileReader(
				simple_refsets_by_concept_file));

		// Simple Map
		simple_map_refsets_by_concept = new BufferedReader(new FileReader(
				simple_map_refsets_by_concept_file));

		// Complex map
		complex_map_refsets_by_concept = new BufferedReader(new FileReader(
				complex_map_refsets_by_concept_file));

		// Extended map
		extended_map_refsets_by_concept = new BufferedReader(new FileReader(
				extended_map_refsets_by_concept_file));
		
		// ******************************************************* //
		// Log file
		// ******************************************************* //
		
		log_file_out = new BufferedWriter(new FileWriter(log_file));
	}

	/**
	 * Sorts all files by concept or referencedComponentId
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void sort_RF2_files() throws Exception {

		// ****************//
		// Components //
		// ****************//

		sort_RF2_file(coreConceptInputFile, concepts_by_concept_file, 0);
		sort_RF2_file(coreDescriptionInputFile,
				descriptions_by_description_file, 0);
		sort_RF2_file(coreDescriptionInputFile, descriptions_by_concept_file, 4);
		sort_RF2_file(coreRelInputFile, relationships_by_source_concept_file, 4);
		sort_RF2_file(coreRelInputFile, relationships_by_dest_concept_file, 5);

		// ****************//
		// RefSets //
		// ****************//
		sort_RF2_file(coreAttributeValueInputFile,
				attribute_refsets_by_concept_file, 5);
		sort_RF2_file(coreSimpleRefsetInputFile,
				simple_refsets_by_concept_file, 5);
		sort_RF2_file(coreSimpleMapInputFile,
				simple_map_refsets_by_concept_file, 5);
		sort_RF2_file(coreComplexMapInputFile,
				complex_map_refsets_by_concept_file, 5);
		sort_RF2_file(coreExtendedMapInputFile,
				extended_map_refsets_by_concept_file, 5);
		sort_RF2_file(coreLanguageInputFile,
				language_refsets_by_description_file, 4);

	}

	/**
	 * Helper function for sorting an individual file with colum comparator
	 * 
	 * @param file_in
	 *            the input file to be sorted
	 * @param file_out
	 *            the resulting sorted file
	 * @param sort_column
	 *            the column ([0, 1, ...] to compare by
	 * @throws Exception
	 *             the exception
	 */
	private void sort_RF2_file(File file_in, File file_out,
			final int sort_column) throws Exception {

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
				"      Sorting " + file_in.toString() + "  into "
						+ file_out.toString() + " by column "
						+ Integer.toString(sort_column));
		sort(file_in.toString(), file_out.toString(), comp);

	}

	/**
	 * Returns the concept.
	 * 
	 * @param conceptId
	 *            the concept id
	 * @return the concept
	 */
	private Concept getConcept(String terminologyId, String terminology,
			String terminologyVersion) throws Exception {

		if (conceptCache.containsKey(terminologyId + terminology
				+ terminologyVersion)) {

			// uses hibernate first-level cache
			return conceptCache.get(terminologyId + terminology
					+ terminologyVersion);
		}

		Query query = manager
				.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		// Try to retrieve the single expected result
		// If zero or more than one result are returned, log error and set
		// result to null

		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			conceptCache.put(terminologyId + terminology + terminologyVersion,
					c);

			return c;

		} catch (NoResultException e) {
			// Log and return null if there are no releases
			getLog().info(
					"Concept query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology
							+ ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			return null;
		}

	}

	/**
	 * Returns the description.
	 * 
	 * @param descriptionId
	 *            the description id
	 * @return the description
	 */
	private Description getDescription(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {

		if (descriptionCache.containsKey(terminologyId + terminology
				+ terminologyVersion)) {
			// uses hibernate first-level cache
			return manager.find(
					DescriptionJpa.class,
					descriptionCache.get(terminologyId + terminology
							+ terminologyVersion));
		}

		Query query = manager
				.createQuery("select d from DescriptionJpa d where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Description d = (Description) query.getSingleResult();

			descriptionCache.put(terminologyId + terminology
					+ terminologyVersion, d.getId());

			return d;

		} catch (NoResultException e) {
			// Log and return null if there are no releases
			getLog().info(
					"Description query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology
							+ ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			return null;
		}
	}

	/**
	 * Opens input files.
	 * 
	 * @throws Exception
	 *             if something goes wrong
	 */
	private void openInputFiles() throws Exception {

		// CORE
		coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
		getLog().info(
				"  Core Input Dir = " + coreTerminologyInputDir.toString()
						+ " " + coreTerminologyInputDir.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Relationship_")) {
				if (coreRelInputFile != null)
					throw new MojoFailureException(
							"Multiple Relationships Files!");
				coreRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Rel Input File = " + coreRelInputFile.toString() + " "
						+ coreRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_StatedRelationship_")) {
				if (coreStatedRelInputFile != null)
					throw new MojoFailureException(
							"Multiple Stated Relationships Files!");
				coreStatedRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Stated Rel Input File = "
						+ coreStatedRelInputFile.toString() + " "
						+ coreStatedRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Concept_")) {
				if (coreConceptInputFile != null)
					throw new MojoFailureException("Multiple Concept Files!");
				coreConceptInputFile = f;
			}
		}
		getLog().info(
				"  Core Concept Input File = "
						+ coreConceptInputFile.toString() + " "
						+ coreConceptInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Description_")) {
				if (coreDescriptionInputFile != null)
					throw new MojoFailureException(
							"Multiple Description Files!");
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
				"  Core Identifier Input File = "
						+ coreIdentifierInputFile.toString() + " "
						+ coreIdentifierInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_TextDefinition_")) {
				if (coreTextDefinitionInputFile != null)
					throw new MojoFailureException(
							"Multiple TextDefinition Files!");
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
					throw new MojoFailureException(
							"Multiple Simple Refset Files!");
				coreSimpleRefsetInputFile = f;
			}
		}
		getLog().info(
				"  Core Simple Refset Input File = "
						+ coreSimpleRefsetInputFile.toString() + " "
						+ coreSimpleRefsetInputFile.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Association")) {
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
			if (f.getName().contains("Refset_Attribute")) {
				if (coreAttributeValueInputFile != null)
					throw new MojoFailureException(
							"Multiple Attribute Value Files!");
				coreAttributeValueInputFile = f;
			}
		}
		getLog().info(
				"  Core Attribute Value Input File = "
						+ coreAttributeValueInputFile.toString() + " "
						+ coreAttributeValueInputFile.exists());

		coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
		getLog().info(
				"  Core Crossmap Input Dir = "
						+ coreCrossmapInputDir.toString() + " "
						+ coreCrossmapInputDir.exists());

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("ComplexMap")) {
				if (coreComplexMapInputFile != null)
					throw new MojoFailureException(
							"Multiple Complex Map Files!");
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
					throw new MojoFailureException(
							"Multiple Extended Map Files!");
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
				"  Core Simple Map Input File = "
						+ coreSimpleMapInputFile.toString() + " "
						+ coreSimpleMapInputFile.exists());

		coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
		getLog().info(
				"  Core Language Input Dir = "
						+ coreLanguageInputDir.toString() + " "
						+ coreLanguageInputDir.exists());

		for (File f : coreLanguageInputDir.listFiles()) {
			if (f.getName().contains("Language")) {
				if (coreLanguageInputFile != null)
					throw new MojoFailureException("Multiple Language Files!");
				coreLanguageInputFile = f;
			}
		}
		getLog().info(
				"  Core Language Input File = "
						+ coreLanguageInputFile.toString() + " "
						+ coreLanguageInputFile.exists());

		coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
		getLog().info(
				"  Core Metadata Input Dir = "
						+ coreMetadataInputDir.toString() + " "
						+ coreMetadataInputDir.exists());

		coreRelInput = new BufferedReader(new FileReader(coreRelInputFile));
		coreStatedRelInput = new BufferedReader(new FileReader(
				coreStatedRelInputFile));
		coreConceptInput = new BufferedReader(new FileReader(
				coreConceptInputFile));
		coreDescInput = new BufferedReader(new FileReader(
				coreDescriptionInputFile));
		coreSimpleRefsetInput = new BufferedReader(new FileReader(
				coreSimpleRefsetInputFile));
		coreAssociationReferenceInput = new BufferedReader(new FileReader(
				coreAssociationReferenceInputFile));
		coreAttributeValueInput = new BufferedReader(new FileReader(
				coreAttributeValueInputFile));
		if (coreComplexMapInputFile != null)
			coreComplexMapInput = new BufferedReader(new FileReader(
					coreComplexMapInputFile));
		if (coreExtendedMapInputFile != null)
			coreExtendedMapInput = new BufferedReader(new FileReader(
					coreExtendedMapInputFile));
		coreSimpleMapInput = new BufferedReader(new FileReader(
				coreSimpleMapInputFile));
		coreLanguageInput = new BufferedReader(new FileReader(
				coreLanguageInputFile));
		coreIdentifierInput = new BufferedReader(new FileReader(
				coreIdentifierInputFile));
		if (coreTextDefinitionInputFile != null)
			coreTextDefinitionInput = new BufferedReader(new FileReader(
					coreTextDefinitionInputFile));
	}

	/**
	 * Sets and logs the version based on Concept input filename
	 */
	private void getVersion() {
		int index = coreConceptInputFile.getName().indexOf(".txt");
		version = coreConceptInputFile.getName().substring(index - 8, index);
		getLog().info("Version " + version);
	}

	/**
	 * Closes all files.
	 * 
	 * @throws Exception
	 *             if something goes wrong
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
	 * @throws Exception
	 *             if something goes wrong
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
	 * 
	 * Sort the specified file using the specified {@link Comparator} and
	 * optionally sort uniquely.
	 * 
	 * @param filename
	 *            the file to sort
	 * @param fileout
	 *            the destination sorted file
	 * @param comp
	 *            the {@link Comparator}
	 * @throws IOException
	 *             if failed to sort
	 */

	public static void sort(String filename, String fileout,
			Comparator<String> comp) throws IOException {

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

				sortHelper(lines.toArray(new String[0]), files1,
						comp, sortdir);

				size_so_far = 0;
			}
		}

		//
		// If there are left-over lines, create final tmp file
		//
		if (lines != null && lines.size() != 0 && size_so_far <= segment_size) {
			sortHelper(lines.toArray(new String[0]), files1, comp,
					sortdir);
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

		while (files1.size() > 1) {

			for (int i = 0; i < files1.size(); i += 2) {

				tmp += 2;

				if (files1.size() == i + 1) {
					files2.add(files1.get(i));
					break;
				} else {
					final File f = mergeSortedFiles(files1.get(i),
							files1.get(i + 1), comp, sortdir,
							header_line);

					files2.add(f);

					files1.get(i).delete();
					files1.get(i + 1).delete();
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
	 * @param lines
	 *            the lines to sort
	 * @param all_tmp_files
	 *            the list of files
	 * @param comp
	 *            the comparator
	 * @param sortdir
	 *            the sort dir
	 * @param unique
	 *            whether or not to sort unique
	 * @param bom_present
	 *            indicates if a Byte Order Mark was present on file
	 * @throws IOException
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
	 * @return the sorted {@link File}
	 * @param files1
	 *            the first set of files
	 * @param files2
	 *            the second set of files
	 * @param comp
	 *            the comparator
	 * @param dir
	 *            the sort dir
	 * @param unique
	 *            whether or not to sort unique
	 * @param bom_present
	 *            indicates if a Byte Order Mark was present on file
	 * @throws IOException
	 */
	private static File mergeSortedFiles(File files1, File files2,
			Comparator<String> comp, File dir, String header_line)
			throws IOException {
		final BufferedReader in1 = new BufferedReader(new FileReader(files1));

		final BufferedReader in2 = new BufferedReader(new FileReader(files2));

		final File out_file = File.createTempFile("t+~", ".tmp", dir);

		final BufferedWriter out = new BufferedWriter(new FileWriter(out_file));

		String line1 = in1.readLine();
		String line2 = in2.readLine();
		String line = null;

		if (!header_line.isEmpty()) {
			line = header_line;
			out.write(line);
			out.newLine();
		}

		while (line1 != null || line2 != null) {

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

			out.write(line);
			out.newLine();

		}

		out.flush();
		out.close();
		in1.close();
		in2.close();

		return out_file;

	}

	/**
	 * Sorting tool: Currently unused
	 * 
	 * @param file_in
	 *            the input file
	 * @param column
	 *            (unused)
	 * @return file_in the input file
	 */
	@SuppressWarnings("static-method")
	public String sort_entries(String file_in, int column) {

		return file_in;
	}

	/**
	 * Load concepts.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadConcepts() throws Exception {

		String line = "";
		i = 0;

		while ((line = concepts_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			Concept concept = new ConceptJpa();

			if (!fields[0].equals("id")) { // header
				concept.setTerminologyId(fields[0]);
				concept.setEffectiveTime(dt.parse(fields[1]));
				concept.setActive(fields[2].equals("1") ? true : false);
				concept.setModuleId(Long.valueOf(fields[3].trim()));
				concept.setDefinitionStatusId(Long.valueOf(fields[4].trim()));
				concept.setTerminology(terminology);
				concept.setTerminologyVersion(version);
				concept.setDefaultPreferredName("null");

				manager.persist(concept);

				conceptCache.put(
						new String(fields[0] + concept.getTerminology()
								+ concept.getTerminologyVersion()), concept);

				// 50, same as JDBC batch size
				if (++i % 50 == 0) {
					manager.flush();
					manager.clear();
				}
			}
		}
	}

	/**
	 * Load relationships.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadRelationships() throws Exception {

		String line = "";
		i = 0;

		while ((line = relationships_by_source_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			Relationship relationship = new RelationshipJpa();

			if (!fields[0].equals("id")) { // header
				relationship.setTerminologyId(fields[0]);
				relationship.setEffectiveTime(dt.parse(fields[1]));
				relationship.setActive(fields[2].equals("1") ? true : false); // active
				relationship.setModuleId(Long.valueOf(fields[3].trim())); // moduleId

				relationship.setRelationshipGroup(Integer.valueOf(fields[6].trim())); // relationshipGroup
				relationship.setTypeId(Long.valueOf(fields[7].trim())); // typeId
				relationship.setCharacteristicTypeId(Long.valueOf(fields[8].trim())); // characteristicTypeId
				relationship.setTerminology(terminology);
				relationship.setTerminologyVersion(version);
				relationship
						.setModifierId(Long.valueOf(fields[9].trim()));

				Concept sourceConcept = getConcept(fields[4],
						relationship.getTerminology(),
						relationship.getTerminologyVersion());
				Concept destinationConcept = getConcept(fields[5],
						relationship.getTerminology(),
						relationship.getTerminologyVersion());
				

				if (sourceConcept != null && destinationConcept != null) {
					relationship.setSourceConcept(sourceConcept);
					relationship.setDestinationConcept(destinationConcept);
					
					manager.persist(relationship);
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					if (sourceConcept == null) {
						log_file_out.write("Relationship " + relationship.getTerminologyId() +
								" references non-existent source concept " + fields[4]);
						log_file_out.newLine();
					}
					if (destinationConcept == null) {
						log_file_out.write("Relationship " + relationship.getTerminologyId() + 
								" references non-existent destination concept " + fields[5]);
						log_file_out.newLine();
					}
					
				}
			}
		}
	}

	/**
	 * Load descriptions.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadDescriptions() throws Exception {

		String line = "";
		i = 0;

		// keep concepts from extension descriptions
		while ((line = descriptions_by_description.readLine()) != null) {

			String fields[] = line.split("\t");
			Description description = new DescriptionJpa();

			if (!fields[0].equals("id")) { // header

				description.setTerminologyId(fields[0]);
				description.setEffectiveTime(dt.parse(fields[1]));
				description.setActive(fields[2].equals("1") ? true : false);
				description.setModuleId(Long.valueOf(fields[3].trim()));

				description.setLanguageCode(fields[5]);
				description.setTypeId(Long.valueOf(fields[6].trim()));
				description.setTerm(fields[7]);
				description.setCaseSignificanceId(Long.valueOf(fields[8].trim()));
				description.setTerminology(terminology);
				description.setTerminologyVersion(version);

				Concept concept = getConcept(fields[4],
						description.getTerminology(),
						description.getTerminologyVersion());

				if (concept != null) {
					description.setConcept(concept);
					manager.persist(description);
	
					// add to index
					descriptionCache.put(fields[0] + description.getTerminology()
							+ description.getTerminologyVersion(),
							description.getId());
					
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("Description " + description.getTerminologyId() +
							" references non-existent concept " + fields[4]);
					log_file_out.newLine();
				}
			}
		}
	}

	/**
	 * Load AttributeRefSets (Content)
	 * 
	 * @throws Exception
	 *             the exception
	 */

	private void loadAttributeValueRefSets() throws Exception {

		String line = "";
		i = 0;

		while ((line = attribute_refsets_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			AttributeValueRefSetMember attributeValueRefSetMember = new AttributeValueRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				attributeValueRefSetMember.setTerminologyId(fields[0]);
				attributeValueRefSetMember
						.setEffectiveTime(dt.parse(fields[1]));
				attributeValueRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				attributeValueRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				attributeValueRefSetMember.setRefSetId(fields[4]);

				// AttributeValueRefSetMember unique attributes
				attributeValueRefSetMember.setValueId(Long.valueOf(fields[6].trim()));

				// Terminology attributes
				attributeValueRefSetMember.setTerminology(terminology);
				attributeValueRefSetMember.setTerminologyVersion(version);

				// Retrieve concept -- firstToken is referencedComponentId
				Concept concept = getConcept(fields[5],
						attributeValueRefSetMember.getTerminology(),
						attributeValueRefSetMember.getTerminologyVersion());
				
				if (concept != null) {
				
					attributeValueRefSetMember.setConcept(concept);
					manager.persist(attributeValueRefSetMember);
					
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("attributeValueRefSetMember " + attributeValueRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
					log_file_out.newLine();
				}
			}
		}
	}

	/**
	 * Load SimpleRefSets (Content)
	 * 
	 * @throws Exception
	 *             the exception
	 */

	private void loadSimpleRefSets() throws Exception {

		String line = "";
		i = 0;

		while ((line = simple_refsets_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			SimpleRefSetMember simpleRefSetMember = new SimpleRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				simpleRefSetMember.setTerminologyId(fields[0]);
				simpleRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				simpleRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				simpleRefSetMember.setRefSetId(fields[4]);

				// SimpleRefSetMember unique attributes
				// NONE

				// Terminology attributes
				simpleRefSetMember.setTerminology(terminology);
				simpleRefSetMember.setTerminologyVersion(version);

				// Retrieve Concept -- firstToken is referencedComonentId
				Concept concept = getConcept(fields[5],
						simpleRefSetMember.getTerminology(),
						simpleRefSetMember.getTerminologyVersion());
				
				if (concept != null) {
					simpleRefSetMember.setConcept(concept);
					manager.persist(simpleRefSetMember);
					
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("simpleRefSetMember " + simpleRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
					log_file_out.newLine();
				}
			}
		}
	}

	/**
	 * Load SimpleMapRefSets (Crossmap)
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadSimpleMapRefSets() throws Exception {

		String line = "";
		i = 0;

		while ((line = simple_map_refsets_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			SimpleMapRefSetMember simpleMapRefSetMember = new SimpleMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				simpleMapRefSetMember.setTerminologyId(fields[0]);
				simpleMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleMapRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				simpleMapRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				simpleMapRefSetMember.setRefSetId(fields[4]);

				// SimpleMap unique attributes
				simpleMapRefSetMember.setMapTarget(fields[6]);

				// Terminology attributes
				simpleMapRefSetMember.setTerminology(terminology);
				simpleMapRefSetMember.setTerminologyVersion(version);

				// Retrieve concept -- firstToken is referencedComponentId
				Concept concept = getConcept(fields[5],
						simpleMapRefSetMember.getTerminology(),
						simpleMapRefSetMember.getTerminologyVersion());
				
				if (concept != null) {
					simpleMapRefSetMember.setConcept(concept);
					manager.persist(simpleMapRefSetMember);
					
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("simpleMapRefSetMember " + simpleMapRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
					log_file_out.newLine();
				}
			}
		}
	}

	/**
	 * Load ComplexMapRefSets (Crossmap)
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadComplexMapRefSets() throws Exception {

		String line = "";
		i = 0;

		while ((line = complex_map_refsets_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6].trim()));
				complexMapRefSetMember.setMapPriority(Integer
						.parseInt(fields[7].trim()));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long
						.valueOf(fields[11].trim()));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(terminology);
				complexMapRefSetMember.setTerminologyVersion(version);

				// set Concept
				Concept concept = getConcept(fields[5],
						complexMapRefSetMember.getTerminology(),
						complexMapRefSetMember.getTerminologyVersion());
				
				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					manager.persist(complexMapRefSetMember);

					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("complexMapRefSetMember " + complexMapRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
					log_file_out.newLine();
				}

			}
		}

	}

	/**
	 * Load ExtendedMapRefSets (Crossmap)
	 * 
	 * @throws Exception
	 *             the exception
	 */

	// NOTE: ExtendedMap RefSets are loaded into ComplexMapRefSetMember
	// where mapRelationId = mapCategoryId
	private void loadExtendedMapRefSets() throws Exception {

		String line = "";
		i = 0;

		while ((line = extended_map_refsets_by_concept.readLine()) != null) {

			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6].trim()));
				complexMapRefSetMember.setMapPriority(Integer
						.parseInt(fields[7].trim()));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long
						.valueOf(fields[12].trim()));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(terminology);
				complexMapRefSetMember.setTerminologyVersion(version);

				// set Concept
				Concept concept = getConcept(fields[5],
						complexMapRefSetMember.getTerminology(),
						complexMapRefSetMember.getTerminologyVersion());
				
				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					manager.persist(complexMapRefSetMember);

					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
				} else {
					log_file_out.write("complexMapRefSetMember " + complexMapRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
					log_file_out.newLine();
				}

			}
		}

	}

	private void loadLanguageRefSets() throws Exception {

		String line = "";
		i = 0;
		Concept concept;
		Description description;

		while ((line = language_refsets_by_description.readLine()) != null) {

			String fields[] = line.split("\t");
			LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				// Universal RefSet attributes
				languageRefSetMember.setTerminologyId(fields[0]);
				languageRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				languageRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				languageRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				languageRefSetMember.setRefSetId(fields[4]);

				// Language unique attributes
				languageRefSetMember
						.setAcceptabilityId(Long.valueOf(fields[6].trim()));

				// Terminology attributes
				languageRefSetMember.setTerminology(terminology);
				languageRefSetMember.setTerminologyVersion(version);

				// Set the description
				description = getDescription(fields[5],
						languageRefSetMember.getTerminology(),
						languageRefSetMember.getTerminologyVersion());
				
				if (description != null) {
					languageRefSetMember.setDescription(description);
					manager.persist(languageRefSetMember);
					
					// check if this language refset and description form the
					// defaultPreferredName
					if (description.getTypeId().equals(dpnTypeId)
							&& new Long(languageRefSetMember.getRefSetId()).equals(
									dpnRefSetId)
							&& languageRefSetMember.getAcceptabilityId().equals(
									dpnAcceptabilityId)) {
	
						concept = description.getConcept();

						if (!concept.getDefaultPreferredName().equals("null")) {
							getLog().info(
									"Multiple default preferred names for concept "
											+ concept.getTerminologyId());
							log_file_out.write("Multiple default preferred names for concept " + concept.getTerminologyId() + "\n" +
												"     " + concept.getDefaultPreferredName() + "\n" +
												"     "	+ description.getTerm());
						}
	
						concept.setDefaultPreferredName(description.getTerm());
			
						manager.persist(concept);
						
						
					}
					
					// 50, same as JDBC batch size
					if (++i % 50 == 0) {
						manager.flush();
						manager.clear();
					}
					
					
				} else {
					log_file_out.write("languageRefSetMember " + languageRefSetMember.getTerminologyId() +
							" references non-existent description " + fields[5]);
					log_file_out.newLine();
				}
			}
		}
	}
}
