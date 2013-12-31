package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.services.SearchResult;
import org.ihtsdo.otf.mapping.services.SearchResultList;

/**
 * Container class for SearchResult
 * @author Patrick
 *
 */
@XmlRootElement
public class SearchResultListJpa implements SearchResultList {

	/** The number of results in this list */
	private Long count;
	
	/** The results */
	private List<SearchResult> searchResults;

	/** The default constructor */
	public SearchResultListJpa() {
		// left empty
	}
	
	/**
	 * @return the count
	 */
	public Long getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(Long count) {
		this.count = count;
	}

	/**
	 * @return the searchResults
	 */
	public List<SearchResult> getSearchResults() {
		return searchResults;
	}

	/**
	 * @param searchResults the searchResults to set
	 */
	public void setSearchResults(List<SearchResult> searchResults) {
		this.searchResults = searchResults;
	}
	
	/**
	 * Removes a search result
	 * @param searchResult the search result to remove
	 */
	public void removeSearchResult(SearchResult searchResult) {
		searchResults.remove(searchResult);
	}
	
	/**
	 * Add a search result
	 * @param searchResult the search result to add
	 */
	public void addSearchResult(SearchResult searchResult) {
		searchResults.add(searchResult);
	}
	
	/**
	 * Sorts the SearchResultList numerically by id
	 */
	public void sortSearchResultsById() {
		
		Collections.sort(
				searchResults, 
				new Comparator<SearchResult>() {
					public int compare(SearchResult sr1, SearchResult sr2) {
						return sr1.getId().compareTo(sr2.getId());
					}
				}
		);
	}
	
	/**
	 * Sorts the SearchResultList alphabetically by description
	 */
	public void sortSearchResultsByDescription() {
		
		Collections.sort(
				searchResults, 
				new Comparator<SearchResult>() {
					public int compare(SearchResult sr1, SearchResult sr2) {
						return sr1.getDescription().compareTo(sr2.getDescription());
					}
				}
		);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((count == null) ? 0 : count.hashCode());
		result = prime * result
				+ ((searchResults == null) ? 0 : searchResults.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (count == null) {
			if (other.count != null) {
				return false;
			}
		} else if (!count.equals(other.count)) {
			return false;
		}
		if (searchResults == null) {
			if (other.searchResults != null) {
				return false;
			}
		} else if (!searchResults.equals(other.searchResults)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SearchResultListJpa [count=" + count + ", searchResults="
				+ searchResults + "]";
	}

	
	

}
