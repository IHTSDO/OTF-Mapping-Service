package org.ihtsdo.otf.mapping.helpers;

import java.util.Map;

/**
 * Container for map of ids to default preferred names.
 * Used to return metadata in an XML friendly way.
 *
 * @author ${author}
 */
public interface IdNamePair {


	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id .
	 *
	 * @param id the id 
	 */
	public void setId(Long id);
	
	
	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	public void setName(String name);
	
	/**
	 * Returns the name
	 *
	 * @return the name
	 */
	public String getName();

}
