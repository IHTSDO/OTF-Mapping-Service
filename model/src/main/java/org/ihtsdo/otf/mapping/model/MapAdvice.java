package org.ihtsdo.otf.mapping.model;


// TODO: Auto-generated Javadoc
/**
 * Represents a map lead role.
 *
 * @author ${author}
 */
public interface MapAdvice  {
	
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
	 * Returns the id in string form
	 * @return the string object id
	 */
	public String getObjectId();

	/**
	 * Returns the detail.
	 *
	 * @return the detail
	 */
	public String getDetail();
	

	/**
	 * Sets the detail.
	 *
	 * @param detail the detail
	 */
	public void setDetail(String detail);
	
	/**
	 * Returns the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	public void setName(String name);

}
