/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
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
public class OAuth2SecurityServiceHandler implements SecurityServiceHandler {

  /** The properties. */
  @SuppressWarnings("unused")
  private Properties properties;

  /* see superclass */
  @Override
  public MapUser authenticate(String userName, String password)
    throws Exception {

    if (StringUtils.isAnyBlank(userName)) {
      throw new Exception("Authentication Error - userName cannot be null or empty.");
    }
    
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
    
    // {
    // "@odata.context":"https://graph.microsoft.com/v1.0/$metadata#users/$entity",
    //    "id":"12345678-73a6-4952-a53a-e9916737ff7f",
    //    "businessPhones":[
    //        "+1 555555555"
    //    ],
    //    "displayName":"Chris Green",
    //    "givenName":"Chris",
    //    "jobTitle":"Software Engineer",
    //    "mail":null,
    //    "mobilePhone":"+1 5555555555",
    //    "officeLocation":"Seattle Office",
    //    "preferredLanguage":null,
    //    "surname":"Green",
    //    "userPrincipalName":"ChrisG@contoso.onmicrosoft.com"
    // }

    // Construct user from document
    MapUser user = new MapUserJpa();
    user.setName(doc.get("givenName").asText() + " "
        + doc.get("surname").asText());
    user.setUserName(doc.get("userPrincipalName").asText());
    user.setEmail(doc.get("userPrincipalName").asText());
    user.setApplicationRole(MapUserRole.VIEWER);
    user.setAuthToken(doc.get("access_token").asText());
    
    // Iterator<JsonNode> iter = doc.get("roles").elements();
    // while (iter.hasNext()) {
    // JsonNode role = iter.next();
    // if (role.asText().equals("ROLE_mapping-administrators")) {
    // user.setApplicationRole(MapUserRole.ADMINISTRATOR);
    // }
    // // if (role.asText().equals("ROLE_mapping-users")) {
    // // user.setApplicationRole(MapUserRole.USER);
    // // }
    // }

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
