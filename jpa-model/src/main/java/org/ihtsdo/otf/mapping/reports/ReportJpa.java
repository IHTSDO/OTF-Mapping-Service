package org.ihtsdo.otf.mapping.reports;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JPA enabled implementation of {@link Report}.
 */
@Entity
@Table(name = "reports")
@Indexed
@XmlRootElement(name = "report")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportJpa implements Report {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The active. */
  @Column(nullable = false)
  private boolean active;

  /** The auto generated. */
  @Column(nullable = false)
  private boolean autoGenerated;

  /** The report definition. */
  @ManyToOne(targetEntity = ReportDefinitionJpa.class, fetch = FetchType.EAGER)
  private ReportDefinition reportDefinition = null;

  /** The timestamp. */
  @Column(nullable = false)
  private Long timestamp = null;

  /** The project id. */
  @Column(nullable = false)
  private Long mapProjectId;

  /** The query. */
  @Column(nullable = false, length = 10000)
  private String query; // save the query used at the time report was

  // generaetd

  /** The query type. */
  @Enumerated(EnumType.STRING)
  private ReportQueryType queryType;

  /** The query result type. */
  @Enumerated(EnumType.STRING)
  private ReportResultType resultType;

  /** The owner. */
  @ManyToOne(targetEntity = MapUserJpa.class)
  private MapUser owner;

  /** The results. */
  @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = ReportResultJpa.class)
  @IndexedEmbedded(targetElement = ReportResultJpa.class)
  private List<ReportResult> results = new ArrayList<>();

  /** The reportNotes. */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = ReportNoteJpa.class)
  private List<ReportNote> notes = new ArrayList<>();

  /** The source reports for a comparison report. */
  @Column(nullable = true)
  private Long report1Id = null;

  /** The report2 id. */
  @Column(nullable = true)
  private Long report2Id = null;

  /** Flags for diff and rate report. */
  @Column(nullable = false)
  private boolean isDiffReport = false;

  /**
   * Constructors.
   * 
   */

  /**
   * Instantiates a new report jpa.
   */
  public ReportJpa() {

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
   * Gets the name.
   * 
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   * 
   * @param name the new name
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Checks if is active.
   * 
   * @return true, if is active
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public boolean isActive() {
    return active;
  }

  /**
   * Sets the active.
   * 
   * @param active the new active
   */
  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Gets the auto generated.
   * 
   * @return the auto generated
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Boolean getAutoGenerated() {
    return autoGenerated;
  }

  /**
   * Sets the auto generated.
   * 
   * @param autoGenerated the new auto generated
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public void setAutoGenerated(boolean autoGenerated) {
    this.autoGenerated = autoGenerated;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#getReportDefinition()
   */
  @Override
  @XmlElement(type = ReportDefinitionJpa.class)
  public ReportDefinition getReportDefinition() {
    return this.reportDefinition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#setReportDefinition(org.ihtsdo.
   * otf.mapping.reports.ReportDefinition)
   */
  @Override
  public void setReportDefinition(ReportDefinition definition) {
    this.reportDefinition = definition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#getResultType()
   */
  @Override
  public ReportResultType getResultType() {
    return resultType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.reports.Report#setResultType(org.ihtsdo.otf.mapping
   * .helpers.ReportResultType)
   */
  @Override
  public void setResultType(ReportResultType resultType) {
    this.resultType = resultType;
  }

  /**
   * Gets the timestamp.
   * 
   * @return the timestamp
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp.
   * 
   * @param timestamp the new timestamp
   */
  @Override
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Gets the project.
   * 
   * @return the project
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getMapProjectId() {
    return mapProjectId;
  }

  /**
   * Sets the project.
   * 
   * @param mapProjectId the new map project id
   */
  @Override
  public void setMapProjectId(Long mapProjectId) {
    this.mapProjectId = mapProjectId;
  }

  /**
   * Gets the query.
   * 
   * @return the query
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getQuery() {
    return query;
  }

  /**
   * Sets the query.
   * 
   * @param query the new query
   */
  @Override
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Gets the query type.
   * 
   * @return the query type
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public ReportQueryType getQueryType() {
    return queryType;
  }

  /**
   * Sets the query type.
   * 
   * @param queryType the new query type
   */
  @Override
  public void setQueryType(ReportQueryType queryType) {
    this.queryType = queryType;
  }

  /**
   * Gets the owner.
   * 
   * @return the owner
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "owner")
  public MapUser getOwner() {
    return owner;
  }

  /**
   * Sets the owner.
   * 
   * @param owner the new owner
   */
  @Override
  public void setOwner(MapUser owner) {
    this.owner = owner;
  }

  /**
   * Gets the results.
   * 
   * @return the results
   */
  @Override
  @XmlElement(type = ReportResultJpa.class)
  public List<ReportResult> getResults() {
    return results;
  }

  /**
   * Sets the results.
   * 
   * @param results the new results
   */
  @Override
  public void setResults(List<ReportResult> results) {
    this.results = results;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#addResult(org.ihtsdo.otf.mapping
   * .reports.ReportResult)
   */
  @Override
  public void addResult(ReportResult result) {
    if (this.results == null)
      this.results = new ArrayList<>();

    results.add(result);
  }

  /**
   * Gets the reportNotes.
   * 
   * @return the reportNotes
   */
  @Override
  @XmlElement(type = ReportNoteJpa.class)
  public List<ReportNote> getNotes() {
    return notes;
  }

  /**
   * Sets the reportNotes.
   * 
   * @param notes the new notes
   */
  @Override
  public void setNotes(List<ReportNote> notes) {
    this.notes = notes;
  }

  /**
   * Compare this report to another.
   * 
   * @param report the report
   * @return the report
   */
  @Override
  public Report compare(Report report) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#getReport1Id()
   */
  @Override
  public Long getReport1Id() {
    return this.report1Id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#setReport1Id(java.lang.Long)
   */
  @Override
  public void setReport1Id(Long reportId) {
    this.report1Id = reportId;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#getReport2Id()
   */
  @Override
  public Long getReport2Id() {
    return this.report2Id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#setReport2Id(java.lang.Long)
   */
  @Override
  public void setReport2Id(Long reportId) {
    this.report2Id = reportId;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#isDiffReport()
   */
  @Override
  public boolean isDiffReport() {
    return isDiffReport;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.reports.Report#setDiffReport(boolean)
   */
  @Override
  public void setDiffReport(boolean isDiffReport) {
    this.isDiffReport = isDiffReport;
  }

  @Override
  public String toString() {
    return "ReportJpa [name=" + name + ", active=" + active
        + ", autoGenerated=" + autoGenerated + ", reportDefinition="
        + reportDefinition.toString() + ", timestamp=" + timestamp
        + ", mapProjectId=" + mapProjectId + ", query=" + query
        + ", queryType=" + queryType + ", resultType=" + resultType
        + ", owner=" + owner + ", results=" + results.size() + ", notes="
        + notes.size() + ", report1Id=" + report1Id + ", report2Id="
        + report2Id + ", isDiffReport=" + isDiffReport + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (active ? 1231 : 1237);
    result = prime * result + (autoGenerated ? 1231 : 1237);
    result = prime * result + (isDiffReport ? 1231 : 1237);
    result =
        prime * result + ((mapProjectId == null) ? 0 : mapProjectId.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((query == null) ? 0 : query.hashCode());
    result = prime * result + ((queryType == null) ? 0 : queryType.hashCode());
    result = prime * result + ((report1Id == null) ? 0 : report1Id.hashCode());
    result = prime * result + ((report2Id == null) ? 0 : report2Id.hashCode());
    result =
        prime * result
            + ((reportDefinition == null) ? 0 : reportDefinition.hashCode());
    result =
        prime * result + ((resultType == null) ? 0 : resultType.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ReportJpa other = (ReportJpa) obj;
    if (active != other.active)
      return false;
    if (autoGenerated != other.autoGenerated)
      return false;
    if (isDiffReport != other.isDiffReport)
      return false;
    if (mapProjectId == null) {
      if (other.mapProjectId != null)
        return false;
    } else if (!mapProjectId.equals(other.mapProjectId))
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
    if (report1Id == null) {
      if (other.report1Id != null)
        return false;
    } else if (!report1Id.equals(other.report1Id))
      return false;
    if (report2Id == null) {
      if (other.report2Id != null)
        return false;
    } else if (!report2Id.equals(other.report2Id))
      return false;
    if (reportDefinition == null) {
      if (other.reportDefinition != null)
        return false;
    } else if (!reportDefinition.equals(other.reportDefinition))
      return false;
    if (resultType != other.resultType)
      return false;
    return true;
  }

}
