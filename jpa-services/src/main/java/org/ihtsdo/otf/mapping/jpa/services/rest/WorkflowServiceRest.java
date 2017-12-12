package org.ihtsdo.otf.mapping.jpa.services.rest;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.helpers.FeedbackConversationList;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.FeedbackJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapRecord;

// TODO: Auto-generated Javadoc
/**
 * The Interface WorkflowServiceRest.
 *
 * @author ${author}
 */
public interface WorkflowServiceRest {

	/**
	   * Compute workflow.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void computeWorkflow(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Finds available concepts for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the username
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAvailableConcepts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Finds assigned concepts for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAssignedConcepts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Finds available conflicts for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAvailableConflicts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	 * Removes the feedback conversation.
	 *
	 * @param feedbackConversation the feedback conversation
	 * @param authToken the auth token
	 * @throws Exception the exception
	 */
	void removeFeedbackConversation(FeedbackConversationJpa feedbackConversation, String authToken) throws Exception;

	/**
	 * Removes the feedback.
	 *
	 * @param feedback the feedback
	 * @param authToken the auth token
	 * @throws Exception the exception
	 */
	void removeFeedback(FeedbackJpa feedback, String authToken) throws Exception;

	/**
	 * Returns the tracking records for map project.
	 *
	 * @param mapProjectId the map project id
	 * @param authToken the auth token
	 * @return the tracking records for map project
	 * @throws Exception the exception
	 */
	public TrackingRecordList getTrackingRecordsForMapProject(Long mapProjectId, String authToken) throws Exception;	
	
	/**
	   * Finds assigned conflicts for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAssignedConflicts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Finds available review work for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAvailableReviewWork(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Find available qa work.
	   *
	   * @param mapProjectId the map project id
	   * @param query the query
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAvailableQAWork(Long mapProjectId, String query, PfsParameterJpa pfsParameter,
			String authToken) throws Exception;

	/**
	   * Finds assigned review work for the specified map project and user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the paging parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAssignedReviewWork(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Find assigned qa work.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findAssignedQAWork(Long mapProjectId, String userName, String query, PfsParameterJpa pfsParameter,
			String authToken) throws Exception;

	/**
	   * Assign user to to work based on an existing map record.
	   *
	   * @param userName the user name
	   * @param mapRecord the map record (can be null)
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void assignConceptFromMapRecord(String userName, MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Assigns user to unmapped concept.
	   *
	   * @param mapProjectId the map project id
	   * @param terminologyId the terminology id
	   * @param userName the user name
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void assignConcept(Long mapProjectId, String terminologyId, String userName, String authToken) throws Exception;

	/**
	   * Assign batch to user.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param terminologyIds the terminology ids
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void assignBatch(Long mapProjectId, String userName, List<String> terminologyIds, String authToken)
			throws Exception;

	/**
	   * Unassign user from a concept.
	   *
	   * @param mapProjectId the map project id
	   * @param terminologyId the terminology id
	   * @param userName the user name
	   * @param authToken the auth token
	   * @return the map record
	   * @throws Exception the exception
	   */
	Response unassignConcept(Long mapProjectId, String terminologyId, String userName, String authToken)
			throws Exception;

	/**
	   * Unassign user from a specified batch of currently assigned work.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param terminologyIds the terminology ids
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void unassignWorkBatch(Long mapProjectId, String userName, List<String> terminologyIds, String authToken)
			throws Exception;

	/**
	   * Attempt to validate and finish work on a record.
	   *
	   * @param mapRecord the completed map record
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void finishWork(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Attempt to publish a previously resolved record This action is only
	   * available to map leads, and only for resolved conflict or review work.
	   *
	   * @param mapRecord the completed map record
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void publishWork(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Save map record without validation checks or workflow action.
	   *
	   * @param mapRecord the map record
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void saveWork(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Cancel work for map record.
	   * 
	   * @param mapRecord the map record
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void cancelWorkForMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Creates the qa record.
	   * 
	   * @param mapRecord the map record
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void createQARecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Creates the qa work given a report of concepts.
	   * 
	   * @param reportId the report id
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void createQAWork(Long reportId, String authToken) throws Exception;

	/**
	   * Gets the assigned map record from the existing workflow for concept and map
	   * user, if it exists.
	   *
	   * @param mapProjectId the map project id
	   * @param terminologyId the terminology id
	   * @param userName the user name
	   * @param authToken the auth token
	   * @return the assigned map record for concept and map user
	   * @throws Exception the exception
	   */
	MapRecord getAssignedMapRecordForConceptAndMapUser(Long mapProjectId, String terminologyId, String userName,
			String authToken) throws Exception;

	/**
	   * Is map record false conflict.
	   * 
	   * @param recordId the record id
	   * @param authToken the auth token
	   * @return the boolean
	   * @throws Exception the exception
	   */
	Boolean isMapRecordFalseConflict(Long recordId, String authToken) throws Exception;

	/**
	   * Adds the feedback conversation.
	   *
	   * @param conversation the conversation
	   * @param authToken the auth token
	   * @return the map user
	   * @throws Exception the exception
	   */
	FeedbackConversation addFeedbackConversation(FeedbackConversationJpa conversation, String authToken)
			throws Exception;

	/**
	   * Sets the map record false conflict.
	   * 
	   * @param recordId the record id
	   * @param isFalseConflict the is false conflict
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void setMapRecordFalseConflict(Long recordId, boolean isFalseConflict, String authToken) throws Exception;

	/**
	   * Updates a feedback conversation.
	   *
	   * @param conversation the conversation
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateFeedbackConversation(FeedbackConversationJpa conversation, String authToken) throws Exception;

	/**
	   * Returns the feedback conversation for a given id (auto-generated) in JSON
	   * format.
	   *
	   * @param mapRecordId the mapRecordId
	   * @param authToken the auth token
	   * @return the feedbackConversation
	   * @throws Exception the exception
	   */
	FeedbackConversation getFeedbackConversation(Long mapRecordId, String authToken) throws Exception;

	/**
	   * Returns the feedback conversations for map project.
	   *
	   * @param mapProjectId the map project id
	   * @param userName the user name
	   * @param query the query
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the feedback conversations for map project
	   * @throws Exception the exception
	   */
	FeedbackConversationList findFeedbackConversationsForMapProjectAndUser(Long mapProjectId, String userName,
			String query, PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Returns the feedback conversations for terminology id.
	   *
	   * @param mapProjectId the map project id
	   * @param conceptId the concept id
	   * @param authToken the auth token
	   * @return the feedback conversations for terminology id
	   * @throws Exception the exception
	   */
	FeedbackConversationListJpa getFeedbackConversationsForTerminologyId(Long mapProjectId, String conceptId,
			String authToken) throws Exception;

	/**
	   * Assign batch to fix error path.
	   *
	   * @param mapProjectId the map project id
	   * @param terminologyIds the terminology ids
	   * @param userName the user name
	   * @param authToken the auth token
	   * @return the validation result
	   * @throws Exception the exception
	   */
	ValidationResult assignBatchToFixErrorPath(Long mapProjectId, List<String> terminologyIds, String userName,
			String authToken) throws Exception;

	/**
	   * Sends a feedback message email.
	   *
	   * @param messageInfo the message
	   * @param authToken the auth token
	   * @return the string
	   * @throws Exception the exception
	   */
	Response sendFeedbackEmail(List<String> messageInfo, String authToken) throws Exception;

}