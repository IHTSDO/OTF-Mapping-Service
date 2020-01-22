/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Goal which removes a terminology from a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-terminology
 * @phase package
 * 
 */
public class TerminologyRemoverMojo extends AbstractTerminologyLoaderMojo {

  /**
   * Whether to run this mojo against an active server.
   * @parameter
   */
  private boolean server = false;

  /**
   * Name of terminology to be loaded.
   * 
   * @parameter
   * @required
   */
  protected String terminology;

  /**
   * Version of terminology to be loaded.
   * 
   * @parameter
   * @required
   */
  protected String version;

  /**
   * Whether to send email notification of any errors. Default is false.
   * 
   * @parameter
   */
  protected boolean sendNotification = false;

  /**
   * Instantiates a {@link TerminologyRemoverMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyRemoverMojo() {
    super();
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {

    getLog().info("Starting removing terminology");
    getLog().info("  terminology = " + terminology);
    getLog().info("  version     = " + version);

    setupBindInfoPackage();

    try {

      // Track system level information
      setProcessStartTime();

      // throws exception if server is required but not running.
      // or if server is not required but running.
      validateServerStatus(server);

      if (serverRunning != null && !serverRunning) {
        getLog().info("Running directly");

        ContentServiceRestImpl service = new ContentServiceRestImpl();
        service.removeTerminology(terminology, version, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.removeTerminology(terminology, version, getAuthToken());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    } finally {
      getLog().info("      elapsed time = " + getTotalElapsedTimeStr());
      getLog().info("Done removing terminology");
    }
  }
}
