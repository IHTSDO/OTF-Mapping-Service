/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.ihtsdo.otf.mapping.helpers.CollectionToCSVBridge;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;

/**
 * Default implementation of {@link TrackingRecordJpa}.
 */
@Entity
@Indexed
@Table(name = "tracking_records", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion", "mapProjectId"
}))
public class TrackingRecordJpa implements TrackingRecord {

  /** The id. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The map project. */
  @Column(nullable = false)
  private Long mapProjectId;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The terminology id. */
  @Column(nullable = false)
  private String terminologyId;

  /** The terminology version. */
  @Column(nullable = false)
  private String terminologyVersion;

  /** The default preferred name. */
  @Column(nullable = false)
  private String defaultPreferredName;

  /** The sort key. */
  @Column(nullable = false)
  private String sortKey;

  /** The workflow path. */
  @Enumerated(EnumType.STRING)
  private WorkflowPath workflowPath;

  /**
   * The workflow status. Pairs are constructed in the format
   * workflowStatus_userName e.g. NEW_dmo, EDITING_DONE_kli
   */
  @Column(nullable = true)
  private String userAndWorkflowStatusPairs;

  /** The map record ids. */
  @ElementCollection
  @CollectionTable(name = "tracking_records_map_records", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Set<Long> mapRecordIds = new HashSet<>();

  /** The assigned user names. */
  @Column(nullable = true)
  private String assignedUserNames = null;

  /** The assigned user count. */
  @Column(nullable = false)
  private int assignedUserCount = 0;

  /** The assigned team name. */
  @Column(nullable = true)
  private String assignedTeamName;
  
  /** The tags for this tracking record. */
  @ElementCollection
  @CollectionTable(name = "tracking_records_tags", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  // treat tags as a single field called tags
  private Set<String> tags = new HashSet<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the map project id.
   *
   * @return the map project id
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @FieldBridge(impl = LongBridge.class)
  public Long getMapProjectId() {
    return this.mapProjectId;
  }

  /**
   * Sets the map project id.
   *
   * @param mapProjectId the map project id
   */
  @Override
  public void setMapProjectId(Long mapProjectId) {
    this.mapProjectId = mapProjectId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the terminology id.
   *
   * @return the terminology id
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTerminologyId() {
    return terminologyId;
  }

  /**
   * Sets the terminology id.
   *
   * @param terminologyId the terminology id
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /**
   * Sets the default preferred name.
   *
   * @param defaultPreferredName the default preferred name
   */
  @Override
  public void setDefaultPreferredName(String defaultPreferredName) {
    this.defaultPreferredName = defaultPreferredName;
  }

  /**
   * Returns the default preferred name.
   *
   * @return the default preferred name
   */
  @Override
  @Fields({
      @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO),
      @Field(name = "defaultPreferredNameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
  @SortableField(forField = "defaultPreferredNameSort")
  @Analyzer(definition = "noStopWord")
  public String getDefaultPreferredName() {
    return defaultPreferredName;
  }

  /**
   * Sets the sort key.
   *
   * @param sortKey the sort key
   */
  @Override
  public void setSortKey(String sortKey) {
    this.sortKey = sortKey;
  }

  /**
   * Returns the sort key.
   *
   * @return the sort key
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @SortableField
  public String getSortKey() {
    return sortKey;
  }

  /**
   * Returns the workflow path.
   *
   * @return the workflow path
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public WorkflowPath getWorkflowPath() {
    return workflowPath;
  }

  /**
   * Sets the workflow path.
   *
   * @param workflowPath the workflow path
   */
  @Override
  public void setWorkflowPath(WorkflowPath workflowPath) {
    this.workflowPath = workflowPath;
  }

  /**
   * Returns the user and workflow status pairs.
   *
   * @return the user and workflow status pairs
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(impl = WhitespaceAnalyzer.class))
  public String getUserAndWorkflowStatusPairs() {
    return userAndWorkflowStatusPairs;
  }

  /**
   * Sets the user and workflow status pairs.
   *
   * @param userAndWorkflowStatusPairs the user and workflow status pairs
   */
  @Override
  public void setUserAndWorkflowStatusPairs(String userAndWorkflowStatusPairs) {
    this.userAndWorkflowStatusPairs = userAndWorkflowStatusPairs;
  }

  /**
   * Adds the user and workflow status pair.
   *
   * @param userName the user name
   * @param workflowStatus the workflow status
   */
  @Override
  public void addUserAndWorkflowStatusPair(String userName,
    String workflowStatus) {
    String pair = workflowStatus + "_" + userName;
    if (this.userAndWorkflowStatusPairs == null)
      this.userAndWorkflowStatusPairs = pair;
    if (this.userAndWorkflowStatusPairs.indexOf(pair) == -1) {
      this.userAndWorkflowStatusPairs += " " + pair;
    }
  }

  /**
   * Removes the user and workflow status pair.
   *
   * @param userName the user name
   * @param workflowStatus the workflow status
   */
  @Override
  public void removeUserAndWorkflowStatusPair(String userName,
    String workflowStatus) {
    String pair = workflowStatus + "_" + userName;

    if (this.userAndWorkflowStatusPairs.indexOf(pair) != -1) {
      userAndWorkflowStatusPairs = userAndWorkflowStatusPairs
          .replaceAll(pair, "").replace("  ", " ").trim();
    }
  }

  /**
   * Returns the map record ids.
   *
   * @return the map record ids
   */
  @Override
  public Set<Long> getMapRecordIds() {
    return mapRecordIds;
  }

  /**
   * Sets the map record ids.
   *
   * @param mapRecordIds the map record ids
   */
  @Override
  public void setMapRecordIds(Set<Long> mapRecordIds) {
    this.mapRecordIds = mapRecordIds;
  }

  /**
   * Returns the assigned user names.
   *
   * @return the assigned user names
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getAssignedUserNames() {
    return assignedUserNames;
  }

  /**
   * Returns the assigned user count.
   *
   * @return the assigned user count
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  @FieldBridge(impl = IntegerBridge.class)
  public int getAssignedUserCount() {
    if (this.assignedUserNames == null)
      this.assignedUserCount = 0;
    else {
      StringTokenizer st = new StringTokenizer(this.assignedUserNames, " ");
      this.assignedUserCount = st.countTokens();
    }
    return this.assignedUserCount;
  }

  /**
   * Sets the assigned user names.
   *
   * @param assignedUserNames the assigned user names
   */
  @Override
  public void setAssignedUserNames(String assignedUserNames) {
    this.assignedUserNames = assignedUserNames;

    // call the count function
    this.getAssignedUserCount();
  }

  /**
   * Adds the assigned user name.
   *
   * @param name the name
   */
  @Override
  public void addAssignedUserName(String name) {
    // if string list is null, set it to name
    if (this.assignedUserNames == null)
      this.assignedUserNames = name;

    // otherwise, add if not already present
    else if (this.assignedUserNames.indexOf(name) == -1)
      this.assignedUserNames += " " + name;

    // call the count function
    this.getAssignedUserCount();

  }

  /**
   * Removes the assigned user name.
   *
   * @param name the name
   */
  @Override
  public void removeAssignedUserName(String name) {
    if (this.assignedUserNames.indexOf(name) != -1) {

      // remove the name, tighten any double spaces remaining, and trim
      // the
      // string
      this.assignedUserNames =
          this.assignedUserNames.replace(name, "").replace("  ", " ").trim();

      // call the count function
      this.getAssignedUserCount();

    }

  }

  /**
   * Adds the map record id.
   *
   * @param mapRecordId the map record id
   */
  @Override
  public void addMapRecordId(Long mapRecordId) {
    if (this.mapRecordIds == null)
      this.mapRecordIds = new HashSet<>();
    this.mapRecordIds.add(mapRecordId);
  }

  /**
   * Removes the map record id.
   *
   * @param mapRecordId the map record id
   */
  @Override
  public void removeMapRecordId(Long mapRecordId) {
    if (this.mapRecordIds != null) {
      this.mapRecordIds.remove(mapRecordId);
    }
  }

  /**
   * Sets the assigned team name.
   *
   * @param assignedTeamName the assigned team name
   */
  @Override
  public void setAssignedTeamName(String assignedTeamName) {
    this.assignedTeamName = assignedTeamName;
  }

  /**
   * Returns the assigned team name.
   *
   * @return the assigned team name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getAssignedTeamName() {
    return this.assignedTeamName;
  }

  /**
   * Gets the tags.
   *
   * @return the tags
   */
  @Field(bridge = @FieldBridge(impl = CollectionToCSVBridge.class))
  @Override
  public Set<String> getTags() {
    return tags;
  }

  /**
   * Sets the tags.
   *
   * @param tags the new tags
   */
  @Override
  public void setTags(Set<String> tags) {
    this.tags = tags;
  }  
  
  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "TrackingRecordJpa [id=" + id + ", mapProjectId=" + mapProjectId
        + ", terminology=" + terminology + ", terminologyId=" + terminologyId
        + ", terminologyVersion=" + terminologyVersion
        + ", defaultPreferredName=" + defaultPreferredName + ", sortKey="
        + sortKey + ", workflowPath=" + workflowPath
        + ", userAndWorkflowStatusPairs=" + userAndWorkflowStatusPairs
        + ", mapRecordIds=" + mapRecordIds + ", assignedUserNames="
        + assignedUserNames + ", assignedUserCount=" + assignedUserCount + "]";
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + assignedUserCount;
    result = prime * result
        + ((assignedUserNames == null) ? 0 : assignedUserNames.hashCode());
    result = prime * result + ((defaultPreferredName == null) ? 0
        : defaultPreferredName.hashCode());
    result =
        prime * result + ((mapProjectId == null) ? 0 : mapProjectId.hashCode());
    result =
        prime * result + ((mapRecordIds == null) ? 0 : mapRecordIds.hashCode());
    result = prime * result + ((sortKey == null) ? 0 : sortKey.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result
        + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result = prime * result
        + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    result = prime * result + ((userAndWorkflowStatusPairs == null) ? 0
        : userAndWorkflowStatusPairs.hashCode());
    result =
        prime * result + ((workflowPath == null) ? 0 : workflowPath.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TrackingRecordJpa other = (TrackingRecordJpa) obj;
    if (assignedUserCount != other.assignedUserCount)
      return false;
    if (assignedUserNames == null) {
      if (other.assignedUserNames != null)
        return false;
    } else if (!assignedUserNames.equals(other.assignedUserNames))
      return false;
    if (defaultPreferredName == null) {
      if (other.defaultPreferredName != null)
        return false;
    } else if (!defaultPreferredName.equals(other.defaultPreferredName))
      return false;
    if (mapProjectId == null) {
      if (other.mapProjectId != null)
        return false;
    } else if (!mapProjectId.equals(other.mapProjectId))
      return false;
    if (mapRecordIds == null) {
      if (other.mapRecordIds != null)
        return false;
    } else if (!mapRecordIds.equals(other.mapRecordIds))
      return false;
    if (sortKey == null) {
      if (other.sortKey != null)
        return false;
    } else if (!sortKey.equals(other.sortKey))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null)
        return false;
    } else if (!terminologyVersion.equals(other.terminologyVersion))
      return false;
    if (userAndWorkflowStatusPairs == null) {
      if (other.userAndWorkflowStatusPairs != null)
        return false;
    } else if (!userAndWorkflowStatusPairs
        .equals(other.userAndWorkflowStatusPairs))
      return false;
    if (workflowPath != other.workflowPath)
      return false;
    return true;
  }

}
