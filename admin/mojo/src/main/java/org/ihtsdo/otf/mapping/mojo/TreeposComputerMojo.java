package org.ihtsdo.otf.mapping.mojo;

import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
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
      String conceptId = isaRelType;
      String rootId = null;
      OUTER: while (true) {
        getLog().info("  Walk up tree from " + conceptId);
        Concept c =
            contentService.getConcept(conceptId, terminology,
                terminologyVersion);
        for (Relationship r : c.getRelationships()) {
          if (r.isActive() && r.getTypeId().equals(Long.valueOf(isaRelType))) {
            conceptId = r.getDestinationConcept().getTerminologyId();
            continue OUTER;
          }
        }
        rootId = conceptId;
        break;
      }
      getLog().info("  Compute tree from rootId " + conceptId);
      contentService.computeTreePositions(terminology, terminologyVersion,
          isaRelType, rootId);

      contentService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
