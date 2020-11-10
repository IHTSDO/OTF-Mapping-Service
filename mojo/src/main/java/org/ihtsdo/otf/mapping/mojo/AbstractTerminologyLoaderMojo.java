package org.ihtsdo.otf.mapping.mojo;

import java.util.Properties;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

public abstract class AbstractTerminologyLoaderMojo extends AbstractOtfMappingMojo {

	/**
	 * Indicate if server is running (true) or not (false);
	 */
	protected Boolean serverRunning;

	protected Properties properties;
	
	private long processStartTime;

	/**
	 * Check if the server is running and if required to run. Throw a
	 * MojoFailureException if a running server is required but server is not
	 * running. Throw a MojoFailureException if server must be off but is
	 * running.
	 * 
	 * @throws Exception
	 * @throws MojoFailureException
	 */
	protected void validateServerStatus(Boolean server) throws Exception, MojoFailureException {

		serverRunning = ConfigUtility.isServerActive();

		getLog().info(
				"Server status detected:  " + (!serverRunning ? "DOWN" : "UP"));

		if (serverRunning && !server) {
			throw new MojoFailureException(
					"Mojo expects server to be down, but server is running");
		}

		if (!serverRunning && server) {
			throw new MojoFailureException(
					"Mojo expects server to be running, but server is down");
		}

	}

	protected String getAuthToken() throws Exception {

		if (properties == null)
			setProperties();

		// authenticate
		SecurityService service = new SecurityServiceJpa();
		String authToken = service.authenticate(getConfigProperty("admin.user"),
				getConfigProperty("admin.password")).getAuthToken();
		service.close();

		return authToken;
	}

	protected String getConfigProperty(String propertyName) throws Exception {
		if (properties == null)
			setProperties();
		return properties.getProperty(propertyName);
	}

	private void setProperties() throws Exception {
		properties = ConfigUtility.getConfigProperties();
	}
	
	protected void setProcessStartTime() {
		this.processStartTime = System.nanoTime();
	}
	
	/**
	 * Returns the total elapsed time string.
	 *
	 * @return the total elapsed time string.
	 */
	protected String getTotalElapsedTimeStr() {
		Long resultnum = (System.nanoTime() - this.processStartTime) / 1000000000;
		String result = resultnum.toString() + "s";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "m";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "h";
		return result;
	}
	

}
