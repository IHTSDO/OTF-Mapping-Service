package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public class WorkflowReviewProjectPathHandler extends
    AbstractWorkflowPathHandler {

  public WorkflowReviewProjectPathHandler() {

    // empty workflow is allowed for this path
    setEmptyWorkflowAllowed(true);
    
    // initial state:  tracking record, no map records
    // final state:  no tracking record, one map record ready for publication

    // add states representing specialist-level work
    addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.NEW)));
    addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    addWorkflowCombination(new WorkflowStatusCombination(
        Arrays.asList(WorkflowStatus.REVIEW_NEEDED)));

    // add states representing lead-level work
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));
  }

  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord trackingRecord, WorkflowAction action, MapUser mapUser)
    throws Exception {

    // first validate the tracking record itself
    ValidationResult result =
        super.validateTrackingRecordForActionAndUser(trackingRecord, action,
            mapUser);

    // if failed first check, return
    if (!result.isValid())
      return result;

    MappingService mappingService = new MappingServiceJpa();

    // second, get the map records and find the record assigned to this user (if
    // any)
    MapRecordList records = this.getMapRecordsForTrackingRecord(trackingRecord);
    MapRecord assignedRecord =
        this.getCurrentMapRecordForUser(records, mapUser);

    // third, get the role for this user on this project
    MapUserRole role =
        mappingService.getMapUserRoleForMapProject(mapUser.getUserName(),
            trackingRecord.getMapProjectId());

    // switch on requested action
    switch (action) {
      case ASSIGN_FROM_SCRATCH:
        if (records.getCount() == 1 && !role.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User is not a lead");
        } else if (records.getCount() >= 2) {
          result.addError("Too many records, cannot assign");
        }
        break;
      case FINISH_EDITING:
        if (assignedRecord == null) {
          result
          .addError("Cannot retrieve assigned record");
        }
        break;
      case PUBLISH:
        // if no record, return error
        if (!role.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User is not a lead");
        } else if (assignedRecord == null) {
          result
              .addError("Cannot retrieve assigned record");

          // check if workflow status is a lead REVIEW_* record
        } else if (!assignedRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_NEW)
            && !assignedRecord.getWorkflowStatus().equals(
                WorkflowStatus.REVIEW_IN_PROGRESS)
            && !assignedRecord.getWorkflowStatus().equals(
                WorkflowStatus.REVIEW_RESOLVED)) {
          result.addError("Assigned record not eligible");
        }

        break;
      case SAVE_FOR_LATER:
        if (assignedRecord == null) {
          result
              .addError("Cannot retrieve assigned record");
        }
        break;
      case UNASSIGN:
        if (assignedRecord == null) {
          result
              .addError("Cannot retrieve assigned record");
        }
      default:
        result.addError("Illegal operation requested for workflow path");
        break;

    }

    return result;
  }

}
