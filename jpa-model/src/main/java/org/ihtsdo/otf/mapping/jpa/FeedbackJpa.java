package org.ihtsdo.otf.mapping.jpa;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


// TODO: Auto-generated Javadoc
/**
 * The Class FeedbackJpa.
 *
 * @author ${author}
 */
@Entity
@Table(name = "feedbacks")
@Audited
@XmlRootElement(name = "feedback")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackJpa implements Feedback {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;
  
  /**  The error. */
  @Column(nullable = true, length = 4000)
  private String mapError;
  
  /** The note. */
  @Column(nullable = false, length = 4000)
  private String message;
  
  /** The timestamp. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  private Date timestamp = new Date();

	/**  The user sending feedback. */
	@ManyToOne(targetEntity = MapUserJpa.class)
	@IndexedEmbedded(targetElement = MapUserJpa.class)
	private MapUser sender;
	
	/** The users receiving feedback. */
  @ManyToMany(targetEntity = MapUserJpa.class, fetch = FetchType.LAZY)
  @JoinTable(name="feedback_recipients")
  @IndexedEmbedded(targetElement = MapUserJpa.class)
  private Set<MapUser> recipients = new HashSet<>();
	
  /**  The indicates feedback of type error, rather than general. */
	@Column(nullable = false)
  private boolean isError = false;
  
  /** The feedback conversation. */
  @ManyToOne(targetEntity = FeedbackConversationJpa.class, optional = false)
  @ContainedIn
  private FeedbackConversation feedbackConversation;
	
	/** The users who have viewed this feedback item. */
  @ManyToMany(targetEntity = MapUserJpa.class, fetch = FetchType.LAZY)
  @JoinTable(name="feedback_viewers")
  @IndexedEmbedded(targetElement = MapUserJpa.class)
  private Set<MapUser> viewedBy = new HashSet<>();
  
  /**
   *  Default constructor.
   */	
  public FeedbackJpa() {
  }
  
	/**
	 * Return the id.
	 * 
	 * @return the id
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * Set the id.
	 * 
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setRecipients(java.util.Set)
	 */
	@Override
	public void setRecipients(Set<MapUser> receivingUsers) {
		this.recipients = receivingUsers;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getRecipients()
	 */
	@Override
  @XmlElement(type = MapUserJpa.class, name = "recipients")
	public Set<MapUser> getRecipients() {
		if (recipients == null) recipients = new HashSet<>();// ensures proper serialization
		return recipients;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setSender(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setSender(MapUser sender) {
		this.sender = sender;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getSender()
	 */
	@Override
	@XmlElement(type = MapUserJpa.class, name = "sender") 
	public MapUserJpa getSender() {
		return (MapUserJpa) sender;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#setMessage(java.lang.String)
	 */
	@Override
	public void setMessage(String note) {
		this.message = note;
	}

	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getMessage()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getMessage() {
		return message;
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
		this.mapError = mapError;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getMapError()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getMapError() {
		return mapError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getFeedbackConversation()
	 */  
	@XmlTransient
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
	public void setIsError(boolean isError) {
		this.isError = isError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#getViewedBy()
	 */
	@Override
  @XmlElement(type = MapUserJpa.class, name = "viewedBy")
	public Set<MapUser> getViewedBy() {
		if (viewedBy == null) viewedBy = new HashSet<>();// ensures proper serialization
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
		viewedBy.add(user);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.Feedback#removeViewedBy(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void removeViewedBy(MapUser user) {
		viewedBy.remove(user);
	}

/*	// space separated list
	@Field..
	String getParticipantUserNames() */
}
