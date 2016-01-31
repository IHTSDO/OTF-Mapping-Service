package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * GMDN project specific algorithm handler.
 */
public class GmdnProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /** The term type. */
  private static Long termType = null;

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    final ContentService contentService = new ContentServiceJpa();

    // Cache the "Term" term type - valid codes require it
    cacheTermType(contentService, mapProject.getDestinationTerminology(),
        mapProject.getDestinationTerminologyVersion());

    try {
      // Concept must exist
      final Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      // If there is a concept and it has a "term" description it's valid
      if (concept != null) {
        for (final Description desc : concept.getDescriptions()) {
          if (desc.getTypeId().equals(termType)) {
            return true;
          }
        }
      }
      return false;

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /**
   * Cache term type.
   *
   * @param contentService the content service
   * @param terminology the terminology
   * @param version the version
   * @throws Exception the exception
   */
  private static void cacheTermType(ContentService contentService,
    String terminology, String version) throws Exception {
    // lazy initialize
    if (termType == null) {
      SearchResultList results =
          contentService.findConceptsForQuery("Term", new PfsParameterJpa());
      for (SearchResult result : results.getSearchResults()) {
        if (result.getTerminology().equals(terminology)
            && result.getTerminologyVersion().equals(version)
            && result.getValue().equals("Term")) {
          termType = Long.valueOf(result.getTerminologyId());
        }
      }
    }
  }
}
