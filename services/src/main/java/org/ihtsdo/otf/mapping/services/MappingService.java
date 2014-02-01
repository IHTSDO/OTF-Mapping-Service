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
	public MapProject getMapProject(Long id);
	
	/**
	 * Return map specialist for auto-generated id
	 * @param id the auto-generated id
	 * @return the MapLead
	 */
	public MapLead getMapLead(Long id);
	
	/**
	 * Return map lead for auto-generated id
	 * @param id the auto-generated id
	 * @return the mapSpecialist
	 */
	public MapSpecialist getMapSpecialist(Long id);
	
	/**
	 * Return map record for auto-generated id
	 * @param id the auto-generated id
	 * @return the mapRecord
	 */
	public MapRecord getMapRecord(Long id);
	
	/**
	 * Returns all map projects
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjects();
	
	/**
	 * Retrieve all map specialists
	 * @return a List of MapSpecialists
	 */
	public List<MapSpecialist> getMapSpecialists();
	
	/**
	 * Retrieve all map leads
	 * @return a List of MapLeads
	 */
	public List<MapLead> getMapLeads();
	
	/**
	 * Retrieve all map principles
	 * @return a List of MapPrinciples
	 */
	public List<MapPrinciple> getMapPrinciples();
	
	/**
	 * Retrieve all map advices
	 * @return a List of MapAdvices
	 */
	public List<MapAdvice> getMapAdvices();
	
	/**
	 * Retrieve all map projects assigned to a particular map specialist
	 * @param mapSpecialist the map specialist
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjectsForMapSpecialist(MapSpecialist mapSpecialist);
	
	/**
	 * Retrieve all map projects assigned to a particular map lead
	 * @param mapLead the map lead
	 * @return a List of MapProjects
	 */
	public List<MapProject> getMapProjectsForMapLead(MapLead mapLead);
	
	/**
	 * Retrieve all map records
	 * @return a List of MapRecords
	 */
	public List<MapRecord> getMapRecords();
	
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
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the list of MapProject
	 */
	public SearchResultList findMapProjects(String query, PfsParameter pfsParameter);
	
	/** 
	 * Query for MapSpecialists
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapSpecialists(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapLeads
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapLeads(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapAdvices
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapAdvices
	 */
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapRecords
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapRecords
	 */
	public SearchResultList findMapRecords(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapEntrys
	 * @param query the query
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the List of MapEntrys
	 */
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter);

	////////////////////////////
	// Addition services     ///
    ////////////////////////////
	
	/**
	 * Adds the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist);
	
	/**
	 * Adds the map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapLead mapLead);
	
	/**
	 * Adds the map project.
	 *
	 * @param mapProject the map project
	 * @return the map project
	 */
	public MapProject addMapProject(MapProject mapProject);
	
	/**
	 * Adds the map record.
	 *
	 * @param mapRecord the map record
	 */
	public void addMapRecord(MapRecord mapRecord);
	
	/**
	 * Adds the map entry.
	 *
	 * @param mapEntry the map entry
	 */
	public void addMapEntry(MapEntry mapEntry);
	
	/**
	 * Adds the map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	public void addMapPrinciple(MapPrinciple mapPrinciple);
	
	/**
	 * Adds the map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void addMapAdvice(MapAdvice mapAdvice);
	
	////////////////////////////
	// Update services     ///
	////////////////////////////
	
	/**
	 * Update map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void updateMapSpecialist(MapSpecialist mapSpecialist);
	
	/**
	 * Update map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void updateMapLead(MapLead mapLead);
	
	/**
	 * Update map project.
	 *
	 * @param mapProject the map project
	 */
	public void updateMapProject(MapProject mapProject);
	
	/**
	 * Update map record.
	 *
	 * @param mapRecord the map record
	 */
	public void updateMapRecord(MapRecord mapRecord);
	
	/**
	 * Update map entry.
	 *
	 * @param mapEntry the map entry
	 */
	public void updateMapEntry(MapEntry mapEntry);
	
	/**
	 * Update map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	public void updateMapPrinciple(MapPrinciple mapPrinciple);
	
	/**
	 * Update map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void updateMapAdvice(MapAdvice mapAdvice);
	
	////////////////////////////
	//Removal services     ///
	////////////////////////////
	
	/**
	 * Removes the map specialist.
	 *
	 * @param mapSpecialistId the map specialist id
	 */
	public void removeMapSpecialist(Long mapSpecialistId);
	
	/**
	 * Removes the map lead.
	 *
	 * @param mapLeadId the map lead id
	 */
	public void removeMapLead(Long mapLeadId);
	
	/**
	 * Removes the map project.
	 *
	 * @param mapProjectId the map project id
	 */
	public void removeMapProject(Long mapProjectId);
	
	/**
	 * Removes the map record.
	 *
	 * @param mapRecordId the map record id
	 */
	public void removeMapRecord(Long mapRecordId);
	
	/**
	 * Removes the map entry.
	 *
	 * @param mapEntryId the map entry id
	 */
	public void removeMapEntry(Long mapEntryId);
	
	/**
	 * Removes the map principle.
	 *
	 * @param mapPrincipleId the map principle id
	 */
	public void removeMapPrinciple(Long mapPrincipleId);
	
	/**
	 * Removes the map advice.
	 *
	 * @param mapAdviceId the map advice id
	 */
	public void removeMapAdvice(Long mapAdviceId);
	
	
	///////////////////////////
	// Other services       ///
	///////////////////////////
	
	/**
	 * Returns the map record count for map project id.
	 *
	 * @param mapProjectId the map project id
	 * @return the map record count for map project id
	 */
	public Long getMapRecordCountForMapProjectId(Long mapProjectId);
	
	/**
	 * Returns the map records for map project id.
	 *
	 * @param mapProjectId the map project id
	 * @return the map records for map project id
	 */
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId);
	
	/**
	 * Returns the map records for map project id.
	 *
	 * @param projectId the project id
	 * @param pfs the pfs
	 * @return the map records for map project id
	 */
	public List<MapRecord> getMapRecordsForMapProjectId(Long projectId,
			PfsParameter pfs);
		
	/**
	 * Returns the map records for concept id.
	 *
	 * @param conceptId the concept id
	 * @return the map records for concept id
	 */
	public List<MapRecord> getMapRecordsForConceptId(String conceptId);

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
	public List<Concept> getUnmappedDescendantsForConcept(String terminologyId,
			String terminology, String terminologyVersion, int threshold) throws Exception;

	/**
	 * Returns the map principle.
	 *
	 * @param id the id
	 * @return the map principle
	 */
	public MapPrinciple getMapPrinciple(Long id);

	/**
	 * Creates the map records for map project.
	 *
	 * @param mapProject the map project
	 * @return the list
	 * @throws Exception the exception
	 */
	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject) throws Exception;
	
	/**
	 * Creates the map records for map project.
	 *
	 * @param mapProject the map project
	 * @param complexMapRefSetMembers the complex map ref set members
	 * @return the list
	 * @throws Exception the exception
	 */
	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject, 
			Set<ComplexMapRefSetMember> complexMapRefSetMembers) throws Exception;

	/**
	 * Removes the map records for project id.
	 *
	 * @param mapProjectId the map project id
	 * @return the long
	 */
	public Long removeMapRecordsForProjectId(Long mapProjectId);

	
}
	
	