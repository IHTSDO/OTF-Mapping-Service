/*
 * 
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Goal which recomputes terminology preferred names.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal compute-default-preferred-names
 * 
 * @phase package
 */
public class ComputeDefaultPreferredNameMojo extends AbstractMojo {

  /**
   * Name of terminology.
   * 
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * The terminology version.
   * 
   * @parameter
   * @required
   */
  private String terminologyVersion;

  /** the defaultPreferredNames type id. */
  private Long dpnTypeId = 900000000000003001L;

  /** The dpn ref set id. */
  private Long dpnrefsetId = 900000000000509007L;

  /** The dpn acceptability id. */
  private Long dpnAcceptabilityId = 900000000000548007L;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {

    try {
      getLog().info("Starting comput default preferred names");
      getLog().info("  terminology = " + terminology);
      getLog().info("  terminologyVersion = " + terminologyVersion);

      computeDefaultPreferredNames();

      getLog().info("Done...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

  /**
   * Calculates default preferred names for any concept that has changed. Note:
   * at this time computes for concepts that have only changed due to
   * relationships, which is unnecessary
   *
   * @throws Exception the exception
   */
  private void computeDefaultPreferredNames() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    // Setup vars
    int dpnNotFoundCt = 0;
    int dpnFoundCt = 0;
    int dpnSkippedCt = 0;
    int objectCt = 0;

    ConceptList concepts =
        contentService.getAllConcepts(terminology, terminologyVersion);
    contentService.clear();

    // Iterate over concepts
    for (Concept concept2 : concepts.getConcepts()) {

      Concept concept = contentService.getConcept(concept2.getId());

      // Skip if inactive
      if (!concept.isActive()) {
        dpnSkippedCt++;
        continue;
      }

      getLog().debug("  Concept " + concept.getTerminologyId());

      boolean dpnFound = false;

      // Iterate over descriptions
      for (Description description : concept.getDescriptions()) {

        // If active andn preferred type
        if (description.isActive() && description.getTypeId().equals(dpnTypeId)) {

          // Iterate over language refset members
          for (LanguageRefSetMember language : description
              .getLanguageRefSetMembers()) {
            // If prefrred and has correct refset
            if (new Long(language.getRefSetId()).equals(dpnrefsetId)
                && language.isActive()
                && language.getAcceptabilityId().equals(dpnAcceptabilityId)) {
              // print warning for multiple names found
              if (dpnFound) {
                getLog().warn(
                    "Multiple default preferred names found for concept "
                        + concept.getTerminologyId());
                getLog().warn(
                    "  " + "Existing: " + concept.getDefaultPreferredName());
                getLog().warn("  " + "Replaced with: " + description.getTerm());
              }

              // Set preferred name
              concept.setDefaultPreferredName(description.getTerm());

              // set found to true
              dpnFound = true;

            }
          }
        }

      }

      // Pref name not found
      if (!dpnFound) {
        dpnNotFoundCt++;
        getLog().warn(
            "Could not find defaultPreferredName for concept "
                + concept.getTerminologyId());
        concept.setDefaultPreferredName("[Could not be determined]");
      } else {
        dpnFoundCt++;
      }

      // periodically comit
      if (++objectCt % 5000 == 0) {
        getLog().info("    count = " + objectCt);
        contentService.commit();
        contentService.clear();
        contentService.beginTransaction();
      }
    }

    contentService.commit();
    contentService.close();
    getLog().info("  found =  " + dpnFoundCt);
    getLog().info("  not found = " + dpnNotFoundCt);
    getLog().info("  skipped = " + dpnSkippedCt);

  }

}
