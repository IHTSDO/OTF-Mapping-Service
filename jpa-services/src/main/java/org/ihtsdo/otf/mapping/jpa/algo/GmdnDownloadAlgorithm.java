package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

public class GmdnDownloadAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /**
   * Size of the buffer to read/write data
   */
  private static final int BUFFER_SIZE = 1024;

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
    Logger.getLogger(getClass()).info("Checking GMDN FTP for undownloaded terminologies");

    final String gmdnftpHost =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.host");
    final int gmdnftpPort = Integer.parseInt(
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.port"));
    final String gmdnftpUser =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.user");
    final String gmdnftpPassword =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.password");
    final String gmdnDataDir = 
            ConfigUtility.getConfigProperties().getProperty("gmdnsftp.sftp.data.dir");
    final String saveLocation =
        ConfigUtility.getConfigProperties().getProperty("gmdnsftp.dir");
    
    FTPClient ftpClient;
    
    ftpClient = new FTPClient();
    
    try {
    	ftpClient.connect(gmdnftpHost, gmdnftpPort);
    	ftpClient.enterLocalPassiveMode();
    	ftpClient.login(gmdnftpUser, gmdnftpPassword);
        ftpClient.changeWorkingDirectory(gmdnDataDir);
        FTPFile[] files = ftpClient.listFiles();

      for (int i = 0; i < files.length; i++) {
        String entry = files[i].getName();
        // Find all files named gmdnDatayy_M.zip
        if (entry.matches("gmdnData\\d{2}_\\d{1,2}.zip")) {

          final String datePortion =
              entry.replace("gmdnData", "").replace(".zip", "");
          final String unzipLocation = saveLocation + "/" + datePortion;

          // If destination folder already contains a subfolder named the same
          // as the date portion of the file, this version has already been
          // downloaded previously
          if (new File(unzipLocation).exists()) {
            Logger.getLogger(getClass()).info(entry + " previously downloaded.  Skipping.");            
            continue;
          }
          // If it doesn't exist yet...
          else {
            Logger.getLogger(getClass()).info("New version identified: " + entry + ".  Downloading.");                        
            // download the file
            ftpClient.retrieveFile(entry, new FileOutputStream(new File(saveLocation + "/" + entry))); 

            // Create a new folder
            new File(unzipLocation).mkdir();

            // unzip all contents to the new folder
            ZipInputStream zipIn = new ZipInputStream(
                new FileInputStream(saveLocation + "/" + entry));

            ZipEntry zipEntry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (zipEntry != null) {
              final String filePath =
                  unzipLocation + File.separator + zipEntry.getName();
              Logger.getLogger(getClass()).info("Unzipping " + entry);                        
              extractFile(zipIn, filePath);
              zipIn.closeEntry();
              zipEntry = zipIn.getNextEntry();
            }
            Logger.getLogger(getClass()).info("Closing zip stream for " + entry);                        
            zipIn.close();
            Logger.getLogger(getClass()).info("Zip stream closed.");                        

            // delete the zip file
            // Sometimes there is a delay before the delete works.  If it doesn't go through, try again after a brief pause.
            // Stop trying after 10 attempts.
            boolean deleteSuccessful = false;
            int deleteAttemptCount = 0;
            
            Logger.getLogger(getClass()).info("Trying to delete " + saveLocation + "/" + entry);
            
            while(!deleteSuccessful) {
              deleteAttemptCount++;
              Logger.getLogger(getClass()).info("Attempt " + deleteAttemptCount);
              deleteSuccessful = new File(saveLocation + "/" + entry).delete();
              
              if(deleteSuccessful) {
                Logger.getLogger(getClass()).info(saveLocation + "/" + entry + " successfully deleted.");
              }
              
              if(deleteAttemptCount > 10) {
                Logger.getLogger(getClass()).error("Unable to delete file: " + saveLocation + "/" + entry);
                break;
              }
              
              Thread.currentThread().sleep(6000);
            }
          }
        }
      }
      ftpClient.logout();
    } catch (IOException e) {
      Logger.getLogger(getClass()).error("IOException - " + e.getMessage());
      Logger.getLogger(getClass()).error("IOException - " + e.getStackTrace());
        e.printStackTrace();
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Exception");
        e.printStackTrace();
    } finally {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
    FileOutputStream fos = new FileOutputStream(filePath);
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      fos.write(bytesIn, 0, read);
    }
    fos.close();
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
