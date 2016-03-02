package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
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
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Workflow path handler for "review project path".
 */
public class WorkflowSimplePathHandler extends AbstractWorkflowPathHandler {

  // The workflow states defining the Review Project Path
  /** The initial state. */
  private static WorkflowPathState initialState;

  /** The specialist editing state. */
  private static WorkflowPathState editingState;

  /** The specialist finished state. */
  private static WorkflowPathState finishedState;

  /**
   * Instantiates an empty {@link WorkflowSimplePathHandler}.
   */
  public WorkflowSimplePathHandler() {

    // set the workflow path
    setWorkflowPath(WorkflowPath.SIMPLE_PATH);

    // empty workflow is allowed for this path
    setEmptyWorkflowAllowed(true);

    // initial STATE: tracking record exists, no map records
    // permissible actions: ASSIGN_FROM_SCRATCH
    initialState = new WorkflowPathState("Initial State");
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Specialist level work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    editingState = new WorkflowPathState("Editing In Progress");
    editingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW)));
    editingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(editingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Specialist level work (complete)
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
    // ASSIGN_FROM_SCRATCH
    finishedState = new WorkflowPathState("Editing Finished");
    finishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(finishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN,
            WorkflowAction.PUBLISH)));

  }

  /* see superclass */
  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord tr, WorkflowAction action, MapUser user) throws Exception {

    // first, validate the tracking record itself
    ValidationResult result = validateTrackingRecord(tr);
    if (!result.isValid()) {
      result.addError(
          "Could not validate action for user due to workflow errors.");
      return result;
    }

    // second, check for CANCEL action -- always valid for this path for any
    // state or user (no-op)
    if (action.equals(WorkflowAction.CANCEL)) {
      return result;
    }

    // third, get the user role for this map project
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole userRole = mappingService
        .getMapUserRoleForMapProject(user.getUserName(), tr.getMapProjectId());
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
      result.addError(
          "Could not determine workflow path state for tracking record");
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
    } else if (state.equals(editingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }

    } else if (state.equals(finishedState)) {

      // Case 1: Specialist modifying record
      if (currentRecord != null) {

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not have required role");
        }
      }

    } else {
      result.addError("Could not determine workflow state for tracking record");
    }

    if (result.getErrors().size() != 0) {
      result.addError(
          "Error occured in workflow state " + state.getWorkflowStateName());
    }

    return result;
  }

  @Override
  public String getName() {
    return "SIMPLE_PATH";
  }

  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
      throws Exception {

    Logger.getLogger(this.getClass())
        .debug(getName() + ": findAvailableWork for project "
            + mapProject.getName() + " and user " + mapUser.getUserName());

    SearchResultList availableWork = new SearchResultListJpa();

    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    sb.append(" AND assignedUserCount:0");

    int[] totalCt = new int[1];

    final List<TrackingRecord> results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter,
            totalCt);
    availableWork.setTotalCount(totalCt[0]);
    for (

    TrackingRecord tr : results)

    {
      SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      availableWork.addSearchResult(result);
    }
    return availableWork;

  }

  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
      throws Exception {
    SearchResultList assignedWork = new SearchResultListJpa();
    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    final String type = pfsParameter.getQueryRestriction() != null
        ? pfsParameter.getQueryRestriction() : "";

    // add terms based on query restriction
    switch (type) {
      case "NEW":
        sb.append(
            " AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName());
        break;
      case "EDITING_IN_PROGRESS":
        sb.append(" AND userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
            + mapUser.getUserName());
        break;
      case "EDITING_DONE":
        sb.append(" AND userAndWorkflowStatusPairs:EDITING_DONE_"
            + mapUser.getUserName());
        break;
      default:
        sb.append(
            " AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_DONE_"
                + mapUser.getUserName() + ")");
        break;
    }

    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    pfs.setQueryRestriction(null);
    int[] totalCt = new int[1];
    final List<TrackingRecord> results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
    assignedWork.setTotalCount(totalCt[0]);

    for (final TrackingRecord tr : results) {
      final SearchResult result = new SearchResultJpa();

      final Set<MapRecord> mapRecords =
          workflowService.getMapRecordsForTrackingRecord(tr);

      // get the map record assigned to this user
      MapRecord mapRecord = null;
      for (final MapRecord mr : mapRecords) {

        // find highest-level workflow status (i.e. user can review themselves
        // and want REVIEW_X record)
        if (mr.getOwner().equals(mapUser) && (mapRecord == null || mapRecord
            .getWorkflowStatus().compareTo(mr.getWorkflowStatus()) < 0)) {
          mapRecord = mr;
        }
      }

      if (mapRecord == null) {
        throw new Exception(
            "Failed to retrieve assigned work:  no map record found for user "
                + mapUser.getUserName() + " and concept "
                + tr.getTerminologyId());
      }
      result.setTerminologyId(mapRecord.getConceptId());
      result.setValue(mapRecord.getConceptName());
      result.setTerminology(mapRecord.getLastModified().toString());
      result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
      result.setId(mapRecord.getId());
      assignedWork.addSearchResult(result);
    }
    return assignedWork;

  }

  @Override
  public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord,
    WorkflowAction workflowAction, MapProject mapProject, MapUser mapUser,
    Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {
    Logger.getLogger(this.getClass())
        .debug(getName() + ": Processing workflow action by " + mapUser.getName()
            + ":  " + workflowAction.toString());

    // the set of records returned after processing
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    switch (workflowAction) {
      case ASSIGN_FROM_SCRATCH:

        // create new record
        MapRecord newRecord =
            createMapRecordForTrackingRecordAndUser(trackingRecord, mapUser);

        // set workflow status to new
        newRecord.setWorkflowStatus(WorkflowStatus.NEW);

        newRecords.add(newRecord);
        break;
      case CANCEL:
        // re-retrieve records from database to return to original state
        // and ensure no spurious audit trail entries
        newRecords.clear();
        MappingService mappingService = new MappingServiceJpa();
        for (final Long id : trackingRecord.getMapRecordIds()) {
          newRecords.add(mappingService.getMapRecord(id));
        }
        mappingService.close();
        break;
      case FINISH_EDITING:

        switch (mapRecord.getWorkflowStatus()) {

          case EDITING_DONE:
          case EDITING_IN_PROGRESS:
          case NEW:

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .debug("FinishEditing: SIMPLE_PATH, Specialist level work");

            // check assumptions
            // - should only be one record
            if (mapRecords.size() != 1) {
              throw new Exception(
                  "FINISH called at initial editing level on SIMPLE_PATH where more than one record exists");
            }

            // mark as EDITING_DONE
            mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);

            break;

          default:
            throw new Exception(
                "Called finish on map record with invalid workflow status along SIMPLE_PATH");
        }
        break;
      case PUBLISH:
        // Requirements for SIMPLE_PATH
        // - 1 record marked EDITING_DONE

        // check assumption: owned record is marked resolved
        if (!mapRecord.getWorkflowStatus()
            .equals(WorkflowStatus.EDITING_DONE)) {
          throw new Exception(
              "Publish called on SIMPLE_PATH for map record not marked as EDITING_DONE");
        }

        // set the user's record to READY_FOR_PUBLICATION
        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        break;
      case SAVE_FOR_LATER:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);

        break;
      case UNASSIGN:
        newRecords.clear();
        break;
      default:
        throw new Exception(getName()
            + ": Illegal workflow action requested -- " + workflowAction);

    }

    return newRecords;
  }

}
