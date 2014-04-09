package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler;
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
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflow(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public Workflow getWorkflow(MapProject project) throws Exception {
		Workflow m = null;
		
    List<Workflow> workflows = getWorkflows();
    for (Workflow workflow : workflows) {
    	if (workflow.getMapProject().getId().equals(project.getId())) {
    		return workflow;
    	}
    }
		return m;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#	Workflow(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void computeWorkflow(MapProject project) throws Exception {
	
		/** Remove any existing workflow object for this map project */
		if (getWorkflow(project) != null) {
			removeWorkflow(project);
		}
		
		/** Create a new Workflow object for this map project and set map project and persist it*/
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
		} 
		Workflow workflow = new WorkflowJpa();
		workflow.setMapProject(project);
		manager.persist(workflow);
		
		/** find all unmapped, in scope concepts for the specified project*/
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		SearchResultList searchResultList = mappingService.findUnmappedConceptsInScope(project.getId());
		for (SearchResult sr : searchResultList.getSearchResults()) {
			Concept concept = contentService.getConcept(sr.getTerminologyId(), sr.getTerminology(), sr.getTerminologyVersion());
			
			/* Create a workflow tracking record and persist it */
			WorkflowTrackingRecord trackingRecord = new WorkflowTrackingRecordJpa();
			trackingRecord.setWorkflow(workflow);
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
			/* set sortKey to the first tree position for the concept */
			SearchResultList treePositionsList = contentService.findTreePositionsForConcept(concept.getTerminologyId(), 
					concept.getTerminology(), concept.getTerminologyVersion());
			trackingRecord.setSortKey(treePositionsList.getSearchResults().get(0).getValue());
			manager.persist(trackingRecord);
			
			/* get MapRecords for this concept in this project */
		  List<MapRecord> mapRecords = mappingService.getMapRecordsForConcept(concept.getTerminologyId());
		  boolean conflictDetected = true;
		  boolean earlyStage = false;
		  Set<MapUser> assignedUsers = new HashSet<>();
		  if (mapRecords == null || mapRecords.size() == 0) {
		  	trackingRecord.setHasDiscrepancy(false);
		  	workflow.addTrackingRecord(trackingRecord);
		  	continue;
		  }
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
		  	trackingRecord.setMapRecords(new HashSet<>(mapRecords));
		  	workflow.addTrackingRecord(trackingRecord);
		  } else if (earlyStage) {
		  	trackingRecord.setAssignedUsers(assignedUsers);
		  	trackingRecord.setMapRecords(new HashSet<>(mapRecords));
		  	workflow.addTrackingRecord(trackingRecord);
		  } else {
		  	throw new Exception("ComputeWorkflow exception.");
		  }
		}
		
		Logger.getLogger(WorkflowServiceJpa.class).info("Done computing workflow");
		
		mappingService.close();
		contentService.close();

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.commit();
		}

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflowTrackingRecord(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.rf2.Concept)
	 */
	@Override
	public WorkflowTrackingRecord getWorkflowTrackingRecord(MapProject project,
		Concept c) throws Exception {
		WorkflowTrackingRecord m = null;
		
		Workflow workflow = getWorkflow(project);
		for (WorkflowTrackingRecord trackingRecord : workflow.getTrackingRecords()) {
			if (trackingRecord.getTerminology().equals(c.getTerminology()) &&
					trackingRecord.getTerminologyId().equals(c.getTerminologyId()) &&
					trackingRecord.getTerminologyVersion().equals(c.getTerminologyVersion()))
					return trackingRecord;
		}
		
		return m;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#removeWorkflowTrackingRecord(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void removeWorkflowTrackingRecord(MapProject project,
		WorkflowTrackingRecord record) throws Exception {
		// also need to load workflow, get all tracking records, remove from that list and save the workflow object
		Workflow workflow = getWorkflow(project);
		workflow.getTrackingRecords().remove(record);
		
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#updateWorkflowTrackingRecord(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflows()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Workflow> getWorkflows() throws Exception {

		List<Workflow> m = new ArrayList<>();
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#addWorkflow(org.ihtsdo.otf.mapping.model.MapProject)
	 */
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#removeWorkflow(org.ihtsdo.otf.mapping.model.MapProject)
	 */
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#assignUserToConcept(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public MapRecord assignUserToConcept(MapProject project, Concept concept,
		MapUser user) throws Exception {
		
		/** Creates map record (set owner (user) and workflow status (NEW)) */
		MapRecord mapRecord = new MapRecordJpa();
		mapRecord.setOwner(user);
		mapRecord.setConceptId(concept.getTerminologyId());
		mapRecord.setMapProjectId(project.getId());
		mapRecord.setConceptName(concept.getDefaultPreferredName());
		mapRecord.setWorkflowStatus(WorkflowStatus.NEW);
		mapRecord.setTimestamp(System.currentTimeMillis());
		// TODO: need to compute descendants here?
		mapRecord.setCountDescendantConcepts(0L);
		mapRecord.setFlagForConsensusReview(false);
		mapRecord.setFlagForEditorialReview(false);
		mapRecord.setFlagForMapLeadReview(false);
		mapRecord.setLastModifiedBy(user);
		mapRecord.setLastModified(System.currentTimeMillis());
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
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(trackingRecord);
			tx.commit();
		} else {
			manager.persist(trackingRecord);
		}
		
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#assignUserToConcept(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapRecord, org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void assignUserToConcept(MapProject project, Concept concept,
		MapRecord initialRecord, MapUser user) throws Exception {
		/** clone initialRecord as starting point */
		MapRecord mapRecord = new MapRecordJpa(initialRecord);
		/** set id of the cloned record to null (so Hibernate will assign a new one)*/
		mapRecord.setId(null);
		/** add the id of the initial record to the "origin ids" list of the cloned record */
    mapRecord.addOrigin(initialRecord.getId());
		mapRecord.setLastModifiedBy(user);
		mapRecord.setLastModified(System.currentTimeMillis());
    /** find the workflowTrackingRecord and add user and record to it */
    WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(project, concept);
    trackingRecord.addAssignedUser(user);
    trackingRecord.addMapRecord(mapRecord);
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(trackingRecord);
			tx.commit();
		} else {
	    manager.persist(trackingRecord);
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getMapRecordsAssignedToUser(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser)
	 */
	
	// TODO Add pfs support to this routine
	
	@Override
	public Set<MapRecord> getMapRecordsAssignedToUser(MapProject project,
		MapUser user) throws Exception {
		Set<MapRecord> mapRecordsAssigned = new HashSet<>();
		/** get workflow for map project */
		Workflow workflow = getWorkflow(project);
		/** iterate through all workflow tracking records (for unmapped in scope concepts) 
		 * and find cases where there is a map record entry where that user is the owner*/
		
		if (workflow != null) {
			for (WorkflowTrackingRecord trackingRecord : workflow.getTrackingRecordsForUnmappedInScopeConcepts()) {
				for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
					if (mapRecord.getOwner().equals(user)) {
						mapRecordsAssigned.add(mapRecord);
					}
				}
			}
		}
    return mapRecordsAssigned;

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#unassignUserFromConcept(org.ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void unassignUserFromConcept(MapProject project,
		Concept concept, MapUser user) throws Exception {
		/** get workflow for the project */
		Workflow workflow = getWorkflow(project);
		
		MappingService mappingService = new MappingServiceJpa();

		/** iterate thru tracking records until you find one for the given concept/user combination */
		for (WorkflowTrackingRecord trackingRecord : workflow.getTrackingRecords()) {
			if (trackingRecord.getTerminologyId().equals(concept.getTerminologyId()) &&
					trackingRecord.getAssignedUsers().contains(user)) {
					
				/** remove the user and the mapping record from the tracking record and save the tracking record */
				trackingRecord.removeAssignedUser(user);
				// go through all mapRecords whose owner is that user
				for (MapRecord mapRecord : trackingRecord.getMapRecords()) {
					if (mapRecord.getOwner().equals(user)) {
						
						System.out.println("Removing record");
						trackingRecord.removeMapRecord(mapRecord);
						
						// update the record
						updateWorkflowTrackingRecord(project, trackingRecord);
						
						// delete the record
						mappingService.removeMapRecord(mapRecord.getId());
					}
				}
				


			}
		}
		
		mappingService.close();

	}
	
	
	/**
	 * Returns the available tracking records for workflow and user.
	 *
	 * @param workflowId the workflow id
	 * @param userId the user id
	 * @return the available tracking records for workflow and user
	 */
	@SuppressWarnings("unchecked")
	public List<WorkflowTrackingRecord> getAvailableTrackingRecordsForWorkflowAndUser(Long workflowId, Long userId) {
		
		// return workflow tracking records where:
		// - this user is not in the list of assigned users
		// - the list of assigned users has 0 or 1 elements
		// - the workfowId matches the id of this workflow
		 javax.persistence.Query query = manager.createQuery(
					"SELECT tr FROM WorkflowTrackingRecordJpa tr "
				+ 	"WHERE NOT EXISTS (from tr.assignedUsers as user where user.id = " + userId.toString() 
				+	 ") AND size(tr.assignedUsers) < 2 AND workflow_id = " + workflowId.toString());
		
		return query.getResultList();
	}
	
	// TODO DIscuss model change to have WorkflowTrackingRecords directly connected to Workflow
	// 		i.e. WorkflowTrackingRecord->Workflow (analogous to Record->Entry)
	// 		this would enable searching and sorting in the hibernate environment
	//
	// TODO If above is not desirable, consider converting workflow.getTrackingRecords return a sorted list
	//      This would avoid some clumsy manipulation here
	@Override
	public SearchResultList findAvailableWork(Workflow workflow,
			MapUser mapUser, PfsParameter pfsParameter) {
		
		System.out.println("find available work for " + workflow.getId().toString() + ", " + mapUser.getId().toString());
		
		// create return object
		SearchResultList results = new SearchResultListJpa();
		
		List<WorkflowTrackingRecord> trackingRecords = getAvailableTrackingRecordsForWorkflowAndUser(workflow.getId(), mapUser.getId());
		
		// sort list of tracking records (see TODO above)
		Collections.sort(
						trackingRecords,
						new Comparator<WorkflowTrackingRecord>() {
							@Override
							public int compare(WorkflowTrackingRecord w1, WorkflowTrackingRecord w2) {
								return w1.getSortKey().compareTo(w2.getSortKey());
							}
						});
		
		// set the total count
		// TODO This will return erroneous count if records are aleady assigned to this user
		//      Need a better way to query for records (see TODO above)
		results.setTotalCount(new Long(trackingRecords.size()));
		
		// paging parameters
		int startIndex, maxResults;
		
		// if paging requested, retrieve parameters
		if (pfsParameter != null && pfsParameter.getStartIndex() != -1 && pfsParameter.getMaxResults() != -1) {
			startIndex = pfsParameter.getStartIndex();
			maxResults = pfsParameter.getMaxResults();
		
		// else no paging requested, return all tracking records
		} else {
			startIndex = 0;
			maxResults = trackingRecords.size();
		}
		
		// start at start index, continue until end of list or page size reached
		for (	int i = startIndex; 
				i < trackingRecords.size() && results.getCount() <= maxResults;
				i++) {
			
			WorkflowTrackingRecord trackingRecord = trackingRecords.get(i);
			
						
			// currently a redundant check
			if (!trackingRecord.getAssignedUsers().contains(mapUser) &&
					trackingRecord.getAssignedUsers().size() < 2) {
					
				SearchResult result = new SearchResultJpa();
				
				result.setId(trackingRecord.getId());
				result.setTerminology(trackingRecord.getTerminology());
				result.setTerminologyId(trackingRecord.getTerminologyId());
				result.setTerminologyVersion(trackingRecord.getTerminologyVersion());
				result.setValue(trackingRecord.getDefaultPreferredName());
				
				results.addSearchResult(result);			
			}
		}
		
		// return search results
		return results;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#close()
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getTransactionPerOperation()
	 */
	@Override
	public boolean getTransactionPerOperation() throws Exception {
		return transactionPerOperation;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#setTransactionPerOperation(boolean)
	 */
	@Override
	public void setTransactionPerOperation(boolean transactionPerOperation)
		throws Exception {
		this.transactionPerOperation = transactionPerOperation;
	}
	
	/* (non-Javadoc)
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
	
	/* (non-Javadoc)
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

	@Override
	public Map<Long, Long> compareFinishedMapRecords(MapProject mapProject) throws Exception {
	 
		Map<MapRecord, MapRecord> finishedPairsForComparison = new HashMap<MapRecord, MapRecord>();
		Map<Long, Long> conflicts = new HashMap<Long, Long>();
		
		MappingService mappingService = new MappingServiceJpa();
		List<MapRecord> allMapRecords = mappingService.getMapRecordsForMapProject(mapProject.getId());
		List<MapRecord> finishedMapRecords = new ArrayList<>();
		for (MapRecord mapRecord : allMapRecords) {
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE))
				finishedMapRecords.add(mapRecord);
		}
		MapRecord[] mapRecords = finishedMapRecords.toArray(new MapRecord[0]);
		for (int i=0; i<mapRecords.length; i++) {
			for (int j=0; j<mapRecords.length; j++) {
				if (mapRecords[i].getConceptId().equals(mapRecords[j].getConceptId()) &&
						mapRecords[i].getId() < mapRecords[j].getId()) {
							finishedPairsForComparison.put(mapRecords[i], mapRecords[j]);
				}
			}
		}
		for (Entry<MapRecord, MapRecord> entry : finishedPairsForComparison.entrySet()) {
		  DefaultProjectSpecificAlgorithmHandler handler = new DefaultProjectSpecificAlgorithmHandler();
		  ValidationResult result = handler.compareMapRecords(entry.getKey(), entry.getValue());
	  	// make new map record
	  	MapRecord newMapRecord = new MapRecordJpa(entry.getKey());
		  // assign conflicting records as origin ids and add their origin ids as well
	  	newMapRecord.addOrigin(entry.getKey().getId());
	  	newMapRecord.addOrigins(entry.getKey().getOriginIds());
	  	newMapRecord.addOrigin(entry.getValue().getId());
	  	newMapRecord.addOrigins(entry.getValue().getOriginIds());
	  	// get concept
	  	ContentService contentService = new ContentServiceJpa();
	  	Concept concept = contentService.getConcept(new Long(entry.getKey().getConceptId()));
		  contentService.close();
		  WorkflowTrackingRecord trackingRecord = getWorkflowTrackingRecord(mapProject, concept);
	    
		  if (!result.isValid()) {
		  	conflicts.put(entry.getKey().getId(), entry.getValue().getId());
		  	//assign new record conflict detected
		  	newMapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
			  // set owner to default
		  	MapUser mapUser = mappingService.getMapUser("default");
		  	newMapRecord.setOwner(mapUser);
	  	  // update workflowtrackingrecord
		    trackingRecord.setHasDiscrepancy(true);
		    trackingRecord.addMapRecord(newMapRecord);
		  } else {
		  	// make new map record that is a copy of one of the other ones
		  	// add to it all the notes from the other one
		  	// set origin ids + ids of two records
		  	// TODO: confirm the first steps are the same
		  	// TODO: who is the owner? set it
		  	// set workflowStatus to ready for publication
		  	newMapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
		  	// delete these 2 records and delete the tracking record
		  	mappingService.removeMapRecord(entry.getKey().getId());
		  	mappingService.removeMapRecord(entry.getValue().getId());
		  	removeWorkflowTrackingRecord(mapProject, trackingRecord);
		  }
		}
		mappingService.close();
		return conflicts;
	}



	
}
