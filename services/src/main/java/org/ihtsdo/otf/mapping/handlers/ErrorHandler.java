package org.ihtsdo.otf.mapping.handlers;

import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.ihtsdo.otf.mapping.helpers.LocalException;

public class ErrorHandler {
	
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
	 * Send email regarding Exception e.
	 *
	 * @param e the e
	 * @param whatIsHappening the what is happening
	 * @param uuid the uuid identifier for this exception used to locate exception in log
	 * @param userName the current user's user name
	 */
	public void sendEmail(Exception e, String whatIsHappening, String userName, 
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
			SMTPAuthenticator auth = new SMTPAuthenticator();
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
			
			m_from = config.getProperty("mail.smtp.user");
			host_password = config.getProperty("mail.smtp.password");
			host = config.getProperty("mail.smtp.host");
			port = config.getProperty("mail.smtp.port");
			recipients = config.getProperty("mail.smtp.to");
		}

	}
}
