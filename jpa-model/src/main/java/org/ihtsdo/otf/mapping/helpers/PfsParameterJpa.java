package org.ihtsdo.otf.mapping.helpers;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Jpa implementation of the paging/filtering/sorting object
 * @author Patrick
 * 
 */
@XmlRootElement
public class PfsParameterJpa implements PfsParameter {

  /** The maximum number of results */
  private int maxResults = -1;

  /** The start index for queries */
  private int startIndex = -1;

  /** The filter string */
  private String queryRestriction = null;

  /** The comparator for sorting */
  private String sortField = null;

  /** The default constructor */
  public PfsParameterJpa() {
    // do nothing
  }

  /**
   * Returns the maximum number of results
   * @return the maximum number of results
   */
  @Override
  public int getMaxResults() {
    return maxResults;
  }

  /**
   * Sets the maximum number of results
   * @param maxResults the maximum number of results
   */
  @Override
  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  /**
   * Returns the starting index of a query result subset
   * @return the start index
   */
  @Override
  public int getStartIndex() {
    return startIndex;
  }

  /**
   * Sets the starting index of a query result subset
   * @param startIndex the start index
   */
  @Override
  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  /**
   * Returns the filter string
   * @return the filter string
   */
  @Override
  public String getQueryRestriction() {
    return queryRestriction;
  }

  /**
   * Sets the filter string
   * @param queryRestriction the filter string
   */
  @Override
  public void setQueryRestriction(String queryRestriction) {
    this.queryRestriction = queryRestriction;
  }

  /**
   * Gets the sort field.
   * 
   * @return the sort field
   */
  @Override
  public String getSortField() {
    return sortField;
  }

  /**
   * Sets the sort field
   * 
   * @param sortField
   */
  @Override
  public void setSortField(String sortField) {
    this.sortField = sortField;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.PfsParameter#isIndexInRange(int)
   */
  @Override
  public boolean isIndexInRange(int i) {
    return getStartIndex() != -1 && getMaxResults() != -1
        && i >= getStartIndex() && i < (getStartIndex() + getMaxResults());
  }

  @Override
  public String toString() {
    return "PfsParameterJpa [maxResults=" + maxResults + ", startIndex="
        + startIndex + ", queryRestriction=" + queryRestriction
        + ", sortField=" + sortField + "]";
  }

}
