package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;

/**
 * Generically represents a service for authorization and authentication.
 * We want the web application to avoid needing to know anything about the
 * details of the security implementation (e.g. service URL, technology, etc).
 * The solution is to build a layer around security WITHIN our own service layer
 * where we can inject any security solution we want into the background.
 */
public interface SecurityService extends RootService {

  /**
   * Authenticate the user.
   * 
   * @param username the username
   * @param password the password
   * @return the token
   * @throws Exception the exception
   */
  public String authenticate(String username, String password) throws Exception;

  /**
   * Returns the username for token.
   * 
   * @param authToken the auth token
   * @return the username for token
   * @throws Exception the exception
   */
  public String getUsernameForToken(String authToken) throws Exception;

  /**
   * Returns the map project role for token.
   * 
   * @param authToken the auth token
   * @param mapProjectId the map project id
   * @return the map project role for token
   * @throws Exception the exception
   */
  public MapUserRole getMapProjectRoleForToken(String authToken,
    Long mapProjectId) throws Exception;

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
   * Logout.
   *
   * @param authToken the auth token
   * @throws Exception the exception
   */
  public void logout(String authToken) throws Exception;
}
