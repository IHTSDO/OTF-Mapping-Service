package org.ihtsdo.otf.mapping.mojo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * QA Routine to Fix Invalid Targets and Apply advices
 * 
 * See admin/qa/pom.xml for a sample execution.
 * 
 * @goal fix-invalid-targets
 * @phase package
 */
public class QAFixInvalidTargets extends AbstractMojo {

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {

    try {

      String refSetId = "447562003";
      Set<String> invalidTargetIds =
          new HashSet<>(Arrays.asList("V29.99", "W46.99", "W54.99", "W57.99",
              "X41.99", "X42.99", "X44.99", "X45.99", "X47.99", "X59.92",
              "X59.98", "Y44.29", "Y83.19", "Y84.29"));
          
      

      ContentService contentService = new ContentServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
    

      
      // retrieve the map projet
      MapProject mapProject = null;
      for (MapProject mp : mappingService.getMapProjects().getMapProjects()) {

       if (mp.getRefSetId().equals(refSetId)) {
          mapProject = mp;
        }
      }
      
      if (mapProject == null)
        throw new MojoFailureException("Could not find map project for ref set id " + refSetId);
      
      // get the algorithm handler
      ProjectSpecificAlgorithmHandler handler = mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      
      // retrieve the map advice place of occurence
      MapAdvice pocAdvice = null;
      for (MapAdvice mapAdvice : mapProject.getMapAdvices()) {
      if (mapAdvice.getDetail().equals("POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE"))
        pocAdvice = mapAdvice;
      }
      
      if (pocAdvice == null)
        throw new Exception("Could not retrieve advice POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE");
      
      for (MapRecord mapRecord : mappingService.getMapRecordsForMapProject(
          mapProject.getId()).getMapRecords()) {

        boolean updateRecord = false;
        for (MapEntry me : mapRecord.getMapEntries()) {
          if (me.getTargetId() != null
              && invalidTargetIds.contains(me.getTargetId())) {
            updateRecord = true;
            getLog().info(
                "Revising entry: " + me.getId() + " with target "
                    + me.getTargetId());

            switch (me.getTargetId()) {
              case "V29.99":
                me.setTargetId("V29.9");
                break;
              case "W46.99":
                me.setTargetId("W46");
                me.addMapAdvice(pocAdvice);
                break;
              case "W54.99":
                me.setTargetId("W54");
                me.addMapAdvice(pocAdvice);
                break;
              case "W57.99":
                me.setTargetId("W57");
                me.addMapAdvice(pocAdvice);
                break;
              case "X41.99":
                me.setTargetId("X41");
                me.addMapAdvice(pocAdvice);
                break;
              case "X42.99":
                me.setTargetId("X42");
                me.addMapAdvice(pocAdvice);
                break;
              case "X44.99":
                me.setTargetId("X44");
                me.addMapAdvice(pocAdvice);
                break;
              case "X45.99":
                me.setTargetId("X45");
                me.addMapAdvice(pocAdvice);
                break;
              case "X47.99":
                me.setTargetId("X47");
                me.addMapAdvice(pocAdvice);
                break;
              case "X59.92":
                me.setTargetId("X59");
                me.addMapAdvice(pocAdvice);
                break;
              case "X59.98":
                me.setTargetId("X59");
                me.addMapAdvice(pocAdvice);
                break;
              case "Y44.29":
                me.setTargetId("Y44.2");
                break;
              case "Y83.19":
                me.setTargetId("Y83.1");
                break;
              case "Y84.29":    
                me.setTargetId("Y84.2");
                break;
              default:
                throw new Exception("Bad target id specified for select statement");
            }
            
            Concept concept = contentService.getConcept(me.getTargetId(), mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
            if (concept == null)
              throw new MojoFailureException("Target code " + me.getTargetId() + " could not be found");
         
            me.setTargetName(concept.getDefaultPreferredName());
          }
         

          if (updateRecord == true) {
            
            // validate the record
            if (handler.validateRecord(mapRecord).isValid() == false) {
              throw new MojoFailureException("Map Record still fails validation checks"); 
            }
            
            mappingService.updateMapRecord(mapRecord);
          }
        }

      }

      mappingService.commit();
      mappingService.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing map group QA failed.", e);
    }

  }
}