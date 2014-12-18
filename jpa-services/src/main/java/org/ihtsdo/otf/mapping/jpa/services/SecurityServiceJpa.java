package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

/**
 * Reference implementation of the {@link SecurityService}.
 */
public class SecurityServiceJpa extends RootServiceJpa implements
    SecurityService {

  /** The token username map. */
  private static Map<String, String> tokenUsernameMap = Collections
      .synchronizedMap(new HashMap<String, String>());

  /** The token login time map. */
  private static Map<String, Date> tokenLoginMap = Collections
      .synchronizedMap(new HashMap<String, Date>());

  /** The config. */
  private static Properties config = null;

  /**
   * Instantiates an empty {@link SecurityServiceJpa}.
   * 
   * @throws Exception
   */
  public SecurityServiceJpa() throws Exception {
    super();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#initializeFieldNames()
   */
  @Override
  public void initializeFieldNames() throws Exception {
    // no need
  }

  @SuppressWarnings("unchecked")
  @Override
  public String authenticate(String username, String password) throws Exception {
    if (username == null)
      throw new LocalException("Invalid username: null");
    if (password == null)
      throw new LocalException("Invalid password: null");

    // read ihtsdo security url and active status from config file
    if (config == null) {
      config = ConfigUtility.getConfigProperties();
    }
    String ihtsdoSecurityUrl = config.getProperty("ihtsdo.security.url");
    boolean ihtsdoSecurityActivated =
        new Boolean(config.getProperty("ihtsdo.security.activated"));

    // if ihtsdo security is off, use username as token
    if (!ihtsdoSecurityActivated || username.equals("guest")) {
      tokenUsernameMap.put(username, username);
      tokenLoginMap.put(username, new Date());
      MappingService mappingService = new MappingServiceJpa();
      mappingService.getMapUser(username);
      return username;
    }

    // set up request to be posted to ihtsdo security service
    Form form = new Form();
    form.add("username", username);
    form.add("password", password);
    form.add("queryName", "getUserByNameAuth");

    Client client = Client.create();
    WebResource resource = client.resource(ihtsdoSecurityUrl);

    resource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

    ClientResponse response = resource.post(ClientResponse.class, form);

    String resultString = "";
    if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
      resultString = response.getEntity(String.class);
    } else {
      // TODO Differentiate error messages with NO RESPONSE and
      // Authentication Failed (Check text)
      Logger.getLogger(this.getClass()).info("ERROR! " + response.getStatus());
      resultString = response.getEntity(String.class);
      Logger.getLogger(this.getClass()).info(resultString);
      throw new LocalException("Incorrect user name or password.");
    }

    /*
     * Synchronize the information sent back from ITHSDO with the MapUser
     * object. Add a new map user if there isn't one matching the username If
     * there is, load and update that map user and save the changes
     */
    String ihtsdoUserName = "";
    String ihtsdoEmail = "";
    String ihtsdoGivenName = "";
    String ihtsdoSurname = "";

    // converting json to Map
    byte[] mapData = resultString.getBytes();
    Map<String, HashMap<String, String>> jsonMap = new HashMap<>();

    // parse username from json object
    ObjectMapper objectMapper = new ObjectMapper();
    jsonMap = objectMapper.readValue(mapData, HashMap.class);
    for (Entry<String, HashMap<String, String>> entrySet : jsonMap.entrySet()) {
      if (entrySet.getKey().equals("user")) {
        HashMap<String, String> innerMap = entrySet.getValue();
        for (Entry<String, String> innerEntrySet : innerMap.entrySet()) {
          if (innerEntrySet.getKey().equals("name")) {
            ihtsdoUserName = innerEntrySet.getValue();
          } else if (innerEntrySet.getKey().equals("email")) {
            ihtsdoEmail = innerEntrySet.getValue();
          } else if (innerEntrySet.getKey().equals("givenName")) {
            ihtsdoGivenName = innerEntrySet.getValue();
          } else if (innerEntrySet.getKey().equals("surname")) {
            ihtsdoSurname = innerEntrySet.getValue();
          }
        }
      }
    }
    // check if ihtsdo user matches one of our MapUsers
    MappingService mappingService = new MappingServiceJpa();
    MapUserList userList = mappingService.getMapUsers();
    MapUser userFound = null;
    for (MapUser user : userList.getMapUsers()) {
      if (user.getUserName().equals(ihtsdoUserName)) {
        userFound = user;
        break;
      }
    }
    // if MapUser was found, update to match ihtsdo settings
    if (userFound != null) {
      userFound.setEmail(ihtsdoEmail);
      userFound.setName(ihtsdoGivenName + " " + ihtsdoSurname);
      userFound.setUserName(ihtsdoUserName);
      mappingService.updateMapUser(userFound);
      // if MapUser not found, create one for our use
    } else {
      MapUser newMapUser = new MapUserJpa();
      newMapUser.setName(ihtsdoGivenName + " " + ihtsdoSurname);
      newMapUser.setUserName(ihtsdoUserName);
      newMapUser.setEmail(ihtsdoEmail);
      newMapUser.setApplicationRole(MapUserRole.VIEWER);
      mappingService.addMapUser(newMapUser);
    }
    mappingService.close();

    // Generate application-managed token
    String token = UUID.randomUUID().toString();
    tokenUsernameMap.put(token, ihtsdoUserName);
    tokenLoginMap.put(token, new Date());

    Logger.getLogger(this.getClass()).info("User = " + resultString);

    return token;
  }

  @Override
  public void logout(String userName) throws Exception {

    if (userName == null || userName.isEmpty())
      throw new LocalException("No user specified for logout", "401");

    // remove this user name from the security service maps
    tokenUsernameMap.remove(userName);
    tokenLoginMap.remove(userName);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.SecurityService#getUsernameForToken(java
   * .lang.String)
   */
  @Override
  public String getUsernameForToken(String authToken) throws Exception {

    // read ihtsdo security url and active status from config file
    if (config == null) {
      config = ConfigUtility.getConfigProperties();
    }

    if (authToken == null)
      throw new LocalException("You must be logged in to view that page.",
          "401");
    String parsedToken = authToken.replace("\"", "");

    // see if this user has previously been logged in
    if (tokenUsernameMap.containsKey(parsedToken)) {

      // get user name
      String username = tokenUsernameMap.get(parsedToken);

      // get last activity
      Date lastActivity = tokenLoginMap.get(parsedToken);

      String timeout = config.getProperty("ihtsdo.security.timeout");

      // if the timeout parameter has been set
      if (timeout != null && !timeout.isEmpty()) {

        // check timeout against current time minus time of last activity
        if ((new Date()).getTime() - lastActivity.getTime() > Long
            .valueOf(timeout)) {

          Logger.getLogger(SecurityServiceJpa.class).info(
              "Timeout expired for user " + username + ".  Last login at "
                  + lastActivity.toString() + " ("
                  + (new Date().getTime() - lastActivity.getTime())
                  + " ms difference)");

          throw new LocalException(
              "Your session has expired.  Please log in again.", "401");
        }
      }

      tokenLoginMap.put(parsedToken, new Date());
      return username;

      // throw exception, this user has attempted to view a page without
      // being logged in
    } else
      throw new LocalException("You must be logged in to view that page.",
          "401");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.SecurityService#authorizeToken(java.lang
   * .String, java.lang.Long)
   */
  @Override
  public MapUserRole getMapProjectRoleForToken(String authToken,
    Long mapProjectId) throws Exception {
    if (authToken == null)
      throw new LocalException("You must be logged in to view that page.",
          "401");
    if (mapProjectId == null)
      throw new Exception("Unexpected null map project id");

    String parsedToken = authToken.replace("\"", "");

    String username = getUsernameForToken(parsedToken);
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole result =
        mappingService.getMapUserRoleForMapProject(username, mapProjectId);
    mappingService.close();
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.SecurityService#authorizeToken(java.lang
   * .String)
   */
  @Override
  public MapUserRole getApplicationRoleForToken(String authToken)
    throws Exception {

    if (authToken == null)
      throw new LocalException("You must be logged in to view that page.",
          "401");
    String parsedToken = authToken.replace("\"", "");

    String username = getUsernameForToken(parsedToken);
    MappingService mappingService = new MappingServiceJpa();
    MapUser user = mappingService.getMapUser(username.toLowerCase());
    mappingService.close();

    return user.getApplicationRole();
  }
}
