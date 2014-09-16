package org.ihtsdo.otf.mapping.reports;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Class MapReportSpecialistOutputJpa.
 */
@Entity
@Table(name = "map_report_specialist_output")
@Audited
@XmlRootElement(name = "mapReportSpecialistOutput")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapReportSpecialistOutputJpa extends AbstractMapReport implements
		MapReportSpecialistOutput {

	/** The start date. */
	@Column(nullable = false)
	private Date startDate;

	/** The end date. */
	@Column(nullable = false)
	private Date endDate;

	/** The map project id. */
	@Column(nullable = false)
	private Long mapProjectId;

	/** The map record ids. */
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "map_report_specialist_output_map_records", joinColumns = @JoinColumn(name = "id"))
	@Column(nullable = true)
	private Set<Long> mapRecordIds = new HashSet<>();

	/** The total count. */
	@Column(nullable = false)
	private int totalCount;

	/** The finished count. */
	@Column(nullable = false)
	private int finishedCount;

	/** The conflict count. */
	@Column(nullable = false)
	private int conflictCount;

	/** The error count. */
	@Column(nullable = false)
	private int errorCount;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getStartDate()
	 */
	@Override
	public Date getStartDate() {
		return startDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setStartDate
	 * (java.util.Date)
	 */
	@Override
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getEndDate()
	 */
	@Override
	public Date getEndDate() {
		return endDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setEndDate(java
	 * .util.Date)
	 */
	@Override
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getMapProjectId
	 * ()
	 */
	@Override
	public Long getMapProjectId() {
		return mapProjectId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setMapProjectId
	 * (java.lang.Long)
	 */
	@Override
	public void setMapProjectId(Long mapProjectId) {
		this.mapProjectId = mapProjectId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getMapRecordIds
	 * ()
	 */
	@Override
	public Set<Long> getMapRecordIds() {
		return mapRecordIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setMapRecordIds
	 * (java.util.Set)
	 */
	@Override
	public void setMapRecordIds(Set<Long> mapRecordIds) {
		this.mapRecordIds = mapRecordIds;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#addMapRecordId(java.lang.Long)
	 */
	@Override
	public void addMapRecordId(Long id) {
		this.mapRecordIds.add(id);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#removeMapRecordId(java.lang.Long)
	 */
	@Override
	public void removeMapRecordId(Long id) {
		this.mapRecordIds.remove(id);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getTotalCount()
	 */
	@Override
	public int getTotalCount() {
		return totalCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setTotalCount(int)
	 */
	@Override
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getFinishedCount()
	 */
	@Override
	public int getFinishedCount() {
		return finishedCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setFinishedCount(int)
	 */
	@Override
	public void setFinishedCount(int finishedCount) {
		this.finishedCount = finishedCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getConflictCount()
	 */
	@Override
	public int getConflictCount() {
		return conflictCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setConflictCount(int)
	 */
	@Override
	public void setConflictCount(int conflictCount) {
		this.conflictCount = conflictCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#getErrorCount()
	 */
	@Override
	public int getErrorCount() {
		return errorCount;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput#setErrorCount(int)
	 */
	@Override
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

}
