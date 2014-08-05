package org.ihtsdo.otf.mapping.pojo;

import java.util.Date;

import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.UserError;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link UserError}.
 * Includes hibernate tags for MEME database.
 *
 * @author ${author}
 */
public class UserErrorImpl implements UserError {

	
  /**  The id. */
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
	
	/** The map record. */
	private MapRecord mapRecord;
	
	
  /**
   *  Default constructor.
   */	
  public UserErrorImpl() {
  }
  
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapUserInError(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setMapUserInError(MapUser mapUser) {
		userInError = mapUser;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapUserInError()
	 */
	@Override
	public MapUser getMapUserInError() {
		return userInError;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapUserReporting(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setMapUserReporting(MapUser mapUser) {
		userReportingError = mapUser;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#getMapUserReporting()
	 */
	@Override
	public MapUser getMapUserReporting() {
		return userReportingError;
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
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.UserError#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}


}
