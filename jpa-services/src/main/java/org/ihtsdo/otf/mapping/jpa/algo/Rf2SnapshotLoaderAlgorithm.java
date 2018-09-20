package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
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
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;
import org.ihtsdo.otf.mapping.services.helpers.ProgressEvent;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

import com.google.common.io.Files;

public class Rf2SnapshotLoaderAlgorithm extends RootServiceJpa
		implements Algorithm, AutoCloseable {

	/** Listeners. */
	private List<ProgressListener> listeners = new ArrayList<>();

	/** The request cancel flag. */
	boolean requestCancel = false;
	
	/** Name of terminology to be loaded. */
	private String terminology;

	/** Terminology version */
	private String version;
	
	/** The input file */
	private String inputDir;
	
	/** Calculate tree positions */
	private Boolean treePositions;
	
	/** Send notifications if true */
	private Boolean sendNotification;

	/** the defaultPreferredNames type id. */
	private Long dpnTypeId = 900000000000003001L;

	/** The dpn ref set id. */
	private Long dpnrefsetId = 900000000000509007L;

	/** The dpn acceptability id. */
	private Long dpnAcceptabilityId = 900000000000548007L;

	/** The date format. */
	private final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");

	/** The concepts by concept. */
	private BufferedReader conceptsByConcept;

	/** The descriptions by description. */
	private BufferedReader descriptionsByDescription;

	/** The relationships by source concept. */
	private BufferedReader relationshipsBySourceConcept;

	/** The language refsets by description. */
	private BufferedReader languageRefsetsByDescription;

	/** The attribute refsets by description. */
	private BufferedReader attributeRefsetsByDescription;

	/** The simple refsets by concept. */
	private BufferedReader simpleRefsetsByConcept;

	/** The simple map refsets by concept. */
	private BufferedReader simpleMapRefsetsByConcept;

	/** The complex map refsets by concept. */
	// private BufferedReader complexMapRefsetsByConcept;

	/** The extended map refsets by concept. */
	private BufferedReader extendedMapRefsetsByConcept;

	/** hash sets for retrieving concepts. */
	private Map<String, Concept> conceptCache = new HashMap<>(); // used to

	/** hash set for storing default preferred names. */
	Map<Long, String> defaultPreferredNames = new HashMap<>();

	private final String elapsedTime = "    elapsed time = %,d (s) (Ended at %s )";

	/** counter for objects created, reset in each load section. */
	int objectCt; //

	/** the number of objects to create before committing. */
	int commitCt = 1000;
		   
    /** The log. */
    private static Logger log;
    
    /** The log file. */
    private File logFile;

	/**
	 * Instantiates an empty {@link Rf2SnapshotLoaderAlgorithm}.
	 * 
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Rf2SnapshotLoaderAlgorithm(String terminology, String version, String inputDir,
			Boolean treePositions, Boolean sendNotification) throws Exception {
		super();

		//spaces and trim spaces
		this.terminology = removeSpaces(terminology);
		this.version = removeSpaces(version);
		this.inputDir = inputDir;
		this.treePositions = treePositions;
		this.sendNotification = sendNotification;
		
        //initialize logger
        String rootPath = ConfigUtility.getConfigProperties()
              .getProperty("map.principle.source.document.dir");
        if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
          rootPath += "/";
        }
        rootPath += "logs";
        File logDirectory = new File(rootPath);
        if (!logDirectory.exists()) {
            logDirectory.mkdir();
        }
        
        logFile = new File(logDirectory, "load_" + terminology + ".log");
        LoggerUtility.setConfiguration("load", logFile.getAbsolutePath());
        this.log = LoggerUtility.getLogger("load");
	}

	/**
	 * 
	 * 
	 * @param terminology
	 * @param version
	 * @param inputDir
	 * @param sendNotification
	 * 
	 */
	public void compute() throws Exception {

	  // clear log before starting process
      PrintWriter writer = new PrintWriter(logFile);
      writer.print("");
      writer.close(); 
      
      log.info("Starting Rf2 Snapshot Load of " + terminology);
      
		Properties config = ConfigUtility.getConfigProperties();

		// check that notifications can be sent if requested
		String notificationRecipients = config
				.getProperty("send.notification.recipients.devops");
		if (!sendNotification) {
			log.info(
					"No notifications will be sent as a result of workflow computation.");
		}
		if (sendNotification
				&& config.getProperty("send.notification.recipients.devops") == null) {
			throw new Exception(
					"Email notification was requested, but no recipients were specified.");
		} else {
			log
					.info("Request to send notification email for any errors to recipients: "
							+ notificationRecipients);
		}

		// Set the input directory
		File coreInputDir = new File(inputDir);
		if (!coreInputDir.exists()) {
			throw new Exception("Specified input dir missing");
		}

		// set the parameters for determining defaultPreferredNames
		String prop = config.getProperty("loader.defaultPreferredNames.typeId");
		if (prop != null) {
			dpnTypeId = Long.valueOf(prop);
		}

		prop = config.getProperty("loader.defaultPreferredNames.refsetId");
		if (prop != null) {
			dpnrefsetId = Long.valueOf(prop);
		}
		prop = config
				.getProperty("loader.defaultPreferredNames.acceptabilityId");
		if (prop != null) {
			dpnAcceptabilityId = Long.valueOf(prop);
		}

		//
		// Determine version
		//

		log.info("  terminology = " + terminology);
		log.info("  version = " + version);

		// output relevant properties/settings to console
		log.info("  Default preferred name settings:");
		log.info("    typeId = " + dpnTypeId);
		log.info("    refsetId = " + dpnrefsetId);
		log
				.info("    acceptabilityId = " + dpnAcceptabilityId);
		log.info("  Objects committed in blocks of "
				+ Integer.toString(commitCt));

		// Log memory usage
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());

		SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a"); // format for

		try {

			// Prepare sorted input files
			File sortedFileDir = new File(coreInputDir, "/RF2-sorted-temp/");

			log.info("  Sorting input files...");
			long startTime = System.nanoTime();
			sortRf2Files(coreInputDir, sortedFileDir);
			log.info(
					"      complete in " + getElapsedTime(startTime) + "s");

			// Open readers
			openSortedFileReaders(sortedFileDir);

			// load Concepts
			if (conceptsByConcept != null) {
				log.info("  Loading Concepts...");
				startTime = System.nanoTime();
				loadConcepts(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// load Descriptions and Language Ref Set Members
			if (descriptionsByDescription != null
					&& languageRefsetsByDescription != null) {
				log
						.info("  Loading Descriptions and LanguageRefSets...");
				startTime = System.nanoTime();
				loadDescriptionsAndLanguageRefSets(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));

				// Set default preferred names
				log.info(
						"  Setting default preferred names for all concepts...");
				startTime = System.nanoTime();
				setDefaultPreferredNames();
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));

			}

			// Load Relationships
			if (relationshipsBySourceConcept != null) {
				log.info("  Loading Relationships...");
				startTime = System.nanoTime();
				loadRelationships(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// Load Simple RefSets (Content)
			if (simpleRefsetsByConcept != null) {
				log
						.info("  Loading Simple RefSets...");
				startTime = System.nanoTime();
				loadSimpleRefSets(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// Load SimpleMapRefSets
			if (simpleMapRefsetsByConcept != null) {
				log
						.info("  Loading SimpleMap RefSets...");
				startTime = System.nanoTime();
				loadSimpleMapRefSets(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// Load ComplexMapRefSets
			// if (complexMapRefsetsByConcept != null) {
			// log.info(" Loading ComplexMap
			// RefSets...");
			// startTime = System.nanoTime();
			// loadComplexMapRefSets();
			// log.info(" elapsed time = " +
			// getElapsedTime(startTime) +
			// "s"
			// + " (Ended at " + ft.format(new Date()) + ")");
			// }

			// Load ExtendedMapRefSets
			if (extendedMapRefsetsByConcept != null) {
				log
						.info("  Loading ExtendedMap RefSets...");
				startTime = System.nanoTime();
				loadExtendedMapRefSets(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// Load AttributeValue RefSets (Content)
			if (attributeRefsetsByDescription != null) {
				log
						.info("  Loading AttributeValue RefSets...");
				startTime = System.nanoTime();
				loadAttributeValueRefSets(terminology, version);
				log.info(String.format(elapsedTime,
						getElapsedTime(startTime), ft.format(new Date())));
			}

			// Clear concept cache
			conceptCache.clear();

			// Close files/readers
			closeAllSortedFiles();

			if (treePositions) {
				// Create tree positions
				final MetadataService metadataService = new MetadataServiceJpa();
				Map<String, String> hierRelTypeMap = metadataService
						.getHierarchicalRelationshipTypes(terminology, version);
				String isaRelType = hierRelTypeMap.keySet().iterator().next()
						.toString();
				log.info("  Start creating tree positions.");
				metadataService.close();

				final ContentService contentService = new ContentServiceJpa();

				// Walk up tree to the root
				// ASSUMPTION: single root
				String conceptId = isaRelType;
				String rootId = null;
				OUTER: while (true) {
					log.info("    Walk up tree from " + conceptId);
					Concept c = contentService.getConcept(conceptId,
							terminology, version);
					for (Relationship r : c.getRelationships()) {
						if (r.isActive() && r.getTypeId()
								.equals(Long.valueOf(isaRelType))) {
							conceptId = r.getDestinationConcept()
									.getTerminologyId();
							continue OUTER;
						}
					}
					rootId = conceptId;
					break;
				}
				log.info("    Compute tree from rootId " + conceptId);
				ValidationResult result = contentService.computeTreePositions(
						terminology, version, isaRelType, rootId);
				if (sendNotification && !result.isValid()) {
					ConfigUtility.sendValidationResultEmail(
							config.getProperty("send.notification.recipients.devops"),
							"OTF-Mapping-Tool:  Errors in computing "
									+ terminology + ", " + version
									+ " hierarchical tree positions",
							"Hello,\n\nErrors were detected when computing hierarchical tree positions for "
									+ terminology + ", " + version,
							result);
				}

				// Close service
				contentService.close();

			}
			
			log.info("Done Rf2 Snapshot Load of " + terminology);

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			LoggerUtility.removeLogger("load");
		}
		
		

	}

	/**
	 * Opens sorted data files.
	 *
	 * @param outputDir
	 *            the output dir
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void openSortedFileReaders(File outputDir) throws IOException {
		File conceptsByConceptsFile = new File(outputDir,
				"concepts_by_concept.sort");
		File descriptionsByDescriptionFile = new File(outputDir,
				"descriptions_by_description.sort");
		File relationshipsBySourceConceptFile = new File(outputDir,
				"relationship_by_source_concept.sort");
		File languageRefsetsByDescriptionsFile = new File(outputDir,
				"language_refsets_by_description.sort");
		File attributeRefsetsByConceptFile = new File(outputDir,
				"attribute_refsets_by_concept.sort");
		File simpleRefsetsByConceptFile = new File(outputDir,
				"simple_refsets_by_concept.sort");
		File simpleMapRefsetsByConceptFile = new File(outputDir,
				"simple_map_refsets_by_concept.sort");
		// File complexMapRefsetsByConceptFile =
		// new File(outputDir, "complex_map_refsets_by_concept.sort");
		File extendedMapRefsetsByConceptsFile = new File(outputDir,
				"extended_map_refsets_by_concept.sort");

		// Concept reader
		if (conceptsByConceptsFile != null) {
			conceptsByConcept = new BufferedReader(
					new FileReader(conceptsByConceptsFile));
		}
		// Relationships by source concept reader
		if (relationshipsBySourceConceptFile != null) {
			relationshipsBySourceConcept = new BufferedReader(
					new FileReader(relationshipsBySourceConceptFile));
		}
		// Descriptions by description id reader
		if (descriptionsByDescriptionFile != null) {
			descriptionsByDescription = new BufferedReader(
					new FileReader(descriptionsByDescriptionFile));
		}
		// Language RefSets by description id
		if (languageRefsetsByDescriptionsFile != null) {
			languageRefsetsByDescription = new BufferedReader(
					new FileReader(languageRefsetsByDescriptionsFile));
		}

		// Attribute Value reader
		// NOTE: Discrepancy in naming here, but the sorted by concepts file is
		// the one desired
		// the description file stream is the one used.
		if (attributeRefsetsByConceptFile != null) {
			attributeRefsetsByDescription = new BufferedReader(
					new FileReader(attributeRefsetsByConceptFile));
		}
		// Simple reader
		if (simpleRefsetsByConceptFile != null) {
			simpleRefsetsByConcept = new BufferedReader(
					new FileReader(simpleRefsetsByConceptFile));
		}
		// Simple Map reader
		if (simpleMapRefsetsByConceptFile != null) {
			simpleMapRefsetsByConcept = new BufferedReader(
					new FileReader(simpleMapRefsetsByConceptFile));
		}
		// Complex map reader
		// if (complexMapRefsetsByConceptFile != null) {
		// complexMapRefsetsByConcept =
		// new BufferedReader(new FileReader(complexMapRefsetsByConceptFile));
		// }
		// Extended map reader
		if (extendedMapRefsetsByConceptsFile != null) {
			extendedMapRefsetsByConcept = new BufferedReader(
					new FileReader(extendedMapRefsetsByConceptsFile));
		}
	}

	/**
	 * Returns the elapsed time.
	 *
	 * @param time
	 *            the time
	 * @return the elapsed time
	 */
	@SuppressWarnings("boxing")
	private long getElapsedTime(long time) {
		return (System.nanoTime() - time) / 1000000000;
	}


	/**
	 * Returns the last modified.
	 * 
	 * @param directory
	 *            the directory
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
	 * Sorts all files by concept or referencedComponentId.
	 *
	 * @param coreInputDir
	 *            the core input dir
	 * @param outputDir
	 *            the output dir
	 * @throws Exception
	 *             the exception
	 */
	private void sortRf2Files(File coreInputDir, File outputDir)
			throws Exception {

		// Check expectations and pre-conditions
		if (!outputDir.exists()
				|| getLastModified(outputDir) < getLastModified(coreInputDir)) {

			// log reason for sort
			if (!outputDir.exists()) {
				log.info(
						"     No sorted files exist -- sorting RF2 files");
			} else if (getLastModified(outputDir) < getLastModified(
					coreInputDir)) {
				log.info(
						"     Sorted files older than input files -- sorting RF2 files");
			}

			// delete any existing temporary files
			FileSorter.deleteSortedFiles(outputDir);

			// test whether file/folder still exists (i.e. delete error)
			if (outputDir.exists()) {
				throw new Exception(
						"Could not delete existing sorted files folder "
								+ outputDir.toString());
			}

			// attempt to make sorted files directory
			if (outputDir.mkdir()) {
				log
						.info("  Creating new sorted files folder "
								+ outputDir.toString());
			} else {
				throw new Exception(
						"Could not create temporary sorted file folder "
								+ outputDir.toString());
			}

		} else {
			log.info(
					"    Sorted files exist and are up to date.  No sorting required");
			return;
		}

		//
		// Setup files
		//
		File coreRelInputFile = null;
		File coreConceptInputFile = null;
		File coreDescriptionInputFile = null;
		File coreSimpleRefsetInputFile = null;
		File coreAssociationReferenceInputFile = null;
		File coreAttributeValueInputFile = null;
		// File coreComplexMapInputFile = null;
		File coreExtendedMapInputFile = null;
		File coreSimpleMapInputFile = null;
		File coreLanguageInputFile = null;
		File coreIdentifierInputFile = null;
		File coreTextDefinitionInputFile = null;

		// Termionlogy dir
		File coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
		log
				.info("    Terminology dir = "
						+ coreTerminologyInputDir.toString() + " "
						+ coreTerminologyInputDir.exists());

		// Relationships file
		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Relationship_")) {
				if (coreRelInputFile != null)
					throw new Exception("Multiple Relationships Files!");
				coreRelInputFile = f;
			}
		}
		log
				.info("      Relationships file = "
						+ coreRelInputFile.toString() + " "
						+ coreRelInputFile.exists());

		// Concepts file
		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Concept_")) {
				if (coreConceptInputFile != null)
					throw new Exception("Multiple Concept Files!");
				coreConceptInputFile = f;
			}
		}
		log
				.info("      Concepts file = " + coreConceptInputFile.toString()
						+ " " + coreConceptInputFile.exists());

		// Descriptions file
		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Description_")) {
				if (coreDescriptionInputFile != null)
					throw new Exception("Multiple Description Files!");
				coreDescriptionInputFile = f;
			}
		}
		log
				.info("      Descriptions file = "
						+ coreDescriptionInputFile.toString() + " "
						+ coreDescriptionInputFile.exists());

		// Identifier file
		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Identifier_")) {
				if (coreIdentifierInputFile != null)
					throw new Exception("Multiple Identifier Files!");
				coreIdentifierInputFile = f;
			}
		}
		if (coreIdentifierInputFile != null) {
			log
					.info("      Identifiers file = "
							+ coreIdentifierInputFile.toString() + " "
							+ coreIdentifierInputFile.exists());
		}

		// Text definition file
		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_TextDefinition_")) {
				if (coreTextDefinitionInputFile != null)
					throw new Exception("Multiple TextDefinition Files!");
				coreTextDefinitionInputFile = f;
			}
		}
		if (coreTextDefinitionInputFile != null) {
			log
					.info("      Text definitions file = "
							+ coreTextDefinitionInputFile.toString() + " "
							+ coreTextDefinitionInputFile.exists());
		}

		// Refset/Content dir
		File coreRefsetInputDir = new File(coreInputDir, "/Refset/");
		File coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
		log
				.info("    Refset/Content dir = "
						+ coreContentInputDir.toString() + " "
						+ coreContentInputDir.exists());

		// Simple refset file
		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Simple")) {
				if (coreSimpleRefsetInputFile != null)
					throw new Exception("Multiple Simple Refset Files!");
				coreSimpleRefsetInputFile = f;
			}
		}
		log
				.info("      Simple refset file = "
						+ coreSimpleRefsetInputFile.toString() + " "
						+ coreSimpleRefsetInputFile.exists());

		// Association reference file
		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Association")) {
				if (coreAssociationReferenceInputFile != null)
					throw new Exception(
							"Multiple Association Files!");
				coreAssociationReferenceInputFile = f;
			}
		}
		log
				.info("      Association file = "
						+ coreAssociationReferenceInputFile.toString() + " "
						+ coreAssociationReferenceInputFile.exists());

		// Attribute value file
		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("AttributeValue")) {
				if (coreAttributeValueInputFile != null)
					throw new Exception("Multiple Attribute Value Files!");
				coreAttributeValueInputFile = f;
			}
		}
		log
				.info("      Attribute value file = "
						+ coreAttributeValueInputFile.toString() + " "
						+ coreAttributeValueInputFile.exists());

		// Refset/Map dir
		File coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
		log
				.info("    Refset/Map dir = " + coreCrossmapInputDir.toString()
						+ " " + coreCrossmapInputDir.exists());

		// Complex map file
		// for (File f : coreCrossmapInputDir.listFiles()) {
		// if (f.getName().contains("ComplexMap")) {
		// if (coreComplexMapInputFile != null)
		// throw new Exception("Multiple Complex Map Files!");
		// coreComplexMapInputFile = f;
		// }
		// }
		// if (coreComplexMapInputFile != null) {
		// log.info(
		// " Complex map file = " + coreComplexMapInputFile.toString()
		// + " " + coreComplexMapInputFile.exists());
		// }

		// Extended map file
		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("ExtendedMap")) {
				if (coreExtendedMapInputFile != null)
					throw new Exception("Multiple Extended Map Files!");
				coreExtendedMapInputFile = f;
			}
		}
		if (coreExtendedMapInputFile != null) {
			log
					.info("      Extended map file = "
							+ coreExtendedMapInputFile.toString() + " "
							+ coreExtendedMapInputFile.exists());
		}

		// Simple map file
		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("SimpleMap")) {
				if (coreSimpleMapInputFile != null)
					throw new Exception("Multiple Simple Map Files!");
				coreSimpleMapInputFile = f;
			}
		}
		if (coreSimpleMapInputFile != null) {
			log
					.info("      Simple map file = "
							+ coreSimpleMapInputFile.toString() + " "
							+ coreSimpleMapInputFile.exists());
		}

		// Refset/Langauge dir
		File coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
		log
				.info("    Refset/Language dir = "
						+ coreLanguageInputDir.toString() + " "
						+ coreLanguageInputDir.exists());

		// Language file
		for (File f : coreLanguageInputDir.listFiles()) {
			if (f.getName().contains("Language")) {
				if (coreLanguageInputFile != null)
					throw new Exception("Multiple Language Files!");
				coreLanguageInputFile = f;
			}
		}
		log
				.info("      Language file = "
						+ coreLanguageInputFile.toString() + " "
						+ coreLanguageInputFile.exists());

		// Refset/Metadata dir
		File coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
		log
				.info("    Refset/Metadata dir = "
						+ coreMetadataInputDir.toString() + " "
						+ coreMetadataInputDir.exists());

		// Initialize files
		File conceptsByConceptFile = new File(outputDir,
				"concepts_by_concept.sort");
		File descriptionsByDescriptionFile = new File(outputDir,
				"descriptions_by_description.sort");
		File descriptionsCoreByDescriptionFile = new File(outputDir,
				"descriptions_core_by_description.sort");
		File descriptionsTextByDescriptionFile = new File(outputDir,
				"descriptions_text_by_description.sort");
		File relationshipsBySourceConceptFile = new File(outputDir,
				"relationship_by_source_concept.sort");
		File languageRefsetsByDescriptionFile = new File(outputDir,
				"language_refsets_by_description.sort");
		File attributeRefsetsByConceptFile = new File(outputDir,
				"attribute_refsets_by_concept.sort");
		File simpleRefsetsByConceptFile = new File(outputDir,
				"simple_refsets_by_concept.sort");
		File simpleMapRefsetsByConceptFile = new File(outputDir,
				"simple_map_refsets_by_concept.sort");
		// File complexMapRefsetsByConceptFile =
		// new File(outputDir, "complex_map_refsets_by_concept.sort");
		File extendedMapRefsetsByConceptsFile = new File(outputDir,
				"extended_map_refsets_by_concept.sort");

		log.info("      Sort files");
		// Sort concept files
		if (coreConceptInputFile != null) {
			sortRf2File(coreConceptInputFile, conceptsByConceptFile, 0);
		}
		// Sort description file
		if (coreDescriptionInputFile != null) {
			sortRf2File(coreDescriptionInputFile,
					descriptionsCoreByDescriptionFile, 0);
		}
		// Sort text definitions file
		if (coreTextDefinitionInputFile != null
				&& coreDescriptionInputFile != null) {

			// sort the text definition file
			sortRf2File(coreTextDefinitionInputFile,
					descriptionsTextByDescriptionFile, 0);

			// merge the two description files
			log
					.info("        Merging description files...");
			File mergedDesc = mergeSortedFiles(
					descriptionsTextByDescriptionFile,
					descriptionsCoreByDescriptionFile,
					new Comparator<String>() {
						@Override
						public int compare(String s1, String s2) {
							String v1[] = s1.split("\t");
							String v2[] = s2.split("\t");
							return v1[0].compareTo(v2[0]);
						}
					}, outputDir, ""); // header line

			// rename the temporary file
			Files.move(mergedDesc, descriptionsByDescriptionFile);

		} else {
			// copy the core descriptions file
			Files.copy(descriptionsCoreByDescriptionFile,
					descriptionsByDescriptionFile);
		}

		// Sort relationships file
		if (coreRelInputFile != null) {
			sortRf2File(coreRelInputFile, relationshipsBySourceConceptFile, 4);
		}
		// Sort attribute value file
		if (coreAttributeValueInputFile != null) {
			sortRf2File(coreAttributeValueInputFile,
					attributeRefsetsByConceptFile, 5);
		}
		// Sort simple file
		if (coreSimpleRefsetInputFile != null) {
			sortRf2File(coreSimpleRefsetInputFile, simpleRefsetsByConceptFile,
					5);
		}
		// Sort simple map file
		if (coreSimpleMapInputFile != null) {
			sortRf2File(coreSimpleMapInputFile, simpleMapRefsetsByConceptFile,
					5);
		}
		// Sort complex map file
		// if (coreComplexMapInputFile != null) {
		// sortRf2File(coreComplexMapInputFile, complexMapRefsetsByConceptFile,
		// 5);
		// }
		// sort extended map file
		if (coreExtendedMapInputFile != null) {
			sortRf2File(coreExtendedMapInputFile,
					extendedMapRefsetsByConceptsFile, 5);
		}
		// Sort language file
		if (coreLanguageInputFile != null) {
			sortRf2File(coreLanguageInputFile, languageRefsetsByDescriptionFile,
					5);
		}
	}

	/**
	 * Helper function for sorting an individual file with column comparator.
	 * 
	 * @param fileIn
	 *            the input file to be sorted
	 * @param fileOut
	 *            the resulting sorted file
	 * @param sortColumn
	 *            the column ([0, 1, ...] to compare by
	 * @throws Exception
	 *             the exception
	 */
	private void sortRf2File(File fileIn, File fileOut, final int sortColumn)
			throws Exception {

		Comparator<String> comp;
		// Comparator to split on \t and sort by sortColumn
		comp = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t");
				return v1[sortColumn].compareTo(v2[sortColumn]);
			}
		};

		log
				.info("        Sorting " + fileIn.toString() + "  into "
						+ fileOut.toString() + " by column "
						+ Integer.toString(sortColumn));
		FileSorter.sortFile(fileIn.toString(), fileOut.toString(), comp);

	}

	/**
	 * Merge-sort two files.
	 * 
	 * @param files1
	 *            the first set of files
	 * @param files2
	 *            the second set of files
	 * @param comp
	 *            the comparator
	 * @param dir
	 *            the sort dir
	 * @param headerLine
	 *            the header_line
	 * @return the sorted {@link File}
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("static-method")
	private File mergeSortedFiles(File files1, File files2,
			Comparator<String> comp, File dir, String headerLine)
			throws IOException {
		final BufferedReader in1 = new BufferedReader(new FileReader(files1));
		final BufferedReader in2 = new BufferedReader(new FileReader(files2));
		final File outFile = File.createTempFile("t+~", ".tmp", dir);
		final BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		String line1 = in1.readLine();
		String line2 = in2.readLine();
		String line = null;
		if (!headerLine.isEmpty()) {
			line = headerLine;
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
		return outFile;
	}

	/**
	 * Closes all sorted temporary files.
	 * 
	 * @throws Exception
	 *             if something goes wrong
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
		// if (complexMapRefsetsByConcept != null) {
		// complexMapRefsetsByConcept.close();
		// }
		if (extendedMapRefsetsByConcept != null) {
			extendedMapRefsetsByConcept.close();
		}
	}

	/**
	 * Load concepts.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadConcepts(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		while ((line = conceptsByConcept.readLine()) != null) {

			final String fields[] = line.split("\t");
			final Concept concept = new ConceptJpa();

			if (!fields[0].equals("id")) { // header

				concept.setTerminologyId(fields[0]);
				concept.setEffectiveTime(dt.parse(fields[1]));
				concept.setActive(fields[2].equals("1") ? true : false);
				concept.setModuleId(Long.valueOf(fields[3]));
				concept.setDefinitionStatusId(Long.valueOf(fields[4]));
				concept.setTerminology(terminology);
				concept.setTerminologyVersion(version);
				concept.setDefaultPreferredName("null");
				contentService.addConcept(concept);

				// copy concept to shed any hibernate stuff
				conceptCache.put(fields[0], concept);

				// regularly commit at intervals
				if (++objectCt % commitCt == 0) {
					log
							.info("    commit = " + objectCt);
					contentService.commit();
					contentService.beginTransaction();
				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		defaultPreferredNames.clear();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());

	}

	/**
	 * Load relationships.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadRelationships(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// Begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		// Iterate over relationships
		while ((line = relationshipsBySourceConcept.readLine()) != null) {

			// Split line
			final String fields[] = line.split("\t");
			// Skip header
			if (!fields[0].equals("id")) {

				// Configure relationship
				final Relationship relationship = new RelationshipJpa();
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

				final Concept sourceConcept = conceptCache.get(fields[4]);
				final Concept destinationConcept = conceptCache.get(fields[5]);
				if (sourceConcept != null && destinationConcept != null) {
					relationship.setSourceConcept(sourceConcept);
					relationship.setDestinationConcept(destinationConcept);

					contentService.addRelationship(relationship);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						log
								.info("    commit = " + objectCt);
						contentService.commit();
						contentService.beginTransaction();
					}
				} else {
					if (sourceConcept == null) {
						log
								.info("Relationship "
										+ relationship.getTerminologyId()
										+ " references non-existent source concept "
										+ fields[4]);
					}
					if (destinationConcept == null) {
						log
								.info("Relationship "
										+ relationship.getTerminologyId()
										+ " references non-existent destination concept "
										+ fields[5]);
					}

				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}

	/**
	 * Load descriptions.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadDescriptionsAndLanguageRefSets(String terminology,
			String version) throws Exception {

		Concept concept;
		Description description;
		LanguageRefSetMember language;
		int descCt = 0; // counter for descriptions
		int langCt = 0; // counter for language ref set members
		int skipCt = 0; // counter for number of language ref set members
						// skipped

		// Begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		// Load and persist first description
		description = getNextDescription(contentService, terminology, version);

		// Load first language ref set member
		language = getNextLanguage(terminology, version);

		// Loop while there are descriptions
		while (!description.getTerminologyId().equals("-1")) {

			// if current language ref set references a lexicographically
			// "lower"
			// String terminologyId, SKIP: description is not in data set
			while (language.getDescription().getTerminologyId()
					.compareTo(description.getTerminologyId()) < 0
					&& !language.getTerminologyId().equals("-1")) {

				log
						.info("     " + "Language Ref Set "
								+ language.getTerminologyId()
								+ " references non-existent description "
								+ language.getDescription().getTerminologyId());
				language = getNextLanguage(terminology, version);
				skipCt++;
			}

			// Iterate over language ref sets until new description id found or
			// end of
			// language ref sets found
			while (language.getDescription().getTerminologyId()
					.equals(description.getTerminologyId())
					&& !language.getTerminologyId().equals("-1")) {

				// Set the description
				language.setDescription(description);
				description.addLanguageRefSetMember(language);
				langCt++;

				// Check if this language refset and description form the
				// defaultPreferredName
				if (description.isActive()
						&& description.getTypeId().equals(dpnTypeId)
						&& new Long(language.getRefSetId()).equals(dpnrefsetId)
						&& language.isActive() && language.getAcceptabilityId()
								.equals(dpnAcceptabilityId)) {

					// retrieve the concept for this description
					concept = description.getConcept();
					if (defaultPreferredNames.get(concept.getId()) != null) {
						log
								.info("Multiple default preferred names for concept "
										+ concept.getTerminologyId());
						log.info("  " + "Existing: "
								+ defaultPreferredNames.get(concept.getId()));
						log.info(
								"  " + "Replaced: " + description.getTerm());
					}
					defaultPreferredNames.put(concept.getId(),
							description.getTerm());

				}

				// Get the next language ref set member
				language = getNextLanguage(terminology, version);
			}

			// Persist the description
			contentService.addDescription(description);

			// Pet the next description
			description = getNextDescription(contentService, terminology,
					version);

			// increment description count
			descCt++;

			// regularly commit at intervals
			if (descCt % commitCt == 0) {
				log.info("    commit = " + descCt);
				contentService.commit();
				contentService.beginTransaction();
			}

		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		log
				.info("      " + descCt + " descriptions loaded");
		log
				.info("      " + langCt + " language ref sets loaded");
		log.info("      " + skipCt
				+ " language ref sets skipped (no description)");

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}

	/**
	 * Sets the default preferred names.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void setDefaultPreferredNames() throws Exception {

		// Begin transaction
		ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		Iterator<Concept> conceptIterator = conceptCache.values().iterator();
		objectCt = 0;
		while (conceptIterator.hasNext()) {
			final Concept cachedConcept = conceptIterator.next();
			final Concept dbConcept = contentService
					.getConcept(cachedConcept.getId());
			dbConcept.getDescriptions();
			dbConcept.getRelationships();
			if (defaultPreferredNames.get(dbConcept.getId()) != null) {
				dbConcept.setDefaultPreferredName(
						defaultPreferredNames.get(dbConcept.getId()));
			} else {
				dbConcept.setDefaultPreferredName(
						"No default preferred name found");
			}
			contentService.updateConcept(dbConcept);
			if (++objectCt % commitCt == 0) {
				log.info("    commit = " + objectCt);
				contentService.commit();
				contentService.beginTransaction();
			}
		}
		contentService.commit();
		contentService.close();

		// Log memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());

	}

	/**
	 * Returns the next description.
	 *
	 * @param contentService
	 *            the content service
	 * @return the next description
	 * @throws Exception
	 *             the exception
	 */
	private Description getNextDescription(ContentService contentService,
			String terminology, String version) throws Exception {

		String line, fields[];

		if ((line = descriptionsByDescription.readLine()) != null) {

			line = line.replace("\r", "");
			fields = line.split("\t");

			if (!fields[0].equals("id")) { // header

				final Description description = new DescriptionJpa();
				description.setTerminologyId("-1");
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
				Concept concept = conceptCache.get(fields[4]);

				if (concept != null) {
					description.setConcept(concept);
				} else {
					log.info("Description "
							+ description.getTerminologyId()
							+ " references non-existent concept " + fields[4]);
				}
				return description;
			}

			// otherwise get next line
			else {
				return getNextDescription(contentService, terminology, version);
			}
		}
		final Description description = new DescriptionJpa();
		description.setTerminologyId("-1");
		return description;

	}

	/**
	 * Utility function to return the next line of language ref set files in
	 * object form.
	 * 
	 * @return a partial language ref set member (lacks full description)
	 * @throws Exception
	 *             the exception
	 */
	private LanguageRefSetMember getNextLanguage(String terminology,
			String version) throws Exception {

		String line, fields[];
		// if non-null
		if ((line = languageRefsetsByDescription.readLine()) != null) {
			line = line.replace("\r", "");
			fields = line.split("\t");

			if (!fields[0].equals("id")) { // header line
				final LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
				languageRefSetMember.setTerminologyId("-1");

				// Universal RefSet attributes
				languageRefSetMember.setTerminologyId(fields[0]);
				languageRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				languageRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				languageRefSetMember.setModuleId(Long.valueOf(fields[3]));
				languageRefSetMember.setRefSetId(fields[4]);

				// Language unique attributes
				languageRefSetMember
						.setAcceptabilityId(Long.valueOf(fields[6]));

				// Terminology attributes
				languageRefSetMember.setTerminology(terminology);
				languageRefSetMember.setTerminologyVersion(version);

				// Set a dummy description with terminology id only
				Description description = new DescriptionJpa();
				description.setTerminologyId(fields[5]);
				languageRefSetMember.setDescription(description);
				return languageRefSetMember;

			}
			// if header line, get next record
			else {
				return getNextLanguage(terminology, version);
			}

			// if null, set a dummy description value to avoid null-pointer
			// exceptions
			// in main loop
		} else {
			final LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
			languageRefSetMember.setTerminologyId("-1");
			final Description description = new DescriptionJpa();
			description.setTerminologyId("-1");
			languageRefSetMember.setDescription(description);
			return languageRefSetMember;
		}

	}

	/**
	 * Load AttributeRefSets (Content).
	 * 
	 * @throws Exception
	 *             the exception
	 */

	@SuppressWarnings("boxing")
	private void loadAttributeValueRefSets(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		while ((line = attributeRefsetsByDescription.readLine()) != null) {

			line = line.replace("\r", "");
			final String fields[] = line.split("\t");
			if (!fields[0].equals("id")) { // header
				final AttributeValueRefSetMember attributeValueRefSetMember = new AttributeValueRefSetMemberJpa();

				// Universal RefSet attributes
				attributeValueRefSetMember.setTerminologyId(fields[0]);
				attributeValueRefSetMember
						.setEffectiveTime(dt.parse(fields[1]));
				attributeValueRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				attributeValueRefSetMember.setModuleId(Long.valueOf(fields[3]));
				attributeValueRefSetMember.setRefSetId(fields[4]);

				// AttributeValueRefSetMember unique attributes
				attributeValueRefSetMember.setValueId(Long.valueOf(fields[6]));

				// Terminology attributes
				attributeValueRefSetMember.setTerminology(terminology);
				attributeValueRefSetMember.setTerminologyVersion(version);

				// Some attribute value things are connected to descriptions
				// for those, for now, just skip
				final Concept concept = conceptCache.get(fields[5]);
				if (concept != null) {

					attributeValueRefSetMember.setConcept(concept);
					contentService.addAttributeValueRefSetMember(
							attributeValueRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						log
								.info("    commit = " + objectCt);
						contentService.commit();
						contentService.beginTransaction();
					}
				} else {
					log
							.debug("attributeValueRefSetMember "
									+ attributeValueRefSetMember
											.getTerminologyId()
									+ " references non-existent concept "
									+ fields[5]);
				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}

	/**
	 * Load SimpleRefSets (Content).
	 * 
	 * @throws Exception
	 *             the exception
	 */

	private void loadSimpleRefSets(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		while ((line = simpleRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			final String fields[] = line.split("\t");

			if (!fields[0].equals("id")) { // header
				final SimpleRefSetMember simpleRefSetMember = new SimpleRefSetMemberJpa();

				// Universal RefSet attributes
				simpleRefSetMember.setTerminologyId(fields[0]);
				simpleRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				simpleRefSetMember.setModuleId(Long.valueOf(fields[3]));
				simpleRefSetMember.setRefSetId(fields[4]);

				// SimpleRefSetMember unique attributes
				// NONE

				// Terminology attributes
				simpleRefSetMember.setTerminology(terminology);
				simpleRefSetMember.setTerminologyVersion(version);

				// Retrieve Concept -- firstToken is referencedComonentId
				final Concept concept = conceptCache.get(fields[5]);

				if (concept != null) {
					simpleRefSetMember.setConcept(concept);
					contentService.addSimpleRefSetMember(simpleRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						log
								.info("    commit = " + objectCt);
						contentService.commit();
						contentService.beginTransaction();
					}
				} else {
					log.info("simpleRefSetMember "
							+ simpleRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}

	/**
	 * Load SimpleMapRefSets (Crossmap).
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void loadSimpleMapRefSets(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		while ((line = simpleMapRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			final String fields[] = line.split("\t");

			if (!fields[0].equals("id")) { // header
				final SimpleMapRefSetMember simpleMapRefSetMember = new SimpleMapRefSetMemberJpa();

				// Universal RefSet attributes
				simpleMapRefSetMember.setTerminologyId(fields[0]);
				simpleMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				simpleMapRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				simpleMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				simpleMapRefSetMember.setRefSetId(fields[4]);

				// SimpleMap unique attributes
				simpleMapRefSetMember.setMapTarget(fields[6]);

				// Terminology attributes
				simpleMapRefSetMember.setTerminology(terminology);
				simpleMapRefSetMember.setTerminologyVersion(version);

				// Retrieve concept -- firstToken is referencedComponentId
				final Concept concept = conceptCache.get(fields[5]);

				if (concept != null) {
					simpleMapRefSetMember.setConcept(concept);
					contentService
							.addSimpleMapRefSetMember(simpleMapRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						log
								.info("    commit = " + objectCt);
						contentService.commit();
						contentService.beginTransaction();
					}
				} else {
					log.info("simpleMapRefSetMember "
							+ simpleMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}

	/**
	 * Load ComplexMapRefSets (Crossmap).
	 * 
	 * @throws Exception the exception
	 */
	/*
	 * private void loadComplexMapRefSets() throws Exception {
	 * 
	 * String line = ""; objectCt = 0;
	 * 
	 * // begin transaction final ContentService contentService = new
	 * ContentServiceJpa(); contentService.setTransactionPerOperation(false);
	 * contentService.beginTransaction();
	 * 
	 * while ((line = complexMapRefsetsByConcept.readLine()) != null) {
	 * 
	 * line = line.replace("\r", ""); final String fields[] = line.split("\t");
	 * 
	 * if (!fields[0].equals("id")) { // header final ComplexMapRefSetMember
	 * complexMapRefSetMember = new ComplexMapRefSetMemberJpa();
	 * 
	 * complexMapRefSetMember.setTerminologyId(fields[0]);
	 * complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
	 * complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
	 * complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
	 * complexMapRefSetMember.setRefSetId(fields[4]); // conceptId
	 * 
	 * // ComplexMap unique attributes
	 * complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
	 * complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
	 * complexMapRefSetMember.setMapRule(fields[8]);
	 * complexMapRefSetMember.setMapAdvice(fields[9]);
	 * complexMapRefSetMember.setMapTarget(fields[10]);
	 * complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[11]));
	 * 
	 * // ComplexMap unique attributes NOT set by file (mapBlock // elements)
	 * complexMapRefSetMember.setMapBlock(0); // default value
	 * complexMapRefSetMember.setMapBlockRule(null); // no default
	 * complexMapRefSetMember.setMapBlockAdvice(null); // no default
	 * 
	 * // Terminology attributes
	 * complexMapRefSetMember.setTerminology(terminology);
	 * complexMapRefSetMember.setTerminologyVersion(version);
	 * 
	 * // set Concept final Concept concept = conceptCache.get(fields[5]);
	 * 
	 * if (concept != null) { complexMapRefSetMember.setConcept(concept);
	 * contentService.addComplexMapRefSetMember(complexMapRefSetMember);
	 * 
	 * // regularly commit at intervals if (++objectCt % commitCt == 0) {
	 * log.info("    commit = " + objectCt);
	 * contentService.commit(); contentService.beginTransaction(); } } else {
	 * log.info("complexMapRefSetMember " +
	 * complexMapRefSetMember.getTerminologyId() +
	 * " references non-existent concept " + fields[5]); }
	 * 
	 * } }
	 * 
	 * // commit any remaining objects contentService.commit();
	 * contentService.close();
	 * 
	 * // print memory information Runtime runtime = Runtime.getRuntime();
	 * log.info("MEMORY USAGE:");
	 * log.info(" Total: " + runtime.totalMemory());
	 * log.info(" Free:  " + runtime.freeMemory());
	 * log.info(" Max:   " + runtime.maxMemory()); }
	 */

	/**
	 * Load ExtendedMapRefSets (Crossmap).
	 * 
	 * @throws Exception
	 *             the exception
	 */

	// NOTE: ExtendedMap RefSets are loaded into ComplexMapRefSetMember
	// where mapRelationId = mapCategoryId
	private void loadExtendedMapRefSets(String terminology, String version)
			throws Exception {

		String line = "";
		objectCt = 0;

		// begin transaction
		final ContentService contentService = new ContentServiceJpa();
		contentService.setTransactionPerOperation(false);
		contentService.beginTransaction();

		while ((line = extendedMapRefsetsByConcept.readLine()) != null) {

			line = line.replace("\r", "");
			final String fields[] = line.split("\t");

			if (!fields[0].equals("id")) { // header
				final ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember
						.setActive(fields[2].equals("1") ? true : false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
				complexMapRefSetMember
						.setMapPriority(Integer.parseInt(fields[7]));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember
						.setMapRelationId(Long.valueOf(fields[12]));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(terminology);
				complexMapRefSetMember.setTerminologyVersion(version);

				// set Concept
				final Concept concept = conceptCache.get(fields[5]);

				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					contentService
							.addComplexMapRefSetMember(complexMapRefSetMember);

					// regularly commit at intervals
					if (++objectCt % commitCt == 0) {
						log
								.info("    commit = " + objectCt);
						contentService.commit();
						contentService.beginTransaction();
					}
				} else {
					log.info("complexMapRefSetMember "
							+ complexMapRefSetMember.getTerminologyId()
							+ " references non-existent concept " + fields[5]);
				}
			}
		}

		// commit any remaining objects
		contentService.commit();
		contentService.close();

		// print memory information
		Runtime runtime = Runtime.getRuntime();
		log.info("MEMORY USAGE:");
		log.info(" Total: " + runtime.totalMemory());
		log.info(" Free:  " + runtime.freeMemory());
		log.info(" Max:   " + runtime.maxMemory());
	}
	
	/* see superclass */
	@Override
	public void addProgressListener(ProgressListener l) {
		listeners.add(l);
	}

	/* see superclass */
	@Override
	public void removeProgressListener(ProgressListener l) {
		listeners.remove(l);
	}

	/* see superclass */
	@Override
	public void cancel() {
		requestCancel = true;
	}

	@Override
	public void reset() throws Exception {
		// n/a
	}

	@Override
	public void checkPreconditions() throws Exception {
		// n/a
	}


	/**
	 * Fires a {@link ProgressEvent}.
	 * 
	 * @param pct percent done
	 * @param note progress note
	 */
	public void fireProgressEvent(int pct, String note) {
		ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).updateProgress(pe);
		}
		log.info("    " + pct + "% " + note);
	}
	
	private String removeSpaces(String string) {
		if (string != null)
			return string.replace(" ", "").trim();
		else
			return null;
	}

}
