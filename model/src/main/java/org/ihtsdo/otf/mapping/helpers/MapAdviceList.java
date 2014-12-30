package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * Represents a sortable list of {@link MapAdvice}.
 */
public interface MapAdviceList extends ResultList<MapAdvice> {

  /**
   * Adds the map project.
   * 
   * @param mapAdvice the map project
   */
  public void addMapAdvice(MapAdvice mapAdvice);

  /**
   * Removes the map project.
   * 
   * @param mapAdvice the map project
   */
  public void removeMapAdvice(MapAdvice mapAdvice);

  /**
   * Sets the map projects.
   * 
   * @param mapAdvices the new map projects
   */
  public void setMapAdvices(List<MapAdvice> mapAdvices);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<MapAdvice> getMapAdvices();

}
