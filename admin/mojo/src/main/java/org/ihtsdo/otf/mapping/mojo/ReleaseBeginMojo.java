package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Checks validity of a map project for release.
 * If remove.records set to true, remoes out of scope records
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal begin-release
 * @phase package
 */
public class ReleaseBeginMojo extends AbstractMojo {

  /**
   * The refSet id.
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * The remove records.
   * @parameter removeRecords
   */
  private boolean removeRecords = false;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting begin release QA checks - " + refsetId + ", " + removeRecords);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refsetId.");
    }


    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Perform the QA checks
      ReleaseHandler releaseHandler = new ReleaseHandlerJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Performing release QA for " + mapProject.getName() + ", "
                + mapProject.getId());
        releaseHandler.beginRelease(mapProject, removeRecords);
      }

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }

  }
}