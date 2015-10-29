package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Represents a sortable list of {@link TrackingRecord}.
 */
public interface TrackingRecordList extends ResultList<TrackingRecord> {

  /**
   * Adds the tracking record
   * 
   * @param TrackingRecord the tracking record
   */
  public void addTrackingRecord(TrackingRecord TrackingRecord);

  /**
   * Removes the tracking record.
   * 
   * @param TrackingRecord the tracking record
   */
  public void removeTrackingRecord(TrackingRecord TrackingRecord);

  /**
   * Sets the tracking records.
   * 
   * @param trackingRecords the new tracking records
   */
  public void setTrackingRecords(List<TrackingRecord> trackingRecords);

  /**
   * Gets the tracking records.
   * 
   * @return the tracking records
   */
  public List<TrackingRecord> getTrackingRecords();

}
