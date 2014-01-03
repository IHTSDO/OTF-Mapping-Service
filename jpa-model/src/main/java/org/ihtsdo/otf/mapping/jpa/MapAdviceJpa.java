package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * JPA enabled map advice
 */
@Entity
@Table(name = "map_advices")
@Audited
@XmlRootElement(name="mapAdvice")
public class MapAdviceJpa implements MapAdvice {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The name. */
	@Column(nullable = false, unique = true, length = 255)
	private String name;
	
	/** The description. */
	@Column(nullable = false, unique = true, length = 255)
	private String description;
	
	/**
	 * Instantiates an empty {@link MapAdviceJpa}.
	 */
	public MapAdviceJpa() {
		// empty
	}

	/**
	 * Instantiates a {@link MapAdviceJpa} from the specified parameters.
	 *
	 * @param id the id
	 * @param name the name
	 * @param description the description
	 */
	public MapAdviceJpa(Long id, String name, String description) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getId()
	 */
	@XmlTransient
	@Override
	public Long getId() {
		return id;
	}
	
	/**
	 * Return id as string.
	 *
	 * @return the id in string form
	 */
	@XmlID
	public String getObjectId() {
		return id.toString();
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getDescription()
	 */
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	@Override
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getName()
	 */
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
	 */
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime * result + ((description == null) ? 0 : description.hashCode());
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
		MapAdviceJpa other = (MapAdviceJpa) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
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

}
