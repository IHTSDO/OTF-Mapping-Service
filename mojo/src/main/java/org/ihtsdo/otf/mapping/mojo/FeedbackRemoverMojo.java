/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Goal which removes feedback conversations for a specified project id.
 * 
 * See admin/remover/pom.xml for a sample execution.
 *
 * @goal remove-feedback
 * 
 * @phase package
 */
public class FeedbackRemoverMojo extends AbstractOtfMappingMojo {

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * The input file.
   *
   * @parameter
   */
  private String inputFile = null;

  /**
   * Instantiates a {@link FeedbackRemoverMojo} from the specified parameters.
   * 
   */
  public FeedbackRemoverMojo() {
    // Do nothing
  }

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting removing feedback for project");
    getLog().info("  refsetId = " + refsetId);

    try (final WorkflowService workflowService = new WorkflowServiceJpa();) {

      setupBindInfoPackage();

      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();
      final Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start removing feedback for refsetId - " + refsetId);
      for (final MapProject mapProject : workflowService.getMapProjects()
          .getIterable()) {
        for (final String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      if (mapProjects.isEmpty()) {
        getLog().info("NO PROJECTS FOUND " + refsetId);
        return;
      }

      // Remove feedback conversations
      int ct = 0;
      for (final MapProject mapProject : mapProjects) {
        getLog().debug(
            "    Remove feedback conversations for " + mapProject.getName());
        for (final FeedbackConversation conv : workflowService
            .getFeedbackConversationsForMapProject(mapProject.getId())
            .getFeedbackConversations()) {
          getLog().info("    Removing feedback conversation " + conv.getId()
              + " from " + mapProject.getName());
          workflowService.removeFeedbackConversation(conv.getId());
          if (++ct % 500 == 0) {
            getLog().info("      " + ct + " conversations processed");
          }

        }
      }
      workflowService.commit();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    }
  }

}
