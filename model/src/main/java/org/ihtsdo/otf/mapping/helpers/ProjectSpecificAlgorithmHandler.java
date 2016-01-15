package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Represents a collection of project specific algorithms that can override
 * defaults.
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
   * Performs basic checks against: - record with no entries - duplicate map
   * entries - multiple groups in project with no group structure - higher level
   * groups without any targets - invalid TRUE rules - advices are valid for the
   * project.
   * 
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateRecord(MapRecord mapRecord) throws Exception;

  /**
   * Validate target codes. Must be overwritten for each project handler.
   * 
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception;

  /**
   * Compute map advice and map relations. Must be overwritten for each project
   * handler.
   * 
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the list
   * @throws Exception the exception
   */
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception;

  /**
   * Compute map relations.
   * 
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the list
   * @throws Exception
   */
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception;

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
   * Compute target terminology notes.
   * 
   * @param treePositions the tree positions
   * @throws Exception the exception
   */
  public void computeTargetTerminologyNotes(TreePositionList treePositions)
    throws Exception;

  /**
   * Assign a new map record from existing record, performing any necessary
   * workflow actions
   * 
   * Default Behavior: - Create a new record with origin ids set to the existing
   * record (and its antecedents) - Add the record to the tracking record -
   * Return the tracking record.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapRecord the map record
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> assignFromInitialRecord(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapRecord mapRecord, MapUser mapUser)
    throws Exception;

  /**
   * Assign a map record from scratch, performing any necessary workflow
   * actions.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param concept the concept
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> assignFromScratch(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, Concept concept, MapUser mapUser)
    throws Exception;

  /**
   * Unassign a map record from a user, performing any necessary workflow
   * actions.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> unassign(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception;

  /**
   * Set a user's editing on a map record to finished, performing any necessary
   * workflow actions.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> finishEditing(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception;

  /**
   * Performs workflow actions necessary when a map user wishes to save a record
   * for further editing.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  public Set<MapRecord> saveForLater(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception;

  /**
   * Cancel work.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the sets the
   * @throws Exception the exception
   */
  public Set<MapRecord> cancelWork(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception;

  /**
   * Performs any workflow actions necessary when a user sends a record to
   * publication.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the published records.
   * @throws Exception the exception
   */
  public Set<MapRecord> publish(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception;

  /**
   * Returns the dependent modules.
   *
   * @return the dependent modules
   */
  public Set<String> getDependentModules();

  /**
   * Returns the module dependency ref set id.
   *
   * @return the module dependency ref set id
   */
  public String getModuleDependencyRefSetId();

  /**
   * Validate for release.
   *
   * @param member the member
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception;

  /**
   * Returns the default up propagated map relation.
   *
   * @return the default up propagated map relation
   * @throws Exception the exception
   */
  public MapRelation getDefaultUpPropagatedMapRelation() throws Exception;

  /**
   * Returns the default target name for blank target.
   *
   * @return the default target name for blank target
   */
  String getDefaultTargetNameForBlankTarget();
}
