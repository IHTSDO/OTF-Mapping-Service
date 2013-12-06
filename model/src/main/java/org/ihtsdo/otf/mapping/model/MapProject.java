package org.ihtsdo.otf.mapping.model;

import java.util.Set;

/**
 * Represents a map project.
 *
 */
public interface MapProject  {

	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the map leads.
	 *
	 * @return the map leads
	 */
	public Set<MapLead> getMapLeads();

	
	/**
	 * Sets the map leads.
	 *
	 * @param mapLeads the map leads
	 */
	public void setMapLeads(Set<MapLead> mapLeads);
	

	/**
	 * Adds the map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapLead mapLead);
	

	/**
	 * Removes the map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void removeMapLead(MapLead mapLead);
	
	
	/**
	 * Returns the map specialists.
	 *
	 * @return the map specialists
	 */
	public Set<MapSpecialist> getMapSpecialists();

	
	/**
	 * Sets the map specialists.
	 *
	 * @param mapSpecialists the map specialists
	 */
	public void setMapSpecialists(Set<MapSpecialist> mapSpecialists);
	

	/**
	 * Adds the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist);
	

	/**
	 * Removes the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void removeMapSpecialist(MapSpecialist mapSpecialist);
	

	/**
	 * Returns the source terminology.
	 *
	 * @return the source terminology
	 */
	public String getSourceTerminology();


	/**
	 * Sets the source terminology.
	 *
	 * @param sourceTerminology the source terminology
	 */
	public void setSourceTerminology(String sourceTerminology);
	

	/**
	 * Returns the destination terminology.
	 *
	 * @return the destination terminology
	 */
	public String getDestinationTerminology();


	/**
	 * Sets the destination terminology.
	 *
	 * @param destinationTerminology the destination terminology
	 */
	public void setDestinationTerminology(String destinationTerminology);
	

	/**
	 * Returns the source terminology version.
	 *
	 * @return the source terminology version
	 */
	public String getSourceTerminologyVersion();


	/**
	 * Sets the source terminology version.
	 *
	 * @param sourceTerminologyVersion the source terminology version
	 */
	public void setSourceTerminologyVersion(String sourceTerminologyVersion);
	
	/**
	 * Returns the destination terminology version.
	 *
	 * @return the destination terminology version
	 */
	public String getDestinationTerminologyVersion();


	/**
	 * Sets the destination terminology version.
	 *
	 * @param destinationTerminologyVersion the destination terminology version
	 */
	public void setDestinationTerminologyVersion(String destinationTerminologyVersion);
}
