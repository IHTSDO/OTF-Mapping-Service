package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads unpublished complex maps.
 * 
 * Sample execution:
 * 
 * <pre>
 *     <profile>
 *       <id>Release</id>
 *       <build>
 *         <plugins>
 *           <plugin>
 *             <groupId>org.ihtsdo.otf.mapping</groupId>
 *             <artifactId>mapping-admin-mojo</artifactId>
 *             <version>${project.version}</version>
 *             <executions>
 *               <execution>
 *                 <id>release</id>
 *                 <phase>package</phase>
 *                 <goals>
 *                   <goal>release</goal>
 *                 </goals>
 *                 <configuration>
 *                   <refSetId>${refset.id}</refSetId>
 *                   <outputDirName>$(output.dir)</outputDirName>
 *                   <effectiveTime>${time}</effectiveTime>
 *                   <moduleId>${module.id}</moduleId>
 *                 </configuration>
 *               </execution>
 *             </executions>
 *           </plugin>
 *         </plugins>
 *       </build>
 *     </profile>
 * </pre>
 * 
 * @goal release
 * @phase package
 */
public class ReleaseProcessingMojo extends AbstractMojo {

	/**
	 * The refSet id
	 * 
	 * @parameter refSetId
	 */
	private String refSetId = null;
	
	/**
	 * The refSet id
	 * 
	 * @parameter outputDirName
	 */
	private String outputDirName = null;
	
	/** 
	 * The effective time of release
	 * @parameter effectiveTime
	 */
	private String effectiveTime = null;
	
	/** 
	 * The module id.
	 * @parameter moduleId
	 */
	private String moduleId = null;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting workflow quality assurance checks - " + refSetId);

	    if (refSetId == null) {
	      throw new MojoExecutionException("You must specify a refSetId.");
	    }
	    
	    if (refSetId == null) {
	      throw new MojoExecutionException("You must specify an output file directory.");
	    }
	    
	    File outputDir = new File(outputDirName);
	    if (!outputDir.isDirectory())
	    	throw new MojoExecutionException("Output file directory specified could not be found.");
	    
	    if (effectiveTime == null)
	    	throw new MojoExecutionException("You must specify a release time");
	    
	    if (moduleId == null)
	    	throw new MojoExecutionException("You must specify a module id");
	    
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
	      
	      DateFormat df = new SimpleDateFormat("yyyyMMdd");
	      
	      // Perform the release processing
	      for (MapProject mapProject : mapProjects) {
	        getLog().info(
	            "Processing release for " + mapProject.getName() + ", "
	                + mapProject.getId());
	        
	        
	        
	        String releaseFileName = "release_" + mapProject.getSourceTerminology() + "_" + mapProject.getSourceTerminologyVersion()
	        		+ "_" + mapProject.getDestinationTerminology() + "_" + mapProject.getDestinationTerminologyVersion()
	        		+ "_" + df.format(new Date());
	        
	        getLog().info(
		            "  Release file:  " + releaseFileName);
	        
	        
	        
	        
	        mappingService.processRelease(mapProject, releaseFileName, effectiveTime, moduleId);
	      }

	      getLog().info("done ...");
	      mappingService.close();
	      
	    } catch (Exception e) {
	    	e.printStackTrace();
	        throw new MojoExecutionException("Performing release processing failed.", e);
	    }

	}

}
