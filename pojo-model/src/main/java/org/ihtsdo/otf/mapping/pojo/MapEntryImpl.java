package org.ihtsdo.otf.mapping.pojo;

import java.util.Set;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Reference implementation of {@link MapEntry}.
 */
public class MapEntryImpl implements MapEntry {

  /** The id. */
  private Long id;

  /** The target. */
  private String targetId;

  /** The target name. */
  private String targetName;

  /** The map advices. */
  private Set<MapAdvice> mapAdvices;

  /** The rule. */
  private String rule;

  /** The index. */
  private int mapPriority;

  /**  The map relation. */
  private MapRelation mapRelation;

  /** The map record. */
  private MapRecord mapRecord;

  /** The map group. */
  private int mapGroup;

  /** The map block. */
  private int mapBlock;

  /**
   * Return the id.
   * 
   * @return the id
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.
   * 
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form.
   * 
   * @return the id in string form
   */
  @XmlID
  @Override
  public String getObjectId() {
    return id.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
   */
  /**
   * Returns the target id.
   *
   * @return the target id
   */
  @Override
  public String getTargetId() {
    return targetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setTarget(java.lang.String)
   */
  /**
   * Sets the target id.
   *
   * @param targetId the target id
   */
  @Override
  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTargetName()
   */
  /**
   * Returns the target name.
   *
   * @return the target name
   */
  @Override
  public String getTargetName() {
    return this.targetName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setTargetName(java.lang.String)
   */
  /**
   * Sets the target name.
   *
   * @param targetName the target name
   */
  @Override
  public void setTargetName(String targetName) {
    this.targetName = targetName;

  }

  /**
   * Returns the map relation.
   *
   * @return the map relation
   */
  @Override
  public MapRelation getMapRelation() {
    return this.mapRelation;
  }

  /**
   * Sets the map relation.
   *
   * @param mapRelation the map relation
   */
  @Override
  public void setMapRelation(MapRelation mapRelation) {
    this.mapRelation = mapRelation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getAdvices()
   */
  /**
   * Returns the map advices.
   *
   * @return the map advices
   */
  @Override
  public Set<MapAdvice> getMapAdvices() {
    return mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setAdvices(java.util.Set)
   */
  /**
   * Sets the map advices.
   *
   * @param mapAdvices the map advices
   */
  @Override
  public void setMapAdvices(Set<MapAdvice> mapAdvices) {
    this.mapAdvices = mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#addAdvice(org.ihtsdo.otf.mapping.
   * model.MapAdvice)
   */
  /**
   * Adds the map advice.
   *
   * @param mapAdvice the map advice
   */
  @Override
  public void addMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.add(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#removeAdvice(org.ihtsdo.otf.mapping
   * .model.MapAdvice)
   */
  /**
   * Removes the map advice.
   *
   * @param mapAdvice the map advice
   */
  @Override
  public void removeMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.remove(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getRule()
   */
  /**
   * Returns the rule.
   *
   * @return the rule
   */
  @Override
  public String getRule() {
    return rule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setRule(java.lang.String)
   */
  /**
   * Sets the rule.
   *
   * @param rule the rule
   */
  @Override
  public void setRule(String rule) {
    this.rule = rule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapPriority()
   */
  /**
   * Returns the map priority.
   *
   * @return the map priority
   */
  @Override
  public int getMapPriority() {
    return mapPriority;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapPriority(java.lang.String)
   */
  /**
   * Sets the map priority.
   *
   * @param mapPriority the map priority
   */
  @Override
  public void setMapPriority(int mapPriority) {
    this.mapPriority = mapPriority;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapRecord()
   */
  /**
   * Returns the map record.
   *
   * @return the map record
   */
  @Override
  public MapRecord getMapRecord() {
    return mapRecord;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#setMapRecord(org.ihtsdo.otf.mapping
   * .model.MapRecord)
   */
  /**
   * Sets the map record.
   *
   * @param mapRecord the map record
   */
  @Override
  public void setMapRecord(MapRecord mapRecord) {
    this.mapRecord = mapRecord;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapGroup()
   */
  /**
   * Returns the map group.
   *
   * @return the map group
   */
  @Override
  public int getMapGroup() {
    return this.mapGroup;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapGroup(int)
   */
  /**
   * Sets the map group.
   *
   * @param mapGroup the map group
   */
  @Override
  public void setMapGroup(int mapGroup) {
    this.mapGroup = mapGroup;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapBlock()
   */
  /**
   * Returns the map block.
   *
   * @return the map block
   */
  @Override
  public int getMapBlock() {
    return this.mapBlock;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapBlock(int)
   */
  /**
   * Sets the map block.
   *
   * @param mapBlock the map block
   */
  @Override
  public void setMapBlock(int mapBlock) {
    this.mapBlock = mapBlock;

  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((mapRelation == null) ? 0 : mapRelation.hashCode());
    result = prime * result + ((rule == null) ? 0 : rule.hashCode());
    result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
    result =
        prime * result + ((targetName == null) ? 0 : targetName.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapEntryImpl other = (MapEntryImpl) obj;
    if (mapRelation == null) {
      if (other.mapRelation != null)
        return false;
    } else if (!mapRelation.equals(other.mapRelation))
      return false;
    if (rule == null) {
      if (other.rule != null)
        return false;
    } else if (!rule.equals(other.rule))
      return false;
    if (targetId == null) {
      if (other.targetId != null)
        return false;
    } else if (!targetId.equals(other.targetId))
      return false;
    if (targetName == null) {
      if (other.targetName != null)
        return false;
    } else if (!targetName.equals(other.targetName))
      return false;
    return true;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "MapEntryImpl [id=" + id + ", targetId=" + targetId
        + ", targetName=" + targetName + ", mapAdvices=" + mapAdvices
        + ", rule=" + rule + ", mapPriority=" + mapPriority + ", mapRelation="
        + mapRelation + ", mapRecord=" + mapRecord + ", mapGroup=" + mapGroup
        + ", mapBlock=" + mapBlock + "]";
  }

  /**
   * Indicates whether or not equivalent is the case.
   *
   * @param me the me
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @Override
  public boolean isEquivalent(MapEntry me) {
    return this.equals(me);
  }

}
