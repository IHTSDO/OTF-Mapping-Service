package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.Feedback;

/**
 * Represents a sortable list of {@link Feedback}.
 */
public interface FeedbackList extends ResultList<Feedback> {

  /**
   * Adds the feedback.
   *
   * @param Feedback the feedback
   */
  public void addFeedback(Feedback Feedback);

  /**
   * Removes the feedback.
   *
   * @param Feedback the feedback
   */
  public void removeFeedback(Feedback Feedback);

  /**
   * Sets the feedbacks.
   *
   * @param userErrors the feedbacks
   */
  public void setFeedbacks(List<Feedback> userErrors);

  /**
   * Returns the feedbacks.
   *
   * @return the feedbacks
   */
  public List<Feedback> getFeedbacks();

}
