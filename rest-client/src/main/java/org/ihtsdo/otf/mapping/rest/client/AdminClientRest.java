/**
 * Copyright 2017 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.client;

import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.jpa.services.rest.AdminSerivceRest;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * A client for connecting to administration REST service.
 */
public class AdminClientRest extends RootClientRest
		implements AdminSerivceRest {

	/** The config. */
	private Properties config = null;

	private String URL_SERVICE_ROOT = "/admin";

	/**
	 * Instantiates a {@link AdminClientRest} from the specified parameters.
	 *
	 * @param config
	 *            the config
	 */
	public AdminClientRest(Properties config) {
		this.config = config;
	}

	@Override
	public void luceneReindex(String indexedObjects, String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.debug("Lucene reindexing objects " + indexedObjects);

		validateNotEmpty(authToken, "authToken");
		config = ConfigUtility.getConfigProperties();

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(
				config.getProperty("base.url") + URL_SERVICE_ROOT + "/reindex");
		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.post(Entity.text(indexedObjects));

		if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
			if (response.getStatus() != 204) {
				throw new Exception(
						"Unexpected status " + response.getStatus());
			}
		}

	}
}
