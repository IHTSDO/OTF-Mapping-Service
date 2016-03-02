package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal clear-workflow
 * @phase package
 */
public class ClearWorkflowRemoverMojo extends AbstractMojo {

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
    getLog().info("Starting clear workflow - " + refsetId);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refsetId.");
    }

    try {

      final WorkflowService workflowService = new WorkflowServiceJpa();
      final Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : workflowService.getMapProjects()
          .getIterable()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Clear workflow
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Clearing workflow for " + mapProject.getName() + ", "
                + mapProject.getId());
        workflowService.clearWorkflowForMapProject(mapProject);
      }

      getLog().info("done ...");
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Clearing workflow failed.", e);
    }

  }
}