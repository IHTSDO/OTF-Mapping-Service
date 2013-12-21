package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Interface for services to retrieve (get) map objects
 * @author Patrick
 */
public interface MappingService {
	

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
	 * Return map advice for auto-generated id
	 * @param id the auto-generated id
	 * @return the mapAdvice
	 */
	public MapAdvice getMapAdvice(Long id);
	
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
	 * Retrieve all map advice
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
	
	////////////////////
	// Query services //
	////////////////////
	
	/**
	 * Query for MapProjects
	 * @param query the query
	 * @return the list of MapProject
	 */
	public SearchResultList findMapProjects(String query);
	
	/** 
	 * Query for MapSpecialists
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapSpecialists(String query);
	
	/**
	 * Query for MapLeads
	 * @param query the query
	 * @return the List of MapProjects
	 */
	public SearchResultList findMapLeads(String query);
	
	/**
	 * Query for MapAdvices
	 * @param query the query
	 * @return the List of MapAdvices
	 */
	public SearchResultList findMapAdvices(String query);
	
	/**
	 * Query for MapRecords
	 * @param query the query
	 * @return the List of MapRecords
	 */
	public SearchResultList findMapRecords(String query);
	
	/**
	 * Query for MapEntrys
	 * @param query the query
	 * @return the List of MapEntrys
	 */
	public SearchResultList findMapEntrys(String query);
	
	/**
	 * Query for MapBlocks
	 * @param query the query
	 * @return the List of MapBlocks
	 */
	public SearchResultList findMapBlocks(String query);
	
	/**
	 * Query for MapGroups
	 * @param query the query
	 * @return the List of MapGroups
	 */
	public SearchResultList findMapGroups(String query);
	
	/**
	 * Query for MapNotes
	 * @param query the query
	 * @return the List of MapNotes
	 */
	public SearchResultList findMapNotes(String query);
}