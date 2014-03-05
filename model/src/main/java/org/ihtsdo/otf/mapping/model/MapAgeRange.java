package org.ihtsdo.otf.mapping.model;

// TODO: Auto-generated Javadoc
/**
 * The interface for a preset age range used to construct rules for MapEntry.
 *
 * @author Patrick
 */
public interface MapAgeRange {
	
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
	public int getLowerValue();
	
	/**
	 * Sets the lower value.
	 *
	 * @param value the new lower value
	 */
	public void setLowerValue(int value);
	
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
	 * @return true, if this bound is inclusive (>=)
	 */
	public boolean getLowerInclusive();
	
	/**
	 * Sets the lower inclusive.
	 *
	 * @param the lower inclusive, i.e. true, if this bound is inclusive (>=)
	 */
	public void setLowerInclusive(boolean inclusive);
	
	/**
	 * Gets the upper value.
	 *
	 * @return the upper value
	 */
	public int getUpperValue();
	
	/**
	 * Sets the upper value.
	 *
	 * @param value the new upper value
	 */
	public void setUpperValue(int value);
	
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
	 * @return true, if this bound is inclusive (<=)
	 */
	public boolean getUpperInclusive();
	
	/**
	 * Sets the upper inclusive.
	 *
	 * @param inclusive the upper inclusive, true if this bound is inclusive (i.e. <=)
	 */
	public void setUpperInclusive(boolean inclusive);
}
