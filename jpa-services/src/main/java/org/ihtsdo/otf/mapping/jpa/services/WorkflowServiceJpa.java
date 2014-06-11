package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
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
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

// TODO: Auto-generated Javadoc
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
	 * @throws Exception 
	 */

	public WorkflowServiceJpa() throws Exception {
	  super();

		// created on each instantiation
		manager = factory.createEntityManager();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#addTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public TrackingRecord addTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#removeTrackingRecord
	 * (java.lang.Long)
	 */
	@Override
	public void removeTrackingRecord(Long trackingRecordId)
			throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			TrackingRecord ma = manager.find(
					TrackingRecordJpa.class, trackingRecordId);

			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			TrackingRecord ma = manager.find(
					TrackingRecordJpa.class, trackingRecordId);
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
	 * org.ihtsdo.otf.mapping.services.WorkflowService#updateTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public void updateTrackingRecord(TrackingRecord record)
			throws Exception {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(record);
			tx.commit();
		} else {
			manager.merge(record);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getTrackingRecords
	 * ()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecords() throws Exception {

		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
		
		trackingRecordList.setTrackingRecords(manager.createQuery(
				"select tr from TrackingRecordJpa tr").getResultList());
		
		return trackingRecordList;
	}
	
	@Override
	public TrackingRecord getTrackingRecordForMapProjectAndConcept(MapProject mapProject, Concept concept) {
		return (TrackingRecord) manager.createQuery(
				"select tr from TrackingRecordJpa tr where mapProject_id = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminology", concept.getTerminology())
				.setParameter("terminologyVersion", concept.getTerminologyVersion())
				.setParameter("terminologyId", concept.getTerminologyId())
				.getSingleResult();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getTrackingRecordsForMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecordsForMapProject(
			MapProject mapProject) throws Exception {
		
		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
		trackingRecordList.setTrackingRecords(manager
				.createQuery(
						"select tr from TrackingRecordJpa tr where mapProject_id = :mapProjectId")
				.setParameter("mapProjectId", mapProject.getId())
				.getResultList());
		
		return trackingRecordList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getTrackingRecord
	 * (org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.rf2.Concept)
	 */
	@Override
	public TrackingRecord getTrackingRecord(
			MapProject mapProject, Concept concept) throws Exception {

		javax.persistence.Query query = manager
				.createQuery(
						"select tr from TrackingRecordJpa tr where mapProject_id = :mapProjectId and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminologyId", concept.getTerminologyId());

		try {
			return (TrackingRecord) query.getSingleResult();

		} catch (NoResultException e) {
			Logger.getLogger(this.getClass())
					.debug("WorkflowService.getTrackingRecord(): Concept query for terminologyId = "
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<TrackingRecord> availableWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		for (TrackingRecord trackingRecord : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			// if this tracking record does not have this user assigned to it
			// AND the total users assigned is less than 2
			// AND the workflow status is less than or equal to EDITING_DONE
			// This will eventually be a project specific check (i.e. for legacy
			// handling)

			Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

			if (!getMapUsersFromMapRecords(mapRecords).contains(mapUser)
					&& getMapUsersFromMapRecords(mapRecords).getCount() < 2
					&& getWorkflowStatusFromMapRecords(mapRecords).compareTo(
							WorkflowStatus.EDITING_DONE) <= 0) {

				availableWork.add(trackingRecord);
			}
		}

		// sort the tracking records
		Collections.sort(availableWork,
				new Comparator<TrackingRecord>() {
					@Override
					public int compare(TrackingRecord tr1,
							TrackingRecord tr2) {
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

		for (TrackingRecord tr : availableWork.subList(startIndex,
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<TrackingRecord> availableConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records for this project
		for (TrackingRecord trackingRecord : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			// if this record is marked conflict detected
			if (getWorkflowStatusForTrackingRecord(trackingRecord)
					.equals(WorkflowStatus.CONFLICT_DETECTED)) {

				// add the search result
				availableConflicts.add(trackingRecord);
			}
		}

		// sort the tracking records
		Collections.sort(availableConflicts,
				new Comparator<TrackingRecord>() {
					@Override
					public int compare(TrackingRecord tr1,
							TrackingRecord tr2) {
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

		for (TrackingRecord tr : availableConflicts.subList(startIndex,
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<TrackingRecord> assignedWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (TrackingRecord trackingRecord : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			// cycle over map records to find the NEW or EDITING_IN_PROGRESS
			// record assigned to this user
			for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)
						&& mapRecord.getWorkflowStatus().compareTo(
								WorkflowStatus.EDITING_DONE) < 0) {
					assignedWork.add(trackingRecord);
				}
			}
		}

		// sort the tracking records
		Collections.sort(assignedWork,
				new Comparator<TrackingRecord>() {
					@Override
					public int compare(TrackingRecord tr1,
							TrackingRecord tr2) {
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

		for (TrackingRecord trackingRecord : assignedWork.subList(
				startIndex, toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(trackingRecord.getTerminologyId());
			result.setValue(trackingRecord.getDefaultPreferredName());

			// get the record id and the workflow status
			for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
					result.setTerminologyVersion(mapRecord.getWorkflowStatus()
							.toString());
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<TrackingRecord> assignedConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (TrackingRecord trackingRecord : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			// cycle over map records to find the CONFLICT_DETECTED record
			// assigned to this user
			for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)
						&& (mapRecord.getWorkflowStatus().compareTo(
								WorkflowStatus.CONFLICT_NEW) == 0 || mapRecord
								.getWorkflowStatus().compareTo(
										WorkflowStatus.CONFLICT_IN_PROGRESS) == 0)) {
					assignedConflicts.add(trackingRecord);
				}
			}
		}

		// sort the tracking records
		Collections.sort(assignedConflicts,
				new Comparator<TrackingRecord>() {
					@Override
					public int compare(TrackingRecord tr1,
							TrackingRecord tr2) {
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

		for (TrackingRecord trackingRecord : assignedConflicts.subList(
				startIndex, toIndex)) {
			SearchResult result = new SearchResultJpa();
			result.setTerminologyId(trackingRecord.getTerminologyId());
			result.setValue(trackingRecord.getDefaultPreferredName());

			// get the record id
			for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
					result.setTerminologyVersion(mapRecord.getWorkflowStatus()
							.toString());
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
			PfsParameter pfsParameter) throws Exception {

		List<TrackingRecord> availableConsensus = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// find all the consensus workflow records
		for (TrackingRecord tr : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {

			if (getWorkflowStatusForTrackingRecord(tr).equals(
					WorkflowStatus.CONSENSUS_NEEDED)) {
				availableConsensus.add(tr);
			}
		}

		// sort the tracking records
		Collections.sort(availableConsensus,
				new Comparator<TrackingRecord>() {
					@Override
					public int compare(TrackingRecord tr1,
							TrackingRecord tr2) {
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
		for (TrackingRecord tr : availableConsensus.subList(startIndex,
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
	public void processWorkflowAction(MapProject mapProject, Concept concept,
			MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction)
			throws Exception {
		
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Processing workflow action by " + mapUser.getName() + ":  " + workflowAction.toString());
		if (mapRecord != null) {
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"  Record attached: " + mapRecord.toString());
		}
		
		setTransactionPerOperation(true);


		// instantiate the algorithm handler for this project\
		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(mapProject
										.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
		algorithmHandler.setMapProject(mapProject);

		// locate any existing workflow tracking records for this project and
		// concept
		TrackingRecord trackingRecord = getTrackingRecord(
				mapProject, concept);
		
		Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);
		
		// if the record passed in updates an existing record, replace it in the set
		if (mapRecord != null && mapRecord.getId() != null) {
			for (MapRecord mr : mapRecords) {
				if (mr.getId().equals(mapRecord.getId())) {
					mapRecords.remove(mr);
					mapRecords.add(mapRecord);
					break;
				}
			}
		}

		
		// switch on workflow action
		switch (workflowAction) {
		case ASSIGN_FROM_INITIAL_RECORD:

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"ASSIGN_FROM_INITIAL_RECORD");
			
			// if a tracking record is found, perform no action (this record is already assigned)
			if (trackingRecord == null) {
	
				// expect a map record to be passed in
				if (mapRecord == null) {
					throw new Exception(
							"ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Call to assign from intial record must include an existing map record");
				}
	
				// create a new tracking record for FIX_ERROR_PATH or QA_PATH
				trackingRecord = new TrackingRecordJpa();
				trackingRecord.setMapProject(mapProject);
				trackingRecord.setTerminology(concept.getTerminology());
				trackingRecord.setTerminologyVersion(concept
						.getTerminologyVersion());
				trackingRecord.setTerminologyId(concept.getTerminologyId());
				trackingRecord.setDefaultPreferredName(concept
						.getDefaultPreferredName());
				trackingRecord.addMapRecordId(mapRecord.getId());
				
				// get the tree positions for this concept and set the sort key to
				// the first retrieved
				ContentService contentService = new ContentServiceJpa();
				TreePositionList treePositionsList = contentService
						.getTreePositionsWithDescendants(concept.getTerminologyId(),
								concept.getTerminology(),
								concept.getTerminologyVersion());
				
				// handle inactive concepts - which don't have tree positions
				if (treePositionsList.getCount() == 0) {
					trackingRecord.setSortKey("");
				} else {
					trackingRecord.setSortKey(treePositionsList.getTreePositions()
							.get(0).getAncestorPath());
				}
				
				// only FIX_ERROR_PATH valid, QA_PATH in Phase 2
				trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
	
				// perform the assign action via the algorithm handler
				mapRecords = algorithmHandler.assignFromInitialRecord(
						trackingRecord, mapRecords, mapRecord, mapUser);
			} else {
				// do nothing
			}
	
			break;

		case ASSIGN_FROM_SCRATCH:

//			Logger.getLogger(WorkflowServiceJpa.class).info(
//					"ASSIGN_FROM_SCRATCH");

			// expect existing (pre-computed) workflow tracking record
			if (trackingRecord == null) {
				throw new Exception(
						"Could not find tracking record for assignment.");
			}

			// perform the assignment via the algorithm handler
			mapRecords = algorithmHandler.assignFromScratch(trackingRecord, mapRecords,
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
			if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"ProcessWorkflowAction: UNASSIGN - User not assigned to record for unassignment request");

			// perform the unassign action via the algorithm handler
			mapRecords = algorithmHandler.unassign(trackingRecord, mapRecords, mapUser);

			break;

		case SAVE_FOR_LATER:

//			Logger.getLogger(WorkflowServiceJpa.class).info("SAVE_FOR_LATER");

			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: SAVE_FOR_LATER - Could not find tracking record.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"SAVE_FOR_LATER - User not assigned to record");

//			Logger.getLogger(WorkflowServiceJpa.class).info(
//					"Performing action...");

			mapRecords = algorithmHandler.saveForLater(trackingRecord, mapRecords,
					mapUser);

			break;

		case FINISH_EDITING:

//			Logger.getLogger(WorkflowServiceJpa.class).info("FINISH_EDITING");

			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: FINISH_EDITING - Could not find tracking record to be finished.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"User not assigned to record for finishing request");
//
//			Logger.getLogger(WorkflowServiceJpa.class).info(
//					"Performing action...");
//
//			// perform the action
			mapRecords = algorithmHandler.finishEditing(trackingRecord, mapRecords,
					mapUser);

			break;
			
		case CANCEL:
			
			// expect existing (pre-computed) workflow tracking record to exist
			// with this user assigned
			if (trackingRecord == null)
				throw new Exception(
						"ProcessWorkflowAction: CANCEL - Could not find tracking record to be finished.");

			// expect this user to be assigned to a map record in this tracking
			// record
			if (!getMapUsersForTrackingRecord(trackingRecord).contains(
					mapUser))
				throw new Exception(
						"User not assigned to record for cancel request");
			
			mapRecords = algorithmHandler.cancelWork(trackingRecord, mapRecords, mapUser);

			break;
		default:
			throw new Exception("Unknown action requested.");
		}
			
		Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");
		
		Set<MapRecord> syncedRecords = synchronizeMapRecords(trackingRecord, mapRecords);
		trackingRecord.setMapRecordIds(null);
		for (MapRecord mr : syncedRecords) {
			trackingRecord.addMapRecordId(mr.getId());
		}
		
		// if the tracking record is ready for removal, delete it
		if (getWorkflowStatusForTrackingRecord(trackingRecord).equals(
				WorkflowStatus.READY_FOR_PUBLICATION)
				&& trackingRecord.getMapRecordIds().size() == 1) {

			removeTrackingRecord(trackingRecord.getId());
		
		// else add the tracking record if new
		} else if (trackingRecord.getId() == null) {
			addTrackingRecord(trackingRecord);
			
		// otherwise update the tracking record
		} else	{

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
	 * records passed in as argument - newRecords: The set of records passed in
	 * as argument after persistence detaching - oldRecords: The set of records
	 * retrieved by id from the database for comparison - syncedRecords: The
	 * synchronized set of records for return from this routine
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
	public Set<MapRecord> synchronizeMapRecords(
			TrackingRecord trackingRecord, Set<MapRecord> mapRecords)
			throws Exception {

		// detach the currently persisted map records from the workflow service
		// necessary to avoid conflict with mapping service,
		// as well as forced overwrite on retrieval of previous state
		Set<MapRecord> newRecords = new HashSet<>();
		Set<MapRecord> oldRecords = new HashSet<>();
		Set<MapRecord> syncedRecords = new HashSet<>();

		// detach the map records
		for (MapRecord mr : mapRecords) {
			manager.detach(mr);
			newRecords.add(mr);
		}

		// Instantiate the mapping service
		MappingService mappingService = new MappingServiceJpa();

		// retrieve the old (existing) records
		if (trackingRecord.getMapRecordIds() != null) {
			for (Long id : trackingRecord.getMapRecordIds()) {
				oldRecords.add(mappingService.getMapRecord(id));
			}
		}

		// cycle over new records to check for additions or updates
		for (MapRecord mr : newRecords) {
			if (getMapRecordInSet(oldRecords, mr.getId()) == null) {
	
				// deep copy the detached record into a new
				// persistence-environment record
				// this routine also duplicates child collections to avoid
				// detached object errors
				MapRecord newRecord = new MapRecordJpa(mr, true);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"Adding record: " + newRecord.toString());

				// add the record to the database
				mappingService.addMapRecord(mr);

				// add the record to the return list
				syncedRecords.add(newRecord);
			}

			// otherwise, check for update
			else {
				// if the old map record is changed, update it
				if (!mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
					mappingService.updateMapRecord(mr);
				}

				syncedRecords.add(mr);
			}
		}

		// cycle over old records to check for deletions
		for (MapRecord mr : oldRecords) {

			// if old record is not in the new record set, delete it
			if (getMapRecordInSet(syncedRecords, mr.getId()) == null) {
				mappingService.removeMapRecord(mr.getId());
			}
		}

		// close the service
		mappingService.close();

		return newRecords;

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

	private MapRecord getMapRecordInSet(Set<MapRecord> mapRecords, Long mapRecordId) {
		if (mapRecordId == null) return null;

		for (MapRecord mr : mapRecords) {
			if (mapRecordId.equals(mr.getId()))
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

		setTransactionPerOperation(false);
		int commitCt = 1000;
		int trackingRecordCt = 0;

		beginTransaction();

		for (SearchResult sr : unmappedConceptsInScope.getSearchResults()) {
			Logger.getLogger(WorkflowServiceJpa.class).debug(
					"  Create tracking record for " + sr.getTerminologyId());

			// retrieve the concept for this result
			Concept concept = contentService.getConcept(sr.getTerminologyId(),
					sr.getTerminology(), sr.getTerminologyVersion());

			// create a workflow tracking record for this concept
			TrackingRecord trackingRecord = new TrackingRecordJpa();

			// populate the fields from project and concept
			trackingRecord.setMapProject(mapProject);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept
					.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept
					.getDefaultPreferredName());
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

			// get the tree positions for this concept and set the sort key to
			// the first retrieved
			TreePositionList treePositionsList = contentService
					.getTreePositionsWithDescendants(concept.getTerminologyId(),
							concept.getTerminology(),
							concept.getTerminologyVersion());
			// handle inactive concepts - which don't have tree positions
			if (treePositionsList.getCount() == 0) {
				trackingRecord.setSortKey("");
			} else {
				trackingRecord.setSortKey(treePositionsList.getTreePositions()
						.get(0).getAncestorPath());
			}

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
					trackingRecord.addMapRecordId(mapRecord.getId());
				}
			}

			// persist the workflow tracking record
			addTrackingRecord(trackingRecord);

			if (++trackingRecordCt % commitCt == 0) {
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"  " + trackingRecordCt + " tracking records created");
				commit();
				beginTransaction();
			}
		}

		commit();

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

		for (TrackingRecord tr : getTrackingRecordsForMapProject(mapProject).getTrackingRecords()) {
			removeTrackingRecord(tr.getId());
		}

	}

	/**
	 * Generates up to a desired number of conflicts for a map project.
	 * 
	 * Clears any map records with status != PUBLISHED.
	 * 
	 * @param mapProject
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void generateRandomConflictData(MapProject mapProject,
			int numDesiredConflicts) throws Exception {

		// instantiate the random number generator
		Random rand = new Random();

		// instantiate the services and algorithm handler
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
				.getProjectSpecificAlgorithmHandler(mapProject);

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Reverting project records to published state");

		// get records in non-published state
		List<MapRecord> mapRecords = manager
				.createQuery(
						"select mr from MapRecordJpa mr where workflowStatus != 'PUBLISHED')")
				.getResultList();

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Removing " + mapRecords.size() + " records.");

		// remove the records
		for (MapRecord mapRecord : mapRecords) {
			mappingService.removeMapRecord(mapRecord.getId());
		}

		// recompute the workflow
		computeWorkflow(mapProject);

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Generating random conflicts -- number desired is "
						+ numDesiredConflicts);

		// the tracking records associated with this project
		List<TrackingRecord> trackingRecords = getTrackingRecordsForMapProject(mapProject).getTrackingRecords();

		// the list of tracking records available to specialist assignment
		List<TrackingRecord> specialistTrackingRecords = new ArrayList<>();
		
		// the list of CONFLICT_DETECTED tracking records
		List<TrackingRecord> leadTrackingRecords = new ArrayList<>();

		// the list of CONFLICT_NEW tracking records
		List<TrackingRecord> conflictTrackingRecords = new ArrayList<>();

		// the list of specialists and leads on this project (for convenience)
		List<MapUser> mapSpecialists = new ArrayList<>();
		List<MapUser> mapLeads = new ArrayList<>();

		// select only the 'real' (human) users
		Logger.getLogger(WorkflowServiceJpa.class).info(" Specialists found:");
		for (MapUser mapSpecialist : mapProject.getMapSpecialists()) {
			if (!mapSpecialist.getName().matches(
					"Loader Record|Legacy Record|Default|string")) {
				mapSpecialists.add(mapSpecialist);
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"  " + mapSpecialist.getName());
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(" Leads found:");
		for (MapUser mapLead : mapProject.getMapLeads()) {
			mapLeads.add(mapLead);
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"  " + mapLead.getName());
		}

		// throw exceptions if the user set is not sufficient
		if (mapSpecialists.size() < 2) {
			throw new Exception(
					"Cannot generate random conflicts with less than two specialists attached to the project");
		}
		if (mapLeads.size() == 0) {
			throw new Exception(
					"Cannot generate random conflicts without a lead attached to the project");
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Computing available work " + trackingRecords.size()
						+ " workflow tracking records");

		// sort the currently existing workflow state into the various sets
		for (TrackingRecord trackingRecord : trackingRecords) {

			// if no records attached to this tracking record, it is "clean"
			if (trackingRecord.getMapRecordIds().size() == 0) {

				// add clean record to available for specialist list
				specialistTrackingRecords.add(trackingRecord);

				// otherwise, in-progress workflow already exists
			} else {
				switch (getWorkflowStatusForTrackingRecord(trackingRecord)) {

				// conflict available to lead
				case CONFLICT_DETECTED:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"   Available conflict: "
									+ trackingRecord.getTerminologyId());
					leadTrackingRecords.add(trackingRecord);
					break;

				// assigned conflict is added to final set
				case CONFLICT_IN_PROGRESS:
				case CONFLICT_NEW:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"   Assigned conflict: "
									+ trackingRecord.getTerminologyId());

					conflictTrackingRecords.add(trackingRecord);
					break;

				// Consensus Path ignored
				case CONSENSUS_NEEDED:
				case CONSENSUS_RESOLVED:
					// do nothing
					break;

				// specialist editing in progress
				case EDITING_DONE:
				case EDITING_IN_PROGRESS:
				case NEW:

					// if only one record present, available to another
					// specialist
					if (trackingRecord.getMapRecordIds().size() == 1) {
						specialistTrackingRecords.add(trackingRecord);
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"   Available Concept: "
										+ trackingRecord.getTerminologyId());
					}

					break;

				// ignore published and review status
				case PUBLISHED:
				case READY_FOR_PUBLICATION:
				case REVIEW:
					break;
				default:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"     ERROR:  invalid workflow status");
					break;
				}
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("     Concepts available:  "
						+ specialistTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Conflicts available: " + leadTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Existing conflicts:  " + conflictTrackingRecords.size());

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Generating list of valid targets.");

		int startConceptIndex = 0;
		List<Concept> targetCodes = new ArrayList<>();
		while (targetCodes.size() < 10000) {

			for (Concept concept : (List<Concept>) manager
					.createQuery(
							"select c from ConceptJpa c where terminology = :terminology")
					.setParameter("terminology",
							mapProject.getDestinationTerminology())
					.setFirstResult(startConceptIndex).setMaxResults(1000)
					.getResultList()) {

				if (algorithmHandler.isTargetCodeValid(concept
						.getTerminologyId())) {
					targetCodes.add(concept);
				}
			}
		}

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Target set has " + targetCodes.size() + " concepts.");

		int nRecordsAssignedToSpecialist = 0;
		int nRecordsSavedForLater = 0;

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Begin assigning random work...");

		// perform assignment loop until :
		// - the number of desired conflicts is reached OR
		// - the records available to specialists is exhausted
		while (leadTrackingRecords.size() < numDesiredConflicts
				&& specialistTrackingRecords.size() > 0) {

			// if CONFLICT_DETECTED records are available
			if (leadTrackingRecords.size() > 0) {

				// get the first available CONFLICT_DETECTED tracking record
				TrackingRecord trackingRecord = leadTrackingRecords
						.get(0);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"   Procesing CONFLICT_DETECTED for "
								+ trackingRecord.getTerminologyId() + ", "
								+ trackingRecord.getDefaultPreferredName());

				// get the concept for this tracking record
				Concept concept = contentService.getConcept(
						trackingRecord.getTerminologyId(),
						trackingRecord.getTerminology(),
						trackingRecord.getTerminologyVersion());

				// get the random lead
				MapUser mapLead = getAssignableLead(leadTrackingRecords.get(0),
						mapLeads);

				// if no user available, move to the next tracking record
				if (mapLead == null) {
					leadTrackingRecords.remove(trackingRecord);
					continue;
				}

				// assign the conflict
				processWorkflowAction(mapProject, concept, mapLead, null,
						WorkflowAction.ASSIGN_FROM_SCRATCH);

				// add this workflow tracking record to the conflict assigned
				// list
				conflictTrackingRecords.add(trackingRecord);

				// remove this workflow tracking record from the conflict
				// available list
				leadTrackingRecords.remove(trackingRecord);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"    Conflict assigned to " + mapLead.getName());

				// otherwise, randomly assign a specialist to a record and
				// modify the record
			} else {

				// get a random tracking record available to a specialist
				// (range: [0:size-1])
				TrackingRecord trackingRecord = specialistTrackingRecords
						.get(rand.nextInt(specialistTrackingRecords.size()));

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"   Procesing available Concept for "
								+ trackingRecord.getTerminologyId() + ", "
								+ trackingRecord.getDefaultPreferredName());

				// get the concept for this record
				Concept concept = contentService.getConcept(
						trackingRecord.getTerminologyId(),
						trackingRecord.getTerminology(),
						trackingRecord.getTerminologyVersion());

				// get the available specialist for this tracking record
				MapUser mapSpecialist = getAssignableSpecialist(trackingRecord,
						mapSpecialists);

				// if no user available, move to the next tracking record
				if (mapSpecialist == null) {
					specialistTrackingRecords.remove(trackingRecord);
					continue;
				}

				// assign the specialist to this concept
				processWorkflowAction(mapProject, concept, mapSpecialist, null,
						WorkflowAction.ASSIGN_FROM_SCRATCH);

				// get the record corresponding to this user
				MapRecord mapRecord = getMapRecordForTrackingRecordAndMapUser(
						trackingRecord, mapSpecialist);

				// make some random changes to the record
				randomizeMapRecord(mapProject, mapRecord, targetCodes);

				// determine whether to save for later or finish
				ValidationResult validationResult = algorithmHandler
						.validateRecord(mapRecord);
				if (validationResult.getErrors().size() > 0 // if any errors
															// reported
						|| rand.nextInt(5) == 0) { // randomly save some for
													// later anyway

					if (validationResult.getErrors().size() > 0) {
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"    Randomized record has errors: "
										+ mapSpecialist.getName());
						for (String error : validationResult.getErrors()) {
							Logger.getLogger(WorkflowServiceJpa.class).info(
									"      " + error);
						}
					}

					Logger.getLogger(WorkflowServiceJpa.class).info(
							"    Record saved for later by "
									+ mapSpecialist.getName());

					processWorkflowAction(mapProject, concept, mapSpecialist,
							mapRecord, WorkflowAction.SAVE_FOR_LATER);
					nRecordsSavedForLater++;

				} else {
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"    Finish editing by " + mapSpecialist.getName());
					processWorkflowAction(mapProject, concept, mapSpecialist,
							mapRecord, WorkflowAction.FINISH_EDITING);

				}

				// check if a conflict has arisen
				if (getWorkflowStatusForTrackingRecord(trackingRecord)
						.equals(WorkflowStatus.CONFLICT_DETECTED)) {

					// add the tracking record to the available for lead list
					leadTrackingRecords.add(trackingRecord);

					// remove the tracking record from th
					specialistTrackingRecords.remove(trackingRecord);

					Logger.getLogger(WorkflowServiceJpa.class).info(
							"    New conflict detected!");

					// otherwise, check that this record is not 'stuck' in
					// editing
					// - workflow status is less than CONFLICT_DETECTED
					// - AND two users are assigned
				} else if (getWorkflowStatusForTrackingRecord(
						trackingRecord).compareTo(
						WorkflowStatus.CONFLICT_DETECTED) < 1
						&& trackingRecord.getMapRecordIds().size() == 2) {

					Logger.getLogger(WorkflowServiceJpa.class)
							.info("    Tracking record has two users and is not in conflict, removing.");

					// remove the record from the available list
					specialistTrackingRecords.remove(trackingRecord);
				}

				// increment the counter
				nRecordsAssignedToSpecialist++;
			}
		}
		Logger.getLogger(WorkflowServiceJpa.class).info("Generation complete.");
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Concepts still available:        "
						+ specialistTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Conflicts still available:       "
						+ leadTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Records assigned to specialists: "
						+ nRecordsAssignedToSpecialist);
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Records 'saved for later':       " + nRecordsSavedForLater);
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Conflicts assigned to leads:     "
						+ conflictTrackingRecords.size());
	}
	
	/**
	 * Returns the assignable specialist.
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUsers the map users
	 * @return the assignable specialist
	 * @throws Exception the exception
	 */
	private MapUser getAssignableSpecialist(TrackingRecord trackingRecord, List<MapUser> mapUsers) throws Exception {
		
		// discard any users already assigned to this record
		for (MapUser mapUser : getMapUsersForTrackingRecord(trackingRecord).getMapUsers()) {
			mapUsers.remove(mapUser);
		}

		// if no assignable users, return null
		if (mapUsers.size() == 0)
			return null;

		// return a random user from the truncated list
		Random rand = new Random();
		return mapUsers.get(rand.nextInt(mapUsers.size()));

	}

	
	/**
	 * Returns the assignable lead.
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUsers the map users
	 * @return the assignable lead
	 * @throws Exception the exception
	 */
	private MapUser getAssignableLead(TrackingRecord trackingRecord, List<MapUser> mapUsers) throws Exception {
	
		// discard any users already assigned to this record
		for (MapUser mapUser : getMapUsersForTrackingRecord(trackingRecord).getMapUsers()) {
			mapUsers.remove(mapUser);
		}

		// if no assignable users, return null
		if (mapUsers.size() == 0)
			return null;

		// return a random user from the truncated list
		Random rand = new Random();
		return mapUsers.get(rand.nextInt(mapUsers.size()));
	}
	
	/**
	 * Randomize map record.
	 *
	 * @param mapProject the map project
	 * @param mapRecord the map record
	 * @param targetConcepts the target concepts
	 */
	private void randomizeMapRecord(MapProject mapProject, MapRecord mapRecord, List<Concept> targetConcepts) {
		
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Randomizing map record.");

		Random rand = new Random();

		// /////////////////////////
		// RULES
		// /////////////////////////
		List<String> precomputedRules = new ArrayList<>();

		// add the gender rules
		precomputedRules.add("IFA 248153007 | Male (finding) |");
		precomputedRules.add("IFA 248152002 | Female (finding) |");

		// age rule variables
		List<MapAgeRange> ageRanges;
		int nAgeRanges;

		// add a random number of Age - At Onset rules
		ageRanges = new ArrayList<>(mapProject.getPresetAgeRanges());

		// determine a random number of chronological age ranges to add
		nAgeRanges = rand.nextInt(ageRanges.size());

		for (int i = 0; i < nAgeRanges; i++) {
			// get a random age range from the dynamic list
			MapAgeRange ageRange = ageRanges
					.get(rand.nextInt(ageRanges.size()));

			// compute the rule string
			precomputedRules
					.add(computeAgeRuleString(
							"IFA 424144002 | Current chronological age (observable entity)",
							ageRange));

			// remove the age range from the list
			ageRanges.remove(ageRange);
		}

		// add a random number of Age - Chronological rules
		ageRanges = new ArrayList<>(mapProject.getPresetAgeRanges());

		// determine a random number of onset age ranges to add
		nAgeRanges = rand.nextInt(ageRanges.size());

		for (int i = 0; i < nAgeRanges; i++) {
			// get a random age range from the dynamic list
			MapAgeRange ageRange = ageRanges
					.get(rand.nextInt(ageRanges.size()));

			// compute the rule string
			precomputedRules
					.add(computeAgeRuleString(
							"IFA 445518008 | Age at onset of clinical finding (observable entity)",
							ageRange));

			// remove the age range from the list
			ageRanges.remove(ageRange);
		}

		// if no group structure, 1 group, else, between 1 and 2 groups
		int numGroups = mapProject.isGroupStructure() == true ? rand.nextInt(2) + 1
				: 1;

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"      Record will have " + numGroups + " groups");

		// //////////////////////////////
		// ENTRIES
		// //////////////////////////////
		for (int i = 1; i <= numGroups; i++) {

			// determine the number of entries
			int numEntries = rand.nextInt(3) + 1;

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"       Group " + i + " will have " + numEntries
							+ " entries");

			// generate entries
			for (int j = 1; j <= numEntries; j++) {

				// instantiate the map entry with group and priority
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setMapGroup(i);
				mapEntry.setMapPriority(j);

				// assign a target code
				Concept targetConcept = targetConcepts.get(rand
						.nextInt(targetConcepts.size()));
				mapEntry.setTargetId(targetConcept.getTerminologyId());
				mapEntry.setTargetName(targetConcept.getDefaultPreferredName());

				// if project is rule based
				if (mapProject.isRuleBased()) {

					List<String> availableRules = new ArrayList<>(
							precomputedRules);

					// if last entry in group, assign TRUE rule
					if (j == numEntries) {
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"         Setting rule: TRUE");
						mapEntry.setRule("TRUE");
					} else {
						mapEntry.setRule(availableRules.get(rand
								.nextInt(availableRules.size())));
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"         Setting rule: " + mapEntry.getRule());

					}
				}

				// determine whether to add a note (currently 50% chance)
				if (rand.nextInt(2) == 0) {

					// determine a random number of notes
					int nNote = rand.nextInt(2) + 1;
					for (int iNote = 0; iNote < nNote; iNote++) {

						MapNote mapNote = new MapNoteJpa();
						mapNote.setUser(mapRecord.getOwner());
						mapNote.setNote("I'm note #" + (iNote + 1) + " by "
								+ mapRecord.getOwner().getName());
						mapEntry.addMapNote(mapNote);
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"         Added note: " + mapNote.getNote());
					}
				}

				// add the map entry
				mapRecord.addMapEntry(mapEntry);

			}
		}	
	}
	
	/**
	 * Compute age rule string. Helper function for randomizeMapRecord.
	 *
	 * @param initString the init string
	 * @param ageRange the age range
	 * @return the string
	 */
	
	private String computeAgeRuleString(String initString, MapAgeRange ageRange) {

		String rule = "";

		if (ageRange.hasLowerBound() == true) {
			rule += initString + " | "
					+ (ageRange.getLowerInclusive() == true ? ">=" : ">")
					+ ageRange.getLowerValue() + " " + ageRange.getLowerUnits();
		}

		if (ageRange.hasLowerBound() == true
				&& ageRange.hasUpperBound() == true) {
			rule += " AND ";
		}

		if (ageRange.hasUpperBound() == true) {
			rule += initString + " | "
					+ (ageRange.getUpperInclusive() == true ? ">=" : ">")
					+ ageRange.getUpperValue() + " " + ageRange.getUpperUnits();
		}

		return rule;
	}

	// //////////////////////////
	// Utility functions
	// //////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getMapRecordsForTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public Set<MapRecord> getMapRecordsForTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {
		Set<MapRecord> mapRecords = new HashSet<>();
		MappingService mappingService = new MappingServiceJpa();
		if (trackingRecord != null && trackingRecord.getMapRecordIds() != null) {
			for (Long id : trackingRecord.getMapRecordIds()) {
				mapRecords.add(mappingService.getMapRecord(id));
			}
		}
		mappingService.close();
		return mapRecords;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getMapUsersForTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public MapUserList getMapUsersForTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {
		return getMapUsersFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getWorkflowStatusForTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public WorkflowStatus getWorkflowStatusForTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {
		return getWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getLowestWorkflowStatusForTrackingRecord
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
	 */
	@Override
	public WorkflowStatus getLowestWorkflowStatusForTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {
		return getLowestWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getMapUsersFromMapRecords
	 * (java.util.Set)
	 */
	@Override
	public MapUserList getMapUsersFromMapRecords(Set<MapRecord> mapRecords) {
		MapUserList mapUserList = new MapUserListJpa();
		for (MapRecord mr : mapRecords) {
			mapUserList.addMapUser(mr.getOwner());
		}
		return mapUserList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getWorkflowStatusFromMapRecords(java.util.Set)
	 */
	@Override
	public WorkflowStatus getWorkflowStatusFromMapRecords(
			Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.NEW;
		for (MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#
	 * getLowestWorkflowStatusFromMapRecords(java.util.Set)
	 */
	@Override
	public WorkflowStatus getLowestWorkflowStatusFromMapRecords(
			Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.REVIEW;
		for (MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/**
	 * Returns the map record for workflow tracking record and map user.
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUser the map user
	 * @return the map record for workflow tracking record and map user
	 * @throws Exception the exception
	 */
	private MapRecord getMapRecordForTrackingRecordAndMapUser(TrackingRecord trackingRecord, MapUser mapUser) throws Exception {
		for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
			if (mapRecord.getOwner().equals(mapUser))
				return mapRecord;
		}
		return null;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void generateMapperTestingState(MapProject mapProject) throws Exception {
		
		Logger.getLogger(WorkflowServiceJpa.class).info("Generating clean Mapping User Testing State for project " + mapProject.getName());
		
		String[] conceptsKLI = {
				/*"28221000119103",
				"700189007",
				"295131000119103",
				"72791000119108",
				"295041000119108",
				"295051000119105",
				"700147004",
				"700109009",
				"700112007",
				"700150001",
				"700081008",
				"700075000",
				"700097003",
				"700080009",
				"700082001",
				"402714001",
				"700094005",
				"166631000119101",
				"700076004",
				"403469006",
				"3961000119101",
				"700095006",
				"700153004",
				"700195008",
				"700107006",
				"700111000",
				"700077008",
				"700079006",
				"700167008",
				"700178000",
				"700181005",
				"700176001",
				"700170007",
				"700164001",
				"700173009",
				"440419004",
				"700078003",
				"700168003",
				"700179008",
				"700182003",
				"700177005",
				"700171006",
				"700165000",
				"700174003",
				"700149001",
				"700127007",
				"700132008"*/
				"233734006",
				"269209005",
				"239924002"
				};
		
		Logger.getLogger(WorkflowServiceJpa.class).info("  KLI: " + conceptsKLI.length + " concepts");
		
		String conceptsNIN[] = {
				/*"28221000119103",
				"700189007",
				"295131000119103",
				"72791000119108",
				"295041000119108",
				"295051000119105",
				"700147004",
				"700109009",
				"700112007",
				"700150001",
				"700081008",
				"700075000",
				"700097003",
				"700080009",
				"700082001",
				"402714001",
				"700094005",
				"166631000119101",
				"700076004",
				"403469006",
				"3961000119101",
				"700095006",
				"700153004",
				"700195008",
				"700107006",
				"700111000",
				"700077008",
				"700079006",
				"700167008",
				"700178000",
				"700181005",
				"700176001",
				"700170007",
				"700164001",
				"700173009",
				"440419004",
				"700078003",
				"700168003",
				"700179008",
				"700182003",
				"700177005",
				"700171006",
				"700165000",
				"700174003",
				"700149001",
				"700127007",
				"700132008"*/
				"233734006",
				"269209005",
				"239924002"
				};
		
		Logger.getLogger(WorkflowServiceJpa.class).info("  KLI: " + conceptsKLI.length + " concepts");
		
		// combine the string arrays into a unique-value hash set
		Set<MapRecord> existingRecords = new HashSet<>();
		Set<String> uniqueIds = new HashSet<>();
		for (String terminologyId : conceptsNIN) {
			uniqueIds.add(terminologyId);
		}
		for (String terminologyId : conceptsKLI) {
			uniqueIds.add(terminologyId);
		}
		
		// open the services
		MappingService mappingService = new MappingServiceJpa();
		ContentService contentService = new ContentServiceJpa(); 
		
		// get the map user objects
		MapUser mapUser_KLI = mappingService.getMapUser("kli");
		MapUser mapUser_NIN = mappingService.getMapUser("nin");
		
		// set the terminology and version -- shorthand
		String terminology = mapProject.getSourceTerminology();
		String terminologyVersion = mapProject.getSourceTerminologyVersion();
		
		// retrieve the concepts matching the unique ids and assemble them in a map of terminologyId -> concept
		Map<String, Concept> uniqueConcepts = new HashMap<>();
		for (String terminologyId : uniqueIds) {
			Concept concept = contentService.getConcept(terminologyId, terminology, terminologyVersion);
			if (concept != null) {
				uniqueConcepts.put(terminologyId, concept);
			} else {
				Logger.getLogger(WorkflowServiceJpa.class).warn("  Concept " + terminologyId + " not found in database.");
			}
		}

		contentService.close();
		
		Logger.getLogger(WorkflowServiceJpa.class).info("  Total: " + conceptsKLI.length + " unique concepts");

		// find any existing records for the concepts
		for (String terminologyId : uniqueIds) {
			javax.persistence.Query query = manager.createQuery("select mr from MapRecordJpa mr where conceptId = :conceptId and mapProjectId = :mapProjectId")
			.setParameter("conceptId", terminologyId)
			.setParameter("mapProjectId", mapProject.getId());
			
			
			List<MapRecord> mapRecords = (List<MapRecord>) query.getResultList();
			for (MapRecord mapRecord : mapRecords) {
				existingRecords.add(mapRecord);
			}
			
		}
		Logger.getLogger(WorkflowServiceJpa.class).info("Removing existing map records and updating/creating tracking records for specified concepts, found " + existingRecords.size());
		
		// remove the existing records and update the tracking records
		for (MapRecord mapRecord : existingRecords) {
			Logger.getLogger(WorkflowServiceJpa.class).warn("Removing record " + mapRecord.getId() + ", owned by " + mapRecord.getOwner().getUserName());
			mappingService.removeMapRecord(mapRecord.getId());
			
			TrackingRecord trackingRecord = getTrackingRecordForMapProjectAndConcept(mapProject, uniqueConcepts.get(mapRecord.getConceptId()));
			
			// if tracking record is null, create a new one
			if (trackingRecord == null) {
				trackingRecord = new TrackingRecordJpa();
				trackingRecord.setDefaultPreferredName(mapRecord.getConceptName());
				trackingRecord.setMapProject(mapProject);
				trackingRecord.setTerminologyId(mapRecord.getConceptId());
				trackingRecord.setTerminology(mapProject.getSourceTerminology());
				trackingRecord.setTerminologyVersion(mapProject.getSourceTerminologyVersion());
				
				trackingRecord.setSortKey(
					contentService.getTreePositions(
							trackingRecord.getTerminologyId(),
							trackingRecord.getTerminology(), 
							trackingRecord.getTerminologyVersion())
							.getTreePositions().get(0).getAncestorPath());
		
				addTrackingRecord(trackingRecord);
			} else {
				trackingRecord.removeMapRecordId(mapRecord.getId());
				updateTrackingRecord(trackingRecord);
			}
		}
			
		Logger.getLogger(WorkflowServiceJpa.class).info("Assigning concepts....");
		
		// assign the concepts to Krista (kli)
		for (String terminologyId : conceptsKLI) {
			Concept concept = uniqueConcepts.get(terminologyId);
			if (concept != null)
				this.processWorkflowAction(mapProject, concept, mapUser_KLI, null, WorkflowAction.ASSIGN_FROM_SCRATCH);
		}
		
		// assign the concepts to Nicole (nin)
		for (String terminologyId : conceptsNIN) {
			Concept concept = uniqueConcepts.get(terminologyId);
			if (concept != null)
				this.processWorkflowAction(mapProject, concept, mapUser_NIN, null, WorkflowAction.ASSIGN_FROM_SCRATCH);
		}
		
		// close the services
		contentService.close();
		mappingService.close();
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
