package org.ihtsdo.otf.mapping.model;

import java.util.Date;


// TODO: Auto-generated Javadoc
/**
 * Represents a mapping editing error caused by a map user.
 * 
 * @author ${author}
 */
public interface UserError {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Sets the map user in error.
   *
   * @param mapUser the map user in error
   */
  public void setMapUserInError(MapUser mapUser);
  
  /**
   * Returns the map user in error.
   *
   * @return the map user in error
   */
  public MapUser getMapUserInError();
  
  /**
   * Sets the map user reporting.
   *
   * @param mapUser the map user reporting
   */
  public void setMapUserReporting(MapUser mapUser);
  
  /**
   * Returns the map user reporting.
   *
   * @return the map user reporting
   */
  public MapUser getMapUserReporting();
  
  /**
   * Sets the note.
   *
   * @param note the note
   */
  public void setNote(String note);
  
  /**
   * Returns the note.
   *
   * @return the note
   */
  public String getNote();
  
  /**
   * Sets the timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(Date timestamp);
  
  /**
   * Returns the timestamp.
   *
   * @return the timestamp
   */
  public Date getTimestamp();
  
  /**
   * Sets the map error.
   *
   * @param mapError the map error
   */
  public void setMapError(String mapError);
  
  /**
   * Returns the map error.
   *
   * @return the map error
   */
  public String getMapError();
  
  

  /**
   * Sets the map record id.
   *
   * @param mapRecordId the new map record id
   */
  public void setMapRecordId(Long mapRecordId);

  /**
   * Gets the map record id.
   *
   * @return the map record id
   */
  public Long getMapRecordId();
  
  

}
