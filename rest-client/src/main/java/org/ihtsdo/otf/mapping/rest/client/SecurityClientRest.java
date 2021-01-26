/**
 * Copyright 2017 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.client;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.jpa.services.rest.SecurityServiceRest;

/**
 * A client for connecting to a security REST service.
 */
public class SecurityClientRest extends RootClientRest implements SecurityServiceRest
{

	@Override
	public String authenticate(String username, String password) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String logout(String userName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getConfigProperties() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Response callback() throws Exception {
	  // NOT Used
	  return null;
	}

}
