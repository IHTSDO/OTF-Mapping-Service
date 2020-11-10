/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.util.Properties;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.RootServiceRest;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.OtfErrorHandler;

/**
 * Top level class for all REST services.
 */
public class RootServiceRestImpl implements RootServiceRest {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#handleException(java.lang.
   * Exception, java.lang.String)
   */
  @Override
  public void handleException(Exception e, String whatisHappening) {
    handleException(e, whatisHappening, "", "", "");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#handleException(java.lang.
   * Exception, java.lang.String, java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  @SuppressWarnings("static-method")
  public void handleException(Exception e, String whatIsHappening,
    String userName, String project, String objectdId) {
    OtfErrorHandler errorHandler = new OtfErrorHandler();

    errorHandler.handleException(e, whatIsHappening, userName, project,
        objectdId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#authorizeApp(java.lang.
   * String, org.ihtsdo.otf.mapping.helpers.MapUserRole, java.lang.String,
   * org.ihtsdo.otf.mapping.services.SecurityService)
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

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.RootServiceRest#authorizeProject(java.lang
   * .Long, java.lang.String, org.ihtsdo.otf.mapping.helpers.MapUserRole,
   * java.lang.String, org.ihtsdo.otf.mapping.services.SecurityService)
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
    if (!role.hasPrivilegesOf(requiredRole) && service
        .getApplicationRoleForToken(authToken) != MapUserRole.ADMINISTRATOR) {
      throw new WebApplicationException(Response.status(401)
          .entity("User does not have permissions to " + operation).build());
    }
    return user;
  }

  /**
   * Returns the total elapsed time str.
   *
   * @param time the time
   * @return the total elapsed time str
   */
  @SuppressWarnings({
      "boxing"
  })
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
   * @return Boolean true if the terminology exists and false if it does not
   *         exist.
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

  /**
   * Send release notification.
   *
   * @param notificationMessage the notification message
   * @param userName the user name
   * @throws Exception the exception
   */
  // send an email notification to the user when a process completes or fails
  protected void sendReleaseNotification(String notificationMessage,
    String userName) throws Exception {
    // send the user a notice that the reload is complete
    Properties config = ConfigUtility.getConfigProperties();
    String from;
    if (config.containsKey("mail.smtp.from")) {
      from = config.getProperty("mail.smtp.from");
    } else {
      from = config.getProperty("mail.smtp.user");
    }
    Properties props = new Properties();
    props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    props.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));

    String notificationRecipients;
    try (MappingService mappingService = new MappingServiceJpa();) {
      notificationRecipients = mappingService.getMapUser(userName).getEmail();
    }

    try {
      ConfigUtility.sendEmail(
          "[OTF-Mapping-Tool] Reloading terminology results", from,
          notificationRecipients, notificationMessage, props,
          "true".equals(config.getProperty("mail.smtp.auth")));
    } catch (Exception e) {
      // Don't allow an error here to stop processing
      e.printStackTrace();
    }
  }

}
