package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

/**
 * Custom revision entity for the envers tracking of the set of Mapping Objects
 * @author Patrick
 *
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(MappingRevisionListener.class)
public class MappingRevisionEntity extends DefaultRevisionEntity {
	
	/** Auto-generated serialVersionUID for DefaultRevisionEntity */
	private static final long serialVersionUID = 8817920291933809225L;
	
	/** The name of the user making a modification triggering a revision */
	private String userName;
	
	/**
	 * Sets the user responsible for this revision
	 * @param userName the user's name
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	/**
	 * Returns the user responsible for this revision
	 * @return the user's name
	 */
	public String getUserName() {
		return this.userName;
	}

}
