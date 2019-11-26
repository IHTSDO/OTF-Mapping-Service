/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Helper class for creating metadata for non-RF2 terminologies.
 */
public class SimpleMetadataHelper {

  /** The date format. */
  private final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");

  /** The effective time. */
  private String effectiveTime;

  /** The terminology. */
  private String terminology;

  /** The terminology version. */
  private String terminologyVersion;

  /** The Content service */
  private ContentService contentService;

  /** The concept map. */
  private Map<String, Concept> conceptMap;

  // terminology metadata id counter, start at 1
  private int metadataCounter = 1;

  /**
   * Instantiates a {@link SimpleMetadataHelper} from the specified parameters.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param effectiveTime the effective time
   * @param contentService the content service
   */
  public SimpleMetadataHelper(String terminology, String terminologyVersion,
      String effectiveTime, ContentService contentService) {
    this.terminology = terminology;
    this.terminologyVersion = terminologyVersion;
    this.effectiveTime = effectiveTime;
    this.contentService = contentService;
  }

  /**
   * Creates the metadata in the form of a map of keys to metadata concepts.
   * 
   * <pre>
   * Metadata 
   *   Description type
   *     Preferred
   *     Synonym
   *   Relationship type
   *     Isa
   *   Definition status
   *     Default definition status
   *   Module
   *     Default module
   *   Case significance
   *     Default case significance
   *   Characteristic type
   *     Default characteristic type
   *   Modifier
   *     Default modifier
   *   Refsets
   *     n/a
   * </pre>
   * 
   * @return metadata map
   * @throws Exception the exception
   */
  public Map<String, Concept> createMetadata() throws Exception {
    conceptMap = new HashMap<>();

    //
    // Create concepts representing defaults needed to create
    // other concepts
    //
    final Concept defaultDefinitionStatusConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default definition status", effectiveTime);
    defaultDefinitionStatusConcept.setDefinitionStatusId(
        Long.valueOf(defaultDefinitionStatusConcept.getTerminologyId()));
    conceptMap.put("defaultDefinitionStatus", defaultDefinitionStatusConcept);

    final Concept defaultModuleConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default module", effectiveTime);
    defaultModuleConcept
        .setModuleId(Long.valueOf(defaultModuleConcept.getTerminologyId()));
    conceptMap.put("defaultModule", defaultModuleConcept);

    final Concept defaultCaseSignificanceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default case significance", effectiveTime);
    conceptMap.put("defaultCaseSignificance", defaultCaseSignificanceConcept);

    final Concept preferredConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Preferred", effectiveTime);
    conceptMap.put("preferred", preferredConcept);

    final Concept defaultModifierConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default modifier", effectiveTime);
    conceptMap.put("defaultModifier", defaultModifierConcept);
    contentService.addConcept(defaultModifierConcept);

    final Concept defaultCharacteristicTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default characteristic type", effectiveTime);
    conceptMap.put("defaultCharacteristicType",
        defaultCharacteristicTypeConcept);
    contentService.addConcept(defaultCharacteristicTypeConcept);

    final Concept isaConcept = createNewActiveConcept("" + metadataCounter++,
        terminology, terminologyVersion, "Isa", effectiveTime);
    conceptMap.put("isa", isaConcept);
    contentService.addConcept(isaConcept);

    //
    // Go back and fill in missing references
    // i.e. because "defaultModule" didn't exist when
    // "defaultDefinitionStatus" was created
    //
    defaultDefinitionStatusConcept.setModuleId(
        Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
    for (final Description desc : defaultDefinitionStatusConcept
        .getDescriptions()) {
      desc.setModuleId(
          Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
      desc.setCaseSignificanceId(Long.valueOf(
          conceptMap.get("defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(
          Long.valueOf(conceptMap.get("preferred").getTerminologyId()));
    }
    for (final Description desc : defaultModuleConcept.getDescriptions()) {
      desc.setModuleId(
          Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
      desc.setCaseSignificanceId(Long.valueOf(
          conceptMap.get("defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(
          Long.valueOf(conceptMap.get("preferred").getTerminologyId()));
    }
    for (final Description desc : defaultCaseSignificanceConcept
        .getDescriptions()) {
      desc.setCaseSignificanceId(Long.valueOf(
          conceptMap.get("defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(
          Long.valueOf(conceptMap.get("preferred").getTerminologyId()));
    }

    for (final Description desc : preferredConcept.getDescriptions()) {
      desc.setTypeId(
          Long.valueOf(conceptMap.get("preferred").getTerminologyId()));
    }

    //
    // persist all initial component concepts/descriptions
    //
    contentService.addConcept(defaultModuleConcept);
    contentService.addConcept(defaultDefinitionStatusConcept);
    contentService.addConcept(defaultCaseSignificanceConcept);
    contentService.addConcept(preferredConcept);

    //
    // build remainder of metadata hierarchy (in order)
    //

    //
    // Top level concept
    //
    final Concept metadataConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Metadata", effectiveTime);
    conceptMap.put("Metadata", metadataConcept);
    contentService.addConcept(metadataConcept);

    //
    // Description types
    //
    final Concept descriptionTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Description type", effectiveTime);
    conceptMap.put("descriptionType", descriptionTypeConcept);
    contentService.addConcept(descriptionTypeConcept);

    final Concept synonymConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Synonym", effectiveTime);
    conceptMap.put("synonym", synonymConcept);
    contentService.addConcept(synonymConcept);

    createIsaRelationship(metadataConcept, descriptionTypeConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, synonymConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, preferredConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    //
    // Relationship types - isa
    //
    final Concept relationshipTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Relationship type", effectiveTime);
    conceptMap.put("relationshipType", relationshipTypeConcept);
    contentService.addConcept(relationshipTypeConcept);

    createIsaRelationship(metadataConcept, relationshipTypeConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(relationshipTypeConcept, isaConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    //
    // Case significance
    //
    final Concept caseSignificanceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Case significance", effectiveTime);
    conceptMap.put("caseSignificance", caseSignificanceConcept);
    contentService.addConcept(caseSignificanceConcept);

    createIsaRelationship(metadataConcept, caseSignificanceConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(caseSignificanceConcept,
        defaultCaseSignificanceConcept, "" + metadataCounter++, terminology,
        terminologyVersion, effectiveTime);

    //
    // Characteristic type
    //
    final Concept characteristicTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Characteristic type", effectiveTime);
    conceptMap.put("characteristicType", characteristicTypeConcept);
    contentService.addConcept(characteristicTypeConcept);

    createIsaRelationship(metadataConcept, characteristicTypeConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(characteristicTypeConcept,
        defaultCharacteristicTypeConcept, "" + metadataCounter++, terminology,
        terminologyVersion, effectiveTime);

    //
    // Modifier
    //
    final Concept modifierConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Modifier", effectiveTime);
    conceptMap.put("modifier", modifierConcept);
    contentService.addConcept(modifierConcept);

    createIsaRelationship(metadataConcept, modifierConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(modifierConcept, defaultModifierConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    //
    // Definition status
    //
    final Concept definitionStatusConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Definition status", effectiveTime);
    conceptMap.put("definitionStatus", definitionStatusConcept);
    contentService.addConcept(definitionStatusConcept);

    createIsaRelationship(metadataConcept, definitionStatusConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(definitionStatusConcept,
        defaultDefinitionStatusConcept, "" + metadataCounter++, terminology,
        terminologyVersion, effectiveTime);

    //
    // Module
    //
    final Concept moduleConcept = createNewActiveConcept("" + metadataCounter++,
        terminology, terminologyVersion, "Module", effectiveTime);
    conceptMap.put("module", moduleConcept);
    contentService.addConcept(moduleConcept);

    createIsaRelationship(metadataConcept, moduleConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(moduleConcept, defaultModuleConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    //
    // Refsets
    //
    final Concept refsetsConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Refsets", effectiveTime);
    conceptMap.put("refsets", refsetsConcept);
    contentService.addConcept(refsetsConcept);

    createIsaRelationship(metadataConcept, refsetsConcept,
        Integer.valueOf(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    return conceptMap;
  }

  /**
   * Creates the new active concept and attached description using default
   * metadata.
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param defaultPreferredName the default preferred name
   * @param effectiveTime the effective time
   * @return the concept
   * @throws Exception the exception
   */
  public Concept createNewActiveConcept(String terminologyId,
    String terminology, String terminologyVersion, String defaultPreferredName,
    String effectiveTime) throws Exception {

    final Concept concept = new ConceptJpa();
    concept.setTerminologyId(terminologyId);
    concept.setTerminology(terminology);
    concept.setTerminologyVersion(terminologyVersion);
    concept.setEffectiveTime(dt.parse(effectiveTime));
    // If this is the fifth digit below a 4th digit placeholder, clean text
    if (terminologyId.length() == 6) {
      concept.setDefaultPreferredName(
          defaultPreferredName.replaceAll("- PLACEHOLDER 4th digit ", ""));
    } else {
      concept.setDefaultPreferredName(defaultPreferredName);
    }
    concept.setActive(true);
    // default definition status
    if (conceptMap.containsKey("defaultDefinitionStatus"))
      concept.setDefinitionStatusId(Long.valueOf(
          conceptMap.get("defaultDefinitionStatus").getTerminologyId()));
    // default module
    if (conceptMap.containsKey("defaultModule"))
      concept.setModuleId(
          Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));

    // Create a preferred name description
    final Description desc = new DescriptionJpa();
    desc.setTerminologyId(terminologyId);
    desc.setEffectiveTime(dt.parse(effectiveTime));
    desc.setActive(true);
    // default module
    if (conceptMap.containsKey("defaultModule"))
      desc.setModuleId(
          Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
    desc.setTerminology(terminology);
    desc.setTerminologyVersion(terminologyVersion);
    if (terminologyId.length() == 6) {
      desc.setTerm(
          defaultPreferredName.replaceAll("- PLACEHOLDER 4th digit ", ""));
    } else {
      desc.setTerm(defaultPreferredName);
    }
    desc.setConcept(concept);
    // default case significance
    if (conceptMap.containsKey("defaultCaseSignificance"))
      desc.setCaseSignificanceId(Long.valueOf(
          conceptMap.get("defaultCaseSignificance").getTerminologyId()));
    desc.setLanguageCode("en");
    // preferred description type
    if (conceptMap.containsKey("preferred"))
      desc.setTypeId(
          Long.valueOf(conceptMap.get("preferred").getTerminologyId()));

    concept.addDescription(desc);

    return concept;
  }

  /**
   * Creates the isa relationship with default metadata.
   * 
   * @param parentConcept the parent concept
   * @param childConcept the child concept
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param effectiveTime the effective time
   * @throws Exception the exception
   */
  public void createIsaRelationship(Concept parentConcept, Concept childConcept,
    String terminologyId, String terminology, String terminologyVersion,
    String effectiveTime) throws Exception {
    if (parentConcept == null) {
      throw new Exception("Parent concept may not be null");
    }
    final Relationship relationship = new RelationshipJpa();
    relationship.setTerminologyId(terminologyId);
    relationship.setEffectiveTime(dt.parse(effectiveTime));
    relationship.setActive(true);
    relationship.setModuleId(
        Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
    relationship.setTerminology(terminology);
    relationship.setTerminologyVersion(terminologyVersion);
    // default characteristic type
    relationship.setCharacteristicTypeId(Long.valueOf(
        conceptMap.get("defaultCharacteristicType").getTerminologyId()));
    // default modifier
    relationship.setModifierId(
        Long.valueOf(conceptMap.get("defaultModifier").getTerminologyId()));
    relationship.setDestinationConcept(parentConcept);
    relationship.setSourceConcept(childConcept);
    // default "isa" type
    relationship
        .setTypeId(Long.valueOf(conceptMap.get("isa").getTerminologyId()));

    if (childConcept.getRelationships() == null) {
      final Set<Relationship> rels = new HashSet<>();
      childConcept.setRelationships(rels);
    }
    childConcept.getRelationships().add(relationship);
  }

  public Concept createNewActiveConcept(String defaultPreferredName,
    Concept parent) throws Exception {
    final Concept concept = createNewActiveConcept("" + metadataCounter++,
        terminology, terminologyVersion, defaultPreferredName, effectiveTime);
    contentService.addConcept(concept);
    createIsaRelationship(parent, concept, "" + metadataCounter++, terminology,
        terminologyVersion, effectiveTime);
    return concept;
  }

  public void createAttributeValue(Concept concept, long typeId, String value,
    String version, int objCt, Date now) {
    final Description sy = new DescriptionJpa();
    sy.setTerminologyId(objCt + "");
    sy.setEffectiveTime(now);
    sy.setActive(true);
    sy.setModuleId(
        Long.parseLong(conceptMap.get("defaultModule").getTerminologyId()));
    sy.setTerminology(terminology);
    sy.setTerminologyVersion(version);
    sy.setTerm(value);
    sy.setConcept(concept);
    sy.setCaseSignificanceId(Long
        .valueOf(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
    sy.setLanguageCode("en");
    sy.setTypeId(typeId);
    concept.addDescription(sy);
  }
}
