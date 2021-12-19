package org.ihtsdo.otf.mapping.helpers;

/**
 * Enums for workflow paths
 */
public enum WorkflowPath {
	
	/** The simple path */
	SIMPLE_PATH,

  /** The non legacy path. */
  NON_LEGACY_PATH,

  /** The legacy path. */
  LEGACY_PATH,

  /** The conflict and review path. */
  CONFLICT_AND_REVIEW_PATH,
  
  /** The conditional review path. */
  CONDITIONAL_REVIEW_PATH,
    
  /** The Project Review path */
  REVIEW_PROJECT_PATH,

  /** The fix error path. */
  FIX_ERROR_PATH,

  /** The qa path. */
  QA_PATH,

}
