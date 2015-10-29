/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.FeedbackConversationList;
import org.ihtsdo.otf.mapping.helpers.FeedbackList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;

/**
 * Generically represents a service for answering questions and performing
 * actions related to workflow management.
 */
public interface WorkflowService extends RootService {

  /**
   * Gets the workflow tracking record.
   *
   * @param mapProject the map project
   * @param concept the concept
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public TrackingRecord getTrackingRecord(MapProject mapProject, Concept concept)
    throws Exception;

  /**
   * Gets the workflow tracking records.
   *
   * @return the workflow tracking records
   * @throws Exception the exception
   */
  public TrackingRecordList getTrackingRecords() throws Exception;

  /**
   * Gets the workflow tracking records for map project.
   *
   * @param mapProject the map project
   * @return the workflow tracking records for map project
   * @throws Exception the exception
   */
  public TrackingRecordList getTrackingRecordsForMapProject(
    MapProject mapProject) throws Exception;

  /**
   * Gets the tracking record for map project and concept.
   *
   * @param mapProject the map project
   * @param concept the concept
   * @return the tracking record for map project and concept
   */
  public TrackingRecord getTrackingRecordForMapProjectAndConcept(
    MapProject mapProject, Concept concept);

  /**
   * Adds the workflow tracking record.
   * 
   * @param trackingRecord the workflow tracking record
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public TrackingRecord addTrackingRecord(TrackingRecord trackingRecord)
    throws Exception;

  /**
   * Update workflow tracking record.
   * 
   * @param trackingRecord the workflow tracking record
   * @throws Exception the exception
   */
  public void updateTrackingRecord(TrackingRecord trackingRecord)
    throws Exception;

  /**
   * Removes the workflow tracking record.
   * 
   * @param trackingRecordId the workflow tracking record id
   * @throws Exception the exception
   */
  public void removeTrackingRecord(Long trackingRecordId) throws Exception;

  /**
   * Search Functions.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find available conflicts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableConflicts(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned concepts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned conflicts.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedConflicts(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find available review work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableReviewWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned review work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedReviewWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

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

  Set<MapRecord> synchronizeMapRecords(TrackingRecord trackingRecord,
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
  public WorkflowStatus getLowestWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords);

  /**
   * Gets the workflow status from map records.
   * 
   * @param mapRecords the map records
   * @return the workflow status from map records
   */
  public WorkflowStatus getWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords);

  /**
   * Gets the map users from map records.
   * 
   * @param mapRecords the map records
   * @return the map users from map records
   */
  public MapUserList getMapUsersFromMapRecords(Set<MapRecord> mapRecords);

  /**
   * Gets the lowest workflow status for workflow tracking record.
   * 
   * @param trackingRecord the tracking record
   * @return the lowest workflow status for workflow tracking record
   * @throws Exception the exception
   */
  public WorkflowStatus getLowestWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception;

  /**
   * Gets the workflow status for workflow tracking record.
   * 
   * @param trackingRecord the tracking record
   * @return the workflow status for workflow tracking record
   * @throws Exception the exception
   */
  public WorkflowStatus getWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception;

  /**
   * Gets the map users for workflow tracking record.
   * 
   * @param trackingRecord the tracking record
   * @return the map users for workflow tracking record
   * @throws Exception the exception
   */
  public MapUserList getMapUsersForTrackingRecord(TrackingRecord trackingRecord)
    throws Exception;

  /**
   * Gets the map records for workflow tracking record.
   * 
   * @param trackingRecord the tracking record
   * @return the map records for workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> getMapRecordsForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception;

  /**
   * Closes the manager associated with service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception;

  /**
   * Gets the transaction per operation.
   * 
   * @return the transaction per operation
   * @throws Exception the exception
   */
  @Override
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   * 
   * @param transactionPerOperation the transaction per operation
   * @throws Exception the exception
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Begin transaction.
   * 
   * @throws Exception the exception
   */
  @Override
  public void beginTransaction() throws Exception;

  /**
   * Commit.
   * 
   * @throws Exception the exception
   */
  @Override
  public void commit() throws Exception;

  /**
   * Gets the tracking record for map project and concept.
   *
   * @param mapProject the map project
   * @param terminologyId the terminology id
   * @return the tracking record for map project and concept
   */
  public TrackingRecord getTrackingRecordForMapProjectAndConcept(
    MapProject mapProject, String terminologyId);

  /**
   * QA check: Check that workflow state for all current records is valid.
   *
   * @param mapProject the map project
   * @return the results as a list of strings
   * @throws Exception the exception
   */
  public List<String> computeWorkflowStatusErrors(MapProject mapProject)
    throws Exception;

  /**
   * QA check: Compute untracked map records.
   *
   * @param mapProject the map project
   * @throws Exception the exception
   */
  public void computeUntrackedMapRecords(MapProject mapProject)
    throws Exception;

  /**
   * Adds the user error.
   *
   * @param userError the user error
   * @return the user error
   * @throws Exception the exception
   */
  public Feedback addFeedback(Feedback userError) throws Exception;

  /**
   * Returns the user errors.
   *
   * @return the user errors
   * @throws Exception the exception
   */
  public FeedbackList getFeedbacks() throws Exception;

  /**
   * Adds the feedback conversation.
   *
   * @param conversation the conversation
   * @return the feedback conversation
   * @throws Exception the exception
   */
  public FeedbackConversation addFeedbackConversation(
    FeedbackConversation conversation) throws Exception;

  /**
   * Update workflow exception.
   *
   * @param workflowException the workflow exception
   * @throws Exception the exception
   */
  public void updateWorkflowException(WorkflowException workflowException)
    throws Exception;

  /**
   * Update feedback conversation.
   *
   * @param conversation the feedback conversation
   * @throws Exception the exception
   */
  public void updateFeedbackConversation(FeedbackConversation conversation)
    throws Exception;

  /**
   * Removes the workflow exception.
   *
   * @param workflowExceptiondId the workflow exceptiond id
   * @throws Exception the exception
   */
  public void removeWorkflowException(Long workflowExceptiondId)
    throws Exception;

  /**
   * Returns the feedback conversation.
   *
   * @param id the id
   * @return the feedback conversation
   * @throws Exception the exception
   */
  FeedbackConversation getFeedbackConversation(Long id) throws Exception;

  /**
   * Adds the workflow exception.
   *
   * @param workflowException the workflow exception
   * @return the workflow exception
   * @throws Exception the exception
   */
  public WorkflowException addWorkflowException(
    WorkflowException workflowException) throws Exception;

  /**
   * Returns the workflow exception.
   *
   * @param mapProject the map project
   * @param terminologyId the terminology id
   * @return the workflow exception
   */
  public WorkflowException getWorkflowException(MapProject mapProject,
    String terminologyId);

  /**
   * Returns the feedback conversations for project.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the feedback conversations for project
   * @throws Exception the exception
   */
  public FeedbackConversationList findFeedbackConversationsForProject(
    Long mapProjectId, String userName, String query, PfsParameter pfsParameter)
    throws Exception;

  /**
   * Returns the feedback conversations for concept.
   *
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @return the feedback conversations for concept
   * @throws Exception the exception
   */
  public FeedbackConversationList getFeedbackConversationsForConcept(
    Long mapProjectId, String terminologyId) throws Exception;

  /**
   * Gets the feedback conversations for record.
   *
   * @param mapRecordId the map record id
   * @return the feedback conversations for record
   * @throws Exception the exception
   */
  public FeedbackConversationList getFeedbackConversationsForRecord(
    Long mapRecordId) throws Exception;

  /**
   * Gets the feedback errors for record.
   *
   * @param mapRecord the map record
   * @return the feedback errors for record
   * @throws Exception the exception
   */
  FeedbackList getFeedbackErrorsForRecord(MapRecord mapRecord) throws Exception;

  /**
   * Find available qa work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAvailableQAWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Find assigned qa work.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findAssignedQAWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception;

  /**
   * Creates the qa work.
   *
   * @param report the report
   * @throws Exception the exception
   */
  public void createQAWork(Report report) throws Exception;

  /**
   * Send feedback email.
   *
   * @param name the name
   * @param email the email
   * @param conceptId the concept id
   * @param conceptName the concept name
   * @param refSetId the ref set id
   * @param feedbackMessage the feedback message
   * @throws Exception the exception
   */
  public void sendFeedbackEmail(String name, String email, String conceptId,
    String conceptName, String refSetId, String feedbackMessage)
    throws Exception;

}
