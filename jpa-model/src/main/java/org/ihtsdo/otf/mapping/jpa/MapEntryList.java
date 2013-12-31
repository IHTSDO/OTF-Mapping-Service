package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapEntry;

/**
 * Container for map entrys.
 */
@XmlRootElement(name = "MapEntryList")
public class MapEntryList {

	/** The map entrys. */
	private List<MapEntry> MapEntrys = new ArrayList<MapEntry>();

	/**
	 * Instantiates a new map entry list.
	 */
	public MapEntryList() {
		// do nothing
	}

	/**
	 * Adds the map entry
	 * 
	 * @param MapEntry
	 *            the map entry
	 */
	public void addMapEntry(MapEntry MapEntry) {
		MapEntrys.add(MapEntry);
	}

	/**
	 * Removes the map entry.
	 * 
	 * @param MapEntry
	 *            the map entry
	 */
	public void removeMapEntry(MapEntry MapEntry) {
		MapEntrys.remove(MapEntry);
	}

	/**
	 * Sets the map entrys.
	 * 
	 * @param MapEntrys
	 *            the new map entrys
	 */
	public void setMapEntrys(List<MapEntry> MapEntrys) {
		this.MapEntrys = new ArrayList<MapEntry>();
		this.MapEntrys.addAll(MapEntrys);
		
		
	}
	
	/**
	 * Sorts the map entrys alphabetically by name
	 */
	public void sortMapEntrys() {
	
		Collections.sort(this.MapEntrys,
			new Comparator<MapEntry>() {
				@Override
				public int compare(MapEntry o1, MapEntry o2) {
					return o1.getId().compareTo(o2.getId());
				}

			});
}

	/**
	 * Gets the map entrys.
	 * 
	 * @return the map entrys
	 */
	@XmlElement(type=MapEntryJpa.class, name="MapEntry")
	public List<MapEntry> getMapEntrys() {
		return MapEntrys;
	}

}
