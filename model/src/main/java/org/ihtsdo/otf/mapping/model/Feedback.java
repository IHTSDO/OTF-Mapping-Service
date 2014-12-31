package org.ihtsdo.otf.mapping.model;

import java.util.Date;
import java.util.Set;

/**
 * Generically represents a single communication within a
 * {@link FeedbackConversation}.
 */
public interface Feedback {

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
   * Sets the map users receiving feedback.
   *
   * @param mapUsers map users receiving feedback
   */
  public void setRecipients(Set<MapUser> mapUsers);

  /**
   * Returns the map users receiving feedback.
   *
   * @return map users receiving feedback
   */
  public Set<MapUser> getRecipients();

  /**
   * Sets the map user sender.
   *
   * @param mapUser the map user sender
   */
  public void setSender(MapUser mapUser);

  /**
   * Returns the map user reporting.
   *
   * @return the map user reporting
   */
  public MapUser getSender();

  /**
   * Sets the message.
   *
   * @param message the message
   */
  public void setMessage(String message);

  /**
   * Returns the message.
   *
   * @return the message
   */
  public String getMessage();

  /**
   * Sets the timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(Date timestamp);

  /**
   * Returns the timestamp.
   *
   * @return the timestamp
   */
  public Date getTimestamp();

  /**
   * Sets the map error.
   *
   * @param mapError the map error
   */
  public void setMapError(String mapError);

  /**
   * Returns the map error.
   *
   * @return the map error
   */
  public String getMapError();

  /**
   * Returns the feedback conversation.
   *
   * @return the feedback conversation
   */
  public FeedbackConversation getFeedbackConversation();

  /**
   * Sets the feedback conversation.
   *
   * @param feedbackConversation the feedback conversation
   */
  public void setFeedbackConversation(FeedbackConversation feedbackConversation);

  /**
   * Indicates whether or not error is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean getIsError();

  /**
   * Sets the error.
   *
   * @param error the error
   */
  public void setIsError(boolean error);

  /**
   * Returns the map users who have viewed this feedback item.
   *
   * @return the viewed by
   */
  public Set<MapUser> getViewedBy();

  /**
   * Sets the map users who have viewed this feedback item.
   *
   * @param viewedBy the viewed by
   */
  public void setViewedBy(Set<MapUser> viewedBy);

  /**
   * Adds a user to the set of those who have viewed this feedback item.
   *
   * @param user the user
   */
  public void addViewedBy(MapUser user);

  /**
   * Removes the user from the set of those who have viewed this feedback item.
   *
   * @param user the user
   */
  public void removeViewedBy(MapUser user);

}
