package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.reports.ReportResult;

/**
 * Represents a sortable list of {@link ReportResult}.
 */
public interface ReportResultList extends ResultList<ReportResult> {

  /**
   * Adds the map project.
   * 
   * @param report the map project
   */
  public void addReportResult(ReportResult report);

  /**
   * Removes the map project.
   * 
   * @param report the map project
   */
  public void removeReportResult(ReportResult report);

  /**
   * Sets the map projects.
   * 
   * @param reports the new map projects
   */
  public void setReportResults(List<ReportResult> reports);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<ReportResult> getReportResults();

}
