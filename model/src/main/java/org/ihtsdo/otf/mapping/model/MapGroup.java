package org.ihtsdo.otf.mapping.model;

import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Interface MapGroup.
 *
 * @author ${author}
 */
public interface MapGroup {
	
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
	 * Returns the index.
	 *
	 * @return the index
	 */
	public int getIndex();
	
	/**
	 * Sets the index.
	 *
	 * @param index the index
	 */
	public void setIndex(int index);
	
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
	 * Returns the map block.
	 *
	 * @return the map block
	 */
	public MapBlock getMapBlock();
	
	/**
	 * Sets the map block.
	 *
	 * @param mapBlock the map block
	 */
	public void setMapBlock(MapBlock mapBlock);

}
