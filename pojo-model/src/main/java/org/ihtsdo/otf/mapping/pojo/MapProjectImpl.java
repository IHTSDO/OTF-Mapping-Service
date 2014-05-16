package org.ihtsdo.otf.mapping.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;

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

	/** Indicates whether this project is viewable by public roles */
	private boolean isPublic;
	
	/**
	 * Indicates whether there is block structure for map records of this project.
	 */
	private boolean blockStructure = false;

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

	/** The allowable map advices for this MapProject. */
	private Set<MapAdvice> mapAdvices = new HashSet<>();

	/** The allowable map principles for this MapProject. */
	private Set<MapPrinciple> mapPrinciples = new HashSet<>();

	/** The allowable map relations for this MapProject. */
	private Set<MapRelation> mapRelations = new HashSet<>();

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
	private String mapRelationStyle;

	/** The name of the document containing the map principles. */
	private String mapPrincipleSourceDocument;

	/** Flag for whether the project is rule based. */
	private boolean ruleBased;

	/** The mapping behavior (i.e. SIMPLE_MAP, COMPLEX_MAP, EXTENDED_MAP) */
	private String mapRefsetPattern;

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

	/** The name of the handler class for project specific algorithms */
	private String projectSpecificAlgorithmHandlerClass;

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
	@Override
	public Set<MapUser> getMapLeads() {
		return mapLeads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
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
	@Override
	public void removeMapLead(MapUser mapLead) {
		mapLeads.remove(mapLead);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
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
	@Override
	public void removeMapSpecialist(MapUser mapSpecialist) {
		mapSpecialists.remove(mapSpecialist);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminology()
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
	@Override
	public void setSourceTerminology(String sourceTerminology) {
		this.sourceTerminology = sourceTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminology()
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
	@Override
	public void setDestinationTerminology(String destinationTerminology) {
		this.destinationTerminology = destinationTerminology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminologyVersion()
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
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isBlockStructure()
	 */
	@Override
	public boolean isBlockStructure() {
		return blockStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setBlockStructure(boolean)
	 */
	@Override
	public void setBlockStructure(boolean blockStructure) {
		this.blockStructure = blockStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isGroupStructure()
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
	@Override
	public void setGroupStructure(boolean groupStructure) {
		this.groupStructure = groupStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdvices()
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
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		this.mapAdvices.remove(mapAdvice);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrinciples()
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
	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPublished()
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
	@Override
	public void setPublished(boolean published) {
		this.published = published;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetId()
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
	@Override
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetName()
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
	@Override
	public void setRefSetName(String refSetName) {
		this.refSetName = refSetName;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelationStyle()
	 */
	@Override
	public String getMapRelationStyle() {
		return mapRelationStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapRelationStyle(java.lang.String
	 * )
	 */
	@Override
	public void setMapRelationStyle(String mapRelationStyle) {
		this.mapRelationStyle = mapRelationStyle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#addMapRelation(org.ihtsdo.otf.mapping
	 * .model.MapRelation)
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
	@Override
	public void removeMapRelation(MapRelation mr) {
		this.mapRelations.remove(mr);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#getMapPrincipleSourceDocument()
	 */
	@Override
	public String getMapPrincipleSourceDocument() {
		return mapPrincipleSourceDocument;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapPrincipleSourceDocument(java
	 * .lang.String)
	 */
	@Override
	public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument) {
		this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isRuleBased()
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
	@Override
	public void setRuleBased(boolean ruleBased) {
		this.ruleBased = ruleBased;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRefsetPattern()
	 */
	@Override
	public String getMapRefsetPattern() {
		return mapRefsetPattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapRefsetPattern(java.lang.String
	 * )
	 */
	@Override
	public void setMapRefsetPattern(String mapRefsetPattern) {
		this.mapRefsetPattern = mapRefsetPattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeConcepts()
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
	@Override
	public void setScopeConcepts(Set<String> scopeConcepts) {
		this.scopeConcepts = scopeConcepts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isScopeDescendantsFlag()
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
	@Override
	public void setScopeDescendantsFlag(boolean flag) {
		scopeDescendantsFlag = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeExcludedConcepts()
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
	@Override
	public void setScopeExcludedDescendantsFlag(boolean flag) {
		scopeExcludedDescendantsFlag = flag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getPresetAgeRanges()
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
	@Override
	public void removePresetAgeRange(MapAgeRange ageRange) {
		this.presetAgeRanges.remove(ageRange);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelations()
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
	@Override
	public void setMapRelations(Set<MapRelation> mapRelations) {
		this.mapRelations = mapRelations;
	}

	@Override
	public String getProjectSpecificAlgorithmHandlerClass() {
		return projectSpecificAlgorithmHandlerClass;
	}

	@Override
	public void setProjectSpecificAlgorithmHandlerClass(
			String projectSpecificAlgorithmHandlerClass) {
		this.projectSpecificAlgorithmHandlerClass =
				projectSpecificAlgorithmHandlerClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (blockStructure ? 1231 : 1237);
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapProjectImpl other = (MapProjectImpl) obj;
		if (blockStructure != other.blockStructure)
			return false;
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

	@Override
	public String toString() {
		return "MapProjectImpl [name=" + name + ", blockStructure="
				+ blockStructure + ", groupStructure=" + groupStructure
				+ ", published=" + published + ", mapLeads=" + mapLeads
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
	@Override
	public boolean isPublic() {
		return isPublic;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPublic(boolean)
	 */
	@Override
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}
