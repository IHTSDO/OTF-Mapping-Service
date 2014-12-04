package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;

/**
 * Concrete implementation of {@link SimpleMapRefSetMember}.
 */
@Entity
@Table(name = "simple_map_refset_members")
// @Audited
public class SimpleMapRefSetMemberJpa extends AbstractConceptRefSetMember
    implements SimpleMapRefSetMember {

  /** The map target */
  @Column(nullable = false)
  private String mapTarget;

  /**
   * returns the map target
   * @return the map target
   */
  @Override
  public String getMapTarget() {
    return this.mapTarget;
  }

  /**
   * sets the map target
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
    return this.getId()
        + ","
        + this.getTerminology()
        + ","
        + this.getTerminologyId()
        + ","
        + this.getTerminologyVersion()
        + ","
        + this.getEffectiveTime()
        + ","
        + this.isActive()
        + ","
        +

        (this.getConcept() == null ? null : this.getConcept()
            .getTerminologyId()) + "," + this.getMapTarget();

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.jpa.AbstractConceptRefSetMember#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((mapTarget == null) ? 0 : mapTarget.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractConceptRefSetMember#equals(java.
   * lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleMapRefSetMemberJpa other = (SimpleMapRefSetMemberJpa) obj;
    if (mapTarget == null) {
      if (other.mapTarget != null)
        return false;
    } else if (!mapTarget.equals(other.mapTarget))
      return false;
    return true;
  }

}
