package org.ihtsdo.otf.mapping.helpers;

import java.util.Comparator;

/**
 * The Jpa implementation of the paging/filtering/sorting object
 * @author Patrick
 *
 */
public class PfsParameterJpa implements PfsParameter {

	/** The maximum number of results */
	private int maxResults = -1;
	
	/** The start index for queries */
	private int startIndex = -1;
	
	/** The filter string */
	private String filters = null;
	
	/** The comparator for sorting */
	private Comparator<Object> sortComparator = null;
	
	/** The default constructor */
	public PfsParameterJpa() {
		// do nothing
	}
	
	/**
	 * Returns the maximum number of results
	 * @return the maximum number of results
	 */
	@Override
	public int getMaxResults() {
		return maxResults;
	}

	/**
	 * Sets the maximum number of results
	 * @param maxResults the maximum number of results
	 */
	@Override
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * Returns the starting index of a query result subset
	 * @return the start index
	 */
	@Override
	public int getStartIndex() {
		return startIndex;
	}

	/**
	 * Sets the starting index of a query result subset
	 * @param startIndex the start index
	 */
	@Override
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	/**
	 * Returns the filter string
	 * @return the filter string
	 */
	@Override
	public String getFilters() {
		return filters;
	}

	/**
	 * Sets the filter string
	 * @param filters the filter string
	 */
	@Override
	public void setFilters(String filters) {
		this.filters = filters;
	}

	/** 
	 * Returns the sorting comparator
	 * @return the sorting comparator
	 */
	@Override
	public Comparator<Object> getsortComparator() {
		return sortComparator;
	}

	/** 
	 * Sets the sorting comparator
	 * @param sortComparator the sorting comparator
	 */
	@Override
	public void setsortComparator(Comparator<Object> sortComparator) {
		this.sortComparator = sortComparator;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.PfsParameter#isIndexInRange(int)
	 */
	@Override
	public boolean isIndexInRange(int i) {
		return getStartIndex() != -1 &&
				getMaxResults() != -1 &&
				 i >= getStartIndex() && 
				 i < (getStartIndex() + getMaxResults());
	}
}
