package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Security service for authentication.
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
    } catch (Exception e) {
      handleException(e, "trying to authenticate a map user");
      return null;
    }

  }
}