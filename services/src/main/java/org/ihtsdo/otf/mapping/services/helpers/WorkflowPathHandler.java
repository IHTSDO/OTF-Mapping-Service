package org.ihtsdo.otf.mapping.services.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.Configurable;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * A handler for actions on a workflow path.
 */
public interface WorkflowPathHandler extends Configurable {

  /**
   * Validate tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateTrackingRecord(TrackingRecord trackingRecord)
    throws Exception;

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
   * @param userRole the user role
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param workflowService the workflow service
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
      throws Exception;

  /**
   * Find assigned work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param userRole the user role
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param workflowService the workflow service
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter, WorkflowService workflowService)
      throws Exception;

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName();

  /**
   * Process workflow action.
   *
   * @param trackingRecord the tracking record
   * @param workflowAction the workflow action
   * @param mapProject the map project
   * @param mapUser the map user
   * @param mapRecords the map records
   * @param mapRecord the map record
   * @return the sets the
   * @throws Exception the exception
   */
  public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord,
    WorkflowAction workflowAction, MapProject mapProject, MapUser mapUser,
    Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception;

  /**
   * Indicates whether or not map record in eligible for insertion into workflow
   *
   * @param mapRecord the map record
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isMapRecordInWorkflow(MapRecord mapRecord);

  public MapRecordList getOriginMapRecordsForMapRecord(MapRecord mapRecord,
    WorkflowService workflowService) throws Exception;

}
