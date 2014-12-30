package org.ihtsdo.otf.mapping.model;

import java.util.Date;

/**
 * Generically represents a map note.
 */
public interface MapNote {

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
   * Returns the id in string form
   * @return the string object id
   */
  public String getObjectId();

  /**
   * Returns the user.
   * 
   * @return the user
   */
  public MapUser getUser();

  /**
   * Sets the user.
   * 
   * @param user the user
   */
  public void setUser(MapUser user);

  /**
   * Returns the note.
   * 
   * @return the note
   */
  public String getNote();

  /**
   * Sets the note.
   * 
   * @param note the note
   */
  public void setNote(String note);

  /**
   * Returns the timestamp.
   * 
   * @return the timestamp
   */
  public Date getTimestamp();

  /**
   * Sets the timestamp.
   * 
   * @param timestamp the timestamp
   */
  public void setTimestamp(Date timestamp);

}
