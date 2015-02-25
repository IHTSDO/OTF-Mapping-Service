package org.ihtsdo.otf.mapping.services.helpers;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

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
   * @param subject the subject
   * @param message the message
   */
  public void sendSimpleEmail(String recipients, String subject, String message) {

    Logger.getLogger(OtfEmailHandler.class).info("Sending email...");

    try {
      SMTPAuthenticator auth = new SMTPAuthenticator();
      Session session = Session.getInstance(config, auth);

      MimeMessage msg = new MimeMessage(session);

      // set the message, subject, and sender
      msg.setText(message);
      msg.setSubject(subject);
      msg.setFrom(config.getProperty("mail.smtp.user"));

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
