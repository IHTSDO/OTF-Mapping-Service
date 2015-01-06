package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Workflow path handler for "non legacy path".
 */
public class WorkflowNonLegacyPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the Non Legacy Path
  /**  The lead finished state. */
  private static WorkflowPathState initialState, firstSpecialistEditingState,
      secondSpecialistEditingState, conflictDetectedState, leadEditingState,
      leadFinishedState;

  /**
   * Instantiates an empty {@link WorkflowNonLegacyPathHandler}.
   */
  public WorkflowNonLegacyPathHandler() {

    setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

    setEmptyWorkflowAllowed(true);

    // STATE: Initial initialState has empty tracking record
    initialState = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: One specialist has claimed work
    firstSpecialistEditingState =
        new WorkflowPathState("First Specialist Work");
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.NEW)));
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(
        firstSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Two specialists have claimed work
    secondSpecialistEditingState =
        new WorkflowPathState("Second Specialist Work");
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.NEW, WorkflowStatus.NEW)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.NEW, WorkflowStatus.EDITING_DONE)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.EDITING_IN_PROGRESS,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(
        secondSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Conflict detected
    conflictDetectedState = new WorkflowPathState("Conflict Detected");
    conflictDetectedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(
        conflictDetectedState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Lead review (incomplete)
    leadEditingState = new WorkflowPathState("Lead Conflict Review Incomplet)");
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_NEW)));
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        leadEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Lead review (complete)
    leadFinishedState = new WorkflowPathState("Lead Conflict Review Complete");
    leadFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED)));
    trackingRecordStateToActionMap.put(
        leadFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler#validateTrackingRecordForActionAndUser(org.ihtsdo.otf.mapping.workflow.TrackingRecord, org.ihtsdo.otf.mapping.helpers.WorkflowAction, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings("unused")
  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord tr, WorkflowAction action, MapUser user) throws Exception {

    System.out.println("TrackingRecord " + tr.getUserAndWorkflowStatusPairs());
    System.out.println("  User/action  " + user.getUserName() + "/" + action);

    // throw exception if action or user are undefined
    if (action == null)
      throw new Exception("Action cannot be null.");

    if (user == null)
      throw new Exception("User cannot be null.");

    // first, validate the tracking record itself
    ValidationResult result = validateTrackingRecord(tr);
    if (!result.isValid()) {
      result
          .addError("Could not validate action for user due to workflow errors.");
      System.out.println("  " + result.toString());
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
        mappingService.getMapUserRoleForMapProject(user.getUserName(),
            tr.getMapProjectId());
    mappingService.close();

    // fourth, get the map records and workflow path state from the tracking
    // record
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
    MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
    WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////

    // INITIAL STATE: No specialists have started editing
    // Record requirement : None
    // Permissible actions: ASSIGN_FROM_SCRATCH
    // Minimum role : Specialist
    if (state.equals(initialState)) {

      System.out.println("initial");

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

      System.out.println("firstspecialist");

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }

      // check record
      if (currentRecord == null) {

        System.out.println("No record");

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted.");
        }

        // if a record is already owned by user
      } else {

        // check record status
        if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
            && !currentRecord.getWorkflowStatus().equals(
                WorkflowStatus.EDITING_IN_PROGRESS)
            && !currentRecord.getWorkflowStatus().equals(
                WorkflowStatus.EDITING_DONE)) {

          System.out.println("  Matches");

          result.addError("User's record does not meet requirements");
        }

        // check action
        if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
            && !action.equals(WorkflowAction.FINISH_EDITING)
            && !action.equals(WorkflowAction.UNASSIGN)) {
          result.addError("Action is not permitted.");
        }

      }

      // STATE: Second specialist has begun editing, but both specialists are
      // not finished
      // Record requirement : NEW, EDITING_IN_PROGRESS, EDITING_DONE
      // Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSIGN
      // Minimum role : Specialist

    } else if (state.equals(secondSpecialistEditingState)) {

      System.out.println("Second specialist");

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.EDITING_IN_PROGRESS)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.EDITING_DONE)) {
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

      // Case 1: Lead claiming conflict for review
      // Record requirement : No record
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role : Lead

      // Case 2: Specialist modifying record
      // Record requirement : CONFLICT_DETECTED
      // Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSGIN
      // Minimum role : Specialist
    } else if (state.equals(conflictDetectedState)) {

      System.out.println("Conflict detected");

      // case 1: Lead claiming conflict for review
      if (currentRecord == null) {

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User does not have required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted");
        }

        // Case 2: Specialist modifying record
      } else {
        if (!currentRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED)) {
          result.addError("User's record does meet requirements");
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
      }

      // STATE: Lead editing review
      // Record requirement : REVIEW_RESOLVED
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN, PUBLISHF
      // Minimum role : Lead

      // STATE: Lead editing review
      // Record requirement : REVIEW_NEW, REVIEW_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
      // Minimum role : Lead
    } else if (state.equals(leadEditingState)) {

      System.out.println("Lead editing");

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.CONFLICT_NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_IN_PROGRESS)) {
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

      // STATE: Lead editing review
      // Record requirement : REVIEW_RESOLVED
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN, PUBLISHF
      // Minimum role : Lead
    } else if (state.equals(leadFinishedState)) {

      System.out.println("Lead finished");
      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.CONFLICT_RESOLVED)) {
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
    } else {
      result.addError("Invalid state/could not determine state");
    }

    return result;
  }
}
