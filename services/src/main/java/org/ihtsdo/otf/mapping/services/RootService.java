package org.ihtsdo.otf.mapping.services;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * The Interface RootService. Manages Factory and lucene field names
 */
public interface RootService {

	/**
	 * Open the factory
	 */
	public void openFactory();
	
	/**
	 * Close the factory
	 */
	public void closeFactory();
	
	/**
	 * Initialize field names.
	 */
	public void initializeFieldNames();
}
