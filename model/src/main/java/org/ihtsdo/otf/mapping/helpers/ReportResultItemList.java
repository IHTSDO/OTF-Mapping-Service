package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.reports.ReportResultItem;

/**
 * Represents a sortable list of {@link ReportResultItem}.
 */
public interface ReportResultItemList extends ResultList<ReportResultItem> {

  /**
   * Adds the map project.
   * 
   * @param report the map project
   */
  public void addReportResultItem(ReportResultItem report);

  /**
   * Removes the map project.
   * 
   * @param report the map project
   */
  public void removeReportResultItem(ReportResultItem report);

  /**
   * Sets the map projects.
   * 
   * @param reports the new map projects
   */
  public void setReportResultItems(List<ReportResultItem> reports);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<ReportResultItem> getReportResultItems();

}
