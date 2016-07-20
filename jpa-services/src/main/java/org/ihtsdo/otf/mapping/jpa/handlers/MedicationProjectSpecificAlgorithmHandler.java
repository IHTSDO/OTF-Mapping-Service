package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class MedicationProjectSpecificAlgorithmHandler extends

DefaultProjectSpecificAlgorithmHandler {

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    // disallow "root" and the HT codes
    if (terminologyId.equals("root") || terminologyId.startsWith("HT")) {
      return false;
    }
    return true;
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