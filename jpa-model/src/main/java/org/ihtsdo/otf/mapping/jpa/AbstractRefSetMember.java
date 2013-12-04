package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.ihtsdo.otf.mapping.model.RefSetMember;

/**
 * Abstract implementation of {@link RefSetMember} for use with JPA
 */
@MappedSuperclass
@Audited
public abstract class AbstractRefSetMember extends AbstractComponent implements
		RefSetMember {
	
	/** The ref set id */
	@Column ( nullable = false)
	Long refSetId;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getRefSetId() {
		return this.refSetId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRefSetId(Long refSetId) {
		this.refSetId = refSetId;

	}
}
