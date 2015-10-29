package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Container for map projects.
 */
public class TrackingRecordListJpa extends AbstractResultList<TrackingRecord>
    implements TrackingRecordList {

  /** The map projects. */
  private List<TrackingRecord> trackingRecords = new ArrayList<>();

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
  @Override
  public void addTrackingRecord(TrackingRecord trackingRecord) {
    trackingRecords.add(trackingRecord);
  }

  /**
   * Removes the map project.
   * 
   * @param trackingRecord the map project
   */
  @Override
  public void removeTrackingRecord(TrackingRecord trackingRecord) {
    trackingRecords.remove(trackingRecord);
  }

  /**
   * Sets the map projects.
   * 
   * @param trackingRecords the new map projects
   */
  @Override
  public void setTrackingRecords(List<TrackingRecord> trackingRecords) {
    this.trackingRecords = new ArrayList<>();
    this.trackingRecords.addAll(trackingRecords);

  }

  /**
   * Sorts the map projects alphabetically by name
   */
  public void sortTrackingRecords() {

    Collections.sort(this.trackingRecords, new Comparator<TrackingRecord>() {
      @Override
      public int compare(TrackingRecord o1, TrackingRecord o2) {
        return o1.getSortKey().compareTo(o2.getSortKey());
      }

    });
  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  @Override
  public List<TrackingRecord> getTrackingRecords() {
    return trackingRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return trackingRecords.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<TrackingRecord> comparator) {
    Collections.sort(trackingRecords, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(TrackingRecord element) {
    return trackingRecords.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<TrackingRecord> getIterable() {
    return trackingRecords;
  }

}
