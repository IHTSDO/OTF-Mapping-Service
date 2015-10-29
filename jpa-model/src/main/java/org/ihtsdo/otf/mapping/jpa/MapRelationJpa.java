package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.MapRelation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class MapRelationJpa.
 */
@Entity
@Table(name = "map_relations", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
    "name"
  })
})
@Audited
@XmlRootElement(name = "mapRelation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapRelationJpa implements MapRelation {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The terminology id. */
  @Column(nullable = false)
  private String terminologyId;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The abbreviation for display. */
  @Column(nullable = true)
  private String abbreviation;

  /** Whether this relation can be used for null targets. */
  @Column(nullable = false)
  private boolean isAllowableForNullTarget;

  /** Whether this relation is computed. */
  @Column(nullable = false)
  private boolean isComputed;

  /**
   * Instantiates a new map relation jpa.
   */
  public MapRelationJpa() {
    // do nothing
  }

  /**
   * Instantiates a new map relation jpa.
   *
   * @param id the id
   * @param terminologyId the terminology id
   * @param name the name
   * @param abbreviation the abbreviation
   * @param isAllowableForNullTarget the is allowable for null target
   * @param isComputed the is computed
   */
  public MapRelationJpa(Long id, String terminologyId, String name,
      String abbreviation, boolean isAllowableForNullTarget, boolean isComputed) {
    super();
    this.id = id;
    this.terminologyId = terminologyId;
    this.name = name;
    this.abbreviation = abbreviation;
    this.isAllowableForNullTarget = isAllowableForNullTarget;
    this.isComputed = isComputed;
  }

  /**
   * Instantiates a new map relation jpa.
   *
   * @param mapRelation the map relation
   */
  public MapRelationJpa(MapRelation mapRelation) {
    super();
    this.id = mapRelation.getId();
    this.terminologyId = mapRelation.getTerminologyId();
    this.name = mapRelation.getName();
    this.abbreviation = mapRelation.getAbbreviation();
    this.isAllowableForNullTarget = mapRelation.isAllowableForNullTarget();
    this.isComputed = mapRelation.isComputed();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getId()
   */
  @Override
  public Long getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getTerminologyId()
   */
  @Override
  public String getTerminologyId() {
    return terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRelation#setTerminologyId(java.lang.String)
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getAbbreviation()
   */
  @Override
  public String getAbbreviation() {
    return abbreviation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRelation#setAbbreviation(java.lang.String)
   */
  @Override
  public void setAbbreviation(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#isAllowableForNullTarget()
   * 
   * @XmlAttribute annotation used to override default nomenclature
   * ('allowableForNullTarget')
   */
  @Override
  @XmlAttribute(name = "isAllowableForNullTarget")
  public boolean isAllowableForNullTarget() {
    return isAllowableForNullTarget;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRelation#setAllowableForNullTarget(boolean)
   */
  @Override
  public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
    this.isAllowableForNullTarget = isAllowableForNullTarget;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#isComputed()
   * 
   * Field is true IFF the relation is ALWAYS computed (never assigned by user)
   * 
   * @XmlAttribute annotation used to override default nomenclature ('computed')
   */
  @Override
  @XmlAttribute(name = "isComputed")
  public boolean isComputed() {
    return isComputed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#setComputed(boolean)
   */
  @Override
  public void setComputed(boolean isComputed) {
    this.isComputed = isComputed;
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
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
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
    MapRelationJpa other = (MapRelationJpa) obj;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
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
    return "MapRelationJpa [id=" + id + ", terminologyId=" + terminologyId
        + ", name=" + name + ", abbreviation=" + abbreviation
        + ", isAllowableForNullTarget=" + isAllowableForNullTarget
        + ", isComputed=" + isComputed + "]";
  }

}
