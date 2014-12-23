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
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowQaPathHandlerTest {

  private static WorkflowQaPathHandler handler;

  private static MappingService mappingService;

  private static WorkflowService workflowService;

  private static TrackingRecord trackingRecord;

  private static MapUser specialist, specialist2;

  private static MapUser lead;

  @BeforeClass
  public static void init() throws Exception {

    // instantiate handler
    handler = new WorkflowQaPathHandler();

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

    WorkflowStatusCombination combination;
    Set<WorkflowStatusCombination> allCombinations;
    int nStatesFound = 0;
    int nStatesTotal = 0;

    // test empty state
    if (handler.isEmptyWorkflowAllowed()) {
      combination = new WorkflowStatusCombination();
      if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
        nStatesFound++;
      }
      nStatesTotal++;
    }

    // test all one-record combination states
    allCombinations = new HashSet<>();
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      combination = new WorkflowStatusCombination(Arrays.asList(status1));
      allCombinations.add(combination);
    }

    nStatesTotal += allCombinations.size();

    for (WorkflowStatusCombination c : allCombinations) {
      if (handler.isWorkflowCombinationInTrackingRecordStates(c)) {
        nStatesFound++;
      }
    }

    // test all two-record combination states
    allCombinations = new HashSet<>();
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        combination =
            new WorkflowStatusCombination(Arrays.asList(status1, status2));

        if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
          nStatesFound++;
        }
      }
    }

    nStatesTotal += allCombinations.size();

    for (WorkflowStatusCombination c : allCombinations) {
      if (handler.isWorkflowCombinationInTrackingRecordStates(c)) {
        nStatesFound++;
      }
    }

    // test all three-record combination states
    allCombinations = new HashSet<>();
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        for (WorkflowStatus status3 : WorkflowStatus.values()) {
          combination =
              new WorkflowStatusCombination(Arrays.asList(status1, status2,
                  status3));

          if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
            nStatesFound++;
          }
        }
      }
    }

    nStatesTotal += allCombinations.size();

    for (WorkflowStatusCombination c : allCombinations) {
      if (handler.isWorkflowCombinationInTrackingRecordStates(c)) {
        nStatesFound++;
      }
    }

    // finally assert whether the correct number of legal states was found
    assertTrue("Correct number of states found (" + nStatesTotal + " checked)",
        nStatesFound + (handler.isEmptyWorkflowAllowed() ? 1 : 0) == handler
            .getTrackingRecordStateToActionMap().size());

  }

  @Test
  public void testSpecialistIncompleteStates() throws Exception {

    Logger.getLogger(WorkflowQaPathHandlerTest.class).info(
        "Testing specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED, WorkflowStatus.QA_NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));


    // test all paired workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {

        // reset the managers
        resetRecords();

        // set up the records
        MapRecord record = new MapRecordJpa();
        record.setConceptId("1");
        record.setConceptName("concept1");
        record.setCountDescendantConcepts(0L);
        record.setLastModified(Calendar.getInstance().getTimeInMillis());
        record.setWorkflowStatus(status1);
        record.setOwner(specialist);
        record.setLastModifiedBy(specialist);
        mappingService.addMapRecord(record);

        MapRecord record2 = new MapRecordJpa();
        record2.setConceptId("1");
        record2.setConceptName("concept1");
        record2.setCountDescendantConcepts(0L);
        record2.setLastModified(Calendar.getInstance().getTimeInMillis());
        record2.setWorkflowStatus(status2);
        record2.setOwner(specialist2);
        record2.setLastModifiedBy(specialist2);
        mappingService.addMapRecord(record2);

        // compute workflow
        computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

        // if this tracking record's computed combination is in the legal
        // combination set
        // proceed with action testing
        if (legalCombinations.contains(handler
            .getWorkflowCombinationForTrackingRecord(trackingRecord))) {

          legalCombinationsFound.add(handler
              .getWorkflowCombinationForTrackingRecord(trackingRecord));

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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }
  
  @Test
  public void testSpecialisCompleteStates() throws Exception {

    Logger.getLogger(WorkflowQaPathHandlerTest.class).info(
        "Testing specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));
 

    // test all paired workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {

        // reset the managers
        resetRecords();

        // set up the records
        MapRecord record = new MapRecordJpa();
        record.setConceptId("1");
        record.setConceptName("concept1");
        record.setCountDescendantConcepts(0L);
        record.setLastModified(Calendar.getInstance().getTimeInMillis());
        record.setWorkflowStatus(status1);
        record.setOwner(specialist);
        record.setLastModifiedBy(specialist);
        mappingService.addMapRecord(record);

        MapRecord record2 = new MapRecordJpa();
        record2.setConceptId("1");
        record2.setConceptName("concept1");
        record2.setCountDescendantConcepts(0L);
        record2.setLastModified(Calendar.getInstance().getTimeInMillis());
        record2.setWorkflowStatus(status2);
        record2.setOwner(specialist2);
        record2.setLastModifiedBy(specialist2);
        mappingService.addMapRecord(record2);

        // compute workflow
        computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

        // if this tracking record's computed combination is in the legal
        // combination set
        // proceed with action testing
        if (legalCombinations.contains(handler
            .getWorkflowCombinationForTrackingRecord(trackingRecord))) {

          legalCombinationsFound.add(handler
              .getWorkflowCombinationForTrackingRecord(trackingRecord));

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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @Test
  public void testLeadIncompleteStates() throws Exception {

    Logger.getLogger(WorkflowQaPathHandlerTest.class).info(
        "Testing second specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    
    // test all paired workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {

        // reset the managers
        resetRecords();

        // set up the records
        MapRecord record = new MapRecordJpa();
        record.setConceptId("1");
        record.setConceptName("concept1");
        record.setCountDescendantConcepts(0L);
        record.setLastModified(Calendar.getInstance().getTimeInMillis());
        record.setWorkflowStatus(status1);
        record.setOwner(specialist);
        record.setLastModifiedBy(specialist);
        mappingService.addMapRecord(record);

        MapRecord record2 = new MapRecordJpa();
        record2.setConceptId("1");
        record2.setConceptName("concept1");
        record2.setCountDescendantConcepts(0L);
        record2.setLastModified(Calendar.getInstance().getTimeInMillis());
        record2.setWorkflowStatus(status2);
        record2.setOwner(specialist2);
        record2.setLastModifiedBy(specialist2);
        mappingService.addMapRecord(record2);

        // compute workflow
        computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

        // if this tracking record's computed combination is in the legal
        // combination set
        // proceed with action testing
        if (legalCombinations.contains(handler
            .getWorkflowCombinationForTrackingRecord(trackingRecord))) {

          legalCombinationsFound.add(handler
              .getWorkflowCombinationForTrackingRecord(trackingRecord));

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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @Test
  public void testLeadCompleteStates() throws Exception {

    Logger.getLogger(WorkflowQaPathHandlerTest.class).info(
        "Testing second specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
    
    // test all paired workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {

        // reset the managers
        resetRecords();

        // set up the records
        MapRecord record = new MapRecordJpa();
        record.setConceptId("1");
        record.setConceptName("concept1");
        record.setCountDescendantConcepts(0L);
        record.setLastModified(Calendar.getInstance().getTimeInMillis());
        record.setWorkflowStatus(status1);
        record.setOwner(specialist);
        record.setLastModifiedBy(specialist);
        mappingService.addMapRecord(record);

        MapRecord record2 = new MapRecordJpa();
        record2.setConceptId("1");
        record2.setConceptName("concept1");
        record2.setCountDescendantConcepts(0L);
        record2.setLastModified(Calendar.getInstance().getTimeInMillis());
        record2.setWorkflowStatus(status2);
        record2.setOwner(specialist2);
        record2.setLastModifiedBy(specialist2);
        mappingService.addMapRecord(record2);

        // compute workflow
        computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

        // if this tracking record's computed combination is in the legal
        // combination set
        // proceed with action testing
        if (legalCombinations.contains(handler
            .getWorkflowCombinationForTrackingRecord(trackingRecord))) {

          legalCombinationsFound.add(handler
              .getWorkflowCombinationForTrackingRecord(trackingRecord));

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
                assertTrue(result.getMessages().contains(action.toString()));
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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @AfterClass
  public static void cleanup() throws Exception {

    Logger.getLogger(WorkflowQaPathHandlerTest.class).info("Clean-up");
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

    specialist2 = new MapUserJpa();
    specialist2.setUserName("spec2");
    specialist2.setApplicationRole(MapUserRole.SPECIALIST);
    specialist2.setName("Specialist 2");
    specialist2.setEmail("email");

    lead = new MapUserJpa();
    lead.setUserName("lead");
    lead.setName("Lead");
    lead.setApplicationRole(MapUserRole.LEAD);
    lead.setEmail("email");

    mappingService.addMapUser(specialist);
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
