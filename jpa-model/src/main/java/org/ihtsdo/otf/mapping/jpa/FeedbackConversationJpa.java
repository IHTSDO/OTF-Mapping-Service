package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Class FeedbackConversationJpa.
 *
 * @author ${author}
 */
@Entity
@Table(name = "feedback_conversations")
@Audited
@Indexed
@XmlRootElement(name = "feedbackConversation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedbackConversationJpa implements FeedbackConversation {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;
  
  /**  List of feedback threads in the feedback conversation. */
	@OneToMany(mappedBy = "feedbackConversation", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity = FeedbackJpa.class)
	@IndexedEmbedded(targetElement = FeedbackJpa.class)
  private List<Feedback> feedbacks = new ArrayList<Feedback>();
	
	/** Flag for whether this feedback conversation is still active. */
	@Column(nullable = false)
	private boolean isActive = true;

	/** Flag for whether this feedback conversation requires discrepancy review. */
	@Column(nullable = false)
	private boolean isDiscrepancyReview = false;
	
  /** The last modified timestamp. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  private Date lastModified = new Date();
  
	/** Id for map record. */
  @Column(nullable = false)
	private Long mapRecordId;
  
  /**  The terminology. */
  @Column(nullable = false, length = 4000)
  private String terminology;
  
  /**  The terminology id. */
  @Column(nullable = false, length = 4000)
  private String terminologyId;
  
  /**  The terminology version. */
  @Column(nullable = false, length = 4000)
  private String terminologyVersion;

  /**  The title. */
  @Column(nullable = true, length = 4000) 
  private String title;

  /**  The default preferred name. */
  @Column(nullable = true, length = 4000)
  private String defaultPreferredName;
	
  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId() {
  	return id;
  }

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id) {
  	this.id = id;
  }
  
  /**
   * Returns the feedbacks.
   *
   * @return the feedbacks
   */
	@XmlElement(type = FeedbackJpa.class, name = "feedback")
  public List<Feedback> getFeedbacks() {
		if (feedbacks == null) feedbacks = new ArrayList<>(); // ensures proper deserialization
		return feedbacks;
  }
  
  /**
   * Sets the feedbacks.
   *
   * @param feedbacks the feedbacks
   */
  public void setFeedbacks(List<Feedback> feedbacks) {
  	this.feedbacks = feedbacks;
  }
  
  /**
   * Sets the active.
   *
   * @param active the active
   */
  public void setActive(boolean active) {
  	this.isActive = active;
  }
  
  /**
   * Indicates whether or not active is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isActive() {
  	return isActive;
  }
  
  /**
   * Returns the map record id.
   *
   * @return the map record id
   */
  public Long getMapRecordId() {
  	return mapRecordId;
  }
  
  /**
   * Sets the map record id.
   *
   * @param mapRecordId the map record id
   */
  public void setMapRecordId(Long mapRecordId) {
  	this.mapRecordId = mapRecordId;
  }

	/**
	 * Sets the discrepancy review.
	 *
	 * @param discrepancyReview the discrepancy review
	 */
	@Override
	public void setDiscrepancyReview(boolean discrepancyReview) {
		this.isDiscrepancyReview = discrepancyReview;
	}

	/**
	 * Indicates whether or not discrepancy review is the case.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public boolean isDiscrepancyReview() {
		return isDiscrepancyReview;
	}

	/**
	 * Returns the last modified.
	 *
	 * @return the last modified
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public Date getLastModified() {
		Date localLastModified = null;
		for (Feedback feedback : getFeedbacks()) {
			if (localLastModified == null)
				localLastModified = feedback.getTimestamp();
			else if (feedback.getTimestamp().after(localLastModified))
				localLastModified = feedback.getTimestamp();
		}
		this.setLastModified(localLastModified);
		return lastModified;
	}

	/**
	 * Sets the last modified.
	 *
	 * @param lastModified the last modified
	 */
	@Override
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Sets the terminology id.
	 *
	 * @param terminologyId the terminology id
	 */
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}

	/**
	 * Returns the terminology id.
	 *
	 * @return the terminology id
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminologyId() {
		return terminologyId;
	}

	/**
	 * Sets the terminology version.
	 *
	 * @param terminologyVersion the terminology version
	 */
	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}

	/**
	 * Returns the terminology version.
	 *
	 * @return the terminology version
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminologyVersion() {
		return terminologyVersion;
	}

	/**
	 * Sets the terminology.
	 *
	 * @param terminology the terminology
	 */
	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	/**
	 * Returns the terminology.
	 *
	 * @return the terminology
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminology() {
		return terminology;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.FeedbackConversation#assignToChildren()
	 */
	@Override
	public void assignToChildren() {

		// assign to entries
		for (Feedback feedback : feedbacks) {
			feedback.setFeedbackConversation(this);
		}

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.FeedbackConversation#setDefaultPreferredName(java.lang.String)
	 */
	@Override
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.FeedbackConversation#getDefaultPreferredName()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getDefaultPreferredName() {
		return defaultPreferredName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.FeedbackConversation#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.FeedbackConversation#getTitle()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getTitle() {
		return title;
	}
}
