package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum MapRefsetPattern specifying what map refset pattern a map project uses
 */

public enum MapRefsetPattern {
  
	ExtendedMap("Extended Map"),
	
	ComplexMap("Complex Map"),
	
	SimpleMap("Simple Map");
	
	private String displayName = null;
	private MapRefsetPattern(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName() { return displayName; }
}
