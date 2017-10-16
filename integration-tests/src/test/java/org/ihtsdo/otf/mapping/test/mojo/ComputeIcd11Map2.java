/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.test.mojo;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A mechanism to reset to the stock demo database.
 */
public class ComputeIcd11Map2 {

  /**
   * Mapping category enum (overall for the map, not for each row).
   */
  private enum Category {

    /** The unknown. */
    UNKNOWN,
    /** The low. */
    LOW,
    /** The medium. */
    MEDIUM,
    /** The high. */
    HIGH,
    /** The no map. */
    NO_MAP;
  }

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    //
  }

  /** The icd 11 map. */
  // Vars for test
  final Map<String, List<IcdMap>> icd11Map = new TreeMap<>();

  /** The icd 11 map notes. */
  final Map<String, String> icd11MapNotes = new TreeMap<>();

  /** The sct concepts. */
  final Map<String, String> sctConcepts = new HashMap<>();

  /** The icd 11 map edits. */
  final Map<String, List<IcdMap>> icd11MapEdits = new HashMap<>();

  /** The icd 11 concepts. */
  final Map<String, String> icd11Concepts = new HashMap<>();

  /** The icd 11 par chd. */
  final Map<String, Set<String>> icd11ParChd = new HashMap<>();

  /** The icd 11 index. */
  final Map<String, Set<String>> icd11Index = new HashMap<>();

  /** The icd 10 concepts. */
  final Map<String, String> icd10Concepts = new HashMap<>();

  /** The icd 10 map. */
  final Map<String, List<IcdMap>> icd10Map = new HashMap<>();

  /** The sct anc desc. */
  final Map<String, Set<String>> sctAncDesc = new HashMap<>();

  /** The sct desc anc. */
  final Map<String, Set<String>> sctDescAnc = new HashMap<>();

  /** The sct chd par. */
  final Map<String, Set<String>> sctChdPar = new HashMap<>();

  /** The xt concepts, like "Recurrent". */
  final Map<String, String> xtConcepts = new HashMap<>();

  /** The icd 10 to 11. */
  final Map<String, Set<WhoMap>> icd10To11 = new HashMap<>();

  /** The icd 11 to 10 reverse. */
  final Map<String, Set<WhoMap>> icd11To10Reverse = new HashMap<>();

  /** The sct icd 11 lexical. */
  final Map<String, List<Score>> sctIcd11Matches = new HashMap<>();

  /** The sct scope. */
  final List<String> sctScope = new ArrayList<>();

  /** The starter set. */
  final List<String> starterSet = new ArrayList<>();

  /** The icd 11 scope. */
  final Set<String> icd11Scope = new HashSet<>();

  /** The sct icd 11 equivalence. */
  final Map<String, Set<String>> sctIcd11Equivalence = new HashMap<>();

  /** The sct icd 11 equivalence type. */
  final Map<String, String> sctIcd11EquivalenceType = new HashMap<>();

  /** The icd 11 body part map. */
  final Map<String, String> icd11BodyPartMap = new HashMap<>();

  /** The body part icd 11 map. */
  final Map<String, Set<String>> bodyPartIcd11Map = new HashMap<>();

  /** The sct body part map. */
  final Map<String, String> sctBodyPartMap = new HashMap<>();

  /** The body part sct map. */
  final Map<String, Set<String>> bodyPartSctMap = new HashMap<>();

  /** The advices to include. */
  final Set<String> advicesToInclude =
      new HashSet<>(Arrays.asList(new String[] {
          "THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
          "THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION"
      }));

  /** The advices to exclude. */
  final Set<String> advicesToExclude =
      new HashSet<>(Arrays.asList(new String[] {
          "FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
          "MAP IS CONTEXT DEPENDENT FOR GENDER",
          "MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
          "MAPPED FOLLOWING IHTSDO GUIDANCE", "MAPPED FOLLOWING WHO GUIDANCE",
          "MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
          "POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
          "POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
          "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
          "POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
          "SOURCE SNOMED CONCEPT IS AMBIGUOUS",
          "SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
          "THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE"
      }));

  /** The advices to replace. */
  final Map<String, String> advicesToReplace = new HashMap<>();
  // Initializer
  {
    advicesToReplace.put("POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
        "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE AND EXTENSION CODE(S)");
  }

  /** The sample ct. */
  final int sampleCt = 5;

  /** The note sb. */
  StringBuilder noteSb = null;

  /**
   * Compute ICD11 Map.
   *
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {
    Logger.getLogger(getClass()).info("Starting compute ICD11");
    if (System.getProperty("icd11.dir") == null) {
      throw new Exception("System must specify a icd11.dir property");
    }
    final String icd11Dir = System.getProperty("icd11.dir");
    // Tracking vars
    final String refsetId = "icd11RefsetId";
    final String moduleId = "123456789";
    Logger.getLogger(getClass()).info("  icd11Dir = " + icd11Dir);
    Logger.getLogger(getClass()).info("  moduleId = " + moduleId);
    Logger.getLogger(getClass()).info("  refsetId = " + refsetId);

    try {
      //
      // Load all data resources
      //
      Logger.getLogger(getClass()).info(
          "====================================================================");
      loadDataResources(icd11Dir);

      //
      // PROCESSING
      //
      Logger.getLogger(getClass()).info(
          "====================================================================");

      //
      // Reverse sort SNOMED concepts by max depth (e.g. top-down search)
      // This ensures ancestors are processed (and have available maps)
      // before
      // descendants
      //
      Collections.sort(sctScope, new Comparator<String>() {

        /* see superclass */
        @Override
        public int compare(String c1, String c2) {
          // Sort by thing with most descendants first
          int ct1 = 0;
          if (sctAncDesc.containsKey(c1)) {
            ct1 = sctAncDesc.get(c1).size();
          }
          int ct2 = 0;
          if (sctAncDesc.containsKey(c2)) {
            ct2 = sctAncDesc.get(c2).size();
          }

          return ct2 - ct1;
        }
      });
      // Sanity check
      Logger.getLogger(getClass()).info("   SORT (top) = " + sctScope.get(0));
      Logger.getLogger(getClass()).info("   SORT (top) = " + sctScope.get(1));
      Logger.getLogger(getClass())
          .info("   SORT (leaf) = " + sctScope.get(sctScope.size() - 2));
      Logger.getLogger(getClass())
          .info("   SORT (leaf) = " + sctScope.get(sctScope.size() - 1));

      // Configure writers
      final PrintWriter mapOut =
          new PrintWriter(new FileWriter(new File(icd11Dir, "icd11Map.txt")));
      final PrintWriter notesOut = new PrintWriter(
          new FileWriter(new File(icd11Dir, "icd11MapNotes.txt")));
      final PrintWriter statsOut = new PrintWriter(
          new FileWriter(new File(icd11Dir, "icd11MapStats.txt")));
      final int[] stats = new int[5];
      final Map<String, String> sctIdCategories = new HashMap<>();

      // Iterate through SNOMED concepts in sorted order
      int sctidCt = 0;
      RuleScores primaryScores = null;
      for (final String sctid : sctScope) {
        // skip header
        if (sctid.equals("referencedComponentId")) {
          continue;
        }

        if (sctid.equals("88121000119101")) {
          System.out.println("xxx");
        }

        // Set up initial map (if no ICD10 map -> should be out of
        // scope)

        // initialize category
        Category category = Category.UNKNOWN;
        sctidCt++;
        Logger.getLogger(getClass()).info("------------------------------");
        Logger.getLogger(getClass())
            .info("SCTID = " + sctid + " " + sctConcepts.get(sctid));

        // Reset noteSb
        noteSb = new StringBuilder();
        if (!icd10Map.containsKey(sctid)) {
          Logger.getLogger(getClass())
              .error("  No ICD10 map for SCTID: " + sctid);
        }

        // Prep maps for this SNOMED concept - this is what we are
        // computing
        final List<IcdMap> mapList = new ArrayList<>();
        icd11Map.put(sctid, new ArrayList<IcdMap>());

        // Gather evidence for map, score, and add any maps.
        for (int i = 1; i <= icd10Map.get(sctid).size(); i++) {
          final IcdMap map10 = icd10Map.get(sctid).get(i - 1);
          final RuleDetails[] rules = gatherMapEvidence(sctid, i);

          // Check for evidence to consider a secondary map
          if (map10.getMapGroup() > 1 && map10.getMapPriority() == 1) {
            // Bail if RULE2 does not offer any reason for a higher
            // map - bail
            if (rules[2].getScoreMap().size() == 0) {
              break;
            }
          }

          final RuleScores scores = scoreMapEvidence(sctid, map10, rules);
          // Initialize the category for the primary map
          if (i == 1) {
            primaryScores = scores;
            category = scores.getCategory();
          }

          // Pick up initial advice from ICD10 (though may not be
          // appropriate if
          // RULE2 wasn't used
          if (rules[2].getScoreMap().size() > 0) {
            scores.getMap().setMapAdvice(map10.getMapAdvice());
            fixAdvice(scores.getMap(), scores.getMap().getMapTarget());
          }

          // Add map to list unless already there (for priority 1s)
          if (map10.getMapPriority() == 1) {
            boolean flag = false;
            for (final IcdMap map : mapList) {
              if (map.getMapTarget().equals(scores.getMap().getMapTarget())) {
                flag = true;
                break;
              }
            }
            // Do not map to the same target in separate entries
            if (flag) {
              break;
            }
          }

          mapList.add(scores.getMap());

          // Overall map should have the lowest matching category
          if (category.compareTo(scores.getCategory()) > 0) {
            category = scores.getCategory();
          }

          // If we encountered NO_MAP, we're done looking
          if (scores.getCategory() == Category.NO_MAP) {
            break;
          }

        }

        // Now with complete map, consider the override rules to change
        // the map
        // NOTE: this can change the number of maps in the list and/or
        // the
        // category
        category = applyOverrides(sctid, mapList, category, primaryScores);

        // Finally with the complete map, consider the advice that
        // should exist
        applyAdvices(sctid, mapList, primaryScores);

        // TODO: clean up stem/extension code stuff ...
        
        // Add the resulting maps to the final map
        icd11Map.get(sctid).addAll(mapList);

        // Increment the category for this concept (counted at the
        // concept
        // level, not entry level)
        noteSb.append("FINAL CATEGORY " + category.toString());

        // Add note
        icd11MapNotes.put(sctid, noteSb.toString().replaceAll("\\n", "<br>"));
        Logger.getLogger(getClass()).info(noteSb.toString());

        // Stats only for starter set
        if (starterSet.contains(sctid)) {
          stats[category.ordinal()]++;
        }
        sctIdCategories.put(sctid, category.toString());
      }

      // Log statistics
      Logger.getLogger(getClass()).info("------------------------------------");
      Logger.getLogger(getClass()).info("STATS");
      Logger.getLogger(getClass()).info("------------------------------------");
      Logger.getLogger(getClass()).info("TOTAL = " + sctidCt);
      statsOut.println("TOTAL = " + sctidCt);
      for (

      final Category category : Category.values()) {
        Logger.getLogger(getClass())
            .info(category.toString() + " = " + stats[category.ordinal()]);
        statsOut
            .println(category.toString() + " = " + stats[category.ordinal()]);
      }
      for (

      final String key : sctIdCategories.keySet()) {
        statsOut.println("  " + key + " = " + sctIdCategories.get(key));
      }
      statsOut.close();

      // Write icd11Map.txt - header
      mapOut.print(
          "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tmapGroup\t"
              + "mapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\r\n");
      // Write icd11Map.txt - data
      for (

      final String sctid : icd11Map.keySet()) {
        if (starterSet.contains(sctid)) {
          for (final IcdMap map : icd11Map.get(sctid)) {
            final StringBuilder sb = new StringBuilder();
            sb.append(UUID.randomUUID().toString()).append("\t");
            sb.append("20170901\t1\t");
            sb.append(moduleId).append("\t");
            sb.append(refsetId).append("\t");
            sb.append(sctid).append("\t");
            sb.append(map.getMapGroup()).append("\t");
            sb.append(map.getMapPriority()).append("\t");
            sb.append(map.getMapRule()).append("\t");
            sb.append(map.getMapAdvice()).append("\t");
            sb.append(map.getMapTarget()).append("\t");
            sb.append("447561005\t");
            sb.append(map.getMapCategoryId()).append("\t");
            sb.append("\r\n");
            mapOut.print(sb.toString());
          }
        }
      }

      // Write icd11MapNotes.txt - header
      notesOut.print(
          "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tfullySpecifiedName\tannotation\r\n");

      // Write icd11MapNotes.txt - data
      // Write icd11Map.txt - data
      for (

      final String sctid : icd11MapNotes.keySet()) {
        if (starterSet.contains(sctid)) {
          final String note = icd11MapNotes.get(sctid);
          final StringBuilder sb = new StringBuilder();
          sb.append(UUID.randomUUID().toString()).append("\t");
          sb.append("20170901\t1\t");
          sb.append(moduleId).append("\t");
          sb.append(refsetId).append("\t");
          sb.append(sctid).append("\t");
          sb.append("name blank").append("\t");
          sb.append(note.replaceAll("[\\n\\r]", "<br>"));
          sb.append("\r\n");
          notesOut.print(sb.toString());
        }
      }
      mapOut.close();
      notesOut.close();

      Logger.getLogger(

          getClass()).info("Finished compute ICD11");

    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Gather map evidence.
   *
   * @param sctid the sctid
   * @param index the index
   * @return the rule details[]
   */
  public RuleDetails[] gatherMapEvidence(String sctid, int index) {

    final RuleDetails[] rules = new RuleDetails[6];

    // Extract ICD10 info or this index
    final IcdMap map10 = (icd10Map.get(sctid).size() >= index)
        ? icd10Map.get(sctid).get(index - 1) : null;
    final String icd10Code = map10 != null ? map10.getMapTarget() : null;
    final String icd10Name = (icd10Code == null || icd10Code.equals(""))
        ? "No Mapping" : icd10Concepts.get(icd10Code);
    Logger.getLogger(getClass())
        .info("  ICD10 Map = " + icd10Code + ", " + icd10Name + ", " + map10);

    //
    // RULE0 - "no mapping"
    // - bypass this rule for "symptoms" in snomed
    rules[0] = new RuleDetails(0);
    if (icd10Code != null && icd10Code.equals("")
        && !sctConcepts.get(sctid).toLowerCase().contains("symptom")) {
      rules[0].addNcScore(1);
    }

    //
    // RULE1 - David Robinson's map
    // Any match is a good match and will be factored in score calculation
    // later.
    //
    rules[1] = new RuleDetails(1);
    if (sctIcd11Equivalence.containsKey(sctid)) {
      for (final String code : sctIcd11Equivalence.get(sctid)) {
        // Scale higher for primary map
        double scale = index == 1 ? 5.0 : 2.0;
        rules[1].addScore(code, scale / sctIcd11Equivalence.get(sctid).size());
        rules[1].appendType(code, sctIcd11EquivalenceType.get(sctid));
      }
    }

    //
    // RULE2 - the 10-11 equivalence map
    //
    rules[2] = new RuleDetails(2);
    if (map10 != null && icd10To11.containsKey(map10.getMapTarget())) {

      // Equivalent = 100%
      // InterSects
      // PotentialIntersect
      // StatementDistance
      // Subclass
      // Superclass
      boolean equivalent = false;
      for (final WhoMap map : icd10To11.get(map10.getMapTarget())) {
        final String code = map.getTargetCode();
        final int mapCt = icd10To11.get(map10.getMapTarget()).size();

        // Handle special neoplasm case
        // Neoplasm of X -> "unknown" not "uncertain" behavior.
        double modifier = 1.0;
        if (sctConcepts.get(sctid).startsWith("Neoplasm of ")
            && !sctConcepts.get(sctid).contains("behavior")
            && !sctConcepts.get(sctid).contains("behaviour")
            && icd11Concepts.get(code) != null) {
          // boost "unknown behavior"
          if (icd11Concepts.get(code).contains("of unknown behaviour")) {
            modifier = 1.5;
          }
          // lower "uncertain behavior"
          else if (icd11Concepts.get(code).contains("of uncertain behaviour")) {
            modifier = .5;
          }
        }
        // Favor "other" and "unspecified" if there are >4 maps
        else if (icd10To11.get(map10.getMapTarget()).size() > 4) {
          if (code.endsWith("other") || code.endsWith("unspecified")) {
            modifier = 1.25;
          }

        }

        if (code.equals("No Mapping")) {
          rules[2].addNcScore(1 * modifier);
        } else if (!icd11Scope.contains(code)) {
          if (icd11Concepts.containsKey(code + "/morbidity/unspecified")) {
            // no modifier
            rules[2].addScore(code + "/morbidity/unspecified", 1.0);
            rules[2].appendType(code, "Subclass - unspecified");
          }
          if (icd11Concepts.containsKey(code + "/morbidity/other")) {
            // no modifier
            rules[2].addScore(code + "/morbidity/other", 1.0);
            rules[2].appendType(code, "Subclass - other");
          }
        } else if (map.getRelation().equals("Equivalent")) {
          rules[2].addScore(code, 2.0 * modifier);
          rules[2].appendType(code, "Equivalent");
          equivalent = true;
        } else if (map.getRelation().equals("Subclass")) {
          // Handle subclass
          if (icd11Concepts.containsKey(code + "/morbidity/unspecified")) {
            rules[2].addScore(code + "/morbidity/unspecified", 1.0);
            rules[2].appendType(code, "Subclass - unspecified");
          }
          if (icd11Concepts.containsKey(code + "/morbidity/other")) {
            rules[2].addScore(code + "/morbidity/other", 1.0);
            rules[2].appendType(code, "Subclass - other");
          }
          rules[2].addScore(code, 1.0 * modifier);
          rules[2].appendType(code, map.getRelation());
        } else if (map.getRelation().equals("PotentialIntersect")
            || map.getRelation().equals("InterSects")) {
          // These are often not very good maps, lower but keep
          rules[2].addScore(code, 0.25 * modifier);
          rules[2].appendType(code, map.getRelation());
        } else if (map.getRelation().equals("Subclass")) {
          // These are going to be a more specific code in ICD11 than
          // in
          // SNOMED based on the ID map -> keep this low too.
          rules[2].addScore(code, 0.25 * modifier);
          rules[2].appendType(code, map.getRelation());
        } else {
          rules[2].addScore(code, (1.0 * modifier) / mapCt);
          rules[2].appendType(code, map.getRelation());
        }
      }
      // Boost "unspecified" codes if there are no EXACT matches
      // and there more than 5 lower quality matches
      // don't do this if it is a "with" case
      if (!sctConcepts.get(sctid).toLowerCase().contains(" with ")
          && !equivalent && rules[2].getScoreMap().size() > 5) {
        for (final String code : new HashSet<>(
            rules[2].getScoreMap().keySet())) {
          if (code.endsWith("unspecified")) {
            final double score = rules[2].getScore(code);
            final String type = rules[2].getTypeMap().get(code);
            rules[2].getScoreMap().put(code, score * 1.5);
            rules[2].getTypeMap().put(code, type + " USBOOST");
          }
        }
      }

    }
    if (map10 != null && icd11To10Reverse.containsKey(map10.getMapTarget())) {
      // stated = 100%
      for (final WhoMap map : icd11To10Reverse.get(map10.getMapTarget())) {
        final String code = map.getTargetCode();
        if (code.equals("No Mapping")) {
          rules[2].addNcScore(1);
        } else if (map.isStated()) {
          rules[2].addScore(code, 1.0);
          rules[2].appendType(code, "stated");
        }
      }
    }

    //
    // RULE3 - Lexical Match
    //
    rules[3] = new RuleDetails(3);
    if (sctIcd11Matches.containsKey(sctid)) {
      boolean exact = false;
      for (final Score score : sctIcd11Matches.get(sctid)) {
        String code = score.getCode();

        // If we've already seen the code (because of transformations
        // below) skip it
        if (rules[3].getScoreMap().containsKey(code)) {
          continue;
        }

        // Skip things out of scope, but try sub-codes first
        if (!icd11Scope.contains(code)) {
          if (!code.endsWith("unspecified")
              && icd11Scope.contains(code + "/morbidity/unspecified")) {
            code = code + "/morbidity/unspecified";
          } else if (!code.endsWith("other")
              && icd11Scope.contains(code + "/morbidity/other")) {
            code = code + "/morbidity/other";
          } else {
            continue;
          }
        }

        double modifier = 1.0;

        // xaNeoplasmRule
        // Boost the scores of XA things so that body parts associated
        // with neoplasms can be shown
        if (icd11Concepts.get(code).startsWith("XA")
            && sctConcepts.get(sctid).toLowerCase().contains("neoplasm")) {
          modifier = 2.0;
        }

        double finalScore = score.getScore() * modifier;
        // skip low scores
        if (finalScore < 3) {
          continue;
        }
        // bump high scores
        else if (finalScore > 12) {
          rules[3].addScore(code, 2.05);
          rules[3].appendType(code, "EXACT PT");
          exact = true;
        } else if (finalScore >= 10) {
          rules[3].addScore(code, 1.51);
          rules[3].appendType(code, "EXACT ");
          exact = true;
        }
        // handle other scores
        else {
          // All other things being equal, favor "other" and
          // "unspecified"
          if (code.endsWith("other") || code.endsWith("unspecified")) {
            rules[3].addScore(code, finalScore / 7.5);
            rules[3].appendType(code, "MATCH O/S");
          } else {
            rules[3].addScore(code, finalScore / 8.0);
            rules[3].appendType(code, "MATCH");
          }
        }

      }

      // Boost "unspecified" codes if there are no EXACT matches
      // and there more than 5 lower quality matches (
      // TODO: need to define "lower quality matches")
      if (!exact && rules[3].getScoreMap().size() > 5) {
        for (final String code : new HashSet<>(
            rules[3].getScoreMap().keySet())) {
          if (code.endsWith("unspecified")) {
            final double score = rules[3].getScore(code);
            final String type = rules[3].getTypeMap().get(code);
            rules[3].getScoreMap().put(code, score * 1.5);
            rules[3].getTypeMap().put(code, type + " USBOOST");
          }
        }
      }
    }

    //
    // RULE4 - Matching parent map
    //
    rules[4] = new RuleDetails(4);
    // All parents should have maps at this point.
    if (sctChdPar.containsKey(sctid)) {
      final Map<String, Integer> parentMaps = new HashMap<>();
      int totalCt = 0;
      for (final String par : sctChdPar.get(sctid)) {
        if (!icd11Map.containsKey(par)) {
          // should be top-level things
          Logger.getLogger(getClass()).warn(
              sctid + " without PAR map " + par + " " + sctConcepts.get(par));
          continue;
        }
        for (final IcdMap map : icd11Map.get(par)) {
          totalCt++;
          final String code = map.getMapTarget();
          if (!parentMaps.containsKey(code)) {
            parentMaps.put(code, 1);
          } else {
            parentMaps.put(code, parentMaps.get(code) + 1);
          }
          // Only consider primary code
          break;
        }
      }
      for (final String code : parentMaps.keySet()) {
        if (code.equals("")) {
          rules[4].addNcScore((parentMaps.get(code) * 1.0) / totalCt);
          rules[4].appendType(code, "NC");
        } else {
          rules[4].addScore(code, (parentMaps.get(code) * 1.0) / totalCt);
          rules[4].appendType(code, parentMaps.get(code) + " PARENTS ");
        }
      }
    }

    //
    // RULE5 - Boosts on other rules
    //
    rules[5] = new RuleDetails(5);
    for (final RuleDetails rule : new RuleDetails[] {
        rules[1], rules[2], rules[3], rules[4]
    }) {
      // Boost all scores if "unspecified" and ends with Z/other
      for (final String code : rule.scoreMap.keySet()) {
        if (code != null
            && sctConcepts.get(sctid).toLowerCase().contains("unspecified")
            && code.endsWith("unspecified")) {
          rule.scoreMap.put(code, rule.scoreMap.get(code) * 1.5);
          rules[5].addScore(code, 1d);
          rules[5].appendType(code, "BOOST (unspecified) 1.5");
        }
      }
      // Boost all scores if "other" snomed term matches icd11 parent
      // and icd11 code ends with "other"
      for (final String code : rule.scoreMap.keySet()) {
        final String parCode = code.replace("/morbidity/other", "");
        if (code != null && icd11Concepts.get(parCode) != null
            && sctConcepts.get(sctid).toLowerCase()
                .contains(icd11Concepts.get(parCode))
            && code.endsWith("other")) {
          System.out.println(
              "YYYY: " + sctid + ", " + sctConcepts.get(sctid) + " = " + code);
          rule.scoreMap.put(code, rule.scoreMap.get(code) * 1.5);
          rules[5].addScore(code, 1d);
          rules[5].appendType(code, "BOOST (other) 1.5");
        }
      }
      // Boost child score if parent score also exists
      for (final String parCode : rule.scoreMap.keySet()) {
        for (final String chdCode : rule.scoreMap.keySet()) {
          if (chdCode != null && parCode != null
              && icd11ParChd.get(parCode) != null
              && icd11ParChd.get(parCode).contains(chdCode)) {
            rule.scoreMap.put(chdCode, rule.scoreMap.get(chdCode) * 1.25);
            rules[5].addScore(chdCode, 1d);
            rules[5].appendType(chdCode, "BOOST (child over parent) 1.25");
          }
        }
      }
    }

    return rules;
  }

  /**
   * Score map evidence.
   *
   * @param sctid the sctid
   * @param map10 the map 10
   * @param rules the rules
   * @return the rule scores
   * @throws Exception the exception
   */
  public RuleScores scoreMapEvidence(String sctid, IcdMap map10,
    RuleDetails[] rules) throws Exception {

    // Initialize map (copy advices, etc.), default category
    final IcdMap map11 = new IcdMap(map10);
    map11.setMapCategoryId("447637006");

    //
    // Determine category and targetId
    //
    String targetId = "";
    Category category = Category.UNKNOWN;
    final Map<String, Double> candidates = new HashMap<>();

    // Compute NC Score
    final double ncScore = rules[0].getNcScore() + rules[1].getNcScore()
        + rules[2].getNcScore() + rules[3].getNcScore() + rules[4].getNcScore();
    Logger.getLogger(getClass()).info("  NC SCORE = " + ncScore);
    // If NC score is high enough, assign cateogry 4 and move on.
    if (ncScore > 3.0) {
      category = Category.NO_MAP;
      targetId = "";
      candidates.put(targetId, ncScore);
      map11.setMapAdvice(
          "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA");
      map11.setMapCategoryId("447638001");
    }

    // Compute scores from the RuleDetails
    else {

      // Initialize all candidate scores at 0
      for (final RuleDetails rule : new RuleDetails[] {
          rules[1], rules[2], rules[3], rules[4]
      }) {
        for (final String code : rule.getScoreMap().keySet()) {
          candidates.put(code, 0.0);
        }
      }

      // Calculate rule scores - ignore RULE0
      // Add scores from RULES 1,2,3
      boolean anyScoreAboveParent = false;
      for (final RuleDetails rule : new RuleDetails[] {
          rules[1], rules[2], rules[3]
      }) {
        for (final String code : rule.getScoreMap().keySet()) {
          double prevScore = candidates.get(code);
          double score = rule.getScore(code);
          candidates.put(code, score + prevScore);
          if (score + prevScore > .3) {
            anyScoreAboveParent = true;
          }
        }
      }

      // Calculate rule scores - ignore RULE0
      // Boost rule4
      for (final RuleDetails rule : new RuleDetails[] {
          rules[4]
      }) {
        for (final String code : rule.getScoreMap().keySet()) {
          // If there wasn't a score from the previous section
          // add a low score
          if (!anyScoreAboveParent) {
            candidates.put(code, .3d);
          }
          // otherwise, if there is a score, boost it
          else if (candidates.containsKey(code)) {
            double prevScore = candidates.get(code);
            candidates.put(code, 1.5 * prevScore);
          }
          // check children of the rule4 code too, and boost those
          // as well
          if (icd11ParChd.containsKey(code)) {
            for (final String chd : icd11ParChd.get(code)) {
              if (candidates.containsKey(chd)) {
                double prevScore = candidates.get(chd);
                candidates.put(chd, 1.5 * prevScore);
              }
            }
          }
        }
      }

      // REQUIRED words
      // for candidates without required words, lower score to 0.1
      final Map<String, String> requiredWords = new HashMap<>();
      requiredWords.put("remission", "remission");
      requiredWords.put("hereditary", "hereditary");
      requiredWords.put("congenital", "congenital");
      requiredWords.put("acute-on-chronic", "acute");
      requiredWords.put("acute", "acute");
      requiredWords.put("chronic", "chronic");
      requiredWords.put("natal", "natal");
      for (final String word : requiredWords.keySet()) {
        // Lower match if snomed contains word and ICD does not = /2
        if (sctConcepts.get(sctid).toLowerCase().contains(word)) {
          for (final String key : new HashSet<>(candidates.keySet())) {
            if (icd11Concepts.containsKey(key) && !icd11Concepts.get(key)
                .toLowerCase().contains(requiredWords.get(word))) {
              candidates.put(key, candidates.get(key) * 0.45d);
              rules[5].addScore(key, 1d);
              rules[5].appendType(key, "BOOST-DOWN (" + word + ") 0.45");
            } else if (icd11Concepts.containsKey(key) && icd11Concepts.get(key)
                .toLowerCase().contains(requiredWords.get(word))) {
              candidates.put(key, candidates.get(key) * 1.5d);
              rules[5].addScore(key, 1d);
              rules[5].appendType(key, "BOOST (" + word + ") 1.5");
            }
          }
        }
        // Lower match if ICD contains word and SNOMED does not =
        // minimal
        // 0.1
        if (!sctConcepts.get(sctid).toLowerCase().contains(word)) {
          for (final String key : new HashSet<>(candidates.keySet())) {
            if (icd11Concepts.containsKey(key)
                && icd11Concepts.get(key).toLowerCase().contains(word)) {
              candidates.put(key, 0.1d);
              rules[5].addScore(key, 1d);
              rules[5].appendType(key, "BOOST-DOWN (" + word + ") 0.1");
            }
          }
        }
      }

      // BOOST sustained full remission slightly to prefer
      for (final String key : new HashSet<>(candidates.keySet())) {
        if (icd11Concepts.containsKey(key) && icd11Concepts.get(key)
            .toLowerCase().contains("sustained full remission")) {
          candidates.put(key, candidates.get(key) * 1.01d);
          rules[5].addScore(key, 1d);
          rules[5].appendType(key,
              "BOOST-DOWN (sustained full remission) 1.01");
        }
      }

      // BODY PART requirement
      // If SNOMED has a body part and ICD11 has a matching one, boost
      if (sctBodyPartMap.containsKey(sctid)) {
        final String bodyPart = sctBodyPartMap.get(sctid);
        for (final String key : new HashSet<>(candidates.keySet())) {
          // if no body parts for this code
          // or no icd11 body parts for this body part
          // or body parts for this body part but not for this code
          if (icd11BodyPartMap.containsKey(key)
              && bodyPartIcd11Map.containsKey(bodyPart)
              && bodyPartIcd11Map.get(bodyPart).contains(key)) {
            candidates.put(key, candidates.get(key) * 1.5d);
            rules[5].addScore(key, 1d);
            rules[5].appendType(key,
                "BOOST (" + sctBodyPartMap.get(sctid) + ") 1.5");
          }
        }
      }
      // OR if SNOMED does not have a body part
      // AND ICD11 does, down-boost
      else {
        for (final String key : new HashSet<>(candidates.keySet())) {
          if (icd11BodyPartMap.containsKey(key)) {
            // (unless its a body part ending in "system" -> because
            // of
            // granularity differences)
            if (icd11BodyPartMap.get(key).toLowerCase().endsWith(" system")) {
              continue;
            }
            candidates.put(key, candidates.get(key) * 0.75d);
            rules[5].addScore(key, 1d);
            rules[5].appendType(key,
                "BOOST-DOWN (" + icd11BodyPartMap.get(key) + ") 0.75");
          }
        }
      }

      // Find the max score (or the first one with that value)
      double maxScore = 0.0;
      for (final String code : candidates.keySet()) {
        if (maxScore < candidates.get(code)) {
          maxScore = candidates.get(code);
        }
      }
      for (final String code : candidates.keySet()) {
        if (candidates.get(code).equals(maxScore)) {
          targetId = code;
          break;
        }
      }

      // If no scores, decide what to do - handles otherwise UNKNOWN
      if (maxScore == 0.0 && ncScore == 0.0) {

        // If ICD10 had no target,this is NO MAP
        if (icd10Map.containsKey(sctid) && icd10Map.get(sctid).iterator().next()
            .getMapTarget().equals("")) {
          category = Category.NO_MAP;
          targetId = "";
          candidates.put(targetId, ncScore);
          map11.setMapAdvice(
              "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA");
          map11.setMapCategoryId("447638001");
        }

        // Otherwise, it's "low" because there is probably something
        else {
          if (category.compareTo(category) < 1) {
            category = Category.LOW;
          }
          targetId = "";
          candidates.put(targetId, ncScore);
          map11.setMapAdvice(
              "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA");
          map11.setMapCategoryId("447638001");

        }

        // if NC is greater than max score, use NC
      } else if (ncScore > maxScore) {
        category = Category.NO_MAP;
        targetId = "";
        candidates.put(targetId, ncScore);
        map11.setMapAdvice(
            "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA");
        map11.setMapCategoryId("447638001");
      } else {
        // Determine category
        if (maxScore >= 3.0) {
          category = Category.HIGH;
        } else if (maxScore < 1.5) {
          category = Category.LOW;
        } else {
          category = Category.MEDIUM;
        }

        // Set map advice
        if (targetId.equals("")) {
          map11.setMapAdvice("");
        } else {
          map11.setMapAdvice("ALWAYS " + targetId);
        }

        map11.setMapCategoryId("447637006");
      }
    }

    if (!targetId.equals("") && !icd11Concepts.containsKey(targetId)) {
      throw new Exception(
          "Invalid target id, not in icd11Concepts.txt - " + targetId);
    }
    map11.setMapTarget(targetId);

    //
    // Assemble the note
    //
    noteSb.append("\n");
    if (category == Category.LOW) {
      noteSb.append("LOW");
    }

    if (category == Category.MEDIUM) {
      noteSb.append("MEDIUM");
    }
    if (category == Category.HIGH) {
      noteSb.append("HIGH");
    }
    if (category == Category.NO_MAP) {
      noteSb.append("NO MAP");
    }
    noteSb.append(": ").append(" (").append(candidates.get(targetId))
        .append(") ").append(targetId).append(" ")
        .append(icd11Concepts.get(targetId)).append("\n");
    boolean first = false;
    final List<String> sortedCandidateKeys =
        new ArrayList<>(candidates.keySet());
    Collections.sort(sortedCandidateKeys, new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        final double val = candidates.get(o2) - candidates.get(o1);
        if (val < 0) {
          return -1;
        }
        if (val > 0) {
          return 1;
        }
        return 0;
      }
    });
    for (

    final String other : sortedCandidateKeys) {
      if (other.equals(targetId)) {
        continue;
      }
      if (!first) {
        noteSb.append("  Candidates: ");
        first = true;
      } else {
        noteSb.append("              ");
      }
      noteSb.append("(").append(candidates.get(other)).append(") ")
          .append(other).append(" ");
      if (!other.equals("No Mapping")) {
        noteSb.append(icd11Concepts.get(other));
      }
      noteSb.append("\n");
    }
    for (final RuleDetails rule : rules) {
      if (rule.hasCandidates()) {
        noteSb.append(rule.toNote(icd11Concepts));
      }
    }

    // Prepare return value
    final RuleScores scores = new RuleScores();
    scores.setCandidates(candidates);
    scores.setCategory(category);
    scores.setMap(map11);
    scores.setRuleDetails(rules);
    return scores;

  }

  /**
   * Apply overrides.
   *
   * @param sctid the sctid
   * @param mapList the map list
   * @param category the category
   * @param primaryScores the primary scores
   * @return the list
   */
  public Category applyOverrides(String sctid, List<IcdMap> mapList,
    Category category, RuleScores primaryScores) {

    // If the map was previously edited, use those mappings here
    if (icd11MapEdits.containsKey(sctid)) {
      mapList.clear();
      mapList.addAll(icd11MapEdits.get(sctid));
      noteSb.append("MAP EDITED\n");
      return Category.HIGH;
    }

    final Category[] categoryWrapper = new Category[] {
        category
    };
    final Map<String, Double> candidates = primaryScores.getCandidates();

    boolean override = false;
    if (!override) {
      override = applyXhNeoplasmRule(sctid, icd10Map.get(sctid), mapList,
          noteSb, candidates, categoryWrapper);
    }
    if (!override) {
      override = applyXaNeoplasmRule(sctid, icd10Map.get(sctid), mapList,
          noteSb, candidates, categoryWrapper);
    }
    if (!override) {
      override = applyXtAdditionRule(sctid, icd10Map.get(sctid), mapList,
          noteSb, candidates, categoryWrapper);
    }
    if (!override) {
      override = applyWithRule(sctid, icd10Map.get(sctid), mapList, noteSb,
          candidates, categoryWrapper);
    }
    if (!override) {
      override = applyDiabetesRule(sctid, icd10Map.get(sctid), mapList, noteSb,
          candidates, categoryWrapper);
    }

    //
    // Apply category overrides
    //

    // Override "event" cases to be HIGH
    if (sctAncDesc.get("272379006").contains(sctid)) {
      override = true;
      categoryWrapper[0] = Category.HIGH;
    }

    // Dock the overall category for "with", "due to", "and/or", " AND "
    final String sctName = sctConcepts.get(sctid);
    if (icd11Map.get(sctid).size() == 1 && category == Category.HIGH
        && (sctName.contains(" with ") || sctName.contains(" due to ")
            || sctName.toLowerCase().contains("and/or")
            || sctName.contains(" AND "))) {
      categoryWrapper[0] = Category.MEDIUM;
      noteSb.append("\nOVERRIDE " + category.toString()
          + " two-condition cases need to be reviewed as MEDIUM if they have only one code\n");
    }

    // Override for sickle-cell cases that are "high"
    if (sctConcepts.get(sctid).toLowerCase().contains("sickle cell")
        && categoryWrapper[0] == Category.HIGH) {
      noteSb.append("\nOVERRIDE sickle cell, lower category to MEDIUM\n");
      categoryWrapper[0] = Category.MEDIUM;
    }

    return categoryWrapper[0];
  }

  /**
   * Apply advices.
   *
   * @param sctid the sctid
   * @param mapList the map list
   * @param primaryScores the primary scores
   */
  public void applyAdvices(String sctid, List<IcdMap> mapList,
    RuleScores primaryScores) {

    int i = 0;
    for (final IcdMap map11 : mapList) {

      // For first map, if primary scores contain no RULE2 data for the
      // map
      // target, clear advice
      if (i == 0) {
        if (!primaryScores.getRuleDetails()[2].getScoreMap()
            .containsKey(map11.getMapTarget())) {
          fixAndClearAdvice(map11, map11.getMapTarget());
        }
      }
      i++;

      // Remove all ICD10 excluded advices
      for (final String adviceToRemove : advicesToExclude) {
        map11.setMapAdvice(
            getWithoutAdvice(map11.getMapAdvice(), adviceToRemove));
      }

      // Remove MAPPED FOLLOWING WHO GUIDANCE if descendant of:
      // 75478009 Poisoning (disorder)
      // 125605004 Fracture of bone (disorder)
      // 55342001 Neoplastic disease (disorder)
      if (sctAncDesc.get("75478009").contains(sctid)
          || sctAncDesc.get("125605004").contains(sctid)
          || sctAncDesc.get("55342001").contains(sctid)) {
        map11.setMapAdvice(getWithoutAdvice(map11.getMapAdvice(),
            "MAPPED FOLLOWING WHO GUIDANCE"));
      }

      // Replace advices where appropriate
      String advice = map11.getMapAdvice();
      for (final String advice10 : advicesToReplace.keySet()) {
        advice = advice.replace(advice10, advicesToReplace.get(advice10));
      }
      map11.setMapAdvice(advice);

    }
  }

  /**
   * Apply xh neoplasm rule.
   *
   * @param sctid the sctid
   * @param icd10Map the icd 10 map
   * @param icd11MapOrig the icd 11 map orig
   * @param noteSb the note sb
   * @param candidates the candidates
   * @param category the category
   * @return true, if successful
   */
  private boolean applyXhNeoplasmRule(String sctid, List<IcdMap> icd10Map,
    List<IcdMap> icd11MapOrig, StringBuilder noteSb,
    Map<String, Double> candidates, Category[] category) {

    final List<IcdMap> icd11Map = new ArrayList<>();

    // If nomap, pass on this
    if (category[0] == Category.NO_MAP) {
      return false;
    }

    // For descendants of 55342001 | Neoplastic disease (disorder) |
    // - and NOT descendant of 118599009 | Hodgkin's disease (disorder) |
    // - and NOT descendant of 118601006 | Non-Hodgkin's lymphoma
    // AND icd10Map has a single entry for the concept
    // AND icd11MapOrig has a XH code as primary map with score >= 2.0;
    // AND candidates includes one stem code (starting with 2) and score >
    // 1.5
    String stemCode = null;
    boolean override = false;
    double score = 0;
    if (sctAncDesc.get("55342001").contains(sctid)
        && !sctAncDesc.get("118599009").contains(sctid)
        && !sctAncDesc.get("118601006").contains(sctid) && icd10Map.size() == 1
        && icd11MapOrig.size() == 1) {
      final String targetId = icd11MapOrig.iterator().next().getMapTarget();
      final String targetCode = icd11Concepts.get(targetId);
      if (targetCode != null && targetCode.startsWith("XH")
          && candidates.get(targetId) >= 2.0) {
        score += candidates.get(targetId);
        for (final String otherTargetId : candidates.keySet()) {
          final String otherTargetCode = icd11Concepts.get(otherTargetId);
          if (otherTargetCode != null && otherTargetCode.startsWith("2")
              && candidates.get(otherTargetId) > 1.5) {
            score += candidates.get(otherTargetId);
            stemCode = otherTargetId;
            break;
          }
        }
        if (stemCode != null) {
          override = true;
        }
      }
    }

    // => Add a second icd11 map entry to first position with stem code
    // => remove advice POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE
    // => Add a note override
    // => Bump category to HIGH
    // => return true
    if (override) {
      final IcdMap origMap = icd11MapOrig.iterator().next();
      icd11Map.add(origMap);
      final IcdMap stemMap = new IcdMap(origMap);
      stemMap.setMapTarget(stemCode);
      icd11Map.add(0, stemMap);
      origMap.setMapPriority(2);
      fixAdvice(stemMap, stemCode);
      stemMap.setMapAdvice(getWithoutAdvice(stemMap.getMapAdvice(),
          "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE"));
      origMap.setMapAdvice(getWithoutAdvice(origMap.getMapAdvice(),
          "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE"));
      noteSb.append("\n\nOVERRIDE HIGH: (" + score
          + "): XH Neoplasm Rule (added stem code)\n");
      category[0] = Category.HIGH;

      icd11MapOrig.clear();
      icd11MapOrig.addAll(icd11Map);
    }

    return override;
  }

  /**
   * Apply xt addition rule.
   *
   * @param sctid the sctid
   * @param icd10Map the icd 10 map
   * @param icd11MapOrig the icd 11 map orig
   * @param noteSb the note sb
   * @param candidates the candidates
   * @param category the category
   * @return true, if successful
   */
  private boolean applyXtAdditionRule(String sctid, List<IcdMap> icd10Map,
    List<IcdMap> icd11MapOrig, StringBuilder noteSb,
    Map<String, Double> candidates, Category[] category) {

    final List<IcdMap> icd11Map = new ArrayList<>();

    // If nomap, pass on this
    if (category[0] == Category.NO_MAP) {
      return false;
    }

    final String sctName = sctConcepts.get(sctid).toLowerCase();

    // For descendants of 40733004 | Infectious disease (disorder) |
    // OR 128139000 | Inflammatory disease |
    // AND icd10Map has a single entry for the concept
    // AND icd11MapOrig has a stem code (starting with 1) and a score > .4
    // AND and the snomed term contains an XT word (especially at beginning
    // of
    // the word),
    String xtCode = null;
    String xtName = null;
    boolean override = false;
    double score = 0;
    if ((sctAncDesc.get("40733004").contains(sctid)
        || sctAncDesc.get("128139000").contains(sctid)) && icd10Map.size() == 1
        && icd11MapOrig.size() == 1) {
      final String targetId = icd11MapOrig.iterator().next().getMapTarget();
      final String targetCode = icd11Concepts.get(targetId);
      if (targetCode != null && candidates.get(targetId) >= 0.4) {
        score += 2.0;
        for (final String key : xtConcepts.keySet()) {
          if (icd11Index.get(targetId) != null && sctName.startsWith(key)
              && icd11Index.get(targetId)
                  .contains(sctName.replace(key + " ", ""))) {
            xtCode = xtConcepts.get(key);
            xtName = key;
            category[0] = Category.HIGH;
            break;
          } else if (sctName.startsWith(key)) {
            xtCode = xtConcepts.get(key);
            xtName = key;
            if (category[0].compareTo(Category.HIGH) <= 0) {
              category[0] = Category.HIGH;
            }
            break;
          }
        }
        if (xtCode != null) {
          override = true;
        }
      }
    }

    // => Add a second icd11 map entry at the end for the XT code
    // => Add a note override
    // => Bump category by 1 position
    // => return true
    if (override) {
      final IcdMap origMap = icd11MapOrig.iterator().next();
      icd11Map.add(origMap);
      final IcdMap xtMap = new IcdMap(origMap);
      xtMap.setMapTarget(xtCode);
      xtMap.setMapPriority(2);
      icd11Map.add(xtMap);
      noteSb.append("\nOVERRIDE " + category[0].toString() + ": (" + score
          + "): XT Addition Rule - " + xtName + "\n");

      icd11MapOrig.clear();
      icd11MapOrig.addAll(icd11Map);
    }

    return override;
  }

  /**
   * Apply with rule.
   *
   * @param sctid the sctid
   * @param icd10Map the icd 10 map
   * @param icd11MapOrig the icd 11 map orig
   * @param noteSb the note sb
   * @param candidates the candidates
   * @param category the category
   * @return true, if successful
   */
  private boolean applyWithRule(String sctid, List<IcdMap> icd10Map,
    List<IcdMap> icd11MapOrig, StringBuilder noteSb,
    Map<String, Double> candidates, Category[] category) {

    final List<IcdMap> icd11Map = new ArrayList<>();

    // If nomap, pass on this
    if (category[0] == Category.NO_MAP) {
      return false;
    }

    // For SNOMED concepts containing two parts (via " with " or "
    // associated
    // with ")
    // where ICD10 has a single code
    // And there are two candidates with > 1.5 codes (and exactly 2)
    // then rewrite to assign the 2nd highest ranking also as a second code.
    String withCode = null;
    String withName = null;
    boolean override = false;
    double score = 0;
    if ((sctConcepts.get(sctid).toLowerCase().contains(" with ")
        || sctConcepts.get(sctid).toLowerCase().contains(" associated with "))
        && icd10Map.size() == 1 && icd11MapOrig.size() == 1) {

      final String targetId = icd11MapOrig.iterator().next().getMapTarget();
      for (final String key : candidates.keySet()) {
        if (key.equals(targetId)) {
          continue;
        }
        if (candidates.get(key) >= 1.5) {
          if (withCode == null) {
            withCode = key;
            withName = icd11Concepts.get(withCode);
          } else {
            // too many matching things, bail
            withCode = null;
            break;
          }
        }
      }

      if (withCode != null) {
        // don't increase score: score += 2.0;
        override = true;
      }
    }

    // => Add a second icd11 map entry at the end for the XT code
    // => Add a note override
    // => Bump category by 1 position
    // => return true
    if (override) {
      final IcdMap origMap = icd11MapOrig.iterator().next();
      icd11Map.add(origMap);
      final IcdMap withMap = new IcdMap(origMap);
      withMap.setMapTarget(withCode);
      withMap.setMapGroup(2);
      icd11Map.add(withMap);
      noteSb.append("\nOVERRIDE " + category[0].toString() + ": (" + score
          + "): WITH Rule - " + withName + "\n");

      icd11MapOrig.clear();
      icd11MapOrig.addAll(icd11Map);
    }

    return override;
  }

  /**
   * Apply with diabetes.
   *
   * @param sctid the sctid
   * @param icd10Map the icd 10 map
   * @param icd11MapOrig the icd 11 map orig
   * @param noteSb the note sb
   * @param candidates the candidates
   * @param category the category
   * @return true, if successful
   */
  private boolean applyDiabetesRule(String sctid, List<IcdMap> icd10Map,
    List<IcdMap> icd11MapOrig, StringBuilder noteSb,
    Map<String, Double> candidates, Category[] category) {

    final List<IcdMap> icd11Map = new ArrayList<>();

    // If nomap, pass on this
    if (category[0] == Category.NO_MAP) {
      return false;
    }

    // For SNOMED concepts with diabetes, use first code AS:
    // 1697306310 5A14 : Diabetes mellitus, type unspecified OR
    // 119724091||5A11 : Type 2 diabetes mellitus OR
    // 1651053999|5A10 : Type 1 diabetes mellitus

    final String sctName = sctConcepts.get(sctid).toLowerCase();
    if ((sctName.contains("diabetic") || sctName.contains("diabetes"))) {

      // ICD11 code for type unknown diabetes mellitus
      String diabetesCode = "1697306310";
      // ICD11 code for type 2 diabetes mellitus
      if (sctName.contains("type 2")) {
        diabetesCode = "119724091";
      } else if (sctName.contains("type 1")) {
        // ICD11 code for type 1 diabetes mellitus
        diabetesCode = "1651053999";
      }

      // See if the icd11Map contains this code already
      boolean found = false;
      int foundCt = 0;
      for (final IcdMap map : icd11MapOrig) {
        foundCt++;
        if (map.getMapTarget().equals(diabetesCode)) {
          found = true;
          break;
        }
      }

      // if no diabetes code, add one
      if (!found) {
        // => Add a first icd11 map entry for this code
        // => Add a note override
        // => return true
        final IcdMap origMap = icd11MapOrig.iterator().next();
        final IcdMap withMap = new IcdMap(origMap);
        withMap.setMapTarget(diabetesCode);
        withMap.setMapGroup(1);
        icd11Map.add(withMap);
        int groupCt = 1;
        for (final IcdMap map : icd11MapOrig) {
          groupCt++;
          map.setMapGroup(groupCt);
          icd11Map.add(map);
        }
        noteSb.append("\n\nOVERRIDE <n/a>: " + ": Diabetes rule\n");
      }

      // else if not first, make it first
      else if (foundCt > 1) {
        int groupCt = 1;
        // Add diabetes code first
        for (final IcdMap map : icd11MapOrig) {
          if (map.getMapTarget().equals(diabetesCode)) {
            map.setMapGroup(groupCt);
            icd11Map.add(map);
          }
        }
        // Add other codes next
        for (final IcdMap map : icd11MapOrig) {
          if (!map.getMapTarget().equals(diabetesCode)) {
            groupCt++;
            map.setMapGroup(groupCt);
            icd11Map.add(map);
          }
        }
      } else {
        icd11Map.addAll(icd11MapOrig);
      }

    } else {
      return false;
    }

    icd11MapOrig.clear();
    icd11MapOrig.addAll(icd11Map);

    return true;
  }

  /**
   * Fix advice.
   *
   * @param map11 the map 11
   * @param targetId the target id
   */
  private void fixAdvice(IcdMap map11, String targetId) {
    if (map11.getMapAdvice().startsWith("ALWAYS")) {
      map11.setMapAdvice(map11.getMapAdvice().replaceAll("ALWAYS ([^\\s])+",
          "ALWAYS " + targetId));
    } else {
      map11.setMapAdvice("ALWAYS " + targetId);
    }

    // Remove all ICD10 excluded advices
    for (final String adviceToRemove : advicesToExclude) {
      map11
          .setMapAdvice(getWithoutAdvice(map11.getMapAdvice(), adviceToRemove));
    }
  }

  /**
   * Fix and clear advice.
   *
   * @param map11 the map 11
   * @param targetId the target id
   */
  @SuppressWarnings("static-method")
  private void fixAndClearAdvice(IcdMap map11, String targetId) {
    if (map11.getMapAdvice().startsWith("ALWAYS")) {
      map11.setMapAdvice("ALWAYS " + targetId);
    }
  }

  /**
   * Removes the advice.
   *
   * @param allAdvice the all advice
   * @param adviceToRemove the advice to remove
   * @return the string
   */
  @SuppressWarnings("static-method")
  private String getWithoutAdvice(String allAdvice, String adviceToRemove) {
    return allAdvice.replace(adviceToRemove, "").replace(" \\|  \\| ", " \\| ")
        .replace(" \\| $", "");
  }

  /**
   * Apply xa neoplasm rule.
   *
   * @param sctid the sctid
   * @param icd10Map the icd 10 map
   * @param icd11MapOrig the icd 11 map orig
   * @param noteSb the note sb
   * @param candidates the candidates
   * @param category the category
   * @return true, if successful
   */
  private boolean applyXaNeoplasmRule(String sctid, List<IcdMap> icd10Map,
    List<IcdMap> icd11MapOrig, StringBuilder noteSb,
    Map<String, Double> candidates, Category[] category) {

    final List<IcdMap> icd11Map = new ArrayList<>();

    // If nomap, pass on this
    if (category[0] == Category.NO_MAP) {
      return false;
    }

    // For descendants of 55342001 | Neoplastic disease (disorder) |
    // AND icd10Map has a single entry for the concept
    // AND icd11Map has an single entry with a stem code (starting with 2)
    // and
    // score >= 1.5
    // AND there is an XA candidate with a score > .5
    String xaCode = null;
    boolean override = false;
    double score = 0;
    if (sctAncDesc.get("55342001").contains(sctid) && icd10Map.size() == 1
        && icd11MapOrig.size() == 1) {
      final String targetId = icd11MapOrig.iterator().next().getMapTarget();
      final String targetCode = icd11Concepts.get(targetId);
      if (targetCode != null && targetCode.startsWith("2")
          && candidates.get(targetId) >= 1.5) {
        score += candidates.get(targetId);
        for (final String otherTargetId : candidates.keySet()) {
          final String otherTargetCode = icd11Concepts.get(otherTargetId);
          if (otherTargetCode != null && otherTargetCode.startsWith("XA")
              && candidates.get(otherTargetId) > .5) {
            score += candidates.get(otherTargetId);
            xaCode = otherTargetId;
            break;
          }
        }
        if (xaCode != null) {
          override = true;
        }
      }
    }

    // => Add a second icd11 map entry for the XA
    // => Add a note override
    // => Recompute category as MEDIUM or HIGH
    // => return true
    if (override) {
      final IcdMap origMap = icd11MapOrig.iterator().next();
      icd11Map.add(origMap);
      final IcdMap xaMap = new IcdMap(origMap);
      xaMap.setMapTarget(xaCode);
      xaMap.setMapPriority(2);
      icd11Map.add(xaMap);
      fixAndClearAdvice(xaMap, xaCode);
      if (score > 3.0) {
        category[0] = Category.HIGH;
      } else {
        category[0] = Category.MEDIUM;
      }
      noteSb.append("\nOVERRIDE " + category[0].toString() + ": (" + score
          + "): XA Neoplasm Rule (added xa code for body part)\n");

      icd11MapOrig.clear();
      icd11MapOrig.addAll(icd11Map);
    }

    return override;
  }

  /**
   * Load data resources.
   *
   * @param icd11Dir the icd 11 dir
   * @throws Exception the exception
   */
  private void loadDataResources(String icd11Dir) throws Exception {

    //
    // Cache SNOMED concepts
    // sctid|name|ptFlag
    //
    Logger.getLogger(getClass()).info(" Load SNOMED concepts");
    List<String> lines =
        FileUtils.readLines(new File(icd11Dir, "sctScopeDesc.txt"), "UTF-8");
    int ct = 0;
    int skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      // skip descriptions
      if (fields[2].equals("0")) {
        continue;
      }
      final String code = fields[0];
      sctConcepts.put(code, fields[1]);
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(code + " => " + fields[1]);
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    // Cache ICD11 map (by SCTID)
    Logger.getLogger(getClass()).info(" Load ICD11 map edits");
    lines =
        FileUtils.readLines(new File(icd11Dir, "icd11MapEdits.txt"), "UTF-8");
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
      if (!icd11MapEdits.containsKey(fields[5])) {
        icd11MapEdits.put(fields[5], new ArrayList<IcdMap>());
      }
      icd11MapEdits.get(fields[5]).add(new IcdMap(line));
      if (ct < sampleCt) {
        Logger.getLogger(getClass())
            .debug(fields[5] + " => " + icd11MapEdits.get(fields[5]));
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD11 concepts (by id) - noheader
    // entityCode|code: name
    //
    Logger.getLogger(getClass()).info(" Load ICD11 concepts");
    lines =
        FileUtils.readLines(new File(icd11Dir, "icd11Concepts.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      // entityCode already has URL stripped -
      // http://id.who.int/icd/entity
      final String code = fields[0];
      icd11Concepts.put(code, fields[1]);
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(code + " => " + fields[1]);
      }
      // Add single word Xt concepts
      if (fields[1].startsWith("XA")) {
        if (fields[1].lastIndexOf(" ") == (fields[1].indexOf(":") + 1)) {
          Logger.getLogger(getClass()).info("  XA = "
              + fields[1].substring(fields[1].indexOf(":") + 2).toLowerCase());
          xtConcepts.put(
              fields[1].substring(fields[1].indexOf(":") + 2).toLowerCase(),
              fields[0]);
        }
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD11 index entries id -> Set of entries
    // Id FoundationLocationId Code Title IndexTerm Version:Jan 31 - 23:00
    // UTC
    //
    Logger.getLogger(getClass()).info(" Load ICD11 index");
    lines = FileUtils.readLines(
        new File(icd11Dir, "LinearizationIndexTabulation-Morbidity-en.txt"),
        "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\t");
      // Strip - http://id.who.int/icd/entity
      final String code =
          fields[0].replace("http://id.who.int/icd/entity/", "");
      if (!icd11Index.containsKey(code)) {
        icd11Index.put(code, new HashSet<String>());
      }
      icd11Index.get(code).add(fields[4].replaceAll("\"", "").toLowerCase());
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(
            code + " => " + fields[4].replaceAll("\"", "").toLowerCase());
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD11 scope (by id) - noheader
    // entityCode
    //
    Logger.getLogger(getClass()).info(" Load ICD11 scope");
    lines = FileUtils.readLines(new File(icd11Dir, "icd11Scope.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      // entityCode already has URL stripped -
      // http://id.who.int/icd/entity
      final String code = line;
      if (code.equals("300B")) {
        continue;
      }
      icd11Scope.add(code);

      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(code);
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    // Consider removing from scope anything that has leaf nodes below (not
    // just .y .z)

    //
    // Cache ICD10 concepts (by code) - noheader (from DB)
    //
    Logger.getLogger(getClass()).info(" Load ICD10 concepts");
    lines =
        FileUtils.readLines(new File(icd11Dir, "icd10Concepts.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {

      final String[] fields = line.split("\t");
      final String code = fields[0];
      icd10Concepts.put(code, fields[1]);
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(code + " => " + fields[1]);
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD10 scope (by code) - noheader
    // code
    //
    Logger.getLogger(getClass()).info(" Load ICD10 scope");
    lines = FileUtils.readLines(new File(icd11Dir, "icd10Scope.txt"), "UTF-8");
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
    // sctid
    //
    Logger.getLogger(getClass()).info(" Load SCT scope");
    lines = FileUtils.readLines(new File(icd11Dir, "sctScope.txt"), "UTF-8");
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
    // Cache starter set
    //
    Logger.getLogger(getClass()).info(" Load SCT starterSet");
    lines = FileUtils.readLines(new File(icd11Dir, "starterSet.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      starterSet.add(line);
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(line);
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache SCT ancDesc and descAnc - noheader
    // anc|desc|depth
    //
    Logger.getLogger(getClass()).info(" Load SCT anc/desc (for scope)");
    lines =
        FileUtils.readLines(new File(icd11Dir, "sctScopeAncDesc.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      // skip if if self-row
      if (fields[2].equals("0")) {
        skipCt++;
        // no need to log this, it is very common and verified
        continue;
      }
      if (!sctAncDesc.containsKey(fields[0])) {
        sctAncDesc.put(fields[0], new HashSet<String>());
      }
      sctAncDesc.get(fields[0]).add(new Depth(line, true).getCode());

      if (!sctDescAnc.containsKey(fields[1])) {
        sctDescAnc.put(fields[1], new HashSet<String>());
        sctChdPar.put(fields[1], new HashSet<String>());
      }
      sctDescAnc.get(fields[1]).add(new Depth(line, false).getCode());
      if (fields[2].equals("1")) {
        sctChdPar.get(fields[1]).add(fields[0]);
      }
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
    // Cache sctScopeBodyPartMap.txt
    // 10065003|foot
    //
    Logger.getLogger(getClass()).info(" Load SCT body part map");
    lines = FileUtils.readLines(new File(icd11Dir, "sctScopeBodyPartMap.txt"),
        "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      sctBodyPartMap.put(fields[0], fields[1]);
      if (!bodyPartSctMap.containsKey(fields[1])) {
        bodyPartSctMap.put(fields[1], new HashSet<String>());
      }
      bodyPartSctMap.get(fields[1]).add(fields[0]);

      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);

    //
    // Cache icd11BodyPartMap.txt
    // 545442245/morbidity/other|foot
    //
    Logger.getLogger(getClass()).info(" Load ICD11 body part map");
    lines = FileUtils.readLines(new File(icd11Dir, "icd11BodyPartMap.txt"),
        "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      icd11BodyPartMap.put(fields[0], fields[1]);
      if (!bodyPartIcd11Map.containsKey(fields[1])) {
        bodyPartIcd11Map.put(fields[1], new HashSet<String>());
      }
      bodyPartIcd11Map.get(fields[1]).add(fields[0]);
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);

    //
    // Cache ICD11 PAR/CHD
    // par|chd
    //
    Logger.getLogger(getClass()).info(" Load ICD11 par/chd (for scope)");
    lines = FileUtils.readLines(new File(icd11Dir, "icd11ParChd.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      // Skip if out of scope
      if (!icd11Scope.contains(fields[1])) {
        skipCt++;
        continue;
      }
      if (!icd11ParChd.containsKey(fields[0])) {
        icd11ParChd.put(fields[0], new HashSet<String>());
      }
      icd11ParChd.get(fields[0]).add(fields[1]);

      if (ct < sampleCt) {
        Logger.getLogger(getClass())
            .debug(fields[0] + " => CHD " + icd11ParChd.get(fields[0]));
      }

      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    // Remove scope concepts if they have children
    Logger.getLogger(getClass())
        .info("  Remove icd11 scope concepts that have children");
    ct = 0;
    for (final String par : icd11ParChd.keySet()) {
      // keep things of a specified length
      final String code = icd11Concepts.get(par);
      if (code.contains(":")
          && code.substring(0, code.indexOf(':') - 1).length() >= 6) {
        continue;
      }
      if (icd11ParChd.get(par).size() > 0) {
        ct++;
        icd11Scope.remove(par);
      }
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);

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
    Logger.getLogger(getClass()).info(" Load SCT-ICD11 equivalence (from DR)");
    lines = FileUtils.readLines(new File(icd11Dir, "sctIcd11Equivalence.txt"),
        "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\t");
      final String sctid = fields[1];
      // Skip if the snomed concept is out of scope
      if (!sctConcepts.containsKey(sctid)) {
        continue;
      }
      // Skip header (no skipCt++)
      if (fields[2].equals("mms")) {
        // skipCt++;
        continue;
      }
      // // Skip if "MMS" flag != Y
      // if (!fields[2].equals("Y")) {
      // skipCt++;
      // Logger.getLogger(getClass()).debug("SKIP: " + line);
      // continue;
      // }
      sctIcd11Equivalence.put(sctid, new HashSet<String>());
      final String code = fields[0];
      if (icd11Scope.contains(code)) {
        sctIcd11Equivalence.get(sctid).add(code);
        sctIcd11EquivalenceType.put(sctid, fields[3]);
        if (ct < sampleCt) {
          Logger.getLogger(getClass())
              .debug(sctid + " => " + sctIcd11Equivalence.get(sctid) + ", "
                  + sctIcd11EquivalenceType.get(sctid));
        }
      } else if (icd11ParChd.containsKey(code)) {
        for (final String chdCode : icd11ParChd.get(code)) {
          sctIcd11Equivalence.get(sctid).add(chdCode);
          sctIcd11EquivalenceType.put(sctid, fields[3]);
        }
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD10-11 matches - noheader
    // 10065003|545442245/morbidity/other|12.5|Excoriated acne|Excoriated
    // acne
    //
    Logger.getLogger(getClass()).info(" Load SCT-ICD11 lexical matches");
    lines =
        FileUtils.readLines(new File(icd11Dir, "sctIcd11Matches.txt"), "UTF-8");
    ct = 0;
    skipCt = 0;
    for (final String line : lines) {
      final String[] fields = line.split("\\|");
      final Score score = new Score(line);
      final String code =
          fields[0].replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
      if (!sctIcd11Matches.containsKey(fields[0])) {
        sctIcd11Matches.put(code, new ArrayList<Score>());
      }
      sctIcd11Matches.get(code).add(score);
      if (ct < sampleCt) {
        Logger.getLogger(getClass())
            .debug(code + " => " + sctIcd11Matches.get(code));
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    Logger.getLogger(getClass()).info(" Load ICD10-11 mappings");
    // // 10To11MapToOneCategory.txt - this is redundant in MultipleCategory
    // file
    // lines = FileUtils
    // .readLines(new File(icd11Dir, "10To11MapToOneCategory.txt"),
    // "UTF-8");
    // ct = 0;
    // skipCt = 0;
    // for (final String line : lines) {
    // final String[] fields = line.split("\\t");
    // // Skip header (no skipct++)
    // if (fields[0].equals("10ClassKind")) {
    // // skipCt++;
    // continue;
    // }
    // final WhoMap map = new WhoMap(line);
    // final String code = map.getCode();
    // // Skip cases where the code or target code are not in scope
    // if (!icd10Scope.contains(map.getCode())) {
    // skipCt++;
    // Logger.getLogger(getClass())
    // .debug("SKIP (scope " + map.getCode() + "): " + line);
    // continue;
    // }
    // // AVOID this, let RULE2 look for descendants
    // // if (!map.getTargetCode().equals("No Mapping")
    // // && !icd11Scope.contains(map.getTargetCode())) {
    // // skipCt++;
    // // Logger.getLogger(getClass())
    // // .debug("SKIP (scope " + map.getTargetCode() + "): " + line);
    // // continue;
    // // }
    // if (!icd10To11.containsKey(code)) {
    // icd10To11.put(code, new HashSet<WhoMap>());
    // }
    // icd10To11.get(code).add(map);
    // if (ct < sampleCt) {
    // Logger.getLogger(getClass()).debug(code + " => " +
    // icd10To11.get(code));
    // }
    // ct++;
    // }
    // Logger.getLogger(getClass()).info(" ct = " + ct);
    // Logger.getLogger(getClass()).info(" skipCt = " + skipCt);

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
      final WhoMap map = new WhoMap(line);
      final String code = map.getCode();

      // Skip cases where the code or target code are not in scope
      if (!icd10Scope.contains(map.getCode())) {
        skipCt++;
        Logger.getLogger(getClass())
            .debug("SKIP (scope " + map.getCode() + ") ");// +
                                                          // line);
        continue;
      }
      // AVOID this part, allow RULE2 to look for child codes
      // if (!map.getTargetCode().equals("No Mapping")
      // && !icd11Scope.contains(map.getTargetCode())) {
      // skipCt++;
      // Logger.getLogger(getClass())
      // .debug("SKIP (scope " + map.getTargetCode() + ") ");// + line);
      // continue;
      // }
      if (!icd10To11.containsKey(code)) {
        icd10To11.put(code, new HashSet<WhoMap>());
      }
      icd10To11.get(code).add(map);
      if (ct < sampleCt) {
        Logger.getLogger(getClass()).debug(code + " => " + icd10To11.get(code));
      }
      ct++;
    }
    Logger.getLogger(getClass()).info("   ct = " + ct);
    Logger.getLogger(getClass()).info("   skipCt =  " + skipCt);

    //
    // Cache ICD10-11 mappings (provided in frozen release) - HEADER
    //
    Logger.getLogger(getClass()).info(" Load ICD11-ICD10 mappings");
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

      final WhoMap map = new WhoMap(line);
      final String code = map.getCode();
      // Skip cases where the code or target code are not in scope
      if (!icd11Scope.contains(map.getCode())) {
        skipCt++;
        Logger.getLogger(getClass())
            .debug("SKIP (scope " + map.getCode() + ") ");// +
                                                          // line);
        continue;
      }
      if (!map.getTargetCode().equals("No Mapping")
          && !icd10Scope.contains(map.getTargetCode())) {
        skipCt++;
        Logger.getLogger(getClass())
            .debug("SKIP (scope " + map.getTargetCode() + ") ");// +
                                                                // line);
        continue;
      }
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

    /**
     * To string.
     *
     * @return the string
     */
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

    /** The name. */
    private String name;

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
      // 10065003|545442245/morbidity/other|12.5|Excoriated
      // acne|Excoriated acne
      final String[] fields = line.split("\\|");
      code = fields[1];
      score = Double.valueOf(fields[2]);
      name = fields[4];
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

    /**
     * To string.
     *
     * @return the string
     */
    /* see superclass */
    @Override
    public String toString() {
      return "Score [code=" + code + ", score=" + score + "]";
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

  }

  /**
   * An entry from an ICD 10-11 or 11-10 map.
   */
  class WhoMap {

    /** The kind. */
    private String kind;

    /** The depth in kind. */
    private int depthInKind;

    /** The code. */
    private String code;

    /** The chapter. */
    private String chapter;

    /** The name. */
    private String name;

    /** The target kind. */
    private String targetKind;

    /** The target depth in kind. */
    private int targetDepthInKind;

    /** The target code. */
    private String targetCode;

    /** The target chapter. */
    private String targetChapter;

    /** The chapter match. */
    private boolean chapterMatch = false;

    /** The target name. */
    private String targetName;

    /** The relation. */
    private String relation;

    /** The distance. */
    private String distance;

    /** The issue type. */
    private String issueType;

    /** The match type. */
    private String matchType;

    // 5128 EquivalentMatch
    // 1 Match Type
    // 302 MatchElseWhere
    // 92 NoMapping
    // 546 NoMappingDueToMaptoGroupingWithoutResidual
    // 2279 ToBroaderInTheSame3Char
    // 1792 ToBroaderInTheSameBlock
    // 2344 ToBroaderInTheSameChapter

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
      if (fields.length == 17 || fields.length == 18 || fields.length == 15) {
        // 0 - 10ClassKind
        // 1 - 10DepthInKind
        // 2 - icd10Code
        // 3 - icd10Chapter
        // 4 - icd10Title
        // 5 - 11ClassKind
        // 6 - 11DepthInKind
        // 7 - icd11Id
        // 8 - icd11Code
        // 9 - icd11Chapter
        // 10 - chapterMatch
        // 11 - icd11Title
        // 12 - Relation
        // 13 - Linearization
        // 14 - StatementDistance
        // 15 - IssueType
        // 16 - MatchType
        // 17 - 2017-Mar-24
        kind = fields[0];
        depthInKind = Integer.parseInt(fields[1]);
        code = fields[2].replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/",
            "");
        chapter = fields[3];
        name = fields[4];
        targetKind = fields[5];
        targetCode = fields[7]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");
        if (!targetCode.equals("No Mapping")) {
          targetDepthInKind = Integer.parseInt(fields[6]);
          // skip fields[8]
          targetChapter = fields[9];
          chapterMatch = "True".equals(fields[10]);
          targetName = fields[11];
          relation = fields[12];
          distance = fields[14];
          issueType = "";
          issueType = fields[15];
          matchType = fields[16];
        }

      } else if (fields.length == 10 || fields.length == 11) {
        // 0 - icd11Id
        // 1 - icd11Code
        // 2 - icd11Chapter
        // 3 - icd11Title
        // 4 - icd10Code
        // 5 - icd10Chapter
        // 6 - chapterMatch
        // 7 - icd10Title
        // 8 - Relation
        // 9 - Stated{0}|Deduced
        // 10 - 2015-May-31
        // http://id.who.int/icd/entity/588616678 01 Gastroenteritis and
        // colitis
        // of infectious origin A00-A09 I True Intestinal infectious
        // diseases
        // Equival
        code = fields[0].replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/",
            "");
        // skip fields[1]
        chapter = fields[2];
        name = fields[3];
        targetCode = fields[4]
            .replaceAll("http\\:\\/\\/id.who.int\\/icd\\/entity\\/", "");

        targetChapter = fields[5];
        chapterMatch = "True".equals(fields[6]);
        targetName = fields[7];
        relation = fields[8];
        stated = fields[9].equals("0");
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

    /**
     * To string.
     *
     * @return the string
     */
    /* see superclass */
    @Override
    public String toString() {
      return "WhoMap [kind=" + kind + ", depthInKind=" + depthInKind + ", code="
          + code + ", chapter=" + chapter + ", name=" + name + ", targetKind="
          + targetKind + ", targetDepthInKind=" + targetDepthInKind
          + ", targetCode=" + targetCode + ", targetChapter=" + targetChapter
          + ", chapterMatch=" + chapterMatch + ", targetName=" + targetName
          + ", relation=" + relation + ", distance=" + distance + ", issueType="
          + issueType + ", matchType=" + matchType + ", stated=" + stated + "]";
    }

    /**
     * Returns the depth in kind.
     *
     * @return the depth in kind
     */
    public int getDepthInKind() {
      return depthInKind;
    }

    /**
     * Sets the depth in kind.
     *
     * @param depthInKind the depth in kind
     */
    public void setDepthInKind(int depthInKind) {
      this.depthInKind = depthInKind;
    }

    /**
     * Returns the chapter.
     *
     * @return the chapter
     */
    public String getChapter() {
      return chapter;
    }

    /**
     * Sets the chapter.
     *
     * @param chapter the chapter
     */
    public void setChapter(String chapter) {
      this.chapter = chapter;
    }

    /**
     * Returns the target depth in kind.
     *
     * @return the target depth in kind
     */
    public int getTargetDepthInKind() {
      return targetDepthInKind;
    }

    /**
     * Sets the target depth in kind.
     *
     * @param targetDepthInKind the target depth in kind
     */
    public void setTargetDepthInKind(int targetDepthInKind) {
      this.targetDepthInKind = targetDepthInKind;
    }

    /**
     * Returns the target chapter.
     *
     * @return the target chapter
     */
    public String getTargetChapter() {
      return targetChapter;
    }

    /**
     * Sets the target chapter.
     *
     * @param targetChapter the target chapter
     */
    public void setTargetChapter(String targetChapter) {
      this.targetChapter = targetChapter;
    }

    /**
     * Indicates whether or not chapter match is the case.
     *
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    public boolean isChapterMatch() {
      return chapterMatch;
    }

    /**
     * Sets the chapter match.
     *
     * @param chapterMatch the chapter match
     */
    public void setChapterMatch(boolean chapterMatch) {
      this.chapterMatch = chapterMatch;
    }

    /**
     * Returns the match type.
     *
     * @return the match type
     */
    public String getMatchType() {
      return matchType;
    }

    /**
     * Sets the match type.
     *
     * @param matchType the match type
     */
    public void setMatchType(String matchType) {
      this.matchType = matchType;
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
      this.mapAdvice = fields[9];
      // fields[9].replaceAll("ALWAYS [A-Z\\.0-9]+( \\|)?", "").replace(
      // "MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
      // "");
      this.mapTarget = fields[10];
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

    /**
     * To string.
     *
     * @return the string
     */
    /* see superclass */
    @Override
    public String toString() {
      return "IcdMap [mapGroup=" + mapGroup + ", mapPriority=" + mapPriority
          + ", mapRule=" + mapRule + ", mapAdvice=" + mapAdvice + ", mapTarget="
          + mapTarget + ", mapCategoryId=" + mapCategoryId + "]";
    }

    /**
     * Returns the concept id.
     *
     * @return the concept id
     */
    public String getConceptId() {
      return conceptId;
    }

    /**
     * Sets the concept id.
     *
     * @param conceptId the concept id
     */
    public void setConceptId(String conceptId) {
      this.conceptId = conceptId;
    }
  }

  /**
   * Class for representing rule details.
   */
  class RuleDetails {

    /** The number. */
    private int number;

    /** The nc score. */
    private double ncScore = 0;

    /** The score map. */
    Map<String, Double> scoreMap = new HashMap<>(5);

    /** The type map. */
    private Map<String, String> typeMap = new HashMap<>(5);

    /**
     * Instantiates an empty {@link RuleDetails}.
     *
     * @param number the number
     */
    public RuleDetails(int number) {
      this.number = number;
    }

    /**
     * Returns the number.
     *
     * @return the number
     */
    public int getNumber() {
      return number;
    }

    /**
     * Sets the number.
     *
     * @param number the number
     */
    public void setNumber(int number) {
      this.number = number;
    }

    /**
     * Returns the score map.
     *
     * @return the score map
     */
    public Map<String, Double> getScoreMap() {
      return scoreMap;
    }

    /**
     * Sets the score map.
     *
     * @param scoreMap the score map
     */
    public void setScoreMap(Map<String, Double> scoreMap) {
      this.scoreMap = scoreMap;
    }

    /**
     * Adds the score.
     *
     * @param code the code
     * @param score the score
     */
    public void addScore(String code, Double score) {
      if (!scoreMap.containsKey(code)) {
        scoreMap.put(code, score);
      } else {
        scoreMap.put(code, score + scoreMap.get(code));
      }
    }

    /**
     * Returns the score.
     *
     * @param code the code
     * @return the score
     */
    public double getScore(String code) {
      if (!scoreMap.containsKey(code)) {
        return 0.0;
      } else {
        return scoreMap.get(code);
      }
    }

    /**
     * Returns the type map.
     *
     * @return the type map
     */
    public Map<String, String> getTypeMap() {
      return typeMap;
    }

    /**
     * Sets the type map.
     *
     * @param typeMap the type map
     */
    public void setTypeMap(Map<String, String> typeMap) {
      this.typeMap = typeMap;
    }

    /**
     * Append type.
     *
     * @param code the code
     * @param type the type
     */
    public void appendType(String code, String type) {
      if (!typeMap.containsKey(code)) {
        typeMap.put(code, type);
      } else {
        typeMap.put(code, typeMap.get(code) + ", " + type);
      }
    }

    /**
     * Returns the nc score.
     *
     * @return the nc score
     */
    public double getNcScore() {
      return ncScore;
    }

    /**
     * Sets the nc score.
     *
     * @param ncScore the nc score
     */
    public void setNcScore(double ncScore) {
      this.ncScore = ncScore;
    }

    /**
     * Adds the nc score.
     *
     * @param ncScore the nc score
     */
    public void addNcScore(double ncScore) {
      this.ncScore += ncScore;
    }

    /**
     * Checks for candidates.
     *
     * @return true, if successful
     */
    public boolean hasCandidates() {
      return scoreMap.size() > 0;
    }

    /**
     * To note.
     *
     * @param icd11Concepts the icd 11 concepts
     * @return the string
     */
    public String toNote(Map<String, String> icd11Concepts) {
      // RULE1: 1 matches
      // 1.0 1359329403/.9 Other specified disorders of the sleep-wake
      // schedule
      //
      // RULE2: 2 matches
      // MATCH 0.8 - 2142880493/.8 Risk factors related to personal
      // history of
      // other specified health problems
      // MATCH 1.0 - 1359329403/.9 Other specified disorders of the
      // sleep-wake
      // schedule
      final StringBuilder sb = new StringBuilder();
      sb.append("RULE" + number + ": " + scoreMap.size() + " matches\n");
      for (final String key : scoreMap.keySet()) {
        sb.append("  ");
        if (typeMap.containsKey(key)) {
          sb.append(typeMap.get(key)).append(" (").append(scoreMap.get(key))
              .append(") ").append(key).append(" ");
        } else {
          sb.append(scoreMap.get(key)).append(" ").append(key).append(" ");
        }
        sb.append(icd11Concepts.get(key)).append("\n");
      }
      return sb.toString();
    }

  }

  /**
   * The Class RuleScores.
   */
  class RuleScores {

    /** The candidates. */
    private Map<String, Double> candidates = new HashMap<>();

    /** The rules. */
    private RuleDetails[] ruleDetails = null;

    /** The category. */
    private Category category = Category.UNKNOWN;

    /** The map. */
    private IcdMap map = new IcdMap();

    /**
     * Instantiates an empty {@link RuleScores}.
     */
    public RuleScores() {
      // n/a
    }

    /**
     * Returns the candidates.
     *
     * @return the candidates
     */
    public Map<String, Double> getCandidates() {
      return candidates;
    }

    /**
     * Sets the candidates.
     *
     * @param candidates the candidates
     */
    public void setCandidates(Map<String, Double> candidates) {
      this.candidates = candidates;
    }

    /**
     * Returns the category.
     *
     * @return the category
     */
    public Category getCategory() {
      return category;
    }

    /**
     * Sets the category.
     *
     * @param category the category
     */
    public void setCategory(Category category) {
      this.category = category;
    }

    /**
     * Returns the map.
     *
     * @return the map
     */
    public IcdMap getMap() {
      return map;
    }

    /**
     * Sets the map.
     *
     * @param map the map
     */
    public void setMap(IcdMap map) {
      this.map = map;
    }

    /**
     * Returns the rule details.
     *
     * @return the rule details
     */
    public RuleDetails[] getRuleDetails() {
      return ruleDetails;
    }

    /**
     * Sets the rule details.
     *
     * @param rules the rule details
     */
    public void setRuleDetails(RuleDetails[] rules) {
      this.ruleDetails = rules;
    }
  }
}
