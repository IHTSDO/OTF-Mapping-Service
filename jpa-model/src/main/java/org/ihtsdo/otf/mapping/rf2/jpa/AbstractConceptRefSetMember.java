package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.ConceptRefSetMember;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Abstract implementation of {@link ConceptRefSetMember}.
 */
@MappedSuperclass
@Audited
public abstract class AbstractConceptRefSetMember extends AbstractRefSetMember
		implements ConceptRefSetMember {
	
	/** The Concept associated with this element */
	@ManyToOne(targetEntity=ConceptJpa.class, optional=false)
	/*@ManyToOne(cascade = {
			   CascadeType.PERSIST, CascadeType.MERGE
			 }, targetEntity=ConceptJpa.class)*/
  @JsonBackReference
  @ContainedIn
	private Concept concept;

	/**
     * {@inheritDoc}
     */
	@Override
	@XmlIDREF
	@XmlAttribute
	public ConceptJpa getConcept() {
		return (ConceptJpa)this.concept;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setConcept(Concept concept) {
		this.concept = concept;

	}
}
