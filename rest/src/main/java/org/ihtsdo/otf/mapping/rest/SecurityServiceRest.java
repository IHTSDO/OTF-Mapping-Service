package org.ihtsdo.otf.mapping.rest;

import javax.naming.AuthenticationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Security service for authentication.
 */
@Path("/security")
@Api(value = "/security", description = "Operations supporting security and authentication.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class SecurityServiceRest {

  /** The security service. */
  private SecurityService securityService = new SecurityServiceJpa();

  @POST
  @Path("/authenticate/{username}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Authenticates a map user", notes = "Performs authentication on a map user", response = SearchResultJpa.class)
  public String authenticate(
		@ApiParam(value = "username", required = true) @PathParam("username") String username,
		@ApiParam(value = "password", required = true) String password) {

    Logger.getLogger(SecurityServiceRest.class).info(
        "RESTful call (Authentication): /authentication for map user = " + username);

    try {
    	// TODO: return real token when that is available
    	// TODO: when real users are available, don't hardcode "bob"
      String resultString = securityService.authenticate("bob", password);
      return "TOKEN"; //resultString;
    } catch (AuthenticationException e) {
    	throw new WebApplicationException(Response.status(401).entity(e.getMessage()).build());
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }
}