package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Represents a sortable list of {@link SearchResult} objects.
 */
public interface SearchResultList extends ResultList<SearchResult> {

  /**
   * Add a search result.
   * 
   * @param searchResult the search result to add
   */
  public void addSearchResult(SearchResult searchResult);

  /**
   * Remove a search result.
   * 
   * @param searchResult the search result to remove
   */
  public void removeSearchResult(SearchResult searchResult);

  /**
   * Gets the search results.
   * 
   * @return the SearchResults
   */
  public List<SearchResult> getSearchResults();

  /**
   * Sets the search results.
   * 
   * @param searchResults the search results
   */
  public void setSearchResults(List<SearchResult> searchResults);

	/**
	 * Adds the search results.
	 *
	 * @param searchResultList the search result list
	 */
	public void addSearchResults(SearchResultList searchResultList);

}
