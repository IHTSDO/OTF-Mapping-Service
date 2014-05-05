package org.ihtsdo.otf.mapping.helpers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.helpers.SearchResult;

/**
 * The search result for the Jpa package
 * @author Patrick
 * 
 */
@XmlRootElement(name = "searchResult")
public class SearchResultJpa implements SearchResult {

  private Long id;

  private String terminologyId;

  private String terminology;

  private String terminologyVersion;

  private String value;

  /**
   * Default constructor
   */
  public SearchResultJpa() {
    // left empty
  }

  /**
   * Constructor
   * @param id the id
   * @param terminologyId the terminologyId
   * @param value the value
   */
  public SearchResultJpa(Long id, String terminologyId, String value) {
    this.id = id;
    this.terminologyId = terminologyId;
    this.value = value;
  }

  /**
   * Returns the id
   * @return the id
   */
  @Override
  @XmlElement(name = "id")
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the id
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;

  }

  /**
   * Returns the id
   * @return the id
   */
  @Override
  @XmlElement(name = "terminologyId")
  public String getTerminologyId() {
    return this.terminologyId;
  }

  /**
   * Sets the id
   * @param terminologyId the id
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;

  }

  @Override
  public String getTerminology() {
    return this.terminology;
  }

  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  @Override
  public String getTerminologyVersion() {
    return this.terminologyVersion;
  }

  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * Gets the value
   * @return the value
   */
  @Override
  @XmlElement(name = "value")
  public String getValue() {
    return this.value;
  }

  /**
   * Sets the value
   * @param value the value
   */
  @Override
  public void setValue(String value) {
    this.value = value;

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SearchResultJpa other = (SearchResultJpa) obj;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null)
        return false;
    } else if (!terminologyVersion.equals(other.terminologyVersion))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
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
    return "SearchResultJpa [id=" + id + ", value=" + value + "]";
  }

}
