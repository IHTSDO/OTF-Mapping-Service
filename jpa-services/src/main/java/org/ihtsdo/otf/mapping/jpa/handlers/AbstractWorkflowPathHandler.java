package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
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

  /** The workflow combinations. */
  private Set<WorkflowStatusCombination> workflowCombinations = new HashSet<>();

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

    // check the workflow combination
    if (!workflowCombinations.contains(this
        .getWorkflowCombinationForTrackingRecord(trackingRecord))) {
      result
          .addError("Tracking record has invalid combination of reported workflow statuses for "
              + trackingRecord.getWorkflowPath());
    }

    // extract the user/workflow pairs
    Set<String> userWorkflowPairs =
        new HashSet<>(Arrays.asList(trackingRecord
            .getUserAndWorkflowStatusPairs().split(" ")));

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

  private WorkflowStatusCombination getWorkflowCombinationForTrackingRecord(
    TrackingRecord tr) {
    WorkflowStatusCombination workflowCombination =
        new WorkflowStatusCombination();

    for (String pair : tr.getUserAndWorkflowStatusPairs().split(" ")) {
      workflowCombination.addWorkflowStatus(WorkflowStatus.valueOf(pair
          .substring(0, pair.lastIndexOf("_"))));
    }

    return workflowCombination;
  }

  private MapRecordList getMapRecordsForTrackingRecord(TrackingRecord tr)
    throws Exception {
    MappingService mappingService = new MappingServiceJpa();
    MapRecordList mapRecords = new MapRecordListJpa();
    for (Long id : tr.getMapRecordIds()) {
      mapRecords.addMapRecord(mappingService.getMapRecord(id));
    }
    mappingService.close();
    return mapRecords;
  }

}
