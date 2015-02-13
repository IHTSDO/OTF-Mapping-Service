package org.ihtsdo.otf.mapping.model;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;

/**
 * Generically represents a map record, or a complete mapping of a source
 * concept in a map project.
 */
public interface MapRecord {

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
   * Sets the owner.
   * 
   * @param mapUser the new owner
   */
  public void setOwner(MapUser mapUser);

  /**
   * Gets the owner.
   * 
   * @return the owner
   */
  public MapUser getOwner();

  /**
   * Sets the timestamp.
   * 
   * @param timestamp the new timestamp
   */
  public void setTimestamp(Long timestamp);

  /**
   * Gets the timestamp.
   * 
   * @return the timestamp
   */
  public Long getTimestamp();

  /**
   * Gets the last modified by.
   * 
   * @return the last modified by
   */
  public MapUser getLastModifiedBy();

  /**
   * Sets the last modified by.
   * 
   * @param mapUser the new last modified by
   */
  public void setLastModifiedBy(MapUser mapUser);

  /**
   * Gets the last modified date, in ms since 1970.
   * 
   * @return the last modified date
   */
  public Long getLastModified();

  /**
   * Sets the last modified date, in ms since 1970.
   *
   * @param lastModified the last modified date
   */
  public void setLastModified(Long lastModified);

  /**
   * Returns the map project id.
   * 
   * @return the map project id
   */
  public Long getMapProjectId();

  /**
   * Sets the map project id.
   * 
   * @param mapProjectId the map project id
   */
  public void setMapProjectId(Long mapProjectId);

  /**
   * Returns the concept id.
   * 
   * @return the concept id
   */
  public String getConceptId();

  /**
   * Sets the concept id.
   * 
   * @param conceptId the concept id
   */
  public void setConceptId(String conceptId);

  /**
   * Returns the concept name.
   * 
   * @return the concept name
   */
  public String getConceptName();

  /**
   * Sets the concept name.
   * 
   * @param conceptName the concept name
   */
  public void setConceptName(String conceptName);

  /**
   * Returns the map notes.
   * 
   * @return the map notes
   */
  public Set<MapNote> getMapNotes();

  /**
   * Sets the map notes.
   * 
   * @param mapNotes the map notes
   */
  public void setMapNotes(Set<MapNote> mapNotes);

  /**
   * Adds the map note.
   * 
   * @param mapNote the map note
   */
  public void addMapNote(MapNote mapNote);

  /**
   * Removes the map note.
   * 
   * @param mapNote the map note
   */
  public void removeMapNote(MapNote mapNote);

  /**
   * Returns the map entries.
   * 
   * @return the map entries
   */
  public List<MapEntry> getMapEntries();

  /**
   * Sets the map entries.
   * 
   * @param mapEntries the map entries
   */
  public void setMapEntries(List<MapEntry> mapEntries);

  /**
   * Adds the map entry.
   * 
   * @param mapEntry the map entry
   */
  public void addMapEntry(MapEntry mapEntry);

  /**
   * Removes the map entry.
   * 
   * @param mapEntry the map entry
   */
  public void removeMapEntry(MapEntry mapEntry);

  /**
   * Returns the set of allowable map principles.
   * 
   * @return the map principles
   */
  public Set<MapPrinciple> getMapPrinciples();

  /**
   * Sets the set of allowable map principles.
   * 
   * @param mapPrinciples the map principles
   */
  public void setMapPrinciples(Set<MapPrinciple> mapPrinciples);

  /**
   * Adds an allowable map principle.
   * 
   * @param mapPrinciple the map principle
   */
  public void addMapPrinciple(MapPrinciple mapPrinciple);

  /**
   * Removes an allowable map principle.
   * 
   * @param mapPrinciple the map principle
   */
  public void removeMapPrinciple(MapPrinciple mapPrinciple);

  /**
   * Assigns the map record to its children after deserialization.
   */
  public void assignToChildren();

  /**
   * Indicates whether or not flag for map lead review is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isFlagForMapLeadReview();

  /**
   * Sets the flag for map lead review.
   * 
   * @param flag the flag for map lead review
   */
  public void setFlagForMapLeadReview(boolean flag);

  /**
   * Indicates whether or not flag for editorial review is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isFlagForEditorialReview();

  /**
   * Sets the flag for editorial review.
   * 
   * @param flag the flag for editorial review
   */
  public void setFlagForEditorialReview(boolean flag);

  /**
   * Indicates whether or not flag for consensus review is the case.
   * 
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isFlagForConsensusReview();

  /**
   * Sets the flag for consensus review.
   * 
   * @param flag the flag for consensus review
   */
  public void setFlagForConsensusReview(boolean flag);

  /**
   * Returns the originIds.
   * 
   * @return the originIds
   */
  public Set<Long> getOriginIds();

  /**
   * Sets the originIds.
   * 
   * @param originIds the originIds
   */
  public void setOriginIds(Set<Long> originIds);

  /**
   * Adds the origin.
   * 
   * @param origin the origin
   */
  public void addOrigin(Long origin);

  /**
   * Adds the origins.
   * 
   * @param origins the origins
   */
  public void addOrigins(Set<Long> origins);

  /**
   * Removes the origin.
   * 
   * @param origin the origin
   */
  public void removeOrigin(Long origin);

  /**
   * Sets the workflow status.
   * 
   * @param workflowStatus the workflow status
   */
  public void setWorkflowStatus(WorkflowStatus workflowStatus);

  /**
   * Returns the workflow status.
   * 
   * @return the workflow status
   */
  public WorkflowStatus getWorkflowStatus();

  /**
   * Checks if if another map record is functionally equivalent Added due to
   * inconsistencies with equals() not checking child collections correctly.
   *
   * @param mapRecord the map record
   * @return true, if is equivalent
   */
  public boolean isEquivalent(MapRecord mapRecord);

  /**
   * Checks if is discrepancy review.
   *
   * @return true, if is discrepancy review
   */
  public boolean isDiscrepancyReview();

  /**
   * Sets the discrepancy review status.
   *
   * @param isDiscrepancyReview the new discrepancy review
   */
  public void setDiscrepancyReview(boolean isDiscrepancyReview);

  /**
   * Gets the labels.
   *
   * @return the labels
   */
  public Set<String> getLabels();

  /**
   * Sets the labels.
   *
   * @param labels the new labels
   */
  public void setLabels(Set<String> labels);

  /**
   * Adds the label.
   *
   * @param label the label
   */
  public void addLabel(String label);

  /**
   * Removes the label.
   *
   * @param label the label
   */
  public void removeLabel(String label);
  
  /**
   * Returns the reasons for conflict.
   *
   * @return the reasons for conflict
   */
  public Set<String> getReasonsForConflict();

  /**
   * Sets the reasons for conflict.
   *
   * @param reasons the reasons for conflict
   */
  public void setReasonsForConflict(Set<String> reasons);
  
  /**
   * Adds the reason for conflict.
   *
   * @param reason the reason
   */
  public void addReasonForConflict(String reason);

  /**
   * Removes the reason for conflict.
   *
   * @param reason the reason
   */
  public void removeReasonForConflict(String reason);

}
