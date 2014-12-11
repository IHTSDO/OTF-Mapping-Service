package org.ihtsdo.otf.mapping.services.helpers;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

public interface WorkflowPathHandler {

  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord) throws Exception;
  
  public ValidationResult validateTrackingRecordForActionAndUser(TrackingRecord trackingRecord);
}
