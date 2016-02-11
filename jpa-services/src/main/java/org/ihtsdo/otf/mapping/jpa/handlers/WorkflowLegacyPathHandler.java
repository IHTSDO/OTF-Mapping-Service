package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Workflow path handler for "legacy path".
 */
public class WorkflowLegacyPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the Non Legacy Path
  /** The lead finished state. */
  private static WorkflowPathState initialState;

  /** The first specialist editing state. */
  private static WorkflowPathState firstSpecialistEditingState;

  /** The first specialist conflict state */
  private static WorkflowPathState firstSpecialistConflictState;

  /** The second specialist editing state. */
  private static WorkflowPathState secondSpecialistEditingState;

  /** The second specialist conflict state */
  @SuppressWarnings("unused")
  private static WorkflowPathState secondSpecialistConflictState;

  /** The conflict detected state. */
  private static WorkflowPathState conflictDetectedState;

  /** The lead editing state. */
  private static WorkflowPathState conflictEditingState;

  /** The lead finished state. */
  private static WorkflowPathState conflictFinishedState;

  /**
   * Instantiates an empty {@link WorkflowLegacyPathHandler}.
   */
  public WorkflowLegacyPathHandler() {

    setWorkflowPath(WorkflowPath.LEGACY_PATH);

    setEmptyWorkflowAllowed(true);

    // STATE: Initial initialState has published legacy record
    initialState = new WorkflowPathState("Initial State");
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.PUBLISHED)));
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Legacy record is in revision, One specialist has claimed work
    firstSpecialistEditingState =
        new WorkflowPathState("First Specialist Work");
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));

    trackingRecordStateToActionMap.put(
        firstSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision, first user has finished work
    firstSpecialistConflictState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(
        firstSpecialistConflictState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_INITIAL_RECORD,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision and in conflict with first
    // specialist's work, second specialist has claimed work
    secondSpecialistEditingState =
        new WorkflowPathState("Second Specialist Work");
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.NEW)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED)));

    trackingRecordStateToActionMap.put(
        secondSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision, two specialists have completed
    // work in conflict
    conflictDetectedState = new WorkflowPathState("Conflict Detected");
    conflictDetectedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(
        conflictDetectedState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Lead review (incomplete)
    conflictEditingState =
        new WorkflowPathState("Lead Conflict Review Incomplet)");
    conflictEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_NEW)));
    conflictEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        conflictEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Lead review (complete)
    conflictFinishedState =
        new WorkflowPathState("Lead Conflict Review Complete");
    conflictFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_RESOLVED)));
    trackingRecordStateToActionMap.put(
        conflictFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
  }

  /* see superclass */
  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord tr, WorkflowAction action, MapUser user) throws Exception {

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

    // fifth, basic check that workflow state is in the map
    if (!trackingRecordStateToActionMap.containsKey(state)) {
      result.addError("Invalid state/could not determine state");
    }

    // sixth, basic check that the workflow action is permitted for this
    // state
    if (!trackingRecordStateToActionMap.get(state).contains(action)) {
      result.addError("Workflow action not permitted for state");
    }

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////
    if (state == null) {
      result
          .addError("Could not determine workflow path state for tracking record");
    }

    // INITIAL STATE: No specialists have started editing
    // Record requirement : none
    // Permissible actions: ASSIGN_FROM_SCRATCH
    // Minimum role : Specialist
    else if (state.equals(initialState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

    }

    // STATE: One specialist is editing
    // Record requirement : none
    // Minimum role : Specialist
    else if (state.equals(firstSpecialistEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // no additional checks required

    }

    // STATE: One specialist has finished editing, conflicts with legacy
    // record
    // Record requirement : none for second user, conflict detected record
    // for first
    // Minimum role : Specialist
    else if (state.equals(firstSpecialistConflictState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // if user does not have record, only assign from scratch permitted
      if (currentRecord != null) {
        switch (currentRecord.getWorkflowStatus()) {
          case CONFLICT_DETECTED:
          case EDITING_IN_PROGRESS:
          case NEW:
            // do nothing, valid
            break;
          default:
            result.addError("User's record has invalid workflow state");
            break;
        }

        if (action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted for this user and state");
        }
      }

      // if user has record, cannot assign from scratch
      if (currentRecord == null
          && !action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action is not permitted for this user and state");
      }

    }

    // STATE: One specialist has finished editing, conflicts with legacy
    // record, specialist is editing
    // Record requirement : record for user must exist in editing
    // Minimum role : Specialist
    else if (state.equals(secondSpecialistEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      if (currentRecord == null) {
        result.addError("User must have a record");
      }

      switch (currentRecord.getWorkflowStatus()) {
        case CONFLICT_DETECTED:
        case EDITING_IN_PROGRESS:
        case NEW:
          // do nothing, valid
          break;
        default:
          result.addError("User's record has invalid workflow state");
          break;
      }

    }

    // STATE: Two specialists have finished editing and their work conflicts
    // (legacy record now disregarded)
    // Record requrement : none for lead review, must exist for specialists
    // Minimum role : Specialist
    else if (state.equals(conflictDetectedState)) {

      // case: lead review assignment
      if (currentRecord == null) {
        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User does not meet required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action not permitted");
        }
      }

      // case: specialist performing further edits
      else {
        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not meet required role");
        }

        // check record
        if (!currentRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED)) {
          result.addError("User's record is not in conflict detection state");
        }

        // check action
        if (action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action not permitted");
        }
      }

    }

    // STATE: Lead is performing review on two specialist records
    // Record requirement : record must exist and be in conflict state
    else if (state.equals(conflictEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not meet required role");
      }
    } else if (state.equals(conflictFinishedState)) {
      result.addError("Invalid state/could not determine state");
    }

    return result;
  }

  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

}
