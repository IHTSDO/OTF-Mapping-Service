package org.ihtsdo.otf.mapping.model;

import java.util.Set;


/**
 * Represents a map project.
 *
 * @author ${author}
 */
public interface MapProject  {

	/**
	 * Returns the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param id the id
	 */
	public void setId(Long id);
	
	/**
	 * Returns the id in string form.
	 *
	 * @return the string object id
	 */
	public String getObjectId();
	
	/**
	 * Returns the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Sets the name.
	 *
	 * @param name the name
	 */
	public void setName(String name);
	
	/**
	 * Indicates whether there is block structure for map records of this project.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isBlockStructure();
	
	/**
	 * Sets whether there is block structure for map records of this project.
	 *
	 * @param blockStructure the block structure
	 */
	public void setBlockStructure(boolean blockStructure);
	
	/**
	 * Indicates whether there is group structure for map records of this project.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isGroupStructure();
	
	/**
	 * Sets whether there is group structure for map records of this project.
	 *
	 * @param groupStructure the group structure
	 */
	public void setGroupStructure(boolean groupStructure);
	
	/**
	 * Indicates whether or not published is the case.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isPublished();
	
	/**
	 * Sets the published.
	 *
	 * @param published the published
	 */
	public void setPublished(boolean published);
	
	/**
	 * Returns the ref set id.
	 *
	 * @return the ref set id
	 */
	public String getRefSetId();
	
	/**
	 * Sets the ref set id.
	 *
	 * @param refSetId the ref set id
	 */
	public void setRefSetId(String refSetId);
	
	/**
	 * Returns the ref set name.
	 *
	 * @return the ref set name.
	 */
	public String getRefSetName();
	
	/**
	 * Sets the ref set name.
	 *
	 * @param refSetName the ref set name
	 */
	public void setRefSetName(String refSetName);
	
	/**
	 * Returns the set of allowable map advices.
	 *
	 * @return the map advices
	 */
	public Set<MapAdvice> getMapAdvices();
	
	/**
	 * Sets the set of allowable map advices.
	 *
	 * @param mapAdvices the map advices
	 */
	public void setMapAdvices(Set<MapAdvice> mapAdvices);
	
	/**
	 * Adds an allowable map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void addMapAdvice(MapAdvice mapAdvice);
	
	/**
	 * Removes an allowable map advice.
	 *
	 * @param mapAdvice the map advice
	 */
	public void removeMapAdvice(MapAdvice mapAdvice);
	
	/**
	 * Returns the set of allowable map principles.
	 *
	 * @return the map principles
	 */
	public Set<MapPrinciple> getMapPrinciples();
	
	/**
	 * Sets the set of allowable map principles.
	 *
	 * @param mapPrinciples the map principles
	 */
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples);
	
	/**
	 * Adds an allowable map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	public void addMapPrinciple(MapPrinciple mapPrinciple);
	
	/**
	 * Removes an allowable map principle.
	 *
	 * @param mapPrinciple the map principle
	 */
	public void removeMapPrinciple(MapPrinciple mapPrinciple);
	
	/**
	 * Returns the map leads.
	 *
	 * @return the map leads
	 */
	public Set<MapUser> getMapLeads();

	
	/**
	 * Sets the map leads.
	 *
	 * @param mapLeads the map leads
	 */
	public void setMapLeads(Set<MapUser> mapLeads);
	

	/**
	 * Adds the map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapUser mapLead);
	

	/**
	 * Removes the map lead.
	 *
	 * @param mapLead the map lead
	 */
	public void removeMapLead(MapUser mapLead);
	
	
	/**
	 * Returns the map specialists.
	 *
	 * @return the map specialists
	 */
	public Set<MapUser> getMapSpecialists();

	
	/**
	 * Sets the map specialists.
	 *
	 * @param mapSpecialists the map specialists
	 */
	public void setMapSpecialists(Set<MapUser> mapSpecialists);
	

	/**
	 * Adds the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapUser mapSpecialist);
	

	/**
	 * Removes the map specialist.
	 *
	 * @param mapSpecialist the map specialist
	 */
	public void removeMapSpecialist(MapUser mapSpecialist);
	

	/**
	 * Returns the source terminology.
	 *
	 * @return the source terminology
	 */
	public String getSourceTerminology();


	/**
	 * Sets the source terminology.
	 *
	 * @param sourceTerminology the source terminology
	 */
	public void setSourceTerminology(String sourceTerminology);
	

	/**
	 * Returns the destination terminology.
	 *
	 * @return the destination terminology
	 */
	public String getDestinationTerminology();


	/**
	 * Sets the destination terminology.
	 *
	 * @param destinationTerminology the destination terminology
	 */
	public void setDestinationTerminology(String destinationTerminology);
	

	/**
	 * Returns the source terminology version.
	 *
	 * @return the source terminology version
	 */
	public String getSourceTerminologyVersion();


	/**
	 * Sets the source terminology version.
	 *
	 * @param sourceTerminologyVersion the source terminology version
	 */
	public void setSourceTerminologyVersion(String sourceTerminologyVersion);
	
	/**
	 * Returns the destination terminology version.
	 *
	 * @return the destination terminology version
	 */
	public String getDestinationTerminologyVersion();


	/**
	 * Sets the destination terminology version.
	 *
	 * @param destinationTerminologyVersion the destination terminology version
	 */
	public void setDestinationTerminologyVersion(String destinationTerminologyVersion);
	
	
	/**
	 * Gets the map relation style.
	 *
	 * @return the map relation style
	 */
	public String getMapRelationStyle();

	/**
	 * Sets the map relation style.
	 *
	 * @param mapRelationStyle the map relation style
	 */
	public void setMapRelationStyle(String mapRelationStyle);


	/**
	 * Sets the map principle source document name.
	 *
	 * @param mapPrincipleSourceDocument the name of the map principle source document
	 */
	public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument);
	
	/**
	 * Gets the map principle source document name.
	 *
	 * @return the name of the map principle source document
	 */
	public String getMapPrincipleSourceDocument();

	/**
	 * Returns boolean flag for whether project is rule based.
	 *
	 * @return the boolean rule based
	 */
	public boolean isRuleBased();

	/**
	 * Sets the rule based flag with a boolean value.
	 *
	 * @param ruleBased the boolean rule based
	 */
	public void setRuleBased(boolean ruleBased);

	/**
	 * Returns the map behavior type (e.g. COMPLEX_MAP)
	 * @return the map type
	 */
	public String getMapRefsetPattern();
	
	/**
	 * Sets the map behavior type (e.g. COMPLEX_MAP)
	 * @param mapRefsetPattern the map type
	 */
	public void setMapRefsetPattern(String mapRefsetPattern);
		
	/**
	 * Returns the scope concepts.
	 *
	 * @return the scope concepts
	 */
	public Set<String> getScopeConcepts();
	
	/**
	 * Sets the scope concepts.
	 *
	 * @param scopeConcepts the scope concepts
	 */
	public void setScopeConcepts(Set<String> scopeConcepts);
	
	/**
	 * Indicates whether or not scope descendants flag is the case.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isScopeDescendantsFlag();
	
	/**
	 * Sets the scope descendants flag.
	 *
	 * @param flag the scope descendants flag
	 */
	public void setScopeDescendantsFlag(boolean flag);
	
	/**
	 * Returns the scope excluded concepts.
	 *
	 * @return the scope excluded concepts
	 */
	public Set<String> getScopeExcludedConcepts();
	
	/**
	 * Sets the scope excluded concepts.
	 *
	 * @param scopeExcludedConcepts the scope excluded concepts
	 */
	public void setScopeExcludedConcepts(Set<String> scopeExcludedConcepts);
	
	/**
	 * Indicates whether or not scope excluded descendants flag is the case.
	 *
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isScopeExcludedDescendantsFlag();
	
	/**
	 * Sets the scope excluded descendants flag.
	 *
	 * @param flag the scope excluded descendants flag
	 */
	public void setScopeExcludedDescendantsFlag(boolean flag);
	
	/**
	 * Gets the preset age ranges.
	 *
	 * @return the preset age ranges
	 */
	public Set<MapAgeRange> getPresetAgeRanges();
	
	/**
	 * Sets the preset age ranges.
	 *
	 * @param ageRanges the new preset age ranges
	 */
	public void setPresetAgeRanges(Set<MapAgeRange> ageRanges);
	
	/**
	 * Adds the preset age range.
	 *
	 * @param ageRange the age range
	 */
	public void addPresetAgeRange(MapAgeRange ageRange);
	
	/**
	 * Removes the preset age range.
	 *
	 * @param ageRange the age range
	 */
	public void removePresetAgeRange(MapAgeRange ageRange);

	/**
	 * Gets the map relations.
	 *
	 * @return the map relations
	 */
	public Set<MapRelation> getMapRelations();

	/**
	 * Sets the map relations.
	 *
	 * @param mapRelations the new map relations
	 */
	public void setMapRelations(Set<MapRelation> mapRelations);

	
	/**
	 * Adds the map relation.
	 *
	 * @param mr the mr
	 */
	public void addMapRelation(MapRelation mr);
	
	/**
	 * Removes the map relation.
	 *
	 * @param mr the mr
	 */
	public void removeMapRelation(MapRelation mr);

	/**
	 * Gets the project specific algorithm handler class.
	 *
	 * @return the project specific algorithm handler class
	 */
	public String getProjectSpecificAlgorithmHandlerClass();

	/**
	 * Sets the project specific algorithm handler class.
	 *
	 * @param projectSpecificAlgorithmHandlerClass the new project specific algorithm handler class
	 */
	public void setProjectSpecificAlgorithmHandlerClass(
			String projectSpecificAlgorithmHandlerClass);
	
}
