package org.ihtsdo.otf.mapping.mojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>ComputeWorkflow</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>compute-workflow</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>compute-workflow</goal>
 *                 </goals>
 *                 <configuration>
 *                   <refSetId>${refset.id}</refSetId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile>
 * </pre>
 * 
 * @goal compute-workflow
 * @phase package
 */
public class ComputeWorkflowLoaderMojo extends AbstractMojo {

	/**
	 * The refSet id
	 * 
	 * @parameter refSetId
	 */
	private String refSetId = null;

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException
	 *             the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting compute workflow - " + refSetId);

		if (refSetId == null) {
			throw new MojoExecutionException("You must specify a refSetId.");
		}

		try {

			MappingService mappingService = new MappingServiceJpa();
			Set<MapProject> mapProjects = new HashSet<>();

			for (MapProject mapProject : mappingService.getMapProjects()
					.getIterable()) {
				for (String id : refSetId.split(",")) {
					if (mapProject.getRefSetId().equals(id)) {
						mapProjects.add(mapProject);
					}
				}
			}

			// Get the current workflow and extract concepts for comparison

			WorkflowService workflowService = new WorkflowServiceJpa();

			// Compute workflow

			for (MapProject mapProject : mapProjects) {

				Map<String, String> previousWorkflowConcepts = new HashMap<>();
				int conceptsAdded = 0;
				int conceptsRemoved = 0;

				// add all current concepts with a tracking record to set
				for (TrackingRecord tr : workflowService
						.getTrackingRecordsForMapProject(mapProject)
						.getIterable()) {
					previousWorkflowConcepts.put(tr.getTerminologyId(), tr.getDefaultPreferredName());
				}

				getLog().info(
						"Computing workflow for " + mapProject.getName() + ", "
								+ mapProject.getId());
				workflowService.computeWorkflow(mapProject);
				
				getLog().info(
						"Comparing new workflow to previous workflow for " + mapProject.getName() + ", "
								+ mapProject.getId());
				
				// cycle over new workflow
				for (TrackingRecord tr : workflowService
						.getTrackingRecordsForMapProject(mapProject)
						.getIterable()) {
					
					if (!previousWorkflowConcepts.containsKey(tr.getTerminologyId())) {
						getLog().info("  New concept:  " + tr.getTerminologyId() + ", " + tr.getDefaultPreferredName());
						previousWorkflowConcepts.remove(tr.getTerminologyId());
						conceptsAdded++;
					}
				}
				
				// cycle over remaining concepts in old workflow, which were removed by computing new workflow
				for (String terminologyId : previousWorkflowConcepts.keySet()) {
					getLog().info("  Removed concept:  " + terminologyId + ", " + previousWorkflowConcepts.get(terminologyId));
					conceptsRemoved++;
				}
				
				getLog().info("Workflow summary: " + conceptsAdded + " concepts added, " + conceptsRemoved + " concepts removed");

			}

			getLog().info("done ...");
			mappingService.close();
			workflowService.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Computing workflow failed.", e);
		}

	}
}