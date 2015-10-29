package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapEntry;

/**
 * Represents a sortable list of {@link MapEntry}.
 */
public interface MapEntryList extends ResultList<MapEntry> {
  /**
   * Adds the map entry
   * 
   * @param MapEntry the map entry
   */
  public void addMapEntry(MapEntry MapEntry);

  /**
   * Removes the map entry.
   * 
   * @param MapEntry the map entry
   */
  public void removeMapEntry(MapEntry MapEntry);

  /**
   * Sets the map entrys.
   * 
   * @param mapEntrys the new map entrys
   */
  public void setMapEntrys(List<MapEntry> mapEntrys);

  /**
   * Gets the map entrys.
   * 
   * @return the map entrys
   */
  public List<MapEntry> getmapEntrys();

}