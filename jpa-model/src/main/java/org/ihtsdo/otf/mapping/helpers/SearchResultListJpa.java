package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * JAXB-enabled implementation of {@link SearchResultList}.
 */
@XmlRootElement(name = "searchResultList")
public class SearchResultListJpa extends AbstractResultList<SearchResult>
    implements SearchResultList {

  /** The results */
  private List<SearchResult> searchResults = new ArrayList<>();

  /** The default constructor */
  public SearchResultListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return searchResults.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.SearchResultList#getSearchResults()
   */
  @Override
  @XmlElement(type = SearchResultJpa.class, name = "searchResult")
  public List<SearchResult> getSearchResults() {
    return searchResults;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.SearchResultList#setSearchResults(java.util
   * .List)
   */
  @Override
  public void setSearchResults(List<SearchResult> searchResults) {
    this.searchResults = searchResults;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.SearchResultList#removeSearchResult(org.
   * ihtsdo.otf.mapping.helpers.SearchResult)
   */
  @Override
  public void removeSearchResult(SearchResult searchResult) {
    searchResults.remove(searchResult);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.SearchResultList#addSearchResult(org.ihtsdo
   * .otf.mapping.helpers.SearchResult)
   */
  @Override
  public void addSearchResult(SearchResult searchResult) {
    searchResults.add(searchResult);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.SearchResultList#contains(org.ihtsdo.otf
   * .mapping.helpers.SearchResult)
   */
  @Override
  public boolean contains(SearchResult searchResult) {
    return searchResults.size() == 0 ? false : this.searchResults
        .contains(searchResult);
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
            + ((searchResults == null) ? 0 : searchResults.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
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
    SearchResultListJpa other = (SearchResultListJpa) obj;
    if (searchResults == null) {
      if (other.searchResults != null) {
        return false;
      }
    } else if (!searchResults.equals(other.searchResults)) {
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
    return "SearchResultListJpa [searchResults=" + searchResults
        + ", getCount()=" + getCount() + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<SearchResult> comparator) {
    Collections.sort(searchResults, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<SearchResult> getIterable() {
    return searchResults;
  }

}
