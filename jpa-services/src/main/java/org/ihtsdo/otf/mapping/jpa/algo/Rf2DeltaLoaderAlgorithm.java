/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.DescriptionList;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.RelationshipList;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.LanguageRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

public class Rf2DeltaLoaderAlgorithm extends RootServiceJpa
    implements Algorithm, AutoCloseable {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /** Name of terminology to be loaded. */
  private String terminology;

  /** Terminology version */
  private String version;

  /**
   * Last publication version passed in. This is used to "remove retired
   * cocepts" routine
   */
  private String lastPublicationDate;

  /** The input directory */
  private String inputDir;

  /** The delta dir. */
  private File deltaDir;
  
  /** the type ids. */
  private static Long fsnTypeId = 900000000000003001L;
  private static Long definitionTypeId = 900000000000550004L;
  
  /** the defaultPreferredNames type id - default to FSN. */
  private Long dpnTypeId = fsnTypeId;

  /** The dpn acceptability id - default to Preferred. */
  private Long dpnAcceptabilityId = 900000000000548007L;

  /** The dpn ref set ids. */
  private List<Long> dpnRefsetIdArray = new ArrayList<>();  
  
  /** The concept reader. */
  private BufferedReader conceptReader;

  /** The description reader. */
  private BufferedReader descriptionReader;

  /** The text definition reader. */
  private BufferedReader textDefinitionReader;

  /** The relationship reader. */
  private BufferedReader relationshipReader;

  /** The language reader. */
  private BufferedReader languageReader;

  /** progress tracking variables. */
  private int objectCt; //

  /** The ft. */
  SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a");

  /** The dt. */
  SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");

  /** The start time. */
  long startTime;

  /** The time at which drip feed was started. */
  private Date deltaLoaderStartDate = new Date();

  /** Content and Mapping Services. */
  private ContentServiceJpa contentService = null;

  /** The mapping service. */
  private MappingService mappingService = null;

  /** The concept cache. */
  private Map<String, Concept> conceptCache = new HashMap<>();

  /** The description cache. */
  private Map<String, Description> descriptionCache = new HashMap<>();

  /** The relationship cache. */
  private Map<String, Relationship> relationshipCache = new HashMap<>();

  /** The language ref set member cache. */
  private Map<String, LanguageRefSetMember> languageRefSetMemberCache =
      new HashMap<>();

  // These track data that existed prior to the delta loader run

  /** The delta concept ids. */
  private Set<String> deltaConceptIds = new HashSet<>();

  /** The delta relationship ids. */
  private Set<String> deltaRelationshipIds = new HashSet<>();

  /** The delta description ids. */
  private Set<String> deltaDescriptionIds = new HashSet<>();

  /** The delta language refset member ids. */
  private Set<String> deltaLanguageRefSetMemberIds = new HashSet<>();
  
  /** The default preferred names set (terminologyId -> {rank, dpn}). */
  private Map<String, String[]> defaultPreferredNames = new HashMap<>();

  /** The "recompute preferred name" concept ids. */
  private Set<String> recomputePnConceptIds = new HashSet<>();

  /** The existing concept cache. */
  private Map<String, Concept> existingConceptCache = new HashMap<>();

  /** The existing description ids. */
  private Set<String> existingDescriptionIds = new HashSet<>();

  /** The existing relationship ids. */
  private Set<String> existingRelationshipIds = new HashSet<>();

  /** The existing language ref set member ids. */
  private Set<String> existingLanguageRefSetMemberIds = new HashSet<>();

  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  public Rf2DeltaLoaderAlgorithm(String terminology, String inputDir,
      String lastPublicationDate) throws Exception {
    super();
    this.terminology = terminology;
    this.inputDir = inputDir;
    this.lastPublicationDate = lastPublicationDate;

    // initialize logger
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
    log = LoggerUtility.getLogger("load");
  }

  @Override
  public void compute() throws Exception {

    // clear log before starting process
    PrintWriter writer = new PrintWriter(logFile);
    writer.print("");
    writer.close();

    try {
      setup();

      ConceptList conceptList =
          contentService.getAllConcepts(terminology, version);
      for (Concept c : conceptList.getConcepts()) {
        existingConceptCache.put(c.getTerminologyId(), c);
      }
      log.info("  count = " + conceptList.getCount());

      // Precache the description, langauge refset, and relationship ids
      log.info("  Load all description, language, and relationship ids");
      existingDescriptionIds =
          contentService.getAllDescriptionTerminologyIds(terminology, version);
      log.info("    descriptionCt = " + existingDescriptionIds.size());
      existingLanguageRefSetMemberIds = contentService
          .getAllLanguageRefSetMemberTerminologyIds(terminology, version);
      log.info("    languageCt = " + existingLanguageRefSetMemberIds.size());
      existingRelationshipIds =
          contentService.getAllRelationshipTerminologyIds(terminology, version);
      log.info("    relationshipCt = " + existingRelationshipIds.size());

      // Load delta data
      loadDelta();

      // Compute the number of modified objects of each type
      log.info("  Computing number of modified objects");
      int nConceptsUpdated = 0;
      int nDescriptionsUpdated = 0;
      int nLanguagesUpdated = 0;
      int nRelationshipsUpdated = 0;

      for (Concept c : conceptCache.values()) {
        if (c.getEffectiveTime().equals(deltaLoaderStartDate)) {
          nConceptsUpdated++;
        }
      }
      for (Relationship r : relationshipCache.values()) {
        if (r.getEffectiveTime().equals(deltaLoaderStartDate)) {
          nRelationshipsUpdated++;
        }
      }
      for (Description d : descriptionCache.values()) {
        if (d.getEffectiveTime().equals(deltaLoaderStartDate)) {
          nDescriptionsUpdated++;
        }
      }

      for (LanguageRefSetMember l : languageRefSetMemberCache.values()) {
        if (l.getEffectiveTime().equals(deltaLoaderStartDate)) {
          nLanguagesUpdated++;
        }
      }

      // Report counts
      log.info("  Cached objects modified by this delta");
      log.info("    " + nConceptsUpdated + " concepts");
      log.info("    " + nDescriptionsUpdated + " descriptions");
      log.info("    " + nRelationshipsUpdated + " relationships");
      log.info("    " + nLanguagesUpdated + " language ref set members");

      // Commit the content changes
      log.info("  Committing.");
      contentService.commit();

      // QA
      log.info(
          "  QA - Check database contents against previously modified objects.");
      ConceptList modifiedConcepts =
          contentService.getConceptsModifiedSinceDate(terminology,
              deltaLoaderStartDate, null);
      RelationshipList modifiedRelationships = contentService
          .getRelationshipsModifiedSinceDate(terminology, deltaLoaderStartDate);
      DescriptionList modifiedDescriptions = contentService
          .getDescriptionsModifiedSinceDate(terminology, deltaLoaderStartDate);
      LanguageRefSetMemberList modifiedLanguageRefSetMembers =
          contentService.getLanguageRefSetMembersModifiedSinceDate(terminology,
              deltaLoaderStartDate);

      // Report
      log.info((modifiedConcepts.getCount() != nConceptsUpdated)
          ? "    " + nConceptsUpdated + " concepts expected, found "
              + modifiedConcepts.getCount()
          : "    Concept count matches");
      log.info((modifiedRelationships.getCount() != nRelationshipsUpdated)
          ? "   " + nRelationshipsUpdated + " relationships expected, found"
              + modifiedRelationships.getCount()
          : "    Relationship count matches");
      log.info((modifiedDescriptions.getCount() != nDescriptionsUpdated)
          ? "    " + nDescriptionsUpdated + " descriptions expected, found"
              + modifiedDescriptions.getCount()
          : "    Description count matches");
      log.info((modifiedLanguageRefSetMembers.getCount() != nLanguagesUpdated)
          ? "    " + nLanguagesUpdated
              + " languageRefSetMembers expected, found"
              + modifiedLanguageRefSetMembers.getCount()
          : "    LanguageRefSetMember count matches");

      // Clean up resources
      contentService.close();

      // Compute default preferred names
      log.info("  Compute preferred names for delta concepts.");
      contentService = new ContentServiceJpa();
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();
      computeDefaultPreferredNames();
      contentService.commit();
      log.info("Done");

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      throw new Exception("Unexpected exception:", e);
    }
  }

  /**
   * Instantiate global vars.
   * 
   * @throws Exception the exception
   */
  private void setup() throws Exception {

    Properties config = ConfigUtility.getConfigProperties();

    // instantiate the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();

    // set the transaction per operation on the service managers
    contentService.setTransactionPerOperation(false);
    mappingService.setTransactionPerOperation(false);

    // initialize the transactions
    contentService.beginTransaction();
    mappingService.beginTransaction();

    if (lastPublicationDate == null) {
      // NOTE: this is very MySQL-centric (native query).
      // We want the max effective time where the "time"
      // part of it is 00:00:00
      final javax.persistence.Query query =
          contentService.getEntityManager().createNativeQuery(
              "select date_format(max(effectiveTime),'%Y%m%d') from concepts "
                  + "where terminology = :terminology "
                  + "  and effectiveTime = date(effectiveTime)");
      query.setParameter("terminology", terminology);
      lastPublicationDate = query.getSingleResult().toString();
    }

    // set the delta file directory=
    deltaDir = new File(inputDir);
    if (!deltaDir.exists()) {
      throw new Exception("Specified input dir");
    }

    // get the first file for determining
    File files[] = deltaDir.listFiles();
    if (files.length == 0)
      throw new Exception(
          "Could not determine terminology version, no files exist");

    // Previous computation of terminology version is based on file name
    // but for delta/daily build files, this is not the current version
    // look up the current version instead
    if (version == null) {
      try (final MetadataService metadataService = new MetadataServiceJpa();) {
        version = metadataService.getLatestVersion(terminology);
        if (version == null) {
          throw new Exception("Unable to determine terminology version.");
        }
      }
    }

    // get the config properties for default preferred name variables
    // set the dpn variables and instantiate the concept dpn map
    Properties properties = ConfigUtility.getConfigProperties();


    // set the parameters for determining defaultPreferredNames
    String props = properties.getProperty("loader.defaultPreferredNames.refSetId");
    String tokens[] = props.split(",");
    for (String prop : tokens) {
      if (prop != null) {
        dpnRefsetIdArray.add(Long.valueOf(prop));
      }
    }
    
    // If typeId is specified, override default
    String prop = properties.getProperty("loader.defaultPreferredNames.typeId");
    if (prop != null) {
      dpnTypeId = Long.valueOf(prop);
    }

    // If acceptabilityId is specified, override default
    prop = properties.getProperty("loader.defaultPreferredNames.acceptabilityId");
    if (prop != null) {
      dpnAcceptabilityId = Long.valueOf(prop);
    }

    log.info("  Default preferred name settings:");
    log.info("    dpnRefsetIdArray = " + dpnRefsetIdArray);
    log.info("    dpnTypeId = " + dpnTypeId);
    log.info("    dpnAcceptabilityId = " + dpnAcceptabilityId);
    

    // Open files
    instantiateFileReaders();
  }

  /**
   * Instantiate file readers.
   * 
   * @throws Exception the exception
   */
  private void instantiateFileReaders() throws Exception {

    log.info("  Open readers for terminology files:  ");
    // concepts file
    for (File f : deltaDir.listFiles()) {
      if (f.getName().contains("_Concept_Delta_")) {
        log.info("    Concepts: " + f.getName());
        conceptReader = new BufferedReader(new FileReader(f));
      } else if (f.getName().contains("_Relationship_Delta_")) {
        log.info("    Relationships: " + f.getName());
        relationshipReader = new BufferedReader(new FileReader(f));

        /*
         * Removed due to invalid relationship loading } else if
         * (f.getName().contains("_StatedRelationship_")) { log.
         * info("  Stated Relationship file: " + f.getName());
         * statedRelationshipReader = new BufferedReader(new FileReader(f));
         */
      } else if (f.getName().contains("_Description_")) {
        log.info("  Descriptions: " + f.getName());
        descriptionReader = new BufferedReader(new FileReader(f));
      } else if (f.getName().contains("_TextDefinition_")) {
        log.info("  Text Definitions: " + f.getName());
        textDefinitionReader = new BufferedReader(new FileReader(f));
      } else if (f.getName().contains("_LanguageDelta-en")) {
        log.info("  Languages: " + f.getName());
        languageReader = new BufferedReader(new FileReader(f));
      }
    }

    // check file readers were opened successfully
    if (conceptReader == null)
      throw new Exception("Could not open concept file reader");
    if (relationshipReader == null)
      throw new Exception("Could not open relationship file reader");
    if (descriptionReader == null)
      throw new Exception("Could not open description file reader");
    if (languageReader == null)
      throw new Exception("Could not open language ref set member file reader");
  }

  /**
   * Load delta.
   *
   * @throws Exception the exception
   */
  private void loadDelta() throws Exception {
    log.info("  Load delta data");

    // Load concepts
    if (conceptReader != null) {
      log.info("    Loading Concepts ...");
      startTime = System.nanoTime();
      loadConcepts(conceptReader);
      contentService.commit();
      contentService.beginTransaction();
      log.info("      evaluated = " + Integer.toString(objectCt) + " (Ended at "
          + ft.format(new Date()) + ")");
    }

    // Load relationships
    if (relationshipReader != null) {
      log.info("    Loading Relationships ...");
      startTime = System.nanoTime();
      loadRelationships(relationshipReader);
      contentService.commit();
      contentService.beginTransaction();
      log.info("      evaluated = " + Integer.toString(objectCt) + " (Ended at "
          + ft.format(new Date()) + ")");
    }

    // Load descriptions
    if (descriptionReader != null) {
      log.info("    Loading Descriptions ...");
      startTime = System.nanoTime();
      loadDescriptions(descriptionReader);
      contentService.commit();
      contentService.beginTransaction();
      log.info("      evaluated = " + Integer.toString(objectCt) + " (Ended at "
          + ft.format(new Date()) + ")");
    }

    // Load text definitions
    if (descriptionReader != null) {
      log.info("    Loading Text Definitions...");
      startTime = System.nanoTime();
      loadDescriptions(textDefinitionReader);
      contentService.commit();
      contentService.beginTransaction();
      log.info("      evaluated = " + Integer.toString(objectCt) + " (Ended at "
          + ft.format(new Date()) + ")");
    }

    // Load language refset members
    if (languageReader != null) {
      log.info("    Loading Language Ref Sets...");
      startTime = System.nanoTime();
      loadLanguageRefSetMembers(languageReader);
      contentService.commit();
      contentService.beginTransaction();
      log.info("      evaluated = " + Integer.toString(objectCt) + " (Ended at "
          + ft.format(new Date()) + ")");
    }

    // Skip other delta data structures

    // Remove concepts in the DB that were created by prior
    // deltas that no longer exist in the delta
    log.info("    Retire non-existent content");
    retireRemovedContent();
  }

  /**
   * Loads the concepts from the delta files.
   *
   * @param reader the reader
   * @throws Exception the exception
   */
  private void loadConcepts(BufferedReader reader) throws Exception {

    // Setup vars
    String line;
    objectCt = 0;
    int objectsAdded = 0;
    int objectsUpdated = 0;

    // Iterate through concept reader
    while ((line = reader.readLine()) != null) {

      // Split line
      String fields[] = line.split("\t");

      // if not header
      if (!fields[0].equals("id")) {

        // Check if concept exists from before
        Concept concept = existingConceptCache.get(fields[0]);

        // Track all delta concept ids so we can properly remove
        // concepts later.
        deltaConceptIds.add(fields[0]);

        // Setup delta concept (either new or based on existing one)
        Concept newConcept = null;
        if (concept == null) {
          newConcept = new ConceptJpa();
        } else {
          newConcept = new ConceptJpa(concept, true);
        }

        // Set fields
        newConcept.setTerminologyId(fields[0]);
        newConcept.setEffectiveTime(dt.parse(fields[1]).after(new Date())
            ? deltaLoaderStartDate : dt.parse(fields[1]));
        newConcept.setActive(fields[2].equals("1") ? true : false);
        newConcept.setModuleId(Long.valueOf(fields[3]));
        newConcept.setDefinitionStatusId(Long.valueOf(fields[4]));
        newConcept.setTerminology(terminology);
        newConcept.setTerminologyVersion(version);
        newConcept.setDefaultPreferredName("TBD");

        // If concept is new, add it
        if (concept == null) {
          log.info("        add concept " + newConcept.getTerminologyId());
          recomputePnConceptIds.add(fields[0]);
          contentService.addConcept(newConcept);
          objectsAdded++;
        }

        // If concept has changed, update it
        else if (!newConcept.equals(concept)) {
          log.info("        update concept " + newConcept.getTerminologyId());
          recomputePnConceptIds.add(fields[0]);
          contentService.updateConcept(newConcept);
          objectsUpdated++;
        }

        // Otherwise, reset effective time (for modified check later)
        else {
          newConcept.setEffectiveTime(concept.getEffectiveTime());
        }

        // Cache the concept element
        cacheConcept(newConcept);

      }

    }

    log.info("      new = " + objectsAdded);
    log.info("      updated = " + objectsUpdated);

  }

  /**
   * Load descriptions.
   *
   * @param reader the reader
   * @throws Exception the exception
   */
  private void loadDescriptions(BufferedReader reader) throws Exception {

    // Setup vars
    String line = "";
    objectCt = 0;
    int objectsAdded = 0;
    int objectsUpdated = 0;
    // Iterate through description reader
    while ((line = reader.readLine()) != null) {
      // split line
      String fields[] = line.split("\t");

      // if not header
      if (!fields[0].equals("id")) {

        deltaDescriptionIds.add(fields[0]);

        // Get concept from cache or from db
        Concept concept = null;
        if (conceptCache.containsKey(fields[4])) {
          concept = conceptCache.get(fields[4]);
        } else if (existingConceptCache.containsKey(fields[4])) {
          concept = contentService
              .getConcept(existingConceptCache.get(fields[4]).getId());
        } else {
          // retrieve concept
          concept = contentService.getConcept(fields[4], terminology, version);
        }

        // if the concept is not null
        if (concept != null) {

          // Add concept to the cache
          cacheConcept(concept);

          // Load description from cache or db
          Description description = null;
          if (descriptionCache.containsKey(fields[0])) {
            description = descriptionCache.get(fields[0]);
          } else if (existingDescriptionIds.contains(fields[0])) {
            description =
                contentService.getDescription(fields[0], terminology, version);
          }

          // verify description is found
          if (description == null
              && existingDescriptionIds.contains(fields[0])) {
            throw new Exception("** Description " + fields[0]
                + " is in existing id cache, but was not precached via concept "
                + concept.getTerminologyId());

          }

          // Setup delta description (either new or based on existing
          // one)
          Description newDescription = null;
          if (description == null) {
            newDescription = new DescriptionJpa();
          } else {
            newDescription = new DescriptionJpa(description, true);
          }
          newDescription.setConcept(concept);

          // Set fields
          newDescription.setTerminologyId(fields[0]);
          newDescription.setEffectiveTime(dt.parse(fields[1]).after(new Date())
              ? deltaLoaderStartDate : dt.parse(fields[1]));
          newDescription.setActive(fields[2].equals("1") ? true : false);
          newDescription.setModuleId(Long.valueOf(fields[3]));

          newDescription.setLanguageCode(fields[5]);
          newDescription.setTypeId(Long.valueOf(fields[6]));
          newDescription.setTerm(fields[7]);
          newDescription.setCaseSignificanceId(Long.valueOf(fields[8]));
          newDescription.setTerminology(terminology);
          newDescription.setTerminologyVersion(version);

          // If description is new, add it
          if (description == null) {
            log.info(
                "        add description " + newDescription.getTerminologyId());
            recomputePnConceptIds.add(fields[4]);
            contentService.addDescription(newDescription);
            cacheDescription(newDescription);
            objectsAdded++;
          }

          // If description has changed, update it
          else if (!newDescription.equals(description)) {
            log.info("        update description "
                + newDescription.getTerminologyId());
            recomputePnConceptIds.add(fields[4]);
            contentService.updateDescription(newDescription);
            cacheDescription(newDescription);
            objectsUpdated++;
          }

          // Otherwise, reset effective time (for modified check
          // later)
          else {
            newDescription.setEffectiveTime(description.getEffectiveTime());
          }

        }

        // Major error if there is a delta description with a
        // non-existent concept
        else {
          // skip
          log.info("SKIP DESC with concept " + fields[4]);
          continue;
          // throw new Exception("Could not find concept " + fields[4]
          // + " for Description " + fields[0]);
        }
      }
    }
    log.info("      new = " + objectsAdded);
    log.info("      updated = " + objectsUpdated);
  }

  /**
   * Load language ref set members.
   *
   * @param reader the reader
   * @throws Exception the exception
   */
  private void loadLanguageRefSetMembers(BufferedReader reader)
    throws Exception {

    // Setup variables
    String line = "";
    objectCt = 0;
    int objectsAdded = 0;
    int objectsUpdated = 0;

    // Iterate through language refset reader
    while ((line = reader.readLine()) != null) {

      // split line
      String fields[] = line.split("\t");

      // if not header
      if (!fields[0].equals("id")) {

        deltaLanguageRefSetMemberIds.add(fields[0]);

        // Get the description
        Description description = null;
        if (descriptionCache.containsKey(fields[5])) {
          description = descriptionCache.get(fields[5]);
        } else {
          description =
              contentService.getDescription(fields[5], terminology, version);
        }

        if (description == null) {
          // skip
          log.info("SKIP LANG with desc " + fields[4]);
          continue;
          // throw new Exception("Could not find description " +
          // fields[4]
          // + " for language refset member " + fields[0]);
        }

        // get the concept
        Concept concept = description.getConcept();
        // description should have concept (unless cached descriptions
        // don't
        // have them)
        if (concept == null) {
          throw new Exception(
              "Description" + fields[4] + " does not have concept");

        }

        // Cache concept and description
        cacheConcept(concept);
        cacheDescription(description);

        // Ensure effective time is set on all appropriate objects
        LanguageRefSetMember languageRefSetMember = null;
        if (languageRefSetMemberCache.containsKey(fields[0])) {
          languageRefSetMember = languageRefSetMemberCache.get(fields[0]);
          // to investigate if there will be an update
        } else if (existingLanguageRefSetMemberIds.contains(fields[0])) {
          // retrieve languageRefSetMember
          languageRefSetMember = contentService
              .getLanguageRefSetMember(fields[0], terminology, version);
        }

        if (languageRefSetMember == null
            && existingLanguageRefSetMemberIds.contains(fields[0])) {
          throw new Exception("LanguageRefSetMember " + fields[0]
              + " is in existing id cache, but was not precached via description "
              + description.getTerminologyId());

        }

        // Setup delta language entry (either new or based on existing
        // one)
        LanguageRefSetMember newLanguageRefSetMember = null;
        if (languageRefSetMember == null) {
          newLanguageRefSetMember = new LanguageRefSetMemberJpa();
        } else {
          newLanguageRefSetMember =
              new LanguageRefSetMemberJpa(languageRefSetMember, false);
        }
        newLanguageRefSetMember.setDescription(description);

        // Universal RefSet attributes
        newLanguageRefSetMember.setTerminologyId(fields[0]);
        newLanguageRefSetMember.setEffectiveTime(dt.parse(fields[1]).after(new Date())
            ? deltaLoaderStartDate : dt.parse(fields[1]));
        newLanguageRefSetMember.setActive(fields[2].equals("1") ? true : false);
        newLanguageRefSetMember.setModuleId(Long.valueOf(fields[3]));
        newLanguageRefSetMember.setRefSetId(fields[4]);
        // Language unique attributes
        newLanguageRefSetMember.setAcceptabilityId(Long.valueOf(fields[6]));
        // Terminology attributes
        newLanguageRefSetMember.setTerminology(terminology);
        newLanguageRefSetMember.setTerminologyVersion(version);

        // If language refset entry is new, add it
        if (languageRefSetMember == null) {
          log.info("        add language "
              + newLanguageRefSetMember.getTerminologyId());
          recomputePnConceptIds
              .add(description.getConcept().getTerminologyId());
          contentService.addLanguageRefSetMember(newLanguageRefSetMember);
          cacheLanguageRefSetMember(newLanguageRefSetMember);
          objectsAdded++;
        }

        // If language refset entry is changed, update it
        else if (!newLanguageRefSetMember.equals(languageRefSetMember)) {
          log.info("        update language "
              + newLanguageRefSetMember.getTerminologyId());
          recomputePnConceptIds
              .add(description.getConcept().getTerminologyId());
          contentService.updateLanguageRefSetMember(newLanguageRefSetMember);
          cacheLanguageRefSetMember(newLanguageRefSetMember);
          objectsUpdated++;
        }

        // Otherwise, reset effective time (for modified check later)
        else {
          newLanguageRefSetMember
              .setEffectiveTime(languageRefSetMember.getEffectiveTime());
        }
      }
    }

    log.info("      new = " + objectsAdded);
    log.info("      updated = " + objectsUpdated);

  }

  /**
   * Load relationships.
   *
   * @param reader the reader
   * @throws Exception the exception
   */
  private void loadRelationships(BufferedReader reader) throws Exception {

    // Setup variables
    String line = "";
    objectCt = 0;
    int objectsAdded = 0;
    int objectsUpdated = 0;

    // Iterate through relationships reader
    while ((line = reader.readLine()) != null) {

      // Split line
      String fields[] = line.split("\t");

      // If not header
      if (!fields[0].equals("id")) {
        deltaRelationshipIds.add(fields[0]);

        // Retrieve source concept
        Concept sourceConcept = null;
        Concept destinationConcept = null;
        if (conceptCache.containsKey(fields[4])) {
          sourceConcept = conceptCache.get(fields[4]);
        } else if (existingConceptCache.containsKey(fields[4])) {
          sourceConcept = contentService
              .getConcept(existingConceptCache.get(fields[4]).getId());
        } else {
          sourceConcept =
              contentService.getConcept(fields[4], terminology, version);
        }
        if (sourceConcept == null) {
          // skip
          log.info("SKIP REL with source concept " + fields[4]);
          continue;
          // throw new Exception("Relationship " + fields[0] +
          // " source concept "
          // + fields[4] + " cannot be found");
        }

        // Retrieve destination concept
        if (conceptCache.containsKey(fields[5])) {
          destinationConcept = conceptCache.get(fields[5]);
        } else if (existingConceptCache.containsKey(fields[5])) {
          destinationConcept = contentService
              .getConcept(existingConceptCache.get(fields[5]).getId());
        } else {
          destinationConcept =
              contentService.getConcept(fields[5], terminology, version);
        }
        if (destinationConcept == null) {
          // skip
          log.info("SKIP REL with destination concept " + fields[5]);
          continue;
          // throw new Exception("Relationship " + fields[0]
          // + " destination concept " + fields[5] + " cannot be
          // found");
        }

        // Cache concepts
        cacheConcept(sourceConcept);
        cacheConcept(destinationConcept);

        // Retrieve relationship
        Relationship relationship = null;
        if (relationshipCache.containsKey(fields[0])) {
          relationship = relationshipCache.get(fields[0]);
        } else if (existingRelationshipIds.contains(fields[0])) {
          relationship =
              contentService.getRelationship(fields[0], terminology, version);

        }

        // Verify cache
        if (relationship == null
            && existingRelationshipIds.contains(fields[0])) {
          throw new Exception("** Relationship " + fields[0]
              + " is in existing id cache, but was not precached via concepts "
              + sourceConcept.getTerminologyId() + " or "
              + destinationConcept.getTerminologyId());
        }

        // Setup delta relationship (either new or based on existing
        // one)
        Relationship newRelationship = null;
        if (relationship == null) {
          newRelationship = new RelationshipJpa();
        } else {
          newRelationship = new RelationshipJpa(relationship, false);
        }

        // Set fields
        newRelationship.setTerminologyId(fields[0]);
        newRelationship.setEffectiveTime(dt.parse(fields[1]).after(new Date())
            ? deltaLoaderStartDate : dt.parse(fields[1]));
        newRelationship.setActive(fields[2].equals("1") ? true : false); // active
        newRelationship.setModuleId(Long.valueOf(fields[3])); // moduleId
        newRelationship.setRelationshipGroup(Integer.valueOf(fields[6])); // relationshipGroup
        newRelationship.setTypeId(Long.valueOf(fields[7])); // typeId
        newRelationship.setCharacteristicTypeId(Long.valueOf(fields[8])); // characteristicTypeId
        newRelationship.setTerminology(terminology);
        newRelationship.setTerminologyVersion(version);
        newRelationship.setModifierId(Long.valueOf(fields[9]));
        newRelationship.setSourceConcept(sourceConcept);
        newRelationship.setDestinationConcept(destinationConcept);

        // If relationship is new, add it
        if (!existingRelationshipIds.contains(fields[0])) {
          log.info(
              "        add relationship " + newRelationship.getTerminologyId());
          contentService.addRelationship(newRelationship);
          cacheRelationship(newRelationship);
          objectsAdded++;
        }

        // If relationship is changed, update it
        else if (relationship != null
            && !newRelationship.equals(relationship)) {
          log.info("        update relationship "
              + newRelationship.getTerminologyId());
          contentService.updateRelationship(newRelationship);
          cacheRelationship(newRelationship);
          objectsUpdated++;
        }

        // Otherwise, reset effective time (for modified check later)
        else {
          if (relationship != null) {
            newRelationship.setEffectiveTime(relationship.getEffectiveTime());
          }
        }

        if (objectCt % 2000 == 0) {
          contentService.commit();
          contentService.beginTransaction();
        }
      }
    }

    log.info("      new = " + objectsAdded);
    log.info("      updated = " + objectsUpdated);

  }

  /**
   * Calculates default preferred names for any concept that has changed. Note:
   * at this time computes for concepts that have only changed due to
   * relationships, which is unnecessary
   *
   * @throws Exception the exception
   */
  private void computeDefaultPreferredNames() throws Exception {

    // Setup vars
    int dpnNotFoundCt = 0;
    int dpnFoundCt = 0;
    int dpnSkippedCt = 0;

    // Compute default preferred names for any concept in the delta
    for (String terminologyId : recomputePnConceptIds) {
      Concept concept =
          contentService.getConcept(terminologyId, terminology, version);

      // Skip if inactive
      if (!concept.isActive()) {
        dpnSkippedCt++;
        continue;
      }

      log.info("Checking concept " + concept.getTerminologyId());

      String[] result = computeDefaultPreferredName(concept);
      // Pref name not found
      if (result == null) {
        dpnNotFoundCt++;
        log.warn("Could not find defaultPreferredName for concept "
            + concept.getTerminologyId());
        concept.setDefaultPreferredName("[Could not be determined]");
      } else {
        concept.setDefaultPreferredName(result[1]);
        dpnFoundCt++;
      }
      
      // periodically comit
      if (++objectCt % 5000 == 0) {
        log.info("    count = " + objectCt);
        contentService.commit();
        contentService.clear();
        contentService.beginTransaction();
      }      
    }

    log.info("  found =  " + dpnFoundCt);
    log.info("  not found = " + dpnNotFoundCt);
    log.info("  skipped = " + dpnSkippedCt);

  }

  /**
   * Helper function to access/add to dpn set.
   *
   * @param concept the concept
   * @param dpnTypeId the dpn type id
   * @param dpnRefSetId the dpn ref set id
   * @param dpnAcceptabilityId the dpn acceptability id
   * @return the rank dpn tuple
   * @throws Exception the exception
   */
  private String[] computeDefaultPreferredName(Concept concept) throws Exception {

    if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
      return defaultPreferredNames.get(concept.getTerminologyId());
    } else {
    
      // cycle over descriptions
      for (final Description description : concept.getDescriptions()) {

        // cycle over language ref sets
        for (final LanguageRefSetMember language : description.getLanguageRefSetMembers()) {

          if (description.isActive() && language.isActive()
              && dpnRefsetIdArray.contains(Long.valueOf(language.getRefSetId()))
              && !description.getTypeId().equals(definitionTypeId)) {

            // if the description/language refset pair match any of the ranked
            // refsetId/typeId/acceptabilityId triples,
            // this is a potential defaultPrefferedName
            int index = dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId()));

            // retrieve the concept for this description
            concept = description.getConcept();
            
            // check if this concept already had a dpn stored
            if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
              String[] rankValuePair = defaultPreferredNames.get(concept.getTerminologyId());
              // if the lang refset priority is higher than the priority of the previously
              // stored dpn, replace it
              if (dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId())) > Integer
                  .parseInt(rankValuePair[0])) {
                String[] newRankValuePair = {
                    Integer.valueOf(index).toString(), description.getTerm()
                };
                defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
              }
              // if the lang refset priority is the same as the previously stored dpn, but the typeId is preferred, replace it
              if (dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId())) == Integer
                  .parseInt(rankValuePair[0])) {
                if (description.getTypeId().equals(dpnTypeId) && language.getAcceptabilityId().equals(dpnAcceptabilityId)) {
                  String[] newRankValuePair = {
                      Integer.valueOf(index).toString(), description.getTerm()
                  };
                  defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
                }
              }
            // store first potential dpn
            } else {
              String[] newRankValuePair = {
                  Integer.valueOf(index).toString(), description.getTerm()
              };
              defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
            }
          }
        }
      }
      if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
        return defaultPreferredNames.get(concept.getTerminologyId());
      } else {
        log.warn(
          "Could not retrieve default preferred name for Concept " + concept.getTerminologyId());
        return null;
      }
    }
  }
  /**
   * Retires concepts that were removed from prior deltas. Find concepts in the
   * DB that are not in the current delta and which have effective times greater
   * than the latest release date. The latest release date is the
   * "terminologyVersion" in this case.
   * 
   * NOTE: this does not handle a retraction of a change because we don't
   * preserve a static copy of the previous release to compare against. What
   * this really needs is a daily incremental delta relative to the snapshot
   * from the previous day.
   * 
   * @throws Exception
   */
  public void retireRemovedContent() throws Exception {
    // Base this algortihm on the last publication date
    // If editing resumes before last publication date
    // this will essentially do nothing until afterwards
    // which is fine, it just means some things will remain
    // in scope longer than they should.
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    Date rf2Version = dateFormat.parse(lastPublicationDate);

    // Now remove retired concepts
    // These are concepts created after rf2Version that are no longer in
    // the drip feed
    int ct = 0;
    log.info("    Retire removed concepts");
    for (Concept concept : existingConceptCache.values()) {
      if (concept.getEffectiveTime().after(rf2Version)
          && !deltaConceptIds.contains(concept.getTerminologyId())
          && concept.isActive()) {
        concept = contentService.getConcept(concept.getId());
        // Because it's possible that a concept element changed and that
        // change was retracted, we need to double-check whether all of
        // the concept elements are also new. If so, proceed. It is
        // possible
        // that ALL descriptions and relationships changed and all of
        // those
        // changes were retracted. in that case the worst thing that
        // happens
        // the record has to be remapped
        boolean proceed = true;
        for (Description description : concept.getDescriptions()) {
          if (!description.getEffectiveTime().after(rf2Version)) {
            proceed = false;
            break;
          }
        }
        if (proceed) {
          for (Relationship relationship : concept.getRelationships()) {
            if (!relationship.getEffectiveTime().after(rf2Version)) {
              proceed = false;
              break;
            }
          }
        }
        // One gap in the logic is if a concept was retired and that
        // retirement was retracted, we don't know. again, the
        // consequence
        // is that the concept will have to be remapped.

        // Retire this concept.
        if (proceed) {
          ct++;
          concept.setActive(false);
          concept.setEffectiveTime(deltaLoaderStartDate);
          contentService.updateConcept(concept);
        }
      }
    }
    log.info("      count =  " + ct);
    contentService.commit();
    contentService.clear();
    contentService.beginTransaction();

    // Also retire inferred relationships added after the last release
    // but not in the current delta. Relationships do not change
    // they are created or retired - so we likely do not need to worry
    // about retractions of changes here

    // OK, so after experimenting with this, we can't effectively identify
    // what kind of change was retracted, and so can't assume that it was
    // an addition. Every attempt to model this logic has failed because
    // we simply do not have the intermediate information
    //
    /**
     * ct = 0; log.info(" Retire removed relationships"); List<Relationship>
     * relationships =
     * contentService.getRelationshipsModifiedSinceDate(terminology,
     * rf2Version).getRelationships(); contentService.clear();
     * 
     * for (Relationship relationship : relationships) {
     * 
     * if (relationship.getEffectiveTime().after(rf2Version) &&
     * !deltaRelationshipIds.contains(relationship.getTerminologyId()) &&
     * relationship.isActive()) { log.info(" retire " +
     * relationship.getTerminologyId()); ct++; relationship.setActive(false);
     * relationship.setEffectiveTime(deltaLoaderStartDate);
     * contentService.updateRelationship(relationship); } } log.info(" count = "
     * + ct);
     **/

    contentService.commit();
    contentService.clear();
    contentService.beginTransaction();

    // Identifying the difference between a change in a description that
    // was retracted and an addition of a description that was retracted
    // is difficult and likely very error prone. Failing to properly
    // handle retractions of changes or additions has very minor effect.
    // So, it is recommended to be skipped.
    // As are retracted changes or additions of language refset member
    // entries.

  }

  // helper function to update and store concept
  // as well as putting all descendant objects in the cache
  // for easy retrieval
  /**
   * Cache concept.
   *
   * @param c the c
   */
  private void cacheConcept(Concept c) {
    if (!conceptCache.containsKey(c.getTerminologyId())) {
      for (Relationship r : c.getRelationships()) {
        relationshipCache.put(r.getTerminologyId(), r);
      }
      for (Description d : c.getDescriptions()) {
        for (LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
          languageRefSetMemberCache.put(l.getTerminologyId(), l);
        }
        descriptionCache.put(d.getTerminologyId(), d);
      }
      conceptCache.put(c.getTerminologyId(), c);
    }
  }

  /**
   * Cache description.
   *
   * @param d the d
   */
  private void cacheDescription(Description d) {

    if (!descriptionCache.containsKey(d.getTerminologyId())) {
      for (LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
        languageRefSetMemberCache.put(l.getTerminologyId(), l);
      }
      descriptionCache.put(d.getTerminologyId(), d);
    }
  }

  /**
   * Cache relationship.
   *
   * @param r the r
   */
  private void cacheRelationship(Relationship r) {
    relationshipCache.put(r.getTerminologyId(), r);
  }

  // helper function to cache and update a language ref set member
  /**
   * Cache language ref set member.
   *
   * @param l the l
   */
  private void cacheLanguageRefSetMember(LanguageRefSetMember l) {
    languageRefSetMemberCache.put(l.getTerminologyId(), l);
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
    this.requestCancel = true;
  }
}
