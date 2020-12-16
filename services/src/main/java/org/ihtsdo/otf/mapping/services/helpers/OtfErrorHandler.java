/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.PasswordAuthentication;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;

/**
 * The error handler OtfErrorHandler.
 */
public class OtfErrorHandler {

  /** The config. */
  public Properties config = null;

  /** The address that messages will appear as though they come from. */
  String from = "";

  /** The mail server user. */
  String user = "";

  /** The password for the SMTP host. */
  String hostPassword = "";

  /** The SMTP host that will be transmitting the message. */
  String host = "";

  /** The port on the SMTP host. */
  String port = "";

  /** The list of addresses to send the message to. */
  String recipients = "";

  /** Subject text for the email. */
  String mailSubject = "IHTSDO Mapping Tool Exception Report";

  /** The start tls. */
  boolean startTls = false;

  /** The auth. */
  boolean auth = false;

  /** Text for the email. */
  StringBuffer mailText;

  /** Format for logging */
  SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a");

  /**
   * Handle exception. For {@link LocalException} print the stack trace and
   * inform the user with a message generated by the application. For all other
   * exceptions, also send email to administrators with the message and the
   * stack trace.
   *
   * @param e the e
   * @param whatIsHappening the what is happening
   * @param userName the current user
   * @param project the map project
   * @param objectId the object id
   * @throws WebApplicationException the web application exception
   */
  public void handleException(Exception e, String whatIsHappening,
    String userName, String project, String objectId)
    throws WebApplicationException {

    Logger.getLogger(OtfErrorHandler.class).error("handle exception", e);

    // if a local exception, throw as web application exception
    if (e instanceof LocalException) {
      if (((LocalException) e).getResponseCode() != null) {

        // if a 401 code, build response and throw
        if (((LocalException) e).getResponseCode().equals("401")) {
          Logger.getLogger(OtfErrorHandler.class)
              .error("Unauthorized access detected");

          throw new WebApplicationException(
              Response.status(401).entity(e.getMessage()).build());
        }
        // if no code specified, build and throw a 500
      } else {
    	  	// Ensure message has quotes.
    	    // When migrating from jersey 1 to jersey 2, messages no longer
    	    // had quotes around them when returned to client and angular
    	    // could not parse them as json.
    	    String message = e.getMessage();
    	    if (message != null && !message.startsWith("\"")) {
    	      message = "\"" + message + "\"";
    	    }
    	    
        throw new WebApplicationException(
            Response.status(500).entity(message).type("text/plain").build());
      }
    }

    if (e instanceof WebApplicationException) {
      WebApplicationException ex = (WebApplicationException) e;
      String message = "";
      
      if (ex.getResponse().getEntity() instanceof String) {
        message = ex.getResponse().getEntity().toString();
      }
      // Rebuilding the Exception to ensure the Response status remains the same
      // and setting message for the UI to parse.
      // Throwing a new WebApplicationException was causing the a 500 error in
      // the UI. (throw new WebApplicationException(e))
      // Re-throwing the same error caused the message to drop.
      // (WebApplicationException)e
      if (ex.getResponse().getStatus() == Response.Status.UNAUTHORIZED.getStatusCode())
      {
        message = "\"Session expired, please login.\"";
        throw new WebApplicationException(
            Response.status(ex.getResponse().getStatusInfo())
                .entity(message).build());
      }
      else {
        throw new WebApplicationException(Response
            .status(ex.getResponse().getStatusInfo()).entity(message).build());
      }
    }

    // send email to the recipients designated in config file
    try {
      getConfigProperties();
      sendEmail(e, whatIsHappening, userName, project, objectId);
    } catch (Exception ex) {
      Logger.getLogger(getClass()).error("Error sending email of error in handleException." , ex);
      throw new WebApplicationException(
          Response.status(500).entity(ex.getMessage()).build());
    }

    // finally, throw the web application exception to the user
    throw new WebApplicationException(
        Response.status(500).entity("Unexpected error trying to "
            + whatIsHappening + ". Please contact the administrator.").build());
  }

  /**
   * Send email regarding Exception e.
   *
   * @param e the e
   * @param whatIsHappening the what is happening
   * @param userName the current user's user name
   * @param project the project
   * @param recordId the record id
   * @throws Exception
   */
  public void sendEmail(Exception e, String whatIsHappening, String userName,
    String project, String recordId) throws Exception {

    if (!"true".equals(
        ConfigUtility.getConfigProperties().getProperty("mail.enabled"))) {
      return;
    }
    // Bail if no recipients
    if (recipients == null || recipients.isEmpty()) {
      return;
    }

    mailSubject = "IHTSDO Mapping Tool Exception Report";
    mailText = new StringBuffer();
    if (!(e instanceof LocalException))
      mailText.append("Unexpected error trying to " + whatIsHappening
          + ". Please contact the administrator.").append("\n\n");

    try {
      mailText.append("HOST: " + InetAddress.getLocalHost().getHostName())
          .append("\n");
    } catch (UnknownHostException e1) {
      e1.printStackTrace();
    }
    mailText.append("TIME: " + ft.format(new Date())).append("\n");
    mailText.append("USER: " + userName).append("\n");
    mailText.append("PROJECT: " + project).append("\n");
    mailText.append("ID: " + recordId).append("\n\n");

    mailText.append("MESSAGE: " + e.getMessage()).append("\n\n");
    StringWriter out = new StringWriter();
    PrintWriter pw = new PrintWriter(out);
    e.printStackTrace(pw);
    mailText.append(out.getBuffer());

    // send the revised message
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
    ConfigUtility.sendEmail(mailSubject, from, recipients, mailText.toString(),
        props, "true".equals(config.getProperty("mail.smtp.auth")));

  }

  /**
   * SMTPAuthenticator.
   */
  public class SMTPAuthenticator extends javax.mail.Authenticator {

    /*
     * (non-Javadoc)
     * 
     * @see javax.mail.Authenticator#getPasswordAuthentication()
     */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(from, hostPassword);
    }
  }

  /**
   * Returns the config properties.
   * 
   * @throws Exception the exception
   */
  public void getConfigProperties() throws Exception {

    if (config == null) {

      String configFileName = System.getProperty("run.config");
      config = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      config.load(in);
      in.close();

      from = config.getProperty("mail.smtp.user");
      if (config.getProperty("mail.smtp.from") != null) {
        from = config.getProperty("mail.smtp.from");
      }
      user = config.getProperty("mail.smtp.user");
      hostPassword = config.getProperty("mail.smtp.password");
      host = config.getProperty("mail.smtp.host");
      port = config.getProperty("mail.smtp.port");
      recipients = config.getProperty("mail.smtp.to");
      auth = "true".equals(config.getProperty("mail.smtp.auth"));
      startTls = "true".equals(config.getProperty("mail.smtp.starttls.enable"));
    }

  }
}
