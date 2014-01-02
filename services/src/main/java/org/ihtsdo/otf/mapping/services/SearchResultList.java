package org.ihtsdo.otf.mapping.services;

import java.util.List;

/**
 * Interface for SearchResultList
 * @author Patrick
 *
 */
public interface SearchResultList {

	/**
	 * @return the number of SearchResults
	 */
	public int getCount();

	/**
	 * @return the SearchResults
	 */
	public List<SearchResult> getSearchResults();
	
	 /**
	  * Sets the search results
	  * @param searchResults the search results
	  */
	public void setSearchResults(List<SearchResult> searchResults);
	
	/**
	 * Remove a search result
	 * @param searchResult the search result to remove
	 */
	public void removeSearchResult(SearchResult searchResult);
	
	/**
	 * Add a search result
	 * @param searchResult the search result to add
	 */
	public void addSearchResult(SearchResult searchResult);
	
	/**
	 * Sort results by id
	 */
	public void sortSearchResultsById();
	
	/**
	 * Sort results by description
	 */
	public void sortSearchResultsByDescription();
}
