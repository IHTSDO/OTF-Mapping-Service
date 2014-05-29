package org.ihtsdo.otf.mapping.rf2.pojo;

import java.util.Date;

import org.ihtsdo.otf.mapping.rf2.Component;

/**
 * Abstract implementation of {@link Component}.
 */
public abstract class AbstractComponent implements Component {

  /** The id. */
  private Long id;

  /** The effective time. */
  private Date effectiveTime;

  /** The active. */
  private boolean active;

  /** The module id. */
  private Long moduleId;

  /** The terminology field */
  private String terminology;

  /** The terminology id */
  private String terminologyId;

  /** The terminology version. */
  private String terminologyVersion;

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Date getEffectiveTime() {
    return effectiveTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setEffectiveTime(Date effectiveTime) {
    this.effectiveTime = effectiveTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActive() {
    return active;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getModuleId() {
    return moduleId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setModuleId(Long moduleId) {
    this.moduleId = moduleId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AbstractComponent))
      return false;
    AbstractComponent other = (AbstractComponent) obj;
    if (id == null) {
      if (other.getId() != null)
        return false;
    } else if (!id.equals(other.getId()))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTerminologyId() {
    return terminologyId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {

    return this.getId() + "," + this.getTerminology() + ","
        + this.getTerminologyId() + "," + this.getTerminologyVersion() + ","
        + this.getEffectiveTime() + "," + this.isActive() + ","
        + this.getModuleId(); // end of basic component fields
  }
}
