package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "MapSpecialistList")
public class MapSpecialistList {

	/** The map projects. */
	private List<MapSpecialist> mapSpecialists = new ArrayList<MapSpecialist>();

	/**
	 * Instantiates a new map project list.
	 */
	public MapSpecialistList() {
		// do nothing
	}

	/**
	 * Adds the map project.
	 * 
	 * @param mapSpecialist
	 *            the map project
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
		mapSpecialists.add(mapSpecialist);
	}

	/**
	 * Removes the map project.
	 * 
	 * @param mapSpecialist
	 *            the map project
	 */
	public void removeMapSpecialist(MapSpecialist mapSpecialist) {
		mapSpecialists.remove(mapSpecialist);
	}

	/**
	 * Sets the map projects.
	 * 
	 * @param mapSpecialists
	 *            the new map projects
	 */
	public void setMapSpecialists(List<MapSpecialist> mapSpecialists) {
		this.mapSpecialists = new ArrayList<MapSpecialist>();
		this.mapSpecialists.addAll(mapSpecialists);
		
		
	}
	
	/**
	 * Sorts the map projects alphabetically by name
	 */
	public void sortMapSpecialists() {
	
		Collections.sort(this.mapSpecialists,
			new Comparator<MapSpecialist>() {
				@Override
				public int compare(MapSpecialist o1, MapSpecialist o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=MapSpecialistJpa.class, name="mapSpecialist")
	public List<MapSpecialist> getMapSpecialists() {
		return mapSpecialists;
	}

	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapSpecialists.size();
	}
}
