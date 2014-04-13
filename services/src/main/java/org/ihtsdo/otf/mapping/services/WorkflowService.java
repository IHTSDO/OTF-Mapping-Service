package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

// TODO: Auto-generated Javadoc
/**
 * Represents a service for answering questions and performing actions
 * related to workflow management.
 */
public interface WorkflowService {

	/**
	 * Add workflow tracking record.
	 *
	 * @param workflowTrackingRecord the workflow tracking record
	 * @return the workflow tracking record
	 * @throws Exception 
	 */
	
	public WorkflowTrackingRecord addWorkflowTrackingRecord(WorkflowTrackingRecord workflowTrackingRecord) throws Exception;
	
	/**
	 * Update workflow tracking record.
	 *
	 * @param workflowTrackingRecord the workflow tracking record
	 * @throws Exception 
	 */
	public void updateWorkflowTrackingRecord(WorkflowTrackingRecord workflowTrackingRecord) throws Exception;
	
	/**
	 * Removes the workflow tracking record.
	 *
	 * @param workflowTrackingRecordId the workflow tracking record id
	 * @throws Exception 
	 */
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId) throws Exception;
	
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
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecordsForMapProject(MapProject mapProject);
	
	/**
	 * Gets the workflow tracking record.
	 *
	 * @param mapProject the map project
	 * @param concept the concept
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 */
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject mapProject, Concept concept);
	
	/**
	 * Search Functions.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the search result list
	 */
	public List<WorkflowTrackingRecord> getAvailableWork(MapProject mapProject, MapUser mapUser);
	
	/**
	 * Find available conflicts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the search result list
	 */
	public List<WorkflowTrackingRecord> getAvailableConflicts(MapProject mapProject, MapUser mapUser);
	
	/**
	 * Find assigned concepts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the search result list
	 */
	public List<MapRecord> getAssignedWork(MapProject mapProject,
			MapUser mapUser);

	
	/**
	 * Find assigned conflicts.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the search result list
	 */
	public List<MapRecord> getAssignedConflicts(MapProject mapProject, MapUser mapUser);
	
	/**
	 * Find available consensus work.
	 *
	 * @param mapProject the map project
	 * @return the search result list
	 */
	public List<WorkflowTrackingRecord> getAvailableConsensusWork(MapProject mapProject);
	
	/**
	 * Called by REST services, performs a specific action given a project, concept, and user
	 * @throws Exception 
	 */
	public void processWorkflowAction(MapProject mapProject, Concept concept, MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction) throws Exception;
	
	/**
	 * Compute workflow.
	 *
	 * @param mapProject the map project
	 * @throws Exception 
	 */
	public void computeWorkflow(MapProject mapProject) throws Exception;
	
	/**
	 * Clear workflow for map project.
	 *
	 * @param mapProject the map project
	 * @throws Exception 
	 */
	public void clearWorkflowForMapProject(MapProject mapProject) throws Exception;
	
	
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
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;
	
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


	
	/* OLDER WORKFLOW SERVICES
	/**
	 * Returns the workflow.
	 *
	 * @param project the project
	 * @return the workflow
	 * @throws Exception the exception
	 *//*
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecords(MapProject project)  throws Exception;

	*//**
	 * Compute workflow.
	 *
	 * @param project the project
	 * @throws Exception the exception
	 *//*
	public void computeWorkflow(MapProject project) throws Exception;

	*//**
	 * Returns the workflow tracking record.
	 *
	 * @param project the project
	 * @param c the c
	 * @return the workflow tracking record
	 * @throws Exception the exception
	 *//*
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject project, Concept c) throws Exception;

	*//**
	 * Removes the workflow tracking record.
	 *
	 * @param workflowTrackingRecordId the workflow tracking record id
	 * @throws Exception the exception
	 *//*
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId) throws Exception;
	
	*//**
	 * Update workflow tracking record.
	 *
	 * @param record the record
	 * @throws Exception the exception
	 *//*
	void updateWorkflowTrackingRecord(WorkflowTrackingRecord record)
			throws Exception;
	
	*//**
	 * Assign user to concept.
	 * @param project the project
	 * @param concept the concept
	 * @param user the user
	 * @return the map record resulting from the assignment
	 *
	 * @throws Exception the exception
	 *//*
	public MapRecord assignUserToConcept(MapProject project, Concept concept, MapUser user) throws Exception;

	*//**
	 * Assign user to concept.
	 * @param project the project
	 * @param concept the concept
	 * @param initialRecord the initial record
	 * @param user the user
	 *
	 * @throws Exception the exception
	 * TODO: if the previous method returns a map record, so should this one
	 *//*
	public void assignUserToConcept(MapProject project, Concept concept, MapRecord initialRecord, MapUser user) throws Exception;

	*//**
	 * Returns the map records assigned to user.
	 *
	 * @param project the project
	 * @param user the user
	 * @return the map records assigned to user
	 * @throws Exception the exception
	 *//*
	public Set<MapRecord> getMapRecordsAssignedToUser(MapProject project, MapUser user) throws Exception;

	*//**
	 * Unassign user from concept.
	 * @param project the project
	 * @param concept the concept
	 * @param user the user
	 *
	 * @throws Exception the exception
	 *//*
	public void unassignUserFromConcept (MapProject project, Concept concept, MapUser user) throws Exception;

	
	*//**
	 * Closes the manager associated with service.
	 *
	 * @throws Exception the exception
	 *//*
	public void close() throws Exception;


	*//**
	 * Gets the transaction per operation.
	 *
	 * @return the transaction per operation
	 * @throws Exception the exception
	 *//*
	public boolean getTransactionPerOperation() throws Exception;
	

	*//**
	 * Sets the transaction per operation.
	 *
	 * @param transactionPerOperation the transaction per operation
	 * @throws Exception the exception
	 *//*
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;
	
	*//**
	 * Begin transaction.
	 *
	 * @throws Exception the exception
	 *//*
	public void beginTransaction() throws Exception;
	
	*//**
	 * Commit.
	 *
	 * @throws Exception the exception
	 *//*
	public void commit() throws Exception;

	*//**
	 * Find available work.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 *//*
	public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser,
			PfsParameter pfsParameter);

	*//**
	 * Gets the available tracking records for workflow and user.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the available tracking records for workflow and user
	 *//*
	public List<WorkflowTrackingRecord> getAvailableTrackingRecordsForProjectAndUser(
			MapProject mapProject, MapUser mapUser);
	

	*//**
	 * Compare finished map records.
	 *
	 * @param mapProject the map project
	 * @return the map
	 * @throws Exception the exception
	 *//*
	public Map<Long, Long> compareFinishedMapRecords(MapProject mapProject) throws Exception;
	
	
	*//**
	 * Gets the tracking records for unmapped in scope concepts.
	 *
	 * @param mapProject the map project
	 * @return the tracking records for unmapped in scope concepts
	 *//*
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts(MapProject mapProject);

	*//**
	 * Gets the tracking records for conflict concepts.
	 *
	 * @param mapProject the map project
	 * @return the tracking records for conflict concepts
	 *//*
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts(
			MapProject mapProject);

	*//**
	 * Find map records assigned to user.
	 *
	 * @param project the project
	 * @param user the user
	 * @param pfsParameter the pfs parameter
	 * @return the sets the
	 *//*
	public SearchResultList findMapRecordsAssignedToUser(MapProject project,
			MapUser user, PfsParameter pfsParameter);

	
	*//**
	 * Gets the available conflict records.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the available conflict records
	 *//*
	public List<WorkflowTrackingRecord> getAvailableConflictRecords(
			MapProject mapProject, MapUser mapUser);

	*//**
	 * Gets the assigned conflict records.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @return the assigned conflict records
	 *//*
	public List<MapRecord> getAssignedConflictRecords(
			MapProject mapProject, MapUser mapUser);*/
}


