package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapUserPreferences;

/**
 * Represents a sortable list of {@link MapUserPreferences}.
 */
public interface MapUserPreferencesList extends ResultList<MapUserPreferences> {

  /**
   * Adds the map entry
   * 
   * @param MapUserPreferences the map entry
   */
  public void addMapUserPreferences(MapUserPreferences MapUserPreferences);

  /**
   * Removes the map entry.
   * 
   * @param MapUserPreferences the map entry
   */
  public void removeMapUserPreferences(MapUserPreferences MapUserPreferences);

  /**
   * Sets the map entrys.
   * 
   * @param mapUserPreferences the new map entrys
   */
  public void setMapUserPreferences(List<MapUserPreferences> mapUserPreferences);

  /**
   * Gets the map user preferences
   * 
   * @return the map user preferences
   */
  public List<MapUserPreferences> getMapUserPreferences();

}
