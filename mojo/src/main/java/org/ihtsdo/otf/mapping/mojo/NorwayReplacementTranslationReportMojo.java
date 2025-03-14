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
import java.util.Collections;
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
 * Run the weekly SQLdump report and email
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-norway-replacement-translation-report
 */
public class NorwayReplacementTranslationReportMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start Norway Replacement Translation Report Mojo");

    setupBindInfoPackage();
    
    try {
        runReport();

    } catch (Exception e) {
      getLog().error("Error running Norway Replacement Translation Report Mojo.", e);
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
        NorwayReplacementTranslationReportMojo.this.manager = manager;
      }
    }; MappingService mappingService = new MappingServiceJpa();) {


      final Client client = ClientBuilder.newClient();
      final String accept = "application/json";

      String searchAfter = null;
      final ObjectMapper mapper = new ObjectMapper();

    getLog().info("Identify full list of concepts Norway is interested in, as stored in the Bokmål and Nynorsk langauge refsets (both standard and 'for General Practitioners' versions)");
    //Sample JSON
    /*
{
  "items": [
    {
      "active": true,
      "moduleId": "51000202101",
      "released": true,
      "releasedEffectiveTime": 20221015,
      "memberId": "fffff893-1c68-4dc7-8cee-d6ccc3b823e0",
      "refsetId": "61000202103",
      "referencedComponentId": "5215891000202119",
      "additionalFields": {
        "acceptabilityId": "900000000000548007"
      },
      "referencedComponent": {
        "active": true,
        "moduleId": "51000202101",
        "released": true,
        "releasedEffectiveTime": 20221015,
        "descriptionId": "5215891000202119",
        "term": "inneklemt diafragmahernie",
        "conceptId": "196904003",
        "typeId": "900000000000013009",
        "acceptabilityMap": {
          "61000202103": "PREFERRED"
        },
        "type": "SYNONYM",
        "lang": "no",
        "caseSignificance": "CASE_INSENSITIVE",
        "effectiveTime": "20221015"
      },
      "effectiveTime": "20221015"
    },
   */
    
    Set<String> bokmalRefsetsConceptIds = new HashSet<>();
    Set<String> nynorskRefsetsConceptIds = new HashSet<>();
    Set<String> bokmalGPRefsetsConceptIds = new HashSet<>();
    Set<String> nynorskGPRefsetsConceptIds = new HashSet<>();
    Set<String> allLangRefsetsConceptIds = new HashSet<>();

    bokmalRefsetsConceptIds.addAll(langRefsetLookup("61000202103"));
    allLangRefsetsConceptIds.addAll(bokmalRefsetsConceptIds);
    nynorskRefsetsConceptIds.addAll(langRefsetLookup("91000202106"));
    allLangRefsetsConceptIds.addAll(nynorskRefsetsConceptIds);
    bokmalGPRefsetsConceptIds.addAll(langRefsetLookup("47351000202107"));
    allLangRefsetsConceptIds.addAll(bokmalGPRefsetsConceptIds);
    nynorskGPRefsetsConceptIds.addAll(langRefsetLookup("188001000202106"));
    allLangRefsetsConceptIds.addAll(nynorskGPRefsetsConceptIds);
      
      getLog().info("Identify current and dependent branch paths");
      //Sample JSON
      /*
{
  "items": [
    {
      "name": "Norwegian Edition",
      "owner": "The Norwegian Directorate of eHealth",
      "shortName": "SNOMEDCT-NO",
      "branchPath": "MAIN/SNOMEDCT-NO",
      "dependantVersionEffectiveTime": 20230131,
      "dailyBuildAvailable": true,
      "latestDailyBuild": "2023-03-10-050823",
      "countryCode": "no",
      "latestVersion": {
        "shortName": "SNOMEDCT-NO",
        "importDate": "2022-10-14T11:19:37.529Z",
        "parentBranchPath": "MAIN/SNOMEDCT-NO", <--Look for concepts inactive in this branch
        "effectiveDate": 20221015,
        "version": "2022-10-15",
        "description": "SNOMEDCT-NO 20221015 import.",
        "dependantVersionEffectiveTime": 20220831,
        "branchPath": "MAIN/SNOMEDCT-NO/2022-10-15" <---And active in this branch
      },
     */
      String currentBranch = null;
      String previousVersionBranch = null;

      int returnedConceptsCount = 0;
      
      String targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/codesystems?forBranch=MAIN%2FSNOMEDCT-NO%2FREFSETS";
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
      
      JsonNode doc = mapper.readTree(resultString);
   
      for (final JsonNode node : doc.get("items")) {
        currentBranch = node.get("branchPath").asText();
        
        final JsonNode latestVersionNode = node.get("latestVersion");
        previousVersionBranch = latestVersionNode.get("branchPath").asText();
        
        //We're only interested in the first node
        break;
      }
     
      
      getLog().info("Identify all in-scope concepts that are active in the previousVersionBranch, and inactive in the currentBranch");
      //Sample JSON
      /*
{
  "items": [
    "99999003",
    "99998006",
    "99997001",
    "99996005",
    "99995009",
    "99994008",
    "99993002",
    "99992007",
     */
      
      Set<String> activeInPreviousBranchScopeConcepts = new HashSet<>();
      searchAfter = null;
      int limit = 10000;
      
      while (true) {
        
        returnedConceptsCount = 0;
        
        targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/"+previousVersionBranch.replaceAll("/", "%2F")+"/concepts?activeFilter=true&returnIdOnly=true&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        response =
            target.request(accept)
            .get();
        resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        doc = mapper.readTree(resultString);
        
        // get total amount
        // Get concepts returned in this call (up to 10000)
        final JsonNode conceptIds = doc.get("items");

        for(int i = 0; i<conceptIds.size(); i++) {
          String activeConceptId = conceptIds.get(i).asText();
        if(allLangRefsetsConceptIds.contains(activeConceptId)) {
          activeInPreviousBranchScopeConcepts.add(activeConceptId);
        }

          returnedConceptsCount++;
        }
         
        searchAfter = doc.get("searchAfter").asText();
        // if we don't get a full page of results, we've processed the final page
        if(returnedConceptsCount < limit) {
            break;
        }
      }  
      
      Set<String> scopeConceptsInactivatedSincePreviousBranch = new HashSet<>();
      searchAfter = null;
      limit = 10000;
      
      while (true) {
        
        returnedConceptsCount = 0;
        
        targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/"+currentBranch.replaceAll("/", "%2F")+"/concepts?activeFilter=false&returnIdOnly=true&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        response =
            target.request(accept)
            .get();
        resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        doc = mapper.readTree(resultString);
        
        // get total amount
        // Get concepts returned in this call (up to 10000)
        final JsonNode inactiveConceptIds = doc.get("items");

        for(int i = 0; i<inactiveConceptIds.size(); i++) {
          returnedConceptsCount++;
          
          String inactiveConceptId = inactiveConceptIds.get(i).asText();
          //Don't include "garbage SCTIDs", defined as concepts that contain "10002" as the
          //7th through 11th characters.
          //E.g. "1217661000202103"
          //From Cato Spook in Norway:
          //   They got their nickname because earlier we made them as a plan B to satisfy 
          //   Helseplattformen who wanted SCTIDs to ICD-10 codes that did not have a SCTID 
          //   mapping to them. You can see the ICD-10 code in the square brackets in the FSN. 
          //   When we find a reason to inactivate these SCTIDs we do it, because they are very 
          //   primitive and don’t have the functionality of a properly made SCTID. They are just 
          //   inactivated without replacements because they are “garbage”. We don’t need them 
          //   in our list.
          if(inactiveConceptId.length()>11 && inactiveConceptId.substring(6, 11).equals("10002")) {
            continue;
          }
          
          if(activeInPreviousBranchScopeConcepts.contains(inactiveConceptId)) {
            scopeConceptsInactivatedSincePreviousBranch.add(inactiveConceptId);
          }

        }
         
        searchAfter = doc.get("searchAfter").asText();
        // if we don't get a full page of results, we've processed the final page
        if(returnedConceptsCount < limit) {
            break;
        }
      }                
      
      getLog().info("Grab all replacement concepts");
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
      List<String> inactivedScopeConcepts = new ArrayList<>(scopeConceptsInactivatedSincePreviousBranch);
      int batchSize = 100;
      int counter = 0;

      int inactiveConceptsCount = inactivedScopeConcepts.size();
      boolean reachedFinalConcept= false;
      Set<String> replacementConceptIds = new HashSet<>();
      
      while (!reachedFinalConcept && inactiveConceptsCount != 0) {
        
        targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/browser/MAIN%2FSNOMEDCT-NO/concepts?";
        for(int i=0; i < batchSize; i++ ) {
          targetUri = targetUri + "conceptIds=" + inactivedScopeConcepts.get(counter) + "&";
          counter++;
          if(counter >= inactiveConceptsCount) {
            reachedFinalConcept = true;
            targetUri = targetUri + "limit=10000";
            break;
          }
        }
        
        target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        doc = mapper.readTree(resultString);
     
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
      
      getLog().info("Grab all inactive and replacement concept descriptions");
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
      inactiveAndReplacementTargets.addAll(inactivedScopeConcepts);
      inactiveAndReplacementTargets.addAll(replacementConceptIds);
      
      int inactiveAndReplacementConceptsCount = inactiveAndReplacementTargets.size();
      
      reachedFinalConcept= false;
      
      while (!reachedFinalConcept && inactiveAndReplacementConceptsCount != 0) {
        
        returnedConceptsCount = 0;
        
        targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO/descriptions?";
        for(int i=0; i < batchSize; i++ ) {
          targetUri = targetUri + "conceptIds=" + inactiveAndReplacementTargets.get(counter) + "&";
          counter++;
          if(counter >= inactiveAndReplacementConceptsCount) {
            reachedFinalConcept = true;
            break;
          }
        }

        targetUri = targetUri + "limit=10000";
        
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);
     
        response =
            target.request(accept)
            .header("Cookie", ConfigUtility.getGenericUserCookie())
            .get();
        resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }
        
        doc = mapper.readTree(resultString);
     
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
          "Id\tFSN\tSemTag\tTranslation(s)\tBokmål\tNynorsk\tBokmål GP\tNynorsk GP\tReason\tAssoc Type\tAssoc Id\tAssoc FSN\tAssoc Translation(s)");

      // Add result rows, in conceptId order
      Collections.sort(inactivedScopeConcepts);
      for (String conceptId : inactivedScopeConcepts) {
        
        
        final String inactivationIndicator = conceptInactivationIndicators.get(conceptId);
        final String inactiveConceptInfo = 
            conceptId + "\t" +
            conceptIdToFSN.get(conceptId) + "\t" +
            conceptIdToFSN.get(conceptId).substring(conceptIdToFSN.get(conceptId).indexOf("("), conceptIdToFSN.get(conceptId).indexOf(")") + 1) + "\t" +
            (conceptIdToPTNO.get(conceptId)!=null ? conceptIdToPTNO.get(conceptId) : "") + "\t"+
            (bokmalRefsetsConceptIds.contains(conceptId) ? "TRUE" : "FALSE") + "\t" +
            (nynorskRefsetsConceptIds.contains(conceptId) ? "TRUE" : "FALSE") + "\t" +
            (bokmalGPRefsetsConceptIds.contains(conceptId) ? "TRUE" : "FALSE") + "\t" +
            (nynorskGPRefsetsConceptIds.contains(conceptId) ? "TRUE" : "FALSE") + "\t" +            
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
              (conceptIdToPTNO.get(targetId)!=null ? conceptIdToPTNO.get(targetId) : "");

              results.add(inactiveConceptInfo + "\t" + targetConceptInfo);
            }
          }
        }
      }

      // Add results to file
      final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      final String dateStamp = dateFormat.format(new Date());

      final String filename = "/replacmentTranslationReport_" + dateStamp;

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
          "Norway Replacement Translation Report mojo failed to complete", e);
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
        "Hello,\n\nThe Norway replacement translation report has been generated.";

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
        "[OTF-Mapping-Tool] Norway Replacement Translation Report", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
  
  private Set<String> langRefsetLookup(String refsetId) throws Exception {

    final Client client = ClientBuilder.newClient();
    final String accept = "application/json";

    String searchAfter = null;
    final ObjectMapper mapper = new ObjectMapper();

    
    int limit = 10000;
    Set<String> langRefsetsConceptIds = new HashSet<>();
    
    while (true) {
      
      int returnedConceptsCount = 0;
      
      
      String targetUri = "https://dailybuild.terminologi.helsedirektoratet.no/snowstorm/snomed-ct/MAIN%2FSNOMEDCT-NO%2FREFSETS/members?referenceSet="+refsetId+"&limit="+limit+ (searchAfter != null ? "&searchAfter=" + searchAfter : "");
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
        final JsonNode referencedComponentNode = conceptNode.get("referencedComponent");
        if(referencedComponentNode != null) {
          langRefsetsConceptIds.add(referencedComponentNode.get("conceptId").asText());
          returnedConceptsCount++;
        }
      }
       
      searchAfter = doc.get("searchAfter").asText();
      // if we don't get a full page of results, we've processed the final page
      if(returnedConceptsCount < limit) {
          break;
      }
    }   
    
    client.close();   
    
    return langRefsetsConceptIds;
  }

}
