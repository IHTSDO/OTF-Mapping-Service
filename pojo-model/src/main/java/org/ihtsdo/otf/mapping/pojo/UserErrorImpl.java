package org.ihtsdo.otf.mapping.pojo;

import java.util.Date;


import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.UserError;

public class UserErrorImpl implements UserError {

	
  private Long id;
  
  /**  The error. */
  private String error;
  
  /** The note. */
  private String note;
  
  /** The timestamp. */
  private Date timestamp = new Date();

	/**  The user in error. */
	private MapUser userInError;
	
	/**  The user reporting error. */
	private MapUser userReportingError;
	
  /**
   *  Default constructor.
   */	
  public UserErrorImpl() {
  }
  
	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public void setMapUserInError(MapUser mapUser) {
		userInError = mapUser;
	}

	@Override
	public MapUser getMapUserInError() {
		return userInError;
	}

	@Override
	public void setMapUserReporting(MapUser mapUser) {
		userReportingError = mapUser;
	}

	@Override
	public MapUser getMapUserReporting() {
		return userReportingError;
	}

	@Override
	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public String getNote() {
		return note;
	}

	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	public void setMapError(String mapError) {
		this.error = mapError;
	}

	@Override
	public String getMapError() {
		return error;
	}

}
