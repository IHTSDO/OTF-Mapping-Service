package org.ihtsdo.otf.mapping.rf2;


// TODO: Auto-generated Javadoc
/**
 * Represents a tree position.
 *
 */
public interface TreePosition {

	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();


	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the terminology.
	 *
	 * @return the terminology
	 */
	public String getTerminology();

	/**
	 * Sets the terminology.
	 *
	 * @param terminology the terminology
	 */
	public void setTerminology(String terminology);
	
	/**
	 * Returns the terminology version.
	 *
	 * @return the terminology version
	 */
	public String getTerminologyVersion();

	/**
	 * Sets the terminology version.
	 *
	 * @param terminologyVersion the terminology version
	 */
	public void setTerminologyVersion(String terminologyVersion);
	

	/**
	 * Returns the ancestor path. 
	 * This is a "." separated list of terminology IDs starting with the root.
	 *
	 * @return the ancestor path
	 */
	public String getAncestorPath();


	/**
	 * Sets the ancestor path.
	 *
	 * @param path the ancestor path
	 */
	public void setAncestorPath(String path);

  /**
   * Returns the concept id.
   *
   * @return the concept id
   */
  public String getConceptId();
  
  /**
   * Sets the concept id.
   *
   * @param conceptId the concept id
   */
  public void setConceptId(String conceptId);
}
