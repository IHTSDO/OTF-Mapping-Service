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

// TODO: Auto-generated Javadoc
/**
 * Workflow path handler for "qa path".
 */
public class WorkflowQaPathHandler extends AbstractWorkflowPathHandler {

  // The workflow path states defining the QA Path Workflow
  /** The finished state. */
  private static WorkflowPathState qaNeededState;

  /** The editing state. */
  private static WorkflowPathState editingState;

  /** The finished state. */
  private static WorkflowPathState finishedState;

  /**
   * Instantiates an empty {@link WorkflowQaPathHandler}.
   */
  public WorkflowQaPathHandler() {

    setWorkflowPath(WorkflowPath.QA_PATH);

    setEmptyWorkflowAllowed(true);

    // STATE: Initial state has no tracking record
    // Only valid action is CREATE_QA_RECORD

    // workflow states representing a record marked for qa and the original
    // published record
    qaNeededState = new WorkflowPathState("QA_NEEDED");
    qaNeededState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED)));
    trackingRecordStateToActionMap.put(
        qaNeededState,
        new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH,
            WorkflowAction.UNASSIGN)));

    // workflow states representing record marked for revision, specialist
    // work,
    // and lead QA (incomplete)
    editingState = new WorkflowPathState("QA_NEW/QA_IN_PROGRESS");
    editingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_NEW)));
    editingState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_IN_PROGRESS)));
    trackingRecordStateToActionMap.put(
        editingState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

    // workflow finishedStates representing record marked for revision,
    // specialist work,
    // and lead QA (complete)
    finishedState = new WorkflowPathState("QA_RESOLVED");
    finishedState.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED,
            WorkflowStatus.QA_RESOLVED)));
    trackingRecordStateToActionMap.put(
        finishedState,
        new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
            WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER,
            WorkflowAction.UNASSIGN)));

    // final state: no tracking record, one READY_FOR_PUBLICATION record
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler#
   * validateTrackingRecordForActionAndUser(org.ihtsdo.otf.mapping.workflow.
   * TrackingRecord, org.ihtsdo.otf.mapping.helpers.WorkflowAction,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  /* see superclass */
  /* see superclass */
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

    // check for CANCEL action -- always valid for this path for any
    // state or user (no-op)
    if (action.equals(WorkflowAction.CANCEL)) {
      return result;
    }

    // check for CREATE_QA_RECORD action -- this is always valid, as it
    // merely
    // applies labels to concepts that are already in the workflow
    if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {
      return result;
    }

    // get the user role for this map project
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole userRole =
        mappingService.getMapUserRoleForMapProject(user.getUserName(),
            tr.getMapProjectId());
    mappingService.close();

    // get the map records and workflow path state from the tracking
    // record
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
    MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
    WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////
    if (state == null) {
      result
          .addError("Could not determine workflow path state for tracking record");
    }

    else if (state.equals(qaNeededState)) {

      // check record -- null means none assigned
      if (currentRecord == null) {

        // check role
        if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          result.addError("User does not have required role");
        }

        // check action
        if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
          result.addError("Action is not permitted.");
        }
      } else {

        // check role
        // qa user does not have role requirements for unassign

        // check action
        if (!action.equals(WorkflowAction.UNASSIGN)) {
          result.addError("Action is not permitted.");
        }

      }

      // STATE: Specialist level work
      // Record requirement : NEW, EDITING_IN_PROGRESS
      // Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
      // Minimum role : Specialist
    } else if (state.equals(editingState)) {

      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.QA_NEW)
          && !currentRecord.getWorkflowStatus().equals(
              WorkflowStatus.QA_IN_PROGRESS)) {
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

    } else if (state.equals(finishedState)) {
      // check record
      if (currentRecord == null) {
        result.addError("User must have a record");
      } else if (!currentRecord.getWorkflowStatus().equals(
          WorkflowStatus.QA_RESOLVED)) {
        result.addError("User's record does meet requirements");
      }

      // check role
      if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
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
      result.addError("Error occured in workflow state "
          + state.getWorkflowStateName());
    }

    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler#getName()
   */
  /* see superclass */
  @Override
  public String getName() {
    return "QA_PATH";
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
    throws Exception {
    Logger.getLogger(this.getClass()).info(
        getName() + ": findAvailableWork for project " + mapProject.getName()
            + " and user " + mapUser.getUserName());

    SearchResultList availableWork = new SearchResultListJpa();

    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("mapProjectId:" + mapProject.getId()
        + " AND workflowPath:QA_PATH");

    // add the query terms specific to findAvailableReviewWork
    // - a user (any) and workflowStatus pair of QA_NEEDED_userName
    // exists
    // - the QA_NEEDED pair is not for this user (i.e. user can't review
    // their own work, UNLESS there is only one lead on the project
    // - user and workflowStatus pairs of
    // CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

    // must have a QA_NEEDED tag with any user
    sb.append(" AND userAndWorkflowStatusPairs:QA_NEEDED_*");

    // there must not be an already claimed review record
    sb.append(" AND NOT (userAndWorkflowStatusPairs:QA_NEW_*"
        + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_*"
        + " OR userAndWorkflowStatusPairs:QA_RESOLVED_*" + ")");

    int[] totalCt = new int[1];
    final List<TrackingRecord> results =
        (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
            TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter,
            totalCt);
    availableWork.setTotalCount(totalCt[0]);

    for (final TrackingRecord tr : results) {
      final SearchResult result = new SearchResultJpa();
      final Set<MapRecord> mapRecords =
          workflowService.getMapRecordsForTrackingRecord(tr);

      StringBuffer labelBuffer = new StringBuffer();
      for (final MapRecord mr : mapRecords) {

        // extract all labels for this tracking record
        for (final String label : mr.getLabels()) {
          if (labelBuffer.indexOf(label) == -1)
            labelBuffer.append(";").append(label);
        }
      }

      // construct the search result
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      result.setValue2(labelBuffer.toString());
      availableWork.addSearchResult(result);
    }
    return availableWork;

  }

  /* see superclass */
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

    final String type =
        pfsParameter.getQueryRestriction() != null ? pfsParameter
            .getQueryRestriction() : "";

    if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
      throw new Exception("Specialist role or above required for QA Work");
    }

    // add terms based on query restriction
    switch (type) {
      case "QA_NEW":
        sb.append(" AND userAndWorkflowStatusPairs:QA_NEW_"
            + mapUser.getUserName());

        break;
      case "QA_IN_PROGRESS":
        sb.append(" AND userAndWorkflowStatusPairs:QA_IN_PROGRESS_"
            + mapUser.getUserName());
        break;
      case "QA_RESOLVED":
        sb.append(" AND userAndWorkflowStatusPairs:QA_RESOLVED_"
            + mapUser.getUserName());
        break;
      default:
        sb.append(" AND (userAndWorkflowStatusPairs:QA_NEW_"
            + mapUser.getUserName()
            + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_"
            + mapUser.getUserName()
            + " OR userAndWorkflowStatusPairs:QA_RESOLVED_"
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

      // get the map record assigned to this user -- note: can only be one
      // if QA Path properly used
      MapRecord mapRecord = null;
      StringBuffer labelBuffer = new StringBuffer();
      for (final MapRecord mr : mapRecords) {
        if (mr.getOwner().equals(mapUser)) {
          mapRecord = mr;
        }

        // extract all labels for this tracking record
        for (final String label : mr.getLabels()) {
          if (labelBuffer.indexOf(label) == -1)
            labelBuffer.append(";").append(label);
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
      result.setValue2(labelBuffer.toString());
      result.setTerminology(mapRecord.getLastModified().toString());
      result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
      result.setId(mapRecord.getId());
      assignedWork.addSearchResult(result);
    }
    return assignedWork;
  }

  /* see superclass */
  @Override
  public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord,
    WorkflowAction workflowAction, MapProject mapProject, MapUser mapUser,
    Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {

    Logger.getLogger(this.getClass()).info(
        getName() + ": Processing workflow action by " + mapUser.getName()
            + ":  " + workflowAction.toString());

    // the set of records returned after processing
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    switch (workflowAction) {
      case CREATE_QA_RECORD:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "assignFromInitialRecord:  QA_PATH");

        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.READY_FOR_PUBLICATION)) {

          // deep copy the map record
          MapRecord newRecord = new MapRecordJpa(mapRecord, false);

          // set origin ids
          newRecord.addOrigin(mapRecord.getId());
          newRecord.addOrigins(mapRecord.getOriginIds());

          // set other relevant fields
          // get QA User MapUser
          newRecord.setOwner(mapUser);
          newRecord.setLastModifiedBy(mapUser);
          newRecord.setWorkflowStatus(WorkflowStatus.QA_NEEDED);

          // add the record to the list
          newRecords.add(newRecord);

          // set the workflow status of the old record to review and add
          // it to
          // new records
          mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
          newRecords.add(mapRecord);
        } else {
          throw new Exception(getName() + ", " + workflowAction
              + ": Cannot QA non-publication-ready record");
        }
        break;
      case ASSIGN_FROM_SCRATCH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Assigning concept along QA_PATH");

        MapRecord qaRecord =
            createMapRecordForTrackingRecordAndUser(trackingRecord, mapUser);

        // set workflow status to QA_NEW
        qaRecord.setWorkflowStatus(WorkflowStatus.QA_NEW);

        // set origin id and copy labels
        for (MapRecord record : mapRecords) {

          // copy from the QA_NEEDED record
          if (record.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
            qaRecord.setLabels(record.getLabels());
            qaRecord.addOrigin(record.getId());
          }
        }

        // add the users QA record to the set
        newRecords.add(qaRecord);

        break;
      case CANCEL:
        newRecords.clear();
        MappingService mappingService = new MappingServiceJpa();
        for (final Long id : trackingRecord.getMapRecordIds()) {
          newRecords.add(mappingService.getMapRecord(id));
        }
        mappingService.close();
        break;

      case FINISH_EDITING:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "QA_PATH");

        // a lead has finished reviewing a QA
        if (newRecords.size() == 3) {

          // assumption check: should be exactly three records
          // 1) original published record, marked REVISION
          // 2) specialist (QA) record, marked QA_NEEDED
          // 3) lead's record, marked QA_NEW or QA_IN_PROGRESS

          MapRecord originalRecord = null;
          MapRecord modifiedRecord = null;
          MapRecord leadRecord = null;

          for (MapRecord mr : newRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              originalRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED))
              modifiedRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
                || mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
                || mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED))
              leadRecord = mr;
          }

          if (originalRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find previously published record");

          if (modifiedRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find the specialist's (QA) record");

          if (leadRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find their record.");

          leadRecord.setWorkflowStatus(WorkflowStatus.QA_RESOLVED);

        } else {
          throw new Exception(
              "Unexpected error along QA_PATH, invalid number of records passed in");
        }
        break;
      case PUBLISH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "QA_PATH - Called Publish on resolved qa");

        // Requirements for QA_PATH publish action
        // - 1 record marked REVISION
        // - 1 record marked QA_NEEDED
        // - 1 record marked QA_RESOLVED

        // check assumption: owned record is QA_RESOLVED
        if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED))
          throw new Exception(
              "Publish called on QA_PATH for map record not marked as QA_RESOLVED");

        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        newRecords.clear();
        newRecords.add(mapRecord);
        break;
      case SAVE_FOR_LATER:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.QA_IN_PROGRESS);

        break;
      case UNASSIGN:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Unassign:  QA_PATH");

        MapRecord revisionRecord = null;
        MapRecord qaNeededRecord = null;
        MapRecord editRecord = null;
        for (MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            revisionRecord = mr;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED))
            qaNeededRecord = mr;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW))
            editRecord = mr;
        }

        if (revisionRecord == null)
          throw new Exception(
              "Attempted to unassign a published revision record, but no such previously published record exists!");

        // Case 1: A lead unassigns themselves from reviewing a fixed
        // error, delete the lead's record, no other action required
        if (editRecord != null) {
          newRecords.remove(editRecord);

        }
        // Case 2: The concept is removed from QA, and unassigned from
        // the qa user
        else if (qaNeededRecord != null) {

          // clear the record set
          newRecords.clear();

          WorkflowService workflowService = new WorkflowServiceJpa();

          try {

            // get the previously published version of the revision
            // record
            revisionRecord.setWorkflowStatus(workflowService
                .getPreviouslyPublishedVersionOfMapRecord(revisionRecord)
                .getWorkflowStatus());

            // remove all records and re-add the revision record
            newRecords.clear();
            newRecords.add(revisionRecord);

          } catch (Exception e) {
            throw e;
          } finally {
            workflowService.close();
          }
        } else {

          throw new Exception(
              "Unexpected error attempt to unassign a QA record.  Contact an administrator.");
        }

        break;
      default:
        throw new Exception(getName() + ": Unexpected workfow action "
            + workflowAction);
    }

    return newRecords;
  }
  
  @Override
  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception {

    MapRecordList originRecords = new MapRecordListJpa();

   
    boolean qaNeededFound = true;

    for (final Long originId : mapRecord.getOriginIds()) {
      MapRecord mr = workflowService.getMapRecord(originId);
      try {
     
        if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
         qaNeededFound = true;
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
      if (qaNeededFound == false) {
        throw new Exception(
            "Could not retrieve record needing review along QA Path");
      }
   
      return originRecords;
    } else {
      throw new Exception(
          "Expected two origin records for review but instead found "
              + originRecords.getCount());
    }
  }

}
