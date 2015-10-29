package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Checks validity of a map project for release. If remove.records set to true,
 * removes out of scope records
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal begin-release
 * @phase package
 */
public class ReleaseBeginMojo extends AbstractMojo {

  /**
   * The refSet id.
   * @parameter
   */
  private String refsetId = null;

  /**
   * Flag indicating test mode
   * @parameter
   */
  private boolean testModeFlag = false;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Begin RF2 release");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  testModeFlag = " + testModeFlag);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a ref set id");
    }

    if (refsetId.contains(",")) {
      throw new MojoExecutionException(
          "You must specify only a single ref set id");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = null;

      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        if (project.getRefSetId().equals(refsetId)) {
          mapProject = project;
          break;
        }
      }

      // Begin the release
      ReleaseHandler releaseHandler = new ReleaseHandlerJpa(testModeFlag);
      getLog().info(
          "  Handle project " + mapProject.getName() + ", "
              + mapProject.getId());
      releaseHandler.setMapProject(mapProject);
      releaseHandler.beginRelease();

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing begin release.", e);
    }

  }
}