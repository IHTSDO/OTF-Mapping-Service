/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal append-rf2-complex-map
 * @phase package
 */
public class MapRecordRf2ComplexMapAppenderMojo
    extends AbstractTerminologyLoaderMojo {

  /**
   * Whether to run this mojo against an active server.
   * @parameter
   */
  private boolean server = false;

  /**
   * The input file.
   * 
   * @parameter
   * @required
   */
  private String inputFile;

  /**
   * The refset id.
   * 
   * @parameter
   */
  private String refsetId;

  /**
   * The workflow status to assign to created map records.
   *
   * @parameter
   * @required
   */
  private String workflowStatus;

  /**
   * The user name.
   * 
   * @parameter
   */
  private String userName;

  /**
   * Instantiates a {@link MapRecordRf2ComplexMapAppenderMojo} from the
   * specified parameters.
   * 
   * @parameter
   */
  public MapRecordRf2ComplexMapAppenderMojo() {
    super();
    // do nothing
  }

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading complex map data");
    getLog().info("  inputFile      = " + inputFile);
    getLog().info("  workflowStatus = " + workflowStatus);
    getLog().info("  userName       = " + userName);
    getLog().info("  refsetId       = " + refsetId);

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
        service.appendMapRecordRf2ComplexMap(inputFile, refsetId,
            workflowStatus, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.appendMapRecordRf2ComplexMap(inputFile, refsetId, workflowStatus,
            getAuthToken());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Unexpected exception:", e);
    } finally {
      getLog().info("      elapsed time = " + getTotalElapsedTimeStr());
      getLog().info("done ...");
    }

  }

}