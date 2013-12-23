package org.ihtsdo.otf.mapping.model;

import java.util.List;

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
	
	
	// TODO: Removed map block and map group elements from model, update/rethink/etc.
	/**
	 * Returns the map blocks.
	 *
	 * @return the map blocks
	 *//*
	public List<MapBlock> getMapBlocks();
	
	*//**
	 * Sets the map blocks.
	 *
	 * @param mapBlocks the map blocks
	 *//*
	public void setMapBlocks(List<MapBlock> mapBlocks);
	
	*//**
	 * Adds the map block.
	 *
	 * @param mapBlock the map block
	 *//*
	public void addMapBlock(MapBlock mapBlock);
	
	*//**
	 * Removes the map block.
	 *
	 * @param mapBlock the map block
	 *//*
	public void removeMapBlock(MapBlock mapBlock);
	
	*//**
	 * Returns the map groups.
	 *
	 * @return the map groups
	 *//*
	public List<MapGroup> getMapGroups();
	
	*//**
	 * Sets the map groups.
	 *
	 * @param mapGroups the map groups
	 *//*
	public void setMapGroups(List<MapGroup> mapGroups);
	
	*//**
	 * Adds the map group.
	 *
	 * @param mapGroup the map group
	 *//*
	public void addMapGroup(MapGroup mapGroup);
	
	*//**
	 * Removes the map group.
	 *
	 * @param mapGroup the map group
	 *//*
	public void removeMapGroup(MapGroup mapGroup);*/
	/*
  *//**
   * Returns the map notes.
   *
   * @return the map notes
   *//*
  public List<MapNote> getNotes();
	
	*//**
	 * Sets the map notes.
	 *
	 * @param mapNotes the map notes
	 *//*
	public void setNotes(List<MapNote> notes);
	
	*//**
	 * Adds the map note.
	 *
	 * @param mapNote the map note
	 *//*
	public void addNote(MapNote note);
	
	*//**
	 * Removes the map note.
	 *
	 * @param mapNote the map note
	 *//*
	public void removeNote(MapNote note);*/
	
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
	
	
}
