package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>Workflow</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>qa-workflow</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>qa-workflow</goal>
 *                 </goals>
 *                 <configuration>
 *                   <refSetId>${refset.id}</refSetId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile>
 * </pre>
 * 
 * @goal qa-workflow
 * @phase package
 */
public class QAWorkflow extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refSetId
   */
  private String refSetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting workflow quality assurance checks - " + refSetId);

    if (refSetId == null) {
      throw new MojoExecutionException("You must specify a refSetId.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refSetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Perform the QA checks
      WorkflowService workflowService = new WorkflowServiceJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Checking workflow for " + mapProject.getName() + ", "
                + mapProject.getId());
        workflowService.computeWorkflowStatusErrors(mapProject);
      }

      getLog().info("done ...");
      mappingService.close();
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }

  }
}