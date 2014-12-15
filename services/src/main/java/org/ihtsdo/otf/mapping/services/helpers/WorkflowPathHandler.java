package org.ihtsdo.otf.mapping.services.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public interface WorkflowPathHandler {

  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord) throws Exception;
  
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord trackingRecord, WorkflowAction action, MapUser mapUser)
    throws Exception;

}
