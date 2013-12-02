package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.DescriptionRefSetMember;

/**
 * Abstract implementation of {@link DescriptionRefSetMember}.
 */
@MappedSuperclass
@Audited
public abstract class AbstractDescriptionRefSetMember extends
		AbstractRefSetMember implements DescriptionRefSetMember {
	
	
	/** The Description associated with this element */
	@ManyToOne(cascade = {
			   CascadeType.PERSIST, CascadeType.MERGE
			 }, targetEntity=DescriptionJpa.class)
	private Description description;

	/**
     * {@inheritDoc}
     */
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
}
