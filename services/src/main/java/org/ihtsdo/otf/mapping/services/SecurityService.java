package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;


/**
 * We want the web application to avoid needing to know anything about the details
 * of the security implementation (e.g. service URL, technology, etc). The solution 
 * is to build a layer around security WITHIN our own service layer where we can 
 * inject any security solution we want into the background.
 *
 */
public interface SecurityService {


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
	 * Authorize token.
	 *
	 * @param authToken the auth token
	 * @param mapProjectId the map project id
	 * @return the map user role
	 */
	public MapUserRole authorizeToken(String authToken, Long mapProjectId);

	/**
	 * Authorize token in cases where the mapProjectId isn't available or isn't relevant.
	 * These are cases that are visible to all users and role is not relevant.
	 *
	 * @param authToken the auth token
	 * @param mapProjectId the map project id
	 * @return the string
	 */
	public boolean authorizeToken(String authToken);
}
