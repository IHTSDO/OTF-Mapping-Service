package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The Class SnomedMetadataServiceJpaHelper.
 * 
 * @author ${author}
 */
public class SnomedMetadataServiceJpaHelper implements MetadataService {

  /** The Constant isaRelationshipType. */
  private final static long isaRelationshipType = 116680003l;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getAllMetadata(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<String, Map<Long, String>> getAllMetadata(String terminology,
    String version) {
    // no-op - this is just helper class
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getModules(java.lang.String
   * , java.lang.String)
   */
  @Override
  public Map<Long, String> getModules(String terminology, String version)
    throws Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000443000
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000443000", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getAttributeValueRefSets
   * (java.lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getAttributeValueRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000480006
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000480006", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getComplexMapRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 447250001
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("447250001", terminology, version,
            isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getLanguageRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getLanguageRefSets(String terminology, String version)
    throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000506000
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000506000", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleMapRefSets(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getSimpleMapRefSets(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000496009
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000496009", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleRefSets(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<Long, String> getSimpleRefSets(String terminology, String version)
    throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 446609009
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("446609009", terminology, version,
            isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getMapRelations(java.lang
   * .String, java.lang.String)
   */
  @Override
  public Map<Long, String> getMapRelations(String terminology, String version)
    throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 609330002
    // 609330002 - Map category value
    // TODO: figure out why that doesn't work. opting for 447634004 instead
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("447634004", terminology, version,
            isaRelationshipType);

    Logger.getLogger(this.getClass()).debug(
        "Descendants of 447634004 " + descendants);
    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }

    // find all active descendants of 447247004
    // 447247004 - SNOMED CT source code not mappable to target coding scheme
    descendants =
        contentService.getDescendants("447247004", terminology, version,
            isaRelationshipType);

    Logger.getLogger(this.getClass()).debug(
        "Descendants of 447247004 " + descendants);
    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }

    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getDefinitionStatuses(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getDefinitionStatuses(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000444006
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000444006", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getDescriptionTypes(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getDescriptionTypes(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000446008
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000446008", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getCaseSignificances(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getCaseSignificances(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000447004
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000447004", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipTypes(java
   * .lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getRelationshipTypes(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 106237007
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("106237007", terminology, version,
            isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MetadataService#
   * getHierarchicalRelationshipTypes(java.lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getHierarchicalRelationshipTypes(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants
    ContentService contentService = new ContentServiceJpa();
    Concept isaRel =
        contentService.getConcept(isaRelationshipType + "", terminology,
            version);
    map.put(new Long(isaRel.getTerminologyId()),
        isaRel.getDefaultPreferredName());
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MetadataService#
   * getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getRelationshipCharacteristicTypes(
    String terminology, String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000449001
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000449001", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipModifiers
   * (java.lang.String, java.lang.String)
   */
  @Override
  public Map<Long, String> getRelationshipModifiers(String terminology,
    String version) throws NumberFormatException, Exception {
    Map<Long, String> map = new HashMap<>();

    // find all active descendants of 900000000000450001
    ContentService contentService = new ContentServiceJpa();
    Set<Concept> descendants =
        contentService.getDescendants("900000000000450001", terminology,
            version, isaRelationshipType);

    for (Concept descendant : descendants) {
      if (descendant.isActive()) {
        map.put(new Long(descendant.getTerminologyId()),
            descendant.getDefaultPreferredName());
      }
    }
    contentService.close();
    return map;
  }

  @Override
  public void close() {
    // no-op - this is just helper class
  }

  @Override
  public List<String> getTerminologies() {
    // no-op - this is just helper class
    return null;
  }

  @Override
  public List<String> getVersions(String terminology) {
    // no-op - this is just helper class
    return null;
  }

  @Override
  public String getLatestVersion(String terminology) {
    // no-op - this is just helper class
    return null;
  }

  @Override
  public Map<String, String> getTerminologyLatestVersions() {
    // no-op - this is just helper class
    return null;
  }

}
