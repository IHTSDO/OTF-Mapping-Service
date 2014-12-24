package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;

/**
 * JAXB enabled implementation of {@link ReportDefinitionList}.
 */
@XmlRootElement(name = "reportDefinitionList")
public class ReportDefinitionListJpa extends
    AbstractResultList<ReportDefinition> implements ReportDefinitionList {

  /** The map users. */
  private List<ReportDefinition> reportDefinitions = new ArrayList<>();

  /**
   * Instantiates a new map user list.
   */
  public ReportDefinitionListJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link ReportDefinitionListJpa} from the specified
   * parameters.
   *
   * @param reportDefinitions the report definitions
   */
  public ReportDefinitionListJpa(List<ReportDefinition> reportDefinitions) {
    this.reportDefinitions = reportDefinitions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinitionList#addReportDefinition
   * (org.ihtsdo.otf.mapping .model.ReportDefinition)
   */
  @Override
  public void addReportDefinition(ReportDefinition ReportDefinition) {
    reportDefinitions.add(ReportDefinition);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinitionList#removeReportDefinition
   * (org.ihtsdo.otf .mapping.model.ReportDefinition)
   */
  @Override
  public void removeReportDefinition(ReportDefinition ReportDefinition) {
    reportDefinitions.remove(ReportDefinition);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinitionList#setReportDefinitions
   * (java.util.List)
   */
  @Override
  public void setReportDefinitions(List<ReportDefinition> reportDefinitions) {
    this.reportDefinitions = new ArrayList<>();
    this.reportDefinitions.addAll(reportDefinitions);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinitionList#getReportDefinitions()
   */
  @Override
  @XmlElement(type = ReportDefinitionJpa.class, name = "reportDefinition")
  public List<ReportDefinition> getReportDefinitions() {
    return reportDefinitions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return reportDefinitions.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<ReportDefinition> comparator) {
    Collections.sort(reportDefinitions, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(ReportDefinition element) {
    return reportDefinitions.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<ReportDefinition> getIterable() {
    return reportDefinitions;
  }

}
