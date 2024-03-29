/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.model;

import java.util.Set;

/**
 * Generically represents an entry in a {@link MapRecord}.
 */
public interface MapEntry {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the id in string form.
   * 
   * @return the string object id
   */
  public String getObjectId();

  /**
   * Returns the target.
   * 
   * @return the target
   */
  public String getTargetId();

  /**
   * Sets the target.
   * 
   * @param targetId the target terminology Id
   */
  public void setTargetId(String targetId);

  /**
   * Returns the target name.
   * 
   * @return the target name
   */
  public String getTargetName();

  /**
   * Sets the target name.
   * 
   * @param targetName the target name
   */
  public void setTargetName(String targetName);

  // /**
  // * Gets the terminology note.
  // * This is a place where a terminology like ICD10 can decorate
  // * the code with asterisk/dagger.
  // *
  // * @return the terminology note
  // */
  // public String getTerminologyNote();
  //
  // /**
  // * Lists the terminology note.
  // *
  // * @param terminologyNote the new terminology note
  // */
  // public void setTerminologyNote(String terminologyNote);

  /**
   * Returns the additional map entry info.
   * 
   * @return the additional map entry info
   */
  public Set<AdditionalMapEntryInfo> getAdditionalMapEntryInfos();

  /**
   * Sets the additionalMapEntryInfo.
   * 
   * @param additionalMapEntryInfo the additional Map Entry Info
   */
  public void setAdditionalMapEntryInfos(Set<AdditionalMapEntryInfo> additionalMapEntryInfo);

  /**
   * Adds the additional map entry info.
   *
   * @param additionalMapEntryInfo the additional map entry info
   */
  public void addAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo);

  /**
   * Removes the additional map entry info.
   *
   * @param additionalMapEntryInfo the additional map entry info
   */
  public void removeAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo);

  /**
   * Returns the advices.
   * 
   * @return the advices
   */
  public Set<MapAdvice> getMapAdvices();

  /**
   * Sets the advices.
   * 
   * @param mapAdvices the advices
   */
  public void setMapAdvices(Set<MapAdvice> mapAdvices);

  /**
   * Adds the advice.
   * 
   * @param mapAdvice the map advice
   */
  public void addMapAdvice(MapAdvice mapAdvice);

  /**
   * Removes the advice.
   * 
   * @param mapAdvice the map advice
   */
  public void removeMapAdvice(MapAdvice mapAdvice);

  /**
   * Returns the rule.
   * 
   * @return the rule
   */
  public String getRule();

  /**
   * Sets the rule.
   * 
   * @param rule the rule
   */
  public void setRule(String rule);

  /**
   * Returns the index, the map priority.
   * 
   * @return the index
   */
  public int getMapPriority();

  /**
   * Sets the index, the map priority.
   * 
   * @param index the index
   */
  public void setMapPriority(int index);

  /**
   * Gets the map group.
   * 
   * @return the map group
   */
  public int getMapGroup();

  /**
   * Sets the map group.
   * 
   * @param mapGroup the map group
   */
  public void setMapGroup(int mapGroup);

  /**
   * Gets the map block.
   * 
   * @return the map block
   */
  public int getMapBlock();

  /**
   * Sets the map block.
   * 
   * @param mapBlock the map block
   */
  public void setMapBlock(int mapBlock);

  /**
   * Returns the map record.
   * 
   * @return the map record
   */
  public MapRecord getMapRecord();

  /**
   * Sets the map record.
   * 
   * @param mapRecord the map record
   */
  public void setMapRecord(MapRecord mapRecord);

  /**
   * Gets the map relation.
   * 
   * @return the map relation
   */
  public MapRelation getMapRelation();

  /**
   * Sets the map relation.
   * 
   * @param mapRelation the new map relation
   */
  public void setMapRelation(MapRelation mapRelation);

  /**
   * Checks if entry is functionally equivalent to another entry, based on: -
   * targetId - rule - relation - advice
   * @param me the map entry
   * @return true, if is equivalent
   */
  public boolean isEquivalent(MapEntry me);
}
