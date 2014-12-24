package org.ihtsdo.otf.mapping.jpa.services;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
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

  private static TrackingRecord trackingRecord;

  private static MapUser specialist, specialist2;

  private static MapUser lead;

  // maximum number of combinations to check each state with
  // added to 'sample' illegal states without running them all
  // which takes an extremely long amount of time for 3-record sets
  private static int nRandomCombinations;

  @BeforeClass
  public static void init() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Initializing...");

    nRandomCombinations = 30;

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

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing all possible combinations against legal states...");

    WorkflowStatusCombination combination;
    Set<WorkflowStatusCombination> allCombinations = new HashSet<>();
    Set<WorkflowStatusCombination> combinationsFound = new HashSet<>();

    // test empty state
    combination = new WorkflowStatusCombination();
    if (handler.isEmptyWorkflowAllowed()) {
      assertTrue("Empty workflow permitted",
          handler.isWorkflowCombinationInTrackingRecordStates(combination));
    } else {
      assertTrue("Empty workflow not permitted",
          !handler.isWorkflowCombinationInTrackingRecordStates(combination));
    }

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "  Empty state tested.");

    // test all one-record combination states
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      combination = new WorkflowStatusCombination(Arrays.asList(status1));
      allCombinations.add(combination);

      if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
        combinationsFound.add(combination);
      }
    }

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "  Single-record states tested.");

    // test all two-record combination states
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        combination =
            new WorkflowStatusCombination(Arrays.asList(status1, status2));

        allCombinations.add(combination);

        if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
          combinationsFound.add(combination);
        }
      }
    }

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "  Double-record states tested.");

    // test all three-record combination states
    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        for (WorkflowStatus status3 : WorkflowStatus.values()) {
          combination =
              new WorkflowStatusCombination(Arrays.asList(status1, status2,
                  status3));

          allCombinations.add(combination);

          if (handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
            combinationsFound.add(combination);
          }
        }
      }
    }

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "  Triple-record states tested.");

    // finally assert whether the correct number of legal states was found
    assertTrue("Correct number of states found (" + allCombinations.size()
        + " checked, " + combinationsFound.size() + " valid states found, "
        + handler.getWorkflowStatusCombinations().size() + " expected)",
        combinationsFound.size() == handler.getWorkflowStatusCombinations()
            .size());

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
  public void testSpecialistIncompleteStates() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));

    // get the state corresponding to the first legal workflow status
    // combination
    WorkflowPathState state =
        handler
            .getWorkflowPathStateForWorkflowStatusCombination(legalCombinations
                .iterator().next());

    // test all single workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status : WorkflowStatus.values()) {

      Logger.getLogger("  Testing " + status);

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
        // assert true that
        // workflow path state does not contain this combination OR
        assertTrue("State does not contain this combination",
            !state.contains(handler
                .getWorkflowCombinationForTrackingRecord(trackingRecord)));

      }
    }

    assertTrue("Checking legal status combinations evaluated ("
        + legalCombinations.size() + ")",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @Test
  public void testSpecialistCompleteStates() throws Exception {

    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing specialist editing complete state...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED)));

    // get the state corresponding to the first legal workflow status
    // combination
    WorkflowPathState state =
        handler
            .getWorkflowPathStateForWorkflowStatusCombination(legalCombinations
                .iterator().next());

    // test all single workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status : WorkflowStatus.values()) {

      Logger.getLogger("  Testing " + status);

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
        // assert true that
        // workflow path state does not contain this combination OR
        assertTrue("State does not contain this combination",
            !state.contains(handler
                .getWorkflowCombinationForTrackingRecord(trackingRecord)));

      }
    }

    assertTrue("Checking legal status combinations evaluated ("
        + legalCombinations.size() + ")",
        legalCombinations.size() == legalCombinationsFound.size());

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

    // get the state corresponding to the first legal workflow status
    // combination
    WorkflowPathState state =
        handler
            .getWorkflowPathStateForWorkflowStatusCombination(legalCombinations
                .iterator().next());

    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();
    for (WorkflowStatusCombination combination : this.generateTestCombinations(
        2, nRandomCombinations, legalCombinations)) {

      // extract the workflow statuses for this combination
      List<WorkflowStatus> statuses = combination.getWorkflowStatusesAsList();

      // reset the managers
      resetRecords();

      // set up the records
      MapRecord record = new MapRecordJpa();
      record.setConceptId("1");
      record.setConceptName("concept1");
      record.setCountDescendantConcepts(0L);
      record.setLastModified(Calendar.getInstance().getTimeInMillis());
      record.setWorkflowStatus(statuses.get(0));
      record.setOwner(specialist);
      record.setLastModifiedBy(specialist);
      mappingService.addMapRecord(record);

      MapRecord record2 = new MapRecordJpa();
      record2.setConceptId("1");
      record2.setConceptName("concept1");
      record2.setCountDescendantConcepts(0L);
      record2.setLastModified(Calendar.getInstance().getTimeInMillis());
      record2.setWorkflowStatus(statuses.get(1));
      record2.setOwner(lead);
      record2.setLastModifiedBy(lead);
      mappingService.addMapRecord(record2);

      // compute workflow
      computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

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
      } else {
        // assert true that
        // workflow path state does not contain this combination OR
        assertTrue("State does not contain this combination",
            !state.contains(handler
                .getWorkflowCombinationForTrackingRecord(trackingRecord)));

      }
    }

    assertTrue("Checking legal status combinations evaluated ("
        + legalCombinations.size() + ")",
        legalCombinations.size() == legalCombinationsFound.size());
  }

  @Test
  public void testLeadCompleteState() throws Exception {
    Logger.getLogger(WorkflowReviewProjectPathHandlerTest.class).info(
        "Testing lead complete state...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));

    // get the state corresponding to the first legal workflow status
    // combination
    WorkflowPathState state =
        handler
            .getWorkflowPathStateForWorkflowStatusCombination(legalCombinations
                .iterator().next());

    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();
    for (WorkflowStatusCombination combination : this.generateTestCombinations(
        2, nRandomCombinations, legalCombinations)) {

      // extract the workflow statuses for this combination
      List<WorkflowStatus> statuses = combination.getWorkflowStatusesAsList();
      // reset the managers
      resetRecords();

      // set up the records
      MapRecord record = new MapRecordJpa();
      record.setConceptId("1");
      record.setConceptName("concept1");
      record.setCountDescendantConcepts(0L);
      record.setLastModified(Calendar.getInstance().getTimeInMillis());
      record.setWorkflowStatus(statuses.get(0));
      record.setOwner(specialist);
      record.setLastModifiedBy(specialist);
      mappingService.addMapRecord(record);

      MapRecord record2 = new MapRecordJpa();
      record2.setConceptId("1");
      record2.setConceptName("concept1");
      record2.setCountDescendantConcepts(0L);
      record2.setLastModified(Calendar.getInstance().getTimeInMillis());
      record2.setWorkflowStatus(statuses.get(1));
      record2.setOwner(lead);
      record2.setLastModifiedBy(lead);
      mappingService.addMapRecord(record2);

      // compute workflow
      computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2)));

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
      } else {
        // assert true that
        // workflow path state does not contain this combination OR
        assertTrue("State does not contain this combination",
            !state.contains(handler
                .getWorkflowCombinationForTrackingRecord(trackingRecord)));

      }

    }
    assertTrue("Checking legal status combinations evaluated ("
        + legalCombinations.size() + ")",
        legalCombinations.size() == legalCombinationsFound.size());
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
    mappingService.addMapUser(specialist2);
    mappingService.addMapUser(lead);
  }

  private static void removeUsers() throws Exception {
    // remove map users
    for (MapUser user : mappingService.getMapUsers().getMapUsers())
      mappingService.removeMapUser(user.getId());
  }

  private void computeWorkflow(Set<MapRecord> mapRecords) throws Exception {
    trackingRecord = new TrackingRecordJpa();
    trackingRecord.setWorkflowPath(handler.getWorkflowPath());

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

  /**
   * Helper function to generate a set of test states Creates nResults
   * combinations, which include the passed combinations
   * @param nRecords
   * @param legalStates
   * @return
   */
  private Set<WorkflowStatusCombination> generateTestCombinations(int nRecords,
    int nResults, Set<WorkflowStatusCombination> legalCombinations) {
    Set<WorkflowStatusCombination> combinations =
        new HashSet<>(legalCombinations);

    // extract the values
    WorkflowStatus[] statuses = WorkflowStatus.values();

    // calculate the maximum random combinations that can be generated from
    // these values
    int maxResults = statuses.length ^ (nRecords - 1);

    System.out.println(nRecords + " " + maxResults);

    while (combinations.size() < nResults && combinations.size() < maxResults) {
      WorkflowStatusCombination combination = new WorkflowStatusCombination();
      for (int i = 0; i < nRecords; i++) {

        Random random = new Random();
        combination
            .addWorkflowStatus(statuses[random.nextInt(statuses.length)]);
      }
    }

    return combinations;

  }
}
