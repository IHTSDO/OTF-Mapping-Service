package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

// TODO: Auto-generated Javadoc
/**
 * Represents a service for answering questions and performing actions related
 * to workflow management.
 */
public interface WorkflowService {

  /**
   * Gets the workflow tracking record.
   * 
   * @param mapProject the map project
   * @param concept the concept
   * @return the workflow tracking record
   */
  public WorkflowTrackingRecord getWorkflowTrackingRecord(
    MapProject mapProject, Concept concept);

  /**
   * Gets the workflow tracking records.
   * 
   * @return the workflow tracking records
   */
  public List<WorkflowTrackingRecord> getWorkflowTrackingRecords();

  /**
   * Gets the workflow tracking records for map project.
   * 
   * @param mapProject the map project
   * @return the workflow tracking records for map project
   */
  public List<WorkflowTrackingRecord> getWorkflowTrackingRecordsForMapProject(
    MapProject mapProject);

  /**
   * Adds the workflow tracking record.
   * 
   * @param workflowTrackingRecord the workflow tracking record
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public WorkflowTrackingRecord addWorkflowTrackingRecord(
    WorkflowTrackingRecord workflowTrackingRecord) throws Exception;

  /**
   * Update workflow tracking record.
   * 
   * @param workflowTrackingRecord the workflow tracking record
   * @throws Exception the exception
   */
  public void updateWorkflowTrackingRecord(
    WorkflowTrackingRecord workflowTrackingRecord) throws Exception;

  /**
   * Removes the workflow tracking record.
   * 
   * @param workflowTrackingRecordId the workflow tracking record id
   * @throws Exception the exception
   */
  public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId)
    throws Exception;

  /**
   * Search Functions.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, PfsParameter pfsParameter) throws Exception;

  /**
   * Find available conflicts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableConflicts(MapProject mapProject,
    MapUser mapUser, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned concepts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned conflicts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedConflicts(MapProject mapProject,
    MapUser mapUser, PfsParameter pfsParameter) throws Exception;

  /**
   * Find available consensus work.
   *
   * @param mapProject the map project
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableConsensusWork(MapProject mapProject,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Called by REST services, performs a specific action given a project,
   * concept, and user.
   * 
   * @param mapProject the map project
   * @param concept the concept
   * @param mapUser the map user
   * @param mapRecord the map record
   * @param workflowAction the workflow action
   * @throws Exception the exception
   */
  public void processWorkflowAction(MapProject mapProject, Concept concept,
    MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction)
    throws Exception;

  /**
   * Synchronize workflow tracking record given the new version and the old
   * version.
   *
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @return the sets the
   * @throws Exception the exception
   */


  Set<MapRecord> synchronizeMapRecords(WorkflowTrackingRecord trackingRecord,
		Set<MapRecord> mapRecords) throws Exception;


  /**
   * Compute workflow.
   * 
   * @param mapProject the map project
   * @throws Exception the exception
   */
  public void computeWorkflow(MapProject mapProject) throws Exception;

  /**
   * Clear workflow for map project.
   * 
   * @param mapProject the map project
   * @throws Exception the exception
   */
  public void clearWorkflowForMapProject(MapProject mapProject)
    throws Exception;
  
  /**
   * Gets the lowest workflow status from map records.
   *
   * @param mapRecords the map records
   * @return the lowest workflow status from map records
   */
  public WorkflowStatus getLowestWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords);

  /**
   * Gets the workflow status from map records.
   *
   * @param mapRecords the map records
   * @return the workflow status from map records
   */
  public WorkflowStatus getWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords);

  /**
   * Gets the map users from map records.
   *
   * @param mapRecords the map records
   * @return the map users from map records
   */
  public Set<MapUser> getMapUsersFromMapRecords(Set<MapRecord> mapRecords);

  /**
   * Gets the lowest workflow status for workflow tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the lowest workflow status for workflow tracking record
   * @throws Exception the exception
   */
  public WorkflowStatus getLowestWorkflowStatusForWorkflowTrackingRecord(
		  WorkflowTrackingRecord trackingRecord) throws Exception;

  /**
   * Gets the workflow status for workflow tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the workflow status for workflow tracking record
   * @throws Exception the exception
   */
  public WorkflowStatus getWorkflowStatusForWorkflowTrackingRecord(
		  WorkflowTrackingRecord trackingRecord) throws Exception;

  /**
   * Gets the map users for workflow tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the map users for workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapUser> getMapUsersForWorkflowTrackingRecord(
		  WorkflowTrackingRecord trackingRecord) throws Exception;

  /**
   * Gets the map records for workflow tracking record.
   *
   * @param trackingRecord the tracking record
   * @return the map records for workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> getMapRecordsForWorkflowTrackingRecord(
		  WorkflowTrackingRecord trackingRecord) throws Exception;


  /**
   * Closes the manager associated with service.
   * 
   * @throws Exception the exception
   */
  public void close() throws Exception;

  /**
   * Gets the transaction per operation.
   * 
   * @return the transaction per operation
   * @throws Exception the exception
   */
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   * 
   * @param transactionPerOperation the transaction per operation
   * @throws Exception the exception
   */
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Begin transaction.
   * 
   * @throws Exception the exception
   */
  public void beginTransaction() throws Exception;

  /**
   * Commit.
   * 
   * @throws Exception the exception
   */
  public void commit() throws Exception;

  /**
   * Generate random conflict data.
   *
   * @param mapProject the map project
   * @param numDesiredConflicts the num desired conflicts
   * @throws Exception the exception
   */
  public void generateRandomConflictData(MapProject mapProject, int numDesiredConflicts)
		throws Exception;

  

}
