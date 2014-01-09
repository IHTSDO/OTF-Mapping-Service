package org.ihtsdo.otf.mapping.helpers;

import java.util.List;
import java.util.Map;

/**
 * Container for list of ids to default preferred name tuples (IdNamePairs).
 * Used to return metadata in an XML friendly way.
 *
 * @author ${author}
 */
public interface IdNameMap {


	/**
	 * Returns the id name pair list.
	 *
	 * @return the id name pair list
	 */
	public List<IdNamePair> getIdNamePairList();

	/**
	 * Sets the id name pair list.
	 *
	 * @param idNamePairs the id name pairs
	 */
	public void setIdNamePairList(List<IdNamePair> idNamePairs);
	
  /**
   * Adds the id name pair.
   *
   * @param IdNamePair
   */
  public void addIdNamePair(IdNamePair idNamePair);
	
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
