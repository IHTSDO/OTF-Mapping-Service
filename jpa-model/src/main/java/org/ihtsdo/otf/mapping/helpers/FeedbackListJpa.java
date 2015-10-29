package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.FeedbackJpa;
import org.ihtsdo.otf.mapping.model.Feedback;

/**
 * JAXB enabled implementation of {@link FeedbackList}.
 */
@XmlRootElement(name = "feedbackList")
public class FeedbackListJpa extends AbstractResultList<Feedback> implements
    FeedbackList {

  /** The user errors. */
  private List<Feedback> feedbacks = new ArrayList<>();

  /**
   * Instantiates a new user error list.
   */
  public FeedbackListJpa() {
    // do nothing
  }

  @Override
  public void addFeedback(Feedback feedback) {
    feedbacks.add(feedback);
  }

  @Override
  public void removeFeedback(Feedback feedback) {
    feedbacks.remove(feedback);
  }

  @Override
  public void setFeedbacks(List<Feedback> feedbacks) {
    this.feedbacks = new ArrayList<>();
    this.feedbacks.addAll(feedbacks);

  }

  @Override
  @XmlElement(type = FeedbackJpa.class, name = "feedback")
  public List<Feedback> getFeedbacks() {
    return feedbacks;
  }

  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return feedbacks.size();
  }

  @Override
  public void sortBy(Comparator<Feedback> comparator) {
    Collections.sort(feedbacks, comparator);
  }

  @Override
  public boolean contains(Feedback element) {
    return feedbacks.contains(element);
  }

  @Override
  @XmlTransient
  public Iterable<Feedback> getIterable() {
    return feedbacks;
  }

}
