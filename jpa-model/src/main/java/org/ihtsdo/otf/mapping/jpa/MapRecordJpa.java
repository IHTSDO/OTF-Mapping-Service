package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.StandardFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.ihtsdo.otf.mapping.helpers.CollectionToCSVBridge;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Map Record Jpa object.
 *
 * @author ${author}
 */
@Entity
// @UniqueConstraint here is being used to create an index, not to enforce
// uniqueness
@Table(name = "map_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "mapProjectId", "id"
    }), @UniqueConstraint(columnNames = {
        "conceptId", "id"
    }), @UniqueConstraint(columnNames = {
        "owner_id", "id"
    })
})
@Audited
@Indexed
@AnalyzerDef(name = "noStopWord", tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class), filters = {
    @TokenFilterDef(factory = StandardFilterFactory.class),
    @TokenFilterDef(factory = LowerCaseFilterFactory.class)
})
@XmlRootElement(name = "mapRecord")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapRecordJpa implements MapRecord {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The owner. */
  @ManyToOne(targetEntity = MapUserJpa.class, fetch = FetchType.EAGER)
  @JoinColumn(nullable = false)
  private MapUser owner;

  /** The timestamp. */
  @Column(nullable = false)
  private Long timestamp = (new Date()).getTime();

  /** The user last modifying this record. */
  @ManyToOne(targetEntity = MapUserJpa.class, fetch = FetchType.EAGER)
  @JoinColumn(nullable = false)
  private MapUser lastModifiedBy;

  /** The time at which the last user modified this record. */
  @Column(nullable = false)
  private Long lastModified = (new Date()).getTime();

  /** The map project id. */
  @Column(nullable = true)
  private Long mapProjectId;

  /** The concept id. */
  @Column(nullable = false)
  private String conceptId;

  /** The concept name. */
  @Column(nullable = false)
  private String conceptName;

  /** The count descendant concepts. */
  @Column(nullable = false)
  private Long countDescendantConcepts;

  /** The map entries. */
  @OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity = MapEntryJpa.class)
  @IndexedEmbedded(targetElement = MapEntryJpa.class)
  private List<MapEntry> mapEntries = new ArrayList<>();

  /** The map notes. */
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity = MapNoteJpa.class)
  private Set<MapNote> mapNotes = new HashSet<>();

  /** The map principles. */
  @ManyToMany(targetEntity = MapPrincipleJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = MapPrincipleJpa.class)
  private Set<MapPrinciple> mapPrinciples = new HashSet<>();

  /** The originIds. */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "map_records_origin_ids", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Set<Long> originIds = new HashSet<>();

  /** Indicates whether the record is flagged for map lead review. */
  @Column(unique = false, nullable = false)
  private boolean flagForMapLeadReview = false;

  /** Indicates whether the record is flagged for editorial review. */
  @Column(unique = false, nullable = false)
  private boolean flagForEditorialReview = false;

  /** Indicates if the record is flagged for consensus review. */
  @Column(unique = false, nullable = false)
  private boolean flagForConsensusReview = false;

  /** The workflow status. */
  @Enumerated(EnumType.STRING)
  private WorkflowStatus workflowStatus;

  /**  Whether this record has discrepancy review. */
  @Transient
  private boolean isDiscrepancyReview = false;

  /** The labels for this map record. */
  @ElementCollection
  @CollectionTable(name = "map_records_labels", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  // treat labels as a single field called labels
  @Field(bridge = @FieldBridge(impl = CollectionToCSVBridge.class))
  private Set<String> labels = new HashSet<>();

  /** The reasons for conflict for this map record. */
  @ElementCollection
  @CollectionTable(name = "map_records_reasons", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  // treat reasons as a single field called reasonsForConflict
  @Field(bridge = @FieldBridge(impl = CollectionToCSVBridge.class))
  private Set<String> reasonsForConflict = new HashSet<>();
  
  /**
   * Default constructor.
   */
  public MapRecordJpa() {
  }

  /**
   * Deep copy constructor for Map Record Instantiates a {@link MapRecordJpa}
   * from the specified parameters.
   *
   * @param mapRecord the map record to be copied
   * @param keepIds true: copy persisted objects into new JPA objects
   */
  public MapRecordJpa(MapRecord mapRecord, boolean keepIds) {

    // if deep copy not indicated, copy id and timestamp
    if (keepIds) {
      this.id = mapRecord.getId();
      this.timestamp = mapRecord.getTimestamp();
    }

    // copy basic type fields (non-persisted objects)
    this.mapProjectId = mapRecord.getMapProjectId();
    this.conceptId = mapRecord.getConceptId();
    this.conceptName = mapRecord.getConceptName();
    this.countDescendantConcepts = mapRecord.getCountDescendantConcepts();
    this.originIds = mapRecord.getOriginIds();
    this.flagForMapLeadReview = mapRecord.isFlagForMapLeadReview();
    this.flagForEditorialReview = mapRecord.isFlagForEditorialReview();
    this.flagForConsensusReview = mapRecord.isFlagForConsensusReview();
    this.workflowStatus = mapRecord.getWorkflowStatus();
    this.lastModified = (new Date()).getTime(); // overwrite last modified by

    // copy objects/collections excluded from deep copy (i.e. retain persistence
    // references)
    for (MapPrinciple mapPrinciple : mapRecord.getMapPrinciples()) {
      addMapPrinciple(new MapPrincipleJpa(mapPrinciple));
    }

    this.owner = new MapUserJpa(mapRecord.getOwner());
    this.lastModifiedBy = new MapUserJpa(mapRecord.getLastModifiedBy());

    // copy objects/collections with deep copy potential
    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      addMapEntry(new MapEntryJpa(mapEntry, keepIds));
    }
    for (MapNote mapNote : mapRecord.getMapNotes()) {
      addMapNote(new MapNoteJpa(mapNote, keepIds));
    }
    for (String label : mapRecord.getLabels()) {
      addLabel(label);
    }
    
    for (String reason : mapRecord.getReasonsForConflict()) {
    	addReasonForConflict(reason);
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
    return id.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getOwner()
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "owner")
  public MapUser getOwner() {
    return owner;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setOwner(org.ihtsdo.otf.mapping.
   * model.MapUser)
   */
  @Override
  public void setOwner(MapUser owner) {
    this.owner = owner;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getTimestamp()
   */
  @Override
  public Long getTimestamp() {
    return timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setTimestamp(java.lang.Long)
   */
  @Override
  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getlastModifiedBy()
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "lastModifiedBy")
  public MapUser getLastModifiedBy() {
    return lastModifiedBy;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setLastModifiedBy(org.ihtsdo.otf
   * .mapping.model.MapUser)
   */
  @Override
  public void setLastModifiedBy(MapUser mapUser) {
    this.lastModifiedBy = mapUser;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getLastModified()
   */
  @Override
  public Long getLastModified() {
    return this.lastModified;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setLastModified(java.lang.Long)
   */
  @Override
  public void setLastModified(Long lastModified) {
    this.lastModified = lastModified;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapProjectId()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public Long getMapProjectId() {
    return mapProjectId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapProjectId(java.lang.Long)
   */
  @Override
  public void setMapProjectId(Long mapProjectId) {
    this.mapProjectId = mapProjectId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getConceptId()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getConceptId() {
    return conceptId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setConceptId(java.lang.String)
   */
  @Override
  public void setConceptId(String conceptId) {
    this.conceptId = conceptId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getConceptName()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  @Analyzer(definition = "noStopWord")
  public String getConceptName() {
    return this.conceptName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setConceptName(java.lang.String)
   */
  @Override
  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getCountDescendantConcepts()
   */
  @Override
  public Long getCountDescendantConcepts() {
    return countDescendantConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setCountDescendantConcepts(java.
   * lang.Long)
   */
  @Override
  public void setCountDescendantConcepts(Long countDescendantConcepts) {
    this.countDescendantConcepts = countDescendantConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapNotes()
   */
  @Override
  @XmlElement(type = MapNoteJpa.class, name = "mapNote")
  public Set<MapNote> getMapNotes() {
    if (mapNotes == null)
      mapNotes = new HashSet<>(); // ensures proper deserialization
    return mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapNotes(java.util.Set)
   */
  @Override
  public void setMapNotes(Set<MapNote> mapNotes) {
    this.mapNotes = mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#addMapNote(org.ihtsdo.otf.mapping
   * .model.MapNote)
   */
  @Override
  public void addMapNote(MapNote mapNote) {
    mapNotes.add(mapNote);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#removeMapNote(org.ihtsdo.otf.mapping
   * .model.MapNote)
   */
  @Override
  public void removeMapNote(MapNote mapNote) {
    mapNotes.remove(mapNote);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapEntries()
   */
  @Override
  @XmlElement(type = MapEntryJpa.class, name = "mapEntry")
  public List<MapEntry> getMapEntries() {
    if (mapEntries == null)
      mapEntries = new ArrayList<>(); // ensures proper deserialization
    return mapEntries;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapEntries(java.util.List)
   */
  @Override
  public void setMapEntries(List<MapEntry> mapEntries) {
    if (mapEntries == null)
      this.mapEntries = new ArrayList<>();
    else
      this.mapEntries = mapEntries;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#addMapEntry(org.ihtsdo.otf.mapping
   * .model.MapEntry)
   */
  @Override
  public void addMapEntry(MapEntry mapEntry) {
    mapEntries.add(mapEntry);
  }

  /**
   * Function to correctly set the record object for map entries Must be called
   * after deserialization in RESTful services after receiving Json/XML object
   * Rationale: deserialization provides only the record id, not the Jpa object.
   * 
   */
  @Override
  public void assignToChildren() {

    // assign to entries
    for (MapEntry entry : mapEntries) {
      entry.setMapRecord(this);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#removeMapEntry(org.ihtsdo.otf.mapping
   * .model.MapEntry)
   */
  @Override
  public void removeMapEntry(MapEntry mapEntry) {
    mapEntries.remove(mapEntry);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapPrinciples()
   */
  @Override
  @XmlElement(type = MapPrincipleJpa.class, name = "mapPrinciple")
  public Set<MapPrinciple> getMapPrinciples() {
    return mapPrinciples;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapPrinciples(java.util.Set)
   */
  @Override
  public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
    this.mapPrinciples = mapPrinciples;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#addMapPrinciple(org.ihtsdo.otf.mapping
   * .model.MapPrinciple)
   */
  @Override
  public void addMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.add(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#removeMapPrinciple(org.ihtsdo.otf
   * .mapping.model.MapPrinciple)
   */
  @Override
  public void removeMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.remove(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getOriginIds()
   */
  @Override
  public Set<Long> getOriginIds() {
    return originIds;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setOriginIds(java.util.Set)
   */
  @Override
  public void setOriginIds(Set<Long> originIds) {
    this.originIds = originIds;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#addOrigin(java.lang.Long)
   */
  @Override
  public void addOrigin(Long origin) {
    this.originIds.add(origin);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#removeOrigin(java.lang.Long)
   */
  @Override
  public void removeOrigin(Long origin) {
    originIds.remove(origin);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#isFlagForMapLeadReview()
   */
  @Override
  public boolean isFlagForMapLeadReview() {
    return flagForMapLeadReview;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setFlagForMapLeadReview(boolean)
   */
  @Override
  public void setFlagForMapLeadReview(boolean flag) {
    flagForMapLeadReview = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#isFlagForEditorialReview()
   */
  @Override
  public boolean isFlagForEditorialReview() {
    return flagForEditorialReview;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setFlagForEditorialReview(boolean)
   */
  @Override
  public void setFlagForEditorialReview(boolean flag) {
    flagForEditorialReview = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#isFlagForConsensusReview()
   */
  @Override
  public boolean isFlagForConsensusReview() {
    return flagForConsensusReview;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setFlagForConsensusReview(boolean)
   */
  @Override
  public void setFlagForConsensusReview(boolean flag) {
    flagForConsensusReview = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#isEquivalent(org.ihtsdo.otf.mapping
   * .model.MapRecord)
   */
  @Override
  public boolean isEquivalent(MapRecord mapRecord) {

    // first check all the non-collection fields
    if (conceptId == null) {
      if (mapRecord.getConceptId() != null)
        return false;
    } else if (!conceptId.equals(mapRecord.getConceptId()))
      return false;
    if (conceptName == null) {
      if (mapRecord.getConceptName() != null)
        return false;
    } else if (!conceptName.equals(mapRecord.getConceptName()))
      return false;
    if (countDescendantConcepts == null) {
      if (mapRecord.getCountDescendantConcepts() != null)
        return false;
    } else if (!countDescendantConcepts.equals(mapRecord
        .getCountDescendantConcepts()))
      return false;
    if (flagForConsensusReview != mapRecord.isFlagForConsensusReview())
      return false;
    if (flagForEditorialReview != mapRecord.isFlagForEditorialReview())
      return false;
    if (flagForMapLeadReview != mapRecord.isFlagForMapLeadReview())
      return false;
    if (lastModified == null) {
      if (mapRecord.getLastModified() != null)
        return false;
    } else if (!lastModified.equals(mapRecord.getLastModified()))
      return false;
    if (lastModifiedBy == null) {
      if (mapRecord.getLastModifiedBy() != null)
        return false;
    } else if (!lastModifiedBy.equals(mapRecord.getLastModifiedBy()))
      return false;

    if (mapProjectId == null) {
      if (mapRecord.getMapProjectId() != null)
        return false;
    } else if (!mapProjectId.equals(mapRecord.getMapProjectId()))
      return false;

    if (owner == null) {
      if (mapRecord.getOwner() != null)
        return false;
    } else if (!owner.equals(mapRecord.getOwner()))
      return false;
    if (timestamp == null) {
      if (mapRecord.getTimestamp() != null)
        return false;
    } else if (!timestamp.equals(mapRecord.getTimestamp()))
      return false;
    if (workflowStatus != mapRecord.getWorkflowStatus())
      return false;

    // check the collection fields
    // * if not same length, return false
    // * if each collection element not contained in the mapRecord, return false
    // * don't care about order of collections
    // * not currently checking advices (not used presently)

    // check entries
    if (mapRecord.getMapEntries().size() != mapEntries.size())
      return false;
    for (MapEntry entry : mapEntries) {
      if (!mapRecord.getMapEntries().contains(entry))
        return false;
    }

    // check notes
    if (mapRecord.getMapNotes().size() != mapNotes.size())
      return false;
    for (MapNote note : mapNotes) {
      if (!mapRecord.getMapNotes().contains(note))
        return false;
    }

    // check principles
    if (mapRecord.getMapPrinciples().size() != mapPrinciples.size())
      return false;
    for (MapPrinciple principle : mapPrinciples) {
      if (!mapRecord.getMapPrinciples().contains(principle))
        return false;
    }

    // if passed all checks, return true
    return true;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#setWorkflowStatus(org.ihtsdo.otf
   * .mapping.helpers.WorkflowStatus)
   */
  @Override
  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getWorkflowStatus()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#addOrigins(java.util.Set)
   */
  @Override
  public void addOrigins(Set<Long> origins) {
    originIds.addAll(origins);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#isDiscrepancyReview()
   */
  @Override
  public boolean isDiscrepancyReview() {
    return isDiscrepancyReview;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setDiscrepancyReview(boolean)
   */
  @Override
  public void setDiscrepancyReview(boolean isDiscrepancyReview) {
    this.isDiscrepancyReview = isDiscrepancyReview;
  }

  /**
   * Gets the labels.
   *
   * @return the labels
   */
  @Override
  public Set<String> getLabels() {
    return labels;
  }

  /**
   * Sets the labels.
   *
   * @param labels the new labels
   */
  @Override
  public void setLabels(Set<String> labels) {
    this.labels = labels;
  }


  
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#addLabel(java.lang.String)
   */
  @Override
  public void addLabel(String label) {
    labels.add(label);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#removeLabel(java.lang.String)
   */
  @Override
  public void removeLabel(String label) {
    labels.remove(label);
  }


  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getReasonsForConflict()
   */
  @Override
  public Set<String> getReasonsForConflict() {
    return reasonsForConflict;
  }


  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setReasonsForConflict(java.util.Set)
   */
  @Override
  public void setReasonsForConflict(Set<String> reasons) {
    this.reasonsForConflict = reasons;
  }
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapRecord#addReasonForConflict(java.lang.String)
   */
  @Override
  public void addReasonForConflict(String reason) {
    reasonsForConflict.add(reason);
  }


  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapRecord#removeReasonForConflict(java.lang.String)
   */
  @Override
  public void removeReasonForConflict(String reason) {
    reasonsForConflict.remove(reason);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
    result =
        prime * result + ((conceptName == null) ? 0 : conceptName.hashCode());
    result =
        prime
            * result
            + ((countDescendantConcepts == null) ? 0 : countDescendantConcepts
                .hashCode());
    result = prime * result + (flagForConsensusReview ? 1231 : 1237);
    result = prime * result + (flagForEditorialReview ? 1231 : 1237);
    result = prime * result + (flagForMapLeadReview ? 1231 : 1237);
    result =
        prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
    result =
        prime * result
            + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
    result =
        prime * result + ((mapEntries == null) ? 0 : mapEntries.hashCode());
    result = prime * result + ((mapNotes == null) ? 0 : mapNotes.hashCode());
    result =
        prime * result
            + ((mapPrinciples == null) ? 0 : mapPrinciples.hashCode());
    result =
        prime * result + ((mapProjectId == null) ? 0 : mapProjectId.hashCode());
    result = prime * result + ((originIds == null) ? 0 : originIds.hashCode());
    result = prime * result + ((owner == null) ? 0 : owner.hashCode());
    result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
    result =
        prime * result
            + ((workflowStatus == null) ? 0 : workflowStatus.hashCode());
    result = prime * result + ((labels == null) ? 0 : labels.hashCode());
    result = prime * result + ((reasonsForConflict == null) ? 0 : reasonsForConflict.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapRecordJpa other = (MapRecordJpa) obj;
    if (conceptId == null) {
      if (other.conceptId != null)
        return false;
    } else if (!conceptId.equals(other.conceptId))
      return false;
    if (conceptName == null) {
      if (other.conceptName != null)
        return false;
    } else if (!conceptName.equals(other.conceptName))
      return false;
    if (countDescendantConcepts == null) {
      if (other.countDescendantConcepts != null)
        return false;
    } else if (!countDescendantConcepts.equals(other.countDescendantConcepts))
      return false;
    if (flagForConsensusReview != other.flagForConsensusReview)
      return false;
    if (flagForEditorialReview != other.flagForEditorialReview)
      return false;
    if (flagForMapLeadReview != other.flagForMapLeadReview)
      return false;
    if (lastModified == null) {
      if (other.lastModified != null)
        return false;
    } else if (!lastModified.equals(other.lastModified))
      return false;
    if (lastModifiedBy == null) {
      if (other.lastModifiedBy != null)
        return false;
    } else if (!lastModifiedBy.equals(other.lastModifiedBy))
      return false;
    if (mapEntries == null) {
      if (other.mapEntries != null)
        return false;
    } else if (!mapEntries.equals(other.mapEntries))
      return false;
    if (mapNotes == null) {
      if (other.mapNotes != null)
        return false;
    } else if (!mapNotes.equals(other.mapNotes))
      return false;
    if (mapPrinciples == null) {
      if (other.mapPrinciples != null)
        return false;
    } else if (!mapPrinciples.equals(other.mapPrinciples))
      return false;
    if (mapProjectId == null) {
      if (other.mapProjectId != null)
        return false;
    } else if (!mapProjectId.equals(other.mapProjectId))
      return false;
    if (originIds == null) {
      if (other.originIds != null)
        return false;
    } else if (!originIds.equals(other.originIds))
      return false;
    if (owner == null) {
      if (other.owner != null)
        return false;
    } else if (!owner.equals(other.owner))
      return false;
    if (timestamp == null) {
      if (other.timestamp != null)
        return false;
    } else if (!timestamp.equals(other.timestamp))
      return false;
    if (labels == null) {
      if (other.labels != null)
        return false;
    } else if (!labels.equals(other.labels))
      return false;
    if (reasonsForConflict == null) {
      if (other.reasonsForConflict != null)
        return false;
    } else if (!reasonsForConflict.equals(other.reasonsForConflict))
      return false;
    if (workflowStatus != other.workflowStatus)
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MapRecordJpa [id=" + id + ", owner=" + owner + ", timestamp="
        + timestamp + ", lastModifiedBy=" + lastModifiedBy + ", lastModified="
        + lastModified + ", mapProjectId=" + mapProjectId + ", conceptId="
        + conceptId + ", conceptName=" + conceptName
        + ", countDescendantConcepts=" + countDescendantConcepts
        + ", mapEntries=" + mapEntries.size() + ", mapNotes=" + mapNotes
        + ", mapPrinciples=" + mapPrinciples + ", originIds=" + originIds
        + ", flagForMapLeadReview=" + flagForMapLeadReview
        + ", flagForEditorialReview=" + flagForEditorialReview
        + ", flagForConsensusReview=" + flagForConsensusReview
        + ", workflowStatus=" + workflowStatus + ", labels=" + labels 
        + ", reasonsForConflict=" + reasonsForConflict + "]";
  }

}
