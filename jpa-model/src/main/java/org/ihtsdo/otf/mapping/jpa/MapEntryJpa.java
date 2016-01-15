package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
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

  /** The map advices. */
  @ManyToMany(targetEntity = MapAdviceJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = MapAdviceJpa.class)
  private Set<MapAdvice> mapAdvices = new HashSet<>();

  /** The target. */
  @Column(nullable = true, length = 4000)
  private String targetId;

  /** The target name. */
  @Column(nullable = true, length = 4000)
  @Analyzer(definition = "noStopWord")
  private String targetName;

  /** The rule. */
  @Column(nullable = true, length = 4000)
  private String rule;

  /** The map priority. */
  @Column(nullable = false)
  private int mapPriority;

  /**  The map relation. */
  @OneToOne(targetEntity = MapRelationJpa.class, fetch = FetchType.EAGER)
  @IndexedEmbedded(targetElement = MapRelationJpa.class)
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
   * Constructor using fields.
   * 
   * @param id the id
   * @param mapRecord the map record
   * @param mapAdvices the map advices
   * @param targetId the target id
   * @param targetName the target name
   * @param rule the rule
   * @param mapPriority the map priority
   * @param mapRelation the map relation
   * @param mapBlock the map block
   * @param mapGroup the map group
   */
  public MapEntryJpa(Long id, MapRecord mapRecord, Set<MapAdvice> mapAdvices,
      String targetId, String targetName, String rule, int mapPriority,
      MapRelation mapRelation, int mapBlock, int mapGroup) {
    super();
    this.id = id;
    this.mapRecord = mapRecord;
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

    // System.out.println("Deep copying entry.");

    // copy id, otherwise leave null
    if (keepIds) {
      this.id = mapEntry.getId();
    }
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
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
   */
  /* see superclass */
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
  /* see superclass */
  @Override
  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getTargetName()
   */
  /* see superclass */
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
  /* see superclass */
  @Override
  public void setTargetName(String targetName) {
    this.targetName = targetName;

  }

  /* see superclass */
  @Override
  @XmlElement(type = MapRelationJpa.class, name = "mapRelation")
  public MapRelation getMapRelation() {
    return mapRelation;
  }

  /* see superclass */
  @Override
  public void setMapRelation(MapRelation mapRelation) {
    this.mapRelation = mapRelation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapAdvices()
   */
  /* see superclass */
  @XmlElement(type = MapAdviceJpa.class, name = "mapAdvice")
  @Override
  public Set<MapAdvice> getMapAdvices() {
    if (mapAdvices == null)
      mapAdvices = new HashSet<>();// ensures proper serialization
    return mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapAdvices(java.util.Set)
   */
  /* see superclass */
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
  /* see superclass */
  @Override
  public void addMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.add(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#removeMapAdvice(org.ihtsdo.otf.
   * mapping .model.MapAdvice)
   */
  /* see superclass */
  @Override
  public void removeMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.remove(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getRule()
   */
  /* see superclass */
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
  /* see superclass */
  @Override
  public void setRule(String rule) {
    this.rule = rule;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getIndex()
   */
  /* see superclass */
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
  /* see superclass */
  @Override
  public void setMapPriority(int mapPriority) {
    this.mapPriority = mapPriority;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapRecord()
   */
  /* see superclass */
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
  /* see superclass */
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
   * serialized entry with only mapRecordId.
   *
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
  /* see superclass */
  @Override
  public int getMapGroup() {
    return this.mapGroup;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapGroup(int)
   */
  /* see superclass */
  @Override
  public void setMapGroup(int mapGroup) {
    this.mapGroup = mapGroup;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapBlock()
   */
  /* see superclass */
  @Override
  public int getMapBlock() {
    return this.mapBlock;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapBlock(int)
   */
  /* see superclass */
  @Override
  public void setMapBlock(int mapBlock) {
    this.mapBlock = mapBlock;

  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
    result = prime * result + mapBlock;
    result = prime * result + mapGroup;
    result = prime * result + mapPriority;

    // note: use map record id instead of map record to prevent hashCode()
    // circular reference chain
    result =
        prime * result
            + ((mapRecord.getId() == null) ? 0 : mapRecord.getId().hashCode());
    result =
        prime * result + ((mapRelation == null) ? 0 : mapRelation.hashCode());
    result = prime * result + ((rule == null) ? 0 : rule.hashCode());
    result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
    result =
        prime * result + ((targetName == null) ? 0 : targetName.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapEntryJpa other = (MapEntryJpa) obj;
    if (mapAdvices == null) {
      if (other.mapAdvices != null)
        return false;
    } else if (!mapAdvices.equals(other.mapAdvices))
      return false;
    if (mapBlock != other.mapBlock)
      return false;
    if (mapGroup != other.mapGroup)
      return false;
    if (mapPriority != other.mapPriority)
      return false;

    // note: only compare map record id, otherwise equals() circular
    // reference chain
    if (mapRecord.getId() == null) {
      if (other.mapRecord.getId() != null)
        return false;
    } else if (!mapRecord.getId().equals(other.mapRecord.getId()))
      return false;
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

  /* see superclass */
  @Override
  public String toString() {
    return "MapEntryJpa [id=" + id + ", mapRecord="
        + (mapRecord == null ? "" : mapRecord.getId()) + ", mapAdvices="
        + (mapAdvices == null ? "null" : mapAdvices) + ", targetId=" + targetId
        + ", targetName=" + targetName + ", rule=" + rule + ", mapPriority="
        + mapPriority + ", mapRelation="
        + (mapRelation == null ? "null" : mapRelation) + ", mapBlock="
        + mapBlock + ", mapGroup=" + mapGroup + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapEntry#isEquivalent(org.ihtsdo.otf.mapping
   * .model.MapEntry)
   */
  /* see superclass */
  @Override
  public boolean isEquivalent(MapEntry me) {

    // if comparison entry is null, return false
    if (me == null)
      return false;

    // targets must be equal
    final String id1 = this.targetId == null ? "" : this.targetId;
    final String id2 = me.getTargetId() == null ? "" : me.getTargetId();
    if (!id1.equals(id2)) {
      return false;
    }

    // rules must be identical
    if (this.rule == null && me.getRule() != null)
      return false;
    if (!this.rule.equals(me.getRule()))
      return false;

    // relation must be identical
    if (this.mapRelation != null) {

      // if both non-null, return false if non equal
      if (me.getMapRelation() != null) {
        if (!this.mapRelation.equals(me.getMapRelation())) {
          return false;
        }

        // return false if this relation is non-null and me's relation
        // is null
      } else {
        return false;
      }

      // if this relation is null and me's relation is non-null, return
      // false
    } else if (me.getMapRelation() != null) {
      return false;
    }

    // System.out.println("  Relations equal");
    // advices must be identical
    if (this.mapAdvices == null && me.getMapAdvices() != null) {
      return false;
    } else if (this.mapAdvices != null && me.getMapAdvices() == null) {
      return false;
    } else if (mapAdvices != null
        && mapAdvices.size() != me.getMapAdvices().size()) {
      return false;
    } else if (mapAdvices != null) {
      for (MapAdvice ma : this.mapAdvices) {
        if (!me.getMapAdvices().contains(ma))
          return false;
      }
    }

    // System.out.println("  Advices equal");

    // System.out.println("Entries equivalent.");

    return true;
  }

}
