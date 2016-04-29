package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapAdvice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JPA enabled map advice.
 */
@Entity
@Table(name = "map_advices", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
    "name"
  })
})
@Audited
@XmlRootElement(name = "mapAdvice")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapAdviceJpa implements MapAdvice {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The name. */
  @Column(nullable = false, unique = true, length = 255)
  private String name;

  /** The detail. */
  @Column(nullable = false, unique = true, length = 255)
  private String detail;

  /** Flag for whether this advice is valid for a null target. */
  @Column(nullable = false)
  private boolean isAllowableForNullTarget;

  /** The is computable. */
  @Column(nullable = false)
  private boolean isComputed;

  /**
   * Instantiates an empty {@link MapAdviceJpa}.
   */
  public MapAdviceJpa() {
    // empty
  }

  /**
   * Instantiates a new map advice jpa.
   *
   * @param id the id
   * @param name the name
   * @param detail the detail
   * @param isAllowableForNullTarget the is allowable for null target
   * @param isComputed the is computed
   */
  public MapAdviceJpa(Long id, String name, String detail,
      boolean isAllowableForNullTarget, boolean isComputed) {
    super();
    this.id = id;
    this.name = name;
    this.detail = detail;
    this.isAllowableForNullTarget = isAllowableForNullTarget;
    this.isComputed = isComputed;
  }

  /**
   * Instantiates a new map advice jpa.
   *
   * @param mapAdvice the map advice
   */
  public MapAdviceJpa(MapAdvice mapAdvice) {
    super();
    this.id = mapAdvice.getId();
    this.name = mapAdvice.getName();
    this.detail = mapAdvice.getDetail();
    this.isAllowableForNullTarget = mapAdvice.isAllowableForNullTarget();
    this.isComputed = mapAdvice.isComputed();
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
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#getDetail()
   */
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  @Override
  public String getDetail() {
    return detail;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#setDetail(java.lang.String)
   */
  @Override
  public void setDetail(String detail) {
    this.detail = detail;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#getName()
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#isAllowableForNullTarget()
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
   * org.ihtsdo.otf.mapping.model.MapAdvice#setAllowableForNullTarget(boolean)
   */
  @Override
  public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
    this.isAllowableForNullTarget = isAllowableForNullTarget;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#isComputable()
   */
  @Override
  @XmlAttribute(name = "isComputed")
  public boolean isComputed() {
    return isComputed;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapAdvice#setComputable(boolean)
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
    result = prime * result + ((detail == null) ? 0 : detail.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    MapAdviceJpa other = (MapAdviceJpa) obj;
    if (detail == null) {
      if (other.detail != null)
        return false;
    } else if (!detail.equals(other.detail))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
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
    return "MapAdviceJpa [id=" + id + ", name=" + name + ", detail=" + detail
        + ", isAllowableForNullTarget=" + isAllowableForNullTarget
        + ", isComputed=" + isComputed + "]";
  }

}
