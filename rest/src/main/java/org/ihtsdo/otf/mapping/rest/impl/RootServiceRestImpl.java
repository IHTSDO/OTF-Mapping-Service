package org.ihtsdo.otf.mapping.rest.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.RootServiceRest;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.OtfErrorHandler;

/**
 * Top level class for all REST services.
 */
public class RootServiceRestImpl implements RootServiceRest {

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#handleException(java.lang.Exception, java.lang.String)
   */
  @Override
  public void handleException(Exception e, String whatisHappening) {
    handleException(e, whatisHappening, "", "", "");
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#handleException(java.lang.Exception, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @SuppressWarnings("static-method")
  public void handleException(Exception e, String whatIsHappening,
    String userName, String project, String objectdId) {
    OtfErrorHandler errorHandler = new OtfErrorHandler();

    errorHandler.handleException(e, whatIsHappening, userName, project,
        objectdId);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#authorizeApp(java.lang.String, org.ihtsdo.otf.mapping.helpers.MapUserRole, java.lang.String, org.ihtsdo.otf.mapping.services.SecurityService)
   */
  @Override
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#authorizeProject(java.lang.Long, java.lang.String, org.ihtsdo.otf.mapping.helpers.MapUserRole, java.lang.String, org.ihtsdo.otf.mapping.services.SecurityService)
   */
  @Override
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
  
	/**
	 * Returns the total elapsed time str.
	 *
	 * @param time
	 *            the time
	 * @return the total elapsed time str
	 */
	@SuppressWarnings({ "boxing" })
	protected static String getTotalElapsedTimeStr(long time) {
		Long resultnum = (System.nanoTime() - time) / 1000000000;
		String result = resultnum.toString() + "s";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "m";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "h";
		return result;
	}
	  
	/**
	 * Checks if the terminology and version exist in the system.
	 * 
	 * @param terminology the terminology
	 * @param version the version
	 * @return Boolean true if the terminology exists and false if it does not exist. 
	 * @throws Exception the exception
	 */
	protected Boolean doesTerminologyVersionExist(String terminology,
			String version) throws Exception {
		Boolean exists = false;

		// validate that the terminology & version to not already exist.
		MetadataService ms;

		ms = new MetadataServiceJpa();
		if (ms.checkTerminologyVersionExists(terminology, version)) {
			exists = true;
		}

		ms.close();

		return exists;

	}
}
