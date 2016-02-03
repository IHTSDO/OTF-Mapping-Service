/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.CacheMode;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationList;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationListJpa;
import org.ihtsdo.otf.mapping.helpers.FeedbackList;
import org.ihtsdo.otf.mapping.helpers.FeedbackListJpa;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowFixErrorPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowNonLegacyPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowReviewProjectPathHandler;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

/**
 * Default workflow service implementation.
 */
public class WorkflowServiceJpa extends RootServiceJpa implements WorkflowService {

	/**
	 * Instantiates an empty {@link WorkflowServiceJpa}.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowServiceJpa() throws Exception {
		super();

	}

	/* see superclass */
	@Override
	public TrackingRecord addTrackingRecord(TrackingRecord trackingRecord) throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(trackingRecord);
			tx.commit();
		} else {
			manager.persist(trackingRecord);
		}

		return trackingRecord;
	}

	/* see superclass */
	@Override
	public void removeTrackingRecord(Long trackingRecordId) throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			TrackingRecord ma = manager.find(TrackingRecordJpa.class, trackingRecordId);

			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			TrackingRecord ma = manager.find(TrackingRecordJpa.class, trackingRecordId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}

	}

	/* see superclass */
	@Override
	public void updateTrackingRecord(TrackingRecord record) throws Exception {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(record);
			tx.commit();
		} else {
			manager.merge(record);
		}
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecords() throws Exception {

		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();

		trackingRecordList
				.setTrackingRecords(manager.createQuery("select tr from TrackingRecordJpa tr").getResultList());

		return trackingRecordList;
	}

	/* see superclass */
	@Override
	public TrackingRecord getTrackingRecordForMapProjectAndConcept(MapProject mapProject, Concept concept) {

		try {
			return (TrackingRecord) manager
					.createQuery(
							"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
					.setParameter("mapProjectId", mapProject.getId())
					.setParameter("terminology", concept.getTerminology())
					.setParameter("terminologyVersion", concept.getTerminologyVersion())
					.setParameter("terminologyId", concept.getTerminologyId()).getSingleResult();
		} catch (Exception e) {
			return null;
		}

	}

	/* see superclass */
	@Override
	public TrackingRecord getTrackingRecordForMapProjectAndConcept(MapProject mapProject, String terminologyId) {

		try {
			return (TrackingRecord) manager
					.createQuery(
							"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
					.setParameter("mapProjectId", mapProject.getId())
					.setParameter("terminology", mapProject.getSourceTerminology())
					.setParameter("terminologyVersion", mapProject.getSourceTerminologyVersion())
					.setParameter("terminologyId", terminologyId).getSingleResult();
		} catch (Exception e) {
			return null;
		}

	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecordsForMapProject(MapProject mapProject) throws Exception {

		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
		javax.persistence.Query query = manager
				.createQuery("select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId")
				.setParameter("mapProjectId", mapProject.getId());

		trackingRecordList.setTrackingRecords(query.getResultList());

		return trackingRecordList;
	}

	/* see superclass */
	@Override
	public TrackingRecord getTrackingRecord(MapProject mapProject, Concept concept) throws Exception {

		javax.persistence.Query query = manager
				.createQuery(
						"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminologyId", concept.getTerminologyId());

		return (TrackingRecord) query.getSingleResult();
	}

	/* see superclass */
	@Override
	public WorkflowException addWorkflowException(WorkflowException trackingRecord) throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(trackingRecord);
			tx.commit();
		} else {
			manager.persist(trackingRecord);
		}

		return trackingRecord;
	}

	/* see superclass */
	@Override
	public void removeWorkflowException(Long trackingRecordId) throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			WorkflowException ma = manager.find(WorkflowExceptionJpa.class, trackingRecordId);

			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			WorkflowException ma = manager.find(WorkflowExceptionJpa.class, trackingRecordId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}

	}

	/* see superclass */
	@Override
	public void updateWorkflowException(WorkflowException workflowException) throws Exception {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(workflowException);
			tx.commit();
		} else {
			manager.merge(workflowException);
		}
	}

	/* see superclass */
	@Override
	public WorkflowException getWorkflowException(MapProject mapProject, String terminologyId) {

		final javax.persistence.Query query = manager
				.createQuery("select we from WorkflowExceptionJpa we where mapProjectId = :mapProjectId"
						+ " and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminology", mapProject.getSourceTerminology())
				.setParameter("terminologyVersion", mapProject.getSourceTerminologyVersion())
				.setParameter("terminologyId", terminologyId);

		// try to get the expected single result
		try {
			return (WorkflowException) query.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableWork(MapProject mapProject, final MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {
		final SearchResultList availableWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAvailableWork
		// - must be NON_LEGACY PATH
		// - any tracking record with no assigned users is by definition
		// available
		// - any tracking record with one assigned user on NON_LEGACY_PATH with
		// workflowstatus NEW, EDITING_IN_PROGRESS, or EDITING_DONE. Assigned
		// user must not be this user

		switch (mapProject.getWorkflowType()) {
		case CONFLICT_PROJECT:
			sb.append(" AND workflowPath:NON_LEGACY_PATH");
			// Handle "team" based assignment
			if (mapProject.isTeamBased() && mapUser.getTeam() != null && !mapUser.getTeam().isEmpty()) {
				// Use "AND NOT" clauses for all members matching my user's
				// team.
				final MappingService service = new MappingServiceJpa();
				try {
					sb.append(" AND (assignedUserCount:0 OR (assignedUserCount:1 ");
					for (final MapUser user : service.getMapUsersForTeam(mapUser.getTeam()).getMapUsers()) {
						sb.append(" AND NOT assignedUserNames:" + user.getUserName());
					}
					sb.append(") )");
				} catch (Exception e) {
					throw e;
				} finally {
					service.close();
				}
			} else {
				sb.append(" AND (assignedUserCount:0 OR " + "(assignedUserCount:1 AND NOT assignedUserNames:"
						+ mapUser.getUserName() + "))");
			}
			break;
		case REVIEW_PROJECT:
			sb.append(" AND workflowPath:REVIEW_PROJECT_PATH");
			sb.append(" AND assignedUserCount:0");
			break;
		default:
			throw new Exception("Invalid workflow type specified for project " + mapProject.getWorkflowType());

		}

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);

		availableWork.setTotalCount(totalCt[0]);
		for (final TrackingRecord tr : results) {
			final SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			availableWork.addSearchResult(result);
		}
		return availableWork;
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableConflicts(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList availableConflicts = new SearchResultListJpa();

		// if not a conflict project, return empty list
		if (!mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT)) {
			return availableConflicts;
		}

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAvailableConflicts
		// - user and workflowStatus pair of CONFLICT_DETECTED_userName exists
		// - user and workflowStatus pairs of
		// CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist
		sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_DETECTED_*");
		sb.append(" AND NOT (" + "userAndWorkflowStatusPairs:CONFLICT_NEW_* OR "
				+ "userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_* OR "
				+ "userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		availableConflicts.setTotalCount(totalCt[0]);
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			availableConflicts.addSearchResult(result);
		}
		return availableConflicts;
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableQAWork(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList availableQAWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAvailableReviewWork
		// - a user (any) and workflowStatus pair of QA_NEEDED_userName
		// exists
		// - the QA_NEEDED pair is not for this user (i.e. user can't review
		// their own work, UNLESS there is only one lead on the project
		// - user and workflowStatus pairs of
		// CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

		// must have a QA_NEEDED tag with any user
		sb.append(" AND userAndWorkflowStatusPairs:QA_NEEDED_*");

		sb.append(" AND workflowPath:QA_PATH");

		// there must not be an already claimed review record
		sb.append(" AND NOT (userAndWorkflowStatusPairs:QA_NEW_*" + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_*"
				+ " OR userAndWorkflowStatusPairs:QA_RESOLVED_*" + ")");

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		availableQAWork.setTotalCount(totalCt[0]);
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			availableQAWork.addSearchResult(result);
		}
		return availableQAWork;

	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableReviewWork(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList availableReviewWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAvailableReviewWork
		// - a user (any) and workflowStatus pair of REVIEW_NEEDED_userName
		// exists
		// - the REVIEW_NEEDED pair is not for this user (i.e. user can't review
		// their own work, UNLESS there is only one lead on the project
		// - user and workflowStatus pairs of
		// CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

		// must have a REVIEW_NEEDED tag with any user
		sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEEDED_*");

		// there must not be an already claimed review record
		sb.append(" AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
				+ " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
				+ " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*" + ")");

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		availableReviewWork.setTotalCount(totalCt[0]);
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			availableReviewWork.addSearchResult(result);
		}
		return availableReviewWork;

	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedWork(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList assignedWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAssignedWork
		// - user and workflowStatus must exist in a pair of form:
		// workflowStatus_userName, e.g. NEW_dmo or EDITING_IN_PROGRESS_kli
		// - modify search term based on pfs parameter query restriction field
		// * default: NEW, EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED
		// * NEW: NEW
		// * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

		// add terms based on query restriction
		final String type = pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";
		switch (type) {
		case "NEW":
			sb.append(" AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName());
			break;
		case "EDITING_IN_PROGRESS":
			sb.append(" AND (userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName() + ")");
			break;
		case "EDITING_DONE":
			sb.append(" AND (userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_" + mapUser.getUserName() + ")");
			break;
		default:
			sb.append(" AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_" + mapUser.getUserName() + ")");
			break;
		}

		// add terms to exclude concepts that a lead has claimed
		sb.append(" AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_*"
				+ " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*"
				+ " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*" + " OR userAndWorkflowStatusPairs:REVIEW_NEW_*"
				+ " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_*"
				+ " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*)");

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		assignedWork.setTotalCount(totalCt[0]);

		for (final TrackingRecord tr : results) {

			// instantiate the result list
			final SearchResult result = new SearchResultJpa();

			// get the map records associated with this tracking record
			final Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

			// get the map record assigned to this user
			MapRecord mapRecord = null;

			// SEE BELOW/MAP-617
			WorkflowStatus mapLeadAlternateRecordStatus = null;
			for (final MapRecord mr : mapRecords) {

				if (mr.getOwner().equals(mapUser)) {

					// if this lead has review or conflict work, set the flag
					if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
							|| mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)
							|| mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
							|| mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_IN_PROGRESS)) {

						mapLeadAlternateRecordStatus = mr.getWorkflowStatus();

						// added to prevent user from getting REVISION record
						// back on FIX_ERROR_PATH
						// yet another problem related to leads being able to
						// serve as dual roles
					} else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
						// do nothing

						// otherwise, this is the specialist/concept-level work
					} else {
						mapRecord = mr;
					}
				}
			}

			// if no record and no review or conflict work was found, throw
			// error
			if (mapRecord == null) {
				throw new Exception("Failed to retrieve assigned work:  no map record found for user "
						+ mapUser.getUserName() + " and concept " + tr.getTerminologyId());

			} else {

				// alter the workflow status if a higher-level record exists for
				// this user
				if (mapLeadAlternateRecordStatus != null) {

					Logger.getLogger(WorkflowServiceJpa.class)
							.info("Setting alternate record status: " + mapLeadAlternateRecordStatus);
					mapRecord.setWorkflowStatus(mapLeadAlternateRecordStatus);
				}
				// create the search result
				result.setTerminologyId(mapRecord.getConceptId());
				result.setValue(mapRecord.getConceptName());
				result.setTerminology(mapRecord.getLastModified().toString());
				result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
				result.setId(mapRecord.getId());
				assignedWork.addSearchResult(result);
			}
		}
		return assignedWork;
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedConflicts(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList assignedConflicts = new SearchResultListJpa();

		if (mapProject.getWorkflowType().toString().equals("REVIEW_PROJECT_PATH"))
			return assignedConflicts;

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAssignedConflicts
		// - workflow status CONFLICT_NEW or CONFLICT_IN_PROGRESS with this user
		// name in pair

		// add terms based on query restriction
		final String type = pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";
		switch (type) {
		case "CONFLICT_NEW":
			sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName());
			break;
		case "CONFLICT_IN_PROGRESS":
			sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName());
			break;
		case "CONFLICT_RESOLVED":
			sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_RESOLVED_" + mapUser.getUserName());
			break;
		default:
			sb.append(" AND (userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_" + mapUser.getUserName() + ")");
			break;
		}

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		assignedConflicts.setTotalCount(totalCt[0]);

		for (final TrackingRecord tr : results) {
			final SearchResult result = new SearchResultJpa();

			final Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (final MapRecord mr : mapRecords) {
				if (mr.getOwner().equals(mapUser)) {

					// SEE MAP-617:
					// Lower level record may exist with same owner, only
					// add if actually a conflict

					if (mr.getWorkflowStatus().compareTo(WorkflowStatus.CONFLICT_DETECTED) < 0) {
						// do nothing, this is the specialist level work
					} else {
						mapRecord = mr;
					}
				}
			}

			if (mapRecord == null) {
				throw new Exception("Failed to retrieve assigned conflicts:  no map record found for user "
						+ mapUser.getUserName() + " and concept " + tr.getTerminologyId());
			} else {
				result.setTerminologyId(mapRecord.getConceptId());
				result.setValue(mapRecord.getConceptName());
				result.setTerminology(mapRecord.getLastModified().toString());
				result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
				result.setId(mapRecord.getId());
				assignedConflicts.addSearchResult(result);
			}
		}

		return assignedConflicts;
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedReviewWork(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList assignedReviewWork = new SearchResultListJpa();
		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId());

		// add the query terms specific to findAssignedReviewWork
		// - user and workflow status must exist in the form REVIEW_NEW_userName
		// or REVIEW_IN_PROGRESS_userName

		// add terms based on query restriction
		final String type = pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";
		switch (type) {
		case "REVIEW_NEW":
			sb.append(" AND userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName());

			break;
		case "REVIEW_IN_PROGRESS":
			sb.append(" AND userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName());
			break;
		case "REVIEW_RESOLVED":
			sb.append(" AND userAndWorkflowStatusPairs:REVIEW_RESOLVED_" + mapUser.getUserName());
			break;
		default:
			sb.append(" AND (userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_" + mapUser.getUserName() + ")");
			break;
		}

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		assignedReviewWork.setTotalCount(totalCt[0]);

		for (final TrackingRecord tr : results) {
			final SearchResult result = new SearchResultJpa();

			final Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (final MapRecord mr : mapRecords) {

				if (mr.getOwner().equals(mapUser)) {

					// check for the case where REVIEW work is both specialist
					// and
					// lead level for same user
					if (mr.getWorkflowStatus().compareTo(WorkflowStatus.REVIEW_NEW) < 0) {
						// do nothing, this is the specialist level work

					} else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
						// do nothing

					} else {
						// add the record
						mapRecord = mr;
					}
				}
			}

			if (mapRecord == null) {
				throw new Exception("Failed to retrieve assigned work:  no map record found for user "
						+ mapUser.getUserName() + " and concept " + tr.getTerminologyId());
			}
			result.setTerminologyId(mapRecord.getConceptId());
			result.setValue(mapRecord.getConceptName());
			result.setTerminology(mapRecord.getLastModified().toString());
			result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
			result.setId(mapRecord.getId());
			assignedReviewWork.addSearchResult(result);
		}
		return assignedReviewWork;
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedQAWork(MapProject mapProject, MapUser mapUser, String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList assignedReviewWork = new SearchResultListJpa();
		PfsParameter localPfsParameter = pfsParameter;

		// create a blank pfs parameter object if one not passed in
		if (localPfsParameter == null)
			localPfsParameter = new PfsParameterJpa();

		// create a blank query restriction if none provided
		if (localPfsParameter.getQueryRestriction() == null)
			localPfsParameter.setQueryRestriction("");

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String fullQuery = "mapProjectId:" + mapProject.getId();

		// add the query terms specific to findAssignedReviewWork
		// - user and workflow status must exist in the form QA_NEW_userName
		// or QA_IN_PROGRESS_userName

		// add terms based on query restriction
		switch (localPfsParameter.getQueryRestriction()) {
		case "QA_NEW":
			fullQuery += " AND userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName();

			break;
		case "QA_IN_PROGRESS":
			fullQuery += " AND userAndWorkflowStatusPairs:QA_IN_PROGRESS_" + mapUser.getUserName();
			break;
		case "QA_RESOLVED":
			fullQuery += " AND userAndWorkflowStatusPairs:QA_RESOLVED_" + mapUser.getUserName();
			break;
		default:
			fullQuery += " AND (userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:QA_RESOLVED_" + mapUser.getUserName() + ")";
			break;
		}

		// don't get path
		fullQuery += " AND workflowPath:QA_PATH";

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		try {
			luceneQuery = queryParser.parse(fullQuery);
		} catch (ParseException e) {
			throw new LocalException(
					"The specified search terms cannot be parsed.  Please check syntax and try again.");
		}
		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager.createFullTextQuery(luceneQuery,
				TrackingRecordJpa.class);

		List<TrackingRecord> allResults = ftquery.getResultList();
		List<TrackingRecord> results = new ArrayList<>();

		if (query == null || query.equals("") || query.equals("null") || query.equals("undefined")) {
			results = allResults;
		} else {
			// remove tracking records that don't have a map record with a label
			// matching the query
			for (final TrackingRecord tr : allResults) {
				boolean labelFound = false;
				for (final MapRecord record : getMapRecordsForTrackingRecord(tr)) {
					for (final String label : record.getLabels()) {
						if (label.equals(query)) {
							labelFound = true;
						}
					}
				}
				if (labelFound) {
					results.add(tr);
				}
			}
		}

		assignedReviewWork.setTotalCount(results.size());

		// apply paging, and sorting if appropriate
		if (pfsParameter != null && (pfsParameter.getSortField() != null && !pfsParameter.getSortField().isEmpty())) {
			// check that specified sort field exists on Concept and is
			// a string
			final Field sortField = TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField());
			if (!sortField.getType().equals(String.class)) {

				throw new Exception("findAssignedQAWork error:  Referenced sort field is not of type String");
			}

			// allow the field to access the Concept values
			sortField.setAccessible(true);

			// sort the list - UNTESTED
			Collections.sort(results, new Comparator<TrackingRecord>() {
				@Override
				public int compare(TrackingRecord c1, TrackingRecord c2) {

					// if an exception is returned, simply pass equality
					try {
						return ((String) sortField.get(c1)).compareTo((String) sortField.get(c2));
					} catch (Exception e) {
						return 0;
					}
				}
			});
		}

		// get the start and end indexes based on paging parameters
		int startIndex = 0;
		int toIndex = results.size();
		if (pfsParameter != null) {
			if (pfsParameter.getStartIndex() != -1) {
				// ensure that start index is within array boundaries
				startIndex = Math.min(results.size(), pfsParameter.getStartIndex());

				// ensure startIndex not less than zero
				if (startIndex < 0)
					startIndex = 0;
			}

			if (pfsParameter.getMaxResults() != -1) {
				toIndex = Math.min(results.size(), startIndex + pfsParameter.getMaxResults());
			}
		}

		for (final TrackingRecord tr : results.subList(startIndex, toIndex)) {

			SearchResult result = new SearchResultJpa();

			Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (final MapRecord mr : mapRecords) {

				if (mr.getOwner().equals(mapUser)) {

					if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
						// do nothing, this is the specialist level QA work

					} else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
						// do nothing

					} else {
						// this is the user's record
						mapRecord = mr;
					}
				}
			}

			if (mapRecord == null) {
				throw new Exception("Failed to retrieve assigned work:  no map record found for user "
						+ mapUser.getUserName() + " and concept " + tr.getTerminologyId());
			}
			result.setTerminologyId(mapRecord.getConceptId());
			result.setValue(mapRecord.getConceptName());
			StringBuffer labelBuffer = new StringBuffer();
			for (final MapRecord record : getMapRecordsForTrackingRecord(tr)) {
				for (final String label : record.getLabels()) {
					if (labelBuffer.indexOf(label) == -1)
						labelBuffer.append(";").append(label);
				}
			}
			result.setValue2(labelBuffer.toString());
			result.setTerminology(mapRecord.getLastModified().toString());
			result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
			result.setId(mapRecord.getId());
			assignedReviewWork.addSearchResult(result);
		}
		return assignedReviewWork;
	}

	/* see superclass */
	@Override
	public void createQAWork(Report report) throws Exception {

		if (report.getResults() == null || report.getResults().size() != 1) {
			throw new Exception("Failed to provide a report with one result set " + report.getId());
		}

		Set<String> conceptIds = new HashSet<>();
		for (final ReportResultItem resultItem : report.getResults().get(0).getReportResultItems()) {
			conceptIds.add(resultItem.getItemId());
		}

		// open the services
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();

		// get the map project and concept
		MapProject mapProject = mappingService.getMapProject(report.getMapProjectId());

		// find the qa user
		MapUser mapUser = null;
		for (final MapUser user : mappingService.getMapUsers().getMapUsers()) {
			if (user.getUserName().equals("qa"))
				mapUser = user;
		}
		mappingService.close();

		for (final String conceptId : conceptIds) {

			Concept concept = contentService.getConcept(conceptId, mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			mappingService = new MappingServiceJpa();
			MapRecordList recordList = mappingService.getMapRecordsForProjectAndConcept(mapProject.getId(), conceptId);
			// lazy initialize
			recordList.getMapRecords().size();
			mappingService.close();

			for (final MapRecord mapRecord : recordList.getMapRecords()) {
				// set the label on the record
				mapRecord.addLabel(report.getReportDefinition().getName());

				// process the workflow action
				processWorkflowAction(mapProject, concept, mapUser, mapRecord, WorkflowAction.CREATE_QA_RECORD);
			}
		}

		contentService.close();
	}

	/**
	 * Perform workflow actions based on a specified action.
	 * ASSIGN_FROM_INITIAL_RECORD is the only routine that requires a map record
	 * to be passed in All other cases that all required mapping information
	 * (e.g. map records) be current in the database (i.e. updateMapRecord has
	 * been called)
	 * 
	 * @param mapProject
	 *            the map project
	 * @param concept
	 *            the concept
	 * @param mapUser
	 *            the map user
	 * @param mapRecord
	 *            the map record
	 * @param workflowAction
	 *            the workflow action
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public void processWorkflowAction(MapProject mapProject, Concept concept, MapUser mapUser, MapRecord mapRecord,
			WorkflowAction workflowAction) throws Exception {

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Processing workflow action by " + mapUser.getName() + ":  " + workflowAction.toString());
		if (mapRecord != null) {
			Logger.getLogger(WorkflowServiceJpa.class).info("  Record attached: " + mapRecord.toString());
		}

		setTransactionPerOperation(true);

		// instantiate the algorithm handler for this project\
		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();
		algorithmHandler.setMapProject(mapProject);

		// locate any existing workflow tracking records for this project and
		// concept
		// NOTE: Exception handling deliberate, since tracking record may or may
		// not exist
		// depending on workflow path
		TrackingRecord trackingRecord = null;
		try {
			trackingRecord = getTrackingRecord(mapProject, concept);
		} catch (NoResultException e) {
			// do nothing (leave trackingRecord null)
		}

		// declare the workflow handler
		AbstractWorkflowPathHandler handler = null;

		// if no tracking record, create one (NOTE: Must be FIX_ERROR or QA
		// path)
		if (trackingRecord == null) {

			// create a new tracking record for both paths
			trackingRecord = new TrackingRecordJpa();
			trackingRecord.setMapProjectId(mapProject.getId());
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
			trackingRecord.addMapRecordId(mapRecord.getId());

			// get the tree positions for this concept and set the sort key //
			// to
			// the first retrieved
			final ContentService contentService = new ContentServiceJpa();
			try {
				TreePositionList treePositionsList = contentService.getTreePositionsWithDescendants(
						concept.getTerminologyId(), concept.getTerminology(), concept.getTerminologyVersion());

				// handle inactive concepts - which don't have tree positions
				if (treePositionsList.getCount() == 0) {
					trackingRecord.setSortKey("");
				} else {
					trackingRecord.setSortKey(treePositionsList.getTreePositions().get(0).getAncestorPath());
				}
			} catch (Exception e) {
				throw e;
			} finally {
				contentService.close();
			}

			// if Qa Path, instantiate Qa Path handler
			if (workflowAction.equals(WorkflowAction.CREATE_QA_RECORD)) {
				handler = (AbstractWorkflowPathHandler) Class
						.forName(ConfigUtility.getConfigProperties().getProperty("workflow.path.handler.QA_PATH.class"))
						.newInstance();
				trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);
			}

			// otherwise, use Fix Error Path
			else {
				handler = (AbstractWorkflowPathHandler) Class.forName(
						ConfigUtility.getConfigProperties().getProperty("workflow.path.handler.FIX_ERROR_PATH.class"))
						.newInstance();
				trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
			}
		}
		// otherwise, instantiate based on tracking record
		else {

			handler = (AbstractWorkflowPathHandler) Class
					.forName(ConfigUtility.getConfigProperties()
							.getProperty("workflow.path.handler." + trackingRecord.getWorkflowPath() + ".class"))
					.newInstance();
		}

		if (handler == null) {
			throw new Exception("Could not determine workflow handler");
		}

		// validate the tracking record by its handler
		ValidationResult result = handler.validateTrackingRecordForActionAndUser(trackingRecord, workflowAction,
				mapUser);

		if (!result.isValid()) {

			Logger.getLogger(WorkflowServiceJpa.class).info(result.toString());

			StringBuffer message = new StringBuffer();

			message.append("Errors were detected in the workflow for:\n");
			message.append("  Project\t: " + mapProject.getName() + "\n");
			message.append("  Concept\t: " + concept.getTerminologyId() + "\n");
			message.append("  Path:\t " + trackingRecord.getWorkflowPath().toString() + "\n");
			message.append("  User\t: " + mapUser.getUserName() + "\n");
			message.append("  Action\t: " + workflowAction.toString() + "\n");

			message.append("\n");

			// record information
			message.append("Records involved:\n");
			message.append("  " + "id\tUser\tWorkflowStatus\n");

			for (final MapRecord mr : getMapRecordsForTrackingRecord(trackingRecord)) {
				message.append("  " + mr.getId().toString() + "\t" + mr.getOwner().getUserName() + "\t"
						+ mr.getWorkflowStatus().toString() + "\n");
			}
			message.append("\n");

			message.append("Errors reported:\n");

			for (final String error : result.getErrors()) {
				message.append("  " + error + "\n");
			}

			message.append("\n");

			// log the message
			Logger.getLogger(WorkflowServiceJpa.class).error("Workflow error detected\n" + message.toString());

			// send email if indicated
			Properties config = ConfigUtility.getConfigProperties();

			String notificationRecipients = config.getProperty("send.notification.recipients");
			if (!notificationRecipients.isEmpty()) {
				ConfigUtility.sendEmail(notificationRecipients,
						mapProject.getName() + " Workflow Error Alert, Concept " + concept.getTerminologyId(),
						message.toString());
			}

			throw new LocalException("Workflow action " + workflowAction.toString()
					+ " could not be performed on concept " + trackingRecord.getTerminologyId());
		}

		Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

		// if the record passed in updates an existing record, replace it in the
		// set
		if (mapRecord != null && mapRecord.getId() != null)

		{
			for (final MapRecord mr : mapRecords) {
				if (mr.getId().equals(mapRecord.getId())) {

					Logger.getLogger(WorkflowService.class)
							.info("Replacing record " + mr.toString() + "\n  with" + mapRecord.toString());

					mapRecords.remove(mr);
					mapRecords.add(mapRecord);
					break;
				}
			}
		}

		// process the workflow action
		mapRecords = handler.processWorkflowAction(trackingRecord, workflowAction, mapUser, mapRecords, mapRecord);

		Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");

		// synchronize the map records via helper function
		Set<MapRecord> syncedRecords = synchronizeMapRecords(trackingRecord, mapRecords);

		// clear the pointer fields (i.e. ids and names of mapping
		// objects)
		trackingRecord.setMapRecordIds(null);
		trackingRecord.setAssignedUserNames(null);
		trackingRecord.setUserAndWorkflowStatusPairs(null);

		// recalculate the pointer fields
		for (final MapRecord mr : syncedRecords)

		{
			trackingRecord.addMapRecordId(mr.getId());
			trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
			trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner().getUserName(), mr.getWorkflowStatus().toString());
		}

		Logger.getLogger(WorkflowServiceJpa.class).info("Revised tracking record: " + trackingRecord.toString());

		// if the tracking record is ready for removal, delete it
		if ((

		getWorkflowStatusForTrackingRecord(trackingRecord).equals(WorkflowStatus.READY_FOR_PUBLICATION)
				|| getWorkflowStatusForTrackingRecord(trackingRecord).equals(WorkflowStatus.PUBLISHED))
				&& trackingRecord.getMapRecordIds().size() == 1) {

			Logger.getLogger(WorkflowServiceJpa.class).info("  Publication ready, removing tracking record.");
			removeTrackingRecord(trackingRecord.getId());

			// else add the tracking record if new
		} else if (trackingRecord.getId() == null) {
			Logger.getLogger(WorkflowServiceJpa.class).info("  New workflow concept, adding tracking record.");
			addTrackingRecord(trackingRecord);

			// otherwise update the tracking record
		} else {
			Logger.getLogger(WorkflowServiceJpa.class).info("  Still in workflow, updating tracking record.");
			updateTrackingRecord(trackingRecord);
		}

	}

	/**
	 * Algorithm has gotten needlessly complex due to conflicting service
	 * changes and algorithm handler changes. However, the basic process is
	 * this:
	 * 
	 * 1) Function takes a set of map records returned from the algorithm
	 * handler These map records may have a hibernate id (updated/unchanged) or
	 * not (added) 2) The passed map records are detached from the persistence
	 * environment. 3) The existing (in database) records are re-retrieved from
	 * the database. Note that this is why the passed map records are detached
	 * -- otherwise they are overwritten. 4) Each record in the detached set is
	 * checked against the 'refreshed' database record set - if the detached
	 * record is not in the set, then it has been added - if the detached record
	 * is in the set, check it for updates - if it has been changed, update it -
	 * if no change, disregard 5) Each record in the 'refreshed' databased
	 * record set is checked against the new set - if the refreshed record is
	 * not in the new set, delete it from the database 6) Return the detached
	 * set as re-synchronized with the database
	 * 
	 * Note on naming conventions used in this method: - mapRecords: the set of
	 * records passed in as argument - newRecords: The set of records to be
	 * returned after synchronization - oldRecords: The set of records retrieved
	 * by id from the database for comparison - syncedRecords: The synchronized
	 * set of records for return from this routine
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapRecords
	 *            the map records
	 * @return the sets the
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public Set<MapRecord> synchronizeMapRecords(TrackingRecord trackingRecord, Set<MapRecord> mapRecords)
			throws Exception {

		Set<MapRecord> newRecords = new HashSet<>();
		Set<MapRecord> oldRecords = new HashSet<>();
		Set<MapRecord> syncedRecords = new HashSet<>();

		// detach the currently persisted map records from the workflow service
		// to avoid overwrite by retrieval of existing records
		for (final MapRecord mr : mapRecords) {
			manager.detach(mr);
			newRecords.add(mr);
		}

		// Instantiate the mapping service
		MappingService mappingService = new MappingServiceJpa();

		// retrieve the old (existing) records
		if (trackingRecord.getMapRecordIds() != null) {
			for (final Long id : trackingRecord.getMapRecordIds()) {
				oldRecords.add(mappingService.getMapRecord(id));
			}
		}

		// cycle over new records to check for additions or updates
		for (final MapRecord mr : newRecords) {
			if (getMapRecordInSet(oldRecords, mr.getId()) == null) {

				// deep copy the detached record into a new
				// persistence-environment record
				// this routine also duplicates child collections to avoid
				// detached object errors
				MapRecord newRecord = new MapRecordJpa(mr, false);

				/*
				 * Logger.getLogger(WorkflowServiceJpa.class).info(
				 * "Adding record: " + newRecord.toString());
				 */
				// add the record to the database

				mappingService.addMapRecord(newRecord);

				// add the record to the return list
				syncedRecords.add(newRecord);
			}

			// otherwise, check for update
			else {
				// if the old map record is changed, update it
				/*
				 * Logger.getLogger(WorkflowServiceJpa.class).info(
				 * "New record: " + mr.toString());
				 * Logger.getLogger(WorkflowServiceJpa.class).info(
				 * "Old record: " + getMapRecordInSet(oldRecords,
				 * mr.getId()).toString());
				 */

				if (!mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
					Logger.getLogger(WorkflowServiceJpa.class).info("  Changed: UPDATING");
					mappingService.updateMapRecord(mr);
				} else {
					Logger.getLogger(WorkflowServiceJpa.class).info("  No change: NOT UPDATING");
				}

				syncedRecords.add(mr);
			}
		}

		// cycle over old records to check for deletions
		for (final MapRecord mr : oldRecords) {

			// if old record is not in the new record set, delete it
			if (getMapRecordInSet(syncedRecords, mr.getId()) == null) {

				Logger.getLogger(WorkflowServiceJpa.class).info("Deleting record " + mr.getId());
				mappingService.removeMapRecord(mr.getId());
			}
		}

		// close the service
		mappingService.close();

		return syncedRecords;

	}

	/**
	 * Gets the map record in set.
	 * 
	 * @param mapRecords
	 *            the map records
	 * @param mapRecordId
	 *            the map record id
	 * @return the map record in set
	 */
	@SuppressWarnings("static-method")
	private MapRecord getMapRecordInSet(Set<MapRecord> mapRecords, Long mapRecordId) {
		if (mapRecordId == null)
			return null;

		for (final MapRecord mr : mapRecords) {
			if (mapRecordId.equals(mr.getId()))
				return mr;
		}
		return null;
	}

	/* see superclass */
	@Override
	public void computeWorkflow(MapProject mapProject) throws Exception {

		Logger.getLogger(WorkflowServiceJpa.class).info("Start computing workflow for " + mapProject.getName());

		// set the transaction parameter and tracking variables
		setTransactionPerOperation(false);
		int commitCt = 1000;
		int trackingRecordCt = 0;

		// Clear the workflow for this project
		Logger.getLogger(WorkflowServiceJpa.class).info("  Clear old workflow");
		clearWorkflowForMapProject(mapProject);

		// open the services
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();

		// get the concepts in scope
		SearchResultList conceptsInScope = mappingService.findConceptsInScope(mapProject.getId(), null);

		// construct a hashset of concepts in scope
		Set<String> conceptIds = new HashSet<>();
		for (final SearchResult sr : conceptsInScope.getIterable()) {
			conceptIds.add(sr.getTerminologyId());
		}

		Logger.getLogger(WorkflowServiceJpa.class).info("  Concept ids put into hash set: " + conceptIds.size());

		// get the current records
		MapRecordList mapRecords = mappingService.getMapRecordsForMapProject(mapProject.getId());

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Processing existing records (" + mapRecords.getCount() + " found)");

		// instantiate a mapped set of non-published records
		Map<String, List<MapRecord>> unpublishedRecords = new HashMap<>();

		// cycle over the map records, and remove concept ids if a map record is
		// publication-ready
		for (final MapRecord mapRecord : mapRecords.getIterable()) {

			// if this map record is published, skip and remove this concept
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
					|| mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {

				conceptIds.remove(mapRecord.getConceptId());
			}

			// if this concept is in scope, add to workflow
			else if (conceptIds.contains(mapRecord.getConceptId())) {

				List<MapRecord> originIds;

				// if this key does not yet have a constructed list, make one,
				// otherwise get the existing list
				if (unpublishedRecords.containsKey(mapRecord.getConceptId())) {
					originIds = unpublishedRecords.get(mapRecord.getConceptId());
				} else {
					originIds = new ArrayList<>();
				}

				originIds.add(mapRecord);
				unpublishedRecords.put(mapRecord.getConceptId(), originIds);
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("  Concepts with no publication-ready map record: " + conceptIds.size());
		Logger.getLogger(WorkflowServiceJpa.class)
				.info("  Concepts with unpublished map record content:  " + unpublishedRecords.size());

		beginTransaction();

		// construct the tracking records for unmapped concepts
		for (final String terminologyId : conceptIds) {

			// retrieve the concept for this result
			Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			// if concept could not be retrieved, throw exception
			if (concept == null) {
				throw new Exception("Failed to retrieve concept " + terminologyId);
			}

			// skip inactive concepts
			if (!concept.isActive()) {
				Logger.getLogger(WorkflowServiceJpa.class).warn("Skipped inactive concept " + terminologyId);
				continue;
			}

			// bypass for integration tests
			String sortKey = "";
			if (concept.getLabel() == null || !concept.getLabel().equals("integration-test")) {
				// get the tree positions for this concept and set the sort key
				// to
				// the first retrieved
				TreePositionList treePositionsList = contentService.getTreePositions(concept.getTerminologyId(),
						concept.getTerminology(), concept.getTerminologyVersion());

				// if no tree position, throw exception
				if (treePositionsList.getCount() == 0) {
					throw new Exception("Active concept " + terminologyId + " has no tree positions");
				}

				sortKey = treePositionsList.getTreePositions().get(0).getAncestorPath();
			}
			// create a workflow tracking record for this concept
			TrackingRecord trackingRecord = new TrackingRecordJpa();

			// populate the fields from project and concept
			trackingRecord.setMapProjectId(mapProject.getId());
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
			trackingRecord.setSortKey(sortKey);

			// add any existing map records to this tracking record
			Set<MapRecord> mapRecordsForTrackingRecord = new HashSet<>();
			if (unpublishedRecords.containsKey(trackingRecord.getTerminologyId())) {
				for (final MapRecord mr : unpublishedRecords.get(trackingRecord.getTerminologyId())) {
					Logger.getLogger(WorkflowServiceJpa.class)
							.info("    Adding existing map record " + mr.getId() + ", owned by "
									+ mr.getOwner().getUserName() + " to tracking record for "
									+ trackingRecord.getTerminologyId());

					trackingRecord.addMapRecordId(mr.getId());
					trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
					trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner().getUserName(),
							mr.getWorkflowStatus().toString());

					// add to the local set for workflow calculation
					mapRecordsForTrackingRecord.add(mr);
				}
			}

			// check if REVISION record is present
			// check if QA record is present
			boolean revisionRecordPresent = false;
			boolean qaRecordPresent = false;
			for (final MapRecord mr : mapRecordsForTrackingRecord) {
				if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
					revisionRecordPresent = true;
				if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)
						|| mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
						|| mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
						|| mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)) {
					qaRecordPresent = true;
				}
			}

			// if REVISION found and no qa records, set to FIX_ERROR_PATH
			if (revisionRecordPresent && !qaRecordPresent) {
				trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
				// if REVISION found and qa records, set to QA_PATH
			} else if (revisionRecordPresent && qaRecordPresent) {
				trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);
				// otherwise, set to the WorkflowPath corresponding to the
				// project WorkflowType
			} else {
				if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT))
					trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);
				else if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT))
					trackingRecord.setWorkflowPath(WorkflowPath.REVIEW_PROJECT_PATH);
				else {
					throw new Exception("Could not set workflow path from workflow type " + mapProject.getWorkflowType()
							+ " for records " + trackingRecord.getMapRecordIds().toString());
				}
			}

			addTrackingRecord(trackingRecord);

			if (++trackingRecordCt % commitCt == 0) {
				Logger.getLogger(WorkflowServiceJpa.class).info("  " + trackingRecordCt + " tracking records created");
				commit();
				beginTransaction();

				// close and re-instantiate the content service to prevent
				// memory buildup from Concept and TreePosition objects
				contentService.close();
				contentService = new ContentServiceJpa();
			}
		}

		// commit any remaining transactions
		commit();

		// instantiate the full text eneity manager and set version
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		fullTextEntityManager.setProperty("Version", Version.LUCENE_36);

		// create the indexes
		Logger.getLogger(WorkflowServiceJpa.class).info("  Creating indexes for TrackingRecordJpa");
		fullTextEntityManager.purgeAll(TrackingRecordJpa.class);
		fullTextEntityManager.flushToIndexes();
		fullTextEntityManager.createIndexer(TrackingRecordJpa.class).batchSizeToLoadObjects(100)
				.cacheMode(CacheMode.NORMAL).threadsToLoadObjects(4).threadsForSubsequentFetching(8).startAndWait();

		Logger.getLogger(WorkflowServiceJpa.class).info("Done.");
	}

	/* see superclass */
	@Override
	public void clearWorkflowForMapProject(MapProject mapProject) throws Exception {

		int commitCt = 0;
		int commitInterval = 1000;

		// begin transaction not in transaction-per-operation mode
		if (!getTransactionPerOperation()) {
			beginTransaction();
		}

		for (final TrackingRecord tr : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			removeTrackingRecord(tr.getId());

			if (++commitCt % commitInterval == 0) {

				// if not a transaction for every operation, commit at intervals
				if (!getTransactionPerOperation()) {
					commit();
					beginTransaction();
				}

				Logger.getLogger(WorkflowServiceJpa.class).info("  Removed " + commitCt + " tracking records");
			}
		}

		// commit any last deletions if not in transaction-per-operation mode
		if (!getTransactionPerOperation()) {
			commit();
		}

	}

	// //////////////////////////
	// Utility functions
	// //////////////////////////

	/* see superclass */
	@Override
	public Set<MapRecord> getMapRecordsForTrackingRecord(TrackingRecord trackingRecord) throws Exception {
		Set<MapRecord> mapRecords = new HashSet<>();
		MappingService mappingService = new MappingServiceJpa();
		if (trackingRecord != null && trackingRecord.getMapRecordIds() != null) {
			for (final Long id : trackingRecord.getMapRecordIds()) {
				mapRecords.add(mappingService.getMapRecord(id));
			}
		}
		mappingService.close();
		return mapRecords;
	}

	/* see superclass */
	@Override
	public MapUserList getMapUsersForTrackingRecord(TrackingRecord trackingRecord) throws Exception {
		return getMapUsersFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/* see superclass */
	@Override
	public WorkflowStatus getWorkflowStatusForTrackingRecord(TrackingRecord trackingRecord) throws Exception {
		return getWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/* see superclass */
	@Override
	public WorkflowStatus getLowestWorkflowStatusForTrackingRecord(TrackingRecord trackingRecord) throws Exception {
		return getLowestWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/* see superclass */
	@Override
	public MapUserList getMapUsersFromMapRecords(Set<MapRecord> mapRecords) {
		MapUserList mapUserList = new MapUserListJpa();
		for (final MapRecord mr : mapRecords) {
			mapUserList.addMapUser(mr.getOwner());
		}
		return mapUserList;
	}

	/* see superclass */
	@Override
	public WorkflowStatus getWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.NEW;
		for (final MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/* see superclass */
	@Override
	public WorkflowStatus getLowestWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.REVISION;
		for (final MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/* see superclass */
	@Override
	public List<String> computeWorkflowStatusErrors(MapProject mapProject) throws Exception {

		List<String> results = new ArrayList<>();

		// instantiate the mapping service
		MappingService mappingService = new MappingServiceJpa();

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Retrieving tracking records for project " + mapProject.getId() + ", " + mapProject.getName());

		// instantiate a copy of all workflow handlers
		WorkflowNonLegacyPathHandler nonLegacyHandler = new WorkflowNonLegacyPathHandler();
		WorkflowFixErrorPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
		WorkflowQaPathHandler qaHandler = new WorkflowQaPathHandler();
		WorkflowReviewProjectPathHandler reviewHandler = new WorkflowReviewProjectPathHandler();

		// get all the tracking records for this project
		TrackingRecordList trackingRecords = this.getTrackingRecordsForMapProject(mapProject);

		// construct a set of terminology ids for which a tracking record exists
		Set<String> terminologyIdsWithTrackingRecord = new HashSet<>();

		for (final TrackingRecord trackingRecord : trackingRecords.getTrackingRecords()) {

			terminologyIdsWithTrackingRecord.add(trackingRecord.getTerminologyId());

			// instantiate the handler based on tracking record workflow type
			AbstractWorkflowPathHandler handler = null;
			switch (trackingRecord.getWorkflowPath()) {
			case FIX_ERROR_PATH:
				handler = fixErrorHandler;
				break;
			case LEGACY_PATH:
				break;
			case NON_LEGACY_PATH:
				handler = nonLegacyHandler;
				break;
			case QA_PATH:
				handler = qaHandler;
				break;
			case REVIEW_PROJECT_PATH:
				handler = reviewHandler;
				break;
			default:
				results.add("ERROR: Could not determine workflow handler from tracking record for concept "
						+ trackingRecord.getTerminologyId() + " for path: "
						+ trackingRecord.getWorkflowPath().toString());
			}

			ValidationResult result = handler.validateTrackingRecord(trackingRecord);

			if (!result.isValid()) {
				results.add(constructErrorMessageStringForTrackingRecordAndValidationResult(trackingRecord, result));
			}
		}
		Logger.getLogger(WorkflowServiceJpa.class)
				.info("  Checking map records for " + mapProject.getId() + ", " + mapProject.getName());

		// second, check all records for non-publication ready content without
		// tracking record, skip inactive concepts
		final ContentService contentService = new ContentServiceJpa();
		for (final MapRecord mapRecord : mappingService.getMapRecordsForMapProject(mapProject.getId())
				.getMapRecords()) {

			// if not publication ready
			if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
					&& !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {

				final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
						mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
				// if no tracking record found for this concept
				// and the concept is active, then report an error
				if (!terminologyIdsWithTrackingRecord.contains(mapRecord.getConceptId()) && concept != null
						&& concept.isActive()) {
					results.add("Map Record " + mapRecord.getId() + ": " + mapRecord.getWorkflowStatus()
							+ " but no tracking record exists (Concept " + mapRecord.getConceptId() + " "
							+ mapRecord.getConceptName());
				}
			}
		}
		mappingService.close();
		contentService.close();
		return results;

	}

	/* see superclass */
	@Override
	public void computeUntrackedMapRecords(MapProject mapProject) throws Exception {

		final MappingService mappingService = new MappingServiceJpa();

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Retrieving map records for project " + mapProject.getId() + ", " + mapProject.getName());

		final MapRecordList mapRecordsInProject = mappingService.getMapRecordsForMapProject(mapProject.getId());

		Logger.getLogger(WorkflowServiceJpa.class).info("  " + mapRecordsInProject.getCount() + " retrieved");

		// set the reporting interval based on number of tracking records
		int nObjects = 0;
		int nMessageInterval = (int) Math.floor(mapRecordsInProject.getCount() / 10);

		final Set<MapRecord> recordsUntracked = new HashSet<>();

		for (final MapRecord mr : mapRecordsInProject.getIterable()) {

			final TrackingRecord tr = this.getTrackingRecordForMapProjectAndConcept(mapProject, mr.getConceptId());

			// if no tracking record, check that this is a publication ready map
			// record
			if (tr == null) {
				if (!mr.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
						&& !mr.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
					recordsUntracked.add(mr);
				}

			}

			if (++nObjects % nMessageInterval == 0) {
				Logger.getLogger(WorkflowServiceJpa.class).info("  " + nObjects + " records processed, "
						+ recordsUntracked.size() + " unpublished map records without tracking record");
			}
		}

		mappingService.close();

	}

	// /////////////////////////////////////
	// FEEDBACK FUNCTIONS
	// /////////////////////////////////////

	/**
	 * Adds the feedback.
	 * 
	 * @param feedback
	 *            the feedback
	 * @return the feedback
	 */
	@Override
	public Feedback addFeedback(Feedback feedback) {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(feedback);
			tx.commit();
		} else {
			manager.persist(feedback);
		}

		return feedback;
	}

	/* see superclass */
	@Override
	@SuppressWarnings("unchecked")
	public FeedbackList getFeedbacks() {
		List<Feedback> feedbacks = null;
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from FeedbackJpa m");
		// Try query
		feedbacks = query.getResultList();
		FeedbackListJpa feedbackList = new FeedbackListJpa();
		feedbackList.setFeedbacks(feedbacks);
		feedbackList.setTotalCount(feedbacks.size());

		return feedbackList;
	}

	/* see superclass */
	@Override
	public FeedbackConversation addFeedbackConversation(FeedbackConversation conversation) {

		// set the conversation of all elements of this conversation
		conversation.assignToChildren();

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(conversation);
			tx.commit();
		} else {
			manager.persist(conversation);
		}

		return conversation;
	}

	/* see superclass */
	@Override
	public void updateFeedbackConversation(FeedbackConversation conversation) {

		// set the conversation of all elements of this conversation
		conversation.assignToChildren();

		if (getTransactionPerOperation()) {

			tx = manager.getTransaction();

			tx.begin();
			manager.merge(conversation);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(conversation);
		}
	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public FeedbackConversation getFeedbackConversation(Long id) throws Exception {

		// construct query
		javax.persistence.Query query = manager
				.createQuery("select m from FeedbackConversationJpa m where mapRecordId = :recordId");

		// Try query
		query.setParameter("recordId", id);
		List<FeedbackConversation> feedbackConversations = query.getResultList();

		if (feedbackConversations != null && feedbackConversations.size() > 0)
			handleFeedbackConversationLazyInitialization(feedbackConversations.get(0));

		Logger.getLogger(this.getClass()).debug(
				"Returning feedback conversation id... " + ((feedbackConversations != null) ? id.toString() : "null"));

		return feedbackConversations != null && feedbackConversations.size() > 0 ? feedbackConversations.get(0) : null;
	}

	/**
	 * Handle feedback conversation lazy initialization.
	 *
	 * @param feedbackConversation
	 *            the feedback conversation
	 */
	@SuppressWarnings("static-method")
	private void handleFeedbackConversationLazyInitialization(FeedbackConversation feedbackConversation) {
		// handle all lazy initializations
		for (final Feedback feedback : feedbackConversation.getFeedbacks()) {
			feedback.getSender().getName();
			for (final MapUser recipient : feedback.getRecipients())
				recipient.getName();
			for (final MapUser viewedBy : feedback.getViewedBy())
				viewedBy.getName();
		}

	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public FeedbackConversationList getFeedbackConversationsForConcept(Long mapProjectId, String terminologyId)
			throws Exception {

		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapProjectId);
		mappingService.close();

		javax.persistence.Query query = manager
				.createQuery("select m from FeedbackConversationJpa m where terminology = :terminology and"
						+ " terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
				.setParameter("terminology", mapProject.getSourceTerminology())
				.setParameter("terminologyVersion", mapProject.getSourceTerminologyVersion())
				.setParameter("terminologyId", terminologyId);

		List<FeedbackConversation> feedbackConversations = query.getResultList();
		for (final FeedbackConversation feedbackConversation : feedbackConversations) {
			handleFeedbackConversationLazyInitialization(feedbackConversation);
		}

		// set the total count
		FeedbackConversationListJpa feedbackConversationList = new FeedbackConversationListJpa();
		feedbackConversationList.setTotalCount(feedbackConversations.size());

		// extract the required sublist of feedback conversations
		feedbackConversationList.setFeedbackConversations(feedbackConversations);

		return feedbackConversationList;
	}

	/* see superclass */
	@SuppressWarnings({ "unchecked" })
	@Override
	public FeedbackConversationList findFeedbackConversationsForProject(Long mapProjectId, String userName,
			String query, PfsParameter pfsParameter) throws Exception {

		final MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = null;
		try {
			mapProject = mappingService.getMapProject(mapProjectId);
		} catch (Exception e) {
			throw e;
		} finally {
			mappingService.close();
		}

		String modifiedQuery = "";
		if (query.contains(" AND viewed:false"))
			modifiedQuery = query.replace(" AND viewed:false", "");
		else if (query.contains(" AND viewed:true"))
			modifiedQuery = query.replace(" AND viewed:true", "");
		else
			modifiedQuery = query;

		final StringBuilder sb = new StringBuilder();
		sb.append(modifiedQuery).append(" AND ");
		sb.append("mapProjectId:" + mapProject.getId());

		// remove from the query the viewed parameter, if it exists
		// viewed will be handled later because it is on the Feedback object,
		// not the FeedbackConversation object
		sb.append(" AND terminology:" + mapProject.getSourceTerminology() + " AND terminologyVersion:"
				+ mapProject.getSourceTerminologyVersion() + " AND " + "( feedbacks.sender.userName:" + userName
				+ " OR " + "feedbacks.recipients.userName:" + userName + ")");

		Logger.getLogger(getClass()).info("  query = " + sb.toString());

		final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
		if (pfs.getSortField() == null || pfs.getSortField().isEmpty()) {
			pfs.setSortField("lastModified");
		}
		if (query.contains("viewed")) {
			pfs.setStartIndex(-1);
		}

		int[] totalCt = new int[1];
		final List<FeedbackConversation> feedbackConversations = (List<FeedbackConversation>) getQueryResults(
				sb.toString(), FeedbackConversationJpa.class, FeedbackConversationJpa.class, pfsParameter, totalCt);

		if (pfsParameter != null && query.contains("viewed")) {

			// Handle viewed flag
			final List<FeedbackConversation> conversationsToKeep = new ArrayList<>();
			for (final FeedbackConversation fc : feedbackConversations) {
				if (query.contains("viewed:false")) {
					for (final Feedback feedback : fc.getFeedbacks()) {
						final Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
						boolean found = false;
						for (final MapUser user : alreadyViewedBy) {
							if (user.getUserName().equals(userName))
								found = true;
						}
						if (!found)
							conversationsToKeep.add(fc);
					}
				}
				if (query.contains("viewed:true")) {
					boolean found = false;
					for (final Feedback feedback : fc.getFeedbacks()) {
						Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
						for (final MapUser user : alreadyViewedBy) {
							if (user.getUserName().equals(userName)) {
								found = true;
								break;
							}
						}
						if (!found)
							break;
					}
					if (found)
						conversationsToKeep.add(fc);
				}
			}
			totalCt[0] = conversationsToKeep.size();
			feedbackConversations.clear();
			for (int i = pfsParameter.getStartIndex(); i < pfsParameter.getStartIndex() + pfsParameter.getMaxResults()
					&& i < conversationsToKeep.size(); i++) {
				feedbackConversations.add(conversationsToKeep.get(i));
			}

		}

		Logger.getLogger(this.getClass())
				.debug(Integer.toString(feedbackConversations.size()) + " feedbackConversations retrieved");

		for (final FeedbackConversation feedbackConversation : feedbackConversations) {
			handleFeedbackConversationLazyInitialization(feedbackConversation);
		}

		// set the total count
		FeedbackConversationListJpa feedbackConversationList = new FeedbackConversationListJpa();
		feedbackConversationList.setTotalCount(totalCt[0]);

		// extract the required sublist of feedback conversations
		feedbackConversationList.setFeedbackConversations(feedbackConversations);

		return feedbackConversationList;

	}

	/* see superclass */
	@SuppressWarnings("unchecked")
	@Override
	public FeedbackConversationList getFeedbackConversationsForRecord(Long mapRecordId) throws Exception {

		javax.persistence.Query query = manager
				.createQuery("select m from FeedbackConversationJpa m where mapRecordId=:mapRecordId")
				.setParameter("mapRecordId", mapRecordId);

		List<FeedbackConversation> feedbackConversations = query.getResultList();
		for (final FeedbackConversation feedbackConversation : feedbackConversations) {
			handleFeedbackConversationLazyInitialization(feedbackConversation);
		}

		// set the total count
		FeedbackConversationListJpa feedbackConversationList = new FeedbackConversationListJpa();
		feedbackConversationList.setTotalCount(feedbackConversations.size());

		// extract the required sublist of feedback conversations
		feedbackConversationList.setFeedbackConversations(feedbackConversations);

		return feedbackConversationList;
	}

	/* see superclass */
	@Override
	public FeedbackList getFeedbackErrorsForRecord(MapRecord mapRecord) throws Exception {

		List<Feedback> feedbacksWithError = new ArrayList<>();

		// find any feedback conersations for this record
		FeedbackConversationList conversations = this.getFeedbackConversationsForRecord(mapRecord.getId());

		// cycle over feedbacks
		for (final FeedbackConversation conversation : conversations.getIterable()) {
			for (final Feedback feedback : conversation.getFeedbacks()) {
				if (feedback.getIsError()) {
					feedbacksWithError.add(feedback);

				}

			}
		}
		FeedbackList feedbackList = new FeedbackListJpa();
		feedbackList.setFeedbacks(feedbacksWithError);
		return feedbackList;
	}

	/* see superclass */
	@Override
	public void sendFeedbackEmail(String name, String email, String conceptId, String conceptName, String refSetId,
			String message) throws Exception {
		// get to address from config.properties
		Properties config = ConfigUtility.getConfigProperties();
		String feedbackUserRecipient = config.getProperty("mail.smtp.to.feedback.user");
		String baseUrlWebapp = config.getProperty("base.url.webapp");
		String conceptUrl = baseUrlWebapp + "/#/record/conceptId/" + conceptId + "/autologin?refSetId=" + refSetId;

		ConfigUtility.sendEmail(feedbackUserRecipient, "Mapping Tool User Feedback: " + conceptId + "-" + conceptName,
				"User: " + name + "<br>" + "Email: " + email + "<br>" + "Concept: <a href=" + conceptUrl + ">"
						+ conceptId + "- " + conceptName + "</a><br><br>" + message);

	}

	/**
	 * Construct error message string for tracking record and validation result.
	 *
	 * @param trackingRecord
	 *            the tracking record
	 * @param result
	 *            the result
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	private String constructErrorMessageStringForTrackingRecordAndValidationResult(TrackingRecord trackingRecord,
			ValidationResult result) throws Exception {

		StringBuffer message = new StringBuffer();

		message.append("ERROR for Concept " + trackingRecord.getTerminologyId() + ", Path "
				+ trackingRecord.getWorkflowPath().toString() + "\n");

		// record information
		message.append("  Records involved:\n");
		message.append("    " + "id\tUser\tWorkflowStatus\n");

		for (final MapRecord mr : getMapRecordsForTrackingRecord(trackingRecord)) {
			message.append("    " + mr.getId().toString() + "\t" + mr.getOwner().getUserName() + "\t"
					+ mr.getWorkflowStatus().toString() + "\n");
		}

		message.append("  Errors reported:\n");

		for (final String error : result.getErrors()) {
			message.append("    " + error + "\n");
		}

		return message.toString();
	}
}
