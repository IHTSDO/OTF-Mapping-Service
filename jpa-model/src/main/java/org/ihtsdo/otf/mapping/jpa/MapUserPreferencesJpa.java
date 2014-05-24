package org.ihtsdo.otf.mapping.jpa;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;

/**
 * Set of preferences for a user Preferences are accessed via services.
 * 
 * @author Patrick
 */
@Entity
//@UniqueConstraint here is being used to create an index, not to enforce uniqueness
@Table(name = "map_user_preferences", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
      "mapUser_id", "id"
  })
})
@XmlRootElement(name = "mapUserPreferences")
public class MapUserPreferencesJpa implements MapUserPreferences {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The map user id. */
  @OneToOne(targetEntity = MapUserJpa.class)
  private MapUser mapUser;

  /** The time of last login (in ms since 1970). */
  @Column(nullable = false)
  private Long lastLogin = (new Date()).getTime();

  /** The map project id for the project last worked on. */
  @Column(nullable = false)
  private Long mapProjectId;

  /** The map of name->model dashboards. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "map_user_preferences_dashboard_models", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Map<String, String> dashboardModels = new HashMap<>();

  /** Whether this user wants email notifications. */
  @Column(nullable = false)
  private boolean notifiedByEmail;

  /** Whether this user wants notifications in digest form. */
  @Column(nullable = false)
  private boolean digestForm;

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUserPreferences#getId()
   */
  @Override
  public Long getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUserPreferences#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the map user.
   * 
   * @return the map user
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "mapUser")
  public MapUser getMapUser() {
    return mapUser;
  }

  /**
   * Sets the map user.
   * 
   * @param mapUser the new map user
   */
  @Override
  public void setMapUser(MapUser mapUser) {
    this.mapUser = mapUser;
  }

  /**
   * Gets the last login.
   * 
   * @return the last login
   */
  @Override
  public Long getLastLogin() {
    return lastLogin;
  }

  /**
   * Sets the last login.
   * 
   * @param lastLogin the new last login
   */
  @Override
  public void setLastLogin(Long lastLogin) {
    this.lastLogin = lastLogin;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUserPreferences#getLastMapProjectId()
   */
  @Override
  public Long getLastMapProjectId() {
    return mapProjectId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapUserPreferences#setLastMapProjectId(java
   * .lang.Long)
   */
  @Override
  public void setLastMapProjectId(Long mapProjectId) {
    this.mapProjectId = mapProjectId;
  }

  /**
   * Gets the dashboard models.
   * 
   * @return the dashboard models
   */
  @Override
  public Map<String, String> getDashboardModels() {
    return dashboardModels;
  }

  /**
   * Sets the dashboard models.
   * 
   * @param dashboardModels the dashboard models
   */
  @Override
  public void setDashboardModels(Map<String, String> dashboardModels) {
    this.dashboardModels = dashboardModels;
  }

  /**
   * Checks if is email notifications.
   * 
   * @return true, if is email notifications
   */
  @Override
  public boolean isNotifiedByEmail() {
    return notifiedByEmail;
  }

  /**
   * Sets the email notifications.
   * 
   * @param notifiedByEmail the new email notifications
   */
  @Override
  public void setNotifiedByEmail(boolean notifiedByEmail) {
    this.notifiedByEmail = notifiedByEmail;
  }

  /**
   * Checks if is digest form.
   * 
   * @return true, if is digest form
   */
  @Override
  public boolean isDigestForm() {
    return digestForm;
  }

  /**
   * Sets the digest form.
   * 
   * @param digestForm the new digest form
   */
  @Override
  public void setDigestForm(boolean digestForm) {
    this.digestForm = digestForm;
  }

}
