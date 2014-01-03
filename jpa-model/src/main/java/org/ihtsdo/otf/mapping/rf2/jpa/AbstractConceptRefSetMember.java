package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.ConceptRefSetMember;

/**
 * Abstract implementation of {@link ConceptRefSetMember}.
 */
@MappedSuperclass
@Audited
public abstract class AbstractConceptRefSetMember extends AbstractRefSetMember
		implements ConceptRefSetMember {

	/** The Concept associated with this element */
	@ManyToOne(targetEntity = ConceptJpa.class, optional = false)
	private Concept concept;

	/**
	 * {@inheritDoc}
	 */
	@XmlTransient
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

	/**
	 * Returns the concept id. Used for XML/JSON serialization.
	 * 
	 * @return the concept id
	 */
	@XmlElement
	public String getConceptId() {
		return concept != null ? concept.getTerminologyId() : null;
	}
}
