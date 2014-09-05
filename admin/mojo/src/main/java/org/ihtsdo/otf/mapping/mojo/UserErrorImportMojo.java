package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Admin tool to convert old user error objects into feedback objects
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>ImportUserErrors</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>import-user-errors</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>import-user-errors</goal>
 *                 </goals>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile> 
 * </pre>
 * 
 * @goal import-user-errors
 * @phase package
 */
public class UserErrorImportMojo extends AbstractMojo {


  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting conversion of user errors into feedback");


    try {

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();
      
      workflowService.convertUserErrors();
      
      workflowService.commit();

      getLog().info("done ...");
      workflowService.close();
      workflowService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing map group QA failed.", e);
    }

  }
}