/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class ICD10NOMaintProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    // No current validation restrictions

    final ValidationResult validationResult = new ValidationResultJpa();

    return validationResult;
  }

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    // See if map record exists in Norway's snowstorm.
    // If so, return a pre-populated record with its information
    // If not, return null
    // Need to check if it exists in the Norway module (51000202101),
    // and in the International module (449080006)
    
    MapRecord intMapRecord = pullMapRecordFromSnowstorm("449080006", "447562003", mapRecord);
    MapRecord noMapRecord = pullMapRecordFromSnowstorm("51000202101", "447562003", mapRecord);

    if (intMapRecord == null && noMapRecord == null) {
      return null;
    }
    else if (intMapRecord == null && noMapRecord != null) {
      return noMapRecord;
    }
    else if (intMapRecord != null && noMapRecord == null) {
      return intMapRecord;
    }
    // This is the tricky one.
    // Norway map entries take precedence, so they override any Int entries, matching by group and priority.
    // BUT, 
    else if (intMapRecord != null && noMapRecord != null) {
//      MapRecord returnMapRecord = intMapRecord;
//      for(MapEntry intMapEntry : returnMapRecord.getMapEntries()) {
//        for (MapEntry noMapEntry : noMapRecord.getMapEntries()) {
//          if(intMapEntry.getMapGroup() == noMapEntry.getMapGroup() && intMapEntry.getMapPriority() == noMapEntry.getMapPriority()) {
//            returnMapRecord.getMapEntries().remove(intMapEntry);
//            returnMapRecord.getMapEntries().add(noMapEntry);
//          }
//        }
//      }
//      //
//      return returnMapRecord;
      return noMapRecord;
    }
    else {
      //Shouldn't be able to get here
      return null;
    }
  }

  private MapRecord pullMapRecordFromSnowstorm(String moduleId, String refsetId,
    MapRecord mapRecord) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    try {

      final String url =
          "https://dailybuild.terminologi.ehelse.no/snowstorm/snomed-ct/browser/MAIN%2FSNOMEDCT-NO%2FREFSETS/members";

      final Client client = ClientBuilder.newClient();
      final String accept = "*/*";

      final ObjectMapper mapper = new ObjectMapper();

      String targetUri = url + "?referenceSet=" + refsetId + "&module=" + moduleId
          + "&offset=0&limit=100&referencedComponentId=" + mapRecord.getConceptId();
      WebTarget target = client.target(targetUri);
      target = client.target(targetUri);
      Logger.getLogger(getClass()).info(targetUri);

      Response response = target.request(accept).get();
      String resultString = response.readEntity(String.class);
      if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
        // n/a
      } else {
        throw new LocalException(
            "Unexpected terminology server failure. Message = " + resultString);
      }

      final JsonNode doc = mapper.readTree(resultString);

      MapRecord existingMapRecord = null;

      for (final JsonNode mapNode : doc.get("items")) {
        JsonNode mapActive = mapNode.get("active");
        JsonNode mapDeleted = mapNode.get("deleted");
        // Only load in active, non-deleted map records
        if (!(mapActive.asText().equals("true") && mapDeleted.asText().equalsIgnoreCase("false"))) {
          continue;
        }

        // If this is the first entry, set up basic mapRecord info
        if (existingMapRecord == null) {
          existingMapRecord = new MapRecordJpa();
          existingMapRecord.setConceptId(mapRecord.getConceptId());
          existingMapRecord.setConceptName(mapRecord.getConceptName());
        }

        JsonNode additionalFields = mapNode.get("additionalFields");
        MapEntry existingMapEntry = new MapEntryJpa();
        existingMapEntry.setRule(additionalFields.get("mapRule").asText());
        existingMapEntry.setMapPriority(additionalFields.get("mapPriority").asInt());
        existingMapEntry.setMapGroup(additionalFields.get("mapGroup").asInt());

        Set<MapAdvice> mapAdvices = new HashSet<>();
        for (MapAdvice mapAdvice : mapProject.getMapAdvices()) {
          if (additionalFields.get("mapAdvice").asText().contains(mapAdvice.getName())) {
            mapAdvices.add(mapAdvice);
          }
        }
        existingMapEntry.setMapAdvices(mapAdvices);

        MapRelation mapRelation = null;
        for (MapRelation relation : mapProject.getMapRelations()) {
          if (relation.getTerminologyId().equals(additionalFields.get("mapCategoryId").asText())) {
            mapRelation = relation;
          }
        }
        existingMapEntry.setMapRelation(mapRelation);

        existingMapEntry.setTargetId(additionalFields.get("mapTarget").asText());
        Concept targetConcept = contentService.getConcept(existingMapEntry.getTargetId(),
            mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
        if (targetConcept != null) {
          existingMapEntry.setTargetName(targetConcept.getDefaultPreferredName());
        } else {
          existingMapEntry.setTargetName(existingMapEntry.getTargetId() + " DOES NOT EXIST IN "
              + mapProject.getDestinationTerminology() + ", "
              + mapProject.getDestinationTerminologyVersion());
        }

        existingMapRecord.addMapEntry(existingMapEntry);

      }

      response.close();
      return existingMapRecord;
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();

    }
  }
}