/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Recalculates the map advices for all PUBLISHED and READY_FOR_PUBLICIATION map
 * records for a specific project. Perform at release time for projects that
 * require it (e.g. SNOMEDCT_US to ICD10CM)
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal recalculate-map-advice
 * @phase package
 */
public class MapAdviceRecalculateMojo extends AbstractOtfMappingMojo {

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
    getLog().info("Recalculating map advice for project");
    getLog().info("  refsetId = " + refsetId);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refset Id.");
    }

    try {

      setupBindInfoPackage();

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

      // Recalculate the map advices for all READY_FOR_PULBICATION and PUBLISHED
      // records
      mappingService.recalculateMapAdviceForProject(mapProject);
      mappingService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Recalculating map advices for project failed.", e);
    }
  }
}