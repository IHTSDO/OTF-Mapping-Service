package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Container for map projects.
 */
public class TrackingRecordListJpa {

  /** The map projects. */
  private List<TrackingRecord> trackingRecords =
      new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public TrackingRecordListJpa() {
    // do nothing
  }

  /**
   * Adds the map project.
   * 
   * @param trackingRecord the map project
   */
  public void addTrackingRecord(
    TrackingRecord trackingRecord) {
    trackingRecords.add(trackingRecord);
  }

  /**
   * Removes the map project.
   * 
   * @param trackingRecord the map project
   */
  public void removeTrackingRecord(
    TrackingRecord trackingRecord) {
    trackingRecords.remove(trackingRecord);
  }

  /**
   * Sets the map projects.
   * 
   * @param trackingRecords the new map projects
   */
  public void setTrackingRecords(
    List<TrackingRecord> trackingRecords) {
    this.trackingRecords = new ArrayList<>();
    this.trackingRecords.addAll(trackingRecords);

  }

  /**
   * Sorts the map projects alphabetically by name
   */
  public void sortTrackingRecords() {

    Collections.sort(this.trackingRecords,
        new Comparator<TrackingRecord>() {
          @Override
          public int compare(TrackingRecord o1,
            TrackingRecord o2) {
            return o1.getSortKey().compareTo(o2.getSortKey());
          }

        });
  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<TrackingRecord> getTrackingRecords() {
    return trackingRecords;
  }

  /**
   * Return the count as an xml element
   * @return the number of objects in the list
   */
  public int getCount() {
    return trackingRecords.size();
  }

}
