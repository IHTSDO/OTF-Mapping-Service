package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Reference implementation of {@link ProjectSpecificAlgorithmHandler}.
 */
public class DefaultProjectSpecificAlgorithmHandler implements
    ProjectSpecificAlgorithmHandler {

  /** The map project. */
  protected MapProject mapProject = null;

  /* see superclass */
  @Override
  public MapProject getMapProject() {
    return this.mapProject;
  }

  /* see superclass */
  @Override
  public void setMapProject(MapProject mapProject) {
    this.mapProject = mapProject;
  }

  /* see superclass */
  @Override
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return null;
  }

  /* see superclass */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    return null;
  }

  /* see superclass */
  @Override
  public ValidationResult validateRecord(MapRecord mapRecord) throws Exception {

    // Universal checks
    final ValidationResult validationResult = new ValidationResultJpa();
    validationResult.merge(performDefaultChecks(mapRecord));

    // Generally should be overridden
    validationResult.merge(validateTargetCodes(mapRecord));
    validationResult.merge(validateSemanticChecks(mapRecord));

    return validationResult;
  }

  /**
   * Perform validation checks that likely apply to all mappings. This is mostly
   * structural stuff and not semantically tied to any particular map.
   * 
   * @param mapRecord the map record
   * @return the validation result
   */
  public ValidationResult performDefaultChecks(MapRecord mapRecord) {
    Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);

    final ValidationResult validationResult = new ValidationResultJpa();

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

    // Verify that groups begin at index 1 and are sequential (i.e. no empty
    // groups)
    validationResult
        .merge(checkMapRecordGroupStructure(mapRecord, entryGroups));

    // Validation Check: verify correct positioning of TRUE rules
    validationResult.merge(checkMapRecordRules(mapRecord, entryGroups));

    // Validation Check: very higher map groups do not have only NC nodes
    validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));

    // Validation Check: verify entries are not duplicated
    validationResult.merge(checkMapRecordForDuplicateEntries(mapRecord));

    // Validation Check: verify advice values are valid for the project (this
    // can happen if "allowable map advice" changes without updating map
    // entries)
    validationResult.merge(checkMapRecordAdvices(mapRecord, entryGroups));

    // Validation Check: all entries are non-null (empty entries are empty
    // strings)
    validationResult.merge(checkMapRecordForNullTargetIds(mapRecord));

    return validationResult;
  }

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception {
    return new ValidationResultJpa();
  }

  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord)
    throws Exception {
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public ValidationResult compareMapRecords(MapRecord record1, MapRecord record2) {
    final ValidationResult validationResult = new ValidationResultJpa();
    
    if (record1 == null) {
      validationResult.addError("First record not supplied to comparison routine");
      return validationResult;
    }
    
    if (record2 == null) {
      validationResult.addError("Second record not supplied to comparison routine");
      return validationResult;
    }

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
    final Map<Integer, List<MapEntry>> groupToMapEntryList1 = new HashMap<>();
    for (final MapEntry entry : record1.getMapEntries()) {
      if (groupToMapEntryList1.containsKey(entry.getMapGroup())) {
        final List<MapEntry> entryList =
            groupToMapEntryList1.get(entry.getMapGroup());
        entryList.add(entry);
      } else {
        final List<MapEntry> entryList = new ArrayList<>();
        entryList.add(entry);
        groupToMapEntryList1.put(entry.getMapGroup(), entryList);
      }
    }
    final Map<Integer, List<MapEntry>> groupToMapEntryList2 = new HashMap<>();
    for (final MapEntry entry : record2.getMapEntries()) {
      if (groupToMapEntryList2.containsKey(entry.getMapGroup())) {
        final List<MapEntry> entryList =
            groupToMapEntryList2.get(entry.getMapGroup());
        entryList.add(entry);
      } else {
        final List<MapEntry> entryList = new ArrayList<>();
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
      final List<MapEntry> entries1 = groupToMapEntryList1.get(new Integer(i));
      final List<MapEntry> entries2 = groupToMapEntryList2.get(new Integer(i));

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
      final List<String> stringEntries1 = new ArrayList<>();
      final List<String> stringEntries2 = new ArrayList<>();
      for (final MapEntry entry1 : entries1) {
        stringEntries1.add(convertToString(entry1));
      }
      for (final MapEntry entry2 : entries2) {
        stringEntries2.add(convertToString(entry2));
      }

      // check for matching entries in different order
      boolean outOfOrderFlag = false;
      boolean missingEntry = false;

      System.out.println("strings1=" + stringEntries1);
      System.out.println("strings2=" + stringEntries2);
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

            validationResult.merge(checkAdviceDifferences(entries1.get(d),
                entries2.get(f)));
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

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {
    return true;
  }

  // ////////////////////
  // HELPER FUNCTIONS //
  // ////////////////////

  /**
   * Verify map records do not have null target ids
   * 
   * @param mapRecord the map record
   * @return the validation result
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForNullTargetIds(MapRecord mapRecord) {
    final ValidationResult validationResult = new ValidationResultJpa();

    for (final MapEntry me : mapRecord.getMapEntries()) {
      if (me.getTargetId() == null)
        validationResult.addError("Map entry at group " + me.getMapGroup()
            + ", priority " + me.getMapPriority()
            + " has no target (valid or empty) selected.");
    }

    return validationResult;
  }

  /**
   * Verify there are no empty map groups
   * 
   * @param mapRecord the map record
   * @param entryGroups the entry groups
   * @return the validation result
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordGroupStructure(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    final ValidationResult validationResult = new ValidationResultJpa();

    // get the list of groups
    final Set<Integer> mapGroups = entryGroups.keySet();

    // cycle over the expected group numbers
    for (int i = 1; i <= mapGroups.size(); i++) {
      if (!mapGroups.contains(i)) {
        validationResult.addError("Group " + i + " is empty");
      }
    }
    return validationResult;
  }

  /**
   * Verify no duplicate entries in the map.
   * 
   * @param mapRecord the map record
   * @return a list of errors detected
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForDuplicateEntries(MapRecord mapRecord) {
    ValidationResult validationResult = new ValidationResultJpa();
    final List<MapEntry> entries = mapRecord.getMapEntries();

    // cycle over all entries but last
    for (int i = 0; i < entries.size() - 1; i++) {

      // cycle over all entries after this one
      // NOTE: separated boolean checks for easier handling of possible null
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
    return validationResult;
  }

  /**
   * Verify proper use of TRUE rules (for rule based project). Verify no rules
   * on non-rule based projects.
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  public ValidationResult checkMapRecordRules(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    final ValidationResult validationResult = new ValidationResultJpa();

    // if not rule based, check for rules present
    if (!mapProject.isRuleBased()) {

      for (final MapEntry me : mapRecord.getMapEntries()) {
        if (me.getRule() != null && !me.getRule().isEmpty()) {
          validationResult
              .addError("Rule found for non-rule based project at map group "
                  + me.getMapGroup() + ", priority " + me.getMapPriority()
                  + ", rule specified is " + me.getRule() + ".");
        }
      }

      // otherwise check TRUE rules and gender rules (Female must be before
      // Male)
    } else {

      // cycle over the groups and note if there are both female and
      // male entries in a group
      for (final Integer key : entryGroups.keySet()) {

        int maleEntry = 0;
        int femaleEntry = 0;

        for (int i = 1; i < entryGroups.get(key).size() + 1; i++) {
          MapEntry entry = entryGroups.get(key).get(i - 1);
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
      for (final Integer key : entryGroups.keySet()) {

        for (final MapEntry mapEntry : entryGroups.get(key)) {

          Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).debug(
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

    return validationResult;
  }

  /**
   * Verify higher level map groups do not have only NC target codes.
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordNcNodes(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    final ValidationResult validationResult = new ValidationResultJpa();

    // if only one group, return empty validation result (also covers
    // non-group-structure projects)
    if (entryGroups.keySet().size() == 1)
      return validationResult;

    // otherwise cycle over the high-level groups (i.e. all but first group)
    for (int group : entryGroups.keySet()) {

      if (group != 1) {
        List<MapEntry> entries = entryGroups.get(group);

        boolean isValidGroup = false;
        for (final MapEntry entry : entries) {
          if (entry.getTargetId() != null && !entry.getTargetId().equals(""))
            isValidGroup = true;
        }

        if (!isValidGroup) {
          validationResult.addError("High-level group "
              + Integer.toString(group) + " has no entries with targets");
        }
      }
    }
    for (final String error : validationResult.getErrors()) {
      Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).debug(
          "    " + error);
    }
    return validationResult;
  }

  /**
   * Verify advices are valid for the project
   * 
   * @param mapRecord the map record
   * @param entryGroups the binned entry lists by group
   * @return a list of errors detected
   */
  public ValidationResult checkMapRecordAdvices(MapRecord mapRecord,
    Map<Integer, List<MapEntry>> entryGroups) {

    final ValidationResult validationResult = new ValidationResultJpa();
    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {
      for (final MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
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
    for (final MapEntry entry : mapRecord.getMapEntries()) {
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
   * @param entry1 the entry1
   * @param entry2 the entry2
   * @return the advice differences
   */
  @SuppressWarnings("static-method")
  private ValidationResult checkAdviceDifferences(MapEntry entry1,
    MapEntry entry2) {

    final ValidationResult result = new ValidationResultJpa();
    final Comparator<Object> advicesComparator = new Comparator<Object>() {
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

    final List<MapAdvice> advices1 = new ArrayList<>(entry1.getMapAdvices());
    Collections.sort(advices1, advicesComparator);
    final List<MapAdvice> advices2 = new ArrayList<>(entry2.getMapAdvices());
    Collections.sort(advices2, advicesComparator);

    for (int i = 0; i < Math.max(advices1.size(), advices2.size()); i++) {
      try {
        if (advices1.get(i) == null && advices2.get(i) != null)
          continue;
        if (advices1.get(i) != null && advices2.get(i) == null)
          continue;
        if (!advices1.get(i).equals(advices2.get(i))) {
          result.addError("Map Advice is Different: "
              + (advices1.get(i).getName() + " vs. " + advices2.get(i)
                  .getName()));
        }
      } catch (IndexOutOfBoundsException e) {
        result.addError("Map Advice is Different: "
            + (advices1.size() > i ? advices1.get(i).getName() : "No advice")
            + " vs. "
            + (advices2.size() > i ? advices2.get(i).getName() : "No advice"));

      }
    }

    return result;
  }

  /**
   * Convert to string.
   * 
   * @param mapEntry the map entry
   * @return the string
   */
  @SuppressWarnings("static-method")
  private String convertToString(MapEntry mapEntry) {

    final Comparator<Object> advicesComparator = new Comparator<Object>() {
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

    final List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
    Collections.sort(advices, advicesComparator);

    final StringBuffer sb = new StringBuffer();
    sb.append(mapEntry.getTargetId()
        + " "
        + mapEntry.getRule()
        + " "
        + (mapEntry.getMapRelation() != null ? mapEntry.getMapRelation()
            .getId() : ""));
    for (final MapAdvice mapAdvice : advices) {
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
    for (final MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }


  @Override
  public void computeIdentifyAlgorithms(MapRecord mapRecord) throws Exception {
    // Default behavior is to do nothing
  }

  /* see superclass */
  @Override
  public void computeTargetTerminologyNotes(List<TreePosition> treePositions)
    throws Exception {

    // DO NOTHING -- Override in project specific handlers if necessary
  }

  /* see superclass */
  @Override
  public Set<String> getDependentModules() {
    return new HashSet<>();
  }

  /* see superclass */
  @Override
  public String getModuleDependencyRefSetId() {
    return null;
  }

  /* see superclass */
  @Override
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception {
    // do nothing
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public MapRelation getDefaultUpPropagatedMapRelation() throws Exception {
    // does not apply
    return null;
  }

  /* see superclass */
  @Override
  public String getDefaultTargetNameForBlankTarget() {
    return "No target";
  }

  /* see superclass */
  @Override
  public Map<String, String> getAllTerminologyNotes() throws Exception {
    return new HashMap<>();
  }

}
