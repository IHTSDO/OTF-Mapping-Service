package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Reference implementation of {@link MetadataService}
 */
public class MetadataServiceJpa extends RootServiceJpa implements
    MetadataService {


  /** The helper map. */
  private static Map<String, MetadataService> helperMap = null;
  static {
    helperMap = new HashMap<>();
    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
      String key = "metadata.service.handler";
      for (String handlerName : config.getProperty(key).split(",")) {

        // Add handlers to map
        MetadataService handlerService =
            ConfigUtility.newStandardHandlerInstanceWithConfiguration(key,
                handlerName, MetadataService.class);
        helperMap.put(handlerName, handlerService);
      }
    } catch (Exception e) {
      e.printStackTrace();
      helperMap = null;
    }
  }
  /**
   * Instantiates an empty {@link MetadataServiceJpa}.
   * @throws Exception
   */
  public MetadataServiceJpa() throws Exception {
    super();

    if (helperMap == null) {
      throw new Exception("Helper map not properly initialized, serious error.");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getAllMetadata(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<String, Map<String, String>> getAllMetadata(String terminology,
    String version) throws Exception {
    Map<String, Map<String, String>> idNameMapList = new HashMap<>();
    Map<String, String> modulesIdNameMap = getModules(terminology, version);
    if (modulesIdNameMap != null) {
      idNameMapList.put("Modules", modulesIdNameMap);
    }
    Map<String, String> atvIdNameMap =
        getAttributeValueRefSets(terminology, version);
    if (atvIdNameMap != null) {
      idNameMapList.put("Attribute Value Refsets", atvIdNameMap);
    }
    Map<String, String> csIdNameMap =
        getCaseSignificances(terminology, version);
    if (csIdNameMap != null) {
      idNameMapList.put("Case Significances", csIdNameMap);
    }
    Map<String, String> cmIdNameMap =
        getComplexMapRefSets(terminology, version);
    if (cmIdNameMap != null) {
      idNameMapList.put("Complex Map Refsets", cmIdNameMap);
    }
    Map<String, String> dsIdNameMap =
        getDefinitionStatuses(terminology, version);
    if (dsIdNameMap != null) {
      idNameMapList.put("Definition Statuses", dsIdNameMap);
    }
    Map<String, String> dtIdNameMap = getDescriptionTypes(terminology, version);
    if (dtIdNameMap != null) {
      idNameMapList.put("Description Types", dtIdNameMap);
    }
    Map<String, String> lIdNameMap = getLanguageRefSets(terminology, version);
    if (lIdNameMap != null) {
      idNameMapList.put("Language Refsets", lIdNameMap);
    }
    Map<String, String> mrIdNameMap = getMapRelations(terminology, version);
    if (mrIdNameMap != null) {
      idNameMapList.put("Map Relations", mrIdNameMap);
    }
    Map<String, String> rctIdNameMap =
        getRelationshipCharacteristicTypes(terminology, version);
    if (rctIdNameMap != null) {
      idNameMapList.put("Relationship Characteristic Types", rctIdNameMap);
    }
    Map<String, String> rmIdNameMap =
        getRelationshipModifiers(terminology, version);
    if (rmIdNameMap != null) {
      idNameMapList.put("Relationship Modifiers", rmIdNameMap);
    }
    Map<String, String> rtIdNameMap =
        getRelationshipTypes(terminology, version);
    if (rtIdNameMap != null) {
      idNameMapList.put("Relationship Types", rtIdNameMap);
    }
    Map<String, String> hierRtIdNameMap =
        getHierarchicalRelationshipTypes(terminology, version);
    if (hierRtIdNameMap != null) {
      idNameMapList.put("Hierarchical Relationship Types", hierRtIdNameMap);
    }
    Map<String, String> smIdNameMap = getSimpleMapRefSets(terminology, version);
    if (smIdNameMap != null) {
      idNameMapList.put("Simple Map Refsets", smIdNameMap);
    }
    Map<String, String> sIdNameMap = getSimpleRefSets(terminology, version);
    if (sIdNameMap != null) {
      idNameMapList.put("Simple Refsets", sIdNameMap);
    }
    return idNameMapList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getModules(java.lang.String
   * , java.lang.String)
   */
  @Override
  public Map<String, String> getModules(String terminology, String version)
    throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getModules(terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getAttributeValueRefSets
   * (java.lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getAttributeValueRefSets(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getAttributeValueRefSets(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getComplexMapRefSets(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getComplexMapRefSets(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getLanguageRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getLanguageRefSets(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology)
          .getLanguageRefSets(terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleMapRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getSimpleMapRefSets(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getSimpleMapRefSets(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleRefSets(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<String, String> getSimpleRefSets(String terminology, String version)
    throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getSimpleRefSets(terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getMapRelations(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<String, String> getMapRelations(String terminology, String version)
    throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getMapRelations(terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getDefinitionStatuses(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getDefinitionStatuses(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getDefinitionStatuses(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getDescriptionTypes(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getDescriptionTypes(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getDescriptionTypes(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getCaseSignificances(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getCaseSignificances(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getCaseSignificances(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipTypes(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getRelationshipTypes(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getRelationshipTypes(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MetadataService#
   * getHierarchicalRelationshipTypes(java.lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getHierarchicalRelationshipTypes(
    String terminology, String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getHierarchicalRelationshipTypes(
          terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MetadataService#
   * getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getRelationshipCharacteristicTypes(
    String terminology, String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getRelationshipCharacteristicTypes(
          terminology, version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipModifiers
   * (java.lang.String, java.lang.String)
   */
  @Override
  public Map<String, String> getRelationshipModifiers(String terminology,
    String version) throws Exception {
    if (helperMap.containsKey(terminology)) {
      return helperMap.get(terminology).getRelationshipModifiers(terminology,
          version);
    } else {
      // return an empty map
      return new HashMap<>();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MetadataService#getTerminologies()
   */
  @Override
  public List<String> getTerminologies() throws Exception {

    javax.persistence.Query query =
        manager.createQuery("SELECT distinct c.terminology from ConceptJpa c");
    @SuppressWarnings("unchecked")
    List<String> terminologies = query.getResultList();
    return terminologies;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getVersions(java.lang.String
   * )
   */
  @Override
  public List<String> getVersions(String terminology) throws Exception {
    javax.persistence.Query query =
        manager
            .createQuery("SELECT distinct c.terminologyVersion from ConceptJpa c where terminology = :terminology order by 1");

    query.setParameter("terminology", terminology);
    @SuppressWarnings("unchecked")
    List<String> versions = query.getResultList();
    return versions;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getLatestVersion(java.lang
   * .String)
   */
  @Override
  public String getLatestVersion(String terminology) throws Exception {

    javax.persistence.Query query =
        manager
            .createQuery("SELECT max(c.terminologyVersion) from ConceptJpa c where terminology = :terminology");

    query.setParameter("terminology", terminology);
    String version = query.getSingleResult().toString();
    return version;

  }

  /**
   * Returns the previous version.
   *
   * @param terminology the terminology
   * @return the previous version
   * @throws Exception the exception
   */
  @Override
  public String getPreviousVersion(String terminology) throws Exception {
    List<String> versions = getVersions(terminology);
    if (versions.size() < 2) {
      return null;
    }
    return versions.get(versions.size() - 2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getTerminologyLatestVersions
   * ()
   */
  @Override
  public Map<String, String> getTerminologyLatestVersions() throws Exception {

    javax.persistence.TypedQuery<Object[]> query =
        manager
            .createQuery(
                "SELECT c.terminology, max(c.terminologyVersion) from ConceptJpa c group by c.terminology",
                Object[].class);

    List<Object[]> resultList = query.getResultList();
    Map<String, String> resultMap = new HashMap<>(resultList.size());
    for (Object[] result : resultList)
      resultMap.put((String) result[0], (String) result[1]);

    return resultMap;

  }

}
