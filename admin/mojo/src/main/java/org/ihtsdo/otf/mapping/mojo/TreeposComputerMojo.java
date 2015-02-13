package org.ihtsdo.otf.mapping.mojo;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal compute-treepos
 * 
 * @phase package
 */
public class TreeposComputerMojo extends AbstractMojo {

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
   * Instantiates a {@link TreeposComputerMojo} from the specified parameters.
   * 
   */
  public TreeposComputerMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting computing tree positions");
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  rootIds = " + rootIds);

    try {

      // creating tree positions
      // first get isaRelType from metadata
      MetadataServiceJpa metadataService = new MetadataServiceJpa();
      Map<String, String> hierRelTypeMap =
          metadataService.getHierarchicalRelationshipTypes(terminology,
              terminologyVersion);
      String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
      metadataService.close();

      ContentService contentService = new ContentServiceJpa();
      getLog().info("Start creating tree positions.");

      // Walk up tree to the root
      // ASSUMPTION: single root
      for (String rootId : rootIds.split(",")) {
        getLog().info("  Compute tree from rootId " + rootId);
        contentService.computeTreePositions(terminology, terminologyVersion,
            isaRelType, rootId);
      }
      contentService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
