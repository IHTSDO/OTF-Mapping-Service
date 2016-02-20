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
public class SearchResultListJpa extends AbstractResultList<SearchResult> implements SearchResultList {

	/**  The results. */
	private List<SearchResult> searchResults = new ArrayList<>();

	/**
	 *  The default constructor.
	 */
	public SearchResultListJpa() {
		// do nothing
	}

	/**
	 * Gets the count.
	 *
	 * @return the count
	 */
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

	/**
	 * Gets the search results.
	 *
	 * @return the search results
	 */
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

	/**
	 * Sets the search results.
	 *
	 * @param searchResults the new search results
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.SearchResultList#setSearchResults(java.
	 * util .List)
	 */
	@Override
	public void setSearchResults(List<SearchResult> searchResults) {
		this.searchResults = searchResults;
	}

	/**
	 * Removes the search result.
	 *
	 * @param searchResult the search result
	 */
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

	/**
	 * Adds the search result.
	 *
	 * @param searchResult the search result
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.SearchResultList#addSearchResult(org.
	 * ihtsdo .otf.mapping.helpers.SearchResult)
	 */
	@Override
	public void addSearchResult(SearchResult searchResult) {
		searchResults.add(searchResult);
	}

	/**
	 * Contains.
	 *
	 * @param searchResult the search result
	 * @return true, if successful
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.SearchResultList#contains(org.ihtsdo.otf
	 * .mapping.helpers.SearchResult)
	 */
	@Override
	public boolean contains(SearchResult searchResult) {
		return searchResults.size() == 0 ? false : this.searchResults.contains(searchResult);
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((searchResults == null) ? 0 : searchResults.hashCode());
		return result;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
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

	/**
	 * To string.
	 *
	 * @return the string
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SearchResultListJpa [searchResults=" + searchResults + ", getCount()=" + getCount() + "]";
	}

	/**
	 * Sort by.
	 *
	 * @param comparator the comparator
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
	 */
	@Override
	public void sortBy(Comparator<SearchResult> comparator) {
		Collections.sort(searchResults, comparator);
	}

	/**
	 * Gets the iterable.
	 *
	 * @return the iterable
	 */
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

	/**
	 * Adds the search results.
	 *
	 * @param searchResultList the search result list
	 */
	@Override
	public void addSearchResults(SearchResultList searchResultList) {
		searchResults.addAll(searchResultList.getSearchResults());
		setTotalCount(getTotalCount() + searchResultList.getTotalCount());
	}

}
