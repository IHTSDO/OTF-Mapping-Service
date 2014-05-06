package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class MapUserJpa.
 * 
 * @author ${author}
 */
@Entity
@Table(name = "map_users")
@Audited
@XmlRootElement(name = "mapUser")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapUserJpa implements MapUser {

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

  /** The default constructor */
  public MapUserJpa() {
  }

  /**
   * Return the id
   * @return the id
   */
  @Override
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#getUserName()
   */
  @Override
  public String getUserName() {
    return userName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#setUserName(java.lang.String)
   */
  @Override
  public void setUserName(String username) {
    this.userName = username;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#getEmail()
   */
  @Override
  public String getEmail() {
    return email;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#setEmail(java.lang.String)
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

    return this.getId() + "," + this.getUserName() + "," + this.getEmail()
        + "," + this.getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((email == null) ? 0 : email.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((userName == null) ? 0 : userName.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapUserJpa other = (MapUserJpa) obj;
    if (email == null) {
      if (other.email != null)
        return false;
    } else if (!email.equals(other.email))
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

}
