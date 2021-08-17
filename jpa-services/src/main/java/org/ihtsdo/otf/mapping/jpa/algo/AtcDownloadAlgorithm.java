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

public class AtcDownloadAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /**
   * Instantiates an empty {@link AtcDownloadAlgorithm}.
   *
   * @throws Exception the exception
   */
  public AtcDownloadAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void compute() throws Exception {
    Logger.getLogger(getClass()).info("Checking ATC API for recent terminologies");
    
    // Pull ATC codes from API  
    final String url = ConfigUtility.getConfigProperties().getProperty("atcAPI.url");
    
     final Client client = ClientBuilder.newClient();
     final String accept = "*/*";
    
	 int skip = 0;
	 final ObjectMapper mapper = new ObjectMapper();
	 
	 //housekeeping for creating folder later
	 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");  
	 LocalDateTime now = LocalDateTime.now();  
	 String dateFormat = dtf.format(now);
	 
	 String parentChild = "";
	 String conceptDesc = "";
	 Set<String> conceptsMap = new HashSet<String>();
	 
	 File folder = new File(ConfigUtility.getConfigProperties().getProperty("atcAPI.dir") + "/" + dateFormat);
	  
	 while (true) {
	
	   String targetUri = url + "?skip="+skip+"&take=1000";
	   WebTarget target = client.target(targetUri);
	   target = client.target(targetUri);
	   Logger.getLogger(getClass()).info(targetUri);
	
	   Response response =
	       target.request(accept)
	       .header("Ocp-Apim-Subscription-Key",ConfigUtility.getConfigProperties().getProperty("atcAPI.apiKey"))
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
	   for (final JsonNode conceptNode : doc) {
	     JsonNode atcKode = conceptNode.get("atcKode");
	     if(atcKode != null) {
	        System.out.println("atcKode = " + atcKode.asText());
	        conceptDesc += atcKode.asText() + "|" + conceptNode.get("nivånavn").asText() + System.lineSeparator();
	        String parent = atcKode.asText();
	        while(parent != "") {
	        	if(conceptsMap.contains(parent))
	        		break;
	        	parent = StringUtils.chop(parent);
	        }
	        if(parent == "") {
	        	parentChild += "root|" + atcKode.asText() + System.lineSeparator();
	        }
	        else {
	        	parentChild += parent + "|" + atcKode.asText() + System.lineSeparator();
	        }
	        conceptsMap.add(atcKode.asText());
	     }
	   }
	    
	   skip+=1000;
	   if(doc.size() < 1000) // partial return (<1000) = last batch in API
		   break;
	 }
	 BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "/parent_child.txt"));
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
