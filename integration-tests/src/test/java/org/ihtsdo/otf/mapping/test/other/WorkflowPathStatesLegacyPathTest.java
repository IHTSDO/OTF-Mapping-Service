package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for workflow path states on "non legacy path".
 */
public class WorkflowPathStatesLegacyPathTest {

  /** The ws. */
  static WorkflowService ws;

  /** The cs. */
  static ContentService cs;

  /** The concept. */
  static Concept concept;

  /** The map project. */
  static MapProject mapProject;

  /** The legacy record. */
  static MapRecord legacyRecord;

  /** The lead user. */
  static MapUser legacyUser;

  /** The spec1 user. */
  static MapUser spec1User;

  /** The spec2 user. */
  static MapUser spec2User;

  /** The lead user. */
  static MapUser leadUser;

  /** The pfs. */
  static PfsParameter pfs;

  /** The wph. */
  static WorkflowPathHandler wph;

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void init() throws Exception {
    ws = new WorkflowServiceJpa();
    cs = new ContentServiceJpa();

    // ///////////////////////
    // Clear Objects
    // ///////////////////////

    // clear all concepts
    for (Concept c : cs.getAllConcepts("st", "stv").getConcepts()) {
      cs.removeConcept(c.getId());
    }

    // clear all map records
    for (MapRecord m : ws.getMapRecords().getMapRecords()) {
      ws.removeMapRecord(m.getId());
    }

    // clear all map projects
    for (MapProject m : ws.getMapProjects().getMapProjects()) {
      ws.removeMapProject(m.getId());
    }

    // clear all map users
    for (MapUser m : ws.getMapUsers().getMapUsers()) {
      ws.removeMapUser(m.getId());
    }

    // ///////////////////////
    // Construct Objects
    // //////////////////////

    // create a concept
    concept = new ConceptJpa();
    concept.setTerminology("st");
    concept.setTerminologyVersion("stv");
    concept.setTerminologyId("1");
    concept.setDefaultPreferredName("source");
    concept.setModuleId(1L);
    concept.setActive(true);
    concept.setDefinitionStatusId(1L);
    cs.addConcept(concept);

    cs.computeTreePositions("st", "stv", "1", "1");

    // create map users
    legacyUser = new MapUserJpa();
    legacyUser.setApplicationRole(MapUserRole.SPECIALIST);
    legacyUser.setEmail("email");
    legacyUser.setName("legacy");
    legacyUser.setUserName("legacy");
    ws.addMapUser(legacyUser);

    spec1User = new MapUserJpa();
    spec1User.setApplicationRole(MapUserRole.SPECIALIST);
    spec1User.setEmail("email");
    spec1User.setName("spec1");
    spec1User.setUserName("spec1");
    ws.addMapUser(spec1User);

    spec2User = new MapUserJpa();
    spec2User.setApplicationRole(MapUserRole.SPECIALIST);
    spec2User.setEmail("email");
    spec2User.setName("spec2");
    spec2User.setUserName("spec2");
    ws.addMapUser(spec2User);

    leadUser = new MapUserJpa();
    leadUser.setApplicationRole(MapUserRole.SPECIALIST);
    leadUser.setEmail("email");
    leadUser.setName("lead");
    leadUser.setUserName("lead");
    ws.addMapUser(leadUser);

    // create a map project
    mapProject = new MapProjectJpa();
    mapProject.setSourceTerminology("st");
    mapProject.setSourceTerminologyVersion("stv");
    mapProject.setDestinationTerminology("dt");
    mapProject.setDestinationTerminologyVersion("dtv");
    mapProject.setName("project");
    mapProject.addScopeConcept(concept.getTerminologyId());
    mapProject.setWorkflowType(WorkflowType.LEGACY_PATH);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass(DefaultProjectSpecificAlgorithmHandler.class
            .getCanonicalName());
    mapProject.addMapSpecialist(spec1User);
    mapProject.addMapSpecialist(spec2User);
    mapProject.addMapLead(leadUser);

    ws.addMapProject(mapProject);

    // create a new map record (legacy owned)
    // do not persist record, will be used in @Before
    legacyRecord = new MapRecordJpa();
    legacyRecord.setConceptId(concept.getTerminologyId());
    legacyRecord.setConceptName(concept.getDefaultPreferredName());
    legacyRecord.setLastModified(new Date().getTime());
    legacyRecord.setLastModifiedBy(legacyUser);
    legacyRecord.setOwner(legacyUser);
    legacyRecord.setMapProjectId(mapProject.getId());
    legacyRecord.setWorkflowStatus(WorkflowStatus.PUBLISHED);

    MapEntry e = new MapEntryJpa();
    e.setMapBlock(1);
    e.setMapGroup(1);
    e.setMapPriority(1);
    e.setMapRecord(legacyRecord);
    e.setRule("TRUE");
    e.setTargetId("2");
    e.setTargetName("tc");
    legacyRecord.addMapEntry(e);

    pfs = new PfsParameterJpa();
    pfs.setMaxResults(10);

    wph = ws.getWorkflowPathHandler(mapProject.getWorkflowType().toString());

  }

  /**
   * Reset state.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Before
  public void resetState() throws Exception {

    Thread.sleep(2000);

    if (ws == null) {
      ws = new WorkflowServiceJpa();
    }
    if (cs == null) {
      cs = new ContentServiceJpa();
    }

    // delete any existing map records
    for (TrackingRecord tr : ws.getTrackingRecordsForMapProject(mapProject)
        .getTrackingRecords()) {
      for (Long mrId : tr.getMapRecordIds()) {
        try {
          ws.removeMapRecord(mrId);
        } catch (Exception e) {
          // do nothing
        }
      }
    }

    // copy and add the legacy record
    MapRecord newRecord = new MapRecordJpa(legacyRecord, false);
    ws.addMapRecord(newRecord);

    // compute the workflow
    ws.computeWorkflow(mapProject);

    TrackingRecordList trList = ws.getTrackingRecordsForMapProject(mapProject);
    assertTrue(trList.getTrackingRecords().size() == 1);

  }

  /*
   * private static void clearWorkflow() throws Exception {
   * 
   * for (TrackingRecord tr : ws.getTrackingRecordsForMapProject(mapProject)
   * .getTrackingRecords()) { for (Long mrId : tr.getMapRecordIds()) { MapRecord
   * mr = ws.getMapRecord(mrId); if
   * (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
   * mr.setWorkflowStatus(WorkflowStatus.PUBLISHED); ws.updateMapRecord(mr); }
   * else { ws.removeMapRecord(mr.getId()); } } }
   * ws.computeWorkflow(mapProject); ws.clear(); }
   */
  /**
   * Test normal workflow legacy specialist no conflict.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalWorkflowLegacySpecialistNoConflict() throws Exception {

    Logger.getLogger(this.getClass()).info(
        "---------------------------------------------------");
    Logger.getLogger(this.getClass()).info(
        "Normal workflow: No Conflict, Legacy and Specialist");
    Logger.getLogger(this.getClass()).info(
        "---------------------------------------------------");

    SearchResultList list;

    // find available work
    list =
        ws.findAvailableWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept =
        cs.getConcept(firstResult.getTerminologyId(),
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

    // assign work to spec1User
    ws.processWorkflowAction(mapProject, concept, spec1User, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for spec1User
    list =
        ws.findAssignedWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    // get the map record for the user
    MapRecord mr1 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    // get the legacy record
    MapRecord legacyRecord = null;
    for (MapRecord mr : ws.getMapRecordsForProjectAndConcept(
        mr1.getMapProjectId(), mr1.getConceptId()).getMapRecords()) {
      if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
        legacyRecord = mr;
      }
    }

    assertNotNull(legacyRecord);

    // copy the legacy record
    MapRecord duplicateRecord = new MapRecordJpa(legacyRecord, false);
    duplicateRecord.setId(mr1.getId());
    duplicateRecord.setOwner(mr1.getOwner());
    duplicateRecord.setWorkflowStatus(mr1.getWorkflowStatus());

    // assert records are equivalent
    ValidationResult result =
        ws.getProjectSpecificAlgorithmHandler(mapProject).compareMapRecords(
            legacyRecord, duplicateRecord);
    assertTrue(result.isValid());

    // finish work for spec1User -- make sure to copy map record into new
    // persistent object
    // or it is overwritten by subsequent retrieval
    ws.processWorkflowAction(mapProject, concept, spec1User, new MapRecordJpa(
        duplicateRecord, true), WorkflowAction.FINISH_EDITING);

    // re-retrieve concept/project records, should only be 1, and it should be
    // publication ready
    MapRecordList finalRecords =
        ws.getMapRecordsForProjectAndConcept(mr1.getMapProjectId(),
            mr1.getConceptId());
    assertTrue(finalRecords.getCount() == 1);
    assertTrue(finalRecords.getMapRecords().get(0).getWorkflowStatus()
        .equals(WorkflowStatus.READY_FOR_PUBLICATION));

  }

  /**
   * Test normal workflow two specialist no conflict.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalWorkflowTwoSpecialistNoConflict() throws Exception {

    Logger.getLogger(this.getClass()).info(
        "--------------------------------------------------");
    Logger.getLogger(this.getClass()).info(
        "Normal workflow: Two Specialists, No Conflict");
    Logger.getLogger(this.getClass()).info(
        "--------------------------------------------------");
    SearchResultList list;

    // find available work
    list =
        ws.findAvailableWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept =
        cs.getConcept(firstResult.getTerminologyId(),
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

    // assign work to spec1User
    ws.processWorkflowAction(mapProject, concept, spec1User, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for spec1User
    list =
        ws.findAssignedWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    // get the map record for the user
    MapRecord mr1 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    // finish work for spec1User
    ws.processWorkflowAction(mapProject, concept, spec1User, new MapRecordJpa(
        mr1, true), WorkflowAction.FINISH_EDITING);

    // find available work for spec2User
    list =
        ws.findAvailableWork(mapProject, spec2User, MapUserRole.SPECIALIST,
            null, pfs);

    // assign work to spec1User
    ws.processWorkflowAction(mapProject, concept, spec2User, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for spec1User
    list =
        ws.findAssignedWork(mapProject, spec2User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    // get the map record for the user
    MapRecord mr2 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    // finish work for spec2User
    ws.processWorkflowAction(mapProject, concept, spec2User, new MapRecordJpa(
        mr2, true), WorkflowAction.FINISH_EDITING);

    // re-retrieve concept/project records, should only be 1, and it should be
    // publication ready
    MapRecordList finalRecords =
        ws.getMapRecordsForProjectAndConcept(mr1.getMapProjectId(),
            mr1.getConceptId());
    assertTrue(finalRecords.getCount() == 1);
    assertTrue(finalRecords.getMapRecords().get(0).getWorkflowStatus()
        .equals(WorkflowStatus.READY_FOR_PUBLICATION));

  }

  /**
   * Test normal workflow two specialists with conflict.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalWorkflowTwoSpecialistsWithConflict() throws Exception {
    Logger.getLogger(this.getClass()).info(
        "--------------------------------------------------");
    Logger.getLogger(this.getClass()).info(
        "Normal workflow: Two Specialists, In Conflict");
    Logger.getLogger(this.getClass()).info(
        "--------------------------------------------------");
    SearchResultList list;

    // find available work
    list =
        ws.findAvailableWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() > 0);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept =
        cs.getConcept(firstResult.getTerminologyId(),
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

    // assign work to spec1User
    ws.processWorkflowAction(mapProject, concept, spec1User, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for spec1User
    list =
        ws.findAssignedWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() == 1);

    // get the map record for the user
    MapRecord mr1 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    assertTrue(mr1.getWorkflowStatus().equals(WorkflowStatus.NEW));

    // finish work for spec1User (conflict with legacy)
    ws.processWorkflowAction(mapProject, concept, spec1User, new MapRecordJpa(
        mr1, true), WorkflowAction.FINISH_EDITING);

    // find assigned work for spec1User
    list =
        ws.findAssignedWork(mapProject, spec1User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() == 1);
    assertTrue(ws.getMapRecord(list.getSearchResults().get(0).getId())
        .getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE));

    // find available work for spec2User
    list =
        ws.findAvailableWork(mapProject, spec2User, MapUserRole.SPECIALIST,
            null, pfs);

    // assign work to spec2User
    ws.processWorkflowAction(mapProject, concept, spec2User, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for spec2User
    list =
        ws.findAssignedWork(mapProject, spec2User, MapUserRole.SPECIALIST,
            null, pfs);
    assertTrue(list.getCount() == 1);
    MapRecord mr2 = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(mr2.getWorkflowStatus().equals(WorkflowStatus.NEW));

    // get the map record for the user from the legacy record and set id
    // goal is to generate a conflict between users
    MapRecord newRecord = new MapRecordJpa(legacyRecord, false);
    newRecord.setId(list.getSearchResults().get(0).getId());
    newRecord.setOwner(spec2User);
    newRecord.setWorkflowStatus(mr2.getWorkflowStatus());

    // finish work for spec2User
    ws.processWorkflowAction(mapProject, concept, spec2User, new MapRecordJpa(
        newRecord, true), WorkflowAction.FINISH_EDITING);

    MapRecordList currentRecords =
        ws.getMapRecordsForProjectAndConcept(mapProject.getId(),
            concept.getTerminologyId());
    assertTrue(currentRecords.getCount() == 3);
    for (MapRecord mr : currentRecords.getMapRecords()) {
      if (mr.getOwner().equals(legacyUser)) {
        assertTrue(mr.getWorkflowStatus().equals(WorkflowStatus.REVISION));
      } else {
        assertTrue(mr.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED));
      }
    }

    // find available work for lead
    list =
        ws.findAvailableWork(mapProject, leadUser, MapUserRole.LEAD, null, pfs);
    assertTrue(list.getCount() == 1);

    // assign work to lead
    ws.processWorkflowAction(mapProject, concept, leadUser, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // get assigned work for lead
    list =
        ws.findAssignedWork(mapProject, leadUser, MapUserRole.LEAD, null, pfs);
    assertTrue(list.getCount() == 1);

    // get map record for lead
    MapRecord mr3 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    assertTrue(mr3.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW));

    // finish work for lead
    ws.processWorkflowAction(mapProject, concept, leadUser, new MapRecordJpa(
        mr3, true), WorkflowAction.FINISH_EDITING);

    // get assigned work for lead
    list =
        ws.findAssignedWork(mapProject, leadUser, MapUserRole.LEAD, null, pfs);
    assertTrue(list.getCount() == 1);

    // get map record for lead
    mr3 = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(mr3.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED));

    // publish work
    ws.processWorkflowAction(mapProject, concept, leadUser, new MapRecordJpa(
        mr3, true), WorkflowAction.PUBLISH);

    // check tracking records
    assertTrue(ws.getTrackingRecordForMapProjectAndConcept(mapProject,
        concept.getTerminologyId()) == null);

    // check map record
    MapRecordList mrs =
        ws.getMapRecordsForProjectAndConcept(mapProject.getId(),
            concept.getTerminologyId());
    assertTrue(mrs.getCount() == 1);
    assertTrue(mrs.getMapRecords().get(0).getWorkflowStatus()
        .equals(WorkflowStatus.READY_FOR_PUBLICATION));

  }

  /**
   * Cleanup.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void cleanup() throws Exception {
    if (ws != null) {
      ws.close();
    }
    if (cs != null) {
      cs.close();
    }
  }

}
