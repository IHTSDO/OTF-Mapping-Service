package org.ihtsdo.otf.mapping.services;


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


}
