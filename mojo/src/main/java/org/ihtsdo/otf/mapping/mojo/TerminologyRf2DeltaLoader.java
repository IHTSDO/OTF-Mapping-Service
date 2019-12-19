/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Goal which loads an RF2 Delta of SNOMED CT data
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-delta
 * 
 * @phase package ***
 */
public class TerminologyRf2DeltaLoader extends AbstractTerminologyLoaderMojo {

  /**
   * Whether to run this mojo against an active server.
   * 
   * @parameter
   */
  private boolean server = false;

  /**
   * The input directory
   * 
   * @parameter
   * @required
   */
  private String inputDir;

  /**
   * Name of terminology to be loaded.
   * 
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * Requirement to have the last publication version passed in. This is used
   * for the "remove retired concepts" routine.
   * 
   * @parameter
   */
  private String lastPublicationDate;

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {

    // Create and configure services and variables and open files
    getLog().info("Run delta loader");
    getLog().info("    terminology         = " + terminology);
    getLog().info("    lastPublicationDate = " + lastPublicationDate);

    try {

      setupBindInfoPackage();
      
      // Track system level information
      setProcessStartTime();

      // throws exception if server is required but not running.
      // or if server is not required but running.
      validateServerStatus(server);

      if (serverRunning != null && !serverRunning) {
        getLog().info("Running directly");

        ContentServiceRestImpl service = new ContentServiceRestImpl();
        service.loadTerminologyRf2Delta(terminology, lastPublicationDate,
            inputDir, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.loadTerminologyRf2Delta(terminology, lastPublicationDate,
            inputDir, getAuthToken());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    } finally {
      getLog().info("      elapsed time = " + getTotalElapsedTimeStr());
      getLog().info("done ...");
    }

  }
}
