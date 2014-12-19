package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public class WorkflowReviewProjectPathHandler extends
    AbstractWorkflowPathHandler {

  // constructor defines the tracking record states that exist for this workflow
  public WorkflowReviewProjectPathHandler() {

    // set the workflow path
    setWorkflowPath(WorkflowPath.REVIEW_PROJECT_PATH);

    // empty workflow is allowed for this path
    setEmptyWorkflowAllowed(true);

    // declare state variable for constructoin
    WorkflowPathState state;

    // initial STATE: tracking record exists, no map records
    // permissible actions: ASSIGN_FROM_SCRATCH
    state = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(state,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Specialist level work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    state = new WorkflowPathState("Unfinished Specialist Work");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    state = new WorkflowPathState("Unfinished lead Work");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Finished lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, PUBLISH, UNASSIGN
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

  }
}
