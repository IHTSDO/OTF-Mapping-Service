package org.ihtsdo.otf.mapping.model;

import java.util.Map;

public interface MapUserPreferences {

	public Long getId();
	
	public void setId(Long id);
	
	public MapUser getMapUser();

	public void setMapUser(MapUser mapUser);

	public Long getLastLogin();

	public void setLastLogin(Long lastLogin);

	public Long getLastMapProjectId();

	public void setLastMapProjectId(Long lastProjectId);

	public Map<String, String> getDashboardModels();

	public void setDashboardModels(Map<String, String> dashboardModels);

	public void setNotifiedByEmail(boolean notifiedByEmail);

	public boolean isNotifiedByEmail();

	public boolean isDigestForm();

	public void setDigestForm(boolean digestForm);
}
