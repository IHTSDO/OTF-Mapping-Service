/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import java.util.Properties;

/**
 * Config utility integration tests.
 */
public class ConfigUtilityIntegrationTest {

	/**
	 * Test email.
	 *
	 * @throws Exception the exception
	 */
	// @Test
	public void testEmail() throws Exception {
		final Properties details = new Properties();
		details.setProperty("mail.enabled", "true");
		ConfigUtility.getConfigProperties().setProperty("mail.smtp.user", "EDIT_THIS");
		ConfigUtility.getConfigProperties().setProperty("mail.smtp.password", "EDIT_THIS");
		details.setProperty("mail.smtp.host", "smtp.gmail.com");
		details.setProperty("mail.smtp.port", "587");
		details.setProperty("mail.smtp.starttls.enable", "true");
		details.setProperty("mail.smtp.auth", "true");

		ConfigUtility.sendEmail("Test Email", "EDIT_FROM", "EDIT_TO", "Test support email.", details, true);

	}
}
