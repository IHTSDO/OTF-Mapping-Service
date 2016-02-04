package org.ihtsdo.otf.mapping.services.helpers;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

// TODO: Auto-generated Javadoc
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
  
  /**
   * Find available work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   */
  public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole, String query, PfsParameter pfsParameter, WorkflowService workflowService ) throws Exception;
  /**
   * Find assigned work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
 * @throws Exception 
   */
  public SearchResultList findAssignedWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole, String query, PfsParameter pfsParameter, WorkflowService workflowService) throws Exception;
  
  
  
  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName();

}
