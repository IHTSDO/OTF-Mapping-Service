package org.ihtsdo.otf.mapping.jpa.services;

import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for conflict workflow.
 */
public class ConflictProjectWorkflowJpaTest {

  /** The map project. */
  private static MapProject mapProject;

  /** The map users. */
  private static MapUser specialist1, specialist2, lead;

  /** The map records. */
  private static MapRecord specialistRecord1, specialistRecord2, leadRecord;

  /** The concept. */
  private static Concept concept;

  /** The services. */
  private static ContentService contentService;

  /** The mapping service. */
  private static MappingService mappingService;

  /** The workflow service. */
  private static WorkflowService workflowService;

  /**
   * Initialize the required objects.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void init() throws Exception {

    // initialize the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // clear all objects
    destroyAll();

    // initialize the map users
    specialist1 = new MapUserJpa();
    specialist1.setApplicationRole(MapUserRole.SPECIALIST);
    specialist1.setEmail("specialist1@nowhere.com");
    specialist1.setName("Specialist One");
    specialist1.setUserName("specialist1");

    specialist2 = new MapUserJpa();
    specialist2.setApplicationRole(MapUserRole.SPECIALIST);
    specialist2.setEmail("specialist2@nowhere.com");
    specialist2.setName("Specialist Two");
    specialist2.setUserName("specialist2");

    lead = new MapUserJpa();
    lead.setApplicationRole(MapUserRole.LEAD);
    lead.setEmail("lead@nowhere.com");
    lead.setName("Lead");
    lead.setUserName("lead");

    mappingService.addMapUser(specialist1);
    mappingService.addMapUser(specialist2);
    mappingService.addMapUser(lead);

    // initialize the map project
    mapProject = new MapProjectJpa();
    mapProject.setName("Map Project");
    mapProject.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
    mapProject.setRefSetId("refSetId");
    mapProject.setRefSetName("refSetName");
    mapProject.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setSourceTerminology("SourceTerminology");
    mapProject.setSourceTerminologyVersion("version");
    mapProject.setDestinationTerminology("DestinationTerminology");
    mapProject.setDestinationTerminologyVersion("version");
    mapProject.setRuleBased(true);
    mapProject.addMapLead(lead);
    mapProject.addMapSpecialist(specialist1);
    mapProject.addMapSpecialist(specialist2);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler");
    Set<String> scopeConcepts = new HashSet<>();
    scopeConcepts.add("1");
    mapProject.setScopeConcepts(scopeConcepts);

    mappingService.addMapProject(mapProject);

    // initialize the concept
    concept = new ConceptJpa();
    concept.setActive(true);
    concept.setEffectiveTime(new Date());
    concept.setTerminology(mapProject.getSourceTerminology());
    concept.setTerminologyVersion(mapProject.getSourceTerminologyVersion());
    concept.setTerminologyId("1");
    concept.setModuleId((long) 1.0);
    concept.setDefinitionStatusId((long) 1.0);
    concept.setDefaultPreferredName("Generic Concept");

    contentService.addConcept(concept);

    contentService.computeTreePositions(mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion(), "1", "1");
  }

  /**
   * Test standard progression with conflict.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStandardProgressionWithConflict() throws Exception {

    // clear work created by other tests
    revertWorkToCleanState();

    // assign specialists to concept
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        null, WorkflowAction.ASSIGN_FROM_SCRATCH);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        null, WorkflowAction.ASSIGN_FROM_SCRATCH);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(WorkflowStatus.NEW))
      fail("ASSIGN_FROM_SCRATCH failed to create NEW record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(WorkflowStatus.NEW))
      fail("ASSIGN_FROM_SCRATCH failed to create NEW record for specialist 2");
    if (leadRecord != null)
      fail("ASSIGN_FROM_SCRATCH erroneously created a lead record");

    // save both for later
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        specialistRecord1, WorkflowAction.SAVE_FOR_LATER);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        specialistRecord2, WorkflowAction.SAVE_FOR_LATER);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(
            WorkflowStatus.EDITING_IN_PROGRESS))
      fail("SAVE_FOR_LATER failed to create EDITING_IN_PROGRESS record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(
            WorkflowStatus.EDITING_IN_PROGRESS))
      fail("SAVE_FOR_LATER failed to create EDITING_IN_PROGRESS record for specialist 2");
    if (leadRecord != null)
      fail("SAVE_FOR_LATER erroneously created a lead record");

    // add entry for first record
    MapEntry entry1 = new MapEntryJpa();
    entry1.setMapRecord(specialistRecord1);
    entry1.setMapPriority(1);
    entry1.setMapGroup(1);
    entry1.setMapBlock(1);
    entry1.setTargetId("target1");
    specialistRecord1.addMapEntry(entry1);

    // add entry to second record
    MapEntry entry2 = new MapEntryJpa();
    entry2.setMapRecord(specialistRecord2);
    entry2.setMapPriority(1);
    entry2.setMapGroup(1);
    entry2.setMapBlock(1);
    entry2.setTargetId("target2");
    specialistRecord2.addMapEntry(entry2);

    // finish editing both
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        specialistRecord1, WorkflowAction.FINISH_EDITING);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        specialistRecord2, WorkflowAction.FINISH_EDITING);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("FINISH_EDITING failed to create CONFLICT_DETECTED record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("FINISH_EDITING failed to create CONFLICT_DETECTED record for specialist 2");
    if (leadRecord != null)
      fail("FINISH_EDITING erroneously created a lead record");

    // assign lead to conflict
    workflowService.processWorkflowAction(mapProject, concept, lead, null,
        WorkflowAction.ASSIGN_FROM_SCRATCH);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("ASSIGN_FROM_SCRATCH for lead modified previous CONFLICT_DETECTED record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("ASSIGN_FROM_SCRATCH for lead modified previous CONFLICT_DETECTED record for specialist 2");
    if (leadRecord == null
        || !leadRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW))
      fail("ASSIGN_FROM_SCRATCH for lead failed to create a CONFLICT_NEW record");

    // save conflict for later
    workflowService.processWorkflowAction(mapProject, concept, lead,
        leadRecord, WorkflowAction.SAVE_FOR_LATER);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("SAVE_FOR_LATER for lead modified previous CONFLICT_DETECTED record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_DETECTED))
      fail("SAVE_FOR_LATER for lead modified previous CONFLICT_DETECTED record for specialist 2");
    if (leadRecord == null
        || !leadRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_IN_PROGRESS))
      fail("SAVE_FOR_LATER for lead failed to create a CONFLICT_IN_PROGRESS record");

    // finish conflict
    workflowService.processWorkflowAction(mapProject, concept, lead,
        leadRecord, WorkflowAction.FINISH_EDITING);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 != null)
      fail("FINISH_EDITING for lead did not delete record for specialist 1");
    if (specialistRecord2 != null)
      fail("FINISH_EDITING for lead did not delete record for specialist 2");
    if (leadRecord == null
        || !leadRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_RESOLVED))
      fail("FINISH_EDITING for lead failed to create a CONFLICT_RESOLVED record");

    // publish conflict
    workflowService.processWorkflowAction(mapProject, concept, lead,
        leadRecord, WorkflowAction.PUBLISH);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 != null)
      fail("PUBLISH for lead did not move record out of workflow for specialist 1");
    if (specialistRecord2 != null)
      fail("PUBLISH for lead did not move record out of workflow for specialist 2");
    if (leadRecord != null)
      fail("PUBLISH for lead did not move record out of workflow for lead");

    MapRecordList records =
        mappingService.getMapRecordsForConcept(concept.getTerminologyId());
    if (records.getCount() == 0)
      fail("PUBLISH resulted in no records for concept");
    else if (records.getCount() != 1)
      fail("PUBLISH resulted in multiple records for concept");
    else if (records.getCount() == 1) {
      if (!records.getIterable().iterator().next().getWorkflowStatus()
          .equals(WorkflowStatus.READY_FOR_PUBLICATION))
        fail("PUBLISH resulted in one record, but without status READY_FOR_PUBLICATION");
    }

  }

  /**
   * Test standard progression without conflict.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStandardProgressionWithoutConflict() throws Exception {

    // clear work created by other tests
    revertWorkToCleanState();

    // assign specialists to concept
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        null, WorkflowAction.ASSIGN_FROM_SCRATCH);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        null, WorkflowAction.ASSIGN_FROM_SCRATCH);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(WorkflowStatus.NEW))
      fail("ASSIGN_FROM_SCRATCH failed to create NEW record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(WorkflowStatus.NEW))
      fail("ASSIGN_FROM_SCRATCH failed to create NEW record for specialist 2");
    if (leadRecord != null)
      fail("ASSIGN_FROM_SCRATCH erroneously created a lead record");

    // save both for later
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        specialistRecord1, WorkflowAction.SAVE_FOR_LATER);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        specialistRecord2, WorkflowAction.SAVE_FOR_LATER);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 == null
        || !specialistRecord1.getWorkflowStatus().equals(
            WorkflowStatus.EDITING_IN_PROGRESS))
      fail("SAVE_FOR_LATER failed to create EDITING_IN_PROGRESS record for specialist 1");
    if (specialistRecord2 == null
        || !specialistRecord2.getWorkflowStatus().equals(
            WorkflowStatus.EDITING_IN_PROGRESS))
      fail("SAVE_FOR_LATER failed to create EDITING_IN_PROGRESS record for specialist 2");
    if (leadRecord != null)
      fail("SAVE_FOR_LATER erroneously created a lead record");

    // add entry for first record
    MapEntry entry1 = new MapEntryJpa();
    entry1.setMapRecord(specialistRecord1);
    entry1.setMapPriority(1);
    entry1.setMapGroup(1);
    entry1.setMapBlock(1);
    entry1.setTargetId("target");
    specialistRecord1.addMapEntry(entry1);

    // add entry to second record
    MapEntry entry2 = new MapEntryJpa();
    entry2.setMapRecord(specialistRecord2);
    entry2.setMapPriority(1);
    entry2.setMapGroup(1);
    entry2.setMapBlock(1);
    entry2.setTargetId("target");
    specialistRecord2.addMapEntry(entry2);

    // finish editing both
    workflowService.processWorkflowAction(mapProject, concept, specialist1,
        specialistRecord1, WorkflowAction.FINISH_EDITING);
    workflowService.processWorkflowAction(mapProject, concept, specialist2,
        specialistRecord2, WorkflowAction.FINISH_EDITING);

    retrieveAssignedWork();

    // check status
    if (specialistRecord1 != null)
      fail("FINISH_EDITING failed to remove concept from workflow for specialist 1");
    if (specialistRecord2 != null)
      fail("FINISH_EDITING failed to remove concept from workflow for specialist 2");
    if (leadRecord != null)
      fail("FINISH_EDITING erroneously created a lead record where no conflict existed");

    MapRecordList records =
        mappingService.getMapRecordsForConcept(concept.getTerminologyId());
    if (records.getCount() == 0)
      fail("PUBLISH resulted in no records for concept");
    else if (records.getCount() != 1)
      fail("PUBLISH resulted in multiple records for concept");
    else if (records.getCount() == 1) {
      if (!records.getIterable().iterator().next().getWorkflowStatus()
          .equals(WorkflowStatus.READY_FOR_PUBLICATION))
        fail("PUBLISH resulted in one record, but without status READY_FOR_PUBLICATION");
    }

  }

  /**
   * Cleanup.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void cleanup() throws Exception {

    // remove all objects
    destroyAll();

    // close the services
    workflowService.close();
    mappingService.close();
    contentService.close();
  }

  /**
   * Retrieve assigned work.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public void retrieveAssignedWork() throws Exception {

    // set all records to null
    specialistRecord1 = null;
    specialistRecord2 = null;
    leadRecord = null;

    // retrieve specialist records
    SearchResultList assignedWork1 =
        workflowService.findAssignedWork(mapProject, specialist1, null, null);
    SearchResultList assignedWork2 =
        workflowService.findAssignedWork(mapProject, specialist2, null, null);
    SearchResultList assignedConflicts =
        workflowService.findAssignedConflicts(mapProject, lead, null, null);

    // try to retrieve specialist 1's record, if it exists
    if (assignedWork1.getCount() != 0) {
      specialistRecord1 =
          mappingService.getMapRecord(assignedWork1.getIterable().iterator()
              .next().getId());
    }

    // try to retrieve specialist 1's record, if it exists
    if (assignedWork2.getCount() != 0) {
      specialistRecord2 =
          mappingService.getMapRecord(assignedWork2.getIterable().iterator()
              .next().getId());
    }

    // try to retrieve specialist 1's record, if it exists
    if (assignedConflicts.getCount() != 0) {
      leadRecord =
          mappingService.getMapRecord(assignedConflicts.getIterable()
              .iterator().next().getId());
    }
  }

  /**
   * Helper function to remove all objects.
   *
   * @throws Exception the exception
   */
  public static void destroyAll() throws Exception {

    for (TrackingRecord trackingRecord : workflowService.getTrackingRecords()
        .getIterable()) {
      workflowService.removeTrackingRecord(trackingRecord.getId());
    }
    for (MapRecord mapRecord : mappingService.getMapRecords().getIterable()) {
      mappingService.removeMapRecord(mapRecord.getId());
    }
    for (MapProject mapProject : mappingService.getMapProjects().getIterable()) {
      mappingService.removeMapProject(mapProject.getId());
    }
    for (MapUser mapUser : mappingService.getMapUsers().getIterable()) {
      mappingService.removeMapUser(mapUser.getId());
    }
    for (Concept concept : contentService.getConcepts().getIterable()) {
      contentService.removeConcept(concept.getId());
    }
  }

  /**
   * Helper function to remove all mapping and workflow objects and recompute
   * workflow.
   *
   * @throws Exception the exception
   */
  public static void revertWorkToCleanState() throws Exception {

    for (TrackingRecord trackingRecord : workflowService.getTrackingRecords()
        .getIterable()) {
      workflowService.removeTrackingRecord(trackingRecord.getId());
    }
    for (MapRecord mapRecord : mappingService.getMapRecords().getIterable()) {
      mappingService.removeMapRecord(mapRecord.getId());
    }

    workflowService.computeWorkflow(mapProject);

  }

}
