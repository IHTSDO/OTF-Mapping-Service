package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapPrinciple;

/**
 * Represents a sortable list of {@link MapPrinciple}.
 */
public interface MapPrincipleList extends ResultList<MapPrinciple> {

  /**
   * Adds the map principle.
   * 
   * @param mapPrinciple the map principle
   */
  public void addMapPrinciple(MapPrinciple mapPrinciple);

  /**
   * Removes the map principle.
   * 
   * @param mapPrinciple the map principle
   */
  public void removeMapPrinciple(MapPrinciple mapPrinciple);

  /**
   * Sets the map principles.
   * 
   * @param mapPrinciples the new map principles
   */
  public void setMapPrinciples(List<MapPrinciple> mapPrinciples);

  /**
   * Gets the map principles.
   * 
   * @return the map principles
   */
  public List<MapPrinciple> getMapPrinciples();

}
