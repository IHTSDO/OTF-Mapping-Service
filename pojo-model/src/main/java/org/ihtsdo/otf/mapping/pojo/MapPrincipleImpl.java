package org.ihtsdo.otf.mapping.pojo;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapPrinciple;

/**
 * The Map Principle Object for the Jpa Domain
 * @author Patrick
 * 
 */

public class MapPrincipleImpl implements MapPrinciple {

  /** The id */
  private Long id;

  /** The documented principle id */
  private String principleId;

  /** The name */
  private String name;

  /** The detail */
  private String detail;

  /** The section reference */
  private String sectionRef;

  /** Default constructor */
  public MapPrincipleImpl() {
    // left empty
  }

  /**
   * Return the id
   * @return the id
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form
   * @return the id in string form
   */
  @XmlID
  @Override
  public String getObjectId() {
    return id.toString();
  }

  @Override
  public String getPrincipleId() {
    return this.principleId;
  }

  @Override
  public void setPrincipleId(String principleId) {
    this.principleId = principleId;
  }

  /**
   * Get the detail
   * @return the detail
   */
  @Override
  public String getDetail() {
    return this.detail;
  }

  /**
   * Set the detail
   * @param detail the detail
   */
  @Override
  public void setDetail(String detail) {
    this.detail = detail;

  }

  /**
   * Get the name
   * @return the name
   */
  @Override
  public String getName() {
    return this.name;
  }

  /**
   * Set the name
   * @param name the name
   */
  @Override
  public void setName(String name) {
    this.name = name;

  }

  /**
   * Get the section reference
   * @return the section reference
   */
  @Override
  public String getSectionRef() {
    return this.sectionRef;
  }

  /**
   * Set the section reference
   * @param sectionRef the section reference
   */
  @Override
  public void setSectionRef(String sectionRef) {
    this.sectionRef = sectionRef;

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((detail == null) ? 0 : detail.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result =
        prime * result + ((sectionRef == null) ? 0 : sectionRef.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MapPrincipleImpl other = (MapPrincipleImpl) obj;
    if (detail == null) {
      if (other.detail != null) {
        return false;
      }
    } else if (!detail.equals(other.detail)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (sectionRef == null) {
      if (other.sectionRef != null) {
        return false;
      }
    } else if (!sectionRef.equals(other.sectionRef)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "MapPrincipleJpa [id=" + id + ", name=" + name + ", detail="
        + detail + ", sectionRef=" + sectionRef + "]";
  }

}
