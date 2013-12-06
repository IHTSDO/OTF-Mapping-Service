package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;

/**
 * Concrete implementation of {@link AttributeValueRefSetMember}.
 */
@Entity
@Table(name = "attribute_value_refset_members")
@Audited
public class AttributeValueRefSetMemberJpa extends
		AbstractConceptRefSetMember implements AttributeValueRefSetMember {

	/** The value id */
	@Column (nullable = false)
	private Long valueId;
	
	/**
     * {@inheritDoc}
     */
	@Override
	public Long getValueId() {
		return this.valueId;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public void setValueId(long valueId) {
		this.valueId = valueId;
	}
	
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

 				 (this.getConcept() == null ? null : this.getConcept().getTerminologyId()) + "," +
 				 this.getValueId();
	}


}
