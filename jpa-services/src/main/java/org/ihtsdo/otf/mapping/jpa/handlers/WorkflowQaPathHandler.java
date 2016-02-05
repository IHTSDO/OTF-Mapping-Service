package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

// TODO: Auto-generated Javadoc
/**
 * Workflow path handler for "qa path".
 */
public class WorkflowQaPathHandler extends AbstractWorkflowPathHandler {

	// The workflow path states defining the QA Path Workflow
	/** The finished state. */
	private static WorkflowPathState qaNeededState;

	/** The editing state. */
	private static WorkflowPathState editingState;

	/** The finished state. */
	private static WorkflowPathState finishedState;

	/**
	 * Instantiates an empty {@link WorkflowQaPathHandler}.
	 */
	public WorkflowQaPathHandler() {

		setWorkflowPath(WorkflowPath.QA_PATH);

		setEmptyWorkflowAllowed(false);

		// STATE: Initial state has no tracking record
		// Only valid action is CREATE_QA_RECORD

		// workflow states representing a record marked for qa and the original
		// published record
		qaNeededState = new WorkflowPathState("QA_NEEDED");
		qaNeededState.addWorkflowCombination(
				new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED)));
		trackingRecordStateToActionMap.put(qaNeededState,
				new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH, WorkflowAction.UNASSIGN)));

		// workflow states representing record marked for revision, specialist
		// work,
		// and lead QA (incomplete)
		editingState = new WorkflowPathState("QA_NEW/QA_IN_PROGRESS");
		editingState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED, WorkflowStatus.QA_NEW)));
		editingState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED, WorkflowStatus.QA_IN_PROGRESS)));
		trackingRecordStateToActionMap.put(editingState, new HashSet<>(
				Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// workflow finishedStates representing record marked for revision,
		// specialist work,
		// and lead QA (complete)
		finishedState = new WorkflowPathState("QA_RESOLVED");
		finishedState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.REVISION, WorkflowStatus.QA_NEEDED, WorkflowStatus.QA_RESOLVED)));
		trackingRecordStateToActionMap.put(finishedState, new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
				WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// final state: no tracking record, one READY_FOR_PUBLICATION record
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler#
	 * validateTrackingRecordForActionAndUser(org.ihtsdo.otf.mapping.workflow.
	 * TrackingRecord, org.ihtsdo.otf.mapping.helpers.WorkflowAction,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	/* see superclass */
	@Override
	public ValidationResult validateTrackingRecordForActionAndUser(TrackingRecord tr, WorkflowAction action,
			MapUser user) throws Exception {

		// first, validate the tracking record itself
		ValidationResult result = validateTrackingRecord(tr);
		if (!result.isValid()) {
			result.addError("Could not validate action for user due to workflow errors.");

			return result;
		}

		// check for CANCEL action -- always valid for this path for any
		// state or user (no-op)
		if (action.equals(WorkflowAction.CANCEL)) {
			return result;
		}

		// check for CREATE_QA_RECORD action -- this is always valid, as it
		// merely
		// applies labels to concepts that are already in the workflow
		if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {
			return result;
		}

		// get the user role for this map project
		MappingService mappingService = new MappingServiceJpa();
		MapUserRole userRole = mappingService.getMapUserRoleForMapProject(user.getUserName(), tr.getMapProjectId());
		mappingService.close();

		// get the map records and workflow path state from the tracking
		// record
		MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
		MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
		WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

		// /////////////////////////////////
		// Switch on workflow path state //
		// /////////////////////////////////
		if (state == null) {
			result.addError("Could not determine workflow path state for tracking record");
		}

		else if (state.equals(qaNeededState)) {

			// check record -- null means none assigned
			if (currentRecord == null) {

				// check role
				if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
					result.addError("User does not have required role");
				}

				// check action
				if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
					result.addError("Action is not permitted.");
				}
			} else {

				// check role
				// qa user does not have role requirements for unassign

				// check action
				if (!action.equals(WorkflowAction.UNASSIGN)) {
					result.addError("Action is not permitted.");
				}

			}

			// STATE: Specialist level work
			// Record requirement : NEW, EDITING_IN_PROGRESS
			// Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
			// Minimum role : Specialist
		} else if (state.equals(editingState)) {

			// check record
			if (currentRecord == null) {
				result.addError("User must have a record");
			} else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
					&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)) {
				result.addError("User's record does not meet requirements");
			}

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				result.addError("User does not have required role");
			}

			// check action
			if (!action.equals(WorkflowAction.SAVE_FOR_LATER) && !action.equals(WorkflowAction.FINISH_EDITING)
					&& !action.equals(WorkflowAction.UNASSIGN)) {
				result.addError("Action is not permitted.");
			}

		} else if (state.equals(finishedState)) {
			// check record
			if (currentRecord == null) {
				result.addError("User must have a record");
			} else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)) {
				result.addError("User's record does meet requirements");
			}

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				result.addError("User does not have required role");
			}

			// check action
			if (!action.equals(WorkflowAction.SAVE_FOR_LATER) && !action.equals(WorkflowAction.FINISH_EDITING)
					&& !action.equals(WorkflowAction.UNASSIGN) && !action.equals(WorkflowAction.PUBLISH)) {
				result.addError("Action is not permitted.");
			}
		}

		if (result.getErrors().size() != 0) {
			result.addError("Error occured in workflow state " + state.getWorkflowStateName());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler#getName()
	 */
	@Override
	public String getName() {
		return "QA_PATH";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler#
	 * findAvailableWork(org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser,
	 * org.ihtsdo.otf.mapping.helpers.MapUserRole, java.lang.String,
	 * org.ihtsdo.otf.mapping.helpers.PfsParameter,
	 * org.ihtsdo.otf.mapping.services.WorkflowService)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole,
			String query, PfsParameter pfsParameter, WorkflowService workflowService) throws Exception {
		Logger.getLogger(this.getClass()).info(getName() + ": findAvailableWork for project " + mapProject.getName()
				+ " and user " + mapUser.getUserName());

		SearchResultList availableWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:QA_PATH");

		// add the query terms specific to findAvailableReviewWork
		// - a user (any) and workflowStatus pair of QA_NEEDED_userName
		// exists
		// - the QA_NEEDED pair is not for this user (i.e. user can't review
		// their own work, UNLESS there is only one lead on the project
		// - user and workflowStatus pairs of
		// CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

		// must have a QA_NEEDED tag with any user
		sb.append(" AND userAndWorkflowStatusPairs:QA_NEEDED_*");

		// there must not be an already claimed review record
		sb.append(" AND NOT (userAndWorkflowStatusPairs:QA_NEW_*" + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_*"
				+ " OR userAndWorkflowStatusPairs:QA_RESOLVED_*" + ")");

		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);
		availableWork.setTotalCount(totalCt[0]);
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			availableWork.addSearchResult(result);
		}
		return availableWork;

	}

	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole, String query,
			PfsParameter pfsParameter, WorkflowService workflowService) throws Exception {

		SearchResultList assignedWork = new SearchResultListJpa();
		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:" + getName());

		final String type = pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";

		if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
			throw new Exception("Specialist role or above required for QA Work");
		}

		// add terms based on query restriction
		switch (type) {
		case "QA_NEW":
			sb.append(" AND userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName());

			break;
		case "QA_IN_PROGRESS":
			sb.append(" AND userAndWorkflowStatusPairs:QA_IN_PROGRESS_" + mapUser.getUserName());
			break;
		case "QA_RESOLVED":
			sb.append(" AND userAndWorkflowStatusPairs:QA_RESOLVED_" + mapUser.getUserName());
			break;
		default:
			sb.append(" AND (userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_" + mapUser.getUserName()
					+ " OR userAndWorkflowStatusPairs:QA_RESOLVED_" + mapUser.getUserName() + ")");
			break;
		}

		final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
		pfs.setQueryRestriction(null);
		int[] totalCt = new int[1];
		final List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(),
				TrackingRecordJpa.class, TrackingRecordJpa.class, pfs, totalCt);
		assignedWork.setTotalCount(totalCt[0]);

		for (final TrackingRecord tr : results) {
			final SearchResult result = new SearchResultJpa();
			final Set<MapRecord> mapRecords = workflowService.getMapRecordsForTrackingRecord(tr);

			// get the map record assigned to this user -- note: can only be one
			// if QA Path properly used
			MapRecord mapRecord = null;
			for (final MapRecord mr : mapRecords) {
				if (mr.getOwner().equals(mapUser)) {
					mapRecord = mr;
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
			assignedWork.addSearchResult(result);
		}
		return assignedWork;
	}

	public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord, WorkflowAction workflowAction,
			MapProject mapProject, MapUser mapUser, Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {

		Logger.getLogger(this.getClass()).info(
				getName() + ": Processing workflow action by " + mapUser.getName() + ":  " + workflowAction.toString());

		// the set of records returned after processing
		Set<MapRecord> newRecords = new HashSet<>(mapRecords);

		switch (workflowAction) {
		case ASSIGN_FROM_INITIAL_RECORD:
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info("assignFromInitialRecord:  QA_PATH");

			// case 1 : User claims a PUBLISHED or READY_FOR_PUBLICATION record
			// to start qa on.
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
					|| mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {

				// check that only one record exists for this tracking record
				if (!(trackingRecord.getMapRecordIds().size() == 1)) {
					throw new Exception(
							"DefaultProjectSpecificHandlerException - assignFromInitialRecord: More than one record exists for QA_PATH assignment.");
				}

				// deep copy the map record
				final MapRecord newRecord = new MapRecordJpa(mapRecord, false);

				// set origin ids
				newRecord.addOrigin(mapRecord.getId());
				newRecord.addOrigins(mapRecord.getOriginIds());

				// set other relevant fields
				// get QA User MapUser
				newRecord.setOwner(mapUser);
				newRecord.setLastModifiedBy(mapUser);
				newRecord.setWorkflowStatus(WorkflowStatus.QA_NEEDED);

				// add the record to the list
				newRecords.add(newRecord);

				// set the workflow status of the old record to review and add
				// it to
				// new records
				mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
				newRecords.add(mapRecord);

			}

			break;
		case ASSIGN_FROM_SCRATCH:
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info("Assigning concept along QA_PATH");

			// set origin id and copy labels
			for (final MapRecord record : newRecords) {
				// if
				// (record.getWorkflowStatus().equals(WorkflowStatus.REVISION))
				// mapRecord.addOrigin(record.getId());
				if (record.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
					record.setLabels(record.getLabels());
					record.addOrigin(record.getId());
					// set workflow status to review new
					record.setWorkflowStatus(WorkflowStatus.QA_NEW);
				}
			}

			break;
		case CANCEL:
			newRecords.clear();
			MappingService mappingService = new MappingServiceJpa();
			for (final Long id : trackingRecord.getMapRecordIds()) {
				newRecords.add(mappingService.getMapRecord(id));
			}
			mappingService.close();
			break;

		case CREATE_QA_RECORD:
			break;
		case FINISH_EDITING:
			break;
		case PUBLISH:
			break;
		case SAVE_FOR_LATER:
			break;
		case UNASSIGN:
			break;
		default:
			break;
		}

		return newRecords;
	}

}
