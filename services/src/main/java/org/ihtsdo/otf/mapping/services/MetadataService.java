package org.ihtsdo.otf.mapping.services;

import java.util.Map;



/**
 * Interface for services to retrieve (get) metadata objects
 */
public interface MetadataService {
	
	//////////////////////////////
	// Basic retrieval services //
	//////////////////////////////
	public Map<Long,String> getAllMetadata(String terminology, String version);
	
	public Map<Long,String> getModules(String terminology, String version);
	
	public Map<Long,String> getAttributeValueRefSets(String terminology, String version);

	public Map<Long,String> getComplexMapRefSets(String terminology, String version);

	public Map<Long,String> getLanguageRefsets(String terminology, String version);
	
	public Map<Long,String> getSimpleMapRefsets(String terminology, String version);
	
	public Map<Long,String> getSimpleRefsets(String terminology, String version);
	
	public Map<Long,String> getMapRelations(String terminology, String version);
	
	public Map<Long,String> getDefinitionStatuses(String terminology, String version);
 
	public Map<Long,String> getDescriptionTypes(String terminology, String version);
  
	public Map<Long,String> getCaseSignificances(String terminology, String version);
  
	public Map<Long,String> getRelationshipTypes(String terminology, String version);
  
	public Map<Long,String> getRelationshipCharacteristicTypes(String terminology, String version);
  
	public Map<Long,String> getRelationshipModifiers(String terminology, String version);
	
	
}