package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.DescriptionRefSetMember;

/**
 * Abstract implementation of {@link DescriptionRefSetMember}.
 */
public abstract class AbstractDescriptionRefSetMember extends
		AbstractRefSetMember implements DescriptionRefSetMember {
	/** The Description associated with this element */
	private Description description;

	/** Returns the Description
	 * @return the Description
	 */
	@Override
	public Description getDescription() {
		return this.description;
	}

	/** Sets the Description
	 * @param description the Description
	 */
	@Override
	public void setDescription(Description description) {
		this.description = description;

	}
}
