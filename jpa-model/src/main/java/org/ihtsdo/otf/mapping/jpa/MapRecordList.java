package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Container for map records.
 */
@XmlRootElement(name = "mapRecordList")
public class MapRecordList {

	/** The map records. */
	private List<MapRecord> mapRecords = new ArrayList<>();

	/**
	 * Instantiates a new map record list.
	 */
	public MapRecordList() {
		// do nothing
	}

	/**
	 * Adds the map record.
	 * 
	 * @param mapRecord
	 *            the map record
	 */
	public void addMapRecord(MapRecord mapRecord) {
		mapRecords.add(mapRecord);
	}

	/**
	 * Removes the map record.
	 * 
	 * @param mapRecord
	 *            the map record
	 */
	public void removeMapRecord(MapRecord mapRecord) {
		mapRecords.remove(mapRecord);
	}

	/**
	 * Sets the map records.
	 * 
	 * @param mapRecords
	 *            the new map records
	 */
	public void setMapRecords(List<MapRecord> mapRecords) {
		this.mapRecords = new ArrayList<>();
		if (mapRecords != null) {
			this.mapRecords.addAll(mapRecords);
		}
		
		
	}
	
	/**
	 * Sorts the map records alphabetically by name
	 */
	public void sortMapRecords() {
	
		Collections.sort(this.mapRecords,
			new Comparator<MapRecord>() {
				@Override
				public int compare(MapRecord o1, MapRecord o2) {
					return o1.getId().compareTo(o2.getId());
				}

			});
}

	/**
	 * Gets the map records.
	 * 
	 * @return the map records
	 */
	@XmlElement(type=MapRecordJpa.class, name="mapRecord")
	public List<MapRecord> getMapRecords() {
		return mapRecords;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapRecords.size();
	}
	

}
