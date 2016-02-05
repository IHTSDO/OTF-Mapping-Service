package org.ihtsdo.otf.mapping.helpers;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

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
   * Checks if is target code valid for this project.
   * 
   * @param terminologyId the terminology id
   * @return true, if is target code valid
   * @throws Exception the exception
   */
  public boolean isTargetCodeValid(String terminologyId) throws Exception;

  /**
   * Validate semantic checks.
   *
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateSemanticChecks(MapRecord mapRecord)
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
   * Compute target terminology notes. These notes are passed back when looking
   * through a destination terminology hierarchy. It's a way of providing extra
   * information/context.
   * 
   * @param treePositions the tree positions
   * @throws Exception the exception
   */
  public void computeTargetTerminologyNotes(List<TreePosition> treePositions)
    throws Exception;


  /**
   * Called after "assign from scratch" to give handlers the opportunity to
   * attach notes or map principles to the.
   *
   * @param mapRecord the map record
   * @throws Exception the exception
   */
  public void computeIdentifyAlgorithms(MapRecord mapRecord) throws Exception;

  
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
