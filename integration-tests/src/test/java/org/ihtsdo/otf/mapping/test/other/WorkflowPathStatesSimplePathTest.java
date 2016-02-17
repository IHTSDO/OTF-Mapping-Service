package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
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
public class WorkflowPathStatesSimplePathTest {

  static WorkflowService ws;

  static ContentService cs;

  static Concept concept;

  static MapProject mapProject;

  static MapUser user;

  static PfsParameter pfs;

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

    /////////////////////////
    // Clear Objects
    /////////////////////////

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

    /////////////////////////
    // Construct Objects
    ////////////////////////

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
    user = new MapUserJpa();
    user.setApplicationRole(MapUserRole.SPECIALIST);
    user.setEmail("email");
    user.setName("user");
    user.setUserName("user");
    ws.addMapUser(user);

    // create a map project
    mapProject = new MapProjectJpa();
    mapProject.setSourceTerminology("st");
    mapProject.setSourceTerminologyVersion("stv");
    mapProject.setDestinationTerminology("dt");
    mapProject.setDestinationTerminologyVersion("dtv");
    mapProject.setName("project");
    mapProject.addScopeConcept(concept.getTerminologyId());
    mapProject.setWorkflowType(WorkflowType.SIMPLE_PATH);
    mapProject.setProjectSpecificAlgorithmHandlerClass(
        DefaultProjectSpecificAlgorithmHandler.class.getCanonicalName());
    mapProject.addMapSpecialist(user);

    ws.addMapProject(mapProject);

    pfs = new PfsParameterJpa();
    pfs.setMaxResults(10);

    wph = ws.getWorkflowPathHandler(mapProject.getWorkflowType().toString());

  }

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
  @Test
  public void testNormalWorkflowLegacySpecialistNoConflict() throws Exception {

    Logger.getLogger(this.getClass())
        .info("---------------------------------------------------");
    Logger.getLogger(this.getClass()).info("Normal workflow");
    Logger.getLogger(this.getClass())
        .info("---------------------------------------------------");

    SearchResultList list;

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept = cs.getConcept(firstResult.getTerminologyId(),
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());

    // assign work to user
    ws.processWorkflowAction(mapProject, concept, user, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find assigned work for spec1User
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);

    // get the map record for the user
    MapRecord mr = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(mr.getWorkflowStatus().equals(WorkflowStatus.NEW));

    // unassign
    ws.processWorkflowAction(mapProject, concept, user, null,
        WorkflowAction.UNASSIGN);

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);

    // find assigned work for spec1User
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // get the map records for this project and concept
    MapRecordList mrList = ws.getMapRecordsForProjectAndConcept(
        mapProject.getId(), concept.getTerminologyId());
    assertTrue(mrList.getCount() == 0);

    // reassign work to user
    ws.processWorkflowAction(mapProject, concept, user, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // get the map record for the user
    mrList = ws.getMapRecordsForProjectAndConcept(mapProject.getId(),
        concept.getTerminologyId());
    assertTrue(mrList.getCount() == 1);
    assertTrue(mrList.getMapRecords().get(0).getWorkflowStatus()
        .equals(WorkflowStatus.NEW));

    // save for later
    ws.processWorkflowAction(mapProject, concept, user,
        new MapRecordJpa(mrList.getMapRecords().get(0), true), WorkflowAction.SAVE_FOR_LATER);

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find assigned work for user
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);
    
    mr = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(
        mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS));

    // finish work for user
    ws.processWorkflowAction(mapProject, concept, user,
        new MapRecordJpa(mr, true), WorkflowAction.FINISH_EDITING);


    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find assigned work for user
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);

    mr = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE));

    // finish work for user
    ws.processWorkflowAction(mapProject, concept, user,
        new MapRecordJpa(mr, true), WorkflowAction.FINISH_EDITING);

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find assigned work for spec1User
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 1);

    mr = ws.getMapRecord(list.getSearchResults().get(0).getId());
    assertTrue(mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE));

    ws.processWorkflowAction(mapProject, concept, user,
        new MapRecordJpa(mr, true), WorkflowAction.PUBLISH);

    // find available work
    list = ws.findAvailableWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find assigned work for spec1User
    list = ws.findAssignedWork(mapProject, user, MapUserRole.SPECIALIST, null,
        pfs);
    assertTrue(list.getCount() == 0);

    // find the map record for this project and concept
    mrList = ws.getMapRecordsForProjectAndConcept(mapProject.getId(),
        concept.getTerminologyId());
    assertTrue(mrList.getCount() == 1);
    assertTrue(mrList.getMapRecords().get(0).getWorkflowStatus()
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
