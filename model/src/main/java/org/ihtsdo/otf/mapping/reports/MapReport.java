package org.ihtsdo.otf.mapping.reports;

import java.util.Date;

import org.ihtsdo.otf.mapping.model.MapUser;

public interface MapReport {

	public Long getId();
	
	public void setId(Long id);
	
	public MapUser getOwner();
	
	public void setOwner(MapUser owner);
	
	public Date getTimestamp();
	
	public void setTimestamp(Date timestamp);

	public String getName();

	public void setName(String name);

	
	
}
