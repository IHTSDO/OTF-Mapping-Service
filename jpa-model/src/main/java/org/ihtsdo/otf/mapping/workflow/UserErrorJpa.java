package org.ihtsdo.otf.mapping.workflow;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity
@Table(name = "user_errors")
@Audited
@XmlRootElement(name = "userError")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserErrorJpa implements UserError {

	

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private Long mapRecordId;
	
	@Column(nullable = false)
	private Date timestamp;
	
	@Column(nullable = false)
	private String error;
	
	@Column(nullable = false)
	private String note;
	
	@ManyToOne(targetEntity=MapUserJpa.class)
	private MapUser userInError;
	
	@ManyToOne(targetEntity=MapUserJpa.class)
	private MapUser userReportingError;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getMapRecordId() {
		return mapRecordId;
	}

	@Override
	public void setMapRecordId(Long mapRecordId) {
		this.mapRecordId = mapRecordId;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String getNote() {
		return note;
	}

	@Override
	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public MapUser getUserInError() {
		return userInError;
	}

	@Override
	public void setUserInError(MapUser userInError) {
		this.userInError = userInError;
	}

	@Override
	public MapUser getUserReportingError() {
		return userReportingError;
	}

	@Override
	public void setUserReportingError(MapUser userReportingError) {
		this.userReportingError = userReportingError;
	}	
	
	@Override
	public String toString() {
		return "UserErrorJpa [id=" + id + ", mapRecordId=" + mapRecordId
				+ ", timestamp=" + timestamp + ", error=" + error + ", note="
				+ note + ", userInError=" + userInError
				+ ", userReportingError=" + userReportingError + "]";
	}
	
}
