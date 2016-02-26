package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

// TODO: Auto-generated Javadoc
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

    try {

      if (inputFile == null || !new File(inputFile).exists()) {
        throw new MojoFailureException("Specified input file missing");
      }
      
      if (workflowStatus == null || WorkflowStatus.valueOf(workflowStatus) == null) {
        throw new MojoFailureException("Missing or invalid workflow status. Acceptable values are " + WorkflowStatus.values().toString());
      }
      
      if (userName == null) {
        getLog().info("No user specified, defaulting to user 'loader'");
      }

      // sort input file
      getLog().info(
          "  Sorting the file into " + System.getProperty("java.io.tmpdir"));
      File outputFile =
          File.createTempFile("ttt", ".sort",
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
                      i =
                          (fields1[0] + fields1[1] + fields1[2] + fields1[3])
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

      // Set up map of refsetIds that we may encounter
      MappingService mappingService = new MappingServiceJpa();

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

      Map<String, MapProject> mapProjectMap = new HashMap<>();
      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        mapProjectMap.put(project.getRefSetId(), project);
      }

      // load complexMapRefSetMembers from extendedMap file
      Map<String, List<ComplexMapRefSetMember>> complexMapRefSetMemberMap =
          loadExtendedMapRefSets(outputFile, mapProjectMap);

      // Call mapping service to create records as we go along
      for (String refsetId : complexMapRefSetMemberMap.keySet()) {
        mappingService.createMapRecordsForMapProject(mapProjectMap
            .get(refsetId).getId(), loaderUser, complexMapRefSetMemberMap
            .get(refsetId), WorkflowStatus.valueOf(workflowStatus));
      }

      // clean-up
      mappingService.close();
      // outputFile.delete();
      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Loading of Unpublished RF2 Complex Maps failed.", e);
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
  private static Map<String, List<ComplexMapRefSetMember>> loadExtendedMapRefSets(
    File complexMapFile, Map<String, MapProject> mapProjectMap)
    throws Exception {

    // Open reader and service
    BufferedReader complexMapReader =
        new BufferedReader(new FileReader(complexMapFile));
    ContentService contentService = new ContentServiceJpa();

    // Set up sets for any map records we encounter
    String line = null;
    Map<String, List<ComplexMapRefSetMember>> complexMapRefSetMemberMap =
        new HashMap<>();
    for (MapProject mapProject : mapProjectMap.values()) {
      complexMapRefSetMemberMap.put(mapProject.getRefSetId(),
          new ArrayList<ComplexMapRefSetMember>());
    }

    final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
    while ((line = complexMapReader.readLine()) != null) {
      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      ComplexMapRefSetMember complexMapRefSetMember =
          new ComplexMapRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        // ComplexMap attributes
        complexMapRefSetMember.setTerminologyId(fields[0]);
        complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        complexMapRefSetMember.setActive(fields[2].equals("1"));
        complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
        final String refsetId = fields[4];
        complexMapRefSetMember.setRefSetId(refsetId);
        complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
        complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
        complexMapRefSetMember.setMapRule(fields[8]);
        complexMapRefSetMember.setMapAdvice(fields[9]);
        complexMapRefSetMember.setMapTarget(fields[10]);
        complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[12]));

        // BLOCK is unused
        complexMapRefSetMember.setMapBlock(0); // default value
        complexMapRefSetMember.setMapBlockRule(null); // no default
        complexMapRefSetMember.setMapBlockAdvice(null); // no default

        // Terminology attributes
        complexMapRefSetMember.setTerminology(mapProjectMap.get(refsetId)
            .getSourceTerminology());
        complexMapRefSetMember.setTerminologyVersion(mapProjectMap
            .get(refsetId).getSourceTerminologyVersion());

        // set Concept
        Concept concept =
            contentService.getConcept(
                fields[5], // referencedComponentId
                mapProjectMap.get(refsetId).getSourceTerminology(),
                mapProjectMap.get(refsetId).getSourceTerminologyVersion());

        if (concept != null) {
          complexMapRefSetMember.setConcept(concept);
          // don't persist, non-published shouldn't be in the db
          complexMapRefSetMemberMap.get(refsetId).add(complexMapRefSetMember);
        } else {
          complexMapReader.close();
          throw new IllegalStateException("complexMapRefSetMember "
              + complexMapRefSetMember.getTerminologyId()
              + " references non-existent concept " + fields[5]);
        }
      }
    }
    complexMapReader.close();

    // Remove any map projects for which we did not encounter any records
    for (MapProject mapProject : mapProjectMap.values()) {
      if (complexMapRefSetMemberMap.get(mapProject.getRefSetId()).size() == 0) {
        complexMapRefSetMemberMap.remove(mapProject.getRefSetId());
      }
    }

    return complexMapRefSetMemberMap;
  }
}