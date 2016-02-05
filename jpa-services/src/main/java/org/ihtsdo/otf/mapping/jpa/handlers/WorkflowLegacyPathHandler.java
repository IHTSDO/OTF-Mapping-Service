package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Workflow path handler for "legacy path".
 */
public class WorkflowLegacyPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the Non Legacy Path
  /** The lead finished state. */
  private static WorkflowPathState initialState;

  /** The first specialist editing state. */
  private static WorkflowPathState firstSpecialistEditingState;

  /** The second specialist editing state. */
  private static WorkflowPathState secondSpecialistEditingState;

  /** The conflict detected state. */
  private static WorkflowPathState conflictDetectedState;

  /** The lead editing state. */
  private static WorkflowPathState leadEditingState;

  /** The lead finished state. */
  private static WorkflowPathState leadFinishedState;

  /**
   * Instantiates an empty {@link WorkflowLegacyPathHandler}.
   */
  public WorkflowLegacyPathHandler() {

    setWorkflowPath(WorkflowPath.LEGACY_PATH);

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
    leadFinishedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_RESOLVED)));
    trackingRecordStateToActionMap.put(
        leadFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
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
