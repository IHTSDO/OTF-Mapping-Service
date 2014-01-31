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
	 * @return the list of MapProject
	 */
	public SearchResultList findMapProjects(String query, PfsParameter pfsParameter);
	
	/** 
	 * Query for MapSpecialists
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapSpecialists(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapLeads
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapLeads(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapAdvices
	 * @param query the query
	 * @return the List of MapAdvices
	 */
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapRecords
	 * @param query the query
	 * @return the List of MapRecords
	 */
	public SearchResultList findMapRecords(String query, PfsParameter pfsParameter);
	
	/**
	 * Query for MapEntrys
	 * @param query the query
	 * @return the List of MapEntrys
	 */
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter);

	////////////////////////////
	// Addition services     ///
    ////////////////////////////
	
	public void addMapSpecialist(MapSpecialist mapSpecialist);
	
	public void addMapLead(MapLead mapLead);
	
	public MapProject addMapProject(MapProject mapProject);
	
	public void addMapRecord(MapRecord mapRecord);
	
	public void addMapEntry(MapEntry mapEntry);
	
	public void addMapPrinciple(MapPrinciple mapPrinciple);
	
	public void addMapAdvice(MapAdvice mapAdvice);
	
	////////////////////////////
	// Update services     ///
	////////////////////////////
	
	public void updateMapSpecialist(MapSpecialist mapSpecialist);
	
	public void updateMapLead(MapLead mapLead);
	
	public void updateMapProject(MapProject mapProject);
	
	public void updateMapRecord(MapRecord mapRecord);
	
	public void updateMapEntry(MapEntry mapEntry);
	
	public void updateMapPrinciple(MapPrinciple mapPrinciple);
	
	public void updateMapAdvice(MapAdvice mapAdvice);
	
	////////////////////////////
	//Removal services     ///
	////////////////////////////
	
	public void removeMapSpecialist(Long mapSpecialistId);
	
	public void removeMapLead(Long mapLeadId);
	
	public void removeMapProject(Long mapProjectId);
	
	public void removeMapRecord(Long mapRecordId);
	
	public void removeMapEntry(Long mapEntryId);
	
	public void removeMapPrinciple(Long mapPrincipleId);
	
	public void removeMapAdvice(Long mapAdviceId);
	
	
	///////////////////////////
	// Other services       ///
	///////////////////////////
	
	public Long getMapRecordCountForMapProjectId(Long mapProjectId);
	
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId);
	
	public List<MapRecord> getMapRecordsForMapProjectId(Long projectId,
			PfsParameter pfs);
		
	public List<MapRecord> getMapRecordsForConceptId(String conceptId);

	public List<Concept> getUnmappedDescendantsForConcept(String terminologyId,
			String terminology, String terminologyVersion, int threshold) throws Exception;

	public MapPrinciple getMapPrinciple(Long id);

	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject) throws Exception;
	
	public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject, 
			Set<ComplexMapRefSetMember> complexMapRefSetMembers) throws Exception;

	public Long removeMapRecordsForProjectId(Long mapProjectId);

	
}
	
	