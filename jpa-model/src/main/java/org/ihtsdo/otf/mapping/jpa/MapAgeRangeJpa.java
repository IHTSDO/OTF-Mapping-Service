package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapAgeRange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A JPA enabled {@link MapAgeRange}.
 */
@Entity
@Table(name = "map_age_ranges", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
    "name"
  })
})
@Audited
@XmlRootElement(name = "mapAgeRange")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapAgeRangeJpa implements MapAgeRange {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The age range preset name. */
  @Column(nullable = false)
  private String name;

  /** The lower bound parameters. */
  @Column(nullable = false)
  private Integer lowerValue;

  /** The lower units. */
  @Column(nullable = false)
  private String lowerUnits;

  /** The lower inclusive. */
  @Column(nullable = false)
  private boolean lowerInclusive;

  /** The upper bound parameters. */
  @Column(nullable = false)
  private Integer upperValue;

  /** The upper units. */
  @Column(nullable = false)
  private String upperUnits;

  /** The upper inclusive. */
  @Column(nullable = false)
  private boolean upperInclusive;

  /**
   * Instantiates a new map age range jpa.
   */
  public MapAgeRangeJpa() {
    // do nothing
  }

  /**
   * Instantiates a new map age range jpa.
   *
   * @param id the id
   * @param name the name
   * @param lowerValue the lower value
   * @param lowerUnits the lower units
   * @param lowerInclusive the lower inclusive
   * @param upperValue the upper value
   * @param upperUnits the upper units
   * @param upperInclusive the upper inclusive
   */
  public MapAgeRangeJpa(Long id, String name, Integer lowerValue,
      String lowerUnits, boolean lowerInclusive, Integer upperValue,
      String upperUnits, boolean upperInclusive) {
    super();
    this.id = id;
    this.name = name;
    this.lowerValue = lowerValue;
    this.lowerUnits = lowerUnits;
    this.lowerInclusive = lowerInclusive;
    this.upperValue = upperValue;
    this.upperUnits = upperUnits;
    this.upperInclusive = upperInclusive;
  }

  /**
   * Return the id.
   * 
   * @return the id
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.
   * 
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form.
   * 
   * @return the id in string form
   */
  @XmlID
  @Override
  public String getObjectId() {
    return id.toString();
  }

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

  /**
   * Returns <code>true</code> if lowerValue is -1.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasLowerBound() {
    return this.lowerValue == -1 ? false : true;
  }

  /**
   * Returns <code>true</code> if upperValue is -1.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasUpperBound() {
    return this.upperValue == -1 ? false : true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (lowerInclusive ? 1231 : 1237);
    result =
        prime * result + ((lowerUnits == null) ? 0 : lowerUnits.hashCode());
    result =
        prime * result + ((lowerValue == null) ? 0 : lowerValue.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + (upperInclusive ? 1231 : 1237);
    result =
        prime * result + ((upperUnits == null) ? 0 : upperUnits.hashCode());
    result =
        prime * result + ((upperValue == null) ? 0 : upperValue.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapAgeRangeJpa other = (MapAgeRangeJpa) obj;
    if (lowerInclusive != other.lowerInclusive)
      return false;
    if (lowerUnits == null) {
      if (other.lowerUnits != null)
        return false;
    } else if (!lowerUnits.equals(other.lowerUnits))
      return false;
    if (lowerValue == null) {
      if (other.lowerValue != null)
        return false;
    } else if (!lowerValue.equals(other.lowerValue))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (upperInclusive != other.upperInclusive)
      return false;
    if (upperUnits == null) {
      if (other.upperUnits != null)
        return false;
    } else if (!upperUnits.equals(other.upperUnits))
      return false;
    if (upperValue == null) {
      if (other.upperValue != null)
        return false;
    } else if (!upperValue.equals(other.upperValue))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MapAgeRangeJpa [name=" + name + ", lowerValue=" + lowerValue
        + ", lowerUnits=" + lowerUnits + ", lowerInclusive=" + lowerInclusive
        + ", upperValue=" + upperValue + ", upperUnits=" + upperUnits
        + ", upperInclusive=" + upperInclusive + "]";
  }

}
