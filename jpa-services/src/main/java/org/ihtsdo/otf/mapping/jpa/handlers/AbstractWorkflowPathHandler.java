package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
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

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractWorkflowPathHandler.
 * 
 * @author ${author}
 */
public abstract class AbstractWorkflowPathHandler implements
    WorkflowPathHandler {

  /** The workflow path */
  private WorkflowPath workflowPath = null;

  /** The workflow combinations. */
  private Set<WorkflowStatusCombination> workflowCombinations = new HashSet<>();

  /** Whether an empty workflow state is allowed. Default: true. */
  boolean emptyWorkflowAllowed = true;

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
   * Returns the workflow combinations.
   * 
   * @return the workflow combinations
   */
  public Set<WorkflowStatusCombination> getWorkflowCombinations() {
    return workflowCombinations;
  }

  /**
   * Sets the workflow combinations.
   * 
   * @param workflowCombinations the workflow combinations
   */
  public void setWorkflowCombinations(
    Set<WorkflowStatusCombination> workflowCombinations) {
    this.workflowCombinations = workflowCombinations;
  }

  /**
   * Adds the workflow combination.
   * 
   * @param workflowCombination the workflow combination
   */
  public void addWorkflowCombination(
    WorkflowStatusCombination workflowCombination) {
    if (this.workflowCombinations == null)
      workflowCombinations = new HashSet<>();
    this.workflowCombinations.add(workflowCombination);
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

    WorkflowStatusCombination workflowCombination =
        this.getWorkflowCombinationForTrackingRecord(trackingRecord);

    // check for empty (allowed) combination
    if (workflowCombination.isEmpty()) {
      if (this.emptyWorkflowAllowed == false) {
        result
            .addError("Empty workflow combination not allowed for this workflow path");
      }

      // otherwise, check whether this combination is allowed
    } else if (!workflowCombinations.contains(workflowCombination)) {
      result
          .addError("Tracking record has invalid combination of reported workflow statuses for "
              + trackingRecord.getWorkflowPath());
    }

    // if invalid, return now
    if (!result.isValid())
      return result;

    // extract the user/workflow pairs
    Set<String> userWorkflowPairs =
        new HashSet<>(Arrays.asList(trackingRecord
            .getUserAndWorkflowStatusPairs().split(" ")));

    // get the map records
    MapRecordList mapRecords =
        this.getMapRecordsForTrackingRecord(trackingRecord);

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
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord trackingRecord, WorkflowAction action, MapUser mapUser)
    throws Exception {

    // This must be overwritten by the individual handlers
    return null;
  }

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

    for (String pair : tr.getUserAndWorkflowStatusPairs().split(" ")) {
      workflowCombination.addWorkflowStatus(WorkflowStatus.valueOf(pair
          .substring(0, pair.lastIndexOf("_"))));
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
    MappingService mappingService = new MappingServiceJpa();
    MapRecordList mapRecords = new MapRecordListJpa();
    for (Long id : tr.getMapRecordIds()) {
      mapRecords.addMapRecord(mappingService.getMapRecord(id));
    }
    mappingService.close();
    return mapRecords;
  }

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

}
