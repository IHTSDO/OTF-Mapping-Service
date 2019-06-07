package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Loads simple maps. - the members flag loads refset members if "true" - the
 * records flag loads map records if "true"
 *
 * 
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-simple-map
 * @phase package
 */
public class MapRecordRf2SimpleMapLoaderMojo
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
	 * The members flag.
	 * 
	 * @parameter
	 * @required
	 */
	private boolean memberFlag;

	/**
	 * The records flag.
	 * 
	 * @parameter
	 * @required
	 */
	private boolean recordFlag;


    /**
     * The refset Id to filter the input file by (optional).
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
	private String workflowStatus = WorkflowStatus.READY_FOR_PUBLICATION
			.toString();

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException
	 *             the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {

		getLog().info("Starting loading simple map data");
		getLog().info("  inputFile      = " + inputFile);
		getLog().info("  membersFlag    = " + memberFlag);
		getLog().info("  recordFlag     = " + recordFlag);
        getLog().info("  refsetId       = " + refsetId);
		getLog().info("  workflowStatus = " + workflowStatus);

		try {

			// Track system level information
			setProcessStartTime();

			// throws exception if server is required but not running.
			// or if server is not required but running.
			validateServerStatus(server);

			if (serverRunning != null && !serverRunning) {
				getLog().info("Running directly");

				ContentServiceRestImpl service = new ContentServiceRestImpl();
				service.loadMapRecordRf2SimpleMap(inputFile, memberFlag,
						recordFlag, refsetId, workflowStatus, getAuthToken()); 

			} else {
				getLog().info("Running against server");

				// invoke the client
				ContentClientRest client = new ContentClientRest(properties);
				client.loadMapRecordRf2SimpleMap(inputFile, memberFlag,
						recordFlag, refsetId, workflowStatus, getAuthToken());
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