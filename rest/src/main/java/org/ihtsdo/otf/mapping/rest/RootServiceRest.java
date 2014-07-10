package org.ihtsdo.otf.mapping.rest;

import java.io.File;
import java.io.FileReader;
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
import org.ihtsdo.otf.mapping.helpers.LocalException;



public class RootServiceRest {
	
	//
	// Fields
	//

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

	/**
	 * Returns the config properties.
	 *
	 * @return the config properties
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

		e.printStackTrace(); 
		try {
			getConfigProperties();				
		  sendEmail(e, whatIsHappening);
		} catch(Exception ex) {
			throw new WebApplicationException(Response.status(500).entity(ex.getMessage()).build());
		}
		
		if (e instanceof LocalException) {				
				throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
	  } else { 
				throw new WebApplicationException(Response.status(500).entity(
						"Unexpected error trying to " + whatIsHappening + ". Please contact the administrator.").build());			
	  }
	}
	
	/**
	 * Send email regarding Exception e.
	 *
	 * @param e the e
	 * @param whatIsHappening the what is happening
	 */
	private void sendEmail(Exception e, String whatIsHappening) {

		Properties props = new Properties();
		props.put("mail.smtp.user", m_from);
		props.put("mail.smtp.password", host_password);
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", port);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		
		m_text = new StringBuffer();
		if (!(e instanceof LocalException))
			m_text.append("Unexpected error trying to " + whatIsHappening + ". Please contact the administrator.").append("\n");
		for (StackTraceElement element : e.getStackTrace()) {
			m_text.append(element).append("\n");
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
	 * The Class SMTPAuthenticator.
	 *
	 * @author ${author}
	 */
	public class SMTPAuthenticator extends javax.mail.Authenticator {
	  public PasswordAuthentication getPasswordAuthentication() {
	    return new PasswordAuthentication(m_from, host_password);
	  }
	}
}
