package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum WorkflowType specifying what type of workflow a map project uses
 */

public enum WorkflowType {
  
	/** Two specialists map, lead reviews conflicts */
	CONFLICT_PROJECT("Conflict Project"),
	
	/** One specialist maps, lead reviews result */
	REVIEW_PROJECT("Review Project");
	
	private String displayName = null;
	private WorkflowType(String displayName) {
		this.displayName = displayName;
	}
	public String getDisplayName() {return displayName;}
}
