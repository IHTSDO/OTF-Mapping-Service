package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapLead;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "mapLeadList")
public class MapLeadList {

	/** The map projects. */
	private List<MapLead> mapLeads = new ArrayList<MapLead>();

	/**
	 * Instantiates a new map project list.
	 */
	public MapLeadList() {
		// do nothing
	}

	/**
	 * Adds the map project.
	 * 
	 * @param mapLead
	 *            the map project
	 */
	public void addMapLead(MapLead mapLead) {
		mapLeads.add(mapLead);
	}

	/**
	 * Removes the map project.
	 * 
	 * @param mapLead
	 *            the map project
	 */
	public void removeMapLead(MapLead mapLead) {
		mapLeads.remove(mapLead);
	}

	/**
	 * Sets the map projects.
	 * 
	 * @param mapLeads
	 *            the new map projects
	 */
	public void setMapLeads(List<MapLead> mapLeads) {
		this.mapLeads = new ArrayList<MapLead>();
		this.mapLeads.addAll(mapLeads);
		
		
	}
	
	/**
	 * Sorts the map projects alphabetically by name
	 */
	public void sortMapLeads() {
	
		Collections.sort(this.mapLeads,
			new Comparator<MapLead>() {
				@Override
				public int compare(MapLead o1, MapLead o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=MapLeadJpa.class, name="mapLead")
	public List<MapLead> getMapLeads() {
		return mapLeads;
	}

}
