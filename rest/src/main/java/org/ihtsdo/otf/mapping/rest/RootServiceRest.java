package org.ihtsdo.otf.mapping.rest;

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

}
