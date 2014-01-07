package org.ihtsdo.otf.mapping.helpers;

import java.util.List;


/**
 * The Interface IdNameMapList.
 *
 * @author ${author}
 */
public interface IdNameMapList {

	/**
	 * Returns the id name map list.
	 *
	 * @return the id name map list
	 */
	public List<IdNameMap> getIdNameMapList();
  
	/**
	 * Sets the id name map list.
	 *
	 * @param metadataMap the id name map list
	 */
	public void setIdNameMapList(List<IdNameMap> metadataMap);
	
	/**
	 * Adds the id name map.
	 *
	 * @param idNameMap the id name map
	 */
	public void addIdNameMap(IdNameMap idNameMap);
	
	/**
	 * Removes the id name map.
	 *
	 * @param idNameMap the id name map
	 */
	public void removeIdNameMap(IdNameMap idNameMap);
	
	/**
	 * Returns the count.
	 *
	 * @return the count
	 */
	public int getCount();

}
