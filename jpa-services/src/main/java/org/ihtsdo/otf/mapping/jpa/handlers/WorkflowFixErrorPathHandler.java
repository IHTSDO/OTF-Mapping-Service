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
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Workflow path handler for "fix error path".
 */
public class WorkflowFixErrorPathHandler extends AbstractWorkflowPathHandler {

  // Initial state is a tracking record with no records or users assigned

  /** The lead finished state. */
  private static WorkflowPathState specialistEditingState;

  /** The specialist finished state. */
  private static WorkflowPathState specialistFinishedState;

  /** The lead editing state. */
  private static WorkflowPathState leadEditingState;

  /** The lead finished state. */
  private static WorkflowPathState leadFinishedState;

  /**
   * Instantiates an empty {@link WorkflowFixErrorPathHandler}.
   */
  public WorkflowFixErrorPathHandler() {

    setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

    // initial state has no user/workflow status pairs
    // e.g. PUBLISHED record, no assigned user
    setEmptyWorkflowAllowed(true);

    // workflow states representing a record marked for revision and the
    // specialist-level editing
    specialistEditingState = new WorkflowPathState("NEW/IN_PROGRESS");
    specialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    specialistEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(specialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    specialistFinishedState = new WorkflowPathState("REVIEW_NEEDED");
    specialistFinishedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays
            .asList(WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));
    trackingRecordStateToActionMap.put(specialistFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN,
            WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // workflow leadEditingStates representing record marked for revision,
    // specialist work,
    // and lead review
    leadEditingState = new WorkflowPathState("Lead Review Incomplete");
    leadEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    leadEditingState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(leadEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    leadFinishedState = new WorkflowPathState("Lead Review Complete");
    leadFinishedState.addWorkflowCombination(
        new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
    trackingRecordStateToActionMap
        .put(leadFinishedState,
            new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
                WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
                WorkflowAction.UNASSIGN)));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler#
   * validateTrackingRecordForActionAndUser
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord,
   * org.ihtsdo.otf.mapping.helpers.WorkflowAction,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  /* see superclass */
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

    // INITIAL STATE: Claiming concept for error fix
    // Record requirement : None
    // Permissible actions: ASSIGN_FROM_INITIAL_RECORD
    // Minimum role : Specialist
    if (tr == null) {
      result.addError(
          "Blank, non-committed tracking record required (i.e. project and concept specified only)");
    } else if (state == null) {

      // check that tracking record has not yet been persisted (i.e. will
      // be created)
      if (tr.getId() != null) {
        result.addError(
            "Blank, non-committed tracking record required (i.e. project and concept specified only)");
      }
      // check record
      if (currentRecord != null) {
        result.addError("User's record does not meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_INITIAL_RECORD)) {
        result.addError("Action is not permitted");
      }
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
      // Record requirement : No record
      // Permissible actions: ASSIGN_FROM_SCRATCH
      // Minimum role: Lead

    } else if (state.equals(specialistFinishedState)) {

      // Case 1: ASSIGN_FROM_SCRATCH requested
      if (action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User does not have required role");
        }
      }

      // Case 2: Any other action must be Specialist modifying finished
      // record
      else if (currentRecord != null) {

        // check record
        if (!currentRecord.getWorkflowStatus()
            .equals(WorkflowStatus.REVIEW_NEEDED)) {
          result.addError("User's record is marked "
              + currentRecord.getWorkflowStatus().toString()
              + " instead of REVIEW_NEW");
        }

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User must be a Lead");
        }

        // check action
        if (!action.equals(WorkflowAction.SAVE_FOR_LATER)
            && !action.equals(WorkflowAction.FINISH_EDITING)
            && !action.equals(WorkflowAction.UNASSIGN)) {
          result.addError("Action " + action.toString() + " is not permitted.");
        }

      }

      // otherwise, not ASSIGN_FROM_SCRATCH and record is null
      else {
        result.addError("Action " + action.toString()
            + " not permitted where user is not editing a record");
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
    }

    if (result.getErrors().size() != 0) {
      result.addError("Error occured on " + getName() + " in workflow state "
          + (state != null ? state.getWorkflowStateName()
              : "Undetermined State"));
    }

    return result;
  }

  @Override
  public String getName() {
    return "FIX_ERROR_PATH";
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

    final SearchResultList availableWork = new SearchResultListJpa();

    final StringBuilder sb = new StringBuilder();

    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    switch (userRole) {
      case LEAD:
        // must have a REVIEW_NEEDED tag with any user
        sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEEDED_*");

        // there must not be an already claimed review record
        sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*" + ")");
        break;
      default:
        throw new Exception(getName()
            + ", findAvailableWork: invalid project role " + userRole);
    }

    // determine the query restrictions, type and pfs parameter
    final List<TrackingRecord> results;
    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    int[] totalCt = new int[1];

    results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
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
        .debug(getName() + ": findAssignedWork for project "
            + mapProject.getName() + " and user " + mapUser.getUserName());

    // instantiate the assigned work search results
    SearchResultList assignedWork = new SearchResultListJpa();

    // build the initial query
    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    // determine the query restrictions, type and pfs parameter
    final String type = pfsParameter.getQueryRestriction() != null
        ? pfsParameter.getQueryRestriction() : "";
    final List<TrackingRecord> results;
    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    pfs.setQueryRestriction(null);
    int[] totalCt = new int[1];

    // switch on user role (Specialist or Lead)
    switch (userRole) {

      // for lead-level work, get assigned fix error reviews
      case LEAD:
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

      // for specialist-level work, get in-editing error reviews
      case SPECIALIST:
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

        // exclude records where review is in progress
        sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*)");
        break;

      default:
        throw new Exception(getName()
            + ", findAvailableWork: invalid project role " + userRole);
    }

    results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
    assignedWork.setTotalCount(totalCt[0]);

    for (TrackingRecord tr : results) {

      MapRecord mapRecord = null;
      for (final MapRecord mr : workflowService
          .getMapRecordsForTrackingRecord(tr)) {
        // ignore REVISION record
        if (mr.getOwner().equals(mapUser)
            && !mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {

          // get highest workflow status record
          if (mapRecord == null || mapRecord.getWorkflowStatus()
              .compareTo(mr.getWorkflowStatus()) < 0) {
            mapRecord = mr;
          }
        }
      }
      SearchResult result = new SearchResultJpa();
      result.setTerminology(mapRecord.getLastModified().toString());
      result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
      result.setTerminologyId(mapRecord.getConceptId());
      result.setValue(mapRecord.getConceptName());
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

    // commonly used variables in switch
    MapRecord revisionRecord = null;
    MapRecord editingRecord = null;
    MapRecord reviewRecord = null;
    MapRecord newRecord = null;

    switch (workflowAction) {
      case ASSIGN_FROM_INITIAL_RECORD:
    
        // case 1 : User claims a PUBLISHED or READY_FOR_PUBLICATION record
        // to
        // fix error on.
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus()
                .equals(WorkflowStatus.READY_FOR_PUBLICATION)) {

          // deep copy the map record
          newRecord = new MapRecordJpa(mapRecord, false);

          // set origin ids
          newRecord.addOrigin(mapRecord.getId());
          newRecord.addOrigins(mapRecord.getOriginIds());

          // set other relevant fields
          newRecord.setOwner(mapUser);
          newRecord.setLastModifiedBy(mapUser);
          newRecord.setWorkflowStatus(WorkflowStatus.NEW);

          // clear the set of records (eliminate the publication-ready record)
          newRecords.clear();

          // add the record to the list
          newRecords.add(newRecord);

          // set the workflow status of the old record to review and add
          // it to
          // new records
          mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
          newRecords.add(mapRecord);

        } else {
          throw new Exception(getName() + ", " + workflowAction
              + ": Attempted to fix errors on non-publication-ready record");
        }
        break;
      case ASSIGN_FROM_SCRATCH:

        newRecord = this.createMapRecordForTrackingRecordAndUser(trackingRecord,
            mapUser);
        newRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);

        for (MapRecord mr : newRecords) {

          // find the map record needing review
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
            newRecord.addOrigin(mr.getId());
          }

          // find the map record needing review
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            newRecord.addOrigin(mr.getId());
          }
        }

        newRecords.add(newRecord);
        break;
      case CANCEL:
        newRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

        // check for the appropriate map records
        for (final MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            reviewRecord = mr;
          }
        }

        // check assumption: user has an assigned record
        if (newRecord == null)
          throw new Exception("Cancel work called for user " + mapUser.getName()
              + " and " + trackingRecord.getTerminology() + " concept "
              + trackingRecord.getTerminologyId()
              + ", but could not retrieve assigned record.");

        // check assumption: a REVIEW record exists
        if (reviewRecord == null)
          throw new Exception("Cancel work called for user " + mapUser.getName()
              + " and " + trackingRecord.getTerminology() + " concept "
              + trackingRecord.getTerminologyId()
              + ", but could not retrieve the previously published or ready-for-publication record.");

        // perform action only if the user's record is NEW
        // if editing has occured (EDITING_IN_PROGRESS or above), null-op
        if (newRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)) {

          newRecords =
              processWorkflowAction(trackingRecord, WorkflowAction.UNASSIGN,
                  mapProject, mapUser, mapRecords, mapRecord);
        }
        break;
      case FINISH_EDITING:
      

        // case 1: A user has finished correcting an error on a previously
        // published record
        // requires a workflow state to exist below that of REVEW_NEW (in
        // this
        // case, NEW, EDITING_IN_PROGRESS
        if (mapRecords.size() == 2) {

          // assumption check: should only be 2 records
          // 1) The original record (now marked REVISION)
          // 2) The modified record (NEW, EDITING_IN_PROGRESS,
          // EDITING_DONE, or
          // REVIEW_NEEDED)
          boolean foundOriginalRecord = false;
          boolean foundModifiedRecord = false;

          for (final MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              foundOriginalRecord = true;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.NEW)
                || mr.getWorkflowStatus()
                    .equals(WorkflowStatus.EDITING_IN_PROGRESS)
                || mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)
                || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
              foundModifiedRecord = true;
          }

          if (!foundOriginalRecord)
            throw new Exception(
                "FIX_ERROR_PATH: Specialist finished work, but could not find previously published record");

          if (!foundModifiedRecord)
            throw new Exception(
                "FIX_ERROR_PATH: Specialist finished work, but could not find their record");

          // cycle over the records
          for (final MapRecord mr : mapRecords) {

            // two records, one marked REVISION, one marked with NEW,
            // EDITING_IN_PROGRESS
            if (!mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
              mr.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);
            }
          }

          // Case 2: A lead has finished reviewing a corrected error
        } else if (mapRecords.size() == 3) {

          // assumption check: should be exactly three records
          // 1) original published record, marked REVISION
          // 2) specialist's record, marked REVIEW_NEEDED
          // 3) lead's record, marked REVIEW_NEW or REVIEW_IN_PROGRESS

          MapRecord originalRecord = null;
          MapRecord modifiedRecord = null;
          MapRecord leadRecord = null;

          for (final MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              originalRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
              modifiedRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
                || mr.getWorkflowStatus()
                    .equals(WorkflowStatus.REVIEW_IN_PROGRESS)
                || mr.getWorkflowStatus()
                    .equals(WorkflowStatus.REVIEW_RESOLVED))
              leadRecord = mr;
          }

          if (originalRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find previously published record");

          if (modifiedRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find the specialist's record record");

          if (leadRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find their record.");

          // set to resolved
          leadRecord.setWorkflowStatus(WorkflowStatus.REVIEW_RESOLVED);

        } else {
          throw new Exception(
              "Unexpected error along FIX_ERROR_PATH, invalid number of records passed in");
        }

        break;
      case PUBLISH:
     
        // Requirements for FIX_ERROR_PATH publish action
        // - 1 record marked REVISION
        // - 1 record marked REVIEW_NEEDED
        // - 1 record marked REVIEW_RESOLVED

        // check assumption: owned record is REVIEW_RESOLVED
        if (!mapRecord.getWorkflowStatus()
            .equals(WorkflowStatus.REVIEW_RESOLVED))
          throw new Exception(
              "Publish called on FIX_ERROR_PATH for map record not marked as REVIEW_RESOLVED (Workflow status found on map record "
                  + mapRecord.getId() + " is " + mapRecord.getWorkflowStatus());

        // check assumption: REVISION and REVIEW_NEEDED records are present
        boolean revisionRecordFound = false;
        boolean reviewNeededRecordFound = false;

        for (final MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            revisionRecordFound = true;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
            reviewNeededRecordFound = true;
        }

        if (!revisionRecordFound)
          throw new Exception(
              "Publish called on FIX_ERROR_PATH, but no REVISION record found");

        if (!reviewNeededRecordFound)
          throw new Exception(
              "Publish called on FIX_ERROR_PATH, but no REVIEW_NEEDED record found");

        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        newRecords.clear();
        newRecords.add(mapRecord);

        break;
      case SAVE_FOR_LATER:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_IN_PROGRESS);

        break;
      case UNASSIGN:
       
        for (final MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            revisionRecord = mr;
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.NEW)
              || mr.getWorkflowStatus()
                  .equals(WorkflowStatus.EDITING_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
            editingRecord = mr;
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
              || mr.getWorkflowStatus()
                  .equals(WorkflowStatus.REVIEW_IN_PROGRESS)
              || mr.getWorkflowStatus()
                  .equals(WorkflowStatus.REVIEW_RESOLVED)) {
            reviewRecord = mr;
          }
        }

        if (revisionRecord == null)
          throw new Exception(
              "Attempted to unassign a published revision record, but no such previously published record exists!");

        // Case 1: A user decides to abandon fixing an error
        if (editingRecord != null && reviewRecord == null) {

          // retrieve the workflow status of the revision record
          WorkflowService workflowService = new WorkflowServiceJpa();
          try {
            MapRecord prevRecord = workflowService
                .getPreviouslyPublishedVersionOfMapRecord(revisionRecord);
            revisionRecord.setWorkflowStatus(prevRecord.getWorkflowStatus());
          } catch (Exception e) {
            throw e;
          } finally {
            workflowService.close();
          }

          newRecords.clear();
          newRecords.add(revisionRecord);

          // Case 2: A lead unassigns themselves from reviewing a fixed
          // error
          // delete the lead's record, no other action required
        } else if (reviewRecord != null) {
          newRecords.remove(reviewRecord);
        } else {
          throw new Exception(
              "Unexpected error attempt to unassign a Revision record.  Contact an administrator.");
        }

        break;
      default:
        throw new Exception(getName() + ", " + workflowAction
            + ": Action not permitted for path");

    }

    return newRecords;

  }

  @Override
  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception {

    MapRecordList originRecords = new MapRecordListJpa();

    boolean revisionFound = false;
    boolean reviewNeededFound = true;

    for (final Long originId : mapRecord.getOriginIds()) {
      MapRecord mr = workflowService.getMapRecord(originId);
      try {
        if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
          revisionFound = true;
          originRecords.addMapRecord(workflowService.getMapRecord(originId));

        }
        if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
          reviewNeededFound = true;
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

    if (originRecords.getCount() == 2) {
      originRecords.setTotalCount(originRecords.getCount());
      if (reviewNeededFound == false) {
        throw new Exception(
            "Could not retrieve record needing review along Fix Error Path");
      }
      if (revisionFound == false) {
        throw new Exception(
            "Could not retrieve original publication-ready record for comparison along Fix Error Path");
      }
      return originRecords;
    } else {
      throw new Exception(
          "Expected two origin records for review but instead found "
              + originRecords.getCount());
    }

  }
}
