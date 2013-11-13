package org.ihtsdo.otf.mapping.mojo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

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
 *           <id>update-db</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>update-db</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal updatedb
 * 
 * @phase process-resources
 */
public class UpdateDbMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * Instantiates a {@link UpdateDbMojo} from the specified parameters.
	 *
	 */
	public UpdateDbMojo() {
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {

		try {

			getLog().info("  Testing UpdateDbMojo.java");

			EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			System.out.println(".. done");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
