package org.ihtsdo.otf.mapping.helpers;

import java.util.Comparator;

/**
 * Container for some kind of results
 * @param <T> the type for sorting
 */
public interface ResultList<T> {

  /**
   * Returns the number of objects in the list.
   * @return the number of objects in the list
   */
  public int getCount();

  /**
   * Returns the total count.
   * 
   * @return the totalCount
   */
  public int getTotalCount();

  /**
   * Sets the total count.
   * 
   * @param totalCount the totalCount to set
   */
  public void setTotalCount(int totalCount);

  /**
   * Sorts by the specified comparator.
   * 
   * @param comparator the comparator
   */
  public void sortBy(Comparator<T> comparator);

  /**
   * Indicates whether or not the list contains the specified element.
   * 
   * @param element the element
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  public boolean contains(T element);

  /**
   * Returns the iterable.
   * 
   * @return the iterable
   */
  public Iterable<T> getIterable();
}
