package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportNote;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;

/**
 * The Interface ReportService.
 */
public interface ReportService {

	
	/**
	 * Close the service.
	 * 
	 * @throws Exception
	 *             the exception
	 */
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
	public SearchResultList findReportsForQuery(String query, PfsParameter pfsParameter)
			throws Exception;
	
	/**
	 * Gets the report.
	 *
	 * @param reportId the report id
	 * @return the report
	 */
	public Report getReport(Long reportId);
	
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





}
