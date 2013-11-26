package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.ConceptRefSetMember;

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

	private Concept concept;

	/**
     * {@inheritDoc}
     */
	@Override
	public Concept getConcept() {
		return this.concept;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setConcept(Concept concept) {
		this.concept = concept;

	}
}
