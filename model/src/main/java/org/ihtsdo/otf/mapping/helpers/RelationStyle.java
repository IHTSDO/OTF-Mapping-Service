package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum RelationSytle specifying what type of relation style a map project uses
 */

public enum RelationStyle {
  
	MAP_CATEGORY_STYLE("Map Category Style"),
	
	RELATIONSHIP_STYLE("Relationship Style");
	
	private String displayName = null;
	private RelationStyle(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName() { return displayName; }
}
