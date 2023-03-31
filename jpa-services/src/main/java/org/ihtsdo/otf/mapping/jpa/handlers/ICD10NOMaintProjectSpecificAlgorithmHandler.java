/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

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
    // Norway maps take precedence, so they override Int maps.
    
    final MappingService mappingService = new MappingServiceJpa();
    MapRecord existingMapRecord = null;   
    
    try {
      final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
      if (dataDir == null) {
        throw new Exception("Config file must specify a data.dir property");
      }
      
      MapRecord intMapRecord = pullMapRecordFromSnowstorm("449080006", "447562003", mapRecord);
      MapRecord noMapRecord = pullMapRecordFromSnowstorm("51000202101", "447562003", mapRecord);
      
      if (noMapRecord != null) {
        existingMapRecord = noMapRecord;
      }
      else if (intMapRecord != null) {
        existingMapRecord = intMapRecord;
      }
      else {
        return null;
      }
      
      // Add any previously created notes to the map
      String inputFile =
          dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/previouslyCreatedMapNotes.txt";
     
      if (!new File(inputFile).exists()) {
        throw new Exception("Specified input file missing: " + inputFile);
      }
      
      BufferedReader noteReader = new BufferedReader(new FileReader(inputFile));

      String line = null;

      while ((line = noteReader.readLine()) != null) {
        String fields[] = line.split("\t");
        
        String conceptId = fields[0];
        
        if(mapRecord.getConceptId().equals(conceptId)) {
          String userName = fields[1];
          String timestamp = fields[2];
          String note = fields[3];

          MapNote previousMapNote = new MapNoteJpa();
          previousMapNote.setUser(mappingService.getMapUser(userName));
          previousMapNote.setNote(note);
          //Timestamp is in 2022-05-04 11:46:20.683000 format.
          previousMapNote.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").parse(timestamp));
          existingMapRecord.addMapNote(previousMapNote);
        }      
      }
      noteReader.close();
    }

    finally {
      mappingService.close();
    }
   
    return existingMapRecord;
    
  }

  /* see superclass */
  @Override
  public void preReleaseProcessing() throws Exception {

    // Delete all complex_map_refset_members for the project
    // Identify all maps in READY_FOR_PUBLICATION
    // Lookup maps for those concepts in snowstorm
    // If norway moduleId map:
    //   *Copy existing maps into complex_map_refset_members, so Delta can be generated, and it will have correct UUIDs.
    // If no norway, but INT moduleId map:
    //   *Pull existing maps from snowstorm and compare to mapping tool map records
    //   *If exact match across the entire map record, we do NOT want these to be in the release.  Delete them from the scope so they don’t get included in the release (don’t remove the map records yet, this will be handled later).
    //   *If not exact match across the entire map record, we want these in the release, but with new UUIDs and the norway moduleId.  Keep records in scope, but don’t copy the snowstorm maps into complex_map_refset_members.

    
    Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Starting pre-release processes for " + mapProject.getRefSetId());
    
    final MappingService mappingService = new MappingServiceJpa();
    MapRecord existingMapRecord = null;   
    
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // Remove previous release information for the project
    Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Removing all previous release information for project");
    int commitCount = 0;
    for (final ComplexMapRefSetMember member : contentService
        .getComplexMapRefSetMembersForRefSetId(mapProject.getRefSetId()).getIterable()) {
      if (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap) {
        contentService.removeComplexMapRefSetMember(member.getId());
      } else {
        contentService.removeSimpleMapRefSetMember(member.getId());
      }
      if(++commitCount % 5000 == 0) {
        Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("    count = " + commitCount);
        contentService.commit();
        contentService.clear();
        contentService.beginTransaction();
      }
    }
    
    contentService.commit();
    contentService.clear();
    contentService.beginTransaction();
    
    // Identify ready for publication maps
    final Set<MapRecord> readyForPublicationMapRecords = new HashSet<>();
    Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Identifying ready for publication maps");
    
    final MapRecordList mapRecords =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    for(MapRecord mapRecord : mapRecords.getMapRecords()) {
      if(mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
        readyForPublicationMapRecords.add(mapRecord);
      }
    }
    
    // Lookup maps for ready for publication concepts in snowstorm
    Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Looking up ready for publication maps in snowstorm");
    Set<String> scopeConcepts = mapProject.getScopeConcepts();
    for(MapRecord mapRecord : readyForPublicationMapRecords) {

      // If norway moduleId map exists:
      //   *Copy existing maps into complex_map_refset_members, so Delta can be generated, and it will have correct UUIDs.     
      MapRecord noMapRecord = pullMapRecordFromSnowstorm("51000202101", "447562003", mapRecord);
      if(noMapRecord != null) {
        addComplexMapRefsetMembersForMap("51000202101", "447562003", mapRecord);
        continue;
      }
      
      // If no norway, but INT moduleId map:
      //   *Pull existing maps from snowstorm and compare to mapping tool map records
      //   *If exact match across the entire map record, we do NOT want these to be in the release, as it would create a Norway moduleId version.
      //      Delete them from the scope so they don’t get included in the release (don’t remove the map records yet, this will be handled later).
      //   *If not exact match across the entire map record, we want these in the release, but with new UUIDs and the norway moduleId.  
      //      Keep records in scope, but don’t copy the snowstorm maps into complex_map_refset_members.
      MapRecord intMapRecord = pullMapRecordFromSnowstorm("449080006", "447562003", mapRecord);
      if(intMapRecord != null) {
        Boolean mapRecordsMatch = true;
        if(intMapRecord.getMapEntries().size() != mapRecord.getMapEntries().size()) {
          mapRecordsMatch = false;
        }
        //Check each entry
        else {
          int checkedEntries = 0;
          for(int i = 0; i < mapRecord.getMapEntries().size(); i++) {
            for (int j = 0; j < intMapRecord.getMapEntries().size(); j++) {
              if(mapRecord.getMapEntries().get(i).getMapGroup() == intMapRecord.getMapEntries().get(j).getMapGroup() &&
                  (mapRecord.getMapEntries().get(i).getMapPriority() == intMapRecord.getMapEntries().get(j).getMapPriority())) {
                if(!mapRecord.getMapEntries().get(i).isEquivalent(intMapRecord.getMapEntries().get(j))) {
                  mapRecordsMatch = false;
                  break;
                }
                else {
                  checkedEntries++;
                }
              }
            }
          }
          // Check for edge-case where compared maps have same number of entries, but different group and priority combinations
          if(checkedEntries != mapRecord.getMapEntries().size()) {
            mapRecordsMatch = false;
          }
        }
        
        //Delete 100% matching records from the scope
        if(mapRecordsMatch) {
          Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Unmodified International map identified for concept " + mapRecord.getConceptId() + " - removing from scope and not publishing.");
          scopeConcepts.remove(mapRecord.getConceptId());
        }
        else {
          //If records don't match, we can leave things as they are.
        }
        
      }
    }
    
    mapProject.setScopeConcepts(scopeConcepts);
    mappingService.updateMapProject(mapProject);
    
    contentService.commit();
    contentService.close();
    mappingService.close();
        
  }
  
  /* see superclass */
  @Override
  public void postReleaseProcessing(String effectiveTime) throws Exception {

    //There is an edge-case where if only certain fields are changed (e.g. map advice) 
    //in a map pulled from the International module,
    //it will generate the same UUID as the international map, causing a duplicate UUID.
    //For each UUID about to be uploaded, check them against the international module refset.
    //Each UUID that exists in the international module needs to be replaced with a newly generated UUID.
    
    Logger.getLogger(ICD10NOMaintProjectSpecificAlgorithmHandler.class).info("Starting post-release processes for " +  mapProject.getRefSetId());
    
    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
      }
 
    
    final String snowstormBaseURL = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.baseUrl");
    if (snowstormBaseURL == null) {
      throw new Exception("Config file must specify a snowstormAPI.baseUrl property");
    }

    final String icd10noBranch = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.ICD10NOBranch");
    if (icd10noBranch == null) {
      throw new Exception("Config file must specify a snowstormAPI.ICD10NOBranch property");
    }
    
      // Iterate through the Delta file
    String releaseDir = dataDir + "/doc/release/" + mapProject.getSourceTerminology() + "_to_"
        + mapProject.getDestinationTerminology() + "_" + mapProject.getRefSetId() + "/"
        + effectiveTime + "/";
    releaseDir.replaceAll("\\s", "");
    
    String releaseDeltaFile = releaseDir + "der2_iisssccRefset_" + mapProject.getMapRefsetPattern() + "Delta_"
        + this.getReleaseFile3rdElement() + "_" + effectiveTime + ".txt";
    
    BufferedReader fileReader = new BufferedReader(new FileReader(releaseDeltaFile));

    String line = null;

    Set<String> allUUIDs = new HashSet<>();
    Map<String,String> UUIDReplacements = new HashMap<>();
    while ((line = fileReader.readLine()) != null) {
      String fields[] = line.split("\t");
      
      if(fields[0].equals("id")) {
        //Skip header row
      }
      else {
        allUUIDs.add(fields[0]);        
      }
    }
    
    fileReader.close();
    
    //For each UUID, look them up in snowstorm.  If they are associated with an 
    //international module map, they must be replaced
    for(String thisUUID : allUUIDs) {
      final String url =
          snowstormBaseURL + icd10noBranch + "/members/";

      final Client client = ClientBuilder.newClient();
      final String accept = "*/*";

      final ObjectMapper mapper = new ObjectMapper();

      String targetUri = url + thisUUID;
      WebTarget target = client.target(targetUri);
      target = client.target(targetUri);
      Logger.getLogger(getClass()).info(targetUri);

      Response response = target.request(accept).get();
      String resultString = response.readEntity(String.class);
      if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
        // n/a
      } else {
        //Unsuccessful call due to UUID not existing.  This is good - go to next UUID.
        continue;
      }

      final JsonNode doc = mapper.readTree(resultString);

      MapRecord existingMapRecord = null;

     //If the UUID is associated with the International moduleId, generate a random replacement
     JsonNode mapModuleId = doc.get("moduleId");
     if(mapModuleId.asText().equals("449080006")) {
       UUIDReplacements.put(thisUUID, ConfigUtility.getReleaseUuid(UUID.randomUUID().toString()).toString());
      }
    }
    
    //If there are any replacements required, replace them directly in the release file
    if(UUIDReplacements.size() > 0) {
      String fileContent = "";
      
      fileReader = new BufferedReader(new FileReader(releaseDeltaFile));
      
      line = null;

      //Read the contents from the Delta
      while ((line = fileReader.readLine()) != null) {
        fileContent = fileContent + line + System.lineSeparator();
      }
      //Replace the UUIDs as needed
      for(String oldUUID : UUIDReplacements.keySet()) {
        String newUUID = UUIDReplacements.get(oldUUID);
        fileContent = fileContent.replaceAll(oldUUID, newUUID);
      }
      //Write the updated contents back to the delta
      FileWriter writer = new FileWriter(releaseDeltaFile);
      writer.write(fileContent);
      writer.close();
      
    }
    
    fileReader.close();

  }

  
  /**
   * Computes the map relation for the SNOMEDCT to ICD10NO map project. Based
   * solely on whether an entry has a TRUE rule or not. No advices are computed
   * for this project.
   *
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the map relation
   * @throws Exception the exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

    if (mapEntry == null) {
      return null;
    }
    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null
          && mapEntry.getMapRelation().isAllowableForNullTarget())
        return mapEntry.getMapRelation();
      else {
        // retrieve the not classifiable relation
        // 447638001 - Map source concept cannot be classified with available
        // data
        for (final MapRelation relation : mapProject.getMapRelations()) {
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
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if entry has an age rule
    } else if (mapEntry.getRule().contains("AGE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if the entry has a non-gender, non-age IFA
    } else if (mapEntry.getRule().startsWith("IFA")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // using contains here to capture TRUE and OTHERWISE TRUE
    } else if (mapEntry.getRule().contains("TRUE")) {

      // retrieve the relations by terminology id
      for (final MapRelation relation : mapProject.getMapRelations()) {
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
  
  private MapRecord pullMapRecordFromSnowstorm(String moduleId, String refsetId,
    MapRecord mapRecord) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    final String snowstormBaseURL = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.baseUrl");
    if (snowstormBaseURL == null) {
      throw new Exception("Config file must specify a snowstormAPI.baseUrl property");
    }

    final String icd10noBranch = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.ICD10NOBranch");
    if (icd10noBranch == null) {
      throw new Exception("Config file must specify a snowstormAPI.ICD10NOBranch property");
    }
    
    try {

      final String url =
          snowstormBaseURL + "browser/" + icd10noBranch + "/members";

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

  private void addComplexMapRefsetMembersForMap(String moduleId, String refsetId,
    MapRecord mapRecord) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    final String snowstormBaseURL = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.baseUrl");
    if (snowstormBaseURL == null) {
      throw new Exception("Config file must specify a snowstormAPI.baseUrl property");
    }

    final String icd10noBranch = ConfigUtility.getConfigProperties().getProperty("snowstormAPI.ICD10NOBranch");
    if (icd10noBranch == null) {
      throw new Exception("Config file must specify a snowstormAPI.ICD10NOBranch property");
    }
    
    try {

      final String url =
          snowstormBaseURL + "browser/" + icd10noBranch + "/members";

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

      ComplexMapRefSetMember mapRefsetMember = null;

      for (final JsonNode mapNode : doc.get("items")) {
        JsonNode mapActive = mapNode.get("active");
        JsonNode mapDeleted = mapNode.get("deleted");
        JsonNode mapMemberId = mapNode.get("memberId");
        JsonNode mapReleasedEffectiveTime = null;
        if(mapNode.has("releasedEffectiveTime")) {
          mapReleasedEffectiveTime = mapNode.get("releasedEffectiveTime");
        }

        // Only load in non-deleted map records
        if (mapDeleted.asText().equalsIgnoreCase("true")) {
          continue;
        }

        // Create the refset member and populate
        mapRefsetMember = new ComplexMapRefSetMemberJpa();
        mapRefsetMember.setActive(Boolean.parseBoolean(mapActive.asText()));
        mapRefsetMember.setRefSetId(refsetId);
        mapRefsetMember.setModuleId(Long.parseLong(moduleId));
        mapRefsetMember.setTerminologyId(mapMemberId.asText());
        mapRefsetMember.setTerminology(mapProject.getSourceTerminology());
        mapRefsetMember.setTerminologyVersion(mapProject.getSourceTerminologyVersion());
        if(mapReleasedEffectiveTime != null) {
          mapRefsetMember.setEffectiveTime(new SimpleDateFormat("yyyyMMdd").parse(mapReleasedEffectiveTime.asText()));
        }
        //If map has no releasedEffectiveTime, set placeholder date
        else {
          mapRefsetMember.setEffectiveTime(new SimpleDateFormat("yyyyMMdd").parse("19990101"));
        }
                
        Concept sourceConcept = contentService.getConcept(mapRecord.getConceptId(),
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
        if (sourceConcept != null) {
          mapRefsetMember.setConcept(sourceConcept);
        } else {
          throw new Exception("Could not find source concept: " + mapRecord.getConceptId() + " for map record id" + mapRecord.getId());
        }

        JsonNode additionalFields = mapNode.get("additionalFields");

        mapRefsetMember.setMapGroup(additionalFields.get("mapGroup").asInt());
        mapRefsetMember.setMapPriority(additionalFields.get("mapPriority").asInt());
        mapRefsetMember.setMapRule(additionalFields.get("mapRule").asText());
        mapRefsetMember.setMapAdvice(additionalFields.get("mapAdvice").asText());
        mapRefsetMember.setMapRelationId(Long.parseLong(additionalFields.get("mapCategoryId").asText()));
        mapRefsetMember.setMapTarget(additionalFields.get("mapTarget").asText());
          
        contentService.addComplexMapRefSetMember(mapRefsetMember);

      }

      response.close();

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

}
