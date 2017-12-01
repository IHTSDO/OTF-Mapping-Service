package org.ihtsdo.otf.mapping.jpa.services.rest;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.services.SecurityService;

public interface RootServiceRest {

	/**
	   * Handle exception.
	   *
	   * @param e the e
	   * @param whatisHappening the whatis happening
	   */
	void handleException(Exception e, String whatisHappening);

	/**
	   * Handle exceptions via the Error Handler Class
	   *
	   * @param e the e
	   * @param whatIsHappening the what is happening
	   * @param userName the user name
	   * @param project the project
	   * @param objectdId the objectd id
	   */
	void handleException(Exception e, String whatIsHappening, String userName, String project, String objectdId);

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
	String authorizeApp(String authToken, MapUserRole requiredRole, String operation, SecurityService service)
			throws Exception;

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
	String authorizeProject(Long projectId, String authToken, MapUserRole requiredRole, String operation,
			SecurityService service) throws Exception;

}