package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapLead;

public class MapLeadImpl implements MapLead {

	/** The id. */
	private Long id;
	
	/** The user name. */
	private String userName;
	
	/** The name. */
	private String name;
	
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public void setUserName(String username) {
		this.userName = username;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

}
