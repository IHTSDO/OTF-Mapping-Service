package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.ihtsdo.otf.mapping.rf2.RefSetMember;

/**
 * Abstract implementation of {@link RefSetMember} for use with JPA
 */
@MappedSuperclass
// @Audited
public abstract class AbstractRefSetMember extends AbstractComponent implements
    RefSetMember {

  /** The ref set id */
  @Column(nullable = false)
  String refSetId;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRefSetId() {
    return this.refSetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.jpa.AbstractComponent#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((refSetId == null) ? 0 : refSetId.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractComponent#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractRefSetMember other = (AbstractRefSetMember) obj;
    if (refSetId == null) {
      if (other.refSetId != null)
        return false;
    } else if (!refSetId.equals(other.refSetId))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRefSetId(String refSetId) {
    this.refSetId = refSetId;

  }
}
