package org.ihtsdo.otf.mapping.reports;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportType;

/**
 * The Interface ReportDefinition.
 */
public interface ReportDefinition {

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

	/**
	 * Sets the role required.
	 *
	 * @param roleRequired the new role required
	 */
	public void setRoleRequired(MapUserRole roleRequired);

}
