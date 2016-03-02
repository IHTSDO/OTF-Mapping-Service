package org.ihtsdo.otf.mapping.mojo;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * Goal which performs a cycle check.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal cycle-check
 * 
 * @phase package
 */
public class CycleCheckMojo extends AbstractMojo {

  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * The terminology version.
   * @parameter
   * @required
   */
  private String terminologyVersion;

  /**
   * A comma-separated list of the root ids
   * @parameter
   * @requried
   */
  private String rootIds;

  /**
   * Instantiates a {@link CycleCheckMojo} from the specified parameters.
   * 
   */
  public CycleCheckMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting cycle check");
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  rootIds = " + rootIds);

    try {

      // creating tree positions
      // first get isaRelType from metadata
      final MetadataService metadataService = new MetadataServiceJpa();
      Map<String, String> hierRelTypeMap =
          metadataService.getHierarchicalRelationshipTypes(terminology,
              terminologyVersion);
      String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
      metadataService.close();
      getLog().info("Start creating tree positions.");

      final ContentService contentService = new ContentServiceJpa();
      for (String rootId : rootIds.split(",")) {
        getLog().info("  Cycle check from rootId " + rootId);
        contentService.cycleCheck(terminology, terminologyVersion, isaRelType,
            rootId);
      }
      contentService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
