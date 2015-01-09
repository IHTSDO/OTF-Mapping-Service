package org.ihtsdo.otf.mapping.services.helpers;

import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Generically represents a handler for performing a release.
 */
public interface BeginEditingCycleHandler {
  
  /**
   * Starts the editing cycle.
   * @throws Exception 
   */
  public void beginEditingCycle() throws Exception;

  /**
   * Close.
   *
   * @throws Exception the exception
   */
  public void close() throws Exception;

  /**
   * Sets the map project.
   *
   * @param mapProject the mapProject to set
   * @throws Exception the exception
   */
  public void setMapProject(MapProject mapProject) throws Exception;

}
