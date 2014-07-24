package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum WorkflowType specifying what type of workflow a map project uses
 */

public enum WorkflowType {
  
	/** Two specialists map, lead reviews conflicts */
	CONFLICT_PROJECT,
	
	/** One specialist maps, lead reviews result */
	REVIEW_PROJECT
}
