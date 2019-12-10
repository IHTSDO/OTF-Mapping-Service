/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which removes map notes for a specified project id.
 * 
 * See admin/remover/pom.xml for a sample execution.
 *
 * @goal remove-map-records
 * 
 * @phase package
 */
public class MapRecordRemoverMojo extends AbstractOtfMappingMojo {

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * The input file.
   *
   * @parameter
   */
  private String inputFile = null;

  /**
   * Instantiates a {@link MapRecordRemoverMojo} from the specified parameters.
   * 
   */
  public MapRecordRemoverMojo() {
    // Do nothing
  }

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting removing map records for project");
    getLog().info("  refsetId = " + refsetId);

    try (final MappingService mappingService = new MappingServiceJpa();) {

      setupBindInfoPackage();

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

      // If input file is set, take map records with matching refset id
      // and remove only those
      final Set<String> toRemove = new HashSet<>();
      if (inputFile != null) {
        if (!new File(inputFile).exists()) {
          throw new Exception("Input file does not exist: " + inputFile);
        }
        try (final BufferedReader in =
            new BufferedReader(new FileReader(new File(inputFile)))) {
          String line = null;
          while ((line = in.readLine()) != null) {
            final String[] tokens = line.split("\t");
            // If matching refsetid, add this concept id
            if (tokens[4].equals(refsetId)) {
              toRemove.add(tokens[5]);
            }
          }
        }
      }

      // Remove map record and entry notes
      int ct = 0;
      for (final MapProject mapProject : mapProjects) {
        getLog().debug("    Remove map records for " + mapProject.getName());
        for (final MapRecord record : mappingService
            .getMapRecordsForMapProject(mapProject.getId()).getMapRecords()) {
          // If either inputFile is null, or the concept id exists, remove it
          if (inputFile == null || toRemove.contains(record.getConceptId())) {
            getLog().info("    Removing map record " + record.getId() + " from "
                + mapProject.getName());
            mappingService.removeMapRecord(record.getId());
            if (++ct % 500 == 0) {
              getLog().info("      " + ct + " records processed");
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
