package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
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
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Workflow path handler for "non legacy path".
 */
public class WorkflowNonLegacyPathHandler extends AbstractWorkflowPathHandler {

	// The workflow path states defining the Non Legacy Path
	/** The lead finished state. */
	private static WorkflowPathState initialState;

	/** The first specialist editing state. */
	private static WorkflowPathState firstSpecialistEditingState;

	/** The second specialist editing state. */
	private static WorkflowPathState secondSpecialistEditingState;

	/** The conflict detected state. */
	private static WorkflowPathState conflictDetectedState;

	/** The lead editing state. */
	private static WorkflowPathState leadEditingState;

	/** The lead finished state. */
	private static WorkflowPathState leadFinishedState;

	/**
	 * Instantiates an empty {@link WorkflowNonLegacyPathHandler}.
	 */
	public WorkflowNonLegacyPathHandler() {

		setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

		setEmptyWorkflowAllowed(true);

		// STATE: Initial initialState has empty tracking record
		initialState = new WorkflowPathState("Initial State");
		trackingRecordStateToActionMap.put(initialState,
				new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH)));

		// STATE: One specialist has claimed work
		firstSpecialistEditingState = new WorkflowPathState("First Specialist Work");
		firstSpecialistEditingState
				.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW)));
		firstSpecialistEditingState.addWorkflowCombination(
				new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS)));
		firstSpecialistEditingState
				.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.EDITING_DONE)));
		trackingRecordStateToActionMap.put(firstSpecialistEditingState,
				new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH, WorkflowAction.FINISH_EDITING,
						WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// STATE: Two specialists have claimed work
		secondSpecialistEditingState = new WorkflowPathState("Second Specialist Work");
		secondSpecialistEditingState.addWorkflowCombination(
				new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.NEW)));
		secondSpecialistEditingState.addWorkflowCombination(
				new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)));
		secondSpecialistEditingState.addWorkflowCombination(
				new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_DONE)));
		secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_IN_PROGRESS)));
		secondSpecialistEditingState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.EDITING_IN_PROGRESS, WorkflowStatus.EDITING_DONE)));
		trackingRecordStateToActionMap.put(secondSpecialistEditingState, new HashSet<>(
				Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// STATE: Conflict detected
		conflictDetectedState = new WorkflowPathState("Conflict Detected");
		conflictDetectedState.addWorkflowCombination(new WorkflowStatusCombination(
				Arrays.asList(WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_DETECTED)));
		trackingRecordStateToActionMap.put(conflictDetectedState,
				new HashSet<>(Arrays.asList(WorkflowAction.ASSIGN_FROM_SCRATCH, WorkflowAction.FINISH_EDITING,
						WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// STATE: Lead review (incomplete)
		leadEditingState = new WorkflowPathState("Lead Conflict Review Incomplet)");
		leadEditingState
				.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
						WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_NEW)));
		leadEditingState
				.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
						WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_IN_PROGRESS)));
		trackingRecordStateToActionMap.put(leadEditingState, new HashSet<>(
				Arrays.asList(WorkflowAction.FINISH_EDITING, WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// STATE: Lead review (complete)
		leadFinishedState = new WorkflowPathState("Lead Conflict Review Complete");
		leadFinishedState
				.addWorkflowCombination(new WorkflowStatusCombination(Arrays.asList(WorkflowStatus.CONFLICT_DETECTED,
						WorkflowStatus.CONFLICT_DETECTED, WorkflowStatus.CONFLICT_RESOLVED)));
		trackingRecordStateToActionMap.put(leadFinishedState, new HashSet<>(Arrays.asList(WorkflowAction.FINISH_EDITING,
				WorkflowAction.PUBLISH, WorkflowAction.SAVE_FOR_LATER, WorkflowAction.UNASSIGN)));

		// Terminal State: No tracking record
	}

	/* see superclass */
	@Override
	public ValidationResult validateTrackingRecordForActionAndUser(TrackingRecord tr, WorkflowAction action,
			MapUser user) throws Exception {

		// throw exception if action or user are undefined
		if (action == null)
			throw new Exception("Action cannot be null.");

		if (user == null)
			throw new Exception("User cannot be null.");

		// first, validate the tracking record itself
		ValidationResult result = validateTrackingRecord(tr);
		if (!result.isValid()) {
			result.addError("Could not validate action for user due to workflow errors.");
			return result;
		}

		// second, check for CANCEL action -- always valid for this path for any
		// state or user (no-op)
		if (action.equals(WorkflowAction.CANCEL)) {
			return result;
		}

		// third, get the user role for this map project
		MappingService mappingService = new MappingServiceJpa();
		MapUserRole userRole = mappingService.getMapUserRoleForMapProject(user.getUserName(), tr.getMapProjectId());
		mappingService.close();

		// fourth, get the map records and workflow path state from the tracking
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

		// for CREATE_QA_RECORD, only a label is assigned, check role only
		else if (action.equals(WorkflowAction.CREATE_QA_RECORD)) {

			// for creating qa record, only check role
			if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				result.addError("User does not have required role");
			}
		}
		// INITIAL STATE: No specialists have started editing
		// Record requirement : None
		// Permissible actions: ASSIGN_FROM_SCRATCH
		// Minimum role : Specialist
		else if (state.equals(initialState)) {

			// check record
			if (currentRecord != null) {
				result.addError("User's record does not meet requirements");
			}

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				result.addError("User does not meet required role");
			}

			// check action
			if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
				result.addError("Action is not permitted");
			}
		}

		// STATE: One specialist has started editing
		// Record requirement : NEW, EDITING_IN_PROGRESS, EDITING_DONE
		// Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSIGN
		// Minimum role : Specialist
		else if (state.equals(firstSpecialistEditingState)) {

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				result.addError("User does not have required role");
			}

			// check record
			if (currentRecord == null) {

				// check action
				if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
					result.addError("Action is not permitted.");
				}

				// if a record is already owned by user
			} else {

				// check record status
				if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
						&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS)
						&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)) {

					result.addError("User's record does not meet requirements");
				}

				// check action
				if (!action.equals(WorkflowAction.SAVE_FOR_LATER) && !action.equals(WorkflowAction.FINISH_EDITING)
						&& !action.equals(WorkflowAction.UNASSIGN)) {
					result.addError("Action is not permitted.");
				}

			}

			// STATE: Second specialist has begun editing, but both specialists
			// are
			// not finished
			// Record requirement : NEW, EDITING_IN_PROGRESS, EDITING_DONE
			// Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSIGN
			// Minimum role : Specialist

		} else if (state.equals(secondSpecialistEditingState)) {

			// check record
			if (currentRecord == null) {
				result.addError("User must have a record");
			} else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
					&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS)
					&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)) {
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

			// STATE: Conflict detected after both specialists finished

			// Case 1: Lead claiming conflict for review
			// Record requirement : No record
			// Permissible actions: ASSIGN_FROM_SCRATCH
			// Minimum role : Lead

			// Case 2: Specialist modifying record
			// Record requirement : CONFLICT_DETECTED
			// Permissible actions: FINISH_EDITING, SAVE_FOR_LATER, UNASSGIN
			// Minimum role : Specialist
		} else if (state.equals(conflictDetectedState)) {

			// case 1: Lead claiming conflict for review
			if (currentRecord == null) {

				// check role
				if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
					result.addError("User does not have required role");
				}

				// check action
				if (!action.equals(WorkflowAction.ASSIGN_FROM_SCRATCH)) {
					result.addError("Action is not permitted");
				}

				// Case 2: Specialist modifying record
			} else {
				if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
					result.addError("User's record does meet requirements");
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
			}

			// STATE: Lead editing review
			// Record requirement : REVIEW_RESOLVED
			// Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
			// PUBLISHF
			// Minimum role : Lead

			// STATE: Lead editing review
			// Record requirement : REVIEW_NEW, REVIEW_IN_PROGRESS
			// Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
			// Minimum role : Lead
		} else if (state.equals(leadEditingState)) {

			// check record
			if (currentRecord == null) {
				result.addError("User must have a record");
			} else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
					&& !currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)) {
				result.addError("User's record does meet requirements");
			}

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
				result.addError("User does not have required role");
			}

			// check action
			if (!action.equals(WorkflowAction.SAVE_FOR_LATER) && !action.equals(WorkflowAction.FINISH_EDITING)
					&& !action.equals(WorkflowAction.UNASSIGN)) {
				result.addError("Action is not permitted.");
			}

			// STATE: Lead editing review
			// Record requirement : REVIEW_RESOLVED
			// Permissible actions: SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN,
			// PUBLISHF
			// Minimum role : Lead
		} else if (state.equals(leadFinishedState)) {

			// check record
			if (currentRecord == null) {
				result.addError("User must have a record");
			} else if (!currentRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {
				result.addError("User's record does meet requirements");
			}

			// check role
			if (!userRole.hasPrivilegesOf(MapUserRole.LEAD)) {
				result.addError("User does not have required role");
			}

			// check action
			if (!action.equals(WorkflowAction.SAVE_FOR_LATER) && !action.equals(WorkflowAction.FINISH_EDITING)
					&& !action.equals(WorkflowAction.UNASSIGN) && !action.equals(WorkflowAction.PUBLISH)) {
				result.addError("Action is not permitted.");
			}
		} else {
			result.addError("Invalid state/could not determine state");
		}

		if (result.getErrors().size() != 0) {
			result.addError("Error occured in workflow state " + state.getWorkflowStateName());
		}

		return result;
	}

	@Override
	public Set<MapRecord> processWorkflowAction(TrackingRecord trackingRecord, WorkflowAction workflowAction,
			MapProject mapProject, MapUser mapUser, Set<MapRecord> mapRecords, MapRecord mapRecord) throws Exception {

		Logger.getLogger(this.getClass()).info(getName() + ": Processing workflow action by " + mapUser.getName()
				+ ":  " + workflowAction.toString());

		// the set of records returned after processing
		Set<MapRecord> newRecords = new HashSet<>(mapRecords);

		switch (workflowAction) {

		/** The assign from scratch. */
		case ASSIGN_FROM_SCRATCH:

			// create a new record for this tracking record and user
			MapRecord newRecord = createMapRecordForTrackingRecordAndUser(trackingRecord, mapUser);

			// if a "new" tracking record (i.e. prior to conflict detection),
			// add a NEW record
			if (getWorkflowStatusFromMapRecords(mapRecords).compareTo(WorkflowStatus.CONFLICT_DETECTED) < 0) {

				// check that this record is valid to be assigned (i.e. no more
				// than
				// one other specialist assigned)
				if (mapRecords.size() >= 2) {
					throw new Exception(
							"WorkflowNonLegacyPathHandlerException - assignFromScratch:  Two users already assigned");
				}

				newRecord.setWorkflowStatus(WorkflowStatus.NEW);
				Logger.getLogger(WorkflowNonLegacyPathHandler.class).info("NON_LEGACY_PATH: NEW");

				// otherwise, if this is a tracking record with conflict
				// detected, add a CONFLICT_NEW record
			} else if (getWorkflowStatusFromMapRecords(mapRecords).equals(WorkflowStatus.CONFLICT_DETECTED)) {

				newRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_NEW);

				ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
						.forName(mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();
				handler.setMapProject(mapProject);

				MapRecord mapRecord1 = (MapRecord) mapRecords.toArray()[0];
				MapRecord mapRecord2 = (MapRecord) mapRecords.toArray()[1];
				ValidationResult validationResult = handler.compareMapRecords(mapRecord1, mapRecord2);
				newRecord.setReasonsForConflict(validationResult.getConciseErrors());

				// get the origin ids from the tracking record
				for (final MapRecord mr : newRecords) {
					newRecord.addOrigin(mr.getId());
				}
				Logger.getLogger(WorkflowNonLegacyPathHandler.class).info("NON_LEGACY_PATH: CONFLICT_NEW");

			} else {
				throw new Exception("ASSIGN_FROM_SCRATCH on NON_LEGACY_PATH failed.");
			}

			newRecords.add(newRecord);
			break;

		/** The assign from initial record. */
		case ASSIGN_FROM_INITIAL_RECORD:
			throw new Exception("ASSIGN_FROM_INITIAL_RECORD on NON_LEGACY_PATH not permitted");

			/** The unassign. */
		case UNASSIGN:
			Logger.getLogger(WorkflowNonLegacyPathHandler.class).info("Unassign: NON_LEGACY_PATH");

			MapRecord assignedRecord = getCurrentMapRecordForUser(newRecords, mapUser);

			if (assignedRecord == null)
				throw new Exception("unassign called for concept that does not have specified user assigned");

			// remove this record from the tracking record
			newRecords.remove(assignedRecord);

			// where a conflict is detected, two cases to handle
			// (1) any lead-claimed resolution record is now invalid, and
			// should
			// be deleted (no conflict remaining)
			// (2) a specialist's record is now not in conflict, and should
			// be
			// reverted to EDITING_DONE
			if (assignedRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {

				for (final MapRecord mr : newRecords) {

					// if another specialist's record, revert to EDITING_DONE
					if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
						mr.setWorkflowStatus(WorkflowStatus.EDITING_DONE);
					}
				}
			}

			break;

		/** The save for later. */
		case SAVE_FOR_LATER:
			MapRecord recordToSave = this.getCurrentMapRecordForUser(newRecords, mapUser);
			if (recordToSave.getWorkflowStatus().equals(WorkflowStatus.NEW))
				recordToSave.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
			if (recordToSave.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW))
				recordToSave.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
			break;

		/** The finish editing. */
		case FINISH_EDITING:
			// case 1: A specialist is finished with a record
			if (getWorkflowStatusFromMapRecords(mapRecords).compareTo(WorkflowStatus.CONFLICT_DETECTED) <= 0) {

				Logger.getLogger(WorkflowNonLegacyPathHandler.class)
						.info("NON_LEGACY_PATH - New finished record, checking for other records");

				// set this record to EDITING_DONE
				mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);

				// check if two specialists have completed work (lowest workflow
				// status is EDITING_DONE, highest workflow status is
				// CONFLICT_DETECTED)
				if (getLowestWorkflowStatusFromMapRecords(mapRecords).compareTo(WorkflowStatus.EDITING_DONE) >= 0
						&& mapRecords.size() == 2) {

					Logger.getLogger(this.getClass()).info("NON_LEGACY_PATH - Two records found");

					final Iterator<MapRecord> recordIter = mapRecords.iterator();
					final MapRecord mapRecord1 = recordIter.next();
					final MapRecord mapRecord2 = recordIter.next();

					ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
							.forName(mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();
					handler.setMapProject(mapProject);

					// TODO Decide how to do comparisons
					final ValidationResult validationResult = handler.compareMapRecords(mapRecord1, mapRecord2);

					// if map records validation is successful, publish
					if (validationResult.isValid()) {

						Logger.getLogger(WorkflowNonLegacyPathHandler.class)
								.info("NON_LEGACY_PATH - No conflicts detected.");

						newRecords = processWorkflowAction(trackingRecord, WorkflowAction.PUBLISH, mapProject, mapUser,
								mapRecords, null);

					} else {

						Logger.getLogger(WorkflowNonLegacyPathHandler.class)
								.info("NON_LEGACY_PATH - Conflicts detected");

						// conflict detected, change workflow status of all
						// records (if not a lead's existing conflict record)
						// and
						// update records
						for (final MapRecord mr : newRecords) {
							if (mr.getWorkflowStatus().compareTo(WorkflowStatus.CONFLICT_DETECTED) <= 0)
								mr.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
						}
					}
					// otherwise, only one specialist has finished work, do
					// nothing else
				} else {
					Logger.getLogger(WorkflowNonLegacyPathHandler.class)
							.info("NON_LEGACY_PATH - Only this specialist has completed work");
				}

				// case 2: A lead is finished with a conflict resolution
				// Determined by workflow status of:
				// CONFLICT_NEW (i.e. conflict was resolved immediately)
				// CONFLICT_IN_PROGRESS (i.e. conflict had been previously saved
				// for later)
				// CONFLICT_RESOLVED (i.e. conflict marked resolved, but lead
				// revisited)
			} else if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
					|| mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)
					|| mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {

				Logger.getLogger(getClass()).info("NON_LEGACY_PATH - Conflict resolution detected");

				// set the lead's record to CONFLICT_RESOLVED
				mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_RESOLVED);

			} else {
				throw new Exception("finishEditing failed! Invalid workflow status combination on record(s)");
			}
			break;

		/** The publish */
		case PUBLISH:

			Logger.getLogger(WorkflowNonLegacyPathHandler.class).info("NON_LEGACY_PATH - Publishing resolved conflict");

			// Requirements for NON_LEGACY_PATH publish action
			// - 2 records marked EDITING_DONE
			// *OR*
			// - 1 record marked CONFLICT_RESOLVED
			// - 2 records marked CONFLICT_DETECTED

			// if two map records, must be two EDITING_DONE or CONFLICT_DETECTED
			// records
			// with publish called by finishEditing
			if (mapRecords.size() == 2) {

				// check assumption: records are both marked EDITING_DONE or
				// CONFLICT_DETECTED
				for (final MapRecord mr : mapRecords) {
					if (!mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)
							&& !mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED))
						throw new Exception(
								"Publish called, expected two matching specialist records marked EDITING_DONE or CONFLICT_DETECTED, but found record with status "
										+ mr.getWorkflowStatus().toString());
				}

				// check assumption: records are not in conflict
				// note that this duplicates the call in finishEditing
				Iterator<MapRecord> iter = mapRecords.iterator();
				MapRecord mapRecord1 = iter.next();
				MapRecord mapRecord2 = iter.next();

				ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
						.forName(mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();
				handler.setMapProject(mapProject);

				// TODO Another comparison call using default project specific
				// handler -- decide behavior
				if (!handler.compareMapRecords(mapRecord1, mapRecord2).isValid()) {
					throw new Exception(
							"Publish called for two matching specialist records, but the records did not pass comparator validation checks");
				}

				// deep copy the record and mark the new record
				// READY_FOR_PUBLICATION
				MapRecord publishedRecord = new MapRecordJpa(mapRecord1, false);
				publishedRecord.setOwner(mapUser);
				publishedRecord.setLastModifiedBy(mapUser);
				publishedRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

				// construct and set the new origin ids
				final Set<Long> originIds = new HashSet<>();
				originIds.add(mapRecord1.getId());
				originIds.add(mapRecord2.getId());
				originIds.addAll(mapRecord1.getOriginIds());
				originIds.addAll(mapRecord2.getOriginIds());
				publishedRecord.setOriginIds(originIds);

				// clear the records and add a single record owned by
				// this user -- note that this will remove any existing
				// conflict records
				newRecords.clear();
				newRecords.add(publishedRecord);

				Logger.getLogger(WorkflowNonLegacyPathHandler.class)
						.info("publish- NON_LEGACY_PATH - Creating READY_FOR_PUBLICATION record "
								+ publishedRecord.toString());

			} else if (mapRecords.size() == 3) {

				// Check assumption: owned record is CONFLICT_RESOLVED
				if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {
					throw new Exception(
							"Publish called on NON_LEGACY_PATH for map record not marked as CONFLICT_RESOLVED");
				}

				// Check assumption: two CONFLICT_DETECTED records
				int nConflictRecords = 0;
				for (final MapRecord mr : mapRecords) {
					if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED))
						nConflictRecords++;
				}

				if (nConflictRecords != 2) {
					throw new Exception("Bad workflow state for concept " + mapRecord.getConceptId()
							+ ":  CONFLICT_RESOLVED is not accompanied by two CONFLICT_DETECTED records");
				}

				// cycle over the previously existing records
				for (final MapRecord mr : mapRecords) {

					// remove the CONFLICT_DETECTED records from the revised set
					if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
						newRecords.remove(mr);

						// set the CONFLICT_NEW or CONFLICT_IN_PROGRESS record
						// to READY_FOR_PUBLICATION and update
					} else {
						mr.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
					}
				}

				// otherwise, bad workflow state, throw exception
			} else {
				throw new Exception("Bad workflow state for concept " + mapRecord.getConceptId()
						+ ":  Expected either two or three records, but found " + mapRecords.size());
			}

			break;

		/** Cancel work */
		case CANCEL:
			// re-retrieve the records for this tracking record and return those
			// used to ensure no spurious alterations from serialization are
			// saved
			// and therefore reflected in the audit trail

			newRecords.clear();
			MappingService mappingService = new MappingServiceJpa();
			for (final Long id : trackingRecord.getMapRecordIds()) {
				newRecords.add(mappingService.getMapRecord(id));
			}
			mappingService.close();
			break;

		/** Create qa record */
		case CREATE_QA_RECORD:
			throw new Exception("CREATE_QA_RECORD on NON_LEGACY_PATH not allowed");

		default:
			throw new Exception("NON_LEGACY_PATH received unknown workflow action: " + workflowAction);

		}

		Logger.getLogger(this.getClass()).debug("NON_LEGACY_PATH records after completion");
		for (MapRecord mr : newRecords) {
			Logger.getLogger(this.getClass()).debug("  " + mr.toString());
		}

		return newRecords;
	}

	@Override
	public String getName() {
		return "NON_LEGACY_PATH";
	}

	@Override
	public SearchResultList findAvailableWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole,
			String query, PfsParameter pfsParameter, WorkflowService workflowService) throws Exception {
		
		Logger.getLogger(this.getClass()).info(getName() + ": findAvailableWork for project " + mapProject.getName() + " and user " + mapUser.getUserName());
		
		final SearchResultList availableWork = new SearchResultListJpa();

		final StringBuilder sb = new StringBuilder();

		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:" + getName());

		// add the query terms specific to findAvailableWork
		// - must be NON_LEGACY PATH
		// - any tracking record with no assigned users is by definition
		// available
		// - any tracking record with one assigned user on NON_LEGACY_PATH
		// with workflowstatus NEW, EDITING_IN_PROGRESS, or EDITING_DONE.
		// Assigned user must not be this user
		
		switch (userRole) {
		
		case LEAD:
			sb.append(" AND userAndWorkflowStatusPairs:CONFLICT_DETECTED_*");
			sb.append(" AND NOT (" + "userAndWorkflowStatusPairs:CONFLICT_NEW_* OR "
					+ "userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_* OR "
					+ "userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");
			break;
	
		case SPECIALIST:
			// Handle "team" based assignment
			if (mapProject.isTeamBased() && mapUser.getTeam() != null && !mapUser.getTeam().isEmpty()) {
				// Use "AND NOT" clauses for all members matching my user's
				// team.
				sb.append(" AND (assignedUserCount:0 OR (assignedUserCount:1 ");
				for (final MapUser user : workflowService.getMapUsersForTeam(mapUser.getTeam()).getMapUsers()) {
					sb.append(" AND NOT assignedUserNames:" + user.getUserName());
				}
				sb.append(") )");
			} else {
				sb.append(" AND (assignedUserCount:0 OR " + "(assignedUserCount:1 AND NOT assignedUserNames:"
						+ mapUser.getUserName() + "))");
			}
			break;
	
		default:
			throw new Exception(getName() + ", findAvailableWork: invalid project role " + userRole);
		
		}

	
		int[] totalCt = new int[1];
		@SuppressWarnings("unchecked")
		final List<TrackingRecord> results = (List<TrackingRecord>) ((RootServiceJpa) workflowService).getQueryResults(
				sb.toString(), TrackingRecordJpa.class, TrackingRecordJpa.class, pfsParameter, totalCt);

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

	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedWork(MapProject mapProject, MapUser mapUser, MapUserRole userRole, String query,
			PfsParameter pfsParameter, WorkflowService workflowService) throws Exception {

		Logger.getLogger(this.getClass()).info(getName() + ": findAssignedWork for project " + mapProject.getName() + " and user " + mapUser.getUserName());
		
		// instantiate the assigned work search results
		SearchResultList assignedWork = new SearchResultListJpa();

		// build the initial query
		final StringBuilder sb = new StringBuilder();
		if (query != null && !query.isEmpty() && !query.equals("null")) {
			sb.append(query).append(" AND ");
		}
		sb.append("mapProjectId:" + mapProject.getId() + " AND workflowPath:" + getName());

		// determine the query restrictions, type and pfs parameter
		final String type = pfsParameter.getQueryRestriction() != null ? pfsParameter.getQueryRestriction() : "";
		final List<TrackingRecord> results;
		final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
		pfs.setQueryRestriction(null);
		int[] totalCt = new int[1];

		// switch on user role (Specialist or Lead)
		switch (userRole) {

		// for lead-level work, get assigned conflicts
		case LEAD:

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

			results = (List<TrackingRecord>) workflowService.getQueryResults(
					sb.toString(),
					TrackingRecordJpa.class, 
					TrackingRecordJpa.class, 
					pfs, 
					totalCt);
			assignedWork.setTotalCount(totalCt[0]);

			for (final TrackingRecord tr : results) {
				final SearchResult result = new SearchResultJpa();

				final Set<MapRecord> mapRecords = workflowService.getMapRecordsForTrackingRecord(tr);

				// get the map record assigned to this user
				MapRecord mapRecord = null;
				for (final MapRecord mr : mapRecords) {
					if (mr.getOwner().equals(mapUser)) {

						// SEE MAP-617:
						// Lower level record may exist with same owner,
						// only
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
					assignedWork.addSearchResult(result);
				}
			}

			break;

		// for specialist-level work, get assigned work
		case SPECIALIST:

			// add the query terms specific to findAssignedWork
			// - user and workflowStatus must exist in a pair of form:
			// workflowStatus_userName, e.g. NEW_dmo or
			// EDITING_IN_PROGRESS_kli
			// - modify search term based on pfs parameter query restriction
			// field
			// * default: NEW, EDITING_IN_PROGRESS,
			// EDITING_DONE/CONFLICT_DETECTED
			// * NEW: NEW
			// * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

			// add terms based on query restriction
			switch (type) {
			case "NEW":
				sb.append(" AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName());
				break;
			case "EDITING_IN_PROGRESS":
				sb.append(" AND userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName());
				break;
			case "EDITING_DONE":
				sb.append(" AND (userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName()
						+ " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_" + mapUser.getUserName() + ")");
				break;
			default:
				sb.append(" AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
						+ " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName()
						+ " OR userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName()
						+ " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_" + mapUser.getUserName() + ")");
				break;
			}

			// add terms to exclude concepts that a lead has claimed
			sb.append(" AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_*"
					+ " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*"
					+ " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)");

				results = (List<TrackingRecord>) workflowService.getQueryResults(sb.toString(), TrackingRecordJpa.class,
					TrackingRecordJpa.class, pfs, totalCt);
			assignedWork.setTotalCount(totalCt[0]);

			for (final TrackingRecord tr : results) {

				// instantiate the result list
				final SearchResult result = new SearchResultJpa();

				// get the map records associated with this tracking record
				final Set<MapRecord> mapRecords = workflowService.getMapRecordsForTrackingRecord(tr);

				// get the map record assigned to this user
				MapRecord mapRecord = null;

				// SEE BELOW/MAP-617
				WorkflowStatus mapLeadAlternateRecordStatus = null;
				for (final MapRecord mr : mapRecords) {

					if (mr.getOwner().equals(mapUser)) {

						// if this lead has review or conflict work, set the
						// flag
						if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
								|| mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)) {

							mapLeadAlternateRecordStatus = mr.getWorkflowStatus();

							// added to prevent user from getting REVISION
							// record
							// back on FIX_ERROR_PATH
							// yet another problem related to leads being
							// able
							// to
							// serve as dual roles
							// TODO This should be removed
						} else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
							// do nothing

							// otherwise, this is the
							// specialist/concept-level
							// work
						} else {
							mapRecord = mr;
						}
					}
				}

				// if no record and no review or conflict work was found,
				// throw
				// error
				if (mapRecord == null) {
					throw new Exception("Failed to retrieve assigned work:  no map record found for user "
							+ mapUser.getUserName() + " and concept " + tr.getTerminologyId());

				} else {

					// alter the workflow status if a higher-level record
					// exists
					// for
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
			break;
		default:
			throw new Exception("Cannot retrieve work for NON_LEGACY_PATH for user role " + userRole);
		}

		return assignedWork;
	}

}
