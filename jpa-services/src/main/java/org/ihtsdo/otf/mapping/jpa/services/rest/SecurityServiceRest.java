package org.ihtsdo.otf.mapping.jpa.services.rest;

import java.util.Map;

import javax.ws.rs.core.Response;

public interface SecurityServiceRest {

	/**
	   * Authenticate.
	   * 
	   * @param username the username
	   * @param password the password
	   * @return the string
	   * @throws Exception
	   */
	String authenticate(String username, String password) throws Exception;

	/**
	   * Authenticate.
	   *
	   * @param userName the user name
	   * @return the string
	   * @throws Exception
	   */
	String logout(String userName) throws Exception;

	/**
	   * Returns the config properties.
	   *
	   * @return the config properties
	   */
	Map<String, String> getConfigProperties();
	
	
	/**
	 * Callback function for OAuth2 login
	 * 
	 * @return
	 */
	public Response callback() throws Exception;

}