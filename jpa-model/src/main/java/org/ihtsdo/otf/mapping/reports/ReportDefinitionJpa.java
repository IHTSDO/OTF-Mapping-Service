package org.ihtsdo.otf.mapping.reports;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportFrequency;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JPA enabled implementation of {@link ReportDefinition}.
 */
@Entity
@Audited
@Table(name = "report_definitions", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
    "name"
  })
})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "reportDefinition")
public class ReportDefinitionJpa implements ReportDefinition {

  /** Auto-generated id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The report type name. */
  @Column(nullable = false)
  private String name;
  
  /** The report description */
  @Column(length = 4000, nullable = true)
  private String description;

  /** The is diff report. */
  @Column(nullable = false)
  private boolean isDiffReport = false;

  /** The is qa check. */
  @Column(nullable = false)
  private boolean isQACheck = false;

  /** The time period (in days) for diff and rate reports */
  @Enumerated(EnumType.STRING)
  private ReportTimePeriod timePeriod;

  /** The frequency with which the report is run */
  @Enumerated(EnumType.STRING)
	@Column(nullable = false)
  private ReportFrequency frequency;

  /** The result type. */
  @Enumerated(EnumType.STRING)
  private ReportResultType resultType;

  /** The query type. */
  @Enumerated(EnumType.STRING)
  private ReportQueryType queryType;

  /** The query. */
  @Column(nullable = true, length = 10000)
  private String query;

  /** The role required. */
  @Enumerated(EnumType.STRING)
  private MapUserRole roleRequired;

  /** The report definition used for constructing diff reports (if applicable) */
  @Column(nullable = true)
  private String diffReportDefinitionName;
  
  /** Default constructor */
  public ReportDefinitionJpa() {}

  /** Copy constructor */
  public ReportDefinitionJpa(ReportDefinition reportDefinition) {
    super();
    this.name = reportDefinition.getName();
    this.description = reportDefinition.getDescription();
    this.isDiffReport = reportDefinition.isDiffReport();
    this.isQACheck = reportDefinition.isQACheck();
    this.timePeriod = reportDefinition.getTimePeriod();
    this.frequency = reportDefinition.getFrequency();
    this.resultType = reportDefinition.getResultType();
    this.queryType = reportDefinition.getQueryType();
    this.query = reportDefinition.getQuery();
    this.roleRequired = reportDefinition.getRoleRequired();
    this.diffReportDefinitionName = reportDefinition.getDiffReportDefinitionName();
  }

  /**
   * Gets the id.
   * 
   * @return the id
   */
  @Override
  public Long getId() {
    return id;
  }

  /**
   * Sets the id.
   * 
   * @param id the new id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form.
   * 
   * @return the id in string form
   */
  @XmlID
  @Override
  public String getObjectId() {
    return id.toString();
  }
  
  /**
   * Gets the report name.
   * 
   * @return the report name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the report name.
   * 
   * @param name the new report name
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }
  

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }


  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getResultType()
   */
  /**
   * Gets the result type.
   * 
   * @return the result type
   */
  @Override
  public ReportResultType getResultType() {
    return resultType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setResultType(org.ihtsdo
   * .otf.mapping.helpers.ReportResultType)
   */
  /**
   * Sets the result type.
   * 
   * @param resultType the new result type
   */
  @Override
  public void setResultType(ReportResultType resultType) {
    this.resultType = resultType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQueryType()
   */
  /**
   * Gets the query type.
   * 
   * @return the query type
   */
  @Override
  public ReportQueryType getQueryType() {
    return queryType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQueryType(org.ihtsdo
   * .otf.mapping.helpers.ReportQueryType)
   */
  /**
   * Sets the query type.
   * 
   * @param queryType the new query type
   */
  @Override
  public void setQueryType(ReportQueryType queryType) {
    this.queryType = queryType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQuery()
   */
  /**
   * Gets the query.
   * 
   * @return the query
   */
  @Override
  public String getQuery() {
    return query;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQuery(java.lang.String )
   */
  /**
   * Sets the query.
   * 
   * @param query the new query
   */
  @Override
  public void setQuery(String query) {
    this.query = query;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getRoleRequired()
   */
  /**
   * Gets the role required.
   * 
   * @return the role required
   */
  @Override
  public MapUserRole getRoleRequired() {
    return roleRequired;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.ReportDefinition#setRoleRequired(org.ihtsdo
   * .otf.mapping.helpers.MapUserRole)
   */
  @Override
  public void setRoleRequired(MapUserRole roleRequired) {
    this.roleRequired = roleRequired;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#isDiffReport()
   */
  @Override
  public boolean isDiffReport() {
    return isDiffReport;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#setDiffReport(boolean)
   */
  @Override
  public void setDiffReport(boolean isDiffReport) {
    this.isDiffReport = isDiffReport;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#getTimePeriodInDays()
   */
  @Override
  public ReportTimePeriod getTimePeriod() {
    return this.timePeriod;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.ReportDefinition#setTimePeriodInDays(int)
   */
  @Override
  public void setTimePeriod(ReportTimePeriod timePeriod) {
    this.timePeriod = timePeriod;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#isQACheck()
   */
  @Override
  public boolean isQACheck() {
    return isQACheck;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#setQACheck(boolean)
   */
  @Override
  public void setQACheck(boolean isQACheck) {
    this.isQACheck = isQACheck;
  }

  @Override
  public String toString() {
    return "ReportDefinitionJpa [id=" + id + ", name=" + name
        + ", isDiffReport=" + isDiffReport + ", isQACheck=" + isQACheck
        + ", timePeriod=" + timePeriod + ", frequency=" + frequency
        + ", resultType=" + resultType + ", queryType=" + queryType
        + ", query=" + query + ", roleRequired=" + roleRequired
        + ", diffReportDefinitionName=" + diffReportDefinitionName + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#getFrequency()
   */
  @Override
  public ReportFrequency getFrequency() {
    return this.frequency;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.ReportDefinition#setFrequency(org.ihtsdo
   * .otf.mapping.helpers.ReportTimePeriod)
   */
  @Override
  public void setFrequency(ReportFrequency timePeriod) {
    this.frequency = timePeriod;
  }

  @Override
  public String getDiffReportDefinitionName() {
    return diffReportDefinitionName;
  }

  @Override
  public void setDiffReportDefinitionName(String diffReportDefinitionName) {
    this.diffReportDefinitionName = diffReportDefinitionName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result
            + ((diffReportDefinitionName == null) ? 0
                : diffReportDefinitionName.hashCode());
    result = prime * result + ((frequency == null) ? 0 : frequency.hashCode());
    result = prime * result + (isDiffReport ? 1231 : 1237);
    result = prime * result + (isQACheck ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((query == null) ? 0 : query.hashCode());
    result = prime * result + ((queryType == null) ? 0 : queryType.hashCode());
    result =
        prime * result + ((resultType == null) ? 0 : resultType.hashCode());
    result =
        prime * result + ((roleRequired == null) ? 0 : roleRequired.hashCode());
    result =
        prime * result + ((timePeriod == null) ? 0 : timePeriod.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ReportDefinitionJpa other = (ReportDefinitionJpa) obj;
    if (diffReportDefinitionName == null) {
      if (other.diffReportDefinitionName != null)
        return false;
    } else if (!diffReportDefinitionName.equals(other.diffReportDefinitionName))
      return false;
    if (frequency != other.frequency)
      return false;
    if (isDiffReport != other.isDiffReport)
      return false;
    if (isQACheck != other.isQACheck)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (query == null) {
      if (other.query != null)
        return false;
    } else if (!query.equals(other.query))
      return false;
    if (queryType != other.queryType)
      return false;
    if (resultType != other.resultType)
      return false;
    if (roleRequired != other.roleRequired)
      return false;
    if (timePeriod != other.timePeriod)
      return false;
    return true;
  }

}
