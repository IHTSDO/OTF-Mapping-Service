package org.ihtsdo.otf.mapping.mojo;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.algo.LuceneReindexAlgorithm;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which makes lucene indexes based on hibernate-search annotations.
 * 
 * See admin/lucene/pom.xml for a sample execution.
 * 
 * @goal reindex
 * @phase package
 */
public class LuceneReindexMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * The specified objects to index
	 * @parameter 
	 */
	private String indexedObjects;

	/**
	 * Whether to run this mojo against an active server.
	 * @parameter 
	 */
	private boolean server = false;

	/**
	 * Instantiates a {@link LuceneReindexMojo} from the specified parameters.
	 */
	public LuceneReindexMojo() {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		try {
			getLog().info("Lucene reindexing called via mojo.");
			getLog().info("  Indexed objects : " + indexedObjects);
			getLog().info("  Expect server up: " + server);
			Properties properties = ConfigUtility.getConfigProperties();

			boolean serverRunning = ConfigUtility.isServerActive();

			getLog().info("Server status detected:  "
					+ (!serverRunning ? "DOWN" : "UP"));

			if (serverRunning && !server) {
				throw new MojoFailureException(
						"Mojo expects server to be down, but server is running");
			}

			if (!serverRunning && server) {
				throw new MojoFailureException(
						"Mojo expects server to be running, but server is down");
			}

			// authenticate
			SecurityService service = new SecurityServiceJpa();
			String authToken = service
					.authenticate(properties.getProperty("admin.user"),
							properties.getProperty("admin.password"))
					.getAuthToken();
			service.close();

			getLog().info("Starting reindexing for:");

			if (!serverRunning) {
				Logger.getLogger(getClass()).info("Running directly");
				
//				AdminServiceRestImpl adminService = new AdminServiceRestImpl();
//				adminService.luceneReindex(indexedObjects, authToken);
				// Track system level information
				final LuceneReindexAlgorithm algo = new LuceneReindexAlgorithm();
				try {
					algo.setIndexedObjects(indexedObjects);
					algo.compute();
				}
				finally
				{
					algo.close();
				}

			} else {
				Logger.getLogger(getClass()).info("Running against server");

				// invoke the client
				Logger.getLogger(getClass()).info(
						"Content Client - lucene reindex " + indexedObjects);

				final Client client = ClientBuilder.newClient();
				final WebTarget target = client.target(
						properties.getProperty("base.url") + "/admin/reindex");
				final Response response = target
						.request(MediaType.APPLICATION_JSON)
						.header("Authorization", authToken)
						.post(Entity.text(indexedObjects));

				if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
					// do nothing
				} else {
					if (response.getStatus() != 204)
						throw new Exception(
								"Unexpected status " + response.getStatus());
				}
			}
			
		} catch (Exception e) {
			Logger.getLogger(getClass()).error("Unexpected exception", e);
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}
}
