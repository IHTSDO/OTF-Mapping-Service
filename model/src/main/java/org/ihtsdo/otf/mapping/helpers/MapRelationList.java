package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Represents a sortable list of {@link MapRelation}.
 */
public interface MapRelationList extends ResultList<MapRelation> {

  /**
   * Adds the map relation.
   * 
   * @param mapRelation the map relation
   */
  public void addMapRelation(MapRelation mapRelation);

  /**
   * Removes the map relation.
   * 
   * @param mapRelation the map relation
   */
  public void removeMapRelation(MapRelation mapRelation);

  /**
   * Sets the map relations.
   * 
   * @param mapRelations the new map relations
   */
  public void setMapRelations(List<MapRelation> mapRelations);

  /**
   * Gets the map relations.
   * 
   * @return the map relations
   */
  public List<MapRelation> getMapRelations();

}
