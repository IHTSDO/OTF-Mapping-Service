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
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which updates concept and target code names to match current terminology
 * versions.
 * 
 * See admin/loader/pom.xml for a sample execution.
 *
 * @goal update-map-records
 * 
 * @phase package
 */
public class MapRecordUpdaterMojo extends AbstractMojo {

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * Instantiates a {@link MapRecordUpdaterMojo} from the specified parameters.
   * 
   */
  public MapRecordUpdaterMojo() {
    // Do nothing
  }

  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting updating map records for project");
    getLog().info("  refsetId = " + refsetId);

    try {

      final MappingService mappingService = new MappingServiceJpa();
      final ContentService contentService = new ContentServiceJpa();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      final Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start removing map records for refsetId - " + refsetId);
      for (final MapProject mapProject : mappingService.getMapProjects()
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

      // Update names
      int ct = 0;
      for (final MapProject mapProject : mapProjects) {
        getLog().debug("    Remove map records for " + mapProject.getName());
        for (final MapRecord record : mappingService
            .getMapRecordsForMapProject(mapProject.getId()).getMapRecords()) {

          final Concept concept =
              contentService.getConcept(record.getConceptId(),
                  mapProject.getSourceTerminology(),
                  mapProject.getSourceTerminologyVersion());

          boolean changed = false;
          // Handle source name
          if (concept != null
              && !record.getConceptName().equals(
                  concept.getDefaultPreferredName())) {
            record.setConceptName(concept.getDefaultPreferredName());
            changed = true;
          }

          // Handle target names
          for (final MapEntry entry : record.getMapEntries()) {
            if (entry.getTargetId() != null && !entry.getTargetId().isEmpty()) {
              final Concept concept2 =
                  contentService.getConcept(entry.getTargetId(),
                      mapProject.getDestinationTerminology(),
                      mapProject.getDestinationTerminologyVersion());
              if (concept != null
                  && !entry.getTargetName().equals(
                      concept2.getDefaultPreferredName())) {
                entry.setTargetName(concept2.getDefaultPreferredName());
                changed = true;
              }
            }
          }

          // Update if changed.
          if (changed) {
            mappingService.updateMapRecord(record);
            if (++ct % 2000 == 0) {
              getLog().info("      " + ct + " records processed");
              mappingService.commit();
              mappingService.clear();
              mappingService.beginTransaction();
            }
          }
        }
      }
      mappingService.commit();
      mappingService.close();
      contentService.close();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    }
  }

}
