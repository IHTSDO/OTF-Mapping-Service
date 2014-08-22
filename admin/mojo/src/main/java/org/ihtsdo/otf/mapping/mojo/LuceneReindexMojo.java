package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.util.Version;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Goal which makes lucene indexes based on hibernate-search annotations.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <plugin>
 *        <groupId>org.ihtsdo.otf.mapping</groupId>
 *        <artifactId>mapping-admin-mojo</artifactId>
 *        <version>${project.version}</version>
 *        <executions>
 *          <execution>
 *            <id>reindex</id>
 *            <phase>package</phase>
 *            <goals>
 *              <goal>reindex</goal>
 *            </goals>
 *            <configuration>
                <indexedObjects>${indexedObjects}</indexedObjects>
              </configuration>
 *          </execution>
 *        </executions>
 *      </plugin>
 * </pre>
 * 
 * @goal reindex
 * 
 * @phase package
 */
public class LuceneReindexMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;
	
	/** The specified objects to index 
	 * 	@parameter
	*/

	private String indexedObjects;

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
			
		// otherwise, construct set of indexed objects
		} else {
			
			// remove white-space and split by comma
			String[] objects = indexedObjects.replaceAll(" ",  "").split(",");
			
			// add each value to the set
			for (String object : objects) objectsToReindex.add(object);
			
		}
		getLog().info("Starting reindexing for:");
		for (String objectToReindex : objectsToReindex) {
			getLog().info("  " + objectToReindex);
		}
		
		try {
			String configFileName = System.getProperty("run.config");
			getLog().info("  run.config = " + configFileName);
			Properties config = new Properties();
			FileReader in = new FileReader(new File(configFileName)); 
			config.load(in);
			in.close();
			getLog().info("  properties = " + config);

			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS", config);

			manager = factory.createEntityManager();

			// full text entity manager
			FullTextEntityManager fullTextEntityManager =
					Search.getFullTextEntityManager(manager);

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
				fullTextEntityManager.createIndexer(FeedbackConversationJpa.class)
				.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
				.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
				.startAndWait();
				objectsToReindex.remove("FeedbackConversationJpa");
			}
			
			if (objectsToReindex.size() != 0) {
				throw new MojoFailureException("The following objects were specified for re-indexing, but do not exist as indexed objects: " + objectsToReindex.toString());
			}

			// Cleanup
			getLog().info("done ...");
			manager.close();
			factory.close();

		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
