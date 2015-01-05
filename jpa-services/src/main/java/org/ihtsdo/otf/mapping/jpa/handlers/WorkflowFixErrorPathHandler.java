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
 * Workflow path handler for "fix error path"
 */
public class WorkflowFixErrorPathHandler extends AbstractWorkflowPathHandler {

  // The workflow states defining the Fix Error workflow path
  /**  The lead finished state. */
  private static WorkflowPathState specialistEditingState,
      specialistFinishedState, leadEditingState, leadFinishedState;

  /**
   * Instantiates an empty {@link WorkflowFixErrorPathHandler}.
   */
  public WorkflowFixErrorPathHandler() {

    setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

    setEmptyWorkflowAllowed(false);

    // STATE: Initial state has no tracking record

    // workflow states representing a record marked for revision and the
    // specialist-level editing
    specialistEditingState = new WorkflowPathState("NEW/IN_PROGRESS");
    specialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    specialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        specialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    specialistFinishedState = new WorkflowPathState("REVIEW_NEEDED");
    specialistFinishedState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(
        specialistFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN,
            WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // workflow leadEditingStates representing record marked for revision, specialist work,
    // and lead review
    leadEditingState = new WorkflowPathState("Lead Review Incomplete");
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_NEW)));
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        leadEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    leadFinishedState = new WorkflowPathState("Lead Review Complete");
    leadFinishedState.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(
        leadFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler#validateTrackingRecordForActionAndUser(org.ihtsdo.otf.mapping.workflow.TrackingRecord, org.ihtsdo.otf.mapping.helpers.WorkflowAction, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings("unused")
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

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////
    
    // INITIAL STATE: Claiming concept for error fix
    // Record requirement : None
    // Permissible actions: ASSIGN_FROM_INITIAL_RECORD
    // Minimum role : Specialist
    if (tr == null) {
      
      // check record
      if (currentRecord != null) {
        result.addError("User's record does not meet requirements");
      }
      
      // check role
      if (userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }
      
      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_INITIAL_RECORD)) {
        result.addError("Action is not permitted");
      }
    }

    else if (state.equals(specialistEditingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.EDITING_IN_PROGRESS)) {
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

      // STATE: Specialist level work (complete)
      // Case 1: Specialist modifying record
      // Record requirement : REVIEW_NEEDED
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
      // Minimum role : Specialist
      //
      // Case 2: Lead assigning review
      // Record requirement : No record
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role: Lead

    } else if (state.equals(specialistFinishedState)) {

      // Case 1: Specialist modifying record
      if (currentRecord != null) {

        // check record
        if (!currentRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_NEEDED)) {
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

        // Case 2: Lead assigning review
      } else {

        // check record
        // no-op, already verified null

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User does not have required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted.");
        }
      }

      // STATE: Lead editing review
      // Record requirement : REVIEW_NEW, REVIEW_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
      // Minimum role : Lead
    } else if (state.equals(leadEditingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.REVIEW_NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.REVIEW_IN_PROGRESS)) {
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
      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.REVIEW_RESOLVED)) {
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

    return result;
  }
}
