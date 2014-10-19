package org.ihtsdo.otf.mapping.rest;

import org.ihtsdo.otf.mapping.handlers.OtfErrorHandler;

// TODO: Auto-generated Javadoc
/**
 * Top level class for all REST services.
 */
public class RootServiceRest {
	
	
	/**
	 * Returns the config properties.
	 *
	 * @throws Exception the exception
	 */
	public void getConfigProperties() throws Exception {

	  Properties config = ConfigUtility.getConfigProperties();
			
	  m_from = config.getProperty("mail.smtp.user");
	  host_password = config.getProperty("mail.smtp.password");
	  host = config.getProperty("mail.smtp.host");
	  port = config.getProperty("mail.smtp.port");
	  recipients = config.getProperty("mail.smtp.to");
	  
	  Logger.getLogger(this.getClass()).info("  properties = " + config);

	}

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
	public void handleException(Exception e, String whatIsHappening, String userName, 
			  String project, String objectdId) {
		OtfErrorHandler errorHandler = new OtfErrorHandler();

		
		errorHandler.handleException(e, whatIsHappening, userName, project, objectdId);
	}

	
}
