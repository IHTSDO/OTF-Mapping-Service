package org.ihtsdo.otf.mapping.services;

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
