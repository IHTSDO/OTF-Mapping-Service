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
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Map Note Jpa object
 */
@Entity
@Table(name = "map_notes")
@Audited
@XmlRootElement(name="mapNote")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapNoteJpa implements MapNote {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The user. */
	@ManyToOne(targetEntity=MapUserJpa.class)
	private MapUser user;
	
	/** The note. */
	@Column(nullable = false)
	private String note;
	
	/** The timestamp. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = false)
	private Date timestamp;
	
	
	/** Default constructor */
	public MapNoteJpa() {
	}

	/** 
	 * Constructor
	 * @param id the id
	 * @param user the user
	 * @param note the note
	 * @param timestamp the timestamp
	 */
	public MapNoteJpa(Long id, MapUser user, String note, Date timestamp) {
		super();
		this.id = id;
		this.user = user;
		this.note = note;
		this.timestamp = timestamp;
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#getUser()
	 */
	@Override
	@XmlElement(type=MapUserJpa.class)
	public MapUserJpa getUser() {
		return (MapUserJpa) user;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#setUser(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setUser(MapUser user) {
		this.user = user;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#getNote()
	 */
	@Override
	public String getNote() {
		return note;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#setNote(java.lang.String)
	 */
	@Override
	public void setNote(String note) {
		this.note = note;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#getTimestamp()
	 */
	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapNote#setTimestamp(java.lang.String)
	 */
	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapNoteImpl [id=" + id + ", user=" + user + ", note=" + note
				+ ", timestamp=" + timestamp + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((note == null) ? 0 : note.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		MapNoteJpa other = (MapNoteJpa) obj;
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
