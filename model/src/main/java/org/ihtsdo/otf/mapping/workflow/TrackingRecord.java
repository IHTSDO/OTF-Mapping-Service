package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.WorkflowPath;

/**
 * Generically represents a workflow tracking record.
 */
public interface TrackingRecord {

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
   * Gets the map project id.
   *
   * @return the map project id
   */
  public Long getMapProjectId();

  /**
   * Sets the map project id.
   *
   * @param mapProjectId the new map project id
   */
  public void setMapProjectId(Long mapProjectId);

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
   * Returns the map records.
   * 
   * @return the map records
   */
  public Set<Long> getMapRecordIds();

  /**
   * Sets the map records.
   * 
   * @param mapRecordIds the map records
   */
  public void setMapRecordIds(Set<Long> mapRecordIds);

  /**
   * Adds the map record.
   * 
   * @param mapRecordId the map record id
   */
  public void addMapRecordId(Long mapRecordId);

  /**
   * Removes the map record.
   * 
   * @param mapRecordId the map record id
   */
  public void removeMapRecordId(Long mapRecordId);

  /**
   * Gets the workflow path.
   * 
   * @return the workflow path
   */
  public WorkflowPath getWorkflowPath();

  /**
   * Sets the workflow status.
   *
   * @param userAndWorkflowStatusPairs the new user and workflow status pairs
   */
  public void setUserAndWorkflowStatusPairs(String userAndWorkflowStatusPairs);

  /**
   * Gets the workflow status.
   *
   * @return the workflow status
   */
  public String getUserAndWorkflowStatusPairs();

  /**
   * Sets the workflow path.
   * 
   * @param workflowPath the new workflow path
   */
  public void setWorkflowPath(WorkflowPath workflowPath);

  /**
   * Gets the assigned user names.
   *
   * @return the assigned user names
   */
  public String getAssignedUserNames();

  /**
   * Sets the assigned user names.
   *
   * @param assignedUserNames the new assigned user names
   */
  public void setAssignedUserNames(String assignedUserNames);

  /**
   * Gets the assigned user count.
   *
   * @return the assigned user count
   */
  public int getAssignedUserCount();

  /**
   * Adds the assigned user.
   *
   * @param name the name
   */
  public void addAssignedUserName(String name);

  /**
   * Removes the assigned user.
   *
   * @param name the name
   */
  public void removeAssignedUserName(String name);

  /**
   * Removes the user and workflow status pair.
   *
   * @param userName the user name
   * @param workflowStatus the workflow status
   */
  public void removeUserAndWorkflowStatusPair(String userName,
    String workflowStatus);

  /**
   * Adds the user and workflow status pair.
   *
   * @param userName the user name
   * @param workflowStatus the workflow status
   */
  public void addUserAndWorkflowStatusPair(String userName,
    String workflowStatus);

}
