/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.helpers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

// TODO: Auto-generated Javadoc
/**
 * Represents a collection of project specific algorithms that can override
 * defaults.
 *
 * @author ${author}
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
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception;

  /**
   * Checks if is target code valid for this project.
   * 
   * @param terminologyId the terminology id
   * @return true, if is target code valid
   * @throws Exception the exception
   */
  public boolean isTargetCodeValid(String terminologyId) throws Exception;

  /**
   * Indicates whether or not a map record line is valid to load.
   *
   * @param line the line
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isMapRecordLineValid(String line) throws Exception;

  /**
   * Validate semantic checks.
   *
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateSemanticChecks(MapRecord mapRecord) throws Exception;

  /**
   * Compute map advice and map relations. Must be overwritten for each project
   * handler.
   * 
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the list
   * @throws Exception the exception
   */
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry) throws Exception;

  /**
   * Compute map relations.
   *
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the list
   * @throws Exception the exception
   */
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) throws Exception;

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
  public void computeTargetTerminologyNotes(List<TreePosition> treePositions) throws Exception;

  /**
   * Compute initial map record. Read in published map records from an RF2 file,
   * and if the concept has been mapped there, return that map record to
   * pre-populate the map record for this project
   *
   * @param mapRecord the map record
   * @return the map record
   * @throws Exception the exception
   */
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception;

  /**
   * Runs any project-specific pre-release processing that may be required
   * Only select projects need this, so it is blank by default.
   *
   * @throws Exception the exception
   */
  public void preReleaseProcessing() throws Exception;
  
  /**
   * Runs any project-specific post-release processing that may be required
   * Only select projects need this, so it is blank by default.
   *
   * @param effectiveTime the effective time
   * @throws Exception the exception
   */
  public void postReleaseProcessing(String effectiveTime) throws Exception;  
  
  /**
   * Load tags associated with the concept. Read in tags from a pipe-delimited file,
   * and if the concept id is associated with tags, return them so they can be
   * assigned to the tracking record for this concept.
   *
   * @param conceptId the concept id
   * @return the sets the
   * @throws Exception the exception
   */ 
  public Set<String> loadTags(String conceptId) throws Exception;
  
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
  public ValidationResult validateForRelease(ComplexMapRefSetMember member) throws Exception;

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
  public String getDefaultTargetNameForBlankTarget();

  /**
   * For terminologies that decorate codes with additional information this
   * supplies a map from the code to the additional information. For example
   * "asterisk" codes in ICD10.
   *
   * @return the all terminology notes
   * @throws Exception the exception
   */
  public Map<String, String> getAllTerminologyNotes() throws Exception;

  /**
   * Limit tree positions when returning from a query.
   *
   * @param treePositions the tree positions
   * @return the list
   */
  public List<TreePosition> limitTreePositions(List<TreePosition> treePositions);

  /**
   * Checks if is one to one constrained.
   *
   * @return true, if is one to one constrained
   */
  public boolean isOneToOneConstrained();

  /**
   * Record violates one to one constraint.
   *
   * @param record the record
   * @return true, if violates constraint
   * @throws Exception the exception
   */
  public boolean recordViolatesOneToOneConstraint(MapRecord record) throws Exception;

  /**
   * Gets the release file 3rd element (e.g. INT, US1000124)
   *
   * @return the release file 3rd element
   * @throws Exception the exception
   */
  public String getReleaseFile3rdElement() throws Exception;

  /**
   * Initialize.
   *
   * @throws Exception the exception
   */
  public void initialize() throws Exception;

}
