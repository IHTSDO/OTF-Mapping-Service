package org.ihtsdo.otf.mapping.jpa.services.rest;

import java.io.InputStream;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;

public interface ReportServiceRest {

	/**
	   * Returns the report definitions.
	   *
	   * @param authToken the auth token
	   * @return the report definition
	   * @throws Exception the exception
	   */
	ReportDefinitionList getReportDefinitions(String authToken) throws Exception;

	/**
	   * Returns the report definitions.
	   *
	   * @param id the id
	   * @param authToken the auth token
	   * @return the report definition
	   * @throws Exception
	   */
	ReportDefinition getReportDefinition(Long id, String authToken) throws Exception;

	/**
	   * Adds the report definitions.
	   *
	   * @param reportDefinition the report definition
	   * @param authToken the auth token
	   * @return the report definition
	   * @throws Exception the exception
	   */
	ReportDefinition addReportDefinition(ReportDefinitionJpa reportDefinition, String authToken) throws Exception;

	/**
	   * Update report definitions.
	   * 
	   * @param definition the definition
	   * @param authToken the auth token
	   * @throws Exception
	   */
	void updateReportDefinitions(ReportDefinitionJpa definition, String authToken) throws Exception;

	/**
	   * Deletes the report definitions.
	   *
	   * @param reportDefinition the report definition
	   * @param authToken the auth token
	   * @throws Exception the exception
	   */
	void removeReportDefinitions(ReportDefinitionJpa reportDefinition, String authToken) throws Exception;

	/**
	   * Add reports.
	   * 
	   * @param report the report
	   * @param authToken the auth token
	   * @throws Exception
	   */
	void addReport(ReportJpa report, String authToken) throws Exception;

	/**
	   * Deletes the report.
	   * 
	   * @param report the report
	   * @param authToken the auth token
	   * @throws Exception
	   */
	void removeReport(ReportJpa report, String authToken) throws Exception;

	/**
	   * Returns the report.
	   *
	   * @param projectId the project id
	   * @param id the id
	   * @param authToken the auth token
	   * @return the report
	   * @throws Exception the exception
	   */
	Report getReport(Long projectId, Long id, String authToken) throws Exception;

	/**
	   * Returns the reports for map project.
	   * 
	   * @param projectId the project id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the reports for map project
	   * @throws Exception
	   */
	ReportList getReportsForMapProject(Long projectId, PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Returns the reports for map project and report type.
	   * 
	   * @param projectId the project id
	   * @param definitionId the definition id
	   * @param pfsParameter the pfs parameter
	   * @param authToken the auth token
	   * @return the reports for map project and report type
	   * @throws Exception
	   */
	ReportList getReportsForMapProjectAndReportDefinition(Long projectId, Long definitionId,
			PfsParameterJpa pfsParameter, String authToken) throws Exception;

	/**
	   * Returns the most recent report for map project and report type.
	   * 
	   * @param projectId the project id
	   * @param definitionId the definition id
	   * @param authToken the auth token
	   * @return the reports for map project and report type
	   * @throws Exception
	   */
	Report getLatestReport(Long projectId, Long definitionId, String authToken) throws Exception;

	/**
	   * Generate report.
	   * @param reportDefinition the report definition
	   * 
	   * @param projectId the project id
	   * @param userName the user name
	   * @param authToken the auth token
	   * @return the report
	   * @throws Exception
	   */
	Report generateReport(ReportDefinitionJpa reportDefinition, Long projectId, String userName, String authToken)
			throws Exception;

	/**
	   * Tests the generation of a report.
	   * @param reportDefinition the report definition
	   * 
	   * @param projectId the project id
	   * @param userName the user name
	   * @param authToken the auth token
	   * @return the report
	   * @throws Exception
	   */
	Boolean testReport(ReportDefinitionJpa reportDefinition, Long projectId, String userName, String authToken)
			throws Exception;

	/**
	   * Returns the report result items.
	   * 
	   * @param pfsParameter the pfs parameter
	   * @param reportResultId the report result id
	   * @param authToken the auth token
	   * @return the report results
	   * @throws Exception
	   */
	ReportResultItemList getReportResultItems(PfsParameterJpa pfsParameter, Long reportResultId, String authToken)
			throws Exception;

	/**
	   * Returns the qaCheck definitions.
	   * 
	   * @param authToken the auth token
	   * @return the qaCheck definition
	   * @throws Exception
	   */
	ReportDefinitionList getQACheckDefinitions(String authToken) throws Exception;

	/**
	   * Returns the QA labels.
	   *
	   * @param mapProjectId the map project id
	   * @param authToken the auth token
	   * @return the QA labels
	   * @throws Exception the exception
	   */
	SearchResultList getQALabels(Long mapProjectId, String authToken) throws Exception;

	/**
	   * Export report.
	   * 
	   * @param reportId the report id
	   * @param authToken the auth token
	   * @return the input stream
	   * @throws Exception
	   */
	InputStream exportReport(Long reportId, String authToken) throws Exception;
	
	  /**
	   * Execute report.
	   *
	   * @param authToken the auth token
	   * @param reportName the report name
	   * @return the string
	   * @throws Exception the exception
	   */
	  String executeReport(final String authToken,
	    final String reportName) throws Exception;

}