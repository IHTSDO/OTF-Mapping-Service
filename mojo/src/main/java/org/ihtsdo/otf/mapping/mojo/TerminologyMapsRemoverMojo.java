/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Goal which removes a terminology from a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-terminology-maps
 * 
 * @phase package
 */
public class TerminologyMapsRemoverMojo extends AbstractTerminologyLoaderMojo {

  /**
   * Whether to run this mojo against an active server.
   * @parameter
   */
  private boolean server = false;

  /**
   * Ref set id to remove
   * 
   * @parameter
   * @required
   */
  private String refsetId;

  /**
   * Instantiates a {@link TerminologyMapsRemoverMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyMapsRemoverMojo() {
    super();
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting removing terminology maps");
    getLog().info("  refsetId = " + refsetId);

    setupBindInfoPackage();

    try {
      
      // Track system level information
      setProcessStartTime();

      // throws exception if server is required but not running.
      // or if server is not required but running.
      Logger.getLogger(getClass()).info("server is:" + this.server);
      validateServerStatus(server);

      if (serverRunning != null && !serverRunning) {
        getLog().info("Running directly");

        ContentServiceRestImpl service = new ContentServiceRestImpl();
        service.removeMapRecord(refsetId, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.removeMapRecord(refsetId, getAuthToken());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    } finally {
      getLog().info("      elapsed time = " + getTotalElapsedTimeStr());
      getLog().info("Done removing terminology maps");
    }
  }
}
