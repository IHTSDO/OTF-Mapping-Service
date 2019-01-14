package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.algo.helpers.SimpleMetadataHelper;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

public class SimpleLoaderAlgorithm extends RootServiceJpa
		implements Algorithm, AutoCloseable {

	/** Listeners. */
	private List<ProgressListener> listeners = new ArrayList<>();

	/** The request cancel flag. */
	private boolean requestCancel = false;

	/** Name of terminology to be loaded. */
	private String terminology;
	
	/** The input directory */
	private String inputDir;

	/** Terminology version */
	private String version;
	
	/** The date format. */
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    
    /** The log. */
    private static Logger log;
    
    /** The log file. */
    private File logFile;
    
    final private String CONCEPT_FILE_NAME = "concepts.txt";
    final private String PARENT_CHILD_FILE_NAME = "parent-child.txt";
    
	/**
	 * Instantiates a {@link SimpleLoaderAlgorithm} from the
	 * specified parameters.
	 * 
	 */
	public SimpleLoaderAlgorithm(String terminology, String version,
			String inputDir) throws Exception {

		super();
		this.terminology = terminology;
		this.version = version;
		this.inputDir = inputDir;
		
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

	/* see superclass */
	@SuppressWarnings("resource")
	/* see superclass */
	@Override
	public void compute() throws Exception {
	  
		// clear log before starting process
		PrintWriter writer = new PrintWriter(logFile);
		writer.print("");
		writer.close();

		boolean parChdFileExists = false;
  
		// Check the input directory
		File inputDirFile = new File(this.inputDir);
		if (!inputDirFile.exists()) {
			throw new Exception("Specified input directory does not exist");
		}
		if (!new File(this.inputDir, CONCEPT_FILE_NAME).exists()) {
			throw new Exception("The " + CONCEPT_FILE_NAME
					+ " file of the input directory does not exist");
		}
		if (!new File(this.inputDir, PARENT_CHILD_FILE_NAME).exists()) {
			throw new Exception("The " + PARENT_CHILD_FILE_NAME
					+ " file of the input directory does not exist");
		} else {
			parChdFileExists = true;
		}
      
		log.info("Starting loading simple data");
		log.info("  terminology = " + terminology);
		log.info("  version     = " + version);
		log.info("  inputDir    = " + inputDir);

		try {
			final ContentService contentService = new ContentServiceJpa();
			contentService.setTransactionPerOperation(false);
			contentService.beginTransaction();
			int objCt = 1000;

			final Date now = new Date();
			SimpleMetadataHelper helper = new SimpleMetadataHelper(terminology,
					version, dateFormat.format(now), contentService);
			log.info("  Create concept metadata");
			Map<String, Concept> conceptMap = helper.createMetadata();

			// Create the root concept
			log.info("  Create the root concept");
			Concept rootConcept = new ConceptJpa();
			rootConcept.setTerminologyId("root");
			rootConcept.setEffectiveTime(now);
			// assume active
			rootConcept.setActive(true);
			rootConcept.setModuleId(Long.parseLong(
					conceptMap.get("defaultModule").getTerminologyId()));
			rootConcept.setDefinitionStatusId(Long.parseLong(conceptMap
					.get("defaultDefinitionStatus").getTerminologyId()));
			rootConcept.setTerminology(terminology);
			rootConcept.setTerminologyVersion(version);
			rootConcept.setDefaultPreferredName(terminology + " Root Concept");

			final Description rootDesc = new DescriptionJpa();
			rootDesc.setTerminologyId("root");
			rootDesc.setEffectiveTime(now);
			rootDesc.setActive(true);
			rootDesc.setModuleId(Long.parseLong(
					conceptMap.get("defaultModule").getTerminologyId()));
			rootDesc.setTerminology(terminology);
			rootDesc.setTerminologyVersion(version);
			rootDesc.setTerm(terminology + " Root Concept");
			rootDesc.setConcept(rootConcept);
			rootDesc.setCaseSignificanceId(new Long(conceptMap
					.get("defaultCaseSignificance").getTerminologyId()));
			rootDesc.setLanguageCode("en");
			rootDesc.setTypeId(Long
					.parseLong(conceptMap.get("preferred").getTerminologyId()));
			rootConcept.addDescription(rootDesc);
			rootConcept = contentService.addConcept(rootConcept);
			conceptMap.put(rootConcept.getTerminologyId(), rootConcept);

			//
			// Open the file and process the data
			// code\tpreferred\t[synonym\t,..]
			log.info("  Load concepts");
			String line;
			final BufferedReader concepts = new BufferedReader(
					new FileReader(new File(inputDir, CONCEPT_FILE_NAME)));
			
			while ((line = concepts.readLine()) != null) {
				line = line.replace("\r", "");
				final String[] fields = line.indexOf('\t') != -1
						? line.split("\t") : line.split("\\|");
				// skip header
				if (fields[0].equals("code")) {
					continue;
				}

				if (fields.length < 2) {
					throw new Exception(
							"Unexpected line, not enough fields: " + line);
				}
				final String code = fields[0];
				final String preferred = fields[1];
				Concept concept = new ConceptJpa();
				concept.setTerminologyId(code);
				concept.setEffectiveTime(now);
				// assume active
				concept.setActive(true);
				concept.setModuleId(Long.parseLong(
						conceptMap.get("defaultModule").getTerminologyId()));
				concept.setDefinitionStatusId(Long.parseLong(conceptMap
						.get("defaultDefinitionStatus").getTerminologyId()));
				concept.setTerminology(terminology);
				concept.setTerminologyVersion(version);
				concept.setDefaultPreferredName(preferred);

				final Description pref = new DescriptionJpa();
				pref.setTerminologyId(++objCt + "");
				pref.setEffectiveTime(now);
				pref.setActive(true);
				pref.setModuleId(Long.parseLong(
						conceptMap.get("defaultModule").getTerminologyId()));
				pref.setTerminology(terminology);
				pref.setTerminologyVersion(version);
				pref.setTerm(preferred);
				pref.setConcept(concept);
				pref.setCaseSignificanceId(new Long(conceptMap
						.get("defaultCaseSignificance").getTerminologyId()));
				pref.setLanguageCode("en");
				pref.setTypeId(Long.parseLong(
						conceptMap.get("preferred").getTerminologyId()));
				concept.addDescription(pref);

				for (int i = 2; i < fields.length; i++) {
					final Description sy = new DescriptionJpa();
					sy.setTerminologyId(++objCt + "");
					sy.setEffectiveTime(now);
					sy.setActive(true);
					sy.setModuleId(Long.parseLong(conceptMap
							.get("defaultModule").getTerminologyId()));
					sy.setTerminology(terminology);
					sy.setTerminologyVersion(version);
					sy.setTerm(fields[i]);
					sy.setConcept(concept);
					sy.setCaseSignificanceId(
							new Long(conceptMap.get("defaultCaseSignificance")
									.getTerminologyId()));
					sy.setLanguageCode("en");
					sy.setTypeId(Long.parseLong(
							conceptMap.get("synonym").getTerminologyId()));
					concept.addDescription(sy);
				}

				log.info("  concept = " + concept.getTerminologyId() + ", "
								+ concept.getDefaultPreferredName());
				concept = contentService.addConcept(concept);
				conceptMap.put(concept.getTerminologyId(), concept);
				concept = contentService.getConcept(concept.getId());

				// If no par/chd file, make isa relationships to the root
				if (!parChdFileExists) {
					helper.createIsaRelationship(rootConcept, concept,
							++objCt + "", terminology, version,
							dateFormat.format(now));
				}

			}

			// If there is a par/chd file, need to create all those
			// relationships now
			if (parChdFileExists) {
				log.info("  Load par/chd relationships");
				final BufferedReader parentChild = new BufferedReader(
						new FileReader(
								new File(inputDir, PARENT_CHILD_FILE_NAME)));

				while ((line = parentChild.readLine()) != null) {
					line = line.replace("\r", "");
					final String[] fields = line.indexOf('\t') != -1
							? line.split("\t") : line.split("\\|");

					if (fields.length != 2) {
						throw new Exception("Unexpected number of fields: "
								+ fields.length);
					}
					final Concept par = conceptMap.get(fields[0]);
					if (par == null) {
						throw new Exception(
								"Unable to find parent concept " + line);
					}
					final Concept chd = conceptMap.get(fields[1]);
					if (chd == null) {
						throw new Exception(
								"Unable to find child concept " + line);
					}
					helper.createIsaRelationship(par, chd, ++objCt + "",
							terminology, version, dateFormat.format(now));

				}
				parentChild.close();
			}

			concepts.close();
			contentService.commit();

			// Tree position computation
			String isaRelType = conceptMap.get("isa").getTerminologyId();
			log
					.info("Start creating tree positions root, " + isaRelType);
			contentService.computeTreePositions(terminology, version,
					isaRelType, "root");

			// Clean-up
			log.info("done ...");
			contentService.close();

		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
			throw new Exception("Unexpected exception:", e);
		}
	}

	@Override
	public void addProgressListener(ProgressListener l) {
		listeners.add(l);
	}

	@Override
	public void removeProgressListener(ProgressListener l) {
		listeners.remove(l);
	}

	@Override
	public void reset() throws Exception {
		// n/a
	}

	@Override
	public void checkPreconditions() throws Exception {
		// n/a
	}

	@Override
	public void cancel() throws Exception {
		requestCancel = true;
	}
	
}
