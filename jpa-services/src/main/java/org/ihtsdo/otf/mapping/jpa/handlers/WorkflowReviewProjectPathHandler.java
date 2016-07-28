package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
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
public class WorkflowReviewProjectPathHandler
    extends AbstractWorkflowPathHandler {

  // The workflow states defining the Review Project Path
  /** The initial state. */
  private static WorkflowPathState initialState;

  /** The specialist editing state. */
  private static WorkflowPathState specialistEditingState;

  /** The specialist finished state. */
  private static WorkflowPathState specialistFinishedState;

  /** The lead editing state. */
  private static WorkflowPathState leadEditingState;

  /** The lead finished state. */
  private static WorkflowPathState leadFinishedState;

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
    specialistEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW)));
    specialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(specialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Specialist level work (complete)
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
    // ASSIGN_FROM_SCRATCH
    specialistFinishedState = new WorkflowPathState("REVIEW_NEEDED");
    specialistFinishedState
        .addWorkflowCombination(new WorkflowStatusCombination(
            Arrays.asList(WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(specialistFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN,
            WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
    leadEditingState = new WorkflowPathState("REVIEW_NEW/REVIEW_IN_PROGRESS");
    leadEditingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    leadEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(leadEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Finished lead work
    // permissible actions: SAVE_FOR_LATER, FINISH_EDITING, PUBLISH,
    // UNASSIGN
    leadFinishedState = new WorkflowPathState("REVIEW_RESOLVED");
    leadFinishedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap.put(leadFinishedState,
        new HashSet<>(
            Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.PUBLISH,
                WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

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
    } else if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {

      // for creating qa record, only check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not have required role");
      }
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
          && !currentRecord.getWorkflowStatus()
              .equals(WorkflowStatus.EDITING_IN_PROGRESS)) {
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
      // Record requirement : No record (or if lead edited, could be
      // REVIEW_NEEDED)
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role: Lead

    } else if (state.equals(specialistFinishedState)) {

      // Case 1: Specialist modifying record
      if (currentRecord != null) {

        // check record
        if (!currentRecord.getWorkflowStatus()
            .equals(WorkflowStatus.REVIEW_NEEDED)) {
          result.addError("User's record does not meet requirements");
        }

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not have required role");
        }

        // If lead role, ASSIGN_FROM_SCRATCH is allowed
        // ( e.g., this is the case where a lead edited the record
        // instead of a specialist - reviewing their own work)
        if (userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
            result.addError("Action is not permitted.");
          }
        }

        else {
          // check action
          if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
              && !action.equals(WorkflowAction.FINISH_EDITING)
              && !action.equals(WorkflowAction.UNASSIGN)) {
            result.addError("Action is not permitted.");
          }
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
      } else if (!currentRecord.getWorkflowStatus()
          .equals(WorkflowStatus.REVIEW_NEW)
          && !currentRecord.getWorkflowStatus()
              .equals(WorkflowStatus.REVIEW_IN_PROGRESS)) {
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
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
      // PUBLISHF
      // Minimum role : Lead
    } else if (state.equals(leadFinishedState)) {
      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus()
          .equals(WorkflowStatus.REVIEW_RESOLVED)) {
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
    return "REVIEW_PROJECT_PATH";
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

    switch (userRole) {

      // REVIEW_NEEDED record with no REVIEW_NEW, REVIEW_IN_PROGRESS, or
      // REVIEW_RESOLVED
      case LEAD:
        // Case requires review
        sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEEDED_*");
        // And was not edited by this lead
        sb.append(" AND NOT userAndWorkflowStatusPairs:REVIEW_NEEDED_"
            + mapUser.getUserName());

        // And has not been picked up by a different lead
        sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*" + ")");
        break;

      // any concept with no assigned users
      case SPECIALIST:
        sb.append(" AND assignedUserCount:0");
        break;
      default:
        throw new Exception(getName()
            + ", findAvailableWork: invalid project role " + userRole);

    }

    int[] totalCt = new int[1];
    final List<TrackingRecord> results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter,
            totalCt);
    availableWork.setTotalCount(totalCt[0]);
    for (TrackingRecord tr : results) {
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

    Logger.getLogger(this.getClass())
        .debug(getName() + ": findAvailableWork for project "
            + mapProject.getName() + " and user " + mapUser.getUserName());

    SearchResultList assignedWork = new SearchResultListJpa();
    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    final String type = pfsParameter.getQueryRestriction() != null
        ? pfsParameter.getQueryRestriction() : "";

    switch (userRole) {
      case LEAD:

        // add the query terms specific to findassignedWork
        // - user and workflow status must exist in the form
        // REVIEW_NEW_userName
        // or REVIEW_IN_PROGRESS_userName

        // add terms based on query restriction
        switch (type) {
          case "REVIEW_NEW":
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEW_"
                + mapUser.getUserName());

            break;
          case "REVIEW_IN_PROGRESS":
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_"
                + mapUser.getUserName());
            break;
          case "REVIEW_RESOLVED":
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_RESOLVED_"
                + mapUser.getUserName());
            break;
          default:
            sb.append(" AND (userAndWorkflowStatusPairs:REVIEW_NEW_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_"
                + mapUser.getUserName() + ")");
            break;
        }

        break;
      case SPECIALIST:
        // add the query terms specific to findAssignedWork
        // - user and workflowStatus must exist in a pair of form:
        // workflowStatus_userName, e.g. NEW_dmo or EDITING_IN_PROGRESS_kli
        // - modify search term based on pfs parameter query restriction
        // field
        // * default: NEW, EDITING_IN_PROGRESS,
        // EDITING_DONE/CONFLICT_DETECTED
        // * NEW: NEW
        // * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

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
            sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEEDED_"
                + mapUser.getUserName());
            break;
          default:
            sb.append(
                " AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
                    + " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                    + mapUser.getUserName()
                    + " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_"
                    + mapUser.getUserName() + ")");
            break;
        }

        // add terms to exclude concepts that a lead has claimed
        sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*)");

        break;
      default:
        throw new Exception(
            getName() + ", findAssignedWork: invalid project role " + userRole);
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
        .debug(getName() + ": Processing workflow action by "
            + mapUser.getName() + ":  " + workflowAction.toString());

    // the set of records returned after processing
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    switch (workflowAction) {
      case ASSIGN_FROM_SCRATCH:

        // create new record
        MapRecord newRecord =
            createMapRecordForTrackingRecordAndUser(trackingRecord, mapUser);

        // check for LEAD assignment
        if (getWorkflowStatusFromMapRecords(mapRecords)
            .equals(WorkflowStatus.REVIEW_NEEDED)) {
          // check that one record exists and is not owned by this user
          if (mapRecords.size() != 1) {
            throw new Exception("  Expected exactly one map record");
          }

          // set origin id to the existing record
          newRecord.addOrigin(mapRecords.iterator().next().getId());

          // set workflow status to review needed
          newRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);
        }

        // check for SPECIALIST assignment
        else if (mapRecords.size() == 0) {

          // set workflow status to new
          newRecord.setWorkflowStatus(WorkflowStatus.NEW);

        } else {
          throw new Exception(
              "ASSIGN_FROM_SCRATCH on REVIEW_PROJECT_PATH failed for concept "
                  + mapRecord.getConceptId());
        }

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

          // case 1: specialist finishes a map record
          case REVIEW_NEEDED:
          case EDITING_IN_PROGRESS:
          case NEW:

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .debug(
                    "FinishEditing: REVIEW_PROJECT_PATH, Specialist level work");

            // check assumptions
            // - should only be one record
            if (mapRecords.size() != 1) {
              throw new Exception(
                  "FINISH called at initial editing level on REVIEW_PROJECT_PATH where more than one record exists");
            }

            // mark as REVIEW_NEEDED
            mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);

            break;

          case REVIEW_RESOLVED:
          case REVIEW_IN_PROGRESS:
          case REVIEW_NEW:

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .debug("FinishEditing: REVIEW_PROJECT_PATH, Lead level work");

            // check assumptions
            // - should be two map records, this one and one marked
            // REVIEW_NEEDED

            // check assumption: only two records
            if (mapRecords.size() != 2)
              throw new Exception(
                  "FINISH called at review editing level on REVIEW_PROJECT_PATH without exactly two map records");

            // check assumption: review needed record present
            MapRecord reviewRecord = null;
            for (final MapRecord mr : mapRecords) {
              if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
                reviewRecord = mr;
            }

            if (reviewRecord == null)
              throw new Exception(
                  "FINISH called at review editing level on REVIEW_PROJECT_PATH, but could not locate REVIEW_NEEDED record");

            // mark as REVIEW_RESOLVED
            mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_RESOLVED);

            break;

          default:
            throw new Exception(
                "Called finish on map record with invalid workflow status along REVIEW_PROJECT_PATH");
        }
        break;
      case PUBLISH:
        // Requirements for REVIEW_PROJECT_PATH
        // - 1 record marked REVIEW_NEEDED
        // - 1 record marked REVIEW_RESOLVED

        // check assumption: owned record is marked resolved
        if (!mapRecord.getWorkflowStatus()
            .equals(WorkflowStatus.REVIEW_RESOLVED)) {
          throw new Exception(
              "Publish called on REVIEW_PROJECT_PATH for map record not marked as REVIEW_RESOLVED");
        }

        // check assumption: record requiring review is present
        MapRecord reviewNeededRecord = null;
        for (final MapRecord mr : newRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
            reviewNeededRecord = mr;
        }

        if (reviewNeededRecord == null) {
          throw new Exception(
              "Publish called on REVIEW_PROJECT_PATH, but no REVIEW_NEEDED record found");
        }

        // remove the review needed record
        newRecords.remove(reviewNeededRecord);

        // set the lead's record to READY_FOR_PUBLICATION
        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        break;
      case SAVE_FOR_LATER:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_IN_PROGRESS);

        break;
      case UNASSIGN:
        // find the highest level map record assigned to this user and
        // remove
        MapRecord recordToRemove = null;
        for (MapRecord mr : newRecords) {
          if (mr.getOwner().equals(mapUser)
              && (recordToRemove == null || recordToRemove.getWorkflowStatus()
                  .compareTo(mr.getWorkflowStatus()) < 0)) {
            recordToRemove = mr;
          }
        }
        newRecords.remove(recordToRemove);
        break;
      default:
        throw new Exception(getName()
            + ": Illegal workflow action requested -- " + workflowAction);

    }

    return newRecords;
  }

  @Override
  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception {

    MapRecordList originRecords = new MapRecordListJpa();

    for (final Long originId : mapRecord.getOriginIds()) {
      MapRecord mr = workflowService.getMapRecord(originId);
      try {
        if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
          originRecords.addMapRecord(workflowService.getMapRecord(originId));
        }
      } catch (Exception e) {
        // if not a null pointer exception due to
        // audited reference not presently in database,
        // rethrow the exception
        if (mr != null) {
          throw new Exception(e);
        }
      }
    }

    if (originRecords.getCount() == 1) {
      originRecords.setTotalCount(originRecords.getCount());
      return originRecords;
    } else {
      throw new Exception(
          "Expected one record requiring review along Review Path, instead found "
              + originRecords.getCount());
    }
  }
}
