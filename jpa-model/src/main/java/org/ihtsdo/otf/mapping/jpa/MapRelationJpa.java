package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Indexed;
import org.ihtsdo.otf.mapping.model.MapRelation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Class MapRelationJpa.
 */
@Entity
//add indexes
@Table(name="map_relations")
@Audited
@Indexed
@XmlRootElement(name = "mapRecord")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapRelationJpa implements MapRelation {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The terminology id. */
	@Column(nullable = false)
	private String terminologyId;
	
	/** The name. */
	@Column(nullable = false)
	private String name;
	
	/** Whether this relation can be used for null targets. */
	@Column(nullable = false)
	private boolean isAllowableForNullTarget;
	
	/** Whether this relation is computed. */ 
	@Column(nullable = false)
	private boolean isComputed;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#getTerminologyId()
	 */
	@Override
	public String getTerminologyId() {
		return terminologyId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#setTerminologyId(java.lang.String)
	 */
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#isAllowableForNullTarget()
	 */
	@Override
	public boolean isAllowableForNullTarget() {
		return isAllowableForNullTarget;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#setAllowableForNullTarget(boolean)
	 */
	@Override
	public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
		this.isAllowableForNullTarget = isAllowableForNullTarget;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#isComputed()
	 */
	@Override
	public boolean isComputed() {
		return isComputed;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRelation#setComputed(boolean)
	 */
	@Override
	public void setComputed(boolean isComputed) {
		this.isComputed = isComputed;
	}

}
