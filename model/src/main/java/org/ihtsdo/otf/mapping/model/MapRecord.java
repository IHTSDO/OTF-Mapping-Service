package org.ihtsdo.otf.mapping.model;

import java.util.List;
import java.util.Set;

/**
 * The Interface MapRecord.
 *
 */
public interface MapRecord {
	
	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();
	
	/**
	 * Returns the id in string form
	 *
	 * @return the id in string form
	 */
	public String getObjectId();
	
	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the concept id.
	 *
	 * @return the concept id
	 */
	public String getConceptId();
	
	/**
	 * Sets the concept id.
	 *
	 * @param conceptId the concept id
	 */
	public void setConceptId(String conceptId);
	
	
	/**
	 * Returns the map notes.
	 *
	 * @return the map notes
	 */
    public Set<MapNote> getMapNotes();
	
	/**
	 * Sets the map notes.
	 *
	 * @param mapNotes the map notes
	 */
	public void setMapNotes(Set<MapNote> mapNotes);
	
	/**
	 * Adds the map note.
	 *
	 * @param mapNote the map note
	 */
	public void addMapNote(MapNote mapNote);
	
	/**
	 * Removes the map note.
	 *
	 * @param mapNote the map note
	 */
	public void removeMapNote(MapNote mapNote);
	
	/**
	 * Returns the map entries.
	 *
	 * @return the map entries
	 */
	public List<MapEntry> getMapEntries();
	
	/**
	 * Sets the map entries.
	 *
	 * @param mapEntries the map entries
	 */
	public void setMapEntries(List<MapEntry> mapEntries);
	
	/**
	 * Adds the map entry.
	 *
	 * @param mapEntry the map entry
	 */
	public void addMapEntry(MapEntry mapEntry);
	
	/**
	 * Removes the map entry.
	 *
	 * @param mapEntry the map entry
	 */
	public void removeMapEntry(MapEntry mapEntry);
	
	/**
	 * Returns the set of allowable map principles.
	 *
	 * @return the map principles
	 */
	public Set<MapPrinciple> getMapPrinciples();
	
	/**
	 * Sets the set of allowable map principles.
	 *
	 * @param mapAdvices the map principles
	 */
	public void setMapPrinciples(Set<MapPrinciple> mapAdvices);
	
	/**
	 * Adds an allowable map principle.
	 *
	 * @param mapAdvice the map principle
	 */
	public void addMapPrinciple(MapPrinciple mapAdvice);
	
	/**
	 * Removes an allowable map principle.
	 *
	 * @param mapAdvice the map principle
	 */
	public void removeMapPrinciple(MapPrinciple mapAdvice);
	
	
}
