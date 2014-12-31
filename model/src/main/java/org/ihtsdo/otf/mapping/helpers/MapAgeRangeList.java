package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapAgeRange;

/**
 * Represents a sortable list of {@link MapAgeRange}.
 */
public interface MapAgeRangeList extends ResultList<MapAgeRange> {

  /**
   * Adds the map project.
   * 
   * @param mapAgeRange the map project
   */
  public void addMapAgeRange(MapAgeRange mapAgeRange);

  /**
   * Removes the map project.
   * 
   * @param mapAgeRange the map project
   */
  public void removeMapAgeRange(MapAgeRange mapAgeRange);

  /**
   * Sets the map projects.
   * 
   * @param mapAgeRanges the new map projects
   */
  public void setMapAgeRanges(List<MapAgeRange> mapAgeRanges);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<MapAgeRange> getMapAgeRanges();

}
