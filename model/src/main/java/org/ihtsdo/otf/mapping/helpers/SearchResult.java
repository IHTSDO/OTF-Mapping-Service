package org.ihtsdo.otf.mapping.helpers;

/**
 * Generic object to contain search results
 * @author Patrick
 *
 */
public interface SearchResult {

	/**
	 * @return the id
	 */
	public Long getId();

	/**
	 * @param id the id to set
	 */
	public void setId(Long id);
	
	/**
	 * @return the terminologyId
	 */
	public String getTerminologyId();

	/**
	 * @param terminologyId the terminologyId to set
	 */
	public void setTerminologyId(String terminologyId);

	/**
	 * @return the value
	 */
	public String getValue();

	/**
	 * @param value the value to set
	 */
	public void setValue(String value);

}
