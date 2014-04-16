package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
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
public class WorkflowServiceJpa implements WorkflowService {

	/** The factory. */
	private static EntityManagerFactory factory;

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

		// created once or if the factory has been closed
		if (factory == null || !factory.isOpen()) {
			factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		}

		// created on each instantiation
		manager = factory.createEntityManager();

	}

	@Override
	public WorkflowTrackingRecord addWorkflowTrackingRecord(
			WorkflowTrackingRecord workflowTrackingRecord) throws Exception {
		
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(workflowTrackingRecord);
			tx.commit();
		} else {
			manager.persist(workflowTrackingRecord);
		}
		
		return workflowTrackingRecord;
	}

	@Override
	public void removeWorkflowTrackingRecord(Long workflowTrackingRecordId) throws Exception {


		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			WorkflowTrackingRecord ma = manager.find(WorkflowTrackingRecordJpa.class, workflowTrackingRecordId);
			
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			WorkflowTrackingRecord ma = manager.find(WorkflowTrackingRecordJpa.class, workflowTrackingRecordId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}

	}

	@Override
	public void updateWorkflowTrackingRecord(WorkflowTrackingRecord record) throws Exception {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(record);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(record);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecords() {
		
		return manager.createQuery("select tr from WorkflowTrackingRecordJpa tr")
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<WorkflowTrackingRecord> getWorkflowTrackingRecordsForMapProject(
			MapProject mapProject) {
		return manager.createQuery("select tr from WorkflowTrackingRecordJpa tr where mapProject_id = :mapProjectId")
				.setParameter("mapProjectId", mapProject.getId())
				.getResultList();
	}

	@Override
	public WorkflowTrackingRecord getWorkflowTrackingRecord(
			MapProject mapProject, Concept concept) {
		
		javax.persistence.Query query = manager.createQuery("select tr from WorkflowTrackingRecordJpa tr where mapProject_id = :mapProjectId and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminologyId", concept.getTerminologyId());
		
		try {
			return (WorkflowTrackingRecord) query.getSingleResult();
			
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"WorkflowService.getWorkflowTrackingRecord(): Concept query for terminologyId = "
							+ concept.getTerminologyId() + ", mapProjectId = " + mapProject.getId().toString() + " returned no results.");
			return null;
		}
	}

	@Override
	public List<WorkflowTrackingRecord> getAvailableWork(MapProject mapProject,
			MapUser mapUser) {
		
		List<WorkflowTrackingRecord> trackingRecords = new ArrayList<>();

		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {

			// if this tracking record does not have this user assigned to it AND the total users assigned is less than 2
			// TODO: This will eventually be a project specific check (i.e. for legacy handling)
			if (!trackingRecord.getAssignedUsers().contains(mapUser) &&
					trackingRecord.getAssignedUsers().size() < 2) {

					trackingRecords.add(trackingRecord);
			}
		}
		return trackingRecords;
	}

	@Override
	public List<WorkflowTrackingRecord> getAvailableConflicts(MapProject mapProject,
			MapUser mapUser) {
		
		List<WorkflowTrackingRecord> availableConflicts = new ArrayList<>();

		// cycle over all tracking records for this project
		for(WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			
			// if this record is marked conflict detected
			if (trackingRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
				
				// add the search result
				availableConflicts.add(trackingRecord);
			}
		}
		return availableConflicts;
	}

	@Override
	public List<MapRecord> getAssignedWork(MapProject mapProject,
			MapUser mapUser) {
		
		List<MapRecord> mapRecordsAssigned = new ArrayList<>();

		// cycle over all tracking records
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				
				// if this record is owned by user and has workflow status < EDITING_DONE. add tp ;ost
				if (mapRecord.getOwner().equals(mapUser) &&
						mapRecord.getWorkflowStatus().compareTo(WorkflowStatus.EDITING_DONE) < 0) {
					mapRecordsAssigned.add(mapRecord);
				}
			}
		}

		return mapRecordsAssigned;
	}

	@Override
	public List<MapRecord> getAssignedConflicts(MapProject mapProject,
			MapUser mapUser) {
		
		List<MapRecord> conflictsAssigned = new ArrayList<>();
		
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			
			// cycle over all map records
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				
				// if this record is in conflict resolution and has this user assigned
				if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS) 
						&& mapRecord.getOwner().equals(mapUser)) {
					conflictsAssigned.add(mapRecord);
				}
			}
		}
		
		return conflictsAssigned;
	}

	@Override
	public List<WorkflowTrackingRecord> getAvailableConsensusWork(MapProject mapProject) {
		
		List<WorkflowTrackingRecord> consensusWorkflowTrackingRecords = new ArrayList<>();
		
		for (WorkflowTrackingRecord tr : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			if (tr.getWorkflowStatus().equals(WorkflowStatus.CONSENSUS_NEEDED)) {
				consensusWorkflowTrackingRecords.add(tr);
			}
		}
		
		return consensusWorkflowTrackingRecords;
	}

	@Override
	public void processWorkflowAction(MapProject mapProject, Concept concept, MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction) throws Exception {
		
		// instantiate the algorithm handler for this project
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				(ProjectSpecificAlgorithmHandler) Class.forName("org.ihtsdo.otf.mapping.jpa.handlers." + mapProject.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
		algorithmHandler.setMapProject(mapProject); 
		
		// locate any existing workflow tracking records for this project and concept
		WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(mapProject, concept);

		// switch on workflow action
		switch (workflowAction) {
			case ASSIGN_FROM_INITIAL_RECORD:
				
				Logger.getLogger(WorkflowServiceJpa.class).info("ASSIGN_FROM_INITIAL_RECORD");
				
				
				// expect no tracking record, double-check
				if (trackingRecord != null) {
					throw new Exception("ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Found tracking record for published record where none was expected!");
				}
				
				// expect a map record to be passed in
				if (mapRecord == null) {
					throw new Exception("ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Call to assign from intial record must include an existing map record");
				}
				
				trackingRecord = new WorkflowTrackingRecordJpa();
				trackingRecord.setMapProject(mapProject);
				trackingRecord.setTerminology(concept.getTerminology());
				trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
				trackingRecord.setTerminologyId(concept.getTerminologyId());
				trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
				
				// perform the assign action via the algorithm handler
				trackingRecord = algorithmHandler.assignFromInitialRecord(trackingRecord, mapRecord, mapUser);
				
				// add the tracking record
				addWorkflowTrackingRecord(trackingRecord);
				
				break;
				
			case ASSIGN_FROM_SCRATCH:
				
				Logger.getLogger(WorkflowServiceJpa.class).info("ASSIGN_FROM_SCRATCH");
				
				// expect existing (pre-computed) workflow tracking record
				if (trackingRecord == null) {
					throw new Exception("Could not find tracking record for assignment.");
				}
				
				// perform the assignment via the algorithm handler
				trackingRecord = algorithmHandler.assignFromScratch(trackingRecord, concept, mapUser);
				
				// update the tracking record
				updateWorkflowTrackingRecord(trackingRecord);

				
				break;
				
				
				
			case UNASSIGN:
				
				Logger.getLogger(WorkflowServiceJpa.class).info("UNASSIGN");
				
				
				// expect existing (pre-computed) workflow tracking record to exist with this user assigned
				if (trackingRecord == null) throw new Exception("ProcessWorkflowAction: UNASSIGN - Could not find tracking record for unassignment.");
				
				// expect this user to be assigned to a map record in this tracking record
				if (!trackingRecord.getAssignedUsers().contains(mapUser)) throw new Exception("ProcessWorkflowAction: UNASSIGN - User not assigned to record for unassignment request");
				
				// perform the unassign action via the algorithm handler
				trackingRecord = algorithmHandler.unassign(trackingRecord, mapUser);
				
				// update the tracking record
				updateWorkflowTrackingRecord(trackingRecord);
				
				break;
				
			case FINISH_EDITING:
				
				Logger.getLogger(WorkflowServiceJpa.class).info("FINISH_EDITING");
				
				// expect existing (pre-computed) workflow tracking record to exist with this user assigned
				if (trackingRecord == null) throw new Exception("ProcessWorkflowAction: FINISH_EDITING - Could not find tracking record for unassignment.");
				
				// expect this user to be assigned to a map record in this tracking record
				if (!trackingRecord.getAssignedUsers().contains(mapUser)) throw new Exception("User not assigned to record for unassignment request");
				
				// perform the finish editing action via the algorithm handler
				trackingRecord = algorithmHandler.finishEditing(trackingRecord, mapUser);
				
				// if ready for publication, remove workflow tracking record
				if (trackingRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
					removeWorkflowTrackingRecord(trackingRecord.getId());
					
				// otherwise update workflow tracking record
				} else {
					updateWorkflowTrackingRecord(trackingRecord);
				}		

				break;
				
			default: 
				throw new Exception("Unknown action requested.");
		}
		
		
	}

	@Override
	public void computeWorkflow(MapProject mapProject) throws Exception {

		// Clear the workflow for this project
		clearWorkflowForMapProject(mapProject);

		// open the services
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		
		// find the unmapped concepts in scope
		SearchResultList unmappedConceptsInScope = mappingService.findUnmappedConceptsInScope(mapProject.getId());
	
		for (SearchResult sr : unmappedConceptsInScope.getSearchResults()) {
			
			// retrieve the concept for this result
			Concept concept = contentService.getConcept(sr.getTerminologyId(), sr.getTerminology(), sr.getTerminologyVersion());

			// create a workflow tracking record for this concept
			WorkflowTrackingRecord trackingRecord = new WorkflowTrackingRecordJpa();
			
			// populate the fields from project and concept
			trackingRecord.setMapProject(mapProject);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH); // TODO: Make this a project specific call later (phase 2)
			
			// get the tree positions for this concept and set the sort key to the first retrieved
			SearchResultList treePositionsList = contentService.findTreePositionsForConcept(
					concept.getTerminologyId(), concept.getTerminology(), concept.getTerminologyVersion());
			trackingRecord.setSortKey(treePositionsList.getSearchResults().get(0).getValue());
							
			// persist the workflow tracking record
			addWorkflowTrackingRecord(trackingRecord);

			// retrieve map records for this project and concept
			List<MapRecord> mapRecords = mappingService.getMapRecordsForConcept(concept.getTerminologyId());

			// cycle over records retrieved
			for (MapRecord mapRecord : mapRecords) {

				// if this record belongs to project, add it to tracking record
				if (mapRecord.getMapProjectId().equals(mapProject.getId())) {				 
					trackingRecord.addMapRecord(mapRecord);
				}
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info("Done computing workflow");

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			tx.commit();
		}
		
		mappingService.close();
		contentService.close();

		

	}
	
	@Override
	public void clearWorkflowForMapProject(MapProject mapProject) throws Exception {

		for (WorkflowTrackingRecord tr : getWorkflowTrackingRecordsForMapProject(mapProject)) {
			removeWorkflowTrackingRecord(tr.getId());
		}
		
	}

	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
	}

	@Override
	public boolean getTransactionPerOperation() throws Exception {
		return transactionPerOperation;
	}

	@Override
	public void setTransactionPerOperation(boolean transactionPerOperation)
		throws Exception {
		this.transactionPerOperation = transactionPerOperation;
	}

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

	/*

	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts(MapProject mapProject) {
		Set<WorkflowTrackingRecord> unmappedTrackingRecords = new HashSet<>();
		for (WorkflowTrackingRecord trackingRecord : getWorkflowTrackingRecords(mapProject)) {
			if (trackingRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)
					|| trackingRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS)
					|| trackingRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE))
				unmappedTrackingRecords.add(trackingRecord);
		}
		return unmappedTrackingRecords;
	}

	*/


}
