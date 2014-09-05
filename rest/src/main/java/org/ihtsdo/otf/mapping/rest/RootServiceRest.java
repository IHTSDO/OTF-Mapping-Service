package org.ihtsdo.otf.mapping.rest;

import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.FeedbackEmail;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Top level class for all REST services.
 */
public class RootServiceRest {
	
	//
	// Fields
	//

	/**  The config. */
	public Properties config = null;

	/** The address that messages will appear as though they come from. */
	String m_from = ""; 
	
	/** The password for the SMTP host. */
	String host_password = ""; 

	/** The SMTP host that will be transmitting the message. */
	String host = ""; 

	/** The port on the SMTP host. */
	String port = ""; 

	/** The list of addresses to send the message to. */
	String recipients = ""; 

	/** Subject text for the email. */
	String m_subject = "IHTSDO Mapping Tool Exception Report";

	/** Text for the email. */
	StringBuffer m_text; 
	
	/** Format for logging */
	SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a");

	/**
	 * Returns the config properties.
	 *
	 * @throws Exception the exception
	 */
	public void getConfigProperties() throws Exception {

		if (config == null) {

			String configFileName = System.getProperty("run.config");
			Logger.getLogger(this.getClass())
					.info("  run.config = " + configFileName);
			config = new Properties();
			FileReader in = new FileReader(new File(configFileName));
			config.load(in);
			in.close();
			
			m_from = config.getProperty("mail.smtp.user");
			host_password = config.getProperty("mail.smtp.password");
			host = config.getProperty("mail.smtp.host");
			port = config.getProperty("mail.smtp.port");
			recipients = config.getProperty("mail.smtp.to");

			Logger.getLogger(this.getClass()).info("  properties = " + config);
		}

	}

	/**
	 * Handle exception.
	 *
	 * @param e the e
	 * @param whatIsHappening the what is happening
	 * @throws WebApplicationException the web application exception
	 */
	public void handleException(Exception e, String whatIsHappening)
			throws WebApplicationException {
		handleException(e, whatIsHappening, "", "", "");
	}
	
	/**
	 * Handle exception. For {@link LocalException} print the stack trace and inform the
	 * user with a message generated by the application.  For all other exceptions, also
	 * send email to administrators with the message and the stack trace.
	 *
	 * @param e the e
	 * @param whatIsHappening the what is happening
	 * @param userName the current user
	 * @param project the map project
	 * @param recordId the map record id
	 * @throws WebApplicationException the web application exception
	 */
	public void handleException(Exception e, String whatIsHappening, String userName,
		  String project, String recordId)
		throws WebApplicationException {

		e.printStackTrace(); 
		if (e instanceof LocalException) {				
			throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
		}
		
		try {
  	 	getConfigProperties();				
		  sendEmail(e, whatIsHappening, userName, project, recordId);
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(ex.getMessage()).build());
		}
		
        throw new WebApplicationException(Response.status(500).entity(
        		"Unexpected error trying to " + whatIsHappening + ". Please contact the administrator.").build());			
	}
	
	/**
	 * Send email regarding Exception e.
	 *
	 * @param e the e
	 * @param whatIsHappening the what is happening
	 * @param uuid the uuid identifier for this exception used to locate exception in log
	 * @param userName the current user's user name
	 */
	private void sendEmail(Exception e, String whatIsHappening, String userName, 
		  String project, String recordId) {

		// Bail if no recipients
		if (recipients == null || recipients.isEmpty()) {
			return;
		}
		
		Properties props = new Properties();
		props.put("mail.smtp.user", m_from);
		props.put("mail.smtp.password", host_password);
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		
		m_subject = "IHTSDO Mapping Tool Exception Report";
		m_text = new StringBuffer();
		if (!(e instanceof LocalException))
			m_text.append("Unexpected error trying to " + whatIsHappening + ". Please contact the administrator.").append("\n\n");

		try {
			m_text.append("HOST: " +  InetAddress.getLocalHost().getHostName()).append("\n");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		m_text.append("TIME: " + ft.format(new Date())).append("\n");
		m_text.append("USER: " + userName).append("\n");
		m_text.append("PROJECT: " + project).append("\n");
		m_text.append("ID: " + recordId).append("\n\n");
		
		m_text.append("MESSAGE: " + e.getMessage()).append("\n\n");
		for (StackTraceElement element : e.getStackTrace()) {
			m_text.append("  ").append(element).append("\n");
		}

		try {
			Authenticator auth = new SMTPAuthenticator();
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			msg.setText(m_text.toString());
			msg.setSubject(m_subject);
			msg.setFrom(new InternetAddress(m_from));
			String[] recipientsArray = recipients.split(";");
			for (String recipient : recipientsArray) {
			  msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
			}
			Transport.send(msg);

		} catch (Exception mex) {
			mex.printStackTrace();
		}
	}

	/**
	 * Send user error email.
	 *
	 * @param userError the user error
	 */
	public void sendUserErrorEmail(Feedback userError) {
		
		Properties props = new Properties();
		props.put("mail.smtp.user", m_from);
		props.put("mail.smtp.password", host_password);
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");

		try {
			Long mapRecordId = userError.getFeedbackConversation().getMapRecordId();
			MappingServiceJpa mappingService = new MappingServiceJpa();
			MapRecord errorRecord = mappingService.getMapRecord(mapRecordId);

			m_subject =
					"IHTSDO Mapping Tool Editing Error Report: "
							+ errorRecord.getConceptId();
			String[] recipientsArray =
					new String[userError.getRecipients().size()];
			int i = 0;
			for (MapUser user : userError.getRecipients()) {
				recipientsArray[i++] = user.getEmail();
			}

			m_text = new StringBuffer();

			m_text.append(
					"USER ERROR on " + errorRecord.getConceptId() + ": "
							+ errorRecord.getConceptName()).append("\n\n");
			m_text.append("Error type: " + userError.getMapError()).append("\n");
			m_text.append("Reporting lead: " + userError.getSender().getName())
					.append("\n");
			m_text.append("Comment: " + userError.getMessage()).append("\n");
			m_text.append("Reporting date: " + userError.getTimestamp()).append(
					"\n\n");
			// TODO: the base url here can not be hardcoded, won't work in dev and uat
			m_text
					.append("Record URL: https://mapping.snomedtools.org/index.html#/record/recordId/"
							+ mapRecordId);

			Authenticator auth = new SMTPAuthenticator();
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			msg.setText(m_text.toString());
			msg.setSubject(m_subject);
			msg.setFrom(new InternetAddress(m_from));
			for (String recipient : recipientsArray) {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
						recipient));
			}
			Transport.send(msg);

		} catch (Exception mex) {
			mex.printStackTrace();
		}
	}

	public void sendEmail(FeedbackEmail feedbackEmail) {
		
		
		try {
			getConfigProperties();
			
			Properties props = new Properties();
			props.put("mail.smtp.user", m_from);
			props.put("mail.smtp.password", host_password);
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.port", port);
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.auth", "true");
			
			Authenticator auth = new SMTPAuthenticator();
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);
			//msg.setText(feedbackEmail.getEmailText());
			msg.setSubject(feedbackEmail.getSubject());
			msg.setFrom(new InternetAddress(m_from));
			
			// get the recipients
			MappingService mappingService = new MappingServiceJpa();
			for (String recipient : feedbackEmail.getRecipients()) {
				MapUser mapUser = mappingService.getMapUser(recipient);
				System.out.println(mapUser.getEmail());
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mapUser.getEmail()));	
			}
			
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress("***REMOVED***"));
			
			msg.setContent(feedbackEmail.getEmailText(), "text/html");
			
			
			// msg.addRecipient(Message.RecipientType.TO, new InternetAddress(feedbackEmail.getSender().getEmail()));
			
			
			
			Transport.send(msg);

		} catch (Exception mex) {
			mex.printStackTrace();
		}
	}


	/**
	 * SMTPAuthenticator.
	 */
	public class SMTPAuthenticator extends javax.mail.Authenticator {
	  
  	/* (non-Javadoc)
  	 * @see javax.mail.Authenticator#getPasswordAuthentication()
  	 */
  	@Override
    public PasswordAuthentication getPasswordAuthentication() {
	    return new PasswordAuthentication(m_from, host_password);
	  }
	}
}
