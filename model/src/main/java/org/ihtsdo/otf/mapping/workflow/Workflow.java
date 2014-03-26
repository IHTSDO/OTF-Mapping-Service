package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;
import org.ihtsdo.otf.mapping.model.MapProject;

// TODO: Auto-generated Javadoc
/**
 * The Interface Workflow.
 *
 * @author ${author}
 */
public interface Workflow {

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
	 * Returns the map project.
	 *
	 * @return the map project
	 */
	public MapProject getMapProject();

	/**
	 * Sets the map project.
	 * @param mapProject TODO
	 */
	public void setMapProject(MapProject mapProject);

	/**
	 * Returns the tracking records for unmapped in scope concepts.
	 *
	 * @return the tracking records for unmapped in scope concepts
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts();


	/**
	 * Sets the tracking records.
	 *
	 * @param trackingRecords the tracking records
	 */
	public void setTrackingRecords(
		Set<WorkflowTrackingRecord> trackingRecords);

	/**
	 * Adds the tracking record.
	 *
	 * @param trackingRecord the tracking record
	 */
	public void addTrackingRecord(
		WorkflowTrackingRecord trackingRecord);


	/**
	 * Removes the tracking record.
	 *
	 * @param trackingRecord the tracking record
	 */
	public void removeTrackingRecord(
		WorkflowTrackingRecord trackingRecord);

	/**
	 * Returns the tracking records for conflict concepts.
	 *
	 * @return the tracking records for conflict concepts
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts();


	/**
	 * Returns the tracking records.
	 *
	 * @return the tracking records
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecords();
}
