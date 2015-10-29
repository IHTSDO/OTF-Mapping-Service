package org.ihtsdo.otf.mapping.model;

/**
 * Generically represents an age range for a map rule.
 */
public interface MapAgeRange {

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
   * Gets the object id.
   * 
   * @return the object id
   */
  public String getObjectId();

  /**
   * Gets the name.
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
   * Gets the lower value.
   * 
   * @return the lower value
   */
  public Integer getLowerValue();

  /**
   * Sets the lower value.
   * 
   * @param value the new lower value
   */
  public void setLowerValue(Integer value);

  /**
   * Gets the lower units.
   * 
   * @return the lower units
   */
  public String getLowerUnits();

  /**
   * Sets the lower units.
   * 
   * @param units the new lower units
   */
  public void setLowerUnits(String units);

  /**
   * Gets the lower inclusive.
   * 
   * @return true, if this bound is inclusive (&gt;=)
   */
  public boolean getLowerInclusive();

  /**
   * Sets the lower inclusive.
   * 
   * @param inclusive the new lower inclusive
   */
  public void setLowerInclusive(boolean inclusive);

  /**
   * Gets the upper value.
   * 
   * @return the upper value
   */
  public Integer getUpperValue();

  /**
   * Sets the upper value.
   * 
   * @param value the new upper value
   */
  public void setUpperValue(Integer value);

  /**
   * Gets the upper units.
   * 
   * @return the upper units
   */
  public String getUpperUnits();

  /**
   * Sets the upper units.
   * 
   * @param units the new upper units
   */
  public void setUpperUnits(String units);

  /**
   * Gets the upper inclusive.
   * 
   * @return true, if this bound is inclusive (&lt;=)
   */
  public boolean getUpperInclusive();

  /**
   * Sets the upper inclusive.
   * 
   * @param inclusive the upper inclusive, true if this bound is inclusive (i.e.
   *          &lt;=)
   */
  public void setUpperInclusive(boolean inclusive);

  /**
   * Checks for lower bound.
   * 
   * @return true, if successful
   */
  public boolean hasLowerBound();

  /**
   * Checks for upper bound.
   * 
   * @return true, if successful
   */
  public boolean hasUpperBound();

}
