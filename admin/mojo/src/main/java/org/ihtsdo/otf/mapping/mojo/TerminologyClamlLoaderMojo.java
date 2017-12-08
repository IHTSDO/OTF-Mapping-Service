package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Converts claml data to RF2 objects.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-claml
 * @phase package
 * 
 */
public class TerminologyClamlLoaderMojo extends AbstractTerminologyLoaderMojo {

	/**
	 * Whether to run this mojo against an active server.
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
	 * Instantiates a {@link TerminologyClamlLoaderMojo} from the specified
	 * parameters.
	 */
	public TerminologyClamlLoaderMojo() {
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
		getLog().info("Starting loading GMDN terminology");
		getLog().info("  inputDir    = " + inputDir);
		getLog().info("  terminology = " + terminology);
		getLog().info("  version     = " + version);

		try {

			// Track system level information
			setProcessStartTime();

			// throws exception if server is required but not running.
			// or if server is not required but running.
			validateServerStatus(server);

			if (serverRunning != null && !serverRunning) {
				getLog().info("Running directly");

				ContentServiceRestImpl service = new ContentServiceRestImpl();
				service.loadTerminologyClaml(terminology, version, inputDir,
						getAuthToken());

			} else {
				getLog().info("Running against server");

				// invoke the client
				ContentClientRest client = new ContentClientRest(properties);
				client.loadTerminologyClaml(terminology, version, inputDir,
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
