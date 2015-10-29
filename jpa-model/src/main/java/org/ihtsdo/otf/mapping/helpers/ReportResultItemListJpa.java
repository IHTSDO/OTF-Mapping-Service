package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.reports.ReportResultItemJpa;

/**
 * JAXB enabled implementation of {@link ReportResultItemList}
 */
@XmlRootElement(name = "reportResultItemList")
public class ReportResultItemListJpa extends
    AbstractResultList<ReportResultItem> implements ReportResultItemList {

  /** The map projects. */
  private List<ReportResultItem> reportResultItems = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public ReportResultItemListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultItemList#addReportResultItem
   * (org.ihtsdo. otf.mapping.model.ReportResultItem)
   */
  @Override
  public void addReportResultItem(ReportResultItem reportResultItem) {
    reportResultItems.add(reportResultItem);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultItemList#removeReportResultItem
   * (org.ihtsdo .otf.mapping.model.ReportResultItem)
   */
  @Override
  public void removeReportResultItem(ReportResultItem reportResultItem) {
    reportResultItems.remove(reportResultItem);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultItemList#setReportResultItems
   * (java.util. List)
   */
  @Override
  public void setReportResultItems(List<ReportResultItem> reportResultItems) {
    this.reportResultItems = new ArrayList<>();
    if (reportResultItems != null) {
      this.reportResultItems.addAll(reportResultItems);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportResultItemList#getReportResultItems()
   */
  @Override
  @XmlElement(type = ReportResultItemJpa.class, name = "reportResultItem")
  public List<ReportResultItem> getReportResultItems() {
    return reportResultItems;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return reportResultItems.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<ReportResultItem> comparator) {
    Collections.sort(reportResultItems, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(ReportResultItem element) {
    return reportResultItems.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<ReportResultItem> getIterable() {
    return reportResultItems;
  }

}
