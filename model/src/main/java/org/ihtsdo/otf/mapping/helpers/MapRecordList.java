package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Represents a sortable list of {@link MapRecord}.
 */
public interface MapRecordList extends ResultList<MapRecord> {

  /**
   * Adds the map record.
   * 
   * @param mapRecord the map record
   */
  public void addMapRecord(MapRecord mapRecord);

  /**
   * Removes the map record.
   * 
   * @param mapRecord the map record
   */
  public void removeMapRecord(MapRecord mapRecord);

  /**
   * Sets the map records.
   * 
   * @param mapRecords the new map records
   */
  public void setMapRecords(List<MapRecord> mapRecords);

  /**
   * Gets the map records.
   * 
   * @return the map records
   */
  public List<MapRecord> getMapRecords();

}
