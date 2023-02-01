/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
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
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

/**
 * The Class MapRecordRf2SimpleMapLoaderAlgorithm.
 */
public class MapRecordRf2SimpleMapLoaderAlgorithm extends RootServiceJpa
    implements Algorithm, AutoCloseable {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /** The input file. */
  private String inputFile;

  /** The members flag. */
  private boolean memberFlag = true;

  /** The records flag. */
  private boolean recordFlag = true;

  /** The refsetId. */
  private String refsetId;

  /** The workflow status to assign to created map records. */
  private String workflowStatus;

  /** The user name. */
  private String userName;

  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  /**
   * Instantiates a {@link MapRecordRf2SimpleMapLoaderAlgorithm} from the
   * specified parameters.
   *
   * @param inputFile the input file
   * @param memberFlag the member flag
   * @param recordFlag the record flag
   * @param refsetId the refset id
   * @param workflowStatus the workflow status
   * @throws Exception the exception
   */
  public MapRecordRf2SimpleMapLoaderAlgorithm(String inputFile,
      Boolean memberFlag, Boolean recordFlag, String refsetId,
      String workflowStatus) throws Exception {
    super();
    this.inputFile = inputFile;
    this.memberFlag = memberFlag;
    this.recordFlag = recordFlag;
    this.refsetId = refsetId;
    this.workflowStatus = workflowStatus;

    // initialize logger
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

  /* see superclass */
  @Override
  public void compute() throws Exception {

    // clear log before starting process
    PrintWriter writer = new PrintWriter(logFile);
    writer.print("");
    writer.close();

    log.info("Starting loading simple map data");
    log.info("  inputFile      = " + inputFile);
    log.info("  membersFlag    = " + memberFlag);
    log.info("  recordFlag     = " + recordFlag);
    log.info("  refsetId       = " + refsetId);
    log.info("  workflowStatus = " + workflowStatus);

    // Set up map of refsetIds that we may encounter
    MappingService mappingService = null;
    ContentService contentService = null;
    try {

      // Check preconditions
      if (inputFile == null || !new File(inputFile).exists()) {
        throw new Exception("Specified input file missing");
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
      final Map<String, MapProject> mapProjectMap = new HashMap<>();
      for (final MapProject project : mappingService.getMapProjects()
          .getIterable()) {
        mapProjectMap.put(project.getRefSetId(), project);
      }
      log.info("  Map projects");
      for (final String refsetId : mapProjectMap.keySet()) {
        final MapProject project = mapProjectMap.get(refsetId);
        log.info("    project = " + project.getId() + ","
            + project.getRefSetId() + ", " + project.getName());

      }

      // if refsetId is specified, remove all rows that don't have that refsetId
      if (refsetId != null) {
        log.info("  Filtering the file by refsetId into "
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

      // load complexMapRefSetMembers from simpleMap file
      final List<SimpleMapRefSetMember> members =
          getSimpleMaps(new File(inputFile), mapProjectMap);
      log.info("  members = " + members.size());

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
        final Set<String> refSetIds = new HashSet<>();
        for (final SimpleMapRefSetMember member : members) {
          refSetIds.add(member.getRefSetId());
        }

        // For each refset id
        for (final String refSetId : refSetIds) {

          // Get a list of entries for that id
          int ct = 0;
          final List<ComplexMapRefSetMember> complexMembers = new ArrayList<>();
          for (final SimpleMapRefSetMember member : members) {
            if (refSetIds.contains(member.getRefSetId())) {
              complexMembers.add(new ComplexMapRefSetMemberJpa(member));
              ct++;
            }
          }
          log.info("  records = " + ct);

          // Then call the mapping service to create the map records
          final WorkflowStatus status = WorkflowStatus.valueOf(workflowStatus);

          // IF loading refSet members too, these are published already
          mappingService.createMapRecordsForMapProject(
              mapProjectMap.get(refSetId).getId(), loaderUser, complexMembers,
              status);
        }
      }

      // clean-up
      log.info("Done loading simple map data");
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      throw new Exception("Loading of Unpublished RF2 Complex Maps failed.", e);
    } finally {
      try {
        mappingService.close();
        contentService.close();
      } catch (Exception e) {
        // do nothing
      }
    }
  }

  /* see superclass */
  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  /* see superclass */
  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void cancel() throws Exception {
    requestCancel = true;
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
          final MapProject mapProject = mapProjectMap.get(refsetId);
          if (mapProject == null) {
            throw new Exception("Unexpected refset id in line - " + line);
          }
          
          final String terminology = mapProject.getSourceTerminology();
          final String version = mapProject.getSourceTerminologyVersion();
          // Terminology attributes
          member.setTerminologyVersion(version);
          member.setTerminology(terminology);

          final int terminologyFieldId =
              (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap
                  && mapProject.getReverseMapPattern()) ? 5 : 6;
          final int targetFieldId =
              (mapProject.getMapRefsetPattern() != MapRefsetPattern.SimpleMap
                  && mapProject.getReverseMapPattern()) ? 6 : 5;

          // SimpleMap attributes
          member.setTerminologyId(fields[0]);
          member.setEffectiveTime(dt.parse(fields[1]));
          member.setActive(fields[2].equals("1"));
          member.setModuleId(Long.valueOf(fields[3]));
          member.setRefSetId(refsetId);
          // set referenced component
          final Concept concept = contentService.getConcept(fields[terminologyFieldId], // referencedComponentId
              mapProjectMap.get(refsetId).getSourceTerminology(),
              mapProjectMap.get(refsetId).getSourceTerminologyVersion());
          if (concept != null) {
            member.setConcept(concept);
          } else {
            log.error("member " + member.getTerminologyId()
                + " references non-existent concept " + fields[terminologyFieldId]);
            // TODO: this should throw an exception - commented out for testing
            // throw new IllegalStateException("member "
            // + member.getTerminologyId()
            // + " references non-existent concept " + fields[terminologyFieldId]);
          }
          // If blank last field
          if (fields.length == 6) {
            member.setMapTarget("");
          } else {
            member.setMapTarget(fields[targetFieldId]);
          }

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
