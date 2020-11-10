/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.text.SimpleDateFormat;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Goal which loads a simple code list data file.
 * 
 * The format of the file is: code|string[|synonym,...]
 * 
 * It uses the claml metadata help for metadat
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-simple
 * 
 * @phase package
 */
public class TerminologySimpleLoaderMojo extends AbstractTerminologyLoaderMojo {

  /** The date format. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /**
   * Whether to run this mojo against an active server.
   * @parameter
   */
  private boolean server = false;

  /**
   * Input directory.
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
	 * Version of terminology to be loaded.
   * 
   * @parameter
   * @required
   */
  private String version;

	 /**
     * Version of terminology to be loaded.
     * 
     * @parameter
     */
    private String metadataCounter = "1";
	
	
	/**
   * Instantiates a {@link TerminologySimpleLoaderMojo} from the specified
   * parameters.
   * 
   */
  public TerminologySimpleLoaderMojo() {
    super();
    // do nothing
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {

    getLog().info("Simple Terminology Loader called via mojo.");
    getLog().info("  Terminology        : " + terminology);
    getLog().info("  Version            : " + version);
    getLog().info("  Input directory    : " + inputDir);
        getLog().info("  Metadata counter   : " + metadataCounter);
    getLog().info("  Expect server up   : " + server);

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
				service.loadTerminologySimple(terminology, version, inputDir, metadataCounter, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
				client.loadTerminologySimple(terminology, version, inputDir, metadataCounter, getAuthToken());
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
