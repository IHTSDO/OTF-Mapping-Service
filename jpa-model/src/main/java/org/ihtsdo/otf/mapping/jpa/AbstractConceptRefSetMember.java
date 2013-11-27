package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.ConceptRefSetMember;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Abstract implementation of {@link ConceptRefSetMember}.
 */
@MappedSuperclass
public abstract class AbstractConceptRefSetMember extends AbstractRefSetMember
		implements ConceptRefSetMember {
	
	/** The Concept associated with this element */
	@ManyToOne(cascade = {
			   CascadeType.PERSIST, CascadeType.MERGE
			 }, targetEntity=ConceptJpa.class)
  @JsonBackReference
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
