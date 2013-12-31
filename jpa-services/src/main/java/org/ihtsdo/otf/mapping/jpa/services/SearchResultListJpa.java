package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.services.SearchResult;
import org.ihtsdo.otf.mapping.services.SearchResultList;

/**
 * Container class for SearchResult
 * @author Patrick
 *
 */
@XmlRootElement(name = "searchResultsList")
public class SearchResultListJpa implements SearchResultList {
	
	/** The results */
	private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	/** The default constructor */
	public SearchResultListJpa() {
	}
	
	/**
	 * @return the count
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return searchResults.size();
	}

	/**
	 * @return the searchResults
	 */
	@XmlElement(name = "searchResults")
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((searchResults == null) ? 0 : searchResults.hashCode());
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

	@Override
	public String toString() {
		return "SearchResultListJpa [searchResults=" + searchResults
				+ ", getCount()=" + getCount() + "]";
	}

	
	

}
