package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
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
	 * Returns the workflow.
	 *
	 * @param project the project
	 * @return the workflow
	 * @throws Exception the exception
	 */
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecords(MapProject project)  throws Exception;

	/**
	 * Compute workflow.
	 *
	 * @param project the project
	 * @throws Exception the exception
	 */
	public void computeWorkflow(MapProject project) throws Exception;

	/**
	 * Returns the workflow tracking record.
	 *
	 * @param project the project
	 * @param c the c
	 * @return the workflow tracking record
	 * @throws Exception the exception
	 */
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject project, Concept c) throws Exception;

	/**
	 * Removes the workflow tracking record.
	 *
	 * @param workflowTrackingRecordId the workflow tracking record id
	 * @throws Exception the exception
	 */
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId) throws Exception;
	
	/**
	 * Update workflow tracking record.
	 *
	 * @param record the record
	 * @throws Exception the exception
	 */
	void updateWorkflowTrackingRecord(WorkflowTrackingRecord record)
			throws Exception;
	
	/**
	 * Assign user to concept.
	 * @param project the project
	 * @param concept the concept
	 * @param user the user
	 * @return the map record resulting from the assignment
	 *
	 * @throws Exception the exception
	 */
	public MapRecord assignUserToConcept(MapProject project, Concept concept, MapUser user) throws Exception;

	/**
	 * Assign user to concept.
	 * @param project the project
	 * @param concept the concept
	 * @param initialRecord the initial record
	 * @param user the user
	 *
	 * @throws Exception the exception
	 * TODO: if the previous method returns a map record, so should this one
	 */
	public void assignUserToConcept(MapProject project, Concept concept, MapRecord initialRecord, MapUser user) throws Exception;

	/**
	 * Returns the map records assigned to user.
	 *
	 * @param project the project
	 * @param user the user
	 * @return the map records assigned to user
	 * @throws Exception the exception
	 */
	public Set<MapRecord> getMapRecordsAssignedToUser(MapProject project, MapUser user) throws Exception;

	/**
	 * Unassign user from concept.
	 * @param project the project
	 * @param concept the concept
	 * @param user the user
	 *
	 * @throws Exception the exception
	 */
	public void unassignUserFromConcept (MapProject project, Concept concept, MapUser user) throws Exception;

	
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

	/**
	 * Find available work.
	 *
	 * @param mapProject the map project
	 * @param mapUser the map user
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser,
			PfsParameter pfsParameter);

	/**
	 * Gets the available tracking records for workflow and user.
	 *
	 * @param mapProjectId the map project id
	 * @param userId the user id
	 * @return the available tracking records for workflow and user
	 */
	public List<WorkflowTrackingRecord> getAvailableTrackingRecordsForProjectAndUser(
			Long mapProjectId, Long userId);

	
	/**
	 * Gets the tracking records for unmapped in scope concepts.
	 *
	 * @param mapProject the map project
	 * @return the tracking records for unmapped in scope concepts
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts(MapProject mapProject);

	/**
	 * Gets the tracking records for conflict concepts.
	 *
	 * @param mapProject the map project
	 * @return the tracking records for conflict concepts
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts(
			MapProject mapProject);

	/**
	 * Find map records assigned to user.
	 *
	 * @param project the project
	 * @param user the user
	 * @param pfsParameter the pfs parameter
	 * @return the sets the
	 */
	public SearchResultList findMapRecordsAssignedToUser(MapProject project,
			MapUser user, PfsParameter pfsParameter);
}


