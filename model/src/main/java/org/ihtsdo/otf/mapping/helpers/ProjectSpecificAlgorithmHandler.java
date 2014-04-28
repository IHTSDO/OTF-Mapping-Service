package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;


/**
 * The Interface ProjectSpecificAlgorithmHandler.
 */
public interface ProjectSpecificAlgorithmHandler extends Configurable {

	/**
	 * Gets the map project.
	 *
	 * @return the map project
	 */
	public MapProject getMapProject();
	
	/**
	 * Sets the map project.
	 *
	 * @param mapProject the new map project
	 */
	public void setMapProject(MapProject mapProject);
	
	/**
	 * Checks if the map advice is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map advice computable
	 */
	public boolean isMapAdviceComputable(MapRecord mapRecord);
	
	/**
	 * Checks if the map relation is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map relation computable
	 */
	public boolean isMapRelationComputable(MapRecord mapRecord);
	
	
	/**
	 * Performs basic checks against:
	 * - record with no entries
	 * - duplicate map entries
	 * - multiple groups in project with no group structure
	 * - higher level groups without any targets
	 * - invalid TRUE rules
	 * - advices are valid for the project.
	 *
	 * @param mapRecord the map record
	 * @return the validation result
	 * @throws Exception the exception
	 */
	public ValidationResult validateRecord(MapRecord mapRecord) throws Exception;

	
	/**
	 * Validate target codes.  Must be overwritten for each project handler.
	 *
	 * @param mapRecord the map record
	 * @return the validation result
	 * @throws Exception the exception
	 */
	public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception;
	
	/**
	 * Compute map advice and map relations. Must be overwritten for each project handler.
	 *
	 * @param mapRecord the map record
	 * @param mapEntry the map entry
	 * @return the list
	 */
	public List<MapAdvice> computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry);
	
	/**
	 * Compute map relations.
	 *
	 * @param mapRecord the map record
	 * @param mapEntry the map entry
	 * @return the list
	 */
	public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry);
	
	/**
	 * Compute map record conflicts.
	 *
	 * @param record1 the record1
	 * @param record2 the record2
	 * @return the validation result
	 */
	public ValidationResult compareMapRecords(MapRecord record1, MapRecord record2);
	
	/**
	 * Checks if is target code valid for this project.
	 *
	 * @param terminologyId the terminology id
	 * @return true, if is target code valid
	 * @throws Exception the exception
	 */
	public boolean isTargetCodeValid(String terminologyId) throws Exception;

	/**
	 * Assign a new map record from existing record, performing any necessary workflow actions
	 *
	 * Default Behavior:
	 * - Create a new record with origin ids set to the existing record (and its antecedents)
	 * - Add the record to the tracking record
	 * - Return the tracking record
	 * 
	 * @param trackingRecord the tracking record
	 * @param mapRecord the map record
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 * @throws Exception 
	 */
	public WorkflowTrackingRecord assignFromInitialRecord(
			WorkflowTrackingRecord trackingRecord, MapRecord mapRecord,
			MapUser mapUser) throws Exception;

	/**
	 * Assign a map record from scratch, performing any necessary workflow actions
	 *
	 * @param trackingRecord the tracking record
	 * @param concept the concept
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 * @throws Exception 
	 */
	public WorkflowTrackingRecord assignFromScratch(
			WorkflowTrackingRecord trackingRecord, Concept concept,
			MapUser mapUser) throws Exception;

	/**
	 * Unassign a map record from a user, performing any necessary workflow actions
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 * @throws Exception 
	 */
	public WorkflowTrackingRecord unassign(WorkflowTrackingRecord trackingRecord,
			MapUser mapUser) throws Exception;

	/**
	 * Set a user's editing on a map record to finished, performing any necessary workflow actions.
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 * @throws Exception the exception
	 */
	public WorkflowTrackingRecord finishEditing(WorkflowTrackingRecord trackingRecord,
			MapUser mapUser) throws Exception;

	/**
	 * Performs workflow actions necessary when a map user wishes to save a record for further editing
	 *
	 * @param trackingRecord the tracking record
	 * @param mapUser the map user
	 * @return the workflow tracking record
	 * @throws Exception 
	 */
	public WorkflowTrackingRecord saveForLater(
			WorkflowTrackingRecord trackingRecord, MapUser mapUser) throws Exception;
}
