package org.ihtsdo.otf.mapping.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "workflowTrackingRecordList")
public class WorkflowTrackingRecordList {

  /** The map projects. */
  private List<WorkflowTrackingRecord> workflowTrackingRecords =
      new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public WorkflowTrackingRecordList() {
    // do nothing
  }

  /**
   * Adds the map project.
   * 
   * @param workflowTrackingRecord the map project
   */
  public void addWorkflowTrackingRecord(
    WorkflowTrackingRecord workflowTrackingRecord) {
    workflowTrackingRecords.add(workflowTrackingRecord);
  }

  /**
   * Removes the map project.
   * 
   * @param workflowTrackingRecord the map project
   */
  public void removeWorkflowTrackingRecord(
    WorkflowTrackingRecord workflowTrackingRecord) {
    workflowTrackingRecords.remove(workflowTrackingRecord);
  }

  /**
   * Sets the map projects.
   * 
   * @param workflowTrackingRecords the new map projects
   */
  public void setWorkflowTrackingRecords(
    List<WorkflowTrackingRecord> workflowTrackingRecords) {
    this.workflowTrackingRecords = new ArrayList<>();
    this.workflowTrackingRecords.addAll(workflowTrackingRecords);

  }

  /**
   * Sorts the map projects alphabetically by name
   */
  public void sortWorkflowTrackingRecords() {

    Collections.sort(this.workflowTrackingRecords,
        new Comparator<WorkflowTrackingRecord>() {
          @Override
          public int compare(WorkflowTrackingRecord o1,
            WorkflowTrackingRecord o2) {
            return o1.getSortKey().compareTo(o2.getSortKey());
          }

        });
  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  @XmlElement(type = WorkflowTrackingRecordJpa.class, name = "workflowTrackingRecord")
  public List<WorkflowTrackingRecord> getWorkflowTrackingRecords() {
    return workflowTrackingRecords;
  }

  /**
   * Return the count as an xml element
   * @return the number of objects in the list
   */
  @XmlElement(name = "count")
  public int getCount() {
    return workflowTrackingRecords.size();
  }

}
