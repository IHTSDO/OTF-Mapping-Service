package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.ConceptRefSetMember;

/**
 * Abstract implementation of {@link ConceptRefSetMember}.
 */
public abstract class AbstractConceptRefSetMember extends AbstractRefSetMember
		implements ConceptRefSetMember {
	
	/** The Concept associated with this element */
	private Concept concept;

	/** Returns the Concept
	 * @return the Concept
	 */
	@Override
	public Concept getConcept() {
		return this.concept;
	}

	/** Sets the Concept
	 * @param concept the Concept
	 */
	@Override
	public void setConcept(Concept concept) {
		this.concept = concept;

	}
}
