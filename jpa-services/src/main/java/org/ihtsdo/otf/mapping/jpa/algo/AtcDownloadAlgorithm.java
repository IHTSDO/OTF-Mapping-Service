package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

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
    Logger.getLogger(getClass()).info("Checking GMDN SFTP for undownloaded terminologies");
    
    // Pull ATC codes from API  
    final String url = "https://api.helsedirektoratet.no/legemidler/legemidler/atc";
    
     final Client client = ClientBuilder.newClient();
     final String accept = "*/*";
    
      int skip = 0;
    
      while (true) {

        String targetUri = url + "?skip="+skip+"&take=10";
        WebTarget target = client.target(targetUri);
        target = client.target(targetUri);
        Logger.getLogger(getClass()).info(targetUri);

        Response response =
            target.request(accept)
            .header("Ocp-Apim-Subscription-Key","b104d51d1ee54769aa8cab321f2f9f98")
            .get();
        String resultString = response.readEntity(String.class);
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
          // n/a
        } else {
          throw new LocalException(
              "Unexpected terminology server failure. Message = " + resultString);
        }

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode doc = mapper.readTree(resultString);
        // get total amount
        // Get concepts returned in this call (up to 100)

        for (final JsonNode conceptNode : doc) {
          JsonNode atcKode = doc.get("atcKode");
          if(atcKode != null) {
             System.out.println("atcKode = " + atcKode.asText());
          }
        }
        
        skip+=1000;
        break;
      } 
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
