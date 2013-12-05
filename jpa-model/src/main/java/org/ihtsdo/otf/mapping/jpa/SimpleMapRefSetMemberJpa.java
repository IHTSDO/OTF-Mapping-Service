package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.SimpleMapRefSetMember;
/**
 * Concrete implementation of {@link SimpleMapRefSetMember}.
 */
@Entity
@Table ( name = "simple_map_refset_members")
@Audited
public class SimpleMapRefSetMemberJpa extends AbstractConceptRefSetMember
		implements SimpleMapRefSetMember {
	
	/** The map target */
	@Column (nullable = false)
	private String mapTarget;

	/** returns the map target
	 * @return the map target
	 */
	@Override
	public String getMapTarget() {
		return this.mapTarget;
	}

	/** sets the map target
	 * @param mapTarget the map target
	 */
	@Override
	public void setMapTarget(String mapTarget) {
		this.mapTarget = mapTarget;
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
				 this.getMapTarget();
			
	}

}
