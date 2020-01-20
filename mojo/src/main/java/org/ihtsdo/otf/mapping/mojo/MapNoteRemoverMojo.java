/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which removes map notes for a specified project id.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-map-notes
 * 
 * @phase package
 */
public class MapNoteRemoverMojo extends AbstractOtfMappingMojo {

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * Instantiates a {@link MapNoteRemoverMojo} from the specified parameters.
   * 
   */
  public MapNoteRemoverMojo() {
    // Do nothing
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting removing map notes");
    getLog().info("  refsetId = " + refsetId);

    setupBindInfoPackage();

    try (final MappingService mappingService = new MappingServiceJpa();) {

      Set<MapProject> mapProjects = new HashSet<>();

      getLog().info("Start removing map notes for project - " + refsetId);
      for (MapProject mapProject : mappingService.getMapProjects()
          .getMapProjects()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      if (mapProjects.isEmpty()) {
        getLog().info("NO PROJECTS FOUND " + refsetId);
        return;
      }

      // Remove map record and entry notes
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      int ct = 0;
      for (MapProject project : mapProjects) {
        for (MapRecord record : mappingService
            .getMapRecordsForMapProject(project.getId()).getMapRecords()) {
          if (record.getMapNotes().size() > 0) {
            final MapRecord record2 =
                mappingService.getMapRecord(record.getId());
            getLog().debug(
                "    Remove map record notes from record - " + record2.getId());
            record2.getMapNotes().clear();
            mappingService.updateMapRecord(record2);
            if (++ct % 2000 == 0) {
              getLog().info("      " + ct + " notes processed");
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
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
