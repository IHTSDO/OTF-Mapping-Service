package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Admin tool to fix the error flag on feedback objects based on the setting of mapError.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>FixFeedbackErrorFlag</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>fix-feedback-error-flag</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>fix-feedback-error-flag</goal>
 *                 </goals>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile> 
 * </pre>
 * 
 * @goal fix-feedback-error-flag
 * @phase package
 */
public class FixFeedbackErrorFlagMojo extends AbstractMojo {


  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting the fix of feedback error flags.");


    try {

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();
      
      workflowService.fixFeedbackErrorFlag();
      
      workflowService.commit();

      getLog().info("done ...");
      workflowService.close();
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Fixing the feedback error flag failed.", e);
    }

  }
}