package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;

public class WorkflowQaPathHandler extends AbstractWorkflowPathHandler {

  public WorkflowQaPathHandler() {

    setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

    setEmptyWorkflowAllowed(false);

    WorkflowPathState state;

    // STATE: Initial state has no tracking record

    // workflow states representing a record marked for qa and the original
    // published record
    state = new WorkflowPathState("QA_NEEDED");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // workflow states representing record marked for revision, specialist work,
    // and lead QA (incomplete)
    state = new WorkflowPathState("QA_NEW/QA_IN_PROGREss");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
        WorkflowStatus.QA_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
        WorkflowStatus.QA_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // workflow states representing record marked for revision, specialist work,
    // and lead QA (complete)
    state = new WorkflowPathState("QA_RESOLVED");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.QA_RESOLVED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));
    
    // final state:  no tracking record, one READY_FOR_PUBLICATION record
  }

}
