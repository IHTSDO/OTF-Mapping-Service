package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Indexed;
import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * The Class MapAdviceJpa.
 */
@Entity
@Table(name = "map_advices")
@Audited
@Indexed
@XmlRootElement(name="mapAdvice")
public class MapAdviceJpa implements MapAdvice {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The name. */
	@Column(nullable = false, unique = true, length = 25)
	private String name;
	
	/** The description. */
	@Column(nullable = false, unique = true)
	private String description;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getId()
	 */
	@Override
	public Long getId() {
		return id;
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
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

}
