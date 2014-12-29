package org.ihtsdo.otf.mapping.jpa.services;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowReviewProjectPathHandler;
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

public class WorkflowPathStatesQAPathTest {

  private static WorkflowQaPathHandler handler;

  private static ContentService contentService;

  private static MappingService mappingService;

  private static WorkflowService workflowService;

  private static TrackingRecord trackingRecord;

  private static MapUser specialist;

  private static MapUser lead;

  private static MapProject mapProject;

  private static Concept concept;

  @BeforeClass
  public static void init() throws Exception {

    System.out.println("Initialization");

    // instantiate the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // instantiate the workflow handler
    handler = new WorkflowQaPathHandler();

    // ensure database is clean
    for (Concept c : contentService.getConcepts().getIterable())
      contentService.removeConcept(c.getId());

    for (MapProject mp : mappingService.getMapProjects().getIterable())
      mappingService.removeMapProject(mp.getId());

    for (MapUser mu : mappingService.getMapUsers().getIterable())
      mappingService.removeMapUser(mu.getId());

    for (TrackingRecord tr : workflowService.getTrackingRecords().getIterable())
      workflowService.removeTrackingRecord(tr.getId());

    concept = new ConceptJpa();
    concept.setActive(true);
    concept.setDefaultPreferredName("Test Concept");
    concept.setDefinitionStatusId(0L);
    concept.setEffectiveTime(new Date());
    concept.setModuleId(0L);
    concept.setTerminology("sourceTerminology");
    concept.setTerminologyVersion("sourceTerminologyVersion");
    concept.setTerminologyId("1");
    contentService.addConcept(concept);

    specialist = new MapUserJpa();
    specialist.setApplicationRole(MapUserRole.VIEWER);
    specialist.setEmail("none");
    specialist.setName("Specialist");
    specialist.setUserName("spec");
    mappingService.addMapUser(specialist);

    lead = new MapUserJpa();
    lead.setApplicationRole(MapUserRole.VIEWER);
    lead.setEmail("none");
    lead.setName("Lead");
    lead.setUserName("lead");
    mappingService.addMapUser(lead);

    // instantiate the project
    mapProject = new MapProjectJpa();
    mapProject.setSourceTerminology("sourceTerminology");
    mapProject.setSourceTerminologyVersion("sourceTerminologyVersion");
    mapProject.setDestinationTerminology("destinationTerminology");
    mapProject
        .setDestinationTerminologyVersion("destinationTerminologyVersion");
    mapProject.setGroupStructure(false);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ExtendedMap);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setName("Test Project");
    mapProject.setPropagatedFlag(false);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.ICD10ProjectSpecificAlgorithmHandler");
    mapProject.setPublic(true);
    mapProject.setRefSetId("refsetId");
    mapProject.setRuleBased(true);
    mapProject.setWorkflowType(WorkflowType.REVIEW_PROJECT);
    mapProject.addMapSpecialist(specialist);
    mapProject.addMapLead(lead);
    mapProject.addScopeConcept("1");
    mappingService.addMapProject(mapProject);

  }

  @Test
  public void testLegalWorkflowCombinations() throws Exception {

    Logger.getLogger(WorkflowPathStatesQAPathTest.class).info(
        "Testing all possible combinations against legal states...");

    // test empty state
    if (handler.isEmptyWorkflowAllowed()) {
      assertTrue(
          "Empty workflow permitted",
          handler
              .isWorkflowCombinationInTrackingRecordStates(new WorkflowStatusCombination()));
    } else {
      assertTrue(
          "Empty workflow not permitted",
          !handler
              .isWorkflowCombinationInTrackingRecordStates(new WorkflowStatusCombination()));
    }

    // test declared states
    for (WorkflowStatusCombination combination : handler
        .getWorkflowStatusCombinations()) {

      // compute workflow
      getTrackingRecord(combination);

      // validate the tracking record
      assertTrue(handler.validateTrackingRecord(trackingRecord).isValid());

    }

  }

  @Test
  public void testIllegalWorkflowStatusCombinations() throws Exception {

    // set of combinations to test
    Set<WorkflowStatusCombination> combinations = new HashSet<>();

    // maximum number to test
    int nResults = 10;

    // random number generator
    Random random = new Random();

    // extract the status values for convenience
    WorkflowStatus[] statuses = WorkflowStatus.values();

    // cycle over possible combinations of records
    for (int nRecords = 1; nRecords <= 4; nRecords++) {

      // maximum calculations for statuses and number of records
      // e.g. 6 statuses, 2 records -> 36 statuses
      int maxResults = statuses.length ^ (nRecords - 1);

      // while combinations less than max results and less than desired results
      while (combinations.size() < nResults && combinations.size() < maxResults) {

        // create a new random combination
        WorkflowStatusCombination combination = new WorkflowStatusCombination();
        for (int i = 0; i < nRecords; i++) {

          combination.addWorkflowStatus(statuses[random
              .nextInt(statuses.length)]);

        }
        if (!handler.isWorkflowCombinationInTrackingRecordStates(combination)) {
          combinations.add(combination);
        }

      }
    }

    // make sure the number of generated concepts is in desired range
    assertTrue(combinations.size() > 0 && combinations.size() <= nResults);

    // test the combinations
    for (WorkflowStatusCombination combination : combinations) {

      try {
        getTrackingRecord(combination);
      } catch (Exception e) {

        // if this tracking record contains a PUBLISHED or READY_FOR_PUBLICATION
        // record, ignore error, otherwise fail
        if (combination.getWorkflowStatuses().size() == 1
            && combination.getWorkflowStatuses().keySet().iterator().next()
                .equals(WorkflowStatus.READY_FOR_PUBLICATION)
            || combination.getWorkflowStatuses().keySet().iterator().next()
                .equals(WorkflowStatus.PUBLISHED)) {
          // do nothing
        } else {
          fail("Error computing tracking record for combination "
              + combination.toString());
        }

      }
      assertTrue(!handler.validateTrackingRecord(trackingRecord).isValid());
    }

  }

  @AfterClass
  public static void cleanup() throws Exception {

    Logger.getLogger(WorkflowPathStatesQAPathTest.class).info("Clean-up");
    resetRecords();

    mappingService.removeMapProject(mapProject.getId());

    mappingService.removeMapUser(specialist.getId());
    mappingService.removeMapUser(lead.getId());

    mappingService.close();
    workflowService.close();
  }

  // helper function to clear persistence environment
  // and reinstantiate empty tracking record and map users
  private static void resetRecords() throws Exception {

    // remove tracking records
    for (TrackingRecord trackingRecord : workflowService.getTrackingRecords()
        .getTrackingRecords())
      workflowService.removeTrackingRecord(trackingRecord.getId());

    // remove map records
    for (MapRecord mapRecord : mappingService.getMapRecords().getMapRecords())
      mappingService.removeMapRecord(mapRecord.getId());
  }

  /** Construct and return a map record for a user and status */
  private MapRecord createRecord(MapUser user, WorkflowStatus status) {
    MapRecord record = new MapRecordJpa();

    record.setConceptId(concept.getTerminologyId());
    record.setConceptName(concept.getDefaultPreferredName());
    record.setCountDescendantConcepts(0L);
    record.setLastModified(new Date().getTime());
    record.setLastModifiedBy(user);
    record.setMapProjectId(mapProject.getId());
    record.setOwner(user);
    record.setTimestamp(new Date().getTime());
    record.setWorkflowStatus(status);
    return record;
  }

  /** Computes the tracking record based on map records */
  private void getTrackingRecord(WorkflowStatusCombination combination)
    throws Exception {
    System.out.println("Computing tracking record for combination: "
        + combination.toString());

    // reset the records
    resetRecords();

    // sleep 0.5s to allow transaction to complete
    Thread.sleep(500);

    Iterator<WorkflowStatus> statusIter =
        combination.getWorkflowStatusesAsList().iterator();

    // switch on size of combination
    switch (combination.getWorkflowStatuses().size()) {

    // always invalid
      case 1:
        mappingService
            .addMapRecord(createRecord(specialist, statusIter.next()));
        break;
        // Valid:  REVISION + QA_NEEDED
      case 2:
        mappingService
            .addMapRecord(createRecord(specialist, statusIter.next()));
        mappingService.addMapRecord(createRecord(lead, statusIter.next()));
        break;
        // valid: REVISION + QA_NEEDED + Specialist/Lead editing/complete
      case 3:
        mappingService
            .addMapRecord(createRecord(specialist, statusIter.next()));
        mappingService
            .addMapRecord(createRecord(specialist, statusIter.next()));
        mappingService.addMapRecord(createRecord(lead, statusIter.next()));
        break;
      default:
        fail("Unexpected number of workflow statuses, combination = "
            + combination.toString());
    }

    // compute workflow
    workflowService.computeWorkflow(mapProject);

    // sleep 1s before retrieving tracking record
    Thread.sleep(1000);
    trackingRecord = workflowService.getTrackingRecord(mapProject, concept);
  }

}
