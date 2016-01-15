package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapPrinciple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A JPA enabled implementation of {@link MapPrinciple}.
 */
@Entity
@Table(name = "map_principles", uniqueConstraints = {
  @UniqueConstraint(columnNames = {
      "name", "principleId"
  })
})
@Audited
@XmlRootElement(name = "mapPrinciple")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapPrincipleJpa implements MapPrinciple {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The principle id. */
  @Column(nullable = true, length = 255)
  private String principleId;

  /** The name. */
  @Column(nullable = false, length = 255)
  private String name;

  /** The detail. */
  @Column(nullable = true, length = 4000)
  private String detail;

  /** The section ref. */
  @Column(nullable = true, length = 4000)
  private String sectionRef;

  /**
   * Default constructor.
   */
  public MapPrincipleJpa() {
    // left empty
  }

  /**
   * Instantiates a new map principle jpa.
   *
   * @param id the id
   * @param principleId the principle id
   * @param name the name
   * @param detail the detail
   * @param sectionRef the section ref
   */
  public MapPrincipleJpa(Long id, String principleId, String name,
      String detail, String sectionRef) {
    super();
    this.id = id;
    this.principleId = principleId;
    this.name = name;
    this.detail = detail;
    this.sectionRef = sectionRef;
  }

  /**
   * Instantiates a {@link MapPrincipleJpa} from the specified parameters.
   *
   * @param mapPrinciple the map principle
   */
  public MapPrincipleJpa(MapPrinciple mapPrinciple) {
    id = mapPrinciple.getId();
    detail = mapPrinciple.getDetail();
    name = mapPrinciple.getName();
    principleId = mapPrinciple.getPrincipleId();
    sectionRef = mapPrinciple.getSectionRef();

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

  /* see superclass */
  @Override
  public String getPrincipleId() {
    return this.principleId;
  }

  /* see superclass */
  @Override
  public void setPrincipleId(String principleId) {
    this.principleId = principleId;
  }

  /**
   * Get the detail.
   *
   * @return the detail
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getDetail() {
    return this.detail;
  }

  /**
   * Set the detail.
   *
   * @param detail the detail
   */
  @Override
  public void setDetail(String detail) {
    this.detail = detail;

  }

  /**
   * Get the name.
   *
   * @return the name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getName() {
    return this.name;
  }

  /**
   * Set the name.
   *
   * @param name the name
   */
  @Override
  public void setName(String name) {
    this.name = name;

  }

  /**
   * Get the section reference.
   *
   * @return the section reference
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getSectionRef() {
    return this.sectionRef;
  }

  /**
   * Set the section reference.
   *
   * @param sectionRef the section reference
   */
  @Override
  public void setSectionRef(String sectionRef) {
    this.sectionRef = sectionRef;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((detail == null) ? 0 : detail.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result =
        prime * result + ((principleId == null) ? 0 : principleId.hashCode());
    result =
        prime * result + ((sectionRef == null) ? 0 : sectionRef.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapPrincipleJpa other = (MapPrincipleJpa) obj;
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
    if (principleId == null) {
      if (other.principleId != null)
        return false;
    } else if (!principleId.equals(other.principleId))
      return false;
    if (sectionRef == null) {
      if (other.sectionRef != null)
        return false;
    } else if (!sectionRef.equals(other.sectionRef))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "MapPrincipleJpa [id=" + id + ", name=" + name + ", detail="
        + detail + ", sectionRef=" + sectionRef + "]";
  }

}
