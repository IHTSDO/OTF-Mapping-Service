package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecordJpa;

/**
 * Default workflow service implementation.
 */
public class WorkflowServiceJpa extends RootServiceJpa implements
		WorkflowService {

	/** The manager. */
	private EntityManager manager;

	/** The transaction per operation. */
	private boolean transactionPerOperation = true;

	/** The transaction entity. */
	private EntityTransaction tx;

	/**
	 * Instantiates an empty {@link WorkflowServiceJpa}.
	 */
	public WorkflowServiceJpa() {
	  super();

		// created on each instantiation
		manager = factory.createEntityManager();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#addWorkflowTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public WorkflowTrackingRecord addWorkflowTrackingRecord(
			WorkflowTrackingRecord workflowTrackingRecord) throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(workflowTrackingRecord);
			tx.commit();
		} else {
			manager.persist(workflowTrackingRecord);
		}

		return workflowTrackingRecord;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#removeWorkflowTrackingRecord
	 * (java.lang.Long)
	 */
	@Override
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId)
			throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			WorkflowTrackingRecord ma = manager.find(
					WorkflowTrackingRecordJpa.class, workflowTrackingRecordId);

			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			WorkflowTrackingRecord ma = manager.find(
					WorkflowTrackingRecordJpa.class, workflowTrackingRecordId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#updateWorkflowTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void updateWorkflowTrackingRecord(WorkflowTrackingRecord record)
			throws Exception {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(record);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflowTrackingRecords
	 * ()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecords() {

		return manager.createQuery(
				"select tr from WorkflowTrackingRecordJpa tr").getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getWorkflowTrackingRecordsForMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecordsForMapProject(
			MapProject mapProject) {
		return manager
				.createQuery(
						"select tr from WorkflowTrackingRecordJpa tr where mapProject_id = :mapProjectId")
				.setParameter("mapProjectId", mapProject.getId())
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflowTrackingRecord
	 * (org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.rf2.Concept)
	 */
	@Override
	public WorkflowTrackingRecord getWorkflowTrackingRecord(
			MapProject mapProject, Concept concept) {

		javax.persistence.Query query = manager
				.createQuery(
						"select tr from WorkflowTrackingRecordJpa tr where mapProject_id = :mapProjectId and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminologyId", concept.getTerminologyId());

		try {
			return (WorkflowTrackingRecord) query.getSingleResult();

		} catch (NoResultException e) {
			Logger.getLogger(this.getClass())
					.debug("WorkflowService.getWorkflowTrackingRecord(): Concept query for terminologyId = "
							+ concept.getTerminologyId()
							+ ", mapProjectId = "
							+ mapProject.getId().toString()
							+ " returned no results.");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableWork(org.
	 * ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public SearchResultList findAvailableWork(MapProject mapProject,
			MapUser mapUser, PfsParameter pfsParameter) {

		List<WorkflowTrackingRecord> availableWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// if this tracking record does not have this user assigned to it
			// AND the total users assigned is less than 2
			// AND the workflow status is less than or equal to EDITING_DONE
			// This will eventually be a project specific check (i.e. for legacy
			// handling)
			if (!trackingRecord.getAssignedUsers().contains(mapUser)
					&& trackingRecord.getAssignedUsers().size() < 2
					&& trackingRecord.getWorkflowStatus().compareTo(
							WorkflowStatus.EDITING_DONE) <= 0) {

				availableWork.add(trackingRecord);
			}
		}

		// sort the tracking records
		Collections.sort(availableWork,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord tr1,
							WorkflowTrackingRecord tr2) {
						return tr1.getSortKey().compareTo(tr2.getSortKey());
					}
				});

		// set the default start index and the max results
		int startIndex = 0;
		int toIndex = availableWork.size();

		// if a pfsParameter has been supplied, get the start index and max
		// results
		if (pfsParameter != null) {
			startIndex = pfsParameter.getStartIndex() == -1 ? 0 : pfsParameter
					.getStartIndex();
			toIndex = pfsParameter.getMaxResults() == -1 ? availableWork.size()
					: Math.min(availableWork.size(),
							startIndex + pfsParameter.getMaxResults());
		}

		// construct search result list
		results.setTotalCount(availableWork.size());

		for (WorkflowTrackingRecord tr : availableWork.subList(startIndex,
				toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			results.addSearchResult(result);
		}

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableConflicts
	 * (org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public SearchResultList findAvailableConflicts(MapProject mapProject,
			MapUser mapUser, PfsParameter pfsParameter) {

		List<WorkflowTrackingRecord> availableConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records for this project
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// if this record is marked conflict detected
			if (trackingRecord.getWorkflowStatus().equals(
					WorkflowStatus.CONFLICT_DETECTED)) {

				// add the search result
				availableConflicts.add(trackingRecord);
			}
		}

		// sort the tracking records
		Collections.sort(availableConflicts,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord tr1,
							WorkflowTrackingRecord tr2) {
						return tr1.getSortKey().compareTo(tr2.getSortKey());
					}
				});

		// set the default start index and the max results
		int startIndex = 0;
		int toIndex = availableConflicts.size();

		// if a pfsParameter has been supplied, get the start index and max
		// results
		if (pfsParameter != null) {
			startIndex = pfsParameter.getStartIndex() == -1 ? 0 : pfsParameter
					.getStartIndex();
			toIndex = pfsParameter.getMaxResults() == -1 ? availableConflicts
					.size() : Math.min(availableConflicts.size(), startIndex
					+ pfsParameter.getMaxResults());
		}

		// construct search result list
		results.setTotalCount(availableConflicts.size());

		for (WorkflowTrackingRecord tr : availableConflicts.subList(startIndex,
				toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			results.addSearchResult(result);
		}

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAssignedWork(org.ihtsdo
	 * .otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public SearchResultList findAssignedWork(MapProject mapProject,
			MapUser mapUser, PfsParameter pfsParameter) {

		List<WorkflowTrackingRecord> assignedWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// cycle over map records to find the NEW or EDITING_IN_PROGRESS
			// record assigned to this user
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				if (mapRecord.getOwner().equals(mapUser)
						&& mapRecord.getWorkflowStatus().compareTo(
								WorkflowStatus.EDITING_DONE) < 0) {
					assignedWork.add(trackingRecord);
				}
			}
		}

		// sort the tracking records
		Collections.sort(assignedWork,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord tr1,
							WorkflowTrackingRecord tr2) {
						return tr1.getSortKey().compareTo(tr2.getSortKey());
					}
				});

		// set the default start index and themax results
		int startIndex = 0;
		int toIndex = assignedWork.size();

		// if a pfsParameter has been supplied, get the start index and max
		// results
		if (pfsParameter != null) {
			startIndex = pfsParameter.getStartIndex() == -1 ? 0 : pfsParameter
					.getStartIndex();
			toIndex = pfsParameter.getMaxResults() == -1 ? assignedWork.size()
					: Math.min(assignedWork.size(),
							startIndex + pfsParameter.getMaxResults());
		}

		// construct the search results
		results.setTotalCount(assignedWork.size());

		for (WorkflowTrackingRecord trackingRecord : assignedWork.subList(
				startIndex, toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(trackingRecord.getTerminologyId());
			result.setValue(trackingRecord.getDefaultPreferredName());

			// get the record id
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
				}
			}

			results.addSearchResult(result);
		}

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAssignedConflicts(
	 * org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public SearchResultList findAssignedConflicts(MapProject mapProject,
			MapUser mapUser, PfsParameter pfsParameter) {

		List<WorkflowTrackingRecord> assignedConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// cycle over map records to find the CONFLICT_DETECTED record
			// assigned to this user
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				if (mapRecord.getOwner().equals(mapUser)
						&& mapRecord.getWorkflowStatus().compareTo(
								WorkflowStatus.CONFLICT_IN_PROGRESS) == 0) {
					assignedConflicts.add(trackingRecord);
				}
			}
		}

		// sort the tracking records
		Collections.sort(assignedConflicts,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord tr1,
							WorkflowTrackingRecord tr2) {
						return tr1.getSortKey().compareTo(tr2.getSortKey());
					}
				});

		// set the default start index and themax results
		int startIndex = 0;
		int toIndex = assignedConflicts.size();

		// if a pfsParameter has been supplied, get the start index and max
		// results
		if (pfsParameter != null) {
			startIndex = pfsParameter.getStartIndex() == -1 ? 0 : pfsParameter
					.getStartIndex();
			toIndex = pfsParameter.getMaxResults() == -1 ? assignedConflicts
					.size() : Math.min(assignedConflicts.size(), startIndex
					+ pfsParameter.getMaxResults());
		}

		// construct the search results
		results.setTotalCount(assignedConflicts.size());

		for (WorkflowTrackingRecord trackingRecord : assignedConflicts.subList(
				startIndex, toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(trackingRecord.getTerminologyId());
			result.setValue(trackingRecord.getDefaultPreferredName());

			// get the record id
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
				}
			}

			results.addSearchResult(result);
		}

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableConsensusWork
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public SearchResultList findAvailableConsensusWork(MapProject mapProject,
			PfsParameter pfsParameter) {

		List<WorkflowTrackingRecord> availableConsensus = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// find all the consensus workflow records
		for (WorkflowTrackingRecord tr : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			if (tr.getWorkflowStatus().equals(WorkflowStatus.CONSENSUS_NEEDED)) {
				availableConsensus.add(tr);
			}
		}

		// sort the tracking records
		Collections.sort(availableConsensus,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord tr1,
							WorkflowTrackingRecord tr2) {
						return tr1.getSortKey().compareTo(tr2.getSortKey());
					}
				});

		// set the default start index and the max results
		int startIndex = 0;
		int toIndex = availableConsensus.size();

		// if a pfsParameter has been supplied, get the start index and max
		// results
		if (pfsParameter != null) {
			startIndex = pfsParameter.getStartIndex() == -1 ? 0 : pfsParameter
					.getStartIndex();
			toIndex = Math.min(availableConsensus.size(), startIndex
					+ pfsParameter.getMaxResults());
		}

		// construct search result list
		results.setTotalCount(availableConsensus.size());
		for (WorkflowTrackingRecord tr : availableConsensus.subList(startIndex,
				toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(tr.getTerminologyId());
			result.setValue(tr.getDefaultPreferredName());
			result.setId(tr.getId());
			results.addSearchResult(result);
		}

		return results;
	}

	/**
	 * Perform workflow actions based on a specified action.
	 * ASSIGN_FROM_INITIAL_RECORD is the only routine that requires a map record
	 * to be passed in All other cases that all required mapping information
	 * (e.g. map records) be current in the database (i.e. updateMapRecord has
	 * been called)
	 */
	@SuppressWarnings("unused")
	@Override
	public void processWorkflowAction(MapProject mapProject, Concept concept,
			MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction)
			throws Exception {

		// set the transaction per operation

		// instantiate the algorithm handler for this project\
		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(
						"org.ihtsdo.otf.mapping.jpa.handlers."
								+ mapProject
										.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
		algorithmHandler.setMapProject(mapProject);

		// locate any existing workflow tracking records for this project and
		// concept
		WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(
				mapProject, concept);

		// force a read of map records and users and detach the record for
		// passing into project algorithm handler
		// Rationale: the algorithm handler is not expected to perform any
		// actions requiring services
		Logger.getLogger(WorkflowServiceJpa.class).info("Detaching...");
		for (MapUser mu : trackingRecord.getAssignedUsers())
			mu.getEmail(); // force retrieval of users by requesting email
		trackingRecord.getWorkflowStatus(); // force retrieval of records by
											// calculating workflow status
		manager.detach(trackingRecord);

		// switch on workflow action
		switch (workflowAction) {
		case ASSIGN_FROM_INITIAL_RECORD:

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"ASSIGN_FROM_INITIAL_RECORD");

			// expect no tracking record, double-check
			if (trackingRecord != null) {
				throw new Exception(
						"ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Found tracking record for published record where none was expected!");
			}

			// expect a map record to be passed in
			if (mapRecord == null) {
				throw new Exception(
						"ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Call to assign from intial record must include an existing map record");
			}

			trackingRecord = new WorkflowTrackingRecordJpa();
			trackingRecord.setMapProject(mapProject);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyVersion(concept
					.getTerminologyVersion());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setDefaultPreferredName(concept
					.getDefaultPreferredName());

			// perform the assign action via the algorithm handler
			trackingRecord = algorithmHandler.assignFromInitialRecord(
					trackingRecord, mapRecord, mapUser);

			break;

		case ASSIGN_FROM_SCRATCH:

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"ASSIGN_FROM_SCRATCH");

			// expect existing (pre-computed) workflow tracking record
			if (trackingRecord == null) {
				throw new Exception(
						"Could not find tracking record for assignment.");
			}

			// perform the assignment via the algorithm handler
			trackingRecord = algorithmHandler.assignFromScratch(trackingRecord,
					concept, mapUser);

			break;

		case UNASSIGN:

			Logger.getLogger(WorkflowServiceJpa.class).info("UNASSIGN");

			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: UNASSIGN - Could not find tracking record for unassignment.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!trackingRecord.getAssignedUsers().contains(mapUser))
				throw new Exception(
						"ProcessWorkflowAction: UNASSIGN - User not assigned to record for unassignment request");

			// perform the unassign action via the algorithm handler
			trackingRecord = algorithmHandler.unassign(trackingRecord, mapUser);

			break;

		case SAVE_FOR_LATER:

			Logger.getLogger(WorkflowServiceJpa.class).info("SAVE_FOR_LATER");

			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: SAVE_FOR_LATER - Could not find tracking record.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!trackingRecord.getAssignedUsers().contains(mapUser))
				throw new Exception(
						"SAVE_FOR_LATER - User not assigned to record");

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"Performing action...");

			trackingRecord = algorithmHandler.saveForLater(trackingRecord,
					mapUser);

			break;

		case FINISH_EDITING:

			Logger.getLogger(WorkflowServiceJpa.class).info("FINISH_EDITING");

			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: FINISH_EDITING - Could not find tracking record to be finished.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!trackingRecord.getAssignedUsers().contains(mapUser))
				throw new Exception(
						"User not assigned to record for finishing request");

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"Performing action...");

			// perform the action
			trackingRecord = algorithmHandler.finishEditing(trackingRecord,
					mapUser);

			break;

		default:
			throw new Exception("Unknown action requested.");
		}
		
		Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");
		synchronizeWorkflowTrackingRecord(trackingRecord,
				getWorkflowTrackingRecord(mapProject, concept));

	}

	/**
	 * Given a modified workflow tracking record and a as-in-database workflow
	 * tracking record, merges any changes (add/update/delete records) from the
	 * newer tracking record
	 * 
	 * This method created to ensure that all map record changes are complete
	 * before workflow tracking record is modified.
	 * 
	 * @param newTrackingRecord
	 *            the new tracking record (MUST be detached)
	 * @param oldTrackingRecord
	 *            the old tracking record
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public void synchronizeWorkflowTrackingRecord(
			WorkflowTrackingRecord newTrackingRecord,
			WorkflowTrackingRecord oldTrackingRecord) throws Exception {

		MappingService mappingService = new MappingServiceJpa();

		// force lazy collection instantiation of map records, then detach/evict
		// tracking record and all its map records
		oldTrackingRecord.getWorkflowStatus(); // force lazy collection
		manager.detach(oldTrackingRecord);

		// detach/evict tracking record and all its map records
		newTrackingRecord.getWorkflowStatus(); // force lazy collection
		manager.detach(newTrackingRecord);

		Logger.getLogger(WorkflowServiceJpa.class).info(
				newTrackingRecord.getMapRecords().size()
						+ " records in tracking record");

		manager.close();

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"SYNC: checking for changes to workflow tracking record "
						+ newTrackingRecord.getId().toString() + "...");

		// check for record deletions
		List<MapRecord> oldRecords = new ArrayList<>(
				oldTrackingRecord.getMapRecords());
		for (int i = 0; i < oldRecords.size(); i++) {

			MapRecord mapRecord = oldRecords.get(i);

			// if this map record is not in the new tracking record, it should
			// be deleted
			if (getMapRecordInWorkflowTrackingRecord(newTrackingRecord,
					mapRecord.getId()) == null) {
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"       Deleting record " + mapRecord.getId());

				// remove from the as-in-database record
				oldTrackingRecord.removeMapRecord(mapRecord);

				// update in database to allow removal of map record (child
				// field)
				manager = factory.createEntityManager();
				updateWorkflowTrackingRecord(oldTrackingRecord);
				manager.close();

				// remove the map record
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"       Updated tracking record successfully");
				mappingService.removeMapRecord(mapRecord.getId());
			}
		}

		// update and/or add the new records
		List<MapRecord> newRecords = new ArrayList<>(
				newTrackingRecord.getMapRecords());
		for (int i = 0; i < newRecords.size(); i++) {

			MapRecord mapRecord = newRecords.get(i);

			// if record is not present on tracking record, add it via mapping
			// service
			if (getMapRecordInWorkflowTrackingRecord(oldTrackingRecord,
					mapRecord.getId()) == null) {

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"       Found record to add");

				mappingService.addMapRecord(mapRecord);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"       --> Added record "
								+ mapRecord.getId().toString());

				// otherwise, if map record has changed, update it
			} else if (getMapRecordInWorkflowTrackingRecord(oldTrackingRecord,
					mapRecord.getId()).equals(mapRecord)) {

				Logger.getLogger(WorkflowServiceJpa.class).info(
						mapRecord.toString());
				Logger.getLogger(WorkflowServiceJpa.class).info(
						mapRecord.getMapEntries().size());
				Logger.getLogger(WorkflowServiceJpa.class).info(
						getMapRecordInWorkflowTrackingRecord(oldTrackingRecord,
								mapRecord.getId()).toString());
				Logger.getLogger(WorkflowServiceJpa.class).info(
						getMapRecordInWorkflowTrackingRecord(oldTrackingRecord,
								mapRecord.getId()).getMapEntries().size());

				// if
				// (!mapRecord.equals(getMapRecordInWorkflowTrackingRecord(oldTrackingRecord,
				// mapRecord.getId()))) {
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"       Updating record " + mapRecord.getId());
				mappingService.updateMapRecord(mapRecord);
				// }
			}
		}

		mappingService.close();

		manager = factory.createEntityManager();

		// check for workflow removal (i.e. one record with
		// READY_FOR_PUBLICATION
		if (newTrackingRecord.getWorkflowStatus().equals(
				WorkflowStatus.READY_FOR_PUBLICATION)
				&& newTrackingRecord.getMapRecords().size() == 1) {
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"SYNC: Deleting workflow tracking record");
			removeWorkflowTrackingRecord(newTrackingRecord.getId());
		} else {
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"SYNC: Updating workflow tracking record");
			updateWorkflowTrackingRecord(newTrackingRecord);
		}
	}

	/**
	 * Helper function for synchronizeWorkflowTrackingRecord. Gets a map record
	 * from a workflow tracking record.
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapRecordId
	 *            the map record id
	 * @return the map record in workflow tracking record
	 */
	public MapRecord getMapRecordInWorkflowTrackingRecord(
			WorkflowTrackingRecord trackingRecord, Long mapRecordId) {
		for (MapRecord mr : trackingRecord.getMapRecords()) {
			if (mr.getId() == mapRecordId)
				return mr;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#computeWorkflow(org.ihtsdo
	 * .otf.mapping.model.MapProject)
	 */
	@Override
	public void computeWorkflow(MapProject mapProject) throws Exception {
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Start computing workflow for " + mapProject.getName());

		// Clear the workflow for this project
		Logger.getLogger(WorkflowServiceJpa.class).info("  Clear old workflow");
		clearWorkflowForMapProject(mapProject);

		// open the services
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();

		// find the unmapped concepts in scope
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Find unmapped concepts in scope");
		SearchResultList unmappedConceptsInScope = mappingService
				.findUnmappedConceptsInScope(mapProject.getId());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"    Found = " + unmappedConceptsInScope.getTotalCount());

		for (SearchResult sr : unmappedConceptsInScope.getSearchResults()) {
			Logger.getLogger(WorkflowServiceJpa.class).debug(
					"  Create tracking record for " + sr.getTerminologyId());

			// retrieve the concept for this result
			Concept concept = contentService.getConcept(sr.getTerminologyId(),
					sr.getTerminology(), sr.getTerminologyVersion());

			// create a workflow tracking record for this concept
			WorkflowTrackingRecord trackingRecord = new WorkflowTrackingRecordJpa();

			// populate the fields from project and concept
			trackingRecord.setMapProject(mapProject);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept
					.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept
					.getDefaultPreferredName());
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH); // Make
																			// this
																			// a
																			// project
																			// specific
																			// call
																			// later
																			// (phase
																			// 2)

			// get the tree positions for this concept and set the sort key to
			// the first retrieved
			SearchResultList treePositionsList = contentService
					.findTreePositionsForConcept(concept.getTerminologyId(),
							concept.getTerminology(),
							concept.getTerminologyVersion());
			// handle inactive concepts - which don't have tree positions
			if (treePositionsList.getSearchResults().size() == 0) {
				trackingRecord.setSortKey("");
			} else {
				trackingRecord.setSortKey(treePositionsList.getSearchResults()
						.get(0).getValue());
			}

			// persist the workflow tracking record
			addWorkflowTrackingRecord(trackingRecord);

			// retrieve map records for this project and concept
			List<MapRecord> mapRecords = mappingService
					.getMapRecordsForConcept(concept.getTerminologyId())
					.getMapRecords();

			// cycle over records retrieved
			Logger.getLogger(WorkflowServiceJpa.class).debug(
					"    Find existing map records");
			for (MapRecord mapRecord : mapRecords) {

				// if this record belongs to project, add it to tracking record
				if (mapRecord.getMapProjectId().equals(mapProject.getId())) {
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"      found - " + mapRecord.getId() + " "
									+ mapRecord.getOwner());
					trackingRecord.addMapRecord(mapRecord);
				}
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Done computing workflow");

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			tx.commit();
		}

		mappingService.close();
		contentService.close();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#clearWorkflowForMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void clearWorkflowForMapProject(MapProject mapProject)
			throws Exception {

		for (WorkflowTrackingRecord tr : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			removeWorkflowTrackingRecord(tr.getId());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#close()
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getTransactionPerOperation
	 * ()
	 */
	@Override
	public boolean getTransactionPerOperation() throws Exception {
		return transactionPerOperation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#setTransactionPerOperation
	 * (boolean)
	 */
	@Override
	public void setTransactionPerOperation(boolean transactionPerOperation)
			throws Exception {
		this.transactionPerOperation = transactionPerOperation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#beginTransaction()
	 */
	@Override
	public void beginTransaction() throws Exception {

		if (getTransactionPerOperation())
			throw new IllegalStateException(
					"Error attempting to begin a transaction when using transactions per operation mode.");
		else if (tx != null && tx.isActive())
			throw new IllegalStateException(
					"Error attempting to begin a transaction when there "
							+ "is already an active transaction");
		tx = manager.getTransaction();
		tx.begin();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#commit()
	 */
	@Override
	public void commit() throws Exception {

		if (getTransactionPerOperation())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when using transactions per operation mode.");
		else if (tx != null && !tx.isActive())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when there "
							+ "is no active transaction");
		tx.commit();
	}

}
