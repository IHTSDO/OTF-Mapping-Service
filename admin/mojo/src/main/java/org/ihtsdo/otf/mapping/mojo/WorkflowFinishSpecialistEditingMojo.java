package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Admin tool to force comparison and validation of Conflict Project tracking
 * records in situations where two specialists have finished work but the
 * workflow did not successfully compare and generate CONFLICT_DETECTED or
 * READY_FOR_PUBLICATION map records.
 * 
 * Created to address an issue discovered in early October 2014 where workflow
 * advancement was not properly being executed. Only valid for projects of
 * workflow type CONFLICT_PROJECT
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>FinishSpecialistEditing</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>finish-specialist-editing</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>finish-specialist-editing</goal>
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
 * @goal finish-specialist-editing
 * @phase package
 */
public class WorkflowFinishSpecialistEditingMojo extends AbstractMojo {

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
    getLog().info(
        "Forcing workflow finish actions on errant tracking records - "
            + refSetId);

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

        if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT)) {
          getLog().info(
              "Checking workflow for " + mapProject.getName() + ", "
                  + mapProject.getId());
          workflowService.finishEditingDoneTrackingRecords(mapProject);
        } else {
          getLog().error(
              "Project " + mapProject.getName()
                  + " is not a Conflict Project -- cannot process");
        }
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