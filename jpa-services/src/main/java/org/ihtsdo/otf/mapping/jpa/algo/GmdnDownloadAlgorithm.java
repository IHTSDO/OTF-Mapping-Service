package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class GmdnDownloadAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /**
   * Instantiates an empty {@link GmdnDownloadAlgorithm}.
   *
   * @throws Exception the exception
   */
  public GmdnDownloadAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    Logger.getLogger(getClass()).info("Attempting to download from GMDN sFTP");

    JSch jsch = new JSch();
    Session session = null;
    
    final String gmdnsftpHost = ConfigUtility.getConfigProperties()
        .getProperty("gmdnsftp.host");
    final int gmdnsftpPort = Integer.parseInt(ConfigUtility.getConfigProperties()
        .getProperty("gmdnsftp.port"));
    final String gmdnsftpUser = ConfigUtility.getConfigProperties()
        .getProperty("gmdnsftp.user");
    final String gmdnsftpPassword = ConfigUtility.getConfigProperties()
        .getProperty("gmdnsftp.password");
    final String saveLocation = ConfigUtility.getConfigProperties()
        .getProperty("gmdnsftp.dir");    
    
    if(new File(saveLocation + "gmdnData17_11.zip").exists()){
      Logger.getLogger(getClass()).info("gmdnData17_11 file already downloaded - exiting.");
      return;
    }
    else{
      Logger.getLogger(getClass()).info("gmdnData17_11 not downloaded yet - attempting download.");
    }
    
    try {     
      session = jsch.getSession(gmdnsftpUser, gmdnsftpHost, gmdnsftpPort);
      session.setConfig("StrictHostKeyChecking", "no");
      session.setPassword(gmdnsftpPassword);
      session.connect();

      Channel channel = session.openChannel("sftp");
      channel.connect();
      ChannelSftp sftpChannel = (ChannelSftp) channel;
      sftpChannel.get("/data/gmdnData17_11.zip",
          saveLocation + "/gmdnData17_11.zip");
      sftpChannel.exit();
      session.disconnect();
    } catch (JSchException e) {
      e.printStackTrace();
    } catch (SftpException e) {
      e.printStackTrace();
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
