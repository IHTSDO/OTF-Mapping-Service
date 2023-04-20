/**
 * 
 */
package org.ihtsdo.otf.mapping.rest.client;

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
import org.ihtsdo.otf.mapping.jpa.services.rest.WorkflowServiceRest;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * @author Nuno Marques
 *
 */
public class WorkflowClientRest extends RootClientRest implements WorkflowServiceRest {

	@Override
	public void computeWorkflow(Long mapProjectId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SearchResultList findAvailableConcepts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAssignedConcepts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAvailableConflicts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeFeedbackConversation(FeedbackConversationJpa feedbackConversation, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFeedback(FeedbackJpa feedback, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public TrackingRecordList getTrackingRecordsForMapProject(Long mapProjectId, String authToken) throws Exception{
      // TODO Auto-generated method stub
	  return null;
	}


	@Override
	public SearchResultList findAssignedConflicts(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAvailableReviewWork(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAvailableQAWork(Long mapProjectId, String query, PfsParameterJpa pfsParameter,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAssignedReviewWork(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findAssignedQAWork(Long mapProjectId, String userName, String query,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void assignConceptFromMapRecord(String userName, MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void assignConcept(Long mapProjectId, String terminologyId, String userName, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void assignBatch(Long mapProjectId, String userName, List<String> terminologyIds, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Response unassignConcept(Long mapProjectId, String terminologyId, String userName, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unassignWorkBatch(Long mapProjectId, String userName, List<String> terminologyIds, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finishWork(MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publishWork(MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveWork(MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelWorkForMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createQARecord(MapRecordJpa mapRecord, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createQAWork(Long reportId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MapRecord getAssignedMapRecordForConceptAndMapUser(Long mapProjectId, String terminologyId, String userName,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isMapRecordFalseConflict(Long recordId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedbackConversation addFeedbackConversation(FeedbackConversationJpa conversation, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMapRecordFalseConflict(Long recordId, boolean isFalseConflict, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateFeedbackConversation(FeedbackConversationJpa conversation, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FeedbackConversation getFeedbackConversation(Long mapRecordId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedbackConversationList findFeedbackConversationsForMapProjectAndUser(Long mapProjectId, String userName,
			String query, PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedbackConversationListJpa getFeedbackConversationsForTerminologyId(Long mapProjectId, String conceptId,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValidationResult assignBatchToFixErrorPath(Long mapProjectId, List<String> terminologyIds, String userName,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response sendFeedbackEmail(List<String> messageInfo, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	   @Override
	    public Response sendTranslationRequestEmail(List<String> messageInfo, String authToken) throws Exception {
	        // TODO Auto-generated method stub
	        return null;
	    }
}
