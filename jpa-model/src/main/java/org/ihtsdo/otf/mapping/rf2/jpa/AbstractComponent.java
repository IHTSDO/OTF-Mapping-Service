package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.rf2.Component;

/**
 * Abstract implementation of {@link Component} for use with JPA.
 */
@Audited
@MappedSuperclass
public abstract class AbstractComponent implements Component {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The effective time. */
	@Temporal(TemporalType.TIMESTAMP)
	private Date effectiveTime;

	/** The active. */
	@Column(nullable = false)
	private boolean active;

	/** The module id. */
	@Column(nullable = false)
	private Long moduleId;

	/** The terminology. */
	@Column(nullable = false)
	private String terminology;

	/** The terminology id */
	@Column(nullable = false)
	private String terminologyId;

	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@XmlTransient
	public Long getId() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Date getEffectiveTime() {
		return effectiveTime;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEffectiveTime(Date effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive() {
		return active;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getModuleId() {
		return moduleId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModuleId(Long moduleId) {
		this.moduleId = moduleId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractComponent))
			return false;
		AbstractComponent other = (AbstractComponent) obj;
		if (id == null) {
			if (other.getId() != null)
				return false;
		} else if (!id.equals(other.getId()))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminologyVersion() {
		return terminologyVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTerminology() {
		return terminology;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@XmlID
	@XmlElement(name="objectId")
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminologyId() {
		return terminologyId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		return this.getId() + "," + this.getTerminology() + ","
				+ this.getTerminologyId() + "," + this.getTerminologyVersion() + ","
				+ this.getEffectiveTime() + "," + this.isActive() + ","
				+ this.getModuleId(); // end of basic component fields
	}
}
