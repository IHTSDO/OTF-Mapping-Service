package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public class WorkflowReviewProjectPathHandler extends
    AbstractWorkflowPathHandler {

  // constructor defines the tracking record states that exist for this workflow
  public WorkflowReviewProjectPathHandler() {

    // empty workflow is allowed for this path
    setEmptyWorkflowAllowed(true);

    // initial state: tracking record, no map records
    // final state: no tracking record, one map record ready for publication

    // state: SPECIALIST_WORK
    WorkflowState state = new WorkflowState("Specialist Work");

    // add states representing specialist-level work
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.EDITING_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays
        .asList(WorkflowStatus.REVIEW_NEEDED)));

    workflowStates.add(state);

    // add states representing lead-level work
    state = new WorkflowState("Lead Work");

    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_NEW)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_IN_PROGRESS)));
    state.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(
        WorkflowStatus.REVIEW_NEEDED, WorkflowStatus.REVIEW_RESOLVED)));

    workflowStates.add(state);
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

    /*
     * // add states representing specialist-level work
     * addWorkflowCombination(new WorkflowStatusCombination(
     * Arrays.asList(WorkflowStatus.NEW))); addWorkflowCombination(new
     * WorkflowStatusCombination(
     * Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
     * addWorkflowCombination(new WorkflowStatusCombination(
     * Arrays.asList(WorkflowStatus.REVIEW_NEEDED)));
     * 
     * // add states representing lead-level work addWorkflowCombination(new
     * WorkflowStatusCombination(Arrays.asList( WorkflowStatus.REVIEW_NEEDED,
     * WorkflowStatus.REVIEW_NEW))); addWorkflowCombination(new
     * WorkflowStatusCombination(Arrays.asList( WorkflowStatus.REVIEW_NEEDED,
     * WorkflowStatus.REVIEW_IN_PROGRESS))); addWorkflowCombination(new
     * WorkflowStatusCombination(Arrays.asList( WorkflowStatus.REVIEW_NEEDED,
     * WorkflowStatus.REVIEW_RESOLVED)));
     */

    WorkflowStatusCombination workflowCombination =
        this.getWorkflowCombinationForTrackingRecord(trackingRecord);
    WorkflowState workflowState =
        this.getWorkflowStateForWorkflowCombination(workflowCombination);

    switch (workflowState.getWorkflowStateName()) {
      case "Specialist Work":
        break;
      case "Lead Work":
        break;
      default:
        break;
    }

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
          result.addError("Cannot retrieve assigned record");
        }
        break;
      case PUBLISH:
        // if no record, return error
        if (!role.hasPrivilegesOf(MapUserRole.LEAD)) {
          result.addError("User is not a lead");
        } else if (assignedRecord == null) {
          result.addError("Cannot retrieve assigned record");

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
          result.addError("Cannot retrieve assigned record");
        }
        break;
      case UNASSIGN:
        if (assignedRecord == null) {
          result.addError("Cannot retrieve assigned record");
        } else if (records.getCount() == 2
            && (assignedRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
                || assignedRecord.getWorkflowStatus().equals(
                    WorkflowStatus.EDITING_IN_PROGRESS) || assignedRecord
                .getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))) {
          result
              .addError("Cannot unassign specialist-level work once a lead has begun review");
        }
      default:
        result.addError("Illegal operation requested for workflow path");
        break;

    }

    return result;
  }
}
