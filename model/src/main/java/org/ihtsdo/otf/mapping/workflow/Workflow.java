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
	 * Sets the tracking records for unmapped in scope concepts.
	 *
	 * @param trackingRecordsForUnmappedInScopeConcepts the tracking records for unmapped in scope concepts
	 */
	public void setTrackingRecordsForUnmappedInScopeConcepts(
		Set<WorkflowTrackingRecord> trackingRecordsForUnmappedInScopeConcepts);

	/**
	 * Adds the tracking records for unmapped in scope concept.
	 *
	 * @param trackingRecordsForUnmappedInScopeConcept the tracking records for unmapped in scope concept
	 */
	public void addTrackingRecordsForUnmappedInScopeConcept(
		WorkflowTrackingRecord trackingRecordsForUnmappedInScopeConcept);

	/**
	 * Removes the tracking records for unmapped in scope concept.
	 *
	 * @param trackingRecordsForUnmappedInScopeConcept the tracking records for unmapped in scope concept
	 */
	public void removeTrackingRecordsForUnmappedInScopeConcept(
		WorkflowTrackingRecord trackingRecordsForUnmappedInScopeConcept);

	/**
	 * Returns the tracking records for conflict concepts.
	 *
	 * @return the tracking records for conflict concepts
	 */
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts();

	/**
	 * Sets the tracking records for conflict concepts.
	 *
	 * @param trackingRecordsForConflictConcepts the tracking records for conflict concepts
	 */
	public void setTrackingRecordsForConflictConcepts(
		Set<WorkflowTrackingRecord> trackingRecordsForConflictConcepts);

	/**
	 * Adds the tracking records for conflict concepts.
	 *
	 * @param trackingRecordsForConflictConcept the tracking records for conflict concept
	 */
	public void addTrackingRecordsForConflictConcepts(
		WorkflowTrackingRecord trackingRecordsForConflictConcept);

	/**
	 * Removes the tracking records for conflict concepts.
	 *
	 * @param trackingRecordsForConflictConcept the tracking records for conflict concept
	 */
	public void removeTrackingRecordsForConflictConcepts(
		WorkflowTrackingRecord trackingRecordsForConflictConcept);

}
