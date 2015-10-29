/*
 * 
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Date;

import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.BeginEditingCycleHandler;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * RF2 implementation of {@link ReleaseHandler}.
 */
public class BeginEditingCycleHandlerJpa implements BeginEditingCycleHandler {

  /** The map project. */
  private MapProject mapProject = null;

  /**
   * Instantiates an empty {@link BeginEditingCycleHandlerJpa}.
   *
   * @throws Exception the exception
   */
  public BeginEditingCycleHandlerJpa() throws Exception {

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.StartEditingCycleHandler#close()
   */
  @Override
  public void close() throws Exception {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.helpers.StartEditingCycleHandler#setMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void setMapProject(MapProject mapProject)
    throws InstantiationException, IllegalAccessException,
    ClassNotFoundException {
    this.mapProject = mapProject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.helpers.BeginEditingCycleHandler#
   * startEditingCycle()
   */
  @Override
  public void beginEditingCycle() throws Exception {

    // Simply set the editing cycle start date
    if (mapProject == null) {
      throw new Exception("Map project is null.");
    }
    MappingService mappingService = new MappingServiceJpa();
    mapProject.setEditingCycleBeginDate(new Date());
    mappingService.updateMapProject(mapProject);
    mappingService.close();

    // Consider removing all report result items as well
  }

}
