package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultJpa;

/**
 * JAXB enabled implementation of {@link ReportResultList}
 */
@XmlRootElement(name = "reportList")
public class ReportResultListJpa extends AbstractResultList<ReportResult>
    implements ReportResultList {

  /** The map projects. */
  private List<ReportResult> reports = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public ReportResultListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultList#addReportResult(org.ihtsdo.
   * otf.mapping.model.ReportResult)
   */
  @Override
  public void addReportResult(ReportResult ReportResult) {
    reports.add(ReportResult);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultList#removeReportResult(org.
   * ihtsdo .otf.mapping.model.ReportResult)
   */
  @Override
  public void removeReportResult(ReportResult ReportResult) {
    reports.remove(ReportResult);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportResultList#setreports(java.util.
   * List)
   */
  @Override
  public void setReportResults(List<ReportResult> reports) {
    this.reports = new ArrayList<>();
    if (reports != null) {
      this.reports.addAll(reports);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportResultList#getreports()
   */
  @Override
  @XmlElement(type = ReportResultJpa.class, name = "report")
  public List<ReportResult> getReportResults() {
    return reports;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return reports.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<ReportResult> comparator) {
    Collections.sort(reports, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(ReportResult element) {
    return reports.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<ReportResult> getIterable() {
    return reports;
  }

}
