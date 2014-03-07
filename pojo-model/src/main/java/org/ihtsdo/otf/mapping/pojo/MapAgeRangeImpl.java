package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapAgeRange;

public class MapAgeRangeImpl implements MapAgeRange {
	
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
	public int getLowerValue() {
		return this.lowerValue;
	}
	
	@Override
	public void setLowerValue(int value) {
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
	public int getUpperValue() {
		return this.upperValue;
	}
	
	@Override
	public void setUpperValue(int value) {
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
	

	
}


