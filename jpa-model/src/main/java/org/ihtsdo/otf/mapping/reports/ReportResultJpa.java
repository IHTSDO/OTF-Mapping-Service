package org.ihtsdo.otf.mapping.reports;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.ContainedIn;

// TODO: Auto-generated Javadoc
/**
 * The Class ReportResultJpa.
 */
@Entity
@Table(name = "report_results")
@XmlRootElement(name = "reportResult")
public class ReportResultJpa implements ReportResult {

	/** The report. */
	@ManyToOne(targetEntity = ReportJpa.class, optional = false)
	@ContainedIn
	private Report report;

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The name. */
	@Column(nullable = true)
	private String name;

	/** The project name. */
	@Column(nullable = true)
	private String projectName;

	/** The value. */
	@Column(nullable = true)
	private String value;
	
	/** The date value string */
	@Column(nullable = true)
	private String dateValue;
	
	/** The qualified user */
	@Column(nullable = true)
	private String qualifiedUserName;

	/** The ct. */
	@Column(nullable = true)
	private long ct;

	/** The report result items. 
	 * NOTE:  These are set to @XmlTransient below due to the potentiall huge size of the list
	 */
	@OneToMany(mappedBy = "reportResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = ReportResultItemJpa.class)
	private List<ReportResultItem> reportResultItems = new ArrayList<>();

	
	@Override
	@XmlTransient
	public Report getReport() {
		return report;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.ReportResult#setReport(org.ihtsdo.otf.
	 * mapping.reports.Report)
	 */
	/**
	 * Sets the report.
	 *
	 * @param report the new report
	 */
	@Override
	public void setReport(Report report) {
		this.report = report;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getId()
	 */
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setId(java.lang.Long)
	 */
	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getName()
	 */
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.ReportResult#setName(java.lang.String)
	 */
	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the project name.
	 *
	 * @return the project name
	 */
	@Override
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Sets the project name.
	 *
	 * @param projectName the new project name
	 */
	@Override
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getValue()
	 */
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	@Override
	public String getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.ReportResult#setValue(java.lang.String)
	 */
	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	
	@Override
	public String getDateValue() {
		return dateValue;
	}
	
	@Override
	public void setDateValue(String dateValue) {
		this.dateValue = dateValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getCt()
	 */
	/**
	 * Gets the ct.
	 *
	 * @return the ct
	 */
	@Override
	public long getCt() {
		return ct;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setCt(long)
	 */
	/**
	 * Sets the ct.
	 *
	 * @param ct the new ct
	 */
	@Override
	public void setCt(long ct) {
		this.ct = ct;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getReportResultItems()
	 */
	/**
	 * Gets the report result items.
	 *
	 * @return the report result items
	 */
	@Override
	@XmlTransient
	public List<ReportResultItem> getReportResultItems() {
		return reportResultItems;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.reports.ReportResult#setReportResultItems(java
	 * .util.List)
	 */
	/**
	 * Sets the report result items.
	 *
	 * @param reportResultItems the new report result items
	 */
	@Override
	public void setReportResultItems(List<ReportResultItem> reportResultItems) {
		this.reportResultItems = reportResultItems;
	}

	@Override
	public void addReportResultItem(ReportResultItem reportResultItem) {
		if (this.reportResultItems == null)
			this.reportResultItems = new ArrayList<>();
		this.reportResultItems.add(reportResultItem);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getNotes()
	 * 
	 * @Override public List<ReportNote> getNotes() { return reportResultNotes;
	 * }
	 * 
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setNotes(java.util.List)
	 * 
	 * @Override public void setNotes(List<ReportNote> reportResultNotes) {
	 * this.reportResultNotes = reportResultNotes; }
	 */

}
