/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * Loads and serves configuration.
 */
public class TerminologyUtility {

  /** The asterisk refset. */
  private static Map<String, String> asteriskRefsetIdMap = new HashMap<>();

  /** The dagger refset id map. */
  private static Map<String, String> daggerRefsetIdMap = new HashMap<>();

  /** The asterisk to dagger id. */
  private static Map<String, String> asteriskToDaggerIdMap = new HashMap<>();

  /** The isa rel types. */
  private static Map<String, Long> isaRelTypes = new HashMap<>();

  /** The loader. */
  private static MapUser loader = null;

  /**
   * Indicates whether or not asterisk code is the case.
   *
   * @param concept the concept
   * @param service the service
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isAsteriskCode(Concept concept, ContentService service)
    throws Exception {
    // Lazy initialize asterisk refset
    if (!asteriskRefsetIdMap.containsKey(concept.getTerminology()
        + concept.getTerminologyVersion())) {
      initDaggerAsterisk(concept, service);
    }
    for (final SimpleRefSetMember member : concept.getSimpleRefSetMembers()) {
      if (member.getRefSetId().equals(
          asteriskRefsetIdMap.get(concept.getTerminology()
              + concept.getTerminologyVersion()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates whether or not dagger code is the case.
   *
   * @param concept the concept
   * @param service the service
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isDaggerCode(Concept concept, ContentService service)
    throws Exception {
    // Lazy initialize dagger refset
    if (!daggerRefsetIdMap.containsKey(concept.getTerminology()
        + concept.getTerminologyVersion())) {
      initDaggerAsterisk(concept, service);
    }
    for (final SimpleRefSetMember member : concept.getSimpleRefSetMembers()) {
      if (member.getRefSetId().equals(
          daggerRefsetIdMap.get(concept.getTerminology()
              + concept.getTerminologyVersion()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates whether the daggerId is a dagger code for a corresponding
   * asterisk concept.
   *
   * @param asterisk the asterisk
   * @param dagger the dagger
   * @param service the service
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isDaggerForAsterisk(Concept asterisk, Concept dagger,
    ContentService service) throws Exception {
    // Lazy initialize asterisk to dagger rel type id
    if (!asteriskToDaggerIdMap.containsKey(asterisk.getTerminology()
        + asterisk.getTerminologyVersion())) {
      initDaggerAsterisk(asterisk, service);
    }
    // Assume concept is an asterisk concept
    for (Relationship rel : asterisk.getRelationships()) {
      if (rel.getTypeId().equals(
          Long.valueOf(asteriskToDaggerIdMap.get(asterisk.getTerminology()
              + asterisk.getTerminologyVersion())))
          && rel.getDestinationConcept().getTerminologyId()
              .equals(dagger.getTerminologyId())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Inits the dagger asterisk.
   *
   * @param concept the concept
   * @param service the service
   * @throws Exception
   */
  private static void initDaggerAsterisk(Concept concept, ContentService service)
    throws Exception {
    final SearchResultList list =
        service.findConceptsForQuery(
            "(defaultPreferredName:asterisk OR defaultPreferredName:dagger) "
                + " AND terminology:" + concept.getTerminology()
                + " AND terminologyVersion:" + concept.getTerminologyVersion(),
            null);
    final String key =
        concept.getTerminology() + concept.getTerminologyVersion();
    for (final SearchResult result : list.getSearchResults()) {
      System.out.println("ASTERISK: " + result);
      if (result.getValue().equals("Asterisk refset")) {
        asteriskRefsetIdMap.put(key, result.getTerminologyId());
      } else if (result.getValue().equals("Dagger refset")) {
        daggerRefsetIdMap.put(key, result.getTerminologyId());
      } else if (result.getValue().equals("Asterisk to dagger")) {
        asteriskToDaggerIdMap.put(key, result.getTerminologyId());
      }
    }
    if (!asteriskToDaggerIdMap.containsKey(key)) {
      throw new Exception(
          "Unable to find 'Asterisk to dagger' relationship type for " + key);
    }
    if (!asteriskRefsetIdMap.containsKey(key)) {
      throw new Exception("Unable to find asterisk refset for " + key);
    }
    if (!daggerRefsetIdMap.containsKey(key)) {
      throw new Exception("Unable to find dagger refset for " + key);
    }

  }

  /**
   * Returns the active parent concepts.
   *
   * @param concept the concept
   * @return the active parent concepts
   * @throws Exception the exception
   */
  public static List<Concept> getActiveParents(Concept concept)
    throws Exception {
    if (concept == null) {
      throw new Exception("Unexpected null concept passed to getActiveParents.");
    }
    final Long isaType =
        getHierarchicalType(concept.getTerminology(),
            concept.getTerminologyVersion());
    final List<Concept> results = new ArrayList<>();
    for (Relationship rel : concept.getRelationships()) {
      if (rel.getTypeId().equals(isaType) && rel.isActive()) {
        results.add(rel.getDestinationConcept());
      }
    }
    return results;
  }

  /**
   * Returns the active parent children;.
   *
   * @param concept the concept
   * @return the active child concepts
   * @throws Exception the exception
   */
  public static List<Concept> getActiveChildren(Concept concept)
    throws Exception {
    if (concept == null) {
      throw new Exception(
          "Unexpected null concept passed to getActiveChildren.");
    }
    final Long isaType =
        getHierarchicalType(concept.getTerminology(),
            concept.getTerminologyVersion());
    final List<Concept> results = new ArrayList<>();
    for (Relationship rel : concept.getInverseRelationships()) {
      if (rel.getTypeId().equals(isaType) && rel.isActive()) {
        results.add(rel.getSourceConcept());
      }
    }
    return results;

  }

  /**
   * Indicates whether the specified concept has active children.
   *
   * @param concept the concept
   * @return true, if successful
   * @throws Exception the exception
   */
  public static boolean hasActiveChildren(Concept concept) throws Exception {
    if (concept == null) {
      throw new Exception(
          "Unexpected null concept passed to getActiveChildren.");
    }
    final Long isaType =
        getHierarchicalType(concept.getTerminology(),
            concept.getTerminologyVersion());
    for (Relationship rel : concept.getInverseRelationships()) {
      if (rel.getTypeId().equals(isaType) && rel.isActive()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the hierarchical type.
   *
   * @param terminology the terminology
   * @param version the version
   * @return the hierarchical type
   * @throws Exception the exception
   */
  public static Long getHierarchicalType(String terminology, String version)
    throws Exception {
    if (!isaRelTypes.containsKey(terminology + version)) {
      final MetadataService service = new MetadataServiceJpa();
      try {
        isaRelTypes.put(
            terminology + version,
            Long.valueOf(service
                .getHierarchicalRelationshipTypes(terminology, version)
                .keySet().iterator().next()));
      } catch (Exception e) {
        throw e;
      } finally {
        service.close();
      }
    }
    return isaRelTypes.get(terminology + version);
  }

  /**
   * Indicates whether the entry uses the specified advice.
   *
   * @param entry the entry
   * @param advice the advice
   * @return true, if successful
   */
  public static boolean hasAdvice(MapEntry entry, String advice) {
    for (final MapAdvice mapAdvice : entry.getMapAdvices()) {
      if (mapAdvice.getName().equals(advice)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the matching advice.
   *
   * @param project the project
   * @param advice the advice
   * @return the matching advice
   * @throws Exception the exception
   */
  public static MapAdvice getAdvice(MapProject project, String advice)
    throws Exception {
    for (final MapAdvice mapAdvice : project.getMapAdvices()) {
      if (mapAdvice.getName().equals(advice)) {
        return mapAdvice;
      }
    }
    throw new Exception("Unalbe to find advice - " + advice);
  }

  /**
   * Returns the loader user.
   *
   * @return the loader user
   * @throws Exception the exception
   */
  public static MapUser getLoaderUser() throws Exception {
    if (loader == null) {
      final MappingService service = new MappingServiceJpa();
      try {
        loader = service.getMapUser("loader");
      } catch (Exception e) {
        throw e;
      } finally {
        service.close();
      }
    }
    return loader;

  }

  /**
   * Comparator for mapGroup/mapPriority
   */
  public static class MapEntryComparator implements Comparator<MapEntry> {

    /* see superclass */
    @Override
    public int compare(MapEntry o1, MapEntry o2) {
      if (o1.getMapGroup() != o2.getMapGroup()) {
        return o1.getMapGroup() - o2.getMapGroup();
      }
      return o1.getMapPriority() - o2.getMapPriority();
    }

  }
}
