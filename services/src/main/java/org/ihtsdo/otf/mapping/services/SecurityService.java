/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;

/**
 * We want the web application to avoid needing to know anything about the
 * details of the security implementation (e.g. service URL, technology, etc).
 * The solution is to build a layer around security WITHIN our own service layer
 * where we can inject any security solution we want into the background.
 * 
 */
public interface SecurityService extends RootService {

  /**
   * Authenticate the user.
   * 
   * @param userName the userName
   * @param password the password
   * @return the token
   * @throws Exception the exception
   */
  public MapUser authenticate(String userName, String password)
    throws Exception;

  /**
   * Logout.
   *
   * @param authToken the auth token
   * @return the string
   * @throws Exception the exception
   */
  public String logout(String authToken) throws Exception;

  /**
   * Returns the userName for token.
   * 
   * @param authToken the auth token
   * @return the userName for token
   * @throws Exception the exception
   */
  public String getUsernameForToken(String authToken) throws Exception;

  /**
   * Returns the application role for token.
   * 
   * @param authToken the auth token
   * @return the application role
   * @throws Exception the exception
   */
  public MapUserRole getApplicationRoleForToken(String authToken)
    throws Exception;

  /**
   * Returns the application role for token.
   *
   * @param authToken the auth token
   * @param projectId the project id
   * @return the application role
   * @throws Exception the exception
   */
  public MapUserRole getMapProjectRoleForToken(String authToken, Long projectId)
    throws Exception;

  /**
   * Get user by id.
   * @param id the id
   *
   * @return the user
   * @throws Exception the exception
   */
  public MapUser getMapUser(Long id) throws Exception;

  /**
   * Get user by user.
   *
   * @param userName the userName
   * @return the user
   * @throws Exception the exception
   */
  public MapUser getMapUser(String userName) throws Exception;

  /**
   * Returns the users.
   *
   * @return the users
   */
  public MapUserList getMapUsers();

  /**
   * Adds the user.
   *
   * @param user the user
   * @return the user
   */
  public MapUser addMapUser(MapUser user);

  /**
   * Removes the user.
   *
   * @param id the id
   */
  public void removeMapUser(Long id);

  /**
   * Update user.
   *
   * @param user the user
   */
  public void updateMapUser(MapUser user);

  /**
   * Find users.
   *
   * @param query the query
   * @param pfs the pfs
   * @return the user list
   * @throws Exception the exception
   */
  public MapUserList findMapUsersForQuery(String query, PfsParameter pfs)
    throws Exception;

  /**
   * Adds the user preferences.
   *
   * @param userPreferences the user preferences
   * @return the userPreferences
   */
  public MapUserPreferences addMapUserPreferences(
    MapUserPreferences userPreferences);

  /**
   * Removes user preferences.
   *
   * @param id the id
   */
  public void removeMapUserPreferences(Long id);

  /**
   * Update user preferences.
   *
   * @param userPreferences the user preferences
   */
  public void updateMapUserPreferences(MapUserPreferences userPreferences);

  /**
   * Handle lazy init.
   *
   * @param user the user
   */
  public void handleLazyInit(MapUser user);
}
//