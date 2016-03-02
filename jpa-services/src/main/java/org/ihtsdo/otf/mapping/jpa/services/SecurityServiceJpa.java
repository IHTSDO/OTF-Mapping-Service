package org.ihtsdo.otf.mapping.jpa.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
   * @throws Exception the exception
   */
  public SecurityServiceJpa() throws Exception {
    super();
  }

  /* see superclass */
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

    // Handle guest user
    boolean ihtsdoSecurityActivated =
        !config.getProperty("ihtsdo.security.activated").equals("false");

    // if ihtsdo security is off, use username as token
    if (!ihtsdoSecurityActivated || username.equals("guest")) {
      tokenUsernameMap.put(username, username);
      tokenLoginMap.put(username, new Date());
      MappingService mappingService = new MappingServiceJpa();
      try {
        mappingService.getMapUser(username);
      } catch (Exception e) {
        mappingService.close();
        throw new LocalException("Unable to find map user for username", "401");
      }
      mappingService.close();
      return username;
    }    
    
    // Use ihtsdo.security.activated as a switch
    // - false or true = ihtsdo
    // uts = uts
    if (config.getProperty("ihtsdo.security.activated") != null
        && config.getProperty("ihtsdo.security.activated").equals("uts")) {
      return utsAuthenticate(username, password);
    } else {
      return ihtsdoAuthenticate(username, password);
    }
  }

  /**
   * IHTSDO authenticate.
   *
   * @param username the username
   * @param password the password
   * @return the string
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private String ihtsdoAuthenticate(String username, String password)
    throws Exception {

    String ihtsdoSecurityUrl = config.getProperty("ihtsdo.security.url");

    // Use ihtsdo.security.activated as a switch
    // - false or true = ihtsdo
    // uts = uts
    if (config.getProperty("ihtsdo.security.activated") != null
        && config.getProperty("ihtsdo.security.activated").equals("uts")) {
      return utsAuthenticate(username, password);
    } else {
      return ihtsdoAuthenticate(username, password);
    }
  }

  /**
   * IHTSDO authenticate.
   *
   * @param username the username
   * @param password the password
   * @return the string
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private String ihtsdoAuthenticate(String username, String password)
    throws Exception {

    String ihtsdoSecurityUrl = config.getProperty("ihtsdo.security.url");

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

  /**
   * UTS authenticate.
   *
   * @param username the username
   * @param password the password
   * @return the string
   * @throws Exception the exception
   */
  @SuppressWarnings("unused")
  private String utsAuthenticate(String username, String password)
    throws Exception {
    final String utsSecurityUrl = config.getProperty("ihtsdo.security.url");
    final String licenseCode =
        config.getProperty("ihtsdo.security.license.code");
    if (licenseCode == null) {
      throw new Exception("License code must be specified.");
    }
    if (licenseCode == null) {
      throw new Exception("Security URL must be specified.");
    }

    String data =
        URLEncoder.encode("licenseCode", "UTF-8") + "="
            + URLEncoder.encode(licenseCode, "UTF-8");
    data +=
        "&" + URLEncoder.encode("user", "UTF-8") + "="
            + URLEncoder.encode(username, "UTF-8");
    data +=
        "&" + URLEncoder.encode("password", "UTF-8") + "="
            + URLEncoder.encode(password, "UTF-8");

    Logger.getLogger(getClass()).debug(data);
    URL url = new URL(utsSecurityUrl);
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
    wr.write(data);
    wr.flush();

    BufferedReader rd =
        new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    boolean authenticated = false;
    while ((line = rd.readLine()) != null) {
      Logger.getLogger(getClass()).debug(line);
      if (line.toLowerCase().contains("true")) {
        authenticated = true;
      }
    }
    wr.close();
    rd.close();

    if (!authenticated) {
      throw new LocalException("Incorrect user name or password.");
    }

    /*
     * Synchronize the information sent back from ITHSDO with the MapUser
     * object. Add a new map user if there isn't one matching the username If
     * there is, load and update that map user and save the changes
     */
    String userName = username;
    String email = "test@example.com";
    String givenName = "UTS User - " + username;
    String surname = "";

    // check if ihtsdo user matches one of our MapUsers
    MappingService mappingService = new MappingServiceJpa();
    MapUserList userList = mappingService.getMapUsers();
    MapUser userFound = null;
    for (MapUser user : userList.getMapUsers()) {
      if (user.getUserName().equals(userName)) {
        userFound = user;
        break;
      }
    }
    // if MapUser not found, add it (as a viewer)
    if (userFound == null) {
      MapUser newMapUser = new MapUserJpa();
      newMapUser.setName(givenName + " " + surname);
      newMapUser.setUserName(userName);
      newMapUser.setEmail(email);
      newMapUser.setApplicationRole(MapUserRole.VIEWER);
      mappingService.addMapUser(newMapUser);
    }
    mappingService.close();

    // Generate application-managed token
    String token = UUID.randomUUID().toString();
    tokenUsernameMap.put(token, userName);
    tokenLoginMap.put(token, new Date());

    Logger.getLogger(this.getClass()).info("User = " + username);

    return token;
  }

  /* see superclass */
  @Override
  public void logout(String userName) throws Exception {

    if (userName == null || userName.isEmpty())
      throw new LocalException("No user specified for logout", "401");

    // remove this user name from the security service maps
    tokenUsernameMap.remove(userName);
    tokenLoginMap.remove(userName);

  }

  /* see superclass */
  @SuppressWarnings("unused")
  @Override
  public String getUsernameForToken(String authToken) throws Exception {

    // if this authToken consists only of the string "false", then most likely
    // browser security settings are not properly configured
    if (authToken.equals("false")) {
      throw new LocalException(
          "Could not authenticate requests.  This is most likely to the Tool not being able to access your local cache.  Check that cookies are enabled in your browser and try again.");
    }

    // bypass steps below and don't login
    if (authToken.equals("guest"))
      return "guest";

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

  /* see superclass */
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

  /* see superclass */
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
