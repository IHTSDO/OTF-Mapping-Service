/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import org.ihtsdo.otf.mapping.helpers.Configurable;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Generically represents a handler that can authenticate a user.
 */
public interface SecurityServiceHandler extends Configurable {

  /**
   * Authenticate.
   *
   * @param user the user
   * @param password the password
   * @return the user
   * @throws Exception
   */
  public MapUser authenticate(String user, String password) throws Exception;

  /**
   * Returns the logout url.
   *
   * @return the logout url
   * @throws Exception the exception
   */
  public String getLogoutUrl() throws Exception;

  /**
   * Indicates whether or not the user should be timed out.
   *
   * @param user the user
   * @return true, if successful
   */
  public boolean timeoutUser(String user);

  /**
   * Computes token for user. For example, a UUID or an MD5 or a counter. Each
   * login requires yields a potentially different token, even for the same
   * user.
   *
   * @param user the user
   * @return the string
   */
  public String computeTokenForUser(String user);
}
