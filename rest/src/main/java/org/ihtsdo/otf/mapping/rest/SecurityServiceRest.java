/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.Consumes;
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
    @ApiParam(value = "Password, as string post data", required = true) String password) {

    Logger.getLogger(SecurityServiceRest.class).info(
        "RESTful call (Authentication): /authentication for map user = "
            + username);
    try {
      SecurityService securityService = new SecurityServiceJpa();
      return securityService.authenticate(username, password);
    } catch (LocalException e) {
      Logger.getLogger(SecurityServiceRest.class).error(
          "Local exception thrown");
      throw new WebApplicationException(Response.status(401)
          .entity(e.getMessage()).build());
    } catch (Exception e) {
      Logger.getLogger(SecurityServiceRest.class).error(
          "General exception thrown");
      handleException(e, "Unexpected error trying to authenticate a map user");
      return null;
    }

  }

  /**
   * Authenticate.
   *
   * @param userName the user name
   * @return the string
   */
  @POST
  @Path("/logout/user/id/{userName}")
  @ApiOperation(value = "Log out.", notes = "Logs a map user out of the tool.", response = String.class)
  public String logout(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName) {

    Logger.getLogger(SecurityServiceRest.class).info(
        "RESTful call (Logout) : /logout/user/id/" + userName);
    try {
      SecurityService securityService = new SecurityServiceJpa();
      securityService.logout(userName);
      securityService.close();
      return null;
    } catch (LocalException e) {
      Logger.getLogger(SecurityServiceRest.class).error(
          "Local exception thrown");
      throw new WebApplicationException(Response.status(401)
          .entity(e.getMessage()).build());
    } catch (Exception e) {
      Logger.getLogger(SecurityServiceRest.class).error(
          "General exception thrown");
      handleException(e, "Unexpected error trying to authenticate a map user");
    }
    return null;
  }
}