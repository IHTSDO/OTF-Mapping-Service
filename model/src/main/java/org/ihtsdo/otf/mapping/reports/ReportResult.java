package org.ihtsdo.otf.mapping.reports;

import java.util.List;

/**
 * Generically represents a result in a report.
 */
public interface ReportResult {

  /**
   * Returns the report.
   *
   * @return the report
   */
  public Report getReport();

  /**
   * Sets the report.
   *
   * @param report the report
   */
  public void setReport(Report report);

  /**
   * Returns the id.
   *
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   *
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(String name);

  /**
   * Returns the value.
   *
   * @return the value
   */
  public String getValue();

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(String value);

  /**
   * Returns the ct.
   *
   * @return the ct
   */
  public long getCt();

  /**
   * Sets the ct.
   *
   * @param ct the ct
   */
  public void setCt(long ct);

  /**
   * Returns the report result items.
   *
   * @return the report result items
   */
  public List<ReportResultItem> getReportResultItems();

  /**
   * Sets the report result items.
   *
   * @param reportResultItems the report result items
   */
  public void setReportResultItems(List<ReportResultItem> reportResultItems);

  /**
   * Returns the project name.
   *
   * @return the project name
   */
  public String getProjectName();

  /**
   * Sets the project name.
   *
   * @param projectName the project name
   */
  public void setProjectName(String projectName);

  /**
   * Returns the date value.
   *
   * @return the date value
   */
  public String getDateValue();

  /**
   * Sets the date value.
   *
   * @param dateValue the date value
   */
  public void setDateValue(String dateValue);

  /**
   * Adds the report result item.
   *
   * @param reportResultItem the report result item
   */
  public void addReportResultItem(ReportResultItem reportResultItem);

}
