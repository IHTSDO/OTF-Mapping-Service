package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Default implementatino of {@link WorkflowTrackingRecordJpa}.
 */
@Entity
@Table(name = "workflow_tracking_records", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion", "mapProject_id"
}))
public class WorkflowTrackingRecordJpa implements WorkflowTrackingRecord {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The map project */
  @ManyToOne(targetEntity = MapProjectJpa.class)
  private MapProject mapProject;

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
  @Column(nullable = false)
  private WorkflowPath workflowPath;

  /** The map records. */
  @OneToMany(targetEntity = MapRecordJpa.class)
  private Set<MapRecord> mapRecords = new HashSet<>();

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

  @Override
  public MapProject getMapProject() {
    return this.mapProject;
  }

  @Override
  public void setMapProject(MapProject mapProject) {
    this.mapProject = mapProject;
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
   * {@inheritDoc}
   */
  @Override
  public String getTerminologyId() {
    return terminologyId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setDefaultPreferredName
   * (java.lang.String)
   */
  @Override
  public void setDefaultPreferredName(String defaultPreferredName) {
    this.defaultPreferredName = defaultPreferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getDefaultPreferredName
   * ()
   */
  @Override
  public String getDefaultPreferredName() {
    return defaultPreferredName;
  }

  @Override
  public WorkflowStatus getWorkflowStatus() {

    // initialize the workflowStatus
    WorkflowStatus workflowStatus = WorkflowStatus.NEW;

    // cycle over tracking record's map records
    for (MapRecord mapRecord : this.getMapRecords()) {
      workflowStatus =
          mapRecord.getWorkflowStatus().compareTo(workflowStatus) > 0
              ? mapRecord.getWorkflowStatus() : workflowStatus;

    }

    return workflowStatus;

  }

  @Override
  public WorkflowStatus getLowestWorkflowStatus() {
    WorkflowStatus workflowStatus = null;
    for (MapRecord mapRecord : getMapRecords()) {
      if (workflowStatus == null)
        workflowStatus = mapRecord.getWorkflowStatus();
      else if (workflowStatus.compareTo(mapRecord.getWorkflowStatus()) > 0)
        workflowStatus = mapRecord.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setSortKey(java.
   * lang.String)
   */
  @Override
  public void setSortKey(String sortKey) {
    this.sortKey = sortKey;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getSortKey()
   */
  @Override
  public String getSortKey() {
    return sortKey;
  }

  @Override
  public WorkflowPath getWorkflowPath() {
    return workflowPath;
  }

  @Override
  public void setWorkflowPath(WorkflowPath workflowPath) {
    this.workflowPath = workflowPath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getAssignedUsers()
   */
  @Override
  public Set<MapUser> getAssignedUsers() {
    Set<MapUser> assignedUsers = new HashSet<>();
    for (MapRecord mapRecord : this.getMapRecords()) {
      assignedUsers.add(mapRecord.getOwner());
    }
    return assignedUsers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getMapRecords()
   */
  @Override
  public Set<MapRecord> getMapRecords() {
    return mapRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setMapRecords(java
   * .util.Set)
   */
  @Override
  public void setMapRecords(Set<MapRecord> mapRecords) {
    this.mapRecords = mapRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#addMapRecord(org
   * .ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public void addMapRecord(MapRecord mapRecord) {
    if (this.mapRecords == null)
      this.mapRecords = new HashSet<>();
    this.mapRecords.add(mapRecord);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#removeMapRecord(
   * org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public void removeMapRecord(MapRecord mapRecord) {
    this.mapRecords.remove(mapRecord);
  }

  @Override
  public String toString() {
    return "WorkflowTrackingRecordJpa [id=" + id + ", mapProject=" + mapProject.getId().toString()
        + ", terminology=" + terminology + ", terminologyId=" + terminologyId
        + ", terminologyVersion=" + terminologyVersion
        + ", defaultPreferredName=" + defaultPreferredName + ", sortKey="
        + sortKey + ", workflowPath=" + workflowPath + ", mapRecords="
        + mapRecords + ", getWorkflowStatus()=" + getWorkflowStatus()
        + ", getAssignedUsers()=" + getAssignedUsers() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((mapProject == null) ? 0 : mapProject.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    result =
        prime * result + ((workflowPath == null) ? 0 : workflowPath.hashCode());
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
    WorkflowTrackingRecordJpa other = (WorkflowTrackingRecordJpa) obj;
    if (mapProject == null) {
      if (other.mapProject != null)
        return false;
    } else if (!mapProject.equals(other.mapProject))
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
    if (workflowPath != other.workflowPath)
      return false;
    return true;
  }

}
