/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * Goal is run after loading CCI in via ClaML3.
 * Adds the Rubric name to all sub-level partial concept names,
 * and adds additional partial codes required by CIHI but not present in the ClaML
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal cci-post-load-fixer
 * 
 * @phase package
 */
public class CCIPostLoadFixerMojo extends AbstractOtfMappingMojo {

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
      getLog().info("Starting CCI post load fixer");
      getLog().info("  terminology = " + terminology);
      getLog().info("  terminologyVersion = " + terminologyVersion);

      setupBindInfoPackage();

      //TESTTEST re-enable this
      fixCCIConceptNames();
      addCCIPartialCodes();

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
   * Also, additional partial concepts are required that aren't present in the ClaML.
   * We will be adding adding partial code carets wherever there are hyphens.
   * For example:
   *    1.VG.53.LA-PM requires a 1.VG.53.LA-^^
   *    1.VG.53.LA-PM-A require a 1.VG.53.LA-PM-^
   * For naming these concepts, take the parent’s name, and add:
   *    For 1.VG.53.^^ - already exists.
   *    For 1.VG.53.LA-^^, then “, unknown agent or device”
   *    For 1.VG.53.LA-PM-^, then “, unknown tissue”
   *        For section 8 codes, use “, unknown type, group, or strain” instead.


   *
   * @throws Exception the exception
   */
  private void fixCCIConceptNames() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    //TODO: Lookup metatdata concepts (isa rel type, module id, etc.)  
    // Currently hard-coded in, but this is hacky.
    
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

  /*
  * @throws Exception the exception
  */
 private void addCCIPartialCodes() throws Exception {
   ContentService contentService = new ContentServiceJpa();
   contentService.setTransactionPerOperation(false);
   contentService.beginTransaction();
   
   // Setup vars
   int objectCt = 0;   
   
   //Setting an arbitrary number to create terminologyIds for new objects.  
   //Setting it high to avoid conflicts with previously created ones.
   int relIdCounter = 100000;
   int descriptionIdCounter = 100000;
   
   // first get isaRelType from metadata
   final MetadataService metadataService = new MetadataServiceJpa();
   final Map<String, String> hierRelTypeMap =
       metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);

   final String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
   metadataService.close();
   
   final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
   
   final String effectiveTime = dt.format(new Date());

   ConceptList concepts =
       contentService.getAllConcepts(terminology, terminologyVersion);
   contentService.clear();

   // Iterate through once to collect all existing terminology ids and their names.
   //Also grab all of the root concepts (those that start with "Section") so we can generate tree positions later
   Map<String,String> existingTerminologyIdstoNames = new HashMap<>();
   List<String> roots = new ArrayList<>();

   for (Concept concept : concepts.getConcepts()) {
     existingTerminologyIdstoNames.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
     
     if(concept.getTerminologyId().startsWith("Section")) {
       roots.add(concept.getTerminologyId());
     }
   }
   
   // Iterate over concepts once, identifying places where a new partial code is required.
   // Examples:
   // 1.VG.53.LA-PM requires a 1.VG.53.LA-^^
   // 1.VG.53.LA-PM-A require a 1.VG.53.LA-PM-^
   //

   Set<String> newConceptTerminologyIds = new HashSet<>();
   Map<String,String> newConceptToNameMap = new HashMap<>();
   Map<String,Long> newConceptToParentIdMap = new HashMap<>();

   Pattern pattern = Pattern.compile(".*-[A-Z][A-Z]$");
   Pattern pattern2 = Pattern.compile(".*-[A-Z]$");   
   
   for (Concept concept : concepts.getConcepts()) {
     // Check if needs to generate a -^^ partial code
     if(pattern.matcher(concept.getTerminologyId()).find()) {
       //Calculate the new concept's terminologyId
       final String newConceptTerminologyId = concept.getTerminologyId().substring(0, concept.getTerminologyId().length()-3) + "-^^";
       
       //Only set up each new concept once
       if(!newConceptTerminologyIds.contains(newConceptTerminologyId)) {
         newConceptTerminologyIds.add(newConceptTerminologyId);
       }
       else {
         continue;
       }
       
       //Identify the concept's parent, and link the new concept terminologyId to it
       List<Concept> parents = TerminologyUtility.getActiveParents(contentService.getConcept(concept.getId()));
       if(parents.size() != 1) {
         getLog().error("Parent for concept " + concept.getTerminologyId() + " cannot be found. Skipping.");
         continue;
       }
       final Concept parentConcept = parents.get(0);
       newConceptToParentIdMap.put(newConceptTerminologyId, parentConcept.getId());
       
       // Check if a direct predecessor exists (e.g. for 1.VG.53.LA-PM, the predecessor would be 1.VG.53.LA)
       // If so, use its name to generate the new partial-code concept's name
       final String predecessorTerminologyId = concept.getTerminologyId().substring(0, concept.getTerminologyId().length()-3);
       if(existingTerminologyIdstoNames.keySet().contains(predecessorTerminologyId)) {
         String newConceptName = calculatePartialCodeName(newConceptTerminologyId, existingTerminologyIdstoNames.get(predecessorTerminologyId));
         newConceptToNameMap.put(newConceptTerminologyId, newConceptName);
       }
       // If no predecessor exists, use the concept's parent instead.
       else {
         String newConceptName = calculatePartialCodeName(newConceptTerminologyId, parentConcept.getDefaultPreferredName());
         newConceptToNameMap.put(newConceptTerminologyId, newConceptName);
       }
     }

     // Check if needs to generate a -^ partial code
     else if(pattern2.matcher(concept.getTerminologyId()).find()) {
       //Calculate the new concept's terminologyId
       final String newConceptTerminologyId = concept.getTerminologyId().substring(0, concept.getTerminologyId().length()-2) + "-^";
       
       //Only set up each new concept once
       if(!newConceptTerminologyIds.contains(newConceptTerminologyId)) {
         newConceptTerminologyIds.add(newConceptTerminologyId);
       }
       else {
         continue;
       }

       //Identify the concept's parent, and link the new concept terminologyId to it
       List<Concept> parents = TerminologyUtility.getActiveParents(contentService.getConcept(concept.getId()));
       if(parents.size() != 1) {
         getLog().error("Parent for concept " + concept.getTerminologyId() + " cannot be found. Skipping.");
         continue;
       }
       final Concept parentConcept = parents.get(0);
       newConceptToParentIdMap.put(newConceptTerminologyId, parentConcept.getId());
       
       // Check if a direct predecessor exists (e.g. for 1.VG.53.LA-PM-A, the predecessor would be 1.VG.53.LA-PM)
       // If so, use its name to generate the new partial-code concept's name
       final String predecessorTerminologyId = concept.getTerminologyId().substring(0, concept.getTerminologyId().length()-2);
       if(existingTerminologyIdstoNames.keySet().contains(predecessorTerminologyId)) {
         String newConceptName = calculatePartialCodeName(newConceptTerminologyId, existingTerminologyIdstoNames.get(predecessorTerminologyId));
         newConceptToNameMap.put(newConceptTerminologyId, newConceptName);
       }
       // If no predecessor exists, use the concept's parent instead.
       else {
         String newConceptName = calculatePartialCodeName(newConceptTerminologyId, parentConcept.getDefaultPreferredName());
         newConceptToNameMap.put(newConceptTerminologyId, newConceptName);
       }
     }
   }
   
   //Now that we've identified the new partial-code concepts, their parents, and their names, 
   // go through and construct the required objects (concept, description, relationship).
   
   for(final String newConceptTerminologyId : newConceptTerminologyIds) {    
     //Create the new concept
     Concept newConcept = new ConceptJpa();
     newConcept.setTerminologyId(newConceptTerminologyId);
     newConcept.setTerminology(terminology);
     newConcept.setTerminologyVersion(terminologyVersion);
     newConcept.setEffectiveTime(dt.parse(effectiveTime));
     newConcept.setDefaultPreferredName(newConceptToNameMap.get(newConceptTerminologyId));
     newConcept.setActive(true);
     newConcept.setDefinitionStatusId(1L);
     newConcept.setModuleId(2L);
          
     // Create a preferred name description
     final Description desc = new DescriptionJpa();
     desc.setTerminologyId(descriptionIdCounter++ + "");
     desc.setEffectiveTime(dt.parse(effectiveTime));
     desc.setActive(true);
     desc.setModuleId(2L);
     desc.setTerminology(terminology);
     desc.setTerminologyVersion(terminologyVersion);
     desc.setTerm(newConceptToNameMap.get(newConceptTerminologyId));
     desc.setConcept(newConcept);
     desc.setCaseSignificanceId(3L);
     desc.setLanguageCode("en");
     desc.setTypeId(4L);

     newConcept.addDescription(desc);
               
     //Create a relationship connecting the concept to its parent
     final Relationship relationship = new RelationshipJpa();
     relationship.setTerminologyId(relIdCounter++ + "");
     relationship.setEffectiveTime(dt.parse(effectiveTime));
     relationship.setActive(true);
     relationship.setModuleId(2L);
     relationship.setTerminology(terminology);
     relationship.setTerminologyVersion(terminologyVersion);
     relationship.setCharacteristicTypeId(6L);
     relationship.setModifierId(5L);
     relationship.setDestinationConcept(contentService.getConcept(newConceptToParentIdMap.get(newConceptTerminologyId)));
     relationship.setSourceConcept(newConcept);
     relationship.setTypeId(Long.parseLong(isaRelType));
     final Set<Relationship> rels = new HashSet<>();
     rels.add(relationship);
     newConcept.setRelationships(rels);
     
     newConcept = contentService.addConcept(newConcept);
     
     // periodically commit
     if (++objectCt % 5000 == 0) {
       getLog().info("    count = " + objectCt);
       contentService.commit();
       contentService.clear();
       contentService.beginTransaction();
     }
   }

   contentService.commit();
   contentService.clear();
   contentService.beginTransaction();


   //Now since we added a bunch of new par/chd relationships between concepts, we
   //need to remove and regenerate the tree positions
   
   contentService.clearTreePositions(terminology, terminologyVersion);
   contentService.commit();
   contentService.clear();
   
   for (final String root : roots) {
     getLog().info("Start creating tree positions " + root + ", " + isaRelType);
     contentService.computeTreePositions(terminology, terminologyVersion, isaRelType, root);
   }
   
   contentService.close();
 }
 
 private String calculatePartialCodeName(String newConceptTerminologyId, String predecessorConceptName) throws Exception{
   
   if(newConceptTerminologyId.length() == 13) {
     return predecessorConceptName + ", unknown agent or device";
   }
   else if (newConceptTerminologyId.length() == 15) {
     //Section 8 lowest-level concepts are a special case
     if(predecessorConceptName.startsWith("8")) {
       return predecessorConceptName + ", unknown type, group, or strain";
     }
     //All other sections use the same pattern
     else {
       return predecessorConceptName + ", unknown tissue";
     }       
   }
   // Shouldn't ever get here
   else {
     return "";
   }
 }
  
  
}