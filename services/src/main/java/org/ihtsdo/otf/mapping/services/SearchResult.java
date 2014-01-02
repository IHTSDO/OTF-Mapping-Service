package org.ihtsdo.otf.mapping.services;

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
	 * @return the description
	 */
	public String getDescription();

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description);

}
