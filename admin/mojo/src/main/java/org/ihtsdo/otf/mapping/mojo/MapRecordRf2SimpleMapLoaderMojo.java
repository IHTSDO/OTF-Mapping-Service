package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads simple maps. - the members flag loads refset members if "true" - the
 * records flag loads map records if "true"
 *
 * 
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-simple-map
 * @phase package
 */
public class MapRecordRf2SimpleMapLoaderMojo extends AbstractMojo {

  /**
   * The input file.
   * @parameter
   * @required
   */
  private String inputFile;

  /**
   * The members flag.
   * @parameter
   * @required
   */
  private boolean memberFlag;

  /**
   * The records flag.
   * @parameter
   * @required
   */
  private boolean recordFlag;

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading complex map data");
    getLog().info("  inputFile = " + inputFile);

    // Set up map of refsetIds that we may encounter
    MappingService mappingService = null;
    ContentService contentService = null;
    try {

      if (inputFile == null || !new File(inputFile).exists()) {
        throw new MojoFailureException("Specified input file missing");
      }

      mappingService = new MappingServiceJpa();
      contentService = new ContentServiceJpa();

      // get the loader user
      MapUser loaderUser = mappingService.getMapUser("loader");

      // if loader user does not exist, add it
      if (loaderUser == null) {
        loaderUser = new MapUserJpa();
        loaderUser.setApplicationRole(MapUserRole.VIEWER);
        loaderUser.setUserName("loader");
        loaderUser.setName("Loader Record");
        loaderUser.setEmail("none");
        loaderUser = mappingService.addMapUser(loaderUser);
      }

      // Get map projects
      Map<String, MapProject> mapProjectMap = new HashMap<>();
      for (final MapProject project : mappingService.getMapProjects()
          .getIterable()) {
        mapProjectMap.put(project.getRefSetId(), project);
      }

      // load complexMapRefSetMembers from simpleMap file
      List<SimpleMapRefSetMember> members =
          getSimpleMaps(new File(inputFile), mapProjectMap);

      // If the member flag is set, insert all of these
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();
      if (memberFlag) {
        for (final SimpleMapRefSetMember member : members) {
          contentService.addSimpleMapRefSetMember(member);
        }
      }
      contentService.commit();

      // If the record flag is set, add map records
      if (recordFlag) {

        // Get the distinct refsetIds involved
        Set<String> refSetIds = new HashSet<>();
        for (final SimpleMapRefSetMember member : members) {
          refSetIds.add(member.getRefSetId());
        }

        // For each refset id
        for (final String refSetId : refSetIds) {

          // Get a list of entries for that id
          int ct = 0;
          List<ComplexMapRefSetMember> complexMembers = new ArrayList<>();
          for (final SimpleMapRefSetMember member : members) {
            if (refSetIds.contains(member.getRefSetId())) {
              complexMembers.add(new ComplexMapRefSetMemberJpa(member));
              ct++;
            }
          }
          getLog().info("  Refset " + refSetId + " count = " + ct);

          // Then call the mapping service to create the map records
          WorkflowStatus status = WorkflowStatus.READY_FOR_PUBLICATION;
          // IF loading refSet members too, these are published already
          if (memberFlag) {
            status = WorkflowStatus.PUBLISHED;
          }
          mappingService.createMapRecordsForMapProject(
              mapProjectMap.get(refSetId).getId(), loaderUser, complexMembers,
              status);
        }
      }

      // clean-up
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Loading of Unpublished RF2 Complex Maps failed.", e);
    } finally {
      try {
        mappingService.close();
        contentService.close();
      } catch (Exception e) {
        // do nothing
      }
    }

  }

  /**
   * Load simple members from file and return as a list.
   *
   * @param file the file
   * @param mapProjectMap the map project map
   * @return the map
   * @throws Exception the exception
   */
  private List<SimpleMapRefSetMember> getSimpleMaps(File file,
    Map<String, MapProject> mapProjectMap) throws Exception {

    // Open reader and service
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    final ContentService contentService = new ContentServiceJpa();
    try {
      // Set up sets for any map records we encounter
      String line = null;
      final List<SimpleMapRefSetMember> members = new ArrayList<>();
      final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
      while ((line = reader.readLine()) != null) {
        line = line.replace("\r", "");
        String fields[] = line.split("\t");
        final SimpleMapRefSetMember member = new SimpleMapRefSetMemberJpa();

        // header
        if (!fields[0].equals("id")) {

          final String refsetId = fields[4];
          final String terminology =
              mapProjectMap.get(refsetId).getSourceTerminology();
          final String version =
              mapProjectMap.get(refsetId).getSourceTerminologyVersion();
          // Terminology attributes
          member.setTerminologyVersion(version);
          member.setTerminology(terminology);

          // SimpleMap attributes
          member.setTerminologyId(fields[0]);
          member.setEffectiveTime(dt.parse(fields[1]));
          member.setActive(fields[2].equals("1"));
          member.setModuleId(Long.valueOf(fields[3]));
          member.setRefSetId(refsetId);
          // set referenced component
          final Concept concept =
              contentService.getConcept(
                  fields[5], // referencedComponentId
                  mapProjectMap.get(refsetId).getSourceTerminology(),
                  mapProjectMap.get(refsetId).getSourceTerminologyVersion());
          if (concept != null) {
            member.setConcept(concept);
          } else {
            getLog().error(
                "member " + member.getTerminologyId()
                    + " references non-existent concept " + fields[5]);
            // TODO: this should throw an exception - commented out for testing
            // throw new IllegalStateException("member "
            // + member.getTerminologyId()
            // + " references non-existent concept " + fields[5]);
          }
          member.setMapTarget(fields[6]);

          members.add(member);
        }
      }
      return members;

    } catch (Exception e) {
      throw e;
    } finally {
      reader.close();
      contentService.close();
    }

  }
}