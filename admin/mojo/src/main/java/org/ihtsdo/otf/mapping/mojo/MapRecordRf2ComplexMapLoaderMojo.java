package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-complex-map
 * @phase package
 */
public class MapRecordRf2ComplexMapLoaderMojo extends AbstractMojo {

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
  private boolean memberFlag = true;

  /**
   * The records flag.
   * @parameter
   * @required
   */
  private boolean recordFlag = true;

  /**
   * The workflow status to assign to created map records.
   *
   * @parameter
   * @required
   */
  private String workflowStatus;

  /**
   * The user name.
   * 
   * @parameter
   */
  private String userName;

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading complex map data");
    getLog().info("  inputFile = " + inputFile);
    getLog().info("  workflowStatus = " + workflowStatus);
    getLog().info("  userName = " + userName);
    getLog().info("  membersFlag = " + memberFlag);
    getLog().info("  recordFile = " + recordFlag);

    // Set up map of refsetIds that we may encounter
    MappingService mappingService = null;
    ContentService contentService = null;

    try {

      // Check preconditions
      if (inputFile == null || !new File(inputFile).exists()) {
        throw new MojoFailureException("Specified input file missing");
      }

      if (workflowStatus == null
          || WorkflowStatus.valueOf(workflowStatus) == null) {
        throw new MojoFailureException(
            "Missing or invalid workflow status. Acceptable values are "
                + WorkflowStatus.values().toString());
      }

      if (userName == null) {
        getLog().info("No user specified, defaulting to user 'loader'");
      }

      // sort input file
      getLog().info(
          "  Sorting the file into " + System.getProperty("java.io.tmpdir"));
      File outputFile = File.createTempFile("ttt", ".sort",
          new File(System.getProperty("java.io.tmpdir")));
      outputFile.delete();
      // Sort file according to unix sort
      // -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11
      // -k 12,12 -k 13,13
      FileSorter.sortFile(inputFile, outputFile.getPath(),
          new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
              String[] fields1 = o1.split("\t");
              String[] fields2 = o2.split("\t");

              // keep headers at top
              if (o1.startsWith("id")) {
                return 1;
              }

              long i = fields1[4].compareTo(fields2[4]);
              if (i != 0) {
                return (int) i;
              } else {
                i = fields1[5].compareTo(fields2[5]);
                // i = (Long.parseLong(fields1[5]) -
                // Long.parseLong(fields2[5]));
                if (i != 0) {
                  return (int) i;
                } else {
                  i = Long.parseLong(fields1[6]) - Long.parseLong(fields2[6]);
                  if (i != 0) {
                    return (int) i;
                  } else {
                    i = Long.parseLong(fields1[7]) - Long.parseLong(fields2[7]);
                    if (i != 0) {
                      return (int) i;
                    } else {
                      i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                          .compareTo(fields1[0] + fields1[1] + fields1[2]
                              + fields1[3]);
                      if (i != 0) {
                        return (int) i;
                      } else {
                        i = fields1[8].compareTo(fields2[8]);
                        if (i != 0) {
                          return (int) i;
                        } else {
                          i = fields1[9].compareTo(fields2[9]);
                          if (i != 0) {
                            return (int) i;
                          } else {
                            i = fields1[10].compareTo(fields2[10]);
                            if (i != 0) {
                              return (int) i;
                            } else {
                              i = fields1[11].compareTo(fields2[11]);
                              if (i != 0) {
                                return (int) i;
                              } else {

                                // complex maps do not have mapCategory field
                                if (fields1.length == 12) {
                                  return 0;
                                }

                                // extended maps have extra mapCategory field
                                i = fields1[12].compareTo(fields2[12]);
                                if (i != 0) {
                                  return (int) i;
                                } else {
                                  return 0;
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          });
      getLog().info("  Done sorting the file ");

      // Instantiate services
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

      final Map<String, MapProject> mapProjectMap = new HashMap<>();
      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        mapProjectMap.put(project.getRefSetId(), project);
      }
      getLog().info("  Map projects");
      for (final String refsetId : mapProjectMap.keySet()) {
        final MapProject project = mapProjectMap.get(refsetId);
        getLog().info("    project = " + project.getId() + ","
            + project.getRefSetId() + ", " + project.getName());

      }
      // load complexMapRefSetMembers from extendedMap file
      final List<ComplexMapRefSetMember> members =
          getComplexMaps(outputFile, mapProjectMap);
      getLog().info("  members = " + members.size());

      // If the member flag is set, insert all of these
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();
      if (memberFlag) {
        for (final ComplexMapRefSetMember member : members) {
          contentService.addComplexMapRefSetMember(member);
        }
      }
      contentService.commit();

      if (recordFlag) {
        // Get the distinct refsetIds involved
        Set<String> refSetIds = new HashSet<>();
        for (final ComplexMapRefSetMember member : members) {
          refSetIds.add(member.getRefSetId());
        }

        // For each refset id
        for (final String refSetId : refSetIds) {

          // Get a list of entries for that id
          int ct = 0;
          final List<ComplexMapRefSetMember> complexMembers = new ArrayList<>();
          for (final ComplexMapRefSetMember member : members) {
            if (refSetIds.contains(member.getRefSetId())) {
              complexMembers.add(member);
              ct++;
            }
          }
          getLog().info("  Refset " + refSetId + " count = " + ct);

          // Then call the mapping service to create the map records
          WorkflowStatus status = WorkflowStatus.valueOf(workflowStatus);

          // IF loading refSet members too, these are published already
          mappingService.createMapRecordsForMapProject(
              mapProjectMap.get(refSetId).getId(), loaderUser, complexMembers,
              status);
        }
      }

      // clean-up
      mappingService.close();
      // outputFile.delete();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Loading of RF2 Complex Maps failed.",
          e);
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
   * Load extended map ref sets from the file.
   *
   * @param complexMapFile the complex map file
   * @param mapProjectMap the map project map
   * @return the map
   * @throws Exception the exception
   */
  private static List<ComplexMapRefSetMember> getComplexMaps(
    File complexMapFile, Map<String, MapProject> mapProjectMap)
    throws Exception {

    // Open reader and service
    BufferedReader complexMapReader =
        new BufferedReader(new FileReader(complexMapFile));
    ContentService contentService = new ContentServiceJpa();

    // Set up sets for any map records we encounter
    String line = null;
    List<ComplexMapRefSetMember> members = new ArrayList<>();

    final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
    while ((line = complexMapReader.readLine()) != null) {
      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      ComplexMapRefSetMember member = new ComplexMapRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        // ComplexMap attributes
        member.setTerminologyId(fields[0]);
        member.setEffectiveTime(dt.parse(fields[1]));
        member.setActive(fields[2].equals("1"));
        member.setModuleId(Long.valueOf(fields[3]));
        final String refsetId = fields[4];
        member.setRefSetId(refsetId);
        member.setMapGroup(Integer.parseInt(fields[6]));
        member.setMapPriority(Integer.parseInt(fields[7]));
        member.setMapRule(fields[8]);
        member.setMapAdvice(fields[9]);
        member.setMapTarget(fields[10]);

        // handle complex vs. extended maps -- extended maps have mapCategory as
        // well as correlationId
        member.setMapRelationId(
            Long.valueOf(fields[fields.length == 13 ? 12 : 11]));

        // BLOCK is unused
        member.setMapBlock(0); // default value
        member.setMapBlockRule(null); // no default
        member.setMapBlockAdvice(null); // no default

        // Terminology attributes
        member
            .setTerminology(mapProjectMap.get(refsetId).getSourceTerminology());
        member.setTerminologyVersion(
            mapProjectMap.get(refsetId).getSourceTerminologyVersion());

        // set Concept
        Concept concept = contentService.getConcept(fields[5], // referencedComponentId
            mapProjectMap.get(refsetId).getSourceTerminology(),
            mapProjectMap.get(refsetId).getSourceTerminologyVersion());

        if (concept != null) {
          member.setConcept(concept);
          // don't persist, non-published shouldn't be in the db
          members.add(member);
        } else {
          complexMapReader.close();
          throw new IllegalStateException("member " + member.getTerminologyId()
              + " references non-existent concept " + fields[5]);
        }
      }
    }
    complexMapReader.close();

    return members;
  }
}