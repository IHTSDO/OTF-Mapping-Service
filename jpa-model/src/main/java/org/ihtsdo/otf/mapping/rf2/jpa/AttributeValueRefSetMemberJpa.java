package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;

/**
 * Concrete implementation of {@link AttributeValueRefSetMember}.
 */
@Entity
@Table(name = "attribute_value_refset_members")
// @Audited
public class AttributeValueRefSetMemberJpa extends AbstractConceptRefSetMember
    implements AttributeValueRefSetMember {

  /** The value id */
  @Column(nullable = false)
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
            .getTerminologyId()) + "," + this.getValueId();
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
    result = prime * result + ((valueId == null) ? 0 : valueId.hashCode());
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
    AttributeValueRefSetMemberJpa other = (AttributeValueRefSetMemberJpa) obj;
    if (valueId == null) {
      if (other.valueId != null)
        return false;
    } else if (!valueId.equals(other.valueId))
      return false;
    return true;
  }
}
