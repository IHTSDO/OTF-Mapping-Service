package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * GMDN project specific algorithm handler.
 */
public class GmdnProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {


  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    final ContentService contentService = new ContentServiceJpa();

    try {
      // Concept must exist
      final Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      return concept != null;

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }
}
