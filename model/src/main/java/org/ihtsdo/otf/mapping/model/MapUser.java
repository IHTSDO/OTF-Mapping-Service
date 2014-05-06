package org.ihtsdo.otf.mapping.model;

/**
 * The interface for MapUser
 * 
 */
public interface MapUser {
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
   * Returns the user name.
   * 
   * @return the user name
   */
  public String getUserName();

  /**
   * Sets the user name.
   * 
   * @param username the user name
   */
  public void setUserName(String username);

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

  /**
   * Returns the email.
   * 
   * @return the email
   */
  public String getEmail();

  /**
   * Sets the email.
   * 
   * @param email the email
   */
  public void setEmail(String email);

}
