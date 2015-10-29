package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportJpa;

/**
 * JAXB enabled implementation of {@link ReportList}
 */
@XmlRootElement(name = "reportList")
public class ReportListJpa extends AbstractResultList<Report> implements
    ReportList {

  /** The map projects. */
  private List<Report> reports = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public ReportListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportList#addReport(org.ihtsdo.
   * otf.mapping.model.Report)
   */
  @Override
  public void addReport(Report Report) {
    reports.add(Report);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportList#removeReport(org.ihtsdo
   * .otf.mapping.model.Report)
   */
  @Override
  public void removeReport(Report Report) {
    reports.remove(Report);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportList#setreports(java.util. List)
   */
  @Override
  public void setReports(List<Report> reports) {
    this.reports = new ArrayList<>();
    if (reports != null) {
      this.reports.addAll(reports);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportList#getreports()
   */
  @Override
  @XmlElement(type = ReportJpa.class, name = "report")
  public List<Report> getReports() {
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
  public void sortBy(Comparator<Report> comparator) {
    Collections.sort(reports, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Report element) {
    return reports.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<Report> getIterable() {
    return reports;
  }

}
