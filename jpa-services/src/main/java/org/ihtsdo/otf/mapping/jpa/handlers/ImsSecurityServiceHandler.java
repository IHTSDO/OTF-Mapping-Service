/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.SecurityServiceHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements a security handler that authorizes via IHTSDO authentication.
 */
public class ImsSecurityServiceHandler implements SecurityServiceHandler {

  /** The properties. */
  @SuppressWarnings("unused")
  private Properties properties;

  /* see superclass */
  @Override
  public MapUser authenticate(String userName, String password)
    throws Exception {

    // handle guest user
    if (userName.equals("guest")
        && "false".equals(ConfigUtility.getConfigProperties().getProperty(
            "security.guest.disabled"))) {
      final MapUser user = new MapUserJpa();
      user.setName("Guest");
      user.setApplicationRole(MapUserRole.VIEWER);
      user.setEmail("test@example.com");
      user.setUserName("guest");
      return user;
    }

    // password contains the IMS user document

    ObjectMapper mapper = new ObjectMapper();
    JsonNode doc = mapper.readTree(password);
    Logger.getLogger(getClass()).info("");
    // e.g.
    // {
    // "login": "pgranvold",
    // "password": null,
    // "firstName": "Patrick",
    // "lastName": "Granvold",
    // "email": "***REMOVED***",
    // "langKey": null,
    // "roles": [
    // "ROLE_confluence-users",
    // "ROLE_ihtsdo-ops-admin",
    // "ROLE_ihtsdo-sca-author",
    // "ROLE_ihtsdo-tba-author",
    // "ROLE_ihtsdo-tech-group",
    // "ROLE_ihtsdo-users",
    // "ROLE_jira-developers",
    // "ROLE_jira-users",
    // "ROLE_mapping-dev-team"
    // ]
    // }

    // Construct user from document
    MapUser user = new MapUserJpa();
    user.setName(doc.get("firstName").asText() + " "
        + doc.get("lastName").asText());
    user.setUserName(doc.get("login").asText());
    user.setEmail(doc.get("email").asText());
    user.setApplicationRole(MapUserRole.VIEWER);
    // Not available user.setMobileEmail("");
    Iterator<JsonNode> iter = doc.get("roles").elements();
    while (iter.hasNext()) {
      JsonNode role = iter.next();
      if (role.asText().equals("ROLE_mapping-administrators")) {
        user.setApplicationRole(MapUserRole.ADMINISTRATOR);
      }
      // if (role.asText().equals("ROLE_mapping-users")) {
      // user.setApplicationRole(MapUserRole.USER);
      // }
    }

    return user;
  }

  /* see superclass */
  @Override
  public boolean timeoutUser(String user) {
    // Never timeout user
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

  /* see superclass */
  @Override
  public String getLogoutUrl() throws Exception {
    return ConfigUtility.getConfigProperties().getProperty(
        "security.handler.IMS.url.logout");
  }

}
