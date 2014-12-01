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
import java.util.NoSuchElementException;
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
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.reports.ReportResultItemJpa;
import org.ihtsdo.otf.mapping.reports.ReportResultJpa;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

// TODO: Auto-generated Javadoc
/**
 * JPA enabled implementation of {@link ReleaseHandler}.
 */
public class ReleaseHandlerJpa implements ReleaseHandler {

  // class-global services
  /** The mapping service. */
  private MappingService mappingService;

  /** The content service. */
  private ContentService contentService;

  /** The effectiveTime */
  private String effectiveTime;

  /** The module id */
  private String moduleId;

  /** THe flags for writing snapshot and delta */
  private boolean writeSnapshot = false;

  private boolean writeDelta = false;

  /** The map project */
  private MapProject mapProject = null;

  /**
   * Instantiates an empty {@link ReleaseHandlerJpa}.
   */
  public ReleaseHandlerJpa() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processRelease
   * (org.ihtsdo.otf.mapping.model.MapProject, java.lang.String,
   * java.lang.String, java.util.Set, java.lang.String, java.lang.String)
   */

  @Override
  public void processRelease(MapProject mapProject, String outputDirName,
    String effectiveTime, String moduleId) throws Exception {

    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = true;
    this.writeDelta = true;

    // get all map records for this project
    MapRecordList mapRecordList =
        mappingService
            .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                mapProject.getId(), null);

    // process the release
    processReleaseHelper(mapRecordList.getMapRecords(), outputDirName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processReleaseSnapshot
   * (org.ihtsdo.otf.mapping.model.MapProject, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void processReleaseSnapshot(MapProject mapProject,
    String outputDirName, String effectiveTime, String moduleId)
    throws Exception {

    // set the global variables
    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = true;
    this.writeDelta = false;

    // get all map records for this project
    MapRecordList mapRecordList =
        mappingService
            .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                mapProject.getId(), null);

    // process the release
    processReleaseHelper(mapRecordList.getMapRecords(), outputDirName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processReleaseDelta
   * (org.ihtsdo.otf.mapping.model.MapProject, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void processReleaseDelta(MapProject mapProject, String outputDirName,
    String effectiveTime, String moduleId) throws Exception {

    // set the global variables
    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = false;
    this.writeDelta = true;

    // get all map records for this project
    MapRecordList mapRecordList =
        mappingService
            .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                mapProject.getId(), null);

    // process the release
    processReleaseHelper(mapRecordList.getMapRecords(), outputDirName);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processRelease
   * (org.ihtsdo.otf.mapping.model.MapProject, java.util.Set, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void processRelease(MapProject mapProject,
    List<MapRecord> mapRecordsToPublish, String outputDirName,
    String effectiveTime, String moduleId) throws Exception {

    // set the global variables
    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = true;
    this.writeDelta = true;

    // process the release
    processReleaseHelper(mapRecordsToPublish, outputDirName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processReleaseSnapshot
   * (org.ihtsdo.otf.mapping.model.MapProject, java.util.Set, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void processReleaseSnapshot(MapProject mapProject,
    List<MapRecord> mapRecordsToPublish, String outputDirName,
    String effectiveTime, String moduleId) throws Exception {

    // set the global variables
    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = true;
    this.writeDelta = false;

    // process the release
    processReleaseHelper(mapRecordsToPublish, outputDirName);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler#processReleaseDelta
   * (org.ihtsdo.otf.mapping.model.MapProject, java.util.Set, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void processReleaseDelta(MapProject mapProject,
    List<MapRecord> mapRecordsToPublish, String outputDirName,
    String effectiveTime, String moduleId) throws Exception {

    // set the global variables
    this.mapProject = mapProject;
    this.effectiveTime = effectiveTime;
    this.moduleId = moduleId;
    this.writeSnapshot = false;
    this.writeDelta = true;

    // process the release
    processReleaseHelper(mapRecordsToPublish, outputDirName);

  }

  /**
   * Internal function to actually process release.
   * 
   * Called by each of the specific functions above.
   * 
   * @param mapProject the map project
   * @param mapRecordsToPublish the map records to publish
   * @param outputDirName the output dir name
   * @param effectiveTime the effective time
   * @param moduleId the module id
   * @param writeSnapshot the write snapshot
   * @param writeDelta the write delta
   * @throws Exception the exception
   */
  private void processReleaseHelper(List<MapRecord> mapRecordsToPublish,
    String outputDirName) throws Exception {

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Processing publication release for project " + mapProject.getName());

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        " " + mapRecordsToPublish.size() + " records selected for publication");

    // check that either/both snapshot and delta files have been specified
    if (!writeSnapshot && !writeDelta) {
      throw new Exception(
          "processRelease called with both snapshot and delta flags disabled");
    }

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

    if (moduleId == null || moduleId.isEmpty()) {
      throw new Exception("Module id must be specified");
    }

    // check output directory exists
    File outputDir = new File(outputDirName);
    if (!outputDir.isDirectory())
      throw new Exception("Output file directory (" + outputDirName
          + ") could not be found.");

    // ensure output file directory name ends with a '/' for file creation
    if (!outputDirName.endsWith("/"))
      outputDirName += "/";

    // declare the file names and file writers
    String snapshotMachineReadableFileName = null;
    String snapshotHumanReadableFileName = null;
    String deltaMachineReadableFileName = null;

    BufferedWriter snapshotMachineReadableWriter = null;
    BufferedWriter snapshotHumanReadableWriter = null;
    BufferedWriter deltaMachineReadableWriter = null;

    // /////////////////////////////////////////////////////
    // Create file names, instantiate writers, write headers
    // /////////////////////////////////////////////////////
    snapshotMachineReadableFileName =
        outputDirName + "der2_" + mapProject.getDestinationTerminology()
            + mapProject.getMapRefsetPattern() + "Snapshot_INT_"
            + effectiveTime + ".txt";

    snapshotHumanReadableFileName =
        outputDirName + "tls_" + mapProject.getDestinationTerminology()
            + "HumanReadableMap_INT_"
            + mapProject.getSourceTerminologyVersion() + "_" + effectiveTime
            + ".tsv";

    deltaMachineReadableFileName =
        outputDirName + "der2_" + mapProject.getDestinationTerminology()
            + mapProject.getMapRefsetPattern() + "Delta_INT_" + effectiveTime
            + ".txt";

    if (writeSnapshot == true) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Machine-readable release file:  "
              + snapshotMachineReadableFileName);
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Human-readable release file:    " + snapshotHumanReadableFileName);

      // instantiate file writers
      snapshotHumanReadableWriter =
          new BufferedWriter(new FileWriter(snapshotHumanReadableFileName));
      snapshotMachineReadableWriter =
          new BufferedWriter(new FileWriter(snapshotMachineReadableFileName));
    }

    if (writeDelta == true) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Delta release file:             " + deltaMachineReadableFileName);

      deltaMachineReadableWriter =
          new BufferedWriter(new FileWriter(deltaMachineReadableFileName));

    }

    // Write header based on relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      if (snapshotMachineReadableWriter != null) {
        snapshotMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\r\n");
        snapshotMachineReadableWriter.flush();
      }
      if (snapshotHumanReadableWriter != null) {
        snapshotHumanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\treferencedComponentName\tmapTargetName\tmapCategoryName\r\n");
        snapshotHumanReadableWriter.flush();
      }
      if (deltaMachineReadableWriter != null) {
        deltaMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\r\n");
        deltaMachineReadableWriter.flush();
      }

    } else if (mapProject.getMapRefsetPattern().equals(
        MapRefsetPattern.ComplexMap)) {
      if (snapshotMachineReadableWriter != null) {
        snapshotMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\r\n");
        snapshotMachineReadableWriter.flush();
      }
      if (snapshotHumanReadableWriter != null) {
        snapshotHumanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\treferencedComponentName\tmapTargetName\tmapCategoryName\r\n");
        snapshotHumanReadableWriter.flush();
      }
      if (deltaMachineReadableWriter != null) {
        deltaMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\r\n");
        deltaMachineReadableWriter.flush();
      }
    }

    // /////////////////////////////////////////////////////
    // Service, handler, and retrieval map instantiation
    // /////////////////////////////////////////////////////

    // instantiate the required services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();

    // instantiate the project specific handler
    ProjectSpecificAlgorithmHandler algorithmHandler =
        mappingService.getProjectSpecificAlgorithmHandler(mapProject);

    // instantiate the set of complex map ref set members to write
    Set<ComplexMapRefSetMember> complexMapRefSetMembersToWrite =
        new HashSet<>();

    // Create a map by concept id for quick retrieval of descendants
    Map<String, MapRecord> mapRecordMap = new HashMap<>();

    for (MapRecord mr : mapRecordsToPublish) {
      mapRecordMap.put(mr.getConceptId(), mr);
    }

    // map of terminology id -> error message
    Map<String, String> conceptErrors = new HashMap<>();

    int nEntries = 0;
    int nRecords = 0;
    int nRecordsPropagated = 0;

    // create a list from the set
    Logger.getLogger(ReleaseHandlerJpa.class).info("  Sorting records");

    Collections.sort(mapRecordsToPublish, new Comparator<MapRecord>() {

      @Override
      public int compare(MapRecord o1, MapRecord o2) {
        Long conceptId1 = Long.parseLong(o1.getConceptId());
        Long conceptId2 = Long.parseLong(o2.getConceptId());

        return conceptId1.compareTo(conceptId2);

      }
    });

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
      complexMapRefSetMemberMap.put(this.constructUuidKeyString(c), c);
    }

    // output size of each collection
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Cached distinct UUID-quintuples: "
            + complexMapRefSetMemberMap.keySet().size());
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Existing complex ref set members for project: "
            + refSetMembers.getCount());

    // if sizes do not match, output warning
    if (complexMapRefSetMemberMap.keySet().size() != refSetMembers.getCount()) {
      Logger.getLogger(ReleaseHandlerJpa.class).warn("  TOTALS DO NOT MATCH");
    }

    // clear the ref set members list (no longer used)
    refSetMembers = null;

    // count -- used for logging
    int nRefSetMembers = complexMapRefSetMemberMap.keySet().size();
    int nRefSetMatches = 0;

    // /////////////////////////////////////////////////////
    // Perform the release
    // /////////////////////////////////////////////////////

    Logger.getLogger(ReleaseHandlerJpa.class).info("  Processing release...");

    // cycle over the map records marked for publishing
    for (MapRecord mapRecord : mapRecordsToPublish) {

      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "   Processing map record " + mapRecord.getId() + ", "
              + mapRecord.getConceptId() + ", " + mapRecord.getConceptName());

      // instantiate map of entries by group
      // this is the object containing entries to write
      Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

      // /////////////////////////////////////////////////////
      // Check for up-propagation
      // /////////////////////////////////////////////////////
      if (mapProject.isPropagatedFlag() == true
          && mapRecord.getCountDescendantConcepts() < mapProject
              .getPropagationDescendantThreshold()) {

        Logger.getLogger(ReleaseHandlerJpa.class).info(
            "    Record is up-propagated.");

        // /////////////////////////////////////////////////////
        // Get the descendants via tree positions
        // /////////////////////////////////////////////////////

        TreePosition treePosition;
        try {
          // get the first tree position (may be several for this
          // concept)
          treePosition =
              contentService
                  .getTreePositions(mapRecord.getConceptId(),
                      mapProject.getSourceTerminology(),
                      mapProject.getSourceTerminologyVersion()).getIterable()
                  .iterator().next();

          // use the first tree position to retrieve a tree position
          // graph with populated descendants
          treePosition =
              contentService.getTreePositionWithDescendants(treePosition);
        } catch (NoSuchElementException e) {
          conceptErrors.put(mapRecord.getConceptId(),
              "Could not retrieve tree positions");
          continue;
        }

        // get a list of tree positions sorted by position in hiearchy
        // (deepest-first)
        // NOTE: This list will contain the top-level/root map record
        List<TreePosition> treePositionDescendantList =
            getSortedTreePositionDescendantList(treePosition);

        System.out.println("*** Descendant list has "
            + treePositionDescendantList.size() + " elements");

        // /////////////////////////////////////////////////////
        // Construct retrieval maps and check for errors
        // /////////////////////////////////////////////////////

        // construct a map of ancestor path + terminologyId to map
        // records, used to easily retrieve parent records for
        // descendants of
        // up-propagated records
        // key: A~B~C~D, value: map record for concept D
        Map<String, MapRecord> treePositionToMapRecordMap = new HashMap<>();

        // cycle over all descendants of this position
        // and add all required records to the map
        // for use later
        for (TreePosition tp : treePositionDescendantList) {

          System.out.println("Retrieving record for concept "
              + tp.getTerminologyId());

          // retrieve map record from cache, or retrieve from database
          // and add to cache
          MapRecord mr = null;

          // if in cache, use cached records
          if (mapRecordMap.containsKey(tp.getTerminologyId())) {

            mr = mapRecordMap.get(tp.getTerminologyId());

          } else {

            // if not in cache yet, get record(s) for this concept
            MapRecordList mapRecordList =
                mappingService.getMapRecordsForProjectAndConcept(
                    mapProject.getId(), tp.getTerminologyId());

            // check number of records retrieved for erroneous
            // states
            if (mapRecordList.getCount() == 0) {

              // if on excluded list, add to errors to output
              if (mapProject.getScopeExcludedConcepts().contains(
                  tp.getTerminologyId())) {
                conceptErrors.put(tp.getTerminologyId(),
                    "  Concept referenced, but on excluded list for project");
                // if not found, add to errors to output
              } else {
                conceptErrors.put(tp.getTerminologyId(),
                    "No record found for concept.");
              }
            } else if (mapRecordList.getCount() > 1) {
              conceptErrors.put(tp.getTerminologyId(), "Multiple records ("
                  + mapRecordList.getCount() + ") found for concept");
            } else {
              mr = mapRecordList.getMapRecords().iterator().next();

              // if ready for publication, add to map
              if (mr.getWorkflowStatus().equals(
                  WorkflowStatus.READY_FOR_PUBLICATION)
                  || mr.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))
                mapRecordMap.put(tp.getTerminologyId(), mr);
              else {
                conceptErrors.put(tp.getTerminologyId(),
                    "Invalid workflow status " + mr.getWorkflowStatus()
                        + " on record");
              }
            }

          }

          // if record found, add record to TreePosition->MapRecord
          // map
          if (mr != null) {
            treePositionToMapRecordMap.put(
                tp.getAncestorPath() + "~" + tp.getTerminologyId(), mr);
          }
        }

        // /////////////////////////////////////////////////////
        // Process up-propagated entries
        // /////////////////////////////////////////////////////

        // cycle over the tree positions again and add entries
        // note that the tree positions are in reverse order of
        // hierarchy depth
        for (TreePosition tp : treePositionDescendantList) {

          // get the parent map record for this tree position
          // used to check if entries are duplicated on parent
          MapRecord mrParent =
              treePositionToMapRecordMap.get(tp.getAncestorPath());

          // skip the root level record, these entries are added
          // below, after the up-propagated entries
          if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

            // get the map record corresponding to this specific
            // ancestor path + concept Id
            MapRecord mr =
                treePositionToMapRecordMap.get(tp.getAncestorPath() + "~"
                    + tp.getTerminologyId());

            if (mr != null) {

              Logger.getLogger(ReleaseHandlerJpa.class).info(
                  "     Adding entries from map record " + mr.getId() + ", "
                      + mr.getConceptId() + ", " + mr.getConceptName());

              // if no parent, continue, but log error
              if (mrParent == null) {
                Logger.getLogger(ReleaseHandlerJpa.class).warn(
                    "Could not retrieve parent map record!");
                mrParent = new MapRecordJpa(); // only here
                // during
                // testing
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

                  // set map priority based on size of current
                  // list
                  newEntry.setMapPriority(existingEntries.size() + 1);

                  // set the propagated rule for this entry
                  if (mapProject.isRuleBased()) {
                    newEntry = setPropagatedRuleForMapEntry(newEntry);

                    System.out
                        .println("Propagated rule: " + newEntry.getRule());
                  }

                  // recalculate the map relation
                  newEntry.setMapRelation(algorithmHandler.computeMapRelation(
                      mapRecord, newEntry));

                  // add to the list
                  existingEntries.add(newEntry);

                  // replace existing list with modified list
                  entriesByGroup.put(newEntry.getMapGroup(), existingEntries);

                }
              }
            } else {
              conceptErrors
                  .put(tp.getTerminologyId(),
                      "No record exists for descendant concept, cannot up-propagate");
            }
          }
        }

        // increment the propagated counter
        nRecordsPropagated++;
      }

      // /////////////////////////////////////////////////////
      // Add the original (non-propagated) entries
      // /////////////////////////////////////////////////////

      for (MapEntry me : mapRecord.getMapEntries()) {

        List<MapEntry> existingEntries = entriesByGroup.get(me.getMapGroup());

        if (existingEntries == null)
          existingEntries = new ArrayList<>();

        // create a new managed instance for this entry
        MapEntry newEntry = new MapEntryJpa();
        newEntry.setMapAdvices(me.getMapAdvices());
        newEntry.setMapGroup(me.getMapGroup());
        newEntry.setMapBlock(me.getMapBlock());
        newEntry.setMapRecord(mapRecord); // used for rule propagation
        // (i.e. concept Id and
        // concept Name)
        newEntry.setRule(me.getRule()); // note that this is effectively
        // no-op for non-rule-based
        // projects
        newEntry.setTargetId(me.getTargetId());
        newEntry.setTargetName(me.getTargetName());

        // add map entry to map
        newEntry.setMapPriority(existingEntries.size() + 1);

        // if not the first entry and contains TRUE rule, set to
        // OTHERWISE TRUE
        if (newEntry.getMapPriority() > 1 && newEntry.getRule().equals("TRUE"))
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
            newEntry.setMapPriority(existingEntries.size() + 1);

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

      System.out.println("Groups: " + entriesByGroup.keySet().size());
      System.out.println("Entries: " + entriesByGroup.values().size());

      // /////////////////////////////////////////////////////
      // Convert the record to complex map ref set members
      // /////////////////////////////////////////////////////

      // get the source concept for this record
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // cycle over groups and entries in sequence
      for (int mapGroup : entriesByGroup.keySet()) {
        for (MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

          // convert this map entry into a complex map ref set member
          ComplexMapRefSetMember c_release =
              this.convertMapEntryToComplexMapRefSetMemberMapEntry(mapEntry,
                  mapRecord, mapProject, concept);

          System.out.println(c_release.toString());

          String uuid_str = this.constructUuidKeyString(c_release);

          // attempt to retrieve any existing complex map ref set
          // member
          ComplexMapRefSetMember c_existing =
              complexMapRefSetMemberMap.get(uuid_str);

          // if existing found, re-use uuid, otherwise generate new
          if (c_existing == null) {
            System.out.println("No existing refset member found");

            c_release.setTerminologyId(this.getReleaseUuid(c_release)
                .toString());
          } else {
            System.out.println("Existing refset member found");
            nRefSetMatches++;
            // copy the UUID
            c_release.setTerminologyId(c_existing.getTerminologyId());
          }

          // add this entry to the list of members to write
          complexMapRefSetMembersToWrite.add(c_release);

          nEntries++;

        }
      }

      // increment the total record count
      nRecords++;

    }

    // /////////////////////////////////////////////////////
    // Prepare for file write
    // /////////////////////////////////////////////////////

    // First, construct set of previously active complex map ref set members
    Set<ComplexMapRefSetMember> complexMapRefSetMembersPreviouslyActive =
        new HashSet<>();
    for (ComplexMapRefSetMember c : complexMapRefSetMemberMap.values()) {
      if (c.isActive())
        complexMapRefSetMembersPreviouslyActive.add(c);

    }

    // temporary set for write oeprations
    Set<ComplexMapRefSetMember> tempSet;

    // set of map relations for human readable file
    List<MapRelation> mapRelations =
        mappingService.getMapRelations().getMapRelations();

    Comparator<String> comparator = new Comparator<String>() {

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
                            return 0;
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

    // /////////////////////////////////////////////////////
    // Write the snapshot files if indicated
    // /////////////////////////////////////////////////////
    if (writeSnapshot == true) {

      // Case 1: Current & active -- the records in database
      for (ComplexMapRefSetMember c : complexMapRefSetMembersToWrite) {

        // human readable file requires target concept name
        Concept concept = null;
        if (!c.getMapTarget().isEmpty()) {
          concept =
              contentService.getConcept(c.getMapTarget(),
                  mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
        }

        // human readable file requires map relation name
        MapRelation mapRelation = null;
        for (MapRelation mr : mapRelations) {
          if (mr.getTerminologyId().equals(c.getMapRelationId())) {
            mapRelation = mr;
          }
        }

        // write to machine and human-readable files
        snapshotMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));
        snapshotHumanReadableWriter.write(this
            .getHumanReadableTextforComplexMapRefSetMember(c, concept,
                mapRelation));

      }

      // Case 2: Previously existing, but not in current records
      tempSet = new HashSet<>(complexMapRefSetMemberMap.values());
      tempSet.removeAll(complexMapRefSetMembersToWrite);

      // write the members
      for (ComplexMapRefSetMember c : tempSet) {
        deltaMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));

        // human readable file requires target concept name
        Concept concept = null;
        if (!c.getMapTarget().isEmpty()) {
          concept =
              contentService.getConcept(c.getMapTarget(),
                  mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
        }

        // this is clunky, result of rewrite from storing map enries
        MapRelation mapRelation = null;
        for (MapRelation mr : mapRelations) {
          if (mr.getTerminologyId().equals(c.getMapRelationId())) {
            mapRelation = mr;
          }
        }

        // write to machine and human-readable files
        snapshotMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));
        snapshotHumanReadableWriter.write(this
            .getHumanReadableTextforComplexMapRefSetMember(c, concept,
                mapRelation));
      }

      // sort the files
      FileSorter.sortFile(snapshotMachineReadableFileName, comparator);
      FileSorter.sortFile(snapshotHumanReadableFileName, comparator);
    }

    // /////////////////////////////////////////////////////
    // Write the delta files
    // /////////////////////////////////////////////////////

    if (writeDelta == true) {
      // case 1: currently active now modified
      // copy the current, published write set
      // remove any that are unchanged
      tempSet = complexMapRefSetMembersToWrite;
      tempSet.removeAll(complexMapRefSetMembersPreviouslyActive);

      // set remainder to active (redundant operation) and write to delta
      for (ComplexMapRefSetMember c : tempSet) {
        c.setActive(true);
        deltaMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));
      }

      // case 2: previously active no longer present
      // copy the previously active set
      // remove all items that are unchanged in the current write set
      tempSet = complexMapRefSetMembersPreviouslyActive;
      tempSet.removeAll(complexMapRefSetMembersToWrite);

      // remove any still current
      for (ComplexMapRefSetMember c : tempSet) {
        c.setActive(false);
        deltaMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));
      }

      // sort the file
      FileSorter.sortFile(deltaMachineReadableFileName, comparator);
    }

    // /////////////////////////////////////////////////////
    // Output errors and run statistics
    // /////////////////////////////////////////////////////

    // write the errors
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Concept errors (" + conceptErrors.keySet().size() + ")");
    for (String terminologyId : conceptErrors.keySet()) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  " + terminologyId + ": " + conceptErrors.get(terminologyId));
    }

    // write the statistics
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total records released      : " + nRecords);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total records up-propagated : " + nRecordsPropagated);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total entries released      : " + nEntries);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total records with errors   : " + conceptErrors.keySet().size());

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total existing refsets      : " + nRefSetMembers);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total entries matched       : " + nRefSetMatches);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Total entries inactivated   : " + (nRefSetMembers - nRefSetMatches));
    // /////////////////////////////////////////////////////
    // Clean up
    // /////////////////////////////////////////////////////

    // close the services
    contentService.close();
    mappingService.close();

    // close the writers
    snapshotMachineReadableWriter.close();
    snapshotHumanReadableWriter.close();
    deltaMachineReadableWriter.close();
  }

  /**
   * Helper function to get a map key identifier for a complex map Used to
   * determine whether an entry exists in set
   * 
   * @param c
   * @return
   */
  private String constructUuidKeyString(ComplexMapRefSetMember c) {
    return c.getRefSetId() + "_" + c.getConcept().getTerminologyId() + "_"
        + c.getMapGroup() + "_" + c.getMapRule() + "_" + c.getMapTarget();
  }

  /**
   * Convert a map entry to a complex map ref set member. Does not set effective
   * time.
   * 
   * @param mapEntry the map entry
   * @param mapRecord the map record
   * @param mapProject the map project
   * @param concept the concept
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws NoSuchAlgorithmException the no such algorithm exception
   */
  private ComplexMapRefSetMember convertMapEntryToComplexMapRefSetMemberMapEntry(
    MapEntry mapEntry, MapRecord mapRecord, MapProject mapProject,
    Concept concept) throws IOException, NoSuchAlgorithmException {

    ComplexMapRefSetMember complexMapRefSetMember =
        new ComplexMapRefSetMemberJpa();

    // set the concept, module id, and active
    complexMapRefSetMember.setConcept(concept);
    complexMapRefSetMember.setRefSetId(mapProject.getRefSetId());
    complexMapRefSetMember.setModuleId(new Long(moduleId));
    complexMapRefSetMember.setActive(true);

    // set parameters from the map entry
    complexMapRefSetMember.setMapGroup(mapEntry.getMapGroup());
    complexMapRefSetMember.setMapPriority(mapEntry.getMapPriority());
    complexMapRefSetMember.setMapRule(mapEntry.getRule());
    complexMapRefSetMember.setMapRelationId(new Long(mapEntry.getMapRelation()
        .getTerminologyId()));
    complexMapRefSetMember.setMapTarget(mapEntry.getTargetId());

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

    String mapAdviceStr = getHumanReadableMapAdvice(mapEntry);
    for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
      mapAdviceStr += " | " + mapAdvice.getDetail();
    }

    if (mapEntry.getRule().startsWith("IFA")
        && mapEntry.getRule().toUpperCase().contains("MALE")) {
      // do nothing -- do not add anything for gender rules
    }

    else if (mapEntry.getRule().startsWith("IFA"))
      mapAdviceStr += " | " + "MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT";

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
        "IFA " + mapRecord.getConceptId() + " | " + mapRecord.getConceptName()
            + " |";

    // if an age or gender rule, append the existing rule
    if (!mapEntry.getRule().contains("TRUE")) {
      rule += " AND " + mapEntry.getRule();
    }

    // set the rule
    mapEntry.setRule(rule);

    Logger.getLogger(ReleaseHandlerJpa.class)
        .info("       Set rule to " + rule);

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

    String[] comparatorComponents; // used for parsing age rules

    // TODO: Check whether using rule based is a good proxy here
    if (mapProject.isRuleBased()) {

      System.out.println("Constructing human-readable advice for:  "
          + mapEntry.getRule());

      // if map target is blank

      if (mapEntry.getTargetId() == null || mapEntry.getTargetId() == "") {
        System.out.println("  Use map relation");
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
        String targetName = ifaComponents[1].trim(); // .replace("[(.*)]",
        // "");

        advice = "IF " + targetName + " CHOOSE " + mapEntry.getTargetId();
      }

      System.out.println("   Human-readable advice: " + advice);
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
   * @param name the name
   * @return the release uuid
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  private UUID getReleaseUuid(ComplexMapRefSetMember c)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {

    String name =
        c.getRefSetId() + c.getConcept().getTerminologyId() + c.getMapGroup()
            + c.getMapRule() + c.getMapTarget();

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

  private String getHumanReadableTextforComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember, Concept targetConcept,
    MapRelation mapRelation) throws Exception {

    String entryLine = "";

    // switch line on map relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t"
              + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() == true ? "1" : "0")
              + "\t"
              + moduleId
              + "\t"
              + complexMapRefSetMember.getRefSetId()
              + "\t"
              + complexMapRefSetMember.getConcept().getTerminologyId()
              + "\t"
              + complexMapRefSetMember.getConcept().getDefaultPreferredName()
              + "\t"
              + complexMapRefSetMember.getMapGroup()
              + "\t"
              + complexMapRefSetMember.getMapPriority()
              + "\t"
              + complexMapRefSetMember.getMapRule()
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget())
              + "\t"
              + (targetConcept != null ? targetConcept
                  .getDefaultPreferredName() : "") + "\t" + "447561005"
              + "\t"
              + complexMapRefSetMember.getMapRelationId()
              + "\t"
              + (mapRelation != null ? mapRelation.getName() : "");

      // ComplexMap style is identical to ExtendedMap
      // with the exception of the terminating map relation terminology id
    } else if (mapProject.getMapRefsetPattern().equals(
        MapRefsetPattern.ComplexMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t"
              + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() == true ? "1" : "0")
              + "\t"
              + moduleId
              + "\t"
              + complexMapRefSetMember.getRefSetId()
              + "\t"
              + complexMapRefSetMember.getConcept().getTerminologyId()
              + "\t"
              + complexMapRefSetMember.getConcept().getDefaultPreferredName()
              + "\t"
              + complexMapRefSetMember.getMapGroup()
              + "\t"
              + complexMapRefSetMember.getMapPriority()
              + "\t"
              + complexMapRefSetMember.getMapRule()
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget())
              + "\t"
              + (targetConcept != null ? targetConcept
                  .getDefaultPreferredName() : "") + "\t" + "447561005";
    }

    entryLine += "\r\n";

    return entryLine;

  }

  private String getMachineReadableTextforComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) throws Exception {

    String entryLine = "";

    // switch line on map relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t"
              + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() == true ? "1" : "0")
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
              + complexMapRefSetMember.getMapRule()
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget()) + "\t"
              + "447561005"
              + "\t" + complexMapRefSetMember.getMapRelationId();

      // ComplexMap style is identical to ExtendedMap
      // with the exception of the terminating map relation terminology id
    } else if (mapProject.getMapRefsetPattern().equals(
        MapRefsetPattern.ComplexMap)) {
      entryLine =
          complexMapRefSetMember.getTerminologyId() // the UUID
              + "\t"
              + effectiveTime
              + "\t"
              + (complexMapRefSetMember.isActive() == true ? "1" : "0")
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
              + complexMapRefSetMember.getMapRule()
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget()) + "\t" + "447561005";
    }

    entryLine += "\r\n";

    return entryLine;

  }
  
  @Override
  public void performBeginReleaseQAChecks(MapProject mapProject) throws Exception {
    
    // instantiate required services
    MappingService mappingService = new MappingServiceJpa();
    ReportService reportService = new ReportServiceJpa();
    
    // set transaction per operation to false for report service
    // only committed at end
    reportService.setTransactionPerOperation(false);
    
    // instantiate the algorithm handler
    ProjectSpecificAlgorithmHandler algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mapProject);
  
    Logger.getLogger(ReleaseHandlerJpa.class).info("Creating report...");
    
    // get the report definition
    ReportDefinition reportDefinition = null;
    
    for (ReportDefinition rd : mapProject.getReportDefinitions()) {
      if (rd.getName().equals("Release QA"))
        reportDefinition = rd;
    }
    
    if (reportDefinition == null) {
      throw new Exception("Could not get report definition matching 'Release QA'");
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
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Getting scope concepts for map project...");
    
    // get all scope concept terminology ids for this project
    Set<String> scopeConceptTerminologyIds = new HashSet<>();
    for (SearchResult sr : mappingService.findConceptsInScope(mapProject.getId(), null).getSearchResults()) {
      scopeConceptTerminologyIds.add(sr.getTerminologyId());
    }
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("  " + scopeConceptTerminologyIds.size() + " concepts in scope.");
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Getting records for map project...");
    
    // get all map records for this project
    MapRecordList mapRecords = mappingService.getMapRecordsForMapProject(mapProject.getId());
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("  " + mapRecords.getCount() + " map records retrieved.");
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Cycling over concepts...");
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Cycling over records...");
    
    // for each map record, check for errors
    // NOTE:    Report Result names are constructed from error lists assigned
    //          Each individual result is stored as a Report Result Item
    for (MapRecord mapRecord : mapRecords.getMapRecords()) {
      
      // list of errors for this concept
      List<String> mapRecordErrors = new ArrayList<>();

      // CHECK:  Map record is READY_FOR_PUBLICATION or PUBLISHED
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
        mapRecordErrors.add("Not marked ready for publication");
      }
      
      // CHECK:  Concept is in scope
      if (!scopeConceptTerminologyIds.contains(mapRecord.getConceptId())) {
        mapRecordErrors.add("Concept is not in scope");
      }
      
      // CHECK:  Map record passes project specific validation checks
      if (!algorithmHandler.validateRecord(mapRecord).isValid()) {
        mapRecordErrors.add("Failed validation checks");
      }
      
      
      // CONVERT:  Error list into report results
      for (String error : mapRecordErrors) {
        addReportError(report, mapProject, mapRecord, error);  
      }
    }
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Adding Release QA Report...");
    
    // commit the report
    reportService.commit();
    
    Logger.getLogger(ReleaseHandlerJpa.class).info("Done.");
    
    mappingService.close();
    reportService.close();
    
      
    
  }
  
  private void addReportError(Report report, MapProject mapProject, MapRecord mapRecord, String error) {
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
    
    // create report result item and add it
    ReportResultItem item = new ReportResultItemJpa();
    item.setReportResult(reportResult);
    item.setItemId(mapRecord.getConceptId());
    item.setItemName(mapRecord.getConceptName());
    item.setResultType(ReportResultType.CONCEPT);
    reportResult.addReportResultItem(item);
  }
  

}
