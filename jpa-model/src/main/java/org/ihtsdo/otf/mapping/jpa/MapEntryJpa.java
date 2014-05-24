package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Map Entry Jpa object.
 * 
 */
@Entity
@Table(name = "map_entries")
@Audited
@XmlRootElement(name = "mapEntry")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapEntryJpa implements MapEntry {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The map record. */
  @ManyToOne(targetEntity = MapRecordJpa.class, optional = false)
  @ContainedIn
  private MapRecord mapRecord;

  /** The map notes. */
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, targetEntity = MapNoteJpa.class)
  @IndexedEmbedded(targetElement = MapNoteJpa.class)
  private Set<MapNote> mapNotes = new HashSet<>();

  /** The map advices. */
  @ManyToMany(targetEntity = MapAdviceJpa.class, fetch = FetchType.EAGER)
  @IndexedEmbedded(targetElement = MapAdviceJpa.class)
  private Set<MapAdvice> mapAdvices = new HashSet<>();

  /** The target. */
  @Column(nullable = true)
  private String targetId;

  /** The target name. */
  @Column(nullable = true)
  private String targetName;

  /** The rule. */
  @Column(nullable = true, length = 4000)
  private String rule;

  /** The map priority. */
  @Column(nullable = false)
  private int mapPriority;

  @OneToOne(targetEntity = MapRelationJpa.class, fetch = FetchType.EAGER)
  private MapRelation mapRelation;

  /** The mapBlock. */
  @Column(nullable = false)
  private int mapBlock;

  /** The index (map priority). */
  @Column(nullable = false)
  private int mapGroup;

  /**
   * default constructor.
   */
  public MapEntryJpa() {
    // empty
  }
  
  
  /**
   * Constructor using fields 
   * 
   * @param id
   * @param mapRecord
   * @param mapNotes
   * @param mapAdvices
   * @param targetId
   * @param targetName
   * @param rule
   * @param mapPriority
   * @param mapRelation
   * @param mapBlock
   * @param mapGroup
   */
  public MapEntryJpa(Long id, MapRecord mapRecord, Set<MapNote> mapNotes,
		Set<MapAdvice> mapAdvices, String targetId, String targetName,
		String rule, int mapPriority, MapRelation mapRelation, int mapBlock,
		int mapGroup) {
	super();
	this.id = id;
	this.mapRecord = mapRecord;
	this.mapNotes = mapNotes;
	this.mapAdvices = mapAdvices;
	this.targetId = targetId;
	this.targetName = targetName;
	this.rule = rule;
	this.mapPriority = mapPriority;
	this.mapRelation = mapRelation;
	this.mapBlock = mapBlock;
	this.mapGroup = mapGroup;
}



/**
 * Deep copy constructor.
 *
 * @param mapEntry the map entry
 * @param keepIds the keep ids
 */
  public MapEntryJpa(MapEntry mapEntry, boolean keepIds) {
	  super();
	  
	  System.out.println("Deep copying entry.");

	  // if deep copy not indicated, copy id, otherwise leave null
	  if (keepIds == false) this.id = mapEntry.getId();
	  this.mapRecord = mapEntry.getMapRecord();

	  // copy basic type fields (non-persisted objects)
	  this.targetId = mapEntry.getTargetId();
	  this.targetName = mapEntry.getTargetName();
	  this.rule = mapEntry.getRule();
	  this.mapPriority = mapEntry.getMapPriority();
	  this.mapBlock = mapEntry.getMapBlock();
	  this.mapGroup = mapEntry.getMapGroup();
	
	  // copy advices
	  for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
		  addMapAdvice(new MapAdviceJpa(mapAdvice));
	  }
	  
	  // copy entries
	  if (mapEntry.getMapRelation() != null) {
		  this.mapRelation = new MapRelationJpa(mapEntry.getMapRelation());
	  }
	  
	  // copy notes
	  for (MapNote mapNote : mapRecord.getMapNotes()) {
		  addMapNote(new MapNoteJpa(mapNote, keepIds));
	  }
	  
	  System.out.println("  " + toString());
  }

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
    return (this.id == null ? null : id.toString());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapNotes()
   */
  @Override
  @XmlElement(type = MapNoteJpa.class, name = "mapNote")
  public Set<MapNote> getMapNotes() {
    return mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapNotes(java.util.Set)
   */
  @Override
  public void setMapNotes(Set<MapNote> notes) {
    this.mapNotes = notes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#addMapNote(org.ihtsdo.otf.mapping
   * .model.MapNote)
   */
  @Override
  public void addMapNote(MapNote note) {
    mapNotes.add(note);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#removeMapNote(org.ihtsdo.otf.mapping
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
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
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
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
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
  @XmlElement(type = MapRelationJpa.class, name = "mapRelation")
  public MapRelation getMapRelation() {
    return mapRelation;
  }

  @Override
  public void setMapRelation(MapRelation mapRelation) {
    this.mapRelation = mapRelation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapAdvices()
   */
  @XmlElement(type = MapAdviceJpa.class, name = "mapAdvice")
  @Override
  public Set<MapAdvice> getMapAdvices() {
    return mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapAdvices(java.util.Set)
   */
  @Override
  public void setMapAdvices(Set<MapAdvice> mapAdvices) {
    this.mapAdvices = mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#addMapAdvice(org.ihtsdo.otf.mapping
   * .model.MapAdvice)
   */
  @Override
  public void addMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.add(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#removeMapAdvice(org.ihtsdo.otf.mapping
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
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
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
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getIndex()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public int getMapPriority() {
    return mapPriority;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setIndex(java.lang.String)
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
  @XmlTransient
  @Override
  public MapRecord getMapRecord() {
    return this.mapRecord;
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

  /**
   * Returns the map record id.
   * 
   * @return the map record id
   */
  @XmlElement
  public String getMapRecordId() {
    return mapRecord != null ? mapRecord.getObjectId() : null;
  }

  /**
   * Sets the map record based on serialized id Necessary when receiving a
   * serialized entry with only mapRecordId
   * @param mapRecordId the map record id
   */
  public void setMapRecordId(Long mapRecordId) {
    if (this.mapRecord == null) {
      Logger.getLogger(MapEntryJpa.class).info(
          "Setting map record id to " + mapRecordId.toString());
      this.mapRecord = new MapRecordJpa();
      this.mapRecord.setId(mapRecordId);
    }
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
    MapEntryJpa other = (MapEntryJpa) obj;
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
    return "MapEntryJpa [id=" + id + ", mapRecord=" + mapRecord.getId().toString() + ", mapNotes="
        + mapNotes + ", mapAdvices=" + mapAdvices + ", targetId=" + targetId
        + ", targetName=" + targetName + ", rule=" + rule + ", mapPriority="
        + mapPriority + ", mapRelation=" + mapRelation + ", mapBlock="
        + mapBlock + ", mapGroup=" + mapGroup + "]";
  }

}
