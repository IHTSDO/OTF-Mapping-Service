package org.ihtsdo.otf.mapping.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic search result for use by services
 * @author Patrick
 *
 */
@XmlRootElement
public class SearchResult {

	/** The component id */
	private Long id;
	
	/** The description */
	private String description;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
