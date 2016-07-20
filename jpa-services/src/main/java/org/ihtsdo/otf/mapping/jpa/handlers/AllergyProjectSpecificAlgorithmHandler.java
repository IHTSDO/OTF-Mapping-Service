package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class AllergyProjectSpecificAlgorithmHandler extends

DefaultProjectSpecificAlgorithmHandler {

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    final ContentService contentService = new ContentServiceJpa();
    try {
      // Allergy to substance - 419199007
      return contentService.isDescendantOf(terminologyId,
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion(), "419199007");
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /* see superclass */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) {

    // If the map relation is blank, return "exact" (e.g. the default)
    if (mapEntry.getMapRelation() == null) {

      // retrieve the cannot match relation
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("exact"))
          return relation;
      }

    }
    return mapEntry.getMapRelation();
  }

}