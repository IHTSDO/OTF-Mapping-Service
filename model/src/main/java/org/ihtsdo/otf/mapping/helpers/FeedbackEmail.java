package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

/**
 * The Interface FeedbackEmail.
 */
public interface FeedbackEmail {

	/**
	 * Sets the sender.
	 *
	 * @param mapUser the new sender
	 */
	public void setSender(String mapUser);
	
	/**
	 * Gets the sender.
	 *
	 * @return the sender
	 */
	public String getSender();
	
	/**
	 * Sets the recipients.
	 *
	 * @param mapUserList the new recipients
	 */
	public void setRecipients(List<String> mapUserList);
	
	/**
	 * Gets the recipients.
	 *
	 * @return the recipients
	 */
	public List<String> getRecipients();
	
	/**
	 * Sets the subject.
	 *
	 * @param subject the new subject
	 */
	public void setSubject(String subject);
	
	/**
	 * Gets the subject.
	 *
	 * @return the subject
	 */
	public String getSubject();
	
	/**
	 * Sets the email text.
	 *
	 * @param emailText the new email text
	 */
	public void setEmailText(String emailText);
	
	/**
	 * Gets the email text.
	 *
	 * @return the email text
	 */
	public String getEmailText();
	
	
	
}
