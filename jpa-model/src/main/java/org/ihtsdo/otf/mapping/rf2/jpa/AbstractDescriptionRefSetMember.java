package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.DescriptionRefSetMember;

/**
 * Abstract implementation of {@link DescriptionRefSetMember}.
 */
@MappedSuperclass
@Audited
public abstract class AbstractDescriptionRefSetMember extends
		AbstractRefSetMember implements DescriptionRefSetMember {

	@ManyToOne(targetEntity = DescriptionJpa.class, optional = false)
	@ContainedIn
	private Description description;

	/**
	 * {@inheritDoc}
	 */
	@XmlTransient
	@Override
	public Description getDescription() {
		return this.description;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDescription(Description description) {
		this.description = description;

	}

	/**
	 * Returns the description id. Used for XML/JSON serialization.
	 * 
	 * @return the description id
	 */
	@XmlElement
	public String getDescriptionId() {
		return description != null ? description.getTerminologyId() : null;
	}
}
