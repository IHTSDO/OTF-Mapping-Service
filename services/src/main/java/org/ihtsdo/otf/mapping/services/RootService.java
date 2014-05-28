package org.ihtsdo.otf.mapping.services;

/**
 * The Interface RootService. Manages Factory and lucene field names
 */
public interface RootService {

	/**
	 * Open the factory
	 * @throws Exception 
	 */
	public void openFactory() throws Exception;
	
	/**
	 * Close the factory
	 * @throws Exception 
	 */
	public void closeFactory() throws Exception;
	
	/**
	 * Initialize field names.
	 * @throws Exception 
	 */
	public void initializeFieldNames() throws Exception;
}
