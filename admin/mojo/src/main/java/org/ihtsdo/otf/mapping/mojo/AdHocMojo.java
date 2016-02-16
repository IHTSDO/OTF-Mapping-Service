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
    /**
     * Put ad hoc code below
     */

    // 2/12/16 -- apply map advice to map entries referencing asterisk target
    // codes in ICD10

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

      MapAdvice mapAdvice = null;

      // find the desired advice
      for (MapAdvice m : workflowService.getMapAdvices().getMapAdvices()) {
        if (m
            .getName()
            .equals(
                "THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE")) {
          mapAdvice = m;
        }
      }

      if (mapAdvice == null) {
        throw new MojoExecutionException(
            "Could not find map advice with name THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE");
      }
      Logger.getLogger(this.getClass()).info(
          "Found map advice " + mapAdvice.getName());

      // find the ICD10 asterisk code
      String asteriskConceptId = null;

      PfsParameter pfs = new PfsParameterJpa();
      pfs.setQueryRestriction("terminology:ICD10 AND terminologyVersion:2010 AND defaultPreferredName:\"Asterisk refset\"");
      try {
        SearchResultList searchResults = contentService.findConceptsForQuery(null, pfs);
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

      Logger.getLogger(this.getClass())
          .info("Loading published and publication-ready records...");

      // count variables for log output
      int nTotal = 0;
      int nAdded = 0;

      // cycle over all map records for project
      List<MapRecord> mapRecords =
          workflowService.getMapRecordsForMapProject(mapProject.getId()).getMapRecords();
      
      Logger.getLogger(this.getClass())
      .info("Cycling over published and publication-ready records...");
      
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

        // cycle over entries
        for (MapEntry me : mr.getMapEntries()) {

          // if an icd10 assterisk code
          if (me.getMapGroup() == 1 && isIcd10AsteriskCode(me.getTargetId(), asteriskConceptId)) {

            if (!me.getMapAdvices().contains(mapAdvice)) {

              Logger.getLogger(AdHocMojo.class).info(
                  "Adding missing advice to map record " + mr.getId()
                      + " for concept " + mr.getConceptId() + ", map entry "
                      + me.getId() + " with target " + me.getTargetId());

              // add advice
              me.addMapAdvice(mapAdvice);
              recordChanged = true;
              nAdded++;

            }
          } else if (me.getMapAdvices().contains(mapAdvice)) {
            Logger.getLogger(AdHocMojo.class)
            .info("Removing extraneous advice from map record " + mr.getId()
                + " for concept " + mr.getConceptId() + ", map entry "
                + me.getId() + " with target " + me.getTargetId());
            
            me.removeMapAdvice(mapAdvice);
            recordChanged = true;
          }
        }

        if (recordChanged == true) {
          workflowService.updateMapRecord(mr);
        }
      }

      Logger.getLogger(this.getClass()).info(
          "Checked " + nTotal + " published/publication-ready records ("
              + mapRecords.size() + " in project");
      Logger.getLogger(this.getClass()).info(
          "Added missing advice to " + nAdded + " entries referencing "
              + icd10AsteriskCodes.size() + " asterisk code targets");

      Logger.getLogger(this.getClass()).info("Committing...");

      // execute the transaction
      workflowService.commit();

      Logger.getLogger(this.getClass()).info("Finished");

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
