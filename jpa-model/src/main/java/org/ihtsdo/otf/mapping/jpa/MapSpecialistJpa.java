package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Indexed;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * The Class MapSpecialistJpa.
 *
 */
@Entity
@Table(name = "map_specialists")
@Audited
@Indexed
@XmlRootElement(name="mapSpecialist")
public class MapSpecialistJpa implements MapSpecialist {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The user name. */
	@Column(nullable = false, unique = true, length = 25)
	private String userName;
	
	/** The name. */
	@Column(nullable = false, length = 25)
	private String name;
	
	/** The email. */
	@Column(nullable = false)
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
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		 
		 return this.getId() + "," +
				 this.getUserName() + "," +
				 this.getEmail() + "," +
				 this.getName();
	 }
	
	/**
	 * Tests equality with another map specialist
	 * @param mapSpecialist the map specialist to be compared
	 * @return boolean equality
	 */
	public boolean isEqual(MapSpecialist mapSpecialist) {	
		return (this.id.compareTo(mapSpecialist.getId()) == 0 &&
				this.name.equals(mapSpecialist.getName())) &&
				this.userName.equals(mapSpecialist.getUserName()) &&
				this.email.equals(mapSpecialist.getEmail()) ? true : false;
	}
}
