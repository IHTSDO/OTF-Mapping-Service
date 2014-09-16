package org.ihtsdo.otf.mapping.reports;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.model.MapUser;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractMapReport.
 */
@MappedSuperclass
public abstract class AbstractMapReport implements MapReport {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The owner. */
	@ManyToOne(targetEntity = MapNoteJpa.class)
	private MapUser owner;

	/** The timestamp. */
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;
	
	/** The name. */
	@Column(nullable = false, unique = true, length = 255)
	private String name;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#getId()
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#getOwner()
	 */
	@Override
	public MapUser getOwner() {
		return this.owner;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#setOwner(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void setOwner(MapUser owner) {
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#getTimestamp()
	 */
	@Override
	public Date getTimestamp() {
		return this.timestamp;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReport#setTimestamp()
	 */
	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;

	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

}
