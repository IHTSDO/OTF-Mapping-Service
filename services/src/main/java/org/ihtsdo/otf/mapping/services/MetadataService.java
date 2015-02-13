package org.ihtsdo.otf.mapping.services;

import java.util.List;
import java.util.Map;

/**
 * Generically represents a service to retrieve metadata objects.
 */
public interface MetadataService extends RootService {

  /**
   * Close the service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception;

  /**
   * Returns the terminologies.
   * 
   * @return the terminologies
   * @throws Exception if anything goes wrong
   */
  public List<String> getTerminologies() throws Exception;

  /**
   * Returns the versions.
   * 
   * @param terminology the terminology
   * @return the versions
   * @throws Exception if anything goes wrong
   */
  public List<String> getVersions(String terminology) throws Exception;

  /**
   * Returns the latest version.
   * 
   * @param terminology the terminology
   * @return the latest version
   * @throws Exception if anything goes wrong
   */
  public String getLatestVersion(String terminology) throws Exception;

  /**
   * Returns the previous version.
   *
   * @param terminology the terminology
   * @return the previous version
   * @throws Exception the exception
   */
  public String getPreviousVersion(String terminology) throws Exception;

  /**
   * Returns the terminology latest versions.
   * 
   * @return the terminology latest versions
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getTerminologyLatestVersions() throws Exception;

  // ////////////////////////////
  // Basic retrieval services //
  // ////////////////////////////
  /**
   * Returns the all metadata.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return all metadata
   * @throws Exception if anything goes wrong
   */
  public Map<String, Map<String, String>> getAllMetadata(String terminology,
    String version) throws Exception;

  /**
   * Returns the modules.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the modules
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getModules(String terminology, String version)
    throws Exception;

  /**
   * Returns the attribute value ref sets.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the attribute value ref sets
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getAttributeValueRefSets(String terminology,
    String version) throws Exception;

  /**
   * Returns the complex map ref sets.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the complex map ref sets
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getComplexMapRefSets(String terminology,
    String version) throws Exception;

  /**
   * Returns the language refsets.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the language refsets
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getLanguageRefSets(String terminology,
    String version) throws Exception;

  /**
   * Returns the simple map refsets.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the simple map refsets
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getSimpleMapRefSets(String terminology,
    String version) throws Exception;

  /**
   * Returns the simple refsets.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the simple refsets
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getSimpleRefSets(String terminology, String version)
    throws Exception;

  /**
   * Returns the map relations.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the map relations
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getMapRelations(String terminology, String version)
    throws Exception;

  /**
   * Returns the definition statuses.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the definition statuses
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getDefinitionStatuses(String terminology,
    String version) throws Exception;

  /**
   * Returns the description types.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the description types
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getDescriptionTypes(String terminology,
    String version) throws Exception;

  /**
   * Returns the case significances.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the case significances
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getCaseSignificances(String terminology,
    String version) throws Exception;

  /**
   * Returns the relationship types.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the relationship types
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getRelationshipTypes(String terminology,
    String version) throws Exception;

  /**
   * Returns the hierarchical relationship types. The idea is that these
   * relationship types define "parent" and "child" relationships. When looking
   * through a concept's relationships, anything with one of these types means
   * the destinationId is a "parent". When looking through a concept's inverse
   * relationships, anything with one of these types means the sourceId is a
   * "child".
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the relationship types
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getHierarchicalRelationshipTypes(
    String terminology, String version) throws Exception;

  /**
   * Returns the relationship characteristic types.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the relationship characteristic types
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getRelationshipCharacteristicTypes(
    String terminology, String version) throws Exception;

  /**
   * Returns the relationship modifiers.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the relationship modifiers
   * @throws Exception if anything goes wrong
   */
  public Map<String, String> getRelationshipModifiers(String terminology,
    String version) throws Exception;

}
