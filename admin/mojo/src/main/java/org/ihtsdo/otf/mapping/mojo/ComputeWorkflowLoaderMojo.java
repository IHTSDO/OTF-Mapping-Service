package org.ihtsdo.otf.mapping.mojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.OtfEmailHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

// TODO: Auto-generated Javadoc
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
 *                   <sendNotification>${send.notification}</sendNotification>
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

	/** The refSet id. 
	 * @parameter refSetId 
	 * */
	private String refSetId = null;
	
	/** The send notification.
	 * @parameter sendNotification
	  */
	private int sendNotification = 0;

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException
	 *             the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Starting compute workflow - " + refSetId + " - " + sendNotification);
		
	
		Properties config;
		try {
			config = ConfigUtility.getConfigProperties();
		} catch (Exception e1) {
			throw new MojoExecutionException("Failed to retrieve config properties");
		}
		String notificationRecipients = config.getProperty("loader.SNOMEDCT.delta.notification.recipients");
		String notificationMessage = "";
		

		if (refSetId == null) {
			throw new MojoExecutionException("You must specify a refSetId.");
		}
		
		if (sendNotification == 0) {
			getLog().info("No notifications will be sent as a result of workflow computation.");
		}
		
		if (sendNotification == 1 && 
				config.getProperty("loader.SNOMEDCT.delta.notification.recipients") == null) {
			throw new MojoExecutionException("Email notification was requested, but no recipients were specified.");
		} else {
			getLog().info("Request to send notification email to recipients: " + notificationRecipients);
			notificationMessage += "Hello,\n\nWorkflow for the mapping tool has been recomputed.  Changes to the pool of available work are indicated below for each project\n\n";
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

				// construct a map of terminology id -> concept name
				// used to determine change in workflow status after recomputation
				Map<String, String> previousWorkflowConcepts = new HashMap<>();
				int conceptsAdded = 0;
				int conceptsRemoved = 0;

				// add all current concepts with a tracking record to set
				for (TrackingRecord tr : workflowService
						.getTrackingRecordsForMapProject(mapProject)
						.getIterable()) {
					previousWorkflowConcepts.put(tr.getTerminologyId(), tr.getDefaultPreferredName());
				}

				// recompute workflow
				getLog().info(
						"Computing workflow for " + mapProject.getName() + ", "
								+ mapProject.getId());
				workflowService.computeWorkflow(mapProject);
				
				getLog().info(
						"Comparing new workflow to previous workflow for " + mapProject.getName() + ", "
								+ mapProject.getId());
				
				// cycle over new workflow and compare to previously stored values
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
				
				notificationMessage += "Project: " + mapProject.getName() + "\n"
									+  "\tConcepts Added:   " + conceptsAdded
									+  "\tConcepts Removed: " + conceptsRemoved
									+ "\n\n";
				

			}
			
			notificationMessage += "Key:"
					+ "\t'Concepts Added' refers to new concepts that were added via the drip feed/delta loader\n"
					+ "\t'Concepts Removed' refers to concepts with unfinished editing that were removed from scope, i.e. are no longer referred to in the drip feed";

			getLog().info("done ...");
			mappingService.close();
			workflowService.close();
			
			// if notification requested, send email
			if (sendNotification == 1) {
				OtfEmailHandler emailHandler = new OtfEmailHandler();
				emailHandler.sendSimpleEmail(notificationRecipients, "[OTF-Mapping-Tool] Drip feed results", notificationMessage);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Computing workflow failed.", e);
		}

	}
}