/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Map Note Jpa object.
 */
@Entity
@Table(name = "additional_map_entry_info")
@Audited
@XmlRootElement(name = "additionalMapEntryInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdditionalMapEntryInfoJpa implements AdditionalMapEntryInfo {

  /** The id. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The name. This is a combination of field + "|" + value Although this is
   * duplication of information, it is needed for proper sorting and filtering
   * in the UI
   */
  @Column(nullable = false, length = 4000)
  private String name;

  /** The field. */
  @Column(nullable = false, length = 4000)
  private String field;

  /** The value. */
  @Column(nullable = false, length = 4000)
  private String value;

  /**
   *  Default constructor.
   */
  public AdditionalMapEntryInfoJpa() {
  }

  /**
   * Constructor.
   *
   * @param id the id
   * @param name the name
   * @param field the field
   * @param value the value
   */
  public AdditionalMapEntryInfoJpa(Long id, String name, String field, String value) {
    super();
    this.id = id;
    this.name = name;
    this.field = field;
    this.value = value;
  }

  /**
   * Instantiates a {@link AdditionalMapEntryInfoJpa} from the specified
   * parameters.
   *
   * @param additionalMapEntryInfo the additional map entry info
   */
  public AdditionalMapEntryInfoJpa(AdditionalMapEntryInfo additionalMapEntryInfo) {
    super();
    this.id = additionalMapEntryInfo.getId();
    this.name = additionalMapEntryInfo.getName();
    this.field = additionalMapEntryInfo.getField();
    this.value = additionalMapEntryInfo.getValue();
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

  /* see superclass */
  @Override
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /* see superclass */
  @Override
  public String getField() {
    return field;
  }

  /* see superclass */
  @Override
  public void setField(String field) {
    this.field = field;
  }

  /* see superclass */
  @Override
  public String getValue() {
    return value;
  }

  /* see superclass */
  @Override
  public void setValue(String value) {
    this.value = value;
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "AdditionalMapEntryInfo [id=" + id + ", name=" + name + ", field=" + field + ", value="
        + value + "]";
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  /* see superclass */
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
    AdditionalMapEntryInfoJpa other = (AdditionalMapEntryInfoJpa) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (field == null) {
      if (other.field != null)
        return false;
    } else if (!field.equals(other.field))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

}
