package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Reference implementation of {@link MapSpecialist}.
 *
 */
public class MapSpecialistImpl implements MapSpecialist {

	/** The id. */
	private Long id;
	
	/** The user name. */
	private String userName;
	
	/** The name. */
	private String name;
	
	/** The email. */
	private String email;
	
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#getEmail()
	 */
	@Override
	public String getEmail() {
		return email;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapSpecialist#setEmail(java.lang.String)
	 */
	@Override
	public void setEmail(String email) {
		this.email = email;
	}
	
	/**
	 * Tests equality with another map specialist
	 * @param mapSpecialist the map specialist to be compared
	 * @return boolean equality
	 */
	@Override
	public boolean equals(MapSpecialist mapSpecialist) {
		return (this.id.compareTo(mapSpecialist.getId()) == 0 &&
				this.name.equals(mapSpecialist.getName())) &&
				this.userName.equals(mapSpecialist.getUserName()) &&
				this.email.equals(mapSpecialist.getEmail()) ? true : false;
	}

}
