/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal is run after loading CCI in via ClaML3. Adds the Rubric name to all
 * sub-level partial concept names, and adds additional partial codes required
 * by CIHI but not present in the ClaML
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal cci-post-load-fixer
 * 
 * @phase package
 */
public class CCIPostLoadFixerMojo extends AbstractOtfMappingMojo {

  private Map<String, Set<String>> sevenCharSets = new HashMap<>();
  
  private Map<String, Set<String>> tenCharSets = new HashMap<>();

  private Map<String, Set<String>> thirteenCharSets = new HashMap<>();

  private Map<String, String> terminologyIdToName = new HashMap<>();
  
  private Set<String> newConceptTerminologyIds = new HashSet<>();
  
  private String isaRelType = "";

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

      cacheConceptNames();
      identifyPartialConceptsAndCalculateNames();
      addCCIPartialConcepts();
      prependRubricNameToChildrenConcepts();
      regenerateTreePositions();

      getLog().info("Done...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

  /**
   * When CCI is loaded in via ClaML loader, the lower-level concepts are
   * assigned partial names, and they need a higher-level concept name added as
   * a prefix. For example: concept "1.VG.53.LA-SL-N" has name: "with synthetic
   * material (e.g. bone paste, cement) cement spacer [temporary] [impregnated
   * with antibiotics]" It's Rubric ancestor "1.VG.53.^^" has name:
   * "Implantation of internal device, knee joint" Update "1.VG.53.LA-SL-N" to
   * have name: "Implantation of internal device, knee joint with synthetic
   * material (e.g. bone paste, cement) cement spacer [temporary] [impregnated
   * with antibiotics]"
   *
   * Also, additional partial concepts are required that aren't present in the
   * ClaML. We will be adding adding partial code carets wherever there are
   * hyphens. For example: 1.VG.53.LA-PM requires a 1.VG.53.LA-^^
   * 1.VG.53.LA-PM-A require a 1.VG.53.LA-PM-^ For naming these concepts, take
   * the parent’s name, and add: For 1.VG.53.^^ - already exists. For
   * 1.VG.53.LA-^^, then “, unknown agent or device” For 1.VG.53.LA-PM-^, then
   * “, unknown tissue” For section 8 codes, use “, unknown type, group, or
   * strain” instead.
   *
   * 
   * 
   * @throws Exception the exception
   */
  private void cacheConceptNames() throws Exception {
    ContentService contentService = new ContentServiceJpa();

    ConceptList concepts = contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate over concepts, adding terminology ids and names to respective
    // caches

    for (Concept concept : concepts.getConcepts()) {
      // Skip the metadata concepts and grouper "1AA-1ZZ" concepts
      if (concept.getTerminologyId().contains(".")) {

        terminologyIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());

        if (concept.getTerminologyId().length() >= 7) {
          String tenCharSubstring = concept.getTerminologyId().substring(0, 7);
          if (sevenCharSets.get(tenCharSubstring) == null) {
            sevenCharSets.put(tenCharSubstring, new HashSet<>());
          }
          Set<String> terminologyIds = sevenCharSets.get(tenCharSubstring);
          terminologyIds.add(concept.getTerminologyId());
          sevenCharSets.put(tenCharSubstring, terminologyIds);
        }
        
        if (concept.getTerminologyId().length() >= 10) {
          String tenCharSubstring = concept.getTerminologyId().substring(0, 10);
          if (tenCharSets.get(tenCharSubstring) == null) {
            tenCharSets.put(tenCharSubstring, new HashSet<>());
          }
          Set<String> terminologyIds = tenCharSets.get(tenCharSubstring);
          terminologyIds.add(concept.getTerminologyId());
          tenCharSets.put(tenCharSubstring, terminologyIds);
        }

        if (concept.getTerminologyId().length() >= 13) {
          String thirteenCharSubstring = concept.getTerminologyId().substring(0, 13);
          if (thirteenCharSets.get(thirteenCharSubstring) == null) {
            thirteenCharSets.put(thirteenCharSubstring, new HashSet<>());
          }
          Set<String> terminologyIds = thirteenCharSets.get(thirteenCharSubstring);
          terminologyIds.add(concept.getTerminologyId());
          thirteenCharSets.put(thirteenCharSubstring, terminologyIds);
        }
      }
    }
    
    // Also grab any names that have been added to the manual-override document
    final Properties config = ConfigUtility.getConfigProperties();
    final String dataDir = config.getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }
    
    // Check preconditions
    if (!new File(dataDir + "/" + terminology + "/" + terminologyVersion + "/" + "partialNameOverride.txt").exists()) {
      throw new Exception(
          "Specified input file missing: " + dataDir + "/" + terminology + "/" + terminologyVersion + "/" + "partialNameOverride.txt");
    }

    // Open reader and service
    BufferedReader partialNameOverrideReader =
        new BufferedReader(new FileReader(new File(dataDir + "/" + terminology + "/" + terminologyVersion + "/" + "partialNameOverride.txt")));

    String line = null;

    while ((line = partialNameOverrideReader.readLine()) != null) {
      String tokens[] = line.split("\t");
      final String terminologyId = tokens[0].trim();
      final String name = tokens[1].trim();
      // If this is concept that is not contained in the base ClaML, add to new concept Id list
      if(!terminologyIdToName.containsKey(terminologyId)) {
        newConceptTerminologyIds.add(terminologyId);
      }
      // Add/override name specified in file, so it is used over name specified in CLaML, or calculated by this algorithm
      terminologyIdToName.put(terminologyId, name);
    }

    partialNameOverrideReader.close();

    contentService.close();
  }

  /**
   *
   * @throws Exception the exception
   */
  private void prependRubricNameToChildrenConcepts() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // TODO: Lookup metatdata concepts (module id, definition status id, etc.)
    // Currently hard-coded in, but this is hacky.

    // Setup vars
    int objectCt = 0;

    ConceptList concepts = contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate over concepts once, getting all Rubric names
    // Rubric concepts have 8 characters, followed by wildcard characters
    // For example: 1.VL.87.^^, Excision partial, cruciate ligaments of knee

    Map<String, String> rubricIdToName = new HashMap<>();
    for (Concept concept : concepts.getConcepts()) {
      if (concept.getTerminologyId().length() >= 10
          && concept.getTerminologyId().substring(8, 10).equals("^^")) {
        rubricIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
      }
    }

    // Iterate over concepts again, updating sub-rubric concept names by
    // pre-pending rubric names.
    for (Concept concept2 : concepts.getConcepts()) {

      Concept concept = contentService.getConcept(concept2.getId());

      // Skip if inactive
      if (!concept.isActive()) {
        continue;
      }

      // Only look at sub-rubric concepts (concepts with "." in their
      // terminology Id, that don't have "^^" wildcards in the 9th and 10th positions
      if (!concept.getTerminologyId().contains(".") || concept.getTerminologyId().substring(8, 10).equals("^^")) {
        continue;
      }

      // Now that we've skipped all non-rubric concepts, update the default
      // preferred name description, and the concept itself.
      // Calculate the concept's rubric ancestor, which is the first 8
      // characters of the current concept, followed by "^^".
      final String conceptRubricId = getRubricId(concept.getTerminologyId());

      getLog().debug(
          "  Concept " + concept.getTerminologyId() + ", rubric ancestor " + conceptRubricId);

      String updatedTerm = null;
      // Iterate over descriptions
      for (Description description : concept.getDescriptions()) {

        // If active and preferred type
        if (description.isActive() && description.getTypeId().equals(dpnTypeId)) {

          updatedTerm = rubricIdToName.get(conceptRubricId) + " " + description.getTerm();

          // If combined term is too long, log it for reference, then truncate
          // it so it will fit in the database
          if (updatedTerm.length() >= 4000) {
            getLog().warn("Concept " + concept.getTerminologyId() + " name too long - truncating");
            updatedTerm = updatedTerm.substring(0, 4000);
          }

          description.setTerm(updatedTerm);

          contentService.updateDescription(description);
          break;
        }
      }

      // If the description name was updated, update the concept name
      // accordingly
      if (updatedTerm != null) {
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
  private void addCCIPartialConcepts() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // Setup vars
    int objectCt = 0;

    // Setting an arbitrary number to create terminologyIds for new objects.
    // Setting it high to avoid conflicts with previously created ones.
    int relIdCounter = 100000;
    int descriptionIdCounter = 100000;

    // get isaRelType from metadata, if it hasn't been determined already
    if(isaRelType.isEmpty()) {
    final MetadataService metadataService = new MetadataServiceJpa();
    final Map<String, String> hierRelTypeMap =
        metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);

    isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
    metadataService.close();
    }

    final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");

    final String effectiveTime = dt.format(new Date());

    ConceptList concepts = contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate through to collect all existing terminology ids and their
    // Concept ids (this is to aid more efficient concept lookups later).
    Map<String, Long> existingTerminologyIdstoConceptIds = new HashMap<>();

    for (Concept concept : concepts.getConcepts()) {
      existingTerminologyIdstoConceptIds.put(concept.getTerminologyId(),
          concept.getId());
    }

    // Now go through and construct the required objects (concept, description,
    // relationship) for the new partial concepts.

    for (final String newConceptTerminologyId : newConceptTerminologyIds) {
      // Create the new concept
      Concept newConcept = new ConceptJpa();
      newConcept.setTerminologyId(newConceptTerminologyId);
      newConcept.setTerminology(terminology);
      newConcept.setTerminologyVersion(terminologyVersion);
      newConcept.setEffectiveTime(dt.parse(effectiveTime));
      newConcept.setDefaultPreferredName(terminologyIdToName.get(newConceptTerminologyId));
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
      desc.setTerm(terminologyIdToName.get(newConceptTerminologyId));
      desc.setConcept(newConcept);
      desc.setCaseSignificanceId(3L);
      desc.setLanguageCode("en");
      desc.setTypeId(4L);

      newConcept.addDescription(desc);

      // Create a relationship connecting the concept to its Rubric parent
      final Relationship relationship = new RelationshipJpa();
      relationship.setTerminologyId(relIdCounter++ + "");
      relationship.setEffectiveTime(dt.parse(effectiveTime));
      relationship.setActive(true);
      relationship.setModuleId(2L);
      relationship.setTerminology(terminology);
      relationship.setTerminologyVersion(terminologyVersion);
      relationship.setCharacteristicTypeId(6L);
      relationship.setModifierId(5L);
      relationship.setDestinationConcept(
          contentService.getConcept(existingTerminologyIdstoConceptIds.get(getRubricId(newConceptTerminologyId))));
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
    contentService.close();
  }
  
  // Now since we added a bunch of new par/chd relationships between concepts,
  // we need to remove and regenerate the tree positions
  private void regenerateTreePositions() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // get isaRelType from metadata, if it hasn't been determined already
    if(isaRelType.isEmpty()) {
    final MetadataService metadataService = new MetadataServiceJpa();
    final Map<String, String> hierRelTypeMap =
        metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);

    isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
    metadataService.close();
    }

    ConceptList concepts = contentService.getAllConcepts(terminology, terminologyVersion);
    
    // Grab all of the root concepts (those that start with "Section") so
    // we can generate tree positions based on them
    List<String> roots = new ArrayList<>();

    for (Concept concept : concepts.getConcepts()) {
      if (concept.getTerminologyId().startsWith("Section")) {
        roots.add(concept.getTerminologyId());
      }
    }

    contentService.clearTreePositions(terminology, terminologyVersion);
    contentService.commit();
    contentService.clear();

    for (final String root : roots) {
      getLog().info("Start creating tree positions " + root + ", " + isaRelType);
      contentService.computeTreePositions(terminology, terminologyVersion, isaRelType, root);
    }

    contentService.close();
  }

  /*
   * @throws Exception the exception
   */
  private void identifyPartialConceptsAndCalculateNames() throws Exception {
    ContentService contentService = new ContentServiceJpa();

    ConceptList concepts = contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate over concepts once, identifying places where a new partial code
    // is required.
    // Examples:
    // 1.VG.53.LA-PM requires a 1.VG.53.LA-^^
    // 1.VG.53.LA-PM-A require a 1.VG.53.LA-PM-^ and a 1.VG.53.LA-^^
    // Determine if we can calculate the partial-code name
    //

    for (Concept concept : concepts.getConcepts()) {
      // A -XX or -XX-X terminology id is encountered. Grab all of the
      // terminology ids
      // that share same first 10 characters as it
      // For example, if "1.DA.87.LA-AG" or "1.DA.87.LA-AG-A" is encountered,
      // grab all concepts that start with "1.DA.87.LA"
      if (concept.getTerminologyId().length() == 13 || concept.getTerminologyId().length() == 15) {

        String partialCodeId = concept.getTerminologyId().substring(0, 10) + "-^^";

        // If a code exists that is the partial code minus the "-^^", then the
        // partial is redundant and not needed.
        // For example: "1.NA.56.FA" exists, so we should not create
        // "1.NA.56.FA-^^"
        if (terminologyIdToName.containsKey(concept.getTerminologyId().substring(0, 10))) {
          continue;
        }

        // Only continue if a name has not already been calculated for this
        // partial
        if (terminologyIdToName.get(partialCodeId) == null) {
          
          // If there is only a single code in the rubric, we should not create
          // a partial
          // For example:
          //
          // 1.EB.03.^^ Immobilization, zygoma
          // 1.EB.03.JA-SR using external splinting device
          Set<String> sevenCharIds = sevenCharSets.get(concept.getTerminologyId().substring(0, 7));
          if (sevenCharIds.size() == 2) {
            continue;
          }   
          
          Set<String> tenCharIds = tenCharSets.get(concept.getTerminologyId().substring(0, 10));
          Set<String> tenCharNames = new HashSet<>();
          for (String tenCharId : tenCharIds) {
            tenCharNames.add(terminologyIdToName.get(tenCharId));
          }

          // Now that we've gathered all of the concepts that share the first 10
          // characters,
          // try to identify the shared part of the name, so we can use that to
          // assign the wildcard version

          String sharedName = "";
          String phraseFound = "";

          // "LA" codes are a special case.
          // When 9th-10th characters are "LA", pattern should be "open approach,
          // unknown agent or device"
          // E.g.:
          // 1.EC.55.LA-KD of wire or mesh using open approach
          // 1.EC.55.LA-NW of plate/screw using open approach
          // 1.EC.55.LA-TP of tissue expander using open approach
          // 1.EC.55.LA-^^ open approach, unknown agent or device

          if (concept.getTerminologyId().substring(8, 10).equals("LA")) {
            sharedName = "open approach of";
          }

          else {
            // If there is more than one name, find common substring shared
            // across all names.
            // Look from the front and the back
            if (tenCharNames.size() > 1) {
              for (String name : tenCharNames) {
                if (sharedName.equals("")) {
                  sharedName = name;
                  continue;
                } else {
                  sharedName = name.substring(0, StringUtils.indexOfDifference(sharedName, name));
                }
              }
            }

            // Also look for certain phrases
            for (String name : tenCharNames) {
              int usingIndex = name.indexOf("using");
//              int ofIndex = name.indexOf("of");
//              int withIndex = name.indexOf("with");
//              int endoscopicIndex = name.indexOf("endosopic");
//              int perIndex = name.indexOf("per");
//              int noIndex = name.indexOf("no");
              int approachIndex = name.indexOf("approach");
              int techniqueIndex = name.indexOf("technique");
              int injectionIndex = name.indexOf("injection");
              int infusionIndex = name.indexOf("infusion");
              int openIndex = name.indexOf("open");
//              int deviceIndex = name.indexOf("device");
//              int agentIndex = name.indexOf("agent");
//              int fixationIndex = name.indexOf("fixation");
//              int implantIndex = name.indexOf("implant");
//              int wireIndex = name.indexOf("wire");
//              int plateIndex = name.indexOf("plate");
//              int tissueIndex = name.indexOf("tissue");
//              int usedIndex = name.indexOf("used");
//              int usingWireIndex = name.indexOf("using wire");
//              int syntheticTissueIndex = name.indexOf("synthetic tissue");
//              int combinedSourcesIndex = name.indexOf("combined sources");
//              int deviceAloneIndex = name.indexOf("device alone");
//              int uncementedIndex = name.indexOf("uncemented");

              if (usingIndex != -1) {
                // using ... approach
                if (approachIndex != -1 && approachIndex > usingIndex) {
                  phraseFound = name.substring(usingIndex, approachIndex + 8);
                  break;
                }
                // using ... technique
                else if (techniqueIndex != -1 && techniqueIndex > usingIndex) {
                  phraseFound = name.substring(usingIndex, techniqueIndex + 9);
                  break;
                }
                // using ... injection
                else if (injectionIndex != -1 && injectionIndex > usingIndex) {
                  phraseFound = name.substring(usingIndex, injectionIndex + 9);
                  break;
                }
                // using ... infusion
                else if (infusionIndex != -1 && infusionIndex > usingIndex) {
                  phraseFound = name.substring(usingIndex, infusionIndex + 8);
                  break;
                }
              }

              else if (openIndex != -1) {
                // open ... approach
                if (approachIndex != -1 && approachIndex > openIndex) {
                  phraseFound = name.substring(openIndex, approachIndex + 8);
                  break;
                }
              }
            }
          }

          // Compare the sharedName vs the phraseFound, and keep the longer of
          // the two.
          String bestMatch = "";
          if (sharedName.length() > phraseFound.length()) {
            bestMatch = sharedName;
          } else {
            bestMatch = phraseFound;
          }

          String partialCodeName = "";

          // Now we have the best match. Tack an "unknown" suffix to the end to
          // create the partial code name.
          if (bestMatch.equals("")) {
            partialCodeName = "UNCALCULATABLE";
          } else {
            bestMatch = bestMatch.trim();
            String[] joiners = {
                "of", "with", "using", "and", "by"
            };
            if (!StringUtils.endsWithAny(bestMatch, joiners)) {
              bestMatch = bestMatch + ",";
            }

            // Section 6 lowest-level concepts:
            if (partialCodeId.startsWith("6")) {
              partialCodeName = bestMatch + " unknown method or tool";
            }
            // All other sections use the same pattern
            else {
            partialCodeName = bestMatch + " unknown agent or device";
            }
          }

          // Add the code and name to the name map
          terminologyIdToName.put(partialCodeId, partialCodeName);
          newConceptTerminologyIds.add(partialCodeId);
        }
      }
    }

    for (Concept concept : concepts.getConcepts()) {

      // A -XX-X terminology id is encountered. Grab all of the terminology ids
      // that share same first 13 characters as it
      // For example, if "1.DA.87.LA-AG-A" is encountered, grab all concepts
      // that
      // start with "1.DA.87.LA-AG"
      if (concept.getTerminologyId().length() == 15) {

        String partialCodeId = concept.getTerminologyId().substring(0, 13) + "-^";

        // If a code exists that is the partial code minus the "-^", then the
        // partial is redundant and not needed.
        // For example: "1.EA.87.LA-NW" exists, so we should not create
        // "1.EA.87.LA-NW-^"
        if (terminologyIdToName.containsKey(concept.getTerminologyId().substring(0, 13))) {
          continue;
        }

        // Only continue if a name has not already been calculated for this
        // partial
        if (terminologyIdToName.get(partialCodeId) == null) {
          
          // If there is only a single code in the rubric, we should not create
          // a partial
          // For example:
          //
          // 8.MS.70.^^ Immunization (to prevent) haemophilus influenza and hepatitis
          // 8.MS.70.HA-BV-B    by intramuscular [IM] injection of bacterial polysaccharide and inactivated viral antigen type B (Hib HEP B)
          Set<String> sevenCharIds = sevenCharSets.get(concept.getTerminologyId().substring(0, 7));
          if (sevenCharIds.size() == 2) {
            continue;
          }  
          
          Set<String> thirteenCharIds =
              thirteenCharSets.get(concept.getTerminologyId().substring(0, 13));
          Set<String> thirteenCharNames = new HashSet<>();
          for (String thirteenCharId : thirteenCharIds) {
            thirteenCharNames.add(terminologyIdToName.get(thirteenCharId));
          }

          // Now that we've gathered all of the concepts that share the first 13
          // characters,
          // try to identify the shared part of the name, so we can use that to
          // assign the wildcard version

          String sharedName = "";
          String prefixSharedName = "";
          String suffixSharedName = "";

          // "LA" codes are a special case.
          // When 9-10'th characters are "LA", pattern should be "Open approach
          // " + shared name starting from end of string + "unknown tissue."
          // E.g.:
          // 1.EC.80.LA-KD no tissue used for repair [reshaping only] using wire
          // 1.EC.80.LA-KD-A autograft [e.g. bone] using wire
          // 1.EC.80.LA-KD-N synthetic tissue[Silastic sheath edging] using wire
          // 1.EC.80.LA-KD-Q combined sources of tissue [bone and Silastic
          // sheath edging] using wire
          // 1.EC.80.LA-KD-^ Open approach, using wire, unknown tissue

          if (concept.getTerminologyId().substring(8, 10).equals("LA")) {
            // LA-XX are a special subcase, and will always be set to "open
            // approach using"
            if (concept.getTerminologyId().substring(8, 13).equals("LA-XX")) {
              sharedName = "open approach using";
            } else {
              if (thirteenCharNames.size() > 1) {
                boolean firstName = true;
                for (String name : thirteenCharNames) {
                  if (firstName) {
                    sharedName = name;
                    firstName = false;
                    continue;
                  }
                  // If a previous loop identified that there is no common
                  // string, stop checking
                  if (sharedName.length() == 0) {
                    break;
                  }

                  // Check each character, starting from the end going
                  // backwards,
                  // until a difference is found
                  else {
                    int sharedNameLength = sharedName.length();
                    int nameLength = name.length();
                    for (int i = 0; i <= Math.min(sharedNameLength, nameLength); i++) {
                      // When the first difference is found, set sharedName to
                      // be
                      // the shared portion of the string
                      if (!sharedName.substring(sharedNameLength - i, sharedNameLength)
                          .equals(name.substring(nameLength - i, nameLength))) {
                        sharedName =
                            sharedName.substring(sharedNameLength - (i - 1), sharedNameLength);
                        break;
                      }
                    }
                  }
                }
              }

              // If a shared name was identified, prepend with "open approach"
              if (!sharedName.equals("")) {
                sharedName = "open approach, " + sharedName.trim();
              }
            }
          }

          else {
            // If there is more than one name, find common substring shared
            // across all names.
            if (thirteenCharNames.size() > 1) {
              // Find the longest common string at the beginning
              for (String name : thirteenCharNames) {
                if (prefixSharedName.equals("")) {
                  prefixSharedName = name;
                  continue;
                } else {
                  prefixSharedName =
                      name.substring(0, StringUtils.indexOfDifference(prefixSharedName, name));
                }
              }
              // Find the longest common string at the end
              boolean firstName = true;
              for (String name : thirteenCharNames) {
                if (firstName) {
                  suffixSharedName = name;
                  firstName = false;
                  continue;
                }
                // If a previous loop identified that there is no common
                // string, stop checking
                if (suffixSharedName.length() == 0) {
                  break;
                }

                // Check each character, starting from the end going
                // backwards,
                // until a difference is found
                else {
                  int sharedNameLength = suffixSharedName.length();
                  int nameLength = name.length();
                  for (int i = 0; i <= Math.min(sharedNameLength, nameLength); i++) {
                    // When the first difference is found, set sharedName to
                    // be the shared portion of the string
                    if (!suffixSharedName.substring(sharedNameLength - i, sharedNameLength)
                        .equals(name.substring(nameLength - i, nameLength))) {
                      suffixSharedName =
                          suffixSharedName.substring(sharedNameLength - (i - 1), sharedNameLength);
                      break;
                    }
                  }
                }
              }
            }
          }

          String partialCodeName = "";

          // Now we have the shared-name. Tack an "unknown" suffix to the end to
          // create the partial code name.
          if (sharedName.equals("") && prefixSharedName.equals("") && suffixSharedName.equals("")) {
            partialCodeName = "UNCALCULATABLE";
          } else {
            if (!sharedName.equals("")) {
              sharedName = sharedName.trim();
              String[] joiners = {
                  "of", "with", "using", "and", "by"
              };
              if (!StringUtils.endsWithAny(sharedName, joiners)) {
                sharedName = sharedName + ",";
              }

              // Section 8 lowest-level concepts:
              if (partialCodeId.startsWith("8")) {
                partialCodeName = sharedName + " unknown type, group, or strain";
              }
              // All other sections use the same pattern
              else {
                partialCodeName = sharedName + " unknown tissue";
              }
            } else {
              if (!prefixSharedName.equals("")) {
                prefixSharedName = prefixSharedName.trim();
                String[] joiners = {
                    "of", "with", "using", "and", "by"
                };
                if (!StringUtils.endsWithAny(prefixSharedName, joiners)) {
                  prefixSharedName = prefixSharedName + ",";
                }

                // Section 8 lowest-level concepts:
                if (partialCodeId.startsWith("8")) {
                  partialCodeName = prefixSharedName + " unknown type, group, or strain";
                }
                // All other sections use the same pattern
                else {
                  partialCodeName = prefixSharedName + " unknown tissue";
                }
              }
              if (!suffixSharedName.equals("")) {
                //Cleanup
                if(suffixSharedName.startsWith("]") || suffixSharedName.startsWith(",")) {
                  suffixSharedName = suffixSharedName.substring(1);
                }
                
                suffixSharedName = suffixSharedName.trim();
                
                // If no name has been constructed so far, add unknown prefix
                if (partialCodeName.equals("")) {
                  // Section 8 lowest-level concepts:
                  if (partialCodeId.startsWith("8")) {
                    partialCodeName = "unknown type, group, or strain";
                  }
                  // All other sections use the same pattern
                  else {
                    partialCodeName = "unknown tissue";
                  }
                }
                // Now tack on the suffix
                partialCodeName = partialCodeName + " " + suffixSharedName;
              }
            }
          }

          // Add the code and name to the name map
          terminologyIdToName.put(partialCodeId, partialCodeName);
          newConceptTerminologyIds.add(partialCodeId);
        }
      }
    }

    // Once all concepts have been processed, print the terminologyIds and names
    // to a document for review.
    // Include all previously-existing concepts and newly calculated partials.

    List<String> terminologyIds = new ArrayList<>(terminologyIdToName.keySet());
    Collections.sort(terminologyIds);

    try (PrintWriter writer =
        new PrintWriter(Files.newBufferedWriter(Paths.get("C:\\mapping\\cihi\\cciNames.txt")))) {
      for (String terminologyId : terminologyIds) {
        writer.print(terminologyId + "\t" + terminologyIdToName.get(terminologyId));
        writer.println();
      }
    }

    contentService.close();
  }
  
  private String getRubricId(String terminologyId) throws Exception {
    if(terminologyId == null || terminologyId.length() < 8) {
      throw new Exception("TerminologyId " + terminologyId + " is not valid for determining a Rubric");
    }
    
    return terminologyId.substring(0, 8).concat("^^");
  }
}
