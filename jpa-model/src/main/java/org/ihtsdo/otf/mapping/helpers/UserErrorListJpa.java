package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.jpa.UserErrorJpa;
import org.ihtsdo.otf.mapping.model.UserError;

/**
 * JAXB enabled implementation of {@link UserErrorList}.
 */
@XmlRootElement(name = "userErrorList")
public class UserErrorListJpa extends AbstractResultList<UserError> implements
    UserErrorList {

  /** The user errors. */
  private List<UserError> userErrors = new ArrayList<>();

  /**
   * Instantiates a new user error list.
   */
  public UserErrorListJpa() {
    // do nothing
  }


  @Override
  public void addUserError(UserError userError) {
    userErrors.add(userError);
  }


  @Override
  public void removeUserError(UserError userError) {
    userErrors.remove(userError);
  }


  @Override
  public void setUserErrors(List<UserError> userErrors) {
    this.userErrors = new ArrayList<>();
    this.userErrors.addAll(userErrors);

  }

 
  @Override
  @XmlElement(type = UserErrorJpa.class, name = "userError")
  public List<UserError> getUserErrors() {
    return userErrors;
  }


  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return userErrors.size();
  }

 
  @Override
  public void sortBy(Comparator<UserError> comparator) {
    Collections.sort(userErrors, comparator);
  }


  @Override
  public boolean contains(UserError element) {
    return userErrors.contains(element);
  }


  @Override
  public Iterable<UserError> getIterable() {
    return userErrors;
  }

}
