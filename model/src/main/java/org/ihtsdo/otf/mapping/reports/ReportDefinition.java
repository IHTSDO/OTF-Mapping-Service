package org.ihtsdo.otf.mapping.reports;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;
import org.ihtsdo.otf.mapping.helpers.ReportType;

// TODO: Auto-generated Javadoc
/**
 * The Interface ReportDefinition.
 */
public interface ReportDefinition {
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param id the new id
	 */
	public void setId(Long id);

	/**
	 * Gets the report name.
	 *
	 * @return the report name
	 */
	public String getReportName();

	/**
	 * Sets the report name.
	 *
	 * @param reportName the new report name
	 */
	public void setReportName(String reportName);


	/**
	 * Gets the result type.
	 *
	 * @return the result type
	 */
	public ReportResultType getResultType();

	/**
	 * Sets the result type.
	 *
	 * @param resultType the new result type
	 */
	public void setResultType(ReportResultType resultType);

	/**
	 * Gets the query type.
	 *
	 * @return the query type
	 */
	public ReportQueryType getQueryType();

	/**
	 * Sets the query type.
	 *
	 * @param queryType the new query type
	 */
	public void setQueryType(ReportQueryType queryType);

	/**
	 * Gets the query.
	 *
	 * @return the query
	 */
	public String getQuery();

	/**
	 * Sets the query.
	 *
	 * @param query the new query
	 */
	public void setQuery(String query);

	/**
	 * Gets the role required.
	 *
	 * @return the role required
	 */
	public MapUserRole getRoleRequired();

	
	void setRoleRequired(MapUserRole roleRequired);
	/**
	 * Gets the report type.
	 *
	 * @return the report type
	 */
	public ReportType getReportType();

	/**
	 * Sets the report type.
	 *
	 * @param reportType the new report type
	 */
	public void setReportType(ReportType reportType);

	public ReportTimePeriod getTimePeriod();
	
	public void setTimePeriod(ReportTimePeriod timePeriod);

	boolean isDiffReport();

	void setDiffReport(boolean isDiffReport);

	boolean isRateReport();

	void setRateReport(boolean isRateReport);



	
}
