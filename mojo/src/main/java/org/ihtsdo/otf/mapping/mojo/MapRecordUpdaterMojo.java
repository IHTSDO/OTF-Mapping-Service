/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

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
public class MapRecordUpdaterMojo extends AbstractOtfMappingMojo {

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

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting updating map records for project");
    getLog().info("  refsetId = " + refsetId);

    setupBindInfoPackage();

    try (final MappingService mappingService = new MappingServiceJpa();
        final ContentService contentService = new ContentServiceJpa();) {

      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      final Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start updating map records for refsetId - " + refsetId);
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
        getLog().info("    Update map records for " + mapProject.getName());
        for (final MapRecord record : mappingService
            .getMapRecordsForMapProject(mapProject.getId()).getMapRecords()) {

          final Concept concept = contentService.getConcept(
              record.getConceptId(), mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

          boolean changed = false;
          // Handle source name
          // Don't update if dpn is TBD (which would be the case if concept
          // becomes inactive)
          if (concept != null
              && !concept.getDefaultPreferredName().equals("TBD") && !record
                  .getConceptName().equals(concept.getDefaultPreferredName())) {

            getLog().info("    Update map record " + record.getId() + " : "
                + record.getConceptId() + " from *" + record.getConceptName()
                + "* to *" + concept.getDefaultPreferredName() + "*");
            record.setConceptName(concept.getDefaultPreferredName());
            changed = true;
          }

          // Handle target names
          for (final MapEntry entry : record.getMapEntries()) {
            if (entry.getTargetId() != null && !entry.getTargetId().isEmpty()) {
              final Concept concept2 = contentService.getConcept(
                  entry.getTargetId(), mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
              if (concept2 == null && !entry.getTargetName().equals("CONCEPT NOT FOUND")) {
                getLog().info("    Update map entry " + entry.getId() + " : "
                    + record.getConceptId() + " from *" + entry.getTargetName()
                    + "* to *CONCEPT NOT FOUND*");
                entry.setTargetName("CONCEPT NOT FOUND");
                changed = true;
              }
              if (concept2 != null && !concept2.getDefaultPreferredName()
                  .equals(entry.getTargetName())) {
                getLog().info("    Update map entry " + entry.getId() + " : "
                    + record.getConceptId() + " from *" + entry.getTargetName()
                    + "* to *" + concept2.getDefaultPreferredName() + "*");
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
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    }
  }

}
