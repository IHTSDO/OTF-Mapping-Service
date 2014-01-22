package org.ihtsdo.otf.mapping.helpers;

import java.util.Comparator;

/**
 * The interface for the paging/filtering/sorting parameter oject
 * @author Patrick
 *
 */
public interface PfsParameter {

	/**
	 * Returns the maximum number of results
	 * @return the maximum number of results
	 */
	public int getMaxResults();

	/**
	 * Sets the maximum number of results
	 * @param maxResults the maximum number of results
	 */
	public void setMaxResults(int maxResults);

	/**
	 * Returns the starting index of a query result subset
	 * @return the start index
	 */
	public int getStartIndex();

	/**
	 * Sets the starting index of a query result subset
	 * @param startIndex the start index
	 */
	public void setStartIndex(int startIndex);

	/**
	 * Returns the ending index of a query result subset
	 * @return the end index
	 */
	public int getEndIndex();

	/**
	 * Sets the ending index of a query results subset
	 * @param endIndex the end index
	 */
	public void setEndIndex(int endIndex);

	/**
	 * Returns the filter string
	 * @return the filter string
	 */
	public String getFilters();

	/**
	 * Sets the filter string
	 * @param filters the filter string
	 */
	public void setFilters(String filters);

	/** 
	 * Returns the sorting comparator
	 * @return the sorting comparator
	 */
	public Comparator<Object> getsortComparator();

	/** 
	 * Sets the sorting comparator
	 * @param sortComparator the sorting comparator
	 */
	public void setsortComparator(Comparator<Object> sortComparator);
}
