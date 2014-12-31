package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.reports.ReportDefinition;

/**
 * Represents a sortable list of {@link ReportDefinition}.
 */
public interface ReportDefinitionList extends ResultList<ReportDefinition> {

  /**
   * Adds the map project.
   * 
   * @param reportDefinition the map project
   */
  public void addReportDefinition(ReportDefinition reportDefinition);

  /**
   * Removes the map project.
   * 
   * @param reportDefinition the map project
   */
  public void removeReportDefinition(ReportDefinition reportDefinition);

  /**
   * Sets the map projects.
   * 
   * @param reportDefinitions the new map projects
   */
  public void setReportDefinitions(List<ReportDefinition> reportDefinitions);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<ReportDefinition> getReportDefinitions();

}
