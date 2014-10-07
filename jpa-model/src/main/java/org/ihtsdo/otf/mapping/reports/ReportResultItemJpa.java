package org.ihtsdo.otf.mapping.reports;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;

// TODO: Auto-generated Javadoc
/**
 * The Class ReportResultItemJpa.
 */
@Entity
@Table(name = "report_result_items")
@XmlRootElement(name = "reportResultItem")
public class ReportResultItemJpa implements ReportResultItem {

	/** The report result. */
	@ContainedIn
	@ManyToOne(targetEntity = ReportResultJpa.class, optional = false)
	private ReportResult reportResult;

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The item id. */
	@Column(nullable = false)
	private Long itemId;

	/** The result type. */
	@Enumerated(EnumType.STRING)
	private ReportResultType resultType;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the report result.
	 *
	 * @return the report result
	 */
	@Override
	public ReportResult getReportResult() {
		return reportResult;
	}

	/**
	 * Sets the report result.
	 *
	 * @param reportResult the new report result
	 */
	@Override
	public void setReportResult(ReportResult reportResult) {
		this.reportResult = reportResult;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getItemId()
	 */
	@Override
	public Long getItemId() {
		return itemId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#setItemId(java.lang.Long)
	 */
	@Override
	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getResultType()
	 */
	@Override
	public ReportResultType getResultType() {
		return resultType;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#setResultType(org.ihtsdo.otf.mapping.helpers.ReportResultType)
	 */
	@Override
	public void setResultType(ReportResultType resultType) {
		this.resultType = resultType;
	}

}
