package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.ContainedIn;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;

/**
 * Concrete implementation of {@link Relationship} for use with JPA.
 */
@Entity
// @UniqueConstraint here is being used to create an index, not to enforce
// uniqueness
@Table(name = "relationships", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion"
}))
// @Audited
@XmlRootElement(name = "relationship")
public class RelationshipJpa extends AbstractComponent implements Relationship {

  /** The source concept. */
  @ManyToOne(targetEntity = ConceptJpa.class, optional = false)
  @ContainedIn
  private Concept sourceConcept;

  /** The destination concept. */
  @ManyToOne(targetEntity = ConceptJpa.class, optional = false)
  private Concept destinationConcept;

  /** The type id. */
  @Column(nullable = false)
  private Long typeId;

  /** The characteristic type id. */
  @Column(nullable = false)
  private Long characteristicTypeId;

  /** The modifier id. */
  @Column(nullable = false)
  private Long modifierId;

  /** The relationship group. */
  @Column(nullable = true)
  private Integer relationshipGroup;

  /**
   * Instantiates a new relationship jpa.
   */
  public RelationshipJpa() {
    // empty
  }

  /**
   * Instantiates a new relationship jpa.
   * @param relationship the relationship
   * @param deepCopy indicates whether or not to perform a deep copy
   */
  public RelationshipJpa(Relationship relationship, boolean deepCopy) {
    setId(relationship.getId());
    setActive(relationship.isActive());
    setEffectiveTime(relationship.getEffectiveTime());
    setLabel(relationship.getLabel());
    setModuleId(relationship.getModuleId());
    setTerminology(relationship.getTerminology());
    setTerminologyId(relationship.getTerminologyId());
    setTerminologyVersion(relationship.getTerminologyVersion());
    sourceConcept = relationship.getSourceConcept();
    destinationConcept = relationship.getDestinationConcept();
    typeId = relationship.getTypeId();
    characteristicTypeId = relationship.getCharacteristicTypeId();
    modifierId = relationship.getModifierId();
    relationshipGroup = relationship.getRelationshipGroup();
  }

  /**
   * Returns the type id.
   * 
   * @return the type id
   */
  @Override
  public Long getTypeId() {
    return typeId;
  }

  /**
   * Sets the type id.
   * 
   * @param typeId the type id
   */
  @Override
  public void setTypeId(Long typeId) {
    this.typeId = typeId;
  }

  /**
   * Returns the characteristic type id.
   * 
   * @return the characteristic type id
   */
  @Override
  public Long getCharacteristicTypeId() {
    return characteristicTypeId;
  }

  /**
   * Sets the characteristic type id.
   * 
   * @param characteristicTypeId the characteristic type id
   */
  @Override
  public void setCharacteristicTypeId(Long characteristicTypeId) {
    this.characteristicTypeId = characteristicTypeId;
  }

  /**
   * Returns the modifier id.
   * 
   * @return the modifier id
   */
  @Override
  public Long getModifierId() {
    return modifierId;
  }

  /**
   * Sets the modifier id.
   * 
   * @param modifierId the modifier id
   */
  @Override
  public void setModifierId(Long modifierId) {
    this.modifierId = modifierId;
  }

  /**
   * Returns the source concept.
   * 
   * @return the source concept
   */
  @XmlTransient
  @Override
  public Concept getSourceConcept() {
    return sourceConcept;
  }

  /**
   * Sets the source concept.
   * 
   * @param sourceConcept the source concept
   */
  @Override
  public void setSourceConcept(Concept sourceConcept) {
    this.sourceConcept = sourceConcept;
  }

  /**
   * For serialization.
   *
   * @return the source concept id
   */
  @XmlElement
  private String getSourceConceptId() {
    return sourceConcept.getTerminologyId();
  }

  /**
   * Returns the destination concept.
   * 
   * @return the destination concept
   */
  @XmlTransient
  @Override
  public Concept getDestinationConcept() {
    return this.destinationConcept;
  }

  /**
   * Sets the destination concept.
   * 
   * @param destinationConcept the destination concept
   */
  @Override
  public void setDestinationConcept(Concept destinationConcept) {
    this.destinationConcept = destinationConcept;
  }

  /**
   * For serialization.
   *
   * @return the destination concept id
   */
  @XmlElement
  private String getDestinationConceptId() {
    return destinationConcept.getTerminologyId();
  }

  /**
   * Returns the destination concept preferred name. Used for XML/JSON
   * serialization.
   * @return the destination concept preferred name
   */
  @XmlElement
  public String getDestinationConceptPreferredName() {
    return destinationConcept != null ? destinationConcept
        .getDefaultPreferredName() : null;
  }

  /**
   * Returns the relationship group.
   * 
   * @return the relationship group
   */
  @Override
  public Integer getRelationshipGroup() {
    return relationshipGroup;
  }

  /**
   * Sets the relationship group.
   * 
   * @param relationshipGroup the relationship group
   */
  @Override
  public void setRelationshipGroup(Integer relationshipGroup) {
    this.relationshipGroup = relationshipGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this.getId()
        + ","
        + this.getTerminology()
        + ","
        + this.getTerminologyId()
        + ","
        + this.getTerminologyVersion()
        + ","
        + this.getEffectiveTime()
        + ","
        + this.isActive()
        + ","
        + this.getModuleId()
        + ","
        + // end of basic component fields

        (this.getSourceConcept() == null ? null : this.getSourceConcept()
            .getId())
        + ","
        + (this.getDestinationConcept() == null ? null : this
            .getDestinationConcept().getId()) + ","
        + this.getRelationshipGroup() + "," + this.getTypeId() + ","
        + this.getCharacteristicTypeId() + "," + this.getModifierId(); // end of
                                                                       // relationship
                                                                       // fields

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime
            * result
            + ((characteristicTypeId == null) ? 0 : characteristicTypeId
                .hashCode());
    result =
        prime
            * result
            + ((destinationConcept == null) ? 0 : destinationConcept.hashCode());
    result =
        prime * result + ((modifierId == null) ? 0 : modifierId.hashCode());
    result =
        prime * result
            + ((relationshipGroup == null) ? 0 : relationshipGroup.hashCode());
    result =
        prime * result
            + ((sourceConcept == null) ? 0 : sourceConcept.hashCode());
    result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    RelationshipJpa other = (RelationshipJpa) obj;
    if (characteristicTypeId == null) {
      if (other.characteristicTypeId != null)
        return false;
    } else if (!characteristicTypeId.equals(other.characteristicTypeId))
      return false;
    if (destinationConcept == null) {
      if (other.destinationConcept != null)
        return false;
    } else if (!destinationConcept.equals(other.destinationConcept))
      return false;
    if (modifierId == null) {
      if (other.modifierId != null)
        return false;
    } else if (!modifierId.equals(other.modifierId))
      return false;
    if (relationshipGroup == null) {
      if (other.relationshipGroup != null)
        return false;
    } else if (!relationshipGroup.equals(other.relationshipGroup))
      return false;
    if (sourceConcept == null) {
      if (other.sourceConcept != null)
        return false;
    } else if (!sourceConcept.equals(other.sourceConcept))
      return false;
    if (typeId == null) {
      if (other.typeId != null)
        return false;
    } else if (!typeId.equals(other.typeId))
      return false;
    return true;
  }

}
