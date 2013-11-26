package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.AttributeValueRefSetMember;

/**
 * Concrete implementation of {@link AttributeValueRefSetMember}.
 */
public class AttributeValueRefSetMemberImpl extends
		AbstractConceptRefSetMember implements AttributeValueRefSetMember {

	/** The value id */
	private Long valueId;
	
	/** Returns the value id
	 * @return the value id
	 */
	@Override
	public Long getValueId() {
		return this.valueId;
	}

	/** Sets the value id
	 * @param valueId the value id
	 */
	@Override
	public void setValueId(long valueId) {
		this.valueId = valueId;
	}

}
