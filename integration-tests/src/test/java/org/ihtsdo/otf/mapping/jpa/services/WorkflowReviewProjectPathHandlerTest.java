package org.ihtsdo.otf.mapping.jpa.services;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowReviewProjectPathHandler;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowReviewProjectPathHandlerTest {

  private static WorkflowReviewProjectPathHandler handler;

  private static MappingService mappingService;

  private static WorkflowService workflowService;

  private TrackingRecord trackingRecord;

  private static MapUser specialist;

  private static MapUser lead;

  @BeforeClass
  public static void init() throws Exception {

    // instantiate handler
    handler = new WorkflowReviewProjectPathHandler();

    // open services
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // ensure database is clean
    resetRecords();
    removeUsers();

    // add the users
    addUsers();

  }

  @Test
  public void testStates() throws Exception {
    
  }

  @Test
  public void testInitialState() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing initial state...");

    // reset the managers
    resetRecords();

    // set up the tracking record
    trackingRecord = new TrackingRecordJpa();
    trackingRecord.setTerminologyId("1");
    trackingRecord.setTerminology("terminology");
    trackingRecord.setTerminologyVersion("version");
    trackingRecord.setDefaultPreferredName("concept1");
    trackingRecord.setMapProjectId(1L);
    trackingRecord.setSortKey("");
    workflowService.addTrackingRecord(trackingRecord);

    // set up the map records
    Set<MapRecord> records = new HashSet<>(); // no records for this case

    // compute workflow
    computeWorkflow(records);

    // validate tracking record
    assertTrue("Tracking Record valid",
        handler.validateTrackingRecord(trackingRecord).isValid());

    // test all workflow actions for this state
    ValidationResult result = testWorkflowActions();

    for (WorkflowAction action : WorkflowAction.values()) {
      switch (action) {
        case ASSIGN_FROM_INITIAL_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case ASSIGN_FROM_SCRATCH:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CANCEL:
          assertTrue(result.getMessages().contains(action.toString()));
          break;
        case CREATE_QA_RECORD:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case FINISH_EDITING:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case PUBLISH:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case SAVE_FOR_LATER:
          assertTrue(result.getErrors().contains(action.toString()));
          break;
        case UNASSIGN:
          assertTrue(result.getErrors().contains(action.toString()));
          break;

      }
    }

  }

  @Test
  public void testSpecialistStates() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED)));

    int legalCombinationsTested = 0;

    // test all single workflow states
    for (WorkflowStatus status : WorkflowStatus.values()) {

      // reset the managers
      resetRecords();

      // set up the records
      MapRecord record = new MapRecordJpa();
      record.setConceptId("1");
      record.setConceptName("concept1");
      record.setCountDescendantConcepts(0L);
      record.setLastModified(Calendar.getInstance().getTimeInMillis());
      record.setWorkflowStatus(status);
      record.setOwner(specialist);
      record.setLastModifiedBy(specialist);
      mappingService.addMapRecord(record);

      // compute workflow
      computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record)));

      // if this tracking record's computed combination is in the legal
      // combination set
      // proceed with action testing
      if (legalCombinations.contains(handler
          .getWorkflowCombinationForTrackingRecord(trackingRecord))) {

        legalCombinationsTested++;

        // validate tracking record
        assertTrue("Tracking Record valid",
            handler.validateTrackingRecord(trackingRecord).isValid());

        // test all workflow actions for this state
        ValidationResult result = testWorkflowActions();

        System.out.println(result.getMessages());
        System.out.println("-");
        System.out.println(result.getErrors());

        for (WorkflowAction action : WorkflowAction.values()) {
          switch (action) {
            case ASSIGN_FROM_INITIAL_RECORD:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case ASSIGN_FROM_SCRATCH:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case CANCEL:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case CREATE_QA_RECORD:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case FINISH_EDITING:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case PUBLISH:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case SAVE_FOR_LATER:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case UNASSIGN:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
          }
        }

        // otherwise, assert that tracking record evaluates as invalid
      } else {
        assertTrue("Tracking Record should not be valid", !handler
            .validateTrackingRecord(trackingRecord).isValid());
      }
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsTested);

  }

  @Test
  public void testLeadIncompleteState() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing lead incomplete states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));

    Set<WorkflowStatusCombination> possibleCombinations = new HashSet<>();
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
       
      }
    }

    // test all single workflow states
    for (WorkflowStatus status : WorkflowStatus.values()) {

      // reset the managers
      resetRecords();

      // set up the records
      MapRecord record = new MapRecordJpa();
      record.setConceptId("1");
      record.setConceptName("concept1");
      record.setCountDescendantConcepts(0L);
      record.setLastModified(Calendar.getInstance().getTimeInMillis());
      record.setWorkflowStatus(WorkflowStatus.NEW);
      record.setOwner(specialist);
      record.setLastModifiedBy(specialist);
      mappingService.addMapRecord(record);

      // compute workflow
      computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record)));

      if (legalCombinations.contains(new WorkflowStatusCombination(Arrays
          .asList(status)))) {

        // validate tracking record
        assertTrue("Tracking Record valid",
            handler.validateTrackingRecord(trackingRecord).isValid());

        // test all workflow actions for this state
        ValidationResult result = testWorkflowActions();

        System.out.println(result.getMessages());
        System.out.println("-");
        System.out.println(result.getErrors());

        for (WorkflowAction action : WorkflowAction.values()) {
          switch (action) {
            case ASSIGN_FROM_INITIAL_RECORD:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case ASSIGN_FROM_SCRATCH:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case CANCEL:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case CREATE_QA_RECORD:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case FINISH_EDITING:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case PUBLISH:
              assertTrue(result.getErrors().contains(action.toString()));
              break;
            case SAVE_FOR_LATER:
              assertTrue(result.getMessages().contains(action.toString()));
              break;
            case UNASSIGN:
              assertTrue(result.getMessages().contains(action.toString()));
              break;

          }
        }
      } else {
        assertTrue("Tracking Record should not be valid", handler
            .validateTrackingRecord(trackingRecord).isValid());
      }
    }
  }

  @Test
  public void testLeadCompleteState() {

  }

  @AfterClass
  public static void cleanup() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Clean-up");
    resetRecords();
    removeUsers();

    mappingService.close();
    workflowService.close();
  }

  // helper function to clear persistence environment
  // and reinstantiate empty tracking record and map users
  private static void resetRecords() throws Exception {

    // remove tracking records
    for (TrackingRecord trackingRecord : workflowService.getTrackingRecords()
        .getTrackingRecords())
      workflowService.removeTrackingRecord(trackingRecord.getId());

    // remove map records
    for (MapRecord mapRecord : mappingService.getMapRecords().getMapRecords())
      mappingService.removeMapRecord(mapRecord.getId());
  }

  private static void addUsers() throws Exception {
    // create map users
    specialist = new MapUserJpa();
    specialist.setUserName("spec");
    specialist.setApplicationRole(MapUserRole.SPECIALIST);
    specialist.setName("Specialist");
    specialist.setEmail("email");

    lead = new MapUserJpa();
    lead.setUserName("lead");
    lead.setName("Lead");
    lead.setApplicationRole(MapUserRole.LEAD);
    lead.setEmail("email");

    mappingService.addMapUser(specialist);
    mappingService.addMapUser(lead);
  }

  private static void removeUsers() throws Exception {
    // remove map users
    for (MapUser user : mappingService.getMapUsers().getMapUsers())
      mappingService.removeMapUser(user.getId());
  }

  private void computeWorkflow(Set<MapRecord> mapRecords) throws Exception {
    trackingRecord = new TrackingRecordJpa();

    for (MapRecord mr : mapRecords) {
      trackingRecord.addMapRecordId(mr.getId());
      trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
      trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner().getUserName(),
          mr.getWorkflowStatus().toString());
    }
  }

  private ValidationResult testWorkflowActions() throws Exception {

    ValidationResult result = new ValidationResultJpa();

    for (WorkflowAction action : WorkflowAction.values()) {
      if (handler.validateTrackingRecordForActionAndUser(trackingRecord,
          action, null).isValid()) {
        result.addMessage(action.toString());
      } else {
        result.addError(action.toString());
      }
    }

    return result;

  }
}
