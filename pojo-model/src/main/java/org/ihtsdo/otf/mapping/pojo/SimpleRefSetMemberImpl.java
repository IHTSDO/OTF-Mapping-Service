package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.SimpleRefSetMember;

/**
 * Concrete implementation of {@link SimpleRefSetMember}.
 */
public class SimpleRefSetMemberImpl extends AbstractConceptRefSetMember
		implements SimpleRefSetMember {

	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		 return this.getId() + "," +
				 this.getTerminology() + "," +
				 this.getTerminologyId() + "," +
				 this.getTerminologyVersion() + "," +
				 this.getEffectiveTime() + "," +
				 this.isActive() + "," +
	
				 (this.getConcept() == null ? null : this.getConcept().getTerminologyId());
	}
}
