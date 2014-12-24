package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;

public class WorkflowFixErrorPathHandler extends AbstractWorkflowPathHandler {

  public WorkflowFixErrorPathHandler() {

    setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

    setEmptyWorkflowAllowed(false);

    WorkflowPathState state;

    // STATE: Initial state has no tracking record

    // workflow states representing a record marked for revision and the
    // specialist-level editing
    state = new WorkflowPathState("REVIEW_NEW/REVIEW_IN_PROGRESS");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    state = new WorkflowPathState("REVIEW_NEEDED");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN, WorkflowAction.ASSIGN_FROM_SCRATCH)));
    
    // workflow states representing record marked for revision, specialist work,
    // and lead review
    state = new WorkflowPathState("Lead Review Incomplete");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    state = new WorkflowPathState("Lead Review Complete");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));
    
    // final state:  no tracking record, one READY_FOR_PUBLICATION record
  }

}
