package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;

import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowNonLegacyPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for workflow actions on non legacy path.
 */
@Ignore("Workflow integration tests outdated after workflow revision")
public class WorkflowActionNonLegacyPathTest {

	// the content
	/** The concept. */
	private static Concept concept;

	// the mapping objects
	/** The lead. */
	private static MapUser viewer;

	/** The specialist. */
	private static MapUser specialist;

	/** The specialist2. */
	private static MapUser specialist2;

	/** The lead. */
	private static MapUser lead;

	/** The specialist's record. */
	private static MapRecord specRecord;

	/** The specialist's record id */
	public static Long specRecordId;

	/** The specialist's record2. */
	private static MapRecord specRecord2;

	/** The specialist's record id */
	public static Long specRecordId2;

	/** The lead record. */
	private static MapRecord leadRecord;

	/** The specialist's record id */
	public static Long leadRecordId;

	/** The map project. */
	private static MapProject mapProject;

	// the tracking record
	/** The tracking record. */
	private static TrackingRecord trackingRecord;

	// the services
	/** The content service. */
	private static ContentService contentService;

	/** The workflow service. */
	private static WorkflowService workflowService;

	// the workflow handler
	/** The handler. */
	private static WorkflowNonLegacyPathHandler handler;

	/**
	 * Inits the.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@BeforeClass
	public static void init() throws Exception {
		// instantiate the services
		contentService = new ContentServiceJpa();
		workflowService = new WorkflowServiceJpa();

		// instantiate the workflow handler
		handler = new WorkflowNonLegacyPathHandler();

		// ensure database is clean
		for (Concept c : contentService.getConcepts().getIterable()) {
			contentService.removeConcept(c.getId());
			for (TreePosition tp : contentService
					.getTreePositions(c.getTerminologyId(), c.getTerminology(), c.getTerminologyVersion())
					.getTreePositions()) {
				contentService.removeTreePosition(tp.getId());
			}
		}

		for (MapProject mp : workflowService.getMapProjects().getIterable())
			workflowService.removeMapProject(mp.getId());

		for (MapRecord mp : workflowService.getMapRecords().getIterable())
			workflowService.removeMapRecord(mp.getId());

		for (MapUser mu : workflowService.getMapUsers().getIterable())
			workflowService.removeMapUser(mu.getId());

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

		contentService.computeTreePositions("sourceTerminology", "sourceTerminologyVersion", "0", "1");

		// instantiate and add the users
		viewer = new MapUserJpa();
		viewer.setApplicationRole(MapUserRole.VIEWER);
		viewer.setEmail("none");
		viewer.setName("Viewer");
		viewer.setUserName("view");
		workflowService.addMapUser(viewer);

		specialist = new MapUserJpa();
		specialist.setApplicationRole(MapUserRole.VIEWER);
		specialist.setEmail("none");
		specialist.setName("Specialist");
		specialist.setUserName("spec");
		workflowService.addMapUser(specialist);

		specialist2 = new MapUserJpa();
		specialist2.setApplicationRole(MapUserRole.VIEWER);
		specialist2.setEmail("none");
		specialist2.setName("Specialist2");
		specialist2.setUserName("spec2");
		workflowService.addMapUser(specialist2);

		lead = new MapUserJpa();
		lead.setApplicationRole(MapUserRole.VIEWER);
		lead.setEmail("none");
		lead.setName("Lead");
		lead.setUserName("lead");
		workflowService.addMapUser(lead);

		// instantiate the project
		mapProject = new MapProjectJpa();
		mapProject.setSourceTerminology("sourceTerminology");
		mapProject.setSourceTerminologyVersion("sourceTerminologyVersion");
		mapProject.setDestinationTerminology("destinationTerminology");
		mapProject.setDestinationTerminologyVersion("destinationTerminologyVersion");
		mapProject.setGroupStructure(false);
		mapProject.setMapRefsetPattern(MapRefsetPattern.ExtendedMap);
		mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
		mapProject.setName("Test Project");
		mapProject.setPropagatedFlag(false);
		mapProject.setProjectSpecificAlgorithmHandlerClass(
				"org.ihtsdo.otf.mapping.jpa.handlers.ICD10ProjectSpecificAlgorithmHandler");
		mapProject.setPublic(true);
		mapProject.setRefSetId("refsetId");
		mapProject.setRuleBased(false); // NOTE: If this is changed to true,
										// must
										// set rules in map entry test cases
		mapProject.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
		mapProject.addMapSpecialist(specialist);
		mapProject.addMapSpecialist(specialist2);
		mapProject.addMapLead(lead);
		mapProject.addScopeConcept("1");
		workflowService.addMapProject(mapProject);

	}

	/**
	 * Test initial state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testInitialState() throws Exception {

		// clear existing records
		clearMapRecords();

		// compute workflow
		getTrackingRecord();

		// Test: assign viewer
		ValidationResult result = testAllActionsForUser(viewer);

		// all actions except cancel should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist
		result = testAllActionsForUser(specialist);

		// all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist2
		result = testAllActionsForUser(specialist2);

		// all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: assign lead
		result = testAllActionsForUser(lead);

		// all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

	}

	/**
	 * Test first specialist editing state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testFirstSpecialistEditingState() throws Exception {

		// same test for both NEW and EDITING_IN_PROGRESS
		for (WorkflowStatus status : Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)) {

			// clear existing records
			clearMapRecords();

			// compute workflow
			getTrackingRecord();

			// create specialist record
			specRecord = createRecord(specialist, status);
			workflowService.addMapRecord(specRecord);

			// compute workflow
			getTrackingRecord();

			// Test: assign viewer
			ValidationResult result = testAllActionsForUser(viewer);

			// all actions except cancel should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: Specialist
			result = testAllActionsForUser(specialist);

			// all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN should
			// fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: Specialist
			result = testAllActionsForUser(specialist2);

			// all actions but Cancel and ASSIGN_FROM_SCRATCH should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: assign lead
			result = testAllActionsForUser(lead);

			// all actions but CANCEL should fail
			// all actions but Cancel and ASSIGN_FROM_SCRATCH should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

		}

	}

	/**
	 * Test second specialist editing state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testSecondSpecialistEditingState() throws Exception {

		// first specialist
		for (WorkflowStatus status1 : Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS,
				WorkflowStatus.EDITING_DONE)) {
			for (WorkflowStatus status2 : Arrays.asList(WorkflowStatus.NEW, WorkflowStatus.EDITING_IN_PROGRESS)) {

				// clear existing records
				clearMapRecords();

				// compute workflow
				getTrackingRecord();

				// create specialist record
				specRecord = createRecord(specialist, status1);
				workflowService.addMapRecord(specRecord);

				// create second specialist record
				specRecord2 = createRecord(specialist2, status2);
				workflowService.addMapRecord(specRecord2);

				// compute workflow
				getTrackingRecord();

				// Test: assign viewer
				ValidationResult result = testAllActionsForUser(viewer);

				// all actions except cancel should fail
				for (WorkflowAction action : WorkflowAction.values()) {
					switch (action) {
					case ASSIGN_FROM_INITIAL_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case ASSIGN_FROM_SCRATCH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case CANCEL:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case CREATE_QA_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case FINISH_EDITING:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case PUBLISH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case SAVE_FOR_LATER:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case UNASSIGN:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					default:
						break;

					}
				}

				// Test: Specialist
				result = testAllActionsForUser(specialist);

				// all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
				// should fail
				for (WorkflowAction action : WorkflowAction.values()) {
					switch (action) {
					case ASSIGN_FROM_INITIAL_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case ASSIGN_FROM_SCRATCH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case CANCEL:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case CREATE_QA_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case FINISH_EDITING:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case PUBLISH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case SAVE_FOR_LATER:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case UNASSIGN:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					default:
						break;

					}
				}

				// Test: Specialist
				result = testAllActionsForUser(specialist2);

				// all actions but SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
				// should fail
				for (WorkflowAction action : WorkflowAction.values()) {
					switch (action) {
					case ASSIGN_FROM_INITIAL_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case ASSIGN_FROM_SCRATCH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case CANCEL:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case CREATE_QA_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case FINISH_EDITING:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case PUBLISH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case SAVE_FOR_LATER:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case UNASSIGN:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					default:
						break;

					}
				}

				// Test: assign lead
				result = testAllActionsForUser(lead);

				// all actions but CANCEL should fail
				for (WorkflowAction action : WorkflowAction.values()) {
					switch (action) {
					case ASSIGN_FROM_INITIAL_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case ASSIGN_FROM_SCRATCH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case CANCEL:
						assertTrue(result.getMessages().contains(action.toString()));
						break;
					case CREATE_QA_RECORD:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case FINISH_EDITING:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case PUBLISH:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case SAVE_FOR_LATER:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					case UNASSIGN:
						assertTrue(result.getErrors().contains(action.toString()));
						break;
					default:
						break;

					}
				}
			}
		}

	}

	/**
	 * Test conflict detected state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testConflictDetectedState() throws Exception {

		// clear existing records
		clearMapRecords();

		// compute workflow
		getTrackingRecord();

		// create specialist record
		specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
		workflowService.addMapRecord(specRecord);

		// create specialist2 record
		specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
		workflowService.addMapRecord(specRecord2);

		// compute workflow
		getTrackingRecord();

		// Test: assign viewer
		ValidationResult result = testAllActionsForUser(viewer);

		// all actions except cancel should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist
		result = testAllActionsForUser(specialist);

		// all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
		// should
		// fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist2
		result = testAllActionsForUser(specialist2);

		// all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
		// should
		// fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Lead
		result = testAllActionsForUser(lead);

		// all actions but CANCEL and ASSIGN_FROM_SCRATCH should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

	}

	/**
	 * Test lead editing state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testLeadEditingState() throws Exception {

		for (WorkflowStatus status : Arrays.asList(WorkflowStatus.CONFLICT_NEW, WorkflowStatus.CONFLICT_IN_PROGRESS)) {

			// clear existing records
			clearMapRecords();

			// compute workflow
			getTrackingRecord();

			// create specialist record
			specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
			workflowService.addMapRecord(specRecord);

			// create specialist2 record
			specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
			workflowService.addMapRecord(specRecord2);

			leadRecord = createRecord(lead, status);
			workflowService.addMapRecord(leadRecord);

			// compute workflow
			getTrackingRecord();

			// Test: assign viewer
			ValidationResult result = testAllActionsForUser(viewer);

			// all actions except cancel should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: Specialist
			result = testAllActionsForUser(specialist);

			// all actions but CANCEL should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: Specialist2
			result = testAllActionsForUser(specialist2);

			// all actions but CANCEL should fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				default:
					break;

				}
			}

			// Test: Lead
			result = testAllActionsForUser(lead);

			// all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
			// should
			// fail
			for (WorkflowAction action : WorkflowAction.values()) {
				switch (action) {
				case ASSIGN_FROM_INITIAL_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case ASSIGN_FROM_SCRATCH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case CANCEL:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case CREATE_QA_RECORD:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case FINISH_EDITING:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case PUBLISH:
					assertTrue(result.getErrors().contains(action.toString()));
					break;
				case SAVE_FOR_LATER:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				case UNASSIGN:
					assertTrue(result.getMessages().contains(action.toString()));
					break;
				default:
					break;

				}
			}
		}

	}

	/**
	 * Test lead finished state.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testLeadFinishedState() throws Exception {

		// clear existing records
		clearMapRecords();

		// compute workflow
		getTrackingRecord();

		// create specialist record
		specRecord = createRecord(specialist, WorkflowStatus.CONFLICT_DETECTED);
		workflowService.addMapRecord(specRecord);

		// create specialist2 record
		specRecord2 = createRecord(specialist2, WorkflowStatus.CONFLICT_DETECTED);
		workflowService.addMapRecord(specRecord2);

		leadRecord = createRecord(lead, WorkflowStatus.CONFLICT_RESOLVED);
		workflowService.addMapRecord(leadRecord);

		// compute workflow
		getTrackingRecord();

		// Test: assign viewer
		ValidationResult result = testAllActionsForUser(viewer);

		// all actions except cancel should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist
		result = testAllActionsForUser(specialist);

		// all actions but CANCEL should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Specialist2
		result = testAllActionsForUser(specialist2);

		// all actions but CANCEL should fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			default:
				break;

			}
		}

		// Test: Lead
		result = testAllActionsForUser(lead);

		// all actions but CANCEL, SAVE_FOR_LATER, FINISH_EDITING, UNASSIGN
		// should
		// fail
		for (WorkflowAction action : WorkflowAction.values()) {
			switch (action) {
			case ASSIGN_FROM_INITIAL_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case ASSIGN_FROM_SCRATCH:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case CANCEL:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case CREATE_QA_RECORD:
				assertTrue(result.getErrors().contains(action.toString()));
				break;
			case FINISH_EDITING:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case PUBLISH:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case SAVE_FOR_LATER:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			case UNASSIGN:
				assertTrue(result.getMessages().contains(action.toString()));
				break;
			default:
				break;

			}
		}
	}

	/**
	 * Test normal workflow with conflict.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("unused")
	@Test
	public void testNormalWorkflowWithConflict() throws Exception {

		// compute workflow and get tracking record
		getTrackingRecord();

		// assign specialist 1
		workflowService.processWorkflowAction(mapProject, concept, specialist, null,
				WorkflowAction.ASSIGN_FROM_SCRATCH);

		// retrieve record - expect owner set with status NEW
		for (MapRecord mr : workflowService
				.getMapRecordsForProjectAndConcept(mapProject.getId(), concept.getTerminologyId()).getMapRecords()) {
			if (mr.getOwner().equals(specialist)) {
				specRecord = mr;
				specRecordId = mr.getId();
			}
		}
		assertTrue(specRecord.getWorkflowStatus().equals(WorkflowStatus.NEW));

		// add entry
		MapEntry entry = new MapEntryJpa();
		entry.setMapRecord(specRecord);
		entry.setMapBlock(1);
		entry.setMapGroup(1);
		entry.setMapPriority(1);
		entry.setTargetId("001");
		specRecord.addMapEntry(entry);

		// save for later
		workflowService.processWorkflowAction(mapProject, concept, specialist, specRecord,
				WorkflowAction.SAVE_FOR_LATER);
		assertTrue(workflowService.getMapRecord(specRecord.getId()).getWorkflowStatus()
				.equals(WorkflowStatus.EDITING_IN_PROGRESS));

		// finish editing
		workflowService.processWorkflowAction(mapProject, concept, specialist, specRecord,
				WorkflowAction.FINISH_EDITING);
		assertTrue(workflowService.getMapRecord(specRecordId).getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE));

		// assign specialist 2
		workflowService.processWorkflowAction(mapProject, concept, specialist2, null,
				WorkflowAction.ASSIGN_FROM_SCRATCH);

		// retrieve record - expect owner set with status NEW
		for (MapRecord mr : workflowService
				.getMapRecordsForProjectAndConcept(mapProject.getId(), concept.getTerminologyId()).getMapRecords()) {
			if (mr.getOwner().equals(specialist2)) {
				specRecord2 = mr;
				specRecordId2 = mr.getId();
			}
		}
		assertTrue(specRecord2.getWorkflowStatus().equals(WorkflowStatus.NEW));

		// add entry
		entry = new MapEntryJpa();
		entry.setMapRecord(specRecord2);
		entry.setMapBlock(1);
		entry.setMapGroup(1);
		entry.setMapPriority(1);
		entry.setTargetId("002");
		specRecord2.addMapEntry(entry);

		// save for later
		workflowService.processWorkflowAction(mapProject, concept, specialist2, specRecord2,
				WorkflowAction.SAVE_FOR_LATER);
		assertTrue(workflowService.getMapRecord(specRecordId2).getWorkflowStatus()
				.equals(WorkflowStatus.EDITING_IN_PROGRESS));

		// finish editing - expect conflict on both specialist records
		workflowService.processWorkflowAction(mapProject, concept, specialist2, specRecord2,
				WorkflowAction.FINISH_EDITING);
		Thread.sleep(1000);
		MapRecord test = workflowService.getMapRecord(specRecordId);
		assertTrue(workflowService.getMapRecord(specRecordId).getWorkflowStatus()
				.equals(WorkflowStatus.CONFLICT_DETECTED));
		assertTrue(workflowService.getMapRecord(specRecordId2).getWorkflowStatus()
				.equals(WorkflowStatus.CONFLICT_DETECTED));

		// assign lead
		workflowService.processWorkflowAction(mapProject, concept, lead, null, WorkflowAction.ASSIGN_FROM_SCRATCH);

		// retrieve record - expect owner set with status NEW
		for (MapRecord mr : workflowService
				.getMapRecordsForProjectAndConcept(mapProject.getId(), concept.getTerminologyId()).getMapRecords()) {
			if (mr.getOwner().equals(lead)) {
				leadRecord = mr;
				leadRecordId = mr.getId();
			}
		}
		assertTrue(leadRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW));

		// add entry
		entry = new MapEntryJpa();
		entry.setMapRecord(leadRecord);
		entry.setMapBlock(1);
		entry.setMapGroup(1);
		entry.setMapPriority(1);
		entry.setTargetId("001");
		leadRecord.addMapEntry(entry);

		// save for later
		workflowService.processWorkflowAction(mapProject, concept, lead, leadRecord, WorkflowAction.SAVE_FOR_LATER);
		assertTrue(workflowService.getMapRecord(leadRecord.getId()).getWorkflowStatus()
				.equals(WorkflowStatus.CONFLICT_IN_PROGRESS));

		// finish editing
		workflowService.processWorkflowAction(mapProject, concept, lead, leadRecord, WorkflowAction.FINISH_EDITING);
		assertTrue(workflowService.getMapRecord(leadRecord.getId()).getWorkflowStatus()
				.equals(WorkflowStatus.CONFLICT_RESOLVED));

		// publish
		workflowService.processWorkflowAction(mapProject, concept, lead, leadRecord, WorkflowAction.PUBLISH);

		// expect tracking record to be gone
		assertNull(workflowService.getTrackingRecordForMapProjectAndConcept(mapProject, concept));

		// expect specialist map records to be deleted
		assertNull(workflowService.getMapRecord(specRecordId));
		assertNull(workflowService.getMapRecord(specRecordId2));

		// expect lead record to be PUBLISHED
		assertTrue(workflowService.getMapRecord(leadRecordId).getWorkflowStatus().equals(WorkflowStatus.PUBLISHED));
	}

	/**
	 * Cleanup.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@AfterClass
	public static void cleanup() throws Exception {
		workflowService.clearWorkflowForMapProject(mapProject);

		if (specRecord != null)
			workflowService.removeMapRecord(specRecord.getId());
		if (specRecord2 != null)
			workflowService.removeMapRecord(specRecord2.getId());
		if (leadRecord != null)
			workflowService.removeMapRecord(leadRecord.getId());
		workflowService.removeMapProject(mapProject.getId());
		workflowService.removeMapUser(specialist.getId());
		workflowService.removeMapUser(specialist2.getId());
		workflowService.removeMapUser(lead.getId());
		workflowService.close();

		contentService.removeConcept(concept.getId());
		contentService.close();

	}

	/**
	 * Returns the tracking record.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("static-method")
	private void getTrackingRecord() throws Exception {
		workflowService.computeWorkflow(mapProject);
		trackingRecord = workflowService.getTrackingRecord(mapProject, concept);
	}

	/**
	 * Clear map records.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("static-method")
	private void clearMapRecords() throws Exception {
		for (MapRecord mr : workflowService.getMapRecords().getIterable()) {
			workflowService.removeMapRecord(mr.getId());
		}
		specRecord = null;
		specRecord2 = null;
		leadRecord = null;
	}

	/**
	 * Test all actions for user.
	 *
	 * @param user
	 *            the user
	 * @return the validation result
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("static-method")
	private ValidationResult testAllActionsForUser(MapUser user) throws Exception {
		ValidationResult result = new ValidationResultJpa();

		for (WorkflowAction action : WorkflowAction.values()) {
			ValidationResult actionResult = handler.validateTrackingRecordForActionAndUser(trackingRecord, action,
					user);
			if (actionResult.isValid()) {
				result.addMessage(action.toString());
			} else {
				result.addError(action.toString());
			}
		}
		return result;
	}

	/**
	 * Creates the record.
	 *
	 * @param user
	 *            the user
	 * @param status
	 *            the status
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

}
