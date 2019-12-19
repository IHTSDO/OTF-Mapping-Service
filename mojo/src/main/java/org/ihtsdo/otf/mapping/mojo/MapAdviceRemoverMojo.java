/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Removes a map advice entirely from environment
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-map-advice
 * @phase package
 */
public class MapAdviceRemoverMojo extends AbstractOtfMappingMojo {

  /**
   * The refSet id
   * @parameter refsetId
   */
  private String mapAdviceName = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Removing map advice");
    getLog().info("  mapAdviceName = " + mapAdviceName);

    if (mapAdviceName == null) {
      throw new MojoExecutionException(
          "You must specify the full name of the map advice.");
    }

    try (final MappingService mappingService = new MappingServiceJpa();) {

      setupBindInfoPackage();

      MapAdvice mapAdvice = null;
      for (MapAdvice ma : mappingService.getMapAdvices().getIterable()) {
        if (ma.getName().equals(mapAdviceName))
          mapAdvice = ma;
      }
      if (mapAdvice == null)
        throw new MojoExecutionException(
            "The map advice to be removed does not exist");

      getLog()
          .info("Found map advice to remove (id = " + mapAdvice.getId() + ")");

      // Remove the advice
      mappingService.removeMapAdviceFromEnvironment(mapAdvice);

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Removing map advice from environment failed.", e);
    }

  }
}