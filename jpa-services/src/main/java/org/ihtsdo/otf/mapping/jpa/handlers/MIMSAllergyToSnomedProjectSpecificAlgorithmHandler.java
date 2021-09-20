/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class MIMSAllergyToSnomedProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception {

    // No current validation restrictions

    final ValidationResult validationResult = new ValidationResultJpa();

    return validationResult;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    return true;
  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord)
    throws Exception {
    final ValidationResult result = new ValidationResultJpa();
    
    // Map record name must share at least one word with target name
    final Set<String> recordWords = new HashSet<>(
        Arrays.asList(mapRecord.getConceptName().toLowerCase().split(" ")));
    final Set<String> entryWords = new HashSet<>();
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getTargetName() != null) {
        entryWords.addAll(
            Arrays.asList(entry.getTargetName().toLowerCase().split(" ")));
      }
    }
    final Set<String> recordMinusEntry = new HashSet<>(recordWords);
    recordMinusEntry.removeAll(entryWords);

    // If there are entry words and none match, warning
    // if (entryWords.size() > 0 && recordWords.size() ==
    // recordMinusEntry.size()) {
    // result
    // .addWarning("From concept and target code names must share at least one
    // word.");
    // }

    return result;
  }

}