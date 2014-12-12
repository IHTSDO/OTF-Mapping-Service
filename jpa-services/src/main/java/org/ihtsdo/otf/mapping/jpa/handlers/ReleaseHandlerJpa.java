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
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
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

  /** Map of terminology id to error messages */
  Map<String, String> conceptErrors = new HashMap<>();

  /** Map of terminology id to map record */
  Map<String, MapRecord> mapRecordMap = new HashMap<>();

  /** the defaultPreferredNames type id. */
  private String dpnTypeId = null;

  /** The dpn ref set id. */
  private String dpnRefSetId = null;

  /** The dpn acceptability id. */
  private String dpnAcceptabilityId = null;

  /** The default preferred names set (terminologyId -> dpn) */
  private Map<String, String> defaultPreferredNames = new HashMap<>();

  /**
   * Instantiates an empty {@link ReleaseHandlerJpa}.
   * @throws Exception
   */
  public ReleaseHandlerJpa() throws Exception {

    // instantiate services
    mappingService = new MappingServiceJpa();
    contentService = new ContentServiceJpa();
  }

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
        "  Map project refset pattern is "
            + mapProject.getMapRefsetPattern().toString());

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Map project is " + (mapProject.isRuleBased() ? "" : "not ")
            + " rule-based");

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

    // get the config properties for default preferred name variables
    Properties properties = ConfigUtility.getConfigProperties();

    // set the dpn variables and instantiate the concept dpn map
    dpnTypeId = properties.getProperty("loader.defaultPreferredNames.typeId");
    dpnRefSetId =
        properties.getProperty("loader.defaultPreferredNames.refSetId");
    dpnAcceptabilityId =
        properties.getProperty("loader.defaultPreferredNames.acceptabilityId");

    // map of all concepts and default preferred names
    Map<String, Concept> scopeConcepts = new HashMap<>();

    Logger
        .getLogger(ReleaseHandlerJpa.class)
        .info(
            "Retrieving concepts for records and recomputing default preferred names...");
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  dpnTypeId          : " + dpnTypeId);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  dpnRefSetId        : " + dpnRefSetId);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  dpnAccetabilityId  : " + dpnAcceptabilityId);

    for (MapRecord mapRecord : mapRecordsToPublish) {
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      scopeConcepts.put(concept.getTerminologyId(), concept);
      defaultPreferredNames.put(concept.getTerminologyId(),
          computeDefaultPreferredName(concept));
    }

    // declare the file names and file writers
    String snapshotMachineReadableFileName = null;
    String deltaMachineReadableFileName = null;
    String humanReadableFileName = null;
    String moduleDependencyFileName = null;

    BufferedWriter snapshotMachineReadableWriter = null;
    BufferedWriter deltaMachineReadableWriter = null;
    BufferedWriter humanReadableWriter = null;
    BufferedWriter moduleDependencyWriter = null;

    // instantiate the project specific handler
    ProjectSpecificAlgorithmHandler algorithmHandler =
        mappingService.getProjectSpecificAlgorithmHandler(mapProject);

    // /////////////////////////////////////////////////////
    // If indicated, write module dependency file
    // /////////////////////////////////////////////////////

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Checking for module dependencies...");

    Set<String> moduleDependencies = algorithmHandler.getDependentModules();

    if (moduleDependencies.size() > 0) {

      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Module dependencies found: " + moduleDependencies.size());
      moduleDependencyFileName =
          outputDirName + "/der2_ssRefset_ModuleDependencySnapshot_INT_"
              + effectiveTime + ".txt";

      moduleDependencyWriter =
          new BufferedWriter(new FileWriter(moduleDependencyFileName));

      moduleDependencyWriter
          .write("id   effectiveTime   active  moduleId    refsetId    referencedComponentId   sourceEffectiveTime targetEffectiveTime"
              + "\r\n");

      for (String module : moduleDependencies) {
        String moduleStr =
            this.getUuidForString(
                moduleId + algorithmHandler.getModuleDependencyRefSetId()
                    + module).toString()
                + "\t"
                + effectiveTime
                + "\t"
                + "1"
                + "\t"
                + moduleId
                + "\t"
                + "900000000000534007" // TODO Move this to project specific
                                       // handler
                + "\t"
                + module
                + "\t"
                + effectiveTime
                + "\t"
                + effectiveTime
                + "\r\n";

        System.out.println(moduleStr);

        moduleDependencyWriter.write(moduleStr);
      }

      moduleDependencyWriter.flush();
      moduleDependencyWriter.close();
    } else {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  No module dependencies found");
    }

    // /////////////////////////////////////////////////////
    // Create file names, instantiate writers, write headers
    // /////////////////////////////////////////////////////
    String camelCaseName =
        mapProject.getDestinationTerminology().substring(0, 1)
            + mapProject.getDestinationTerminology().substring(1).toLowerCase();
    String pattern =
        (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap
            ? "iissscRefset_" : "iisssccRefset_");
    snapshotMachineReadableFileName =
        outputDirName + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "Snapshot_INT_" + effectiveTime + ".txt";

    humanReadableFileName =
        outputDirName + "/tls_" + camelCaseName + "HumanReadableMap_INT_"
            + effectiveTime + ".tsv";

    deltaMachineReadableFileName =
        outputDirName + "/der2_" + pattern + mapProject.getMapRefsetPattern()
            + "Delta_INT_" + effectiveTime + ".txt";

    if (writeSnapshot == true) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Machine-readable release file:  "
              + snapshotMachineReadableFileName);

      // instantiate file writer
      snapshotMachineReadableWriter =
          new BufferedWriter(new FileWriter(snapshotMachineReadableFileName));
    }

    if (writeDelta == true) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Delta release file:             " + deltaMachineReadableFileName);

      // instantiate file writer
      deltaMachineReadableWriter =
          new BufferedWriter(new FileWriter(deltaMachineReadableFileName));

    }

    // human readable file is always written
    humanReadableWriter =
        new BufferedWriter(new FileWriter(humanReadableFileName));

    // Write header based on relation style
    if (mapProject.getMapRefsetPattern().equals(MapRefsetPattern.ExtendedMap)) {
      if (snapshotMachineReadableWriter != null) {
        snapshotMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\r\n");
        snapshotMachineReadableWriter.flush();
      }
      if (humanReadableWriter != null) {
        humanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tmapCategoryId\tmapCategoryName\r\n");
        humanReadableWriter.flush();
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
      if (humanReadableWriter != null) {
        humanReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\treferencedComponentName\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tmapTargetName\tcorrelationId\tcorrelationValue\r\n");
        humanReadableWriter.flush();
      }
      if (deltaMachineReadableWriter != null) {
        deltaMachineReadableWriter
            .write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\r\n");
        deltaMachineReadableWriter.flush();
      }

    }

    // /////////////////////////////////////////////////////
    // Prepare data
    // /////////////////////////////////////////////////////

    // instantiate the set of complex map ref set members to write
    Map<String, ComplexMapRefSetMember> complexMapRefSetMembersToWrite =
        new HashMap<>();

    // put all map records into the map record map
    for (MapRecord mr : mapRecordsToPublish) {
      if (mr == null)
        throw new Exception("Null record found in published list");
      mapRecordMap.put(mr.getConceptId(), mr);
    }

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

    Logger.getLogger(ReleaseHandler.class).info("Retrieving existing maps...");

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

      // get the source concept for this record

      /*
       * Logger.getLogger(ReleaseHandlerJpa.class).info(
       * "   Processing map record " + mapRecord.getId() + ", " +
       * mapRecord.getConceptId() + ", " + mapRecord.getConceptName());
       */
      // instantiate map of entries by group
      // this is the object containing entries to write
      Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

      // /////////////////////////////////////////////////////
      // Check for short-normal form no-write operation
      // If all parents have same code/advice present,
      // do not write
      // /////////////////////////////////////////////////////
      /*
       * boolean entriesDuplicated = true;
       * 
       * // cycle over all tree positions for this concept for (TreePosition tp
       * : treePositions.getTreePositions()) {
       * 
       * // // System.out.println("Checking ancestor path: " + //
       * tp.getAncestorPath());
       * 
       * String[] pathComponents = tp.getAncestorPath().split("~");
       * 
       * // // System.out.println(pathComponents[pathComponents.length-1]);
       * MapRecord parentRecord =
       * this.getMapRecordForTerminologyId(pathComponents[pathComponents.length
       * - 1]);
       * 
       * // if parent record is null if (parentRecord == null) { // record error
       * conceptErrors.put(tp.getTerminologyId(),
       * "Failed to retrieve parent concept " +
       * pathComponents[pathComponents.length - 1]);
       * 
       * // force exit by treating as short-form (i.e. do not process
       * entriesDuplicated = true; break;
       * 
       * } else { // if number of entries is different, automatically fails
       * short-form // check if (mapRecord.getMapEntries().size() !=
       * parentRecord.getMapEntries() .size()) { entriesDuplicated = false; }
       * 
       * // otherwise, check if all record entries are present on this parent
       * for (MapEntry childEntry : mapRecord.getMapEntries()) {
       * 
       * // check all parent entries for target and advice for (MapEntry
       * parentEntry : parentRecord.getMapEntries()) { if
       * (!childEntry.getTargetId().equals(parentEntry.getTargetId()) ||
       * !childEntry.getMapAdvices().equals( parentEntry.getMapAdvices())) {
       * entriesDuplicated = false; } } } }
       * 
       * // if failed test, break loop if (entriesDuplicated == false) break; }
       * 
       * // if entries duplicated on all parents, skip to next record if
       * (entriesDuplicated == false) {
       * 
       * conceptErrors .put(mapRecord.getConceptId(),
       * "Skipped due to short-form"); continue; }
       */

      // /////////////////////////////////////////////////////
      // Check for up-propagation
      // /////////////////////////////////////////////////////
      if (mapProject.isPropagatedFlag() == true
          && contentService.getDescendantConceptsCount(
              mapRecord.getConceptId(), mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion()) < mapProject
              .getPropagationDescendantThreshold()) {

        // /////////////////////////////////////////////////////
        // Get the tree positions for this concept
        // /////////////////////////////////////////////////////

        TreePositionList treePositions;
        try {
          // get all tree positions for this concept
          treePositions =
              contentService.getTreePositionsWithDescendants(
                  mapRecord.getConceptId(), mapProject.getSourceTerminology(),
                  mapProject.getSourceTerminologyVersion());
        } catch (Exception e) {
          conceptErrors.put(mapRecord.getConceptId(),
              "Could not retrieve tree positions");
          continue;
        }
        /*
         * Logger.getLogger(ReleaseHandlerJpa.class).info(
         * "    Record is up-propagated.");
         */
        // /////////////////////////////////////////////////////
        // Get descendant concepts
        // /////////////////////////////////////////////////////

        // check if tree positions were successfully retrieved
        if (treePositions.getCount() == 0) {
          conceptErrors.put(mapRecord.getConceptId(),
              "Could not retrieve tree positions");
          continue;
        }

        // use the first tree position in the list
        TreePosition treePosition =
            treePositions.getIterable().iterator().next();

        // get a list of tree positions sorted by position in hiearchy
        // (deepest-first)
        // NOTE: This list will contain the top-level/root map record
        List<TreePosition> treePositionDescendantList =
            getSortedTreePositionDescendantList(treePosition);
        /*
         * System.out.println("*** Descendant list has " +
         * treePositionDescendantList.size() + " elements");
         */

        // /////////////////////////////////////////////////////
        // Process up-propagated entries
        // /////////////////////////////////////////////////////

        // cycle over the tree positions again and add entries
        // note that the tree positions are in reverse order of
        // hierarchy depth
        for (TreePosition tp : treePositionDescendantList) {

          // get the parent map record for this tree position
          // used to check if entries are duplicated on parent
          String parent =
              tp.getAncestorPath().substring(
                  tp.getAncestorPath().lastIndexOf("~") + 1);
          MapRecord mrParent = this.getMapRecordForTerminologyId(parent);

          // skip the root level record, these entries are added
          // below, after the up-propagated entries
          if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

            // get the map record corresponding to this specific
            // ancestor path + concept Id
            MapRecord mr =
                this.getMapRecordForTerminologyId(tp.getTerminologyId());

            if (mr != null) {

              /*
               * Logger.getLogger(ReleaseHandlerJpa.class).info(
               * "     Adding entries from map record " + mr.getId() + ", " +
               * mr.getConceptId() + ", " + mr.getConceptName());
               */

              // if no parent, continue, but log error
              if (mrParent == null) {

                mrParent = new MapRecordJpa(); // create a blank for comparison
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
                  }

                  // use the map relation from the original entry
                  newEntry.setMapRelation(me.getMapRelation());

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

      /*
       * Logger.getLogger(ReleaseHandlerJpa.class).info(
       * "     Adding original entries");
       */
      for (MapEntry me : mapRecord.getMapEntries()) {

        /*
         * Logger.getLogger(ReleaseHandlerJpa.class).info(
         * "       Adding entry " + me.getId());
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

        // add map entry to map
        newEntry.setMapPriority(existingEntries.size() + 1);

        // if not the first entry and contains TRUE rule, set to
        // OTHERWISE TRUE
        if (mapProject.isRuleBased() == true && newEntry.getMapPriority() > 1
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

      // /////////////////////////////////////////////////////
      // Convert the record to complex map ref set members
      // /////////////////////////////////////////////////////

      // cycle over groups and entries in sequence
      for (int mapGroup : entriesByGroup.keySet()) {
        for (MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

          Concept concept = scopeConcepts.get(mapRecord.getConceptId());

          // convert this map entry into a complex map ref set member
          ComplexMapRefSetMember c_release =
              this.convertMapEntryToComplexMapRefSetMemberMapEntry(mapEntry,
                  mapRecord, mapProject, concept);

          String uuid_str = this.constructUuidKeyString(c_release);

          // attempt to retrieve any existing complex map ref set
          // member
          ComplexMapRefSetMember c_existing =
              complexMapRefSetMemberMap.get(uuid_str);

          // if existing found, re-use uuid, otherwise generate new
          if (c_existing == null) {

            c_release.setTerminologyId(this.getReleaseUuid(c_release)
                .toString());
          } else {
            c_release.setTerminologyId(c_existing.getTerminologyId());
            nRefSetMatches++;
          }

          // add this entry to the list of members to write
          complexMapRefSetMembersToWrite.put(c_release.getTerminologyId(),
              c_release);

          nEntries++;

        }
      }

      // increment the total record count
      if (++nRecords % Math.floor(mapRecordsToPublish.size() / 10) == 0) {
        Logger.getLogger(ReleaseHandlerJpa.class).info(
            "  " + nRecords + " processed, " + nEntries + " maps created");

      }

      contentService.clear();

    }

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Number of maps marked for writing: "
            + complexMapRefSetMembersToWrite.size());

    // /////////////////////////////////////////////////////
    // Prepare for file write
    // /////////////////////////////////////////////////////

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Retrieved map relations for human readable file:");

    // replace map relation ALL CAPS name with actual concept name from
    Set<MapRelation> mapRelations = mapProject.getMapRelations();

    Comparator<String> comparator = new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        String[] fields1 = o1.split("\t");
        String[] fields2 = o2.split("\t");

        // check for header (begins with id)
        if (fields1[0].equals("id"))
          return -1;
        if (fields2[0].equals("id"))
          return 1;

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

    // declare maps in use for computation
    Map<String, ComplexMapRefSetMember> complexMapRefSetMembersPreviouslyActive =
        new HashMap<>();
    Map<String, ComplexMapRefSetMember> tempMap = new HashMap<>();

    // First, construct set of previously active complex map ref set members
    for (ComplexMapRefSetMember c : complexMapRefSetMemberMap.values()) {
      if (c.isActive())
        complexMapRefSetMembersPreviouslyActive.put(c.getTerminologyId(), c);
    }

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Number of previously-published, active complex maps: "
            + complexMapRefSetMembersPreviouslyActive.size());

    // /////////////////////////////////////////////////////
    // Write the human-readable file
    // /////////////////////////////////////////////////////

    // TODO Parameterize or otherwise handle this
    if (true) {

      // write the computed complex map ref sets to file
      for (ComplexMapRefSetMember c : complexMapRefSetMembersToWrite.values()) {

        if (c.isActive() == true) {

          // get the map relation name for the human readable file
          MapRelation mapRelation = null;
          for (MapRelation mr : mapProject.getMapRelations()) {
            if (mr.getTerminologyId().equals(c.getMapRelationId().toString())) {
              mapRelation = mr;
            }
          }

          // get target concept, if not null
          Concept targetConcept = null;
          if (c.getMapTarget() != null && !c.getMapTarget().isEmpty()) {
            targetConcept =
                contentService.getConcept(c.getMapTarget(),
                    mapProject.getDestinationTerminology(),
                    mapProject.getDestinationTerminologyVersion());
          }

          humanReadableWriter.write(this
              .getHumanReadableTextforComplexMapRefSetMember(c, targetConcept,
                  mapRelation));
        }

      }

      humanReadableWriter.close();
    }

    // /////////////////////////////////////////////////////
    // Write the delta files
    // /////////////////////////////////////////////////////

    if (writeDelta == true) {

      Logger.getLogger(ReleaseHandlerJpa.class).info("Writing delta...");

      // case 1: currently active now modified
      // Copy current map of uuids to write into temp map
      // For each previously active uuid:
      // - check temp map for this uuid
      // - if present AND unchanged, remove from temp map
      // Write the values of the temp map
      tempMap = new HashMap<>(complexMapRefSetMembersToWrite);

      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Computing maps created or changed this cycle from "
              + tempMap.size() + " maps marked for writing...");

      int nModified = 0;
      int nUnchanged = 0;

      // cycle over all previously active
      for (ComplexMapRefSetMember c : complexMapRefSetMembersPreviouslyActive
          .values()) {

        // if set to write contains this previously active uuid
        if (tempMap.containsKey(c.getTerminologyId())) {

          // if this previously active member is present (equality check) in the
          // set to be written
          if (c.equals(tempMap.get(c.getTerminologyId()))) {

            // remove this concept from the set to be written -- unchanged
            tempMap.remove(c.getTerminologyId());

            nUnchanged++;
          } else {
            nModified++;
          }
        }
      }

      Logger.getLogger(ReleaseHandlerJpa.class).info("  Computation results:");
      Logger
          .getLogger(ReleaseHandlerJpa.class)
          .info(
              "    New       : "
                  + (complexMapRefSetMembersToWrite.size() - nModified - nUnchanged));
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "    Modified  : " + nModified);
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "    Unchanged : " + nUnchanged);

      // write new or modified maps to file
      for (ComplexMapRefSetMember c : tempMap.values()) {

        // write to files
        deltaMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));

      }

      Logger.getLogger(ReleaseHandlerJpa.class).info("  Writing complete.");

      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Computing maps inactivated this cycle from "
              + complexMapRefSetMembersPreviouslyActive.size()
              + " existing maps...");

      // case 2: previously active no longer present
      // Copy previously active map of uuids to write into temp map
      // For each uuid in current write set
      // - check temp map for this uuid
      // - if present, remove from temp map
      // Inactivate all remaining uuids in the temp map

      tempMap = new HashMap<>(complexMapRefSetMembersPreviouslyActive);

      for (String uuid : complexMapRefSetMembersToWrite.keySet()) {

        if (tempMap.containsKey(uuid)) {
          tempMap.remove(uuid);
        }

      }

      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "    Number of inactivated maps : " + tempMap.size());

      // set active to false and write inactivated complex maps
      for (ComplexMapRefSetMember c : tempMap.values()) {
        c.setActive(false);
        deltaMachineReadableWriter.write(this
            .getMachineReadableTextforComplexMapRefSetMember(c));

      }

      Logger.getLogger(ReleaseHandlerJpa.class).info("  Writing complete.");

      deltaMachineReadableWriter.flush();
      deltaMachineReadableWriter.close();

      Logger.getLogger(ReleaseHandlerJpa.class).info("  Sorting delta file...");

      // sort the file
      FileSorter.sortFile(deltaMachineReadableFileName, comparator);

      Logger.getLogger(ReleaseHandlerJpa.class).info("  Sorting complete.");
    }

    // /////////////////////////////////////////////////////
    // Write the snapshot files if indicated
    // /////////////////////////////////////////////////////
    /*
     * if (writeSnapshot == true) {
     * 
     * // Case 1: Current & active -- the records in database for
     * (ComplexMapRefSetMember c : complexMapRefSetMembersToWrite.values()) {
     * 
     * // human readable file requires target concept name Concept concept =
     * null; if (!c.getMapTarget().isEmpty()) { concept =
     * contentService.getConcept(c.getMapTarget(),
     * mapProject.getDestinationTerminology(),
     * mapProject.getDestinationTerminologyVersion()); }
     * 
     * // human readable file requires map relation name MapRelation mapRelation
     * = null; for (MapRelation mr : mapRelations) { if
     * (mr.getTerminologyId().equals(c.getMapRelationId())) { mapRelation = mr;
     * } }
     * 
     * // write to machine and human-readable files
     * snapshotMachineReadableWriter.write(this
     * .getMachineReadableTextforComplexMapRefSetMember(c));
     * snapshotHumanReadableWriter.write(this
     * .getHumanReadableTextforComplexMapRefSetMember(c, concept, mapRelation));
     * 
     * }
     * 
     * // Case 2: Previously existing, but not in current records tempSet = new
     * HashSet<>(complexMapRefSetMemberMap.values());
     * tempSet.removeAll(complexMapRefSetMembersToWrite.values());
     * 
     * // write the members for (ComplexMapRefSetMember c : tempSet) {
     * deltaMachineReadableWriter.write(this
     * .getMachineReadableTextforComplexMapRefSetMember(c));
     * 
     * // human readable file requires target concept name Concept concept =
     * null; if (!c.getMapTarget().isEmpty()) { concept =
     * contentService.getConcept(c.getMapTarget(),
     * mapProject.getDestinationTerminology(),
     * mapProject.getDestinationTerminologyVersion()); }
     * 
     * // this is clunky, result of rewrite from storing map enries MapRelation
     * mapRelation = null; for (MapRelation mr : mapRelations) { if
     * (mr.getTerminologyId().equals(c.getMapRelationId())) { mapRelation = mr;
     * } }
     * 
     * // write to machine and human-readable files
     * snapshotMachineReadableWriter.write(this
     * .getMachineReadableTextforComplexMapRefSetMember(c));
     * snapshotHumanReadableWriter.write(this
     * .getHumanReadableTextforComplexMapRefSetMember(c, concept, mapRelation));
     * }
     * 
     * // sort the files FileSorter.sortFile(snapshotMachineReadableFileName,
     * comparator); FileSorter.sortFile(snapshotHumanReadableFileName,
     * comparator); }
     */

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
        "Statistics for full release (Snapshot)");
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Total records in release      : " + nRecords);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Total records with errors     : " + conceptErrors.keySet().size());
    if (mapProject.isPropagatedFlag() == true) {
      Logger.getLogger(ReleaseHandlerJpa.class).info(
          "  Total records up-propagated : " + nRecordsPropagated);
    }
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Maps in full release        : " + nEntries);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Maps from previous release  : " + nRefSetMembers);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "     UUIDs remaining          : " + nRefSetMatches);
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "     UUIDs inactivated        : " + (nRefSetMembers - nRefSetMatches));
    // /////////////////////////////////////////////////////
    // Clean up
    // /////////////////////////////////////////////////////

    // close the services
    contentService.close();
    mappingService.close();
  }

  /**
   * Helper function to retrieve a map record for a given tree position. If in
   * set, returns that map record, if not, retrieves and adds it if possible.
   * @param tp
   * @return
   * @throws Exception
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

    // extract all advices and add to a list
    List<String> sortedAdvices = new ArrayList<>();

    for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
      sortedAdvices.add(mapAdvice.getDetail());
    }

    if (mapEntry.getRule().startsWith("IFA")
        && mapEntry.getRule().toUpperCase().contains("MALE")) {
      // do nothing -- do not add anything for gender rules
    }

    else if (mapEntry.getRule().startsWith("IFA")
        && mapEntry.getTargetId() != null && !mapEntry.getTargetId().isEmpty()) {
      sortedAdvices.add("MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT");
    }

    String mapAdviceStr = getHumanReadableMapAdvice(mapEntry);

    // sort advices and add them
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
     * Logger.getLogger(ReleaseHandlerJpa.class) .info("       Set rule to " +
     * rule);
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

    return getUuidForString(name);
  }

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
              + (mapProject.isRuleBased() ? complexMapRefSetMember.getMapRule()
                  : "")
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget())
              + "\t"
              + (targetConcept != null ? targetConcept
                  .getDefaultPreferredName() : "")
              + "\t"
              + "447561005"
              + "\t"
              + complexMapRefSetMember.getMapRelationId()
              + "\t"
              + (mapRelation != null ? mapRelation.getName()
                  : "FAILED MAP RELATION");

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
              + (mapProject.isRuleBased() ? complexMapRefSetMember.getMapRule()
                  : "")
              + "\t"
              + complexMapRefSetMember.getMapAdvice()
              + "\t"
              + (complexMapRefSetMember.getMapTarget() == null ? ""
                  : complexMapRefSetMember.getMapTarget())
              + "\t"
              + (targetConcept != null ? targetConcept
                  .getDefaultPreferredName() : "")
              + "\t"
              + complexMapRefSetMember.getMapRelationId()
              + "\t"
              + (mapRelation != null ? mapRelation.getName()
                  : "FAILED MAP RELATION");
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
              + (mapProject.isRuleBased() == true ? complexMapRefSetMember
                  .getMapRule() : "")
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
                  : complexMapRefSetMember.getMapTarget())
              + "\t"
              + complexMapRefSetMember.getMapRelationId();
    }

    entryLine += "\r\n";

    return entryLine;

  }

  /**
   * Helper function to access/add to dpn set
   * @param terminologyId
   * @return
   * @throws Exception
   */
  private String computeDefaultPreferredName(Concept concept) throws Exception {

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

  @Override
  public void beginRelease(MapProject mapProject, boolean removeRecords)
    throws Exception {

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Performing operations required for begin release");
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  Map project: " + mapProject.getName());
    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  "
            + (removeRecords == true ? "Removing out-of-scope records"
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

    Logger.getLogger(ReleaseHandlerJpa.class).info("Creating report...");

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

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Getting scope concepts for map project...");

    // get all scope concept terminology ids for this project
    Set<String> scopeConceptTerminologyIds = new HashSet<>();
    for (SearchResult sr : mappingService.findConceptsInScope(
        mapProject.getId(), null).getSearchResults()) {
      scopeConceptTerminologyIds.add(sr.getTerminologyId());
    }

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  " + scopeConceptTerminologyIds.size() + " concepts in scope.");

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Getting records for map project...");

    // get all map records for this project
    MapRecordList mapRecords =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "  " + mapRecords.getCount() + " map records retrieved.");

    // create a temp set of scope terminology ids
    Set<String> conceptsWithNoRecord =
        new HashSet<>(scopeConceptTerminologyIds);

    Logger.getLogger(ReleaseHandlerJpa.class).info("Cycling over records...");

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
            removeRecords == true ? "Removed record for concept not in scope"
                : "Concept not in scope";

        // separate error-type by previously-published or this-cycle-edited
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))
          resultMessages.add(reportMsg + " - previously published");
        else
          resultMessages.add(reportMsg + " - edited this cycle");

        // remove record if flag set
        if (removeRecords == true)
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

      this.addReportError(report, mapProject, terminologyId,
          c.getDefaultPreferredName(), "In-scope concept has no map record");
    }

    Logger.getLogger(ReleaseHandlerJpa.class).info(
        "Adding Release QA Report...");

    // commit the report
    reportService.commit();

    Logger.getLogger(ReleaseHandlerJpa.class).info("Done.");

    mappingService.close();
    reportService.close();

  }

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

    // create report result item and add it
    ReportResultItem item = new ReportResultItemJpa();
    item.setReportResult(reportResult);
    item.setItemId(terminologyId);
    item.setItemName(conceptName);
    item.setResultType(ReportResultType.CONCEPT);
    reportResult.addReportResultItem(item);
  }

}
