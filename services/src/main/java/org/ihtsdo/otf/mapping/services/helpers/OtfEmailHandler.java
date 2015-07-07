package org.ihtsdo.otf.mapping.services.helpers;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;

/**
 * Email handler.
 */
public class OtfEmailHandler {

  Properties config = null;

  // on instantiation, initialize config properties
  /**
   * Instantiates an empty {@link OtfEmailHandler}.
   * 
   * @throws Exception the exception
   */
  public OtfEmailHandler() throws Exception {
    config = ConfigUtility.getConfigProperties();
  }

  // basic email format, with specified users
  /**
   * Send simple email.
   *
   * @param recipients the recipients
   * @param from the from
   * @param subject the subject
   * @param message the message
   */
  public void sendSimpleEmail(String recipients, String from, String subject,
    String message) {

    Logger.getLogger(OtfEmailHandler.class).info("Sending email...");

    try {
      SMTPAuthenticator auth = new SMTPAuthenticator();
      Session session = Session.getInstance(config, auth);

      MimeMessage msg = new MimeMessage(session);

      // set the message, subject, and sender
      if (message.contains("<html")) {
        msg.setContent(message, "text/html; charset=utf-8");
      } else {
        msg.setText(message);
      }
      msg.setSubject(subject);
      msg.setFrom(from);

      // split recipients if needed and add each
      String[] recipientsArray = recipients.split(";");
      for (String recipient : recipientsArray) {
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
            recipient));
      }
      Transport.send(msg);

    } catch (Exception mex) {
      mex.printStackTrace();
    }
  }

  /**
   * Send simple email.
   *
   * @param recipients the recipients
   * @param subject the subject
   * @param message the message
   */
  public void sendSimpleEmail(String recipients, String subject, String message) {

    String from = config.getProperty("mail.smtp.user");
    sendSimpleEmail(recipients, from, subject, message);

  }

  // helper function to send simple email to recipients specified in config file
  /**
   * Send simple email.
   * 
   * @param subject the subject
   * @param message the message
   */
  public void sendSimpleEmail(String subject, String message) {

    String recipients = config.getProperty("mail.smtp.to");
    sendSimpleEmail(recipients, subject, message);

  }

  public void sendValidationResultEmail(String recipients, String subject,
    String message, ValidationResult validationResult) {

    // validation message is specified header message and two new lines
    String validationMessage = message + "\n\n";

    // add the messages
    if (validationResult.getMessages().isEmpty()) {
      validationMessage += "Messages:\n";
      for (String s : validationResult.getMessages()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // add the errors
    if (validationResult.getErrors().isEmpty()) {
      validationMessage += "Errors:\n";
      for (String s : validationResult.getErrors()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // add the warnings
    if (validationResult.getWarnings().isEmpty()) {
      validationMessage += "Warnings:\n";
      for (String s : validationResult.getWarnings()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // send the revised message
    this.sendSimpleEmail(recipients, subject, validationMessage);

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
      return new PasswordAuthentication(config.getProperty("mail.smtp.user"),
          config.getProperty("mail.smtp.password"));
    }
  }

  /**
   * Indicates whether or not recipients list is specified (non-empty)
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isRecipientsListSpecified() {
    return !config.getProperty("send.notification.recipients").isEmpty();
  }

}
