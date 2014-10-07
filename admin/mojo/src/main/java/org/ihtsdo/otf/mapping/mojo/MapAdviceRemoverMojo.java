package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Removes a map advice entirely from environment
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
			<id>MapAdvice</id>
			<build>
				<plugins>
					<plugin>
						<groupId>org.ihtsdo.otf.mapping</groupId>
						<artifactId>mapping-admin-mojo</artifactId>
						<version>${project.version}</version>
						<executions>
							<execution>
								<id>remove-map-advice</id>
								<phase>package</phase>
								<goals>
									<goal>remove-map-advice</goal>
								</goals>
								<configuration>
									<mapAdviceName>${mapAdvice.name}</mapAdviceName>
								</configuration>
							</execution>
						</executions>
					</plugin>
				</plugins>
			</build>
		</profile>
 * </pre>
 * 
 * @goal remove-map-advice
 * @phase package
 */
public class MapAdviceRemoverMojo extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refSetId
   */
  private String mapAdviceName = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Removing map advice from environment - " + mapAdviceName);

    if (mapAdviceName == null) {
      throw new MojoExecutionException("You must specify the full name of the map advice.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();

      MapAdvice mapAdvice = null;
      for (MapAdvice ma : mappingService.getMapAdvices().getIterable()) {
    	  if (ma.getName().equals(mapAdviceName))
    		  mapAdvice = ma;
      }
      
      if (mapAdvice == null)
    	  throw new MojoExecutionException("The map advice to be removed does not exist");
      
      getLog().info("Found map advice to remove (id = " + mapAdvice.getId() + ")");
      
      mappingService.removeMapAdviceFromEnvironment(mapAdvice);
      

      getLog().info("done ...");
      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }

  }
}