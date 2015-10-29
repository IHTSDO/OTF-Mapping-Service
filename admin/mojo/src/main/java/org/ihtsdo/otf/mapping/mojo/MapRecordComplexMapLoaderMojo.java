package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal create-map-records-from-complex-map
 * @phase package
 */
public class MapRecordComplexMapLoaderMojo extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refsetId
   * @required
   */
  private String refsetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting generating map records from complex map records");
    getLog().info("  refsetId = " + refsetId);

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

      // Generate members
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "  Generating records for " + mapProject.getName() + ", "
                + mapProject.getId());
        mappingService.createMapRecordsForMapProject(mapProject.getId(),
            WorkflowStatus.PUBLISHED);
      }

      mappingService.close();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Generating map records from complex maps failed.", e);
    }

  }
}