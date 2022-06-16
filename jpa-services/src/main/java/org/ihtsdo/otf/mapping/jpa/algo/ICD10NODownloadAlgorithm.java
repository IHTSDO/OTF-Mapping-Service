package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ICD10NODownloadAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /**
   * Instantiates an empty {@link ICD10NODownloadAlgorithm}.
   *
   * @throws Exception the exception
   */
  public ICD10NODownloadAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void compute() throws Exception {
    Logger.getLogger(getClass()).info("Checking ICD10NO API for recent terminologies");
    
    //Example data from API
//    {
//    "data": [
//             {
//               "codeValue": "A00",
//               "nameNorwegian": "Kolera (cholera)",
//               "compactCode": "A00",
//               "parentCode": "A00-A09",
//               "active": true,
//               "statusChangedDate": "1998-12-31T23:00:00.000+00:00"
//             },
//             {
//               "codeValue": "A00-A09",
//               "nameNorwegian": "Infeksiøse tarmsykdommer",
//               "compactCode": "A00-A09",
//               "parentCode": "I",
//               "active": true,
//               "statusChangedDate": "1998-12-31T23:00:00.000+00:00"
//             },
//         ...
//         ],
//    "succeeded": true,
//    "pageNumber": 1,
//    "pageSize": 100,
//    "totalPages": 216,
//    "totalRecords": 21526
//    }
    
    // Set up parent-correction map
    final Map<String,String> parentCorrectionsMap = new HashMap<>();
    final String parentCorrectionsList = ConfigUtility.getConfigProperties().getProperty("icd10noAPI.parentCorrections");
    for (final String correctionPair : parentCorrectionsList.split(";")) {
      String[] values = correctionPair.split("\\|");
      final String fatCode = values[0];
      final String correctCode = values[1];
      parentCorrectionsMap.put(fatCode, correctCode);
    }
    
    // Pull ATC codes from API  
    final String url = ConfigUtility.getConfigProperties().getProperty("icd10noAPI.url");
    
     final Client client = ClientBuilder.newClient();
     final String accept = "*/*";
    
	 int pageNumber = 1;
	 final ObjectMapper mapper = new ObjectMapper();
	 
	 //housekeeping for creating folder later
	 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");  
	 LocalDateTime now = LocalDateTime.now();  
	 String dateFormat = dtf.format(now);
	 
	 String parentChild = "";
	 String conceptDesc = "";
	 Set<String> conceptsMap = new HashSet<String>();
     Set<String> parentsMap = new HashSet<String>();
	 
	 File folder = new File(ConfigUtility.getConfigProperties().getProperty("icd10noAPI.dir") + "/" + dateFormat);
	  
	 while (true) {
	
	   String targetUri = url + "?PageNumber="+pageNumber+"&PageSize=1000";
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

	   // create folder if necessary
	   if(doc.size() > 0 && !folder.exists()) {
		   folder.mkdir();
	   }
	
	   // get total amount
	   // Get concepts returned in this call (up to 1000)
	   for (final JsonNode conceptNode : doc.get("data")) {
	     JsonNode codeActive = conceptNode.get("active");
	     //Only load in active codes
	     if(!codeActive.asText().equals("true")) {
	       continue;
	     }
	     
	     JsonNode icd10noTerm = conceptNode.get("nameNorwegian");
	     JsonNode icd10noKode = conceptNode.get("codeValue");
	     JsonNode icd10noParent = conceptNode.get("parentCode");
	     if(icd10noKode != null && icd10noTerm != null && icd10noParent != null) {
           //There are some parent codes specified on the fat server which incorrectly reference non-existent codes.
	       String icd10noParentKode = icd10noParent.asText();
	       if(parentCorrectionsMap.containsKey(icd10noParentKode)) {
	         icd10noParentKode = parentCorrectionsMap.get(icd10noParentKode);
	       }
	       
	       //Additionally, the ICD-10 node has itself as its parent, which is incompatible with this tool.
	       //Update to root to resolve this.
	       if(icd10noKode.asText().equals("ICD-10")) {
	         icd10noParentKode = "root";
	       }
	       
	        conceptDesc += icd10noKode.asText() + "|" + icd10noTerm.asText() + System.lineSeparator();
	        parentChild += icd10noParentKode + "|" + icd10noKode.asText() + System.lineSeparator();
	        conceptsMap.add(icd10noKode.asText());
	        parentsMap.add(icd10noParentKode);
	     }
	   }
	    
	   pageNumber+=1;
	   if(pageNumber > Long.parseLong(doc.get("totalPages").asText())) // if we've processed the final page
		   break;
	 }
	 
	 //Check for invalid parent codes
	 for(String parentCode : parentsMap) {
	   if(!conceptsMap.contains(parentCode)  && parentCode != "root") {
	     Logger.getLogger(getClass()).error("No concept code found for specified parent: " + parentCode + ""); 
	   }
	 }
	 
	 BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "/parent-child.txt"));
	 writer.write(parentChild);
	 writer.close();
	 
	 writer = new BufferedWriter(new FileWriter(folder + "/concepts.txt"));
	 writer.write(conceptDesc);
	 writer.close();
  }

  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  @Override
  public void reset() throws Exception {
    // n/a
  }

  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

  @Override
  public void cancel() throws Exception {
    requestCancel = true;
  }
}
