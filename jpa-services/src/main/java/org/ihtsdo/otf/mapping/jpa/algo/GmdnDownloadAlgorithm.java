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

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
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
   * Size of the buffer to read/write data
   */
  private static final int BUFFER_SIZE = 4096;

  /**
   * Instantiates an empty {@link GmdnDownloadAlgorithm}.
   *
   * @throws Exception the exception
   */
  public GmdnDownloadAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public void compute() throws Exception {
    Logger.getLogger(getClass()).info("Checking GMDN SFTP for undownloaded terminologies");
    
    JSch jsch = new JSch();
    Session session = null;

    final String gmdnsftpHost =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.host");
    final int gmdnsftpPort = Integer.parseInt(
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.port"));
    final String gmdnsftpUser =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.user");
    final String gmdnsftpPassword =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.password");
    final String gmdnDataDir = 
            ConfigUtility.getConfigProperties().getProperty("gmdnsftp.sftp.data.dir");
    final String saveLocation =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.dir");

    try {
      session = jsch.getSession(gmdnsftpUser, gmdnsftpHost, gmdnsftpPort);
      session.setConfig("StrictHostKeyChecking", "no");
      session.setPassword(gmdnsftpPassword);
      session.connect();

      Channel channel = session.openChannel("sftp");
      channel.connect();
      ChannelSftp sftpChannel = (ChannelSftp) channel;
      sftpChannel.cd(gmdnDataDir);
      Vector filelist = sftpChannel.ls(gmdnDataDir);
      for (int i = 0; i < filelist.size(); i++) {
        LsEntry entry = (LsEntry) filelist.get(i);
        // Find all files named gmdnDatayy_M.zip
        if (entry.getFilename().matches("gmdnData\\d{2}_\\d{1,2}.zip")) {

          final String datePortion =
              entry.getFilename().replace("gmdnData", "").replace(".zip", "");
          final String unzipLocation = saveLocation + "/" + datePortion;

          // If destination folder already contains a subfolder named the same
          // as the date portion of the file, this version has already been
          // downloaded previously
          if (new File(unzipLocation).exists()) {
            Logger.getLogger(getClass()).info(entry.getFilename() + " previously downloaded.  Skipping.");            
            continue;
          }
          // If it doesn't exist yet...
          else {
            Logger.getLogger(getClass()).info("New version identified: " + entry.getFilename() + ".  Downloading.");                        
            // download the file
            sftpChannel.get(entry.getFilename(),
                saveLocation + "/" + entry.getFilename());

            // Create a new folder
            new File(unzipLocation).mkdir();

            // unzip all contents to the new folder
            ZipInputStream zipIn = new ZipInputStream(
                new FileInputStream(saveLocation + "/" + entry.getFilename()));

            ZipEntry zipEntry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (zipEntry != null) {
              final String filePath =
                  unzipLocation + File.separator + zipEntry.getName();
              extractFile(zipIn, filePath);
              zipIn.closeEntry();
              zipEntry = zipIn.getNextEntry();
            }
            zipIn.close();

            // delete the zip file
            new File(saveLocation + "/" + entry.getFilename()).delete();
          }
        }
      }
      sftpChannel.exit();
      session.disconnect();
    } catch (JSchException e) {
      e.printStackTrace();
    } catch (SftpException e) {
      e.printStackTrace();
    }

  }

  /**
   * Extracts a zip entry (file entry)
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private void extractFile(ZipInputStream zipIn, String filePath)
    throws IOException {
    BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
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
