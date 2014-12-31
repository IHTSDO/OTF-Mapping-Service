package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Represents a sortable list of {@link MapProject}.
 */
public interface MapProjectList extends ResultList<MapProject> {

  /**
   * Adds the map project.
   * 
   * @param mapProject the map project
   */
  public void addMapProject(MapProject mapProject);

  /**
   * Removes the map project.
   * 
   * @param mapProject the map project
   */
  public void removeMapProject(MapProject mapProject);

  /**
   * Sets the map projects.
   * 
   * @param mapProjects the new map projects
   */
  public void setMapProjects(List<MapProject> mapProjects);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<MapProject> getMapProjects();

}
