package org.ihtsdo.otf.mapping.reports;

import org.ihtsdo.otf.mapping.helpers.ReportResultType;

/**
 * Generically represents an item within a {@link ReportResult}.
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
   * @param id the new id
   */
  public void setId(Long id);

  /**
   * Gets the item id.
   * 
   * @return the item id
   */
  public String getItemId();

  /**
   * Sets the item id.
   * 
   * @param itemId the new item id
   */
  public void setItemId(String itemId);

  /**
   * Returns the item name.
   *
   * @return the item name
   */
  public String getItemName();

  /**
   * Sets the item name.
   *
   * @param itemName the item name
   */
  public void setItemName(String itemName);

  /**
   * Gets the result type.
   * 
   * @return the result type
   */
  public ReportResultType getResultType();

  /**
   * Sets the result type.
   * 
   * @param resultType the new result type
   */
  public void setResultType(ReportResultType resultType);

  /**
   * Gets the report result.
   *
   * @return the report result
   */
  public ReportResult getReportResult();

  /**
   * Sets the report result.
   *
   * @param reportResult the new report result
   */
  public void setReportResult(ReportResult reportResult);

}
