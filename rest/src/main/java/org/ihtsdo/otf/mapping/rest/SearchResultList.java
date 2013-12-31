package org.ihtsdo.otf.mapping.rest;

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
	
	

}
