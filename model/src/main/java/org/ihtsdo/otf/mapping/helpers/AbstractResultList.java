package org.ihtsdo.otf.mapping.helpers;

/**
 * Abstract implementation of {@link ResultList}.
 * @param <T> the type sorting
 */
public abstract class AbstractResultList<T> implements ResultList<T> {

  /** The total count. */
  private int totalCount = 0;

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getTotalCount()
   */
  @Override
  public int getTotalCount() {
    return totalCount;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#setTotalCount(int)
   */
  @Override
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

}
