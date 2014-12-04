package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>Reports</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>remove-reports</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>remove-reports</goal>
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
 * 
 * 
 * @goal remove-reports
 * @phase package
 */
public class ReportRemoverMojo extends AbstractMojo {

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
    getLog().info("Starting to remove reports - " + refSetId);

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

      // Clear workflow
      ReportService reportService = new ReportServiceJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Removing reports for " + mapProject.getName() + ", "
                + mapProject.getId());
        reportService.removeReportsForMapProject(mapProject);
      }

      getLog().info("done ...");
      mappingService.close();
      reportService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Clearing workflow failed.", e);
    }

  }
}