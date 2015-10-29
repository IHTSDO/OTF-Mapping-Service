package org.ihtsdo.otf.mapping.model;

import java.util.Date;
import java.util.List;

/**
 * Generically represents a conversation regarding {@link Feedback} between
 * users.
 */
public interface FeedbackConversation {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the map project id.
   * 
   * @return the map project id
   */
  public Long getMapProjectId();

  /**
   * Sets the map project id.
   * 
   * @param mapProjectId the map project id
   */
  public void setMapProjectId(Long mapProjectId);

  /**
   * Returns the feedbacks.
   * 
   * @return the feedbacks
   */
  public List<Feedback> getFeedbacks();

  /**
   * Sets the feedbacks.
   * 
   * @param feedbacks the feedbacks
   */
  public void setFeedbacks(List<Feedback> feedbacks);

  /**
   * Sets the active.
   * 
   * @param active the active
   */
  public void setResolved(boolean active);

  /**
   * Indicates whether or not active is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isResolved();

  /**
   * Returns the map record id.
   * 
   * @return the map record id
   */
  public Long getMapRecordId();

  /**
   * Sets the map record id.
   * 
   * @param mapRecordId the map record id
   */
  public void setMapRecordId(Long mapRecordId);

  /**
   * Sets the discrepancy review.
   * 
   * @param discrepancyReview the discrepancy review
   */
  public void setDiscrepancyReview(boolean discrepancyReview);

  /**
   * Indicates whether or not discrepancy review is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isDiscrepancyReview();

  /**
   * Returns the last modified.
   * 
   * @return the last modified
   */
  public Date getLastModified();

  /**
   * Sets the last modified.
   * 
   * @param lastModified the last modified
   */
  public void setLastModified(Date lastModified);

  /**
   * Sets the terminology id.
   * 
   * @param terminologyId the terminology id
   */
  public void setTerminologyId(String terminologyId);

  /**
   * Returns the terminology id.
   * 
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Sets the terminology version.
   * 
   * @param terminologyVersion the terminology version
   */
  public void setTerminologyVersion(String terminologyVersion);

  /**
   * Returns the terminology version.
   * 
   * @return the terminology version
   */
  public String getTerminologyVersion();

  /**
   * Sets the terminology.
   * 
   * @param terminology the terminology
   */
  public void setTerminology(String terminology);

  /**
   * Returns the terminology.
   * 
   * @return the terminology
   */
  public String getTerminology();

  /**
   * Assign to children.
   */
  public void assignToChildren();

  /**
   * Sets the default preferred name.
   * 
   * @param defaultPreferredName the default preferred name
   */
  public void setDefaultPreferredName(String defaultPreferredName);

  /**
   * Returns the default preferred name.
   * 
   * @return the default preferred name
   */
  public String getDefaultPreferredName();

  /**
   * Sets the title.
   * 
   * @param title the title
   */
  public void setTitle(String title);

  /**
   * Returns the title.
   * 
   * @return the title
   */
  public String getTitle();

  /**
   * Adds the feedback.
   *
   * @param feedback the feedback
   */
  public void addFeedback(Feedback feedback);

  /**
   * Sets the user name.
   *
   * @param userName the user name
   */
  public void setUserName(String userName);

  /**
   * Returns the user name.
   *
   * @return the user name
   */
  public String getUserName();

}
