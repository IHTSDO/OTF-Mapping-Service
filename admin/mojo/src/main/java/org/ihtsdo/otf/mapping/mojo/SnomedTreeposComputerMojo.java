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
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>compute-snomed-treepos</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>compute-snomed-treepos</goal>
 *           </goals>
 *           <configuration>
 *             <terminology>SNOMEDCT</terminology>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal compute-snomed-treepos
 * 
 * @phase package
 */
public class SnomedTreeposComputerMojo extends AbstractMojo {

  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * Instantiates a {@link SnomedTreeposComputerMojo} from the specified
   * parameters.
   * 
   */
  public SnomedTreeposComputerMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting computing of SNOMEDCT tree positions ...");

    try {

      // Get terminology version
      MetadataServiceJpa metadataService = new MetadataServiceJpa();
      String version =
          metadataService.getTerminologyLatestVersions().get(terminology);

      // creating tree positions
      // first get isaRelType from metadata
      Map<String, String> hierRelTypeMap =
          metadataService
              .getHierarchicalRelationshipTypes(terminology, version);
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
        Concept c = contentService.getConcept(conceptId, terminology, version);
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
      contentService.computeTreePositions(terminology, version, isaRelType,
          rootId);

      contentService.close();

      getLog().info("done ...");

    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
