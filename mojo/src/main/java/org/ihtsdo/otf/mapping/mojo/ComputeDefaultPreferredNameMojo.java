/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which recomputes terminology preferred names.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal compute-default-preferred-names
 * 
 * @phase package
 */
public class ComputeDefaultPreferredNameMojo extends AbstractOtfMappingMojo {

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

  /** the type ids. */
  private static Long fsnTypeId = 900000000000003001L;
  private static Long definitionTypeId = 900000000000550004L;

  /** The dpn ref set ids. */
  private List<Long> dpnRefsetIdArray = new ArrayList<>();

  
  /** The default preferred names set (terminologyId -> {rank, dpn}). */
  private Map<String, String[]> defaultPreferredNames = new HashMap<>();

  /* see superclass */
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

      // get the config properties for default preferred name variables
      // set the dpn variables and instantiate the concept dpn map
      Properties properties = ConfigUtility.getConfigProperties();

      // set the parameters for determining defaultPreferredNames
      String props = properties.getProperty("loader.defaultPreferredNames.refSetId");
      String tokens[] = props.split(",");
      for (String prop : tokens) {
        if (prop != null) {
          dpnRefsetIdArray.add(Long.valueOf(prop));
        }
      }

      getLog().info("  dpnRefsetIdArray = " + dpnRefsetIdArray);
      
      setupBindInfoPackage();

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

      String[] result = computeDefaultPreferredName(concept);
      // If Dpn found
      if (result[1] != null) {
        // Set preferred name
        concept.setDefaultPreferredName(result[1]);
        contentService.updateConcept(concept);
        dpnFound = true;
        dpnFoundCt++;
        // Dpn not found
      } else {
        dpnFound = false;
        dpnNotFoundCt++;
        getLog()
            .warn("Could not find defaultPreferredName for concept " + concept.getTerminologyId());
        concept.setDefaultPreferredName("[Could not be determined]");
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

  /**
   * Helper function to access/add to dpn set.
   *
   * @param concept the concept
   * @return the rank dpn tuple
   * @throws Exception the exception
   */
  private String[] computeDefaultPreferredName(Concept concept) throws Exception {

    if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
      return defaultPreferredNames.get(concept.getTerminologyId());
    } else {
    
      // cycle over descriptions
      for (final Description description : concept.getDescriptions()) {

        // cycle over language ref sets
        for (final LanguageRefSetMember language : description.getLanguageRefSetMembers()) {

          if (description.isActive() && language.isActive()
              && dpnRefsetIdArray.contains(Long.valueOf(language.getRefSetId()))
              && !description.getTypeId().equals(definitionTypeId)) {

            // if the description/language refset pair match any of the ranked
            // refsetId/typeId/acceptabilityId triples,
            // this is a potential defaultPrefferedName
            int index = dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId()));

            // retrieve the concept for this description
            concept = description.getConcept();
            
            // check if this concept already had a dpn stored
            if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
              String[] rankValuePair = defaultPreferredNames.get(concept.getTerminologyId());
              // if the lang refset priority is higher than the priority of the previously
              // stored dpn, replace it
              if (dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId())) > Integer
                  .parseInt(rankValuePair[0])) {
                String[] newRankValuePair = {
                    Integer.valueOf(index).toString(), description.getTerm()
                };
                defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
              }
              // if the lang refset priority is the same as the previously stored dpn, but the typeId is fsn, replace it
              if (dpnRefsetIdArray.indexOf(Long.valueOf(language.getRefSetId())) == Integer
                  .parseInt(rankValuePair[0])) {
                if (description.getTypeId().equals(fsnTypeId)) {
                  String[] newRankValuePair = {
                      Integer.valueOf(index).toString(), description.getTerm()
                  };
                  defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
                }
              }
            // store first potential dpn
            } else {
              String[] newRankValuePair = {
                  Integer.valueOf(index).toString(), description.getTerm()
              };
              defaultPreferredNames.put(concept.getTerminologyId(), newRankValuePair);
            }
          }
        }
      }
      if (defaultPreferredNames.containsKey(concept.getTerminologyId())) {
        return defaultPreferredNames.get(concept.getTerminologyId());
      } else {
        getLog().warn(
          "Could not retrieve default preferred name for Concept " + concept.getTerminologyId());
        return null;
      }
    }
  }
}
