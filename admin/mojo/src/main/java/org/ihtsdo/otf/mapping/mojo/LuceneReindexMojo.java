package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.lucene.util.Version;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Goal which makes lucene indexes based on hibernate-search annotations.
 * 
 * See admin/lucene/pom.xml for a sample execution.
 * 
 */
@Mojo(name = "reindex", defaultPhase = LifecyclePhase.PACKAGE)
public class LuceneReindexMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * The specified objects to index
	 * 
	 */
	@Parameter
	private String indexedObjects;

	/**
	 * Whether to run this mojo against an active server.
	 * 
	 */
	@Parameter
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

			// set of objects to be re-indexed
			Set<String> objectsToReindex = new HashSet<>();

			// if no parameter specified, re-index all objects
			if (indexedObjects == null) {
				objectsToReindex.add("ConceptJpa");
				objectsToReindex.add("MapProjectJpa");
				objectsToReindex.add("MapRecordJpa");
				objectsToReindex.add("TreePositionJpa");
				objectsToReindex.add("TrackingRecordJpa");
				objectsToReindex.add("FeedbackConversationJpa");
				objectsToReindex.add("ReportJpa");

				// otherwise, construct set of indexed objects
			} else {

				// remove white-space and split by comma
				String[] objects = indexedObjects.replaceAll(" ", "")
						.split(",");

				// add each value to the set
				for (String object : objects)
					objectsToReindex.add(object);

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
				getLog().info("Running directly");

				/*
				 * AdminServiceRest adminService = new AdminServiceRest();
				 * adminService.luceneReindex(indexedObjects, authToken);
				 */
			} else {
				getLog().info("Running against server");

				// invoke the client
				getLog().info(
						"Content Client - lucene reindex " + indexedObjects);

				final Client client = ClientBuilder.newClient();
				final WebTarget target = client.target(
						properties.getProperty("base.url") + "/admin/reindex");
				final Response response = target
						.request(MediaType.APPLICATION_XML)
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

			for (String objectToReindex : objectsToReindex) {
				getLog().info("  " + objectToReindex);
			}

			Properties config = ConfigUtility.getConfigProperties();

			EntityManagerFactory factory = Persistence
					.createEntityManagerFactory("MappingServiceDS", config);

			manager = factory.createEntityManager();

			// full text entity manager
			FullTextEntityManager fullTextEntityManager = Search
					.getFullTextEntityManager(manager);

			fullTextEntityManager.setProperty("Version", Version.LUCENE_36);

			// Concepts
			if (objectsToReindex.contains("ConceptJpa")) {
				getLog().info("  Creating indexes for ConceptJpa");
				fullTextEntityManager.purgeAll(ConceptJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.createIndexer(ConceptJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();

				objectsToReindex.remove("ConceptJpa");
			}

			// Map Projects
			if (objectsToReindex.contains("MapProjectJpa")) {
				getLog().info("  Creating indexes for MapProjectJpa");
				fullTextEntityManager.purgeAll(MapProjectJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.createIndexer(MapProjectJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();

				objectsToReindex.remove("MapProjectJpa");
			}

			// Map Records
			if (objectsToReindex.contains("MapRecordJpa")) {
				getLog().info("  Creating indexes for MapRecordJpa");
				fullTextEntityManager.purgeAll(MapRecordJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.setProperty(ROLE, ROLE);
				fullTextEntityManager.createIndexer(MapRecordJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();

				objectsToReindex.remove("MapRecordJpa");
			}

			// Tree Positions
			if (objectsToReindex.contains("TreePositionJpa")) {
				getLog().info("  Creating indexes for TreePositionJpa");
				fullTextEntityManager.purgeAll(TreePositionJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.createIndexer(TreePositionJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();

				objectsToReindex.remove("TreePositionJpa");
			}

			// Tracking Records
			if (objectsToReindex.contains("TrackingRecordJpa")) {
				getLog().info("  Creating indexes for TrackingRecordJpa");
				fullTextEntityManager.purgeAll(TrackingRecordJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.createIndexer(TrackingRecordJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();
				objectsToReindex.remove("TrackingRecordJpa");
			}

			// Feedback Conversations
			if (objectsToReindex.contains("FeedbackConversationJpa")) {
				getLog().info("  Creating indexes for FeedbackConversationJpa");
				fullTextEntityManager.purgeAll(FeedbackConversationJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager
						.createIndexer(FeedbackConversationJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();
				objectsToReindex.remove("FeedbackConversationJpa");
			}

			// Feedback Conversations
			if (objectsToReindex.contains("ReportJpa")) {
				getLog().info("  Creating indexes for ReportJpa");
				fullTextEntityManager.purgeAll(ReportJpa.class);
				fullTextEntityManager.flushToIndexes();
				fullTextEntityManager.createIndexer(ReportJpa.class)
						.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
						.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
						.startAndWait();
				objectsToReindex.remove("ReportJpa");
			}

			if (objectsToReindex.size() != 0) {
				throw new MojoFailureException(
						"The following objects were specified for re-indexing, but do not exist as indexed objects: "
								+ objectsToReindex.toString());
			}

			// Cleanup
			manager.close();
			factory.close();

			getLog().info("done ...");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
