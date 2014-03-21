package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.Workflow;
import org.ihtsdo.otf.mapping.workflow.WorkflowJpa;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecordJpa;

public class WorkflowServiceJpa implements WorkflowService {
	
	/** The factory. */
	private static EntityManagerFactory factory;

	/** The manager. */
	private EntityManager manager;

	/** The full text entity manager. */
	private FullTextEntityManager fullTextEntityManager;

	/** The indexed field names. */
	private static Set<String> fieldNames;

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

		// fieldNames created once
		if (fieldNames == null) {
			fieldNames = new HashSet<String>();

			fullTextEntityManager =
					org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
			IndexReaderAccessor indexReaderAccessor =
					fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
			Set<String> indexedClassNames =
					fullTextEntityManager.getSearchFactory().getStatistics()
							.getIndexedClassNames();
			for (String indexClass : indexedClassNames) {
				IndexReader indexReader = indexReaderAccessor.open(indexClass);
				try {
					for (FieldInfo info : ReaderUtil.getMergedFieldInfos(indexReader)) {
						fieldNames.add(info.name);
					}
				} finally {
					indexReaderAccessor.close(indexReader);
				}
			}

			if (fullTextEntityManager != null) {
				fullTextEntityManager.close();
			}

			// closing fullTextEntityManager also closes manager, recreate
			manager = factory.createEntityManager();
		}
	}
	
	@Override
	public Workflow getWorkflow(MapProject project) throws Exception {
		Workflow m = null;
		
    List<Workflow> workflows = getWorkflows();
    for (Workflow workflow : workflows) {
    	if (workflow.getMapProject().equals(project)) {
    		return workflow;
    	}
    }
		return m;
	}

	@Override
	public void computeWorkflow(MapProject project) throws Exception {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
		}
		
		/** Remove any existing workflow object for this map project */
		if (getWorkflow(project) != null) {
			removeWorkflow(project);
		}
		
		/** Create a new Workflow object for this map project and set map project and persist it*/
		Workflow workflow = new WorkflowJpa();
		workflow.setMapProject(project);
		manager.persist(workflow);
		
		/** find all unmapped, in scope concepts for the specified project*/
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		SearchResultList searchResultList = mappingService.findUnmappedConceptsInScope(project);
		for (SearchResult sr : searchResultList.getSearchResults()) {
			Concept concept = contentService.getConcept(new Long(sr.getTerminologyId()));
			
			/* Create a workflow tracking record and persist it */
			WorkflowTrackingRecord trackingRecord = new WorkflowTrackingRecordJpa();
			manager.persist(trackingRecord);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
			/* set sortKey to the first tree position for the concept */
			SearchResultList treePositionsList = contentService.findTreePositionsForConcept(concept.getTerminologyId(), 
					concept.getTerminology(), concept.getTerminologyVersion());
			trackingRecord.setSortKey(treePositionsList.getSearchResults().get(0).getValue());
			
			/* get MapRecords for this concept in this project */
		  List<MapRecord> mapRecords = mappingService.getMapRecordsForConcept(concept);
		  boolean conflictDetected = true;
		  boolean earlyStage = false;
		  Set<MapUser> assignedUsers = new HashSet<MapUser>();
		  for (MapRecord mapRecord : mapRecords) {
		  	if (!mapRecord.getMapProjectId().equals(project.getId()))
		  		continue;
		  	assignedUsers.add(mapRecord.getOwner());
		  	if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED))
		  		conflictDetected = false;
		  	if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW) ||
		  			mapRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_IN_PROGRESS) ||
		  			mapRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE))
		  		earlyStage = true;
		  }
		  if (conflictDetected) {
		  	trackingRecord.setHasDiscrepancy(true);
		  	trackingRecord.setAssignedUsers(assignedUsers);
		  	trackingRecord.setMapRecords(new HashSet<MapRecord>(mapRecords));
		  } else if (earlyStage) {
		  	trackingRecord.setAssignedUsers(assignedUsers);
		  	trackingRecord.setMapRecords(new HashSet<MapRecord>(mapRecords));
		  } else {
		  	throw new Exception("ComputeWorkflow exception.");
		  }
		  //TODO: add transaction management and commit;
		}
		
		mappingService.close();
		contentService.close();

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.commit();
		}

	}

	@Override
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject project,
		Concept c) throws Exception {
		WorkflowTrackingRecord m = null;
		
		Workflow workflow = getWorkflow(project);
		Set<WorkflowTrackingRecord> allTrackingRecords = new HashSet<WorkflowTrackingRecord>();
		allTrackingRecords.addAll(workflow.getTrackingRecordsForConflictConcepts());
		allTrackingRecords.addAll(workflow.getTrackingRecordsForUnmappedInScopeConcepts());
		for (WorkflowTrackingRecord trackingRecord : allTrackingRecords) {
			if (trackingRecord.getTerminology().equals(c.getTerminology()) &&
					trackingRecord.getTerminologyId().equals(c.getTerminologyId()) &&
					trackingRecord.getTerminologyVersion().equals(c.getTerminologyVersion()))
					return trackingRecord;
		}
		
		return m;
	}

	@Override
	public void removeWorkflowTrackingRecord(MapProject project,
		WorkflowTrackingRecord record) throws Exception {
		// also need to load workflow, get all tracking records, remove from that list and save the workflow object
		Workflow workflow = getWorkflow(project);
		Set<WorkflowTrackingRecord> allTrackingRecords = new HashSet<WorkflowTrackingRecord>();
		allTrackingRecords.addAll(workflow.getTrackingRecordsForConflictConcepts());
		allTrackingRecords.addAll(workflow.getTrackingRecordsForUnmappedInScopeConcepts());
		allTrackingRecords.remove(record);
		
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			WorkflowTrackingRecord ma = manager.find(WorkflowTrackingRecordJpa.class, record.getId());
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			if (manager.contains(workflow)) {
				manager.remove(workflow);
			} else {
				manager.remove(manager.merge(workflow));
			}
			tx.commit();
		} else {
			WorkflowTrackingRecord ma = manager.find(WorkflowTrackingRecordJpa.class, record.getId());
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			if (manager.contains(workflow)) {
				manager.remove(workflow);
			} else {
				manager.remove(manager.merge(workflow));
			}
		}

	}

	@Override
	public void updateWorkflowTrackingRecord(MapProject project,
		WorkflowTrackingRecord record) throws Exception {
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
	public List<Workflow> getWorkflows() throws Exception {
		List<Workflow> m = null;
		
		javax.persistence.Query query = 
				manager.createQuery("select m from WorkflowJpa m");
		
		try {
			m = query.getResultList();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Workflow query returned no results!");
			return null;
		}
		return m;
	}

	@Override
	public void addWorkflow(MapProject project) throws Exception {
		// Check if there is a workflow for this project first?
		Workflow workflow = getWorkflow(project);
		if (workflow == null) {
			workflow = new WorkflowJpa();
			workflow.setMapProject(project);
		}
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(workflow);
			tx.commit();
		} else {
			manager.persist(workflow);
		}		
		return;
	}

	@Override
	public void removeWorkflow(MapProject project) throws Exception {
		Workflow workflow = getWorkflow(project);
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			Workflow ma = manager.find(WorkflowJpa.class, workflow.getId());
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			Workflow ma = manager.find(WorkflowJpa.class, workflow.getId());
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}
	}

	@Override
	public void assignUserToConcept(MapProject project, Concept concept,
		MapUser user) throws Exception {
		
		/** Creates map record (set owner (user) and workflow status (NEW)) */
		MapRecord mapRecord = new MapRecordJpa();
		mapRecord.setOwner(user);
		mapRecord.setWorkflowStatus(WorkflowStatus.NEW);
		MappingService mappingService = new MappingServiceJpa();
		mappingService.addMapRecord(mapRecord);
		mappingService.close();
		
		/** Get the workflow tracking record for this map project/concept (throw error if not found) */
		WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(project, concept);
		if (trackingRecord == null)
			throw new Exception("WorkflowTrackingRecord for project: " + project + " concept: " + concept + "was not found.");
		
		/** Adds user and map record to workflow tracking record, throw an exception if the user is already in the assignedUsers */
		if (trackingRecord.getAssignedUsers().contains(user))
			throw new Exception("User " + user.getUserName() + " is already assigned to this tracking record: " + trackingRecord);
		trackingRecord.addAssignedUser(user);
		trackingRecord.addMapRecord(mapRecord);
	}

	@Override
	public void assignUserToConcept(MapProject project, Concept concept,
		MapRecord initialRecord, MapUser user) throws Exception {
		/** clone initialRecord as starting point */
		MapRecord mapRecord = new MapRecordJpa(initialRecord);
		/** set id of the cloned record to null (so Hibernate will assign a new one)*/
		mapRecord.setId(null);
		/** add the id of the initial record to the "origin ids" list of the cloned record */
    mapRecord.addOrigin(initialRecord.getId());
    /** find the workflowTrackingRecord and add user and record to it */
    WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(project, concept);
    trackingRecord.addAssignedUser(user);
    trackingRecord.addMapRecord(mapRecord);
	}

	@Override
	public Set<MapRecord> getMapRecordsAssignedToUser(MapProject project,
		MapUser user) throws Exception {
		Set<MapRecord> mapRecordsAssigned = new HashSet<MapRecord>();
		/** get workflow for map project */
		Workflow workflow = getWorkflow(project);
		/** iterate through all workflow tracking records (for unmapped in scope concepts) 
		 * and find cases where there is a map record entry where that user is the owner*/
		for (WorkflowTrackingRecord trackingRecord : workflow.getTrackingRecordsForUnmappedInScopeConcepts()) {
			for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
				if (mapRecord.getOwner().equals(user)) {
					mapRecordsAssigned.add(mapRecord);
				}
			}
		}
    return mapRecordsAssigned;

	}

	@Override
	public void unassignUserFromConcept(MapProject project,
		Concept concept, MapUser user) throws Exception {
		/** get workflow for the project */
		Workflow workflow = getWorkflow(project);
		
		/** iterate thru tracking records until you find one for the given concept/user combination */
		Set<WorkflowTrackingRecord> allTrackingRecords = new HashSet<WorkflowTrackingRecord>();
		allTrackingRecords.addAll(workflow.getTrackingRecordsForConflictConcepts());
		allTrackingRecords.addAll(workflow.getTrackingRecordsForUnmappedInScopeConcepts());
		for (WorkflowTrackingRecord trackingRecord : allTrackingRecords) {
			if (trackingRecord.getTerminology().equals(concept.getTerminologyId()) &&
					trackingRecord.getAssignedUsers().contains(user)) {
				/** remove the user and the mapping record from the tracking record and save the tracking record */
				trackingRecord.removeAssignedUser(user);
				// go through all mapRecords whose owner is that user
				for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
					if (mapRecord.getOwner().equals(user))
				    trackingRecord.removeMapRecord(mapRecord);
				}
				updateWorkflowTrackingRecord(project, trackingRecord);
			}
		}
		/** remove the matching map record using mappingService.removeMapRecord() */

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
}
