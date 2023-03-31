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
 * Performs special post-release processing for project 
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal postrelease-processing
 * @phase package
 */
public class PostReleaseProcessingMojo extends AbstractOtfMappingMojo {

  /**
   * The refSet id.
   *
   * @parameter refsetId
   */
  private String refsetId = null;
  
  /**
   * The effective time.
   *
   * @parameter effectiveTime
   */
  private String effectiveTime = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Running the post-release processes for ");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  effectiveTime = " + effectiveTime);

    setupBindInfoPackage();

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refset Id.");
    }

    if (effectiveTime == null) {
      throw new MojoExecutionException("You must specify an effective time.");
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

      
      // Runs the post-release processing for the passed-in project, on the specific release based on effectiveTime
      algorithmHandler.postReleaseProcessing(effectiveTime);
      mappingService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Post-release processing for project failed.", e);
    }
  }
}