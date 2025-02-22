/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.services;

import java.util.Date;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportNote;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;

/**
 * Generically represents a service for accessing and generating reports.
 */
public interface ReportService extends RootService {

  /**
   * Close the service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception;

  /**
   * Gets the reports.
   * 
   * @return the reports
   */
  public ReportList getReports();

  /**
   * Find reports for query.
   * 
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findReportsForQuery(String query,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Gets the report.
   * 
   * @param reportId the report id
   * @return the report
   */
  public Report getReport(Long reportId);

  /**
   * Gets the report.
   *
   * @param reportId the report id
   * @param handleLazyInit the handle lazy init
   * @return the report
   */
  public Report getReport(Long reportId, Boolean handleLazyInit);
  
  /**
   * Adds the report.
   * 
   * @param report the report
   * @return the report
   */
  public Report addReport(Report report);

  /**
   * Update report.
   * 
   * @param report the report
   */
  public void updateReport(Report report);

  /**
   * Removes the remport.
   * 
   * @param reportId the report id
   */
  public void removeReport(Long reportId);

  /**
   * Gets the report result.
   * 
   * @param reportResultId the report result id
   * @return the report result
   */
  public ReportResult getReportResult(Long reportResultId);

  /**
   * Adds the report result.
   * 
   * @param reportResult the report result
   * @return the report result
   */
  public ReportResult addReportResult(ReportResult reportResult);

  /**
   * Update report result.
   * 
   * @param reportResult the report result
   */
  public void updateReportResult(ReportResult reportResult);

  /**
   * Removes the report result.
   * 
   * @param reportResultId the report result id
   */
  public void removeReportResult(Long reportResultId);

  /**
   * Gets the report result item.
   * 
   * @param reportResultItemId the report result item id
   * @return the report result item
   */
  public ReportResultItem getReportResultItem(Long reportResultItemId);

  /**
   * Adds the report result item.
   * 
   * @param reportResultItem the report result item
   * @return the report result item
   */
  public ReportResultItem addReportResultItem(ReportResultItem reportResultItem);

  /**
   * Update report result item.
   * 
   * @param reportResultItem the report result item
   */
  public void updateReportResultItem(ReportResultItem reportResultItem);

  /**
   * Removes the report result item.
   * 
   * @param reportResultItemId the report result item id
   */
  public void removeReportResultItem(Long reportResultItemId);

  /**
   * Gets the report note.
   * 
   * @param reportNoteId the report note id
   * @return the report note
   */
  public ReportNote getReportNote(Long reportNoteId);

  /**
   * Adds the report note.
   * 
   * @param reportNote the report note
   * @return the report note
   */
  public ReportNote addReportNote(ReportNote reportNote);

  /**
   * Update report note.
   * 
   * @param reportNote the report note
   */
  public void updateReportNote(ReportNote reportNote);

  /**
   * Removes the report note.
   * 
   * @param reportNoteId the report note id
   */
  public void removeReportNote(Long reportNoteId);

  /**
   * Gets the report definitions for role.
   * 
   * @param role the role
   * @return the report definitions for role
   */
  public ReportDefinitionList getReportDefinitionsForRole(MapUserRole role);

  /**
   * Generate report.
   * 
   * @param mapProject the map project
   * @param owner the owner
   * @param name the name
   * @param reportDefinition the report definition
   * @param date the date
   * @param autoGenerated the auto generated
   * @return the report
   * @throws Exception the exception
   */
  public Report generateReport(MapProject mapProject, MapUser owner,
    String name, ReportDefinition reportDefinition, Date date,
    boolean autoGenerated) throws Exception;

  /**
   * Gets the report definitions.
   * 
   * @return the report definitions
   */
  public ReportDefinitionList getReportDefinitions();

  /**
   * Gets the report definition.
   * 
   * @param id the id
   * @return the report definition
   */
  public ReportDefinition getReportDefinition(Long id);

  /**
   * Adds the report definition.
   * 
   * @param reportDefinition the report definition
   * @return the report definition
   */
  public ReportDefinition addReportDefinition(ReportDefinition reportDefinition);

  /**
   * Update report definition.
   * 
   * @param reportDefinition the report definition
   */
  public void updateReportDefinition(ReportDefinition reportDefinition);

  /**
   * Removes the report definition.
   *
   * @param id the id
   * @throws Exception the exception
   */
  public void removeReportDefinition(Long id) throws Exception;

  /**
   * Gets the reports for map project.
   *
   * @param mapProject the map project
   * @param pfsParameter the pfs parameter
   * @return the reports for map project
   */
  ReportList getReportsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter);

  /**
   * Helper function to generate reports for the current day.
   *
   * @param mapProject the map project
   * @param mapUser the map user
   * @throws Exception the exception
   */
  public void generateDailyReports(MapProject mapProject, MapUser mapUser)
    throws Exception;

  /**
   * Generate reports for date range.
   * 
   * @param mapProject the map project
   * @param mapUser the map user
   * @param startDate the start date
   * @param endDate the end date
   * @throws Exception the exception
   */
  public void generateReportsForDateRange(MapProject mapProject,
    MapUser mapUser, Date startDate, Date endDate) throws Exception;

  /**
   * Removes the reports for map project in the specified date range. If a null
   * start date is passed 01/01/1970 is used. If a null end date is passed "now"
   * is used.
   * If keepManualRuns is set to True, it will only delete auto-run reports, and will keep reports that were run manually 
   *
   * @param mapProject the map project
   * @param startDate the start date
   * @param endDate the end date
   * @param keepManualRuns the keep Manual Runs
   */
  public void removeReportsForMapProject(MapProject mapProject, Date startDate,
    Date endDate, boolean keepManualRuns);

  /**
   * Gets the report result items for report result.
   *
   * @param reportResultId the report result id
   * @param pfsParameter the pfs parameter
   * @return the report result items for report result
   */
  public ReportResultItemList getReportResultItemsForReportResult(
    Long reportResultId, PfsParameter pfsParameter);

  /**
   * Returns the reports for map project and report definition.
   *
   * @param mapProject the map project
   * @param reportDefinition the report definition
   * @param pfsParameter the pfs parameter
   * @return the reports for map project and report definition
   */
  public ReportList getReportsForMapProjectAndReportDefinition(
    MapProject mapProject, ReportDefinition reportDefinition,
    PfsParameter pfsParameter);

  /**
   * Returns the QA check definitions for role.
   *
   * @param role the role
   * @return the QA check definitions for role
   */
  public ReportDefinitionList getQACheckDefinitionsForRole(MapUserRole role);

  /**
   * Returns the QA check definitions.
   *
   * @return the QA check definitions
   */
  public ReportDefinitionList getQACheckDefinitions();

  /**
   * Returns the QA labels.
   *
   * @param mapProjectId the map project id
   * @return the QA labels
   * @throws Exception the exception
   */
  public SearchResultList getQALabels(Long mapProjectId) throws Exception;

  /**
   * Removes the reports for id range.
   *
   * @param startId the start id
   * @param endId the end id
   */
  public void removeReportsForIdRange(Long startId, Long endId);

}
