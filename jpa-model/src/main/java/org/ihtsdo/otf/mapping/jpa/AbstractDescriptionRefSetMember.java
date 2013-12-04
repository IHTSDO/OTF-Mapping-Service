package org.ihtsdo.otf.mapping.jpa;

import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.DescriptionRefSetMember;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

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
	@JsonBackReference
	@ContainedIn
	private Description description;

	/**
     * {@inheritDoc}
     */
	@Override
	@XmlIDREF
	@XmlAttribute
	public DescriptionJpa getDescription() {
		return (DescriptionJpa)this.description;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setDescription(Description description) {
		this.description = description;

	}
}
