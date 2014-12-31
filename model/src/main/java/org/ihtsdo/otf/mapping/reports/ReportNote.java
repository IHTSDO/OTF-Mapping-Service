package org.ihtsdo.otf.mapping.reports;

import java.util.Date;

/**
 * Generically represents a report note.
 */
public interface ReportNote {

  /**
   * Gets the id.
   *
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   *
   * @param id the new id
   */
  public void setId(Long id);

  /**
   * Gets the note.
   *
   * @return the note
   */
  public String getNote();

  /**
   * Sets the note.
   *
   * @param note the new note
   */
  public void setNote(String note);

  /**
   * Gets the timestamp.
   *
   * @return the timestamp
   */
  public Date getTimestamp();

  /**
   * Sets the timestamp.
   *
   * @param timestamp the new timestamp
   */
  public void setTimestamp(Date timestamp);

}
