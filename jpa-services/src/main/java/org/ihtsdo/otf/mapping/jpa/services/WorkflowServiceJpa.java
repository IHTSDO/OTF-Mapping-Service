package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<WorkflowTrackingRecord> availableWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// if this tracking record does not have this user assigned to it
			// AND the total users assigned is less than 2
			// AND the workflow status is less than or equal to EDITING_DONE
			// This will eventually be a project specific check (i.e. for legacy
			// handling)
			
			Set<MapRecord> mapRecords = getMapRecordsForWorkflowTrackingRecord(trackingRecord);
			
			if (!getMapUsersFromMapRecords(mapRecords).contains(mapUser)
					&& getMapUsersFromMapRecords(mapRecords).size() < 2
					&& getWorkflowStatusFromMapRecords(mapRecords).compareTo(
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<WorkflowTrackingRecord> availableConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records for this project
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// if this record is marked conflict detected
			if (getWorkflowStatusForWorkflowTrackingRecord(trackingRecord).equals(
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
			MapUser mapUser, PfsParameter pfsParameter) throws Exception {

		List<WorkflowTrackingRecord> assignedWork = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// cycle over map records to find the NEW or EDITING_IN_PROGRESS
			// record assigned to this user
			for (MapRecord mapRecord : getMapRecordsForWorkflowTrackingRecord(trackingRecord)) {
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

			// get the record id and the workflow status
			for (MapRecord mapRecord : getMapRecordsForWorkflowTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
					result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
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

		List<WorkflowTrackingRecord> assignedConflicts = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// cycle over all tracking records
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// cycle over map records to find the CONFLICT_DETECTED record
			// assigned to this user
			for (MapRecord mapRecord : getMapRecordsForWorkflowTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)
						&& (mapRecord.getWorkflowStatus().compareTo(
								WorkflowStatus.CONFLICT_NEW) == 0 
								||	mapRecord.getWorkflowStatus().compareTo(
										WorkflowStatus.CONFLICT_IN_PROGRESS) == 0)){
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
			for (MapRecord mapRecord : getMapRecordsForWorkflowTrackingRecord(trackingRecord)) {
				if (mapRecord.getOwner().equals(mapUser)) {
					result.setId(mapRecord.getId());
					result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
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

		List<WorkflowTrackingRecord> availableConsensus = new ArrayList<>();
		SearchResultList results = new SearchResultListJpa();

		// find all the consensus workflow records
		for (WorkflowTrackingRecord tr : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			if (getWorkflowStatusForWorkflowTrackingRecord(tr).equals(WorkflowStatus.CONSENSUS_NEEDED)) {
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
		
		setTransactionPerOperation(true);


		// instantiate the algorithm handler for this project\
		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(mapProject
										.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
		algorithmHandler.setMapProject(mapProject);

		// locate any existing workflow tracking records for this project and
		// concept
		WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(
				mapProject, concept);
		
		Set<MapRecord> mapRecords = getMapRecordsForWorkflowTrackingRecord(trackingRecord);
		
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
				trackingRecord = new WorkflowTrackingRecordJpa();
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

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"ASSIGN_FROM_SCRATCH");

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
			if (!getMapUsersForWorkflowTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"ProcessWorkflowAction: UNASSIGN - User not assigned to record for unassignment request");

			// perform the unassign action via the algorithm handler
			mapRecords = algorithmHandler.unassign(trackingRecord, mapRecords, mapUser);

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
			if (!getMapUsersForWorkflowTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"SAVE_FOR_LATER - User not assigned to record");

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"Performing action...");

			mapRecords = algorithmHandler.saveForLater(trackingRecord, mapRecords,
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
			if (!getMapUsersForWorkflowTrackingRecord(trackingRecord).contains(mapUser))
				throw new Exception(
						"User not assigned to record for finishing request");

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"Performing action...");

			// perform the action
			mapRecords = algorithmHandler.finishEditing(trackingRecord, mapRecords,
					mapUser);

			break;

		default:
			throw new Exception("Unknown action requested.");
		}
			
		Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");
		
		Set<MapRecord> syncedRecords = synchronizeMapRecords(trackingRecord, mapRecords);
		trackingRecord.setMapRecordIds(null);
		for (MapRecord mr : syncedRecords) {
			System.out.println("Post-sync:  Adding map record " + mr.getId().toString());
			trackingRecord.addMapRecordId(mr.getId());
		}
		
		// if the tracking record is ready for removal, delete it
		if (getWorkflowStatusForWorkflowTrackingRecord(trackingRecord).equals(
				WorkflowStatus.READY_FOR_PUBLICATION)
				&& trackingRecord.getMapRecordIds().size() == 1) {
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"SYNC: Deleting workflow tracking record");
			removeWorkflowTrackingRecord(trackingRecord.getId());
		
		// else add the tracking record if new
		} else if (trackingRecord.getId() == null) {
			
			System.out.println("Adding tracking record " + trackingRecord.toString());
			addWorkflowTrackingRecord(trackingRecord);
			
		// otherwise update the tracking record
		} else	{
			System.out.println("Updating tracking record");
			System.out.println(trackingRecord.toString());
			updateWorkflowTrackingRecord(trackingRecord);
		}

	}
	
	
	/**
	 * Algorithm has gotten needlessly complex due to conflicting service changes and algorithm handler changes.
	 * However, the basic process is this:
	 * 
	 * 1) 	Function takes a set of map records returned from the algorithm handler
	 *     	These map records may have a hibernate id (updated/unchanged) or not (added)
	 * 2)	The passed map records are detached from the persistence environment.
	 * 3)	The existing (in database) records are re-retrieved from the database.
	 * 		Note that this is why the passed map records are detached -- otherwise they are overwritten.
	 * 4)	Each record in the detached set is checked against the 'refreshed' database record set
	 * 		- if the detached record is not in the set, then it has been added
	 * 		- if the detached record is in the set, check it for updates
	 * 			- if it has been changed, update it
	 * 			- if no change, disregard
	 *  5)  Each record in the 'refreshed' databased record set is checked against the new set
	 *  	- if the refreshed record is not in the new set, delete it from the database
	 *  6)  Return the detached set as re-synchronized with the database
	 *  
	 *  Note on naming conventions used in this method:
	 *  - mapRecords:  		the set of records passed in as argument
	 *  - newRecords:  		The set of records passed in as argument after persistence detaching
	 *  - oldRecords:  		The set of records retrieved by id from the database for comparison
	 *  - syncedRecords: 	The synchronized set of records for return from this routine
	 */
	@Override
	public Set<MapRecord> synchronizeMapRecords(WorkflowTrackingRecord trackingRecord, Set<MapRecord> mapRecords) throws Exception {
		
		// detach the currently persisted map records from the workflow service
		// necessary to avoid conflict with mapping service, 
		// as well as forced overwrite on retrieval of previous state
		Set<MapRecord> newRecords = new HashSet<>();
		Set<MapRecord> oldRecords = new HashSet<>();
		Set<MapRecord> syncedRecords = new HashSet<>();
		
		// detach the map records
		System.out.println("NEW RECORDS -- " + mapRecords.size());
		for (MapRecord mr : mapRecords) {
			manager.detach(mr);
			newRecords.add(mr);
			System.out.println("  Detached record: " + mr.toString());
		}
		
		// Instantiate the mapping service
		MappingService mappingService = new MappingServiceJpa();

		// retrieve the old (existing) records
		System.out.println("OLD RECORDS -- " + trackingRecord.getMapRecordIds());
		if (trackingRecord.getMapRecordIds() != null) {
			for (Long id : trackingRecord.getMapRecordIds()) {
				oldRecords.add(mappingService.getMapRecord(id));
			}
		}
		for (MapRecord mr : oldRecords) System.out.println("  " + mr.toString());
		
		// cycle over new records to check for additions or updates
		for (MapRecord mr : newRecords) {
			if (getMapRecordInSet(oldRecords, mr.getId()) == null) {
				System.out.println("Adding record: Adding detached entries to new record");
				System.out.println("  " + mr.toString());
				
				// deep copy the detached record into a new persistence-environment record
				// this routine also duplicates child collections to avoid detached object errors
				MapRecord newRecord = new MapRecordJpa(mr, true);
				
				// add the record to the database
				System.out.println("  Record id before add: " + mr.getId());
				mappingService.addMapRecord(mr);
				System.out.println("  Record id after add:  " + mr.getId());
				
				// add the record to the return list
				syncedRecords.add(newRecord);
			}

			// otherwise, check for update
			else {
				// if the old map record is changed, update it
				if (! mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
					System.out.println("Updating record " + mr.getId().toString());
					mappingService.updateMapRecord(mr);
				}
				
				syncedRecords.add(mr);
			}
		}
		
		System.out.println("New records after synchronization");
		for (MapRecord mr : syncedRecords) {
			System.out.println("  " + mr.toString());
		}
		
		// cycle over old records to check for deletions
		for (MapRecord mr : oldRecords) {
			
			System.out.println("Checking delete for record " + mr.toString());
			
			// if old record is not in the new record set, delete it
			if(getMapRecordInSet(syncedRecords, mr.getId()) == null) { 
				System.out.println("Removing record " + mr.getId().toString());
				mappingService.removeMapRecord(mr.getId());
			}
		}
		
		// close the service
		mappingService.close();
		
		
		
		return newRecords;
		
		
	}
	
	public MapRecord getMapRecordInSet(Set<MapRecord> mapRecords, Long mapRecordId) {
		if (mapRecordId == null) return null;
		
		System.out.println(mapRecordId);
		System.out.println(mapRecords.size());
		
		for (MapRecord mr : mapRecords) {
			System.out.println(mr.toString());
			System.out.println("Comparing" + mr.getId() + " " + mapRecordId);
			if (mapRecordId.equals(mr.getId())) return mr;
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
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH); 
			
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
					trackingRecord.addMapRecordId(mapRecord.getId());
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
	
	@Override
	public Set<MapRecord> getMapRecordsForWorkflowTrackingRecord(WorkflowTrackingRecord trackingRecord) throws Exception {
		Set<MapRecord> mapRecords = new HashSet<>();
		MappingService mappingService = new MappingServiceJpa();
		if (trackingRecord != null && trackingRecord.getMapRecordIds() != null) {
			for (Long id : trackingRecord.getMapRecordIds()) {
				System.out.println("getMapRecordsForWorkflowTrackingRecord: Found record " + id.toString());
				mapRecords.add(mappingService.getMapRecord(id));
			}
		}
		mappingService.close();
		return mapRecords;
	}
	
	@Override
	public Set<MapUser> getMapUsersForWorkflowTrackingRecord(WorkflowTrackingRecord trackingRecord) throws Exception {
		return getMapUsersFromMapRecords(getMapRecordsForWorkflowTrackingRecord(trackingRecord));
	}
	
	@Override
	public WorkflowStatus getWorkflowStatusForWorkflowTrackingRecord(WorkflowTrackingRecord trackingRecord) throws Exception {
		return getWorkflowStatusFromMapRecords(getMapRecordsForWorkflowTrackingRecord(trackingRecord));
	}

	@Override
	public WorkflowStatus getLowestWorkflowStatusForWorkflowTrackingRecord(WorkflowTrackingRecord trackingRecord) throws Exception {
		return getLowestWorkflowStatusFromMapRecords(getMapRecordsForWorkflowTrackingRecord(trackingRecord));
	}
	
	@Override
	public Set<MapUser> getMapUsersFromMapRecords(Set<MapRecord> mapRecords) {
		Set<MapUser> mapUsers = new HashSet<>();
		for (MapRecord mr : mapRecords) {
			mapUsers.add(mr.getOwner());
		}
		return mapUsers;
	}
	
	@Override
	public WorkflowStatus getWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.NEW;
		for (MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0) workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	@Override
	public WorkflowStatus getLowestWorkflowStatusFromMapRecords(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.REVIEW;
		for (MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0) workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
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
