/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Workflow path handler for "conflict and Review".
 */

// Originally made for the SNOMED <-> MedDRA projects
// 2 specialist will map, and two leads will review.
// If the 2 specialists' maps agree, then both leads will review.
// If the 2 specialists generate a conflict, then one lead will
// conflict-resolve, and the second lead will review.

public class WorkflowConflictAndReviewPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the Conflict and Review Path
  /** The lead finished state. */
  private static WorkflowPathState initialState;

  /** The first specialist editing state. */
  private static WorkflowPathState firstSpecialistEditingState;

  /** The second specialist editing state. */
  private static WorkflowPathState secondSpecialistEditingState;

  /** The conflict detected state. */
  private static WorkflowPathState conflictDetectedState;

  /** The first lead conflict editing state. */
  private static WorkflowPathState firstLeadConflictEditingState;

  /** The first lead conflict finished state. */
  private static WorkflowPathState firstLeadConflictFinishedState;

  /** The no conflict detected state. */
  private static WorkflowPathState noConflictDetectedState;  
  
  /** The first lead review editing state. */
  private static WorkflowPathState firstLeadReviewEditingState;

  /** The first lead review finished state. */
  private static WorkflowPathState firstLeadReviewFinishedState;

  /** The second lead editing state. */
  private static WorkflowPathState secondLeadReviewEditingState;

  /** The second lead finished state. */
  private static WorkflowPathState secondLeadReviewFinishedState;

  /**
   * Instantiates an empty {@link WorkflowConflictAndReviewPathHandler}.
   */
  public WorkflowConflictAndReviewPathHandler() {

    setWorkflowPath(WorkflowPath.CONFLICT_AND_REVIEW_PATH);

    setEmptyWorkflowAllowed(true);

    // STATE: Initial initialState has empty tracking record
    initialState = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: One specialist has claimed work
    firstSpecialistEditingState = new WorkflowPathState("First Specialist Work");
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW)));
    firstSpecialistEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    firstSpecialistEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(firstSpecialistEditingState,
        new HashSet<>(
            Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH, WorkflowAction.FINISH_EDITING,
                WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Two specialists have claimed work
    secondSpecialistEditingState = new WorkflowPathState("Second Specialist Work");
    secondSpecialistEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.NEW)));
    secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)));
    secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_DONE)));
    secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_IN_PROGRESS)));
    secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(secondSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Conflict detected
    conflictDetectedState = new WorkflowPathState("Conflict Detected");
    conflictDetectedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(conflictDetectedState,
        new HashSet<>(
            Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: First Lead conflict resolution (incomplete)
    firstLeadConflictEditingState =
        new WorkflowPathState("First Lead Conflict Resolution Incomplete");
    firstLeadConflictEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_NEW)));
    firstLeadConflictEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(firstLeadConflictEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: First Lead conflict resolution (complete)
    firstLeadConflictFinishedState =
        new WorkflowPathState("First Lead Conflict Resolution Complete");
    firstLeadConflictFinishedState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED)));
    trackingRecordStateToActionMap.put(firstLeadConflictFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: No Conflict Detected
    noConflictDetectedState = new WorkflowPathState("No Conflict Detected");
    noConflictDetectedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(noConflictDetectedState,
        new HashSet<>(
            Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));
    
    
    // STATE: First Lead review (incomplete)
    firstLeadReviewEditingState = new WorkflowPathState("First Lead Review Incomplete");
    firstLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    firstLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(firstLeadReviewEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: First Lead review (complete)
    firstLeadReviewFinishedState = new WorkflowPathState("First Lead Review Complete");
    firstLeadReviewFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(firstLeadReviewFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Second Lead review (incomplete)
    secondLeadReviewEditingState = new WorkflowPathState("Second Lead Review Incomplete");
    secondLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED, WorkflowStatus.REVIEW_NEW)));
    secondLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    secondLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED, WorkflowStatus.REVIEW_NEW)));
    secondLeadReviewEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(secondLeadReviewEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Second Lead review (complete)
    secondLeadReviewFinishedState = new WorkflowPathState("Second Lead Review Complete");
    secondLeadReviewFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED, WorkflowStatus.REVIEW_RESOLVED)));
    secondLeadReviewFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED, WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(secondLeadReviewFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.PUBLISH,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
  }

  /* see superclass */
  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(TrackingRecord tr,
    WorkflowAction action, MapUser user) throws Exception {

    // throw exception if action or user are undefined
    if (action == null)
      throw new Exception("Action cannot be null.");

    if (user == null)
      throw new Exception("User cannot be null.");

    // first, validate the tracking record itself
    ValidationResult result = validateTrackingRecord(tr);
    if (!result.isValid()) {
      result.addError("Could not validate action for user due to workflow errors.");
      return result;
    }

    // second, check for CANCEL action -- always valid for this path for any
    // state or user (no-op)
    if (action.equals(WorkflowAction.CANCEL)) {
      return result;
    }

    // third, get the user role for this map project
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole userRole =
        mappingService.getMapUserRoleForMapProject(user.getUserName(), tr.getMapProjectId());
    mappingService.close();

    // fourth, get the map records and workflow path state from the tracking
    // record
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
    MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
    WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////

    if (state == null) {
      result.addError("Could not determine workflow path state for tracking record");
    }

    // for CREATE_QA_RECORD, only a label is assigned, check role only
    else if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {

      // for creating qa record, only check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }
    }
    // INITIAL STATE: No specialists have started editing
    // Record requirement : None
    // Permissible actions: ASSIGN_FROM_SCRATCH
    // Minimum role : Specialist
    else if (state.equals(initialState)) {

      // check record
      if (currentRecord != null) {
        result.addError("User's record does not meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action is not permitted");
      }
    }

    // STATE: One specialist has started editing
    // Record requirement : NEW, EDITING_IN_PROGRESS, EDITING_DONE
    // Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSIGN
    // Minimum role : Specialist
    else if (state.equals(firstSpecialistEditingState)) {

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }

      // check record
      if (currentRecord == null) {

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted.");
        }

        // if a record is already owned by user
      } else {

        // check record status
        if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
            && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS)
            && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)) {

          result.addError("User's record does not meet requirements");
        }

        // check action
        if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
            && !action.equals(WorkflowAction.FINISH_EDITING)
            && !action.equals(WorkflowAction.UNASSIGN)) {
          result.addError("Action is not permitted.");
        }

      }

      // STATE: Second specialist has begun editing, but both specialists
      // are
      // not finished
      // Record requirement : NEW, EDITING_IN_PROGRESS, EDITING_DONE
      // Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSIGN
      // Minimum role : Specialist

    } else if (state.equals(secondSpecialistEditingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
          && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS)
          && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)) {
        result.addError("User's record does not meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }

      // check action
      if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
          && !action.equals(WorkflowAction.FINISH_EDITING)
          && !action.equals(WorkflowAction.UNASSIGN)) {
        result.addError("Action is not permitted.");
      }

      // STATE: Conflict detected after both specialists finished
      // Record requirement : No record
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role : Lead

    } else if (state.equals(conflictDetectedState)) {

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not have required role");
      }

      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action is not permitted");
      }

      // STATE: First Lead conflict resolution
      // Record requirement : CONFLICT_NEW, CONFLICT_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
      // Minimum role : Lead
    } else if (state.equals(firstLeadConflictEditingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)) {
        result.addError("User's record does meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not have required role");
      }

      // check action
      if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
          && !action.equals(WorkflowAction.FINISH_EDITING)
          && !action.equals(WorkflowAction.UNASSIGN)) {
        result.addError("Action is not permitted.");
      }

      // STATE: First Lead conflict resolution complete
      // Record requirement : CONFLICT_RESOLVED
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
      // Minimum role : Lead
    } else if (state.equals(firstLeadConflictFinishedState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {
        result.addError("User's record does meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not have required role");
      }

      // check action
      if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
          && !action.equals(WorkflowAction.FINISH_EDITING)
          && !action.equals(WorkflowAction.UNASSIGN)) {
        result.addError("Action is not permitted.");
      }
      
      // STATE: No conflict detected after both specialists finished
      // Record requirement : No record
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role : Lead

    } else if (state.equals(noConflictDetectedState)) {

    // check role
    if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
      result.addError("User does not have required role");
    }

    // check action
    if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
      result.addError("Action is not permitted");
    }

    // STATE: First Lead review
    // Record requirement : REVIEW_NEW, REVIEW_IN_PROGRESS
    // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    // Minimum role : Lead
  } else if (state.equals(firstLeadReviewEditingState)) {

    // check record
    if (currentRecord == null) {
      result.addError("User must have a record");
    } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
        && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_IN_PROGRESS)) {
      result.addError("User's record does meet requirements");
    }

    // check role
    if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
      result.addError("User does not have required role");
    }

    // check action
    if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
        && !action.equals(WorkflowAction.FINISH_EDITING)
        && !action.equals(WorkflowAction.UNASSIGN)) {
      result.addError("Action is not permitted.");
    }

    // STATE: First Lead review complete
    // Record requirement : REVIEW_RESOLVED
    // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    // Minimum role : Lead
  } else if (state.equals(firstLeadReviewFinishedState)) {

    // check record
    if (currentRecord == null) {
      result.addError("User must have a record");
    } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)) {
      result.addError("User's record does meet requirements");
    }

    // check role
    if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
      result.addError("User does not have required role");
    }

    // check action
    if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
        && !action.equals(WorkflowAction.FINISH_EDITING)
        && !action.equals(WorkflowAction.UNASSIGN)) {
      result.addError("Action is not permitted.");
    }
    
    // STATE: Second Lead review
    // Record requirement : REVIEW_NEW, REVIEW_IN_PROGRESS
    // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    // Minimum role : Lead
  } else if (state.equals(secondLeadReviewEditingState)) {

    // check record
    if (currentRecord == null) {
      result.addError("User must have a record");
    } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
        && !currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_IN_PROGRESS)) {
      result.addError("User's record does meet requirements");
    }

    // check role
    if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
      result.addError("User does not have required role");
    }

    // check action
    if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
        && !action.equals(WorkflowAction.FINISH_EDITING)
        && !action.equals(WorkflowAction.UNASSIGN)) {
      result.addError("Action is not permitted.");
    }

    // STATE: Second Lead review complete
    // Record requirement : REVIEW_RESOLVED
    // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    // Minimum role : Lead
  } else if (state.equals(secondLeadReviewFinishedState)) {

    // check record
    if (currentRecord == null) {
      result.addError("User must have a record");
    } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)) {
      result.addError("User's record does meet requirements");
    }

    // check role
    if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
      result.addError("User does not have required role");
    }

    // check action
    if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
        && !action.equals(WorkflowAction.FINISH_EDITING)
        && !action.equals(WorkflowAction.UNASSIGN)
        && !action.equals(WorkflowAction.PUBLISH)) {
      result.addError("Action is not permitted.");
    }

  }      


    else {
      result.addError("Invalid state/could not determine state");
    }

    if (result.getErrors().size() != 0) {
      result.addError("Error occured in workflow state " + state.getWorkflowStateName());
    }

    return result;
  }

  /* see superclass */
  @Override
  public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord,
    WorkflowAction workflowAction, MapProject mapProject, MapUser mapUser,
    Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {

    Logger.getLogger(this.getClass()).debug(getName() + ": Processing workflow action by "
        + mapUser.getName() + ":  " + workflowAction.toString());

    // the set of records returned after processing
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    switch (workflowAction) {

      /** The assign from scratch. */
      case ASSIGN_FROM_SCRATCH:

        // create a new record for this tracking record and user
        MapRecord newRecord = createMapRecordForTrackingRecordAndUser(trackingRecord, mapUser);

        // if a "new" tracking record (i.e. prior to conflict detection),
        // add a NEW record
        if (getWorkflowStatusFromMapRecords(mapRecords)
            .compareTo(WorkflowStatus.CONFLICT_DETECTED) < 0) {

          // check that this record is valid to be assigned (i.e. no more
          // than one other specialist assigned)
          if (mapRecords.size() >= 2) {
            throw new Exception(
                "WorkflowConflictAndReviewPathHandlerException - assignFromScratch:  Two users already assigned");
          }

          newRecord.setWorkflowStatus(WorkflowStatus.NEW);

          // Compute identify
          ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
              .forName(mapProject.getProjectSpecificAlgorithmHandlerClass())
              .getDeclaredConstructor().newInstance();
          handler.setMapProject(mapProject);
          newRecord.setLastModifiedBy(mapUser);
          handler.computeIdentifyAlgorithms(newRecord);

        }
        // if this is a tracking record with conflict
        // detected, add a CONFLICT_NEW record
        else if (getWorkflowStatusFromMapRecords(mapRecords)
            .equals(WorkflowStatus.CONFLICT_DETECTED)) {

          newRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_NEW);

          ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
              .forName(mapProject.getProjectSpecificAlgorithmHandlerClass())
              .getDeclaredConstructor().newInstance();
          handler.setMapProject(mapProject);

          MapRecord mapRecord1 = (MapRecord) mapRecords.toArray()[0];
          MapRecord mapRecord2 = (MapRecord) mapRecords.toArray()[1];
          ValidationResult validationResult = handler.compareMapRecords(mapRecord1, mapRecord2);
          newRecord.setReasonsForConflict(validationResult.getConciseErrors());

          // get the origin ids from the tracking record
          for (final MapRecord mr : newRecords) {
            newRecord.addOrigin(mr.getId());
          }

        }
        // otherwise, if this is a tracking record with REVIEW_NEEDED,
        // add a new REVIEW_NEW record
        else if (getWorkflowStatusFromMapRecords(mapRecords).equals(WorkflowStatus.REVIEW_NEEDED)) {

          newRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);

          // get the origin ids from the tracking record
          for (final MapRecord mr : newRecords) {
            newRecord.addOrigin(mr.getId());
          }

        } else {
          throw new Exception("ASSIGN_FROM_SCRATCH on CONFLICT_AND_REVIEW_PATH failed.");
        }

        newRecords.add(newRecord);
        break;

      /** The assign from initial record. */
      case ASSIGN_FROM_INITIAL_RECORD:
        throw new Exception("ASSIGN_FROM_INITIAL_RECORD on CONFLICT_AND_REVIEW_PATH not permitted");

      /** The unassign. */
      case UNASSIGN:

        MapRecord assignedRecord = getCurrentMapRecordForUser(newRecords, mapUser);

        if (assignedRecord == null)
          throw new Exception(
              "unassign called for concept that does not have specified user assigned");

        // remove this record from the tracking record
        newRecords.remove(assignedRecord);

        if (assignedRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {

          for (final MapRecord mr : newRecords) {

            // if another specialist's record, revert to EDITING_DONE
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
              mr.setWorkflowStatus(WorkflowStatus.EDITING_DONE);
            }
          }
        }

        break;

      /** The save for later. */
      case SAVE_FOR_LATER:
        MapRecord recordToSave = this.getCurrentMapRecordForUser(newRecords, mapUser);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (recordToSave.getWorkflowStatus().equals(WorkflowStatus.NEW))
          recordToSave.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (recordToSave.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW))
          recordToSave.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
        if (recordToSave.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED))
          recordToSave.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED))
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_IN_PROGRESS);

        break;

      /** The finish editing. */
      case FINISH_EDITING:
        // case 1: A specialist is finished with a record
        if (getWorkflowStatusFromMapRecords(mapRecords)
            .compareTo(WorkflowStatus.CONFLICT_DETECTED) <= 0) {
          // set this record to EDITING_DONE
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);

          // check if two specialists have completed work (lowest workflow
          // status is EDITING_DONE, highest workflow status is
          // CONFLICT_DETECTED)
          if (getLowestWorkflowStatusFromMapRecords(mapRecords)
              .compareTo(WorkflowStatus.EDITING_DONE) >= 0 && mapRecords.size() == 2) {

            final Iterator<MapRecord> recordIter = mapRecords.iterator();
            final MapRecord mapRecord1 = recordIter.next();
            final MapRecord mapRecord2 = recordIter.next();

            ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
                .forName(mapProject.getProjectSpecificAlgorithmHandlerClass())
                .getDeclaredConstructor().newInstance();
            handler.setMapProject(mapProject);

            final ValidationResult validationResult =
                handler.compareMapRecords(mapRecord1, mapRecord2);

            // if map records validation is successful, map needs dual review
            if (validationResult.isValid()) {

              Logger.getLogger(getClass())
                  .debug("CONFLICT_AND_REVIEW_PATH - No conflicts detected.");

              // conflict not detected, change workflow status of all
              // records and update records
              for (final MapRecord mr : newRecords) {
                if (mr.getWorkflowStatus().compareTo(WorkflowStatus.REVIEW_NEEDED) <= 0)
                  mr.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);
              }

            } else {

              Logger.getLogger(getClass()).debug("CONFLICT_AND_REVIEW_PATH - Conflicts detected");

              // conflict detected, needs conflict resolution before single
              // review
              // change workflow status of all
              // records (if not a lead's existing conflict record)
              // and update records
              for (final MapRecord mr : newRecords) {
                if (mr.getWorkflowStatus().compareTo(WorkflowStatus.CONFLICT_DETECTED) <= 0)
                  mr.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
              }
            }
            // otherwise, only one specialist has finished work, do
            // nothing else
          } else {
            Logger.getLogger(getClass())
                .debug("CONFLICT_AND_REVIEW_PATH - Only this specialist has completed work");
          }

          // case 2: A lead is finished with a conflict resolution
          // Determined by workflow status of:
          // CONFLICT_NEW (i.e. conflict was resolved immediately)
          // CONFLICT_IN_PROGRESS (i.e. conflict had been previously saved
          // for later)
          // CONFLICT_RESOLVED (i.e. conflict marked resolved, but lead
          // revisited)
        } else if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {

          Logger.getLogger(getClass())
              .debug("CONFLICT_AND_REVIEW_PATH - Conflict resolution detected");

          // once the conflict is resolved, change workflow status of just the
          // lead's record
          mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_RESOLVED);

          // case 3: A lead is finished with a QA review
          // Determined by workflow status of:
          // REVIEW_NEW (i.e. conflict was resolved immediately)
          // REVIEW_IN_PROGRESS (i.e. conflict had been previously saved
          // for later)
          // REVIEW_RESOLVED (i.e. conflict marked resolved, but lead
          // revisited)
        } else if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_IN_PROGRESS)
            || mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)) {

          Logger.getLogger(getClass())
              .debug("CONFLICT_AND_REVIEW_PATH - REVIEW resolution detected");

          // If this is the first lead review (i.e. no conflict was detected), or second lead review,
          // then set lead's record to REVIEW_NEEDED
          if (mapRecords.size() == 3 || mapRecords.size() == 4) {
            mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_RESOLVED);
          } else {
            throw new Exception(
                "WorkflowConflictAndReviewPathHandlerException - lead finishing editing:  unexpected number of associated map records: "
                    + mapRecords.size());
          }
        }

        else {
          throw new Exception(
              "finishEditing failed! Invalid workflow status combination on record(s)");
        }
        break;

      /** The publish */
      case PUBLISH:

        Logger.getLogger(getClass())
            .debug("CONFLICT_AND_REVIEW_PATH - Publishing fully reviewed map");

        // Requirements for CONFLICT_AND_REVIEW_PATH publish action:
        // 4 records, with one marked REVIEW_RESOLVED

        if (mapRecords.size() == 4) {

          // Check assumption: owned record is REVIEW_RESOLVED
          if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)) {
            throw new Exception(
                "Publish called on CONFLICT_AND_REVIEW_PATH for map record not marked as REVIEW_RESOLVED");
          }

          // Check assumption: two CONFLICT_DETECTED records, one
          // CONFLICT_RESOLVED record, and one REVIEW_RESOLVED record
          // OR 
          // two REVIEW_NEEDED records, and two REVIEW_RESOLVED records
          int nConflictNeededRecords = 0;
          int nConflictResolvedRecords = 0;
          int nReviewNeededRecords = 0;
          int nReviewResolvedRecords = 0;
          for (final MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED))
              nConflictNeededRecords++;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED))
              nConflictResolvedRecords++;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
              nReviewNeededRecords++;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED))
              nReviewResolvedRecords++;
          }

          if (!((nConflictNeededRecords == 2 && nConflictResolvedRecords == 1 && nReviewResolvedRecords == 1) || (nReviewNeededRecords == 2 && nReviewResolvedRecords == 2))) {
            throw new Exception("Bad workflow state for concept " + mapRecord.getConceptId()
                + ":  PUBLISH requires two CONFLICT_DETECTED, one CONFLICT_RESOLVED, and one REVIEW_RESOLVED records, OR two REVIEW_NEEDED and two REVIEW_RESOLVED records.\n\n"
                + "Concept has: " + nConflictNeededRecords + " CONFLICT_DETECTED, " + nConflictResolvedRecords + " CONFLICT_RESOLVED, " + nReviewNeededRecords
                + " REVIEW_NEEDED, and " + nReviewResolvedRecords
                + " REVIEW_RESOLVED records.");
          }

          // cycle over the previously existing records
          MapRecord reviewResolvedToKeep = null;
          MapRecord reviewResolvedToRemove = null;
          for (final MapRecord mr : mapRecords) {

            // remove the CONFLICT_DETECTED, CONFLICT_RESOLED, and REVIEW_NEEDED records from the
            // revised set
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)
                || mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)
                || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
              newRecords.remove(mr);

              // Identify the REVIEW_RESOLVED record to keep and remove
              // The REVIEW_RESOLVED record with the higher id number will be kept
              // to mark READY_FOR_PUBLICATION and update
            } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)){
              if(reviewResolvedToKeep == null) {
                reviewResolvedToKeep = mr;
              }
              else {
                if(mr.getId() > reviewResolvedToKeep.getId()) {
                  reviewResolvedToRemove = reviewResolvedToKeep;
                  reviewResolvedToKeep = mr;
                }
                else {
                  reviewResolvedToRemove = mr;
                }
              }
              
            }
            else {
              throw new Exception ("Unexpected workflowStatus for publishing concept " + mapRecord.getConceptId() + ": " + mr.getWorkflowStatus());
            }
          }
          
          if(reviewResolvedToKeep != null) {
          reviewResolvedToKeep.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
          }
          else {
            throw new Exception (mapRecord.getConceptId() + " is missing a REVIEW_RESOLVED record.");
          }
          
          if(reviewResolvedToRemove != null) {
            newRecords.remove(reviewResolvedToRemove);
          }
          
          // otherwise, bad workflow state, throw exception
        } else {
          throw new Exception(
              "Bad workflow state for publishing concept " + mapRecord.getConceptId()
                  + ":  Expected four records, but found " + mapRecords.size());
        }

        break;

      /** Cancel work */
      case CANCEL:
        // re-retrieve the records for this tracking record and return those
        // used to ensure no spurious alterations from serialization are
        // saved
        // and therefore reflected in the audit trail

        newRecords.clear();
        MappingService mappingService = new MappingServiceJpa();
        for (final Long id : trackingRecord.getMapRecordIds()) {
          newRecords.add(mappingService.getMapRecord(id));
        }
        mappingService.close();
        break;

      /** Create qa record */
      case CREATE_QA_RECORD:
        // TODO fill this in

      default:
        throw new Exception(
            "CONFLICT_AND_REVIEW_PATH received unknown workflow action: " + workflowAction);

    }

    Logger.getLogger(this.getClass()).debug("CONFLICT_AND_REVIEW_PATH records after completion");
    for (MapRecord mr : newRecords) {
      Logger.getLogger(this.getClass()).debug("  " + mr.toString());
    }

    return newRecords;
  }

  @Override
  public String getName() {
    return "CONFLICT_AND_REVIEW_PATH";
  }

  @Override
  public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser,
    MapUserRole userRole, String query, PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {

    Logger.getLogger(this.getClass()).debug(getName() + ": findAvailableWork for project "
        + mapProject.getName() + " and user " + mapUser.getUserName());

    final SearchResultList availableWork = new SearchResultListJpa();

    final StringBuilder sb = new StringBuilder();

    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:" + getName());

    switch (userRole) {

      case LEAD:
        // Handle "team" based assignment
        if (mapProject.isTeamBased() && mapUser.getTeam() != null && !mapUser.getTeam().isEmpty()) {
          // Only include tracking records assigned to my team
          sb.append(" AND assignedTeamName:" + mapUser.getTeam());
        }

        // Don't show if the lead has already been involved
        sb.append(
            " AND NOT assignedUserNames:\"" + QueryParserBase.escape(mapUser.getUserName()) + "\"");

        // Show records that match either of the following cases:
        sb.append(" AND ((");

        // Case 1: requires conflict resolution
        sb.append(" userAndWorkflowStatusPairs:CONFLICT_DETECTED_*");
        // And has not been picked up by a different lead
        sb.append(" AND NOT (" + "userAndWorkflowStatusPairs:CONFLICT_NEW_* OR "
            + "userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_* OR "
            + "userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");

        sb.append(") OR (");

        // Case 2: requires review
        sb.append(" (userAndWorkflowStatusPairs:REVIEW_NEEDED_* OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");
        // And has not been picked up by a different lead
        sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*" + ")");

        sb.append("))");

        break;

      case SPECIALIST:
        // Handle "team" based assignment
        if (mapProject.isTeamBased() && mapUser.getTeam() != null && !mapUser.getTeam().isEmpty()) {
          // Only include tracking records assigned to my team
          sb.append(" AND assignedTeamName:" + mapUser.getTeam());
        }

        // Don't show if the specialist has already been involved
        sb.append(
            " AND NOT assignedUserNames:\"" + QueryParserBase.escape(mapUser.getUserName()) + "\"");

        // Only show if maximum one other specialist is assigned this record
        sb.append(" AND (assignedUserCount:0 OR assignedUserCount:1)");

        break;

      default:
        throw new Exception(getName() + ", findAvailableWork: invalid project role " + userRole);

    }

    int[] totalCt = new int[1];
    @SuppressWarnings("unchecked")
    final List<TrackingRecord> results =
        (List<TrackingRecord>) ((RootServiceJpa) workflowService).getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);

    availableWork.setTotalCount(totalCt[0]);
    for (final TrackingRecord tr : results) {
      final SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      availableWork.addSearchResult(result);
    }

    return availableWork;

  }

  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedWork(MapProject mapProject, MapUser mapUser,
    MapUserRole userRole, String query, PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {

    Logger.getLogger(this.getClass()).debug(getName() + ": findAssignedWork for project "
        + mapProject.getName() + " and user " + mapUser.getUserName());

    // instantiate the assigned work search results
    SearchResultList assignedWork = new SearchResultListJpa();

    // build the initial query
    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:" + getName());

    // determine the query restrictions, type and pfs parameter
    final String type =
        pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";
    final List<TrackingRecord> results;
    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    pfs.setQueryRestriction(null);
    int[] totalCt = new int[1];

    // switch on user role (Specialist or Lead)
    switch (userRole) {

      // for lead-level work, get assigned conflicts and assigned reviews
      case LEAD:

        switch (type) {
          case "CONFLICT_NEW":
            sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName());
            break;
          case "CONFLICT_IN_PROGRESS":
            sb.append(
                " AND userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName());
            break;
          case "CONFLICT_RESOLVED":
            sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_RESOLVED_" + mapUser.getUserName());
            break;
          case "REVIEW_NEW":
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName());
            break;
          case "REVIEW_IN_PROGRESS":
            sb.append(
                " AND userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName());
            break;
          case "REVIEW_RESOLVED":
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_RESOLVED_" + mapUser.getUserName());
            break;
          default:
            sb.append(" AND (userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_" + mapUser.getUserName() + ")");

            break;
        }

        results = (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
        assignedWork.setTotalCount(totalCt[0]);

        for (final TrackingRecord tr : results) {
          final SearchResult result = new SearchResultJpa();

          final Set<MapRecord> mapRecords = workflowService.getMapRecordsForTrackingRecord(tr);

          // get the map record assigned to this user
          MapRecord mapRecord = null;
          for (final MapRecord mr : mapRecords) {
            if (mr.getOwner().equals(mapUser)) {

              // SEE MAP-617:
              // Lower level record may exist with same owner,
              // only
              // add if actually a conflict or review

              if (mr.getWorkflowStatus().compareTo(WorkflowStatus.CONFLICT_DETECTED) < 0) {
                // do nothing, this is the specialist level work
              } else {
                mapRecord = mr;
              }
            }
          }

          if (mapRecord == null) {
            throw new Exception(
                "Failed to retrieve assigned conflicts and reviews:  no map record found for user "
                    + mapUser.getUserName() + " and concept " + tr.getTerminologyId());
          } else {
            result.setTerminologyId(mapRecord.getConceptId());
            result.setValue(mapRecord.getConceptName());
            result.setTerminology(mapRecord.getLastModified().toString());
            result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
            result.setId(mapRecord.getId());
            assignedWork.addSearchResult(result);
          }
        }

        break;

      // for specialist-level work, get assigned work
      case SPECIALIST:

        // add the query terms specific to findAssignedWork
        // - user and workflowStatus must exist in a pair of form:
        // workflowStatus_userName, e.g. NEW_dmo or
        // EDITING_IN_PROGRESS_kli
        // - modify search term based on pfs parameter query restriction
        // field
        // * default: NEW, EDITING_IN_PROGRESS,
        // EDITING_DONE/CONFLICT_DETECTED
        // * NEW: NEW
        // * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

        // add terms based on query restriction
        switch (type) {
          case "NEW":
            sb.append(" AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName());
            break;
          case "EDITING_IN_PROGRESS":
            sb.append(
                " AND userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName());
            break;
          case "EDITING_DONE":
            sb.append(
                " AND (userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName() + ")");
            // Don't show records anymore that have moved on to lead workflow
            // + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
            // + mapUser.getUserName() + ")");
            break;
          default:
            sb.append(" AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName() + ")"
            // It's confusing to show ALL for non-legacy because mapper is done
            // working on it,
            // there's no "finish" step.
            // + " OR userAndWorkflowStatusPairs:EDITING_DONE_"
            // + mapUser.getUserName()
            // + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
            // + mapUser.getUserName() + ")"
            );
            break;
        }

        // add terms to exclude concepts that a lead has claimed
        sb.append(" AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)"
            + " AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*)");

        results = (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
        assignedWork.setTotalCount(totalCt[0]);

        for (final TrackingRecord tr : results) {

          // instantiate the result list
          final SearchResult result = new SearchResultJpa();

          // get the map records associated with this tracking record
          final Set<MapRecord> mapRecords = workflowService.getMapRecordsForTrackingRecord(tr);

          // get the map record assigned to this user
          MapRecord mapRecord = null;

          // SEE BELOW/MAP-617
          WorkflowStatus mapLeadAlternateRecordStatus = null;
          for (final MapRecord mr : mapRecords) {

            if (mr.getOwner().equals(mapUser)) {

              // if this lead has review or conflict work, set the
              // flag
              if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
                  || mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)) {

                mapLeadAlternateRecordStatus = mr.getWorkflowStatus();

                // added to prevent user from getting REVISION
                // record
                // back on FIX_ERROR_PATH
                // yet another problem related to leads being
                // able
                // to
                // serve as dual roles
                // TODO This should be removed
              } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
                // do nothing

                // otherwise, this is the
                // specialist/concept-level
                // work
              } else {
                mapRecord = mr;
              }
            }
          }

          // if no record and no review or conflict work was found,
          // throw
          // error
          if (mapRecord == null) {
            throw new Exception("Failed to retrieve assigned work:  no map record found for user "
                + mapUser.getUserName() + " and concept " + tr.getTerminologyId());

          } else {

            // alter the workflow status if a higher-level record
            // exists
            // for
            // this user
            if (mapLeadAlternateRecordStatus != null) {

              mapRecord.setWorkflowStatus(mapLeadAlternateRecordStatus);
            }
            // create the search result
            result.setTerminologyId(mapRecord.getConceptId());
            result.setValue(mapRecord.getConceptName());
            result.setTerminology(mapRecord.getLastModified().toString());
            result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
            result.setId(mapRecord.getId());
            assignedWork.addSearchResult(result);
          }

        }
        break;
      default:
        throw new Exception(
            "Cannot retrieve work for CONFLICT_AND_REVIEW_PATH for user role " + userRole);
    }

    return assignedWork;
  }

  @Override
  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception {

    MapRecordList originRecords = new MapRecordListJpa();

    for (final Long originId : mapRecord.getOriginIds()) {
      MapRecord mr = workflowService.getMapRecord(originId);
      try {
        if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)
            || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
          originRecords.addMapRecord(workflowService.getMapRecord(originId));
        }
      } catch (Exception e) {
        // if not a null pointer exception due to
        // audited reference not presently in database,
        // rethrow the exception
        if (mr != null) {
          throw new Exception(e);
        }
      }
    }

    if (originRecords.getCount() == 2 || originRecords.getCount() == 3) {
      originRecords.setTotalCount(originRecords.getCount());
      return originRecords;
    } else {
      throw new Exception("Expected two or three conflict/review needed records, but found "
          + originRecords.getCount());
    }

  }

}
