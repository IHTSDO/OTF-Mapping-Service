package org.ihtsdo.otf.mapping.pojo;

import java.util.Date;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapUser;


/**
 * Reference implementation of {@link Feedback}.
 */
public class FeedbackImpl implements Feedback {

	
  /**  The id. */
  private Long id;
  
  /**  The error. */
  private String error;
  
  /** The note. */
  private String note;
  
  /** The timestamp. */
  private Date timestamp = new Date();

	/**  The user in error. */
	private Set<MapUser> recipientUsers;
	
	/**  The user reporting error. */
	private MapUser sender;
	
  /**  The is error. */
  private boolean isError;
	
	/** The feedback conversation. */
	private FeedbackConversation feedbackConversation;
	
	/**  The viewed by. */
	private Set<MapUser> viewedBy;
	
  /**
   *  Default constructor.
   */	
  public FeedbackImpl() {
  }
  

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setRecipients(java.util.Set)
	 */
	@Override
	public void setRecipients(Set<MapUser> mapUsers) {
		recipientUsers = mapUsers;
	}

	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getRecipients()
	 */
	@Override
	public Set<MapUser> getRecipients() {
		return recipientUsers;
	}

	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setSender(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setSender(MapUser mapUser) {
		sender = mapUser;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getSender()
	 */
	@Override
	public MapUser getSender() {
		return sender;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setMessage(java.lang.String)
	 */
	@Override
	public void setMessage(String note) {
		this.note = note;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getMessage()
	 */
	@Override
	public String getMessage() {
		return note;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setTimestamp(java.util.Date)
	 */
	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getTimestamp()
	 */
	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setMapError(java.lang.String)
	 */
	@Override
	public void setMapError(String mapError) {
		this.error = mapError;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getMapError()
	 */
	@Override
	public String getMapError() {
		return error;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getFeedbackConversation()
	 */
	@Override
	public FeedbackConversation getFeedbackConversation() {
		return feedbackConversation;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setFeedbackConversation(org.ihtsdo.otf.mapping.model.FeedbackConversation)
	 */
	@Override
	public void setFeedbackConversation(FeedbackConversation feedbackConversation) {
		this.feedbackConversation = feedbackConversation;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#isError()
	 */
	@Override
	public boolean isError() {
		return isError;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setError(boolean)
	 */
	@Override
	public void setIsError(boolean error) {
		this.isError = error;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getViewedBy()
	 */
	@Override
	public Set<MapUser> getViewedBy() {
		return viewedBy;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setViewedBy(java.util.Set)
	 */
	@Override
	public void setViewedBy(Set<MapUser> viewedBy) {
		this.viewedBy = viewedBy;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#addViewedBy(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void addViewedBy(MapUser user) {
		this.viewedBy.add(user);
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#removeViewedBy(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void removeViewedBy(MapUser user) {
		this.viewedBy.remove(user);
	}


}
