
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

// TODO: Auto-generated Javadoc
/**
 * The Class SimpleLoaderAlgorithm.
 */
public class SimpleLoaderAlgorithm extends RootServiceJpa implements Algorithm, AutoCloseable {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** Name of terminology to be loaded. */
  private String terminology;

  /** The input directory. */
  private String inputDir;

  /** Terminology version. */
  private String version;

  /** Metadata counter. */
  private int metadataCounter;

  /** The date format. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  /** The concept file name. */
  final private String CONCEPT_FILE_NAME = "concepts.txt";

  /** The parent child file name. */
  final private String PARENT_CHILD_FILE_NAME = "parent-child.txt";

  /** The concept attributes file name. */
  final private String CONCEPT_ATTRIBUTES_FILE_NAME = "concept-attributes.txt";

  /** The concept relationships file name. */
  final private String CONCEPT_RELATIONSHIPS_FILE_NAME = "concept-relationships.txt";

  /** The simple refsets file name. */
  final private String SIMPLE_REFSETS_FILE_NAME = "simple-refset-members.txt";

  /** The obj ct. */
  private int objCt = 1001;

  /**
   * Instantiates a {@link SimpleLoaderAlgorithm} from the specified parameters.
   *
   * @param terminology the terminology
   * @param version the version
   * @param inputDir the input dir
   * @param metadataCounter the metadata counter
   * @throws Exception the exception
   */
  public SimpleLoaderAlgorithm(String terminology, String version, String inputDir,
      String metadataCounter) throws Exception {

    super();
    this.terminology = terminology;
    this.version = version;
    this.inputDir = inputDir;
    this.metadataCounter = metadataCounter == null ? 1 : Integer.parseInt(metadataCounter);

    // initialize logger
    String rootPath =
        ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
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
    SimpleLoaderAlgorithm.log = LoggerUtility.getLogger("load");
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
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
    boolean conAttrFileExists = false;
    boolean conRelFileExists = false;
    boolean simpleRefsetFileExists = false;

    // Check the input directory
    File inputDirFile = new File(this.inputDir);
    if (!inputDirFile.exists()) {
      throw new Exception("Specified input directory does not exist");
    }
    if (!new File(this.inputDir, CONCEPT_FILE_NAME).exists()) {
      throw new Exception(
          "The " + CONCEPT_FILE_NAME + " file of the input directory does not exist");
    }
    if (!new File(this.inputDir, PARENT_CHILD_FILE_NAME).exists()) {
      log.info("The " + PARENT_CHILD_FILE_NAME
          + " file of the input directory does not exist. Making default isa relationships to root.");
      parChdFileExists = false;
    } else {
      parChdFileExists = true;
    }
    if (new File(this.inputDir, CONCEPT_ATTRIBUTES_FILE_NAME).exists()) {
      conAttrFileExists = true;
    }
    if (new File(this.inputDir, CONCEPT_RELATIONSHIPS_FILE_NAME).exists()) {
      conRelFileExists = true;
    }
    if (new File(this.inputDir, SIMPLE_REFSETS_FILE_NAME).exists()) {
      simpleRefsetFileExists = true;
    }

    log.info("Starting loading simple data");
    log.info("  terminology = " + terminology);
    log.info("  version     = " + version);
    log.info("  inputDir    = " + inputDir);

    try {
      final ContentService contentService = new ContentServiceJpa();
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      final Date now = new Date();
      SimpleMetadataHelper helper =
          new SimpleMetadataHelper(terminology, version, dateFormat.format(now), contentService);
      log.info("  Create concept metadata");
      helper.setMetadataCounter(metadataCounter);
      Map<String, Concept> conceptMap = helper.createMetadata();

      // Create the root concept
      log.info("  Create the root concept");
      Concept rootConcept = new ConceptJpa();
      rootConcept.setTerminologyId("root");
      rootConcept.setEffectiveTime(now);
      // assume active
      rootConcept.setActive(true);
      rootConcept.setModuleId(Long.parseLong(conceptMap.get("defaultModule").getTerminologyId()));
      rootConcept.setDefinitionStatusId(
          Long.parseLong(conceptMap.get("defaultDefinitionStatus").getTerminologyId()));
      rootConcept.setTerminology(terminology);
      rootConcept.setTerminologyVersion(version);
      rootConcept.setDefaultPreferredName(terminology + " Root Concept");

      final Description rootDesc = new DescriptionJpa();
      rootDesc.setTerminologyId("root");
      rootDesc.setEffectiveTime(now);
      rootDesc.setActive(true);
      rootDesc.setModuleId(Long.parseLong(conceptMap.get("defaultModule").getTerminologyId()));
      rootDesc.setTerminology(terminology);
      rootDesc.setTerminologyVersion(version);
      rootDesc.setTerm(terminology + " Root Concept");
      rootDesc.setConcept(rootConcept);
      rootDesc.setCaseSignificanceId(
          Long.valueOf(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
      rootDesc.setLanguageCode("en");
      rootDesc.setTypeId(Long.parseLong(conceptMap.get("preferred").getTerminologyId()));
      rootConcept.addDescription(rootDesc);
      rootConcept = contentService.addConcept(rootConcept);
      conceptMap.put(rootConcept.getTerminologyId(), rootConcept);

      //
      // Open the file and process the data
      // code\tpreferred\t[synonym\t,..]
      log.info("  Load concepts");
      String line;
      final BufferedReader concepts =
          new BufferedReader(new FileReader(new File(inputDir, CONCEPT_FILE_NAME)));

      while ((line = concepts.readLine()) != null) {
        final String[] fields = parseLine(line);

        // skip header
        if (fields[0].equals("code")) {
          continue;
        }

        if (fields.length < 2) {
          throw new Exception("Unexpected line, not enough fields: " + line);
        }
        final String code = fields[0];
        final String preferred = fields[1];
        Concept concept = new ConceptJpa();
        concept.setTerminologyId(code);
        concept.setEffectiveTime(now);
        // assume active
        concept.setActive(true);
        concept.setModuleId(Long.parseLong(conceptMap.get("defaultModule").getTerminologyId()));
        concept.setDefinitionStatusId(
            Long.parseLong(conceptMap.get("defaultDefinitionStatus").getTerminologyId()));
        concept.setTerminology(terminology);
        concept.setTerminologyVersion(version);
        concept.setDefaultPreferredName(preferred);

        final Description pref = new DescriptionJpa();
        pref.setTerminologyId(objCt++ + "");
        pref.setEffectiveTime(now);
        pref.setActive(true);
        pref.setModuleId(Long.parseLong(conceptMap.get("defaultModule").getTerminologyId()));
        pref.setTerminology(terminology);
        pref.setTerminologyVersion(version);
        pref.setTerm(preferred);
        pref.setConcept(concept);
        pref.setCaseSignificanceId(
            Long.valueOf(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
        pref.setLanguageCode("en");
        pref.setTypeId(Long.parseLong(conceptMap.get("preferred").getTerminologyId()));
        concept.addDescription(pref);

        for (int i = 2; i < fields.length; i++) {
          helper.createAttributeValue(concept,
              Long.parseLong(conceptMap.get("synonym").getTerminologyId()), fields[i], version,
              objCt++, now);
        }

        log.info(
            "  concept = " + concept.getTerminologyId() + ", " + concept.getDefaultPreferredName());
        concept = contentService.addConcept(concept);
        conceptMap.put(concept.getTerminologyId(), concept);
        concept = contentService.getConcept(concept.getId());

        // If no par/chd file, make isa relationships to the root
        if (!parChdFileExists) {
          helper.createIsaRelationship(rootConcept, concept, objCt++ + "", terminology, version,
              dateFormat.format(now));
        }

      }

      // If there is a par/chd file, need to create all those
      // relationships now
      if (parChdFileExists) {
        log.info("  Load par/chd relationships");
        final BufferedReader parentChild =
            new BufferedReader(new FileReader(new File(inputDir, PARENT_CHILD_FILE_NAME)));

        while ((line = parentChild.readLine()) != null) {
          final String[] fields = parseLine(line);

          if (fields.length != 2) {
            throw new Exception("Unexpected number of fields: " + fields.length);
          }
          final Concept par = conceptMap.get(fields[0]);
          if (par == null) {
            throw new Exception("Unable to find parent concept " + line);
          }
          final Concept chd = conceptMap.get(fields[1]);
          if (chd == null) {
            throw new Exception("Unable to find child concept " + line);
          }
          helper.createIsaRelationship(par, chd, objCt++ + "", terminology, version,
              dateFormat.format(now));

        }
        parentChild.close();
      }

      // Create terminology-specific metadata concept - for any metadata created
      // with attr/rel/refsets
      final Concept targetTerminologyMetadataConcept =
          helper.createNewActiveConcept(terminology + " metadata", conceptMap.get("Metadata"));
      conceptMap.put(terminology + " metadata", targetTerminologyMetadataConcept);

      final Concept refsetsConcept =
  	        helper.createNewActiveConcept("Refsets", targetTerminologyMetadataConcept);
      conceptMap.put("Refsets", refsetsConcept);
      
    final Concept simpleRefsetsConcept =
  	        helper.createNewActiveConcept("Simple refsets", refsetsConcept);
    conceptMap.put("Simple refsets", simpleRefsetsConcept);
    
    final Concept asteriskRefsetConcept =
  	        helper.createNewActiveConcept("Asterisk refset", simpleRefsetsConcept);
    conceptMap.put("Asterisk refset", asteriskRefsetConcept);
    
    final Concept daggerRefsetConcept =
  	        helper.createNewActiveConcept("Dagger refset", simpleRefsetsConcept);
    conceptMap.put("Dagger refset", daggerRefsetConcept);
    
      // If there is a concept attributes file, need to create all those
      // attributes now
      if (conAttrFileExists) {
        loadConceptAttributes(helper, conceptMap, now);
      }

      // If there is a concept relationships file, load now
      if (conRelFileExists) {
        loadConceptRelationships(helper, conceptMap, now);
      }

      // If there is a simple refsets file , load now
      if (simpleRefsetFileExists) {
        loadSimpleRefsets(helper, conceptMap, now);
      }

      concepts.close();
      contentService.commit();

      // Tree position computation
      String isaRelType = conceptMap.get("isa").getTerminologyId();
      log.info("Start creating tree positions root, " + isaRelType);
      contentService.computeTreePositions(terminology, version, isaRelType, "root");

      // Clean-up
      log.info("done ...");
      contentService.close();

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      throw new Exception("Unexpected exception:", e);
    }
  }

  /**
   * Load concept attributes.
   *
   * @param helper the helper
   * @param conceptMap the concept map
   * @param now the now
   * @throws Exception the exception
   */
  private void loadConceptAttributes(SimpleMetadataHelper helper, Map<String, Concept> conceptMap,
    Date now) throws Exception {
    log.info("  Load concept attributes");

    String line;
    try (final BufferedReader conAttr =
        new BufferedReader(new FileReader(new File(inputDir, CONCEPT_ATTRIBUTES_FILE_NAME)));) {

      final Concept targetTerminologyMetadataConcept = conceptMap.get(terminology + " metadata");

      
      while ((line = conAttr.readLine()) != null) {
        final String[] fields = parseLine(line);

        if (fields.length == 1) {
          addConceptAttributesMetadata(helper, targetTerminologyMetadataConcept, conceptMap,
              fields);
        } else if (fields.length != 3) {
          throw new Exception("Unexpected number of fields: " + fields.length);
        } else {
          final Concept con = conceptMap.get(fields[0]);
          if (con == null) {
            throw new Exception("Unable to find parent concept " + line);
          }

          final String attrType = fields[1];
          final String value = fields[2];

          helper.createAttributeValue(con,
              Long.parseLong(conceptMap.get(attrType).getTerminologyId()), value, version, objCt++,
              now);
        }
      }

    }
  }

  /**
   * Adds the concept attributes metadata.
   *
   * @param helper the helper
   * @param parentConcept the parent concept
   * @param conceptMap the concept map
   * @param fields the fields
   * @throws Exception the exception
   */
  private void addConceptAttributesMetadata(SimpleMetadataHelper helper, Concept parentConcept,
    Map<String, Concept> conceptMap, String[] fields) throws Exception {
    // Create metadata concept from terminology input file
    final Concept newConcept = helper.createNewActiveConcept(fields[0], parentConcept);
    conceptMap.put(fields[0], newConcept);
  }

  /**
   * Load concept relationships.
   *
   * @param helper the helper
   * @param conceptMap the concept map
   * @param now the now
   * @throws Exception the exception
   */
  private void loadConceptRelationships(SimpleMetadataHelper helper,
    Map<String, Concept> conceptMap, Date now) throws Exception {
    log.info("  Load concept relationships");

    String line;
    try (final BufferedReader conAttr =
        new BufferedReader(new FileReader(new File(inputDir, CONCEPT_RELATIONSHIPS_FILE_NAME)));) {

      final Concept targetTerminologyMetadataConcept = conceptMap.get(terminology + " metadata");
      while ((line = conAttr.readLine()) != null) {
        final String[] fields = parseLine(line);

        if (fields.length == 1) {
          addConceptRelationshipsMetadata(helper, targetTerminologyMetadataConcept, conceptMap,
              fields);
        } else if (fields.length != 4) {
          throw new Exception("Unexpected number of fields: " + fields.length);
        } else {
          final Concept sourceCon = conceptMap.get(fields[0]);
          if (sourceCon == null) {
            throw new Exception("Unable to find source concept " + line);
          }
          final Concept destinationCon = conceptMap.get(fields[2]);
          if (destinationCon == null) {
            throw new Exception("Unable to find source concept " + line);
          }

          final String type = fields[2];
          final String label = fields[3];
          helper.createRelationship(sourceCon, destinationCon, label, type, "" + objCt++, terminology,
              version, dateFormat.format(now));
        }
      }

    }
  }

  /**
   * Adds the concept relationships metadata.
   *
   * @param helper the helper
   * @param parentConcept the parent concept
   * @param conceptMap the concept map
   * @param fields the fields
   * @throws Exception the exception
   */
  private void addConceptRelationshipsMetadata(SimpleMetadataHelper helper, Concept parentConcept,
    Map<String, Concept> conceptMap, String[] fields) throws Exception {
    // Create metadata concept from terminology input file
    final Concept newConcept = helper.createNewActiveConcept(fields[0], parentConcept);
    conceptMap.put(fields[0], newConcept);
  }

  private void loadSimpleRefsets(SimpleMetadataHelper helper, Map<String, Concept> conceptMap,
    Date now) throws Exception {
    log.info("  Load simple refsets and members");

    String line;
    try (final BufferedReader conAttr =
        new BufferedReader(new FileReader(new File(inputDir, SIMPLE_REFSETS_FILE_NAME)));) {
	    
      while ((line = conAttr.readLine()) != null) {
        final String[] fields = parseLine(line);

        if (fields.length == 2) {
          final Concept con = conceptMap.get(fields[0]);
          if (con == null) {
            throw new Exception("Unable to find concept " + line);
          }

          final String refsetName = fields[1];
          helper.createRefsetMember(con, refsetName, "" + objCt++, terminology, version,
              dateFormat.format(now));

        }
      }

    }
  }

  private void addSimpleRefsetMetadata(SimpleMetadataHelper helper, Concept parentConcept,
    Map<String, Concept> conceptMap, String[] fields) throws Exception {
    // Create metadata concept from terminology input file
    final Concept newConcept = helper.createNewActiveConcept(fields[0], parentConcept);
    conceptMap.put(fields[0], newConcept);
  }

  /**
   * Parses the line.
   *
   * @param line the line
   * @return the string[]
   */
  private String[] parseLine(String line) {
    line = line.replace("\r", "");
    return line.indexOf('\t') != -1 ? line.split("\t") : line.split("\\|");
  }

  /**
   * Adds the progress listener.
   *
   * @param l the l
   */
  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  /**
   * Removes the progress listener.
   *
   * @param l the l
   */
  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  /**
   * Reset.
   *
   * @throws Exception the exception
   */
  @Override
  public void reset() throws Exception {
    // n/a
  }

  /**
   * Check preconditions.
   *
   * @throws Exception the exception
   */
  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

  /**
   * Cancel.
   *
   * @throws Exception the exception
   */
  @Override
  public void cancel() throws Exception {
    // n/a
  }

}
