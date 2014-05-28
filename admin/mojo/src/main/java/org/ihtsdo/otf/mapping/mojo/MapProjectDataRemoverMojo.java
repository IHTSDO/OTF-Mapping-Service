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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which removes all map projects and associated data from the database.
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>remove-map-projects</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>remove-map-projects</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal remove-map-projects
 * 
 * @phase package
 */
public class MapProjectDataRemoverMojo extends AbstractMojo {

  /**
   * Instantiates a {@link MapProjectDataRemoverMojo} from the specified
   * parameters.
   * 
   */
  public MapProjectDataRemoverMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting removing map project data ...");

    try {

      MappingService service = new MappingServiceJpa();
      // Remove map projects
      for (MapProject p : service.getMapProjects().getIterable()) {
        getLog().info("  Remove map project - " + p.getName());
        if (service.getMapRecordsForMapProject(p.getId()).getTotalCount() != 0) {
          throw new MojoFailureException(
              "Attempt to delete a map project that has map records, delete the map records first");
        }
        service.removeMapProject(p.getId());
      }

      // Remove map users
      for (MapUser l : service.getMapUsers().getIterable()) {
        getLog().info("  Remove map user - " + l.getName());
        service.removeMapUser(l.getId());
      }

      // Remove map advices
      for (MapAdvice a : service.getMapAdvices().getIterable()) {
        getLog().info("  Remove map advice - " + a.getName());
        service.removeMapAdvice(a.getId());
      }

      // Remove map relations
      for (MapRelation a : service.getMapRelations().getIterable()) {
        getLog().info("  Remove map relation - " + a.getName());
        service.removeMapRelation(a.getId());
      }

      // Remove map principles
      for (MapPrinciple p : service.getMapPrinciples().getIterable()) {
        getLog().info("  Remove map principle - " + p.getName());
        service.removeMapPrinciple(p.getId());
      }

      // Remove map age ranges
      for (MapAgeRange r : service.getMapAgeRanges()) {
        getLog().info("  Remove map age range - " + r.getName());
        service.removeMapAgeRange(r.getId());
      }

      getLog().info("done ...");

      service.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
