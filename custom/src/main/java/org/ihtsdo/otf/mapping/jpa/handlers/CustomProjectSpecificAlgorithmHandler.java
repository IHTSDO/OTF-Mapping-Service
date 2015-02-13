package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Sample {@link ProjectSpecificAlgorithmHandler} for a custom project.
 *
 */
public class CustomProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /**
   * VALIDATION OF MAP RECORDS
   * 
   * isTargetCodeValid -- default implementation always returns true
   * validateRecord -- default implementation checks target codes and performs
   * universal checks.
   *
   * @param terminologyId the terminology id
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */

  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    return super.isTargetCodeValid(terminologyId);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#validateRecord(org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public ValidationResult validateRecord(MapRecord mapRecord) throws Exception {
    return super.validateRecord(mapRecord);
  }
  
  /**
   * AUTOMATIC COMPUTATION OF MAPPING PARAMETERS
   * 
   * Map Relation, Map Advices, Target Terminology Informational Notes.
   *
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the map relation
   * @throws Exception the exception
   */

  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return super.computeMapRelation(mapRecord, mapEntry);

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#computeMapAdvice(org.ihtsdo.otf.mapping.model.MapRecord, org.ihtsdo.otf.mapping.model.MapEntry)
   */
  @Override
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return super.computeMapAdvice(mapRecord, mapEntry);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#computeTargetTerminologyNotes(org.ihtsdo.otf.mapping.helpers.TreePositionList)
   */
  @Override
  public void computeTargetTerminologyNotes(TreePositionList treePositionList)
    throws Exception {

    // NO DEFAULT BEHAVIOR
  }
  
  /**
   * RELEASE-SPECIFIC VALIDATION AND METHODS
   * 
   * Dependency modules, release-specific validation, up-propagation.
   *
   * @return the dependent modules
   */

  @Override
  public Set<String> getDependentModules() {
    return super.getDependentModules();
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#getModuleDependencyRefSetId()
   */
  @Override
  public String getModuleDependencyRefSetId() {
    return super.getModuleDependencyRefSetId();
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#validateForRelease(org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember)
   */
  @Override
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception {
    return super.validateForRelease(member);

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#getDefaultUpPropagatedMapRelation()
   */
  @Override
  public MapRelation getDefaultUpPropagatedMapRelation() throws Exception {
    return super.getDefaultUpPropagatedMapRelation();
  }

  /**
   * WORKFLOW ACTIONS
   * 
   * Only necessary to change these if you have written a custom workflow path.
   *
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapRecord the map record
   * @param mapUser the map user
   * @return the sets the
   * @throws Exception the exception
   */

  @Override
  public Set<MapRecord> assignFromInitialRecord(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapRecord mapRecord, MapUser mapUser)
    throws Exception {

    return super.assignFromInitialRecord(trackingRecord, mapRecords, mapRecord,
        mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#assignFromScratch(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> assignFromScratch(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, Concept concept, MapUser mapUser)
    throws Exception {

    return super
        .assignFromScratch(trackingRecord, mapRecords, concept, mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#unassign(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> unassign(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    return super.unassign(trackingRecord, mapRecords, mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#publish(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> publish(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    return super.publish(trackingRecord, mapRecords, mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#finishEditing(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> finishEditing(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    return super.finishEditing(trackingRecord, mapRecords, mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#saveForLater(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> saveForLater(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    return super.saveForLater(trackingRecord, mapRecords, mapUser);
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler#cancelWork(org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> cancelWork(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    return super.cancelWork(trackingRecord, mapRecords, mapUser);
  }

}
