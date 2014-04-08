package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapNote;

/**
 * Container for map notes.
 */
@XmlRootElement(name = "mapNoteList")
public class MapNoteList {

	/** The map notes. */
	private List<MapNote> mapNotes = new ArrayList<>();

	/**
	 * Instantiates a new map note list.
	 */
	public MapNoteList() {
		// do nothing
	}

	/**
	 * Adds the map note.
	 * 
	 * @param mapNote
	 *            the map note
	 */
	public void addMapNote(MapNote mapNote) {
		mapNotes.add(mapNote);
	}

	/**
	 * Removes the map note.
	 * 
	 * @param mapNote
	 *            the map note
	 */
	public void removeMapNote(MapNote mapNote) {
		mapNotes.remove(mapNote);
	}

	/**
	 * Sets the map notes.
	 * 
	 * @param mapNotes
	 *            the new map notes
	 */
	public void setMapNotes(List<MapNote> mapNotes) {
		this.mapNotes = new ArrayList<>();
		this.mapNotes.addAll(mapNotes);
		
		
	}
	
	/**
	 * Sorts the map notes alphabetically by name
	 */
	public void sortMapNotes() {
	
		Collections.sort(this.mapNotes,
			new Comparator<MapNote>() {
				@Override
				public int compare(MapNote o1, MapNote o2) {
					return o1.getId().compareTo(o2.getId());
				}

			});
}

	/**
	 * Gets the map notes.
	 * 
	 * @return the map notes
	 */
	@XmlElement(type=MapNoteJpa.class, name="mapNote")
	public List<MapNote> getMapNotes() {
		return mapNotes;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapNotes.size();
	}

}
