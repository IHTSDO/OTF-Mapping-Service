package org.ihtsdo.otf.mapping.helpers;

import java.util.HashSet;
import java.util.Set;

public class WorkflowState {

  private String workflowStateName;

  private Set<WorkflowStatusCombination> workflowCombinations = new HashSet<>();

  public WorkflowState() {
  }

  public WorkflowState(String name) {
    this.workflowStateName = name;
  }

  public String getWorkflowStateName() {
    return workflowStateName;
  }

  public void setWorkflowStateName(String workflowStateName) {
    this.workflowStateName = workflowStateName;
  }

  public Set<WorkflowStatusCombination> getWorkflowCombinations() {
    return workflowCombinations;
  }

  public void setWorkflowCombinations(
    Set<WorkflowStatusCombination> workflowCombinations) {
    this.workflowCombinations = workflowCombinations;
  }

  public void addWorkflowCombination(WorkflowStatusCombination workflowCombination) {
    if (this.workflowCombinations == null) {
      this.workflowCombinations = new HashSet<>();
    }
    workflowCombinations.add(workflowCombination);
  }
  
  public boolean contains(WorkflowStatusCombination workflowCombination) {
    return this.workflowCombinations.contains(workflowCombination);
  }
}
