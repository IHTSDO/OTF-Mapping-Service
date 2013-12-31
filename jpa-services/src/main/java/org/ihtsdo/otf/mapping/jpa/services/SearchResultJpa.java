package org.ihtsdo.otf.mapping.jpa.services;

import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.services.SearchResult;

/**
 * The search result for the Jpa package
 * @author Patrick
 *
 */
@XmlRootElement
public class SearchResultJpa implements SearchResult {
	
	private Long id;
	private String description;
	
	/**
	 * Default constructor
	 */
	public SearchResultJpa() {
		// left empty
	}

	/**
	 * Constructor
	 * @param id the id
	 * @param description the description
	 */
	public SearchResultJpa(Long id, String description) {
		this.id = id;
		this.description = description;
	}


	/**
	 * Returns the id
	 * @return the id
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the id
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
		
	}

	/**
	 * Gets the description
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Sets the description
	 * @param description the description
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SearchResultJpa other = (SearchResultJpa) obj;
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SearchResultJpa [id=" + id + ", description=" + description
				+ "]";
	}

	

}
