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
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowNonLegacyPathHandler;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowNonLegacyPathHandlerTest {

  private static WorkflowNonLegacyPathHandler handler;

  private static MappingService mappingService;

  private static WorkflowService workflowService;

  private static TrackingRecord trackingRecord;

  private static MapUser specialist, specialist2;

  private static MapUser lead;

  @BeforeClass
  public static void init() throws Exception {

    // instantiate handler
    handler = new WorkflowNonLegacyPathHandler();

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
  public void testInitialState() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
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
  public void testFirstSpecialistStates() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
        "Testing specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED)));

    // test all single workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

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

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @Test
  public void testSecondSpecialistStates() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
        "Testing second specialist new/editing states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.NEW, WorkflowStatus.NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.NEW, WorkflowStatus.EDITING_DONE)));
    legalCombinations
        .add(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.EDITING_IN_PROGRESS,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_DONE)));

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
  public void testConflictDetectedState() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
        "Testing conflict-detected state...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED)));

    // test all single workflow states
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
  public void testLeadIncompleteState() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
        "Testing lead incomplete states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_IN_PROGRESS)));

    // test all single workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        for (WorkflowStatus status3 : WorkflowStatus.values()) {
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

          MapRecord record3 = new MapRecordJpa();
          record3.setConceptId("1");
          record3.setConceptName("concept1");
          record3.setCountDescendantConcepts(0L);
          record3.setLastModified(Calendar.getInstance().getTimeInMillis());
          record3.setWorkflowStatus(status3);
          record3.setOwner(lead);
          record3.setLastModifiedBy(lead);
          mappingService.addMapRecord(record3);

          // compute workflow
          computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2,
              record3)));

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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @Test
  public void testLeadCompleteState() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info(
        "Testing lead incomplete states...");

    Set<WorkflowStatusCombination> legalCombinations = new HashSet<>();
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_NEW)));
    legalCombinations.add(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_IN_PROGRESS)));

    // test all single workflow states
    Set<WorkflowStatusCombination> legalCombinationsFound = new HashSet<>();

    for (WorkflowStatus status1 : WorkflowStatus.values()) {
      for (WorkflowStatus status2 : WorkflowStatus.values()) {
        for (WorkflowStatus status3 : WorkflowStatus.values()) {
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

          MapRecord record3 = new MapRecordJpa();
          record3.setConceptId("1");
          record3.setConceptName("concept1");
          record3.setCountDescendantConcepts(0L);
          record3.setLastModified(Calendar.getInstance().getTimeInMillis());
          record3.setWorkflowStatus(status3);
          record3.setOwner(lead);
          record3.setLastModifiedBy(lead);
          mappingService.addMapRecord(record3);

          // compute workflow
          computeWorkflow(new HashSet<MapRecord>(Arrays.asList(record, record2,
              record3)));

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
    }

    assertTrue("Checking legal status combinations evaluated",
        legalCombinations.size() == legalCombinationsFound.size());

  }

  @AfterClass
  public static void cleanup() throws Exception {

    Logger.getLogger(WorkflowNonLegacyPathHandlerTest.class).info("Clean-up");
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
