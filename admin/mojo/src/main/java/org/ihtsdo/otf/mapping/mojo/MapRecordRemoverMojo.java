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
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which removes map notes for a specified project id.
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>remove-map-records</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>remove-map-records</goal>
 *           </goals>
 *           <configuration>
 *             <!-- one of the two must be used -->
 *             <refsetId>${refset.id}</refsetId>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal remove-map-records
 * 
 * @phase package
 */
public class MapRecordRemoverMojo extends AbstractMojo {

  /**
   * The specified refSetId
   * @parameter
   */
  private String refSetId = null;

  /**
   * Instantiates a {@link MapRecordRemoverMojo} from the specified parameters.
   * 
   */
  public MapRecordRemoverMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting removing map records for project - " + refSetId);

    if (refSetId == null) {
      throw new MojoExecutionException("You must specify a refSetId.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start removing map records for refSetId - " + refSetId);
      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refSetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      if (mapProjects.isEmpty()) {
        getLog().info("NO PROJECTS FOUND " + refSetId);
        return;
      }

      // Remove map record and entry notes
      int ct = 0;
      for (MapProject mapProject : mapProjects) {
        getLog().debug("    Remove map records for " + mapProject.getName());
        for (MapRecord record : mappingService.getMapRecordsForMapProject(
            mapProject.getId()).getMapRecords()) {
          getLog().info(
              "    Removing map record " + record.getId() + " from "
                  + mapProject.getName());
          mappingService.removeMapRecord(record.getId());
          if (++ct % 500 == 0) {
            getLog().info("      " + ct + " records processed");
          }
        }
      }
      mappingService.commit();
      getLog().info("done ...");

      mappingService.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    }
  }

}
