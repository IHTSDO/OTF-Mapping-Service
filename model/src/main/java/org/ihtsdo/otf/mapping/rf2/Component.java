package org.ihtsdo.otf.mapping.rf2;

import java.util.Date;

/**
 * Generically represents a terminology component.
 */
public interface Component {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the terminology.
   * 
   * @return the terminology
   */
  public String getTerminology();

  /**
   * Sets the terminology.
   * 
   * @param terminology the terminology
   */
  public void setTerminology(String terminology);

  /**
   * Returns the effective time.
   * 
   * @return the effective time
   */
  public Date getEffectiveTime();

  /**
   * Sets the effective time.
   * 
   * @param effectiveTime the effective time
   */
  public void setEffectiveTime(Date effectiveTime);

  /**
   * Indicates whether or not active is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isActive();

  /**
   * Sets the active.
   * 
   * @param active the active
   */
  public void setActive(boolean active);

  /**
   * Returns the module id.
   * 
   * @return the module id
   */
  public Long getModuleId();

  /**
   * Sets the module id.
   * 
   * @param moduleId the module id
   */
  public void setModuleId(Long moduleId);

  /**
   * Returns the terminology version.
   * 
   * @return the terminology version
   */
  public String getTerminologyVersion();

  /**
   * Sets the terminology version.
   * 
   * @param terminologyVersion the terminology version
   */
  public void setTerminologyVersion(String terminologyVersion);

  /**
   * Returns the terminology id.
   * 
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Sets the terminology id.
   * 
   * @param terminologyId the terminology id
   */
  public void setTerminologyId(String terminologyId);

  /**
   * Returns the label.
   *
   * @return the label
   */
  public String getLabel();

  /**
   * Sets the label.
   *
   * @param label the label to set
   */
  public void setLabel(String label);

  /**
   * Returns a string of comma-separated fields of this object.
   * 
   * @return a string of comma-separated fields
   */
  @Override
  public String toString();

}