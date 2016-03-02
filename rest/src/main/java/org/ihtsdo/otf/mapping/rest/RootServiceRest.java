package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.OtfErrorHandler;

/**
 * Top level class for all REST services.
 */
public class RootServiceRest {

  /**
   * Handle exception.
   *
   * @param e the e
   * @param whatisHappening the whatis happening
   */
  public void handleException(Exception e, String whatisHappening) {
    handleException(e, whatisHappening, "", "", "");
  }

  /**
   * Handle exceptions via the Error Handler Class
   *
   * @param e the e
   * @param whatIsHappening the what is happening
   * @param userName the user name
   * @param project the project
   * @param objectdId the objectd id
   */
  @SuppressWarnings("static-method")
  public void handleException(Exception e, String whatIsHappening,
    String userName, String project, String objectdId) {
    OtfErrorHandler errorHandler = new OtfErrorHandler();

    errorHandler.handleException(e, whatIsHappening, userName, project,
        objectdId);
  }

  /**
   * Authorize.
   *
   * @param authToken the auth token
   * @param requiredRole the required role
   * @param operation the operation
   * @param service the service
   * @return the map user role
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public String authorizeApp(String authToken, MapUserRole requiredRole,
    String operation, SecurityService service) throws Exception {
    // authorize call
    final MapUserRole role = service.getApplicationRoleForToken(authToken);
    final String user = service.getUsernameForToken(authToken);
    if (!role.hasPrivilegesOf(requiredRole))
      throw new WebApplicationException(Response.status(401)
          .entity("User does not have permissions to " + operation).build());
    return user;
  }

  /**
   * Authorize project.
   *
   * @param projectId the project id
   * @param authToken the auth token
   * @param requiredRole the required role
   * @param operation the operation
   * @param service the service
   * @return the string
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public String authorizeProject(Long projectId, String authToken,
    MapUserRole requiredRole, String operation, SecurityService service)
    throws Exception {
    // authorize call
    final MapUserRole role =
        service.getMapProjectRoleForToken(authToken, projectId);
    final String user = service.getUsernameForToken(authToken);
    if (!role.hasPrivilegesOf(requiredRole)
        && service.getApplicationRoleForToken(authToken) != MapUserRole.ADMINISTRATOR) {
      throw new WebApplicationException(Response.status(401)
          .entity("User does not have permissions to " + operation).build());
    }
    return user;
  }
  
}
