package org.ihtsdo.otf.mapping.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container class for SearchResult
 * @author Patrick
 *
 */
@XmlRootElement
public class SearchResultList {

	/** The number of results in this list */
	private Long count;
	
	/** The results */
	private List<SearchResult> searchResults;

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
	
	public void sortResultsById() {
		
		Collections.sort(
				searchResults, 
				new Comparator<SearchResult>() {
					public int compare(SearchResult sr1, SearchResult sr2) {
						return sr1.getId().compareTo(sr2.getId());
					}
				}
		);
	}
	
	public void sortResultsByDescription() {
		
		Collections.sort(
				searchResults, 
				new Comparator<SearchResult>() {
					public int compare(SearchResult sr1, SearchResult sr2) {
						return sr1.getDescription().compareTo(sr2.getDescription());
					}
				}
		);
	}

	
	

}
