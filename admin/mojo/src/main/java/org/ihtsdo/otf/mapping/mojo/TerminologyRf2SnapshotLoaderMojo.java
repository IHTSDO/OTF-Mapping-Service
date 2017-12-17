package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-snapshot
 * @phase package
 * 
 */
public class TerminologyRf2SnapshotLoaderMojo
		extends AbstractTerminologyLoaderMojo {

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
	protected String inputDir;

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
	 * Whether to create tree positions. Default is true.
	 * 
	 * @parameter
	 */
	private boolean treePositions = true;

	/**
	 * Instantiates a {@link TerminologyRf2SnapshotLoaderMojo} from the
	 * specified parameters.
	 * 
	 * @parameter
	 */
	public TerminologyRf2SnapshotLoaderMojo() {
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

		getLog().info("Starting loading RF2 data");
		getLog().info("  terminology      = " + terminology);
		getLog().info("  inputDir         = " + inputDir);
		getLog().info("  treePositions    = " + treePositions);
		getLog().info("  sendNotification = " + sendNotification);

		try {

			// Track system level information
			setProcessStartTime();

			// throws exception if server is required but not running.
			// or if server is not required but running.
			validateServerStatus(server);

			if (serverRunning != null && !serverRunning) {
				getLog().info("Running directly");

				ContentServiceRestImpl service = new ContentServiceRestImpl();
				service.loadTerminologyRf2Snapshot(terminology, version,
						inputDir, treePositions, sendNotification,
						getAuthToken());

			} else {
				getLog().info("Running against server");

				// invoke the client
				ContentClientRest client = new ContentClientRest(properties);
				client.loadTerminologyRf2Snapshot(terminology, version,
						inputDir, treePositions, sendNotification,
						getAuthToken());
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
