package org.ihtsdo.otf.mapping.pojo;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Reference implementation of {@link MapRecord}. Includes hibernate tags for
 * persistence
 * 
 */
public class MapRecordImpl implements MapRecord {

  /** The id. */
  private Long id;

  /** The owner. */
  private MapUser owner;

  /** The timestamp. */
  private Long timestamp;

  private MapUser lastModifiedBy;

  private Long lastModified;

  /** The map project id. */
  private Long mapProjectId;

  /** The concept id. */
  private String conceptId;

  /** The concept name. */
  private String conceptName;

  /** The number of descendant concepts for the concept id. */
  private Long countDescendantConcepts;

  /** The notes. */
  private Set<MapNote> mapNotes;

  /** The map entries. */
  private List<MapEntry> mapEntries;

  /** The map principles. */
  private Set<MapPrinciple> mapPrinciples;

  /** The originIds. */
  private Set<Long> originIds;

  /** The flag for map lead review. */
  private boolean flagForMapLeadReview = false;

  /** The flag for editorial review. */
  private boolean flagForEditorialReview = false;

  /** The flag for consensus review. */
  private boolean flagForConsensusReview = false;

  /** The workflow status. */
  private WorkflowStatus workflowStatus;

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
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getLastModifiedBy()
   */
  @Override
  public MapUser getLastModifiedBy() {
    return lastModifiedBy;
  }

  @Override
  public void setLastModifiedBy(MapUser mapUser) {
    this.lastModifiedBy = mapUser;
  }

  @Override
  public Long getLastModified() {
    return this.lastModified;
  }

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
   * @see org.ihtsdo.otf.mapping.model.MapRecord#getNotes()
   */
  @Override
  public Set<MapNote> getMapNotes() {
    return mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setNotes(java.util.List)
   */
  @Override
  public void setMapNotes(Set<MapNote> mapNotes) {
    this.mapNotes = mapNotes;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#addNote(org.ihtsdo.otf.mapping.model
   * .MapNote)
   */
  @Override
  public void addMapNote(MapNote mapNote) {
    mapNotes.add(mapNote);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRecord#removeNote(org.ihtsdo.otf.mapping
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
  public List<MapEntry> getMapEntries() {
    return mapEntries;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapEntries(java.util.List)
   */
  @Override
  public void setMapEntries(List<MapEntry> mapEntries) {
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

  /**
   * Function to correctly set the record object for map entries.
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
    MapRecordImpl other = (MapRecordImpl) obj;
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
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MapRecordImpl [id=" + id + ", conceptId=" + conceptId
        + ", mapNotes=" + mapNotes + ", mapEntries=" + mapEntries
        + ", originIds=" + originIds + "]";
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
   * org.ihtsdo.otf.mapping.model.MapRecord#setWorkflowStatus(java.lang.String)
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
  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  @Override
  public void addOrigins(Set<Long> origins) {
    origins.addAll(origins);
  }
  

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
		} else if (!countDescendantConcepts.equals(mapRecord.getCountDescendantConcepts()))
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
		if (mapRecord.getMapEntries().size() != mapEntries.size()) return false;
		for (MapEntry entry : mapEntries) {
			if (!mapRecord.getMapEntries().contains(entry)) return false;
		}
		
		// check notes
		if (mapRecord.getMapNotes().size() != mapNotes.size()) return false;
		for (MapNote note : mapNotes) {
			if (!mapRecord.getMapNotes().contains(note)) return false;
		}
		
		// check entries
		if (mapRecord.getMapPrinciples().size() != mapPrinciples.size()) return false;
		for (MapPrinciple principle : mapPrinciples) {
			if (!mapRecord.getMapPrinciples().contains(principle)) return false;
		}
		
		// if passed all checks, return true
		return true;
	  
  }


}
