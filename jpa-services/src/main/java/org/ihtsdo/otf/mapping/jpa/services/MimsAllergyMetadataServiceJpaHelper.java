/*
 *    Copyright 2021 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * Implementation of {@link MetadataService} for MIMS Allergy terminology.
 * 
 */
public class MimsAllergyMetadataServiceJpaHelper extends RootServiceJpa
    implements MetadataService {

  private Map<String, String> descriptionTypeMap = null;

  /**
   * Instantiates an empty {@link MimsAllergyMetadataServiceJpaHelper}.
   * 
   * @throws Exception the exception
   */
  public MimsAllergyMetadataServiceJpaHelper() throws Exception {
    super();
  }

  /**
   * Returns the isa relationship type.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the isa relationship type
   * @throws Exception the exception
   */
  private static String getIsaRelationshipType(String terminology,
    String version) throws Exception {
    ContentService contentService = new ContentServiceJpa();
    SearchResultList results =
        contentService.findConceptsForQuery("Isa", new PfsParameterJpa());
    for (SearchResult result : results.getSearchResults()) {
      if (result.getTerminology().equals(terminology)
          && result.getTerminologyVersion().equals(version)
          && result.getValue().equals("Isa")) {

        contentService.close();
        return new String(result.getTerminologyId());
      }
    }
    contentService.close();
    return "-1";
  }

  /* see superclass */
  @Override
  public Map<String, Map<String, String>> getAllMetadata(String terminology,
    String version) {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public Map<String, String> getModules(String terminology, String version)
    throws Exception {

    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results =
          contentService.findConceptsForQuery("Module", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Module")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Module concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getAttributeValueRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    return new HashMap<>();
  }

  /* see superclass */
  @Override
  public Map<String, String> getComplexMapRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    return new HashMap<>();

  }

  /* see superclass */
  @Override
  public Map<String, String> getLanguageRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    return new HashMap<>();
  }

  /* see superclass */
  @Override
  public Map<String, String> getSimpleMapRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    return new HashMap<>();
  }

  /* see superclass */
  @Override
  public Map<String, String> getSimpleRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    return new HashMap<>();
  }

  /* see superclass */
  @Override
  public Map<String, String> getMapRelations(String terminology, String version)
    throws NumberFormatException, Exception {
    return new HashMap<>();
  }

  /* see superclass */
  @Override
  public Map<String, String> getDefinitionStatuses(String terminology,
    String version) throws NumberFormatException, Exception {

    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results = contentService
          .findConceptsForQuery("Definition status", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Definition status")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Definition status concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getDescriptionTypes(String terminology,
    String version) throws NumberFormatException, Exception {
    if (descriptionTypeMap != null) {
      return descriptionTypeMap;
    }

    descriptionTypeMap = new HashMap<>();

    // find all active descendants of 'Terminology metadata' concept
    ContentService contentService = new ContentServiceJpa();

    // want all descendants, do not use pfsParameter
    List<Concept> descendants = getDescendantConcepts(contentService,
        terminology + " metadata", terminology, version);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        descriptionTypeMap.put(new String(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return descriptionTypeMap;
  }

  /* see superclass */
  @Override
  public Map<String, String> getCaseSignificances(String terminology,
    String version) throws NumberFormatException, Exception {

    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results = contentService
          .findConceptsForQuery("Case significance", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Case significance")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Case significance concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getRelationshipTypes(String terminology,
    String version) throws NumberFormatException, Exception {

    // find all active descendants of 106237007
    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results = contentService
          .findConceptsForQuery("Relationship type", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Relationship type")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Relationship type concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getHierarchicalRelationshipTypes(
    String terminology, String version)
    throws NumberFormatException, Exception {
    Map<String, String> map = new HashMap<>();

    // find all active descendants of isa
    try (ContentService contentService = new ContentServiceJpa();) {
      Concept isaRel = contentService.getConcept(
          getIsaRelationshipType(terminology, version).toString(), terminology,
          version);
      map.put(new String(isaRel.getTerminologyId()),
          isaRel.getDefaultPreferredName());

      return map;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getRelationshipCharacteristicTypes(
    String terminology, String version)
    throws NumberFormatException, Exception {

    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results = contentService
          .findConceptsForQuery("Characteristic type", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Characteristic type")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Characteristic type concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getRelationshipModifiers(String terminology,
    String version) throws NumberFormatException, Exception {

    try (ContentService contentService = new ContentServiceJpa();) {
      String rootId = null;
      SearchResultList results = contentService.findConceptsForQuery("Modifier",
          new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Modifier")) {
          rootId = result.getTerminologyId();
          break;
        }
      }
      if (rootId == null)
        throw new Exception("Modifier concept cannot be found.");

      Map<String, String> result =
          getDescendantMap(contentService, rootId, terminology, version);

      return result;
    }
  }

  /* see superclass */
  @Override
  public void close() {
    // no-op - this is just helper class
  }

  /* see superclass */
  @Override
  public List<String> getTerminologies() {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public List<String> getVersions(String terminology) {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public String getLatestVersion(String terminology) {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public String getPreviousVersion(String terminology) {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public Map<String, String> getTerminologyLatestVersions() {
    // no-op - this is just helper class
    return null;
  }

  /* see superclass */
  @Override
  public Boolean checkTerminologyVersionExists(String terminology,
    String version) {
    // no-op - this is just helper class
    return null;
  }

  /**
   * Returns the descendant map for the specified parameters.
   *
   * @param contentService the content service
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param version the version
   * @return the descendant map
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private Map<String, String> getDescendantMap(ContentService contentService,
    String terminologyId, String terminology, String version) throws Exception {
    Map<String, String> map = new HashMap<>();

    // want all descendants, do not use pfsParameter
    Concept concept =
        contentService.getConcept(terminologyId, terminology, version);
    List<Concept> descendants =
        TerminologyUtility.getActiveDescendants(concept);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new String(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    return map;
  }

  /* see superclass */
  @Override
  public void setProperties(Properties properties) {
    // n/a
  }

  /**
   * Helper method for getting descendants.
   *
   * @param contentService the content service
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the descendant concepts
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private List<Concept> getDescendantConcepts(ContentService contentService,
    String term, String terminology, String terminologyVersion)
    throws Exception {
    final SearchResultList list = contentService.findConceptsForQuery(
        "defaultPreferredName:" + term + " AND terminology:" + terminology
            + " AND terminologyVersion:" + terminologyVersion,
        null);

    if (list.getTotalCount() == 0) {
      return new ArrayList<Concept>();
    }

    Concept concept = null;

    for (SearchResult result : list.getIterable()) {
      if (result.getValue().equals(term)) {
        concept = contentService.getConcept(result.getTerminologyId(),
            terminology, terminologyVersion);
        break;
      }
    }

    if (concept == null) {
      throw new Exception("Cannot locate the MIMS Allergy Metadata concept");
    }

    return TerminologyUtility.getActiveDescendants(concept);

  }
}
