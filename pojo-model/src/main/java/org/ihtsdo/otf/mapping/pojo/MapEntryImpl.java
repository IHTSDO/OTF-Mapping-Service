package org.ihtsdo.otf.mapping.pojo;

import java.util.Set;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Reference implementation of {@link MapEntry}. Includes hibernate tags for
 * MEME database.
 * 
 * @author ${author}
 */
public class MapEntryImpl implements MapEntry {

  /** The id. */
  private Long id;

  /** The map notes. */
  private Set<MapNote> mapNotes;

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

  /** The map relation */
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
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getNotes()
   */
  @Override
  public Set<MapNote> getMapNotes() {
    return mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setNotes(java.util.List)
   */
  @Override
  public void setMapNotes(Set<MapNote> mapNotes) {
    this.mapNotes = mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#addNote(org.ihtsdo.otf.mapping.model
   * .MapNote)
   */
  @Override
  public void addMapNote(MapNote note) {
    mapNotes.add(note);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#removeNote(org.ihtsdo.otf.mapping
   * .model.MapNote)
   */
  @Override
  public void removeMapNote(MapNote note) {
    mapNotes.remove(note);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
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
  @Override
  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTargetName()
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
  @Override
  public void setTargetName(String targetName) {
    this.targetName = targetName;

  }

  @Override
  public MapRelation getMapRelation() {
    return this.mapRelation;
  }

  @Override
  public void setMapRelation(MapRelation mapRelation) {
    this.mapRelation = mapRelation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getAdvices()
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
  @Override
  public void removeMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.remove(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getRule()
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
  @Override
  public void setRule(String rule) {
    this.rule = rule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapPriority()
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
  @Override
  public void setMapPriority(int mapPriority) {
    this.mapPriority = mapPriority;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapRecord()
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
  @Override
  public void setMapRecord(MapRecord mapRecord) {
    this.mapRecord = mapRecord;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapGroup()
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
  @Override
  public void setMapGroup(int mapGroup) {
    this.mapGroup = mapGroup;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapBlock()
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
  @Override
  public void setMapBlock(int mapBlock) {
    this.mapBlock = mapBlock;

  }

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

  @Override
  public String toString() {
    return "MapEntryImpl [id=" + id + ", mapNotes=" + mapNotes + ", targetId="
        + targetId + ", targetName=" + targetName + ", mapAdvices="
        + mapAdvices + ", rule=" + rule + ", mapPriority=" + mapPriority
        + ", mapRelation=" + mapRelation + ", mapRecord=" + mapRecord
        + ", mapGroup=" + mapGroup + ", mapBlock=" + mapBlock + "]";
  }

}
