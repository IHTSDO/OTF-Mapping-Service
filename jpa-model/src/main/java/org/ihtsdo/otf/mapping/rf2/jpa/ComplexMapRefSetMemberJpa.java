package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;

/**
 * Concrete implementation of {@link ComplexMapRefSetMember}.
 */
@Entity
@Table(name = "complex_map_refset_members")
@Audited
public class ComplexMapRefSetMemberJpa extends AbstractConceptRefSetMember
    implements ComplexMapRefSetMember {

  /** The map block */
  @Column(nullable = false)
  private int mapBlock;

  /** the map block rule */
  @Column(nullable = true)
  private String mapBlockRule;

  /** the map block advice */
  @Column(nullable = true)
  private String mapBlockAdvice;

  /** The map group */
  @Column(nullable = false)
  private int mapGroup;

  /** The map priority */
  @Column(nullable = false)
  private int mapPriority;

  /** the map rule */
  @Column(nullable = true)
  private String mapRule;

  /** the map advice */
  @Column(nullable = true, length = 500)
  private String mapAdvice;

  /** the map target */
  @Column(nullable = true)
  private String mapTarget;

  /** the correlation id */
  @Column(nullable = false)
  private Long mapRelationId;

  /**
   * returns the mapBlock
   * @return the mapBlock
   */
  @Override
  public int getMapBlock() {
    return this.mapBlock;
  }

  /**
   * sets the mapBlock
   * @param mapBlock the mapBlock
   */
  @Override
  public void setMapBlock(int mapBlock) {
    this.mapBlock = mapBlock;
  }

  /**
   * returns the mapBlockRule
   * @return the mapBlockRule
   */
  @Override
  public String getMapBlockRule() {
    return this.mapBlockRule;
  }

  /**
   * sets the mapBlockRule
   * @paran the mapBlockRule
   */
  @Override
  public void setMapBlockRule(String mapBlockRule) {
    this.mapBlockRule = mapBlockRule;
  }

  /**
   * returns the mapBlockAdvice
   * @return the mapBlockAdvice
   */
  @Override
  public String getMapBlockAdvice() {
    return this.mapBlockAdvice;
  }

  /**
   * sets the mapBlockAdvice
   * @param mapBlockAdvice the mapBlockAdvice
   */
  @Override
  public void setMapBlockAdvice(String mapBlockAdvice) {
    this.mapBlockAdvice = mapBlockAdvice;
  }

  /**
   * returns the map group
   * @return the map group
   */
  @Override
  public int getMapGroup() {
    return this.mapGroup;
  }

  /**
   * sets the map group
   * @param mapGroup the map group
   */
  @Override
  public void setMapGroup(int mapGroup) {
    this.mapGroup = mapGroup;
  }

  /**
   * returns the map priority
   * @return the map priority
   */
  @Override
  public int getMapPriority() {
    return this.mapPriority;
  }

  /**
   * sets the map priority
   * @param mapPriority the map priority
   */
  @Override
  public void setMapPriority(int mapPriority) {
    this.mapPriority = mapPriority;
  }

  /**
   * returns the map rule
   * @return the map rule
   */
  @Override
  public String getMapRule() {
    return this.mapRule;
  }

  /**
   * sets the map rule
   * @param mapRule the map rule
   */
  @Override
  public void setMapRule(String mapRule) {
    this.mapRule = mapRule;
  }

  /**
   * returns the map advice
   * @return mapAdvice the map advice
   */
  @Override
  public String getMapAdvice() {
    return this.mapAdvice;
  }

  /**
   * sets the map advice
   * @param mapAdvice the map advice
   */
  @Override
  public void setMapAdvice(String mapAdvice) {
    this.mapAdvice = mapAdvice;

  }

  /**
   * returns the map target
   * @return the map target
   */
  @Override
  public String getMapTarget() {
    return this.mapTarget;
  }

  /**
   * sets the map target
   * @param mapTarget the map target
   */
  @Override
  public void setMapTarget(String mapTarget) {
    this.mapTarget = mapTarget;
  }

  /**
   * returns the correlation id
   * @return the correlation id
   */
  @Override
  public Long getMapRelationId() {
    return this.mapRelationId;
  }

  /**
   * sets the correlation id
   * @param mapRelationId the correlation id
   */
  @Override
  public void setMapRelationId(Long mapRelationId) {
    this.mapRelationId = mapRelationId;
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
        +

        (this.getConcept() == null ? null : this.getConcept()
            .getTerminologyId()) + "," + Integer.toString(this.getMapGroup())
        + "," + Integer.toString(this.getMapPriority()) + ","
        + this.getMapRule() + "," + this.getMapAdvice() + ","
        + this.getMapTarget() + "," + this.getMapRelationId() + "," +

        Integer.toString(this.getMapBlock()) + "," + this.getMapBlockRule()
        + "," + this.getMapBlockAdvice();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.jpa.AbstractConceptRefSetMember#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result
            + ((mapRelationId == null) ? 0 : mapRelationId.hashCode());
    result = prime * result + ((mapRule == null) ? 0 : mapRule.hashCode());
    result = prime * result + ((mapTarget == null) ? 0 : mapTarget.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractConceptRefSetMember#equals(java.
   * lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    ComplexMapRefSetMemberJpa other = (ComplexMapRefSetMemberJpa) obj;
    if (mapRelationId == null) {
      if (other.mapRelationId != null)
        return false;
    } else if (!mapRelationId.equals(other.mapRelationId))
      return false;
    if (mapRule == null) {
      if (other.mapRule != null)
        return false;
    } else if (!mapRule.equals(other.mapRule))
      return false;
    if (mapTarget == null) {
      if (other.mapTarget != null)
        return false;
    } else if (!mapTarget.equals(other.mapTarget))
      return false;
    return true;
  }

}
