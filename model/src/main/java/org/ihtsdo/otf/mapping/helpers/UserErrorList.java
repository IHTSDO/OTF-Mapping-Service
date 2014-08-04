package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.UserError;

/**
 * Represents a sortable list of {@link UserError}.
 *
 * @author ${author}
 */
public interface UserErrorList extends ResultList<UserError> {


  /**
   * Adds the user error.
   *
   * @param UserError the user error
   */
  public void addUserError(UserError UserError);


  /**
   * Removes the user error.
   *
   * @param UserError the user error
   */
  public void removeUserError(UserError UserError);

  /**
   * Sets the user errors.
   *
   * @param userErrors the user errors
   */
  public void setUserErrors(List<UserError> userErrors);

  /**
   * Returns the user errors.
   *
   * @return the user errors
   */
  public List<UserError> getUserErrors();

}
