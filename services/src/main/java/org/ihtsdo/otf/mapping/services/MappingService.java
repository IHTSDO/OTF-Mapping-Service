package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * Interface for services to retrieve (get) map objects
 * @author Patrick
 */
public interface MappingService {
	
	
	/**
	 * Closes the manager associated with service
	 * @exception Exception the exception
	 */
	public void close() throws Exception;

	//////////////////////////////
	// Basic retrieval services //
	//////////////////////////////
	
	/**
	 * Return map project for auto-generated id
	 * @param id the auto-generated id
	 * @return the MapProject
	 */
	public MapProject getMapProject(Long id) throws Exception;
	
	/**
	 * Return map specialist for auto-generated id
	 * @param id the auto-generated id
	 * @return the MapLead
	 */
	public MapLead getMapLead(Long id) throws Exception;
	
	/**
	 * Return map lead for auto-generated id
	 * @param id the auto-generated id
	 * @return the mapSpecialist
	 */
	public MapSpecialist getMapSpecialist(Long id) throws Exception;
	
	/**
	 * Return map record for auto-generated id
	 * @param id the auto-generated id
	 * @return the mapRecord
	 */
	public MapRecord getMapRecord(Long id) throws Exception;
	
	/**
	 * Returns all map projects
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjects() throws Exception;
	
	/**
	 * Retrieve all map specialists
	 * @return a List of MapSpecialists
	 */
	public List<MapSpecialist> getMapSpecialists() throws Exception;
	
	/**
	 * Retrieve all map leads
	 * @return a List of MapLeads
	 */
	public List<MapLead> getMapLeads() throws Exception;
	
	/**
	 * Retrieve all map principles
	 * @return a List of MapPrinciples
	 */
	public List<MapPrinciple> getMapPrinciples() throws Exception;
	
	/**
	 * Retrieve all map advices
	 * @return a List of MapAdvices
	 */
	public List<MapAdvice> getMapAdvices() throws Exception;
	
	/**
	 * Retrieve all map projects assigned to a particular map specialist
	 * @param mapSpecialist the map specialist
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjectsForMapSpecialist(MapSpecialist mapSpecialist) throws Exception;
	
	/**
	 * Retrieve all map projects assigned to a particular map lead
	 * @param mapLead the map lead
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjectsForMapLead(MapLead mapLead) throws Exception;
	
	/**
	 * Retrieve all map records
	 * @return a List of MapRecords
	 */
	public List<MapRecord> getMapRecords() throws Exception;
	
	/**
	 * Retrieve all map records associated with a given concept id
	 * @return a List of MapRecords
	 */
	
	////////////////////
	// Query services //
	////////////////////
	
	/**
	 * Query for MapProjects
	 * @param query the query
	 * @return the list of MapProject
	 */
	public SearchResultList findMapProjects(String query, PfsParameter pfsParameter) throws Exception;
	
	/** 
	 * Query for MapSpecialists
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapSpecialists(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapLeads
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapLeads(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapAdvices
	 * @param query the query
	 * @return the List of MapAdvices
	 */
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapRecords
	 * @param query the query
	 * @return the List of MapRecords
	 */
	public SearchResultList findMapRecords(String query, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Query for MapEntrys
	 * @param query the query
	 * @return the List of MapEntrys
	 */
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter) throws Exception;

	////////////////////////////
	// Addition services     ///
    ////////////////////////////
	
	public void addMapSpecialist(MapSpecialist mapSpecialist) throws Exception;
	
	public void addMapLead(MapLead mapLead) throws Exception;
	
	public MapProject addMapProject(MapProject mapProject) throws Exception;
	
	public void addMapRecord(MapRecord mapRecord) throws Exception;
	
	public void addMapEntry(MapEntry mapEntry) throws Exception;
	
	public void addMapPrinciple(MapPrinciple mapPrinciple) throws Exception;
	
	public void addMapAdvice(MapAdvice mapAdvice) throws Exception;
	
	////////////////////////////
	// Update services     ///
	////////////////////////////
	
	public void updateMapSpecialist(MapSpecialist mapSpecialist) throws Exception;
	
	public void updateMapLead(MapLead mapLead) throws Exception;
	
	public void updateMapProject(MapProject mapProject) throws Exception;
	
	public void updateMapRecord(MapRecord mapRecord) throws Exception;
	
	public void updateMapEntry(MapEntry mapEntry) throws Exception;
	
	public void updateMapPrinciple(MapPrinciple mapPrinciple) throws Exception;
	
	public void updateMapAdvice(MapAdvice mapAdvice) throws Exception;
	
	////////////////////////////
	//Removal services     ///
	////////////////////////////
	
	public void removeMapSpecialist(Long mapSpecialistId) throws Exception;
	
	public void removeMapLead(Long mapLeadId) throws Exception;
	
	public void removeMapProject(Long mapProjectId) throws Exception;
	
	public void removeMapRecord(Long mapRecordId) throws Exception;
	
	public void removeMapEntry(Long mapEntryId) throws Exception;
	
	public void removeMapPrinciple(Long mapPrincipleId) throws Exception;
	
	public void removeMapAdvice(Long mapAdviceId) throws Exception;
	
	
	///////////////////////////
	// Other services       ///
	///////////////////////////
	
	public Long getMapRecordCountForMapProjectId(Long mapProjectId) throws Exception;
	
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId) throws Exception;
	
	public List<MapRecord> getMapRecordsForMapProjectId(Long projectId,
			PfsParameter pfs) throws Exception;
		
	public List<MapRecord> getMapRecordsForConceptId(String conceptId) throws Exception;

	public List<Concept> getUnmappedDescendantsForConcept(String terminologyId,
			String terminology, String terminologyVersion, int threshold) throws Exception;

	public MapPrinciple getMapPrinciple(Long id) throws Exception;

	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject) throws Exception;
	
	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject, 
			Set<ComplexMapRefSetMember> complexMapRefSetMembers) throws Exception;

	public Long removeMapRecordsForProjectId(Long mapProjectId) throws Exception;

	public boolean getTransactionPerOperation() throws Exception;
	
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;
	
	public void beginTransaction() throws Exception;
	
	public void commit() throws Exception;
}
	
	