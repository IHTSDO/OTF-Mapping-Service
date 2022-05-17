package org.ihtsdo.otf.mapping.jpa.algo;

import java.time.format.DateTimeFormatter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime; 
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ICPC2NODownloadAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /**
   * Instantiates an empty {@link ICPC2NODownloadAlgorithm}.
   *
   * @throws Exception the exception
   */
  public ICPC2NODownloadAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void compute() throws Exception {
    Logger.getLogger(getClass()).info("Checking ICPC2 API for recent terminologies");
    
    //Example data from API
//    {
//      "data": [
//        {
//          "codeValue": "A",
//          "nameNorwegian": "Allment og uspesifisert",
//          "nameEnglish": "General and unspecified",
//          "active": true,
//          "statusChangeDate": "2002-02-28T23:00:00.000+00:00"
//        },
//        {
//          "codeValue": "A.1",
//          "nameNorwegian": "(A01-A29) Symptomer og plager",
//          "nameEnglish": "Symptoms and complaints",
//          "active": true,
//          "statusChangeDate": "2002-02-28T23:00:00.000+00:00"
//        },
//         ...
//  ],
//    "succeeded": true,
//    "pageNumber": 1,
//    "pageSize": 100,
//    "totalPages": 178,
//    "totalRecords": 17717
//  }
    
    // Pull ATC codes from API  
    final String url = ConfigUtility.getConfigProperties().getProperty("icpc2noAPI.url");
    
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
	 
	 File folder = new File(ConfigUtility.getConfigProperties().getProperty("icpc2noAPI.dir") + "/" + dateFormat);
	  
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
	     
	     JsonNode icpc2Term = conceptNode.get("nameNorwegian");
	     JsonNode icpc2Kode = conceptNode.get("codeValue");
	     if(icpc2Kode != null && icpc2Term != null) {
	        conceptDesc += icpc2Kode.asText() + "|" + icpc2Term.asText() + System.lineSeparator();

	        //There are multiple different patterns for hierarchy in icpc2.
	        
	        //If the code is a single letter, it is a top-level chapter 
            //(e.g. A parent is root)
	        if(icpc2Kode.asText().length()==1) {
	          parentChild += "root|" + icpc2Kode.asText() + System.lineSeparator();
	        }
	        
            //If the code contains a decimal, parent can be determined by
            //removing characters until you find a match with a previously loaded concept
            //(e.g. A02.0003 parent is A02, A.1 parent is A)
	        else if (icpc2Kode.asText().contains(".")){
	          String parent = icpc2Kode.asText();
	          while(parent != "") {
                if(conceptsMap.contains(parent))
                    break;
                parent = StringUtils.chop(parent);
	            }
	            if(parent == "") {
	              Logger.getLogger(getClass()).warn("No parent determined for: " + icpc2Kode.asText()); 
	            }
	            else {
	                parentChild += parent + "|" + icpc2Kode.asText() + System.lineSeparator();
	            }
	        }
	        
            //If the code is a letter plus a two-decimal number without decimal, it is a concept.
            //Its parent is determined by the number:
            //01-29 Symptoms and complaints, belongs to X.1
            //30-69 Process codes, belongs to X.3
            //70-99 Diagnosis/diseases, belongs to X.7
            //(e.g. A09 parent is A.1, A20 parent is A.1, B99 parent is B.7, etc.)	        
	        else if (icpc2Kode.asText().length()==3) {
	          //Separate the letter and number portion
	          String icpc2Chapter = icpc2Kode.asText().substring(0,1);
	          Long icpc2Number = Long.parseLong(icpc2Kode.asText().substring(1));
	          String parent = null;
	          if(icpc2Number >=1 && icpc2Number <= 29) {
	            parent = icpc2Chapter + ".1";
	          }
	          else if (icpc2Number >=30 && icpc2Number <= 69) {
	            parent = icpc2Chapter + ".3";
	          }
	          else if (icpc2Number >=70 && icpc2Number <= 99) {
                parent = icpc2Chapter + ".7";	            
	          }
	          else {
	            //Should not be able to get here
	          }
	          
	          if(parent != null) {
	            parentChild += parent + "|" + icpc2Kode.asText() + System.lineSeparator();
	          }else {
	              Logger.getLogger(getClass()).warn("No parent determined for: " + icpc2Kode.asText());	            
	          }
	        }
	        conceptsMap.add(icpc2Kode.asText());
	     }
	   }
	    
	   pageNumber+=1;
	   if(pageNumber > Long.parseLong(doc.get("totalPages").asText())) // if we've processed the final page
		   break;
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
