package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;

/**
 * Concrete implementation of {@link Relationship}.
 */
public class RelationshipImpl extends AbstractComponent implements Relationship {

  /** The source concept. */
  private Concept sourceConcept;

  /** The destination concept. */
  private Concept destinationConcept;

  /** The type id. */
  private Long typeId;

  /** The characteristic type id. */
  private Long characteristicTypeId;

  /** The modifier id. */
  private Long modifierId;

  /** The relationship group. */
  private Integer relationshipGroup;

  /** The terminology. */
  private String terminology;

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
   * Returns the destination concept.
   * 
   * @return the destination concept
   */
  @Override
  public Concept getDestinationConcept() {
    return destinationConcept;
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
  public String getTerminology() {
    return terminology;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
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
            .getTerminologyId())
        + ","
        + (this.getDestinationConcept() == null ? null : this
            .getDestinationConcept().getTerminologyId()) + ","
        + this.getRelationshipGroup() + "," + this.getTypeId() + ","
        + this.getCharacteristicTypeId() + "," + this.getModifierId(); // end of
                                                                       // relationship
                                                                       // fields

  }
}
