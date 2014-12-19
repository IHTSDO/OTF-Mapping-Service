package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;

public class WorkflowNonLegacyPathHandler extends AbstractWorkflowPathHandler {

  public WorkflowNonLegacyPathHandler() {

    setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

    setEmptyWorkflowAllowed(true);

    WorkflowPathState state;

    // STATE: Initial state has empty tracking record
    state = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(state,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: One specialist has claimed work
    state = new WorkflowPathState("Specialist Work");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Two specialists have claimed work
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.NEW, WorkflowStatus.EDITING_DONE)));
    state
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.EDITING_IN_PROGRESS,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Conflict detected
    state = new WorkflowPathState("Conflict Detected");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Lead review (incomplete)
    state = new WorkflowPathState("Lead Conflict Review Incomplet)");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Lead review (complete)
    state = new WorkflowPathState("Lead Conflict Review Complete");
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
        WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        state,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
  }
}
