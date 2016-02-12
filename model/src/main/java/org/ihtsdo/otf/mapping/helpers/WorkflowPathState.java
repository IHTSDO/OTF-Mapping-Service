package org.ihtsdo.otf.mapping.helpers;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a combination of workflow states with allowed workflow status
 * combinations and allowed actions.
 */
public class WorkflowPathState {

  /** The workflow state name, used only for display purposes. */
  private String workflowStateName;

  /** The workflow combinations. */
  private Set<WorkflowStatusCombination> workflowCombinations = new HashSet<>();

  /**
   * Instantiates an empty {@link WorkflowPathState}.
   */
  public WorkflowPathState() {
  }

  /**
   * Instantiates a {@link WorkflowPathState} from the specified parameters.
   *
   * @param name the name
   */
  public WorkflowPathState(String name) {
    this.workflowStateName = name;
  }

  /**
   * Returns the workflow state name.
   *
   * @return the workflow state name
   */
  public String getWorkflowStateName() {
    return workflowStateName;
  }

  /**
   * Sets the workflow state name.
   *
   * @param workflowStateName the workflow state name
   */
  public void setWorkflowStateName(String workflowStateName) {
    this.workflowStateName = workflowStateName;
  }

  /**
   * Returns the workflow combinations.
   *
   * @return the workflow combinations
   */
  public Set<WorkflowStatusCombination> getWorkflowCombinations() {
    return workflowCombinations;
  }

  /**
   * Sets the workflow combinations.
   *
   * @param workflowCombinations the workflow combinations
   */
  public void setWorkflowCombinations(
    Set<WorkflowStatusCombination> workflowCombinations) {
    this.workflowCombinations = workflowCombinations;
  }

  /**
   * Adds the workflow combination.
   *
   * @param workflowCombination the workflow combination
   */
  public void addWorkflowCombination(
    WorkflowStatusCombination workflowCombination) {
    if (this.workflowCombinations == null) {
      this.workflowCombinations = new HashSet<>();
    }
    workflowCombinations.add(workflowCombination);
  }

  /**
   * Contains.
   *
   * @param workflowCombination the workflow combination
   * @return true, if successful
   */
  public boolean contains(WorkflowStatusCombination workflowCombination) {

    if (this.workflowCombinations.size() == 0 && workflowCombination.isEmpty())
      return true;

    return this.workflowCombinations.contains(workflowCombination);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((workflowCombinations == null) ? 0
        : workflowCombinations.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    int i = 0;
    if (this == obj)

      return true;
    System.out.println(i++);
    if (obj == null)
      return false;
    System.out.println(i++);
    if (getClass() != obj.getClass())
      return false;
    System.out.println(i++);
    WorkflowPathState other = (WorkflowPathState) obj;
    if (workflowCombinations == null) {
      if (other.workflowCombinations != null)
        return false;
      System.out.println(i++);
    } else if (!workflowCombinations.equals(other.workflowCombinations))

    {
      System.out.println(i++);
      return false;
    }
    System.out.println(i++);
    return true;
  }

  @Override
  public String toString() {
    return "WorkflowPathState [workflowStateName=" + workflowStateName
        + ", workflowCombinations=" + workflowCombinations + "]";
  }

}
