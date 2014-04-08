package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

// TODO: Auto-generated Javadoc
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
	 * Gets the workflow.
	 *
	 * @return the workflow
	 */
	public Workflow getWorkflow();

	/**
	 * Sets the workflow.
	 *
	 * @param workflow the new workflow
	 */
	public void setWorkflow(Workflow workflow);
	
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
	 * Sets the checks for discrepancy.
	 *
	 * @param hasDiscrepancy the checks for discrepancy
	 */
	public void setHasDiscrepancy(boolean hasDiscrepancy);
	
	/**
	 * Indicates whether or not checks for discrepancy is the case.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isHasDiscrepancy();
	
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
	 * Sets the assigned specialists.
	 *
	 * @param assignedUsers the assigned specialists
	 */
	public void setAssignedUsers(Set<MapUser> assignedUsers);
	
	/**
	 * Adds the assigned specialist.
	 *
	 * @param assignedUser the assigned specialist
	 */
	public void addAssignedUser(MapUser assignedUser);
	
	/**
	 * Removes the assigned specialist.
	 *
	 * @param assignedUser the assigned specialist
	 */
	public void removeAssignedUser(MapUser assignedUser);
	
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

	
}
