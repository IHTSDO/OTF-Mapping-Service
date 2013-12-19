package org.ihtsdo.otf.mapping.model;

import java.util.List;
import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * The Interface MapEntry.
 *
 * @author ${author}
 */
public interface MapEntry {
	
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
	 * Returns the notes.
	 *
	 * @return the notes
	 */
	public List<MapNote> getNotes();
	
	/**
	 * Sets the notes.
	 *
	 * @param notes the notes
	 */
	public void setNotes(List<MapNote> notes);
	
	/**
	 * Adds the note.
	 *
	 * @param note the note
	 */
	public void addNote(MapNote note);
	
	/**
	 * Removes the note.
	 *
	 * @param note the note
	 */
	public void removeNote(MapNote note);
	
	/**
	 * Returns the target.
	 *
	 * @return the target
	 */
	public String getTarget();
	
	/**
	 * Sets the target.
	 *
	 * @param target the target
	 */
	public void setTarget(String target);
	
	/**
	 * Returns the advices.
	 *
	 * @return the advices
	 */
	public Set<MapAdvice> getAdvices();
	
	/**
	 * Sets the advices.
	 *
	 * @param mapAdvices the advices
	 */
	public void setAdvices(Set<MapAdvice> mapAdvices);
	
	/**
	 * Adds the advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void addAdvice(MapAdvice mapAdvice);
	
	/**
	 * Removes the advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void removeAdvice(MapAdvice mapAdvice);
	
	/**
	 * Returns the rule.
	 *
	 * @return the rule
	 */
	public String getRule();
	
	/**
	 * Sets the rule.
	 *
	 * @param rule the rule
	 */
	public void setRule(String rule);
	
	/**
	 * Returns the index, the map priority.
	 *
	 * @return the index
	 */
	public int getIndex();
	
	/**
	 * Sets the index, the map priority.
	 *
	 * @param index the index
	 */
	public void setIndex(int index);
	
	/**
	 * Returns the relation id.
	 *
	 * @return the relation id
	 */
	public String getRelationId();
	
	/**
	 * Sets the relation id.
	 *
	 * @param relationId the relation id
	 */
	public void setRelationId(String relationId);
	
	/**
	 * Returns the map record.
	 *
	 * @return the map record
	 */
	public MapRecord getMapRecord();
	
	/**
	 * Sets the map record.
	 *
	 * @param mapRecord the map record
	 */
	public void setMapRecord(MapRecord mapRecord);
	

	
}
