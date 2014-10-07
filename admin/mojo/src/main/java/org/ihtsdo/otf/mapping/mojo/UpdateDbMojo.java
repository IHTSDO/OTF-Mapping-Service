package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;

/**
 * Goal which updates the db to sync it with the model via JPA.
 * 
 * <pre>
 *   <plugin>
 *      <groupId>org.ihtsdo.otf.mapping</groupId>
 *      <artifactId>mapping-admin-mojo</artifactId>
 *      <version>${project.version}</version>
 *      <executions>
 *        <execution>
 *          <id>updatedb</id>
 *          <phase>package</phase>
 *          <goals>
 *            <goal>updatedb</goal>
 *          </goals>
 *        </execution>
 *      </executions>
 *    </plugin>
 * </pre>
 * 
 * @goal updatedb
 * 
 * @phase package
 */
public class UpdateDbMojo extends AbstractMojo {

  /**
   * Instantiates a {@link UpdateDbMojo} from the specified parameters.
   * 
   */
  public UpdateDbMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Start updating database schema...");
    try {
      String configFileName = System.getProperty("run.config");
      getLog().info("  run.config = " + configFileName);
      Properties config = new Properties();
      FileReader in = new FileReader(new File(configFileName)); 
      config.load(in);
      in.close();
      getLog().info("  properties = " + config);
      // Trigger a JPA event
      new RootServiceJpa().close();
      getLog().info("done ...");
    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

}
