/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.services;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.AdditionalMapEntryInfoList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeList;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleList;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRelationList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;

/**
 * Generically represents a service for interacting with mapping objects.
 */
public interface MappingService extends RootService {

  /**
   * Closes the manager associated with service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception;

  // ////////////////////////////
  // Basic retrieval services //
  // ////////////////////////////

  /**
   * Return map project for auto-generated id.
   * 
   * @param id the auto-generated id
   * @return the MapProject
   * @throws Exception the exception
   */
  public MapProject getMapProject(Long id) throws Exception;

  /**
   * Gets the map project by ref set id.
   * 
   * @param refSetId the ref set id
   * @return the map project by ref set id
   * @throws Exception the exception
   */
  public MapProject getMapProjectForRefSetId(String refSetId) throws Exception;

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
   * Gets the map principle.
   * 
   * @param id the id
   * @return the map principle
   * @throws Exception the exception
   */
  public MapPrinciple getMapPrinciple(Long id) throws Exception;

  /**
   * Returns all map projects.
   * 
   * @return a List of MapProjects
   * @throws Exception the exception
   */
  public MapProjectList getMapProjects() throws Exception;

  /**
   * Retrieve all map users.
   * 
   * @return a List of MapUsers
   * @throws Exception the exception
   */
  public MapUserList getMapUsers() throws Exception;

  /**
   * Returns the map users for team.
   *
   * @param team the team
   * @return the map users for team
   * @throws Exception the exception
   */
  public MapUserList getMapUsersForTeam(String team) throws Exception;

  /**
   * Retrieve all map principles.
   * 
   * @return a List of MapPrinciples
   * @throws Exception the exception
   */
  public MapPrincipleList getMapPrinciples() throws Exception;

  /**
   * Retrieve all map advices.
   * 
   * @return a List of MapAdvices
   * @throws Exception the exception
   */
  public MapAdviceList getMapAdvices() throws Exception;

  /**
   * Retrieve all additional map entry infos.
   * 
   * @return a List of AdditionalMapEntryInfos
   * @throws Exception the exception
   */
  public AdditionalMapEntryInfoList getAdditionalMapEntryInfos() throws Exception;  
  
  /**
   * Retrieve all map projects assigned to a particular map user.
   * 
   * @param mapUser the map user
   * @return a List of MapProjects
   * @throws Exception the exception
   */
  public MapProjectList getMapProjectsForMapUser(MapUser mapUser) throws Exception;

  /**
   * Retrieve all map records.
   * 
   * @return a List of MapRecords
   * @throws Exception the exception
   */
  public MapRecordList getMapRecords() throws Exception;

  /**
   * Retrieve all map records associated with a given concept id.
   * 
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return a List of MapRecords
   * @throws Exception the exception
   */

  // //////////////////
  // Query services //
  // //////////////////

  /**
   * Query for MapProjects
   * 
   * @param query the query
   * @param pfsParameter the paging, filtering, sorting parameter
   * @return the list of MapProjects
   * @throws Exception
   */
  public SearchResultList findMapProjectsForQuery(String query, PfsParameter pfsParameter)
    throws Exception;

  /**
   * Query for MapRecords.
   * 
   * @param query the query
   * @param pfsParameter the paging, filtering, sorting parameter
   * @return the List of MapRecords
   * @throws Exception the exception
   */
  public SearchResultList findMapRecordsForQuery(String query, PfsParameter pfsParameter)
    throws Exception;

  // //////////////////////////
  // Addition services ///
  // //////////////////////////

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
   * Adds the map principle.
   * 
   * @param mapPrinciple the map principle
   * @return the map principle
   * @throws Exception the exception
   */
  public MapPrinciple addMapPrinciple(MapPrinciple mapPrinciple) throws Exception;

  /**
   * Adds the map advice.
   * 
   * @param mapAdvice the map advice
   * @return the map advice
   * @throws Exception the exception
   */
  public MapAdvice addMapAdvice(MapAdvice mapAdvice) throws Exception;

  /**
   * Adds the additional map entry info.
   * 
   * @param additionalMapEntryInfo the additional map entry info
   * @return the additional map entry info
   * @throws Exception the exception
   */
  public AdditionalMapEntryInfo addAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo) throws Exception;  
  
  // //////////////////////////
  // Update services ///
  // //////////////////////////

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
   * @throws Exception the exception
   */
  public void updateMapRecord(MapRecord mapRecord) throws Exception;

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

  /**
   * Update additional map entry info.
   * 
   * @param additionalMapEntryInfo the additional map entry info
   * @throws Exception the exception
   */
  public void updateAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo) throws Exception;  
  
  // //////////////////////////
  // Removal services ///
  // //////////////////////////

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
  
  /**
   * Removes the additional map entry info.
   * 
   * @param additionalMapEntryInfoId the map advice id
   * @throws Exception the exception
   */
  public void removeAdditionalMapEntryInfo(Long additionalMapEntryInfoId) throws Exception;

  // /////////////////////////
  // Other services ///
  // /////////////////////////

  /**
   * Gets the map records for concept id. Only used by MapNote
   * 
   * @param terminologyId the concept id
   * @return the map records for concept id
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordsForConcept(String terminologyId) throws Exception;

  /**
   * Returns the unmapped descendants for concept.
   *
   * @param terminologyId the terminology id
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findUnmappedDescendantsForConcept(String terminologyId, Long mapProjectId,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Creates the map records for map project.
   * 
   * @param mapProjectId the map project id
   * @param workflowStatus the workflow status
   * @throws Exception the exception
   */
  public void createMapRecordsForMapProject(Long mapProjectId, WorkflowStatus workflowStatus)
    throws Exception;

  /**
   * Creates the map records for map project.
   *
   * @param mapProjectId the map project id
   * @param mapUser the map user
   * @param complexMapRefSetMembers the complex map ref set members
   * @param workflowStatus the workflow status
   * @throws Exception the exception
   */
  public void createMapRecordsForMapProject(Long mapProjectId, MapUser mapUser,
    List<ComplexMapRefSetMember> complexMapRefSetMembers, WorkflowStatus workflowStatus)
    throws Exception;

  /**
   * Append entries to map records for project.
   *
   * @param mapProjectId the map project id
   * @param mapUser the map user
   * @param complexMapRefSetMembers the complex map ref set members
   * @param workflowStatus the workflow status
   * @throws Exception the exception
   */
  public void appendEntriesToMapRecordsForProject(Long mapProjectId, MapUser mapUser,
    List<ComplexMapRefSetMember> complexMapRefSetMembers, WorkflowStatus workflowStatus)
    throws Exception;

  /**
   * Removes the map records for project id.
   * 
   * @param mapProjectId the map project id
   * @return the long
   * @throws Exception the exception
   */
  public Long removeMapRecordsForMapProject(Long mapProjectId) throws Exception;

  /**
   * Helper function not requiring a PFS object.
   * 
   * @param mapProjectId the id of the map project
   * @return the map records for a project id
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordsForMapProject(Long mapProjectId) throws Exception;

  /**
   * Find concepts in scope.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findConceptsInScope(Long mapProjectId, PfsParameter pfsParameter)
    throws Exception;

  /**
   * Find unmapped concepts in scope.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findUnmappedConceptsInScope(Long mapProjectId, PfsParameter pfsParameter)
    throws Exception;

  /**
   * Find mapped concepts out of scope bounds.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findMappedConceptsOutOfScopeBounds(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Find concepts excluded from scope.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findConceptsExcludedFromScope(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Indicates whether or not concept in scope is the case.
   * 
   * @param terminologyId the concept id
   * @param mapProjectId the map project id
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isConceptInScope(String terminologyId, Long mapProjectId) throws Exception;

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
   * @throws Exception the exception
   */
  public MapUser getMapUser(String userName) throws Exception;

  /**
   * Returns the map age ranges.
   * 
   * @return the map age ranges
   * @throws Exception the exception
   */
  public MapAgeRangeList getMapAgeRanges() throws Exception;

  /**
   * Adds the map age range.
   * 
   * @param ageRange the age range
   * @return the map age range
   * @throws Exception the exception
   */
  public MapAgeRange addMapAgeRange(MapAgeRange ageRange) throws Exception;

  /**
   * Removes the map age range.
   * 
   * @param ageRangeId the age range id
   * @throws Exception the exception
   */
  public void removeMapAgeRange(Long ageRangeId) throws Exception;

  /**
   * Update map age range.
   * 
   * @param ageRange the age range
   * @throws Exception the exception
   */
  public void updateMapAgeRange(MapAgeRange ageRange) throws Exception;

  /**
   * Gets the record revisions.
   * 
   * @param mapRecordId the map record id
   * @return the record revisions
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordRevisions(Long mapRecordId) throws Exception;

  /**
   * Returns the map relations.
   * 
   * @return the map relations
   * @throws Exception the exception
   */
  public MapRelationList getMapRelations() throws Exception;

  /**
   * Adds the map relation.
   * 
   * @param mapRelation the map relation
   * @return the map relation
   * @throws Exception the exception
   */
  public MapRelation addMapRelation(MapRelation mapRelation) throws Exception;

  /**
   * Update map relation.
   * 
   * @param mapRelation the map relation
   * @throws Exception the exception
   */
  public void updateMapRelation(MapRelation mapRelation) throws Exception;

  /**
   * Removes the map relation.
   * 
   * @param mapRelationId the map relation id
   * @throws Exception the exception
   */
  public void removeMapRelation(Long mapRelationId) throws Exception;

  /**
   * Gets the transaction per operation.
   * 
   * @return the transaction per operation
   * @throws Exception the exception
   */
  @Override
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   * 
   * @param transactionPerOperation the new transaction per operation
   * @throws Exception the exception
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;

  /**
   * Begin transaction.
   * 
   * @throws Exception the exception
   */
  @Override
  public void beginTransaction() throws Exception;

  /**
   * Commit.
   * 
   * @throws Exception the exception
   */
  @Override
  public void commit() throws Exception;

  /**
   * Returns the most recent map record revision.
   * 
   * @param projectId the project id
   * @param userName the user name
   * @param pfsParameter the pfs parameter
   * @return the most recent map record revision
   * @throws Exception the exception
   */
  public MapRecordList getRecentlyEditedMapRecords(Long projectId, String userName,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Gets the project specific algorithm handler.
   *
   * @param mapProject the map project
   * @return the project specific algorithm handler
   * @throws InstantiationException the instantiation exception
   * @throws IllegalAccessException the illegal access exception
   * @throws ClassNotFoundException the class not found exception
   * @throws NoSuchMethodException the no such method exception
   * @throws InvocationTargetException the invocation target exception
   */
  public ProjectSpecificAlgorithmHandler getProjectSpecificAlgorithmHandler(MapProject mapProject)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException,
    NoSuchMethodException, InvocationTargetException;

  /**
   * Gets the map user preferences.
   * 
   * @return the map user preferences
   * @throws Exception the exception
   */
  public MapUserPreferencesList getMapUserPreferences() throws Exception;

  /**
   * Adds the map user preferences.
   * 
   * @param mapUserPreferences the map user preferences
   * @return the map user preferences
   * @throws Exception the exception
   */
  public MapUserPreferences addMapUserPreferences(MapUserPreferences mapUserPreferences)
    throws Exception;

  /**
   * Update map user preferences.
   * 
   * @param mapUserPreferences the map user preferences
   * @throws Exception the exception
   */
  public void updateMapUserPreferences(MapUserPreferences mapUserPreferences) throws Exception;

  /**
   * Removes the map user preferences.
   * 
   * @param mapUserPreferencesId the map user preferences id
   * @throws Exception the exception
   */
  public void removeMapUserPreferences(Long mapUserPreferencesId) throws Exception;

  /**
   * Given a list of tree positions and a map project id, sets the valid codes
   * for each node.
   *
   * @param mapProject the map project
   * @param treePositions the tree positions
   * @param handler the handler
   * @return the list
   * @throws Exception the exception
   */
  public TreePositionList setTreePositionValidCodes(MapProject mapProject,
    TreePositionList treePositions, ProjectSpecificAlgorithmHandler handler) throws Exception;

  /**
   * Computes any display notes for tree position, depending on project
   * algorithm handler.
   *
   * @param mapProject the map project
   * @param treePositions the tree positions
   * @param handler the handler
   * @return the tree position list
   * @throws Exception the exception
   */
  public TreePositionList setTreePositionTerminologyNotes(MapProject mapProject,
    TreePositionList treePositions, ProjectSpecificAlgorithmHandler handler) throws Exception;

  /**
   * Gets the map user preferences.
   * 
   * @param userName the user name
   * @return the map user preferences
   * @throws Exception the exception
   */
  public MapUserPreferences getMapUserPreferences(String userName) throws Exception;

  /**
   * Gets the published and ready for publication map records for map project.
   * 
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the published and ready for publication map records for map project
   * @throws Exception the exception
   */
  public MapRecordList getPublishedAndReadyForPublicationMapRecordsForMapProject(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Gets the published map records for map project.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the published map records for map project
   * @throws Exception the exception
   */
  public MapRecordList getPublishedMapRecordsForMapProject(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Returns the tags for map project.
   *
   * @param mapProjectId the map project id
   * @return the tags for map project
   * @throws Exception the exception
   */
  public SearchResultList getTagsForMapProject(Long mapProjectId) throws Exception;

  /**
   * Returns the clear data on change for map project.
   *
   * @param mapProjectId the map project id
   * @return the clear data on change boolean for map project
   * @throws Exception the exception
   */
  public Boolean getClearDataOnChangeForMapProject(Long mapProjectId) throws Exception;
  
  /**
   * Returns the map user role.
   * 
   * @param userName the user name
   * @param mapProjectId the map project id
   * @return the map user role
   * @throws Exception the exception
   */
  public MapUserRole getMapUserRoleForMapProject(String userName, Long mapProjectId)
    throws Exception;

  /**
   * Check map groups for map project.
   * 
   * @param mapProject the map project
   * @param updateRecords whether to update records or simply check for map
   *          group errors
   * @throws Exception the exception
   */
  public void checkMapGroupsForMapProject(MapProject mapProject, boolean updateRecords)
    throws Exception;

  /**
   * Creates the map records for map project.
   *
   * @param mapProjectId the map project id
   * @param mapUser the map user
   * @param complexMapRefSetMembers the complex map ref set members
   * @param workflowStatus the workflow status
   * @param samplingRate the sampling rate
   * @throws Exception the exception
   */
  void createMapRecordsForMapProject(Long mapProjectId, MapUser mapUser,
    List<ComplexMapRefSetMember> complexMapRefSetMembers, WorkflowStatus workflowStatus,
    float samplingRate) throws Exception;

  /**
   * Simple routine to removes a map advice from the environment.
   *
   * @param mapAdvice the map advice name
   * @throws Exception the exception
   */
  public void removeMapAdviceFromEnvironment(MapAdvice mapAdvice) throws Exception;

  /**
   * Recalculate map advice for project.
   *
   * @param mapProject the map project
   * @throws Exception the exception
   */
  void recalculateMapAdviceForProject(MapProject mapProject) throws Exception;

  /**
   * Gets the map project metadata.
   *
   * @return the map project metadata
   * @throws Exception the exception
   */
  public Map<String, Map<String, String>> getMapProjectMetadata() throws Exception;

  /**
   * Gets the map records for a given project and concept.
   *
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @return the map records for project and concept
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordsForProjectAndConcept(Long mapProjectId, String terminologyId)
    throws Exception;

  /**
   * Gets the latest map record revision for each map record for a given
   * concept.
   *
   * @param conceptId the concept id
   * @param mapProjectId the map project id
   * @return the latest map record revisions for concept
   * @throws Exception the exception
   */
  public MapRecordList getMapRecordRevisionsForConcept(String conceptId, Long mapProjectId)
    throws Exception;

  /**
   * Gets the scope excluded concepts for map project.
   *
   * @param mapProject the map project
   * @param pfsParameter the pfs parameter
   * @return the scope excluded concepts for map project
   * @throws Exception the exception
   */
  public SearchResultList getScopeExcludedConceptsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Gets the scope concepts for map project.
   *
   * @param mapProject the map project
   * @param pfsParameter the pfs parameter
   * @return the scope concepts for map project
   * @throws Exception the exception
   */
  public SearchResultList getScopeConceptsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Find map records.
   *
   * @param mapProjectd the map projectd
   * @param ancestorId the ancestor id
   * @param excludeDescendants the exclude descendants
   * @param relationshipName the relationship name
   * @param relationshipValue the relationship value
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param pfsParameter the pfs parameter
   * @param mapConcepts the map concepts
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findMapRecords(Long mapProjectd, String ancestorId,
    boolean excludeDescendants, String relationshipName, String relationshipValue,
    String terminology, String terminologyVersion, PfsParameter pfsParameter,
    Collection<String> mapConcepts) throws Exception;

  /**
   * Retrieve latest map record for a given terminology id.
   *
   * @param mapProjectId the concept id
   * @param terminologyId the concept id
   * @return the list of map records
   * @throws Exception the exception
   */
  public MapRecord getLatestMapRecordForConcept(Long mapProjectId, String terminologyId)
    throws Exception;

  /**
   * Returns the release file names.
   *
   * @param mapProject the map project
   * @return the release file names
   * @throws Exception the exception
   */
  public String getReleaseFileNames(MapProject mapProject) throws Exception;

  /**
   * Returns the map user role for application.
   *
   * @param userName the user name
   * @return the map user role for application
   * @throws Exception the exception
   */
  public MapUserRole getMapUserRoleForApplication(String userName) throws Exception;

  /**
   * Return a list of target ids for maps which are Ready for
   * Publication/In revision/Published
   *
   * @param mapProjectId the map project id
   * @return list of map record ids for the given map project
   * @throws Exception the exception
   */
  public Set<String> getTargetCodeForReadyForPublication(Long mapProjectId)
    throws Exception;
}
