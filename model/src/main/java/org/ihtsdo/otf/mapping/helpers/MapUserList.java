package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Represents a sortable list of {@link MapUser}.
 */
public interface MapUserList extends ResultList<MapUser> {

  /**
   * Adds the map entry
   * 
   * @param MapUser the map entry
   */
  public void addMapUser(MapUser MapUser);

  /**
   * Removes the map entry.
   * 
   * @param MapUser the map entry
   */
  public void removeMapUser(MapUser MapUser);

  /**
   * Sets the map entrys.
   * 
   * @param mapUsers the new map entrys
   */
  public void setMapUsers(List<MapUser> mapUsers);

  /**
   * Gets the map entrys.
   * 
   * @return the map entrys
   */
  public List<MapUser> getMapUsers();

}
