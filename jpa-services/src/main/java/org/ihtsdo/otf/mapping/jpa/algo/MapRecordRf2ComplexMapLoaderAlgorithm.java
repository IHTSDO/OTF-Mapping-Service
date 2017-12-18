package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

public class MapRecordRf2ComplexMapLoaderAlgorithm extends RootServiceJpa
    implements Algorithm, AutoCloseable {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /** The input file. */
  private String inputFile;

  /** The refset id. */
  private String refsetId;

  /** The members flag. */
  private boolean memberFlag = true;

  /** The records flag. */
  private boolean recordFlag = true;

  /** The workflow status to assign to created map records. */
  private String workflowStatus;

  /** The user name. */
  private String userName;

  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  public MapRecordRf2ComplexMapLoaderAlgorithm(String inputFile,
      Boolean memberFlag, Boolean recordFlag, String refsetId,
      String workflowStatus) throws Exception {
    super();
    this.inputFile = inputFile;
    this.memberFlag = memberFlag;
    this.recordFlag = recordFlag;
    this.refsetId = refsetId;
    this.workflowStatus = workflowStatus;
    
    //initialize logger
    String rootPath = ConfigUtility.getConfigProperties()
          .getProperty("map.principle.source.document.dir");
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }
    rootPath += "logs";
    File logDirectory = new File(rootPath);
    if (!logDirectory.exists()) {
        logDirectory.mkdir();
    }
    
    logFile = new File(logDirectory, "load_maps_" + refsetId + ".log");
    LoggerUtility.setConfiguration("load_maps", logFile.getAbsolutePath());
    this.log = LoggerUtility.getLogger("load_maps");
  }

  @Override
  public void compute() throws Exception {

    
    // clear log before starting process
    PrintWriter writer = new PrintWriter(logFile);
    writer.print("");
    writer.close(); 
    
    log.info("Starting loading complex map data");
    log.info("  inputFile      = " + inputFile);
    log.info("  workflowStatus = " + workflowStatus);
    log.info("  userName       = " + userName);
    log.info("  membersFlag    = " + memberFlag);
    log.info("  recordFlag     = " + recordFlag);
    log.info("  refsetId       = " + refsetId);

    // Set up map of refsetIds that we may encounter
    MappingService mappingService = null;
    ContentService contentService = null;

    try {

      // Check preconditions
      if (inputFile == null || !new File(inputFile).exists()) {
        throw new Exception("Specified input file missing");
      }

      if (workflowStatus == null
          || WorkflowStatus.valueOf(workflowStatus) == null) {
        throw new Exception(
            "Missing or invalid workflow status. Acceptable values are "
                + WorkflowStatus.values().toString());
      }

      if (userName == null) {
        log
            .info("No user specified, defaulting to user 'loader'");
      }

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
      log.info("  Map projects");
      for (final String refsetId : mapProjectMap.keySet()) {
        final MapProject project = mapProjectMap.get(refsetId);
        log.info("    project = " + project.getId()
            + "," + project.getRefSetId() + ", " + project.getName());

      }

      // if refsetId is specified, remove all rows that don't have that refsetId
      if (refsetId != null) {
        log
            .info("  Filtering the file by refsetId into "
                + System.getProperty("java.io.tmpdir"));

        // Open reader
        BufferedReader fileReader =
            new BufferedReader(new FileReader(inputFile));

        // Open writer
        File outputFile = File.createTempFile("ttt", ".filter",
            new File(System.getProperty("java.io.tmpdir")));
        FileWriter fw = new FileWriter(outputFile);
        BufferedWriter bw = new BufferedWriter(fw);

        String line = null;

        // Write each line where the refsetId matches the specified one into the
        // output file
        while ((line = fileReader.readLine()) != null) {
          Boolean keepLine = true;
          line = line.replace("\r", "");
          String fields[] = line.split("\t");

          if (!fields[4].equals(refsetId)) {
            keepLine = false;
          }

          // also take into account any additional project-specific validation,
          // if any
          ProjectSpecificAlgorithmHandler handler = mappingService
              .getProjectSpecificAlgorithmHandler(mapProjectMap.get(refsetId));

          if (!handler.isMapRecordLineValid(line)) {
            keepLine = false;
          }

          if (keepLine) {
            bw.write(line);
            bw.newLine();
          }
        }
        fileReader.close();
        bw.close();

        // overwrite the input file as this new temp file
        inputFile = outputFile.getAbsolutePath();

      }

      // sort input file
      log.info(
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
      log.info("  Done sorting the file ");

      // load complexMapRefSetMembers from extendedMap file
      final List<ComplexMapRefSetMember> members =
          getComplexMaps(outputFile, mapProjectMap);
      log.info("  members = " + members.size());

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
          log
              .info("  Refset " + refSetId + " count = " + ct);

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
      log.info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      log.info(e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        log.info(element.toString());
      }
      throw new Exception("Loading of RF2 Complex Maps failed.", e);
    } finally {
      try {
        mappingService.close();
        contentService.close();
      } catch (Exception e) {
        // do nothing
      }
    }
  }

  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  @Override
  public void reset() throws Exception {
    // n/a
  }

  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

  @Override
  public void cancel() throws Exception {
    requestCancel = true;
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
    contentService.close();
    complexMapReader.close();

    return members;
  }
}
