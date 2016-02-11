package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for workflow path states on "non legacy path".
 */
public class WorkflowPathStatesLegacyPathTest {

  static WorkflowService ws;

  static ContentService cs;

  static MapProject mapProject;

  static MapUser mapUser1, mapUser2, mapUser3;

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

    mapProject = ws.getMapProject(4L);
    Iterator<MapUser> iter = mapProject.getMapSpecialists().iterator();
    mapUser1 = iter.next();
    mapUser2 = iter.next();
    mapUser3 = iter.next();

    pfs = new PfsParameterJpa();
    pfs.setMaxResults(10);

    wph = ws.getWorkflowPathHandler(mapProject.getWorkflowType().toString());


  }
/*
  private static void clearWorkflow() throws Exception {

    for (TrackingRecord tr : ws.getTrackingRecordsForMapProject(mapProject)
        .getTrackingRecords()) {
      for (Long mrId : tr.getMapRecordIds()) {
        MapRecord mr = ws.getMapRecord(mrId);
        if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
          mr.setWorkflowStatus(WorkflowStatus.PUBLISHED);
          ws.updateMapRecord(mr);
        } else {
          ws.removeMapRecord(mr.getId());
        }
      }
    }
    ws.computeWorkflow(mapProject);
    ws.clear();
  }
*/
  @Test
  public void testNormalWorkflowLegacySpecialistNoConflict() throws Exception {
    
    Logger.getLogger(this.getClass()).info("---------------------------------------------------");
    Logger.getLogger(this.getClass()).info("Normal workflow: No Conflict, Legacy and Specialist");
    Logger.getLogger(this.getClass()).info("---------------------------------------------------");  
    
    SearchResultList list;

    // find available work
    list = ws.findAvailableWork(mapProject, mapUser1, MapUserRole.SPECIALIST,
        null, pfs);
    assertTrue(list.getCount() > 0);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept = cs.getConcept(firstResult.getTerminologyId(),
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());

    // assign work to mapUser1
    ws.processWorkflowAction(mapProject, concept, mapUser1, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for mapUser1
    list = ws.findAssignedWork(mapProject, mapUser1, MapUserRole.SPECIALIST,
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
    ValidationResult result = ws.getProjectSpecificAlgorithmHandler(mapProject)
        .compareMapRecords(legacyRecord, duplicateRecord);
    assertTrue(result.isValid());

    // finish work for mapUser1 -- make sure to copy map record into new
    // persistent object
    // or it is overwritten by subsequent retrieval
    ws.processWorkflowAction(mapProject, concept, mapUser1,
        new MapRecordJpa(duplicateRecord, true), WorkflowAction.FINISH_EDITING);

    // re-retrieve concept/project records, should only be 1, and it should be
    // publication ready
    MapRecordList finalRecords = ws.getMapRecordsForProjectAndConcept(
        mr1.getMapProjectId(), mr1.getConceptId());
    assertTrue(finalRecords.getCount() == 1);
    assertTrue(finalRecords.getMapRecords().get(0).getWorkflowStatus()
        .equals(WorkflowStatus.READY_FOR_PUBLICATION));

  }

  @Test
  public void testNormalWorkflowTwoSpecialistNoConflict() throws Exception {
    
    Logger.getLogger(this.getClass()).info("--------------------------------------------------");
    Logger.getLogger(this.getClass()).info("Normal workflow: Two Specialists, No Conflict");
    Logger.getLogger(this.getClass()).info("--------------------------------------------------");  
    SearchResultList list;

    // find available work
    list = ws.findAvailableWork(mapProject, mapUser1, MapUserRole.SPECIALIST,
        null, pfs);
    assertTrue(list.getCount() > 0);

    SearchResult firstResult = list.getSearchResults().get(0);
    Concept concept = cs.getConcept(firstResult.getTerminologyId(),
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());

    // assign work to mapUser1
    ws.processWorkflowAction(mapProject, concept, mapUser1, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for mapUser1
    list = ws.findAssignedWork(mapProject, mapUser1, MapUserRole.SPECIALIST,
        null, pfs);
    assertTrue(list.getCount() > 0);

    // get the map record for the user
    MapRecord mr1 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    // finish work for mapUser1
    ws.processWorkflowAction(mapProject, concept, mapUser1,
        new MapRecordJpa(mr1, true), WorkflowAction.FINISH_EDITING);

    // find available work for mapUser2
    list = ws.findAvailableWork(mapProject, mapUser2, MapUserRole.SPECIALIST,
        null, pfs);

    // assign work to mapUser1
    ws.processWorkflowAction(mapProject, concept, mapUser2, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    // find assigned work for mapUser1
    list = ws.findAssignedWork(mapProject, mapUser2, MapUserRole.SPECIALIST,
        null, pfs);
    assertTrue(list.getCount() > 0);

    // get the map record for the user
    MapRecord mr2 = ws.getMapRecord(list.getSearchResults().get(0).getId());

    // finish work for mapUser1
    ws.processWorkflowAction(mapProject, concept, mapUser1,
        new MapRecordJpa(mr2, true), WorkflowAction.FINISH_EDITING);

    // re-retrieve concept/project records, should only be 1, and it should be
    // publication ready
    MapRecordList finalRecords = ws.getMapRecordsForProjectAndConcept(
        mr1.getMapProjectId(), mr1.getConceptId());
    assertTrue(finalRecords.getCount() == 1);
    assertTrue(finalRecords.getMapRecords().get(0).getWorkflowStatus()
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
