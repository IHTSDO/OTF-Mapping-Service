package org.ihtsdo.otf.mapping.pojo;


import org.ihtsdo.otf.mapping.model.MapPrinciple;


/**
 * The Map Principle Object for the Jpa Domain
 * @author Patrick
 *
 */

public class MapPrincipleImpl implements MapPrinciple {

	/** The id */
	private Long id;
	
	/** The name */
	private String name;
	
	/** The description */
	private String description;
	
	/** The section reference */
	private String sectionRef;
	
	/** Default constructor */
	public MapPrincipleImpl() {
		// left empty
	}

	/**
	 * Return the id
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}
	
	/**
	 * Returns the id in string form
	 * @return the id in string form
	 */
	public String getObjectId() {
		return id.toString();
	}

	/**
	 * Set the id
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;		
	}

	/**
	 * Get the description
	 * @return the description
	 */
	@Override
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the description
	 * @param description the description
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
		
	}

	/**
	 * Get the name
	 * @return the name
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Set the name
	 * @param name the name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
		
	}

	/**
	 * Get the section reference
	 * @return the section reference
	 */
	@Override
	public String getSectionRef() {
		return this.sectionRef;
	}

	/**
	 * Set the section reference
	 * @param sectionRef the section reference
	 */
	@Override
	public void setSectionRef(String sectionRef) {
		this.sectionRef = sectionRef;
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((sectionRef == null) ? 0 : sectionRef.hashCode());
		return result;
	}

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
		MapPrincipleImpl other = (MapPrincipleImpl) obj;
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
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (sectionRef == null) {
			if (other.sectionRef != null) {
				return false;
			}
		} else if (!sectionRef.equals(other.sectionRef)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "MapPrincipleJpa [id=" + id + ", name=" + name
				+ ", description=" + description + ", sectionRef=" + sectionRef
				+ "]";
	}

}
