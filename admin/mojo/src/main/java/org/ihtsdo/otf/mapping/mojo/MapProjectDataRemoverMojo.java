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
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * Goal which removes all map projects and associated data from the database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-map-projects
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
    getLog().info("Starting removing map project data");

    try {

      MappingService mappingService = new MappingServiceJpa();
      ReportService reportService = new ReportServiceJpa();

      // Remove map projects
      for (MapProject p : mappingService.getMapProjects().getIterable()) {
        getLog().info("  Remove map project - " + p.getName());
        if (mappingService.getMapRecordsForMapProject(p.getId())
            .getTotalCount() != 0) {
          throw new MojoFailureException(
              "Attempt to delete a map project that has map records, delete the map records first");
        }
        mappingService.removeMapProject(p.getId());
      }

      // Remove map preferences
      for (MapUserPreferences p : mappingService.getMapUserPreferences()
          .getIterable()) {
        getLog().info(
            "  Remove map user preferences - " + p.getMapUser().getName());
        mappingService.removeMapUserPreferences(p.getId());
      }

      // Remove map users
      for (MapUser l : mappingService.getMapUsers().getIterable()) {
        getLog().info("  Remove map user - " + l.getName());
        mappingService.removeMapUser(l.getId());
      }

      // Remove map advices
      for (MapAdvice a : mappingService.getMapAdvices().getIterable()) {
        getLog().info("  Remove map advice - " + a.getName());
        mappingService.removeMapAdvice(a.getId());
      }

      // Remove map relations
      for (MapRelation a : mappingService.getMapRelations().getIterable()) {
        getLog().info("  Remove map relation - " + a.getName());
        mappingService.removeMapRelation(a.getId());
      }

      // Remove map principles
      for (MapPrinciple p : mappingService.getMapPrinciples().getIterable()) {
        getLog().info("  Remove map principle - " + p.getName());
        mappingService.removeMapPrinciple(p.getId());
      }

      // Remove map age ranges
      for (MapAgeRange r : mappingService.getMapAgeRanges().getIterable()) {
        getLog().info("  Remove map age range - " + r.getName());
        mappingService.removeMapAgeRange(r.getId());
      }

      // Remove report definitions
      for (ReportDefinition def : reportService.getReportDefinitions()
          .getReportDefinitions()) {
        getLog().info("  Remove report definition - " + def.getName());
        reportService.removeReportDefinition(def.getId());
      }
      
      // Remove qa check definitions
      for (ReportDefinition def : reportService.getQACheckDefinitions()
          .getReportDefinitions()) {
        getLog().info("  Remove report definition - " + def.getName());
        reportService.removeReportDefinition(def.getId());
      }

      mappingService.close();
      reportService.close();
      getLog().info("done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
