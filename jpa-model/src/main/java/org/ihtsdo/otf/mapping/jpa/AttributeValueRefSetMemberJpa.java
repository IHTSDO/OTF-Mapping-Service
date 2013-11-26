package org.ihtsdo.otf.mapping.jpa;

import org.ihtsdo.otf.mapping.model.AttributeValueRefSetMember;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Concrete implementation of {@link AttributeValueRefSetMember}.
 */
@Entity
@Table(name = "attribute_value_refset_members")
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
		return String.valueOf(getRefSetId());
	}


}
