package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapLead;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapLead}.
 * Includes hibernate tags for MEME database.
 *
 * @author ${author}
 */
public class MapLeadImpl implements MapLead {

	/** The id. */
	private Long id;
	
	/** The user name. */
	private String userName;
	
	/** The name. */
	private String name;
	
	/** The email. */
	private String email;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#getUserName()
	 */
	@Override
	public String getUserName() {
		return userName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#setUserName(java.lang.String)
	 */
	@Override
	public void setUserName(String username) {
		this.userName = username;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#getEmail()
	 */
	@Override
	public String getEmail() {
		return email;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapLead#setEmail(java.lang.String)
	 */
	@Override
	public void setEmail(String email) {
		this.email = email;
	}
	
	/**
	 * Tests equality with an object
	 * @param o the object to be compared
	 * @return boolean equality
	 */
	@Override
	public boolean equals(Object o) {

		if (o == this) {
	        return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        
        MapLead m = (MapLead) o;


		return ((this.id.compareTo(m.getId()) == 0) &&
				 this.name.equals(m.getName())) &&
				 this.userName.equals(m.getUserName()) &&
				 this.email.equals(m.getEmail())
						 ? true : false;
	}

}
