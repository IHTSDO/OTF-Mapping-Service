package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.FeedbackConversation;

/**
 * Represents a sortable list of {@link FeedbackConversation}.
 */
public interface FeedbackConversationList extends
    ResultList<FeedbackConversation> {

  /**
   * Adds the feedbackConversation.
   *
   * @param FeedbackConversation the feedbackConversation
   */
  public void addFeedbackConversation(FeedbackConversation FeedbackConversation);

  /**
   * Removes the feedbackConversation.
   *
   * @param FeedbackConversation the feedbackConversation
   */
  public void removeFeedbackConversation(
    FeedbackConversation FeedbackConversation);

  /**
   * Sets the feedbackConversations.
   *
   * @param userErrors the feedbackConversations
   */
  public void setFeedbackConversations(List<FeedbackConversation> userErrors);

  /**
   * Returns the feedbackConversationConversations.
   *
   * @return the feedbackConversations
   */
  public List<FeedbackConversation> getFeedbackConversations();

}
