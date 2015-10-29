package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.reports.Report;

/**
 * Represents a sortable list of {@link Report}.
 */
public interface ReportList extends ResultList<Report> {

  /**
   * Adds the map project.
   * 
   * @param report the map project
   */
  public void addReport(Report report);

  /**
   * Removes the map project.
   * 
   * @param report the map project
   */
  public void removeReport(Report report);

  /**
   * Sets the map projects.
   * 
   * @param reports the new map projects
   */
  public void setReports(List<Report> reports);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<Report> getReports();

}
