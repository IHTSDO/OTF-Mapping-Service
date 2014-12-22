package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Goal which finishes a release and loads the release version of the complex
 * map refset members for the project back in.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal finish-release
 * 
 * @phase package
 */
public class ReleaseFinishMojo extends AbstractMojo {

  /**
   * The refSet id.
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * The RF2 input file
   * @parameter
   * @required
   */
  private String inputFile;

  /** The date format. */
  private final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");

  /** counter for objects created, reset in each load section. */
  int objectCt; //

  /** the number of objects to create before committing. */
  int logCt = 1000;

  /**
   * Instantiates a {@link ReleaseFinishMojo} from the specified parameters.
   * 
   */
  public ReleaseFinishMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Finishing RF2 Release");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  inputFile = " + inputFile);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a ref set id");
    }

    if (refsetId.contains(",")) {
      throw new MojoExecutionException(
          "You must specify only a single ref set id");
    }

    if (inputFile == null) {
      throw new MojoExecutionException("You must specify an input file");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = null;
      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        if (project.getRefSetId().equals(refsetId)) {
          mapProject = project;
          break;
        }
      }
      if (mapProject == null) {
        throw new Exception("Unable to find map project for refset " + refsetId);
      }

      //
      // Determine version
      //
      getLog().info("  terminology = " + mapProject.getSourceTerminology());
      getLog().info("  version = " + mapProject.getSourceTerminologyVersion());

      // Load map refset
      loadMapRefSets(mapProject);

      getLog().info("Done...");

      // Clean-up
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

  /**
   * Load map refset from file
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  private void loadMapRefSets(MapProject mapProject) throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();
    File f = new File(inputFile);
    if (!f.exists()) {
      throw new Exception("Input file does not exist: " + f.toString());
    }

    BufferedReader reader = new BufferedReader(new FileReader(f));

    final String terminology = mapProject.getSourceTerminology();
    final String version = mapProject.getSourceTerminologyVersion();
    while ((line = reader.readLine()) != null) {

      line = line.replace("\r", "");
      final String fields[] = line.split("\t");
      
      // skip header
      if (!fields[0].equals("id")) {
        final ComplexMapRefSetMember member = new ComplexMapRefSetMemberJpa();

        member.setTerminologyId(fields[0]);
        member.setEffectiveTime(dt.parse(fields[1]));
        member.setActive(fields[2].equals("1") ? true : false);
        member.setModuleId(Long.valueOf(fields[3]));
        member.setRefSetId(fields[4]);
        // conceptId

        // ComplexMap unique attributes
        member.setMapGroup(Integer.parseInt(fields[6]));
        member.setMapPriority(Integer.parseInt(fields[7]));
        member.setMapRule(fields[8]);
        member.setMapAdvice(fields[9]);
        member.setMapTarget(fields[10]);
        if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ComplexMap) {
          member.setMapRelationId(Long.valueOf(fields[11]));
        } else if (mapProject.getMapRefsetPattern() == MapRefsetPattern.ExtendedMap) {
          member.setMapRelationId(Long.valueOf(fields[12]));

        } else {
          throw new Exception("Unsupported map type "
              + mapProject.getMapRefsetPattern());
        }

        // ComplexMap unique attributes NOT set by file (mapBlock
        // elements) - set defaults
        member.setMapBlock(0); 
        member.setMapBlockRule(null);
        member.setMapBlockAdvice(null);

        // Terminology attributes
        member.setTerminology(terminology);
        member.setTerminologyVersion(version);

        // set Concept
        final Concept concept =
            contentService.getConcept(fields[5], terminology, version);

        // regularly log at intervals
        if (++objectCt % logCt == 0) {
          getLog().info("    count = " + objectCt);
        }
        
        if (concept != null) {
          member.setConcept(concept);
          contentService.addComplexMapRefSetMember(member);
        } else {
          throw new Exception("Member references non-existent concept - " + member);
        }

      }
    }

    // commit any remaining objects
    contentService.commit();
    contentService.close();
    reader.close();

  }

}
