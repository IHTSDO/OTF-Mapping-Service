package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Reference implementation of {@link ProjectSpecificAlgorithmHandler}.
 */
public class DefaultProjectSpecificAlgorithmHandler implements
    ProjectSpecificAlgorithmHandler {

  /** The map project. */
  MapProject mapProject = null;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getMapProject
   * ()
   */
  @Override
  public MapProject getMapProject() {
    return this.mapProject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#setMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void setMapProject(MapProject mapProject) {
    this.mapProject = mapProject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * computeMapAdvice (org.ihtsdo.otf.mapping.model.MapRecord,
   * org.ihtsdo.otf.mapping.model.MapEntry)
   */
  @Override
  /**
   * Given a map record and a map entry, returns any computed advice.
   * This must be overwritten for each project specific handler.
   * @param mapRecord
   * @return
   */
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return null;
  }

  /**
   * Given a map record and a map entry, returns the computed map relation (if
   * applicable) This must be overwritten for each project specific handler.
   * 
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return computed map relation
   * @throws Exception the exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#validateRecord
   * (org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public ValidationResult validateRecord(MapRecord mapRecord) throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    validationResult.merge(performUniversalValidationChecks(mapRecord));
    validationResult.merge(validateTargetCodes(mapRecord));

    return validationResult;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * validateTargetCodes(org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  /**
   * This must be overwritten for each project specific handler
   */
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception {
    return new ValidationResultJpa();
  }

  /**
   * Perform universal validation checks.
   * 
   * @param mapRecord the map record
   * @return the validation result
   */
  public ValidationResult performUniversalValidationChecks(MapRecord mapRecord) {
    Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);

    ValidationResult validationResult = new ValidationResultJpa();

    // FATAL ERROR: map record has no entries
    if (mapRecord.getMapEntries().size() == 0) {
      validationResult.addError("Map record has no entries");
      return validationResult;
    }

    // FATAL ERROR: multiple map groups present for a project without group
    // structure
    if (!mapProject.isGroupStructure() && entryGroups.keySet().size() > 1) {
      validationResult
          .addError("Project has no group structure but multiple map groups were found.");
      return validationResult;
    }

    /*
     * Verify that groups begin at index 1 and are sequential (i.e. no empty
     * groups)
     */
    validationResult
        .merge(checkMapRecordGroupStructure(mapRecord, entryGroups));

    /*
     * Group validation checks â€¢ Verify the last entry in a group is a TRUE
     * rule â€¢ Verify higher map groups do not have only NC nodes
     */

    // Validation Check: verify correct positioning of TRUE rules
    validationResult.merge(checkMapRecordRules(mapRecord, entryGroups));

    // Validation Check: very higher map groups do not have only NC nodes
    validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));

    /*
     * Entry Validation Checks 
     * Verify no duplicate entries in record 
     * Verify advice values are valid for the project (this can happen if
     * allowable map advice changes without updating map entries) 
     * Entry must have target code that is both in the target terminology and
     * valid (e.g. leaf nodes) OR have a relationId corresponding to a valid map
     * category
     */

    // Validation Check: verify entries are not duplicated
    validationResult.merge(checkMapRecordForDuplicateEntries(mapRecord));

    // Validation Check: verify advice values are valid for the project
    // (this
    // can happen if â€œallowable map adviceâ€� changes without
    // updating map
    // entries)
    validationResult.merge(checkMapRecordAdvices(mapRecord, entryGroups));

    // Validation Check: all entries are non-null (empty entries are empty
    // strings)
    validationResult.merge(checkMapRecordForNullTargetIds(mapRecord));

    return validationResult;
  }

  // ////////////////////
  // HELPER FUNCTIONS //
  // ////////////////////

  /**
   * Check map record for null target ids.
   * 
   * @param mapRecord the map record
   * @return the validation result
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForNullTargetIds(MapRecord mapRecord) {
    ValidationResult validationResult = new ValidationResultJpa();

    for (MapEntry me : mapRecord.getMapEntries()) {
      if (me.getTargetId() == null)
        validationResult.addError("Map entry at group " + me.getMapGroup()
            + ", priority " + me.getMapPriority()
            + " has no target (valid or empty) selected.");
    }

    return validationResult;
  }

  /**
   * Check map record group structure.
   * 
   * @param mapRecord the map record
   * @param entryGroups the entry groups
   * @return the validation result
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordGroupStructure(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    ValidationResult validationResult = new ValidationResultJpa();

    // get the list of groups
    Set<Integer> mapGroups = entryGroups.keySet();

    // cycle over the expected group numbers
    for (int i = 1; i <= mapGroups.size(); i++) {
      if (!mapGroups.contains(i)) {
        validationResult.addError("Group " + i + " is empty");
      }
    }

    return validationResult;
  }

  // ///////////////////////////////////////////////////////
  // Map Record Validation Checks and Helper Functions
  // ///////////////////////////////////////////////////////

  /**
   * Function to check a map record for duplicate entries within map groups.
   * 
   * @param mapRecord the map record
   * @return a list of errors detected
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForDuplicateEntries(MapRecord mapRecord) {

    // Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
    // .info("  Checking map record for duplicate entries within map groups...");

    ValidationResult validationResult = new ValidationResultJpa();
    List<MapEntry> entries = mapRecord.getMapEntries();

    // cycle over all entries but last
    for (int i = 0; i < entries.size() - 1; i++) {

      // cycle over all entries after this one
      // NOTE: separated boolean checks for easier handling of possible
      // null
      // relations
      for (int j = i + 1; j < entries.size(); j++) {

        // if first entry target null
        if (entries.get(i).getTargetId() == null) {

          // if both null, check relations
          if (entries.get(j).getTargetId() == null) {

            if (entries.get(i).getMapRelation() != null
                && entries.get(j).getMapRelation() != null
                && entries.get(i).getMapRelation()
                    .equals(entries.get(j).getMapRelation())) {
              validationResult
                  .addError("Duplicate entries (null target code, same map relation) found: "
                      + "Group "
                      + Integer.toString(entries.get(i).getMapGroup())
                      + ", priority "
                      + entries.get(i).getMapPriority()
                      + " and "
                      + "Group "
                      + Integer.toString(entries.get(j).getMapGroup())
                      + ", priority " + entries.get(j).getMapPriority());
            }

          }
        } else {

          // check if second entry's target identical to this one
          if (entries.get(i).getTargetId().equals(entries.get(j).getTargetId())) {
            validationResult
                .addError("Duplicate entries (same target code) found: "
                    + "Group " + Integer.toString(entries.get(i).getMapGroup())
                    + ", priority " + Integer.toString(i) + " and " + "Group "
                    + Integer.toString(entries.get(j).getMapGroup())
                    + ", priority " + Integer.toString(j));
          }
        }

      }
    }

    for (String error : validationResult.getErrors()) {
      Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
          "    " + error);
    }

    return validationResult;
  }

  /**
   * Function to check proper use of TRUE rules and presence of rules on
   * non-rule based projects.
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  public ValidationResult checkMapRecordRules(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    // Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
    // "  Checking map record for proper use of TRUE rules...");

    ValidationResult validationResult = new ValidationResultJpa();

    // if not rule based, check for rules present
    if (!mapProject.isRuleBased()) {

      for (MapEntry me : mapRecord.getMapEntries()) {
        if (me.getRule() != null && !me.getRule().isEmpty()) {
          validationResult
              .addError("Rule found for non-rule based project at map group "
                  + me.getMapGroup() + ", priority " + me.getMapPriority()
                  + ", rule specified is " + me.getRule() + ".");
        }
      }

    // otherwise check TRUE rules and gender rules (Female must be before Male)
    } else {

      // cycle over the groups and note if there are both female and 
      // male entries in a group
      for (Integer key : entryGroups.keySet()) {
        
        int maleEntry = 0;
        int femaleEntry = 0;
        
        for (int i=1; i<entryGroups.get(key).size() + 1; i++) {
          MapEntry entry = entryGroups.get(key).get(i-1);
          if (entry.getRule().contains("Male")) {
            maleEntry = i;
          }
          if (entry.getRule().contains("Female")) {
            femaleEntry = i;
          }
        }
        
        // ensure female entry is before male entry
        if (femaleEntry != 0 && maleEntry != 0) {
          if (femaleEntry > maleEntry) 
            validationResult
            .addError("Female rule must be ordered before the male rule.");
        }
      }
      
      // cycle over the groups
      for (Integer key : entryGroups.keySet()) {
        
        for (MapEntry mapEntry : entryGroups.get(key)) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "    Checking entry "
                  + Integer.toString(mapEntry.getMapPriority()));

          // add message if TRUE rule found at non-terminating entry
          if (mapEntry.getMapPriority() != entryGroups.get(key).size()
              && mapEntry.getRule().equals("TRUE")) {
            validationResult
                .addError("Found non-terminating entry with TRUE rule."
                    + " Entry:"
                    + (mapProject.isGroupStructure() ? " group "
                        + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                    + " map priority "
                    + Integer.toString(mapEntry.getMapPriority()));

            // add message if terminating entry rule is not TRUE
          } else if (mapEntry.getMapPriority() == entryGroups.get(key).size()
              && !mapEntry.getRule().equals("TRUE")) {
            validationResult.addError("Terminating entry has non-TRUE rule."
                + " Entry:"
                + (mapProject.isGroupStructure() ? " group "
                    + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                + " map priority "
                + Integer.toString(mapEntry.getMapPriority()));
          }
        }
      }
    }
    for (String error : validationResult.getErrors()) {
      Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
          "    " + error);
    }

    return validationResult;
  }

  /**
   * Function to check higher level groups do not have only NC target codes.
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordNcNodes(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    // Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
    // .info("  Checking map record for high-level groups with only NC target codes...");

    ValidationResult validationResult = new ValidationResultJpa();

    // if only one group, return empty validation result (also covers
    // non-group-structure projects)
    if (entryGroups.keySet().size() == 1)
      return validationResult;

    // otherwise cycle over the high-level groups (i.e. all but first group)
    for (int group : entryGroups.keySet()) {

      if (group != 1) {
        List<MapEntry> entries = entryGroups.get(group);

        boolean isValidGroup = false;
        for (MapEntry entry : entries) {
          if (entry.getTargetId() != null && !entry.getTargetId().equals(""))
            isValidGroup = true;
        }

        if (!isValidGroup) {
          validationResult.addError("High-level group "
              + Integer.toString(group) + " has no entries with targets");
        }
      }
    }

    for (String error : validationResult.getErrors()) {
      Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
          "    " + error);
    }

    return validationResult;
  }

  /**
   * Function to check that all advices attached are allowable by the project.
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  public ValidationResult checkMapRecordAdvices(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    // Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
    // "  Checking map record for valid map advices...");

    ValidationResult validationResult = new ValidationResultJpa();

    for (MapEntry mapEntry : mapRecord.getMapEntries()) {

      for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {

        if (!mapProject.getMapAdvices().contains(mapAdvice)) {
          validationResult.addError("Invalid advice "
              + mapAdvice.getName()
              + "."
              + " Entry:"
              + (mapProject.isGroupStructure() ? " group "
                  + Integer.toString(mapEntry.getMapGroup()) + "," : "")
              + " map priority " + Integer.toString(mapEntry.getMapPriority()));
        }
      }

    }

    for (String error : validationResult.getErrors()) {
      Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
          "    " + error);
    }

    return validationResult;
  }

  /**
   * Helper function to sort a records entries into entry lists binned by group.
   * 
   * @param mapRecord the map record
   * @return a map of group to entry list
   */
  @SuppressWarnings("static-method")
  public Map<Integer, List<MapEntry>> getEntryGroups(MapRecord mapRecord) {

    Map<Integer, List<MapEntry>> entryGroups = new HashMap<>();

    for (MapEntry entry : mapRecord.getMapEntries()) {

      // if no existing set for this group, create a blank set
      List<MapEntry> entryGroup = entryGroups.get(entry.getMapGroup());
      if (entryGroup == null) {
        entryGroup = new ArrayList<>();
      }

      // add this entry to group and put it in group map
      entryGroup.add(entry);
      entryGroups.put(entry.getMapGroup(), entryGroup);
    }

    return entryGroups;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * compareMapRecords(org.ihtsdo.otf.mapping.model.MapRecord,
   * org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public ValidationResult compareMapRecords(MapRecord record1, MapRecord record2) {
    ValidationResult validationResult = new ValidationResultJpa();

    // compare mapProjectId
    if (!record1.getMapProjectId().equals(record2.getMapProjectId())) {
      validationResult
          .addError("Invalid comparison, map project ids do not match ("
              + record1.getMapProjectId() + ", " + record2.getMapProjectId()
              + ").");
      validationResult
          .addConciseError("Invalid comparison - map project ids do not match");
      return validationResult;
    }

    // compare conceptId
    if (!record1.getConceptId().equals(record2.getConceptId())) {
      validationResult
          .addError("Invalid comparison, map record concept ids do not match ("
              + record1.getConceptId() + ", " + record2.getConceptId() + ").");
      validationResult
          .addConciseError("Invalid comparison - map record concept ids do not match");
      return validationResult;
    }

    // DO NOT compare mapPrinciples -- as of MAP-1139

    // check force map lead review flag
    if (record1.isFlagForMapLeadReview()) {
      validationResult
          .addError("Mapping specialist #1 requests map lead review.");
      validationResult
          .addConciseError("Mapping specialist requests map lead review");
    }
    if (record2.isFlagForMapLeadReview()) {
      validationResult
          .addError("Mapping specialist #2 requests map lead review.");
      validationResult
          .addConciseError("Mapping specialist requests map lead review");
    }

    // check consensus review flag
    if (record1.isFlagForConsensusReview()) {
      validationResult
          .addError("Mapping specialist #1 requests consensus review.");
      validationResult
          .addConciseError("Mapping specialist requests consensus review");
    }
    if (record2.isFlagForConsensusReview()) {
      validationResult
          .addError("Mapping specialist #2 requests consensus review.");
      validationResult
          .addConciseError("Mapping specialist requests consensus review");
    }

    // check editorial review flag
    if (record1.isFlagForEditorialReview()) {
      validationResult
          .addError("Mapping specialist #1 requests editorial review.");
      validationResult
          .addConciseError("Mapping specialist requests editorial review");
    }
    if (record2.isFlagForEditorialReview()) {
      validationResult
          .addError("Mapping specialist #2 requests editorial review.");
      validationResult
          .addConciseError("Mapping specialist requests editorial review");
    }

    // compare mapEntries
    // organize map entries by group
    Map<Integer, List<MapEntry>> groupToMapEntryList1 = new HashMap<>();
    for (MapEntry entry : record1.getMapEntries()) {
      if (groupToMapEntryList1.containsKey(entry.getMapGroup())) {
        List<MapEntry> entryList =
            groupToMapEntryList1.get(entry.getMapGroup());
        entryList.add(entry);
      } else {
        List<MapEntry> entryList = new ArrayList<>();
        entryList.add(entry);
        groupToMapEntryList1.put(entry.getMapGroup(), entryList);
      }
    }
    Map<Integer, List<MapEntry>> groupToMapEntryList2 = new HashMap<>();
    for (MapEntry entry : record2.getMapEntries()) {
      if (groupToMapEntryList2.containsKey(entry.getMapGroup())) {
        List<MapEntry> entryList =
            groupToMapEntryList2.get(entry.getMapGroup());
        entryList.add(entry);
      } else {
        List<MapEntry> entryList = new ArrayList<>();
        entryList.add(entry);
        groupToMapEntryList2.put(entry.getMapGroup(), entryList);
      }
    }

    // if records have differing numbers of groups
    if (groupToMapEntryList1.keySet().size() != groupToMapEntryList2.keySet()
        .size()) {
      validationResult.addError("Number of map groups is different");
      validationResult.addConciseError("Number of map groups is different");
    }

    // for each group
    for (int i = 1; i < Math.max(groupToMapEntryList1.size(),
        groupToMapEntryList2.size()) + 1; i++) {
      List<MapEntry> entries1 = groupToMapEntryList1.get(new Integer(i));
      List<MapEntry> entries2 = groupToMapEntryList2.get(new Integer(i));

      // error if different numbers of entries
      if (entries1 == null) {
        validationResult.addError("Number of map entries is different");
        validationResult.addConciseError("Number of map entries is different");
        continue;
      } else if (entries2 == null) {
        validationResult.addError("Number of map entries is different");
        validationResult.addConciseError("Number of map entries is different");
        continue;
      } else if (entries1.size() != entries2.size()) {
        validationResult.addError("Number of map entries is different");
        validationResult.addConciseError("Number of map entries is different");
      }

      // create string lists for entry comparison
      List<String> stringEntries1 = new ArrayList<>();
      List<String> stringEntries2 = new ArrayList<>();
      for (MapEntry entry1 : entries1) {
        stringEntries1.add(convertToString(entry1));
      }
      for (MapEntry entry2 : entries2) {
        stringEntries2.add(convertToString(entry2));
      }

      // check for matching entries in different order
      boolean outOfOrderFlag = false;
      boolean missingEntry = false;

      for (int d = 0; d < Math
          .min(stringEntries1.size(), stringEntries2.size()); d++) {

        if (stringEntries1.get(d).equals(stringEntries2.get(d)))
          continue;
        else if (stringEntries2.contains(stringEntries1.get(d)))
          outOfOrderFlag = true;
        else
          missingEntry = true;
      }
      if (!outOfOrderFlag && !missingEntry) {
        continue; // to next group for comparison
      }
      if (outOfOrderFlag && !missingEntry) {
        validationResult.addWarning("Map entries in different order");
        continue; // to next group for comparison
      }

      // check for details of missing entries

      for (int d = 0; d < entries1.size(); d++) {
        for (int f = 0; f < entries2.size(); f++) {
          if (isRulesEqual(entries1.get(d), entries2.get(f))
              && isTargetIdsEqual(entries1.get(d), entries2.get(f))
              && !isMapRelationsEqual(entries1.get(d), entries2.get(f))) {

            validationResult.addError("Map relation is different: "
                + (entries1.get(d).getMapRelation() == null
                    ? "No relation specified" : entries1.get(d)
                        .getMapRelation().getName())
                + " vs. "
                + (entries2.get(f).getMapRelation() == null
                    ? "No relation specified" : entries2.get(f)
                        .getMapRelation().getName()));

            validationResult.addConciseError("Map relation is different");
          }
        }
      }
      for (int d = 0; d < entries1.size(); d++) {
        for (int f = 0; f < entries2.size(); f++) {
          if (isRulesEqual(entries1.get(d), entries2.get(f))
              && isTargetIdsEqual(entries1.get(d), entries2.get(f))
              && !entries1.get(d).getMapAdvices()
                  .equals(entries2.get(f).getMapAdvices()))

            printAdviceDifferences(validationResult, entries1.get(d),
                entries2.get(f));
        }
      }

      // check that any rule/target pairs are not different
      // for non-rule based projects, this will compare a single entry in each
      // group
      for (int d = 0; d < entries1.size(); d++) {
        for (int f = 0; f < entries2.size(); f++) {
          if (isRulesEqual(entries1.get(d), entries2.get(f))
              && !isTargetIdsEqual(entries1.get(d), entries2.get(f))) {

            validationResult.addError("Target code is different: "
                + (entries1.get(d).getTargetId() == null
                    || entries1.get(d).getTargetId().equals("") ? "No target"
                    : entries1.get(d).getTargetId())
                + " vs. "
                + (entries2.get(f).getTargetId() == null
                    || entries2.get(f).getTargetId().equals("") ? "No target"
                    : entries2.get(f).getTargetId()));
            validationResult.addConciseError("Target code is different");
          }
        }
      }

      // only check TRUE rules if project is rule based
      if (mapProject.isRuleBased()) {

        for (int d = 0; d < entries1.size(); d++) {
          for (int f = 0; f < entries2.size(); f++) {
            if (!entries1.get(d).getRule().equals("TRUE")
                && !entries2.get(f).getRule().equals("TRUE")
                && !isRulesEqual(entries1.get(d), entries2.get(f))) {

              validationResult.addError("Map rule is different: "
                  + entries1.get(d).getRule() + " vs. "
                  + entries2.get(f).getRule());
              validationResult.addConciseError("Map rule is different");
            }
          }
        }
      }

    }

    return validationResult;
  }

  /**
   * Indicates whether or not rules are equal.
   * 
   * @param entry1 the entry1
   * @param entry2 the entry2
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isRulesEqual(MapEntry entry1, MapEntry entry2) {

    // if not rule based, automatically return true
    if (!mapProject.isRuleBased())
      return true;

    // check null comparisons first
    if (entry1.getRule() == null && entry2.getRule() != null)
      return false;
    if (entry1.getRule() != null && entry2.getRule() == null)
      return false;
    if (entry1.getRule() == null && entry2.getRule() == null)
      return true;
    return entry1.getRule().equals(entry2.getRule());
  }

  /**
   * Indicates whether or not target ids are equal.
   * 
   * @param entry1 the entry1
   * @param entry2 the entry2
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @SuppressWarnings("static-method")
  public boolean isTargetIdsEqual(MapEntry entry1, MapEntry entry2) {
    // check null comparisons first
    if (entry1.getTargetId() == null && entry2.getTargetId() != null)
      return false;
    if (entry1.getTargetId() != null && entry2.getTargetId() == null)
      return false;
    if (entry1.getTargetId() == null && entry2.getTargetId() == null)
      return true;
    return entry1.getTargetId().equals(entry2.getTargetId());
  }

  /**
   * Indicates whether or not map relations are equal.
   * 
   * @param entry1 the entry1
   * @param entry2 the entry2
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @SuppressWarnings("static-method")
  public boolean isMapRelationsEqual(MapEntry entry1, MapEntry entry2) {
    // check null comparisons first
    if (entry1.getMapRelation() == null && entry2.getMapRelation() != null)
      return false;
    if (entry1.getMapRelation() != null && entry2.getMapRelation() == null)
      return false;
    if (entry1.getMapRelation() == null && entry2.getMapRelation() == null)
      return true;
    return entry1.getMapRelation().getId()
        .equals(entry2.getMapRelation().getId());
  }

  /**
   * Prints the advice differences.
   * 
   * @param validationResult the validation result
   * @param entry1 the entry1
   * @param entry2 the entry2
   */
  private void printAdviceDifferences(ValidationResult validationResult,
    MapEntry entry1, MapEntry entry2) {

    Comparator<Object> advicesComparator = new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        String x1 = ((MapAdvice) o1).getName();
        String x2 = ((MapAdvice) o2).getName();
        if (!x1.equals(x2)) {
          return x1.compareTo(x2);
        }
        return 0;
      }
    };

    List<MapAdvice> advices1 = new ArrayList<>(entry1.getMapAdvices());
    Collections.sort(advices1, advicesComparator);
    List<MapAdvice> advices2 = new ArrayList<>(entry2.getMapAdvices());
    Collections.sort(advices2, advicesComparator);

    for (int i = 0; i < Math.max(advices1.size(), advices2.size()); i++) {
      try {
        if (advices1.get(i) == null && advices2.get(i) != null)
          continue;
        if (advices1.get(i) != null && advices2.get(i) == null)
          continue;
        if (!advices1.get(i).equals(advices2.get(i))) {
          validationResult.addError("Map Advice is Different: "
              + (advices1.get(i).getName() + " vs. " + advices2.get(i)
                  .getName()));
        }
      } catch (IndexOutOfBoundsException e) {
        validationResult.addError("Map Advice is Different: "
            + (advices1.size() > i ? advices1.get(i).getName() : "No advice")
            + " vs. "
            + (advices2.size() > i ? advices2.get(i).getName() : "No advice"));

      }
    }
  }

  /**
   * Convert to string.
   * 
   * @param mapEntry the map entry
   * @return the string
   */
  private String convertToString(MapEntry mapEntry) {

    Comparator<Object> advicesComparator = new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        String x1 = ((MapAdvice) o1).getName();
        String x2 = ((MapAdvice) o2).getName();
        if (!x1.equals(x2)) {
          return x1.compareTo(x2);
        }
        return 0;
      }

    };

    List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
    Collections.sort(advices, advicesComparator);

    StringBuffer sb = new StringBuffer();
    sb.append(mapEntry.getTargetId()
        + " "
        + mapEntry.getRule()
        + " "
        + (mapEntry.getMapRelation() != null ? mapEntry.getMapRelation()
            .getId() : ""));
    for (MapAdvice mapAdvice : advices) {
      sb.append(mapAdvice.getObjectId() + " ");
    }

    return sb.toString();
  }

  /**
   * Returns the lowest workflow status.
   * 
   * @param mapRecords the map records
   * @return the lowest workflow status
   */
  @SuppressWarnings("static-method")
  public WorkflowStatus getLowestWorkflowStatus(Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.REVISION;
    for (MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /**
   * For default project, all target codes are considered valid.
   * 
   * @param terminologyId the terminology id
   * @return true, if is target code valid
   * @throws Exception the exception
   */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    return true;
  }

  /**
   * Assign a new map record from existing record, performing any necessary
   * workflow actions based on workflow status
   * 
   * - READY_FOR_PUBLICATION, PUBLICATION to FIX_ERROR_PATH: Create a new record
   * with origin ids set to the existing record (and its antecedents) - Add the
   * record to the tracking record - Return the tracking record.
   * 
   * - NEW, EDITING_IN_PROGRESS, EDITING_DONE, CONFLICT_NEW,
   * CONFLICT_IN_PROGRESS Invalid workflow statuses, should never be called with
   * a record of this nature
   * 
   * Expects the tracking record to be a detached Jpa entity. Does not modify
   * objects via services.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapRecord the map record
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> assignFromInitialRecord(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapRecord mapRecord, MapUser mapUser)
    throws Exception {

    Set<MapRecord> newRecords = new HashSet<>();

    switch (trackingRecord.getWorkflowPath()) {

      case FIX_ERROR_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "assignFromInitialRecord:  FIX_ERROR_PATH");

        // case 1 : User claims a PUBLISHED or READY_FOR_PUBLICATION record
        // to
        // fix error on.
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.READY_FOR_PUBLICATION)) {

          // check that only one record exists for this tracking record
          if (!(trackingRecord.getMapRecordIds().size() == 1)) {
            // System.out.println(trackingRecord.toString());
            throw new Exception(
                "DefaultProjectSpecificHandlerException - assignFromInitialRecord: More than one record exists for FIX_ERROR_PATH assignment.");
          }

          // deep copy the map record
          MapRecord newRecord = new MapRecordJpa(mapRecord, false);

          // set origin ids
          newRecord.addOrigin(mapRecord.getId());
          newRecord.addOrigins(mapRecord.getOriginIds());

          // set other relevant fields
          newRecord.setOwner(mapUser);
          newRecord.setLastModifiedBy(mapUser);
          newRecord.setWorkflowStatus(WorkflowStatus.NEW);

          // add the record to the list
          newRecords.add(newRecord);

          // set the workflow status of the old record to review and add
          // it to
          // new records
          mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
          newRecords.add(mapRecord);

        }
        break;

      case CONSENSUS_PATH:
        break;
      case LEGACY_PATH:
        break;
      case NON_LEGACY_PATH:
        throw new Exception(
            "Invalid assignFromInitialRecord call for NON_LEGACY_PATH workflow");
      case REVIEW_PROJECT_PATH:
        throw new Exception(
            "Invalid assignFromInitialRecord call for REVIEW_PROJECT_PATH workflow");
      case QA_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "assignFromInitialRecord:  QA_PATH");

        // case 1 : User claims a PUBLISHED or READY_FOR_PUBLICATION record
        // to run qa on.
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.READY_FOR_PUBLICATION)) {

          // check that only one record exists for this tracking record
          if (!(trackingRecord.getMapRecordIds().size() == 1)) {
            // System.out.println(trackingRecord.toString());
            throw new Exception(
                "DefaultProjectSpecificHandlerException - assignFromInitialRecord: More than one record exists for QA_PATH assignment.");
          }

          // deep copy the map record
          MapRecord newRecord = new MapRecordJpa(mapRecord, false);

          // set origin ids
          newRecord.addOrigin(mapRecord.getId());
          newRecord.addOrigins(mapRecord.getOriginIds());

          // set other relevant fields
          // get QA User MapUser
          newRecord.setOwner(mapUser);
          newRecord.setLastModifiedBy(mapUser);
          newRecord.setWorkflowStatus(WorkflowStatus.QA_NEEDED);

          // add the record to the list
          newRecords.add(newRecord);

          // set the workflow status of the old record to review and add
          // it to
          // new records
          mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
          newRecords.add(mapRecord);

        }
        break;
      default:
        throw new Exception(
            "assignFromInitialRecord called with invalid Workflow Path.");

    }

    return newRecords;
  }

  /**
   * Assign a map user to a concept for this project
   * 
   * Conditions: - Only valid workflow paths: NON_LEGACY_PATH, LEGACY_PATH, and
   * CONSENSUS_PATH Note that QA_PATH and FIX_ERROR_PATH should never call this
   * method - Only valid workflow statuses: Any status preceding
   * CONFLICT_DETECTED and CONFLICT_DETECTED
   * 
   * Default Behavior: - Create a record with workflow status based on current
   * workflow status - Add the record to the tracking record - Return the
   * tracking record.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param concept the concept
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> assignFromScratch(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, Concept concept, MapUser mapUser)
    throws Exception {

    // the list of map records to return
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // for (MapRecord mr : mapRecords) {
    // System.out.println(mr.toString());
    // }

    // create new record
    MapRecord mapRecord = new MapRecordJpa();
    mapRecord.setMapProjectId(mapProject.getId());
    mapRecord.setConceptId(concept.getTerminologyId());
    mapRecord.setConceptName(concept.getDefaultPreferredName());
    mapRecord.setOwner(mapUser);
    mapRecord.setLastModifiedBy(mapUser);

    // set additional record parameters based on workflow path and status
    switch (trackingRecord.getWorkflowPath()) {
      case NON_LEGACY_PATH:

        // if a "new" tracking record (i.e. prior to conflict detection),
        // add a NEW record
        if (getWorkflowStatus(mapRecords).compareTo(
            WorkflowStatus.CONFLICT_DETECTED) < 0) {

          // check that this record is valid to be assigned (i.e. no more
          // than
          // one other specialist assigned)
          if (getMapUsers(mapRecords).size() >= 2) {
            throw new Exception(
                "DefaultProjectSpecificHandlerException - assignFromScratch:  Two users already assigned");
          }

          mapRecord.setWorkflowStatus(WorkflowStatus.NEW);
          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "NON_LEGACY_PATH: NEW");

          // otherwise, if this is a tracking record with conflict
          // detected, add a CONFLICT_NEW record
        } else if (getWorkflowStatus(mapRecords).equals(
            WorkflowStatus.CONFLICT_DETECTED)) {

          mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_NEW);

          MapRecord mapRecord1 = (MapRecord) mapRecords.toArray()[0];
          MapRecord mapRecord2 = (MapRecord) mapRecords.toArray()[1];
          ValidationResult validationResult =
              compareMapRecords(mapRecord1, mapRecord2);
          mapRecord.setReasonsForConflict(validationResult.getConciseErrors());

          // get the origin ids from the tracking record
          for (MapRecord mr : newRecords) {
            mapRecord.addOrigin(mr.getId());
          }
          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "NON_LEGACY_PATH: CONFLICT_NEW");

        } else {
          throw new Exception("ASSIGN_FROM_SCRATCH on NON_LEGACY_PATH failed.");
        }

        break;

      case REVIEW_PROJECT_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Assigning concept along REVIEW_PROJECT_PATH");

        if (getWorkflowStatus(mapRecords).equals(WorkflowStatus.REVIEW_NEEDED)) {
          // check that one record exists and is not owned by this user
          if (mapRecords.size() == 1) {
            /*
             * Removed this, see MAP-617 if
             * (mapRecords.iterator().next().getOwner().equals(mapUser)) throw
             * new Exception(
             * "  Cannot assign review record, user attempting to review own work"
             * );
             */
          } else {
            throw new Exception("  Expected exactly one map record");
          }

          // set origin id to the existing record
          mapRecord.addOrigin(mapRecords.iterator().next().getId());

          // set workflow status to review needed
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);
        } else if (mapRecords.size() == 0) {

          // set workflow status to new
          mapRecord.setWorkflowStatus(WorkflowStatus.NEW);

        } else {
          throw new Exception(
              "ASSIGN_FROM_SCRATCH on REVIEW_PROJECT_PATH failed for concept "
                  + mapRecord.getConceptId());
        }

        break;

      case FIX_ERROR_PATH:

        // Case 1: A lead claims an error-fixed record for review
        if (getLowestWorkflowStatus(mapRecords).equals(
            WorkflowStatus.REVIEW_NEEDED)) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "FIX_ERROR_PATH: Lead claiming an error-fixed record");

          // check that only two records exists for this tracking record
          if (!(trackingRecord.getMapRecordIds().size() == 2)) {
            // System.out.println(trackingRecord.toString());
            throw new Exception(
                "assignFromScratch: There are not two records for FIX_ERROR_PATH assignment.");
          }

          // set origin id
          for (MapRecord mr : mapRecords) {
            mapRecord.addOrigin(mr.getId());
            mapRecord.addOrigins(mr.getOriginIds());
          }

          // set workflow status
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);

          Logger.getLogger("FIX_ERROR_PATH final record: "
              + mapRecord.toString());

        } else {
          throw new Exception(
              "assignFromScratch called on FIX_ERROR_PATH but tracking record does not contain a record marked REVIEW_NEEDED");
        }

        break;

      case CONSENSUS_PATH:
        break;
      case LEGACY_PATH:
        break;
      case QA_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Assigning concept along QA_PATH");

        if (getLowestWorkflowStatus(mapRecords)
            .equals(WorkflowStatus.QA_NEEDED)) {
          if (mapRecords.size() == 2) {
            // do nothing
          } else {
            throw new Exception("  Expected exactly two map records.");
          }

          // set origin id and copy labels
          for (MapRecord record : mapRecords) {
            // if (record.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            // mapRecord.addOrigin(record.getId());
            if (record.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
              mapRecord.setLabels(record.getLabels());
              mapRecord.addOrigin(record.getId());
            }
          }

          // set workflow status to review new
          mapRecord.setWorkflowStatus(WorkflowStatus.QA_NEW);
        } else {
          throw new Exception(
              "ASSIGN_FROM_SCRATCH on QA_PATH failed for concept "
                  + mapRecord.getConceptId());
        }

        break;

      default:
        throw new Exception(
            "assignFromScratch called with erroneous Workflow Path.");
    }

    // add this record to the tracking record
    newRecords.add(mapRecord);

    // return the modified record set
    return newRecords;
  }

  /**
   * Unassigns a user from a concept, and performs any other necessary workflow
   * actions
   * 
   * Conditions: - Valid workflow paths: All - Valid workflow status: All except
   * READY_FOR_PUBLICATION, PUBLISHED.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> unassign(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
        "Unassign called along " + trackingRecord.getWorkflowPath() + " path");

    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // find the record assigned to this user
    MapRecord mapRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

    // switch on workflow path
    switch (trackingRecord.getWorkflowPath()) {

    // non legacy path and review project path function identically
      case NON_LEGACY_PATH:
      case REVIEW_PROJECT_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Unassign: NON_LEGACY_PATH");

        if (mapRecord == null)
          throw new Exception(
              "unassign called for concept that does not have specified user assigned");

        // remove this record from the tracking record
        newRecords.remove(mapRecord);

        // determine action based on record's workflow status after removal
        // of
        // the unassigned record
        switch (mapRecord.getWorkflowStatus()) {

        // standard removal cases, no special action required
          case NEW:
          case EDITING_IN_PROGRESS:
          case EDITING_DONE:
          case CONFLICT_NEW:
          case CONFLICT_IN_PROGRESS:
          case CONFLICT_RESOLVED:
          case REVIEW_NEEDED:
          case REVIEW_NEW:
          case REVIEW_IN_PROGRESS:
          case REVIEW_RESOLVED:
          case QA_NEEDED:
          case QA_NEW:
          case QA_IN_PROGRESS:
          case QA_RESOLVED:

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info(
                    "Unassign: NON_LEGACY_PATH -- "
                        + mapRecord.getWorkflowStatus()
                        + " -- No special action required");

            break;

          // where a conflict is detected, two cases to handle
          // (1) any lead-claimed resolution record is now invalid, and should
          // be deleted (no conflict remaining)
          // (2) a specialist's record is now not in conflict, and should be
          // reverted to EDITING_DONE
          case CONFLICT_DETECTED:

            MapRecord recordToRemove = null;
            for (MapRecord mr : newRecords) {

              // if another specialist's record, revert to EDITING_DONE
              if (mr.getWorkflowStatus().equals(
                  WorkflowStatus.CONFLICT_DETECTED)) {
                mr.setWorkflowStatus(WorkflowStatus.EDITING_DONE);
              }

              // if the lead's record, delete
              if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)) {
                recordToRemove = mr;
              }
            }

            if (recordToRemove != null)
              newRecords.remove(recordToRemove);

            break;

          // consensus path not implemented
          case CONSENSUS_NEEDED:
            break;
          case CONSENSUS_IN_PROGRESS:
            break;

          // If REVISION is detected, something has gone horribly wrong for
          // the
          // NON_LEGACY_PATH
          case REVISION:

            throw new Exception(
                "Unassign:  A user has been improperly assigned to a review record");

            // throw an exception if somehow an invalid workflow status is
            // found
          default:
            throw new Exception(
                "unassign found a map record with invalid workflow status");
        }

        break;

      case LEGACY_PATH:
        break;

      // If unassignment for error fixing, need to reset the existing record
      // from REVIEW
      case FIX_ERROR_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Unassign:  FIX_ERROR_PATH");

        // get the REVISION record
        MapRecord revisionRecord = null;
        MapRecord editingRecord = null;
        MapRecord reviewRecord = null;
        for (MapRecord mr : mapRecords) {
          System.out.println(mr.toString());
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {

            revisionRecord = mr;
            System.out.println("Revision record: " + mr.toString());
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.NEW)
              || mr.getWorkflowStatus().equals(
                  WorkflowStatus.EDITING_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
            editingRecord = mr;
            System.out.println("Editing record:  " + mr.toString());
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
              || mr.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)) {
            reviewRecord = mr;
            System.out.println("Review record:   " + mr.toString());
          }
        }

        if (revisionRecord == null)
          throw new Exception(
              "Attempted to unassign a published revision record, but no such previously published record exists!");

        // Case 1: A user decides to abandon fixing an error
        if (editingRecord != null && reviewRecord == null) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "Unassign:  FIX_ERROR_PATH - User unassigning review work");

          MapRecord previousRevisionRecord =
              getPreviouslyPublishedVersionOfMapRecord(revisionRecord);

          if (previousRevisionRecord == null) {
            throw new Exception(
                "Could not retrieve previous version of map record, id = "
                    + revisionRecord.getId());
          } else {
            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info(
                    "Unassign:  FIX_ERROR_PATH - Reverting to previous record: "
                        + previousRevisionRecord.toString());

          }

          newRecords.clear();
          newRecords.add(previousRevisionRecord);

          // Case 2: A lead unassigns themselves from reviewing a fixed
          // error
          // delete the lead's record, no other action required
        } else if (reviewRecord != null) {
          newRecords.remove(reviewRecord);
        } else {
          throw new Exception(
              "Unexpected error attempt to unassign a Revision record.  Contact an administrator.");
        }

        break;
      case QA_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Unassign:  QA_PATH");

        revisionRecord = null;
        editingRecord = null;
        reviewRecord = null;
        for (MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            revisionRecord = mr;
          else if (mr.getWorkflowStatus().compareTo(WorkflowStatus.QA_NEEDED) <= 0)
            editingRecord = mr;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW))
            reviewRecord = mr;
        }

        if (revisionRecord == null)
          throw new Exception(
              "Attempted to unassign a published revision record, but no such previously published record exists!");

        // Case 1: A lead unassigns themselves from reviewing a fixed
        // error
        // delete the lead's record, no other action required
        if (reviewRecord != null) {
          newRecords.remove(reviewRecord);
          // Case 2: The concept is removed from QA, and unassigned from the qa
          // user
        } else if (editingRecord != null) {

          // clear the record set
          newRecords.clear();

          // get the previously published version of the revision record
          revisionRecord =
              getPreviouslyPublishedVersionOfMapRecord(revisionRecord);

          // add the previously published version to the map records set
          newRecords.add(revisionRecord);
        } else {

          throw new Exception(
              "Unexpected error attempt to unassign a QA record.  Contact an administrator.");
        }
        break;
      case CONSENSUS_PATH:
        break;

      default:
        break;
    }

    return newRecords;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#publish(
   * org.ihtsdo.otf.mapping.workflow.TrackingRecord, java.util.Set,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> publish(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // find the record assigned to this user
    MapRecord mapRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

    if (mapRecord == null)
      throw new Exception("publish:  Record for user could not be found");

    // clear any labels before publication
    mapRecord.setLabels(new HashSet<String>());
    // clear any reasonsForConflicts before publication
    mapRecord.setReasonsForConflict(new HashSet<String>());

    switch (trackingRecord.getWorkflowPath()) {
      case CONSENSUS_PATH:
        // do nothing
        break;
      case DRIP_FEED_REVIEW_PATH:
        // do nothing
        break;
      case FIX_ERROR_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "FIX_ERROR_PATH - Called Publish on resolved review");

        // Requirements for FIX_ERROR_PATH publish action
        // - 1 record marked REVISION
        // - 1 record marked REVIEW_NEEDED
        // - 1 record marked REVIEW_RESOLVED

        // check assumption: owned record is REVIEW_RESOLVED
        if (!mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_RESOLVED))
          throw new Exception(
              "Publish called on FIX_ERROR_PATH for map record not marked as REVIEW_RESOLVED (Workflow status found on map record "
                  + mapRecord.getId() + " is " + mapRecord.getWorkflowStatus());

        // check assumption: REVISION and REVIEW_NEEDED records are present
        boolean revisionRecordFound = false;
        boolean reviewNeededRecordFound = false;

        for (MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            revisionRecordFound = true;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
            reviewNeededRecordFound = true;
        }

        if (!revisionRecordFound)
          throw new Exception(
              "Publish called on FIX_ERROR_PATH, but no REVISION record found");

        if (!reviewNeededRecordFound)
          throw new Exception(
              "Publish called on FIX_ERROR_PATH, but no REVIEW_NEEDED record found");

        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        newRecords.clear();
        newRecords.add(mapRecord);

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "publish - FIX_ERROR_PATH - Creating READY_FOR_PUBLICATION record "
                + mapRecord.toString());

        break;
      case QA_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "QA_PATH - Called Publish on resolved qa");

        // Requirements for QA_PATH publish action
        // - 1 record marked REVISION
        // - 1 record marked QA_NEEDED
        // - 1 record marked QA_RESOLVED

        // check assumption: owned record is QA_RESOLVED
        if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED))
          throw new Exception(
              "Publish called on QA_PATH for map record not marked as QA_RESOLVED");

        // check assumption: REVISION and QA_NEEDED records are present
        boolean qaRecordFound = false;
        boolean qaNeededRecordFound = false;

        for (MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
            qaRecordFound = true;
          else if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED))
            qaNeededRecordFound = true;
        }

        if (!qaRecordFound)
          throw new Exception(
              "Publish called on QA_PATH, but no REVISION record found");

        if (!qaNeededRecordFound)
          throw new Exception(
              "Publish called on QA_PATH, but no QA_NEEDED record found");

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "publish - QA_PATH - Creating READY_FOR_PUBLICATION record "
                + mapRecord.toString());

        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        newRecords.clear();
        newRecords.add(mapRecord);

        break;
      case LEGACY_PATH:
        // do nothing
        break;
      case NON_LEGACY_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "NON_LEGACY_PATH - Publishing resolved conflict");

        // Requirements for NON_LEGACY_PATH publish action
        // - 2 records marked EDITING_DONE
        // *OR*
        // - 1 record marked CONFLICT_RESOLVED
        // - 2 records marked CONFLICT_DETECTED

        // if two map records, must be two EDITING_DONE or CONFLICT_DETECTED
        // records
        // with publish called by finishEditing
        if (mapRecords.size() == 2) {

          // check assumption: records are both marked EDITING_DONE or
          // CONFLICT_DETECTED
          for (MapRecord mr : mapRecords) {
            if (!mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)
                && !mr.getWorkflowStatus().equals(
                    WorkflowStatus.CONFLICT_DETECTED))
              throw new Exception(
                  "Publish called, expected two matching specialist records marked EDITING_DONE or CONFLICT_DETECTED, but found record with status "
                      + mr.getWorkflowStatus().toString());
          }

          // check assumption: records are not in conflict
          // note that this duplicates the call in finishEditing
          Iterator<MapRecord> iter = mapRecords.iterator();
          MapRecord mapRecord1 = iter.next();
          MapRecord mapRecord2 = iter.next();
          if (!compareMapRecords(mapRecord1, mapRecord2).isValid()) {
            throw new Exception(
                "Publish called for two matching specialist records, but the records did not pass comparator validation checks");
          }

          // deep copy the record and mark the new record
          // READY_FOR_PUBLICATION
          MapRecord newRecord = new MapRecordJpa(mapRecord, false);
          newRecord.setOwner(mapUser);
          newRecord.setLastModifiedBy(mapUser);
          newRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

          // construct and set the new origin ids
          Set<Long> originIds = new HashSet<>();
          originIds.add(mapRecord1.getId());
          originIds.add(mapRecord2.getId());
          originIds.addAll(mapRecord1.getOriginIds());
          originIds.addAll(mapRecord2.getOriginIds());
          newRecord.setOriginIds(originIds);

          // clear the records and add a single record owned by
          // this user -- note that this will remove any existing
          // conflict records
          newRecords.clear();
          newRecords.add(newRecord);

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "publish- NON_LEGACY_PATH - Creating READY_FOR_PUBLICATION record "
                  + newRecord.toString());

        } else if (mapRecords.size() == 3) {

          // Check assumption: owned record is CONFLICT_RESOLVED
          if (!mapRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_RESOLVED)) {
            throw new Exception(
                "Publish called on NON_LEGACY_PATH for map record not marked as CONFLICT_RESOLVED");
          }

          // Check assumption: two CONFLICT_DETECTED records
          int nConflictRecords = 0;
          for (MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED))
              nConflictRecords++;
          }

          if (nConflictRecords != 2) {
            throw new Exception(
                "Bad workflow state for concept "
                    + mapRecord.getConceptId()
                    + ":  CONFLICT_RESOLVED is not accompanied by two CONFLICT_DETECTED records");
          }

          // cycle over the previously existing records
          for (MapRecord mr : mapRecords) {

            // remove the CONFLICT_DETECTED records from the revised set
            if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
              newRecords.remove(mr);

              // set the CONFLICT_NEW or CONFLICT_IN_PROGRESS record
              // to
              // READY_FOR_PUBLICATION
              // and update
            } else {
              mr.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
            }
          }

          // otherwise, bad workflow state, throw exception
        } else {
          throw new Exception("Bad workflow state for concept "
              + mapRecord.getConceptId()
              + ":  Expected either two or three records, but found "
              + mapRecords.size());
        }

        break;
      case REVIEW_PROJECT_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "REVIEW_PROJECT_PATH - Publishing resolved conflict");

        // Requirements for REVIEW_PROJECT_PATH
        // - 1 record marked REVIEW_NEEDED
        // - 1 record marked REVIEW_RESOLVED

        // check assumption: owned record is marked resolved
        if (!mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_RESOLVED)) {
          throw new Exception(
              "Publish called on REVIEW_PROJECT_PATH for map record not marked as REVIEW_RESOLVED");
        }

        // check assumption: record requiring review is present
        MapRecord reviewNeededRecord = null;
        for (MapRecord mr : newRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
            reviewNeededRecord = mr;
        }

        if (reviewNeededRecord == null) {
          throw new Exception(
              "Publish called on REVIEW_PROJECT_PATH, but no REVIEW_NEEDED record found");
        }

        // remove the review needed record
        newRecords.remove(reviewNeededRecord);

        // set the lead's record to READY_FOR_PUBLICATION
        mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        break;
      default:
        break;

    }
    return newRecords;

  }

  /**
   * Updates workflow information when a specialist or lead clicks "Finished"
   * Expects the tracking record to be detached from persistence environment.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the workflow tracking record
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> finishEditing(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // find the record assigned to this user
    MapRecord mapRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

    if (mapRecord == null)
      throw new Exception("finishEditing:  Record for user could not be found");

    // switch on workflow path
    switch (trackingRecord.getWorkflowPath()) {
      case NON_LEGACY_PATH:

        // case 1: A specialist is finished with a record
        if (getWorkflowStatus(mapRecords).compareTo(
            WorkflowStatus.CONFLICT_DETECTED) <= 0) {

          Logger
              .getLogger(DefaultProjectSpecificAlgorithmHandler.class)
              .info(
                  "NON_LEGACY_PATH - New finished record, checking for other records");

          // set this record to EDITING_DONE
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);

          // check if two specialists have completed work (lowest workflow
          // status is EDITING_DONE, highest workflow status is
          // CONFLICT_DETECTED)
          if (getLowestWorkflowStatus(mapRecords).compareTo(
              WorkflowStatus.EDITING_DONE) >= 0
              && mapRecords.size() == 2) {

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info("NON_LEGACY_PATH - Two records found");

            java.util.Iterator<MapRecord> recordIter = mapRecords.iterator();
            MapRecord mapRecord1 = recordIter.next();
            MapRecord mapRecord2 = recordIter.next();
            ValidationResult validationResult =
                compareMapRecords(mapRecord1, mapRecord2);

            // if map records validation is successful, publish
            if (validationResult.isValid()) {

              Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                  .info("NON_LEGACY_PATH - No conflicts detected.");

              newRecords = publish(trackingRecord, mapRecords, mapUser);

            } else {

              Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                  .info("NON_LEGACY_PATH - Conflicts detected");

              // conflict detected, change workflow status of all
              // records (if not a lead's existing conflict record)
              // and
              // update records
              for (MapRecord mr : newRecords) {
                if (mr.getWorkflowStatus().compareTo(
                    WorkflowStatus.CONFLICT_DETECTED) <= 0)
                  mr.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
              }
            }
            // otherwise, only one specialist has finished work, do
            // nothing else
          } else {
            Logger
                .getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info(
                    "NON_LEGACY_PATH - Only this specialist has completed work");
          }

          // case 2: A lead is finished with a conflict resolution
          // Determined by workflow status of:
          // CONFLICT_NEW (i.e. conflict was resolved immediately)
          // CONFLICT_IN_PROGRESS (i.e. conflict had been previously saved
          // for later)
          // CONFLICT_RESOLVED (i.e. conflict marked resolved, but lead
          // revisited)
        } else if (mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.CONFLICT_NEW)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.CONFLICT_IN_PROGRESS)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.CONFLICT_RESOLVED)) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "NON_LEGACY_PATH - Conflict resolution detected");

          // set the lead's record to CONFLICT_RESOLVED
          mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_RESOLVED);

        } else {
          throw new Exception(
              "finishEditing failed! Invalid workflow status combination on record(s)");
        }

        break;

      case REVIEW_PROJECT_PATH:

        switch (mapRecord.getWorkflowStatus()) {

        // case 1: specialist finishes a map record
          case REVIEW_NEEDED:
          case EDITING_DONE:
          case EDITING_IN_PROGRESS:
          case NEW:

            Logger
                .getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info(
                    "FinishEditing: REVIEW_PROJECT_PATH, Specialist level work");

            // check assumptions
            // - should only be one record
            if (mapRecords.size() != 1) {
              throw new Exception(
                  "FINISH called at initial editing level on REVIEW_PROJECT_PATH where more than one record exists");
            }

            // mark as REVIEW_NEEDED
            mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);

            break;

          case REVIEW_RESOLVED:
          case REVIEW_IN_PROGRESS:
          case REVIEW_NEW:

            Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
                .info("FinishEditing: REVIEW_PROJECT_PATH, Lead level work");

            // check assumptions
            // - should be two map records, this one and one marked
            // REVIEW_NEEDED

            // check assumption: only two records
            if (mapRecords.size() != 2)
              throw new Exception(
                  "FINISH called at review editing level on REVIEW_PROJECT_PATH without exactly two map records");

            // check assumption: review needed record present
            MapRecord reviewRecord = null;
            for (MapRecord mr : mapRecords) {
              if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
                reviewRecord = mr;
            }

            if (reviewRecord == null)
              throw new Exception(
                  "FINISH called at review editing level on REVIEW_PROJECT_PATH, but could not locate REVIEW_NEEDED record");

            // mark as READY_FOR_PUBLICATION
            mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_RESOLVED);

            break;

          default:
            throw new Exception(
                "Called finish on map record with invalid workflow status along REVIEW_PROJECT_PATH");
        }

        break;

      case FIX_ERROR_PATH:

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "FIX_ERROR_PATH");

        // case 1: A user has finished correcting an error on a previously
        // published record
        // requires a workflow state to exist below that of REVEW_NEW (in
        // this
        // case, NEW, EDITING_IN_PROGRESS
        if (mapRecords.size() == 2) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
              "  User has finished correcting an error");

          // assumption check: should only be 2 records
          // 1) The original record (now marked REVISION)
          // 2) The modified record (NEW, EDITING_IN_PROGRESS, EDITING_DONE, or
          // REVIEW_NEEDED)
          boolean foundOriginalRecord = false;
          boolean foundModifiedRecord = false;

          for (MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              foundOriginalRecord = true;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.NEW)
                || mr.getWorkflowStatus().equals(
                    WorkflowStatus.EDITING_IN_PROGRESS)
                || mr.getWorkflowStatus().equals(WorkflowStatus.EDITING_DONE)
                || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
              foundModifiedRecord = true;
          }

          if (!foundOriginalRecord)
            throw new Exception(
                "FIX_ERROR_PATH: Specialist finished work, but could not find previously published record");

          if (!foundModifiedRecord)
            throw new Exception(
                "FIX_ERROR_PATH: Specialist finished work, but could not find their record");

          // instantiate mapping service to get user's project role
          MappingService mappingService = new MappingServiceJpa();

          // cycle over the records
          for (MapRecord mr : mapRecords) {

            // two records, one marked REVISION, one marked with NEW,
            // EDITING_IN_PROGRESS
            if (!mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
              mr.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);
            }
          }

          // close the mapping service
          mappingService.close();

          // Case 2: A lead has finished reviewing a corrected error
        } else if (mapRecords.size() == 3) {

          // assumption check: should be exactly three records
          // 1) original published record, marked REVISION
          // 2) specialist's record, marked REVIEW_NEEDED
          // 3) lead's record, marked REVIEW_NEW or REVIEW_IN_PROGRESS

          MapRecord originalRecord = null;
          MapRecord modifiedRecord = null;
          MapRecord leadRecord = null;

          for (MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              originalRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED))
              modifiedRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
                || mr.getWorkflowStatus().equals(
                    WorkflowStatus.REVIEW_IN_PROGRESS)
                || mr.getWorkflowStatus()
                    .equals(WorkflowStatus.REVIEW_RESOLVED))
              leadRecord = mr;
          }

          if (originalRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find previously published record");

          if (modifiedRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find the specialist's record record");

          if (leadRecord == null)
            throw new Exception(
                "FIX_ERROR_PATH: Lead finished reviewing work, but could not find their record.");

          // set to resolved
          leadRecord.setWorkflowStatus(WorkflowStatus.REVIEW_RESOLVED);

        } else {
          throw new Exception(
              "Unexpected error along FIX_ERROR_PATH, invalid number of records passed in");
        }

        break;

      case LEGACY_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "LEGACY_PATH");
        break;
      case QA_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "QA_PATH");

        // a lead has finished reviewing a QA
        if (mapRecords.size() == 3) {

          // assumption check: should be exactly three records
          // 1) original published record, marked REVISION
          // 2) specialist (QA) record, marked QA_NEEDED
          // 3) lead's record, marked QA_NEW or QA_IN_PROGRESS

          MapRecord originalRecord = null;
          MapRecord modifiedRecord = null;
          MapRecord leadRecord = null;

          for (MapRecord mr : mapRecords) {
            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
              originalRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED))
              modifiedRecord = mr;
            if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
                || mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
                || mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED))
              leadRecord = mr;
          }

          if (originalRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find previously published record");

          if (modifiedRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find the specialist's (QA) record");

          if (leadRecord == null)
            throw new Exception(
                "QA_PATH: User finished reviewing work, but could not find their record.");

          mapRecord.setWorkflowStatus(WorkflowStatus.QA_RESOLVED);

        } else {
          throw new Exception(
              "Unexpected error along QA_PATH, invalid number of records passed in");
        }

        break;
      case CONSENSUS_PATH:
        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "CONSENSUS_PATH");
        break;
      default:
        throw new Exception("finishEditing: Unexpected workflow path");
    }

    return newRecords;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#saveForLater
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public Set<MapRecord> saveForLater(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    // find the record assigned to this user
    MapRecord mapRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

    if (mapRecord == null)
      throw new Exception("saveForLater:  Record for user could not be found");

    // check for blank entries and remove them
    // "blank" is defined as a null target id and a null map relation id
    Set<MapEntry> entriesToRemove = new HashSet<>();
    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      if (mapEntry.getTargetId() == null && mapEntry.getMapRelation() == null) {

        Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
            "Removing empty map entry");
        entriesToRemove.add(mapEntry);
      }
    }
    for (MapEntry mapEntry : entriesToRemove) {
      mapRecord.removeMapEntry(mapEntry);
    }

    switch (trackingRecord.getWorkflowPath()) {
      case CONSENSUS_PATH:
        break;

      // review project and fix error paths behave identically
      case REVIEW_PROJECT_PATH:
      case FIX_ERROR_PATH:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_IN_PROGRESS);

        break;
      case QA_PATH:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.QA_IN_PROGRESS);

        break;
      case LEGACY_PATH:
        break;
      case NON_LEGACY_PATH:
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW))
          mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
        break;

      default:
        break;

    }

    return newRecords;
  }

  /**
   * Cancel work on a map record.
   * 
   * Only use-case is for the FIX_ERROR_PATH where a new map record has been
   * assigned due to editing a published record.
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the sets the
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> cancelWork(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

    // copy the map records into a new array
    Set<MapRecord> newRecords = new HashSet<>(mapRecords);

    switch (trackingRecord.getWorkflowPath()) {

      case FIX_ERROR_PATH:

        MapRecord reviewRecord = null;
        MapRecord newRecord = getCurrentMapRecordForUser(mapRecords, mapUser);

        // check for the appropriate map records
        for (MapRecord mr : mapRecords) {
          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            reviewRecord = mr;
          }
        }

        // check assumption: user has an assigned record
        if (newRecord == null)
          throw new Exception("Cancel work called for user "
              + mapUser.getName() + " and " + trackingRecord.getTerminology()
              + " concept " + trackingRecord.getTerminologyId()
              + ", but could not retrieve assigned record.");

        // check assumption: a REVIEW record exists
        if (reviewRecord == null)
          throw new Exception(
              "Cancel work called for user "
                  + mapUser.getName()
                  + " and "
                  + trackingRecord.getTerminology()
                  + " concept "
                  + trackingRecord.getTerminologyId()
                  + ", but could not retrieve the previously published or ready-for-publication record.");

        // perform action only if the user's record is NEW
        // if editing has occured (EDITING_IN_PROGRESS or above), null-op
        if (newRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)) {

          newRecords = unassign(trackingRecord, mapRecords, mapUser);
        }

        break;
      default:
        // re-retrieve the records for this tracking record and return those
        // used to ensure no spurious alterations from serialization are saved
        // and therefore reflected in the audit trail
        newRecords.clear();
        MappingService mappingService = new MappingServiceJpa();
        for (Long id : trackingRecord.getMapRecordIds()) {
          newRecords.add(mappingService.getMapRecord(id));
        }
        break;
    }

    // return the modified records
    return newRecords;
  }

  /**
   * Returns the current map record for user.
   * 
   * @param mapRecords the map records
   * @param mapUser the map user
   * @return the current map record for user
   */
  @SuppressWarnings("static-method")
  public MapRecord getCurrentMapRecordForUser(Set<MapRecord> mapRecords,
    MapUser mapUser) {

    MapRecord mapRecord = null;

    for (MapRecord mr : mapRecords) {
      if (mr.getOwner().equals(mapUser)) {

        // if there are multiple records on this tracking record
        // for a particular user, return the one with highest workflow status
                
        // EXCEPTION: Never return a REVISION record
        if (!mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
          if (mapRecord != null) {
            if (mr.getWorkflowStatus().compareTo(mapRecord.getWorkflowStatus()) > 0)
              mapRecord = mr;
          } else {
            mapRecord = mr;
          }
        }
      }
    }
    return mapRecord;
  }

  /**
   * Returns the previously published/ready-for-publicatoin version of map
   * record.
   * 
   * @param mapRecord the map record
   * @return the previously publication-ready version of map record
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public MapRecord getPreviouslyPublishedVersionOfMapRecord(MapRecord mapRecord)
    throws Exception {

    MappingService mappingService = new MappingServiceJpa();

    // get the record revisions
    List<MapRecord> revisions =
        mappingService.getMapRecordRevisions(mapRecord.getId()).getMapRecords();

    // ensure revisions are sorted by descending timestamp
    Collections.sort(revisions, new Comparator<MapRecord>() {
      @Override
      public int compare(MapRecord mr1, MapRecord mr2) {
        return mr2.getLastModified().compareTo(mr1.getLastModified());
      }
    });

    // check assumption: last revision exists, at least two records must be
    // present
    if (revisions.size() < 2)
      throw new Exception(
          "Attempted to get the previously published version of map record with id "
              + mapRecord.getId() + ", " + mapRecord.getOwner().getName()
              + ", and concept id " + mapRecord.getConceptId()
              + ", but no previous revisions exist.");

    for (MapRecord revision : revisions) {
      System.out.println(revision.getId() + "\t" + revision.getWorkflowStatus()
          + "\t" + revision.getTimestamp());
    }

    // cycle over records until the previously
    // published/ready-for-publication
    // state record is found
    for (MapRecord revision : revisions) {
      // System.out.println("Previous record = " + revision.toString());
      if (revision.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
          || revision.getWorkflowStatus().equals(
              WorkflowStatus.READY_FOR_PUBLICATION)) {
        mappingService.close();
        return revision;
      }
    }

    mappingService.close();
    throw new Exception(
        "Could not retrieve previously published state of map record for concept "
            + mapRecord.getConceptId() + ", " + mapRecord.getConceptName());

  }

  /**
   * Returns the workflow status.
   * 
   * @param mapRecords the map records
   * @return the workflow status
   */
  @SuppressWarnings("static-method")
  public WorkflowStatus getWorkflowStatus(Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.NEW;
    for (MapRecord mr : mapRecords) {
      // System.out.println(mr.getWorkflowStatus());
      if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /**
   * Returns the map users.
   * 
   * @param mapRecords the map records
   * @return the map users
   */
  @SuppressWarnings("static-method")
  public Set<MapUser> getMapUsers(Set<MapRecord> mapRecords) {
    Set<MapUser> mapUsers = new HashSet<>();
    for (MapRecord mr : mapRecords) {
      mapUsers.add(mr.getOwner());
    }
    return mapUsers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * computeTargetTerminologyNotes
   * (org.ihtsdo.otf.mapping.helpers.TreePositionList)
   */
  @Override
  public void computeTargetTerminologyNotes(TreePositionList treePositions)
    throws Exception {

    // DO NOTHING -- Override in project specific handlers if necessary
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * getDependentModules()
   */
  @Override
  public Set<String> getDependentModules() {
    return new HashSet<>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * getModuleDependencyRefSetId()
   */
  @Override
  public String getModuleDependencyRefSetId() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * validateForRelease(org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember)
   */
  @Override
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception {
    // do nothing
    return new ValidationResultJpa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * getDefaultUpPropagatedMapRelation()
   */
  @Override
  public MapRelation getDefaultUpPropagatedMapRelation() throws Exception {
    // does not apply
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
   * getDefaultTargetNameForBlankTarget()
   */
  @Override
  public String getDefaultTargetNameForBlankTarget() {
    return "No target";
  }

}
