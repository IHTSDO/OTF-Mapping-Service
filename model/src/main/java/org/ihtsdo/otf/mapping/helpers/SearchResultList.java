package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Interface for SearchResultList.
 *
 * @author Patrick
 */
public interface SearchResultList {

	/**
	 * Gets the count.
	 *
	 * @return the number of SearchResults
	 */
	public int getCount();

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
	 * Remove a search result.
	 *
	 * @param searchResult the search result to remove
	 */
	public void removeSearchResult(SearchResult searchResult);
	
	/**
	 * Add a search result.
	 *
	 * @param searchResult the search result to add
	 */
	public void addSearchResult(SearchResult searchResult);
	
	/**
	 * Sort results by id.
	 */
	public void sortSearchResultsById();
	
	/**
	 * Sort results by description.
	 */
	public void sortSearchResultsByValue();

	/**
	 * Test if search result is in this list.
	 *
	 * @param searchResult the search result
	 * @return boolean true/false on search result in list
	 */
	public boolean contains(SearchResult searchResult);

	/**
	 * Gets the total count.
	 *
	 * @return the total count
	 */
	public Long getTotalCount();

	/**
	 * Sets the total count.
	 *
	 * @param totalCount the new total count
	 */
	public void setTotalCount(Long totalCount);
}
