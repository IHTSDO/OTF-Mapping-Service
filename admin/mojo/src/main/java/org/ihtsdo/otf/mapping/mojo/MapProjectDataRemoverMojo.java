/**
 * Copyright (c) 2012 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which removes all map projects and associated data from the database.
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <dependencies>
 *         <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-remover-config</artifactId>
 *           <version>${project.version}</version>
 *           <scope>system</scope>
 *           <systemPath>${project.build.directory}/mapping-admin-remover-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>remove-map-projects</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>remove-map-projects</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal remove-map-projects
 * 
 * @phase package
 */
public class MapProjectDataRemoverMojo extends AbstractMojo {

	/**
	 * Instantiates a {@link MapProjectDataRemoverMojo} from the specified
	 * parameters.
	 * 
	 */
	public MapProjectDataRemoverMojo() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		try {

			getLog().info("Start removing map project data ...");

			MappingService service = new MappingServiceJpa();
			service.setTransactionPerOperation(false);
			service.beginTransaction();
			// Remove map projects
			for (MapProject p : service.getMapProjects()) {
				getLog().info("  Remove map project - " + p.getName());
				service.removeMapProject(p.getId());
			}

			// Remove map leads
			for (MapLead l : service.getMapLeads()) {
				getLog().info("  Remove map lead - " + l.getName());
				service.removeMapLead(l.getId());
			}

			// Remove map specialists
			for (MapSpecialist s : service.getMapSpecialists()) {
				getLog().info("  Remove map specialist - " + s.getName());
				service.removeMapSpecialist(s.getId());
			}

			// Remove map advices
			for (MapAdvice a : service.getMapAdvices()) {
				getLog().info("  Remove map advice - " + a.getName());
				service.removeMapAdvice(a.getId());
			}

			// Remove map principles
			for (MapPrinciple p : service.getMapPrinciples()) {
				getLog().info("  Remove map principle - " + p.getName());
				service.removeMapPrinciple(p.getId());
			}
			service.commit();
			getLog().info("done ...");

			service.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}

}
