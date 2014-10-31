package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which exports project data to text files.
 * 
 * Sample execution;
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>export-project-data</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>export-project-data</goal>
 *           </goals>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal export-project-data
 * 
 * @phase package
 */
public class MapProjectDataExportMojo extends AbstractMojo {

  /**
   * Instantiates a {@link MapProjectDataExportMojo} from the specified
   * parameters.
   * 
   */
  public MapProjectDataExportMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting exporting metadata ...");

    try {

      Properties config = ConfigUtility.getConfigProperties();

      // set the output directory
      String outputDirString = config.getProperty("export.output.dir");

      File outputDir = new File(outputDirString);
      if (!outputDir.exists()) {
        throw new MojoFailureException(
            "Specified export.output.dir directory does not exist: "
                + outputDirString);
      }
      
      JAXBContext jaxbContext = null;
      jaxbContext = JAXBContext.newInstance(MapProjectJpa.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            
      // export project data to project specific files
      MappingService mappingService = new MappingServiceJpa();
      for (MapProject mpr : mappingService.getMapProjects().getMapProjects()) {
      	StringWriter writer = new StringWriter();
        
        File projectsFile = new File(outputDir, "Project" + mpr.getId() + ".xml");
        // if file doesn't exist, then create it
        if (!projectsFile.exists()) {
        	projectsFile.createNewFile();
        }
      	
      	BufferedWriter projectWriter =
            new BufferedWriter(
                new FileWriter(projectsFile.getAbsoluteFile()));
        
        jaxbMarshaller.marshal(mpr, writer);

        projectWriter.write(writer.toString());
        writer.close();
        projectWriter.close();
        
        
        // Write out scope includes/excludes lists to separate files, also by project
        File scopeIncludesFile = new File(outputDir, "Project" + mpr.getId() + "Scope.txt");
        // if file doesn't exist, then create it
        if (!scopeIncludesFile.exists()) {
          scopeIncludesFile.createNewFile();
        }
        BufferedWriter scopeIncludesWriter =
            new BufferedWriter(
                new FileWriter(scopeIncludesFile.getAbsoluteFile()));

        File scopeExcludesFile = new File(outputDir, "Project" + mpr.getId() + "ScopeExcludes.txt");
        // if file doesn't exist, then create it
        if (!scopeExcludesFile.exists()) {
          scopeExcludesFile.createNewFile();
        }
        BufferedWriter scopeExcludesWriter =
            new BufferedWriter(
                new FileWriter(scopeExcludesFile.getAbsoluteFile()));

        
        for (String concept : mpr.getScopeConcepts()) {
          scopeIncludesWriter.write(concept + "\n");
        }
        scopeIncludesWriter.close();
        
        for (String concept : mpr.getScopeExcludedConcepts()) {
          scopeExcludesWriter.write(concept + "\n");
        }
        scopeExcludesWriter.close();
      }
      

      mappingService.close();

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

}
