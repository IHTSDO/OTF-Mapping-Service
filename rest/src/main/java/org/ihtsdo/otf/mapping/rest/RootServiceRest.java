package org.ihtsdo.otf.mapping.rest;

import org.ihtsdo.otf.mapping.handlers.ErrorHandler;

/**
 * Top level class for all REST services.
 */
public class RootServiceRest {
	
	
	public void handleException(Exception e, String whatisHappening) {
		handleException(e, whatisHappening, "", "", "");
	}
	
	
	public void handleException(Exception e, String whatIsHappening, String userName, 
			  String project, String recordId) {
		ErrorHandler errorHandler = new ErrorHandler();
		
		errorHandler.sendEmail(e, whatIsHappening, userName, project, recordId);
	}

	
}
