/*
 * 
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
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
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.ReportService;
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

  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

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
    NEW_CONCEPTS("New concepts mapped this release "),
    /** The retired concepts. */
    RETIRED_CONCEPTS("Concepts mapped retired this release "),
    /** The changed concepts. */
    CHANGED_CONCEPTS("Concept mappings changed this release ");

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

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#close()
   */
  @Override
  public void close() throws Exception {
    mappingService.close();
    contentService.close();
    metadataService.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processRelease
   * (org.ihtsdo.otf.mapping.model.MapProject, java.lang.String,
   * java.lang.String, java.util.Set, java.lang.String, java.lang.String)
   */
  @Override
  public void processRelease() throws Exception {

    // get all map records for this project
    if (mapRecords == null || mapRecords.isEmpty()) {
      MapRecordList mapRecordList =
          mappingService
              .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                  mapProject.getId(), null);
      mapRecords = mapRecordList.getMapRecords();
    }

    // Log config
    Logger.getLogger(getClass()).info(
        "  pattern = " + mapProject.getMapRefsetPattern().toString());
    Logger.getLogger(getClass()).info(
        "  rule-based = " + mapProject.isRuleBased());
    Logger.getLogger(getClass()).info("  record count = " + mapRecords.size());

    // check that either/both snapshot and delta files have been specified
    if (!writeSnapshot && !writeDelta) {
      throw new Exception(
          "processRelease called with both snapshot and delta flags disabled");
    }

    //
    // Check preconditions
    //

    // check for supported ref set pattern
    if (!mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ComplexMap)
        && !mapProject.getMapRefsetPattern().equals(
            MapRefsetPattern.ExtendedMap)) {

      throw new Exception("Unsupported map refset pattern - "
          + mapProject.getMapRefsetPattern());
    }
    // check that effectiveTime and moduleId have been properly specified
    if (effectiveTime == null || effectiveTime.isEmpty()) {
      throw new Exception("Effective time must be specified");
    }

    // check module id
    if (moduleId == null || moduleId.isEmpty()) {
      throw new Exception("Module id must be specified");
    }
    if (!metadataService.getModules(mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion()).containsKey(moduleId)) {
      throw new Exception("Module id is not a valid module id " + moduleId);
    }

    // Refset id
    if (!metadataService.getComplexMapRefSets(
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion()).containsKey(
        mapProject.getRefSetId())) {
      throw new Exception(
          "Map project refset id is not a valid complex map refset id "
              + mapProject.getRefSetId());
    }

    // check output directory exists
    File outputDirFile = new File(outputDir);
    if (!outputDirFile.isDirectory())
      throw new Exception("Output file directory (" + outputDir
          + ") could not be found.");

    //
    // Compute default preferred names
    //
    Logger.getLogger(getClass()).info("  Compute default preferred names");
    computeDefaultPreferredNames();

    // instantiate the project specific handler
    algorithmHandler =
        mappingService.getProjectSpecificAlgorithmHandler(mapProject);

    // Write module dependency file
    Set<String> moduleDependencies = algorithmHandler.getDependentModules();
    if (moduleDependencies.size() > 0) {
      writeModuleDependencyFile(moduleDependencies,
          algorithmHandler.getModuleDependencyRefSetId());
    }

    //
    // Prepare data
    //

    // put all map records into the map record map
    for (MapRecord mr : mapRecords) {
      if (mr == null)
        throw new Exception("Null record found in published list");
      mapRecordMap.put(mr.getConceptId(), mr);
    }

    // create a list from the set and sort by concept id
    Logger.getLogger(getClass()).info("  Sorting records");
    Collections.sort(mapRecords, new Comparator<MapRecord>() {
      @Override
      public int compare(MapRecord o1, MapRecord o2) {
        Long conceptId1 = Long.parseLong(o1.getConceptId());
        Long conceptId2 = Long.parseLong(o2.getConceptId());
        return conceptId1.compareTo(conceptId2);
      }
    });

    Logger.getLogger(ReleaseHandler.class).info("  Retrieving maps");

    // retrieve the complex map ref set members for this project's refset id
    ComplexMapRefSetMemberList prevMemberList =
        contentService.getComplexMapRefSetMembersForRefSetId(mapProject
            .getRefSetId());

    // construct map of existing complex ref set members by UUID fields
    // this is used for comparison purposes later
    // after record processing, the remaining ref set members
    // represent those entries that are now inactive
    Map<String, ComplexMapRefSetMember> prevMembersHashMap = new HashMap<>();
    for (ComplexMapRefSetMember c : prevMemberList.getComplexMapRefSetMembers()) {
      prevMembersHashMap.put(getHash(c), c);
    }

    // output size of each collection
    Logger.getLogger(getClass()).info(
        "    Cached distinct UUID-quintuples = "
            + prevMembersHashMap.keySet().size());
    Logger.getLogger(getClass()).info(
        "    Existing complex ref set members for project = "
            + prevMemberList.getCount());

    // if sizes do not match, output warning
    if (prevMembersHashMap.keySet().size() != prevMemberList.getCount()) {
      throw new Exception(
          "UUID-quintuples count does not match refset member count");
    }

    // clear the ref set members list (no longer used)
    prevMemberList = null;

    // /////////////////////////////////////////////////////
    // Perform the release
    // /////////////////////////////////////////////////////

    // Prep map relation to use for up propagated records
    MapRelation ifaRuleRelation =
        algorithmHandler.getDefaultUpPropagatedMapRelation();
    if (ifaRuleRelation == null) {
      throw new Exception(
          "Unable to find default map relation for up propagated records");
    }

    Logger.getLogger(getClass()).info("  Processing release");
    // cycle over the map records marked for publishing
    int ct = 0;
    Map<String, ComplexMapRefSetMember> activeMembersMap = new HashMap<>();
    for (MapRecord mapRecord : mapRecords) {
      Logger.getLogger(getClass()).info(
          "    Processing record for " + mapRecord.getConceptId());

      ct++;

      if (ct % 5000 == 0) {
        Logger.getLogger(getClass()).info("    count = " + ct);
      }

      // instantiate map of entries by group
      // this is the object containing entries to write
      Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

      // /////////////////////////////////////////////////////
      // Check for up-propagation
      // /////////////////////////////////////////////////////
      if (mapProject.isPropagatedFlag()
          && contentService.getDescendantConceptsCount(
              mapRecord.getConceptId(), mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion()) < mapProject
              .getPropagationDescendantThreshold()) {

        // Handle up propagation for this record
        if (!handleUpPropagation(mapRecord, entriesByGroup, ifaRuleRelation)) {
          // handle cases that cannot be up propagated
          continue;
        }

      } else {
        Logger.getLogger(getClass()).debug(
            "  DO NOT up propagate " + mapRecord.getConceptId());

      }

      // /////////////////////////////////////////////////////
      // Add the original (non-propagated) entries
      // /////////////////////////////////////////////////////
      Logger.getLogger(getClass()).debug("     Adding original entries");
      for (MapEntry me : mapRecord.getMapEntries()) {
        Logger.getLogger(getClass()).debug("       Adding entry " + me.getId());

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
        newEntry.setMapRelation(algorithmHandler.computeMapRelation(mapRecord,
            me));

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
              || !existingEntries.get(existingEntries.size() - 1).getRule()
                  .contains("TRUE")) {

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
            newEntry.setMapRelation(algorithmHandler.computeMapRelation(
                mapRecord, newEntry));

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
        throw new Exception("Map record exists for nonexistent concept: "
            + mapRecord.getConceptId());
      }
      if (!concept.isActive()) {
        throw new Exception("Map record exists for inactive concept: "
            + mapRecord.getConceptId());
      }

      // cycle over groups and entries in sequence
      // Collect active only entries
      for (int mapGroup : entriesByGroup.keySet()) {

        int mapPriority = 1;

        for (MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

          // convert this map entry into a complex map ref set member
          ComplexMapRefSetMember member =
              getComplexMapRefSetMemberForMapEntry(mapEntry, mapRecord,
                  mapProject, concept);

          String uuidStr = getHash(member);

          // attempt to retrieve any existing complex map ref set
          // member
          ComplexMapRefSetMember prevMember = prevMembersHashMap.get(uuidStr);

          // if existing found, re-use uuid, otherwise generate new
          if (prevMember == null) {
            member.setTerminologyId(ConfigUtility.getReleaseUuid(uuidStr)
                .toString());
          } else {
            member.setTerminologyId(prevMember.getTerminologyId());
          }

          // assign and increment map priority
          member.setMapPriority(mapPriority++);

          // add this entry to the list of members to write
          if (activeMembersMap.containsKey(member.getTerminologyId())) {
            Logger.getLogger(getClass()).error(
                activeMembersMap.get(member.getTerminologyId()).toString());
            Logger.getLogger(getClass()).error(member.toString());
            throw new Exception("Duplicate id found");
          }

          ValidationResult result = null;
          result = algorithmHandler.validateForRelease(member);

          if (result != null && !result.isValid()) {
            throw new Exception("Invalid member for "
                + member.getConcept().getTerminologyId() + " - " + result);
          }
          activeMembersMap.put(member.getTerminologyId(), member);
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
    Map<String, ComplexMapRefSetMember> prevInactiveMembersMap =
        new HashMap<>();

    // First, construct set of previously active complex map ref set members
    for (ComplexMapRefSetMember member : prevMembersHashMap.values()) {
      if (member.isActive()) {
        prevActiveMembersMap.put(member.getTerminologyId(), member);
      } else {
        prevInactiveMembersMap.put(member.getTerminologyId(), member);
      }
    }

    Logger.getLogger(getClass()).info(
        "  prev inactive members = " + prevInactiveMembersMap.size());
    Logger.getLogger(getClass()).info(
        "  prev active members = " + prevActiveMembersMap.size());
    Logger.getLogger(getClass()).info(
        "  active members = " + activeMembersMap.size());

    // Write human readable file
    writeHumanReadableFile(activeMembersMap);

    // Write snapshot file
    if (writeSnapshot) {
      writeActiveSnapshotFile(activeMembersMap);
      writeSnapshotFile(prevInactiveMembersMap, prevActiveMembersMap,
          activeMembersMap);
    }

    // Write delta file
    if (writeDelta) {
      writeDeltaFile(activeMembersMap, prevActiveMembersMap);
    }

    // Write statistics
    writeStatsFile(activeMembersMap, prevActiveMembersMap);

    // write the concept errors
    Logger.getLogger(getClass()).info(
        "Concept errors (" + conceptErrors.keySet().size() + ")");
    for (String terminologyId : conceptErrors.keySet()) {
      Logger.getLogger(getClass()).info(
          "  " + terminologyId + ": " + conceptErrors.get(terminologyId));
    }

    // /////////////////////////////////////////////////////
    // Clean up
    // /////////////////////////////////////////////////////

    // close the services
    contentService.close();
    mappingService.close();
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
    Map<Integer, List<MapEntry>> entriesByGroup, MapRelation ifaRuleRelation)
    throws Exception {

    // /////////////////////////////////////////////////////
    // Get the tree positions for this concept
    // /////////////////////////////////////////////////////

    TreePosition treePosition = null;
    try {
      // get any tree position for this concept
      treePosition =
          contentService.getAnyTreePositionWithDescendants(
              mapRecord.getConceptId(), mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      if (treePosition != null) {
        Logger.getLogger(getClass()).debug(
            "  Tree position: " + treePosition.getAncestorPath() + " - "
                + mapRecord.getConceptId());
      }
    } catch (Exception e) {
      throw new Exception("Error getting tree position for "
          + mapRecord.getConceptId());
    }

    // check if tree positions were successfully retrieved
    if (treePosition == null) {
      throw new Exception("Could not retrieve any tree position for "
          + mapRecord.getConceptId());
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
    for (TreePosition tp : treePositionDescendantList) {

      // avoid re-rendering nodes already rendered
      if (!descendantsProcessed.contains(tp.getTerminologyId())) {
        Logger.getLogger(getClass()).debug(
            "  Processing descendant " + tp.getTerminologyId());

        // add this descendant to the processed list
        descendantsProcessed.add(tp.getTerminologyId());

        // skip the root level record, these entries are added
        // below, after the up-propagated entries
        if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

          // get the parent map record for this tree position
          // used to check if entries are duplicated on parent
          String parent =
              tp.getAncestorPath().substring(
                  tp.getAncestorPath().lastIndexOf("~") + 1);
          // TODO: this really should be parent(s) (e.g. via the "concept"
          // object)
          MapRecord mrParent = getMapRecordForTerminologyId(parent);

          // get the map record corresponding to this specific
          // ancestor path + concept Id
          MapRecord mr = getMapRecordForTerminologyId(tp.getTerminologyId());

          if (mr != null) {

            Logger.getLogger(getClass()).debug(
                "     Adding entries from map record " + mr.getId() + ", "
                    + mr.getConceptId() + ", " + mr.getConceptName());

            // if no parent, continue, but log error
            if (mrParent == null) {
              // create a blank for comparison
              mrParent = new MapRecordJpa();
            }

            // cycle over the entries
            // TODO: this should actually compare entire groups and not just
            // entries
            // to account for embedded age/gender rules. Otherwise a partial
            // group could
            // be explicitly rendered and the logic would be wrong
            //
            // Thus if all the entries for a group match the parent, then none
            // need to be rendered, otherwise all do.
            for (MapEntry me : mr.getMapEntries()) {

              // get the current list of entries for this group
              List<MapEntry> existingEntries =
                  entriesByGroup.get(me.getMapGroup());

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
              for (MapEntry parentEntry : mrParent.getMapEntries()) {
                if (parentEntry.getMapGroup() == me.getMapGroup()
                    && parentEntry.isEquivalent(me)) {
                  isDuplicateEntry = true;
                  break;
                }
              }

              // if not a duplicate entry, add it to the map
              if (!isDuplicateEntry) {

                Logger.getLogger(getClass()).debug(
                    "  Entry is not a duplicate of parent");
                Logger.getLogger(getClass()).debug("    entry = " + me);

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
                if (newEntry.getTargetId() == null
                    || newEntry.getTargetId().isEmpty()) {
                  newEntry.setMapRelation(me.getMapRelation());
                } else {
                  newEntry.setMapRelation(ifaRuleRelation);
                }

                // add to the list
                existingEntries.add(newEntry);

                // replace existing list with modified list - unnecessary
                entriesByGroup.put(newEntry.getMapGroup(), existingEntries);

              } else {

                Logger.getLogger(getClass()).debug(
                    "  Entry IS DUPLICATE of parent, do not write");
                Logger.getLogger(getClass()).debug("    entry = " + me);
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
  private void writeModuleDependencyFile(Set<String> moduleDependencies,
    String refSetId) throws Exception {
    Logger.getLogger(getClass()).info("  Write module dependency file");
    Logger.getLogger(getClass()).info(
        "    count = " + moduleDependencies.size());
    // Open file
    String filename = null;
    BufferedWriter writer = null;
    filename =
        outputDir + "/der2_ssRefset_ModuleDependencyDelta_INT_" + effectiveTime
            + ".txt";
    writer = new BufferedWriter(new FileWriter(filename));

    // Write header
    writer
        .write("id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tsourceEffectiveTime\ttargetEffectiveTime"
            + "\r\n");

    // Write lines
    for (String module : moduleDependencies) {
      String moduleStr =
          ConfigUtility.getUuidForString(moduleId + refSetId + module)
              .toString()
              + "\t"
              + effectiveTime
              + "\t"
              + "1"
              + "\t"
              + moduleId
              + "\t"
              + refSetId
              + "\t"
              + module
              + "\t"
              + effectiveTime
              + "\t"
              + effectiveTime + "\r\n";
      writer.write(moduleStr);
    }

    // Close
    writer.flush();
    writer.close();
  }

  /**
   * Write delta.
   *
   * @param activeMembers the active members
   * @param prevActiveMembers the previous active members
   * @throws Exception the exception
   */
  private void writeDeltaFile(
    Map<String, ComplexMapRefSetMember> activeMembers,
    Map<String, ComplexMapRefSetMember> prevActiveMembers) throws Exception {

    // Open file and writer
    String filename = null;
    BufferedWriter writer = null;
    String pattern =
        (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap
            ? "iissscRefset_" : "iisssccRefset_");
    filename =
        outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "Delta_INT_" + effectiveTime + ".txt";
    Logger.getLogger(getClass()).info("  delta:  " + filename);

    // Write headers (subject to pattern)
    writer = new BufferedWriter(new FileWriter(filename));
    writer
        .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\t"
            + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId");
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      writer.write("\tmapCategoryId");
    }
    writer.write("\r\n");

    // Compute retired, new, and changed.. discard unchanged for delta
    Map<String, ComplexMapRefSetMember> tmpActiveMembers =
        new HashMap<>(activeMembers);

    Logger.getLogger(getClass()).info("  Computing delta entries");

    // cycle over all previously active members
    for (ComplexMapRefSetMember member : prevActiveMembers.values()) {

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
    for (ComplexMapRefSetMember c : tmpActiveMembers.values()) {
      writer.write(getOutputLine(c, false));
    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    // case 2: previously active no longer present
    // Copy previously active map of uuids to write into temp map
    // For each uuid in current write set
    // - check temp map for this uuid
    // - if present, remove from temp map
    // Inactivate all remaining uuids in the temp map

    tmpActiveMembers = new HashMap<>(prevActiveMembers);

    for (String uuid : activeMembers.keySet()) {
      if (tmpActiveMembers.containsKey(uuid)) {
        tmpActiveMembers.remove(uuid);
      }

    }

    // set active to false and write inactivated complex maps
    for (ComplexMapRefSetMember c : tmpActiveMembers.values()) {
      c.setActive(false);
      writer.write(this.getOutputLine(c, false));
      // restore active
      c.setActive(true);
    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    writer.flush();
    writer.close();

  }

  /**
   * Write stats file.
   *
   * @param activeMembers the active members
   * @param prevActiveMembers the prev active members
   * @throws Exception the exception
   */
  private void writeStatsFile(
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
    for (ComplexMapRefSetMember member : activeMembers.values()) {
      String key = member.getConcept().getTerminologyId();
      alwaysNc.add(key);
      neverNc.add(key);
      if (!entryCount.containsKey(key)) {
        entryCount.put(key, new Integer(0));
      }
      int maxCt = entryCount.get(key).intValue() + 1;
      entryCount.put(key, maxCt);
      updateStatMax(Stats.MAX_ENTRIES.getValue(), maxCt);
    }
    for (ComplexMapRefSetMember member : activeMembers.values()) {
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
    Set<String> prevActiveConcepts = new HashSet<>();
    for (ComplexMapRefSetMember member : prevActiveMembers.values()) {
      prevActiveConcepts.add(member.getConcept().getTerminologyId());
    }

    updateStatMax(Stats.ACTIVE_ENTRIES.getValue(), activeMembers.size());
    updateStatMax(Stats.CONCEPTS_MAPPED.getValue(), activeConcepts.size());
    updateStatMax(Stats.COMPLEX_MAPS.getValue(), multipleEntryConcepts.size());
    updateStatMax(Stats.MULTIPLE_GROUPS.getValue(),
        multipleGroupConcepts.size());
    updateStatMax(Stats.ALWAYS_MAP.getValue(), neverNc.size());
    updateStatMax(Stats.SOMETIMES_MAP.getValue(), sometimesMap.size());
    updateStatMax(Stats.NEVER_MAP.getValue(), alwaysNc.size());

    // Determine count of retired concepts - inactive minus active
    int ct = 0;
    for (String id : prevActiveConcepts) {
      if (!activeConcepts.contains(id)) {
        ct++;
      }
    }
    updateStatMax(Stats.RETIRED_CONCEPTS.getValue(), ct);

    // Determine count of new concepts - active minus inactive
    ct = 0;
    for (String id : activeConcepts) {
      if (!prevActiveConcepts.contains(id)) {
        ct++;
      }
    }
    updateStatMax(Stats.NEW_CONCEPTS.getValue(), ct);

    Set<String> changedConcepts = new HashSet<>();
    boolean found = false;
    for (String key : activeMembers.keySet()) {
      ComplexMapRefSetMember member = activeMembers.get(key);
      ComplexMapRefSetMember member2 = prevActiveMembers.get(key);
      if (member2 != null && !member.equals(member2)) {
        changedConcepts.add(member.getConcept().getId().toString());
        found = true;
      }
    }
    if (!found) {
      throw new Exception("no changed concepts found.");
    }
    updateStatMax(Stats.CHANGED_CONCEPTS.getValue(), changedConcepts.size());

    String camelCaseName =
        mapProject.getDestinationTerminology().substring(0, 1)
            + mapProject.getDestinationTerminology().substring(1).toLowerCase();
    BufferedWriter statsWriter =
        new BufferedWriter(new FileWriter(outputDir + "/" + camelCaseName
            + "stats.txt"));
    List<String> statistics = new ArrayList<>(reportStatistics.keySet());
    Collections.sort(statistics);
    for (String statistic : statistics) {
      statsWriter.write(statistic + "\t" + reportStatistics.get(statistic)
          + "\r\n");
    }
    statsWriter.close();
  }

  /**
   * Write active snapshot file.
   *
   * @param members the members
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private void writeActiveSnapshotFile(
    Map<String, ComplexMapRefSetMember> members) throws Exception {

    Logger.getLogger(getClass()).info("Writing active snapshot...");
    String pattern =
        (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap
            ? "iissscRefset_" : "iisssccRefset_");
    String filename = null;
    BufferedWriter writer = null;
    filename =
        outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "ActiveSnapshot_INT_" + effectiveTime + ".txt";

    // write headers
    Logger.getLogger(getClass()).info("  active snapshot:  " + filename);

    writer = new BufferedWriter(new FileWriter(filename));
    writer
        .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\t"
            + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId");
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      writer.write("\tmapCategoryId");
    }
    writer.write("\r\n");

    // Write members
    List<String> lines = new ArrayList<>();
    for (ComplexMapRefSetMember member : members.values()) {
      if (!member.isActive()) {
        throw new Exception("Unexpected inactive member " + member);
      }
      // collect lines
      lines.add(getOutputLine(member, false));
    }

    // Sort lines
    Collections.sort(lines, ConfigUtility.COMPLEX_MAP_COMPARATOR);
    // Write lines
    for (String line : lines) {
      writer.write(line);
    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    // Close
    writer.flush();
    writer.close();

  }

  /**
   * Write snapshot file.
   *
   * @param prevInactiveMembers the prev inactive members
   * @param prevActiveMembers the prev active members
   * @param currentActiveMembers the current active members
   * @throws Exception the exception
   */
  private void writeSnapshotFile(
    Map<String, ComplexMapRefSetMember> prevInactiveMembers,
    Map<String, ComplexMapRefSetMember> prevActiveMembers,
    Map<String, ComplexMapRefSetMember> currentActiveMembers) throws Exception {

    Logger.getLogger(getClass()).info("Writing snapshot...");
    String pattern =
        (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap
            ? "iissscRefset_" : "iisssccRefset_");
    String filename = null;
    BufferedWriter writer = null;
    filename =
        outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "Snapshot_INT_" + effectiveTime + ".txt";

    // write headers
    Logger.getLogger(getClass()).info("  snapshot file:  " + filename);

    writer = new BufferedWriter(new FileWriter(filename));
    writer
        .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\t"
            + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId");
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      writer.write("\tmapCategoryId");
    }
    writer.write("\r\n");

    List<String> lines = new ArrayList<>();

    // Write previously inactive members that are not active now
    for (String key : prevInactiveMembers.keySet()) {
      if (!currentActiveMembers.containsKey(key)) {
        // write out previous inactive line
        lines.add(getOutputLine(prevInactiveMembers.get(key), true));
      } else {
        // write out the current active line
        lines.add(getOutputLine(currentActiveMembers.get(key), true));
      }
    }

    // Write previous active members (changed, unchanged, or inactive)
    for (String key : prevActiveMembers.keySet()) {
      if (!currentActiveMembers.containsKey(key)) {
        ComplexMapRefSetMember member = prevActiveMembers.get(key);
        member.setActive(false);
        lines.add(getOutputLine(member, true));
        member.setActive(true);
      } else {
        ComplexMapRefSetMember member = currentActiveMembers.get(key);
        ComplexMapRefSetMember member2 = prevActiveMembers.get(key);
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
    for (String key : currentActiveMembers.keySet()) {
      if (!prevActiveMembers.containsKey(key)
          && !prevInactiveMembers.containsKey(key)) {
        lines.add(getOutputLine(currentActiveMembers.get(key), true));
      }
    }

    // Sort lines
    Collections.sort(lines, ConfigUtility.COMPLEX_MAP_COMPARATOR);
    // Write lines
    for (String line : lines) {
      writer.write(line);
    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    // Close
    writer.flush();
    writer.close();

  }

  /**
   * Write human readable file.
   *
   * @param members the members
   * @throws Exception the exception
   */
  private void writeHumanReadableFile(
    Map<String, ComplexMapRefSetMember> members) throws Exception {

    // Open file and writer
    String humanReadableFileName = null;
    BufferedWriter humanReadableWriter = null;
    String camelCaseName =
        mapProject.getDestinationTerminology().substring(0, 1)
            + mapProject.getDestinationTerminology().substring(1).toLowerCase();
    humanReadableFileName =
        outputDir + "/tls_" + camelCaseName + "HumanReadableMap_INT_"
            + effectiveTime + ".tsv";
    humanReadableWriter =
        new BufferedWriter(new FileWriter(humanReadableFileName));

    // Write headers (subject to pattern)
    MapRefsetPattern pattern = mapProject.getMapRefsetPattern();
    if (pattern == MapRefsetPattern.ExtendedMap) {
      if (humanReadableWriter != null) {
        humanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tmapCategoryId\tmapCategoryName\r\n");
        humanReadableWriter.flush();
      }
    } else if (pattern == MapRefsetPattern.ComplexMap) {
      if (humanReadableWriter != null) {
        humanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tcorrelationValue\r\n");
        humanReadableWriter.flush();
      }
    }

    // Write entries
    List<String> lines = new ArrayList<>();
    for (ComplexMapRefSetMember member : members.values()) {

      // get the map relation name for the human readable file
      MapRelation mapRelation = null;
      for (MapRelation mr : mapProject.getMapRelations()) {
        if (mr.getTerminologyId().equals(member.getMapRelationId().toString())) {
          mapRelation = mr;
        }
      }

      // get target concept, if not null for its preferred name
      Concept targetConcept = null;
      if (member.getMapTarget() != null && !member.getMapTarget().isEmpty()) {
        targetConcept =
            contentService.getConcept(member.getMapTarget(),
                mapProject.getDestinationTerminology(),
                mapProject.getDestinationTerminologyVersion());
      }

      // switch line on map relation style
      String entryLine = null;
      if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
        entryLine =
            member.getTerminologyId()
                + "\t"
                + effectiveTime
                + "\t"
                + (member.isActive() ? "1" : "0")
                + "\t"
                + moduleId
                + "\t"
                + member.getRefSetId()
                + "\t"
                + member.getConcept().getTerminologyId()
                + "\t"
                + member.getConcept().getDefaultPreferredName()
                + "\t"
                + member.getMapGroup()
                + "\t"
                + member.getMapPriority()
                + "\t"
                + (mapProject.isRuleBased() ? member.getMapRule() : "")
                + "\t"
                + member.getMapAdvice()
                + "\t"
                + (member.getMapTarget() == null ? "" : member.getMapTarget())
                + "\t"
                + (targetConcept != null ? targetConcept
                    .getDefaultPreferredName() : "")
                + "\t"
                + "447561005"
                + "\t" // fixed value for Extended map
                + member.getMapRelationId()
                + "\t"
                + (mapRelation != null ? mapRelation.getName()
                    : "FAILED MAP RELATION");

        // ComplexMap style is identical to ExtendedMap
        // with the exception of the terminating map relation terminology id
      } else if (mapProject.getMapRefsetPattern().equals(
          MapRefsetPattern.ComplexMap)) {
        entryLine =
            member.getTerminologyId() // the UUID
                + "\t"
                + effectiveTime
                + "\t"
                + (member.isActive() ? "1" : "0")
                + "\t"
                + moduleId
                + "\t"
                + member.getRefSetId()
                + "\t"
                + member.getConcept().getTerminologyId()
                + "\t"
                + member.getConcept().getDefaultPreferredName()
                + "\t"
                + member.getMapGroup()
                + "\t"
                + member.getMapPriority()
                + "\t"
                + (mapProject.isRuleBased() ? member.getMapRule() : "")
                + "\t"
                + member.getMapAdvice()
                + "\t"
                + member.getMapTarget()
                + "\t"
                + (targetConcept != null ? targetConcept
                    .getDefaultPreferredName() : "")
                + "\t"
                + member.getMapRelationId()
                + "\t"
                + (mapRelation != null ? mapRelation.getName()
                    : "FAILED MAP RELATION");
      }

      entryLine += "\r\n";
      lines.add(entryLine);
    }
    // Sort lines
    Collections.sort(lines, ConfigUtility.TSV_COMPARATOR);
    // Write file
    for (String line : lines) {
      humanReadableWriter.write(line);
    }

    // Close
    humanReadableWriter.flush();
    humanReadableWriter.close();

  }

  /**
   * Helper function to retrieve a map record for a given tree position. If in
   * set, returns that map record, if not, retrieves and adds it if possible.
   *
   * @param terminologyId the terminology id
   * @return the map record for terminology id
   * @throws Exception the exception
   */
  private MapRecord getMapRecordForTerminologyId(String terminologyId)
    throws Exception {

    // if in cache, use cached records
    if (mapRecordMap.containsKey(terminologyId)) {
      return mapRecordMap.get(terminologyId);

    } else {

      MapRecord mapRecord = null;
      // if not in cache yet, get record(s) for this concept
      MapRecordList mapRecordList =
          mappingService.getMapRecordsForProjectAndConcept(mapProject.getId(),
              terminologyId);

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
          throw new Exception("Unable to find map record for " + terminologyId);
        }
      } else if (mapRecordList.getCount() > 1) {
        throw new Exception("Multiple map records found for " + terminologyId);
      } else {
        mapRecord = mapRecordList.getMapRecords().iterator().next();

        // if ready for publication, add to map
        if (mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.READY_FOR_PUBLICATION)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          // add to map record map and return it
          mapRecordMap.put(terminologyId, mapRecord);
          return mapRecord;
        } else {
          throw new Exception("Invalid workflow status "
              + mapRecord.getWorkflowStatus() + " on record for "
              + terminologyId);
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
  private String getHash(ComplexMapRefSetMember c) {
    return c.getRefSetId() + c.getConcept().getTerminologyId()
        + c.getMapGroup() + c.getMapRule() + c.getMapTarget();
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
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws ParseException
   */
  private ComplexMapRefSetMember getComplexMapRefSetMemberForMapEntry(
    MapEntry mapEntry, MapRecord mapRecord, MapProject mapProject,
    Concept concept) throws IOException, NoSuchAlgorithmException,
    ParseException {

    ComplexMapRefSetMember complexMapRefSetMember =
        new ComplexMapRefSetMemberJpa();

    // set the base parameters
    // NOTE: do not set UUID here, done in main logic
    complexMapRefSetMember.setConcept(concept);
    complexMapRefSetMember.setRefSetId(mapProject.getRefSetId());
    complexMapRefSetMember.setModuleId(new Long(moduleId));
    complexMapRefSetMember.setActive(true);
    complexMapRefSetMember.setEffectiveTime(dateFormat.parse(effectiveTime));
    complexMapRefSetMember.setTerminology(mapProject.getSourceTerminology());
    complexMapRefSetMember.setTerminologyVersion(mapProject
        .getSourceTerminologyVersion());

    // set parameters from the map entry
    complexMapRefSetMember.setMapGroup(mapEntry.getMapGroup());
    complexMapRefSetMember.setMapPriority(mapEntry.getMapPriority());
    complexMapRefSetMember.setMapRule(mapProject.isRuleBased() ? mapEntry
        .getRule() : "");
    complexMapRefSetMember.setMapRelationId(new Long(mapEntry.getMapRelation()
        .getTerminologyId()));
    complexMapRefSetMember.setMapTarget(mapEntry.getTargetId() == null ? ""
        : mapEntry.getTargetId());

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

    for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
      sortedAdvices.add(mapAdvice.getDetail());
    }

    // check for context dependent advice
    if (mapEntry.getRule().startsWith("IFA") && mapEntry.getTargetId() != null
        && !mapEntry.getTargetId().isEmpty()) {

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
    for (String advice : sortedAdvices) {
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
    String rule =
        "IFA " + mapRecord.getConceptId() + " | "
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
   */
  private String getHumanReadableMapAdvice(MapEntry mapEntry) {

    String advice = "";

    // Construct advice only if using Extended Map pattern
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {

      // System.out.println("Constructing human-readable advice for:  "
      // + mapEntry.getRule());

      String[] comparatorComponents; // used for parsing age rules

      // if map target is blank
      if (mapEntry.getTargetId() == null || mapEntry.getTargetId() == "") {
        // System.out.println("  Use map relation");
        advice = mapEntry.getMapRelation().getName();
      }

      // if map rule is IFA (age)
      else if (mapEntry.getRule().toUpperCase().contains("AGE")) {
        // IF AGE AT ONSET OF
        // CLINICAL FINDING BETWEEN 1.0 YEAR AND 18.0 YEARS CHOOSE
        // M08.939

        // Rule examples
        // IFA 104831000119109 | Drug induced central sleep apnea
        // (disorder) | AND IFA 445518008 | Age at onset of clinical
        // finding
        // (observable
        // entity) | < 65 years
        // IFA 104831000119109 | Drug induced central sleep apnea
        // (disorder) | AND IFA 445518008 | Age at onset of clinical
        // finding
        // (observable entity) | <= 28.0 days
        // (disorder)

        // split by pipe (|) character. Expected fields
        // 0: IFA conceptId
        // 1: conceptName
        // 2: AND IFA ageConceptId
        // 3: Age rule type (Age at onset, Current chronological age)
        // 4: Comparator, Value, Units (e.g. < 65 years)
        // ---- The following only exist for two-value age rules
        // 5: AND IFA ageConceptId
        // 6: Age rule type (Age at onset, Current chronological age
        // 7: Comparator, Value, Units
        String[] ruleComponents = mapEntry.getRule().split("|");

        // add the type of age rule
        advice = "IF " + ruleComponents[3];

        // if a single component age rule, construct per example:
        // IF CURRENT CHRONOLOGICAL AGE ON OR AFTER 15.0 YEARS CHOOSE
        // J20.9
        if (ruleComponents.length == 5) {

          comparatorComponents = ruleComponents[4].split(" ");

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
              break;
          }

          // add the value and units
          advice +=
              " " + comparatorComponents[1] + " " + comparatorComponents[2];

          // otherwise, if a double-component age rule, construct per
          // example
          // IF AGE AT ONSET OF CLINICAL FINDING BETWEEN 1.0 YEAR AND
          // 18.0
          // YEARS CHOOSE M08.939
        } else if (ruleComponents.length == 8) {

          advice += " BETWEEN ";

          // get the first comparator/value/units triple
          comparatorComponents = ruleComponents[4].split(" ");

          advice += comparatorComponents[1] + " " + comparatorComponents[2];
        }

        // finally, add the CHOOSE {targetId}
        advice += " CHOOSE " + mapEntry.getTargetId();

        // if a gender rule (i.e. contains (FE)MALE)
      } else if (mapEntry.getRule().toUpperCase().contains("MALE")) {

        // add the advice based on gender
        if (mapEntry.getRule().toUpperCase().contains("FEMALE")) {
          advice += "IF FEMALE CHOOSE " + mapEntry.getTargetId();
        } else {
          advice += "IF MALE CHOOSE " + mapEntry.getTargetId();
        }
      } // if not an IFA rule (i.e. TRUE, OTHERWISE TRUE), simply return
        // ALWAYS
      else if (!mapEntry.getRule().toUpperCase().contains("IFA")) {

        advice = "ALWAYS " + mapEntry.getTargetId();

        // otherwise an IFA rule
      } else {
        String[] ifaComponents = mapEntry.getRule().toUpperCase().split("\\|");

        // remove any (disorder), etc.
        String targetName = ifaComponents[1].trim();

        // if classifier (e.g. (disorder)) present, remove it and any trailing
        // spaces
        if (targetName.lastIndexOf("(") != -1)
          targetName =
              targetName.substring(0, targetName.lastIndexOf("(")).trim();

        advice = "IF " + targetName + " CHOOSE " + mapEntry.getTargetId();
      }

      // System.out.println("   Human-readable advice: " + advice);
    }

    return advice;

  }

  /**
   * Takes a tree position graph and converts it to a sorted list of tree
   * positions where order is based on depth in tree.
   * 
   * @param tp the tp
   * @return the sorted tree position descendant list
   * @throws Exception the exception
   */
  private List<TreePosition> getSortedTreePositionDescendantList(TreePosition tp)
    throws Exception {

    // construct list of unprocessed tree positions and initialize with root
    // position
    List<TreePosition> positionsToAdd = new ArrayList<>();
    positionsToAdd.add(tp);

    List<TreePosition> sortedTreePositionDescendantList = new ArrayList<>();

    while (!positionsToAdd.isEmpty()) {

      // add the first element
      sortedTreePositionDescendantList.add(positionsToAdd.get(0));

      // add the children of first element
      for (TreePosition childTp : positionsToAdd.get(0).getChildren()) {
        positionsToAdd.add(childTp);
      }

      // remove the first element
      positionsToAdd.remove(0);
    }

    // sort the tree positions by position in the hierarchy (e.g. # of ~
    // characters)
    Collections.sort(sortedTreePositionDescendantList,
        new Comparator<TreePosition>() {
          @Override
          public int compare(TreePosition tp1, TreePosition tp2) {
            int levels1 =
                tp1.getAncestorPath().length()
                    - tp1.getAncestorPath().replace("~", "").length();
            int levels2 =
                tp1.getAncestorPath().length()
                    - tp1.getAncestorPath().replace("~", "").length();

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
  private String getOutputLine(ComplexMapRefSetMember member,
    boolean trueEffectiveTimeFlag) throws Exception {

    String entryLine = "";

    // switch line on map relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      entryLine =
          member.getTerminologyId() // the UUID
              + "\t"
              + (trueEffectiveTimeFlag ? dateFormat.format(member
                  .getEffectiveTime()) : effectiveTime)
              + "\t"
              + (member.isActive() ? "1" : "0") + "\t" + moduleId
              + "\t"
              + member.getRefSetId()
              + "\t"
              + member.getConcept().getTerminologyId()
              + "\t"
              + member.getMapGroup() + "\t" + member.getMapPriority()
              + "\t"
              + (mapProject.isRuleBased() ? member.getMapRule() : "")
              + "\t"
              + member.getMapAdvice() + "\t" + member.getMapTarget()
              + "\t"
              + "447561005" + "\t" + member.getMapRelationId();

      // ComplexMap style is identical to ExtendedMap
      // with the exception of the terminating map relation terminology id
    } else if (mapProject.getMapRefsetPattern().equals(
        MapRefsetPattern.ComplexMap)) {
      entryLine =
          member.getTerminologyId() // the UUID
              + "\t"
              + (trueEffectiveTimeFlag ? dateFormat.format(member
                  .getEffectiveTime()) : effectiveTime)
              + "\t"
              + (member.isActive() ? "1" : "0") + "\t" + moduleId
              + "\t"
              + member.getRefSetId()
              + "\t"
              + member.getConcept().getTerminologyId()
              + "\t"
              + member.getMapGroup() + "\t" + member.getMapPriority()
              + "\t"
              + member.getMapRule() + "\t" + member.getMapAdvice()
              + "\t"
              + member.getMapTarget() + "\t" + member.getMapRelationId();
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

    String dpnTypeId =
        properties.getProperty("loader.defaultPreferredNames.typeId");
    String dpnRefSetId =
        properties.getProperty("loader.defaultPreferredNames.refSetId");
    String dpnAcceptabilityId =
        properties.getProperty("loader.defaultPreferredNames.acceptabilityId");

    // Compute preferred names
    int ct = 0;
    for (MapRecord mapRecord : mapRecords) {
      ct++;
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      conceptCache.put(concept.getTerminologyId(), concept);
      if (testModeFlag) {
        defaultPreferredNames.put(concept.getTerminologyId(),
            concept.getDefaultPreferredName());
      } else {
        defaultPreferredNames.put(
            concept.getTerminologyId(),
            computeDefaultPreferredName(concept, dpnTypeId, dpnRefSetId,
                dpnAcceptabilityId));
      }
      if (ct % 5000 == 0) {
        Logger.getLogger(getClass()).info("    count = " + ct);
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
  private String computeDefaultPreferredName(Concept concept, String dpnTypeId,
    String dpnRefSetId, String dpnAcceptabilityId) throws Exception {

    if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
      return defaultPreferredNames.get(concept.getTerminologyId());
    } else {

      // cycle over descriptions
      for (Description description : concept.getDescriptions()) {

        // if active and type id matches
        if (description.isActive()
            && description.getTypeId().equals(new Long(dpnTypeId))) {

          // cycle over language ref sets
          for (LanguageRefSetMember language : description
              .getLanguageRefSetMembers()) {

            if (language.getRefSetId().equals(dpnRefSetId)
                && language.isActive()
                && language.getAcceptabilityId().equals(
                    new Long(dpnAcceptabilityId))) {

              defaultPreferredNames.put(concept.getTerminologyId(),
                  description.getTerm());

              // Report info if semantic tag cannot be found
              if (!description.getTerm().trim().endsWith(")")) {
                Logger.getLogger(getClass()).warn(
                    "Could not find semantic tag for concept "
                        + concept.getTerminologyId() + ", name selected="
                        + description.getTerm());
                for (Description d : concept.getDescriptions()) {
                  Logger.getLogger(getClass()).warn(
                      "Description " + d.getTerminologyId() + ", active="
                          + d.isActive() + ", typeId = " + d.getTypeId());
                  for (LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
                    Logger.getLogger(getClass()).warn(
                        "    Language Refset Member " + l.getTerminologyId()
                            + ", active = " + l.isActive() + ", refsetId="
                            + l.getRefSetId() + ", acceptabilityId = "
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
          "Could not retrieve default preferred name for Concept "
              + concept.getTerminologyId());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#beginRelease(boolean
   * )
   */
  @Override
  public void beginRelease() throws Exception {

    // instantiate required services
    MappingService mappingService = new MappingServiceJpa();
    ReportService reportService = new ReportServiceJpa();
    reportService.setTransactionPerOperation(false);

    // get the report definition
    Logger.getLogger(getClass()).info("  Create release QA report");
    ReportDefinition reportDefinition = null;
    for (ReportDefinition rd : mapProject.getReportDefinitions()) {
      if (rd.getName().equals("Release QA"))
        reportDefinition = rd;
    }

    if (reportDefinition == null) {
      throw new Exception(
          "Could not get report definition matching 'Release QA'");
    }
    // create the report QA object and instantiate fields
    Report report = new ReportJpa();
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

    // begin the transaction and add/persist the report
    reportService.beginTransaction();
    reportService.addReport(report);

    // get all scope concept terminology ids for this project
    Logger.getLogger(getClass()).info("  Get scope concepts for map project");
    Set<String> scopeConceptTerminologyIds = new HashSet<>();
    for (SearchResult sr : mappingService.findConceptsInScope(
        mapProject.getId(), null).getSearchResults()) {
      scopeConceptTerminologyIds.add(sr.getTerminologyId());
    }

    Logger.getLogger(getClass()).info(
        "    count = " + scopeConceptTerminologyIds.size());

    // get all map records for this project
    Logger.getLogger(getClass()).info("  Get records for map project");
    MapRecordList mapRecords =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(getClass()).info("    count = " + mapRecords.getCount());

    // create a temp set of scope terminology ids
    Set<String> conceptsWithNoRecord =
        new HashSet<>(scopeConceptTerminologyIds);
    List<MapRecord> mapRecordsToProcess = mapRecords.getMapRecords();

    // for each map record, check for errors
    // NOTE: Report Result names are constructed from error lists assigned
    // Each individual result is stored as a Report Result Item
    Logger.getLogger(getClass()).info("  Validate records");
    boolean errorFlag = false;
    while (mapRecordsToProcess.size() != 0) {

      // extract the concept and remove it from list
      MapRecord mapRecord = mapRecordsToProcess.get(0);
      mapRecordsToProcess.remove(0);
      Logger.getLogger(getClass()).debug(
          "    concept = " + mapRecord.getConceptId() + " "
              + mapRecord.getConceptName());

      // first, remove this concept id from the dynamic conceptsWithNoRecord set
      conceptsWithNoRecord.remove(mapRecord.getConceptId());

      // constuct a list of errors for this concept
      List<String> resultMessages = new ArrayList<>();

      // CHECK: Map record is READY_FOR_PUBLICATION or PUBLISHED
      if (!mapRecord.getWorkflowStatus().equals(
          WorkflowStatus.READY_FOR_PUBLICATION)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
        resultMessages.add("Not marked ready for publication");
        errorFlag = true;
        // if record is ready for publication
      } else {
        // CHECK: Map record (must be ready for publication) passes project
        // specific validation checks
        ValidationResult result = algorithmHandler.validateRecord(mapRecord);
        if (!result.isValid()) {
          Logger.getLogger(getClass()).debug("    FAILED");
          errorFlag = true;
          resultMessages.add("Failed validation check");
        } else {
          resultMessages.add("Ready for publication");
        }
      }

      // Remove out of scope concepts if not in test mode
      if (!scopeConceptTerminologyIds.contains(mapRecord.getConceptId())) {

        // construct message based on whether record is to be removed
        String reportMsg = "Concept not in scope";

        // separate error-type by previously-published or this-cycle-edited
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))
          resultMessages.add(reportMsg + " - previously published");
        else
          resultMessages.add(reportMsg + " - edited this cycle");

        // remove record if flag set
        if (!testModeFlag) {
          Logger.getLogger(getClass()).debug("    REMOVE out of scope records");
          mappingService.removeMapRecord(mapRecord.getId());
        }
      }

      // Add all reported errors to the report
      for (String error : resultMessages) {
        addReportError(report, mapProject, mapRecord.getConceptId(),
            mapRecord.getConceptName(), error);
      }
    }

    // CHECK: In-scope concepts with no map record
    Logger.getLogger(getClass()).debug(
        "  Report in scope concepts with no record");
    for (String terminologyId : conceptsWithNoRecord) {

      // get the concept
      Concept c =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      addReportError(report, mapProject, terminologyId,
          c.getDefaultPreferredName(), "In-scope concept has no map record");
      errorFlag = true;
    }

    Logger.getLogger(getClass()).info("  Adding Release QA Report");
    Logger.getLogger(getClass()).info(
        "    Log into the application to see the report results");

    // commit the report
    // TODO: need a way to override the errors if we want to proceed with a
    // release anyway
    if (errorFlag) {
      mappingService.rollback();
    } else {
      mappingService.commit();
    }

    // Commit the new report either way
    reportService.commit();

    Logger.getLogger(getClass()).info("Done.");

    mappingService.close();
    reportService.close();

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
  private void addReportError(Report report, MapProject mapProject,
    String terminologyId, String conceptName, String error) {

    ReportResult reportResult = null;

    // find the report result corresponding to this error, if it exists
    for (ReportResult rr : report.getResults()) {
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
    for (ReportResultItem item : reportResult.getReportResultItems()) {
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#finishRelease()
   */
  @Override
  public void finishRelease() throws Exception {

    if (mapRecords == null || mapRecords.isEmpty()) {
      MapRecordList mapRecordList =
          mappingService
              .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                  mapProject.getId(), null);
      mapRecords = mapRecordList.getMapRecords();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      for (MapRecord record : mapRecords) {
        if (record.getWorkflowStatus() == WorkflowStatus.READY_FOR_PUBLICATION) {
          Logger.getLogger(getClass()).info(
              "  Update record to PUBLISHED for " + record.getConceptId() + " "
                  + record.getConceptName());
          if (!testModeFlag) {
            record.setWorkflowStatus(WorkflowStatus.PUBLISHED);
            mappingService.updateMapRecord(record);
          }
        }
      }
      mappingService.commit();
    }

    // skip if in test mode
    if (!testModeFlag) {
      // clear old map refset
      Logger.getLogger(getClass()).info("  Clear map refset");
      clearMapRefSet();
      // Load map refset
      Logger.getLogger(getClass()).info("  Load map refset");
      loadMapRefSet();
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

    for (ComplexMapRefSetMember member : contentService
        .getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId())
        .getIterable()) {
      Logger.getLogger(getClass()).debug("    Remove member - " + member);
      if (!testModeFlag) {
        contentService.removeComplexMapRefSetMember(member.getId());
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
    Logger.getLogger(getClass()).info("    Open " + inputFile);
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

        // ComplexMap unique attributes
        member.setMapGroup(Integer.parseInt(fields[6]));
        member.setMapPriority(Integer.parseInt(fields[7]));
        member.setMapRule(fields[8]);
        member.setMapAdvice(fields[9]);
        member.setMapTarget(fields[10]);
        if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
          member.setMapRelationId(Long.valueOf(fields[11]));
        } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
          member.setMapRelationId(Long.valueOf(fields[12]));

        } else {
          throw new Exception("Unsupported map type "
              + mapProject.getMapRefsetPattern());
        }

        // ComplexMap unique attributes NOT set by file (mapBlock
        // elements) - set defaults
        member.setMapBlock(0);
        member.setMapBlockRule(null);
        member.setMapBlockAdvice(null);

        // Terminology attributes
        member.setTerminology(terminology);
        member.setTerminologyVersion(version);

        // set Concept
        final Concept concept =
            contentService.getConcept(fields[5], terminology, version);

        // regularly log at intervals
        if (++objectCt % 5000 == 0) {
          Logger.getLogger(getClass()).info("    count = " + objectCt);
        }

        if (concept != null) {
          Logger.getLogger(getClass()).debug("    Add member - " + member);
          if (!testModeFlag) {
            member.setConcept(concept);
            contentService.addComplexMapRefSetMember(member);
          }
        } else {
          throw new Exception("Member references non-existent concept - "
              + member);
        }

      }
    }

    // commit any remaining objects
    contentService.commit();
    contentService.close();
    reader.close();

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

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setEffectiveTime
   * (java.lang.String)
   */
  @Override
  public void setEffectiveTime(String effectiveTime) {
    this.effectiveTime = effectiveTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setModuleId(java
   * .lang.String)
   */
  @Override
  public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setOutputDir(java
   * .lang.String)
   */
  @Override
  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setWriteSnapshot
   * (boolean)
   */
  @Override
  public void setWriteSnapshot(boolean writeSnapshot) {
    this.writeSnapshot = writeSnapshot;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setWriteDelta(boolean
   * )
   */
  @Override
  public void setWriteDelta(boolean writeDelta) {
    this.writeDelta = writeDelta;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setMapProject(org
   * .ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void setMapProject(MapProject mapProject)
    throws InstantiationException, IllegalAccessException,
    ClassNotFoundException {
    this.mapProject = mapProject;
    // instantiate the algorithm handler
    algorithmHandler =
        mappingService.getProjectSpecificAlgorithmHandler(mapProject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setMapRecords(java
   * .util.List)
   */
  @Override
  public void setMapRecords(List<MapRecord> mapRecords) {
    this.mapRecords = mapRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#setInputFile(java
   * .lang.String)
   */
  @Override
  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

}
