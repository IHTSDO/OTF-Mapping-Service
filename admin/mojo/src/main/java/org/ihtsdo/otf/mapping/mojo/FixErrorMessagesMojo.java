package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Admin tool to fix the error messages in the feedback objects.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>FixErrorMessages</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>fix-error-messages</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>fix-error-messages</goal>
 *                 </goals>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile> 
 * </pre>
 * 
 * @goal fix-error-messages
 * @phase package
 */
public class FixErrorMessagesMojo extends AbstractMojo {


  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting the fix of error messages.");


    try {

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();
      
      workflowService.fixErrorMessages();
      
      workflowService.commit();

      getLog().info("done ...");
      workflowService.close();
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Fixing the error messages failed.", e);
    }

  }
}