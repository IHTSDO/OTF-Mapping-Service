/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.SecurityServiceHandler;

import com.fasterxml.jackson.core.JsonParseException;
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

    // password contains the OAuth2 user document
    // {  "aud":"https://graph.microsoft.com",
    //    "iss":"https://sts.windows.net/c1ef41f7-df50-4222-a5fd-476d69ffe429/"
    //    "iat":1611774348,
    //    "nbf":1611774348,
    //    "exp":1611778248,
    //    "acct":0,
    //    "acr":"1",
    //    "acrs":["urn:user:registersecurityinfo",
    //        "urn:microsoft:req1", "urn:microsoft:req2", "urn:microsoft:req3",
    //        "c1","c2","c3","c4","c5","c6","c7","c8","c9","c10","c11","c12","c13","c14","c15","c16","c17","c18","c19","c20","c21","c22","c23","c24","c25"
    //    ],
    //    "aio":"E2JgYFCP/iYhf6qtikEh/XUvu5jbY1nDjdlH/XZtvp9p8ejrEQ0A",
    //    "amr":["pwd","wia"],
    //    "app_displayname":"WCI MappingTool",
    //    "appid":"f77af9df-3ec3-4c60-ac68-1650ebc35fc2",
    //    "appidacr":"1",
    //    "family_name":"LAST",
    //    "given_name":"FIRST",
    //    "idtyp":"user",
    //    "ipaddr":"192.168.1.1",
    //    "name":"FIRST LAST",
    //    "oid":"7531c39e-8e42-4bb2-8a6d-f068e638b5db",
    //    "onprem_sid":"S-1-5-21-69083081-1352353885-1600587428-14942",
    //    "platf":"3",
    //    "puid":"1003BFFDA96E931D",
    //    "rh":"0.AAAA90HvwVDfIkKl_Udtaf_kKd_5evfDPmBMrGgWUOvDX8JuAII.",
    //    "scp":"User.Read profile openid email",
    //    "signin_state":["inknownntwk"],
    //    "sub":"0iWQxTVc2cbgTBVkJ4K9o1q7_uOBpVMi6MhDvLd3opU",
    //    "tenant_region_scope":"NA",
    //    "tid":"c1ef41f7-df50-4222-a5fd-476d69ffe429",
    //    "unique_name":"************", // email removed
    //    "upn":"************", // email removed
    //    "uti":"AC7kcFhSAkqb8FUwEbiTAA",
    //    "ver":"1.0",
    //    "wids":["b79fbf4d-3ef9-4689-8143-76b194e85509"],
    //    "xms_st":{"sub":"tJj-QCTaB2IeQJTVoRVq6oKqqYN0iN9CGcmbA6Yc12g"},
    //    "xms_tcdt":1469199133,
    //    "access_token": "eyJ0eXAiOiJKV1QiLCJub25jZSI6IlYyMUxubGdH...."
    // }

    MapUser user = new MapUserJpa();
    
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode doc = mapper.readTree(password);

      // Construct user from document
      user.setName(doc.get("name").asText());
      user.setUserName(userName);
      user.setEmail(doc.get("upn").asText());
      user.setApplicationRole(MapUserRole.VIEWER);
      user.setAuthToken(doc.get("access_token").asText());
      
      return user;
    
    } catch (JsonParseException e) {
      
      //check if user is allowed.
      final String userAccessKey = ConfigUtility.getConfigProperties().getProperty("security.handler.OAUTH2.users.key");
      
      // create mapUser
      if (getUsersFromConfigFile("security.handler.OAUTH2.users.admin").contains(userName)
          && userAccessKey.equals(password)) {
        user.setName(userName);
        user.setEmail(userName + "@example.com");
        user.setUserName(userName);
        user.setApplicationRole(MapUserRole.ADMINISTRATOR);
        user.setAuthToken(userName);

        return user;
      }
      else if (getUsersFromConfigFile("security.handler.OAUTH2.users.viewer").contains(userName)
          && userAccessKey.equals(password)) {
        user.setName(userName);
        user.setEmail(userName + "@example.com");
        user.setUserName(userName);
        user.setApplicationRole(MapUserRole.VIEWER);
        user.setAuthToken(userName);

        return user;
      }
    }
    
    throw new LocalException("Unable to authenticate user = " + userName);
    
  }
  
  /* see superclass */
  @Override
  public boolean timeoutUser(String user) {
    // Never timeout user
    return true;
  }

  /* see superclass */
  @Override
  public String computeTokenForUser(MapUser mapUser) {
    return mapUser.getAuthToken();
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
    
  /**
   * Returns the users from config file.
   *
   * @return the users from config file
   */
  private Set<String> getUsersFromConfigFile(String propertyName) throws Exception {

    HashSet<String> userSet = new HashSet<>();
    String userList = ConfigUtility.getConfigProperties().getProperty(propertyName);

    Logger.getLogger(getClass()).info(ConfigUtility.getConfigProperties().keySet());

    if (userList == null) {
      Logger
          .getLogger(getClass())
          .warn(
              "Could not retrieve config parameter " + propertyName);
      return userSet;
    }

    for (String user : userList.split(","))
      userSet.add(user);
    return userSet;
  }

}
