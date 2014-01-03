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
@XmlRootElement(name = "mapEntryList")
public class MapEntryList {

	/** The map entrys. */
	private List<MapEntry> mapEntrys = new ArrayList<MapEntry>();

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
		mapEntrys.add(MapEntry);
	}

	/**
	 * Removes the map entry.
	 * 
	 * @param MapEntry
	 *            the map entry
	 */
	public void removeMapEntry(MapEntry MapEntry) {
		mapEntrys.remove(MapEntry);
	}

	/**
	 * Sets the map entrys.
	 * 
	 * @param mapEntrys
	 *            the new map entrys
	 */
	public void setmapEntrys(List<MapEntry> mapEntrys) {
		this.mapEntrys = new ArrayList<MapEntry>();
		this.mapEntrys.addAll(mapEntrys);
		
		
	}
	
	/**
	 * Sorts the map entrys alphabetically by name
	 */
	public void sortmapEntrys() {
	
		Collections.sort(this.mapEntrys,
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
	@XmlElement(type=MapEntryJpa.class, name="mapEntry")
	public List<MapEntry> getmapEntrys() {
		return mapEntrys;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapEntrys.size();
	}

}
