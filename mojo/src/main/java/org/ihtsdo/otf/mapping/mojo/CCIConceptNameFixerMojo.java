/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Goal adds the Rubric name to all sub-level partial concept names.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal cci-concept-name-fixer
 * 
 * @phase package
 */
public class CCIConceptNameFixerMojo extends AbstractOtfMappingMojo {

  /**
   * Name of terminology.
   * 
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * The terminology version.
   * 
   * @parameter
   * @required
   */
  private String terminologyVersion;

  /** the defaultPreferredNames type id. */
  private Long dpnTypeId = 4L;
  
  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {

    try {
      getLog().info("Starting CCI concept name fixer");
      getLog().info("  terminology = " + terminology);
      getLog().info("  terminologyVersion = " + terminologyVersion);

      setupBindInfoPackage();

      fixCCIConceptNames();

      getLog().info("Done...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

  /**
   * When CCI is loaded in via ClaML loader, the lower-level concepts are assigned partial names,
   * and they need a higher-level concept name added as a prefix.
   * For example: concept "1.VG.53.LA-SL-N" has name: "with synthetic material (e.g. bone paste, cement) cement spacer [temporary] [impregnated with antibiotics]"
   * It's Rubric ancestor "1.VG.53.^^" has name: "Implantation of internal device, knee joint"
   * Update "1.VG.53.LA-SL-N" to have name: "Implantation of internal device, knee joint with synthetic material (e.g. bone paste, cement) cement spacer [temporary] [impregnated with antibiotics]"
   *
   * @throws Exception the exception
   */
  private void fixCCIConceptNames() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // Setup vars
    int objectCt = 0;
    
    ConceptList concepts =
        contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate over concepts once, getting all Rubric names
    // Rubric concepts have 8 characters, followed by wildcard characters
    // For example: 1.VL.87.^^, Excision partial, cruciate ligaments of knee
    Map<String,String> rubricIdToName = new HashMap<>();
    for(Concept concept : concepts.getConcepts()) {
      if(concept.getTerminologyId().length() >= 10 && concept.getTerminologyId().substring(8, 10).equals("^^")) {
        rubricIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
      }
    }
    
    // Iterate over concepts again, updating sub-rubric concept names by pre-pending rubric names.
    for (Concept concept2 : concepts.getConcepts()) {

      Concept concept = contentService.getConcept(concept2.getId());

      // Skip if inactive
      if (!concept.isActive()) {
        continue;
      }

      // Only look at sub-rubric concepts (concepts with "." in their terminology Id, but no "^^" wildcard characters
      if(!concept.getTerminologyId().contains(".") || concept.getTerminologyId().contains("^^")) {
        continue;
      }
      
      // Now that we've skipped all non-rubric concepts, update the default preferred name description, and the concept itself.
      // Calculate the concept's rubric ancestor, which is the first 8 characters of the current concept, followed by "^^".
      final String conceptRubricId = concept.getTerminologyId().substring(0, 8).concat("^^");
      
      getLog().debug("  Concept " + concept.getTerminologyId() + ", rubric ancestor " + conceptRubricId);

      String updatedTerm = null;
      // Iterate over descriptions
       for (Description description : concept.getDescriptions()) {

        // If active and preferred type
        if (description.isActive()
            && description.getTypeId().equals(dpnTypeId)) {

          updatedTerm = rubricIdToName.get(conceptRubricId) + " " + description.getTerm();
          
          // If combined term is too long, log it for reference, then truncate it so it will fit in the database
          if (updatedTerm.length() >= 4000) {
            getLog().warn("Concept " + concept.getTerminologyId() + " name too long - truncating");
            updatedTerm = updatedTerm.substring(0, 4000);
          }
          
          description.setTerm(updatedTerm);
                    
          contentService.updateDescription(description);
          break;
          }
        }
       
       // If the description name was updated, update the concept name accordingly
       if(updatedTerm != null) {
         concept.setDefaultPreferredName(updatedTerm);
         contentService.updateConcept(concept);
       }

      // periodically commit
      if (++objectCt % 5000 == 0) {
        getLog().info("    count = " + objectCt);
        contentService.commit();
        contentService.clear();
        contentService.beginTransaction();
      }
    }

    contentService.commit();
    contentService.close();

  }

}
