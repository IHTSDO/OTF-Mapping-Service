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
 * Workflow path handler for "qa path".
 */
public class WorkflowQaPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the QA Path Workflow
  /** The finished state. */
  private static WorkflowPathState qaNeededState;

  /** The editing state. */
  private static WorkflowPathState editingState;

  /** The finished state. */
  private static WorkflowPathState finishedState;

  /**
   * Instantiates an empty {@link WorkflowQaPathHandler}.
   */
  public WorkflowQaPathHandler() {

    setWorkflowPath(WorkflowPath.QA_PATH);

    setEmptyWorkflowAllowed(false);

    // STATE: Initial state has no tracking record
    // Only valid action is CREATE_QA_RECORD

    // workflow states representing a record marked for qa and the original
    // published record
    qaNeededState = new WorkflowPathState("QA_NEEDED");
    qaNeededState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED)));
    trackingRecordStateToActionMap.put(
        qaNeededState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.UNASSIGN)));

    // workflow states representing record marked for revision, specialist work,
    // and lead QA (incomplete)
    editingState = new WorkflowPathState("QA_NEW/QA_IN_PROGRESS");
    editingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_NEW)));
    editingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        editingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // workflow finishedStates representing record marked for revision,
    // specialist work,
    // and lead QA (complete)
    finishedState = new WorkflowPathState("QA_RESOLVED");
    finishedState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_RESOLVED)));
    trackingRecordStateToActionMap.put(
        finishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // final state: no tracking record, one READY_FOR_PUBLICATION record
  }

  /* see superclass */
  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord tr, WorkflowAction action, MapUser user) throws Exception {

    // first, validate the tracking record itself
    ValidationResult result = validateTrackingRecord(tr);
    if (!result.isValid()) {
      result
          .addError("Could not validate action for user due to workflow errors.");

      return result;
    }

    // check for CANCEL action -- always valid for this path for any
    // state or user (no-op)
    if (action.equals(WorkflowAction.CANCEL)) {
      return result;
    }

    // check for CREATE_QA_RECORD action -- this is always valid, as it merely
    // applies labels to concepts that are already in the workflow
    if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {
      return result;
    }

    // get the user role for this map project
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole userRole =
        mappingService.getMapUserRoleForMapProject(user.getUserName(),
            tr.getMapProjectId());
    mappingService.close();

    // get the map records and workflow path state from the tracking
    // record
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
    MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
    WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////
    if (state == null) {
      result
          .addError("Could not determine workflow path state for tracking record");
    }

    else if (state.equals(qaNeededState)) {

      // check record -- null means none assigned
      if (currentRecord == null) {

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not have required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted.");
        }
      } else {

        // check role
        // qa user does not have role requirements for unassign

        // check action
        if (!action.equals(WorkflowAction.UNASSIGN)) {
          result.addError("Action is not permitted.");
        }

      }

      // STATE: Specialist level work
      // Record requirement : NEW, EDITING_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
      // Minimum role : Specialist
    } else if (state.equals(editingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.QA_NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.QA_IN_PROGRESS)) {
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

    } else if (state.equals(finishedState)) {
      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.QA_RESOLVED)) {
        result.addError("User's record does meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
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

    if (result.getErrors().size() != 0) {
      result.addError("Error occured in workflow state "
          + state.getWorkflowStateName());
    }

    return result;
  }

@Override
public String getName() {
	// TODO Auto-generated method stub
	return null;
}

}
