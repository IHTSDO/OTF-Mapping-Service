package org.ihtsdo.otf.mapping.reports;

import org.ihtsdo.otf.mapping.helpers.ReportResultType;
/**
 * The Interface ReportResultItem.
 */
public interface ReportResultItem {

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(Long id);

	/**
	 * Gets the item id.
	 * 
	 * @return the item id
	 */
	public Long getItemId();

	/**
	 * Sets the item id.
	 * 
	 * @param itemId
	 *            the new item id
	 */
	public void setItemId(Long itemId);

	/**
	 * Gets the result type.
	 * 
	 * @return the result type
	 */
	public ReportResultType getResultType();

	/**
	 * Sets the result type.
	 * 
	 * @param resultType
	 *            the new result type
	 */
	public void setResultType(ReportResultType resultType);

	ReportResult getReportResult();

	void setReportResult(ReportResult reportResult);

}
