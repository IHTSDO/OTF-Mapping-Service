package org.ihtsdo.otf.mapping.pojo;

import java.util.Date;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Reference implementation of {@link MapNote}.
 * 
 */
public class MapNoteImpl implements MapNote {

  /** The id. */
  private Long id;

  /** The user. */
  private MapUser user;

  /** The note. */
  private String note;

  /** The timestamp. */
  private Date timestamp;

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
   * @see org.ihtsdo.otf.mapping.model.MapNote#getUser()
   */
  @Override
  public MapUser getUser() {
    return user;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapNote#setUser(org.ihtsdo.otf.mapping.model
   * .MapUser)
   */
  @Override
  public void setUser(MapUser user) {
    this.user = user;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapNote#getNote()
   */
  @Override
  public String getNote() {
    return note;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapNote#setNote(java.lang.String)
   */
  @Override
  public void setNote(String note) {
    this.note = note;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapNote#getTimestamp()
   */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapNote#setTimestamp(java.lang.String)
   */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MapNoteImpl [id=" + id + ", user=" + user + ", note=" + note
        + ", timestamp=" + timestamp + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((note == null) ? 0 : note.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    result = prime * result + ((user == null) ? 0 : user.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapNoteImpl other = (MapNoteImpl) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (note == null) {
      if (other.note != null)
        return false;
    } else if (!note.equals(other.note))
      return false;
    if (timestamp == null) {
      if (other.timestamp != null)
        return false;
    } else if (!timestamp.equals(other.timestamp))
      return false;
    if (user == null) {
      if (other.user != null)
        return false;
    } else if (!user.equals(other.user))
      return false;
    return true;
  }

}
