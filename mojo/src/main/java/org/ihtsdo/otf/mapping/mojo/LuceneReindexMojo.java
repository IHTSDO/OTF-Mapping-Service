/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.AdminClientRest;
import org.ihtsdo.otf.mapping.rest.impl.AdminServiceRestImpl;

/**
 * Goal which makes lucene indexes based on hibernate-search annotations.
 * 
 * See admin/lucene/pom.xml for a sample execution.
 * 
 * @goal reindex
 * @phase package
 */
public class LuceneReindexMojo extends AbstractTerminologyLoaderMojo {

  /**
   * Whether to run this mojo against an active server.
   * @parameter
   */
  private boolean server = false;

  /**
   * The specified objects to index
   * @parameter
   */
  private String indexedObjects;

  /**
   * Instantiates a {@link LuceneReindexMojo} from the specified parameters.
   */
  public LuceneReindexMojo() {
    super();
    // do nothing
  }

  /** see superclass */
  @Override
  public void execute() throws MojoFailureException {

    getLog().info("Lucene reindexing called via mojo.");
    getLog().info("  Indexed objects  = " + indexedObjects);
    getLog().info("  Expect server up = " + server);

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

        AdminServiceRestImpl service = new AdminServiceRestImpl();
        service.luceneReindex(indexedObjects, getAuthToken());

      } else {
        getLog().info("Running against server");

        // invoke the client
        AdminClientRest client = new AdminClientRest(properties);
        client.luceneReindex(indexedObjects, getAuthToken());
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
