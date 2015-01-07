package org.ihtsdo.otf.mapping.reports;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportFrequency;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;

// TODO: Auto-generated Javadoc
/**
 * Generically represents a report definition.
 * 
 * @author ${author}
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
  public String getName();

  /**
   * Sets the report name.
   * 
   * @param name the new report name
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
  void setRoleRequired(MapUserRole roleRequired);

  /**
   * Gets the time period.
   * 
   * @return the time period
   */
  public ReportTimePeriod getTimePeriod();

  /**
   * Sets the time period.
   * 
   * @param timePeriod the new time period
   */
  public void setTimePeriod(ReportTimePeriod timePeriod);

  /**
   * Gets the frequency.
   * 
   * @return the frequency
   */
  public ReportFrequency getFrequency();

  /**
   * Sets the frequency.
   * 
   * @param timePeriod the new frequency
   */
  public void setFrequency(ReportFrequency timePeriod);

  /**
   * Checks if is diff report.
   * 
   * @return true, if is diff report
   */
  public boolean isDiffReport();

  /**
   * Sets the diff report.
   * 
   * @param isDiffReport the new diff report
   */
  public void setDiffReport(boolean isDiffReport);

  /**
   * Indicates whether or not QA check is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isQACheck();

  /**
   * Sets the QA check.
   * 
   * @param isQACheck the QA check
   */
  public void setQACheck(boolean isQACheck);

  /**
   * Gets the diff report definition name.
   * 
   * @return the diff report definition name
   */
  public String getDiffReportDefinitionName();

  /**
   * Sets the diff report definition name.
   * 
   * @param diffReportDefinitionName the diff report definition name
   */
  public void setDiffReportDefinitionName(String diffReportDefinitionName);

  /**
   * Returns the description.
   * 
   * @return the description
   */
  public String getDescription();

  /**
   * Sets the description.
   * 
   * @param description the description
   */
  public void setDescription(String description);

  /**
   * Returns the object id.
   * 
   * @return the object id
   */
  public String getObjectId();

}
