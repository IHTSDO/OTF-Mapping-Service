package org.ihtsdo.otf.mapping.model;

import java.util.Map;

/**
 * Generically represents a user's preferences within the application. This can
 * be greatly expanded to cover other aspects of application state.
 */
public interface MapUserPreferences {

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
   * Returns the map user.
   * 
   * @return the map user
   */
  public MapUser getMapUser();

  /**
   * Sets the map user.
   * 
   * @param mapUser the map user
   */
  public void setMapUser(MapUser mapUser);

  /**
   * Returns the last login.
   * 
   * @return the last login
   */
  public Long getLastLogin();

  /**
   * Sets the last login.
   * 
   * @param lastLogin the last login
   */
  public void setLastLogin(Long lastLogin);

  /**
   * Returns the last map project id.
   * 
   * @return the last map project id
   */
  public Long getLastMapProjectId();

  /**
   * Sets the last map project id.
   * 
   * @param lastProjectId the last map project id
   */
  public void setLastMapProjectId(Long lastProjectId);

  /**
   * Returns the dashboard models.
   * 
   * @return the dashboard models
   */
  public Map<String, String> getDashboardModels();

  /**
   * Sets the dashboard models.
   * 
   * @param dashboardModels the dashboard models
   */
  public void setDashboardModels(Map<String, String> dashboardModels);

  /**
   * Sets the notified by email.
   * 
   * @param notifiedByEmail the notified by email
   */
  public void setNotifiedByEmail(boolean notifiedByEmail);

  /**
   * Indicates whether or not notified by email is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isNotifiedByEmail();

  /**
   * Indicates whether or not digest form is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isDigestForm();

  /**
   * Sets the digest form.
   * 
   * @param digestForm the digest form
   */
  public void setDigestForm(boolean digestForm);

  /**
   * Adds the dashboard model.
   *
   * @param name the name
   * @param model the model
   */
  public void addDashboardModel(String name, String model);

  /**
   * Removes the dashboard model.
   *
   * @param name the name
   */
  public void removeDashboardModel(String name);
}
