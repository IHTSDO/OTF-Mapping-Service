package org.ihtsdo.otf.mapping.model;

import java.util.Set;

/**
 * The Interface MapEntry.
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
	 * Returns the id in string form
	 * @return the string object id
	 */
	public String getObjectId();
	
	/**
	 * Sets the Long id from a string
	 * @param objectId the object id in string form
	 */
	public void setObjectId(String objectId);
	
	/**
	 * Returns the notes.
	 *
	 * @return the notes
	 */
	public Set<MapNote> getMapNotes();
	
	/**
	 * Sets the notes.
	 *
	 * @param notes the notes
	 */
	public void setMapNotes(Set<MapNote> notes);
	
	/**
	 * Adds the note.
	 *
	 * @param note the note
	 */
	public void addMapNote(MapNote note);
	
	/**
	 * Removes the note.
	 *
	 * @param note the note
	 */
	public void removeMapNote(MapNote note);
	
	/**
	 * Returns the target.
	 *
	 * @return the target
	 */
	public String getTargetId();
	
	/**
	 * Sets the target.
	 *
	 * @param targetId the target terminology Id
	 */
	public void setTargetId(String targetId);
	
	/**
	 * Returns the target name.
	 *
	 * @return the target name
	 */
	public String getTargetName();
	
	/**
	 * Sets the target name.
	 *
	 * @param targetName the target name
	 */
	public void setTargetName(String targetName);
	
	/**
	 * Returns the advices.
	 *
	 * @return the advices
	 */
	public Set<MapAdvice> getMapAdvices();
	
	/**
	 * Sets the advices.
	 *
	 * @param mapAdvices the advices
	 */
	public void setMapAdvices(Set<MapAdvice> mapAdvices);
	
	/**
	 * Adds the advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void addMapAdvice(MapAdvice mapAdvice);
	
	/**
	 * Removes the advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void removeMapAdvice(MapAdvice mapAdvice);
	
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
	public int getIndexMapPriority();
	
	/**
	 * Sets the index, the map priority.
	 *
	 * @param index the index
	 */
	public void setIndexMapPriority(int index);
	
	/**
	 * Gets the map group
	 * @return the map group
	 */
	public int getMapGroup();
	
	/**
	 * Sets the map group
	 * @param mapGroup the map group
	 */
	public void setMapGroup(int mapGroup);
	
	/**
	 * Gets the map block
	 * @return the map block
	 */
	public int getMapBlock();
	
	/**
	 * Sets the map block
	 * @param mapBlock the map block
	 */
	public void setMapBlock(int mapBlock);
	
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
