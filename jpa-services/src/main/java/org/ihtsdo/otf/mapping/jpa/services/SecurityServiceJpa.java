package org.ihtsdo.otf.mapping.jpa.services;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
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

  /**
   * Instantiates an empty {@link SecurityServiceJpa}.
   */
  public SecurityServiceJpa() {
    // do nothing
  }


	@SuppressWarnings("unchecked")
	@Override
	public String authenticate(String username, String password) throws Exception {
		// read ihtsdo security url from config file
		String configFileName = System.getProperty("run.config");
    Logger.getLogger(this.getClass()).info("  run.config = " + configFileName);
    
    Properties config = new Properties();
    FileReader in = new FileReader(new File(configFileName)); 
    config.load(in);
    String ihtsdoSecurityUrl = config.getProperty("ihtsdo.security.url");
    in.close();
    
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
		

    /*The authenticate method should also synchronize the information sent back from ITHSDO with the MapUser object.
        Add a new map user if there isn't one matching the username
        If there is, load and update that map user and save the changes*/
		String ihtsdoUserName = "";
		String ihtsdoEmail = "";
		String ihtsdoGivenName = "";
		String ihtsdoMiddleName = "";
		String ihtsdoSurname = "";
		
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
				  } else if (innerEntrySet.getKey().equals("middleName")) {
				  	ihtsdoMiddleName = innerEntrySet.getValue();
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
			userFound.setName(ihtsdoGivenName + " " + ihtsdoMiddleName + " " + ihtsdoSurname);
			userFound.setUserName(ihtsdoUserName);
			mappingService.updateMapUser(userFound);
		// if MapUser not found, create one for our use
		} else {
			MapUser newMapUser = new MapUserJpa();
			newMapUser.setName(ihtsdoGivenName + " " + ihtsdoMiddleName + " " + ihtsdoSurname);
			newMapUser.setUserName(ihtsdoUserName);
			newMapUser.setEmail(ihtsdoEmail);
			mappingService.addMapUser(newMapUser);
		}
		mappingService.close();
	
		return resultString;
	}
}
