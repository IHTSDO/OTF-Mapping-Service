/*
 *    Copyright 2020 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Send an email using specified subject and body.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * Notification recipients are indicated in the config file.
 * 
 * @goal send-email
 * @phase package
 */
public class SendEmailMojo extends AbstractOtfMappingMojo {

	/**
	 * The subject.
	 * 
	 * @parameter subject
	 */
	private String subject = null;

	/**
	 * The body.
	 * 
	 * @parameter body
	 */
	private String body = null;

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Sending an email");
		getLog().info("  subject = " + subject);
		getLog().info("  body = " + body);

		// The '\n' when coming in via parameter are getting double-slashed.
		// Replace with an explicit line separator.
		body = body.replace("\\n", System.lineSeparator());

		Properties config;
		try {
			config = ConfigUtility.getConfigProperties();
		} catch (Exception e1) {
			throw new MojoExecutionException("Failed to retrieve config properties");
		}
		String notificationRecipients = config.getProperty("send.notification.recipients");

		if (config.getProperty("send.notification.recipients") == null) {
			throw new MojoExecutionException("Email was requested, but no recipients specified in the config file.");
		}

		try {
			// send the message
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
			props.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls.enable"));
			props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
			try {
				ConfigUtility.sendEmail(subject, from, notificationRecipients, body, props,
						"true".equals(config.getProperty("mail.smtp.auth")));
			} catch (Exception e) {
				// Don't allow an error here to stop processing
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Sending email failed.", e);
		}

	}
}