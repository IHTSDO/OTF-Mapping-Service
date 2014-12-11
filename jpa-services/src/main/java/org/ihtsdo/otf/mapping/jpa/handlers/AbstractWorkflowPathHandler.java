package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public abstract class AbstractWorkflowPathHandler implements
    WorkflowPathHandler {

  Set<WorkflowStatusCombination> allowableWorkflowStatusCombinations =
      new HashSet<>();

  @Override
  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord)
    throws Exception {

    ValidationResult result = new ValidationResultJpa();

    // extract information from tracking record
    Map<String, WorkflowStatus> usersToWorkflowStatusMap = new HashMap<>();

    String userAndWorkflowStatusPairs =
        trackingRecord.getUserAndWorkflowStatusPairs();
    for (String userAndWorkflowStatusPair : userAndWorkflowStatusPairs
        .split(" ")) {
      int index = userAndWorkflowStatusPair.lastIndexOf("_");
      WorkflowStatus workflowStatus =
          WorkflowStatus.valueOf(userAndWorkflowStatusPair.substring(0,
              index - 1));
      String userName = userAndWorkflowStatusPair.substring(index + 1);
      usersToWorkflowStatusMap.put(userName, workflowStatus);
    }

    // CHECK: Combination of records is valid
    WorkflowStatusCombination workflowStatusCombination =
        new WorkflowStatusCombination(
            (Set<WorkflowStatus>) usersToWorkflowStatusMap.values());
    if (!this.allowableWorkflowStatusCombinations
        .contains(workflowStatusCombination)) {
      result
          .addError("Tracking record has invalid workflow status combination");
    }

    // CHECK: Records exist and are correct for each user-workflow status pair
    MappingService mappingService = new MappingServiceJpa();
    for (Long id : trackingRecord.getMapRecordIds()) {
      MapRecord mr = mappingService.getMapRecord(id);

      if (mr == null) {
        result
            .addError("Tracking record references non-existent map record with id "
                + id);
      } else {

        for (String userName : usersToWorkflowStatusMap.keySet()) {
          if (!userName.equals(mr.getOwner().getUserName())) {
            result
                .addError("Tracking record has incorrect owner for map record with id "
                    + mr.getId());
          } else {
            
          }
        }
      }
    }
    mappingService.close();

    return result;
  }

  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord trackingRecord) {
    // TODO Auto-generated method stub
    return null;
  }

  public Set<WorkflowStatusCombination> getAllowableWorkflowStatusCombinations() {
    return allowableWorkflowStatusCombinations;
  }

  public void setAllowableWorkflowStatusCombinations(
    Set<WorkflowStatusCombination> allowableWorkflowStatusCombinations) {
    this.allowableWorkflowStatusCombinations =
        allowableWorkflowStatusCombinations;
  }
}
