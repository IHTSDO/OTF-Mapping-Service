package org.ihtsdo.otf.mapping.pojo;

import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

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
	
	/**
	 * Return the id
	 * @return the id
	 */
	@Override
	@XmlTransient
	public Long getId() {
		return this.id;
	}
	
	/**
	 * Set the id
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;		
	}
	
	/**
	 * Returns the id in string form
	 * @return the id in string form
	 */
	@XmlID
	@Override
	public String getObjectId() {
		return id.toString();
	}
	
	/**
	 * Sets the object ID from XML String
	 * @param objectId the object Id as string
	 */
	@Override
	public void setObjectId(String objectId) {
		this.id = new Long(objectId);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapLeadImpl other = (MapLeadImpl) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MapLeadImpl [id=" + id + ", userName=" + userName + ", name="
				+ name + ", email=" + email + "]";
	}
	
}
