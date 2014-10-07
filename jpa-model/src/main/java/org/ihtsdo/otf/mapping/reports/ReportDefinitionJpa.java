package org.ihtsdo.otf.mapping.reports;

import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportType;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;

/**
 * The Class ReportDefinitionJpa.
 */
@XmlRootElement
public class ReportDefinitionJpa implements ReportDefinition {

	/** The report type. */
	private ReportType reportType = null;
	
	/** The result type. */
	private ReportResultType resultType = null;
	
	/** The query type. */
	private ReportQueryType queryType = null;
	
	/** The query. */
	private String query = null;
	
	/** The role required. */
	private MapUserRole roleRequired = null;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getReportType()
	 */
	@Override
	public ReportType getReportType() {
		return reportType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#setReportType(org.ihtsdo.otf.mapping.helpers.ReportType)
	 */
	@Override
	public void setReportType(ReportType reportType) {
		this.reportType = reportType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getResultType()
	 */
	@Override
	public ReportResultType getResultType() {
		return resultType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#setResultType(org.ihtsdo.otf.mapping.helpers.ReportResultType)
	 */
	@Override
	public void setResultType(ReportResultType resultType) {
		this.resultType = resultType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQueryType()
	 */
	@Override
	public ReportQueryType getQueryType() {
		return queryType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQueryType(org.ihtsdo.otf.mapping.helpers.ReportQueryType)
	 */
	@Override
	public void setQueryType(ReportQueryType queryType) {
		this.queryType = queryType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQuery()
	 */
	@Override
	public String getQuery() {
		return query;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQuery(java.lang.String)
	 */
	@Override
	public void setQuery(String query) {
		this.query = query;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getRoleRequired()
	 */
	@Override
	public MapUserRole getRoleRequired() {
		return roleRequired;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#setRoleRequired(org.ihtsdo.otf.mapping.helpers.MapUserRole)
	 */
	@Override
	public void setRoleRequired(MapUserRole roleRequired) {
		this.roleRequired = roleRequired;
	}
	
	
}
