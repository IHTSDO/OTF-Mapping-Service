/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Loads unpublished complex map additional info.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-complex-map-additional-info
 * @phase package
 */
public class MapRecordRf2ComplexMapAdditionalInfoLoaderMojo
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
   * The members flag.
   * 
   * @parameter
   * @required
   */
  private boolean memberFlag = true;

  /**
   * The records flag.
   * 
   * @parameter
   * @required
   */
  private boolean recordFlag = true;

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
   * Instantiates a {@link MapRecordRf2ComplexMapAdditionalInfoLoaderMojo} from the specified
   * parameters.
   * 
   * @parameter
   */
  public MapRecordRf2ComplexMapAdditionalInfoLoaderMojo() {
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
    getLog().info("Starting loading complex map additional information");
    getLog().info("  inputFile      = " + inputFile);
    getLog().info("  workflowStatus = " + workflowStatus);
    getLog().info("  userName       = " + userName);
    getLog().info("  membersFlag    = " + memberFlag);
    getLog().info("  recordFlag     = " + recordFlag);
    getLog().info("  refsetId       = " + refsetId);

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
        service.loadMapRecordRf2ComplexMapAdditionalInfo(inputFile, memberFlag, recordFlag,
            refsetId, workflowStatus, userName, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.loadMapRecordRf2ComplexMapAdditionalInfo(inputFile, memberFlag, recordFlag,
            refsetId, workflowStatus, userName, getAuthToken());
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