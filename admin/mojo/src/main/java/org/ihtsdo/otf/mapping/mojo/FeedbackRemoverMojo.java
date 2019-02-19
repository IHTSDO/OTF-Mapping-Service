/**
 * Copyright (c) 2012 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
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
public class FeedbackRemoverMojo extends AbstractMojo {

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

  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting removing feedback for project");
    getLog().info("  refsetId = " + refsetId);

    try {

      final WorkflowService workflowService = new WorkflowServiceJpa();
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
        getLog().debug("    Remove feedback conversations for " + mapProject.getName());
        for (final FeedbackConversation conv : workflowService
            .getFeedbackConversationsForMapProject(mapProject.getId()).getFeedbackConversations()) {
            getLog().info("    Removing feedback conversation " + conv.getId() + " from "
                + mapProject.getName());
            workflowService.removeFeedbackConversation(conv.getId());
            if (++ct % 500 == 0) {
              getLog().info("      " + ct + " conversations processed");
            }
          
        }
      }
      workflowService.commit();
      workflowService.close();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    }
  }

}
