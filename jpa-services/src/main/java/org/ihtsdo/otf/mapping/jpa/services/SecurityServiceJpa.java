package org.ihtsdo.otf.mapping.jpa.services;

import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.naming.AuthenticationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

/**
 * Reference implementation of the {@link SecurityService}.
 * 
 * @author ${author}
 */
public class SecurityServiceJpa implements SecurityService {


  /**  The token username map. */
  private static Map<String, String> tokenUsernameMap = new HashMap<>(); 
  
  /**  The token login time map. */
  private static Map<String, Date> tokenLoginMap = new HashMap<>(); 

  /**
   * Instantiates an empty {@link SecurityServiceJpa}.
   */
  public SecurityServiceJpa() {
    // do nothing
  }


	@SuppressWarnings("unchecked")
	@Override
	public String authenticate(String username, String password) throws Exception {
		
		// read ihtsdo security url and active status from config file
		String configFileName = System.getProperty("run.config");
    Logger.getLogger(this.getClass()).info("  run.config = " + configFileName);
    
    Properties config = new Properties();
    FileReader in = new FileReader(new File(configFileName)); 
    config.load(in);
    String ihtsdoSecurityUrl = config.getProperty("ihtsdo.security.url");
    boolean ihtsdoSecurityActivated = new Boolean(config.getProperty("ihtsdo.security.activated"));
    in.close();
    
    // if ihtsdo security is off, use username as token
    if (!ihtsdoSecurityActivated || username.equals("guest")) {
  		tokenUsernameMap.put(username, username);
  		MappingService mappingService = new MappingServiceJpa();
  		MapUser mapUser =  mappingService.getMapUser(username);
  		mappingService.close();
  		if (mapUser == null) {
		    throw new AuthenticationException("Incorrect user name or password.");
  		}
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
			  Logger.getLogger(this.getClass()).info("Success! " + response.getStatus());
		    resultString = response.getEntity(String.class);
		    Logger.getLogger(this.getClass()).info(resultString);
		} else {
			  Logger.getLogger(this.getClass()).info("ERROR! " + response.getStatus()); 
		    resultString = response.getEntity(String.class);   
			  Logger.getLogger(this.getClass()).info(resultString);
			  throw new AuthenticationException("Incorrect user name or password.");
		}
		

    /*Synchronize the information sent back from ITHSDO with the MapUser object.
        Add a new map user if there isn't one matching the username
        If there is, load and update that map user and save the changes*/
		String ihtsdoUserName = "";
		String ihtsdoEmail = "";
		String ihtsdoGivenName = "";
		String ihtsdoSurname = "";
		//TODO: need to somehow retrieve application role for user
		MapUserRole ihtsdoApplicationRole = MapUserRole.VIEWER;
		
	  //converting json to Map
		byte[] mapData = resultString.getBytes(); 
		Map<String, HashMap<String, String>> jsonMap = new HashMap<String, HashMap<String, String>>();
		 
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
			userFound.setApplicationRole(ihtsdoApplicationRole);
			mappingService.updateMapUser(userFound);
		// if MapUser not found, create one for our use
		} else {
			MapUser newMapUser = new MapUserJpa();
			newMapUser.setName(ihtsdoGivenName + " " + ihtsdoSurname);
			newMapUser.setUserName(ihtsdoUserName);
			newMapUser.setEmail(ihtsdoEmail);
			newMapUser.setApplicationRole(ihtsdoApplicationRole);
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


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.SecurityService#getUsernameForToken(java.lang.String)
	 */
	@Override
	public String getUsernameForToken(String authToken) throws Exception {
		if (tokenUsernameMap.containsKey(authToken))
			return tokenUsernameMap.get(authToken);
		else
			throw new Exception("AuthToken does not have a valid username.");
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.SecurityService#authorizeToken(java.lang.String, java.lang.Long)
	 */
	@Override
	public MapUserRole getMapProjectRoleForToken(String authToken, Long mapProjectId) {
		try {
			
			// TODO Fix this
			String parsedToken = authToken.replace("\"", "");
			System.out.println("parsedToken:  " + parsedToken);
			
			String username = getUsernameForToken(parsedToken);
			MappingService mappingService = new MappingServiceJpa();
			MapUserRole result = mappingService
					.getMapUserRoleForMapProject(username, mapProjectId);
			mappingService.close();
			return result;
		} catch (Exception e) {
			throw new WebApplicationException(Response.status(401).entity(e.getMessage()).build());
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.SecurityService#authorizeToken(java.lang.String)
	 */
	@Override
	public MapUserRole getApplicationRoleForToken(String authToken) {
		try {
			String parsedToken = authToken.replace("\"", "");
			System.out.println("parsedToken:  " + parsedToken);
			
			String username = getUsernameForToken(parsedToken);
			MappingService mappingService = new MappingServiceJpa();
			MapUser user = mappingService
					.getMapUser(username.toLowerCase());
			mappingService.close();
			
			return user.getApplicationRole();
		} catch (Exception e) {
			throw new WebApplicationException(Response.status(401).entity(e.getMessage()).build());
		}
	}
}
