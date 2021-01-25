/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for security service.
 */
@Path("/security")
@Api(value = "/security", description = "Operations supporting application authentication and authorization.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class SecurityServiceRestImpl extends RootServiceRestImpl implements SecurityServiceRest {

  /** The HTTP request */
  // @Resource
  @Context
  private HttpServletRequest httpServletRequest;
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#authenticate(java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/authenticate/{username}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @Produces({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Authenticate a map user.", notes = "Performs authentication on specified username and password and returns a token upon successful authentication. Throws 401 error if not.", response = String.class)
  public String authenticate(
    @ApiParam(value = "Username", required = true) @PathParam("username") String username,
    @ApiParam(value = "Password, as string post data", required = true) String password)
    throws Exception {
	  
    Logger.getLogger(SecurityServiceRestImpl.class)
        .info("RESTful call (Authentication): /authentication for map user = "
            + username);
    final SecurityService securityService = new SecurityServiceJpa();
    try {
      return securityService.authenticate(username, password).getAuthToken();
    } catch (LocalException e) {
      throw new WebApplicationException(
          Response.status(401).entity(e.getMessage()).build());
    } catch (Exception e) {
      handleException(e, "Unexpected error trying to authenticate a map user");
      return null;
    } finally {
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#logout(java.lang.String)
   */
  @Override
  @POST
  @Path("/logout/user/id/{userName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Log out.", notes = "Logs a map user out of the tool.", response = String.class)
  public String logout(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName)
    throws Exception {

    Logger.getLogger(SecurityServiceRestImpl.class)
        .info("RESTful call (Logout) : /logout/user/id/" + userName);
    final SecurityService securityService = new SecurityServiceJpa();
    try {
      return securityService.logout(userName);
    } catch (LocalException e) {
      throw new WebApplicationException(
          Response.status(401).entity(e.getMessage()).build());
    } catch (Exception e) {
      handleException(e, "Unexpected error trying to authenticate a map user");
    } finally {
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#getConfigProperties()
   */
  @Override
  @GET
  @Path("/properties")
  @Produces({
      MediaType.APPLICATION_JSON
  })
  @ApiOperation(value = "Get configuration properties", notes = "Gets user interface-relevant configuration properties", response = String.class, responseContainer = "Map")
  public Map<String, String> getConfigProperties() {
    Logger.getLogger(getClass())
        .info("RESTful call (Configure): /configure/properties");
    try {
      Map<String, String> map = new HashMap<>();
      for (final Map.Entry<Object, Object> o : ConfigUtility
          .getUiConfigProperties().entrySet()) {
        map.put(o.getKey().toString(), o.getValue().toString());
      }
      return map;
    } catch (Exception e) {
      handleException(e, "getting ui config properties");
    } finally {
      // n/a
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.SecurityServiceRest#getConfigProperties()
   */
  @Override
  //@GET
  @Path("/callback")
  @Produces({
    MediaType.APPLICATION_JSON
  })
  @ApiOperation(value = "", notes = "Handle callback from OAuth2", response = String.class)
  public Response callback() throws Exception {
    Logger.getLogger(getClass()).info("RESTful call /security/callback");
    try {
      
      //Log everything for now
      Logger.getLogger(getClass()).info("REQUEST DETAILS");
      Logger.getLogger(getClass()).info("\tHTTP Method: " + httpServletRequest.getMethod());
      Logger.getLogger(getClass()).info("\tQuery String: " + httpServletRequest.getQueryString());
      httpServletRequest.getHeaderNames().asIterator().forEachRemaining(headerName ->
      {
        Logger.getLogger(getClass()).info("\tHeader: " + headerName + " value: " + httpServletRequest.getHeader(headerName));
      });
      httpServletRequest.getParameterNames().asIterator().forEachRemaining(param ->
      {
        Logger.getLogger(getClass()).info("\tParams: " + param + " value: " + httpServletRequest.getHeader(param));
      });
      
      Logger.getLogger(getClass()).info("\tBody: " + getHttpServletRequestBody(httpServletRequest));
      
      final String body = getHttpServletRequestBody(httpServletRequest);
      
      //TODO: Pass JSON into password field
      authenticate("guest", body);
      
      return Response.ok().build();
    }
    catch(Exception e) {
      handleException(e, "callback");
      return Response.serverError().build();
    }
    finally {
      
    }
  }
  
  //temporary until the callback method knows exactly what it is getting
  private String getHttpServletRequestBody(HttpServletRequest request) { 
    StringBuilder stringBuilder = new StringBuilder();  
    BufferedReader bufferedReader = null;  
  
    try {  
        InputStream inputStream = request.getInputStream(); 
  
        if (inputStream != null) {  
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));  
  
            char[] charBuffer = new char[128];  
            int bytesRead = -1;  
  
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {  
                stringBuilder.append(charBuffer, 0, bytesRead);  
            }  
        } else {  
            stringBuilder.append("");  
        }  
    } catch (IOException ex) {  
      Logger.getLogger(getClass()).error("Error reading the request body...");  
    } finally {  
        if (bufferedReader != null) {  
            try {  
                bufferedReader.close();  
            } catch (IOException ex) {  
              Logger.getLogger(getClass()).error("Error closing bufferedReader...");  
            }  
        }  
    }  
  
    return stringBuilder.toString();  
  }
}
  