package org.ihtsdo.otf.mapping.jpa.handlers;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class ICPC2NOProjectSpecificAlgorithmHandler.
 */
public class ICPC2NOProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

  /**
   * @param mapRecord
   * @return the validation result
   * @throws Exception
   */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();
    ContentService contentService = new ContentServiceJpa();

    for (MapEntry mapEntry : mapRecord.getMapEntries()) {

      // accept a blank code
      if (mapEntry.getTargetId().equals("")) {
        continue;
      }

      // get concept
      Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // verify that concept exists
      if (concept == null) {
        validationResult.addError(
            "Target code " + mapEntry.getTargetId() + " not found in database!" + " Entry:"
                + (mapProject.isGroupStructure()
                    ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                + " map priority " + Integer.toString(mapEntry.getMapPriority()));

      }
    }

    contentService.close();
    return validationResult;

  }

  /**
   * Computes the map relation for the SNOMEDCT to ICPC-2 map project. Based
   * solely on whether an entry has a TRUE rule or not. No advices are computed
   * for this project.
   * @throws Exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) throws Exception {

    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null && mapEntry.getMapRelation().isAllowableForNullTarget())
        return mapEntry.getMapRelation();
      else {
        // retrieve the not classifiable relation
        // 447638001 - Map source concept cannot be classified with available
        // data
        for (MapRelation relation : mapProject.getMapRelations()) {
          if (relation.getTerminologyId().equals("447638001"))
            return relation;
        }

        // if cannot find, return null
        return null;
      }
    }

    // if rule is not set, return null
    if (mapEntry.getRule() == null || mapEntry.getRule().isEmpty()) {
      return null;
    }

    // if entry has a gender rule
    if (mapEntry.getRule().contains("MALE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if entry has an age rule
    } else if (mapEntry.getRule().contains("AGE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if the entry has a non-gender, non-age IFA
    } else if (mapEntry.getRule().startsWith("IFA")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // using contains here to capture TRUE and OTHERWISE TRUE
    } else if (mapEntry.getRule().contains("TRUE")) {

      // retrieve the relations by terminology id
      for (MapRelation relation : mapProject.getMapRelations()) {
        // 447637006 - Map source concept is properly classified
        if (relation.getTerminologyId().equals("447637006")) {
          return relation;
        }
      }

      // if entry has a target and not TRUE rule
    } else {

      throw new Exception("Unexpected map relation condition.");
    }

    // if relation not found, return null
    return null;

  }

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    final ContentService contentService = new ContentServiceJpa();
    
    try {

      // See if map record exists in Norway's snowstorm
      // If so, return a pre-populated record with its information
      // If not, return null
      final String url =
          "https://dailybuild.terminologi.ehelse.no/snowstorm/snomed-ct/browser/MAIN%2FSNOMEDCT-NO%2FREFSETS/members";

      final Client client = ClientBuilder.newClient();
      final String accept = "*/*";

      final ObjectMapper mapper = new ObjectMapper();

      String targetUri = url + "?referenceSet=68101000202102&module=51000202101&offset=0&limit=100&referencedComponentId=" + mapRecord.getConceptId();
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
        //Only load in active, non-deleted map records
        if(!(mapActive.asText().equals("true") && mapDeleted.asText().equalsIgnoreCase("false"))) {
          continue;
        }
        
        //If this is the first entry, set up basic mapRecord info
        if(existingMapRecord == null) {
          existingMapRecord = new MapRecordJpa();
          existingMapRecord.setConceptId(mapRecord.getConceptId());
          existingMapRecord.setConceptName(mapRecord.getConceptName());
        }
        
        
        JsonNode additionalFields = mapNode.get("additionalFields");
        MapEntry existingMapEntry = new MapEntryJpa();
        existingMapEntry.setRule(additionalFields.get("mapRule").asText());
        existingMapEntry.setMapPriority(additionalFields.get("mapPriority").asInt());
        existingMapEntry.setMapGroup(additionalFields.get("mapGroup").asInt());
        
        MapRelation mapRelation = null;
        for (MapRelation relation : mapProject.getMapRelations()) {
          if (relation.getTerminologyId().equals(additionalFields.get("mapCategoryId").asText())){
            mapRelation = relation;
          }
        }
        existingMapEntry.setMapRelation(mapRelation);
        existingMapEntry.setTargetId(additionalFields.get("mapTarget").asText());
        Concept targetConcept = contentService.getConcept(existingMapEntry.getTargetId(), mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
        if(targetConcept != null) {
          existingMapEntry.setTargetName(targetConcept.getDefaultPreferredName());
        }
        else {
          existingMapEntry.setTargetName(existingMapEntry.getTargetId() + " DOES NOT EXIST IN " + mapProject.getDestinationTerminology() + ", " + mapProject.getDestinationTerminologyVersion());
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
