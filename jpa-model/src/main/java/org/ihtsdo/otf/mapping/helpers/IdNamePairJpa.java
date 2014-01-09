package org.ihtsdo.otf.mapping.helpers;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Container for id name pairs.
 *
 * @author ${author}
 */
@XmlRootElement(name = "idNamePair")
public class IdNamePairJpa implements IdNamePair {


	/** The id. */
	private Long id;
	
	/** The name. */
	private String name = "";

	/**
	 * Instantiates a new id name map.
	 */
	public IdNamePairJpa() {
		// do nothing
	}

	/**
	 * Instantiates a {@link IdNamePairJpa} from the specified parameters.
	 *
	 * @param id the id
	 * @param name the name
	 */
	public IdNamePairJpa(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.IdNamePair#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.IdNamePair#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;	
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdNamePairJpa other = (IdNamePairJpa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IdNamePairJpa [id=" + id + ", name=" + name + "]";
	}


}
