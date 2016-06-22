/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.model;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;

/**
 * Generically represents an application user.
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
   * Returns the id in string form.
   *
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

  /**
   * Returns the application role.
   *
   * @return the application role
   */
  public MapUserRole getApplicationRole();

  /**
   * Sets the application role.
   *
   * @param role the application role
   */
  public void setApplicationRole(MapUserRole role);

  /**
   * Returns the team.
   *
   * @return the team
   */
  public String getTeam();

  /**
   * Sets the team.
   *
   * @param team the team
   */
  public void setTeam(String team);

  /**
   * Returns the auth token.
   *
   * @return the auth token
   */
  public String getAuthToken();

  /**
   * Sets the auth token.
   *
   * @param authToken the auth token
   */
  public void setAuthToken(String authToken);
}
