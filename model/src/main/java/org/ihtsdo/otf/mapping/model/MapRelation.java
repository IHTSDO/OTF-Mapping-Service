package org.ihtsdo.otf.mapping.model;

/**
 * Generically represents a relationship between the source concept and target
 * id of a {@link MapEntry} in a {@link MapRecord}.
 */
public interface MapRelation {

  /**
   * Gets the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the new id
   */
  public void setId(Long id);

  /**
   * Gets the terminology id.
   * 
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Sets the terminology id.
   * 
   * @param terminologyId the new terminology id
   */
  public void setTerminologyId(String terminologyId);

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param name the new name
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
   * @param allowableForNullTarget the new allowable for null target
   */
  public void setAllowableForNullTarget(boolean allowableForNullTarget);

  /**
   * Checks if is computed.
   * 
   * @return true, if is computed
   */
  public boolean isComputed();

  /**
   * Sets the computed.
   * 
   * @param isComputed the new computed
   */
  public void setComputed(boolean isComputed);

  /**
   * Gets the abbreviation.
   * 
   * @return the abbreviation
   */
  public String getAbbreviation();

  /**
   * Sets the abbreviation.
   * 
   * @param abbreviation the new abbreviation
   */
  public void setAbbreviation(String abbreviation);

}
