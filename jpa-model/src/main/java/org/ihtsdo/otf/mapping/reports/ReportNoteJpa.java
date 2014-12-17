package org.ihtsdo.otf.mapping.reports;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class ReportNoteJpa.
 */
@Entity
@Table(name = "report_notes")
@XmlRootElement(name = "reportNote")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportNoteJpa implements ReportNote {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The note. */
  @Column(nullable = false)
  private String note;

  /** The timestamp. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  private Date timestamp = new Date();

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#getId()
   */
  @Override
  public Long getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#getNote()
   */
  @Override
  public String getNote() {
    return note;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#setNote(java.lang.String)
   */
  @Override
  public void setNote(String note) {
    this.note = note;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#getTimestamp()
   */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportNote#setTimestamp(java.util.Date)
   */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
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
    result = prime * result + ((note == null) ? 0 : note.hashCode());
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
    ReportNoteJpa other = (ReportNoteJpa) obj;
    if (note == null) {
      if (other.note != null)
        return false;
    } else if (!note.equals(other.note))
      return false;
    return true;
  }

}
