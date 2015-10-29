package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;

import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowPathState;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatusCombination;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Workflow path handler for "review project path".
 */
public class CustomWorkflowPathHandler extends AbstractWorkflowPathHandler {

  /**
   * EDIT THIS: Name your workflow path states here. The example here provides
   * an initial state, one interim state, and one terminal state
   */
  private static WorkflowPathState initialState, interimState, terminalState;

  /**
   * Instantiates an empty {@link CustomWorkflowPathHandler}.
   */
  public CustomWorkflowPathHandler() {

    // set the workflow path
    setWorkflowPath(null); // EDIT THIS: Set your WorkflowPath here

    // set whether empty workflow is allowed for your project
    // empty workflow means a Tracking Record with no map records is allowed to
    // exist
    setEmptyWorkflowAllowed(true);

    /**
     * Initial State used as fully fleshed-out example
     */

    // define the state
    initialState = new WorkflowPathState("Initial State");

    // add the workflow combinations to the state
    initialState.addWorkflowCombination(new WorkflowStatusCombination(
    // YOUR CODE HERE
    // Repeat this for each valid workflow combination
    // Example: Arrays.asList(WorkflowStatus.X, WorkflowStatus.X, ...)
        ));

    // add the list of actions permitted for this state
    trackingRecordStateToActionMap.put(initialState,
        new HashSet<WorkflowAction>(
        // YOUR CODE HERE: List of actions permitted for this state
        ));

    /**
     * Define interim and terminal states in the same way
     * Add or delete states are required
     */

    // interim state
    interimState = new WorkflowPathState("Interim State");
    trackingRecordStateToActionMap.put(interimState,
        new HashSet<WorkflowAction>());

    // terminal state
    terminalState = new WorkflowPathState("Terminal State");
    trackingRecordStateToActionMap.put(terminalState,
        new HashSet<WorkflowAction>());

  }

  @Override
  public ValidationResult validateTrackingRecordForActionAndUser(
    TrackingRecord tr, WorkflowAction action, MapUser user) throws Exception {

    // validate the tracking record itself
    ValidationResult result = validateTrackingRecord(tr);
    if (!result.isValid()) {
      result
          .addError("Could not validate action for user due to workflow errors.");
      return result;
    }

    // get the user role for this map project
    MappingService mappingService = new MappingServiceJpa();
    MapUserRole userRole =
        mappingService.getMapUserRoleForMapProject(user.getUserName(),
            tr.getMapProjectId());
    mappingService.close();

    // get the map records and workflow path state from the tracking record
    MapRecordList mapRecords = getMapRecordsForTrackingRecord(tr);
    MapRecord currentRecord = getCurrentMapRecordForUser(mapRecords, user);
    WorkflowPathState state = this.getWorkflowStateForTrackingRecord(tr);

    // /////////////////////////////////
    // Switch on workflow path state //
    // /////////////////////////////////

    // check for null state retrieved
    if (state == null) {
      result
          .addError("Could not determine workflow path state for tracking record");
    }

    else if (state.equals(initialState)) {

      // YOUR CODE HERE

    } else if (state.equals(interimState)) {

      // YOUR CODE HERE

    } else if (state.equals(terminalState)) {

      // YOUR CODE HERE

    } else {
      result.addError("Could not determine workflow state for tracking record");
    }

    if (result.getErrors().size() != 0) {
      result.addError("Error occured in workflow state "
          + state.getWorkflowStateName());
      ;
    }

    return result;
  }
}
