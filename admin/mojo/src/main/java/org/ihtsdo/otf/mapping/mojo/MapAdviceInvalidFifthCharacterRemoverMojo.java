package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Removes the map advice FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE
 * from records where no fifth character exists
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-map-advice-fifth-character
 * @phase package
 */
public class MapAdviceInvalidFifthCharacterRemoverMojo extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    
    String mapAdviceName = "FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE";
    
    getLog().info("Removing map advice for fifth character requirement where no fifth character exists");
    getLog().info("  mapAdviceName = " + mapAdviceName);
    
    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a ref set id");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();
      MapAdvice mapAdvice = null;
      for (MapAdvice ma : mappingService.getMapAdvices().getIterable()) {
        if (ma.getName().equals(mapAdviceName))
          mapAdvice = ma;
      }
      if (mapAdvice == null)
        throw new MojoExecutionException(
            "The map advice to be removed does not exist");

      getLog().info(
          "Found map advice to remove (id = " + mapAdvice.getId() + ")");

      List<MapProject> mapProjects = new ArrayList<>();
      for (MapProject mapProject : mappingService.getMapProjects().getIterable()) {
        if (mapProject.getRefSetId().equals(refsetId)) 
          mapProjects.add(mapProject);
      }
        
      for (MapProject mapProject : mapProjects) {
        
        int nRemoved = 0;
        
        getLog().info("Checking records for map project " + mapProject.getName());
        
        MapRecordList mapRecords = mappingService.getMapRecordsForMapProject(mapProject.getId());
        
        // cycle over all records
        for (MapRecord mapRecord : mapRecords.getIterable()) {
          
          boolean updateRecord = false;
          
          // cycle over all entries
          for (MapEntry mapEntry : mapRecord.getMapEntries()) {
            
            
            // if this target id matches the ICD10 pattern ANN.NN
            if (mapEntry.getTargetId() != null && 
                mapEntry.getTargetId().matches("[a-zA-Z][0-9][0-9]\\.[0-9][0-9]")) {
           
              // check map advices for advice to remove
              if (mapEntry.getMapAdvices().contains(mapAdvice)) {
                getLog().info("Concept " + mapRecord.getConceptId() + ": Found advice attached to target " + mapEntry.getTargetId());
                updateRecord = true;
                mapEntry.removeMapAdvice(mapAdvice);
              }
            }
          }
          if (updateRecord == true) {
            nRemoved++;
            mappingService.updateMapRecord(mapRecord);
          }
        }
        
        getLog().info(nRemoved + " fifth character advices removed from project.");
      }
     
      mappingService.commit();
      mappingService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Removing fifth character map advice for invalid targets failed", e);
    }

  }
}
