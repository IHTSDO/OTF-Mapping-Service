package org.ihtsdo.otf.mapping.mojo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.hibernate.CacheMode;
import org.hibernate.search.FullTextFilter;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.store.DirectoryProvider;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.jpa.ConceptJpa;

/**
 * Goal which updates the db to sync it with the model via JPA.
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>makeIndexes</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>makeIndexes</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
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

			getLog().info("  Testing MakeIndexes.java");
			EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");

			manager = factory.createEntityManager();
			
			FullTextEntityManager fullTextEntityManager = Search
					.getFullTextEntityManager(manager);
			fullTextEntityManager.purgeAll(ConceptJpa.class);
			fullTextEntityManager.flushToIndexes();
			fullTextEntityManager.createIndexer(ConceptJpa.class)
					.batchSizeToLoadObjects(25).cacheMode(CacheMode.NORMAL)
					.threadsToLoadObjects(5).threadsForSubsequentFetching(20)
					.startAndWait();
			
			System.out.println(".. done");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
