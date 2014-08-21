package org.ihtsdo.otf.mapping.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.UserError;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class UserErrorJpa.
 *
 * @author ${author}
 */
@Entity
@Table(name = "user_errors")
@Audited
@XmlRootElement(name = "userError")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserErrorJpa implements UserError {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;
  
  /**  The error. */
  @Column(nullable = false, length = 4000)
  private String error;
  
  /** The note. */
  @Column(nullable = false, length = 4000)
  private String note;
  
  /** The timestamp. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  private Date timestamp = new Date();

	/**  The user in error. */
	@ManyToOne(targetEntity = MapUserJpa.class)
	private MapUser userInError;
	
	/**  The user reporting error. */
	@ManyToOne(targetEntity = MapUserJpa.class)
	private MapUser userReportingError;
	
	/** The map record. */
	@Column(nullable = false)
	private Long mapRecordId;
	
  /**
   *  Default constructor.
   */	
  public UserErrorJpa() {
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
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapUserInError(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setMapUserInError(MapUser userInError) {
		this.userInError = userInError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapUserInError()
	 */
	@Override
  @XmlElement(type = MapUserJpa.class)
	public MapUserJpa getMapUserInError() {
		return (MapUserJpa) userInError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapUserReporting(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setMapUserReporting(MapUser userReportingError) {
		this.userReportingError = userReportingError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapUserReporting()
	 */
	@Override
  @XmlElement(type = MapUserJpa.class)
	public MapUserJpa getMapUserReporting() {
		return (MapUserJpa) userReportingError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setNote(java.lang.String)
	 */
	@Override
	public void setNote(String note) {
		this.note = note;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getNote()
	 */
	@Override
	public String getNote() {
		return note;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setTimestamp(java.util.Date)
	 */
	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getTimestamp()
	 */
	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapError(java.lang.String)
	 */
	@Override
	public void setMapError(String mapError) {
		this.error = mapError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapError()
	 */
	@Override
	public String getMapError() {
		return error;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapRecord()
	 */
	@Override
	public Long getMapRecordId() {
		return mapRecordId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecordId(Long mapRecordId) {
		this.mapRecordId = mapRecordId;
	}

}
