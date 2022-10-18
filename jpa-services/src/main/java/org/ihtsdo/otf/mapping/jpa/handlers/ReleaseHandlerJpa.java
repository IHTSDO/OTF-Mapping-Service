/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ReportFrequency;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.reports.ReportResultItemJpa;
import org.ihtsdo.otf.mapping.reports.ReportResultJpa;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * RF2 implementation of {@link ReleaseHandler}.
 */
public class ReleaseHandlerJpa implements ReleaseHandler {

  /** The mapping service. */
  private MappingService mappingService;

  /** The content service. */
  private ContentService contentService;

  /** The content service. */
  private MetadataService metadataService;

  /** The effectiveTime. */
  private String effectiveTime;

  /** The module id. */
  private String moduleId;

  /** The input file. */
  private String inputFile;

  /** The output dir. */
  private String outputDir;

  /** THe flags for writing snapshot and delta. */
  private boolean writeSnapshot = false;

  private boolean writeActiveSnapshot = false;

  /** The write delta. */
  private boolean writeDelta = false;

  /** The map project. */
  private MapProject mapProject = null;

  /** The algo handler. */
  private ProjectSpecificAlgorithmHandler algorithmHandler;

  /** The map records. */
  private List<MapRecord> mapRecords;

  /** Map of terminology id to error messages. */
  Map<String, String> conceptErrors = new HashMap<>();

  /** Map of terminology id to map record. */
  Map<String, MapRecord> mapRecordMap = new HashMap<>();

  /** The default preferred names set (terminologyId -> dpn). */
  private Map<String, String> defaultPreferredNames = new HashMap<>();

  /** The scope concepts. */
  private Map<String, Concept> conceptCache = new HashMap<>();

  /** The test mode flag. */
  private boolean testModeFlag = false;

  /** The report statistics. */
  private Map<String, Integer> reportStatistics = new HashMap<>();

  /** The date format. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /**
   * Records that will not be PUBLISHED because they've been edited during the
   * release period
   */
  private Set<Long> recentlyEditedRecords = new HashSet<>();

  /** Concept ids read in from previous release file **/
  private Set<String> conceptsFromReleaseFile = new HashSet<>();

  /** The begin log. */
  private static Logger beginLog;

  /** The process log. */
  private static Logger processLog;

  /** The preview finish log. */
  private static Logger previewFinishLog;

  /** The finish log. */
  private static Logger finishLog;

  /** The current logger. */
  private static Logger logger;

  /**
   * The Enum for statistics reporting.
   */
  private enum Stats {

    /** The active entries. */
    ACTIVE_ENTRIES("Active entries "),
    /** The concepts mapped. */
    CONCEPTS_MAPPED("Concepts mapped "),
    /** The complex maps. */
    COMPLEX_MAPS("Concepts with complex maps "),
    /** The multiple groups. */
    MULTIPLE_GROUPS("Concepts with multiple groups "),
    /** The always map. */
    ALWAYS_MAP("Concepts that always yield a target code "),
    /** The sometimes map. */
    SOMETIMES_MAP("Concepts that at least sometimes yield a target code"),
    /** The never map. */
    NEVER_MAP("Concepts that could not be mapped "),
    /** The max entries. */
    MAX_ENTRIES("Max number of map entries for a concept"),
    /** The new concepts. */
    NEW_CONCEPTS("Total New mapped concepts this release "),
    /** The new entries. */
    NEW_ENTRIES("Total New mapped entries this release "),
    /** The retired concepts. */
    RETIRED_CONCEPTS("Total Retired mapped concepts this release "),
    /** The retired entries. */
    RETIRED_ENTRIES("Total Retired mapped entries this release "),
    /** The changed concepts. */
    CHANGED_CONCEPTS("Total Changed mapped concepts this release "),
    /** The changed entries. */
    CHANGED_ENTRIES_NEW("Total Changed mapped entries (new) this release "),
    /** The changed entries. */
    CHANGED_ENTRIES_RETIRED("Total Changed mapped entries (retired) this release ");

    /** The value. */
    private String value;

    /**
     * Instantiates a {@link Stats} from the specified parameters.
     *
     * @param value the value
     */
    private Stats(String value) {
      this.value = value;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }
  }

  /**
   * Instantiates an empty {@link ReleaseHandlerJpa}.
   *
   * @param testModeFlag the test mode flag
   * @throws Exception the exception
   */
  public ReleaseHandlerJpa(boolean testModeFlag) throws Exception {

    // instantiate services
    mappingService = new MappingServiceJpa();
    contentService = new ContentServiceJpa();
    metadataService = new MetadataServiceJpa();
    this.testModeFlag = testModeFlag;

    // initialize logger - done in setMapProject()

  }

  /* see superclass */
  @Override
  public void close() throws Exception {
    mappingService.close();
    contentService.close();
    metadataService.close();
  }

  /* see superclass */
  @Override
  public void processRelease() throws Exception {
    try {
      // set the logger
      logger = processLog;
      clearLog(logger);

      logger.info("  Starting processing the release");

      // Keep track of all of the created files
      final List<String> createdFilenames = new ArrayList<>();

      // get all map records for this project
      if (mapRecords == null || mapRecords.isEmpty()) {
        final MapRecordList mapRecordList = mappingService
            .getPublishedAndReadyForPublicationMapRecordsForMapProject(mapProject.getId(), null);
        mapRecords = mapRecordList.getMapRecords();
      }

      // get all scope concept terminology ids for this project
      logger.info("  Get scope concepts for map project");
      Set<String> scopeConceptTerminologyIds = new HashSet<>();
      for (final SearchResult sr : mappingService.findConceptsInScope(mapProject.getId(), null)
          .getSearchResults()) {
        scopeConceptTerminologyIds.add(sr.getTerminologyId());
      }

      // Log config
      logger.info("  pattern = " + mapProject.getMapRefsetPattern().toString());
      logger.info("  rule-based = " + mapProject.isRuleBased());
      logger.info("  record count = " + mapRecords.size());

      // check that either/both snapshot and delta files have been specified
      if (!writeSnapshot && !writeDelta) {
        throw new Exception("processRelease called with both snapshot and delta flags disabled");
      }

      //
      // Check preconditions
      //

      // check for supported ref set pattern
      if (!EnumSet
          .of(MapRefsetPattern.ComplexMap, MapRefsetPattern.ExtendedMap, MapRefsetPattern.SimpleMap)
          .contains(mapProject.getMapRefsetPattern())) {
        throw new Exception("Unsupported map refset pattern - " + mapProject.getMapRefsetPattern());
      }

      // check that effectiveTime and moduleId have been properly specified
      if (effectiveTime == null || effectiveTime.isEmpty()) {
        throw new Exception("Effective time must be specified");
      }

      // check module id
      if (moduleId == null || moduleId.isEmpty()) {
        throw new LocalException("Module id must be specified");
      }
      boolean moduleFound = false;
      if (metadataService
          .getModules(mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion())
          .containsKey(moduleId)) {
        moduleFound = true;
      }
      // Also check the destination terminology (this is to handle bidirectional
      // maps that are both controlled by a single module id concept
      if (metadataService.getModules(mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion()).containsKey(moduleId)) {
        moduleFound = true;
      }
      // In the edge-case where the correct module Id is not associated with a
      // valid concept, allow "REMOVE_THIS*" as a module id, and strip the
      // REMOVE_THIS from the moduleId later on the process.
      if (!moduleFound && !moduleId.startsWith("REMOVE_THIS")) {
        throw new LocalException("Module id is not a valid module id " + moduleId);
      }

      if (moduleId.startsWith("REMOVE_THIS")) {
        moduleId = moduleId.replace("REMOVE_THIS", "");
      }

      // Refset id against pattern
      if (EnumSet.of(MapRefsetPattern.ComplexMap, MapRefsetPattern.ExtendedMap)
          .contains(mapProject.getMapRefsetPattern())) {
        if (!metadataService.getComplexMapRefSets(mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()).containsKey(mapProject.getRefSetId())) {
          // really, this is to support "fake" map projects
          if (!testModeFlag) {
            throw new LocalException("Map project refset id is not a valid complex map refset id "
                + mapProject.getRefSetId());
          }
        }
      } else if (EnumSet.of(MapRefsetPattern.SimpleMap)
          .contains(mapProject.getMapRefsetPattern())) {
        if (!metadataService.getSimpleMapRefSets(mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()).containsKey(mapProject.getRefSetId())) {
          // really, this is to support "fake" map projects
          if (!testModeFlag) {
            throw new Exception("Map project refset id is not a valid simple map refset id "
                + mapProject.getRefSetId());
          }
        }
      }

      // check output directory exists
      File outputDirFile = new File(outputDir);
      if (!outputDirFile.isDirectory())
        throw new Exception("Output file directory (" + outputDir + ") could not be found.");

      //
      // Compute default preferred names
      //
      logger.info("  Compute default preferred names");
      computeDefaultPreferredNames();

      // instantiate the project specific handler
      algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mapProject);

      // Write module dependency file
      Set<String> moduleDependencies = algorithmHandler.getDependentModules();
      if (moduleDependencies.size() > 0) {
        createdFilenames.add(writeModuleDependencyFile(moduleDependencies,
            algorithmHandler.getModuleDependencyRefSetId()));
      }

      //
      // Prepare data
      //

      // put all map records into the map record map
      for (final MapRecord mr : mapRecords) {
        if (mr == null) {
          throw new Exception("Null record found in published list");
        }
        // Skip out of scope records
        if (!scopeConceptTerminologyIds.contains(mr.getConceptId())) {
          continue;
        }
        mapRecordMap.put(mr.getConceptId(), mr);
      }

      // create a list from the set and sort by concept id
      // If ids are numeric, sort numerically
      // Otherwise, sort alphabetically
      logger.info("  Sorting records");
      Collections.sort(mapRecords, new Comparator<MapRecord>() {
        @Override
        public int compare(MapRecord o1, MapRecord o2) {
          try {
            Long conceptId1 = Long.parseLong(o1.getConceptId());
            Long conceptId2 = Long.parseLong(o2.getConceptId());
            return conceptId1.compareTo(conceptId2);
          } catch (NumberFormatException e) {
            String conceptId1 = o1.getConceptId();
            String conceptId2 = o2.getConceptId();
            return conceptId1.compareTo(conceptId2);
          }
        }
      });

      // Get maps
      // NOTE for simple or complex case, we get complex map records
      // and write the appropriate level of detail
      logger.info("  Retrieving maps");

      // retrieve the complex map ref set members for this project's refset id
      // This also handles simple members
      ComplexMapRefSetMemberList prevMemberList =
          contentService.getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId());

      // construct map of existing complex ref set members by UUID fields
      // this is used for comparison purposes later
      // after record processing, the remaining ref set members
      // represent those entries that are now inactive
      Map<String, ComplexMapRefSetMember> prevMembersHashMap = new HashMap<>();
      int simpleBlankTargetCt = 0;
      for (final ComplexMapRefSetMember member : prevMemberList.getComplexMapRefSetMembers()) {

        // Skip lines for SimpleMap where the map target is empty
        // These are just placeholders for managing scope
        // NOTE: if there is a need to have a simple map with blank targets
        // this could be coded in some other way, like "NOCODE" instead of
        // blank
        if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap
            && member.getMapTarget().isEmpty()) {
          simpleBlankTargetCt++;
          continue;
        }

        prevMembersHashMap.put(getHash(member), member);
      }

      // output size of each collection
      logger.info("    Cached distinct UUID-quintuples = " + prevMembersHashMap.keySet().size());
      logger
          .info("    Existing complex ref set members for project = " + prevMemberList.getCount());

      // if sizes do not match, output warning
      if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap
          && prevMembersHashMap.keySet().size() != prevMemberList.getCount()) {
        throw new Exception("UUID-quintuples count does not match refset member count");
      }

      if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap
          && (prevMembersHashMap.keySet().size() + simpleBlankTargetCt) != prevMemberList
              .getCount()) {
        throw new Exception(
            "UUID-quintuples count does not match refset member count for SimpleMap");
      }

      // clear the ref set members list (no longer used)
      prevMemberList = null;

      // /////////////////////////////////////////////////////
      // Perform the release
      // /////////////////////////////////////////////////////

      // Prep map relation to use for up propagated records
      final MapRelation ifaRuleRelation = algorithmHandler.getDefaultUpPropagatedMapRelation();
      if (mapProject.isPropagatedFlag() && ifaRuleRelation == null) {
        throw new Exception("Unable to find default map relation for up propagated records");
      }

      logger.info("  Processing release");
      // cycle over the map records marked for publishing
      int ct = 0;
      final Map<String, ComplexMapRefSetMember> activeMembersMap = new HashMap<>();
      for (final MapRecord mapRecord : mapRecords) {

        // Skip out of scope records
        if (!scopeConceptTerminologyIds.contains(mapRecord.getConceptId())) {
          continue;
        }

        // if concept has been added and removed in this editing cycle, skip
        if (contentService.getConcept(mapRecord.getConceptId(), mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) == null) {
          continue;
        }

        logger.info("    Processing record for " + mapRecord.getConceptId());

        ct++;

        // If map record is inactive, skip
        if (!contentService.getConcept(mapRecord.getConceptId(), mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()).isActive()) {
          logger.info("      Skipping inactive concept " + mapRecord.getConceptId());
          continue;
        }

        if (ct % 5000 == 0) {
          logger.info("    count = " + ct);
        }

        // instantiate map of entries by group
        // this is the object containing entries to write
        final Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

        // /////////////////////////////////////////////////////
        // Check for up-propagation
        // /////////////////////////////////////////////////////
        if (mapProject.isPropagatedFlag()
            && contentService.getDescendantConceptsCount(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion()) <= mapProject
                    .getPropagationDescendantThreshold()) {

          // Handle up propagation for this record
          if (!handleUpPropagation(mapRecord, entriesByGroup, ifaRuleRelation)) {
            // handle cases that cannot be up propagated
            continue;
          }

        } else {
          logger.debug("  DO NOT up propagate " + mapRecord.getConceptId());

        }

        // /////////////////////////////////////////////////////
        // Add the original (non-propagated) entries
        // /////////////////////////////////////////////////////
        logger.debug("     Adding original entries");

        // Sort entries by group/priority.
        Collections.sort(mapRecord.getMapEntries(), new TerminologyUtility.MapEntryComparator());

        for (MapEntry me : mapRecord.getMapEntries()) {
          logger.debug("       Adding entry " + me.getId());

          List<MapEntry> existingEntries = entriesByGroup.get(me.getMapGroup());
          if (existingEntries == null)
            existingEntries = new ArrayList<>();

          // create a new managed instance for this entry
          // necessary because an up-propagated record might attempt to access
          // the original entry -- thus do not want to modify it
          MapEntry newEntry = new MapEntryJpa();
          newEntry.setMapAdvices(me.getMapAdvices());
          newEntry.setMapGroup(me.getMapGroup());
          newEntry.setMapBlock(me.getMapBlock());
          newEntry.setMapRecord(mapRecord);
          newEntry.setRule(mapProject.isRuleBased() ? me.getRule() : "");
          newEntry.setTargetId(me.getTargetId());
          newEntry.setTargetName(me.getTargetName());

          // if not the first entry and contains TRUE rule, set to
          // OTHERWISE TRUE
          if (mapProject.isRuleBased() && existingEntries.size() > 0
              && newEntry.getRule().equals("TRUE"))
            newEntry.setRule("OTHERWISE TRUE");

          // recalculate the map relation
          newEntry.setMapRelation(algorithmHandler.computeMapRelation(mapRecord, me));

          // add to the existing entries list
          existingEntries.add(newEntry);

          // replace the previous list with the new list
          entriesByGroup.put(newEntry.getMapGroup(), existingEntries);
        }

        // /////////////////////////////////////////////////////
        // Check each group capped with TRUE or OTHERWISE TRUE
        // /////////////////////////////////////////////////////

        // only perform if project is rule based
        if (mapProject.isRuleBased()) {

          for (int mapGroup : entriesByGroup.keySet()) {

            List<MapEntry> existingEntries = entriesByGroup.get(mapGroup);

            // if no entries or last entry is not true
            if (existingEntries.size() == 0
                || !existingEntries.get(existingEntries.size() - 1).getRule().contains("TRUE")) {

              // create a new map entry
              MapEntry newEntry = new MapEntryJpa();

              // set the record and group
              newEntry.setMapRecord(mapRecord);
              newEntry.setMapGroup(mapGroup);

              // set the rule to TRUE if no entries, OTHERWISE true if
              // entries exist

              if (existingEntries.size() == 0)
                newEntry.setRule("TRUE");
              else
                newEntry.setRule("OTHERWISE TRUE");

              // compute the map relation for no target for this
              // project
              newEntry.setMapRelation(algorithmHandler.computeMapRelation(mapRecord, newEntry));

              // add the entry and replace in the entries-by-group map
              existingEntries.add(newEntry);
              entriesByGroup.put(mapGroup, existingEntries);

            }
          }
        }

        // /////////////////////////////////////////////////////
        // Convert the record to complex map ref set members
        // /////////////////////////////////////////////////////

        // get the concept
        Concept concept = conceptCache.get(mapRecord.getConceptId());
        if (concept == null) {
          logger.info("Map record exists for nonexistent concept: " + mapRecord.getConceptId()
              + " record will not be released.");
          continue;
        }
        if (!concept.isActive()) {
          throw new Exception(
              "Map record exists for inactive concept: " + mapRecord.getConceptId());
        }

        // cycle over groups and entries in sequence
        // Collect active only entries
        for (int mapGroup : entriesByGroup.keySet()) {

          int mapPriority = 1;

          for (final MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

            // convert this map entry into a complex map ref set member
            ComplexMapRefSetMember member =
                getComplexMapRefSetMemberForMapEntry(mapEntry, mapRecord, mapProject, concept);

            if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {
              // Run member through simple/complex conversion
              // This makes sure what was read from the database
              // matches for non-simple fields what was generated in
              // getComplexMapRefSetMemberForMapEntry
              member = new ComplexMapRefSetMemberJpa(new SimpleMapRefSetMemberJpa(member));
            }

            final String uuidStr = getHash(member);

            // attempt to retrieve any existing complex map ref set
            // member
            final ComplexMapRefSetMember prevMember = prevMembersHashMap.get(uuidStr);

            // if existing found, re-use uuid, otherwise generate new
            if (prevMember == null) {
              member.setTerminologyId(ConfigUtility.getReleaseUuid(uuidStr).toString());
            } else {
              member.setTerminologyId(prevMember.getTerminologyId());
            }

            // assign and increment map priority
            member.setMapPriority(mapPriority++);

            // add this entry to the list of members to write
            if (activeMembersMap.containsKey(member.getTerminologyId())) {
              logger.error(activeMembersMap.get(member.getTerminologyId()).toString());
              logger.error(member.toString());
              throw new Exception("Duplicate id found");
            }

            ValidationResult result = null;
            result = algorithmHandler.validateForRelease(member);

            if (result != null && !result.isValid()) {
              // LEt it pass if in test mode
              if (testModeFlag) {
                logger.info("      WARNING: invalid map entry: " + member);
                logger.info("        errors = " + result.getErrors());
                // continue;
              } else {
                throw new Exception("Invalid member for " + member.getConcept().getTerminologyId()
                    + " - " + result);
              }
            }

            // Skip lines for SimpleMap where the map target is empty
            // These are just placeholders for managing scope
            // NOTE: if there is a need to have a simple map with blank targets
            // this could be coded in some other way, like "NOCODE" instead of
            // blank
            if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap
                && member.getMapTarget().isEmpty()) {
              // do not add it
            }
            // else, do
            else {
              activeMembersMap.put(member.getTerminologyId(), member);
            }
          }
        }

        // clear the service -- memory management
        contentService.clear();

      }

      // /////////////////////////////////////////////////////
      // Prepare for file write
      // /////////////////////////////////////////////////////

      // declare maps in use for computation
      Map<String, ComplexMapRefSetMember> prevActiveMembersMap = new HashMap<>();
      Map<String, ComplexMapRefSetMember> prevInactiveMembersMap = new HashMap<>();

      // First, construct set of previously active complex map ref set members
      for (final ComplexMapRefSetMember member : prevMembersHashMap.values()) {
        if (member.isActive()) {
          prevActiveMembersMap.put(member.getTerminologyId(), member);
        } else {
          prevInactiveMembersMap.put(member.getTerminologyId(), member);
        }
      }

      logger.info("  prev inactive members = " + prevInactiveMembersMap.size());
      logger.info("  prev active members = " + prevActiveMembersMap.size());
      logger.info("  active members = " + activeMembersMap.size());

      // Write human readable file
      createdFilenames.add(writeHumanReadableFile(activeMembersMap));

      // Write active snapshot file
      if (writeActiveSnapshot) {
        writeActiveSnapshotFile(activeMembersMap);
      }

      // Write snapshot file
      if (writeSnapshot) {
        createdFilenames.add(writeActiveSnapshotFile(activeMembersMap));
        createdFilenames
            .add(writeSnapshotFile(prevInactiveMembersMap, prevActiveMembersMap, activeMembersMap));
      }

      // Write delta file
      if (writeDelta) {
        createdFilenames.add(writeDeltaFile(activeMembersMap, prevActiveMembersMap));
      }

      // Write statistics
      createdFilenames
          .add(writeStatsFile(prevInactiveMembersMap, activeMembersMap, prevActiveMembersMap));

      // Zip up the created files, and datestamp it.
      // Only do for 'real' releases - don't do for 'current' created by delta
      // report process
      if (!outputDir.contains("current")) {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss");
        File outputFile = new File(outputDir + "/" + mapProject.getSourceTerminology() + "_to_"
            + mapProject.getDestinationTerminology() + "_" + mapProject.getRefSetId() + "_"
            + dateFormat.format(date) + ".zip");

        zipFiles(createdFilenames, outputFile);
      }

      // write the concept errors
      logger.info("Concept errors (" + conceptErrors.keySet().size() + ")");
      for (final String terminologyId : conceptErrors.keySet()) {
        logger.info("  " + terminologyId + ": " + conceptErrors.get(terminologyId));
      }

      logger.info("  Done processing the release");

      // /////////////////////////////////////////////////////
      // Clean up
      // /////////////////////////////////////////////////////

      // close the services
      contentService.close();
      mappingService.close();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new Exception(e);
    }
  }

  /**
   * Zip files.
   *
   * @param createdFilenames the created filenames
   * @param outputFile the output file
   */
  private void zipFiles(List<String> createdFilenames, File outputFile) {
    FileOutputStream fos = null;
    ZipOutputStream zipOut = null;
    FileInputStream fis = null;
    try {
      fos = new FileOutputStream(outputFile);
      zipOut = new ZipOutputStream(new BufferedOutputStream(fos));
      for (String filePath : createdFilenames) {
        File input = new File(filePath);
        fis = new FileInputStream(input);
        ZipEntry ze = new ZipEntry(input.getName());
        System.out.println("Zipping the file: " + input.getName());
        zipOut.putNextEntry(ze);
        byte[] tmp = new byte[4 * 1024];
        int size = 0;
        while ((size = fis.read(tmp)) != -1) {
          zipOut.write(tmp, 0, size);
        }
        zipOut.flush();
        fis.close();
      }
      zipOut.close();
      System.out.println("Done... Zipped the files...");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null)
          fos.close();
      } catch (Exception ex) {

      }
    }

  }

  /**
   * Handle up propagation.
   *
   * @param mapRecord the map record
   * @param entriesByGroup the entries by group
   * @param ifaRuleRelation the ifa rule relation
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean handleUpPropagation(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entriesByGroup, MapRelation ifaRuleRelation) throws Exception {

    // /////////////////////////////////////////////////////
    // Get the tree positions for this concept
    // /////////////////////////////////////////////////////

    TreePosition treePosition = null;
    try {
      // get any tree position for this concept
      treePosition = contentService.getAnyTreePositionWithDescendants(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
      if (treePosition != null) {
        logger.debug("  Tree position: " + treePosition.getAncestorPath() + " - "
            + mapRecord.getConceptId());
      }
    } catch (Exception e) {
      throw new Exception("Error getting tree position for " + mapRecord.getConceptId());
    }

    // check if tree positions were successfully retrieved
    if (treePosition == null) {
      throw new Exception("Could not retrieve any tree position for " + mapRecord.getConceptId());
    }

    // get a list of tree positions sorted by position in hierarchy
    // (deepest-first)
    // NOTE: This list will contain the top-level/root map record
    List<TreePosition> treePositionDescendantList =
        getSortedTreePositionDescendantList(treePosition);

    // /////////////////////////////////////////////////////
    // Process up-propagated entries
    // /////////////////////////////////////////////////////

    // set of already processed concepts (may be multiple routes)
    Set<String> descendantsProcessed = new HashSet<>();

    // cycle over the tree positions again and add entries
    // note that the tree positions are in reverse order of
    // hierarchy depth
    for (final TreePosition tp : treePositionDescendantList) {

      // avoid re-rendering nodes already rendered
      if (!descendantsProcessed.contains(tp.getTerminologyId())) {
        logger.debug("  Processing descendant " + tp.getTerminologyId());

        // add this descendant to the processed list
        descendantsProcessed.add(tp.getTerminologyId());

        // skip the root level record, these entries are added
        // below, after the up-propagated entries
        if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

          // get the parent map record for this tree position
          // used to check if entries are duplicated on parent
          String parent = tp.getAncestorPath().substring(tp.getAncestorPath().lastIndexOf("~") + 1);
          MapRecord mrParent = getMapRecordForTerminologyId(parent);

          // get the map record corresponding to this specific
          // ancestor path + concept Id
          MapRecord mr = getMapRecordForTerminologyId(tp.getTerminologyId());

          if (mr != null) {

            logger.debug("     Adding entries from map record " + mr.getId() + ", "
                + mr.getConceptId() + ", " + mr.getConceptName());

            // cycle over the entries
            // TODO: this should actually compare entire groups and not just
            // entries
            // to account for embedded age/gender rules. Otherwise a partial
            // group could
            // be explicitly rendered and the logic would be wrong
            //
            // Thus if all the entries for a group match the parent, then none
            // need to be rendered, otherwise all do.

            // Sort entries by group/priority.
            Collections.sort(mr.getMapEntries(), new TerminologyUtility.MapEntryComparator());

            for (final MapEntry me : mr.getMapEntries()) {

              // get the current list of entries for this group
              List<MapEntry> existingEntries = entriesByGroup.get(me.getMapGroup());

              if (existingEntries == null) {
                existingEntries = new ArrayList<>();
              }

              // flag for whether this entry is a duplicate of
              // an existing or parent entry
              boolean isDuplicateEntry = false;

              // compare to the entries on the parent record to the current
              // entry
              // If a match is found, this entry is duplicated and does not
              // need an explicit entry
              // (this produces short-form)
              // NOTE: This uses unmodified rules
              if (mrParent != null) {
                // Sort entries by group/priority.
                Collections.sort(mrParent.getMapEntries(),
                    new TerminologyUtility.MapEntryComparator());

                for (final MapEntry parentEntry : mrParent.getMapEntries()) {
                  if (parentEntry.getMapGroup() == me.getMapGroup()
                      && parentEntry.isEquivalent(me)) {
                    isDuplicateEntry = true;
                    break;
                  }
                }
              }

              // if not a duplicate entry, add it to the map
              if (!isDuplicateEntry) {

                logger.debug("  Entry is not a duplicate of parent");
                logger.debug("    entry = " + me);

                // create new map entry to prevent
                // hibernate-managed entity modification (leave id unset)
                MapEntry newEntry = new MapEntryJpa();
                newEntry.setMapAdvices(me.getMapAdvices());
                newEntry.setMapGroup(me.getMapGroup());
                newEntry.setMapBlock(me.getMapBlock());
                newEntry.setMapRecord(mr);
                newEntry.setRule(me.getRule()); // no-op for
                // non-rule-based
                // projects
                newEntry.setTargetId(me.getTargetId());
                newEntry.setTargetName(me.getTargetName());

                // set the propagated rule for this entry
                if (mapProject.isRuleBased()) {
                  newEntry = setPropagatedRuleForMapEntry(newEntry);
                }

                // use the map relation
                // MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT | 447639009
                // except where target code is NC
                if (newEntry.getTargetId() == null || newEntry.getTargetId().isEmpty()) {
                  newEntry.setMapRelation(me.getMapRelation());
                } else {
                  newEntry.setMapRelation(ifaRuleRelation);
                }

                // add to the list
                existingEntries.add(newEntry);

                // replace existing list with modified list - unnecessary
                entriesByGroup.put(newEntry.getMapGroup(), existingEntries);

              } else {

                logger.debug("  Entry IS DUPLICATE of parent, do not write");
                logger.debug("    entry = " + me);
              }
            }
          } else {
            // do nothing: no map record for this descendant could be found
            // likely this is a scope excludes condition
          }
        }
      }
    }
    return true;
  }

  /**
   * Write module dependency file.
   *
   * @param moduleDependencies the module dependencies
   * @param refSetId the ref set id
   * @throws Exception the exception
   */
  private String writeModuleDependencyFile(Set<String> moduleDependencies, String refSetId)
    throws Exception {
    logger.info("  Write module dependency file");
    logger.info("    count = " + moduleDependencies.size());
    // Open file
    String filename = null;
    BufferedWriter writer = null;
    filename = outputDir + "/der2_ssRefset_ModuleDependencyDelta_"
        + algorithmHandler.getReleaseFile3rdElement() + "_" + effectiveTime + ".txt";
    writer = new BufferedWriter(new FileWriter(filename));

    // Write header
    writer.write(
        "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tsourceEffectiveTime\ttargetEffectiveTime"
            + "\r\n");

    // Write lines
    for (final String module : moduleDependencies) {
      String moduleStr = ConfigUtility.getUuidForString(moduleId + refSetId + module).toString()
          + "\t" + effectiveTime + "\t" + "1" + "\t" + moduleId + "\t" + refSetId + "\t" + module
          + "\t" + effectiveTime + "\t" + effectiveTime + "\r\n";
      writer.write(moduleStr);
    }

    // Close
    writer.flush();
    writer.close();

    return filename;
  }

  /**
   * Write delta.
   *
   * @param activeMembers the active members
   * @param prevActiveMembers the previous active members
   * @throws Exception the exception
   */
  private String writeDeltaFile(Map<String, ComplexMapRefSetMember> activeMembers,
    Map<String, ComplexMapRefSetMember> prevActiveMembers) throws Exception {

    // Open file and writer
    String filename = null;
    BufferedWriter writer = null;
    String pattern = getPatternForType(mapProject);
    filename = outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern() + "Delta_"
        + algorithmHandler.getReleaseFile3rdElement() + "_" + effectiveTime + ".txt";
    logger.info("  delta:  " + filename);

    // Write headers (subject to pattern)
    // writer = new BufferedWriter(new FileWriter(filename));
    writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8));

    writer.write(getHeader(mapProject));
    writer.write("\r\n");

    // Compute retired, new, and changed.. discard unchanged for delta
    Map<String, ComplexMapRefSetMember> tmpActiveMembers = new HashMap<>(activeMembers);

    logger.info("  Computing delta entries");

    // cycle over all previously active members
    for (final ComplexMapRefSetMember member : prevActiveMembers.values()) {

      // if set to write contains this previously active uuid
      if (tmpActiveMembers.containsKey(member.getTerminologyId())) {

        // if this previously active member is present (equality check) in the
        // set to be written
        if (member.equals(tmpActiveMembers.get(member.getTerminologyId()))) {

          // remove this concept from the set to be written -- unchanged
          tmpActiveMembers.remove(member.getTerminologyId());

        } else {
          // do nothing -- modified, write it
        }
      } else {
        // do nothing -- new, write it
      }
    }

    // write new or modified maps to file
    // no sorting needed here
    for (final ComplexMapRefSetMember c : tmpActiveMembers.values()) {
      writer.write(getOutputLine(c, false));
    }

    logger.info("  Writing complete.");

    // case 2: previously active no longer present
    // Copy previously active map of uuids to write into temp map
    // For each uuid in current write set
    // - check temp map for this uuid
    // - if present, remove from temp map
    // Inactivate all remaining uuids in the temp map

    tmpActiveMembers = new HashMap<>(prevActiveMembers);

    for (final String uuid : activeMembers.keySet()) {
      if (tmpActiveMembers.containsKey(uuid)) {
        tmpActiveMembers.remove(uuid);
      }

    }

    // set active to false and write inactivated complex maps
    for (final ComplexMapRefSetMember c : tmpActiveMembers.values()) {
      c.setActive(false);
      writer.write(this.getOutputLine(c, false));
      // restore active
      c.setActive(true);
    }

    logger.info("  Writing complete.");

    writer.flush();
    writer.close();

    return filename;

  }

  /**
   * Write stats file.
   *
   * @param activeMembers the active members
   * @param prevActiveMembers the prev active members
   * @throws Exception the exception
   */
  private String writeStatsFile(Map<String, ComplexMapRefSetMember> prevInactiveMembers,
    Map<String, ComplexMapRefSetMember> activeMembers,
    Map<String, ComplexMapRefSetMember> prevActiveMembers) throws Exception {

    // Gather stats
    Set<String> activeConcepts = new HashSet<>();
    Map<String, Integer> entryCount = new HashMap<>();
    Set<String> multipleEntryConcepts = new HashSet<>();
    Set<String> multipleGroupConcepts = new HashSet<>();
    Set<String> alwaysNc = new HashSet<>();
    Set<String> neverNc = new HashSet<>();
    Set<String> sometimesMap = new HashSet<>();
    for (final ComplexMapRefSetMember member : activeMembers.values()) {
      String key = member.getConcept().getTerminologyId();
      alwaysNc.add(key);
      neverNc.add(key);
      if (!entryCount.containsKey(key)) {
        entryCount.put(key, Integer.valueOf(0));
      }
      int maxCt = entryCount.get(key).intValue() + 1;
      entryCount.put(key, maxCt);
      updateStatMax(Stats.MAX_ENTRIES.getValue(), maxCt);
    }
    for (final ComplexMapRefSetMember member : activeMembers.values()) {
      String key = member.getConcept().getTerminologyId();
      activeConcepts.add(key);
      if (member.getMapPriority() > 1) {
        multipleEntryConcepts.add(key);
      }
      if (member.getMapGroup() > 1) {
        multipleGroupConcepts.add(key);
      }
      if (member.getMapTarget() == null || member.getMapTarget().isEmpty()) {
        neverNc.remove(key);
      }
      if (member.getMapTarget() != null && !member.getMapTarget().isEmpty()) {
        alwaysNc.remove(key);
        sometimesMap.add(key);
      }
    }

    Set<String> prevInactiveConcepts = new HashSet<>();
    for (final ComplexMapRefSetMember member : prevInactiveMembers.values()) {
      prevInactiveConcepts.add(member.getConcept().getTerminologyId());
    }

    Set<String> prevActiveConcepts = new HashSet<>();
    for (final ComplexMapRefSetMember member : prevActiveMembers.values()) {
      prevActiveConcepts.add(member.getConcept().getTerminologyId());
      // Some inactive members represent inactive maps, rather than inactive
      // concepts.
      // If there is also an active map associated with the concept, then the
      // concept is active
      prevInactiveConcepts.remove(member.getConcept().getTerminologyId());
    }

    updateStatMax(Stats.ACTIVE_ENTRIES.getValue(), activeMembers.size());
    updateStatMax(Stats.CONCEPTS_MAPPED.getValue(), activeConcepts.size());
    updateStatMax(Stats.COMPLEX_MAPS.getValue(), multipleEntryConcepts.size());
    updateStatMax(Stats.MULTIPLE_GROUPS.getValue(), multipleGroupConcepts.size());
    updateStatMax(Stats.ALWAYS_MAP.getValue(), neverNc.size());
    updateStatMax(Stats.SOMETIMES_MAP.getValue(), sometimesMap.size());
    updateStatMax(Stats.NEVER_MAP.getValue(), alwaysNc.size());

    // Determine count of retired concepts - previously active, now inactive
    Set<String> retiredConcepts = new HashSet<>();
    Set<String> retiredMembers = new HashSet<>();
    for (final String id : prevActiveConcepts) {
      if (!activeConcepts.contains(id)) {
        retiredConcepts.add(id);
      }
    }
    updateStatMax(Stats.RETIRED_CONCEPTS.getValue(), retiredConcepts.size());

    for (final ComplexMapRefSetMember member : prevActiveMembers.values()) {
      if (retiredConcepts.contains(member.getConcept().getTerminologyId())) {
        retiredMembers.add(member.getTerminologyId());
      }
    }
    updateStatMax(Stats.RETIRED_ENTRIES.getValue(), retiredMembers.size());

    // Determine count of new concepts - previously non-existent, now active
    Set<String> newConcepts = new HashSet<>();
    for (final String id : activeConcepts) {
      if (!prevActiveConcepts.contains(id) && !prevInactiveConcepts.contains(id)) {
        newConcepts.add(id);
      }
    }
    updateStatMax(Stats.NEW_CONCEPTS.getValue(), newConcepts.size());

    // Determine total new entries for the new concepts
    int ct = 0;
    for (final ComplexMapRefSetMember member : activeMembers.values()) {
      String key = member.getConcept().getTerminologyId();
      if (newConcepts.contains(key)) {
        ct++;
      }
    }
    updateStatMax(Stats.NEW_ENTRIES.getValue(), ct);

    // Determine count of changed concepts - previously and currently active, but change in members
    Set<String> changedConcepts = new HashSet<>();
    Set<String> changedEntriesRetired = new HashSet<>();
    Set<String> changedEntriesNew = new HashSet<>();

    for (final String key : prevActiveMembers.keySet()) {
      ComplexMapRefSetMember previousMember = prevActiveMembers.get(key);
      ComplexMapRefSetMember currentMember = activeMembers.get(key);
      if (previousMember != null && currentMember == null
          && !retiredConcepts.contains(previousMember.getConcept().getTerminologyId())) {
        changedConcepts.add(previousMember.getConcept().getTerminologyId());
        changedEntriesRetired.add(key);
      }
    }
     
    for (final String key : activeMembers.keySet()) {
      ComplexMapRefSetMember previousMember = prevActiveMembers.get(key);
      ComplexMapRefSetMember currentMember = activeMembers.get(key);
      if (previousMember == null && currentMember != null
          && !newConcepts.contains(currentMember.getConcept().getTerminologyId())) {
        changedConcepts.add(currentMember.getConcept().getTerminologyId());
        changedEntriesNew.add(key);
      }
    }

    updateStatMax(Stats.CHANGED_CONCEPTS.getValue(), changedConcepts.size());
    updateStatMax(Stats.CHANGED_ENTRIES_NEW.getValue(), changedEntriesNew.size());
    updateStatMax(Stats.CHANGED_ENTRIES_RETIRED.getValue(), changedEntriesRetired.size());

    String camelCaseName = mapProject.getDestinationTerminology().substring(0, 1)
        + mapProject.getDestinationTerminology().substring(1).toLowerCase();
    final String filename = outputDir + "/" + camelCaseName + "stats.txt";
    BufferedWriter statsWriter = new BufferedWriter(new FileWriter(filename));

    List<String> statistics = new ArrayList<>(reportStatistics.keySet());
    Collections.sort(statistics);
    for (final String statistic : statistics) {
      statsWriter.write(statistic + "\t" + reportStatistics.get(statistic) + "\r\n");
    }
    statsWriter.close();

    return filename;
  }

  /**
   * Write active snapshot file.
   *
   * @param members the members
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private String writeActiveSnapshotFile(Map<String, ComplexMapRefSetMember> members)
    throws Exception {

    logger.info("Writing active snapshot...");
    // Set pattern
    final String pattern = getPatternForType(mapProject);
    String filename = null;
    BufferedWriter writer = null;
    filename = outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern() + "ActiveSnapshot_"
        + algorithmHandler.getReleaseFile3rdElement() + "_" + effectiveTime + ".txt";

    // write headers
    logger.info("  active snapshot:  " + filename);

    // writer = new BufferedWriter(new FileWriter(filename));
    writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8));

    writer.write(getHeader(mapProject));
    writer.write("\r\n");

    // Write members
    final List<String> lines = new ArrayList<>();
    for (final ComplexMapRefSetMember member : members.values()) {
      if (!member.isActive()) {
        throw new Exception("Unexpected inactive member " + member);
      }

      // collect lines
      lines.add(getOutputLine(member, false));
    }

    // Sort lines if not simple
    if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {
      Collections.sort(lines, ConfigUtility.COMPLEX_MAP_COMPARATOR);
    }

    // Write lines
    for (final String line : lines) {
      writer.write(line);
    }

    logger.info("  Writing complete.");

    // Close
    writer.flush();
    writer.close();

    return filename;

  }

  /**
   * Write snapshot file.
   *
   * @param prevInactiveMembers the prev inactive members
   * @param prevActiveMembers the prev active members
   * @param currentActiveMembers the current active members
   * @throws Exception the exception
   */
  private String writeSnapshotFile(Map<String, ComplexMapRefSetMember> prevInactiveMembers,
    Map<String, ComplexMapRefSetMember> prevActiveMembers,
    Map<String, ComplexMapRefSetMember> currentActiveMembers) throws Exception {

    logger.info("Writing snapshot...");
    final String pattern = getPatternForType(mapProject);
    String filename = null;
    BufferedWriter writer = null;
    filename = outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern() + "Snapshot_"
        + algorithmHandler.getReleaseFile3rdElement() + "_" + effectiveTime + ".txt";

    // write headers
    logger.info("  snapshot file:  " + filename);

    // writer = new BufferedWriter(new FileWriter(filename));
    writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8));

    writer.write(getHeader(mapProject));
    writer.write("\r\n");

    final List<String> lines = new ArrayList<>();

    // Write previously inactive members that are not active now
    for (final String key : prevInactiveMembers.keySet()) {
      if (!currentActiveMembers.containsKey(key)) {
        // write out previous inactive line
        lines.add(getOutputLine(prevInactiveMembers.get(key), true));
      } else {
        // write out the current active line
        lines.add(getOutputLine(currentActiveMembers.get(key), true));
      }
    }

    // Write previous active members (changed, unchanged, or inactive)
    for (final String key : prevActiveMembers.keySet()) {
      if (!currentActiveMembers.containsKey(key)) {
        // active value is always changing here from 1 to 0,
        // so we should always write the previous member with an updated
        // effective time (e.g. "trueEffectiveTime" parameter is false)
        final ComplexMapRefSetMember member = prevActiveMembers.get(key);
        member.setActive(false);
        lines.add(getOutputLine(member, false));
        member.setActive(true);
      } else {
        final ComplexMapRefSetMember member = currentActiveMembers.get(key);
        final ComplexMapRefSetMember member2 = prevActiveMembers.get(key);
        if (member.equals(member2)) {
          // write with older effective time
          lines.add(getOutputLine(member2, true));
        } else {
          // write with newer effective time
          lines.add(getOutputLine(member, true));
        }
      }
    }

    // Write new things (things that were not in old release)
    for (final String key : currentActiveMembers.keySet()) {
      if (!prevActiveMembers.containsKey(key) && !prevInactiveMembers.containsKey(key)) {
        lines.add(getOutputLine(currentActiveMembers.get(key), true));
      }
    }

    // Sort lines
    Collections.sort(lines, ConfigUtility.COMPLEX_MAP_COMPARATOR);
    // Write lines
    for (final String line : lines) {
      writer.write(line);
    }

    logger.info("  Writing complete.");

    // Close
    writer.flush();
    writer.close();

    return filename;

  }

  /**
   * Write human readable file.
   *
   * @param members the members
   * @throws Exception the exception
   */
  private String writeHumanReadableFile(Map<String, ComplexMapRefSetMember> members)
    throws Exception {

    // Open file and writer
    String humanReadableFileName = null;
    BufferedWriter humanReadableWriter = null;
    String camelCaseName = mapProject.getDestinationTerminology().substring(0, 1)
        + mapProject.getDestinationTerminology().substring(1).toLowerCase();
    humanReadableFileName = outputDir + "/tls_" + camelCaseName + "HumanReadableMap_"
        + algorithmHandler.getReleaseFile3rdElement() + "_" + effectiveTime + ".tsv";
    humanReadableWriter =
        // new BufferedWriter(new FileWriter(humanReadableFileName));
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(humanReadableFileName),
            StandardCharsets.UTF_8));

    // Write headers (subject to pattern)
    MapRefsetPattern pattern = mapProject.getMapRefsetPattern();
    if (pattern == MapRefsetPattern.ExtendedMap) {
      if (humanReadableWriter != null) {
        humanReadableWriter.write(
            "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tmapCategoryId\tmapCategoryName\r\n");
        humanReadableWriter.flush();
      }
    } else if (pattern == MapRefsetPattern.ComplexMap) {
      if (humanReadableWriter != null) {
        humanReadableWriter.write(
            "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tcorrelationValue\r\n");
        humanReadableWriter.flush();
      }
    } else if (pattern == MapRefsetPattern.SimpleMap) {
      if (humanReadableWriter != null) {
        humanReadableWriter.write(
            "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\treferencedComponentName\tmapTarget\tmapTargetName\r\n");
        humanReadableWriter.flush();
      }
    }

    // Write entries
    List<String> lines = new ArrayList<>();
    for (final ComplexMapRefSetMember member : members.values()) {

      // get the map relation name for the human readable file
      MapRelation mapRelation = null;
      for (final MapRelation mr : mapProject.getMapRelations()) {
        if (mr.getTerminologyId() != null && member.getMapRelationId() != null
            && mr.getTerminologyId().equals(member.getMapRelationId().toString())) {
          mapRelation = mr;
        }
      }

      // get target concept, if not null for its preferred name
      Concept targetConcept = null;
      if (member.getMapTarget() != null && !member.getMapTarget().isEmpty()) {
        targetConcept = contentService.getConcept(member.getMapTarget(),
            mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
      }

      // switch line on map relation style
      String entryLine = null;
      if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
        entryLine = member.getTerminologyId() + "\t" + effectiveTime + "\t"
            + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t" + member.getRefSetId() + "\t"
            + member.getConcept().getTerminologyId() + "\t"
            + member.getConcept().getDefaultPreferredName() + "\t" + member.getMapGroup() + "\t"
            + member.getMapPriority() + "\t" + (mapProject.isRuleBased() ? member.getMapRule() : "")
            + "\t" + member.getMapAdvice() + "\t"
            + (member.getMapTarget() == null ? "" : member.getMapTarget()) + "\t"
            + (targetConcept != null ? targetConcept.getDefaultPreferredName() : "") + "\t"
            + "447561005" + "\t" // fixed value for Extended map
            + member.getMapRelationId() + "\t"
            + (mapRelation != null ? mapRelation.getName() : "FAILED MAP RELATION");

        // ComplexMap style is identical to ExtendedMap
        // with the exception of the terminating map relation terminology id
      } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
        entryLine = member.getTerminologyId() // the UUID
            + "\t" + effectiveTime + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t"
            + member.getRefSetId() + "\t" + member.getConcept().getTerminologyId() + "\t"
            + member.getConcept().getDefaultPreferredName() + "\t" + member.getMapGroup() + "\t"
            + member.getMapPriority() + "\t" + (mapProject.isRuleBased() ? member.getMapRule() : "")
            + "\t" + member.getMapAdvice() + "\t" + member.getMapTarget() + "\t"
            + (targetConcept != null ? targetConcept.getDefaultPreferredName() : "") + "\t"
            + member.getMapRelationId() + "\t"
            + (mapRelation != null ? mapRelation.getName() : "FAILED MAP RELATION");
      }

      // Simple
      else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {
        entryLine = member.getTerminologyId() // the UUID
            + "\t" + effectiveTime + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t"
            + member.getRefSetId() + "\t" + member.getConcept().getTerminologyId() + "\t"
            + member.getConcept().getDefaultPreferredName() + "\t" + member.getMapTarget() + "\t"
            + (targetConcept != null ? targetConcept.getDefaultPreferredName() : "");
      }

      entryLine += "\r\n";
      lines.add(entryLine);
    }
    // Sort lines
    Collections.sort(lines, ConfigUtility.TSV_COMPARATOR);
    // Write file
    for (final String line : lines) {
      humanReadableWriter.write(line);
    }

    // Close
    humanReadableWriter.flush();
    humanReadableWriter.close();

    return humanReadableFileName;

  }

  /**
   * Helper function to retrieve a map record for a given tree position. If in
   * set, returns that map record, if not, retrieves and adds it if possible.
   *
   * @param terminologyId the terminology id
   * @return the map record for terminology id
   * @throws Exception the exception
   */
  private MapRecord getMapRecordForTerminologyId(String terminologyId) throws Exception {

    // if in cache, use cached records
    if (mapRecordMap.containsKey(terminologyId)) {
      return mapRecordMap.get(terminologyId);

    } else {

      MapRecord mapRecord = null;
      // if not in cache yet, get record(s) for this concept
      MapRecordList mapRecordList =
          mappingService.getMapRecordsForProjectAndConcept(mapProject.getId(), terminologyId);

      // check number of records retrieved for erroneous
      // states
      if (mapRecordList.getCount() == 0) {

        // if on excluded list, add to errors to output
        if (mapProject.getScopeExcludedConcepts().contains(terminologyId)) {
          // This is an acceptable condition to have and report
          conceptErrors.put(terminologyId,
              "  Concept referenced, but on excluded list for project");
          // if not found, add to errors to output
        } else {
          // if it cannot be found and is not on scope excluded list
          // this is a serious error and the map file could be wrong without it.

          // If in test mode, allow this to not be the case
          if (testModeFlag) {
            return null;
          }
          throw new Exception("Unable to find map record for " + terminologyId);
        }
      } else if (mapRecordList.getCount() > 1) {
        // If in test mode, allow this to be the case
        if (testModeFlag) {
          return null;
        }
        throw new Exception("Multiple map records found for " + terminologyId);

      } else {
        mapRecord = mapRecordList.getMapRecords().iterator().next();

        // if ready for publication, add to map
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          // add to map record map and return it
          mapRecordMap.put(terminologyId, mapRecord);
          return mapRecord;
        } else {
          if (testModeFlag) {
            return null;
          }
          throw new Exception("Invalid workflow status " + mapRecord.getWorkflowStatus()
              + " on record for " + terminologyId);
        }
      }

    }

    return null;
  }

  /**
   * Helper function to get a map key identifier for a complex map Used to
   * determine whether an entry exists in set.
   *
   * @param c the c
   * @return the string
   */
  @SuppressWarnings("static-method")
  private String getHash(ComplexMapRefSetMember c) {
    return c.getRefSetId() + c.getConcept().getTerminologyId() + c.getMapGroup()
        + (c.getMapRule() == null ? "" : c.getMapRule())
        + (c.getMapTarget() == null ? "" : c.getMapTarget());
  }

  /**
   * Returns the hash.
   *
   * @param entry the entry
   * @return the hash
   */
  private String getHash(MapEntry entry) {
    return mapProject.getRefSetId() + entry.getMapRecord().getConceptId() + entry.getMapGroup()
        + (entry.getRule() == null ? "" : entry.getRule())
        + (entry.getTargetId() == null ? "" : entry.getTargetId());
  }

  /** The relations. */
  private Map<Long, MapRelation> relations = null;

  /** The advices. */
  private List<MapAdvice> advices = null;

  /**
   * Returns the map entry for complex map ref set member.
   *
   * @param member the member
   * @return the map entry for complex map ref set member
   * @throws Exception the exception
   */
  private MapEntry getMapEntryForComplexMapRefSetMember(ComplexMapRefSetMember member)
    throws Exception {
    if (relations == null) {
      relations = new HashMap<>();
      for (MapRelation m : mappingService.getMapRelations().getMapRelations()) {
        relations.put(Long.valueOf(m.getTerminologyId()), m);
      }

    }
    if (advices == null) {
      advices = mappingService.getMapAdvices().getMapAdvices();
    }
    final MapEntry entry = new MapEntryJpa();
    for (MapAdvice advice : advices) {
      if (member.getMapAdvice() != null && member.getMapAdvice().contains(advice.getName())) {
        entry.addMapAdvice(advice);
      }
    }

    entry.setRule(member.getMapRule());
    entry.setMapBlock(member.getMapBlock());
    entry.setMapGroup(member.getMapGroup());
    entry.setMapPriority(member.getMapPriority());
    entry.setMapRelation(relations.get(member.getMapRelationId()));
    entry.setTargetId(member.getMapTarget());

    Concept concept = contentService.getConcept(member.getMapTarget(),
        mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
    if (concept != null) {
      entry.setTargetName(concept.getDefaultPreferredName());
    }

    return entry;
  }

  /**
   * Convert a map entry to a complex map ref set member. Does not set effective
   * time.
   *
   * @param mapEntry the map entry
   * @param mapRecord the map record
   * @param mapProject the map project
   * @param concept the concept
   * @return the complex map ref set member
   * @throws Exception the exception
   */
  private ComplexMapRefSetMember getComplexMapRefSetMemberForMapEntry(MapEntry mapEntry,
    MapRecord mapRecord, MapProject mapProject, Concept concept) throws Exception {

    ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

    // set the base parameters
    // NOTE: do not set UUID here, done in main logic
    complexMapRefSetMember.setConcept(concept);
    complexMapRefSetMember.setRefSetId(mapProject.getRefSetId());
    complexMapRefSetMember.setModuleId(Long.valueOf(moduleId));
    complexMapRefSetMember.setActive(true);
    complexMapRefSetMember.setEffectiveTime(dateFormat.parse(effectiveTime));
    complexMapRefSetMember.setTerminology(mapProject.getSourceTerminology());
    complexMapRefSetMember.setTerminologyVersion(mapProject.getSourceTerminologyVersion());

    // set parameters from the map entry
    complexMapRefSetMember.setMapGroup(mapEntry.getMapGroup());
    complexMapRefSetMember.setMapPriority(mapEntry.getMapPriority());
    complexMapRefSetMember.setMapRule(mapProject.isRuleBased() ? mapEntry.getRule() : "");
    if (mapEntry.getMapRelation() != null) {
      complexMapRefSetMember
          .setMapRelationId(Long.valueOf(mapEntry.getMapRelation().getTerminologyId()));
    }
    complexMapRefSetMember
        .setMapTarget(mapEntry.getTargetId() == null ? "" : mapEntry.getTargetId());

    /**
     * Set the map advice from the advices on the entry.
     * 
     * First, get the human readable map advice. Second, add the attached map
     * advices. Third, add to advice based on target/relation and rule - If the
     * map target is blank, advice contains the map relation name - If it's an
     * IFA rule (gender), add MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT FOR
     * GENDER - If it's an IFA rule (age/upproagated), add MAP OF SOURCE CONCEPT
     * IS CONTEXT DEPENDENT
     */

    // extract all advices and add to a list
    List<String> sortedAdvices = new ArrayList<>();

    for (final MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
      sortedAdvices.add(mapAdvice.getDetail());
    }

    // check for context dependent advice
    if (mapEntry.getRule() != null && mapEntry.getRule().startsWith("IFA")
        && mapEntry.getTargetId() != null && !mapEntry.getTargetId().isEmpty()) {

      // if not a gender rule, add the advice
      if (!mapEntry.getRule().contains("| Male (finding) |")
          && !mapEntry.getRule().contains("| Female (finding) |")) {
        sortedAdvices.add("MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT");

        // if a gender rule, add the advice
      } else if (mapEntry.getRule().contains("| Male (finding) |")
          || mapEntry.getRule().contains("| Female (finding) |")) {
        sortedAdvices.add("MAP IS CONTEXT DEPENDENT FOR GENDER");
      }
    }

    String mapAdviceStr = getHumanReadableMapAdvice(mapEntry);

    // sort unique advices and add them
    sortedAdvices = new ArrayList<>(new HashSet<>(sortedAdvices));
    Collections.sort(sortedAdvices);
    for (final String advice : sortedAdvices) {
      mapAdviceStr += (mapAdviceStr.length() != 0 ? " | " : "") + advice;
    }

    complexMapRefSetMember.setMapAdvice(mapAdviceStr);

    return complexMapRefSetMember;
  }

  /**
   * Function to construct propagated rule for an entry.
   * 
   * @param mapEntry the map entry
   * @return the map entry
   */
  private MapEntry setPropagatedRuleForMapEntry(MapEntry mapEntry) {

    MapRecord mapRecord = mapEntry.getMapRecord();

    // construct propagated rule based on concept id and name
    // e.g. for TRUE rule
    // IFA 104831000119109 | Drug induced central sleep apnea
    //
    // for age rule
    // IFA 104831000119109 | Drug induced central sleep apnea
    // (disorder) | AND IFA 445518008 | Age at onset of clinical finding
    // (observable entity) | <= 28.0 days
    // (disorder)
    String rule = "IFA " + mapRecord.getConceptId() + " | "
        + defaultPreferredNames.get(mapRecord.getConceptId()) + " |";

    // if an age or gender rule, append the existing rule
    if (!mapEntry.getRule().contains("TRUE")) {
      rule += " AND " + mapEntry.getRule();
    }

    // set the rule
    mapEntry.setRule(rule);
    /*
     * Logger.getLogger(getClass()) .info("       Set rule to " + rule);
     */
    return mapEntry;
  }

  /**
   * Gets the human readable map advice.
   *
   * @param mapEntry the map entry
   * @return the human readable map advice
   * @throws Exception the exception
   */
  private String getHumanReadableMapAdvice(MapEntry mapEntry) throws Exception {

    String advice = "";

    // Construct advice only if using Extended Map pattern
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {

      logger.debug("  RULE: " + mapEntry.getRule());

      String[] comparatorComponents; // used for parsing age rules

      // if map target is blank use map relation
      if (mapEntry.getTargetId() == null || mapEntry.getTargetId().equals("")) {
        return mapEntry.getMapRelation().getName();
      }

      // Split rule on "AND IF" conditions
      int ct = 0;
      for (String part : mapEntry.getRule().toUpperCase().split(" AND IF")) {
        ct++;
        if (ct > 1) {
          // Put the "if" back in
          part = "IF" + part;
          // Add an AND clause
          advice += " AND ";
        }
        logger.debug("    PART : " + part);

        // if map rule is IFA (age)
        if (part.contains("AGE AT ONSET OF CLINICAL FINDING")
            || part.contains("CURRENT CHRONOLOGICAL AGE")) {

          // IF AGE AT ONSET OF
          // CLINICAL FINDING BETWEEN 1.0 YEAR AND 18.0 YEARS CHOOSE
          // M08.939

          // Rule examples
          // IFA 445518008 | Age at onset of clinical finding (observable
          // entity) | < 65 years
          // IFA 445518008 | Age at onset of clinical finding (observable
          // entity) | <= 28.0 days

          // split by pipe (|) character. Expected fields
          // 0: ageConceptId
          // 1: Age rule type (Age at onset, Current chronological age)
          // 2: Comparator, Value, Units (e.g. < 65 years)
          String[] ruleComponents = part.split("\\|");

          // add the type of age rule
          advice += "IF " + prepTargetName(part);

          comparatorComponents = ruleComponents[2].trim().split(" ");

          // add appropriate text based on comparator
          switch (comparatorComponents[0]) {
            case ">":
              advice += " AFTER";
              break;
            case "<":
              advice += " BEFORE";
              break;
            case ">=":
              advice += " ON OR AFTER";
              break;
            case "<=":
              advice += " ON OR BEFORE";
              break;
            default:
              throw new Exception("Illgal operator: " + comparatorComponents[0]);
          }

          // add the value and units
          advice += " " + comparatorComponents[1] + " " + comparatorComponents[2];
        }
        // if a gender rule (i.e. contains (FE)MALE)
        else if (part.contains("| MALE (FINDING)") || part.contains("| FEMALE (FINDING)")) {

          // add the advice based on gender
          if (part.contains("| FEMALE (FINDING)")) {
            advice += "IF FEMALE";
          } else {
            advice += "IF MALE";
          }
        } // if not an IFA rule (i.e. TRUE, OTHERWISE TRUE), simply return
          // ALWAYS
        else if (!part.contains("IFA")) {

          advice = "ALWAYS " + mapEntry.getTargetId();

        }
        // Handle regular ifa
        else if (part.contains("IFA")) {
          String targetName = prepTargetName(part);
          advice += "IF " + targetName;
        }
      }

      // finally, add the CHOOSE {targetId}
      if (!advice.startsWith("ALWAYS")) {
        advice += " CHOOSE " + mapEntry.getTargetId();
      }

      logger.debug("    ADVICE: " + advice);
    }

    return advice;

  }

  /**
   * Prep target name.
   *
   * @param rule the rule
   * @return the string
   */
  @SuppressWarnings("static-method")
  private String prepTargetName(String rule) {
    String[] ifaComponents = rule.split("\\|");

    // remove any (disorder), etc.
    String targetName = ifaComponents[1].trim();

    // if classifier (e.g. (disorder)) present, remove it and any trailing
    // spaces
    if (targetName.lastIndexOf("(") != -1)
      targetName = targetName.substring(0, targetName.lastIndexOf("(")).trim();
    return targetName;
  }

  /**
   * Takes a tree position graph and converts it to a sorted list of tree
   * positions where order is based on depth in tree.
   * 
   * @param tp the tp
   * @return the sorted tree position descendant list
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private List<TreePosition> getSortedTreePositionDescendantList(TreePosition tp) throws Exception {

    // construct list of unprocessed tree positions and initialize with root
    // position
    List<TreePosition> positionsToAdd = new ArrayList<>();
    positionsToAdd.add(tp);

    List<TreePosition> sortedTreePositionDescendantList = new ArrayList<>();

    while (!positionsToAdd.isEmpty()) {

      // add the first element
      sortedTreePositionDescendantList.add(positionsToAdd.get(0));

      // add the children of first element
      for (final TreePosition childTp : positionsToAdd.get(0).getChildren()) {
        positionsToAdd.add(childTp);
      }

      // remove the first element
      positionsToAdd.remove(0);
    }

    // sort the tree positions by position in the hierarchy (e.g. # of ~
    // characters)
    Collections.sort(sortedTreePositionDescendantList, new Comparator<TreePosition>() {
      @Override
      public int compare(TreePosition tp1, TreePosition tp2) {
        int levels1 =
            tp1.getAncestorPath().length() - tp1.getAncestorPath().replace("~", "").length();
        int levels2 =
            tp1.getAncestorPath().length() - tp1.getAncestorPath().replace("~", "").length();

        // if first has more ~'s than second, it is considered
        // LESS than the second
        // i.e. this is a reverse sort
        return levels2 - levels1;
      }
    });

    return sortedTreePositionDescendantList;
  }

  /**
   * Returns the machine readable textfor complex map ref set member.
   *
   * @param member the complex map ref set member
   * @param trueEffectiveTimeFlag the true effective time flag
   * @return the machine readable textfor complex map ref set member
   * @throws Exception the exception
   */
  private String getOutputLine(ComplexMapRefSetMember member, boolean trueEffectiveTimeFlag)
    throws Exception {

    String entryLine = "";

    // switch line on map relation style
    if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
      entryLine = member.getTerminologyId() // the UUID
          + "\t"
          + ((trueEffectiveTimeFlag
              && !dateFormat.format(member.getEffectiveTime()).equals("100070607"))
                  ? dateFormat.format(member.getEffectiveTime()) : effectiveTime)
          + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t" + member.getRefSetId()
          + "\t" + member.getConcept().getTerminologyId() + "\t" + member.getMapGroup() + "\t"
          + member.getMapPriority() + "\t" + (mapProject.isRuleBased() ? member.getMapRule() : "")
          + "\t" + member.getMapAdvice() + "\t" + member.getMapTarget() + "\t" + "447561005" + "\t"
          + member.getMapRelationId();

    }

    // ComplexMap style is identical to ExtendedMap
    // with the exception of the terminating map relation terminology id
    else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
      entryLine = member.getTerminologyId() // the UUID
          + "\t"
          + ((trueEffectiveTimeFlag
              && !dateFormat.format(member.getEffectiveTime()).equals("100070607"))
                  ? dateFormat.format(member.getEffectiveTime()) : effectiveTime)
          + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t" + member.getRefSetId()
          + "\t" + member.getConcept().getTerminologyId() + "\t" + member.getMapGroup() + "\t"
          + member.getMapPriority() + "\t" + member.getMapRule() + "\t" + member.getMapAdvice()
          + "\t" + member.getMapTarget() + "\t" + member.getMapRelationId();

      // Simple map
    } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {

      // For simple map, avoid writing entries with blank maps
      // these are placeholders to better manage scope.
      if (member.getConcept() == null || member.getConcept().getTerminology() == null
          || member.getConcept().getTerminology().isEmpty()) {
        return "";
      }
      if (!mapProject.getReverseMapPattern()) {
        entryLine = member.getTerminologyId() // the UUID
            + "\t"
            + ((trueEffectiveTimeFlag && !dateFormat
                .format(member.getEffectiveTime()).equals("100070607"))
                    ? dateFormat.format(member.getEffectiveTime())
                    : effectiveTime)
            + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t"
            + member.getRefSetId() + "\t"
            + member.getConcept().getTerminologyId() + "\t"
            + member.getMapTarget();
      } else {
        entryLine = member.getTerminologyId() // the UUID
            + "\t"
            + ((trueEffectiveTimeFlag && !dateFormat
                .format(member.getEffectiveTime()).equals("100070607"))
                    ? dateFormat.format(member.getEffectiveTime())
                    : effectiveTime)
            + "\t" + (member.isActive() ? "1" : "0") + "\t" + moduleId + "\t"
            + member.getRefSetId() + "\t" + member.getMapTarget() + "\t"
            + member.getConcept().getTerminologyId();
      }
    }

    entryLine += "\r\n";
    return entryLine;

  }

  /**
   * Compute default preferred names.
   *
   * @throws Exception the exception
   */
  private void computeDefaultPreferredNames() throws Exception {

    // get the config properties for default preferred name variables
    // set the dpn variables and instantiate the concept dpn map
    Properties properties = ConfigUtility.getConfigProperties();

    String dpnTypeId = properties.getProperty("loader.defaultPreferredNames.typeId");
    String dpnRefSetId = properties.getProperty("loader.defaultPreferredNames.refSetId");
    String dpnAcceptabilityId =
        properties.getProperty("loader.defaultPreferredNames.acceptabilityId");

    // Compute preferred names
    int ct = 0;
    for (final MapRecord mapRecord : mapRecords) {
      ct++;
      Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // Concept is referenced that is not in th erelease
      if (concept == null) {
        logger.info("Unexpected concept id reference that does not exist - "
            + mapRecord.getConceptId() + ", " + mapProject.getSourceTerminology() + ", "
            + mapProject.getSourceTerminologyVersion());
        continue;
      }
      conceptCache.put(concept.getTerminologyId(), concept);
      if (testModeFlag) {
        defaultPreferredNames.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
      } else {
        defaultPreferredNames.put(concept.getTerminologyId(),
            computeDefaultPreferredName(concept, dpnTypeId, dpnRefSetId, dpnAcceptabilityId));
      }
      if (ct % 5000 == 0) {
        logger.info("    count = " + ct);
      }
    }

  }

  /**
   * Helper function to access/add to dpn set.
   *
   * @param concept the concept
   * @param dpnTypeId the dpn type id
   * @param dpnRefSetId the dpn ref set id
   * @param dpnAcceptabilityId the dpn acceptability id
   * @return the string
   * @throws Exception the exception
   */
  private String computeDefaultPreferredName(Concept concept, String dpnTypeId, String dpnRefSetId,
    String dpnAcceptabilityId) throws Exception {

    if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
      return defaultPreferredNames.get(concept.getTerminologyId());
    } else {

      // cycle over descriptions
      for (final Description description : concept.getDescriptions()) {

        // if active and type id matches
        if (description.isActive() && description.getTypeId().equals(Long.valueOf(dpnTypeId))) {

          // cycle over language ref sets
          for (final LanguageRefSetMember language : description.getLanguageRefSetMembers()) {

            if (language.getRefSetId().equals(dpnRefSetId) && language.isActive()
                && language.getAcceptabilityId().equals(Long.valueOf(dpnAcceptabilityId))) {

              defaultPreferredNames.put(concept.getTerminologyId(), description.getTerm());

              // Report info if semantic tag cannot be found
              if (!description.getTerm().trim().endsWith(")")) {
                logger.warn("Could not find semantic tag for concept " + concept.getTerminologyId()
                    + ", name selected=" + description.getTerm());
                for (final Description d : concept.getDescriptions()) {
                  logger.warn("Description " + d.getTerminologyId() + ", active=" + d.isActive()
                      + ", typeId = " + d.getTypeId());
                  for (final LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
                    logger.warn("    Language Refset Member " + l.getTerminologyId() + ", active = "
                        + l.isActive() + ", refsetId=" + l.getRefSetId() + ", acceptabilityId = "
                        + l.getAcceptabilityId());
                  }
                }
              }

              return description.getTerm();
            }

          }
        }
      }
      throw new Exception(
          "Could not retrieve default preferred name for Concept " + concept.getTerminologyId());
    }
  }

  /* see superclass */
  @Override
  public void beginRelease() throws Exception {
    try {

      logger = beginLog;
      clearLog(logger);

      logger.info("  Starting begin release");

      // instantiate required services
      final MappingService mappingService = new MappingServiceJpa();
      final ReportService reportService = new ReportServiceJpa();
      reportService.setTransactionPerOperation(false);
      reportService.beginTransaction();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();

      // Check preconditions
      // If there are "PUBLISHED" map entries, require
      // either "simple" or "complex" map refset members to exist
      if (mappingService
          .findMapRecordsForQuery(
              "mapProjectId:" + mapProject.getId() + " AND workflowStatus:PUBLISHED", null)
          .getSearchResults().size() > 0) {
        final ContentService contentService = new ContentServiceJpa();

        try {
          if (contentService.getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId())
              .getCount() == 0) {
            throw new LocalException("Map has published records but no refset member entries. "
                + "Reload previous release version file into refset table");
          }
        } catch (Exception e) {
          throw e;
        } finally {
          contentService.close();
        }
      }

      // get the report definition
      logger.info("  Create release QA report");
      ReportDefinition reportDefinition = null;
      for (final ReportDefinition rd : mapProject.getReportDefinitions()) {
        if (rd.getName().equals("Release QA"))
          reportDefinition = rd;
      }
      if (reportDefinition == null) {
        mappingService.close();
        reportService.close();
        throw new Exception("Could not get report definition matching 'Release QA'");
      }
      // create the report QA object and instantiate fields
      final Report report = new ReportJpa();
      report.setActive(true);
      report.setAutoGenerated(false);
      report.setDiffReport(false);
      report.setMapProjectId(mapProject.getId());
      report.setName(reportDefinition.getName());
      report.setOwner(mappingService.getMapUser("qa"));
      report.setQuery("No query -- constructed by services");
      report.setQueryType(ReportQueryType.NONE);
      report.setReportDefinition(reportDefinition);
      report.setResultType(ReportResultType.CONCEPT);
      report.setTimestamp((new Date()).getTime());
      reportService.addReport(report);

      // get all scope concept terminology ids for this project
      logger.info("  Get scope concepts for map project");
      final Set<String> scopeConceptTerminologyIds = new HashSet<>();
      for (final SearchResult sr : mappingService.findConceptsInScope(mapProject.getId(), null)
          .getSearchResults()) {
        scopeConceptTerminologyIds.add(sr.getTerminologyId());
      }

      logger.info("    count = " + scopeConceptTerminologyIds.size());

      // get all map records for this project
      logger.info("  Get records for map project");
      final MapRecordList mapRecords =
          mappingService.getMapRecordsForMapProject(mapProject.getId());

      logger.info("    count = " + mapRecords.getCount());

      // create a temp set of scope terminology ids
      Set<String> conceptsWithNoRecord = new HashSet<>(scopeConceptTerminologyIds);
      final List<MapRecord> mapRecordsToProcess = mapRecords.getMapRecords();

      // create a temp set of concept ids for which a map record exists
      // (irrespective of scope
      final Map<String, Integer> conceptMapRecordCountMap = new HashMap<>();

      // get all mapping refset members for this project
      final Map<Long, List<ComplexMapRefSetMember>> refsetMemberMap = new HashMap<>();
      for (ComplexMapRefSetMember member : contentService
          .getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId())
          .getComplexMapRefSetMembers()) {
        List<ComplexMapRefSetMember> list = refsetMemberMap.get(member.getConcept().getId());
        if (list == null) {
          list = new ArrayList<>();
        }

        // Don't include up-propagated refset member entries
        // Skip concept exclusion rules
        if (member.getMapRule() != null && member.getMapRule().matches("IFA.*")) {
          if (member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
              && !member.getMapRule().contains("AND IFA")) {
            // unless simple gender rule, then keep
          } else if (member.getMapRule().matches(
              "IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*AND IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*")
              && !member.getMapRule().matches(".*AND IFA.*AND IFA.*")) {
            // unless 2-part age rule, then keep
          } else if (member.getMapRule().matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*")
              && !member.getMapRule().contains("AND IFA")) {
            // unless simple age rule without compund clause, then keep
          } else {
            // else skip
            Logger.getLogger(MappingServiceJpa.class)
                .debug("    Skipping refset member exclusion rule " + member.getTerminologyId());
            continue;
          }
        }

        list.add(member);
        refsetMemberMap.put(member.getConcept().getId(), list);
      }

      // for each map record, check for errors
      // NOTE: Report Result names are constructed from error lists assigned
      // Each individual result is stored as a Report Result Item
      logger.info("  Validate records");
      boolean errorFlag = false;
      int pubCt = 0;
      while (mapRecordsToProcess.size() != 0) {

        // extract the concept and remove it from list
        final MapRecord mapRecord = mapRecordsToProcess.get(0);
        mapRecordsToProcess.remove(0);
        logger
            .debug("    concept = " + mapRecord.getConceptId() + " " + mapRecord.getConceptName());

        // first, remove this concept id from the dynamic conceptsWithNoRecord
        // set
        conceptsWithNoRecord.remove(mapRecord.getConceptId());

        // instantiate or increment the number of map records for this concept
        // id
        // NOTE: Only for published/ready for publication
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {

          if (!conceptMapRecordCountMap.containsKey(mapRecord.getConceptId())) {
            conceptMapRecordCountMap.put(mapRecord.getConceptId(), 1);
          } else {
            conceptMapRecordCountMap.put(mapRecord.getConceptId(),
                conceptMapRecordCountMap.get(mapRecord.getConceptId()) + 1);
          }

        }

        // constuct a list of errors for this concept
        final List<String> resultMessages = new ArrayList<>();

        // CHECK: One to one project record has unique mapping
        if (algorithmHandler.isOneToOneConstrained()) {

          // check for violation of target codes
          if (algorithmHandler.recordViolatesOneToOneConstraint(mapRecord)) {
            resultMessages
                .add(mapProject.getDestinationTerminology() + " target used more than once");
          }

          // check for than one entry
          if (mapRecord.getMapEntries().size() > 1) {
            resultMessages.add("Map record has more than one entry");
          }

        }

        // CHECK: Map record is READY_FOR_PUBLICATION or PUBLISHED
        if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
            && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          resultMessages.add("Map record not marked ready for publication");
          errorFlag = true;
          // if record is ready for publication
        } else {
          // Make sure map entries are sorted by by mapGroup/mapPriority
          Collections.sort(mapRecord.getMapEntries(), new TerminologyUtility.MapEntryComparator());

          // CHECK: Map record (must be ready for publication) passes project
          // specific validation checks
          ValidationResult result = algorithmHandler.validateRecord(mapRecord);
          if (!result.isValid()) {
            logger.debug("    FAILED");
            errorFlag = true;
            resultMessages.add("Map record failed validation check");
          } else {
            pubCt++;
          }
        }

        // Check for out of scope map records
        if (!scopeConceptTerminologyIds.contains(mapRecord.getConceptId())) {

          // construct message based on whether record is to be removed
          String reportMsg = mapProject.getSourceTerminology() + " concept not in scope";

          // separate error-type by previously-published or this-cycle-edited
          if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
            resultMessages.add(reportMsg + " - previously published");
          } else {
            resultMessages.add(reportMsg + " - edited this cycle");
          }
        }

        //
        // Concept and refset integrity checks
        //

        final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

        // 1. Mapped concepts that are inactive in current SNOMED release
        // 2. Mapped concepts not in snomed (e.g. because of drip feed issues)//
        if (concept == null) {
          addReportError(report, mapProject, mapRecord.getConceptId(), mapRecord.getConceptName(),
              mapProject.getSourceTerminology() + " concept does not exist");
          continue;
        } else if (!concept.isActive()) {
          resultMessages.add(mapProject.getSourceTerminology() + " concept inactive");
        }

        // Check: destination concepts missing

        Concept conceptCheck = null;
        for (MapEntry me : mapRecord.getMapEntries()) {
          conceptCheck =
              contentService.getConcept(me.getTargetId(), mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
          // exclude map records with empty target IDs from error checks
          if (!me.getTargetId().isEmpty() && conceptCheck == null) {
            logger.info("missing destination = " + me);
            addReportError(report, mapProject, me.getTargetId(), "Concept not found",
                "destination concept doesn't exist");
          }
        }

        // Check: Destination terminology codes NOT used in previous version of
        // the map
        final Set<String> unusedTargetCodes = new HashSet<>();
        if (refsetMemberMap.containsKey(concept.getId())) {
          final List<ComplexMapRefSetMember> members = refsetMemberMap.get(concept.getId());
          for (final ComplexMapRefSetMember member : members) {
            if (member.isActive() && member.getMapTarget() != null
                && !member.getMapTarget().isEmpty()) {
              boolean memberTargetFound = false;
              // Sort entries by group/priority.
              Collections.sort(mapRecord.getMapEntries(),
                  new TerminologyUtility.MapEntryComparator());
              for (MapEntry me : mapRecord.getMapEntries()) {
                if (member.getMapTarget().equals(me.getTargetId())) {
                  memberTargetFound = true;
                }
              }
              if (!memberTargetFound) {
                unusedTargetCodes.add(member.getMapTarget());
              }
            }
          }

          if (unusedTargetCodes.size() > 0) {

            String str = "";
            for (final String unusedTargetCode : unusedTargetCodes) {
              final Concept targetConcept = contentService.getConcept(unusedTargetCode,
                  mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
              str += unusedTargetCode + " " + (targetConcept == null ? "Unknown name"
                  : targetConcept.getDefaultPreferredName()) + "; ";
            }

            // truncate too-long strings (db constraint)
            str = str.substring(0, Math.min(255, str.length() - 2));

            // add names of target codes instead of source concept default
            // preferred name
            this.addReportError(report, mapProject, concept.getTerminologyId(), str,
                mapProject.getDestinationTerminology()
                    + " target code from previous release not used");
          }

          // check: concept mapped to multiple codes (non-group-based only)
          // check: concept mapped to duplicate codes (non-group-based only)
          if (mapProject.isGroupStructure()) {
            final Set<String> targetIds = new HashSet<>();
            // Sort entries by group/priority.
            Collections.sort(mapRecord.getMapEntries(),
                new TerminologyUtility.MapEntryComparator());
            for (MapEntry entry : mapRecord.getMapEntries()) {
              if (entry.getTargetId() != null && !entry.getTargetId().isEmpty()) {
                if (targetIds.contains(entry.getTargetId())) {
                  this.addReportError(report, mapProject, mapRecord.getConceptId(),
                      concept.getDefaultPreferredName(), "Concept mapped to duplicate "
                          + mapProject.getDestinationTerminology() + " codes");
                  break;
                }

              }
            }
            if (targetIds.size() > 1) {
              this.addReportError(report, mapProject, mapRecord.getConceptId(),
                  concept.getDefaultPreferredName(), "Concept mapped to multiple "
                      + mapProject.getDestinationTerminology() + " codes");
            }
          }
        }

        // Add all reported errors to the report
        for (final String error : resultMessages) {
          addReportError(report, mapProject, mapRecord.getConceptId(), mapRecord.getConceptName(),
              error);
        }
      }

      // add multiple map record mappings to report if present
      for (String conceptId : conceptMapRecordCountMap.keySet()) {
        if (conceptMapRecordCountMap.get(conceptId) > 1) {

          // get the concept
          Concept c = contentService.getConcept(conceptId, mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
          addReportError(report, mapProject, conceptId, c.getDefaultPreferredName(),
              "Concept has multiple map records");
        }
      }

      // Check: Source terminology concepts in previous version NOT in
      // current version (possibly with RF2 line from previous version map as
      // the â€œvalueâ€�)
      ComplexMapRefSetMemberList members =
          contentService.getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId());
      for (ComplexMapRefSetMember member : members.getComplexMapRefSetMembers()) {
        Concept sourceConcept = member.getConcept();
        if (member.isActive() && sourceConcept != null && sourceConcept.isActive()
            && !conceptMapRecordCountMap.containsKey(member.getConcept().getTerminologyId())) {
          this.addReportError(report, mapProject, member.getConcept().getTerminologyId(),
              member.getConcept().getDefaultPreferredName(),
              "Concept mapped in previous version no longer mapped");
        }
      }

      ReportResult pubCtResult = new ReportResultJpa();
      pubCtResult.setReport(report);
      pubCtResult.setProjectName(mapProject.getName());
      pubCtResult.setValue("Ready for publication: " + pubCt);
      pubCtResult.setReportResultItems(null);
      report.addResult(pubCtResult);

      // CHECK: In-scope concepts with no map record
      logger.debug("  Report in scope concepts with no record");
      for (final String terminologyId : conceptsWithNoRecord) {

        // get the concept
        Concept c = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

        addReportError(report, mapProject, terminologyId, c.getDefaultPreferredName(),
            "In-scope concept has no map record");
        errorFlag = true;
      }

      logger.info("  Adding Release QA Report");
      logger.info("    Log into the application to see the report results");

      // Commit the new report either way
      reportService.commit();

      // way to override the errors if we want to proceed with a release anyway
      if (!testModeFlag) {
        if (errorFlag) {
          mappingService.rollback();
          mappingService.close();
          reportService.close();
          throw new Exception("The validation had errors, please see the log");
        } else {
          mappingService.commit();
        }
      }

      logger.info("  Done begin release");

      mappingService.close();
      reportService.close();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new Exception(e);
    }
  }

  /**
   * Adds the report error.
   *
   * @param report the report
   * @param mapProject the map project
   * @param terminologyId the terminology id
   * @param conceptName the concept name
   * @param error the error
   */
  @SuppressWarnings("static-method")
  private void addReportError(Report report, MapProject mapProject, String terminologyId,
    String conceptName, String error) {

    ReportResult reportResult = null;

    // find the report result corresponding to this error, if it exists
    for (final ReportResult rr : report.getResults()) {
      if (rr.getValue().equals(error)) {
        reportResult = rr;
      }
    }

    // if no result found, create one
    if (reportResult == null) {
      reportResult = new ReportResultJpa();
      reportResult.setReport(report);
      reportResult.setValue(error);
      reportResult.setProjectName(mapProject.getName());
      report.addResult(reportResult);
    }

    ReportResultItem existingItem = null;
    for (final ReportResultItem item : reportResult.getReportResultItems()) {
      if (item.getItemId().equals(terminologyId)) {
        existingItem = item;
      }
    }
    // if this item does not yet exist, add it
    if (existingItem == null) {
      ReportResultItem item = new ReportResultItemJpa();
      item.setReportResult(reportResult);
      item.setItemId(terminologyId);
      item.setItemName(conceptName);
      item.setResultType(ReportResultType.CONCEPT);
      reportResult.addReportResultItem(item);
    }
  }

  /* see superclass */
  @Override
  public void finishRelease() throws Exception {
    try {
      logger = null;
      if (testModeFlag) {
        logger = previewFinishLog;
      } else {
        logger = finishLog;
      }
      clearLog(logger);

      logger.info("Starting " + (testModeFlag ? "Preview Finish Release" : "Finish Release"));
      logger.info("transactionPerOperation " + mappingService.getTransactionPerOperation());
      Logger.getLogger(getClass())
          .info("transactionPerOperation " + mappingService.getTransactionPerOperation());

      // instantiate required services
      
       final MappingService mappingService = new MappingServiceJpa(); 
       if (!testModeFlag) { 
         mappingService.setTransactionPerOperation(false);
         mappingService.beginTransaction(); 
       }
       

      // compare file to current records
      Report report = compareInputFileToExistingMapRecords();

      int pubCt = 0;
      int recordCt = 0;

      // get all scope concept terminology ids for this project
      logger.info("  Get scope concepts for map project");
      Set<String> scopeConceptTerminologyIds = new HashSet<>();
      for (final SearchResult sr : mappingService.findConceptsInScope(mapProject.getId(), null)
          .getSearchResults()) {
        scopeConceptTerminologyIds.add(sr.getTerminologyId());
      }
      logger.info("  scope concepts: " + scopeConceptTerminologyIds.size());

      if (mapRecords == null || mapRecords.isEmpty()) {
        MapRecordList mapRecordList = mappingService
            .getPublishedAndReadyForPublicationMapRecordsForMapProject(mapProject.getId(), null);
        mapRecords = mapRecordList.getMapRecords();

        // Log recently edited records that won't be PUBLISHED
        for (Long recordId : recentlyEditedRecords) {
          logger.info("    Recently edited record will not be PUBLISHED " + recordId);
        }

        for (final MapRecord record : mapRecords) {

          // Remove out of scope concepts if not in test mode
          if (!scopeConceptTerminologyIds.contains(record.getConceptId())) {

            // remove record if flag set
            if (!testModeFlag) {
              logger.info("    REMOVE out of scope record " + record.getId());
              mappingService.removeMapRecord(record.getId());
            } else {
              this.addReportError(report, mapProject, record.getConceptId(),
                  record.getConceptName(), "Map record for concept out of scope will be removed");

            }
          }
          // Record not marked as PUBLISHED because it has been modified again
          // since
          // the publication date
          else if (record.getWorkflowStatus() == WorkflowStatus.READY_FOR_PUBLICATION
              && recentlyEditedRecords.contains(record.getId())
              && conceptsFromReleaseFile.contains(record.getConceptId())) {
            logger.info("  Record not updated to PUBLISHED for " + record.getConceptId() + " "
                + record.getConceptName());

          }
          // Mark record as PUBLISHED if READY FOR PUBLICATION and in scope
          else if (record.getWorkflowStatus() == WorkflowStatus.READY_FOR_PUBLICATION
              && !recentlyEditedRecords.contains(record.getId())
              && conceptsFromReleaseFile.contains(record.getConceptId())) {
            logger.info("  Update record to PUBLISHED for " + record.getConceptId() + " "
                + record.getConceptName());
            pubCt++;
            // regularly log at intervals
            if (pubCt % 200 == 0) {
              logger.info("    published count = " + pubCt);
            }
            if (!testModeFlag) {
              record.setWorkflowStatus(WorkflowStatus.PUBLISHED);
              mappingService.updateMapRecord(record);
            }
          }

         
          // periodically commit
          if(!testModeFlag) {           
            if (++recordCt % 5000 == 0) {
              logger.info("    record count = " + recordCt);
              mappingService.commit();
              mappingService.clear();
              mappingService.beginTransaction();
            }      
          }
        }

        // Set latest publication date to now.
        if (!testModeFlag) {

          mapProject.setLatestPublicationDate(new Date());
          // mapProject.setPublic(true);
          mappingService.updateMapProject(mapProject);
          mappingService.commit();
        }
      }

      this.addReportError(report, mapProject, "", "Aggregate result (no content)",
          pubCt + " map records " + (testModeFlag ? "will be " : "") + " marked Published");

      // skip if in test mode
      if (!testModeFlag) {
        // clear old map refset
        logger.info("  Clear map refset");
        clearMapRefSet();
        // Load map refset
        logger.info("  Load map refset");
        loadMapRefSet();
      }

      logger.info("  Committing finish release report");

      ReportService reportService = new ReportServiceJpa();
      reportService.addReport(report);
      reportService.close();

      mappingService.close();

      logger.info("Finished " + (testModeFlag ? "test mode " : "") + "release successfully");

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new Exception(e);
    }
  }

  /**
   * Clear complex map refsets for a map project.
   *
   * @throws Exception the exception
   */
  private void clearMapRefSet() throws Exception {
    // begin transaction
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    for (final ComplexMapRefSetMember member : contentService
        .getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId()).getIterable()) {
      logger.debug("    Remove member - " + member);
      if (!testModeFlag) {
        if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {
          contentService.removeComplexMapRefSetMember(member.getId());
        } else {
          contentService.removeSimpleMapRefSetMember(member.getId());
        }
      }
    }
    contentService.commit();
    contentService.close();
  }

  /**
   * Load map refset from file.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private void loadMapRefSet() throws Exception {

    String line = "";
    int objectCt = 0;

    // begin transaction
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();
    logger.info("    Open " + inputFile);
    File f = new File(inputFile);
    if (!f.exists()) {
      throw new Exception("Input file does not exist: " + f.toString());
    }

    BufferedReader reader = new BufferedReader(new FileReader(f));

    final String terminology = mapProject.getSourceTerminology();
    final String version = mapProject.getSourceTerminologyVersion();
    while ((line = reader.readLine()) != null) {

      line = line.replace("\r", "");
      final String fields[] = line.split("\t");

      // skip header
      if (!fields[0].equals("id")) {
        final ComplexMapRefSetMember member = new ComplexMapRefSetMemberJpa();

        member.setTerminologyId(fields[0]);
        member.setEffectiveTime(dateFormat.parse(fields[1]));
        member.setActive(fields[2].equals("1") ? true : false);
        member.setModuleId(Long.valueOf(fields[3]));
        member.setRefSetId(fields[4]);
        // conceptId

        // Terminology attributes
        member.setTerminology(terminology);
        member.setTerminologyVersion(version);

        // set Concept
        final Concept concept = contentService.getConcept(
            !mapProject.getReverseMapPattern() ? fields[5] : fields[6],
            terminology, version);
        ;

        if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {

          // ComplexMap unique attributes
          member.setMapGroup(Integer.parseInt(fields[6]));
          member.setMapPriority(Integer.parseInt(fields[7]));
          member.setMapRule(fields[8]);
          member.setMapAdvice(fields[9]);
          member.setMapTarget(fields[10]);
          if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
            member.setMapRelationId(Long.valueOf(fields[11]));
          } else if (mapProject
              .getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
            member.setMapRelationId(Long.valueOf(fields[12]));

          } else {
            throw new Exception(
                "Unsupported map type " + mapProject.getMapRefsetPattern());
          }
          // ComplexMap unique attributes NOT set by file (mapBlock
          // elements) - set defaults
          member.setMapBlock(0);
          member.setMapBlockRule(null);
          member.setMapBlockAdvice(null);

        } else {
          member.setMapGroup(1);
          member.setMapPriority(1);
          member.setMapRule(null);
          member.setMapAdvice(null);
          member.setMapRelationId(null);
          member.setMapTarget(
              !mapProject.getReverseMapPattern() ? fields[6] : fields[5]);
        }

        // regularly log and commit at intervals
        if (++objectCt % 5000 == 0) {
          logger.info("    count = " + objectCt);
          contentService.commit();
          contentService.clear();
          contentService.beginTransaction();
        }

        if (concept != null) {
          logger.debug("    Add member - " + member);
          if (!testModeFlag) {
            member.setConcept(concept);
            if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {
              contentService.addComplexMapRefSetMember(member);
            } else {
              contentService.addSimpleMapRefSetMember(new SimpleMapRefSetMemberJpa(member));
            }
          }
        } else {
          throw new Exception("Member references non-existent concept - " + member);
        }

      }
    }

    // commit any remaining objects
    contentService.commit();
    contentService.close();
    reader.close();

  }

  /**
   * Load map refset from file.
   *
   * @return the report
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private Report compareInputFileToExistingMapRecords() throws Exception {

    String line = "";

    // begin transaction
    final ContentService contentService = new ContentServiceJpa();

    logger.info("    Open " + inputFile);
    final File f = new File(inputFile);
    if (!f.exists()) {
      throw new Exception("Input file does not exist: " + f.toString());
    }

    final BufferedReader reader = new BufferedReader(new FileReader(f));

    final String terminology = mapProject.getSourceTerminology();
    final String version = mapProject.getSourceTerminologyVersion();

    final Map<String, List<ComplexMapRefSetMember>> conceptRefSetMap = new HashMap<>();

    while ((line = reader.readLine()) != null) {

      line = line.replace("\r", "");
      final String fields[] = line.split("\t");

      // skip header and inactive refsets
      if (!fields[0].equals("id") && fields[2].equals("1")) {
        final ComplexMapRefSetMember member = new ComplexMapRefSetMemberJpa();

        member.setTerminologyId(fields[0]);
        member.setEffectiveTime(dateFormat.parse(fields[1]));
        member.setActive(fields[2].equals("1") ? true : false);
        member.setModuleId(Long.valueOf(fields[3]));

        if (moduleId == null) {
          moduleId = fields[3];
        }
        member.setRefSetId(fields[4]);
        // conceptId
        final Concept tempConcept = new ConceptJpa();
        tempConcept.setTerminologyId(fields[5]);
        member.setConcept(tempConcept);

        // Terminology attributes
        member.setTerminology(terminology);
        member.setTerminologyVersion(version);

        if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {

          // ComplexMap unique attributes
          member.setMapGroup(Integer.parseInt(fields[6]));
          member.setMapPriority(Integer.parseInt(fields[7]));
          if (fields[8].equals("OTHERWISE TRUE")) {
            member.setMapRule("TRUE");
          } else {
            member.setMapRule(fields[8]);
          }
          if (fields[9].contains("|")) {
            member.setMapAdvice(fields[9].substring(fields[9].indexOf("|") + 2));
          } else {
            member.setMapAdvice(fields[9]);
          }
          member.setMapTarget(fields[10]);
          if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
            member.setMapRelationId(Long.valueOf(fields[11]));
          } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
            member.setMapRelationId(Long.valueOf(fields[12]));

          } else {
            throw new Exception("Unsupported map type " + mapProject.getMapRefsetPattern());
          }
          // ComplexMap unique attributes NOT set by file (mapBlock
          // elements) - set defaults
          member.setMapBlock(0);
          member.setMapBlockRule(null);
          member.setMapBlockAdvice(null);

        } else {
          member.setMapGroup(1);
          member.setMapPriority(1);
          member.setMapRule(null);
          member.setMapAdvice(null);
          member.setMapTarget(fields[6]);
          member.setMapRelationId(null);
        }

        // Skip concept exclusion rules
        if (member.getMapRule() != null && member.getMapRule().matches("IFA.*")) {
          if (member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
              && !member.getMapRule().contains("AND IFA")) {
            // unless simple gender rule, then keep
          } else if (member.getMapRule().matches(
              "IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*AND IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*")
              && !member.getMapRule().matches(".*AND IFA.*AND IFA.*")) {
            // unless 2-part age rule, then keep
          } else if (member.getMapRule().matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*")
              && !member.getMapRule().contains("AND IFA")) {
            // unless simple age rule without compound clause, then keep
          } else {
            // else skip
            Logger.getLogger(MappingServiceJpa.class)
                .debug("    Skipping refset member exclusion rule " + member.getTerminologyId());
            continue;
          }
        }

        List<ComplexMapRefSetMember> members =
            conceptRefSetMap.get(member.getConcept().getTerminologyId());
        if (members == null) {
          members = new ArrayList<>();
        }

        // if we get a TRUE/MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH
        // AVAILABLE DATA member
        // AND we've already seen a TRUE member
        // AND it's not in the same map group as an allowed IFA member
        // skip this member. It is due to up-propagation and shouldn't be
        // compared.
        if (member.getMapRule() != null && member.getMapRule().equals("TRUE")
            && member.getMapAdvice() != null
            && member.getMapAdvice()
                .equals("MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA")
            && containsTrueMember(members) && !containsIFAMember(members, member.getMapGroup())) {
          continue;
        }

        members.add(member);
        conceptRefSetMap.put(member.getConcept().getTerminologyId(), members);
        conceptsFromReleaseFile.add(member.getConcept().getTerminologyId());
      }
    }

    logger.info(conceptRefSetMap.size() + " concept ids with mappings");

    // close any remaining objects

    reader.close();

    // construct report
    WorkflowService workflowService = new WorkflowServiceJpa();
    ReportService reportService = new ReportServiceJpa();

    ReportDefinition qaDef = null;
    for (ReportDefinition definition : reportService.getReportDefinitions()
        .getReportDefinitions()) {
      if (definition.getName().equals("Release Finalization QA")) {
        qaDef = definition;
        break;
      }
    }

    if (qaDef == null) {
      qaDef = new ReportDefinitionJpa();
      qaDef.setDescription(
          "Compares release input file to current state of mappings and identifies potential mismatches");
      qaDef.setDiffReport(false);
      qaDef.setFrequency(ReportFrequency.ON_DEMAND);
      qaDef.setName("Release Finalization QA");
      qaDef.setQACheck(false);
      qaDef.setQueryType(ReportQueryType.NONE);
      qaDef.setResultType(ReportResultType.CONCEPT);
      qaDef.setRoleRequired(MapUserRole.LEAD);
      reportService.addReportDefinition(qaDef);
    }

    Report report = new ReportJpa();
    report.setReportDefinition(qaDef);
    report.setMapProjectId(mapProject.getId());
    report.setOwner(mappingService.getMapUser("qa"));
    report.setAutoGenerated(false);
    report.setName("Release Finalization QA");
    report.setQuery("No query -- constructed by services");
    report.setTimestamp(new Date().getTime());

    // if test mode flag, add a null entry indicating
    if (testModeFlag) {
      this.addReportError(report, mapProject, "", "Note indicator (empty content)",
          "Finish Release run in TEST mode");
    }
    // get loader user for construction/update of records
    MapUser loaderUser = mappingService.getMapUser("loader");

    // counter for number of records matching between current and release
    int matchCt = 0;

    logger.info("Checking for discrepancies...");

    for (String conceptId : conceptRefSetMap.keySet()) {
      final List<ComplexMapRefSetMember> members = conceptRefSetMap.get(conceptId);
      Concept concept = contentService.getConcept(conceptId, mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      boolean discrepancyFound = false;

      Date nowDate = new Date();
      MapRecord releaseRecord = new MapRecordJpa();
      releaseRecord.setMapProjectId(mapProject.getId());
      releaseRecord.setConceptId(conceptId);
      releaseRecord.setConceptName(concept.getDefaultPreferredName());
      releaseRecord.setLastModified(nowDate.getTime());
      releaseRecord.setLastModifiedBy(loaderUser);
      releaseRecord.setTimestamp(nowDate.getTime());
      releaseRecord.setOwner(loaderUser);
      releaseRecord.setWorkflowStatus(WorkflowStatus.PUBLISHED);

      // get the map record for this concept id
      MapRecord mapRecord = null;
      try {
        mapRecord = getMapRecordForTerminologyId(conceptId);
      } catch (Exception e) {
        // do nothing, getMapRecord throws exception intended
        // to stop release where true errors exist
      }
      if (mapRecord == null) {
        logger.info("Discrepancy: no current map record for concept id " + conceptId);
        discrepancyFound = true;
      }
      // skip records still in the workflow
      else if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
        System.out.println("Skipping workflow status: " + mapRecord.getWorkflowStatus());
        continue;
      }

      // if entries are mismatched in size, automatic flag
      else if (mapRecord.getMapEntries().size() != members.size()) {
        logger.info("Discrepancy: entry set size mismatch for " + conceptId + " "
            + mapRecord.getMapEntries().size() + " vs " + members.size());
        discrepancyFound = true;
      }

      // otherwise, check for discrepancies in entries
      else {

        for (MapEntry recordEntry : mapRecord.getMapEntries()) {
          boolean entryMatched = false;
          for (ComplexMapRefSetMember member : members) {
            if (getHash(recordEntry).equals(getHash(member))) {
              entryMatched = true;
              break;
            }
          }
          if (!entryMatched) {
            logger.info(
                "Discrepancy: current mapping has no corresponding release mapping " + conceptId);
            discrepancyFound = true;
            break;
          }
        }

        // check release mappings against current mappings
        // iterate through release file members, checking all current members
        int priorMapGroup = 1;
        int priorMapPriority = 0;
        for (ComplexMapRefSetMember member : members) {

          final String memberHash = getHash(member);

          MapEntry releaseEntry = this.getMapEntryForComplexMapRefSetMember(member);
          releaseEntry.setMapRecord(mapRecord);
          // before adding releaseEntry, check if up-propagation requires
          // changes to mapGroup/mapPriority
          if (releaseEntry.getMapGroup() == priorMapGroup) {
            releaseEntry.setMapPriority(++priorMapPriority);
          } else if (releaseEntry.getMapGroup() != priorMapGroup) {
            priorMapPriority = 1;
            releaseEntry.setMapPriority(priorMapPriority);
          }
          releaseRecord.addMapEntry(releaseEntry);

          boolean entryMatched = false;
          for (MapEntry recordEntry : mapRecord.getMapEntries()) {
            if (getHash(recordEntry).equals(memberHash)) {
              entryMatched = true;
              if (entryMatched && !releaseEntry.isEquivalent(recordEntry)) {
                logger.info(
                    "Discrepancy: release mapping has non-equivalent corresponding current mapping "
                        + conceptId);
                discrepancyFound = true;
                break;
              }
            }
          }

          if (!entryMatched) {
            logger.info("Discrepancy: no current map record for concept id " + conceptId);
            discrepancyFound = true;
          }

        }

      }

      // if discrepancy found, add or update the map record
      if (discrepancyFound) {
        logger.info(
            "Discrepancy found for " + conceptId + "|" + concept.getDefaultPreferredName() + "|");
        if (mapRecord != null) {

          if (new Date(mapRecord.getLastModified()).after(mapProject.getEditingCycleBeginDate())) {
            logger.info("Recently edited discrepancy found for " + conceptId + "|"
                + concept.getDefaultPreferredName() + "|");
            recentlyEditedRecords.add(mapRecord.getId());
            this.addReportError(report, mapProject, conceptId, concept.getDefaultPreferredName(),
                "Map record discrepancy with recent edits-- will not be updated to release version");
          } else {
            if (!testModeFlag) {
              // remove and re-add map record to clear previous entries
              mappingService.removeMapRecord(mapRecord.getId());
              mappingService.addMapRecord(releaseRecord);
            }
            this.addReportError(report, mapProject, conceptId, concept.getDefaultPreferredName(),
                "Map record discrepancy -- " + (testModeFlag ? "will be " : "")
                    + "updated to release version");
          }
        } else {

          this.addReportError(report, mapProject, conceptId, concept.getDefaultPreferredName(),
              "Map record found in release but no current record found -- no action "
                  + (testModeFlag ? "will be " : "") + "taken");
        }

      } else {
        matchCt++;
      }

    }

    this.addReportError(report, mapProject, "", "Aggregate result (no content)",
        matchCt + " records matched between release and current records");

    reportService.close();
    workflowService.close();
    contentService.close();

    return report;

  }

  private boolean containsTrueMember(List<ComplexMapRefSetMember> members) {
    for (ComplexMapRefSetMember m : members) {
      if (m.getMapRule().equals("TRUE")) {
        return true;
      }
    }
    return false;
  }

  private boolean containsIFAMember(List<ComplexMapRefSetMember> members, int mapGroup) {
    for (ComplexMapRefSetMember m : members) {
      if (m.getMapRule().matches("IFA.*") && m.getMapGroup() == mapGroup) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update statistic max.
   *
   * @param statistic the statistic
   * @param value the value
   */
  private void updateStatMax(String statistic, int value) {

    Integer stat = reportStatistics.get(statistic);

    if (stat == null) {
      reportStatistics.put(statistic, value);
    } else {
      reportStatistics.put(statistic, Math.max(stat, value));
    }

  }

  /* see superclass */
  @Override
  public void setEffectiveTime(String effectiveTime) {
    this.effectiveTime = effectiveTime;
  }

  /* see superclass */
  @Override
  public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
  }

  /* see superclass */
  @Override
  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  /* see superclass */
  @Override
  public void setWriteSnapshot(boolean writeSnapshot) {
    this.writeSnapshot = writeSnapshot;
  }

  /* see superclass */
  @Override
  public void setWriteActiveSnapshot(boolean writeActiveSnapshot) {
    this.writeActiveSnapshot = writeActiveSnapshot;
  }

  /* see superclass */
  @Override
  public void setWriteDelta(boolean writeDelta) {
    this.writeDelta = writeDelta;
  }

  /* see superclass */
  @Override
  public void setMapProject(MapProject mapProject)
    throws InstantiationException, IllegalAccessException, InvocationTargetException,
    NoSuchMethodException, ClassNotFoundException {
    this.mapProject = mapProject;
    // instantiate the algorithm handler
    algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mapProject);
    initializeLogs();

  }

  /* see superclass */
  @Override
  public void setMapRecords(List<MapRecord> mapRecords) {
    this.mapRecords = mapRecords;
  }

  private void initializeLogs() {
    try {
      String rootPath =
          ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
      if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
        rootPath += "/";
      }
      rootPath += mapProject.getId() + "/logs";
      File logDirectory = new File(rootPath);
      if (!logDirectory.exists()) {
        logDirectory.mkdir();
      }
      File beginLogFile = new File(logDirectory, "begin.log");
      LoggerUtility.setConfiguration("beginRelease", beginLogFile.getAbsolutePath());
      beginLog = LoggerUtility.getLogger("beginRelease");

      File processLogFile = new File(logDirectory, "process.log");
      LoggerUtility.setConfiguration("processRelease", processLogFile.getAbsolutePath());
      processLog = LoggerUtility.getLogger("processRelease");

      File previewFinishLogFile = new File(logDirectory, "previewFinish.log");
      LoggerUtility.setConfiguration("previewFinishRelease",
          previewFinishLogFile.getAbsolutePath());
      previewFinishLog = LoggerUtility.getLogger("previewFinishRelease");

      File finishLogFile = new File(logDirectory, "finish.log");
      LoggerUtility.setConfiguration("finishRelease", finishLogFile.getAbsolutePath());
      finishLog = LoggerUtility.getLogger("finishRelease");

    } catch (Exception e) {
      logger.error(e.getStackTrace());
    }
  }

  private void clearLog(Logger logger) {
    try {
      String rootPath =
          ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
      if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
        rootPath += "/";
      }
      rootPath += mapProject.getId() + "/logs";
      File logDirectory = new File(rootPath);
      if (!logDirectory.exists()) {
        return;
      }
      if (logger == beginLog) {
        File beginLogFile = new File(logDirectory, "begin.log");
        PrintWriter writer = new PrintWriter(beginLogFile);
        writer.print("");
        writer.close();
      }
      if (logger == processLog) {
        File processLogFile = new File(logDirectory, "process.log");
        PrintWriter writer = new PrintWriter(processLogFile);
        writer.print("");
        writer.close();
      }

      File previewFinishLogFile = new File(logDirectory, "previewFinish.log");
      LoggerUtility.setConfiguration("previewFinishRelease",
          previewFinishLogFile.getAbsolutePath());
      previewFinishLog = LoggerUtility.getLogger("previewFinishRelease");

      File finishLogFile = new File(logDirectory, "finish.log");
      LoggerUtility.setConfiguration("finishRelease", finishLogFile.getAbsolutePath());
      finishLog = LoggerUtility.getLogger("finishRelease");
    } catch (Exception e) {
      logger.info(e.getStackTrace());
    }
  }

  /* see superclass */
  @Override
  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

  /**
   * Returns the pattern for type.
   *
   * @param mapProject the map project
   * @return the pattern for type
   */
  @Override
  public String getPatternForType(MapProject mapProject) {
    if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {
      return "sRefset_";
    } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
      return "iissscRefset_";
    } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
      return "iisssccRefset_";
    }
    return null;
  }

  /**
   * Returns the header.
   *
   * @param mapProject the map project
   * @return the header
   */
  private String getHeader(MapProject mapProject) {
    if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {
      if (mapProject.getReverseMapPattern()) {
        return "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tmapSource";
      } else {
        return "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tmapTarget";
      }
    } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
      return "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\t"
          + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId";
    } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
      return "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\t"
          + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId";
    }
    return null;
  }
}
