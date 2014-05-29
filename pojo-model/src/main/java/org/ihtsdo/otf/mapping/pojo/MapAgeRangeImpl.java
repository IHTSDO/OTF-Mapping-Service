package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapAgeRange;

/**
 * Reference implementation of {@link MapAgeRange}.
 */
public class MapAgeRangeImpl implements MapAgeRange {

  /** The id. */
  private Long id;

  /** The age range preset name. */
  private String name;

  /** The lower bound parameters. */
  private int lowerValue;

  /** The lower units. */
  private String lowerUnits;

  /** The lower inclusive. */
  private boolean lowerInclusive;

  /** The upper bound parameters. */
  private int upperValue;

  /** The upper units. */
  private String upperUnits;

  /** The upper inclusive. */
  private boolean upperInclusive;

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getName()
   */
  @Override
  public String getName() {
    return this.name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getLowerValue()
   */
  @Override
  public Integer getLowerValue() {
    return this.lowerValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapAgeRange#setLowerValue(java.lang.Integer)
   */
  @Override
  public void setLowerValue(Integer value) {
    this.lowerValue = value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getLowerUnits()
   */
  @Override
  public String getLowerUnits() {
    return this.lowerUnits;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapAgeRange#setLowerUnits(java.lang.String)
   */
  @Override
  public void setLowerUnits(String units) {
    this.lowerUnits = units;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getLowerInclusive()
   */
  @Override
  public boolean getLowerInclusive() {
    return this.lowerInclusive;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#setLowerInclusive(boolean)
   */
  @Override
  public void setLowerInclusive(boolean inclusive) {
    this.lowerInclusive = inclusive;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getUpperValue()
   */
  @Override
  public Integer getUpperValue() {
    return this.upperValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapAgeRange#setUpperValue(java.lang.Integer)
   */
  @Override
  public void setUpperValue(Integer value) {
    this.upperValue = value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getUpperUnits()
   */
  @Override
  public String getUpperUnits() {
    return this.upperUnits;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapAgeRange#setUpperUnits(java.lang.String)
   */
  @Override
  public void setUpperUnits(String units) {
    this.upperUnits = units;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getUpperInclusive()
   */
  @Override
  public boolean getUpperInclusive() {
    return this.upperInclusive;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#setUpperInclusive(boolean)
   */
  @Override
  public void setUpperInclusive(boolean inclusive) {
    this.upperInclusive = inclusive;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getId()
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#getObjectId()
   */
  @Override
  public String getObjectId() {
    return this.id.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#hasLowerBound()
   */
  @Override
  public boolean hasLowerBound() {
    return this.lowerValue == -1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAgeRange#hasUpperBound()
   */
  @Override
  public boolean hasUpperBound() {
    return this.upperValue == -1;
  }

}
