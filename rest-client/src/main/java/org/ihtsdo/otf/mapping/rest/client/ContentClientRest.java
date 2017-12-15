/**
 * 
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
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.rest.ContentServiceRest;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * @author Nuno Marques
 *
 */
public class ContentClientRest extends RootClientRest
		implements ContentServiceRest {

	/** The config. */
	private Properties config = null;

	private String URL_SERVICE_ROOT = "/content";

	/**
	 * Instantiates a {@link ContentClientRest} from the specified parameters.
	 *
	 * @param config
	 *            the config
	 */
	public ContentClientRest(Properties config) {
		super();
		this.config = config;
	}

	@Override
	public Concept getConcept(String terminologyId, String terminology,
			String terminologyVersion, String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public Concept getConcept(String terminologyId, String terminology,
			String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList findConceptsForQuery(String query, String authToken)
			throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList findDescendantConcepts(String terminologyId,
			String terminology, String terminologyVersion, String authToken)
			throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList findChildConcepts(String id, String terminology,
			String terminologyVersion, String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList findDeltaConceptsForTerminology(String terminology,
			String terminologyVersion, String authToken,
			PfsParameterJpa pfsParameter) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList getIndexDomains(String terminology,
			String terminologyVersion, String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList getIndexViewerPagesForIndex(String terminology,
			String terminologyVersion, String index, String authToken)
			throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public String getIndexViewerDetailsForLink(String terminology,
			String terminologyVersion, String domain, String link,
			String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}

	@Override
	public SearchResultList findIndexViewerEntries(String terminology,
			String terminologyVersion, String domain, String searchField,
			String subSearchField, String subSubSearchField, boolean allFlag,
			String authToken) throws Exception {
		// not yet implemented in rest client
		return null;
	}
	
	/* see superclass */
	@Override
	public void loadMapRecordRf2ComplexMap(String inputFile, Boolean memberFlag,
			Boolean recordFlag, String workflowStatus, String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load map record RF2 complex "
						+ " input file:" + inputFile + " member flag:"
						+ memberFlag + " record flag:" + recordFlag
						+ " workflow status: " + workflowStatus);

		validateNotEmpty(inputFile, "inputFile");

		StringBuilder qs = new StringBuilder();
		qs.append("?");
		if (recordFlag != null) {
			qs.append("memberFlag=").append(memberFlag);
		}
		if (recordFlag != null) {
			qs.append("recordFlag=").append(recordFlag);
		}
		if (workflowStatus != null) {
			qs.append("workflowStatus=").append(workflowStatus);
		}

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/map/record/rf2/complex" + qs.toString());

		final Response response = target.request(MediaType.TEXT_PLAIN)
				.header("Authorization", authToken).put(Entity.text(inputFile));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			if (response.getStatus() != 204)
				throw new Exception(
						"Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void loadMapRecordRf2SimpleMap(String inputFile, Boolean memberFlag,
			Boolean recordFlag, String workflowStatus, String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load map record RF2 simple "
						+ " input file:" + inputFile + " member flag:"
						+ memberFlag + " record flag:" + recordFlag
						+ " workflow status: " + workflowStatus);

		validateNotEmpty(inputFile, "inputFile");

		StringBuilder qs = new StringBuilder();
		qs.append("?");
		if (recordFlag != null) {
			qs.append("memberFlag=").append(memberFlag);
		}
		if (recordFlag != null) {
			qs.append("recordFlag=").append(recordFlag);
		}
		if (workflowStatus != null) {
			qs.append("workflowStatus=").append(workflowStatus);
		}

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/map/record/rf2/simple" + qs.toString());

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputFile));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			if (response.getStatus() != 204)
				throw new Exception(
						"Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void loadTerminologyClaml(String terminology, String version,
			String inputFile, String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load terminology CLAML " + terminology
						+ ", " + version);

		validateNotEmpty(inputFile, "inputFile");
		validateNotEmpty(terminology, "terminology");
		validateNotEmpty(version, "version");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/terminology/load/claml/" + terminology
				+ "/" + version);
		
		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputFile));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void loadTerminologyGmdn(String version,
			String inputDir, String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load terminology GMDN, " + version);

		validateNotEmpty(inputDir, "inputDir");
		validateNotEmpty(version, "version");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/terminology/load/gmdn/" + version);
		
		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputDir));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
    /* see superclass */
    @Override
    public void downloadTerminologyGmdn(String authToken) throws Exception {

        Logger.getLogger(getClass())
                .debug("Content Client - download terminology GMDN");

        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(config.getProperty("base.url")
                + URL_SERVICE_ROOT + "/terminology/download/gmdn");
        
        final Response response = target.request(MediaType.APPLICATION_JSON)
                .header("Authorization", authToken).post(Entity.json(null));

        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            // do nothing
        } else {
            throw new Exception("Unexpected status " + response.getStatus());
        }
    }	
	
	/* see superlass */
	@Override
	public boolean removeMapRecord(String refsetId, String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - remove map record " + refsetId);

		validateNotEmpty(refsetId, "refsetId");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/map/record/" + refsetId);

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).get();

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			if (response.getStatus() != 204)
				throw new Exception(
						"Unexpected status " + response.getStatus());
		}
		return true;
	}
	
	/* see superclass */
	@Override
	public boolean removeTerminology(String terminology, String version,
			String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - remove terminology " + terminology
						+ ", " + version);

		validateNotEmpty(terminology, "terminology");
		validateNotEmpty(version, "version");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client
				.target(config.getProperty("base.url") + URL_SERVICE_ROOT
						+ "/terminology/" + terminology + "/" + version);

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).delete();

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			if (response.getStatus() != 204)
				throw new Exception(
						"Unexpected status " + response.getStatus());
		}
		return true;
	}
	
	/* see superclass */
	@Override
	public void loadTerminologyRf2Delta(String terminology,
			String lastPublicationDate, String inputDir, String authToken)
			throws Exception {

		Logger.getLogger(getClass()).debug(
				"Content Client - load terminology rf2 delta " + terminology);

		validateNotEmpty(inputDir, "inputDir");
		validateNotEmpty(terminology, "terminology");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client
				.target(config.getProperty("base.url") + URL_SERVICE_ROOT
						+ "/terminology/load/rf2/delta/" + terminology
						+ "/" + lastPublicationDate);
		
		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputDir));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void loadTerminologyRf2Snapshot(String terminology, String version,
			String inputDir, Boolean treePositions, Boolean sendNotification,
			String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load terminology rf2 snapshot "
						+ terminology + ", " + version);

		validateNotEmpty(terminology, "terminology");
		validateNotEmpty(version, "version");

		StringBuilder qs = new StringBuilder();
		qs.append("?");
		if (treePositions != null) {
			qs.append("treePositions=").append(treePositions);
		}
		if (sendNotification != null) {
			qs.append("sendNotification=").append(sendNotification);
		}
		
		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/terminology/load/rf2/snapshot/"
				+ terminology + "/" + version + qs.toString());

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputDir));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void loadTerminologySimple(String terminology, String version,
			String inputFile, String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - load terminology simple " + terminology
						+ ", " + version + ", " + inputFile);

		validateNotEmpty(terminology, "terminology");
		validateNotEmpty(version, "version");
		validateNotEmpty(inputFile, "inputFile");

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/terminology/load/simple/" + terminology
				+ "/" + version);

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputFile));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
	/* see superclass */
	@Override
	public void reloadTerminologyRf2Snapshot(String terminology, String version,
			String inputDir, Boolean treePositions, Boolean sendNotification,
			String authToken) throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - reload terminology rf2 snapshot "
						+ terminology + ", " + version);

		validateNotEmpty(terminology, "terminology");
		validateNotEmpty(version, "version");

		StringBuilder qs = new StringBuilder();
		qs.append("?");
		if (treePositions != null) {
			qs.append("treePositions=").append(treePositions);
		}
		if (sendNotification != null) {
			qs.append("sendNotification=").append(sendNotification);
		}
		
		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/terminology/reload/rf2/snapshot/"
				+ terminology + "/" + version + qs.toString());

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputDir));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			throw new Exception("Unexpected status " + response.getStatus());
		}
	}
	
	/* see superlass */
	@Override
	public boolean reloadMapRecord(String refsetId, String inputFile, Boolean memberFlag,
			Boolean recordFlag, String workflowStatus, String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.debug("Content Client - remove and load map record " + refsetId 
						+ " inputFile:" + inputFile
						+ " memberFlag:" + memberFlag
						+ " recordFlag:" + recordFlag
						+ " workflowStatus:" + workflowStatus);

		validateNotEmpty(refsetId, "refsetId");
		validateNotEmpty(inputFile, "inputFile");

		StringBuilder qs = new StringBuilder();
		qs.append("?");
		if (recordFlag != null) {
			qs.append("memberFlag=").append(memberFlag);
		}
		if (recordFlag != null) {
			qs.append("recordFlag=").append(recordFlag);
		}
		if (workflowStatus != null) {
			qs.append("workflowStatus=").append(workflowStatus);
		}

		final Client client = ClientBuilder.newClient();
		final WebTarget target = client.target(config.getProperty("base.url")
				+ URL_SERVICE_ROOT + "/map/record/reload/" + refsetId + qs.toString());

		final Response response = target.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken).put(Entity.text(inputFile));

		if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
			// do nothing
		} else {
			if (response.getStatus() != 204)
				throw new Exception(
						"Unexpected status " + response.getStatus());
		}
		return true;
	}
	
	
}
