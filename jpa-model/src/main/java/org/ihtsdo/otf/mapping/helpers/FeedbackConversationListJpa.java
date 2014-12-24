package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;


/**
 * JAXB enabled implementation of {@link FeedbackConversationList}.
 */
@XmlRootElement(name = "feedbackConversationList")
public class FeedbackConversationListJpa extends
    AbstractResultList<FeedbackConversation> implements
    FeedbackConversationList {

  /** The user errors. */
  private List<FeedbackConversation> feedbackConversations = new ArrayList<>();

  /**
   * Instantiates a new user error list.
   */
  public FeedbackConversationListJpa() {
    // do nothing
  }

  @Override
  public void addFeedbackConversation(FeedbackConversation feedbackConversation) {
    feedbackConversations.add(feedbackConversation);
  }

  @Override
  public void removeFeedbackConversation(
    FeedbackConversation feedbackConversation) {
    feedbackConversations.remove(feedbackConversation);
  }

  @Override
  public void setFeedbackConversations(
    List<FeedbackConversation> feedbackConversations) {
    this.feedbackConversations = new ArrayList<>();
    this.feedbackConversations.addAll(feedbackConversations);

  }

  @Override
  @XmlElement(type = FeedbackConversationJpa.class, name = "feedbackConversation")
  public List<FeedbackConversation> getFeedbackConversations() {
    return feedbackConversations;
  }

  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return feedbackConversations.size();
  }

  @Override
  public void sortBy(Comparator<FeedbackConversation> comparator) {
    Collections.sort(feedbackConversations, comparator);
  }

  @Override
  public boolean contains(FeedbackConversation element) {
    return feedbackConversations.contains(element);
  }

  @Override
  @XmlTransient
  public Iterable<FeedbackConversation> getIterable() {
    return feedbackConversations;
  }

}
