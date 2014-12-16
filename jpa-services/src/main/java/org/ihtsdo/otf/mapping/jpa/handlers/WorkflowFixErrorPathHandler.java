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

public class WorkflowFixErrorPathHandler extends AbstractWorkflowPathHandler {

  public WorkflowFixErrorPathHandler() {

    // empty workflow is not permitted
    this.setEmptyWorkflowAllowed(false);

    // workflow states representing a record marked for revision and the
    // specialist-level editing
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.NEW)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.EDITING_IN_PROGRESS)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED)));

    // workflow states representing record marked for revision, specialist work,
    // and lead review
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_NEW)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_IN_PROGRESS)));
    addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVISION, WorkflowStatus.REVIEW_NEEDED,
        WorkflowStatus.REVIEW_RESOLVED)));
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

    // automatically add error if not specialist or above
    if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
      result.addError("User is not a specialist or above");
      return result;
    }

    // switch on requested action
    switch (action) {
      case ASSIGN_FROM_SCRATCH:
        if (records.getCount() != 2) {
          result.addError("Required records not found");
        }

        if (role.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User is not a lead or above");
        }
        break;
      case FINISH_EDITING:
        if (records.getCount() < 2) {
          result.addError("Could not retrieve required records");
        } else if (records.getCount() == 2) {
          if (!assignedRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.EDITING_IN_PROGRESS)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_NEEDED)) {

            result.addError("Could not retrieve eligible record");
          }
        } else if (records.getCount() == 3) {
          if (!assignedRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_IN_PROGRESS)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_RESOLVED)) {

            result.addError("Could not retrieve eligible record");
          }
        } else {
          result.addError("Extraneous records retrieved");
        }
        break;
      case PUBLISH:
        if (records.getCount() < 2) {
          result.addError("Could not retrieve required records");
        } else if (records.getCount() == 2) {
          if (!assignedRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.EDITING_IN_PROGRESS)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_NEEDED)) {

            result.addError("Could not retrieve eligible record");
          }
        } else if (records.getCount() == 3) {
          if (!assignedRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_IN_PROGRESS)
              || !assignedRecord.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_RESOLVED)) {

            result.addError("Could not retrieve eligible record");
          }
          if (role.hasPrivilegesOf(MapUserRole.LEAD)) {
            result.addError("User is not a lead");
          }
        } else {
          result.addError("Extraneous records retrieved");
        }
        break;
      case SAVE_FOR_LATER:
        if (assignedRecord == null) {
          result.addError("Cannot retrieve assigned record");
        }
        break;
      case UNASSIGN:
        if (assignedRecord == null) {
          result.addError("Cannot retrieve assigned record");
        } else if (records.getCount() == 3
            && (assignedRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
                || assignedRecord.getWorkflowStatus().equals(
                    WorkflowStatus.EDITING_IN_PROGRESS) || assignedRecord
                .getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))) {
          result
              .addError("Cannot unassign specialist-level work once a lead has begun review");
        }
        break;
      default:
        break;
    }
    return result;
  }

}
