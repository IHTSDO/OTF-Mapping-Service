package org.ihtsdo.otf.mapping.model;

/**
 * Generically represents map advice.
 */
public interface MapAdvice {

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
   * Returns the id in string form.
   * 
   * @return the string object id
   */
  public String getObjectId();

  /**
   * Returns the detail.
   * 
   * @return the detail
   */
  public String getDetail();

  /**
   * Sets the detail.
   * 
   * @param detail the detail
   */
  public void setDetail(String detail);

  /**
   * Returns the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param name the name
   */
  public void setName(String name);

  /**
   * Checks if is allowable for null target.
   * 
   * @return true, if is allowable for null target
   */
  public boolean isAllowableForNullTarget();

  /**
   * Sets the allowable for null target.
   * 
   * @param isAllowableForNullTarget the new allowable for null target
   */
  public void setAllowableForNullTarget(boolean isAllowableForNullTarget);

  /**
   * Checks if is computable.
   * 
   * @return true, if is computable
   */
  public boolean isComputed();

  /**
   * Sets the computable.
   * 
   * @param isComputable the new computable
   */
  public void setComputed(boolean isComputable);

}
