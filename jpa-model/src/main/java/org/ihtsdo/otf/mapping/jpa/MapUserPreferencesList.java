package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapUserPreferences;

/**
 * Container for map entrys.
 */
@XmlRootElement(name = "mapUserPreferencesList")
public class MapUserPreferencesList {

	/** The map entrys. */
	private List<MapUserPreferences> mapUserPreferences = new ArrayList<>();

	/**
	 * Instantiates a new map entry list.
	 */
	public MapUserPreferencesList() {
		// do nothing
	}

	/**
	 * Adds the map entry
	 * 
	 * @param MapUserPreferences
	 *            the map entry
	 */
	public void addMapUserPreferences(MapUserPreferences MapUserPreferences) {
		mapUserPreferences.add(MapUserPreferences);
	}

	/**
	 * Removes the map entry.
	 * 
	 * @param MapUserPreferences
	 *            the map entry
	 */
	public void removeMapUserPreferences(MapUserPreferences MapUserPreferences) {
		mapUserPreferences.remove(MapUserPreferences);
	}

	/**
	 * Sets the map entrys.
	 * 
	 * @param mapUserPreferences
	 *            the new map entrys
	 */
	public void setMapUserPreferences(List<MapUserPreferences> mapUserPreferences) {
		this.mapUserPreferences = new ArrayList<>();
		this.mapUserPreferences.addAll(mapUserPreferences);
		
		
	}
	
	/**
	 * Sorts the map entrys alphabetically by name
	 */
	public void sortMapUserPreferences() {
	
		Collections.sort(this.mapUserPreferences,
			new Comparator<MapUserPreferences>() {
				@Override
				public int compare(MapUserPreferences o1, MapUserPreferences o2) {
					return 0;
				}

			});
}

	/**
	 * Gets the map user preferences
	 * 
	 * @return the map user preferences
	 */
	@XmlElement(type=MapUserPreferencesJpa.class, name="mapUserPreferences")
	public List<MapUserPreferences> getMapUserPreferences() {
		return mapUserPreferences;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapUserPreferences.size();
	}

}
