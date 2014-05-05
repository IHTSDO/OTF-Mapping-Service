package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * The Interface WorkflowTrackingRecord.
 * 
 * @author ${author}
 */
public interface WorkflowTrackingRecord {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Gets the map project.
   * 
   * @return the map project.
   */
  public MapProject getMapProject();

  /**
   * Sets the map project.
   * 
   * @param mapProject the new map project
   */
  public void setMapProject(MapProject mapProject);

  /**
   * Sets the terminology id.
   * 
   * @param terminologyId the terminology id
   */
  public void setTerminologyId(String terminologyId);

  /**
   * Returns the terminology id.
   * 
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Sets the terminology.
   * 
   * @param terminology the terminology
   */
  public void setTerminology(String terminology);

  /**
   * Returns the terminology.
   * 
   * @return the terminology
   */
  public String getTerminology();

  /**
   * Sets the version.
   * 
   * @param version the version
   */
  public void setTerminologyVersion(String version);

  /**
   * Returns the version.
   * 
   * @return the version
   */
  public String getTerminologyVersion();

  /**
   * Sets the default preferred name.
   * 
   * @param defaultPreferredName the default preferred name
   */
  public void setDefaultPreferredName(String defaultPreferredName);

  /**
   * Returns the default preferred name.
   * 
   * @return the default preferred name
   */
  public String getDefaultPreferredName();

  /**
   * The most advanced workflow status in the records set.
   * 
   * @return the computed workflow status
   */
  public WorkflowStatus getWorkflowStatus();

  /**
   * Sets the sort key.
   * 
   * @param sortKey the sort key
   */
  public void setSortKey(String sortKey);

  /**
   * Returns the sort key.
   * 
   * @return the sort key
   */
  public String getSortKey();

  /**
   * Returns the assigned specialists.
   * 
   * @return the assigned specialists
   */
  public Set<MapUser> getAssignedUsers();

  /**
   * Returns the map records.
   * 
   * @return the map records
   */
  public Set<MapRecord> getMapRecords();

  /**
   * Sets the map records.
   * 
   * @param mapRecords the map records
   */
  public void setMapRecords(Set<MapRecord> mapRecords);

  /**
   * Adds the map record.
   * 
   * @param mapRecord the map record
   */
  public void addMapRecord(MapRecord mapRecord);

  /**
   * Removes the map record.
   * 
   * @param mapRecord the map record
   */
  public void removeMapRecord(MapRecord mapRecord);

  /**
   * Gets the workflow path.
   * 
   * @return the workflow path
   */
  public WorkflowPath getWorkflowPath();

  /**
   * Sets the workflow path.
   * 
   * @param workflowPath the new workflow path
   */
  public void setWorkflowPath(WorkflowPath workflowPath);

  /**
   * Gets the lowest workflow status of associated map records
   * 
   * @return the lowest workflow status
   */
  WorkflowStatus getLowestWorkflowStatus();

}
