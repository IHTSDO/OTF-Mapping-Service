/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.SecurityServiceHandler;

/**
 * Implements a security handler that authorizes via IHTSDO authentication.
 */
public class DefaultSecurityServiceHandler implements SecurityServiceHandler {

  /** The properties. */
  private Properties properties;

  /* see superclass */
  @Override
  public MapUser authenticate(String userName, String password)
    throws Exception {

    // userName must not be null
    if (userName == null)
      return null;

    // password must not be null
    if (password == null)
      return null;

    // for default security service, the password must equal the user name
    if (!userName.equals(password))
      return null;

    // check properties
    if (properties == null) {
      properties = ConfigUtility.getConfigProperties();
    }

    MapUser user = new MapUserJpa();

    // check specified admin users list from config file
    if (getAdminUsersFromConfigFile().contains(userName)) {
      user.setApplicationRole(MapUserRole.ADMINISTRATOR);
      user.setUserName(userName);
      user.setName(userName.substring(0, 1).toUpperCase()
          + userName.substring(1));
      user.setEmail(userName + "@example.com");
      return user;
    }

    // if (getUserUsersFromConfigFile().contains(userName)) {
    // user.setApplicationRole(MapUserRole.USER);
    // user.setUserName(userName);
    // user.setName(userName.substring(0, 1).toUpperCase()
    // + userName.substring(1));
    // user.setEmail(userName + "@example.com");
    // return user;
    // }

    if (getViewerUsersFromConfigFile().contains(userName)) {
      user.setApplicationRole(MapUserRole.VIEWER);
      user.setUserName(userName);
      user.setName(userName.substring(0, 1).toUpperCase()
          + userName.substring(1));
      user.setEmail(userName + "@example.com");
      return user;
    }

    // if user not specified, return null
    return null;
  }

  /* see superclass */
  @Override
  public boolean timeoutUser(String user) {
    return false;
  }

  /* see superclass */
  @Override
  public String computeTokenForUser(String user) {
    return user;
  }

  /* see superclass */
  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  /**
   * Returns the viewer users from config file.
   *
   * @return the viewer users from config file
   */
  private Set<String> getViewerUsersFromConfigFile() {
    HashSet<String> userSet = new HashSet<>();
    String userList = properties.getProperty("users.viewer");

    if (userList == null) {
      Logger
          .getLogger(getClass())
          .warn(
              "Could not retrieve config parameter users.viewer for security handler DEFAULT");
      return userSet;
    }

    for (String user : userList.split(","))
      userSet.add(user);
    return userSet;
  }

  /**
   * Returns the admin users from config file.
   *
   * @return the admin users from config file
   */
  private Set<String> getAdminUsersFromConfigFile() {

    HashSet<String> userSet = new HashSet<>();
    String userList = properties.getProperty("users.admin");

    Logger.getLogger(getClass()).info(properties.keySet());

    if (userList == null) {
      Logger
          .getLogger(getClass())
          .warn(
              "Could not retrieve config parameter users.admin for security handler DEFAULT");
      return userSet;
    }

    for (String user : userList.split(","))
      userSet.add(user);
    return userSet;
  }

  // /**
  // * Returns the user users from config file.
  // *
  // * @return the user users from config file
  // */
  // private Set<String> getUserUsersFromConfigFile() {
  //
  // HashSet<String> userSet = new HashSet<>();
  // String userList = properties.getProperty("users.user");
  //
  // Logger.getLogger(getClass()).info(properties.keySet());
  //
  // if (userList == null) {
  // Logger
  // .getLogger(getClass())
  // .warn(
  // "Could not retrieve config parameter users.user for security handler DEFAULT");
  // return userSet;
  // }
  //
  // for (String user : userList.split(","))
  // userSet.add(user);
  // return userSet;
  // }

}
