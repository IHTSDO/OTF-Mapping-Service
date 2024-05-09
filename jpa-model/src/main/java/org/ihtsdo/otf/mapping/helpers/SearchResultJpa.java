package org.ihtsdo.otf.mapping.helpers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The search result for the Jpa package.
 */
@XmlRootElement(name = "searchResult")
public class SearchResultJpa implements SearchResult {

  /** The id. */
  private Long id;

  /** The terminology id. */
  private String terminologyId;

  /** The terminology. */
  private String terminology;

  /** The terminology version. */
  private String terminologyVersion;

  /** The terminology version. */
  private String terminologyNote;

  /** The value. */
  private String value;

  /** The value2. */
  private String value2;

  /**
   * Default constructor.
   */
  public SearchResultJpa() {
    // left empty
  }

  /**
   * Constructor.
   *
   * @param id the id
   * @param terminologyId the terminologyId
   * @param value the value
   * @param value2 the value2
   */
  public SearchResultJpa(final Long id, final String terminologyId, final String value,
      final String value2) {
    this.id = id;
    this.terminologyId = terminologyId;
    this.value = value;
    this.value2 = value2;
  }

  /**
   * Returns the id.
   *
   * @return the id
   */
  @Override
  @XmlElement(name = "id")
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the id.
   *
   * @param id the id
   */
  @Override
  public void setId(final Long id) {
    this.id = id;

  }

  /**
   * Returns the id.
   *
   * @return the id
   */
  @Override
  @XmlElement(name = "terminologyId")
  public String getTerminologyId() {
    return this.terminologyId;
  }

  /**
   * Sets the id.
   *
   * @param terminologyId the id
   */
  @Override
  public void setTerminologyId(final String terminologyId) {
    this.terminologyId = terminologyId;

  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#getTerminology()
   */
  @Override
  public String getTerminology() {
    return this.terminology;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#setTerminology(java.lang.String )
   */
  @Override
  public void setTerminology(final String terminology) {
    this.terminology = terminology;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#getTerminologyVersion()
   */
  @Override
  public String getTerminologyVersion() {
    return this.terminologyVersion;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#setTerminologyVersion(java. lang.String)
   */
  @Override
  public void setTerminologyVersion(final String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#getTerminologyNote()
   */
  @Override
  @XmlElement(name = "terminologyNote")
  public String getTerminologyNote() {
    return this.terminologyNote;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#setTerminologyNote(java. lang.String)
   */
  @Override
  public void setTerminologyNote(final String terminologyNote) {
    this.terminologyNote = terminologyNote;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  @Override
  @XmlElement(name = "value")
  public String getValue() {
    return this.value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  @Override
  public void setValue(final String value) {
    this.value = value;

  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#getValue2()
   */
  @Override
  @XmlElement(name = "value2")
  public String getValue2() {
    return value2;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.helpers.SearchResult#setValue2(java.lang.String)
   */
  @Override
  public void setValue2(final String value2) {
    this.value2 = value2;

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
    result = prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result = prime * result + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    result = prime * result + ((terminologyNote == null) ? 0 : terminologyNote.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SearchResultJpa other = (SearchResultJpa) obj;
    if (terminology == null) {
      if (other.terminology != null) {
        return false;
      }
    } else if (!terminology.equals(other.terminology)) {
      return false;
    }
    if (terminologyId == null) {
      if (other.terminologyId != null) {
        return false;
      }
    } else if (!terminologyId.equals(other.terminologyId)) {
      return false;
    }
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null) {
        return false;
      }
    } else if (!terminologyVersion.equals(other.terminologyVersion)) {
      return false;
    }
    if (terminologyNote == null) {
      if (other.terminologyNote != null) {
        return false;
      }
    } else if (!terminologyNote.equals(other.terminologyNote)) {
      return false;
    }
    if (value == null) {
      if (other.value != null) {
        return false;
      }
    } else if (!value.equals(other.value)) {
      return false;
    }
    if (value2 == null) {
      if (other.value2 != null) {
        return false;
      }
    } else if (!value2.equals(other.value2)) {
      return false;
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "SearchResultJpa [id=" + id + ", terminologyId=" + terminologyId + ", terminology="
        + terminology + ", terminologyVersion=" + terminologyVersion + ", value=" + value
        + ", value2=" + value2 + "]";
  }

}
