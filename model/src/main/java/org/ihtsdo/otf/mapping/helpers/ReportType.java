package org.ihtsdo.otf.mapping.helpers;

public enum ReportType {
	
	

	/** The Specialist Productivity report, using READY_FOR_PUBLICATION records */
	SPECIALIST_PRODUCTIVITY(
			"Specialist Productivity Report",
			"select 'Specialist productivity' report, mp.name project, "
					+ "mu.userName value, DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m') date, count(distinct mra.id) ct "
					+ "from map_records mr, map_projects mp, map_records_origin_ids mroi, map_records_AUD mra, map_users mu "
					+ "where mra.mapProjectId = mp.id and mr.id = mroi.id and mroi.originIds = mra.id "
					+ "and mra.owner_id = mu.id and mr.workflowStatus IN ('READY_FOR_PUBLICATION') "
					+ "and mu.username != 'loader' and from_unixtime(mr.lastModified/1000) > date('2014-06-01') "
					+ "and from_unixtime(mr.lastModified/1000) < date('2014-12-01') "
					+ "group by mp.name, mu.userName, DATE_FORMAT(from_unixtime(mra.lastModified/1000),'%Y-%m') ORDER BY 2,3,4;",
					ReportResultType.CONCEPT,
					ReportQueryType.SQL,
					MapUserRole.LEAD),
	
	/** The number of concepts mapped by each project */
	TOTAL_MAPPED(
			"Total Concepts Mapped by Map Project",
			"select 'Total mapped' report, map_projects.name project, '' value, '' date, count(distinct conceptId) ct "
					+ "from map_records, map_projects "
					+ "where map_records.mapProjectId = map_projects.id "
					+ "and workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION') "
					+ "group by mapProjectid ORDER BY 2,3,4;",
					ReportResultType.CONCEPT,
					ReportQueryType.SQL,
					MapUserRole.LEAD),
	
	/** The total number of concepts either finished or in editing in this mapping cycle */
	TOTAL_EDITED(
			"Total Concepts Edited",
			"select 'Total concepts edited' report, mp.name project, '' value, '' date, count(*) ct "
					+ "from map_projects mp, "
					+ "(select mapProjectId, conceptId from map_records "
					+ "where workflowStatus = 'READY_FOR_PUBLICATION' "
					+ "union "
					+ "select mapProjectId, terminologyId from tracking_records "
					+ "where assignedUserCount > 0"
					+ ") data "
					+ "where mp.id = data.mapProjectId "
					+ "group by mp.id;",
					ReportResultType.CONCEPT,
					ReportQueryType.SQL,
					MapUserRole.LEAD)
					;

	private String name;
	
	private String query;
	
	private ReportResultType resultType;
	
	private ReportQueryType queryType;
	
	private MapUserRole roleRequired;
	

	private ReportType(String name, String query, ReportResultType resultType, ReportQueryType queryType, MapUserRole roleRequired) {
		this.name = name;
		this.query = query;
		this.resultType = resultType;
		this.queryType = queryType;
		this.roleRequired = roleRequired;
	}

	/**
	 * Set the query text (independent of type) for each particular report type
	 * NOTE: Whatever the format, the object MUST be returned as a ResultSet
	 * data object with the fields report | project | value | date | ct
	 * 
	 * @return
	 */
	public String getQuery() {
		return this.query;
	}

	public ReportResultType getReportResultType() {
		return this.resultType;
	}

	public ReportQueryType getReportQueryType() {
		return this.queryType;
	}

	public MapUserRole getUserRoleRequired() {
		return this.roleRequired;
	}

}
