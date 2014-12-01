package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>ReleaseQA</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>release-qa</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>release-qa</goal>
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
 * @goal release-qa
 * @phase package
 */
public class QARelease extends AbstractMojo {

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
    getLog().info("Starting begin release QA checks - " + refSetId);

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
      ReleaseHandler releaseHandler = new ReleaseHandlerJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Performing release QA for " + mapProject.getName() + ", "
                + mapProject.getId());
       releaseHandler.performBeginReleaseQAChecks(mapProject);
      }

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }

  }
}