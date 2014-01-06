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
@XmlRootElement(name = "searchResultList")
public class SearchResultListJpa implements SearchResultList {
	
	/** The results */
	private List<SearchResult> searchResults = new ArrayList<SearchResult>();

	/** The default constructor */
	public SearchResultListJpa() {
	}
	
	/**
	 * @return the count
	 */
	@Override
	@XmlElement(name = "count")
	public int getCount() {
		return searchResults.size();
	}

	/**
	 * @return the searchResults
	 */
	@Override
	@XmlElement(type=SearchResultJpa.class, name = "searchResult")
	public List<SearchResult> getSearchResults() {
		return searchResults;
	}

	/**
	 * @param searchResults the searchResults to set
	 */
	@Override
	public void setSearchResults(List<SearchResult> searchResults) {
		this.searchResults = searchResults;
	}
	
	/**
	 * Removes a search result
	 * @param searchResult the search result to remove
	 */
	@Override
	public void removeSearchResult(SearchResult searchResult) {
		searchResults.remove(searchResult);
	}
	
	/**
	 * Add a search result
	 * @param searchResult the search result to add
	 */
	@Override
	public void addSearchResult(SearchResult searchResult) {
		searchResults.add(searchResult);
	}
	
	/** 
	 * Boolean test if object is in search result list
	 * @param searchResult the search result to be compared
	 * @return boolean true/false
	 */
	@Override
	public boolean contains(SearchResult searchResult) {
		return searchResults.size() == 0 ? false : this.searchResults.contains(searchResult);
	}
	
	/**
	 * Sorts the SearchResultList numerically by id
	 */
	@Override
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
	@Override
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
