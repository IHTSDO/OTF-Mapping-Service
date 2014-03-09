package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapAgeRange;

public class MapAgeRangeImpl implements MapAgeRange {
	
	private Long id;
	
	/** The age range preset name */
	private String name;
	
	/** The lower bound parameters */
	private int 	lowerValue;
	private String 	lowerUnits;
	private boolean	lowerInclusive;
	
	/** The upper bound parameters */
	private int		upperValue;
	private String	upperUnits;
	private boolean	upperInclusive;
	
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
	public Long getId() {
		return this.id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
		
	}

	@Override
	public String getObjectId() {
		return this.id.toString();
	}

	@Override
	public boolean hasLowerBound() {
		return this.lowerValue == -1;
	}

	@Override
	public boolean hasUpperBound() {
		return this.upperValue == -1;
	}
	

	
}


