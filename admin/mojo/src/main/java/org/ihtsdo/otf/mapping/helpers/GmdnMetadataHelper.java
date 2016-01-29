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
 * Helper class for creating metadata for GMDN.
 */
public class GmdnMetadataHelper {

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
   * Instantiates a {@link GmdnMetadataHelper} from the specified parameters.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param effectiveTime the effective time
   * @param contentService the content service
   */
  public GmdnMetadataHelper(String terminology, String terminologyVersion,
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
   *     Meta
   *     Term
   *     Collective Term
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
   *     Simple refsets
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
            terminologyVersion, "Default definition status", effectiveTime,
            "meta");
    defaultDefinitionStatusConcept.setDefinitionStatusId(Long
        .valueOf(defaultDefinitionStatusConcept.getTerminologyId()));
    conceptMap.put("defaultDefinitionStatus", defaultDefinitionStatusConcept);

    final Concept defaultModuleConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default module", effectiveTime, "meta");
    defaultModuleConcept.setModuleId(new Long(defaultModuleConcept
        .getTerminologyId()));
    conceptMap.put("defaultModule", defaultModuleConcept);

    final Concept defaultCaseSignificanceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default case significance", effectiveTime,
            "meta");
    conceptMap.put("defaultCaseSignificance", defaultCaseSignificanceConcept);

    final Concept metaConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Meta", effectiveTime, "meta");
    conceptMap.put("meta", metaConcept);

    final Concept defaultModifierConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default modifier", effectiveTime, "meta");
    conceptMap.put("defaultModifier", defaultModifierConcept);
    contentService.addConcept(defaultModifierConcept);

    final Concept defaultCharacteristicTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Default characteristic type", effectiveTime,
            "meta");
    conceptMap.put("defaultCharacteristicType",
        defaultCharacteristicTypeConcept);
    contentService.addConcept(defaultCharacteristicTypeConcept);

    final Concept isaConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Isa", effectiveTime, "meta");
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
      desc.setTypeId(new Long(conceptMap.get("meta").getTerminologyId()));
    }
    for (final Description desc : defaultModuleConcept.getDescriptions()) {
      desc.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(new Long(conceptMap.get("meta").getTerminologyId()));
    }
    for (final Description desc : defaultCaseSignificanceConcept
        .getDescriptions()) {
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setTypeId(new Long(conceptMap.get("meta").getTerminologyId()));
    }

    for (final Description desc : metaConcept.getDescriptions()) {
      desc.setTypeId(new Long(conceptMap.get("meta").getTerminologyId()));
    }

    //
    // persist all initial component concepts/descriptions
    //
    contentService.addConcept(defaultModuleConcept);
    contentService.addConcept(defaultDefinitionStatusConcept);
    contentService.addConcept(defaultCaseSignificanceConcept);
    contentService.addConcept(metaConcept);

    //
    // build remainder of metadata hierarchy (in order)
    //

    //
    // Top level concept
    //
    final Concept metadataConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Metadata", effectiveTime, "meta");
    conceptMap.put("metadata", metadataConcept);
    contentService.addConcept(metadataConcept);

    //
    // Description types
    //
    final Concept descriptionTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Description type", effectiveTime, "meta");
    conceptMap.put("descriptionType", descriptionTypeConcept);
    contentService.addConcept(descriptionTypeConcept);

    final Concept termConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Term", effectiveTime, "meta");
    conceptMap.put("term", termConcept);
    contentService.addConcept(termConcept);

    final Concept collectiveTermConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Collective Term", effectiveTime, "meta");
    conceptMap.put("collectiveTerm", collectiveTermConcept);
    contentService.addConcept(termConcept);

    final Concept definitionTermConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Collective Term", effectiveTime, "meta");
    conceptMap.put("definitionTerm", definitionTermConcept);
    contentService.addConcept(termConcept);

    createIsaRelationship(metadataConcept, descriptionTypeConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    createIsaRelationship(descriptionTypeConcept, termConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);
    createIsaRelationship(descriptionTypeConcept, collectiveTermConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    createIsaRelationship(descriptionTypeConcept, definitionTermConcept,
        new Integer(metadataCounter++).toString(), terminology,
        terminologyVersion, effectiveTime);

    //
    // Relationship types
    //
    final Concept relationshipTypeConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Relationship type", effectiveTime, "meta");
    conceptMap.put("relationshipType", relationshipTypeConcept);
    contentService.addConcept(relationshipTypeConcept);

    createIsaRelationship(metadataConcept, relationshipTypeConcept, ""
        + metadataCounter++, terminology, terminologyVersion, effectiveTime);

    createIsaRelationship(relationshipTypeConcept, isaConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    //
    // Case significance
    //
    final Concept caseSignificanceConcept =
        createNewActiveConcept("" + metadataCounter++, terminology,
            terminologyVersion, "Case significance", effectiveTime, "meta");
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
            terminologyVersion, "Characteristic type", effectiveTime, "meta");
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
            terminologyVersion, "Modifier", effectiveTime, "meta");
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
            terminologyVersion, "Definition status", effectiveTime, "meta");
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
            terminologyVersion, "Module", effectiveTime, "meta");
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
            terminologyVersion, "Refsets", effectiveTime, "meta");
    conceptMap.put("refsets", refsetsConcept);
    contentService.addConcept(refsetsConcept);

    // TODO: if we need a refset concept
    // final Concept simpleRefsetsConcept =
    // createNewActiveConcept("" + metadataCounter++, terminology,
    // terminologyVersion, "Simple refsets", effectiveTime);
    // conceptMap.put("simpleRefsets", simpleRefsetsConcept);
    // contentService.addConcept(simpleRefsetsConcept);

    createIsaRelationship(metadataConcept, refsetsConcept, new Integer(
        metadataCounter++).toString(), terminology, terminologyVersion,
        effectiveTime);

    // createIsaRelationship(refsetsConcept, simpleRefsetsConcept, new Integer(
    // metadataCounter++).toString(), terminology, terminologyVersion,
    // effectiveTime);

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
   * @param descriptionType the description type
   * @return the concept
   * @throws Exception the exception
   */
  public Concept createNewActiveConcept(String terminologyId,
    String terminology, String terminologyVersion, String defaultPreferredName,
    String effectiveTime, String descriptionType) throws Exception {

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
    if (conceptMap.containsKey(descriptionType))
      desc.setTypeId(new Long(conceptMap.get(descriptionType)
          .getTerminologyId()));

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
