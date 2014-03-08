package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapAgeRange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "map_age_ranges")
@Audited
@XmlRootElement(name="mapAgeRange")
@JsonIgnoreProperties(ignoreUnknown = true)

// Unique Constraints on All Values
public class MapAgeRangeJpa implements MapAgeRange {
	
	// TODO
	// make all fields required
	// test on value = -1 to indicate a null bound
	// boolean -> defaults to false
	// add comments to javadoc
	
	// add methods hasLowerBound, hasUpperBound
	// -> add this to serialization
	
	/** The id */
	@Id
	@GeneratedValue
	private Long id;

	/** The age range preset name */
	@Column(nullable = false)
	private String name;
	
	/** The lower bound parameters */
	@Column(nullable = false)
	private Integer 	lowerValue;
	
	@Column(nullable = false)
	private String 	lowerUnits;
	
	@Column(nullable = false)
	private boolean	lowerInclusive;
	
	/** The upper bound parameters */
	@Column(nullable = false)
	private Integer	upperValue;
	
	@Column(nullable = false)
	private String	upperUnits;
	
	@Column(nullable = false)
	private boolean	upperInclusive;
	
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
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;	
	}
	
	@Override
	public Integer getLowerValue() {
		return this.lowerValue;
	}
	
	@Override
	public void setLowerValue(Integer value) {
		this.lowerValue = value;
	}
	
	@Override
	public String getLowerUnits() {
		return this.lowerUnits;
	}
	
	@Override
	public void setLowerUnits(String units) {
		this.lowerUnits = units;
	}
	
	@Override
	public boolean getLowerInclusive() {
		return this.lowerInclusive;
	}
	@Override
	public void setLowerInclusive(boolean inclusive) {
		this.lowerInclusive = inclusive;
		
	}
	@Override
	public Integer getUpperValue() {
		return this.upperValue;
	}
	
	@Override
	public void setUpperValue(Integer value) {
		this.upperValue = value;
	}
	@Override
	public String getUpperUnits() {
		return this.upperUnits;
	}
	
	@Override
	public void setUpperUnits(String units) {
		this.upperUnits = units;
	}
	
	@Override
	public boolean getUpperInclusive() {
		return this.upperInclusive;
	}
	
	@Override
	public void setUpperInclusive(boolean inclusive) {
		this.upperInclusive = inclusive;
	}
	
	@Override
	@XmlElement(name = "hasLowerBound")
	public boolean hasLowerBound() {
		return this.lowerValue == -1;
	}

	@Override
	@XmlElement(name = "hasUpperBound")
	public boolean hasUpperBound() {
		return this.upperValue == -1;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (lowerInclusive ? 1231 : 1237);
		result = prime * result
				+ ((lowerUnits == null) ? 0 : lowerUnits.hashCode());
		result = prime * result
				+ ((lowerValue == null) ? 0 : lowerValue.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (upperInclusive ? 1231 : 1237);
		result = prime * result
				+ ((upperUnits == null) ? 0 : upperUnits.hashCode());
		result = prime * result
				+ ((upperValue == null) ? 0 : upperValue.hashCode());
		return result;
	}

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

	@Override
	public String toString() {
		return "MapAgeRangeJpa [name=" + name + ", lowerValue=" + lowerValue
				+ ", lowerUnits=" + lowerUnits + ", lowerInclusive="
				+ lowerInclusive + ", upperValue=" + upperValue
				+ ", upperUnits=" + upperUnits + ", upperInclusive="
				+ upperInclusive + "]";
	}


	
	

	
}


