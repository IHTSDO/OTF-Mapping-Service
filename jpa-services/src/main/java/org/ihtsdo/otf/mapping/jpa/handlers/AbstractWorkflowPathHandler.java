package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Abstract implementation of {@link WorkflowPathHandler}.
 * 
 */
public abstract class AbstractWorkflowPathHandler implements
    WorkflowPathHandler {

  /** The workflow path. */
  private WorkflowPath workflowPath = null;

  /** The map of tracking record states to acceptable workflow actions. */
  Map<WorkflowPathState, Set<WorkflowAction>> trackingRecordStateToActionMap =
      new HashMap<>();

  /** Whether an empty workflow state is allowed. Default: true. */
  boolean emptyWorkflowAllowed = true;

  /**
   * Returns the workflow path.
   * 
   * @return the workflow path
   */
  public WorkflowPath getWorkflowPath() {
    return workflowPath;
  }

  /**
   * Sets the workflow path.
   * 
   * @param workflowPath the workflow path
   */
  public void setWorkflowPath(WorkflowPath workflowPath) {
    this.workflowPath = workflowPath;
  }

  /**
   * Indicates whether or not empty workflow allowed is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isEmptyWorkflowAllowed() {
    return emptyWorkflowAllowed;
  }

  /**
   * Sets the empty workflow allowed.
   * 
   * @param emptyWorkflowAllowed the empty workflow allowed
   */
  public void setEmptyWorkflowAllowed(boolean emptyWorkflowAllowed) {
    this.emptyWorkflowAllowed = emptyWorkflowAllowed;
  }

  /**
   * Returns the tracking record state to action map.
   * 
   * @return the tracking record state to action map
   */
  public Map<WorkflowPathState, Set<WorkflowAction>> getTrackingRecordStateToActionMap() {
    return trackingRecordStateToActionMap;
  }

  /**
   * Sets the tracking record state to action map.
   * 
   * @param trackingRecordStateToActionMap the tracking record state to action
   *          map
   */
  public void setTrackingRecordStateToActionMap(
    Map<WorkflowPathState, Set<WorkflowAction>> trackingRecordStateToActionMap) {
    this.trackingRecordStateToActionMap = trackingRecordStateToActionMap;
  }
  
  /**
   * Helper function to return all combinations legal for this workflow.
   *
   * @return the workflow status combinations
   */
  public Set<WorkflowStatusCombination> getWorkflowStatusCombinations() {
    Set<WorkflowStatusCombination> combinations = new HashSet<>();
    for (WorkflowPathState state : this.trackingRecordStateToActionMap.keySet()) {
      combinations.addAll(state.getWorkflowCombinations());
    }
    return combinations;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler#
   * validateTrackingRecord(org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord)
    throws Exception {

    ValidationResult result = new ValidationResultJpa();
    
    if (trackingRecord == null) {
      result.addWarning("Result generated for null tracking record");
      return result;
    }

    WorkflowStatusCombination workflowCombination =
        getWorkflowCombinationForTrackingRecord(trackingRecord);

    // check for empty (allowed) combination
    if (workflowCombination.isEmpty()) {
      if (emptyWorkflowAllowed == false) {
        result
            .addError("Empty workflow combination not allowed for this workflow path");
      }

      // otherwise, check whether this combination is allowed
    } else if (!isWorkflowCombinationInTrackingRecordStates(workflowCombination)) {
      result
          .addError("Tracking record has invalid combination of reported workflow statuses for "
              + trackingRecord.getWorkflowPath() + ": " + workflowCombination.toString());
    }

    // if invalid, return now
    if (!result.isValid())
      return result;

    // extract the user/workflow pairs
    Set<String> userWorkflowPairs = new HashSet<>();
    if (trackingRecord.getUserAndWorkflowStatusPairs() != null)
      userWorkflowPairs.addAll(Arrays.asList(trackingRecord
          .getUserAndWorkflowStatusPairs().split(" ")));

    // get the map records
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

    // cycle over map records and verify each exists on the tracking record
    // pairs
    for (MapRecord mr : mapRecords.getMapRecords()) {
      // construct pair
      String pair =
          mr.getWorkflowStatus().toString() + "_" + mr.getOwner().getUserName();

      // check for pair
      if (!userWorkflowPairs.contains(pair)) {
        result.addError("Referenced map record " + mr.getId() + " for user "
            + mr.getOwner().getUserName() + " is not properly tracked");
      }
    }

    // cycle over pairs and verify each exists in the map record list
    for (String pair : userWorkflowPairs) {

      // get the status and user name
      String workflowStatus = pair.substring(0, pair.lastIndexOf("_"));
      String user = pair.substring(pair.lastIndexOf("_") + 1);

      boolean recordFound = false;
      for (MapRecord mr : mapRecords.getMapRecords()) {
        if (mr.getOwner().getUserName().equals(user)
            && mr.getWorkflowStatus().equals(
                WorkflowStatus.valueOf(workflowStatus))) {
          recordFound = true;
        }
      }

      if (!recordFound) {
        result
            .addError("Tracking record references workflow and user pair not present in tracked records");
      }
    }

    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler#
   * validateTrackingRecordForActionAndUser
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord,
   * org.ihtsdo.otf.mapping.helpers.WorkflowAction,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public ValidationResult validateTrackingRecordForAction(
    TrackingRecord trackingRecord, WorkflowAction action, MapUser mapUser)
    throws Exception {

   return null;
  }

  // //////////////////////////////////////////////
  // UTILITY FUNCTIONS
  // //////////////////////////////////////////////

  /**
   * Returns the workflow combination for tracking record.
   * 
   * @param tr the tr
   * @return the workflow combination for tracking record
   */
  public WorkflowStatusCombination getWorkflowCombinationForTrackingRecord(
    TrackingRecord tr) {
    WorkflowStatusCombination workflowCombination =
        new WorkflowStatusCombination();
    
    if (tr == null) {
      return workflowCombination;
    }

    if (tr.getUserAndWorkflowStatusPairs() != null) {

      for (String pair : tr.getUserAndWorkflowStatusPairs().split(" ")) {
        workflowCombination.addWorkflowStatus(WorkflowStatus.valueOf(pair
            .substring(0, pair.lastIndexOf("_"))));
      }
    }

    return workflowCombination;
  }

  /**
   * Returns the map records for tracking record.
   * 
   * @param tr the tr
   * @return the map records for tracking record
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordsForTrackingRecord(TrackingRecord tr)
    throws Exception {
    MapRecordList mapRecords = new MapRecordListJpa();
    
    if (tr == null)
      return mapRecords;
    
    MappingService mappingService = new MappingServiceJpa();
    
    for (Long id : tr.getMapRecordIds()) {
      mapRecords.addMapRecord(mappingService.getMapRecord(id));
    }
    mappingService.close();
    return mapRecords;
  }

  /**
   * Returns the current map record for user.
   * 
   * @param records the records
   * @param mapUser the map user
   * @return the current map record for user
   */
  public MapRecord getCurrentMapRecordForUser(MapRecordList records,
    MapUser mapUser) {
    MapRecord assignedRecord = null;
    for (MapRecord mr : records.getMapRecords()) {
      if (mr.getOwner().equals(mapUser)) {
        if (assignedRecord == null) {
          assignedRecord = mr;
        } else if (mr.getWorkflowStatus().compareTo(
            assignedRecord.getWorkflowStatus()) > 0)
          assignedRecord = mr;
      }
    }
    return assignedRecord;
  }

  /**
   * Indicates whether or not workflow combination in tracking record states is
   * the case.
   * 
   * @param workflowCombination the workflow combination
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isWorkflowCombinationInTrackingRecordStates(
    WorkflowStatusCombination workflowCombination) {

    for (WorkflowPathState state : trackingRecordStateToActionMap.keySet()) {
      if (state.contains(workflowCombination))
        return true;
    }

    return false;

  }

  /**
   * Returns the workflow state for workflow combination.
   * 
   * @param workflowCombination the workflow combination
   * @return the workflow state for workflow combination
   */
  public WorkflowPathState getWorkflowStateForWorkflowCombination(
    WorkflowStatusCombination workflowCombination) {
    for (WorkflowPathState state : trackingRecordStateToActionMap.keySet()) {
      if (state.contains(workflowCombination))
        return state;
    }

    return null;
  }

  /**
   * Returns the workflow state for tracking record.
   * 
   * @param trackingRecord the tracking record
   * @return the workflow state for tracking record
   */
  public WorkflowPathState getWorkflowStateForTrackingRecord(
    TrackingRecord trackingRecord) {
    return getWorkflowStateForWorkflowCombination(getWorkflowCombinationForTrackingRecord(trackingRecord));
  }
  
  /**
   * Returns the workflow state for a given workflow combination.
   *
   * @param combination the combination
   * @return the workflow path state for workflow status combination
   */
  public WorkflowPathState getWorkflowPathStateForWorkflowStatusCombination(WorkflowStatusCombination combination) {
    for (WorkflowPathState state : trackingRecordStateToActionMap.keySet()) {
      if (state.contains(combination))
        return state;
    }
    return null;
  }
}
