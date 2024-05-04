/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference implementation of {@link ProjectSpecificAlgorithmHandler}.
 */
public class DefaultICD10ProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

  final static Logger LOGGER =
      LoggerFactory.getLogger(DefaultICD10ProjectSpecificAlgorithmHandler.class);

  /** The dagger codes. */
  private static Set<String> daggerCodes = new HashSet<>();

  /** The asterisk codes. */
  private static Set<String> asteriskCodes = new HashSet<>();

  /** The asterisk ref set id. */
  private static String asteriskRefSetId;

  /** The dagger ref set id. */
  private static String daggerRefSetId;

  /**
   * Cache dagger, asterisk, and valid 3-digit codes.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "unchecked"
  })
  protected void cacheCodes() throws Exception {

    // lazy initialize
    if (!asteriskCodes.isEmpty()) {
      return;
    }

    LOGGER.info("Caching the asterisk and dagger codes");

    try (final ContentServiceJpa contentService = new ContentServiceJpa();
        final MetadataService metadataService = new MetadataServiceJpa();) {

      final EntityManager manager = contentService.getEntityManager();

      final String terminology = mapProject.getSourceTerminology();
      final String terminologyVersion = mapProject.getSourceTerminologyVersion();

      // open the metadata service and get the relationship types
      final Map<String, String> simpleRefSets =
          metadataService.getSimpleRefSets(terminology, terminologyVersion);

      // find the dagger/asterisk types
      for (final String key : simpleRefSets.keySet()) {
        if (simpleRefSets.get(key).equals("Asterisk refset")) {
          asteriskRefSetId = key;
        }
        if (simpleRefSets.get(key).equals("Dagger refset")) {
          daggerRefSetId = key;
        }
      }

      if (asteriskRefSetId == null) {
        LOGGER.warn("Could not find Asterisk refset");
      }

      if (daggerRefSetId == null) {
        LOGGER.warn("Could not find Dagger refset");
      }

      // Look up asterisk codes
      final javax.persistence.Query asteriskQuery = manager.createQuery(
          "select m.concept from SimpleRefSetMemberJpa m " + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion " + "and m.refSetId = :refSetId ");
      asteriskQuery.setParameter("terminology", terminology);
      asteriskQuery.setParameter("terminologyVersion", terminologyVersion);
      asteriskQuery.setParameter("refSetId", asteriskRefSetId);
      List<Concept> concepts = asteriskQuery.getResultList();
      for (final Concept concept : concepts) {
        asteriskCodes.add(concept.getTerminologyId());
      }

      // Look up dagger codes
      final javax.persistence.Query daggerQuery = manager.createQuery(
          "select m.concept from SimpleRefSetMemberJpa m " + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion " + "and m.refSetId = :refSetId ");
      daggerQuery.setParameter("terminology", terminology);
      daggerQuery.setParameter("terminologyVersion", terminologyVersion);
      daggerQuery.setParameter("refSetId", daggerRefSetId);
      concepts = daggerQuery.getResultList();
      for (final Concept concept : concepts) {
        daggerCodes.add(concept.getTerminologyId());
      }

      // Report to log
      LOGGER.info(" asterisk codes = {} for terminology:{} version:{}", asteriskCodes, terminology,
          terminologyVersion);
      LOGGER.info(" dagger codes = {} for terminology:{} version:{}", daggerCodes, terminology,
          terminologyVersion);
    } catch (final Exception e) {
      throw e;
    }
  }

  /* see superclass */
  @Override
  public Map<String, String> getAllTerminologyNotes() throws Exception {
    final Map<String, String> map = new HashMap<>();
    cacheCodes();
    for (final String code : asteriskCodes) {
      if (isSourceCodeValid(code)) {
        map.put(code, "*");
      }
    }
    for (final String code : daggerCodes) {
      if (isSourceCodeValid(code)) {
        map.put(code, "\u2020");
      }
    }
    return map;
  }

  /* see superclass */
  @Override
  public void computeTargetTerminologyNotes(final List<TreePosition> treePositionList)
    throws Exception {

    LOGGER.info("Computing target terminology notes.");
    cacheCodes();

    // for each tree position initially passed in, call the recursive helper
    for (final TreePosition tp : treePositionList) {
      computeTargetTerminologyNotesHelper(tp, asteriskRefSetId, daggerRefSetId);
    }

  }

  /**
   * Compute target terminology notes helper.
   *
   * @param treePosition the tree position
   * @param asteriskRefSetId the asterisk ref set id
   * @param daggerRefSetId the dagger ref set id
   * @throws Exception the exception
   */
  private void computeTargetTerminologyNotesHelper(final TreePosition treePosition,
    final String asteriskRefSetId, final String daggerRefSetId) throws Exception {

    LOGGER.info("Computing target terminology note for " + treePosition.getTerminologyId());

    // initially set the note to an empty string
    treePosition.setTerminologyNote("");

    // Simple lookup here
    if (asteriskCodes.contains(treePosition.getTerminologyId())) {
      treePosition.setTerminologyNote("*");
    } else if (asteriskCodes.contains(treePosition.getTerminologyId())) {
      treePosition.setTerminologyNote("\u2020");
    }

    // if this tree position has children, set their terminology notes
    // recursively
    for (final TreePosition child : treePosition.getChildren()) {
      computeTargetTerminologyNotesHelper(child, asteriskRefSetId, daggerRefSetId);
    }

  }
}