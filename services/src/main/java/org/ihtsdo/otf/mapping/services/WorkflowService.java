package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.Workflow;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

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
	public Workflow getWorkflow(MapProject project)  throws Exception;

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
	 * @param project the project
	 * @param record the record
	 * @throws Exception the exception
	 */
	public void removeWorkflowTrackingRecord(MapProject project, WorkflowTrackingRecord record) throws Exception;
	
	/**
	 * Update workflow tracking record.
	 *
	 * @param project the project
	 * @param record the record
	 * @throws Exception the exception
	 */
	public void updateWorkflowTrackingRecord(MapProject project, WorkflowTrackingRecord record) throws Exception;

	/**
	 * Returns the workflows.
	 *
	 * @return the workflows
	 * @throws Exception the exception
	 */
	public List<Workflow> getWorkflows() throws Exception;

	/**
	 * Adds the workflow.
	 *
	 * @param project the project
	 * @throws Exception the exception
	 */
	public void addWorkflow(MapProject project) throws Exception;

	/**
	 * Removes the workflow.
	 *
	 * @param project the project
	 * @throws Exception the exception
	 */
	public void removeWorkflow(MapProject project) throws Exception;

	/**
	 * Assign user to concept.
	 * @param project the project
	 * @param concept the concept
	 * @param user the user
	 * @return 
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
	 * @exception Exception the exception
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
	 * @param workflow the workflow
	 * @param mapUser the map user
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 */
	public SearchResultList findAvailableWork(Workflow workflow, MapUser mapUser,
			PfsParameter pfsParameter);
	
}
