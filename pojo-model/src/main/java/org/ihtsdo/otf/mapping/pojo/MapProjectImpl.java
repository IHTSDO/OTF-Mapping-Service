package org.ihtsdo.otf.mapping.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapProject}.
 * 
 * @author ${author}
 */
public class MapProjectImpl implements MapProject {

	/** The id. */
	private Long id;

	/** The name. */
	private String name;

	/** Indicates whether this project is viewable by public roles. */
	private boolean isPublic;
	
	/**
	 * Indicates whether there is group structure for map records of this project.
	 */
	private boolean groupStructure = false;

	/** Indicates if the map project has been published. */
	private boolean published = false;

	/** The map leads working on this MapProject. */
	private Set<MapUser> mapLeads = new HashSet<>();

	/** The map specialists working on this MapProject. */
	private Set<MapUser> mapSpecialists = new HashSet<>();
	
	/** The map administrators working on this MapProject. */
	private Set<MapUser> mapAdministrators = new HashSet<>();

	/** The allowable map advices for this MapProject. */
	private Set<MapAdvice> mapAdvices = new HashSet<>();

	/** The allowable map principles for this MapProject. */
	private Set<MapPrinciple> mapPrinciples = new HashSet<>();

	/** The allowable map relations for this MapProject. */
	private Set<MapRelation> mapRelations = new HashSet<>();
	
	/** The report definitions available to this MapProject. */
	private Set<ReportDefinition> reportDefinitions = new HashSet<>();

	/** The ref set id. */
	private String refSetId;

	/** The ref set name. */
	private String refSetName;

	/** The source terminology. */
	private String sourceTerminology;

	/** The destination terminology. */
	private String destinationTerminology;

	/** The source terminology version. */
	private String sourceTerminologyVersion;

	/** The destination terminology version. */
	private String destinationTerminologyVersion;

	/** The relation style. */
	private RelationStyle mapRelationStyle;

	/** The document containing the map principles. */
	private String mapPrincipleSourceDocument;

	/** The name of the document containing the map principles. */
	private String mapPrincipleSourceDocumentName;

	/** Flag for whether the project is rule based. */
	private boolean ruleBased;

	/** The mapping behavior (i.e. SIMPLE_MAP, COMPLEX_MAP, EXTENDED_MAP) */
	private MapRefsetPattern mapRefsetPattern;

	/** The set of preset age ranges for rule generation. */
	private Set<MapAgeRange> presetAgeRanges = new HashSet<>();

	/** The scope concepts. */
	private Set<String> scopeConcepts = new HashSet<>();

	/** The scope excluded concepts. */
	private Set<String> scopeExcludedConcepts = new HashSet<>();

	/** The scope descendants flag. */
	private boolean scopeDescendantsFlag = false;

	/** The scope excluded descendants flag. */
	private boolean scopeExcludedDescendantsFlag = false;

	/** The name of the handler class for project specific algorithms. */
	private String projectSpecificAlgorithmHandlerClass;
	
	/**  The error messages. */
	private Set<String> errorMessages = new HashSet<>();
	
	/**  The propagation flag. */
	private boolean propagatedFlag = false;
	
	/**  The propagation descendant threshold. */
	private Integer propagationDescendantThreshold;

	/**
	 * The Enum WorkflowType.
	 */
	private WorkflowType workflowType = null;

	/**
	 * Return the id.
	 * 
	 * @return the id
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * Set the id.
	 * 
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the id in string form.
	 * 
	 * @return the id in string form
	 */
	@Override
	public String getObjectId() {
		return id.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
	 */
	/**
	 * Gets the map leads.
	 *
	 * @return the map leads
	 */
	@Override
	public Set<MapUser> getMapLeads() {
		return mapLeads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
	 */
	/**
	 * Sets the map leads.
	 *
	 * @param mapLeads the new map leads
	 */
	@Override
	public void setMapLeads(Set<MapUser> mapLeads) {
		this.mapLeads = mapLeads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapLead(org.ihtsdo.otf.mapping
	 * .model.MapLead)
	 */
	/**
	 * Adds the map lead.
	 *
	 * @param mapLead the map lead
	 */
	@Override
	public void addMapLead(MapUser mapLead) {
		mapLeads.add(mapLead);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removeMapLead(org.ihtsdo.otf.mapping
	 * .model.MapLead)
	 */
	/**
	 * Removes the map lead.
	 *
	 * @param mapLead the map lead
	 */
	@Override
	public void removeMapLead(MapUser mapLead) {
		mapLeads.remove(mapLead);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
	 */
	/**
	 * Gets the map specialists.
	 *
	 * @return the map specialists
	 */
	@Override
	public Set<MapUser> getMapSpecialists() {
		return mapSpecialists;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapSpecialists(java.util.Set)
	 */
	/**
	 * Sets the map specialists.
	 *
	 * @param mapSpecialists the new map specialists
	 */
	@Override
	public void setMapSpecialists(Set<MapUser> mapSpecialists) {
		this.mapSpecialists = mapSpecialists;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapSpecialist(org.ihtsdo.otf
	 * .mapping.model.MapSpecialist)
	 */
	/**
	 * Adds the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	@Override
	public void addMapSpecialist(MapUser mapSpecialist) {
		mapSpecialists.add(mapSpecialist);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removeMapSpecialist(org.ihtsdo.
	 * otf.mapping.model.MapSpecialist)
	 */
	/**
	 * Removes the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	@Override
	public void removeMapSpecialist(MapUser mapSpecialist) {
		mapSpecialists.remove(mapSpecialist);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminology()
	 */
	/**
	 * Gets the source terminology.
	 *
	 * @return the source terminology
	 */
	@Override
	public String getSourceTerminology() {
		return sourceTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminology(java.lang.
	 * String)
	 */
	/**
	 * Sets the source terminology.
	 *
	 * @param sourceTerminology the new source terminology
	 */
	@Override
	public void setSourceTerminology(String sourceTerminology) {
		this.sourceTerminology = sourceTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminology()
	 */
	/**
	 * Gets the destination terminology.
	 *
	 * @return the destination terminology
	 */
	@Override
	public String getDestinationTerminology() {
		return destinationTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminology(java.
	 * lang.String)
	 */
	/**
	 * Sets the destination terminology.
	 *
	 * @param destinationTerminology the new destination terminology
	 */
	@Override
	public void setDestinationTerminology(String destinationTerminology) {
		this.destinationTerminology = destinationTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminologyVersion()
	 */
	/**
	 * Gets the source terminology version.
	 *
	 * @return the source terminology version
	 */
	@Override
	public String getSourceTerminologyVersion() {
		return sourceTerminologyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminologyVersion(java
	 * .lang.String)
	 */
	/**
	 * Sets the source terminology version.
	 *
	 * @param sourceTerminologyVersion the new source terminology version
	 */
	@Override
	public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
		this.sourceTerminologyVersion = sourceTerminologyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminologyVersion()
	 */
	/**
	 * Gets the destination terminology version.
	 *
	 * @return the destination terminology version
	 */
	@Override
	public String getDestinationTerminologyVersion() {
		return destinationTerminologyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminologyVersion
	 * (java.lang.String)
	 */
	/**
	 * Sets the destination terminology version.
	 *
	 * @param destinationTerminologyVersion the new destination terminology version
	 */
	@Override
	public void setDestinationTerminologyVersion(
			String destinationTerminologyVersion) {
		this.destinationTerminologyVersion = destinationTerminologyVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getName()
	 */
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setName(java.lang.String)
	 */
	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isGroupStructure()
	 */
	/**
	 * Checks if is group structure.
	 *
	 * @return true, if is group structure
	 */
	@Override
	public boolean isGroupStructure() {
		return groupStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setGroupStructure(boolean)
	 */
	/**
	 * Sets the group structure.
	 *
	 * @param groupStructure the new group structure
	 */
	@Override
	public void setGroupStructure(boolean groupStructure) {
		this.groupStructure = groupStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdvices()
	 */
	/**
	 * Gets the map advices.
	 *
	 * @return the map advices
	 */
	@Override
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapAdvices(java.util.Set)
	 */
	/**
	 * Sets the map advices.
	 *
	 * @param mapAdvices the new map advices
	 */
	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapAdvice(org.ihtsdo.otf.mapping
	 * .model.MapAdvice)
	 */
	/**
	 * Adds the map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		this.mapAdvices.add(mapAdvice);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removeMapAdvice(org.ihtsdo.otf.
	 * mapping.model.MapAdvice)
	 */
	/**
	 * Removes the map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		this.mapAdvices.remove(mapAdvice);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrinciples()
	 */
	/**
	 * Gets the map principles.
	 *
	 * @return the map principles
	 */
	@Override
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapPrinciples(java.util.Set)
	 */
	/**
	 * Sets the map principles.
	 *
	 * @param mapPrinciples the new map principles
	 */
	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapPrinciple(org.ihtsdo.otf.
	 * mapping.model.MapPrinciple)
	 */
	/**
	 * Adds the map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removeMapPrinciple(org.ihtsdo.otf
	 * .mapping.model.MapPrinciple)
	 */
	/**
	 * Removes the map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPublished()
	 */
	/**
	 * Checks if is published.
	 *
	 * @return true, if is published
	 */
	@Override
	public boolean isPublished() {
		return published;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPublished(boolean)
	 */
	/**
	 * Sets the published.
	 *
	 * @param published the new published
	 */
	@Override
	public void setPublished(boolean published) {
		this.published = published;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetId()
	 */
	/**
	 * Gets the ref set id.
	 *
	 * @return the ref set id
	 */
	@Override
	public String getRefSetId() {
		return refSetId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetId(java.lang.Long)
	 */
	/**
	 * Sets the ref set id.
	 *
	 * @param refSetId the new ref set id
	 */
	@Override
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetName()
	 */
	/**
	 * Gets the ref set name.
	 *
	 * @return the ref set name
	 */
	@Override
	public String getRefSetName() {
		return this.refSetName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setRefSetName(java.lang.String)
	 */
	/**
	 * Sets the ref set name.
	 *
	 * @param refSetName the new ref set name
	 */
	@Override
	public void setRefSetName(String refSetName) {
		this.refSetName = refSetName;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelationStyle()
	 */
	/**
	 * Gets the map relation style.
	 *
	 * @return the map relation style
	 */
	@Override
	public RelationStyle getMapRelationStyle() {
		return mapRelationStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapRelationStyle(java.lang.String
	 * )
	 */
	/**
	 * Sets the map relation style.
	 *
	 * @param mapRelationStyle the new map relation style
	 */
	@Override
	public void setMapRelationStyle(RelationStyle mapRelationStyle) {
		this.mapRelationStyle = mapRelationStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapRelation(org.ihtsdo.otf.mapping
	 * .model.MapRelation)
	 */
	/**
	 * Adds the map relation.
	 *
	 * @param mr the mr
	 */
	@Override
	public void addMapRelation(MapRelation mr) {
		this.mapRelations.add(mr);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removeMapRelation(org.ihtsdo.otf
	 * .mapping.model.MapRelation)
	 */
	/**
	 * Removes the map relation.
	 *
	 * @param mr the mr
	 */
	@Override
	public void removeMapRelation(MapRelation mr) {
		this.mapRelations.remove(mr);

	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isRuleBased()
	 */
	/**
	 * Checks if is rule based.
	 *
	 * @return true, if is rule based
	 */
	@Override
	public boolean isRuleBased() {
		return ruleBased;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRuleBased(boolean)
	 */
	/**
	 * Sets the rule based.
	 *
	 * @param ruleBased the new rule based
	 */
	@Override
	public void setRuleBased(boolean ruleBased) {
		this.ruleBased = ruleBased;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRefsetPattern()
	 */
	/**
	 * Gets the map refset pattern.
	 *
	 * @return the map refset pattern
	 */
	@Override
	public MapRefsetPattern getMapRefsetPattern() {
		return mapRefsetPattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapRefsetPattern(java.lang.String
	 * )
	 */
	/**
	 * Sets the map refset pattern.
	 *
	 * @param mapRefsetPattern the new map refset pattern
	 */
	@Override
	public void setMapRefsetPattern(MapRefsetPattern mapRefsetPattern) {
		this.mapRefsetPattern = mapRefsetPattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeConcepts()
	 */
	/**
	 * Gets the scope concepts.
	 *
	 * @return the scope concepts
	 */
	@Override
	public Set<String> getScopeConcepts() {
		return scopeConcepts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setScopeConcepts(java.util.Set)
	 */
	/**
	 * Sets the scope concepts.
	 *
	 * @param scopeConcepts the new scope concepts
	 */
	@Override
	public void setScopeConcepts(Set<String> scopeConcepts) {
		this.scopeConcepts = scopeConcepts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isScopeDescendantsFlag()
	 */
	/**
	 * Checks if is scope descendants flag.
	 *
	 * @return true, if is scope descendants flag
	 */
	@Override
	public boolean isScopeDescendantsFlag() {
		return scopeDescendantsFlag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setScopeDescendantsFlag(boolean)
	 */
	/**
	 * Sets the scope descendants flag.
	 *
	 * @param flag the new scope descendants flag
	 */
	@Override
	public void setScopeDescendantsFlag(boolean flag) {
		scopeDescendantsFlag = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeExcludedConcepts()
	 */
	/**
	 * Gets the scope excluded concepts.
	 *
	 * @return the scope excluded concepts
	 */
	@Override
	public Set<String> getScopeExcludedConcepts() {
		return scopeExcludedConcepts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedConcepts(java.util
	 * .Set)
	 */
	/**
	 * Sets the scope excluded concepts.
	 *
	 * @param scopeExcludedConcepts the new scope excluded concepts
	 */
	@Override
	public void setScopeExcludedConcepts(Set<String> scopeExcludedConcepts) {
		this.scopeExcludedConcepts = scopeExcludedConcepts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#isScopeExcludedDescendantsFlag()
	 */
	/**
	 * Checks if is scope excluded descendants flag.
	 *
	 * @return true, if is scope excluded descendants flag
	 */
	@Override
	public boolean isScopeExcludedDescendantsFlag() {
		return scopeExcludedDescendantsFlag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedDescendantsFlag
	 * (boolean)
	 */
	/**
	 * Sets the scope excluded descendants flag.
	 *
	 * @param flag the new scope excluded descendants flag
	 */
	@Override
	public void setScopeExcludedDescendantsFlag(boolean flag) {
		scopeExcludedDescendantsFlag = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getPresetAgeRanges()
	 */
	/**
	 * Gets the preset age ranges.
	 *
	 * @return the preset age ranges
	 */
	@Override
	public Set<MapAgeRange> getPresetAgeRanges() {
		return this.presetAgeRanges;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setPresetAgeRanges(java.util.Set)
	 */
	/**
	 * Sets the preset age ranges.
	 *
	 * @param ageRanges the new preset age ranges
	 */
	@Override
	public void setPresetAgeRanges(Set<MapAgeRange> ageRanges) {
		this.presetAgeRanges = ageRanges;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addPresetAgeRange(org.ihtsdo.otf
	 * .mapping.model.MapAgeRange)
	 */
	/**
	 * Adds the preset age range.
	 *
	 * @param ageRange the age range
	 */
	@Override
	public void addPresetAgeRange(MapAgeRange ageRange) {
		this.presetAgeRanges.add(ageRange);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#removePresetAgeRange(org.ihtsdo
	 * .otf.mapping.model.MapAgeRange)
	 */
	/**
	 * Removes the preset age range.
	 *
	 * @param ageRange the age range
	 */
	@Override
	public void removePresetAgeRange(MapAgeRange ageRange) {
		this.presetAgeRanges.remove(ageRange);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelations()
	 */
	/**
	 * Gets the map relations.
	 *
	 * @return the map relations
	 */
	@Override
	public Set<MapRelation> getMapRelations() {
		return mapRelations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapRelations(java.util.Set)
	 */
	/**
	 * Sets the map relations.
	 *
	 * @param mapRelations the new map relations
	 */
	@Override
	public void setMapRelations(Set<MapRelation> mapRelations) {
		this.mapRelations = mapRelations;
	}

	/**
	 * Gets the project specific algorithm handler class.
	 *
	 * @return the project specific algorithm handler class
	 */
	@Override
	public String getProjectSpecificAlgorithmHandlerClass() {
		return projectSpecificAlgorithmHandlerClass;
	}

	/**
	 * Sets the project specific algorithm handler class.
	 *
	 * @param projectSpecificAlgorithmHandlerClass the new project specific algorithm handler class
	 */
	@Override
	public void setProjectSpecificAlgorithmHandlerClass(
			String projectSpecificAlgorithmHandlerClass) {
		this.projectSpecificAlgorithmHandlerClass =
				projectSpecificAlgorithmHandlerClass;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime
				* result
				+ ((destinationTerminology == null) ? 0 : destinationTerminology
						.hashCode());
		result =
				prime
				* result
				+ ((destinationTerminologyVersion == null) ? 0
						: destinationTerminologyVersion.hashCode());
		result = prime * result + (groupStructure ? 1231 : 1237);
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapLeads == null) ? 0 : mapLeads.hashCode());
		result = prime * result + ((mapAdministrators == null) ? 0 : mapAdministrators.hashCode());
		result =
				prime
				* result
				+ ((mapPrincipleSourceDocument == null) ? 0
						: mapPrincipleSourceDocument.hashCode());
		result =
				prime * result
				+ ((mapPrinciples == null) ? 0 : mapPrinciples.hashCode());
		result =
				prime * result
				+ ((mapRefsetPattern == null) ? 0 : mapRefsetPattern.hashCode());
		result =
				prime * result
				+ ((mapRelationStyle == null) ? 0 : mapRelationStyle.hashCode());
		result =
				prime * result + ((mapRelations == null) ? 0 : mapRelations.hashCode());
		result =
				prime * result
				+ ((mapSpecialists == null) ? 0 : mapSpecialists.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result =
				prime * result
				+ ((presetAgeRanges == null) ? 0 : presetAgeRanges.hashCode());
		result = prime * result + (published ? 1231 : 1237);
		result = prime * result + ((refSetId == null) ? 0 : refSetId.hashCode());
		result =
				prime * result + ((refSetName == null) ? 0 : refSetName.hashCode());
		result = prime * result + (ruleBased ? 1231 : 1237);
		result =
				prime * result
				+ ((scopeConcepts == null) ? 0 : scopeConcepts.hashCode());
		result = prime * result + (scopeDescendantsFlag ? 1231 : 1237);
		result =
				prime
				* result
				+ ((scopeExcludedConcepts == null) ? 0 : scopeExcludedConcepts
						.hashCode());
		result = prime * result + (scopeExcludedDescendantsFlag ? 1231 : 1237);
		result =
				prime * result
				+ ((sourceTerminology == null) ? 0 : sourceTerminology.hashCode());
		result =
				prime
				* result
				+ ((sourceTerminologyVersion == null) ? 0
						: sourceTerminologyVersion.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapProjectImpl other = (MapProjectImpl) obj;
		if (destinationTerminology == null) {
			if (other.destinationTerminology != null)
				return false;
		} else if (!destinationTerminology.equals(other.destinationTerminology))
			return false;
		if (destinationTerminologyVersion == null) {
			if (other.destinationTerminologyVersion != null)
				return false;
		} else if (!destinationTerminologyVersion
				.equals(other.destinationTerminologyVersion))
			return false;
		if (groupStructure != other.groupStructure)
			return false;
		if (mapAdvices == null) {
			if (other.mapAdvices != null)
				return false;
		} else if (!mapAdvices.equals(other.mapAdvices))
			return false;
		if (mapLeads == null) {
			if (other.mapLeads != null)
				return false;
		} else if (!mapLeads.equals(other.mapLeads))
			return false;
		if (mapPrincipleSourceDocument == null) {
			if (other.mapPrincipleSourceDocument != null)
				return false;
		} else if (!mapPrincipleSourceDocument
				.equals(other.mapPrincipleSourceDocument))
			return false;
		if (mapPrinciples == null) {
			if (other.mapPrinciples != null)
				return false;
		} else if (!mapPrinciples.equals(other.mapPrinciples))
			return false;
		if (mapRefsetPattern == null) {
			if (other.mapRefsetPattern != null)
				return false;
		} else if (!mapRefsetPattern.equals(other.mapRefsetPattern))
			return false;
		if (mapRelationStyle == null) {
			if (other.mapRelationStyle != null)
				return false;
		} else if (!mapRelationStyle.equals(other.mapRelationStyle))
			return false;
		if (mapRelations == null) {
			if (other.mapRelations != null)
				return false;
		} else if (!mapRelations.equals(other.mapRelations))
			return false;
		if (mapSpecialists == null) {
			if (other.mapSpecialists != null)
				return false;
		} else if (!mapSpecialists.equals(other.mapSpecialists))
			return false;
		if (mapAdministrators == null) {
			if (other.mapAdministrators != null)
				return false;
		} else if (!mapAdministrators.equals(other.mapAdministrators))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (presetAgeRanges == null) {
			if (other.presetAgeRanges != null)
				return false;
		} else if (!presetAgeRanges.equals(other.presetAgeRanges))
			return false;
		if (published != other.published)
			return false;
		if (refSetId == null) {
			if (other.refSetId != null)
				return false;
		} else if (!refSetId.equals(other.refSetId))
			return false;
		if (refSetName == null) {
			if (other.refSetName != null)
				return false;
		} else if (!refSetName.equals(other.refSetName))
			return false;
		if (ruleBased != other.ruleBased)
			return false;
		if (scopeConcepts == null) {
			if (other.scopeConcepts != null)
				return false;
		} else if (!scopeConcepts.equals(other.scopeConcepts))
			return false;
		if (scopeDescendantsFlag != other.scopeDescendantsFlag)
			return false;
		if (scopeExcludedConcepts == null) {
			if (other.scopeExcludedConcepts != null)
				return false;
		} else if (!scopeExcludedConcepts.equals(other.scopeExcludedConcepts))
			return false;
		if (scopeExcludedDescendantsFlag != other.scopeExcludedDescendantsFlag)
			return false;
		if (sourceTerminology == null) {
			if (other.sourceTerminology != null)
				return false;
		} else if (!sourceTerminology.equals(other.sourceTerminology))
			return false;
		if (sourceTerminologyVersion == null) {
			if (other.sourceTerminologyVersion != null)
				return false;
		} else if (!sourceTerminologyVersion.equals(other.sourceTerminologyVersion))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return "MapProjectImpl [name=" + name + ", groupStructure=" + groupStructure
				+ ", published=" + published + ", mapLeads=" + mapLeads
				+ ", mapAdministrators=" + mapAdministrators
				+ ", mapSpecialists=" + mapSpecialists + ", mapAdvices=" + mapAdvices
				+ ", mapPrinciples=" + mapPrinciples + ", mapRelations=" + mapRelations
				+ ", refSetId=" + refSetId + ", refSetName=" + refSetName
				+ ", sourceTerminology=" + sourceTerminology
				+ ", destinationTerminology=" + destinationTerminology
				+ ", sourceTerminologyVersion=" + sourceTerminologyVersion
				+ ", destinationTerminologyVersion=" + destinationTerminologyVersion
				+ ", mapRelationStyle=" + mapRelationStyle
				+ ", mapPrincipleSourceDocument=" + mapPrincipleSourceDocument
				+ ", ruleBased=" + ruleBased + ", mapRefsetPattern=" + mapRefsetPattern
				+ ", presetAgeRanges=" + presetAgeRanges + ", scopeConcepts="
				+ scopeConcepts + ", scopeExcludedConcepts=" + scopeExcludedConcepts
				+ ", scopeDescendantsFlag=" + scopeDescendantsFlag
				+ ", scopeExcludedDescendantsFlag=" + scopeExcludedDescendantsFlag
				+ "]";
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPublic()
	 */
	/**
	 * Checks if is public.
	 *
	 * @return true, if is public
	 */
	@Override
	public boolean isPublic() {
		return isPublic;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPublic(boolean)
	 */
	/**
	 * Sets the public.
	 *
	 * @param isPublic the new public
	 */
	@Override
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	/**
	 * Gets the map administrators.
	 *
	 * @return the map administrators
	 */
	@Override
	public Set<MapUser> getMapAdministrators() {
		return mapAdministrators;
	}

	/**
	 * Sets the map administrators.
	 *
	 * @param mapAdministrators the new map administrators
	 */
	@Override
	public void setMapAdministrators(Set<MapUser> mapAdministrators) {
		this.mapAdministrators = mapAdministrators;
	}

	/**
	 * Adds the map administrator.
	 *
	 * @param mapAdministrator the map administrator
	 */
	@Override
	public void addMapAdministrator(MapUser mapAdministrator) {
		mapAdministrators.add(mapAdministrator);
	}

	/**
	 * Removes the map administrator.
	 *
	 * @param mapAdministrator the map administrator
	 */
	@Override
	public void removeMapAdministrator(MapUser mapAdministrator) {
		mapAdministrators.remove(mapAdministrator);
	}

	/**
	 * Gets the workflow type.
	 *
	 * @return the workflow type
	 */
	@Override
	public WorkflowType getWorkflowType() {
		return this.workflowType;
	}

	/**
	 * Sets the workflow type.
	 *
	 * @param workflowType the new workflow type
	 */
	@Override
	public void setWorkflowType(WorkflowType workflowType) {
		this.workflowType = workflowType;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrincipleSourceDocumentName()
	 */
	@Override
	public String getMapPrincipleSourceDocumentName() {
		return this.mapPrincipleSourceDocumentName;
		
	}
		
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapPrincipleSourceDocumentName(java.lang.String)
	 */
	@Override
	public void setMapPrincipleSourceDocumentName(
		String mapPrincipleSourceDocumentName) {
		this.mapPrincipleSourceDocumentName = mapPrincipleSourceDocumentName;
		
	}

	/**
	 * Adds the scope excluded concept.
	 *
	 * @param terminologyId the terminology id
	 */
	@Override
	public void addScopeExcludedConcept(String terminologyId) {
		this.scopeExcludedConcepts.add(terminologyId);
		
	}

	/**
	 * Removes the scope excluded concept.
	 *
	 * @param terminologyId the terminology id
	 */
	@Override
	public void removeScopeExcludedConcept(String terminologyId) {
		this.scopeExcludedConcepts.remove(terminologyId);
		
	}

	/**
	 * Adds the scope concept.
	 *
	 * @param terminologyId the terminology id
	 */
	@Override
	public void addScopeConcept(String terminologyId) {
		this.scopeConcepts.add(terminologyId);
		
	}

	/**
	 * Removes the scope concept.
	 *
	 * @param terminologyId the terminology id
	 */
	@Override
	public void removeScopeConcept(String terminologyId) {
		this.scopeConcepts.remove(terminologyId);
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapPrincipleSourceDocument(java.lang.String)
	 */
	@Override
	public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument) {
		this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrincipleSourceDocument()
	 */
	@Override
	public String getMapPrincipleSourceDocument() {
		return mapPrincipleSourceDocument;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getErrorMessages()
	 */
	@Override
	public Set<String> getErrorMessages() {
		return errorMessages;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setErrorMessages(java.util.Set)
	 */
	@Override
	public void setErrorMessages(Set<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getPropagationDescendantThreshold()
	 */
	@Override
	public Integer getPropagationDescendantThreshold() {
		return propagationDescendantThreshold;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPropagationDescendantThreshold(java.lang.Integer)
	 */
	@Override
	public void setPropagationDescendantThreshold(
		Integer propagationDescendantThreshold) {
		this.propagationDescendantThreshold = propagationDescendantThreshold;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPropagatedFlag()
	 */
	@Override
	public boolean isPropagatedFlag() {
		return propagatedFlag;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPropagatedFlag(boolean)
	 */
	@Override
	public void setPropagatedFlag(boolean propagatedFlag) {
		this.propagatedFlag = propagatedFlag;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getReportDefinitions()
	 */
	@Override
	public Set<ReportDefinition> getReportDefinitions() {
		return reportDefinitions;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setReportDefinitions(java.util.Set)
	 */
	@Override
	public void setReportDefinitions(Set<ReportDefinition> reportDefinitions) {
		this.reportDefinitions = reportDefinitions;
	}
	
	
}
