package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Interfaces for services to edit (add/update/remove) map objects
 * @author Patrick
 *
 */
public interface EditMappingService {

/*	- addMapProject(MapProject) /project/id/12345 (method=POST XML data)
	- updateMapProject(MapProject) /project/id/12345 (method=PUT, XML data)
	- removeMapProject(long id) /project/id/12345 (method=DELETE)
	- also for "map advice"
	- also for "map specialists"
	- also for "map leads"*/
	
	/**
	 * Add a map project
	 * @param mapProject the map project
	 * @throws Exception 
	 */
	public void addMapProject(MapProject mapProject) throws Exception;
	
	/**
	 * Update a map project
	 * @param mapProject the changed map project
	 * @throws Exception the exception
	 */
	public void updateMapProject(MapProject mapProject) throws Exception;
	
	/**
	 * Remove (delete) a map project
	 * @param mapProjectId the map project to be removed
	 * @throws Exception 
	 */
	public void removeMapProject(Long mapProjectId) throws Exception;
	
	/**
	 * Add a map specialist
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist);
	
	/**
	 * Update a map specialist
	 * @param mapSpecialist the changed map specialist
	 */
	public void updateMapSpecialist(MapSpecialist mapSpecialist);
	
	/**
	 * Remove (delete) a map specialist
	 * @param mapSpecialistId the map specialist to be removed
	 */
	public void removeMapSpecialist(Long mapSpecialistId);
	
	/**
	 * Add a map lead
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapLead mapLead);
	
	/**
	 * Update a map lead
	 * @param mapLead the changed map lead
	 */
	public void updateMapLead(MapLead mapLead);
	
	/**
	 * Remove (delete) a map lead
	 * @param mapLeadId the map lead to be removed
	 */
	public void removeMapLead(Long mapLeadId);
	
}