package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * Goal which imports project data from text files.
 * 
 * See admin/import/pom.xml for a sample execution.
 * 
 * NOTE: run with -Dmini if using a mini data set as all scope concepts may not
 * exist
 * 
 * @goal import-project-data
 * 
 * @phase package
 */
public class MapProjectDataImportMojo extends AbstractMojo {

  /**
   * The input dir
   * @parameter
   * @required
   */
  private String inputDir;

  /**
   * The mini flag
   * @parameter
   */
  private String mini;

  /**
   * Instantiates a {@link MapProjectDataImportMojo} from the specified
   * parameters.
   * 
   */
  public MapProjectDataImportMojo() {
    // Do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @SuppressWarnings("resource")
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting importing metadata ...");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  mini = " + mini);
    try {

      File inputDirFile = new File(inputDir);
      if (!inputDirFile.exists()) {
        throw new MojoFailureException("Specified input dir does not exist");
      }

      // get all project .xml files
      FilenameFilter projectFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          String lowercaseName = name.toLowerCase();
          if (lowercaseName.endsWith(".xml")
              && lowercaseName.startsWith("project")) {
            return true;
          } else {
            return false;
          }
        }
      };
      File[] projectFiles = inputDirFile.listFiles(projectFilter);

      // Open services
      ReportService reportService = new ReportServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      JAXBContext context = JAXBContext.newInstance(MapProjectJpa.class);
      Unmarshaller unmarshaller = context.createUnmarshaller();

      // read project .xml files one at a time
      for (File projectFile : projectFiles) {
        getLog().info("  Handling project file " + projectFile);

        // Unmarshal project from XML
        MapProject project =
            (MapProjectJpa) unmarshaller.unmarshal(new StreamSource(
                new StringReader(new Scanner(projectFile, "UTF-8")
                    .useDelimiter("\\A").next())));

        // Add advices if they do not already exist
        Set<MapAdvice> advices = project.getMapAdvices();
        List<MapAdvice> currentAdvices =
            mappingService.getMapAdvices().getMapAdvices();
        for (MapAdvice advice : advices) {
          if (!currentAdvices.contains(advice)) {
            getLog().info("  Adding advice " + advice.getName());
            advice.setId(null);
            mappingService.addMapAdvice(advice);
          } else {
            getLog().info("  Advice already exists " + advice.getName());
          }
        }

        // Add principles if they do not already exist
        Set<MapPrinciple> principles = project.getMapPrinciples();
        List<MapPrinciple> currentPrinciples =
            mappingService.getMapPrinciples().getMapPrinciples();
        for (MapPrinciple principle : principles) {
          if (!currentPrinciples.contains(principle)) {
            getLog().info("  Adding principle " + principle.getName());
            principle.setId(null);
            mappingService.addMapPrinciple(principle);
          } else {
            getLog().info("  Principle already exists " + principle.getName());
          }
        }

        // Add relations if they do not already exist
        Set<MapRelation> relations = project.getMapRelations();
        List<MapRelation> currentRelations =
            mappingService.getMapRelations().getMapRelations();
        for (MapRelation relation : relations) {
          if (!currentRelations.contains(relation)) {
            getLog().info("  Adding relation " + relation.getName());
            relation.setId(null);
            mappingService.addMapRelation(relation);
          } else {
            getLog().info("  Relation already exists " + relation.getName());
          }
        }

        // Add age ranges if they do not already exist
        Set<MapAgeRange> ageRanges = project.getPresetAgeRanges();
        List<MapAgeRange> currentAgeRanges =
            mappingService.getMapAgeRanges().getMapAgeRanges();
        for (MapAgeRange ageRange : ageRanges) {
          if (!currentAgeRanges.contains(ageRange)) {
            getLog().info("  Adding age range " + ageRange.getName());
            ageRange.setId(null);
            mappingService.addMapAgeRange(ageRange);
          } else {
            getLog().info("  Age range already exists " + ageRange.getName());
          }
        }

        // Add users
        Set<MapUser> leads = project.getMapLeads();
        Set<MapUser> specialists = project.getMapSpecialists();

        List<MapUser> currentUsers = mappingService.getMapUsers().getMapUsers();
        for (MapUser user : leads) {
          if (!currentUsers.contains(user)) {
            getLog().info("  Adding user " + user.getName());
            user.setId(null);
            mappingService.addMapUser(user);
            currentUsers.add(user);
          } else {
            getLog().info("  User already exists " + user.getName());
          }
        }
        for (MapUser user : specialists) {
          if (!currentUsers.contains(user)) {
            getLog().info("  Adding user " + user.getName());
            user.setId(null);
            mappingService.addMapUser(user);
            currentUsers.add(user);
          } else {
            getLog().info("  User already exists " + user.getName());
          }
        }

        // Add report definitions if they don't already exist
        Set<ReportDefinition> reportDefinitions =
            project.getReportDefinitions();
        List<ReportDefinition> currentReportDefinitions =
            reportService.getReportDefinitions().getReportDefinitions();
        currentReportDefinitions.addAll(reportService.getQACheckDefinitions()
            .getReportDefinitions());
        for (ReportDefinition reportDefinition : reportDefinitions) {
          if (!currentReportDefinitions.contains(reportDefinition)) {
            getLog().info(
                "  Adding report definition " + reportDefinition.getName());
            reportDefinition.setId(null);
            reportService.addReportDefinition(reportDefinition);
          } else {
            getLog().info(
                "  Report definition already exists "
                    + reportDefinition.getName());
          }
        }

        // copy project
        MapProject bareProject = new MapProjectJpa(project);

        // clear copied project of all collections
        bareProject.setMapSpecialists(new HashSet<MapUser>());
        bareProject.setMapLeads(new HashSet<MapUser>());

        bareProject.setMapAdvices(new HashSet<MapAdvice>());
        bareProject.setMapPrinciples(new HashSet<MapPrinciple>());
        bareProject.setMapRelations(new HashSet<MapRelation>());
        bareProject.setPresetAgeRanges(new HashSet<MapAgeRange>());

        bareProject.setReportDefinitions(new HashSet<ReportDefinition>());
        bareProject.setScopeConcepts(new HashSet<String>());
        bareProject.setScopeExcludedConcepts(new HashSet<String>());
        bareProject.setTeamBased(false);
        bareProject.setId(null);

        // add the blank project (because cascade is not used)
        getLog().info("  Add Project: " + bareProject.getName());
        mappingService.addMapProject(bareProject);

        // attach specialists
        getLog().info("    Attach specialists");
        for (MapUser specialist : project.getMapSpecialists()) {
          MapUser user = mappingService.getMapUser(specialist.getUserName());
          bareProject.addMapSpecialist(user);
        }
        getLog()
            .info("      count = " + bareProject.getMapSpecialists().size());
        mappingService.updateMapProject(bareProject);

        // attach leads
        getLog().info("    Attach leads");
        for (MapUser lead : project.getMapLeads()) {
          MapUser user = mappingService.getMapUser(lead.getUserName());
          bareProject.addMapLead(user);
        }
        getLog().info("      count = " + bareProject.getMapLeads().size());
        mappingService.updateMapProject(bareProject);

        // attach advices
        getLog().info("    Attach advices");
        for (MapAdvice advice : project.getMapAdvices()) {
          for (MapAdvice avc : mappingService.getMapAdvices().getMapAdvices()) {
            if (avc.equals(advice)) {
              bareProject.addMapAdvice(avc);
            }
          }
        }
        getLog().info("      count = " + bareProject.getMapAdvices().size());
        mappingService.updateMapProject(bareProject);

        // attach principles
        getLog().info("    Attach principles");
        for (MapPrinciple principle : project.getMapPrinciples()) {
          for (MapPrinciple pcpl : mappingService.getMapPrinciples()
              .getMapPrinciples()) {
            if (pcpl.equals(principle)) {
              bareProject.addMapPrinciple(pcpl);
            }
          }
        }
        getLog().info("      count = " + bareProject.getMapPrinciples().size());
        mappingService.updateMapProject(bareProject);

        // attach relations
        getLog().info("    Attach relations");
        for (MapRelation relation : project.getMapRelations()) {
          for (MapRelation rel : mappingService.getMapRelations()
              .getMapRelations()) {
            if (rel.equals(relation)) {
              bareProject.addMapRelation(rel);
            }
          }
        }
        getLog().info("      count = " + bareProject.getMapRelations().size());
        mappingService.updateMapProject(bareProject);

        // attach age ranges
        getLog().info("    Attach age ranges");
        for (MapAgeRange ageRange : project.getPresetAgeRanges()) {
          for (MapAgeRange ar : mappingService.getMapAgeRanges()
              .getMapAgeRanges()) {
            if (ar.equals(ageRange)) {
              bareProject.addPresetAgeRange(ar);
            }
          }
        }
        getLog().info(
            "      count = " + bareProject.getPresetAgeRanges().size());
        mappingService.updateMapProject(bareProject);

        // attach report definitions
        getLog().info("    Attach report definitions");
        for (ReportDefinition reportDefinition : project.getReportDefinitions()) {
          for (ReportDefinition rd : reportService.getReportDefinitions()
              .getReportDefinitions()) {
            if (rd.equals(reportDefinition)) {
              bareProject.addReportDefinition(rd);
            }
          }
        }
        getLog().info(
            "      count = " + bareProject.getPresetAgeRanges().size());
        mappingService.updateMapProject(bareProject);

        // attach scope concepts
        getLog().info("    Attach scope concepts");
        BufferedReader scopeIncludesReader =
            new BufferedReader(new FileReader(new File(projectFile
                .getAbsolutePath().replace(".xml", "Scope.txt"))));
        String line = null;
        Set<String> conceptsInScope = new HashSet<>();
        while ((line = scopeIncludesReader.readLine()) != null) {
          Concept c =
              contentService.getConcept(line.trim(),
                  project.getSourceTerminology(),
                  project.getSourceTerminologyVersion());
          if (c == null && "true".equals(mini)) {
            // if it's mini, just ignore this
          } else if (c == null) {
            // if it's not mini, then throw an error
            throw new Exception("Scope concept + " + line.trim()
                + " cannot be found.");
          } else {
            conceptsInScope.add(line.trim());
          }
        }

        // set the map project scope concepts and update the project
        bareProject.setScopeConcepts(conceptsInScope);
        getLog().info("      count = " + conceptsInScope.size());
        mappingService.updateMapProject(bareProject);
        scopeIncludesReader.close();

        // attach scope excluded concepts
        getLog().info("    Attach scope excluded concepts");
        BufferedReader scopeExcludesReader =
            new BufferedReader(new FileReader(new File(projectFile
                .getAbsolutePath().replace(".xml", "ScopeExcludes.txt"))));
        Set<String> conceptsExcludedScope = new HashSet<>();
        while ((line = scopeExcludesReader.readLine()) != null) {
          Concept c =
              contentService.getConcept(line.trim(),
                  project.getSourceTerminology(),
                  project.getSourceTerminologyVersion());
          if (c == null && "true".equals(mini)) {
            // if it's mini, just ignore this
          } else if (c == null) {
            // if it's not mini, then throw an error
            throw new Exception("Scope excluded concept + " + line.trim()
                + " cannot be found.");
          } else {
            conceptsExcludedScope.add(line.trim());
          }
        }

        // set the map project scope concepts and update the project
        bareProject.setScopeExcludedConcepts(conceptsExcludedScope);
        getLog().info("      count = " + conceptsExcludedScope.size());
        mappingService.updateMapProject(bareProject);
        scopeExcludesReader.close();
      }

      getLog().info("done ...");
      mappingService.close();
      contentService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
