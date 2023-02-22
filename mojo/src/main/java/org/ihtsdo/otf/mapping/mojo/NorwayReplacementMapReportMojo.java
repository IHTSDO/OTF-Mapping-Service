/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Run the weekly SQLdump report and email to MedDRA staff
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-norway-replacement-map-report
 */
public class NorwayReplacementMapReportMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start Norway Replacement Map Report Mojo");

    setupBindInfoPackage();
    
    try {
        runReport();

    } catch (Exception e) {
      getLog().error("Error running Norway Replacement Map Report Mojo.", e);
    }
  }

  /**
   * Run report.
   *
   * @throws Exception the exception
   */
  private void runReport() throws Exception {
    try (ContentService service = new ContentServiceJpa() {
      {
        NorwayReplacementMapReportMojo.this.manager = manager;
      }
    }; MappingService mappingService = new MappingServiceJpa();) {


      final Client client = ClientBuilder.newClient();
      final String accept = "application/json";

      String searchAfter = null;
      final ObjectMapper mapper = new ObjectMapper();

      getLog().info("Grab all inactive concepts from prod-snowstorm");
      //Sample JSON
      /*
{
  "items": [
    {
      "conceptId": "99999003",
      "active": false,
      "definitionStatus": "PRIMITIVE",
      "moduleId": "900000000000207008",
      "effectiveTime": "20090731",
      "fsn": {
        "term": "BISMUSAL SUSPENSION (product)",
        "lang": "en"
      },
      "pt": {
        "term": "BISMUSAL SUSPENSION",
        "lang": "en"
      },
      "id": "99999003",
      "idAndFsnTerm": "99999003 | BISMUSAL SUSPENSION (product) |"
    },
     */
      
      Set<String> inactiveConceptIds = new HashSet<>();
      int limit = 10000;
      
      while (true) {
        
        int returnedConceptsCount = 0;
        
        String targetUri = "https://prod-snowstorm.ihtsdotools.org/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO/concepts?activeFilter=false&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        Response response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        final JsonNode doc = mapper.readTree(resultString);
     
        // get total amount
        // Get concepts returned in this call (up to 1000)
        for (final JsonNode conceptNode : doc.get("items")) {
          inactiveConceptIds.add(conceptNode.get("conceptId").asText());
          returnedConceptsCount++;
        }
         
        searchAfter = doc.get("searchAfter").asText();
        // if we don't get a full page of results, we've processed the final page
        if(returnedConceptsCount < limit) {
            break;
        }
      }
            
      
      getLog().info("Grab all active ICD-10-NO Norwegian map information from dailybuild.terminologi.ehelse.no");
      //Sample JSON
      /*
{
  "items": [
    {
      "active": true,
      "moduleId": "449080006",
      "released": true,
      "releasedEffectiveTime": 20150731,
      "memberId": "ffff7fc0-eda4-5ac2-837f-a0638ae35961",
      "refsetId": "447562003",
      "referencedComponentId": "93342009",
      "additionalFields": {
        "mapCategoryId": "447637006",
        "mapRule": "TRUE",
        "mapAdvice": "ALWAYS Q50.6",
        "mapPriority": "1",
        "mapGroup": "1",
        "correlationId": "447561005",
        "mapTarget": "Q50.6"
      },
      "referencedComponent": {
        "conceptId": "93342009",
        "active": true,
        "definitionStatus": "FULLY_DEFINED",
        "moduleId": "900000000000207008",
        "fsn": {
          "term": "Congenital malposition of fallopian tube (disorder)",
          "lang": "en"
        },
        "pt": {
          "term": "Congenital malposition of fallopian tube",
          "lang": "en"
        },
        "id": "93342009"
      },
      "effectiveTime": "20150731"
    },
     */
      
      Map<String,Set<String>> mappedConceptsAndTargets = new HashMap<>();
      List<String> inactiveMappedConcepts = new ArrayList<>();
      searchAfter = null;
      limit = 10000;
      
      while (true) {
        
        int returnedMapsCount = 0;
        
        String targetUri = "https://dailybuild.terminologi.ehelse.no/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO%2FREFSETS-ICD10/members?referenceSet=447562003&active=true&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        Response response =
            target.request(accept)
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        final JsonNode doc = mapper.readTree(resultString);
     
        // get total amount
        // Get concepts returned in this call (up to 10000)
        for (final JsonNode mapNode : doc.get("items")) {
          final String conceptId = mapNode.get("referencedComponentId").asText();

          if(inactiveConceptIds.contains(conceptId)) {
            inactiveMappedConcepts.add(conceptId);
          }
          
          if(!mappedConceptsAndTargets.containsKey(conceptId)) {
            Set<String> targetSet = new HashSet<>();
            mappedConceptsAndTargets.put(conceptId, targetSet);
          }
          final JsonNode additionalFieldsNode = mapNode.get("additionalFields");
          if(additionalFieldsNode != null && additionalFieldsNode.findValue("mapTarget") != null) {
            Set<String> targetSet = mappedConceptsAndTargets.get(conceptId);
            targetSet.add(additionalFieldsNode.get("mapTarget").asText());
            mappedConceptsAndTargets.put(conceptId, targetSet);
          }

          returnedMapsCount++;
        }
         
        searchAfter = doc.get("searchAfter").asText();
        // if we don't get a full page of results, we've processed the final page
        if(returnedMapsCount < limit) {
            break;
        }
      }      
      
      getLog().info("Grab active ICD-10 INT map information from prod-snowstorm, overriding maps from dailybuild server as needed");
      //Sample JSON
      /*
{
  "items": [
    {
      "active": true,
      "moduleId": "449080006",
      "released": true,
      "releasedEffectiveTime": 20221231,
      "memberId": "ffffab0f-3458-5f9b-aa88-e903aff7133b",
      "refsetId": "447562003",
      "referencedComponentId": "1179784006",
      "additionalFields": {
        "mapCategoryId": "447638001",
        "mapRule": "TRUE",
        "mapAdvice": "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
        "mapPriority": "1",
        "mapGroup": "1",
        "correlationId": "447561005",
        "mapTarget": ""
      },
      "referencedComponent": {
        "conceptId": "1179784006",
        "active": true,
        "definitionStatus": "FULLY_DEFINED",
        "moduleId": "900000000000207008",
        "fsn": {
          "term": "Folic acid within reference range (finding)",
          "lang": "en"
        },
        "pt": {
          "term": "Folic acid within reference range",
          "lang": "en"
        },
        "id": "1179784006"
      },
      "effectiveTime": "20221231"
    },
     */
      
      searchAfter = null;
      limit = 10000;
      Set<String> NOMapOverrideCandidates = new HashSet<>(mappedConceptsAndTargets.keySet());
      
      while (true) {
        
        int returnedMapsCount = 0;
        
        String targetUri = "https://prod-snowstorm.ihtsdotools.org/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO/members?referenceSet=447562003&active=true&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        Response response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        final JsonNode doc = mapper.readTree(resultString);
     
        // get total amount
        // Get concepts returned in this call (up to 10000)
        for (final JsonNode mapNode : doc.get("items")) {
          final String conceptId = mapNode.get("referencedComponentId").asText();

          if(inactiveConceptIds.contains(conceptId)) {
            inactiveMappedConcepts.add(conceptId);
          }
          
          //If this map was populated from the NO server, override it with the map from the INT server.
          //Only do this once per concept (to avoid repeatedly removing target information)
          if(mappedConceptsAndTargets.containsKey(conceptId) && NOMapOverrideCandidates.contains(conceptId)) {
            Set<String> targetSet = new HashSet<>();
            mappedConceptsAndTargets.put(conceptId, targetSet);
            NOMapOverrideCandidates.remove(conceptId);
          }
          
          if(!mappedConceptsAndTargets.containsKey(conceptId)) {
            Set<String> targetSet = new HashSet<>();
            mappedConceptsAndTargets.put(conceptId, targetSet);
          }
          final JsonNode additionalFieldsNode = mapNode.get("additionalFields");
          if(additionalFieldsNode != null && additionalFieldsNode.findValue("mapTarget") != null) {
            Set<String> targetSet = mappedConceptsAndTargets.get(conceptId);
            targetSet.add(additionalFieldsNode.get("mapTarget").asText());
            mappedConceptsAndTargets.put(conceptId, targetSet);
          }

          returnedMapsCount++;
        }
         
        searchAfter = doc.get("searchAfter").asText();
        // if we don't get a full page of results, we've processed the final page
        if(returnedMapsCount < limit) {
            break;
        }
      }            
      
      getLog().info("Grab all replacement concepts from prod-snowstorm");
      //Sample JSON
      /*
{
  "items": [
    {
      "conceptId": "267214002",
      "fsn": {
        "term": "Congenital abnormality of uterus - baby delivered (disorder)",
        "lang": "en"
      },
      "pt": {
        "term": "Congenital abnormality of uterus - baby delivered",
        "lang": "en"
      },
      "active": false,
      "effectiveTime": "20230131",
      "released": true,
      "releasedEffectiveTime": 20230131,
      "inactivationIndicator": "CLASSIFICATION_DERIVED_COMPONENT",
      "associationTargets": {
        "PARTIALLY_EQUIVALENT_TO": [
          "267212003",
          "289256000"
        ]
      },
     */
      
      Map<String,Map<String,String>> conceptAssociationTargets = new HashMap<>();
      Map<String,String> conceptInactivationIndicators = new HashMap<>();
      int batchSize = 100;
      int counter = 0;

      int inactiveConceptsCount = inactiveMappedConcepts.size();
      boolean reachedFinalConcept= false;
      Set<String> replacementConceptIds = new HashSet<>();
      
      while (!reachedFinalConcept) {
        
        String targetUri = "https://prod-snowstorm.ihtsdotools.org/snowstorm/snomed-ct/browser/MAIN%2FSNOMEDCT-NO/concepts?";
        for(int i=0; i < batchSize; i++ ) {
          targetUri = targetUri + "conceptIds=" + inactiveMappedConcepts.get(counter) + "&";
          counter++;
          if(counter >= inactiveConceptsCount) {
            reachedFinalConcept = true;
            targetUri = targetUri + "limit=10000";
            break;
          }
        }
        
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        Response response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        final JsonNode doc = mapper.readTree(resultString);
     
        // get total amount
        // Get concepts returned in this call (up to 1000)
        for (final JsonNode conceptNode : doc.get("items")) {
          String conceptId = conceptNode.get("conceptId").asText();
          String inactivationIndicator = conceptNode.get("inactivationIndicator").asText().replaceAll("_", " ");
          conceptInactivationIndicators.put(conceptId, inactivationIndicator);
          
          JsonNode associationTargetsNode = conceptNode.findValue("associationTargets");
          if (associationTargetsNode == null || associationTargetsNode.size() == 0
              || associationTargetsNode.fields() == null) {
            conceptAssociationTargets.put(conceptId, null);
            continue;
          }

          Entry<String, JsonNode> entry = associationTargetsNode.fields().next();
          String associationType = entry.getKey().replaceAll("_"," ") + " association reference set";
          String values = entry.getValue().toString();
          if (values.contains("[")) {
            values = values.substring(1, values.length() - 1);
          }
          values = values.replaceAll("\"", "");
          // Keep track of all replacement concept Ids for description lookup later
          for (String value : values.split(",")) {
            replacementConceptIds.add(value);
          }
          Map<String,String> associationTargets = new HashMap<>();
          associationTargets.put(associationType,values);
          conceptAssociationTargets.put(conceptId, associationTargets);
        }
      }            
      
      getLog().info("Grab all inactive and replacement concept descriptions from prod-snowstorm");
      //Sample JSON
      /*
{
{
  "items": [
    {
      "active": true,
      "moduleId": "51000202101",
      "released": true,
      "releasedEffectiveTime": 20220415,
      "descriptionId": "2344071000202112",
      "term": "utilsiktet forgiftning med intravenøst bedøvelsesmiddel",
      "conceptId": "216579009",
      "typeId": "900000000000013009",
      "acceptabilityMap": {
        "61000202103": "ACCEPTABLE"
      },
      "type": "SYNONYM",
      "lang": "no",
      "caseSignificance": "CASE_INSENSITIVE",
      "effectiveTime": "20220415"
    },
     */
      
      Map<String,String> conceptIdToFSN = new HashMap<>();
      Map<String,String> conceptIdToPTEN = new HashMap<>();
      Map<String,String> conceptIdToPTNO = new HashMap<>();
      
      batchSize = 10;
      counter = 0;
      
      List<String> inactiveAndReplacementTargets = new ArrayList<>();
      inactiveAndReplacementTargets.addAll(inactiveMappedConcepts);
      inactiveAndReplacementTargets.addAll(replacementConceptIds);
      
      int inactiveAndReplacementConceptsCount = inactiveAndReplacementTargets.size();
      
      reachedFinalConcept= false;
      
      while (!reachedFinalConcept) {
        
        int returnedConceptsCount = 0;
        
        String targetUri = "https://prod-snowstorm.ihtsdotools.org/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO/descriptions?";
        for(int i=0; i < batchSize; i++ ) {
          targetUri = targetUri + "conceptIds=" + inactiveAndReplacementTargets.get(counter) + "&";
          counter++;
          if(counter >= inactiveAndReplacementConceptsCount) {
            reachedFinalConcept = true;
            targetUri = targetUri + "limit=10000";
            break;
          }
        }
        
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        Response response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        final JsonNode doc = mapper.readTree(resultString);
     
        // get total amount
        // Get concepts returned in this call (up to 1000)
        for (final JsonNode conceptNode : doc.get("items")) {
          String active = conceptNode.get("active").asText();
          if(active.equals("false")) {
            continue;
          }
          
          String conceptId = conceptNode.get("conceptId").asText();
          String type = conceptNode.get("type").asText();
          String term = conceptNode.get("term").asText();
          
          if(type.equals("FSN")) {
            conceptIdToFSN.put(conceptId, term);
            continue;
          }
          
          String lang = conceptNode.get("lang").asText();
          
          JsonNode acceptabilityMapNode = conceptNode.findValue("acceptabilityMap");
          if (acceptabilityMapNode == null || acceptabilityMapNode.size() == 0
              || acceptabilityMapNode.fields() == null) {
            conceptAssociationTargets.put(conceptId, null);
            continue;
          }

          // Grab American English preferred term
          if(lang.equals("en") && acceptabilityMapNode.findValue("900000000000509007") != null && acceptabilityMapNode.findValue("900000000000509007").asText().equals("PREFERRED")) {
            conceptIdToPTEN.put(conceptId, term);
            continue;
          }
          // Grab Nynorsk preferred term
          if(lang.equals("no") && acceptabilityMapNode.findValue("61000202103") != null && acceptabilityMapNode.findValue("61000202103").asText().equals("PREFERRED")) {
            conceptIdToPTNO.put(conceptId, term);
            continue;
          }
        }
      }
      

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "conceptId\tfsn.term\tpt.lang\tpt.term\ticD10\tinactivationIndicator.pt.term"+
          "\tassociations.association.pt.term\tassociations.targetId\tassociations.target.fsn.term\t"+
          "associations.target.pt.lang\tassociations.target.pt.term\tNy SCTID Erstatning OK\tassociations.targetICD\tOppr map Ja/Nei\tKommentar");

      // Add result rows
      for (String conceptId : inactiveMappedConcepts) {
        final String inactivationIndicator = conceptInactivationIndicators.get(conceptId);
        final String inactiveConceptInfo = 
            conceptId + "\t" +
            conceptIdToFSN.get(conceptId) + "\t" +
            (conceptIdToPTNO.get(conceptId)!=null ? "no" : "en") + "\t" +
            (conceptIdToPTNO.get(conceptId)!=null ? conceptIdToPTNO.get(conceptId) : conceptIdToPTEN.get(conceptId)) + "\t"+
            (mappedConceptsAndTargets.get(conceptId)!=null ? String.join(", ", mappedConceptsAndTargets.get(conceptId)) : "") + "\t"+
            inactivationIndicator;
        if(conceptAssociationTargets.get(conceptId) == null) {
          results.add(inactiveConceptInfo);
        }
        else {
          for(String associationTerm : conceptAssociationTargets.get(conceptId).keySet()) {
            String associationTargets = conceptAssociationTargets.get(conceptId).get(associationTerm);
            List<String> targetIds = Arrays.asList(associationTargets.split(","));
            for(String targetId : targetIds) {
              final String targetConceptInfo = 
                  associationTerm + "\t" + 
              targetId + "\t"+
              conceptIdToFSN.get(targetId) + "\t" +
              (conceptIdToPTNO.get(targetId)!=null ? "no" : "en") + "\t" +
              (conceptIdToPTNO.get(targetId)!=null ? conceptIdToPTNO.get(targetId) : conceptIdToPTEN.get(targetId)) + "\t"+
              "\t"+
              (mappedConceptsAndTargets.get(targetId)!=null ? String.join(", ", mappedConceptsAndTargets.get(targetId)) : "");
              results.add(inactiveConceptInfo + "\t" + targetConceptInfo);
            }
          }
        }
      }

      // Add results to file
      final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      final String dateStamp = dateFormat.format(new Date());

      final String filename = "/replacmentMapReport_ICD10NO_" + dateStamp;

      File resultFile =
          new File(System.getProperty("java.io.tmpdir") + filename + ".txt");
      getLog().info("Created result file: " + resultFile.getAbsolutePath());

      try (FileWriter writer = new FileWriter(resultFile);) {
        for (String str : results) {
          writer.write(str);
          writer.write(System.getProperty("line.separator"));
        }
      }

      // Zip results file
      File zipFile =
          new File(System.getProperty("java.io.tmpdir") + filename + ".zip");

      try (FileOutputStream fos = new FileOutputStream(zipFile);
          ZipOutputStream zipOut =
              new ZipOutputStream(new BufferedOutputStream(fos));
          FileInputStream fis = new FileInputStream(resultFile);) {

        ZipEntry ze = new ZipEntry(resultFile.getName());
        getLog().info("Zipping the file: " + resultFile.getName());
        zipOut.putNextEntry(ze);
        byte[] tmp = new byte[4 * 1024];
        int size = 0;
        while ((size = fis.read(tmp)) != -1) {
          zipOut.write(tmp, 0, size);
        }

      } catch (Exception e) {
        getLog().error(e);
      }

      // Send file to recipients
      sendEmail(zipFile.getAbsolutePath());

    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(
          "Norway Replacement Map Report mojo failed to complete", e);
    }
  }

  /**
   * Send email.
   *
   * @param fileName the file name
   * @throws Exception the exception
   */
  private void sendEmail(String fileName) throws Exception {

    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e1) {
      throw new MojoExecutionException("Failed to retrieve config properties");
    }
    String notificationRecipients =
        config.getProperty("report.send.notification.recipients.norway."
            + getClass().getSimpleName());
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage +=
        "Hello,\n\nThe Norway replacement map report has been generated.";

    String from;
    if (config.containsKey("mail.smtp.from")) {
      from = config.getProperty("mail.smtp.from");
    } else {
      from = config.getProperty("mail.smtp.user");
    }

    Properties props = new Properties();
    props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    props.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));

    ConfigUtility.sendEmailWithAttachment(
        "[OTF-Mapping-Tool] Norway Replacement Map Report", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
