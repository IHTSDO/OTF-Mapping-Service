package org.ihtsdo.otf.mapping.jpa;

import org.ihtsdo.otf.mapping.model.LanguageRefSetMember;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Concrete implementation of {@link LanguageRefSetMember}.
 */
@Entity
@Table (name = "language_refset_members")
public class LanguageRefSetMemberJpa extends AbstractDescriptionRefSetMember
		implements LanguageRefSetMember {
	
	/** the acceptability id */
	@Column ( nullable = false )
	private Long acceptabilityId;

	/** returns the acceptability id
	 * @return the acceptability id
	 */
	@Override
	public Long getAcceptabilityId() {
		return this.acceptabilityId;
	}

	/** sets the acceptability id
	 * @param acceptabilityId the acceptability id
	 */
	@Override
	public void setAcceptabilityId(Long acceptabilityId) {
		this.acceptabilityId = acceptabilityId;

	}
	
	 /**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		return String.valueOf(getRefSetId());
	}
}
