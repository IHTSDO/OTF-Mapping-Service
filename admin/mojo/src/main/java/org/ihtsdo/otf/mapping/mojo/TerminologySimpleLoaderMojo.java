package org.ihtsdo.otf.mapping.mojo;

import java.text.SimpleDateFormat;

import org.apache.maven.plugin.AbstractMojo;
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
	 * The input file.
	 *
	 * @parameter
	 * @required
	 */
	private String inputFile;

	/**
	 * The par/chd rels file.
	 *
	 * @parameter
	 */
	private String parChdFile;

	/**
	 * Name of terminology to be loaded.
	 * 
	 * @parameter
	 * @required
	 */
	private String terminology;

	/**
	 * Name of terminology to be loaded.
	 * 
	 * @parameter
	 * @required
	 */
	private String version;

	/**
	 * Instantiates a {@link TerminologySimpleLoaderMojo} from the specified
	 * parameters.
	 * 
	 */
	public TerminologySimpleLoaderMojo() {
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
		
		getLog().info("Starting loading simple data");
		getLog().info("  inputFile   = " + inputFile);
		getLog().info("  parChdFile  = " + parChdFile);
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
				service.loadTerminologySimple(terminology, version, inputFile, getAuthToken());

			} else {
				getLog().info("Running against server");

				// invoke the client
				ContentClientRest client = new ContentClientRest(properties);
				client.loadTerminologySimple(terminology, version,inputFile, getAuthToken());
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
