package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;

/**
 * Concrete implementation of {@link LanguageRefSetMember}.
 */
@Entity
@Table(name = "language_refset_members")
//@Audited
public class LanguageRefSetMemberJpa extends AbstractDescriptionRefSetMember
    implements LanguageRefSetMember {

  /** the acceptability id */
  @Column(nullable = false)
  private Long acceptabilityId;

  /**
   * returns the acceptability id
   * @return the acceptability id
   */
  @Override
  public Long getAcceptabilityId() {
    return this.acceptabilityId;
  }

  /**
   * sets the acceptability id
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
    return this.getId() + ","
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
        + this.getModuleId()
        + ","
        + // end of basic component fields

        (this.getDescription() == null ? null : getDescription()
            .getTerminologyId()) + "," + this.getAcceptabilityId();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractDescriptionRefSetMember#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result
            + ((acceptabilityId == null) ? 0 : acceptabilityId.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractDescriptionRefSetMember#equals(java
   * .lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    LanguageRefSetMemberJpa other = (LanguageRefSetMemberJpa) obj;
    if (acceptabilityId == null) {
      if (other.acceptabilityId != null)
        return false;
    } else if (!acceptabilityId.equals(other.acceptabilityId))
      return false;
    return true;
  }
}
