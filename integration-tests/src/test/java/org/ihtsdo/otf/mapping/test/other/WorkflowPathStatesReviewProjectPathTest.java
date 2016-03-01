package org.ihtsdo.otf.mapping.test.other;

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
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowReviewProjectPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for workflow path states on "review project path".
 */
@Ignore("Workflow integration tests outdated after workflow revision")
public class WorkflowPathStatesReviewProjectPathTest {

  /** The handler. */
  private static WorkflowReviewProjectPathHandler handler;

  /** The content service. */
  private static ContentService contentService;

  /** The mapping service. */
  private static MappingService mappingService;

  /** The workflow service. */
  private static WorkflowService workflowService;

  /** The tracking record. */
  private static TrackingRecord trackingRecord;

  /** The specialist. */
  private static MapUser specialist;

  /** The lead. */
  private static MapUser lead;

  /** The map project. */
  private static MapProject mapProject;

  /** The concept. */
  private static Concept concept;

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void init() throws Exception {

    // instantiate the services
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();

    // instantiate the workflow handler
    handler = new WorkflowReviewProjectPathHandler();

    // ensure database is clean
    for (Concept c : contentService.getConcepts().getIterable())
      contentService.removeConcept(c.getId());

    for (MapProject mp : mappingService.getMapProjects().getIterable())
      mappingService.removeMapProject(mp.getId());

    for (MapUser mu : mappingService.getMapUsers().getIterable())
      if (!mu.getUserName().equals("guest")
          && !mu.getUserName().equals("loader")
          && !mu.getUserName().equals("qa")) {
        mappingService.removeMapUser(mu.getId());
      }
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

  /**
   * Test legal workflow combinations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLegalWorkflowCombinations() throws Exception {

    Logger.getLogger(WorkflowPathStatesReviewProjectPathTest.class).info(
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

  /**
   * Test illegal workflow status combinations.
   *
   * @throws Exception the exception
   */
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

  /**
   * Cleanup.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void cleanup() throws Exception {

    Logger.getLogger(WorkflowPathStatesReviewProjectPathTest.class).info(
        "Clean-up");
    resetRecords();

    mappingService.removeMapProject(mapProject.getId());

    mappingService.removeMapUser(specialist.getId());
    mappingService.removeMapUser(lead.getId());

    mappingService.close();
    workflowService.close();
  }

  // helper function to clear persistence environment
  // and reinstantiate empty tracking record and map users
  /**
   * Reset records.
   *
   * @throws Exception the exception
   */
  private static void resetRecords() throws Exception {

    // remove tracking records
    for (TrackingRecord trackingRecord : workflowService.getTrackingRecords()
        .getTrackingRecords())
      workflowService.removeTrackingRecord(trackingRecord.getId());

    // remove map records
    for (MapRecord mapRecord : mappingService.getMapRecords().getMapRecords())
      mappingService.removeMapRecord(mapRecord.getId());
  }

  /**
   * Construct and return a map record for a user and status.
   *
   * @param user the user
   * @param status the status
   * @return the map record
   */
  @SuppressWarnings("static-method")
  private MapRecord createRecord(MapUser user, WorkflowStatus status) {
    MapRecord record = new MapRecordJpa();

    record.setConceptId(concept.getTerminologyId());
    record.setConceptName(concept.getDefaultPreferredName());
    record.setLastModified(new Date().getTime());
    record.setLastModifiedBy(user);
    record.setMapProjectId(mapProject.getId());
    record.setOwner(user);
    record.setTimestamp(new Date().getTime());
    record.setWorkflowStatus(status);
    return record;
  }

  /**
   * Computes the tracking record based on map records.
   *
   * @param combination the combination
   * @throws Exception the exception
   */
  private void getTrackingRecord(WorkflowStatusCombination combination)
    throws Exception {

    // reset the records
    resetRecords();

    // sleep 0.5s to allow transaction to complete
    Thread.sleep(500);

    Iterator<WorkflowStatus> statusIter =
        combination.getWorkflowStatusesAsList().iterator();

    // switch on size of combination
    switch (combination.getWorkflowStatuses().size()) {

    // empty workflow
      case 1:
        mappingService
            .addMapRecord(createRecord(specialist, statusIter.next()));
        break;
      case 2:
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
