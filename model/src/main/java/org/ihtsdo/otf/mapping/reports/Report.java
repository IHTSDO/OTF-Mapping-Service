package org.ihtsdo.otf.mapping.reports;

import java.util.Date;
import java.util.List;

import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportType;
import org.ihtsdo.otf.mapping.model.MapUser;

// TODO: Auto-generated Javadoc
/**
 * The Interface Report.
 */
public interface Report {

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(Long id);

	/**
	 * Checks if is active.
	 * 
	 * @return true, if is active
	 */
	public boolean isActive();

	/**
	 * Sets the active.
	 * 
	 * @param active
	 *            the new active
	 */
	public void setActive(boolean active);

	/**
	 * Gets the auto generated.
	 * 
	 * @return the auto generated
	 */
	public Boolean getAutoGenerated();

	/**
	 * Sets the auto generated.
	 * 
	 * @param autoGenerated
	 *            the new auto generated
	 */
	public void setAutoGenerated(boolean autoGenerated);

	/**
	 * Gets the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Date getTimestamp();

	/**
	 * Sets the timestamp.
	 * 
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(Date timestamp);

	/**
	 * Gets the map project id.
	 * 
	 * @return the map project id
	 */
	public Long getMapProjectId();

	/**
	 * Sets the map project id.
	 * 
	 * @param mapProjectId
	 *            the new map project id
	 */
	public void setMapProjectId(Long mapProjectId);

	/**
	 * Gets the query.
	 * 
	 * @return the query
	 */
	public String getQuery();

	/**
	 * Sets the query.
	 * 
	 * @param query
	 *            the new query
	 */
	public void setQuery(String query);

	/**
	 * Gets the query type.
	 * 
	 * @return the query type
	 */
	public ReportQueryType getQueryType();

	/**
	 * Sets the query type.
	 * 
	 * @param queryType
	 *            the new query type
	 */
	public void setQueryType(ReportQueryType queryType);

	/**
	 * Gets the owner.
	 * 
	 * @return the owner
	 */
	public MapUser getOwner();

	/**
	 * Sets the owner.
	 * 
	 * @param owner
	 *            the new owner
	 */
	public void setOwner(MapUser owner);

	/**
	 * Gets the results.
	 * 
	 * @return the results
	 */
	public List<ReportResult> getResults();

	/**
	 * Sets the results.
	 * 
	 * @param results
	 *            the new results
	 */
	public void setResults(List<ReportResult> results);
	
	/**
	 * Adds the result.
	 *
	 * @param result the result
	 */
	public void addResult(ReportResult result);

	/**
	 * Gets the notes.
	 * 
	 * @return the notes
	 */
	public List<ReportNote> getNotes();

	/**
	 * Sets the notes.
	 * 
	 * @param notes
	 *            the new notes
	 */
	public void setNotes(List<ReportNote> notes);

	/**
	 * Compare.
	 * 
	 * @param report
	 *            the report
	 * @return the report
	 */
	public Report compare(Report report);

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName();

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(String name);

	/**
	 * Gets the result type.
	 * 
	 * @return the result type
	 */
	public ReportResultType getResultType();

	/**
	 * Sets the result type.
	 * 
	 * @param resultType
	 *            the new result type
	 */
	public void setResultType(ReportResultType resultType);

	/**
	 * Gets the report definition.
	 * 
	 * @return the report definition
	 */
	public ReportType getReportType();

	/**
	 * Sets the report definition.
	 * 
	 * @param reportType
	 *            the new report type
	 */
	public void setReportType(ReportType reportType);

	/**
	 * Gets the report1 id.
	 * 
	 * @return the report1 id
	 */
	public Long getReport1Id();

	/**
	 * Sets the report1 id.
	 * 
	 * @param report
	 *            the new report1 id
	 */
	public void setReport1Id(Long report);

	/**
	 * Gets the report2 id.
	 * 
	 * @return the report2 id
	 */
	public Long getReport2Id();

	/**
	 * Sets the report2 id.
	 * 
	 * @param report
	 *            the new report2 id
	 */
	public void setReport2Id(Long report);

	/**
	 * Checks if is comparison report.
	 * 
	 * @return true, if is comparison report
	 */
	public boolean isComparisonReport();

}
