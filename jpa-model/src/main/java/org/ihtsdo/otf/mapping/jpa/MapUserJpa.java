/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A JPA-enabled implementation of {@link MapUser}.
 */
@Entity
@Table(name = "map_users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "userName"
    })
})
@Audited
@XmlRootElement(name = "mapUser")
@JsonIgnoreProperties(ignoreUnknown = true, value = {
    "hibernateLazyInitializer", "handler"
})
public class MapUserJpa implements MapUser {

  /** The id. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The user name. */
  @Column(nullable = false, unique = true)
  private String userName;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The email. */
  @Column(nullable = false)
  private String email;

  /** The application role. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MapUserRole applicationRole;

  /** The team. */
  @Column(nullable = true)
  private String team;

  // Not a column
  /** The auth token. */
  @Transient
  private String authToken;

  /**
   * The default constructor.
   */
  public MapUserJpa() {
  }

  /**
   * Instantiates a new map user jpa.
   *
   * @param mapUser the map user
   */
  public MapUserJpa(MapUser mapUser) {
    super();
    this.id = mapUser.getId();
    this.userName = mapUser.getUserName();
    this.name = mapUser.getName();
    this.email = mapUser.getEmail();
    this.team = mapUser.getTeam();
    this.applicationRole = mapUser.getApplicationRole();
    this.authToken = mapUser.getAuthToken();
  }

  /**
   * Return the id.
   *
   * @return the id
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.
   *
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form.
   *
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
  /**
   * Returns the user name.
   *
   * @return the user name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getUserName() {
    return userName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#setUserName(java.lang.String)
   */
  /**
   * Sets the user name.
   *
   * @param username the user name
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
  /**
   * Returns the name.
   *
   * @return the name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapUser#setName(java.lang.String)
   */
  /**
   * Sets the name.
   *
   * @param name the name
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getEmail() {
    return email;
  }

  /* see superclass */
  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTeam() {
    return team;
  }

  /* see superclass */
  @Override
  public void setTeam(String team) {
    this.team = team;
  }

  /**
   * Returns the application role.
   *
   * @return the application role
   */
  @Override
  public MapUserRole getApplicationRole() {
    return applicationRole;
  }

  /**
   * Sets the application role.
   *
   * @param role the application role
   */
  @Override
  public void setApplicationRole(MapUserRole role) {
    this.applicationRole = role;
  }

  /* see superclass */
  @Override
  public String toString() {

    return this.getId() + "," + this.getUserName() + "," + this.getEmail() + ","
        + this.getName() + "," + this.getApplicationRole().getValue();
  }

  /* see superclass */
  @Override
  public String getAuthToken() {
    return authToken;
  }

  /* see superclass */
  @Override
  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((applicationRole == null) ? 0 : applicationRole.hashCode());
    result = prime * result + ((email == null) ? 0 : email.hashCode());
    result = prime * result + ((team == null) ? 0 : team.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((userName == null) ? 0 : userName.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
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
    if (applicationRole != other.applicationRole)
      return false;
    if (email == null) {
      if (other.email != null)
        return false;
    } else if (!email.equals(other.email))
      return false;
    if (team == null) {
      if (other.team != null)
        return false;
    } else if (!team.equals(other.team))
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
