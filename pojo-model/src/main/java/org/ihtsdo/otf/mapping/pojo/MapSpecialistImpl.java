package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Reference implementation of {@link MapSpecialist}.
 */
public class MapSpecialistImpl implements MapSpecialist {

	/** The id. */
	private Long id;
	
	/** The user name. */
	private String userName;
	
	/** The name. */
	private String name;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#getUserName()
	 */
	@Override
	public String getUserName() {
		return userName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#setUserName(java.lang.String)
	 */
	@Override
	public void setUserName(String username) {
		this.userName = username;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

}
