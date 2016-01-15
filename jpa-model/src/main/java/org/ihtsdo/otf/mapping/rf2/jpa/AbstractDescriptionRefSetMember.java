package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.DescriptionRefSetMember;

/**
 * Abstract implementation of {@link DescriptionRefSetMember}.
 */
@MappedSuperclass
// @Audited
public abstract class AbstractDescriptionRefSetMember extends
    AbstractRefSetMember implements DescriptionRefSetMember {

  /** The description. */
  @ManyToOne(targetEntity = DescriptionJpa.class, optional = false)
  // NOTE: this may apply only to LanguageRefSetMember given how
  // description uses @IndexedEmbedded
  @ContainedIn
  private Description description;

  /**
   * {@inheritDoc}
   */
  @XmlTransient
  @Override
  public Description getDescription() {
    return this.description;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((description == null) ? 0 : description.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractDescriptionRefSetMember other =
        (AbstractDescriptionRefSetMember) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDescription(Description description) {
    this.description = description;

  }

  /**
   * Returns the description id. Used for XML/JSON serialization.
   * 
   * @return the description id
   */
  @XmlElement
  public String getDescriptionId() {
    return description != null ? description.getTerminologyId() : null;
  }
}
