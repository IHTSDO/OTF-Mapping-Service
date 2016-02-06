package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Map;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * GMDN project specific algorithm handler.
 */
public class GmdnProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /** The term type. */
  private static Long termType = null;

  /** The ivd type. */
  private static Long ivdType = null;

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    // Cache the "Term" term type - valid codes require it
    cacheTermType(mapProject.getDestinationTerminology(),
        mapProject.getDestinationTerminologyVersion());

    final ContentService contentService = new ContentServiceJpa();
    try {
      // Concept must exist
      final Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      // Only concepts with "term" description types
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
   * @param terminology the terminology
   * @param version the version
   * @throws Exception the exception
   */
  private static void cacheTermType(String terminology, String version)
    throws Exception {
    // lazy initialize
    if (termType == null) {
      final MetadataService service = new MetadataServiceJpa();
      try {
        for (final Map.Entry<String, String> entry : service
            .getDescriptionTypes(terminology, version).entrySet()) {
          if (entry.getValue().equals("Term")) {
            termType = Long.valueOf(entry.getKey());
          }
          if (entry.getValue().equals("IVD Term")) {
            ivdType = Long.valueOf(entry.getKey());
          }
        }

      } catch (Exception e) {
        throw e;
      } finally {
        service.close();
      }
    }
  }
}
