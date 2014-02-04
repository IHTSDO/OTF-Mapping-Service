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

public class CreateMetadataHelper {

	/** The dt. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

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
	 * Instantiates a {@link CreateMetadataHelper} from the specified parameters.
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param effectiveTime the effective time
	 * @param manager the manager
	 */
	public  CreateMetadataHelper (String terminology, String terminologyVersion,
		String effectiveTime, EntityManager manager) {
		this.terminology = terminology;
		this.terminologyVersion = terminologyVersion;
		this.effectiveTime = effectiveTime;
		this.manager = manager;
	}
	
	/**
	 * Creates the metadata.
	 *
	 * @throws Exception the exception
	 */
	public Map<String, Concept> createMetadata() throws Exception {
		EntityTransaction tx = manager.getTransaction();
		tx.begin();
		conceptMap = new HashMap<String, Concept>();

		// Metadata
		// DescriptionType					
		//   footnote																		
		//   text										
		//   coding-hint						
		//   definition							
		//   introduction
		//   modifierlink
		//   note
		//   exclusion
		//   inclusion
		//   preferredLong
		//   preferredAbbreviation
		//   preferred
		//   consider
		//
		// RelationshipType	
		//		isa
		//		dagger-to-asterisk
		// 		asterisk-to-dagger
		//
		// DefinitionStatus	
		//		defaultDefinitionStatus
		//
		// Module				
		//		defaultModule
		//
		// CaseSignificance
		//    defaultCaseSignificance
		//
		// CharacteristicType
		//    defaultCharacteristicType
		//
		// Modifier
		//		defaultModifier
		//
		// Asterisk refset
		// Dagger refset
		
		
		// 
		// Make metadata concepts and descriptions
		//
		int metadataCounter = 1;
		//
		// create concepts for components of Concept and Description first
		//
		Concept defaultDefinitionStatusConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Default definition status", effectiveTime);
		defaultDefinitionStatusConcept.setDefinitionStatusId(new Long("1"));
		conceptMap.put("defaultDefinitionStatus", defaultDefinitionStatusConcept);

		Concept defaultModuleConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Default module", effectiveTime);	
		defaultModuleConcept.setModuleId(new Long("2"));
		conceptMap.put("defaultModule", defaultModuleConcept);
		
		Concept defaultCaseSignificanceConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Default case significance", effectiveTime);
		conceptMap.put("defaultCaseSignificance",
				defaultCaseSignificanceConcept);
		
		Concept preferredConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Preferred", effectiveTime);	
		conceptMap.put("preferred", preferredConcept);
		
		// fill in values for components that were not yet available during instantiation
		defaultDefinitionStatusConcept.setModuleId(new Long(conceptMap.get("defaultModule").getTerminologyId()));		
		for (Description desc : defaultDefinitionStatusConcept.getDescriptions()) {
			desc.setModuleId(new Long(conceptMap.get("defaultModule").getTerminologyId()));
			desc.setCaseSignificanceId(new Long(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}
		
		for (Description desc : defaultModuleConcept.getDescriptions()) {
			desc.setModuleId(new Long(conceptMap.get("defaultModule").getTerminologyId()));
			desc.setCaseSignificanceId(new Long(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
			desc.setTypeId(new Long(conceptMap.get("preferred").getTerminologyId()));
		}
		
		for (Description desc : defaultCaseSignificanceConcept.getDescriptions()) {
			desc.setCaseSignificanceId(new Long(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
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
		Concept isaConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Isa", effectiveTime);		
		conceptMap.put("isa", isaConcept);
		manager.persist(isaConcept);
		
		Concept asteriskToDaggerConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Asterisk to dagger", effectiveTime);		
		conceptMap.put("asterisk-to-dagger", asteriskToDaggerConcept);
		manager.persist(asteriskToDaggerConcept);
		
		Concept daggerToAsteriskConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Dagger to asterisk", effectiveTime);		
		conceptMap.put("dagger-to-asterisk", daggerToAsteriskConcept);
		manager.persist(daggerToAsteriskConcept);

		Concept exclusionConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Exclusion", effectiveTime);			
		conceptMap.put("exclusion", exclusionConcept);
		manager.persist(exclusionConcept);
		
		Concept inclusionConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Inclusion", effectiveTime);	
		conceptMap.put("inclusion", inclusionConcept);
		manager.persist(inclusionConcept);
		
		Concept considerConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Consider", effectiveTime);	
		conceptMap.put("consider", considerConcept);
		manager.persist(considerConcept);

		Concept relationshipTypeConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Relationship type", effectiveTime);	
		conceptMap.put("relationshipType", relationshipTypeConcept);
		manager.persist(relationshipTypeConcept);

		Concept preferredLongConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Preferred long", effectiveTime);	
		conceptMap.put("preferredLong", preferredLongConcept);
		manager.persist(preferredLongConcept);

		Concept preferredAbbreviatedConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Preferred abbreviated", effectiveTime);		
		conceptMap.put("preferredAbbreviated", preferredAbbreviatedConcept);
		manager.persist(preferredAbbreviatedConcept);

		Concept noteConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Note", effectiveTime);		
		conceptMap.put("note", noteConcept);
		manager.persist(noteConcept);

		Concept descriptionTypeConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Description type", effectiveTime);
		conceptMap.put("descriptionType", descriptionTypeConcept);
		manager.persist(descriptionTypeConcept);
		
		Concept caseSignificanceConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Case significance", effectiveTime);
		conceptMap.put("caseSignificance", caseSignificanceConcept);
		manager.persist(caseSignificanceConcept);

		Concept characteristicTypeConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Characteristic type", effectiveTime);
		conceptMap.put("characteristicType", characteristicTypeConcept);
		manager.persist(characteristicTypeConcept);

		Concept defaultCharacteristicTypeConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Default characteristic type", effectiveTime);
		conceptMap.put("defaultCharacteristicType",
				defaultCharacteristicTypeConcept);
		manager.persist(defaultCharacteristicTypeConcept);

		Concept modifierConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Modifier", effectiveTime);
		conceptMap.put("modifier", modifierConcept);
		manager.persist(modifierConcept);

		Concept defaultModifierConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Default modifier", effectiveTime);
		conceptMap.put("defaultModifier",
				defaultModifierConcept);
		manager.persist(defaultModifierConcept);

		Concept definitionStatusConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Definition status", effectiveTime);
		conceptMap.put("definitionStatus", definitionStatusConcept);
		manager.persist(definitionStatusConcept);

		Concept moduleConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Module", effectiveTime);
		conceptMap.put("module", moduleConcept);
		manager.persist(moduleConcept);
		
		
		Concept footnoteConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Footnote", effectiveTime);
		conceptMap.put("footnote", footnoteConcept);
		manager.persist(footnoteConcept);
		
		Concept textConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Text", effectiveTime);
		conceptMap.put("text", textConcept);
		manager.persist(textConcept);
		
		Concept codingHintConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Coding hint", effectiveTime);
		conceptMap.put("coding-hint", codingHintConcept);
		manager.persist(codingHintConcept);
		
		Concept definitionConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Definition", effectiveTime);
		conceptMap.put("definition", definitionConcept);
		manager.persist(definitionConcept);
		
		Concept introductionConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Introduction", effectiveTime);
		conceptMap.put("introduction", introductionConcept);
		manager.persist(introductionConcept);
		
		Concept modifierlinkConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Modifier link", effectiveTime);
		conceptMap.put("modifierlink", modifierlinkConcept);
		manager.persist(modifierlinkConcept);
		
		Concept asteriskRefsetConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Asterisk refset", effectiveTime);
		conceptMap.put("aster", asteriskRefsetConcept);
		manager.persist(asteriskRefsetConcept);

		Concept daggerRefsetConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Dagger refset", effectiveTime);
		conceptMap.put("dagger", daggerRefsetConcept);
		manager.persist(daggerRefsetConcept);
		
		Concept metadataConcept = createNewActiveConcept(new Integer(metadataCounter++).toString(),
				terminology, terminologyVersion, "Metadata", effectiveTime);
		conceptMap.put("metadata", metadataConcept);
		manager.persist(metadataConcept);
		
		//
		// Make relationships for metadata
		//
		createIsaRelationship(metadataConcept, asteriskRefsetConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(metadataConcept, daggerRefsetConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(metadataConcept, descriptionTypeConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(metadataConcept, relationshipTypeConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(metadataConcept, definitionStatusConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);		
		
		createIsaRelationship(metadataConcept, moduleConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(metadataConcept, caseSignificanceConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);			

		createIsaRelationship(metadataConcept, characteristicTypeConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(metadataConcept, modifierConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
	
		createIsaRelationship(definitionStatusConcept, defaultDefinitionStatusConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(moduleConcept, defaultModuleConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(caseSignificanceConcept, defaultCaseSignificanceConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(characteristicTypeConcept, defaultCharacteristicTypeConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(modifierConcept, defaultModifierConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(relationshipTypeConcept, isaConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(descriptionTypeConcept, footnoteConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);	
		
		createIsaRelationship(descriptionTypeConcept, textConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, codingHintConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, definitionConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, introductionConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, modifierlinkConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, noteConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, exclusionConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, inclusionConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, preferredLongConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, preferredAbbreviatedConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, preferredConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		createIsaRelationship(descriptionTypeConcept, considerConcept,
				new Integer(metadataCounter++).toString(), terminology,
				terminologyVersion, effectiveTime);
		
		tx.commit();
		
		return conceptMap;
	}
	
	/**
	 * Creates the new active concept and attached description.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param defaultPreferredName the default preferred name
	 * @param effectiveTime the effective time
	 * @return the concept
	 * @throws Exception the exception
	 */
	public Concept createNewActiveConcept(String terminologyId, String terminology,
		String terminologyVersion, String defaultPreferredName, String effectiveTime) throws Exception {
		Concept concept = new ConceptJpa();
		concept.setTerminologyId(terminologyId);
		concept.setTerminology(terminology);
		concept.setTerminologyVersion(terminologyVersion);
		concept.setEffectiveTime(dt.parse(effectiveTime));
		concept.setDefaultPreferredName(defaultPreferredName);
		concept.setActive(true);
		if (conceptMap.containsKey("defaultDefinitionStatus"))
		  concept.setDefinitionStatusId(new Long(conceptMap.get("defaultDefinitionStatus").getTerminologyId()));	
		if (conceptMap.containsKey("defaultModule"))
			concept.setModuleId(new Long(conceptMap.get("defaultModule").getTerminologyId()));		
		
		Description desc = new DescriptionJpa();
		desc.setTerminologyId(terminologyId);  
		desc.setEffectiveTime(dt.parse(effectiveTime));
		desc.setActive(true);
		if (conceptMap.containsKey("defaultModule"))
			desc.setModuleId(new Long(conceptMap.get("defaultModule")
				.getTerminologyId()));
		desc.setTerminology(terminology);
		desc.setTerminologyVersion(terminologyVersion);
		desc.setTerm(defaultPreferredName);
		desc.setConcept(concept);
		if (conceptMap.containsKey("defaultCaseSignificance"))
			desc.setCaseSignificanceId(new Long(conceptMap.get(
				"defaultCaseSignificance").getTerminologyId()));
		desc.setLanguageCode("en");
		if (conceptMap.containsKey("preferred")) 
		  desc.setTypeId(new Long(conceptMap.get("preferred")  
					.getTerminologyId()));
		
		concept.addDescription(desc);
			
		return concept;
	}
	
	/**
	 * Creates the isa relationship.
	 *
	 * @param parentConcept the parent concept
	 * @param childConcept the child concept
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param defaultPreferredName the default preferred name
	 * @param effectiveTime the effective time
	 * @throws Exception the exception
	 */
	public void createIsaRelationship(Concept parentConcept, Concept childConcept,
		String terminologyId, String terminology,
		String terminologyVersion, String effectiveTime) throws Exception {
		Relationship relationship = new RelationshipJpa();
		relationship.setTerminologyId(terminologyId);  
		relationship.setEffectiveTime(dt.parse(effectiveTime));
		relationship.setActive(true);
		relationship.setModuleId(new Long(conceptMap.get("defaultModule")
				.getTerminologyId()));
		relationship.setTerminology(terminology);
		relationship.setTerminologyVersion(terminologyVersion);
		relationship.setCharacteristicTypeId(new Long(conceptMap.get(
				"defaultCharacteristicType").getTerminologyId()));
		relationship.setModifierId(new Long(conceptMap.get(
				"defaultModifier").getTerminologyId()));
		relationship.setDestinationConcept(parentConcept);
		relationship.setSourceConcept(childConcept);
		relationship.setTypeId(new Long(conceptMap.get("isa")
				.getTerminologyId()));
		Set<Relationship> rels = new HashSet<Relationship>();
		rels.add(relationship);
		childConcept.setRelationships(rels);
	}
}
