/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.jpa.services.rest;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.ihtsdo.otf.mapping.helpers.AdditionalMapEntryInfoListJpa;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.AdditionalMapEntryInfoJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * The Interface MappingServiceRest.
 */
public interface MappingServiceRest {

	/**
	   * Returns all map projects in either JSON or XML format.
	   *
	   * @param authToken the auth token
	   * @return the map projects
	   * @throws Exception the exception
	   */
	MapProjectListJpa getMapProjects(String authToken) throws Exception;

	/**
	   * Returns the project for a given id (auto-generated) in JSON format.
	   *
	   * @param mapProjectId the mapProjectId
	   * @param authToken the auth token
	   * @return the mapProject
	   * @throws Exception the exception
	   */
	MapProject getMapProject(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Adds a map project.
	   *
	   * @param mapProject the map project to be added
	   * @param authToken the auth token
	   * @return returns the added map project object
	   * @throws Exception the exception
	   */
	MapProject addMapProject(MapProjectJpa mapProject, String authToken) throws Exception;

	/**
	   * Updates a map project.
	   *
	   * @param mapProject the map project to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapProject(MapProjectJpa mapProject, String authToken) throws Exception;

	/**
	   * Removes a map project.
	   *
	   * @param mapProject the map project
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapProject(MapProjectJpa mapProject, String authToken) throws Exception;

	/**
	 * Clone map project.
	 *
	 * @param mapProject the map project
	 * @param authToken the auth token
	 * @return the map project
	 * @throws Exception the exception
	 */
	MapProject cloneMapProject(MapProjectJpa mapProject, String authToken) throws Exception;

	/**
	   * Returns all map projects for a lucene query.
	   *
	   * @param query the string query
	   * @param authToken the auth token
	   * @return the map projects
	   * @throws Exception the exception
	   */
	SearchResultList findMapProjectsForQuery(String query, String authToken) throws Exception;

	/**
	   * Returns all map projects for a map user.
	   *
	   * @param mapUserName the map user name
	   * @param authToken the auth token
	   * @return the map projects
	   * @throws Exception the exception
	   */
	MapProjectListJpa getMapProjectsForUser(String mapUserName, String authToken) throws Exception;

	/**
	   * Returns all map leads in either JSON or XML format.
	   *
	   * @param authToken the auth token
	   * @return the map leads
	   * @throws Exception the exception
	   */
	MapUserListJpa getMapUsers(String authToken) throws Exception;

	/**
	   * Returns the scope concepts for map project.
	   *
	   * @param projectId the project id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the scope concepts for map project
	   * @throws Exception the exception
	   */
	SearchResultList getScopeConceptsForMapProject(Long projectId, PfsParameterJpa pfsParameter, String authToken)
			throws Exception;

	/**
	   * Adds a list of scope concepts to map project.
	   *
	   * @param terminologyIds the terminology ids
	   * @param projectId the project id
	   * @param authToken the auth token
	   * @return the validation result
	   * @throws Exception the exception
	   */
	ValidationResult addScopeConceptsToMapProject(List<String> terminologyIds, Long projectId, String authToken)
			throws Exception;

	/**
	 * Removes a single scope concept from map project.
	 *
	 * @param terminologyId the terminology id
	 * @param projectId the project id
	 * @param authToken the auth token
	 * @return the validation result
	 * @throws Exception the exception
	 */
	ValidationResult removeScopeConceptFromMapProject(String terminologyId, Long projectId, String authToken)
			throws Exception;

	/**
	 * Removes the scope concept from map project.
	 *
	 * @param terminologyIds the terminology ids
	 * @param projectId the project id
	 * @param authToken the auth token
	 * @return the validation result
	 * @throws Exception the exception
	 */
	ValidationResult removeScopeConceptsFromMapProject(List<String> terminologyIds, Long projectId, String authToken)
			throws Exception;

	/**
	   * Returns the scope excluded concepts for map project.
	   *
	   * @param projectId the project id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the scope excluded concepts for map project
	   * @throws Exception the exception
	   */
	SearchResultList getScopeExcludedConceptsForMapProject(Long projectId, PfsParameterJpa pfsParameter,
			String authToken) throws Exception;

	/**
	 * Adds a list of scope excluded concepts to map project.
	 *
	 * @param terminologyIds the terminology ids
	 * @param projectId the project id
	 * @param authToken the auth token
	 * @return the validation result
	 * @throws Exception the exception
	 */
	ValidationResult addScopeExcludedConceptsToMapProject(List<String> terminologyIds, Long projectId, String authToken)
			throws Exception;

	/**
	 * Removes a single scope excluded concept from map project.
	 *
	 * @param terminologyId the terminology id
	 * @param projectId the project id
	 * @param authToken the auth token
	 * @return the validation result
	 * @throws Exception the exception
	 */
	ValidationResult removeScopeExcludedConceptFromMapProject(String terminologyId, Long projectId, String authToken)
			throws Exception;

	/**
	 * Removes a list of scope excluded concepts from map project.
	 *
	 * @param terminologyIds the terminology ids
	 * @param projectId the project id
	 * @param authToken the auth token
	 * @return the validation result
	 * @throws Exception the exception
	 */
	ValidationResult removeScopeExcludedConceptsFromMapProject(List<String> terminologyIds, Long projectId,
			String authToken) throws Exception;

	/**
	   * Returns the user for a given id (auto-generated) in JSON format.
	   *
	   * @param mapUserName the map user name
	   * @param authToken the auth token
	   * @return the mapUser
	   * @throws Exception the exception
	   */
	MapUser getMapUser(String mapUserName, String authToken) throws Exception;

	/**
	   * Adds a map user.
	   *
	   * @param mapUser the map user
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapUser addMapUser(MapUserJpa mapUser, String authToken) throws Exception;

	/**
	   * Updates a map user.
	   *
	   * @param mapUser the map user to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapUser(MapUserJpa mapUser, String authToken) throws Exception;

	/**
	   * Removes a map user.
	   *
	   * @param mapUser the map user
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapUser(MapUserJpa mapUser, String authToken) throws Exception;

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Advice
	// ///////////////////////////////////////////////////
	/**
	 * Returns all map advices in either JSON or XML format.
	 *
	 * @param authToken the auth token
	 * @return the map advices
	 * @throws Exception the exception
	 */

	MapAdviceListJpa getMapAdvices(String authToken) throws Exception;

	/**
	   * Adds a map advice.
	   *
	   * @param mapAdvice the map advice
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapAdvice addMapAdvice(MapAdviceJpa mapAdvice, String authToken) throws Exception;

	/**
	   * Updates a map advice.
	   *
	   * @param mapAdvice the map advice to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapAdvice(MapAdviceJpa mapAdvice, String authToken) throws Exception;

	/**
	   * Removes a map advice.
	   *
	   * @param mapAdvice the map advice
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapAdvice(MapAdviceJpa mapAdvice, String authToken) throws Exception;

    // ///////////////////////////////////////////////////
    // SCRUD functions: Additional Map Entry Info
    // ///////////////////////////////////////////////////
    /**
     * Returns all additional map entry infos in either JSON or XML format.
     *
     * @param authToken the auth token
     * @return the additional map entry infos
     * @throws Exception the exception
     */

	AdditionalMapEntryInfoListJpa getAdditionalMapEntryInfos(String authToken) throws Exception;

    /**
       * Adds an additional map entry info.
       *
       * @param additionalMapEntryInfo the additional map entry info
       * @param authToken the auth token
       * @return Response the response
       * @throws Exception the exception
       */
	AdditionalMapEntryInfo addAdditionalMapEntryInfo(AdditionalMapEntryInfoJpa additionalMapEntryInfo, String authToken) throws Exception;

    /**
       * Updates an additional map entry info.
       *
       * @param additionalMapEntryInfo the additional map entry info to be added
       * @param authToken the auth token
       * @throws Exception the exception
       */
    void updateAdditionalMapEntryInfo(AdditionalMapEntryInfoJpa additionalMapEntryInfo, String authToken) throws Exception;

    /**
       * Removes an additional map entry info.
       *
       * @param additionalMapEntryInfo the additional map entry info
       * @param authToken the auth token
       * @throws Exception the exception
       */
    void removeAdditionalMapEntryInfo(AdditionalMapEntryInfoJpa additionalMapEntryInfo, String authToken) throws Exception;	
	
	// ///////////////////////////////////////////////////
	// SCRUD functions: Map AgeRange
	// ///////////////////////////////////////////////////
	/**
	 * Returns all map age ranges in either JSON or XML format.
	 *
	 * @param authToken the auth token
	 * @return the map age ranges
	 * @throws Exception the exception
	 */

	MapAgeRangeListJpa getMapAgeRanges(String authToken) throws Exception;

	/**
	   * Adds a map age range.
	   *
	   * @param mapAgeRange the map ageRange
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapAgeRange addMapAgeRange(MapAgeRangeJpa mapAgeRange, String authToken) throws Exception;

	/**
	   * Updates a map age range.
	   *
	   * @param mapAgeRange the map ageRange to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapAgeRange(MapAgeRangeJpa mapAgeRange, String authToken) throws Exception;

	/**
	   * Removes a map age range.
	   *
	   * @param mapAgeRange the map age range
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapAgeRange(MapAgeRangeJpa mapAgeRange, String authToken) throws Exception;

	/**
	   * Returns all map relations in either JSON or XML format.
	   *
	   * @param authToken the auth token
	   * @return the map relations
	   * @throws Exception the exception
	   */
	MapRelationListJpa getMapRelations(String authToken) throws Exception;

	/**
	   * Adds a map relation.
	   *
	   * @param mapRelation the map relation
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapRelation addMapRelation(MapRelationJpa mapRelation, String authToken) throws Exception;

	/**
	   * Updates a map relation.
	   *
	   * @param mapRelation the map relation to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapRelation(MapRelationJpa mapRelation, String authToken) throws Exception;

	/**
	   * Removes a map relation.
	   *
	   * @param mapRelation the map relation
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapRelation(MapRelationJpa mapRelation, String authToken) throws Exception;

	/**
	   * Returns all map principles in either JSON or XML format.
	   *
	   * @param authToken the auth token
	   * @return the map principles
	   * @throws Exception the exception
	   */
	MapPrincipleListJpa getMapPrinciples(String authToken) throws Exception;

	/**
	   * Returns the map principle for id.
	   *
	   * @param mapPrincipleId the map principle id
	   * @param authToken the auth token
	   * @return the map principle for id
	   * @throws Exception the exception
	   */
	MapPrinciple getMapPrinciple(Long mapPrincipleId, String authToken) throws Exception;

	/**
	   * Adds a map user principle object.
	   *
	   * @param mapPrinciple the map user principle object to be added
	   * @param authToken the auth token
	   * @return result the newly created map user principle object
	   * @throws Exception the exception
	   */
	MapPrinciple addMapPrinciple(MapPrincipleJpa mapPrinciple, String authToken) throws Exception;

	/**
	   * Updates a map principle.
	   *
	   * @param mapPrinciple the map principle
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapPrinciple(MapPrincipleJpa mapPrinciple, String authToken) throws Exception;

	/**
	   * Removes a set of map user preferences.
	   *
	   * @param principle the principle
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapPrinciple(MapPrincipleJpa principle, String authToken) throws Exception;

	/**
	   * Gets a map user preferences object for a specified user.
	   *
	   * @param username the username
	   * @param authToken the auth token
	   * @return result the newly created map user preferences object
	   * @throws Exception the exception
	   */
	MapUserPreferences getMapUserPreferences(String username, String authToken) throws Exception;

	/**
	   * Adds a map user preferences object.
	   *
	   * @param mapUserPreferences the map user preferences object to be added
	   * @param authToken the auth token
	   * @return result the newly created map user preferences object
	   * @throws Exception the exception
	   */
	MapUserPreferences addMapUserPreferences(MapUserPreferencesJpa mapUserPreferences, String authToken)
			throws Exception;

	/**
	   * Updates a map user preferences object.
	   *
	   * @param mapUserPreferences the map user preferences
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapUserPreferences(MapUserPreferencesJpa mapUserPreferences, String authToken) throws Exception;

	/**
	   * Removes a set of map user preferences.
	   *
	   * @param mapUserPreferences the id of the map user preferences object to be
	   *          deleted
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeMapUserPreferences(MapUserPreferencesJpa mapUserPreferences, String authToken) throws Exception;

	/**
	   * Returns the record for a given id (auto-generated) in JSON format.
	   *
	   * @param mapRecordId the mapRecordId
	   * @param authToken the auth token
	   * @return the mapRecord
	   * @throws Exception the exception
	   */
	MapRecord getMapRecord(Long mapRecordId, String authToken) throws Exception;

	/**
	   * Adds a map record.
	   *
	   * @param mapRecord the map record to be added
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapRecord addMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Updates a map record.
	   *
	   * @param mapRecord the map record to be added
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void updateMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Removes a map record given the object.
	   *
	   * @param mapRecord the map record to delete
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	Response removeMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Removes a set of map records for a project and a set of terminology ids.
	   *
	   * @param terminologyIds the terminology ids
	   * @param projectId the project id
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(List<String> terminologyIds, Long projectId,
			String authToken) throws Exception;

	/**
	   * Returns the records for a given concept id. We don't need to know
	   * terminology or version here because we can get it from the corresponding
	   * map project.
	   *
	   * @param conceptId the concept id
	   * @param authToken the auth token
	   * @return the mapRecords
	   * @throws Exception the exception
	   */
	MapRecordListJpa getMapRecordsForConceptId(String conceptId, String authToken) throws Exception;

	/**
	   * Gets the latest map record revision for each map record with given concept
	   * id.
	   *
	   * @param conceptId the concept id
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the map records for concept id historical
	   * @throws Exception the exception
	   */
	MapRecordListJpa getMapRecordsForConceptIdHistorical(String conceptId, Long mapProjectId, String authToken)
			throws Exception;
	
	/**
   * Gets the users involved with a concept
   * id.
   *
   * @param conceptId the concept id
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the map users for concept id historical
   * @throws Exception the exception
   */
	MapUserListJpa getMapRecordsForConceptIdHistoricalMapUsers(String conceptId, Long mapProjectId, String authToken)
    throws Exception;

	/**
	 * Returns delimited page of Published or Ready For Publication MapRecords
	 * given a paging/filtering/sorting parameters object.
	 *
	 * @param mapProjectId the map project id
	 * @param pfsParameter the JSON object containing the paging/filtering/sorting
	 *          parameters
	 * @param ancestorId the ancestor id
	 * @param relationshipName the relationship name
	 * @param relationshipValue the relationship value
     * @param includeNonPublished the exclude non published
	 * @param excludeDescendants the exclude descendants
	 * @param query the query
	 * @param authToken the auth token
	 * @return the list of map records
	 * @throws Exception the exception
	 */
	MapRecordListJpa getMapRecordsForMapProjectAndQuery(Long mapProjectId, PfsParameterJpa pfsParameter,
			String ancestorId, String relationshipName, String relationshipValue, boolean includeNonPublished, boolean excludeDescendants, String query, String authToken)
			throws Exception;

	/**
	   * Returns delimited page of Published MapRecords
	   * given a paging/filtering/sorting parameters object.
	   *
	   * @param mapProjectId the map project id
	   * @param pfsParameter the JSON object containing the paging/filtering/sorting
	   *          parameters
	   * @param authToken the auth token
	   * @return the list of map records
	   * @throws Exception the exception
	   */
	MapRecordListJpa getPublishedMapRecordsForMapProject(Long mapProjectId, PfsParameterJpa pfsParameter,
			String authToken) throws Exception;

    /**
     * Returns delimited page of Published or Ready For Publication MapRecords
     * given a paging/filtering/sorting parameters object.
     *
     * @param mapProjectId the map project id
     * @param pfsParameter the JSON object containing the paging/filtering/sorting
     *          parameters
     * @param authToken the auth token
     * @return the list of map records
     * @throws Exception the exception
     */
  MapRecordListJpa getPublishedAndReadyForPublicationMapRecordsForMapProject(Long mapProjectId, PfsParameterJpa pfsParameter,
          String authToken) throws Exception;	
	
	/**
	   * Returns the map record revisions.
	   * 
	   * NOTE: currently not called, but we are going to want to call this to do
	   * history-related stuff thus it is anticipating the future dev and should be
	   * kept.
	   *
	   * @param mapRecordId the map record id
	   * @param authToken the auth token
	   * @return the map record revisions
	   * @throws Exception the exception
	   */
	MapRecordList getMapRecordRevisions(Long mapRecordId, String authToken) throws Exception;

	/**
	   * Returns the map record using historical revisions if the record no longer
	   * exists.
	   *
	   * @param mapRecordId the map record id
	   * @param authToken the auth token
	   * @return the map record historical
	   * @throws Exception the exception
	   */
	MapRecord getMapRecordHistorical(Long mapRecordId, String authToken) throws Exception;

	/**
	   * Computes a map relation (if any) for a map entry's current state.
	   *
	   * @param mapRecord the map record
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	MapRelation computeMapRelation(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	 * Computes a map advice (if any) for a map entry's current state.
	 *
	 * @param entryIndex the entry index
	 * @param mapRecord the map record
	 * @param authToken the auth token
	 * @return Response the response
	 * @throws Exception the exception
	 */
	MapAdviceList computeMapAdvice(Integer entryIndex, MapRecordJpa mapRecord, String authToken) throws Exception;


    /**
     * Returns all unique tags for map project.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the tags for map project
     * @throws Exception the exception
     */
    SearchResultList getTagsForMapProject(Long mapProjectId, String authToken)
          throws Exception;	

    /**
     * Returns whether map project is set to clear data on map target change.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the Boolean clear data on change
     * @throws Exception the exception
     */
    Boolean getClearDataOnChange(Long mapProjectId, String authToken)
          throws Exception;     
    
	// ///////////////////////////////////////////////
	// Role Management Services
	// ///////////////////////////////////////////////
	/**
	 * Gets a map user's role for a given map project.
	 *
	 * @param username the username
	 * @param mapProjectId the map project id
	 * @param authToken the auth token
	 * @return result the role
	 * @throws Exception the exception
	 */

	MapUserRole getMapUserRoleForMapProject(String username, Long mapProjectId, String authToken) throws Exception;

	/**
	   * 
	   * Given concept information, returns a ConceptList of descendant concepts
	   * without associated map records.
	   *
	   * @param terminologyId the concept terminology id
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the ConceptList of unmapped descendants
	   * @throws Exception the exception
	   */
	SearchResultList getUnmappedDescendantsForConcept(String terminologyId, Long mapProjectId, String authToken)
			throws Exception;

	// /////////////////////////////////////////////////////
	// Tree Position Routines for Terminology Browser based on destination terminology
	// /////////////////////////////////////////////////////
	/**
	 * Gets tree positions for concept based on destination terminology.
	 *
	 * @param terminologyId the terminology id
	 * @param mapProjectId the contextual project of this tree, used for
	 *          determining valid codes
	 * @param authToken the auth token
	 * @return the search result list
	 * @throws Exception the exception
	 */

	TreePositionList getTreePositionWithDescendantsForConceptAndMapProject(String terminologyId, Long mapProjectId,
			String authToken

	) throws Exception;
	
	// /////////////////////////////////////////////////////
	// Tree Position Routines for Terminology Browser base on source terminology
	// /////////////////////////////////////////////////////
	/**
	 * Gets tree positions for concept base on source terminology.
	 *
	 * @param terminologyId the terminology id
	 * @param mapProjectId the contextual project of this tree, used for
	 *          determining valid codes
	 * @param authToken the auth token
	 * @return the search result list
	 * @throws Exception the exception
	 */

	TreePositionList getSourceTreePositionWithDescendantsForConceptAndMapProject(String terminologyId, Long mapProjectId,
			String authToken

	) throws Exception;

	/**
	   * Gets the root-level tree positions for a given terminology and version.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	TreePositionList getDestinationRootTreePositionsForMapProject(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Gets the root-level tree positions for a given terminology and version.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	TreePositionList getSourceRootTreePositionsForMapProject(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Gets tree positions for concept query.
	   *
	   * @param query the query
	   * @param mapProjectId the map project id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the root-level trees corresponding to the query
	   * @throws Exception the exception
	   */
	TreePositionList getTreePositionGraphsForQueryAndMapProject(String query, Long mapProjectId,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;
	
	/**
	   * Gets tree positions for source concept query.
	   *
	   * @param query the query
	   * @param mapProjectId the map project id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the root-level trees corresponding to the query
	   * @throws Exception the exception
	   */
	TreePositionList getSourceTreePositionGraphsForQueryAndMapProject(String query, Long mapProjectId,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Returns records recently edited for a project and user. Used by editedList
	   * widget.
	   *
	   * @param mapProjectId the map project id
	   * @param username the user name
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the recently edited map records
	   * @throws Exception the exception
	   */
	MapRecordListJpa getMapRecordsEditedByMapUser(Long mapProjectId, String username, PfsParameterJpa pfsParameter,
			String authToken) throws Exception;

	/**
	   * Returns the map records that when compared lead to the specified conflict
	   * record.
	   * 
	   * @param mapRecordId the map record id
	   * @param authToken the auth token
	   * @return map records in conflict for a given conflict lead record
	   * @throws Exception the exception
	   */
	MapRecordList getOriginMapRecordsForConflict(Long mapRecordId, String authToken) throws Exception;

	/**
	   * Validates a map record.
	   *
	   * @param mapRecord the map record to be validated
	   * @param authToken the auth token
	   * @return Response the response
	   * @throws Exception the exception
	   */
	ValidationResult validateMapRecord(MapRecordJpa mapRecord, String authToken) throws Exception;

	/**
	   * Compare map records and return differences.
	   *
	   * @param mapRecordId1 the map record id1
	   * @param mapRecordId2 the map record id2
	   * @param authToken the auth token
	   * @return the validation result
	   * @throws Exception the exception
	   */
	ValidationResult compareMapRecords(Long mapRecordId1, Long mapRecordId2, String authToken) throws Exception;

	/**
	   * Is target code valid.
	   *
	   * @param mapProjectId the map project id
	   * @param terminologyId the terminology id
	   * @param authToken the auth token
	   * @return the concept
	   * @throws Exception the exception
	   */
	Concept isTargetCodeValid(Long mapProjectId, String terminologyId, String authToken) throws Exception;

	/**
	   * Upload file.
	   *
	   * @param fileInputStream the file input stream
	   * @param contentDispositionHeader the content disposition header
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the response
	   * @throws Exception the exception
	   */
	String uploadMappingHandbookFile(InputStream fileInputStream, FormDataContentDisposition contentDispositionHeader,
			Long mapProjectId, String authToken) throws Exception;

	/**
	   * Returns all map projects metadata.
	   *
	   * @param authToken the auth token
	   * @return the map projects metadata
	   * @throws Exception the exception
	   */
	KeyValuePairLists getMapProjectMetadata(String authToken) throws Exception;

	/**
	   * Returns the all terminology notes. e.g. dagger/asterisk mappings for ICD10
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the all terminology notes
	   * @throws Exception the exception
	   */
	KeyValuePairList getAllTerminologyNotes(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Compute default preferred names.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void computeDefaultPreferredNames(Long mapProjectId, String authToken) throws Exception;

	/**
	 * Begin release for map project.
	 *
	 * @param effectiveTime the effective time
	 * @param mapProjectId the map project id
	 * @param authToken the auth token
	 * @return the string
	 * @throws Exception the exception
	 */
	String beginReleaseForMapProject(String effectiveTime, Long mapProjectId, String authToken) throws Exception;

	/**
	 * Process release for map project.
	 *
	 * @param moduleId the module id
	 * @param effectiveTime the effective time
	 * @param mapProjectId the map project id
	 * @param writeDelta the write delta
	 * @param authToken the auth token
	 * @return the string
	 * @throws Exception the exception
	 */
	String processReleaseForMapProject(String moduleId, String effectiveTime, Long mapProjectId, 
	  boolean writeDelta, String authToken)
			throws Exception;

	/**
	 * Finish release for map project.
	 *
	 * @param testModeFlag the test mode flag
	 * @param mapProjectId the map project id
	 * @param effectiveTime the effective time
	 * @param authToken the auth token
	 * @return the string
	 * @throws Exception the exception
	 */
	String finishReleaseForMapProject(boolean testModeFlag, Long mapProjectId, String effectiveTime, String authToken)
			throws Exception;

	/**
	   * Start editing cycle for map project.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void startEditingCycleForMapProject(Long mapProjectId, String authToken) throws Exception;

    /**
     * Gets the log.
     *
     * @param projectId the project id
     * @param logTypes the log types
     * @param query the query
     * @param authToken the auth token
     * @return the log
     * @throws Exception the exception
     */
    String getLog(String projectId, List<String> logTypes, String query,
      String authToken) throws Exception;

    /**
     * Returns the release file names.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the release file names
     * @throws Exception the exception
     */
    String getReleaseFileNames(Long mapProjectId, String authToken)
      throws Exception; 	
	
	/**
	 * Creates the jira issue.
	 *
	 * @param conceptId the concept id
	 * @param conceptAuthor the concept author
	 * @param messageText the message text
	 * @param mapRecord the map record
	 * @param authToken the auth token
	 * @throws Exception the exception
	 */
	void createJiraIssue(String conceptId, String conceptAuthor, String messageText, MapRecordJpa mapRecord,
			String authToken) throws Exception;

	/**
	 * Gets the concept authors.
	 *
	 * @param conceptId the concept id
	 * @param authToken the auth token
	 * @return the concept authors
	 * @throws Exception the exception
	 */
	SearchResultList getConceptAuthors(String conceptId, String authToken) throws Exception;

	/**
	 * Gets the concept authoring changes.
	 *
	 * @param projectId the project id
	 * @param conceptId the concept id
	 * @param authToken the auth token
	 * @return the concept authoring changes
	 * @throws Exception the exception
	 */
	SearchResultList getConceptAuthoringChanges(String projectId, String conceptId, String authToken) throws Exception;

    /**
     * Gets the release report list.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the release report list
     * @throws Exception the exception
     */
    SearchResultList getReleaseReportList(Long mapProjectId, String authToken)
    throws Exception;

   
    /**
     * Gets the files from amazon s3.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the files from amazon s3
     * @throws Exception the exception
     */
    SearchResultList getFileListFromAmazonS3(Long mapProjectId,
      String authToken) throws Exception;

    /**
     * Gets the current release file name.
     *
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the current release files
     * @throws Exception the exception
     */
    SearchResultList getCurrentReleaseFileName(Long mapProjectId, String authToken)
      throws Exception;

    /**
     * Download file from amazon s3.
     *
     * @param filePath the file path
     * @param authToken the auth token
     * @return the input stream
     * @throws Exception the exception
     */
    InputStream downloadFileFromAmazonS3(String filePath, String authToken)
      throws Exception;

    /**
     * Download current release file.
     *
     * @param fileName the file name
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the input stream
     * @throws Exception the exception
     */
    InputStream downloadCurrentReleaseFile(String fileName, Long mapProjectId,
      String authToken) throws Exception;

    /**
     * Returns the map user role for application.
     *
     * @param username the username
     * @param authToken the auth token
     * @return the map user role for application
     * @throws Exception the exception
     */
    MapUserRole getMapUserRoleForApplication(String username, String authToken)
      throws Exception;

    /**
     * Update map project version.
     *
     * @param version the version
     * @param mapProjectId the map project id
     * @param authToken the auth token
     * @return the string
     * @throws Exception the exception
     */
    String updateMapProjectVersion(String version, Long mapProjectId, String authToken)
      throws Exception;

 
}