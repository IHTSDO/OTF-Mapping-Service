package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Customizable mojo to run ad hoc code
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-ad-hoc
 */
public class AdHocMojo extends AbstractMojo {

  /** The content service. */
  ContentService contentService = null;

  /** The workflow service. */
  WorkflowService workflowService = null;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Start Ad hoc mojo");
    /**
     * Put ad hoc code below:
     * 
     * 20160226:
     * 
     * <pre>
     * Find all map records from project with refsetid 447562003
     * If there is a single map entry and it is an asterisk code
     *   Remove advice “THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION”
     *     If it exists
     *   Add  advice “THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE”
     *     If it does not exist
     *   Add advice “THIS MAP REQUIRES A DAGGER CODE AS WELL AS AN ASTERISK CODE”
     *     If it does not exist
     * If there are more than one map entries, for each entry whose targetId is an asterisk code:
     *   Remove advice “THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION”
     * If it exists
     *   Add  advice “THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE”
     *     If it does not exist
     * </pre>
     */

    try {
      contentService = new ContentServiceJpa();
      workflowService = new WorkflowServiceJpa();

      // want a single commit
      workflowService.setTransactionPerOperation(false);

      // find ICD10 project
      MapProject mapProject = null;
      for (MapProject p : workflowService.getMapProjects().getMapProjects()) {
        if (p.getRefSetId().equals("447562003")) {
          mapProject = p;
        }
      }

      if (mapProject == null) {
        throw new MojoExecutionException(
            "Could not find map project for refset id 447562003");
      }
      Logger.getLogger(this.getClass()).info(
          "Found map project " + mapProject.getName());

      MapAdvice advice1 = null;
      MapAdvice advice2 = null;
      MapAdvice advice3 = null;

      // find the desired advice
      for (MapAdvice m : workflowService.getMapAdvices().getMapAdvices()) {
        if (m.getName().equals(
            "THIS CODE MAY BE USED IN THE PRIMARY POSITION "
                + "WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE")) {
          advice1 = m;
        }

        else if (m.getName().equals(
            "THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION")) {
          advice2 = m;
        }

        else if (m.getName().equals(
            "THIS MAP REQUIRES A DAGGER CODE AS WELL AS AN ASTERISK CODE")) {
          advice3 = m;
        }
      }

      if (advice1 == null || advice2 == null || advice3 == null) {
        throw new MojoExecutionException("Could not find map advices");
      }
      getLog().info("  Found map advice - " + advice1.getName());
      getLog().info("  Found map advice - " + advice2.getName());
      getLog().info("  Found map advice - " + advice3.getName());

      // find the ICD10 asterisk code
      String asteriskConceptId = null;

      PfsParameter pfs = new PfsParameterJpa();
      pfs.setQueryRestriction("terminology:ICD10 AND terminologyVersion:2010 AND defaultPreferredName:\"Asterisk refset\"");
      try {
        SearchResultList searchResults =
            contentService.findConceptsForQuery(null, pfs);
        if (searchResults.getCount() != 1) {
          throw new MojoFailureException(
              "Found multiple asterisk concepts for ICD10: "
                  + searchResults.toString());
        }
        asteriskConceptId =
            searchResults.getSearchResults().get(0).getTerminologyId();
      } catch (Exception e) {
        e.printStackTrace();
        throw new MojoFailureException("Could not find asterisk code for ICD10");
      }

      Logger.getLogger(this.getClass()).info(
          "Found hibernate id for ICD10 asterisk refset: " + asteriskConceptId);

      Logger.getLogger(this.getClass()).info(
          "Loading published and publication-ready records...");

      // count variables for log output
      int nTotal = 0;
      int nChanged = 0;

      // cycle over all map records for project
      List<MapRecord> mapRecords =
          workflowService.getMapRecordsForMapProject(mapProject.getId())
              .getMapRecords();

      Logger.getLogger(this.getClass()).info(
          "Cycling over published and publication-ready records...");

      workflowService.beginTransaction();

      Iterator<MapRecord> iter = mapRecords.iterator();

      while (iter.hasNext()) {
        MapRecord mr = iter.next();
        boolean recordChanged = false;

        // skip non-published records
        if (!mr.getWorkflowStatus()
            .equals(WorkflowStatus.READY_FOR_PUBLICATION)
            && !mr.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          continue;
        }

        nTotal++;

        // If there is a single map entry and it is an asterisk code
        // Remove advice “THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION”
        // If it exists
        // Add advice “THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE
        // MANIFESTATION IS THE PRIMARY FOCUS OF CARE”
        // If it does not exist
        // Add advice “THIS MAP REQUIRES A DAGGER CODE AS WELL AS AN ASTERISK
        // CODE”
        // If it does not exist
        if (mr.getMapEntries().size() == 1) {
          final MapEntry entry = mr.getMapEntries().get(0);

          if (isIcd10AsteriskCode(entry.getTargetId(), asteriskConceptId)) {
            if (entry.getMapAdvices().contains(advice2)) {
              entry.removeMapAdvice(advice2);
              recordChanged = true;
              getLog().info("    remove " +  mr.getConceptId() + " " + advice2);
            }
            if (!entry.getMapAdvices().contains(advice1)) {
              entry.addMapAdvice(advice1);
              recordChanged = true;
              getLog().info("    add " +  mr.getConceptId() + " " + advice1);
            } 
            if (!entry.getMapAdvices().contains(advice3)) {
              entry.addMapAdvice(advice3);
              recordChanged = true;
              getLog().info("    add " +  mr.getConceptId() + " " + advice3);
            }
            if (recordChanged) {
              getLog().info("    changed " + mr.getConceptId());
              nChanged++;
            }
          }
        }

        else {

          // cycle over entries
          for (final MapEntry entry : mr.getMapEntries()) {

            // if an icd10 asterisk code
            if (isIcd10AsteriskCode(entry.getTargetId(), asteriskConceptId)) {

              // If there are more than one map entries, for each entry whose
              // targetId is an asterisk code:
              // Remove advice “THIS CODE IS NOT TO BE USED IN THE PRIMARY
              // POSITION”
              // If it exists
              // Add advice “THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN
              // THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE”
              // If it does not exist

              if (entry.getMapAdvices().contains(advice2)) {
                entry.getMapAdvices().remove(advice2);
                recordChanged = true;
                getLog().info("    remove " +  mr.getConceptId() + " " + advice2);
              }

              if (!entry.getMapAdvices().contains(advice1)) {
                entry.getMapAdvices().add(advice1);
                recordChanged = true;
                getLog().info("    add " +  mr.getConceptId() + " " + advice2);
              }
            }
          }
          if (recordChanged) {
            getLog().info("    changed " + mr.getConceptId());
            nChanged++;
          }
        }

        if (recordChanged == true) {
          workflowService.updateMapRecord(mr);
        }
      }

      getLog().info("  changed = " + nChanged);
      getLog().info("  total = " + nTotal);

      getLog().info("Committing...");

      // execute the transaction
      workflowService.commit();

      getLog().info("Finished");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Ad-hoc mojo failed to complete", e);
    } finally {
      try {
        contentService.close();
        workflowService.close();
      } catch (Exception e) {
        e.printStackTrace();
        throw new MojoExecutionException(
            "Ad-hoc mojo failed to close services.", e);
      }
    }

  }

  // because even in an adhoc lightweight mojo I felt the need to cache query
  // results
  /** The icd10 asterisk codes. */
  Set<String> icd10AsteriskCodes = new HashSet<>();

  /**
   * Indicates whether or not icd10 asterisk code is the case.
   *
   * @param targetId the target id
   * @param asteriskCode the asterisk code
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  private boolean isIcd10AsteriskCode(String targetId, String asteriskCode)
    throws Exception {

    // if empty or null, return
    if (targetId == null || targetId.isEmpty()) {
      return false;
    }

    // check cache
    if (icd10AsteriskCodes.contains(targetId)) {
      return true;
    }

    // otherwise get the concept
    Concept c = contentService.getConcept(targetId, "ICD10", "2010");

    if (c == null) {
      throw new MojoFailureException("Could not retrieve ICD10 concept "
          + targetId + ", aborting");
    }

    for (SimpleRefSetMember srs : c.getSimpleRefSetMembers()) {
      if (srs.getRefSetId().equals(asteriskCode)) {
        icd10AsteriskCodes.add(targetId);
        return true;
      }
    }

    return false;

  }

}
