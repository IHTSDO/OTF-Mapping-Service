package org.ihtsdo.otf.mapping.helpers;

import java.util.HashSet;
import java.util.Set;

// TODO: Auto-generated Javadoc
/**
 * The Class WorkflowStatusCombination.
 *
 * @author ${author}
 */
public class WorkflowStatusCombination {

  /** The workflow status combination. */
  Set<WorkflowStatus> workflowStatusCombination = new HashSet<>();
  
  /**
   * Instantiates a {@link WorkflowStatusCombination} from the specified parameters.
   *
   * @param workflowStatusCombination the workflow status combination
   */
  public WorkflowStatusCombination(Set<WorkflowStatus> workflowStatusCombination) {
    this.workflowStatusCombination = workflowStatusCombination;
  }

  public WorkflowStatusCombination() {
    // TODO Auto-generated constructor stub
  }

  /**
   * Returns the workflow status combination.
   *
   * @return the workflow status combination
   */
  public Set<WorkflowStatus> getWorkflowStatusCombination() {
    return workflowStatusCombination;
  }

  /**
   * Sets the workflow status combination.
   *
   * @param workflowStatusCombination the workflow status combination
   */
  public void setWorkflowStatusCombination(
    Set<WorkflowStatus> workflowStatusCombination) {
    this.workflowStatusCombination = workflowStatusCombination;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result
            + ((workflowStatusCombination == null) ? 0
                : workflowStatusCombination.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WorkflowStatusCombination other = (WorkflowStatusCombination) obj;
    if (workflowStatusCombination == null) {
      if (other.workflowStatusCombination != null)
        return false;
    } else if (!workflowStatusCombination
        .equals(other.workflowStatusCombination))
      return false;
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "WorkflowPathCombination [workflowStatusCombination="
        + workflowStatusCombination + "]";
  }
  
  
 
  
  
}
