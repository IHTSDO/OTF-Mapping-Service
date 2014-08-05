package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Class FeedbackEmailJpa.
 */
public class FeedbackEmailJpa implements FeedbackEmail {

	/** The sender. */
	public String sender;
	
	/** The recipients. */
	public List<String> recipients;
	
	/** The subject. */
	public String subject;
	
	/** The email text. */
	public String emailText;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#setSender(org.ihtsdo.otf.mapping.model.String)
	 */
	@Override
	public void setSender(String mapUser) {
		this.sender = mapUser;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#getSender()
	 */
	@Override
	public String getSender() {
		return this.sender;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#setRecipients(org.ihtsdo.otf.mapping.helpers.List<String>)
	 */
	@Override
	public void setRecipients(List<String> mapUserList) {
		this.recipients = mapUserList;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#getRecipients()
	 */
	@Override
	public List<String> getRecipients() {
		return this.recipients;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#setSubject(java.lang.String)
	 */
	@Override
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#getSubject()
	 */
	@Override
	public String getSubject() {
		return this.subject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#setEmailText(java.lang.String)
	 */
	@Override
	public void setEmailText(String emailText) {
		this.emailText = emailText;

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.FeedbackEmail#getEmailText()
	 */
	@Override
	public String getEmailText() {
		return this.emailText;
	}

}
