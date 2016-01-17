package org.ihtsdo.otf.mapping.helpers;

import java.text.SimpleDateFormat;
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
public class ClamlMetadataHelper {

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

  /**
   * Instantiates a {@link ClamlMetadataHelper} from the specified parameters.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param effectiveTime the effective time
   * @param contentService the content service
   */
  public ClamlMetadataHelper(String terminology, String terminologyVersion,
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
   *     Footnote
   *     Text
   *     Coding hint
   *     Definition
   *     Introduction 
   *     Modifier link 
   *     Note
   *     Exclusion
   *     Inclusion
   *     Preferred long
   *     Preferred abbreviation
   *     Preferred
   *     Consider
   *   Relationship type
   *     Isa
   *     Dagger to asterisk
   *     Asterisk to dagger
   *   Definition status
   *     Default definition status
   *   Module
   *     Default module
   *   Case significance
   *     Default case significance
   *   Characteristic type
   *     Default characteristic type
   *   Modifier
   *     Default odifier
   *   Refsets
   *     Simple refsets
   *       Asterisk refset
   *       Dagger refset
   * </pre>
   * 
   * @return metadata map
   * @throws Exception the exception
   */
  public Map<String, Concept> createMetadata() throws Exception {
    conceptMap = new HashMap<>();

    // terminology id counter, start at 1
    int metadataCounter = 1;

    //
    // Create concepts representing defaults needed to create
    // other concepts
    //
    final Concept defaultDefinitionStatusConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default definition status", effectiveTime);
    defaultDefinitionStatusConcept.setDefinitionStatusId(Long
        .valueOf(defaultDefinitionStatusConcept.getTerminologyId()));
    conceptMap.put("defaultDefinitionStatus", defaultDefinitionStatusConcept);

    final Concept defaultModuleConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default module", effectiveTime);
    defaultModuleConcept.setModuleId(new Long(defaultModuleConcept
        .getTerminologyId()));
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

    final Concept isaConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Isa", effectiveTime);
    conceptMap.put("isa", isaConcept);
    contentService.addConcept(isaConcept);

    //
    // Go back and fill in missing references
    // i.e. because "defaultModule" didn't exist when
    // "defaultDefinitionStatus" was created
    //
    defaultDefinitionStatusConcept.setModuleId(new Long(conceptMap.get(
        "defaultModule").getTerminologyId()));
    for (final Description desc : defaultDefinitionStatusConcept
        .getDescriptions()) {
      desc.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
    }
    for (final Description desc : defaultModuleConcept.getDescriptions()) {
      desc.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
    }
    for (final Description desc : defaultCaseSignificanceConcept
        .getDescriptions()) {
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
    }

    for (final Description desc : preferredConcept.getDescriptions()) {
      desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
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
    conceptMap.put("modifier", metadataConcept);
    contentService.addConcept(metadataConcept);

    //
    // Description types
    //
    final Concept descriptionTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Description type", effectiveTime);
    conceptMap.put("descriptionType", descriptionTypeConcept);
    contentService.addConcept(descriptionTypeConcept);

    final Concept codingHintConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Coding hint", effectiveTime);
    conceptMap.put("coding-hint", codingHintConcept);
    contentService.addConcept(codingHintConcept);

    final Concept considerConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Consider", effectiveTime);
    conceptMap.put("consider", considerConcept);
    contentService.addConcept(considerConcept);

    final Concept definitionConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Definition", effectiveTime);
    conceptMap.put("definition", definitionConcept);
    contentService.addConcept(definitionConcept);

    final Concept exclusionConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Exclusion", effectiveTime);
    conceptMap.put("exclusion", exclusionConcept);
    contentService.addConcept(exclusionConcept);

    final Concept inclusionConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Inclusion", effectiveTime);
    conceptMap.put("inclusion", inclusionConcept);
    contentService.addConcept(inclusionConcept);

    final Concept footnoteConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Footnote", effectiveTime);
    conceptMap.put("footnote", footnoteConcept);
    contentService.addConcept(footnoteConcept);

    final Concept introductionConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Introduction", effectiveTime);
    conceptMap.put("introduction", introductionConcept);
    contentService.addConcept(introductionConcept);

    final Concept modifierlinkConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Modifier link", effectiveTime);
    conceptMap.put("modifierlink", modifierlinkConcept);
    contentService.addConcept(modifierlinkConcept);

    final Concept noteConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Note", effectiveTime);
    conceptMap.put("note", noteConcept);
    contentService.addConcept(noteConcept);

    final Concept preferredAbbreviatedConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Preferred abbreviated", effectiveTime);
    conceptMap.put("preferredAbbreviated", preferredAbbreviatedConcept);
    contentService.addConcept(preferredAbbreviatedConcept);

    final Concept preferredLongConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Preferred long", effectiveTime);
    conceptMap.put("preferredLong", preferredLongConcept);
    contentService.addConcept(preferredLongConcept);

    final Concept textConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Text", effectiveTime);
    conceptMap.put("text", textConcept);
    contentService.addConcept(textConcept);

    createIsaRelationship(metadataConcept, descriptionTypeConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(descriptionTypeConcept, footnoteConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(descriptionTypeConcept, textConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(descriptionTypeConcept, codingHintConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, definitionConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, introductionConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, modifierlinkConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, noteConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(descriptionTypeConcept, exclusionConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, inclusionConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, preferredLongConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, preferredAbbreviatedConcept,
        "" + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, preferredConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, considerConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    //
    // Relationship types
    //
    final Concept relationshipTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Relationship type", effectiveTime);
    conceptMap.put("relationshipType", relationshipTypeConcept);
    contentService.addConcept(relationshipTypeConcept);

    final Concept asteriskToDaggerConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Asterisk to dagger", effectiveTime);
    conceptMap.put("asterisk-to-dagger", asteriskToDaggerConcept);
    contentService.addConcept(asteriskToDaggerConcept);

    final Concept daggerToAsteriskConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Dagger to asterisk", effectiveTime);
    conceptMap.put("dagger-to-asterisk", daggerToAsteriskConcept);
    contentService.addConcept(daggerToAsteriskConcept);

    final Concept referenceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Reference", effectiveTime);
    conceptMap.put("reference", referenceConcept);
    contentService.addConcept(referenceConcept);

    createIsaRelationship(metadataConcept, relationshipTypeConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(relationshipTypeConcept, isaConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(relationshipTypeConcept, daggerToAsteriskConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(relationshipTypeConcept, referenceConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(relationshipTypeConcept, asteriskToDaggerConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    //
    // Case significance
    //
    final Concept caseSignificanceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Case significance", effectiveTime);
    conceptMap.put("caseSignificance", caseSignificanceConcept);
    contentService.addConcept(caseSignificanceConcept);

    createIsaRelationship(metadataConcept, caseSignificanceConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

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

    createIsaRelationship(metadataConcept, characteristicTypeConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

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

    createIsaRelationship(metadataConcept, modifierConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(modifierConcept, defaultModifierConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    //
    // Definition status
    //
    final Concept definitionStatusConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Definition status", effectiveTime);
    conceptMap.put("definitionStatus", definitionStatusConcept);
    contentService.addConcept(definitionStatusConcept);

    createIsaRelationship(metadataConcept, definitionStatusConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(definitionStatusConcept,
        defaultDefinitionStatusConcept, "" + metadataCounter++, terminology,
        terminologyVersion, effectiveTime);

    //
    // Module
    //
    final Concept moduleConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Module", effectiveTime);
    conceptMap.put("module", moduleConcept);
    contentService.addConcept(moduleConcept);

    createIsaRelationship(metadataConcept, moduleConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(moduleConcept, defaultModuleConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    //
    // Refsets
    //
    final Concept refsetsConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Refsets", effectiveTime);
    conceptMap.put("refsets", refsetsConcept);
    contentService.addConcept(refsetsConcept);

    final Concept simpleRefsetsConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Simple refsets", effectiveTime);
    conceptMap.put("simpleRefsets", simpleRefsetsConcept);
    contentService.addConcept(simpleRefsetsConcept);

    final Concept asteriskRefsetConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Asterisk refset", effectiveTime);
    conceptMap.put("aster", asteriskRefsetConcept);
    contentService.addConcept(asteriskRefsetConcept);

    final Concept daggerRefsetConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Dagger refset", effectiveTime);
    conceptMap.put("dagger", daggerRefsetConcept);
    contentService.addConcept(daggerRefsetConcept);

    createIsaRelationship(metadataConcept, refsetsConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(refsetsConcept, simpleRefsetsConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(simpleRefsetsConcept, asteriskRefsetConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(simpleRefsetsConcept, daggerRefsetConcept,
        new Integer(metadataCounter++).toString(), terminology,
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
    concept.setDefaultPreferredName(defaultPreferredName);
    concept.setActive(true);
    // default definition status
    if (conceptMap.containsKey("defaultDefinitionStatus"))
      concept.setDefinitionStatusId(new Long(conceptMap.get(
          "defaultDefinitionStatus").getTerminologyId()));
    // default module
    if (conceptMap.containsKey("defaultModule"))
      concept.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));

    // Create a preferred name description
    final Description desc = new DescriptionJpa();
    desc.setTerminologyId(terminologyId);
    desc.setEffectiveTime(dt.parse(effectiveTime));
    desc.setActive(true);
    // default module
    if (conceptMap.containsKey("defaultModule"))
      desc.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));
    desc.setTerminology(terminology);
    desc.setTerminologyVersion(terminologyVersion);
    desc.setTerm(defaultPreferredName);
    desc.setConcept(concept);
    // default case significance
    if (conceptMap.containsKey("defaultCaseSignificance"))
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
    desc.setLanguageCode("en");
    // preferred description type
    if (conceptMap.containsKey("preferred"))
      desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));

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
  public void createIsaRelationship(Concept parentConcept,
    Concept childConcept, String terminologyId, String terminology,
    String terminologyVersion, String effectiveTime) throws Exception {
    if (parentConcept == null) {
      throw new Exception("Parent concept may not be null");
    }
    final Relationship relationship = new RelationshipJpa();
    relationship.setTerminologyId(terminologyId);
    relationship.setEffectiveTime(dt.parse(effectiveTime));
    relationship.setActive(true);
    relationship.setModuleId(new Long(conceptMap.get("defaultModule")
        .getTerminologyId()));
    relationship.setTerminology(terminology);
    relationship.setTerminologyVersion(terminologyVersion);
    // default characteristic type
    relationship.setCharacteristicTypeId(new Long(conceptMap.get(
        "defaultCharacteristicType").getTerminologyId()));
    // default modifier
    relationship.setModifierId(new Long(conceptMap.get("defaultModifier")
        .getTerminologyId()));
    relationship.setDestinationConcept(parentConcept);
    relationship.setSourceConcept(childConcept);
    // default "isa" type
    relationship.setTypeId(new Long(conceptMap.get("isa").getTerminologyId()));
    final Set<Relationship> rels = new HashSet<>();
    rels.add(relationship);
    childConcept.setRelationships(rels);
  }

}
