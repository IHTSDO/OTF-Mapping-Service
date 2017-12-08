/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.rest.client.ContentClientRest;
import org.ihtsdo.otf.mapping.rest.impl.ContentServiceRestImpl;

/**
 * Converts GMDN data to RF2 objects.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-gmdn
 * @phase package
 */
public class TerminologyGmdnLoaderMojo extends AbstractTerminologyLoaderMojo {

	/**
	 * Whether to run this mojo against an active server.
	 * @parameter 
	 */
	private boolean server = false;
	
	/**
	 * The input dir.
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
	 * Terminology version.
	 *
	 * @parameter
	 * @required
	 */
	private String version;

	/**
	 * Instantiates a {@link TerminologyGmdnLoaderMojo} from the specified
	 * parameters.
	 */
	public TerminologyGmdnLoaderMojo() {
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
				service.loadTerminologyGmdn(terminology, version, inputDir,
						getAuthToken());

			} else {
				getLog().info("Running against server");

				// invoke the client
				ContentClientRest client = new ContentClientRest(properties);
				client.loadTerminologyGmdn(terminology, version, inputDir,
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
