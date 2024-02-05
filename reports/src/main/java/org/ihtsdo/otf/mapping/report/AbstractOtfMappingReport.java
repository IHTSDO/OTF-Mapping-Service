/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.report;

import java.util.Properties;

import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AbstractOtfMappingReport.
 */
public class AbstractOtfMappingReport {

  /** The Constant LOGGER. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractOtfMappingReport.class);

  /**
   * Email report file.
   *
   * @param emailSubject the email subject
   * @param fileName the file name
   * @param notificationRecipientsPropertyKey the notification recipients
   *          property key
   * @param notificationMessage the notification message
   * @throws Exception the exception
   */
  protected static void emailReportFile(final String emailSubject,
    final String fileName, final String notificationRecipientsPropertyKey,
    final String notificationMessage) throws Exception {

    try {
      final Properties config = ConfigUtility.getConfigProperties();

      final String notificationRecipients =
          config.getProperty(notificationRecipientsPropertyKey);

      LOGGER.info("Request to send notification email to recipients: "
          + notificationRecipients);

      final String from = (config.containsKey("mail.smtp.from"))
          ? config.getProperty("mail.smtp.from")
          : config.getProperty("mail.smtp.user");

      final Properties props = getEmailProperties(config);

      ConfigUtility.sendEmailWithAttachment(emailSubject, from,
          notificationRecipients, notificationMessage, props, fileName,
          "true".equals(config.getProperty("mail.smtp.auth")));

    } catch (final Exception e) {
      LOGGER.error("ERROR", e);
      throw e;
    }
  }
  

  /**
   * Email report error.
   *
   * @param emailSubject the email subject
   * @param notificationRecipientsPropertyKey the notification recipients property key
   * @param notificationMessage the notification message
   * @throws Exception the exception
   */
  protected static void emailReportError(final String emailSubject,
    final String notificationRecipientsPropertyKey,
    final String notificationMessage) throws Exception {

    try {
      final Properties config = ConfigUtility.getConfigProperties();

      final String notificationRecipients =
          config.getProperty(notificationRecipientsPropertyKey);

      LOGGER.info("Request to send notification email to recipients: "
          + notificationRecipients);

      final String from = (config.containsKey("mail.smtp.from"))
          ? config.getProperty("mail.smtp.from")
          : config.getProperty("mail.smtp.user");

     final Properties props = getEmailProperties(config);

      ConfigUtility.sendEmail(emailSubject, from,
          notificationRecipients, notificationMessage, props,
          "true".equals(config.getProperty("mail.smtp.auth")));

    } catch (final Exception e) {
      LOGGER.error("ERROR", e);
      throw e;
    }
  }
  
  /**
   * Returns the email properties.
   *
   * @param config the config
   * @return the email properties
   */
  private static Properties getEmailProperties(final Properties config) {
    
    final Properties emailProps = new Properties();
    emailProps.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    emailProps.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    emailProps.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    emailProps.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    emailProps.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    emailProps.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
    
    return emailProps;
  }

}
