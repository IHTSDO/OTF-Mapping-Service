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
 * Workflow path handler for "review project path".
 */
public class WorkflowReviewProjectPathHandler extends
    AbstractWorkflowPathHandler {

  // The workflow states defining the Review Project Path
  private static WorkflowPathState initialState, specialistEditingState,
      specialistFinishedState, leadEditingState, leadFinishedState;

  /**
   * Instantiates an empty {@link WorkflowReviewProjectPathHandler}.
   */
  public WorkflowReviewProjectPathHandler() {

    // set the workflow path
    setWorkflowPath(WorkflowPath.REVIEW_PROJECT_PATH);

    // empty workflow is allowed for this path
    setEmptyWorkflowAllowed(true);

    // initial STATE: tracking record exists, no map records
    // permissible actions: ASSIGN_FROM_SCRATCH
    initialState = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Specialist level work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    specialistEditingState =
        new WorkflowPathState("REVIEW_NEW/REVIEW_IN_PROGRESS");
    specialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.NEW)));
    specialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        specialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Specialist level work (complete)
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
    // ASSIGN_FROM_SCRATCH
    specialistFinishedState = new WorkflowPathState("REVIEW_NEEDED");
    specialistFinishedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(
        specialistFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN,
            WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    leadEditingState = new WorkflowPathState("REVIEW_NEW/REVIEW_IN_PROGRESS");
    leadEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED,
            WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        leadEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Finished lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, PUBLISH, UNASSIGN
    leadFinishedState = new WorkflowPathState("REVIEW_RESOLVED");
    leadFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED,
            WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(
        leadFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

  }

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

    if (state == null) {
      result.addError("Could not determine workflow path state for tracking record");
    }
    
    // Record requirement : No record
    // Permissible action : ASSIGN_FROM_SCRATCH
    // Minimum role : Specialist
    else if (state.equals(initialState)) {
  
      // check record
      if (currentRecord != null) {
        result.addError("User record does not meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }

      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action is not permitted.");
      }

      // STATE: Specialist level work
      // Record requirement : NEW, EDITING_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
      // Minimum role : Specialist
    } else if (state.equals(specialistEditingState)) {

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
    } else  {
      result.addError("Could not determine workflow state for tracking record");
    }
    
    if (result.getErrors().size() != 0) {
      result.addError("Error occured in workflow state " + state.getWorkflowStateName());;
    }

    return result;
  }
}
