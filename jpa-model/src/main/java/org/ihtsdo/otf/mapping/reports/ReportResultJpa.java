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
	@ManyToOne(targetEntity=ReportJpa.class, optional = false)
	@ContainedIn
	private Report report;

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The name. */
	@Column(nullable = true)
	private String name;
	
	/** The value. */
	@Column(nullable = true)
	private String value;
	
	/** The ct. */
	@Column(nullable = true)
	private long ct;
	
	/** The report result items. */
	@OneToMany(mappedBy = "reportResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = ReportResultItemJpa.class)
	private List<ReportResultItem> reportResultItems = new ArrayList<>();
	
	/** The reportResultNotes. *//*
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity = ReportNoteJpa.class)
	private List<ReportNote> reportResultNotes = new ArrayList<>();
*/
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getReport()
	 */
	@Override
	public Report getReport() {
		return report;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setReport(org.ihtsdo.otf.mapping.reports.Report)
	 */
	@Override
	public void setReport(Report report) {
		this.report = report;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getValue()
	 */
	@Override
	public String getValue() {
		return value;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setValue(java.lang.String)
	 */
	@Override
	public void setValue(String value) {
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getCt()
	 */
	@Override
	public long getCt() {
		return ct;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setCt(long)
	 */
	@Override
	public void setCt(long ct) {
		this.ct = ct;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getReportResultItems()
	 */
	@Override
	public List<ReportResultItem> getReportResultItems() {
		return reportResultItems;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setReportResultItems(java.util.List)
	 */
	@Override
	public void setReportResultItems(List<ReportResultItem> reportResultItems) {
		this.reportResultItems = reportResultItems;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#getNotes()
	 
	@Override
	public List<ReportNote> getNotes() {
		return reportResultNotes;
	}

	 (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResult#setNotes(java.util.List)
	 
	@Override
	public void setNotes(List<ReportNote> reportResultNotes) {
		this.reportResultNotes = reportResultNotes;
	}*/

}
