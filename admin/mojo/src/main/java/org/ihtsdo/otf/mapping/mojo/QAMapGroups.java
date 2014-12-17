package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * QA Check for Properly Numbered Map Groups
 * 
 * See admin/qa/pom.xml for a sample execution.
 * 
 * @goal qa-map-groups
 * @phase package
 */
public class QAMapGroups extends AbstractMojo {

  /**
   * The refSet id
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * The mode
   * @parameter
   * @required
   */
  private String mode = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting map group quality assurance checks");
    getLog().info("  refsetId = " + refsetId);

    try {

      MappingService mappingService = new MappingServiceJpa();

      Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            if (!mapProject.isGroupStructure()) {
              getLog().info(
                  "Map Project " + mapProject.getName()
                      + " does not have group structure, skipping.");
            }
            mapProjects.add(mapProject);
          }
        }
      }

      // Perform the QA checks
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Checking map groups for " + mapProject.getName() + ", "
                + mapProject.getId());
        boolean updateRecords = mode.equals("update");
        mappingService.checkMapGroupsForMapProject(mapProject, updateRecords);
      }

      mappingService.close();
      mappingService.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing map group QA failed.", e);
    }

  }
}