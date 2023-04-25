/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Performs special pre-release processing for project 
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal prerelease-processing
 * @phase package
 */
public class PreReleaseProcessingMojo extends AbstractOtfMappingMojo {

  /**
   * The refSet id.
   *
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Running the pre-release processes for ");
    getLog().info("  refsetId = " + refsetId);

    setupBindInfoPackage();

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refset Id.");
    }

    try {

      final MappingService mappingService = new MappingServiceJpa();

      MapProject mapProject = null;

      for (final MapProject mp : mappingService.getMapProjects()
          .getIterable()) {
        if (mp.getRefSetId().equals(refsetId)) {
          mapProject = mp;
          break;
        }
      }

      if (mapProject == null) {
        getLog().info("NO PROJECTS FOUND FOR refsetId: " + refsetId);
        mappingService.close();
        return;
      }

      // Initialize the handler
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);

      
      // Runs the pre-release processing for the passed-in project
      algorithmHandler.preReleaseProcessing();
      mappingService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Pre-release processing for project failed.", e);
    }
  }
}