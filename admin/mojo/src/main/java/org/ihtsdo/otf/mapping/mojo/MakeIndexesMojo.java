package org.ihtsdo.otf.mapping.mojo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;

/**
 * Goal which makes lucene indexes based on hibernate-search annotations.
 * 
 * <pre>
 *      <plugin>
 *         <groupId>org.ihtsdo.otf.mapping</groupId>
 *         <artifactId>mapping-admin-mojo</artifactId>
 *         <version>${project.version}</version>
 *         <dependencies>
 *           <dependency>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-lucene-config</artifactId>
 *             <version>${project.version}</version>
 *             <scope>system</scope>
 *             <systemPath>${project.build.directory}/mapping-admin-lucene-${project.version}.jar</systemPath>
 *           </dependency>
 *         </dependencies>
 *         <executions>
 *           <execution>
 *             <id>makeindexes</id>
 *             <phase>package</phase>
 *             <goals>
 *               <goal>makeindexes</goal>
 *             </goals>
 *             <configuration>
 *               <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *             </configuration>
 *           </execution>
 *         </executions>
 *       </plugin>
 * </pre>
 * 
 * @goal makeindexes
 * 
 * @phase process-resources
 */
public class MakeIndexesMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * Instantiates a {@link MakeIndexesMojo} from the specified parameters.
	 *
	 */
	public MakeIndexesMojo() {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {

		try {

			getLog().info("  Starting MakeIndexes.java");
			EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");

			manager = factory.createEntityManager();
			
			getLog().info("  Creating indexes for ConceptJpa");
			FullTextEntityManager fullTextEntityManager = Search
					.getFullTextEntityManager(manager);
			fullTextEntityManager.purgeAll(ConceptJpa.class);
			fullTextEntityManager.flushToIndexes();
			fullTextEntityManager.createIndexer(ConceptJpa.class)
					.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
					.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
					.startAndWait();
			
			getLog().info("  Creating indexes for MapProjectJpa");
			fullTextEntityManager.purgeAll(MapProjectJpa.class);
			fullTextEntityManager.flushToIndexes();
			fullTextEntityManager.createIndexer(MapProjectJpa.class)
					.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
					.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
					.startAndWait();
			getLog().info("  Creating indexes for MapRecordJpa");
			fullTextEntityManager.purgeAll(MapRecordJpa.class);
			fullTextEntityManager.flushToIndexes();
			fullTextEntityManager.createIndexer(MapRecordJpa.class)
					.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
					.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
					.startAndWait();			
			getLog().info("  Completing MakeIndexes.java");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
