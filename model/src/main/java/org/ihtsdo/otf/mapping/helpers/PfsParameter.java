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
	 * Returns the sort field name
	 * @return the sort field name
	 */
	public String getSortField();

	/** 
	 * Sets the sort field name
	 * @param sortField the sort field name
	 */
	public void setSortField(String sortField);
	
	/**
	 * Indicates whether the index is in range for the given start index and max results settings.
	 * @param i the index to check
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isIndexInRange(int i);
}
