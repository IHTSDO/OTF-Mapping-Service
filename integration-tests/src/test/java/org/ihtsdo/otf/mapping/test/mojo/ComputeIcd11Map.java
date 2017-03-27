/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.test.mojo;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A mechanism to reset to the stock demo database.
 */
public class ComputeIcd11Map {

  /** The properties. */
  static Properties config;

  /** The server. */
  static String server = "false";

  /** The security service. */
  SecurityService securityService = null;

  /** The content service. */
  ContentServiceJpa contentService = null;

  /** The mapping service. */
  MappingService mappingService = null;

  /** The workflow service. */
  WorkflowService workflowService = null;

  /** The report service. */
  ReportService reportService = null;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    config = ConfigUtility.getConfigProperties();
  }

  /**
   * Compute ICD11 Map.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void test() throws Exception {
    Logger.getLogger(getClass()).info("Starting compute ICD11");
    if (System.getProperty("icd11.dir") == null) {
      throw new Exception("System must specify a icd11.dir property");
    }
    final String icd11Dir = System.getProperty("icd11.dir");
    try {
      securityService = new SecurityServiceJpa();
      contentService = new ContentServiceJpa();
      mappingService = new MappingServiceJpa();
      workflowService = new WorkflowServiceJpa();
      reportService = new ReportServiceJpa();
      final int sampleCt = 5;
      //
      // Load all data resources
      //

      //
      // Cache ICD11 concepts (by id) - noheader
      //
      Logger.getLogger(getClass()).info(" Load ICD11 concepts");
      List<String> lines =
          FileUtils.readLines(new File(icd11Dir, "icd11Concepts.txt"), "UTF-8");
      final Map<String, String> icd11Concepts = new HashMap<>();
      int ct = 0;
      int skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\|");
        final String code = fields[0]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        icd11Concepts.put(code, fields[1]);
        if (ct < sampleCt) {
          Logger.getLogger(getClass()).debug(code + " => " + fields[1]);
        }

        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD11 scope (by id) - noheader
      //
      Logger.getLogger(getClass()).info(" Load ICD11 scope");
      lines =
          FileUtils.readLines(new File(icd11Dir, "icd11Scope.txt"), "UTF-8");
      final Set<String> icd11Scope = new HashSet<>();
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {

        final String code =
            line.replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        icd11Scope.add(code);

        if (ct < sampleCt) {
          Logger.getLogger(getClass()).debug(code);
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10 concepts (by code) - noheader (from DB)
      //
      Logger.getLogger(getClass()).info(" Load ICD10 concepts");
      final javax.persistence.Query query =
          contentService.getEntityManager().createQuery(
              "select c from ConceptJpa c where terminology = :terminology");
      query.setParameter("terminology", "ICD10");
      final List<Concept> concepts = query.getResultList();
      final Map<String, String> icd10Concepts = new HashMap<>();
      ct = 0;
      skipCt = 0;
      for (final Concept concept : concepts) {
        icd10Concepts.put(concept.getTerminologyId(),
            concept.getDefaultPreferredName());

        if (ct < sampleCt) {
          Logger.getLogger(getClass()).debug(concept.getTerminology() + " => "
              + concept.getDefaultPreferredName());
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10 scope (by code) - noheader
      //
      Logger.getLogger(getClass()).info(" Load ICD10 scope");
      lines =
          FileUtils.readLines(new File(icd11Dir, "icd10Scope.txt"), "UTF-8");
      final Set<String> icd10Scope = new HashSet<>();
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        icd10Scope.add(line);
        if (ct < sampleCt) {
          Logger.getLogger(getClass()).debug(line);
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      // Cache ICD10 map (by SCTID)
      Logger.getLogger(getClass()).info(" Load ICD10 map");
      lines = FileUtils.readLines(new File(icd11Dir, "icd10Map.txt"), "UTF-8");
      final Map<String, List<IcdMap>> icd10Map = new HashMap<>();
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        // id effectiveTime active moduleId refsetId referencedComponentId
        // mapGroup mapPriority mapRule mapAdvice mapTarget correlationId
        // mapCategoryId

        // Skip header (no skipCt++)
        if (fields[0].equals("id")) {
          // skipCt++;
          continue;
        }
        // Create a map entry - assumes the file is ordered
        if (!icd10Map.containsKey(fields[5])) {
          icd10Map.put(fields[5], new ArrayList<IcdMap>());
        }
        icd10Map.get(fields[5]).add(new IcdMap(line));
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(fields[5] + " => " + icd10Map.get(fields[5]));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache SCT scope - noheader
      //
      Logger.getLogger(getClass()).info(" Load SCT scope");
      lines = FileUtils.readLines(new File(icd11Dir, "sctScope.txt"), "UTF-8");
      final List<String> sctScope = new ArrayList<>();
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        sctScope.add(line);
        if (ct < sampleCt) {
          Logger.getLogger(getClass()).debug(line);
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache SCT ancDesc and descAnc - noheader
      //
      Logger.getLogger(getClass()).info(" Load SCT anc/desc (for scope)");
      lines = FileUtils.readLines(new File(icd11Dir, "sctScopeAncDesc.txt"),
          "UTF-8");
      final Map<String, Set<Depth>> sctAncDesc = new HashMap<>();
      final Map<String, Set<Depth>> sctDescAnc = new HashMap<>();
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\|");
        // skip if if self-row
        if (fields[2].equals("0")) {
          skipCt = 0;
          // no need to log this, it is very common and verified
          continue;
        }
        if (!sctAncDesc.containsKey(fields[0])) {
          sctAncDesc.put(fields[0], new HashSet<Depth>());
        }
        sctAncDesc.get(fields[0]).add(new Depth(line, true));

        if (!sctDescAnc.containsKey(fields[1])) {
          sctDescAnc.put(fields[1], new HashSet<Depth>());
        }
        sctDescAnc.get(fields[1]).add(new Depth(line, false));
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(fields[0] + " => DESC " + sctAncDesc.get(fields[0]));
          Logger.getLogger(getClass())
              .debug(fields[1] + " => ANC " + sctDescAnc.get(fields[1]));
        }

        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache the SCT->ICD10 rollup (these are code suggestions based on
      // descendants) - noheader
      // 102594003|R94.3|105|
      // e.g. 102594003 has 105 descendants mapped to R94.3
      // Tricky because it includes the secondary code stuff too.
      //
      Logger.getLogger(getClass()).info(" Load SCT-ICD10 rollup");
      final Map<String, Set<Depth>> sctIcd10Rollup = new HashMap<>();
      lines =
          FileUtils.readLines(new File(icd11Dir, "sctIcdRollup.txt"), "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\|");
        if (!sctIcd10Rollup.containsKey(fields[0])) {
          sctIcd10Rollup.put(fields[0], new HashSet<Depth>());
        }
        sctIcd10Rollup.get(fields[0]).add(new Depth(line, true));
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(fields[0] + " => " + sctIcd10Rollup.get(fields[0]));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10-11 equivalence map(s) - david robinson's file - HEADER
      // icd11 entity id|sctid|In MMS|matchType|
      // * A = match to added concept ,
      // * R = residual match to existing concept
      // * N = ‘normal' match to existing concept
      // 1493554134 91061000119100 Y N
      //
      Logger.getLogger(getClass())
          .info(" Load SCT-ICD11 equivalence (from DR)");
      final Map<String, String> sctIcd11Equivalence = new HashMap<>();
      final Map<String, String> sctIcd11EquivalenceType = new HashMap<>();
      lines = FileUtils.readLines(new File(icd11Dir, "sctIcd11Equivalence.txt"),
          "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        // Skip header (no skipCt++)
        if (fields[2].equals("mms")) {
          // skipCt++;
          // Logger.getLogger(getClass()).debug("SKIP: " + line);
          continue;
        }
        // // Skip if "MMS" flag != Y
        // if (!fields[2].equals("Y")) {
        // skipCt++;
        // Logger.getLogger(getClass()).debug("SKIP: " + line);
        // continue;
        // }
        final String code = fields[0]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        sctIcd11Equivalence.put(fields[1], code);
        sctIcd11EquivalenceType.put(fields[1], fields[3]);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(fields[1] + " => " + sctIcd11Equivalence.get(fields[1])
                  + ", " + sctIcd11EquivalenceType.get(fields[1]));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10-11 matches - noheader
      // 10065003|545442245/morbidity/other|12.5|Excoriated acne|Excoriated acne
      //
      Logger.getLogger(getClass()).info(" Load SCT-ICD11 lexical matches");
      final Map<String, List<Score>> sctIcd11Lexical = new HashMap<>();
      lines = FileUtils.readLines(new File(icd11Dir, "sctIcd11Matches.txt"),
          "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\|");
        final Score score = new Score(line);
        final String code = fields[0]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        if (!sctIcd11Lexical.containsKey(fields[0])) {
          sctIcd11Lexical.put(code, new ArrayList<Score>());
        }
        sctIcd11Lexical.get(code).add(score);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(code + " => " + sctIcd11Lexical.get(code));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10-11 mappings (provided in frozen release) - HEADER
      //
      Logger.getLogger(getClass()).info(" Load ICD10-11 mappings");
      final Map<String, Set<WhoMap>> icd10To11 = new HashMap<>();
      // 10To11MapToOneCategory.txt
      lines = FileUtils
          .readLines(new File(icd11Dir, "10To11MapToOneCategory.txt"), "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        // Skip header (no skipCt++
        if (fields[0].equals("10ClassKind")) {
          // skipCt++;
          Logger.getLogger(getClass()).debug("SKIP: " + line);
          continue;
        }
        // Skip if "No Mapping"
        if (fields[4].equals("No Mapping")) {
          skipCt++;
          // Logger.getLogger(getClass()).debug("SKIP (nomap): " + line);
          continue;
        }
        final WhoMap map = new WhoMap(line);
        // Skip cases where the code or target code are not in scope
        if (!icd10Scope.contains(map.getCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getCode() + "): " + line);
          continue;
        }
        if (!icd11Scope.contains(map.getTargetCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getTargetCode() + "): " + line);
          continue;
        }
        final String code = fields[0]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        if (!icd10To11.containsKey(code)) {
          icd10To11.put(code, new HashSet<WhoMap>());
        }
        icd10To11.get(code).add(map);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(code + " => " + icd10To11.get(code));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // 10To11MapToMultipleCategories.txt - HEADER
      //
      lines = FileUtils.readLines(
          new File(icd11Dir, "10To11MapToMultipleCategories.txt"), "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        // Skip header (no skipCt++
        if (fields[0].equals("10ClassKind")) {
          // skipCt++;
          continue;
        } // Skip if "No Mapping"
        if (fields[4].equals("No Mapping")) {
          skipCt++;
          // Logger.getLogger(getClass()).debug("SKIP (nomap): " + line);
          continue;
        }
        final WhoMap map = new WhoMap(line);
        // Skip cases where the code or target code are not in scope
        if (!icd10Scope.contains(map.getCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getCode() + "): " + line);
          continue;
        }
        if (!icd11Scope.contains(map.getTargetCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getTargetCode() + "): " + line);
          continue;
        }
        final String code = fields[0]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        if (!icd10To11.containsKey(code)) {
          icd10To11.put(code, new HashSet<WhoMap>());
        }
        icd10To11.get(code).add(map);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(code + " => " + icd10To11.get(code));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // Cache ICD10-11 mappings (provided in frozen release) - HEADER
      //
      Logger.getLogger(getClass()).info(" Load ICD11-ICD10 mappings");
      final Map<String, Set<WhoMap>> icd11To10Reverse = new HashMap<>();
      // 11To10MapToOneCategory.txt
      lines = FileUtils
          .readLines(new File(icd11Dir, "11To10MapToOneCategory.txt"), "UTF-8");
      ct = 0;
      skipCt = 0;
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        // Skip header (no skipCt++
        if (fields[0].equals("icd11Id")) {
          // skipCt++;
          continue;
        }
        // Skip if "No Mapping"
        if (fields[3].equals("No Mapping")) {
          skipCt++;
          // Logger.getLogger(getClass()).debug("SKIP (nomap): " + line);
          continue;
        }

        final WhoMap map = new WhoMap(line);
        // Skip cases where the code or target code are not in scope
        if (!icd11Scope.contains(map.getCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getCode() + "): " + line);
          continue;
        }
        if (!icd10Scope.contains(map.getTargetCode())) {
          skipCt++;
          Logger.getLogger(getClass())
              .debug("SKIP (scope " + map.getTargetCode() + "): " + line);
          continue;
        }
        final String code = fields[3]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        if (!icd11To10Reverse.containsKey(code)) {
          icd11To10Reverse.put(code, new HashSet<WhoMap>());
        }
        icd11To10Reverse.get(code).add(map);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(code + " => " + icd11To10Reverse.get(code));
        }
        ct++;
      }
      Logger.getLogger(getClass()).info("   ct = " + ct);
      Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

      //
      // PROCESSING
      //

      // Tracking vars
      final String refsetId = "icd11RefsetId";
      final String moduleId = "icd11ModuleId";
      final Map<String, List<IcdMap>> icd11Map = new HashMap<>();
      final Map<String, String> icd11MapNotes = new HashMap<>();

      //
      // Iterate through SNOMED concepts (in reverse max depth order)
      //
      Collections.sort(sctScope, new Comparator<String>() {
        @Override
        public int compare(String c1, String c2) {
          return getMaxDepth(c2, sctAncDesc) - getMaxDepth(c1, sctAncDesc);
        }
      });

      Logger.getLogger(getClass()).info("   SORT (top) = " + sctScope.get(0));
      Logger.getLogger(getClass()).info("   SORT (top) = " + sctScope.get(1));
      Logger.getLogger(getClass())
          .info("   SORT (leaf) = " + sctScope.get(sctScope.size() - 2));
      Logger.getLogger(getClass())
          .info("   SORT (leaf) = " + sctScope.get(sctScope.size() - 1));

      // Write maps
      final PrintWriter mapOut =
          new PrintWriter(new FileWriter(new File(icd11Dir, "icd11Map.txt")));
      final PrintWriter notesOut = new PrintWriter(
          new FileWriter(new File(icd11Dir, "icd11MapNotes.txt")));

      // Go through SNOMED concepts now in depth-first order
      // This is so that subsequent lookups can determine the mapping of a
      // parent
      for (final String sctid : sctScope) {

        final List<IcdMap> maps = new ArrayList<>();
        final Map<String, Score> candidates = new HashMap<>();
        final Map<String, List<String>> ruleDetails = new HashMap<>();

        // for each ICD10 map, consider an ICD11 map.
        for (final IcdMap map10 : icd10Map.get(sctid)) {

          // Initialize map
          final IcdMap map11 = new IcdMap(map10);

          // Get the ICD10 code
          final String icd10Code = map10.getMapTarget();

          // RULE1 - David Robinson's map
          // If there is an entry, this gets a score of 5, and we ignore the
          // other
          // rules

          // RULE2 - the 10-11 equivlanece map
          // Equivalent (highest) - 100%
          // Subclass (higher) - 90%
          // Relation (medium) - 80%
          // stated = 100%

          // RULE3 - Lexical Match
          // Score > 12 - 1.25
          // Score > 10 - 1.00
          // else = score/ 7.0

          // RULE4 - Matching parent map
          // boost for each parent with a matching code, max boost of .5 (e.g.
          // .5
          // * matching parents / number parents)

          // Write maps file, e.g.

          // For NC things, use advice = MAP SOURCE CONCEPT CANNOT BE CLASSIFIED
          // WITH AVAILABLE DATA
          // 6f9a901c-bae9-590b-8e8f-32e4d5e19d98 20170731 1 0 refsetId 94312000
          // 1
          // 1
          // TRUE ALWAYS morbidity/other | POSSIBLE REQUIREMENT FOR MORPHOLOGY
          // CODE
          // 111033337/morbidity/other 447561005
          // 447637006

          // Write notes file (fix \n to <br>
          // Sample note:
          // "289472005: ICD10 Map: NC<br>
          // Candidates: 1644704601 "name" (0.56)<br>
          // 1151949779 "name" (0.56)<br>
          // 1318576293 "name" (0.74)<br>,
          // RULE1: n/a - NC<br>
          // RULE2: 6 matches -<br>
          // MATCH 1318576293 "name"<br>
          // MATCH 1090115429<br>
          // ...
          final StringBuilder notes = new StringBuilder();

        }

      }

      mapOut.close();
      notesOut.close();

      Logger.getLogger(getClass()).info("Finished compute ICD11");

    } catch (Exception e) {
      throw e;
    } finally {
      securityService.close();
      contentService.close();
      mappingService.close();
      workflowService.close();
      reportService.close();
    }

  }

  /**
   * Returns the max depth.
   *
   * @param cid the cid
   * @param sctAncDesc the sct anc desc
   * @return the max depth
   */
  @SuppressWarnings("static-method")
  private int getMaxDepth(String cid, Map<String, Set<Depth>> sctAncDesc) {
    final Set<Depth> depths = sctAncDesc.get(cid);
    if (depths == null) {
      return 0;
    }
    int maxDepth = 0;
    for (final Depth depth : depths) {
      if (depth.getDepth() > maxDepth) {
        maxDepth = depth.getDepth();
      }
    }
    // a max depth of 0 is a leaf node, something with no children.
    return maxDepth;
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // n/a
  }

  /**
   * Represents a depth relative to anc/desc rows.
   */
  class Depth {

    /** The code. */
    private String code;

    /** The depth. */
    private int depth;

    /**
     * Instantiates an empty {@link Depth}.
     */
    public Depth() {
      // n/a
    }

    /**
     * Instantiates a {@link Depth} from the specified parameters.
     *
     * @param line the line
     * @param ancDesc the anc desc
     */
    public Depth(String line, boolean ancDesc) {
      // 10001005|127091000119100|2|
      final String[] fields = line.split("\\|");
      if (ancDesc) {
        code = fields[1];
      } else {
        code = fields[0];
      }
      depth = Integer.parseInt(fields[2]);

    }

    /**
     * Returns the code.
     *
     * @return the code
     */
    public String getCode() {
      return code;
    }

    /**
     * Sets the code.
     *
     * @param code the code
     */
    public void setCode(String code) {
      this.code = code;
    }

    /**
     * Returns the depth.
     *
     * @return the depth
     */
    public int getDepth() {
      return depth;
    }

    /**
     * Sets the depth.
     *
     * @param depth the depth
     */
    public void setDepth(int depth) {
      this.depth = depth;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "Depth [code=" + code + ", depth=" + depth + "]";
    }
  }

  /**
   * A code and a score.
   */
  class Score {

    /** The code. */
    private String code;

    /** The score. */
    private double score;

    /**
     * Instantiates an empty {@link Score}.
     */
    public Score() {
      // n/a
    }

    /**
     * Instantiates a {@link Score} from the specified parameters.
     *
     * @param line the line
     */
    public Score(String line) {
      // 10065003|545442245/morbidity/other|12.5|Excoriated acne|Excoriated acne
      final String[] fields = line.split("\\|");
      code = fields[1];
      score = Double.valueOf(fields[2]);

    }

    /**
     * Returns the terminology id.
     *
     * @return the terminology id
     */
    public String getCode() {
      return code;
    }

    /**
     * Sets the terminology id.
     *
     * @param code the terminology id
     */
    public void setCode(String code) {
      this.code = code;
    }

    /**
     * Returns the score.
     *
     * @return the score
     */
    public double getScore() {
      return score;
    }

    /**
     * Sets the score.
     *
     * @param score the score
     */
    public void setScore(double score) {
      this.score = score;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "Score [code=" + code + ", score=" + score + "]";
    }

  }

  /**
   * An entry from an ICD 10-11 or 11-10 map.
   */
  class WhoMap {

    /** The kind. */
    private String kind;

    /** The code. */
    private String code;

    /** The name. */
    private String name;

    /** The target kind. */
    private String targetKind;

    /** The target code. */
    private String targetCode;

    /** The target name. */
    private String targetName;

    /** The relation. */
    private String relation;

    /** The distance. */
    private String distance;

    /** The issue type. */
    private String issueType;

    /** The states. */
    private boolean stated;

    /**
     * Instantiates an empty {@link WhoMap}.
     */
    public WhoMap() {
      // n/a
    }

    /**
     * Instantiates a {@link WhoMap} from the specified parameters.
     *
     * @param line the line
     * @throws Exception the exception
     */
    public WhoMap(String line) throws Exception {
      final String[] fields = line.split("\\t");
      if (fields.length == 12 || fields.length == 11 || fields.length == 10) {
        // 0 - 10ClassKind
        // 1 - icd10Code
        // 2 - icd10Title
        // 3 - 11ClassKind
        // 4 - icd11Id
        // 5 - icd11Code
        // 6 - icd11Title
        // 7 - Relation
        // 8 - Linearization
        // 9 - StatementDistance
        // 10 - IssueType
        // 11 - 2015-May-31
        kind = fields[0];
        code = fields[1].replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/",
            "");
        name = fields[2];
        targetKind = fields[3];
        targetCode = fields[4]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");

        targetName = fields[6];
        relation = fields[7];
        distance = fields[9];
        issueType = "";
        if (fields.length > 10) {
          issueType = fields[10];
        }

      } else if (fields.length == 8 || fields.length == 7) {
        // 0 - icd11Id
        // 1 - icd11Code
        // 2 - icd11Title
        // 3 - icd10Code
        // 4 - icd10Title
        // 5 - Relation
        // 6 - Stated{0}|Deduced
        // 7 - 2015-May-31
        // http://id.who.int/icd/entity/434814373 Infections due to Bacteria No
        // Mapping -1
        code = fields[0].replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/",
            "");
        name = fields[2];
        targetCode = fields[3]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");

        targetName = fields[4];
        relation = fields[5];
        stated = fields[6].equals("0");
      } else {
        throw new Exception(
            "Unexpected number of fields - " + fields.length + ", " + line);
      }
    }

    /**
     * Returns the kind.
     *
     * @return the kind
     */
    public String getKind() {
      return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the kind
     */
    public void setKind(String kind) {
      this.kind = kind;
    }

    /**
     * Returns the code.
     *
     * @return the code
     */
    public String getCode() {
      return code;
    }

    /**
     * Sets the code.
     *
     * @param code the code
     */
    public void setCode(String code) {
      this.code = code;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Returns the target kind.
     *
     * @return the target kind
     */
    public String getTargetKind() {
      return targetKind;
    }

    /**
     * Sets the target kind.
     *
     * @param targetKind the target kind
     */
    public void setTargetKind(String targetKind) {
      this.targetKind = targetKind;
    }

    /**
     * Returns the target code.
     *
     * @return the target code
     */
    public String getTargetCode() {
      return targetCode;
    }

    /**
     * Sets the target code.
     *
     * @param targetCode the target code
     */
    public void setTargetCode(String targetCode) {
      this.targetCode = targetCode;
    }

    /**
     * Returns the target name.
     *
     * @return the target name
     */
    public String getTargetName() {
      return targetName;
    }

    /**
     * Sets the target name.
     *
     * @param targetName the target name
     */
    public void setTargetName(String targetName) {
      this.targetName = targetName;
    }

    /**
     * Returns the relation.
     *
     * @return the relation
     */
    public String getRelation() {
      return relation;
    }

    /**
     * Sets the relation.
     *
     * @param relation the relation
     */
    public void setRelation(String relation) {
      this.relation = relation;
    }

    /**
     * Returns the distance.
     *
     * @return the distance
     */
    public String getDistance() {
      return distance;
    }

    /**
     * Sets the distance.
     *
     * @param distance the distance
     */
    public void setDistance(String distance) {
      this.distance = distance;
    }

    /**
     * Returns the issue type.
     *
     * @return the issue type
     */
    public String getIssueType() {
      return issueType;
    }

    /**
     * Sets the issue type.
     *
     * @param issueType the issue type
     */
    public void setIssueType(String issueType) {
      this.issueType = issueType;
    }

    /**
     * Indicates whether or not states is the case.
     *
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    public boolean isStated() {
      return stated;
    }

    /**
     * Sets the stated.
     *
     * @param stated the stated
     */
    public void setStated(boolean stated) {
      this.stated = stated;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "WhoMap [kind=" + kind + ", code=" + code + ", name=" + name
          + ", targetKind=" + targetKind + ", targetCode=" + targetCode
          + ", targetName=" + targetName + ", relation=" + relation
          + ", distance=" + distance + ", issueType=" + issueType + ", stated="
          + stated + "]";
    }
  }

  /**
   * The Class IcdMap.
   */
  class IcdMap {

    /** The concept id. */
    private String conceptId;

    /** The map group. */
    private int mapGroup;

    /** The map priority. */
    private int mapPriority;

    /** The map rule. */
    private String mapRule;

    /** The map advice. */
    private String mapAdvice;

    /** The map target. */
    private String mapTarget;

    /** The map category id. */
    private String mapCategoryId;

    /**
     * Instantiates an empty {@link IcdMap}.
     */
    public IcdMap() {
    }

    /**
     * Instantiates a {@link IcdMap} from the specified parameters.
     *
     * @param line the line
     */
    public IcdMap(String line) {
      // id effectiveTime active moduleId refsetId referencedComponentId
      // mapGroup mapPriority mapRule mapAdvice mapTarget correlationId
      // mapCorrelationId
      final String[] fields = line.split("\\t");
      this.mapGroup = Integer.valueOf(fields[6]);
      this.mapPriority = Integer.valueOf(fields[7]);
      this.mapRule = fields[8];
      this.mapAdvice =
          fields[9].replaceAll("ALWAYS [A-Z\\.0-9]+( \\|)?", "").replace(
              "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
              "");
      // Convert to "NC"
      this.mapTarget = fields[10].equals("") ? "NC" : fields[10];
      this.mapCategoryId = fields[12];
    }

    /**
     * Instantiates a {@link IcdMap} from the specified parameters.
     *
     * @param copy the copy
     */
    public IcdMap(IcdMap copy) {
      mapGroup = copy.getMapGroup();
      mapPriority = copy.getMapPriority();
      mapRule = copy.getMapRule();
      mapAdvice = copy.getMapAdvice();
      mapTarget = copy.getMapTarget();
      mapCategoryId = copy.getMapCategoryId();
      conceptId = copy.getConceptId();
    }

    /**
     * Returns the map group.
     *
     * @return the map group
     */
    public int getMapGroup() {
      return mapGroup;
    }

    /**
     * Sets the map group.
     *
     * @param mapGroup the map group
     */
    public void setMapGroup(int mapGroup) {
      this.mapGroup = mapGroup;
    }

    /**
     * Returns the map priority.
     *
     * @return the map priority
     */
    public int getMapPriority() {
      return mapPriority;
    }

    /**
     * Sets the map priority.
     *
     * @param mapPriority the map priority
     */
    public void setMapPriority(int mapPriority) {
      this.mapPriority = mapPriority;
    }

    /**
     * Returns the map rule.
     *
     * @return the map rule
     */
    public String getMapRule() {
      return mapRule;
    }

    /**
     * Sets the map rule.
     *
     * @param mapRule the map rule
     */
    public void setMapRule(String mapRule) {
      this.mapRule = mapRule;
    }

    /**
     * Returns the map advice.
     *
     * @return the map advice
     */
    public String getMapAdvice() {
      return mapAdvice;
    }

    /**
     * Sets the map advice.
     *
     * @param mapAdvice the map advice
     */
    public void setMapAdvice(String mapAdvice) {
      this.mapAdvice = mapAdvice;
    }

    /**
     * Returns the map target.
     *
     * @return the map target
     */
    public String getMapTarget() {
      return mapTarget;
    }

    /**
     * Sets the map target.
     *
     * @param mapTarget the map target
     */
    public void setMapTarget(String mapTarget) {
      this.mapTarget = mapTarget;
    }

    /**
     * Returns the map category id.
     *
     * @return the map category id
     */
    public String getMapCategoryId() {
      return mapCategoryId;
    }

    /**
     * Sets the map category id.
     *
     * @param mapCategoryId the map category id
     */
    public void setMapCategoryId(String mapCategoryId) {
      this.mapCategoryId = mapCategoryId;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "IcdMap [mapGroup=" + mapGroup + ", mapPriority=" + mapPriority
          + ", mapRule=" + mapRule + ", mapAdvice=" + mapAdvice + ", mapTarget="
          + mapTarget + ", mapCategoryId=" + mapCategoryId + "]";
    }

    public String getConceptId() {
      return conceptId;
    }

    public void setConceptId(String conceptId) {
      this.conceptId = conceptId;
    }
  }
}
