package org.ihtsdo.otf.mapping.helpers;


/**
 * The Enum WorkflowPath, specifying which workflow path a particular tracking record is on
 *
 * @author ${author}
 */

public enum WorkflowPath  {

	NON_LEGACY_PATH,
	LEGACY_PATH,
	FIX_ERROR_PATH,
	QA_PATH,
	CONSENSUS_PATH;
}
