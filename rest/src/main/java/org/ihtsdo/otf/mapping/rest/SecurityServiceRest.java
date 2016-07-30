/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for security service.
 */
@Path("/security")
@Api(value = "/security", description = "Operations supporting application authentication and authorization.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class SecurityServiceRest extends RootServiceRest {

  /**
   * Authenticate.
   * 
   * @param username the username
   * @param password the password
   * @return the string
   * @throws Exception
   */
  @POST
  @Path("/authenticate/{username}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Authenticate a map user.", notes = "Performs authentication on specified username and password and returns a token upon successful authentication. Throws 401 error if not.", response = String.class)
  public String authenticate(
    @ApiParam(value = "Username", required = true) @PathParam("username") String username,
    @ApiParam(value = "Password, as string post data", required = true) String password)
    throws Exception {

    Logger.getLogger(SecurityServiceRest.class)
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

  /**
   * Authenticate.
   *
   * @param userName the user name
   * @return the string
   * @throws Exception
   */
  @POST
  @Path("/logout/user/id/{userName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Log out.", notes = "Logs a map user out of the tool.", response = String.class)
  public String logout(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName)
    throws Exception {

    Logger.getLogger(SecurityServiceRest.class)
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

  /**
   * Returns the config properties.
   *
   * @return the config properties
   */
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
}