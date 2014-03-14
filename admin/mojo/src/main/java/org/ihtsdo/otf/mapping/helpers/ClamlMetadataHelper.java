package org.ihtsdo.otf.mapping.helpers;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;

/**
 * Helper class for creating metadata for non-RF2 terminologies.
 */
public class ClamlMetadataHelper {

	/** The date format. */
	private final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The effective time. */
	private String effectiveTime;

	/** The terminology. */
	private String terminology;

	/** The terminology version. */
	private String terminologyVersion;

	/** The manager. */
	private EntityManager manager;

	/** The concept map. */
	private Map<String, Concept> conceptMap;

	/**
	 * Instantiates a {@link ClamlMetadataHelper} from the specified parameters.
	 * 
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param effectiveTime the effective time
	 * @param manager the manager
	 */
	public ClamlMetadataHelper(String terminology, String terminologyVersion,
			String effectiveTime, EntityManager manager) {
		this.terminology = terminology;
		this.terminologyVersion = terminologyVersion;
		this.effectiveTime = effectiveTime;
		this.manager = manager;
	}

	/**
	 * Creates the metadata in the form of a map of keys to metadata concepts.
	 * 
	 * <pre>
	 * Metadata 
	 *   DescriptionType
	 *     footnote
	 *     text
	 *     coding-hint
	 *     definition
	 *     introduction 
	 *     modifierlink 
	 *     note
	 *     exclusion
	 *     inclusion
	 *     preferredLong
	 *     preferredAbbreviation
	 *     preferred
	 *     consider
	 *   RelationshipType
	 *     isa
	 *     dagger-to-asterisk
	 *     asterisk-to-dagger
	 *   DefinitionStatus
	 *     defaultDefinitionStatus
	 *   Module
	 *     defaultModule
	 *   CaseSignificance
	 *     defaultCaseSignificance
	 *   CharacteristicType
	 *     defaultCharacteristicType
	 *   Modifier
	 *     defaultModifier
	 *   Asterisk refset
	 *   Dagger refset
	 * </pre>
	 * 
	 * @return metadata map
	 * @throws Exception the exception
	 */
	public Map<String, Concept> createMetadata() throws Exception {
		EntityTransaction tx = manager.getTransaction();
		tx.begin();
		conceptMap = new HashMap<String, Concept>();

		// terminology id counter, start at 1
		int metadataCounter = 1;

		//
		// Create concepts for components of Concept and Description first
		//
		Concept defaultDefinitionStatusConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Default definition status", effectiveTime);
		defaultDefinitionStatusConcept.setDefinitionStatusId(Long
				.valueOf(defaultDefinitionStatusConcept.getTerminologyId()));
		conceptMap.put("defaultDefinitionStatus", defaultDefinitionStatusConcept);

		Concept defaultModuleConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Default module", effectiveTime);
		defaultModuleConcept.setModuleId(new Long(defaultModuleConcept
				.getTerminologyId()));
		conceptMap.put("defaultModule", defaultModuleConcept);

		Concept defaultCaseSignificanceConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Default case significance", effectiveTime);
		conceptMap.put("defaultCaseSignificance", defaultCaseSignificanceConcept);

		Concept preferredConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Preferred", effectiveTime);
		conceptMap.put("preferred", preferredConcept);

		// fill in values for components that were not yet available during
		// instantiation
		defaultDefinitionStatusConcept.setModuleId(new Long(conceptMap.get(
				"defaultModule").getTerminologyId()));
		for (Description desc : defaultDefinitionStatusConcept.getDescriptions()) {
			desc.setModuleId(new Long(conceptMap.get("defaultModule")
					.getTerminologyId()));
			desc.setCaseSignificanceId(new Long(conceptMap.get(
					"defaultCaseSignificance").getTerminologyId()));
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}

		for (Description desc : defaultModuleConcept.getDescriptions()) {
			desc.setModuleId(new Long(conceptMap.get("defaultModule")
					.getTerminologyId()));
			desc.setCaseSignificanceId(new Long(conceptMap.get(
					"defaultCaseSignificance").getTerminologyId()));
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}

		for (Description desc : defaultCaseSignificanceConcept.getDescriptions()) {
			desc.setCaseSignificanceId(new Long(conceptMap.get(
					"defaultCaseSignificance").getTerminologyId()));
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}

		for (Description desc : preferredConcept.getDescriptions()) {
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}

		// persist initial component concepts/descriptions
		manager.persist(defaultModuleConcept);
		manager.persist(defaultDefinitionStatusConcept);
		manager.persist(defaultCaseSignificanceConcept);
		manager.persist(preferredConcept);

		//
		// build remainder of metadata hierarchy
		//
		Concept isaConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Isa", effectiveTime);
		conceptMap.put("isa", isaConcept);
		manager.persist(isaConcept);

		Concept asteriskToDaggerConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Asterisk to dagger", effectiveTime);
		conceptMap.put("asterisk-to-dagger", asteriskToDaggerConcept);
		manager.persist(asteriskToDaggerConcept);

		Concept daggerToAsteriskConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Dagger to asterisk", effectiveTime);
		conceptMap.put("dagger-to-asterisk", daggerToAsteriskConcept);
		manager.persist(daggerToAsteriskConcept);

		Concept exclusionConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Exclusion", effectiveTime);
		conceptMap.put("exclusion", exclusionConcept);
		manager.persist(exclusionConcept);

		Concept inclusionConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Inclusion", effectiveTime);
		conceptMap.put("inclusion", inclusionConcept);
		manager.persist(inclusionConcept);

		Concept considerConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Consider", effectiveTime);
		conceptMap.put("consider", considerConcept);
		manager.persist(considerConcept);

		Concept relationshipTypeConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Relationship type", effectiveTime);
		conceptMap.put("relationshipType", relationshipTypeConcept);
		manager.persist(relationshipTypeConcept);

		Concept preferredLongConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Preferred long", effectiveTime);
		conceptMap.put("preferredLong", preferredLongConcept);
		manager.persist(preferredLongConcept);

		Concept preferredAbbreviatedConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Preferred abbreviated", effectiveTime);
		conceptMap.put("preferredAbbreviated", preferredAbbreviatedConcept);
		manager.persist(preferredAbbreviatedConcept);

		Concept noteConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Note", effectiveTime);
		conceptMap.put("note", noteConcept);
		manager.persist(noteConcept);

		Concept descriptionTypeConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Description type", effectiveTime);
		conceptMap.put("descriptionType", descriptionTypeConcept);
		manager.persist(descriptionTypeConcept);

		Concept caseSignificanceConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Case significance", effectiveTime);
		conceptMap.put("caseSignificance", caseSignificanceConcept);
		manager.persist(caseSignificanceConcept);

		Concept characteristicTypeConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Characteristic type", effectiveTime);
		conceptMap.put("characteristicType", characteristicTypeConcept);
		manager.persist(characteristicTypeConcept);

		Concept defaultCharacteristicTypeConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Default characteristic type", effectiveTime);
		conceptMap.put("defaultCharacteristicType",
				defaultCharacteristicTypeConcept);
		manager.persist(defaultCharacteristicTypeConcept);

		Concept modifierConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Modifier", effectiveTime);
		conceptMap.put("modifier", modifierConcept);
		manager.persist(modifierConcept);

		Concept defaultModifierConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Default modifier", effectiveTime);
		conceptMap.put("defaultModifier", defaultModifierConcept);
		manager.persist(defaultModifierConcept);

		Concept definitionStatusConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Definition status", effectiveTime);
		conceptMap.put("definitionStatus", definitionStatusConcept);
		manager.persist(definitionStatusConcept);

		Concept moduleConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Module", effectiveTime);
		conceptMap.put("module", moduleConcept);
		manager.persist(moduleConcept);

		Concept footnoteConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Footnote", effectiveTime);
		conceptMap.put("footnote", footnoteConcept);
		manager.persist(footnoteConcept);

		Concept textConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Text", effectiveTime);
		conceptMap.put("text", textConcept);
		manager.persist(textConcept);

		Concept codingHintConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Coding hint", effectiveTime);
		conceptMap.put("coding-hint", codingHintConcept);
		manager.persist(codingHintConcept);

		Concept definitionConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Definition", effectiveTime);
		conceptMap.put("definition", definitionConcept);
		manager.persist(definitionConcept);

		Concept introductionConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Introduction", effectiveTime);
		conceptMap.put("introduction", introductionConcept);
		manager.persist(introductionConcept);

		Concept modifierlinkConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Modifier link", effectiveTime);
		conceptMap.put("modifierlink", modifierlinkConcept);
		manager.persist(modifierlinkConcept);

		Concept asteriskRefsetConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Asterisk refset", effectiveTime);
		conceptMap.put("aster", asteriskRefsetConcept);
		manager.persist(asteriskRefsetConcept);

		Concept daggerRefsetConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Dagger refset", effectiveTime);
		conceptMap.put("dagger", daggerRefsetConcept);
		manager.persist(daggerRefsetConcept);
		
		Concept treeRootConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Tree root", effectiveTime);
		conceptMap.put("treeRoot", treeRootConcept);
		manager.persist(treeRootConcept);

		Concept metadataConcept =
				createNewActiveConcept("" + metadataCounter++, terminology,
						terminologyVersion, "Metadata", effectiveTime);
		conceptMap.put("metadata", metadataConcept);
		manager.persist(metadataConcept);

		//
		// Make relationships for metadata
		//
		createIsaRelationship(metadataConcept, asteriskRefsetConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(metadataConcept, daggerRefsetConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(metadataConcept, descriptionTypeConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(metadataConcept, relationshipTypeConcept, ""
				+ metadataCounter++, terminology, terminologyVersion, effectiveTime);

		createIsaRelationship(metadataConcept, definitionStatusConcept, ""
				+ metadataCounter++, terminology, terminologyVersion, effectiveTime);

		createIsaRelationship(metadataConcept, moduleConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(metadataConcept, caseSignificanceConcept, ""
				+ metadataCounter++, terminology, terminologyVersion, effectiveTime);

		createIsaRelationship(metadataConcept, characteristicTypeConcept, ""
				+ metadataCounter++, terminology, terminologyVersion, effectiveTime);

		createIsaRelationship(metadataConcept, modifierConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);
		
		createIsaRelationship(metadataConcept, treeRootConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(definitionStatusConcept,
				defaultDefinitionStatusConcept, "" + metadataCounter++, terminology,
				terminologyVersion, effectiveTime);

		createIsaRelationship(moduleConcept, defaultModuleConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(caseSignificanceConcept,
				defaultCaseSignificanceConcept, "" + metadataCounter++, terminology,
				terminologyVersion, effectiveTime);

		createIsaRelationship(characteristicTypeConcept,
				defaultCharacteristicTypeConcept, "" + metadataCounter++, terminology,
				terminologyVersion, effectiveTime);

		createIsaRelationship(modifierConcept, defaultModifierConcept, new Integer(
				metadataCounter++).toString(), terminology, terminologyVersion,
				effectiveTime);

		createIsaRelationship(relationshipTypeConcept, isaConcept, new Integer(
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

		tx.commit();

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

		Concept concept = new ConceptJpa();
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
		Description desc = new DescriptionJpa();
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
		Relationship relationship = new RelationshipJpa();
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
		Set<Relationship> rels = new HashSet<Relationship>();
		rels.add(relationship);
		childConcept.setRelationships(rels);
	}

}
