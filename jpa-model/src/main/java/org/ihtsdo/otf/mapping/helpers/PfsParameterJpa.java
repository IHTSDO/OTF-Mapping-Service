package org.ihtsdo.otf.mapping.helpers;

import java.util.Comparator;

/**
 * The Jpa implementation of the paging/filtering/sorting object
 * @author Patrick
 *
 */
public class PfsParameterJpa implements PfsParameter {

	/** The maximum number of results */
	private Long maxResults;
	
	/** The start index for queries */
	private Long startIndex;
	
	/** The end index for queries */
	private Long endIndex;
	
	/** The filter string */
	private String filters;
	
	/** The comparator for sorting */
	private Comparator<Object> sortComparator;
	
	/** The default constructor */
	public PfsParameterJpa() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Returns the maximum number of results
	 * @return the maximum number of results
	 */
	public Long getMaxResults() {
		return maxResults;
	}

	/**
	 * Sets the maximum number of results
	 * @param maxResults the maximum number of results
	 */
	public void setMaxResults(Long maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * Returns the starting index of a query result subset
	 * @return the start index
	 */
	public Long getStartIndex() {
		return startIndex;
	}

	/**
	 * Sets the starting index of a query result subset
	 * @param startIndex the start index
	 */
	public void setStartIndex(Long startIndex) {
		this.startIndex = startIndex;
	}

	/**
	 * Returns the ending index of a query result subset
	 * @return the end index
	 */
	public Long getEndIndex() {
		return endIndex;
	}

	/**
	 * Sets the ending index of a query results subset
	 * @param endIndex the end index
	 */
	public void setEndIndex(Long endIndex) {
		this.endIndex = endIndex;
	}

	/**
	 * Returns the filter string
	 * @return the filter string
	 */
	public String getFilters() {
		return filters;
	}

	/**
	 * Sets the filter string
	 * @param filters the filter string
	 */
	public void setFilters(String filters) {
		this.filters = filters;
	}

	/** 
	 * Returns the sorting comparator
	 * @return the sorting comparator
	 */
	public Comparator<Object> getsortComparator() {
		return sortComparator;
	}

	/** 
	 * Sets the sorting comparator
	 * @param sortComparator the sorting comparator
	 */
	public void setsortComparator(Comparator<Object> sortComparator) {
		this.sortComparator = sortComparator;
	}

}
