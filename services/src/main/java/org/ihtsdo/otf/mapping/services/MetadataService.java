package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Map;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;


/**
 * Interface for services to retrieve (get) metadata objects.
 *
 * @author ${author}
 */
public interface MetadataService {
	
	public void close() throws Exception;
	
	public List<String> getTerminologies();
	public List<String> getVersions(String terminology);
	public String getLatestVersion(String terminology);
	public Map<String,String> getTerminologyLatestVersions();

	//////////////////////////////
	// Basic retrieval services //
	//////////////////////////////
	/**
	 * Returns the all metadata.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the all metadata
	 */
	public List<IdNameMap> getAllMetadata(String terminology, String version);
	
	/**
	 * Returns the modules.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the modules
	 */
	public IdNameMap getModules(String terminology, String version);
	
	/**
	 * Returns the attribute value ref sets.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the attribute value ref sets
	 */
	public IdNameMap getAttributeValueRefSets(String terminology, String version);

	/**
	 * Returns the complex map ref sets.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the complex map ref sets
	 */
	public IdNameMap getComplexMapRefSets(String terminology, String version);

	/**
	 * Returns the language refsets.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the language refsets
	 */
	public IdNameMap getLanguageRefSets(String terminology, String version);
	
	/**
	 * Returns the simple map refsets.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the simple map refsets
	 */
	public IdNameMap getSimpleMapRefSets(String terminology, String version);
	
	/**
	 * Returns the simple refsets.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the simple refsets
	 */
	public IdNameMap getSimpleRefSets(String terminology, String version);
	
	/**
	 * Returns the map relations.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the map relations
	 */
	public IdNameMap getMapRelations(String terminology, String version);
	
	/**
	 * Returns the definition statuses.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the definition statuses
	 */
	public IdNameMap getDefinitionStatuses(String terminology, String version);
 
	/**
	 * Returns the description types.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the description types
	 */
	public IdNameMap getDescriptionTypes(String terminology, String version);
  
	/**
	 * Returns the case significances.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the case significances
	 */
	public IdNameMap getCaseSignificances(String terminology, String version);
  
	/**
	 * Returns the relationship types.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the relationship types
	 */
	public IdNameMap getRelationshipTypes(String terminology, String version);
  
	/**
	 * Returns the relationship characteristic types.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the relationship characteristic types
	 */
	public IdNameMap getRelationshipCharacteristicTypes(String terminology, String version);
  
	/**
	 * Returns the relationship modifiers.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the relationship modifiers
	 */
	public IdNameMap getRelationshipModifiers(String terminology, String version);
	
	
}