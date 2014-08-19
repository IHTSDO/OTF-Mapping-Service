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
 * QA Check for Properly Numbered Map Groups
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>MapGroups</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>qa-map-groups</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>qa-map-groups</goal>
 *                 </goals>
 *                 <configuration>
 *                   <refSetId>${refset.id}</refSetId>
 *                   <mode>${updateRecords}</mode>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile> 
 * </pre>
 * 
 * @goal qa-map-groups
 * @phase package
 */
public class QAMapGroups extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refSetId
   * @parameter mode
   */
  private String refSetId = null;
  
  /**
   * Flag for updating vs simply checking
   * @parameter updateRecords
   */
  
  private String mode = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting map group quality assurance checks - " + refSetId);

    if (refSetId == null) {
      throw new MojoExecutionException("You must specify a refSetId.");
    }
    
    if (mode == null) {
    	throw new MojoExecutionException("You must specify a mode (check for diagnostics only, update to fix group numbering errors).");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      
      Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refSetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
        	  if (mapProject.isGroupStructure() == false) {
        		  getLog().info("Map Project " + mapProject.getName() + " does not have group structure, skipping.");
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
      
      mappingService.commit();

      getLog().info("done ...");
      mappingService.close();
      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing map group QA failed.", e);
    }

  }
}