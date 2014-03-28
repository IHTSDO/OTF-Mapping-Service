package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;

// TODO: Auto-generated Javadoc
/**
 * Interface for services to retrieve (get) map objects.
 *
 * @author Patrick
 */
public interface MappingService {
	
	
	/**
	 * Closes the manager associated with service.
	 *
	 * @throws Exception the exception
	 */
	public void close() throws Exception;

	//////////////////////////////
	// Basic retrieval services //
	//////////////////////////////
	
	/**
	 * Return map project for auto-generated id.
	 *
	 * @param id the auto-generated id
	 * @return the MapProject
	 * @throws Exception the exception
	 */
	public MapProject getMapProject(Long id) throws Exception;
	
	/**
	 * Gets the map project.
	 *
	 * @param name the name
	 * @return the map project
	 * @throws Exception the exception
	 */
	public MapProject getMapProjectByName(String name) throws Exception;
	
	/**
	 * Gets the map project by ref set id.
	 *
	 * @param refSetId the ref set id
	 * @return the map project by ref set id
	 * @throws Exception the exception
	 */
	public MapProject getMapProjectByRefSetId(String refSetId) throws Exception;
	
	/**
	 * Return map user for auto-generated id.
	 *
	 * @param id the auto-generated id
	 * @return the mapUser
	 * @throws Exception the exception
	 */
	public MapUser getMapUser(Long id) throws Exception;
	
	/**
	 * Return map record for auto-generated id.
	 *
	 * @param id the auto-generated id
	 * @return the mapRecord
	 * @throws Exception the exception
	 */
	public MapRecord getMapRecord(Long id) throws Exception;
	
	/**
	 * Returns all map projects.
	 *
	 * @return a List of MapProjects
	 * @throws Exception the exception
	 */
	public List<MapProject> getMapProjects() throws Exception;
	
	/**
	 * Retrieve all map users.
	 *
	 * @return a List of MapUsers
	 * @throws Exception the exception
	 */
	public List<MapUser> getMapUsers() throws Exception;
	
	/**
	 * Retrieve all map principles.
	 *
	 * @return a List of MapPrinciples
	 * @throws Exception the exception
	 */
	public List<MapPrinciple> getMapPrinciples() throws Exception;
	
	/**
	 * Retrieve all map advices.
	 *
	 * @return a List of MapAdvices
	 * @throws Exception the exception
	 */
	public List<MapAdvice> getMapAdvices() throws Exception;
	
	/**
	 * Retrieve all map projects assigned to a particular map user.
	 *
	 * @param mapUser the map user
	 * @return a List of MapProjects
	 * @throws Exception the exception
	 */
	public List<MapProject> getMapProjectsForMapUser(MapUser mapUser) throws Exception;
	
	/**
	 * Retrieve all map records.
	 *
	 * @return a List of MapRecords
	 * @throws Exception the exception
	 */
	public List<MapRecord> getMapRecords() throws Exception;
	
	/**
	 * Retrieve all map records associated with a given concept id.
	 *
	 * @param query the query
	 * @param pfsParameter the pfs parameter
	 * @return a List of MapRecords
	 * @throws Exception the exception
	 */
	
	////////////////////
	// Query services //
	////////////////////
	
	/**
	 * Query for MapProjects
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the list of MapProjects
	 * @throws Exception 
	 */
	public SearchResultList findMapProjects(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapUsers.
	 *
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapUsers
	 * @throws Exception the exception
	 */
	public SearchResultList findMapUsers(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapAdvices.
	 *
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapAdvices
	 * @throws Exception the exception
	 */
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapRecords.
	 *
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapRecords
	 * @throws Exception the exception
	 */
	public SearchResultList findMapRecords(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapEntrys.
	 *
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapEntrys
	 * @throws Exception the exception
	 */
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter) throws Exception;

	////////////////////////////
	// Addition services     ///
    ////////////////////////////
	
	/**
	 * Adds the map user.
	 *
	 * @param mapUser the map user
	 * @return the map user
	 * @throws Exception the exception
	 */
	public MapUser addMapUser(MapUser mapUser) throws Exception;
	
	/**
	 * Adds the map project.
	 *
	 * @param mapProject the map project
	 * @return the map project
	 * @throws Exception the exception
	 */
	public MapProject addMapProject(MapProject mapProject) throws Exception;
	
	/**
	 * Adds the map record.
	 *
	 * @param mapRecord the map record
	 * @return the map record
	 * @throws Exception the exception
	 */
	public MapRecord addMapRecord(MapRecord mapRecord) throws Exception;
	
	/**
	 * Adds the map entry.
	 *
	 * @param mapEntry the map entry
	 * @throws Exception the exception
	 */
	public void addMapEntry(MapEntry mapEntry) throws Exception;
	
	/**
	 * Adds the map principle.
	 *
	 * @param mapPrinciple the map principle
	 * @throws Exception the exception
	 */
	public void addMapPrinciple(MapPrinciple mapPrinciple) throws Exception;
	
	/**
	 * Adds the map advice.
	 *
	 * @param mapAdvice the map advice
	 * @throws Exception the exception
	 */
	public void addMapAdvice(MapAdvice mapAdvice) throws Exception;
	
	////////////////////////////
	// Update services     ///
	////////////////////////////
	
	/**
	 * Update map user.
	 *
	 * @param mapUser the map user
	 * @throws Exception the exception
	 */
	public void updateMapUser(MapUser mapUser) throws Exception;
	
	
	/**
	 * Update map project.
	 *
	 * @param mapProject the map project
	 * @throws Exception the exception
	 */
	public void updateMapProject(MapProject mapProject) throws Exception;
	
	/**
	 * Update map record.
	 *
	 * @param mapRecord the map record
	 * @return the map record
	 * @throws Exception the exception
	 */
	public MapRecord updateMapRecord(MapRecord mapRecord) throws Exception;
	
	/**
	 * Update map entry.
	 *
	 * @param mapEntry the map entry
	 * @throws Exception the exception
	 */
	public void updateMapEntry(MapEntry mapEntry) throws Exception;
	
	/**
	 * Update map principle.
	 *
	 * @param mapPrinciple the map principle
	 * @throws Exception the exception
	 */
	public void updateMapPrinciple(MapPrinciple mapPrinciple) throws Exception;
	
	/**
	 * Update map advice.
	 *
	 * @param mapAdvice the map advice
	 * @throws Exception the exception
	 */
	public void updateMapAdvice(MapAdvice mapAdvice) throws Exception;
	
	////////////////////////////
	//Removal services     ///
	////////////////////////////
	
	/**
	 * Removes the map user.
	 *
	 * @param mapUserId the map user id
	 * @throws Exception the exception
	 */
	public void removeMapUser(Long mapUserId) throws Exception;
	
	/**
	 * Removes the map project.
	 *
	 * @param mapProjectId the map project id
	 * @throws Exception the exception
	 */
	public void removeMapProject(Long mapProjectId) throws Exception;
	
	/**
	 * Removes the map record.
	 *
	 * @param mapRecordId the map record id
	 * @throws Exception the exception
	 */
	public void removeMapRecord(Long mapRecordId) throws Exception;
	
	/**
	 * Removes the map entry.
	 *
	 * @param mapEntryId the map entry id
	 * @throws Exception the exception
	 */
	public void removeMapEntry(Long mapEntryId) throws Exception;
	
	/**
	 * Removes the map principle.
	 *
	 * @param mapPrincipleId the map principle id
	 * @throws Exception the exception
	 */
	public void removeMapPrinciple(Long mapPrincipleId) throws Exception;
	
	/**
	 * Removes the map advice.
	 *
	 * @param mapAdviceId the map advice id
	 * @throws Exception the exception
	 */
	public void removeMapAdvice(Long mapAdviceId) throws Exception;
	
	
	///////////////////////////
	// Other services       ///
	///////////////////////////
	
	/**
	 * Gets the map record count for map project id.
	 *
	 * @param mapProjectId the map project id
	 * @param pfsParameter the pfs parameter
	 * @return the map record count for map project id
	 * @throws Exception the exception
	 */
	public Long getMapRecordCountForMapProject(Long mapProjectId,
			PfsParameter pfsParameter) throws Exception;
	
	
		
	/**
	 * Gets the map records for concept id.
	 *
	 * @param terminologyId the concept id
	 * @return the map records for concept id
	 * @throws Exception the exception
	 */
	public List<MapRecord> getMapRecordsForConcept(String terminologyId) throws Exception;

	/**
	 * Returns the unmapped descendants for concept.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param threshold the threshold
	 * @return the unmapped descendants for concept
	 * @throws Exception the exception
	 */
	public SearchResultList findUnmappedDescendantsForConcept(String terminologyId,
			String terminology, String terminologyVersion, int threshold) throws Exception;

	/**
	 * Gets the map principle.
	 *
	 * @param id the id
	 * @return the map principle
	 * @throws Exception the exception
	 */
	public MapPrinciple getMapPrinciple(Long id) throws Exception;

	/**
	 * Creates the map records for map project.
	 *
	 * @param mapProjectId the map project id
	 * @param workflowStatus the workflow status
	 * @throws Exception the exception
	 */
	public void createMapRecordsForMapProject(Long mapProjectId,  WorkflowStatus workflowStatus) throws Exception;
	

	/**
	 * Creates the map records for map project.
	 *
	 * @param mapProjectId the map project id
	 * @param complexMapRefSetMembers the complex map ref set members
	 * @param workflowStatus TODO
	 * @throws Exception the exception
	 */
	public void createMapRecordsForMapProject(Long mapProjectId, 
			List<ComplexMapRefSetMember> complexMapRefSetMembers, WorkflowStatus workflowStatus) throws Exception;

	/**
	 * Removes the map records for project id.
	 *
	 * @param mapProjectId the map project id
	 * @return the long
	 * @throws Exception the exception
	 */
	public Long removeMapRecordsForProject(Long mapProjectId) throws Exception;
	
	/**
	 * Helper function not requiring a PFS object.
	 *
	 * @param mapProjectId the id of the map project
	 * @return the map records for a project id
	 * @throws Exception the exception
	 */
	public List<MapRecord> getMapRecordsForMapProject(Long mapProjectId) throws Exception;
	
	/**
	 * Helper function which calls either a simple query or lucene query depending on filter parameters.
	 *
	 * @param mapProjectId the project id
	 * @param pfsParameter the page/filter/sort parameter object
	 * @return the map records for map project id
	 * @throws Exception the exception
	 */
	public List<MapRecord> getMapRecordsForMapProject(Long mapProjectId,
			PfsParameter pfsParameter) throws Exception;

	/**
	 * Helper function for retrieving map records given a concept id.
	 *
	 * @param conceptId the concept id
	 * @return the map records where this concept is referenced
	 */
	public List<MapRecord> getMapRecordsForConcept(Long conceptId);

	/**
	 * Find concepts in scope.
	 *
     * @param mapProjectId the map project id
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findConceptsInScope(Long mapProjectId)
		throws Exception;

	/**
	 * Find unmapped concepts in scope.
	 *
     * @param mapProjectId the map project id
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findUnmappedConceptsInScope(Long mapProjectId)
		throws Exception;

	/**
	 * Find mapped concepts out of scope bounds.
	 *
     * @param mapProjectId the map project id
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findMappedConceptsOutOfScopeBounds(Long mapProjectId)
		throws Exception;

	/**
	 * Find concepts excluded from scope.
	 *
     * @param mapProjectId the map project id
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findConceptsExcludedFromScope(Long mapProjectId)
		throws Exception;

	/**
	 * Indicates whether or not concept in scope is the case.
	 *
	 * @param terminologyId the concept id
     * @param mapProjectId the map project id
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 * @throws Exception the exception
	 */
	public boolean isConceptInScope(String terminologyId, Long mapProjectId)
		throws Exception;

	/**
	 * Indicates whether or not concept excluded from scope is the case.
	 *
     * @param terminologyId the concept id
     * @param mapProjectId the map project id
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 * @throws Exception the exception
	 */
	public boolean isConceptExcludedFromScope(String terminologyId, Long mapProjectId)
		throws Exception;

	/**
	 * Indicates whether or not concept out of scope bounds is the case.
	 *
     * @param terminologyId the concept id
     * @param mapProjectId the map project id
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 * @throws Exception the exception
	 */
	public boolean isConceptOutOfScopeBounds(String terminologyId, Long mapProjectId)
		throws Exception;

	/**
	 * Gets the map user.
	 *
	 * @param userName the user name
	 * @return the map user
	 */
	public MapUser getMapUser(String userName);

	/**
	 * Indicates whether or not a user with the specified username exists.
	 *
	 * @param mapUser the map user name
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean userExists(String mapUser);

	/**
	 * Returns the map age ranges.
	 *
	 * @return the map age ranges
	 */
	public List<MapAgeRange> getMapAgeRanges();
	
	/**
	 * Adds the map age range.
	 *
	 * @param ageRange the age range
	 * @return the map age range
	 */
	public MapAgeRange addMapAgeRange(MapAgeRange ageRange);
	
	/**
	 * Removes the map age range.
	 *
	 * @param ageRangeId the age range id
	 */
	public void removeMapAgeRange(Long ageRangeId);
	
	/**
	 * Update map age range.
	 *
	 * @param ageRange the age range
	 */
	public void updateMapAgeRange(MapAgeRange ageRange);
	
	/**
	 * Gets the record revisions.
	 *
	 * @param mapRecordId the map record id
	 * @return the record revisions
	 */
	public List<MapRecord> getMapRecordRevisions(Long mapRecordId);

	/**
	 * Returns the map relations.
	 *
	 * @return the map relations
	 */
	public List<MapRelation> getMapRelations();

	/**
	 * Finds map relations from the specified query.
	 *
	 * @param query the query
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findMapRelations(String query, PfsParameter pfsParameter)
			throws Exception;

	/**
	 * Adds the map relation.
	 *
	 * @param mapRelation the map relation
	 */
	public void addMapRelation(MapRelation mapRelation);

	/**
	 * Update map relation.
	 *
	 * @param mapRelation the map relation
	 */
	public void updateMapRelation(MapRelation mapRelation);

	/**
	 * Removes the map relation.
	 *
	 * @param mapRelationId the map relation id
	 */
	public void removeMapRelation(Long mapRelationId);

	/**
	 * Compute map relation.
	 *
	 * @param mapRecord the map record
	 * @param mapEntry the map entry
	 * @return the map relation
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public MapRelation computeMapRelation(MapEntry mapEntry) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
	
	/**
	 * Compute map advice.
	 *
	 * @param mapRecord the map record
	 * @param mapEntry the map entry
	 * @return the list
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public List<MapAdvice> computeMapAdvice(MapEntry mapEntry) throws InstantiationException, IllegalAccessException, ClassNotFoundException;
	
    /**
     * Gets the transaction per operation.
     *
     * @return the transaction per operation
     * @throws Exception the exception
     */
    public boolean getTransactionPerOperation() throws Exception;
    
    /**
     * Sets the transaction per operation.
     *
     * @param transactionPerOperation the new transaction per operation
     * @throws Exception the exception
     */
    public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;

    /**
     * Begin transaction.
     *
     * @throws Exception the exception
     */
    public void beginTransaction() throws Exception;
    
    /**
     * Commit.
     *
     * @throws Exception the exception
     */
    public void commit() throws Exception;
    
    /**
     * Returns the most recent map record revision.
     *
     * @return the most recent map record revision
     * @throws Exception the exception
     */
    public MapRecord getMostRecentMapRecordRevision(long mapRecordId) throws Exception;

	/**
	 * Gets the project specific algorithm handler.
	 *
	 * @param mapProject the map project
	 * @return the project specific algorithm handler
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ProjectSpecificAlgorithmHandler getProjectSpecificAlgorithmHandler(MapProject mapProject) throws InstantiationException, IllegalAccessException, ClassNotFoundException;


	
	
}
	
	