package org.ihtsdo.otf.mapping.services.helpers;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * A handler for actions on a workflow path.
 */
public interface WorkflowPathHandler {

  /**
   * Validate tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord) throws Exception;
  
  /**
   * Validate tracking record for action and user.
   *
   * @param trackingRecord the tracking record
   * @param action the action
   * @param mapUser the map user
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord trackingRecord, WorkflowAction action, MapUser mapUser)
    throws Exception;

}
