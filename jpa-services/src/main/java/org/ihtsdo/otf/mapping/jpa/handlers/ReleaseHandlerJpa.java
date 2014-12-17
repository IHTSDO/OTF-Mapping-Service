/*
 * 
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.UUID;

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
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * JPA enabled implementation of {@link ReleaseHandler}.
 */
public class ReleaseHandlerJpa implements ReleaseHandler {

  /** The mapping service. */
  private MappingService mappingService;

  /** The content service. */
  private ContentService contentService;

  /** The effectiveTime. */
  private String effectiveTime;

  /** The module id. */
  private String moduleId;

  /** The output dir. */
  private String outputDir;

  /** THe flags for writing snapshot and delta. */
  private boolean writeSnapshot = false;

  /** The write delta. */
  private boolean writeDelta = false;

  /** The map project. */
  private MapProject mapProject = null;

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

  /** The report statistics. */
  private Map<String, Integer> reportStatistics = new HashMap<>();

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
   * @throws Exception the exception
   */
  public ReleaseHandlerJpa() throws Exception {

    // instantiate services
    mappingService = new MappingServiceJpa();
    contentService = new ContentServiceJpa();

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
    Logger.getLogger(getClass()).info("Processing publication release");
    Logger.getLogger(getClass()).info("  project = " + mapProject.getName());
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
    ProjectSpecificAlgorithmHandler algorithmHandler =
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

    // instantiate the set of complex map ref set members to write
    Map<String, ComplexMapRefSetMember> complexMapRefSetMembersToWrite =
        new HashMap<>();

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
    ComplexMapRefSetMemberList refSetMembers =
        contentService.getComplexMapRefSetMembersForRefSetId(mapProject
            .getRefSetId());

    // construct map of existing complex ref set members by UUID fields
    // this is used for comparison purposes later
    // after record processing, the remaining ref set members
    // represent those entries that are now inactive
    Map<String, ComplexMapRefSetMember> complexMapRefSetMemberMap =
        new HashMap<>();
    for (ComplexMapRefSetMember c : refSetMembers.getComplexMapRefSetMembers()) {
      complexMapRefSetMemberMap.put(getHash(c), c);
    }

    // output size of each collection
    Logger.getLogger(getClass()).info(
        "    Cached distinct UUID-quintuples = "
            + complexMapRefSetMemberMap.keySet().size());
    Logger.getLogger(getClass()).info(
        "    Existing complex ref set members for project = "
            + refSetMembers.getCount());

    // if sizes do not match, output warning
    if (complexMapRefSetMemberMap.keySet().size() != refSetMembers.getCount()) {
      throw new Exception(
          "UUID-quintuples count does not match refset member count");
    }

    // clear the ref set members list (no longer used)
    refSetMembers = null;

    // /////////////////////////////////////////////////////
    // Perform the release
    // /////////////////////////////////////////////////////

    Logger.getLogger(getClass()).info("  Processing release");

    // Prep MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT | 447639009
    MapRelation ifaRuleRelation = null;
    for (MapRelation rel : mappingService.getMapRelations().getMapRelations()) {
      if (rel.getTerminologyId().equals("447639009")) {
        ifaRuleRelation = rel;
        break;
      }
    }
    if (ifaRuleRelation == null) {
      throw new Exception(
          "Unable to find map relation for MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT "
              + "| 447639009");
    }

    // cycle over the map records marked for publishing
    for (MapRecord mapRecord : mapRecords) {

      boolean anyNc = false;
      boolean allNc = true;

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
        } catch (Exception e) {
          conceptErrors.put(mapRecord.getConceptId(),
              "Could not retrieve any tree position");
          continue;
        }

        // check if tree positions were successfully retrieved
        if (treePosition == null) {
          conceptErrors.put(mapRecord.getConceptId(),
              "Could not retrieve tree positions");
          continue;
        }

        // get a list of tree positions sorted by position in hiearchy
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

          if (!descendantsProcessed.contains(tp.getTerminologyId())) {

            // add this descendant to the processed list
            descendantsProcessed.add(tp.getTerminologyId());

            // get the parent map record for this tree position
            // used to check if entries are duplicated on parent
            String parent =
                tp.getAncestorPath().substring(
                    tp.getAncestorPath().lastIndexOf("~") + 1);
            MapRecord mrParent = getMapRecordForTerminologyId(parent);

            // skip the root level record, these entries are added
            // below, after the up-propagated entries
            if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

              // get the map record corresponding to this specific
              // ancestor path + concept Id
              MapRecord mr =
                  getMapRecordForTerminologyId(tp.getTerminologyId());

              if (mr != null) {

                /*
                 * Logger.getLogger(getClass()).info(
                 * "     Adding entries from map record " + mr.getId() + ", " +
                 * mr.getConceptId() + ", " + mr.getConceptName());
                 */

                // if no parent, continue, but log error
                if (mrParent == null) {

                  mrParent = new MapRecordJpa(); // create a blank for
                                                 // comparison
                  conceptErrors.put(tp.getTerminologyId(),
                      "Could not retrieve parent record along ancestor path "
                          + tp.getAncestorPath());
                }

                // cycle over the entries
                for (MapEntry me : mr.getMapEntries()) {

                  // get the current list of entries for this
                  // group
                  List<MapEntry> existingEntries =
                      entriesByGroup.get(me.getMapGroup());

                  if (existingEntries == null)
                    existingEntries = new ArrayList<>();

                  // flag for whether this entry is a duplicate of
                  // an
                  // existing or parent entry
                  boolean isDuplicateEntry = false;

                  // compare to the entries on the parent record
                  // (this
                  // produces short-form)
                  // NOTE: This uses unmodified rules,
                  for (MapEntry parentEntry : mrParent.getMapEntries()) {

                    if (parentEntry.getMapGroup() == me.getMapGroup()
                        && parentEntry.isEquivalent(me))
                      isDuplicateEntry = true;
                  }

                  // if not a duplicate entry, add it to the map
                  if (!isDuplicateEntry) {

                    // create new map entry to prevent
                    // hibernate-managed entity modification
                    // TODO This probably could be handled by
                    // the
                    // entry copy routines
                    // for testing purposes, doing this
                    // explicitly
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

                    // replace existing list with modified list
                    // entriesByGroup.put(newEntry.getMapGroup(),
                    // existingEntries);

                  }
                }
              } else {
                conceptErrors
                    .put(tp.getTerminologyId(),
                        "No record exists for descendant concept, cannot up-propagate");
              }
            }
          }
        }

      }

      // /////////////////////////////////////////////////////
      // Add the original (non-propagated) entries
      // /////////////////////////////////////////////////////

      /*
       * Logger.getLogger(getClass()).info( "     Adding original entries");
       */
      for (MapEntry me : mapRecord.getMapEntries()) {

        /*
         * Logger.getLogger(getClass()).info( "       Adding entry " +
         * me.getId());
         */

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

        if (me.getTargetId() == null || me.getTargetId().isEmpty()) {
          anyNc = true;
        } else {
          allNc = false;
        }

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
            // entriesByGroup.put(mapGroup, existingEntries);

          }
        }
      }

      // /////////////////////////////////////////////////////
      // Convert the record to complex map ref set members
      // /////////////////////////////////////////////////////

      // get the concept
      Concept concept = conceptCache.get(mapRecord.getConceptId());

      // cycle over groups and entries in sequence
      for (int mapGroup : entriesByGroup.keySet()) {

        int mapPriority = 1;

        for (MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

          // convert this map entry into a complex map ref set member
          ComplexMapRefSetMember member =
              convertMapEntryToComplexMapRefSetMemberMapEntry(mapEntry,
                  mapRecord, mapProject, concept);

          String uuidStr = getHash(member);

          // attempt to retrieve any existing complex map ref set
          // member
          ComplexMapRefSetMember existingMember =
              complexMapRefSetMemberMap.get(uuidStr);

          // if existing found, re-use uuid, otherwise generate new
          if (existingMember == null) {
            member.setTerminologyId(getReleaseUuid(uuidStr).toString());
          } else {
            member.setTerminologyId(existingMember.getTerminologyId());
          }

          // assign and increment map priority
          member.setMapPriority(mapPriority++);

          // add this entry to the list of members to write
          complexMapRefSetMembersToWrite.put(member.getTerminologyId(), member);

        }
      }

      // total concepts mapped
      updateStat(Stats.CONCEPTS_MAPPED.getValue());

      // total concepts mapped, by semantic tag (finding, disorder....)
      String dpn = defaultPreferredNames.get(concept.getTerminologyId());
      String semanticTag;
      if (dpn.endsWith(")")) {
        semanticTag =
            dpn.lastIndexOf("(") == -1 ? "(Badly formed semantic tag)" : dpn
                .substring(dpn.lastIndexOf("("));
      } else {
        semanticTag = "(No semantic tag)";
      }
      updateStat(Stats.CONCEPTS_MAPPED.getValue() + semanticTag);

      // concepts with complex maps (more than one entry)
      if (mapRecord.getMapEntries().size() > 1)
        updateStat(Stats.COMPLEX_MAPS.getValue());

      // concepts with multiple groups
      if (entriesByGroup.keySet().size() > 1)
        updateStat(Stats.MULTIPLE_GROUPS.getValue());

      // statistics based on not mappable entries
      if (!anyNc) {
        updateStat(Stats.ALWAYS_MAP.getValue());
      } else if (allNc) {
        updateStat(Stats.NEVER_MAP.getValue());
      } else {
        updateStat(Stats.SOMETIMES_MAP.getValue());
      }

      // max number of entries for a concept
      int nEntriesTotal = 0;
      for (int grp : entriesByGroup.keySet()) {
        nEntriesTotal += entriesByGroup.get(grp).size();
      }
      updateStatisticMax(Stats.MAX_ENTRIES.getValue(), nEntriesTotal);

      // clear the service -- memory management
      contentService.clear();

    }

    // /////////////////////////////////////////////////////
    // Prepare for file write
    // /////////////////////////////////////////////////////

    // declare maps in use for computation
    Map<String, ComplexMapRefSetMember> previousActiveMembers = new HashMap<>();

    // First, construct set of previously active complex map ref set members
    for (ComplexMapRefSetMember c : complexMapRefSetMemberMap.values()) {
      if (c.isActive())
        previousActiveMembers.put(c.getTerminologyId(), c);
    }

    // Collect active only entries
    List<ComplexMapRefSetMember> activeMembers = new ArrayList<>();
    for (ComplexMapRefSetMember c : complexMapRefSetMembersToWrite.values()) {
      // Only write active entries
      if (c.isActive()) {
        activeMembers.add(c);
      }
    }
    // expect the two sets above to actually be the same
    Logger.getLogger(getClass()).error(
        " members to write contains inactive entries");

    // Write human readable file
    writeHumanReadableFile(activeMembers);

    // Write snapshot file
    if (writeSnapshot) {
      writeActiveSnapshotFile(activeMembers);
    }

    // Write delta file
    if (writeDelta) {
      writeDeltaFile(complexMapRefSetMembersToWrite, previousActiveMembers);
    }

    // write the errors
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
   * Write module dependency file.
   *
   * @param moduleDependencies the module dependencies
   * @param refSetId the ref set id
   * @throws IOException
   * @throws NoSuchAlgorithmException
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
          getUuidForString(moduleId + refSetId + module).toString() + "\t"
              + effectiveTime + "\t" + "1" + "\t" + moduleId + "\t" + refSetId
              + "\t" + module + "\t" + effectiveTime + "\t" + effectiveTime
              + "\r\n";
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
   * @param previousActiveMembers the previous active members
   * @throws IOException
   */
  private void writeDeltaFile(
    Map<String, ComplexMapRefSetMember> activeMembers,
    Map<String, ComplexMapRefSetMember> previousActiveMembers) throws Exception {
    Map<String, ComplexMapRefSetMember> tempMap = new HashMap<>();

    // Open file and writer
    String filename = null;
    BufferedWriter writer = null;
    String pattern =
        (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap
            ? "iissscRefset_" : "iisssccRefset_");
    filename =
        outputDir + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "Delta_INT_" + effectiveTime + ".txt";

    // Write headers (subject to pattern)
    writer = new BufferedWriter(new FileWriter(filename));
    writer
        .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\t"
            + "mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId");
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      writer.write("\tmapCategoryId");
    }
    writer.write("\r\n");

    // case 1: currently active now modified
    // Copy current map of uuids to write into temp map
    // For each previously active uuid:
    // - check temp map for this uuid
    // - if present AND unchanged, remove from temp map
    // Write the values of the temp map
    tempMap = new HashMap<>(activeMembers);

    Logger.getLogger(getClass()).info(
        "  Computing maps created or changed this cycle from " + tempMap.size()
            + " maps marked for writing...");

    Set<String> conceptsNew = new HashSet<>();
    Set<String> conceptsModified = new HashSet<>();
    Set<String> conceptsUnchanged = new HashSet<>();

    // cycle over all previously active
    for (ComplexMapRefSetMember c : previousActiveMembers.values()) {

      // if set to write contains this previously active uuid
      if (tempMap.containsKey(c.getTerminologyId())) {

        // if this previously active member is present (equality check) in the
        // set to be written
        if (c.equals(tempMap.get(c.getTerminologyId()))) {

          // remove this concept from the set to be written -- unchanged
          tempMap.remove(c.getTerminologyId());

          conceptsUnchanged.add(c.getConcept().getTerminologyId());
        } else {
          conceptsModified.add(c.getConcept().getTerminologyId());
        }
      } else {
        conceptsNew.add(c.getConcept().getTerminologyId());
      }
    }

    // add to report statistics
    updateStatisticMax(Stats.NEW_CONCEPTS.getValue(), conceptsNew.size());
    updateStatisticMax(Stats.CHANGED_CONCEPTS.getValue(),
        conceptsModified.size());

    // write new or modified maps to file
    for (ComplexMapRefSetMember c : tempMap.values()) {
      writer.write(getOutputLine(c));
    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    updateStatisticMax(Stats.RETIRED_CONCEPTS.getValue(),
        previousActiveMembers.size());

    // case 2: previously active no longer present
    // Copy previously active map of uuids to write into temp map
    // For each uuid in current write set
    // - check temp map for this uuid
    // - if present, remove from temp map
    // Inactivate all remaining uuids in the temp map

    tempMap = new HashMap<>(previousActiveMembers);

    for (String uuid : activeMembers.keySet()) {
      if (tempMap.containsKey(uuid)) {
        tempMap.remove(uuid);
      }

    }

    updateStatisticMax("Concepts inactivated", tempMap.size());

    // set active to false and write inactivated complex maps
    for (ComplexMapRefSetMember c : tempMap.values()) {
      c.setActive(false);
      writer.write(this.getOutputLine(c));

    }

    Logger.getLogger(getClass()).info("  Writing complete.");

    writer.flush();
    writer.close();

    BufferedWriter statsWriter =
        new BufferedWriter(new FileWriter(outputDir + "/stats.txt"));

    List<String> statistics = new ArrayList<>(reportStatistics.keySet());

    Collections.sort(statistics);

    for (String statistic : statistics) {
      statsWriter.write(statistic + "\t" + reportStatistics.get(statistic)
          + "\r\n");
    }

    statsWriter.close();

  }

  /**
   * Write human readable file.
   * @throws Exception
   */
  private void writeActiveSnapshotFile(List<ComplexMapRefSetMember> members)
    throws Exception {

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
    Logger.getLogger(getClass()).info(
        "  Machine-readable release file:  " + filename);

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
    for (ComplexMapRefSetMember member : members) {
      // collect lines
      lines.add(getOutputLine(member));
    }

    // Sort lines
    Collections.sort(lines, COMPARATOR);

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
   * @throws Exception
   */
  private void writeHumanReadableFile(List<ComplexMapRefSetMember> members)
    throws Exception {

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
    for (ComplexMapRefSetMember member : members) {
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

      humanReadableWriter.write(entryLine);
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

    MapRecord mr = null;

    // if in cache, use cached records
    if (mapRecordMap.containsKey(terminologyId)) {

      mr = mapRecordMap.get(terminologyId);

    } else {

      // if not in cache yet, get record(s) for this concept
      MapRecordList mapRecordList =
          mappingService.getMapRecordsForProjectAndConcept(mapProject.getId(),
              terminologyId);

      // check number of records retrieved for erroneous
      // states
      if (mapRecordList.getCount() == 0) {

        // if on excluded list, add to errors to output
        if (mapProject.getScopeExcludedConcepts().contains(terminologyId)) {
          conceptErrors.put(terminologyId,
              "  Concept referenced, but on excluded list for project");
          // if not found, add to errors to output
        } else {
          conceptErrors.put(terminologyId, "No record found for concept.");
        }
      } else if (mapRecordList.getCount() > 1) {
        conceptErrors.put(terminologyId,
            "Multiple records (" + mapRecordList.getCount()
                + ") found for concept");
      } else {
        mr = mapRecordList.getMapRecords().iterator().next();

        // if ready for publication, add to map
        if (mr.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
            || mr.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))
          mapRecordMap.put(terminologyId, mr);
        else {
          conceptErrors.put(terminologyId,
              "Invalid workflow status " + mr.getWorkflowStatus()
                  + " on record");
        }
      }

    }

    return mr;
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
   */
  private ComplexMapRefSetMember convertMapEntryToComplexMapRefSetMemberMapEntry(
    MapEntry mapEntry, MapRecord mapRecord, MapProject mapProject,
    Concept concept) throws IOException, NoSuchAlgorithmException {

    ComplexMapRefSetMember complexMapRefSetMember =
        new ComplexMapRefSetMemberJpa();

    // set the base parameters
    // NOTE: do not set UUID here, done in main logic
    complexMapRefSetMember.setConcept(concept);
    complexMapRefSetMember.setRefSetId(mapProject.getRefSetId());
    complexMapRefSetMember.setModuleId(new Long(moduleId));
    complexMapRefSetMember.setActive(true);
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
   * Returns the raw bytes.
   * 
   * @param uid the uid
   * @return the raw bytes
   */
  private byte[] getRawBytes(UUID uid) {
    String id = uid.toString();
    byte[] rawBytes = new byte[16];

    for (int i = 0, j = 0; i < 36; ++j) {
      // Need to bypass hyphens:
      switch (i) {
        case 8:
        case 13:
        case 18:
        case 23:
          ++i;
          break;
        default:
          break;
      }
      char c = id.charAt(i);

      if (c >= '0' && c <= '9') {
        rawBytes[j] = (byte) ((c - '0') << 4);
      } else if (c >= 'a' && c <= 'f') {
        rawBytes[j] = (byte) ((c - 'a' + 10) << 4);
      }

      c = id.charAt(++i);

      if (c >= '0' && c <= '9') {
        rawBytes[j] |= (byte) (c - '0');
      } else if (c >= 'a' && c <= 'f') {
        rawBytes[j] |= (byte) (c - 'a' + 10);
      }
      ++i;
    }
    return rawBytes;
  }

  /**
   * Gets the release uuid.
   *
   * @param hash the hash
   * @return the release uuid
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  private UUID getReleaseUuid(String hash) throws NoSuchAlgorithmException,
    UnsupportedEncodingException {
    return getUuidForString(hash);
  }

  /**
   * Returns the uuid for string.
   *
   * @param name the name
   * @return the uuid for string
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  private UUID getUuidForString(String name) throws NoSuchAlgorithmException,
    UnsupportedEncodingException {

    MessageDigest sha1Algorithm = MessageDigest.getInstance("SHA-1");

    String namespace = "00000000-0000-0000-0000-000000000000";
    String encoding = "UTF-8";

    UUID namespaceUUID = UUID.fromString(namespace);

    // Generate the digest.
    sha1Algorithm.reset();

    // Generate the digest.
    sha1Algorithm.reset();
    if (namespace != null) {
      sha1Algorithm.update(getRawBytes(namespaceUUID));
    }

    sha1Algorithm.update(name.getBytes(encoding));
    byte[] sha1digest = sha1Algorithm.digest();

    sha1digest[6] &= 0x0f; /* clear version */
    sha1digest[6] |= 0x50; /* set to version 5 */
    sha1digest[8] &= 0x3f; /* clear variant */
    sha1digest[8] |= 0x80; /* set to IETF variant */

    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (sha1digest[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (sha1digest[i] & 0xff);
    }

    return new UUID(msb, lsb);

  }

  /**
   * Returns the machine readable textfor complex map ref set member.
   *
   * @param complexMapRefSetMember the complex map ref set member
   * @return the machine readable textfor complex map ref set member
   * @throws Exception the exception
   */
  private String getOutputLine(ComplexMapRefSetMember complexMapRefSetMember)
    throws Exception {

    String entryLine = "";

    // switch line on map relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t"
              + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() ? "1" : "0")
              + "\t"
              + moduleId
              + "\t"
              + complexMapRefSetMember.getRefSetId()
              + "\t"
              + complexMapRefSetMember.getConcept().getTerminologyId()
              + "\t"
              + complexMapRefSetMember.getMapGroup()
              + "\t"
              + complexMapRefSetMember.getMapPriority()
              + "\t"
              + (mapProject.isRuleBased() ? complexMapRefSetMember.getMapRule()
                  : "") + "\t" + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + complexMapRefSetMember.getMapTarget() + "\t"
              + "447561005"
              + "\t" + complexMapRefSetMember.getMapRelationId();

      // ComplexMap style is identical to ExtendedMap
      // with the exception of the terminating map relation terminology id
    } else if (mapProject.getMapRefsetPattern().equals(
        MapRefsetPattern.ComplexMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t" + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() ? "1" : "0")
              + "\t"
              + moduleId + "\t" + complexMapRefSetMember.getRefSetId()
              + "\t"
              + complexMapRefSetMember.getConcept().getTerminologyId()
              + "\t"
              + complexMapRefSetMember.getMapGroup()
              + "\t"
              + complexMapRefSetMember.getMapPriority()
              + "\t"
              + complexMapRefSetMember.getMapRule()
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + complexMapRefSetMember.getMapTarget()
              + "\t"
              + complexMapRefSetMember.getMapRelationId();
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
    for (MapRecord mapRecord : mapRecords) {
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      conceptCache.put(concept.getTerminologyId(), concept);
      defaultPreferredNames.put(
          concept.getTerminologyId(),
          computeDefaultPreferredName(concept, dpnTypeId, dpnRefSetId,
              dpnAcceptabilityId));
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
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#beginRelease(org
   * .ihtsdo.otf.mapping.model.MapProject, boolean)
   */
  @Override
  public void beginRelease(MapProject mapProject, boolean removeRecords)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "Performing operations required for begin release");
    Logger.getLogger(getClass()).info("  Map project: " + mapProject.getName());
    Logger.getLogger(getClass()).info(
        "  "
            + (removeRecords ? "Removing out-of-scope records"
                : "Not removing out-of-scope records"));

    // instantiate required services
    MappingService mappingService = new MappingServiceJpa();
    ReportService reportService = new ReportServiceJpa();

    // set transaction per operation to false for report service
    // only committed at end
    reportService.setTransactionPerOperation(false);

    // instantiate the algorithm handler
    ProjectSpecificAlgorithmHandler algorithmHandler =
        mappingService.getProjectSpecificAlgorithmHandler(mapProject);

    Logger.getLogger(getClass()).info("Creating report...");

    // get the report definition
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

    Logger.getLogger(getClass()).info(
        "Getting scope concepts for map project...");

    // get all scope concept terminology ids for this project
    Set<String> scopeConceptTerminologyIds = new HashSet<>();
    for (SearchResult sr : mappingService.findConceptsInScope(
        mapProject.getId(), null).getSearchResults()) {
      scopeConceptTerminologyIds.add(sr.getTerminologyId());
    }

    Logger.getLogger(getClass()).info(
        "  " + scopeConceptTerminologyIds.size() + " concepts in scope.");

    Logger.getLogger(getClass()).info("Getting records for map project...");

    // get all map records for this project
    MapRecordList mapRecords =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(getClass()).info(
        "  " + mapRecords.getCount() + " map records retrieved.");

    // create a temp set of scope terminology ids
    Set<String> conceptsWithNoRecord =
        new HashSet<>(scopeConceptTerminologyIds);

    Logger.getLogger(getClass()).info("Cycling over records...");

    List<MapRecord> mapRecordsToProcess = mapRecords.getMapRecords();

    // for each map record, check for errors
    // NOTE: Report Result names are constructed from error lists assigned
    // Each individual result is stored as a Report Result Item
    while (mapRecordsToProcess.size() != 0) {

      // extract the concept and remove it from list
      MapRecord mapRecord = mapRecordsToProcess.get(0);
      mapRecordsToProcess.remove(0);

      // first, remove this concept id from the dynamic conceptsWithNoRecord set
      conceptsWithNoRecord.remove(mapRecord.getConceptId());

      // constuct a list of errors for this concept
      List<String> resultMessages = new ArrayList<>();

      // CHECK: Map record is READY_FOR_PUBLICATION or PUBLISHED
      if (!mapRecord.getWorkflowStatus().equals(
          WorkflowStatus.READY_FOR_PUBLICATION)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
        resultMessages.add("Not marked ready for publication");

        // if record is ready for publication
      } else {
        // CHECK: Map record (must be ready for publication) passes project
        // specific validation checks
        ValidationResult result = algorithmHandler.validateRecord(mapRecord);
        if (!result.isValid()) {
          resultMessages.add("Failed validation check");
        } else {
          resultMessages.add("Ready for publication");
        }
      }

      // CHECK: Concept is in scope
      // ACTION: Remove records for out of scope concepts if flag set
      if (!scopeConceptTerminologyIds.contains(mapRecord.getConceptId())) {

        // construct message based on whether record is to be removed
        String reportMsg =
            removeRecords ? "Removed record for concept not in scope"
                : "Concept not in scope";

        // separate error-type by previously-published or this-cycle-edited
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))
          resultMessages.add(reportMsg + " - previously published");
        else
          resultMessages.add(reportMsg + " - edited this cycle");

        // remove record if flag set
        if (removeRecords)
          mappingService.removeMapRecord(mapRecord.getId());

      }

      // Add all reported errors to the report
      for (String error : resultMessages) {
        addReportError(report, mapProject, mapRecord.getConceptId(),
            mapRecord.getConceptName(), error);
      }
    }
    // CHECK: In-scope concepts with no map record

    // cycle over remaining ids
    for (String terminologyId : conceptsWithNoRecord) {

      // get the concept
      Concept c =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      addReportError(report, mapProject, terminologyId,
          c.getDefaultPreferredName(), "In-scope concept has no map record");
    }

    Logger.getLogger(getClass()).info("Adding Release QA Report...");

    // commit the report
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

  /**
   * Update statistic count.
   *
   * @param stat the stat
   */
  private void updateStat(String stat) {
    if (!reportStatistics.containsKey(stat)) {
      reportStatistics.put(stat, new Integer(0));
    }
    reportStatistics.put(stat, reportStatistics.get(stat) + 1);
  }

  /**
   * Update statistic max.
   *
   * @param statistic the statistic
   * @param value the value
   */
  private void updateStatisticMax(String statistic, int value) {

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
  public void setMapProject(MapProject mapProject) {
    this.mapProject = mapProject;
  }

  /**
   * Sets the map project.
   *
   * @param mapRecords the map project
   */
  @Override
  public void setMapRecords(List<MapRecord> mapRecords) {
    this.mapRecords = mapRecords;
  }

  /** The comparator. */
  private static Comparator<String> COMPARATOR = new Comparator<String>() {

    @Override
    public int compare(String o1, String o2) {
      String[] fields1 = o1.split("\t");
      String[] fields2 = o2.split("\t");

      long i = fields1[4].compareTo(fields2[4]);
      if (i != 0) {
        return (int) i;
      } else {
        i = (Long.parseLong(fields1[5]) - Long.parseLong(fields2[5]));
        if (i != 0) {
          return (int) i;
        } else {
          i = Long.parseLong(fields1[6]) - Long.parseLong(fields2[6]);
          if (i != 0) {
            return (int) i;
          } else {
            i = Long.parseLong(fields1[7]) - Long.parseLong(fields2[7]);
            if (i != 0) {
              return (int) i;
            } else {
              i =
                  (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                      .compareTo(fields1[0] + fields1[1] + fields1[2]
                          + fields1[3]);
              if (i != 0) {
                return (int) i;
              } else {
                i = fields1[8].compareTo(fields2[8]);
                if (i != 0) {
                  return (int) i;
                } else {
                  i = fields1[9].compareTo(fields2[9]);
                  if (i != 0) {
                    return (int) i;
                  } else {
                    i = fields1[10].compareTo(fields2[10]);
                    if (i != 0) {
                      return (int) i;
                    } else {
                      i = fields1[11].compareTo(fields2[11]);
                      if (i != 0) {
                        return (int) i;
                      } else {
                        i = fields1[12].compareTo(fields2[12]);
                        if (i != 0) {
                          return (int) i;
                        } else {
                          return -1;
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
  };
}
