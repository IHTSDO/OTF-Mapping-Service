package org.ihtsdo.otf.mapping.helpers;

import java.util.Map;

/**
 * Container for map of ids to default preferred names.
 * Used to return metadata in an XML friendly way.
 *
 * @author ${author}
 */
public interface IdNameMap {


	/**
	 * Returns the id name map.
	 *
	 * @return the id name map
	 */
	public Map<Long, String> getIdNameMap();

	/**
	 * Sets the id name map.
	 *
	 * @param idNameMap the id name map
	 */
	public void setIdNameMap(Map<Long, String> idNameMap);
	
  /**
   * Adds the id name map entry.
   *
   * @param id the id
   * @param name the name
   */
  public void addIdNameMapEntry(Long id, String name);
	
	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	public void setName(String name);
	
	/**
	 * Returns the name of the type of metadata in the map.
	 *
	 * @return the name
	 */
	public String getName();

}
