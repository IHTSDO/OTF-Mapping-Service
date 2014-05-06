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
 *       <id>ClearWorkflow</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <dependencies>
 *               <dependency>
 *                 <groupId>org.ihtsdo.otf.mapping</groupId>
 *                 <artifactId>mapping-admin-loader-config</artifactId>
 *                 <version>${project.version}</version>
 *                 <scope>system</scope>
 *                 <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *               </dependency>
 *             </dependencies>
 *             <executions>
 *               <execution>
 *                 <id>clear-workflow</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>clear-workflow</goal>
 *                 </goals>
 *                 <configuration>
 *                   <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *                   <refSetId>${refset.id}</refSetId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile> 
 * 
 * 
 * @goal clear-workflow
 * @phase package
 */
public class ClearWorkflowRemoverMojo extends AbstractMojo {

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
    getLog().info("Starting clear workflow - " + refSetId);

    if (refSetId == null) {
      throw new MojoExecutionException("You must specify a refSetId.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<MapProject>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refSetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Clear workflow
      WorkflowService workflowService = new WorkflowServiceJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Computing workflow for " + mapProject.getName() + ", "
                + mapProject.getId());
        workflowService.clearWorkflowForMapProject(mapProject);
      }

      getLog().info("done ...");
      mappingService.close();
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Clearing workflow failed.", e);
    }

  }
}