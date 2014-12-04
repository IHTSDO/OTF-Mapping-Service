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
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;

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
  private String itemId;

  /** The item name. */
  @Column(nullable = true)
  private String itemName;

  /** The result type. */
  @Enumerated(EnumType.STRING)
  private ReportResultType resultType;

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getId()
   */
  @Override
  public Long getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
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
  @XmlTransient
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getItemId()
   */
  @Override
  public String getItemId() {
    return itemId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.ReportResultItem#setItemId(java.lang.Long)
   */
  @Override
  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportResultItem#getResultType()
   */
  @Override
  public ReportResultType getResultType() {
    return resultType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.ReportResultItem#setResultType(org.ihtsdo
   * .otf.mapping.helpers.ReportResultType)
   */
  @Override
  public void setResultType(ReportResultType resultType) {
    this.resultType = resultType;
  }

  @Override
  public String getItemName() {
    return this.itemName;
  }

  @Override
  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((itemId == null) ? 0 : itemId.hashCode());
    result = prime * result + ((itemName == null) ? 0 : itemName.hashCode());
    result =
        prime * result + ((resultType == null) ? 0 : resultType.hashCode());
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
    ReportResultItemJpa other = (ReportResultItemJpa) obj;
    if (itemId == null) {
      if (other.itemId != null)
        return false;
    } else if (!itemId.equals(other.itemId))
      return false;
    if (itemName == null) {
      if (other.itemName != null)
        return false;
    } else if (!itemName.equals(other.itemName))
      return false;
    if (resultType != other.resultType)
      return false;
    return true;
  }

}
