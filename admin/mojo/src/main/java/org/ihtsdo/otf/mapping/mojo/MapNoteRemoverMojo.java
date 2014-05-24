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
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
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
 *           <id>remove-map-notes</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>remove-map-notes</goal>
 *           </goals>
 *           <configuration>
 *             <refSetId>${refset.id}</refSetId>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal remove-map-notes
 * 
 * @phase package
 */
public class MapNoteRemoverMojo extends AbstractMojo {

  /**
   * The specified refSetId
   * @parameter
   */
  private String refSetId = null;

  /**
   * Instantiates a {@link MapNoteRemoverMojo} from the specified parameters.
   * 
   */
  public MapNoteRemoverMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting removing map notes ...");

    if (refSetId == null) {
      throw new MojoFailureException("You must specify a refSetId.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start removing map notes for project - " + refSetId);
      for (MapProject mapProject : mappingService.getMapProjects()
          .getMapProjects()) {
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
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      int ct = 0;
      for (MapProject project : mapProjects) {
        for (MapRecord record : mappingService.getMapRecordsForMapProject(
            project.getId()).getMapRecords()) {
          for (MapEntry entry : record.getMapEntries()) {
            if (entry.getMapNotes().size() > 0) {
              getLog().debug(
                  "    Remove map record note from entry - " + entry.getId());
              entry.getMapNotes().clear();
              mappingService.updateMapEntry(entry);
              if (++ct % 500 == 0) {
                getLog().info("      " + ct + " notes processed");
              }
            }
          }
          if (record.getMapNotes().size() > 0) {
            getLog().debug(
                "    Remove map record notes from record - " + record.getId());
            record.getMapNotes().clear();
            mappingService.updateMapRecord(record);
            if (++ct % 500 == 0) {
              getLog().info("      " + ct + " notes processed");
            }
          }
        }
      }
      mappingService.commit();
      getLog().info("done ...");

      mappingService.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
