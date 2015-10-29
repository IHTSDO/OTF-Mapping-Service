package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which exports project data to text files.
 * 
 * See admin/exporter/pom.xml for a sample execution.
 * 
 * @goal export-project-data
 * 
 * @phase package
 */
public class MapProjectDataExportMojo extends AbstractMojo {

  /**
   * The output dir
   * @parameter
   * @required
   */
  private String outputDir;

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
    getLog().info("Starting exporting metadata");
    getLog().info("  outputDir = " + outputDir);

    try {

      File outputDirFile = new File(outputDir);
      if (!outputDirFile.exists()) {
        throw new MojoFailureException(
            "Specified output directory does not exist");
      }

      JAXBContext jaxbContext = null;
      jaxbContext = JAXBContext.newInstance(MapProjectJpa.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

      // export project data to project specific files
      MappingService mappingService = new MappingServiceJpa();
      for (MapProject mpr : mappingService.getMapProjects().getMapProjects()) {

        // Write out scope and scope excludes info
        File scopeIncludesFile =
            new File(outputDirFile, "Project" + mpr.getId() + "Scope.txt");
        // if file doesn't exist, then create it
        if (!scopeIncludesFile.exists()) {
          scopeIncludesFile.createNewFile();
        }
        BufferedWriter scopeIncludesWriter =
            new BufferedWriter(new FileWriter(
                scopeIncludesFile.getAbsoluteFile()));

        File scopeExcludesFile =
            new File(outputDirFile, "Project" + mpr.getId()
                + "ScopeExcludes.txt");
        // if file doesn't exist, then create it
        if (!scopeExcludesFile.exists()) {
          scopeExcludesFile.createNewFile();
        }
        BufferedWriter scopeExcludesWriter =
            new BufferedWriter(new FileWriter(
                scopeExcludesFile.getAbsoluteFile()));

        for (String concept : mpr.getScopeConcepts()) {
          scopeIncludesWriter.write(concept + "\n");
        }
        scopeIncludesWriter.close();

        for (String concept : mpr.getScopeExcludedConcepts()) {
          scopeExcludesWriter.write(concept + "\n");
        }
        scopeExcludesWriter.close();

        // Clear scope concepts list for this part
        mpr.setScopeConcepts(new HashSet<String>());
        mpr.setScopeExcludedConcepts(new HashSet<String>());

        // Write out map project
        StringWriter writer = new StringWriter();
        File projectsFile =
            new File(outputDirFile, "Project" + mpr.getId() + ".xml");
        // if file doesn't exist, then create it
        if (!projectsFile.exists()) {
          projectsFile.createNewFile();
        }
        BufferedWriter projectWriter =
            new BufferedWriter(new FileWriter(projectsFile.getAbsoluteFile()));
        jaxbMarshaller.marshal(mpr, writer);

        projectWriter.write(writer.toString());
        writer.close();
        projectWriter.close();

      }

      mappingService.close();

      getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

}
