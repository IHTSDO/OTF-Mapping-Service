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
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
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
 * Workflow path handler for "legacy path".
 */
public class WorkflowLegacyPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the Non Legacy Path
  /** The lead finished state. */
  private static WorkflowPathState initialState;

  /** The first specialist editing state. */
  private static WorkflowPathState firstSpecialistEditingState;

  /** The first specialist conflict state */
  private static WorkflowPathState firstSpecialistConflictState;

  /** The second specialist editing state. */
  private static WorkflowPathState secondSpecialistEditingState;

  /** The conflict detected state. */
  private static WorkflowPathState conflictDetectedState;

  /** The lead editing state. */
  private static WorkflowPathState conflictEditingState;

  /** The lead finished state. */
  private static WorkflowPathState conflictFinishedState;

  /**
   * Instantiates an empty {@link WorkflowLegacyPathHandler}.
   */
  public WorkflowLegacyPathHandler() {

    setWorkflowPath(WorkflowPath.LEGACY_PATH);

    setEmptyWorkflowAllowed(false);

    // STATE: Initial initialState has published legacy record
    initialState = new WorkflowPathState("Initial State");
    initialState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION)));
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

    // STATE: Legacy record is in revision, One specialist has claimed work
    firstSpecialistEditingState =
        new WorkflowPathState("First Specialist Editing");
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    firstSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));

    trackingRecordStateToActionMap.put(
        firstSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision, first user has finished work
    firstSpecialistConflictState =
        new WorkflowPathState("Conflict Between Specialist and Legacy");
    firstSpecialistConflictState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.EDITING_DONE)));
    trackingRecordStateToActionMap.put(
        firstSpecialistConflictState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision and in conflict with first
    // specialist's work, second specialist has claimed work
    secondSpecialistEditingState =
        new WorkflowPathState("Second Specialist Editing");
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.EDITING_DONE,
            WorkflowStatus.NEW)));
    secondSpecialistEditingState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.EDITING_DONE,
            WorkflowStatus.EDITING_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        secondSpecialistEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Legacy record is in revision, two specialists have completed
    // work in conflict
    conflictDetectedState =
        new WorkflowPathState("Conflict Detected Between Specialists");
    conflictDetectedState
        .addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
            WorkflowStatus.REVISION, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_DETECTED)));
    trackingRecordStateToActionMap.put(
        conflictDetectedState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // STATE: Lead review (incomplete)
    conflictEditingState =
        new WorkflowPathState("Lead Conflict Review Incomplet)");
    conflictEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_NEW)));
    conflictEditingState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        conflictEditingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // STATE: Lead review (complete)
    conflictFinishedState =
        new WorkflowPathState("Lead Conflict Review Complete");
    conflictFinishedState.addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVISION,
            WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED,
            WorkflowStatus.CONFLICT_RESOLVED)));
    trackingRecordStateToActionMap.put(
        conflictFinishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // Terminal State: No tracking record
  }

  /* see superclass */
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

    // fifth, basic check that workflow state is in the map
    if (!trackingRecordStateToActionMap.containsKey(state)) {
      result.addError("Invalid state/could not determine state");
      return result;
    }

    // sixth, basic check that the workflow action is permitted for this
    // state
    if (!trackingRecordStateToActionMap.get(state).contains(action)) {
      result.addError("Workflow action " + action + " not permitted for state "
          + state.getWorkflowStateName());
      return result;
    }

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////
    if (state == null) {
      result
          .addError("Could not determine workflow path state for tracking record");
    }

    // INITIAL STATE: No specialists have started editing
    // Record requirement : none
    // Permissible actions: ASSIGN_FROM_SCRATCH
    // Minimum role : Specialist
    else if (state.equals(initialState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // check record
      if (currentRecord != null) {
        result.addError("User's record does not meet requirements");
      }

      // check action
      if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action not permitted");
      }

    }

    // STATE: One specialist is editing
    // Record requirement : none
    // Minimum role : Specialist
    else if (state.equals(firstSpecialistEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // check record/action combo
      if (currentRecord == null) {
        result.addError("User's record does not meet requirements");
      }

    }

    // STATE: One specialist has finished editing, conflicts with legacy
    // record
    // Record requirement : none for second user, conflict detected record
    // for first
    // Minimum role : Specialist
    else if (state.equals(firstSpecialistConflictState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      // if user does not have record, only assign from scratch permitted
      if (currentRecord != null) {
        switch (currentRecord.getWorkflowStatus()) {
          case EDITING_DONE:
          case EDITING_IN_PROGRESS:
          case NEW:
            // do nothing, valid
            break;
          default:
            result.addError("User's record has invalid workflow state");
            break;
        }

        if (action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted for this user and state");
        }
      }

      // if user has record, cannot assign from scratch
      if (currentRecord == null
          && !action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
        result.addError("Action is not permitted for this user and state");
      }

    }

    // STATE: One specialist has finished editing, conflicts with legacy
    // record, second specialist is editing
    // Record requirement : record for user must exist in editing
    // Minimum role : Specialist
    else if (state.equals(secondSpecialistEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        result.addError("User does not meet required role");
      }

      if (currentRecord == null) {
        result.addError("User must have a record");
      }

      switch (currentRecord.getWorkflowStatus()) {
        case CONFLICT_DETECTED:
        case EDITING_DONE:
        case EDITING_IN_PROGRESS:
        case NEW:
          // do nothing, valid
          break;
        default:
          result.addError("User's record has invalid workflow state");
          break;
      }

    }

    // STATE: Two specialists have finished editing and their work conflicts
    // (legacy record now disregarded)
    // Record requrement : none for lead review, must exist for specialists
    // Minimum role : Specialist
    else if (state.equals(conflictDetectedState)) {

      // case: lead review assignment
      if (currentRecord == null) {
        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User does not meet required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action not permitted");
        }
      }

      // case: specialist performing further edits
      else {
        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not meet required role");
        }

        // check record
        if (!currentRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED)) {
          result.addError("User's record is not in conflict detection state");
        }

        // check action
        if (action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action not permitted");
        }
      }

    }

    // STATE: Lead is performing review on two specialist records
    // Record requirement : record must exist and be in conflict state
    else if (state.equals(conflictEditingState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not meet required role");
      }

      // check record
      if (!currentRecord.getWorkflowStatus()
          .equals(WorkflowStatus.CONFLICT_NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_IN_PROGRESS)) {
        result.addError("User's record not in correct workflow state");
      }
    }

    // STATE: Lead has finished work and record is ready for publication or
    // further edits
    else if (state.equals(conflictFinishedState)) {
      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
        result.addError("User does not meet required role");
      }

      // check record
      if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.CONFLICT_RESOLVED)) {
        result.addError("User's record not in correct workflow state");
      }
    }

    return result;
  }

  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {
    Logger.getLogger(this.getClass()).debug(
        getName() + ": findAvailableWork for project " + mapProject.getName()
            + " and user " + mapUser.getUserName());

    final SearchResultList availableWork = new SearchResultListJpa();

    final StringBuilder sb = new StringBuilder();

    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:"
        + getName());

    switch (userRole) {

      case LEAD:
        sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_DETECTED_*");
        sb.append(" AND NOT ("
            + "userAndWorkflowStatusPairs:CONFLICT_NEW_* OR "
            + "userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_* OR "
            + "userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");
        break;

      case SPECIALIST:

        sb.append(" AND ("

            // legacy record only (assignedUserCount 1)
            + "assignedUserCount:1 OR"

            // first specialist has finished work (assignedUserCount 2,
            // EDITING_DONE
            // record not owned)
            + " (assignedUserCount:2 AND userAndWorkflowStatusPairs:EDITING_DONE_* AND NOT assignedUserNames:"
            + mapUser.getUserName() + ")"

            + ")");

        // handle team-based assignment
        if (mapProject.isTeamBased() && mapUser.getTeam() != null
            && !mapUser.getTeam().isEmpty()) {
          for (final MapUser user : workflowService.getMapUsersForTeam(
              mapUser.getTeam()).getMapUsers()) {
            sb.append(" AND NOT assignedUserNames:" + user.getUserName());
          }
        }

        break;

      default:
        throw new Exception(getName()
            + ", findAvailableWork: invalid project role " + userRole);

    }

    int[] totalCt = new int[1];
    @SuppressWarnings("unchecked")
    final List<TrackingRecord> results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter,
            totalCt);

    availableWork.setTotalCount(totalCt[0]);
    for (final TrackingRecord tr : results) {
      final SearchResult result = new SearchResultJpa();
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

    Logger.getLogger(this.getClass()).debug(
        getName() + ": findAssignedWork for project " + mapProject.getName()
            + " and user " + mapUser.getUserName());

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
    final String type =
        pfsParameter.getQueryRestriction() != null ? pfsParameter
            .getQueryRestriction() : "";
    final List<TrackingRecord> results;
    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    pfs.setQueryRestriction(null);
    int[] totalCt = new int[1];

    // switch on user role (Specialist or Lead)
    switch (userRole) {

    // for lead-level work, get assigned conflicts
      case LEAD:

        switch (type) {
          case "CONFLICT_NEW":
            sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_NEW_"
                + mapUser.getUserName());
            break;
          case "CONFLICT_IN_PROGRESS":
            sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_"
                + mapUser.getUserName());
            break;
          case "CONFLICT_RESOLVED":
            sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_RESOLVED_"
                + mapUser.getUserName());
            break;
          default:
            sb.append(" AND (userAndWorkflowStatusPairs:CONFLICT_NEW_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_"
                + mapUser.getUserName() + ")");
            break;
        }

        results =
            (List<TrackingRecord>) workflowService.getQueryResults(
                sb.toString(), TrackingRecordJpa.class,
                TrackingRecordJpa.class, pfs, totalCt);
        assignedWork.setTotalCount(totalCt[0]);

        for (final TrackingRecord tr : results) {
          final SearchResult result = new SearchResultJpa();

          final Set<MapRecord> mapRecords =
              workflowService.getMapRecordsForTrackingRecord(tr);

          // get the map record assigned to this user
          MapRecord mapRecord = null;
          for (final MapRecord mr : mapRecords) {
            if (mr.getOwner().equals(mapUser)) {

              // SEE MAP-617:
              // Lower level record may exist with same owner,
              // only add if actually a conflict

              if (mr.getWorkflowStatus().compareTo(
                  WorkflowStatus.CONFLICT_DETECTED) < 0) {
                // do nothing, this is the specialist level work
              } else {
                mapRecord = mr;
              }
            }
          }

          if (mapRecord == null) {
            throw new Exception(
                "Failed to retrieve assigned conflicts:  no map record found for user "
                    + mapUser.getUserName() + " and concept "
                    + tr.getTerminologyId());
          } else {
            result.setTerminologyId(mapRecord.getConceptId());
            result.setValue(mapRecord.getConceptName());
            result.setTerminology(mapRecord.getLastModified().toString());
            result.setTerminologyVersion(mapRecord.getWorkflowStatus()
                .toString());
            result.setId(mapRecord.getId());
            assignedWork.addSearchResult(result);
          }
        }
        break;

      // for specialist-level work, get assigned work
      case SPECIALIST:

        // add the query terms specific to findAssignedWork
        // - user and workflowStatus must exist in a pair of form:
        // workflowStatus_userName, e.g. NEW_dmo or
        // EDITING_IN_PROGRESS_kli
        // - modify search term based on pfs parameter query restriction
        // field
        // * default: NEW, EDITING_IN_PROGRESS,
        // EDITING_DONE/CONFLICT_DETECTED
        // * NEW: NEW
        // * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

        // add terms based on query restriction
        switch (type) {
          case "NEW":
            sb.append(" AND userAndWorkflowStatusPairs:NEW_"
                + mapUser.getUserName());
            break;
          case "EDITING_IN_PROGRESS":
            sb.append(" AND userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                + mapUser.getUserName());
            break;
          case "EDITING_DONE":
            sb.append(" AND (userAndWorkflowStatusPairs:EDITING_DONE_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
                + mapUser.getUserName() + ")");
            break;
          default:
            sb.append(" AND (userAndWorkflowStatusPairs:NEW_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_DONE_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
                + mapUser.getUserName() + ")");
            break;
        }

        // add terms to exclude concepts that a lead has claimed
        sb.append(" AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");

        results =
            (List<TrackingRecord>) workflowService.getQueryResults(
                sb.toString(), TrackingRecordJpa.class,
                TrackingRecordJpa.class, pfs, totalCt);
        assignedWork.setTotalCount(totalCt[0]);

        for (final TrackingRecord tr : results) {

          // instantiate the result list
          final SearchResult result = new SearchResultJpa();

          // get the map records associated with this tracking record
          final Set<MapRecord> mapRecords =
              workflowService.getMapRecordsForTrackingRecord(tr);

          // get the map record assigned to this user
          MapRecord mapRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

          // if no record and no review or conflict work was found,
          // throw error
          if (mapRecord == null) {
            throw new Exception(
                "Failed to retrieve assigned work:  no map record found for user "
                    + mapUser.getUserName() + " and concept "
                    + tr.getTerminologyId());

          } else {

            // create the search result
            result.setTerminologyId(mapRecord.getConceptId());
            result.setValue(mapRecord.getConceptName());
            result.setTerminology(mapRecord.getLastModified().toString());
            result.setTerminologyVersion(mapRecord.getWorkflowStatus()
                .toString());
            result.setId(mapRecord.getId());
            assignedWork.addSearchResult(result);
          }

        }
        break;
      default:
        throw new Exception(
            "Cannot retrieve work for LEGACY_PATH for user role " + userRole);
    }

    return assignedWork;
  }

  @Override
  public String getName() {
    return "LEGACY_PATH";
  }

  @Override
  public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord,
    WorkflowAction workflowAction, MapProject mapProject, MapUser mapUser,
    Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {

    Logger.getLogger(this.getClass()).debug(
        getName() + ": Processing workflow action by " + mapUser.getName()
            + ":  " + workflowAction.toString());

    // the set of records returned after processing
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // declare the mapping service, if needed
    MappingService mappingService = null;

    // the new record to be added, if needed
    MapRecord newRecord = null;

    // extract legacy and user records
    MapRecord legacyRecord = null;
    for (MapRecord mr : newRecords) {
      if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
        legacyRecord = mr;
      }
    }
    MapRecord userRecord =
        getCurrentMapRecordForUserName(newRecords, mapUser.getUserName());

    switch (workflowAction) {

      case ASSIGN_FROM_SCRATCH:
        newRecord =
            this.createMapRecordForTrackingRecordAndUser(trackingRecord,
                mapUser);

        // if legacy + two specialist records, conflict record
        if (newRecords.size() == 3) {
          newRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_NEW);

          // add the origin ids
          for (MapRecord mr : newRecords) {
            newRecord.addOrigin(mr.getId());
          }
        }

        // if legacy + one specialist record, new specialist record
        else if (newRecords.size() == 2) {
          newRecord.setWorkflowStatus(WorkflowStatus.NEW);
        }

        // if legacy record only
        else if (newRecords.size() == 1) {
          newRecords.iterator().next()
              .setWorkflowStatus(WorkflowStatus.REVISION);
          newRecord.setWorkflowStatus(WorkflowStatus.NEW);
        }

        // throw error if does not meet one of these checks
        // NOTE: This is redundant checking covered in validateTrackingRecord
        // methods
        else {
          throw new Exception(
              "Could not assign from scratch, bad workflow status detected "
                  + getWorkflowStatusFromMapRecords(newRecords) + ", from "
                  + newRecords.size() + " records");
        }
        newRecords.add(newRecord);

        break;
      case CANCEL:
        // re-retrieve the records for this tracking record and return those
        // used to ensure no spurious alterations from serialization are
        // saved and therefore reflected in the audit trail

        newRecords.clear();
        mappingService = new MappingServiceJpa();
        for (final Long id : trackingRecord.getMapRecordIds()) {
          newRecords.add(mappingService.getMapRecord(id));
        }
        mappingService.close();
        break;

      case FINISH_EDITING:

        switch (userRecord.getWorkflowStatus()) {

        // lead's record
          case CONFLICT_IN_PROGRESS:
          case CONFLICT_NEW:
          case CONFLICT_RESOLVED:
            mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_RESOLVED);
            break;

          // specialist record
          case CONFLICT_DETECTED:
          case EDITING_DONE:
          case EDITING_IN_PROGRESS:
          case NEW:

            mappingService = new MappingServiceJpa();
            ProjectSpecificAlgorithmHandler handler =
                mappingService.getProjectSpecificAlgorithmHandler(mapProject);
            mappingService.close();

            // find hte other specialist's record
            MapRecord secondSpecialistRecord = null;
            for (MapRecord mr : newRecords) {
              if (!mr.getOwner().getUserName().equals(getLegacyUserName())
                  && !mr.getOwner().equals(mapUser)
                  && mr.getWorkflowStatus().compareTo(
                      WorkflowStatus.CONFLICT_DETECTED) <= 0) {
                secondSpecialistRecord = mr;
              }
            }

            // if no second specialist record exists, perform comparison with
            // legacy record
            if (secondSpecialistRecord == null) {

              ValidationResult validationResult =
                  handler.compareMapRecords(mapRecord, legacyRecord);

              if (validationResult.isValid()) {
                Logger
                    .getLogger(getClass())
                    .debug(
                        "LEGACY_PATH - No conflicts detected between specialist's work and legacy record, marking ready for publication.");

                newRecords.remove(legacyRecord);
                mapRecord
                    .setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
              } else {
                Logger
                    .getLogger(getClass())
                    .debug(
                        "LEGACY_PATH - Conflicts detected between specialist's work and legacy record, marking ready for publication.");
                mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);
              }
            }
            // otherwise, compare two specialist records
            else {
              ValidationResult validationResult =
                  handler.compareMapRecords(userRecord, secondSpecialistRecord);

              if (validationResult.isValid()) {
                Logger
                    .getLogger(getClass())
                    .debug(
                        "LEGACY_PATH - No conflicts detected between two specialist's work, marking ready for publication.");

                newRecords.remove(legacyRecord);
                newRecords.remove(secondSpecialistRecord);
                mapRecord
                    .setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
              } else {
                mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
                secondSpecialistRecord
                    .setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);

              }
            }
            break;

          default:
            throw new Exception("Invalid workflow state on user's map record");

        }
        break;
      case PUBLISH:
        if (!userRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_RESOLVED)) {
          throw new Exception("User's record is not marked conflict resolved");
        } else {
          newRecords = new HashSet<>();
          userRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
          newRecords.add(userRecord);
        }

        break;

      case SAVE_FOR_LATER:

        if (userRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          userRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (userRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW))
          userRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
        break;

      case UNASSIGN:

        // if the legacy record, remove from the workflow entirely
        if (mapUser.getUserName().equals(getLegacyUserName())) {
          newRecords.clear();
        }

        // if lead record, simply remove
        else {
          newRecords.remove(userRecord);
        }
        break;
      default:
        break;

    }

    // double-check that mapping service is closed, if opened
    if (mappingService != null)

    {
      mappingService.close();
    }

    return newRecords;

  }

  /**
   * Returns the legacy user name.
   *
   * @return the legacy user name
   */
  @SuppressWarnings("static-method")
  public String getLegacyUserName() {
    return "legacy";
  }

  @Override
  public boolean isMapRecordInWorkflow(MapRecord mapRecord) {
    return !mapRecord.getWorkflowStatus().equals(
        WorkflowStatus.READY_FOR_PUBLICATION)
        && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED);
  }

  @Override
  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception {

    MapRecordList originRecords = new MapRecordListJpa();

    for (final Long originId : mapRecord.getOriginIds()) {
      MapRecord mr = workflowService.getMapRecord(originId);
      try {
        if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
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
      return originRecords;
    } else {
      throw new Exception(
          "Expected two origin records for review but instead found "
              + originRecords.getCount());
    }

  }

}
