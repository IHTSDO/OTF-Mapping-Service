package org.ihtsdo.otf.mapping.model;

import java.util.List;
import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * The Interface MapBlock.
 *
 * @author ${author}
 */
public interface MapBlock {
	
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
	 * Returns the map advices.
	 *
	 * @return the map advices
	 */
	public Set<MapAdvice> getMapAdvices();
	
	/**
	 * Sets the map advices.
	 *
	 * @param mapAdvices the map advices
	 */
	public void setMapAdvices(Set<MapAdvice> mapAdvices);
	
	/**
	 * Adds the map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void addMapAdvice(MapAdvice mapAdvice);
	
	/**
	 * Removes the map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void removeMapAdvice(MapAdvice mapAdvice);
	
	/**
	 * Returns the map groups.
	 *
	 * @return the map groups
	 */
	public List<MapGroup> getMapGroups();
	
	/**
	 * Sets the map groups.
	 *
	 * @param mapGroups the map groups
	 */
	public void setMapGroups(List<MapGroup> mapGroups);
	
	/**
	 * Adds the map group.
	 *
	 * @param mapGroup the map group
	 */
	public void addMapGroup(MapGroup mapGroup);
	
	/**
	 * Removes the map group.
	 *
	 * @param mapGroup the map group
	 */
	public void removeMapGroup(MapGroup mapGroup);
	
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
