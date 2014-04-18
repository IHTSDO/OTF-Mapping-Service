package org.ihtsdo.otf.mapping.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;

// TODO: Auto-generated Javadoc
/**
 * Set of preferences for a user
 * Preferences are accessed via services.
 *
 * @author Patrick
 */
/*@Entity
@Table(name="map_user_preferences",  uniqueConstraints={
		   @UniqueConstraint(columnNames={"mapUser", "id"})})
@XmlRootElement(name="mapUserPreferences")*/
public class MapUserPreferencesJpa implements MapUserPreferences {

	@Id
	@GeneratedValue
	private Long id;
	
	/** The map user id. */
	@OneToOne(targetEntity=MapUserJpa.class)
	private MapUser mapUser;
	
	/** The time of last login (in ms since 1970). */
	private Long lastLogin;
	
	/** The map project id for the project last worked on. */
	@ManyToOne(targetEntity=MapProjectJpa.class)
	private MapProject lastProject;
	
	/** The map of name->model dashboards. */
	// dashboardModel -> {model: {rows {columns ..... widgets }}}
	private Map<String, String> dashboardModels = new HashMap<>();
	
	/** Whether this user wants email notifications. */
	private boolean notifiedByEmail;
	
	/** Whether this user wants notifications in digest form. */
	private boolean digestForm;


	/**
	 * Gets the map user.
	 *
	 * @return the map user
	 */
	@Override
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

	/**
	 * Gets the last project.
	 *
	 * @return the last project
	 */
	@Override
	public MapProject getLastProject() {
		return lastProject;
	}

	/**
	 * Sets the last project.
	 *
	 * @param lastProject the new last project
	 */
	@Override
	public void setLastProject(MapProject lastProject) {
		this.lastProject = lastProject;
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
