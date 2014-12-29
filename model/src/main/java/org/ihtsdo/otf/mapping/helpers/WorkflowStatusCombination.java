package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a collection of workflow status values that is allowed for a
 * {@link WorkflowPathState}.
 */
public class WorkflowStatusCombination {

  /**
   * The workflow statuses and count for each one E.g. CONFLICT_DETECTED,
   * CONFLICTED_DETECTED -> (CONFLICT_DETECTED, 2) .
   */
  private Map<WorkflowStatus, Integer> workflowStatuses = new HashMap<>();

  /**
   * Instantiates an empty {@link WorkflowStatusCombination}.
   */
  public WorkflowStatusCombination() {

  }

  /**
   * Instantiates a {@link WorkflowStatusCombination} from the specified
   * parameters.
   *
   * @param workflowStatuses the workflow statuses
   */
  public WorkflowStatusCombination(List<WorkflowStatus> workflowStatuses) {
    for (WorkflowStatus w : workflowStatuses) {
      addWorkflowStatus(w);
    }
  }

  /**
   * Adds the workflow status.
   *
   * @param workflowStatus the workflow status
   */
  public void addWorkflowStatus(WorkflowStatus workflowStatus) {

    // check for null map
    if (workflowStatuses == null)
      workflowStatuses = new HashMap<>();

    // if this contains this workflow status, increment count
    workflowStatuses.put(
        workflowStatus,
        workflowStatuses.get(workflowStatus) == null ? 1 : workflowStatuses
            .get(workflowStatus) + 1);

  }

  /**
   * Returns the workflow statuses.
   *
   * @return the workflow statuses
   */
  public Map<WorkflowStatus, Integer> getWorkflowStatuses() {
    return workflowStatuses;
  }

  /**
   * Sets the workflow statuses.
   *
   * @param workflowStatuses the workflow statuses
   */
  public void setWorkflowStatuses(Map<WorkflowStatus, Integer> workflowStatuses) {
    this.workflowStatuses = workflowStatuses;
  }

  /**
   * Indicates whether or not the workflow combination represents an empty state
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isEmpty() {
    return this.workflowStatuses.isEmpty();
  }

  /**
   * Function to return all workflow statuses as a list (duplicate values
   * permitted).
   *
   * @return the workflow statuses as list
   */
  public List<WorkflowStatus> getWorkflowStatusesAsList() {
    List<WorkflowStatus> statuses = new ArrayList<>();
    for (WorkflowStatus status : workflowStatuses.keySet()) {
      for (int i = 0; i < workflowStatuses.get(status); i++) {
        statuses.add(status);
      }
    }
    return statuses;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + ((workflowStatuses == null) ? 0 : workflowStatuses.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WorkflowStatusCombination other = (WorkflowStatusCombination) obj;
    if (workflowStatuses == null) {
      if (other.workflowStatuses != null)
        return false;
    } else if (!workflowStatuses.equals(other.workflowStatuses))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "WorkflowStatusCombination [workflowStatuses=" + workflowStatuses
        + "]";
  }

}
