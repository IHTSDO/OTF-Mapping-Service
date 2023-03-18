package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;

/**
 * Represents a sortable list of {@link AdditionalMapEntryInfo}.
 */
public interface AdditionalMapEntryInfoList extends ResultList<AdditionalMapEntryInfo> {

  /**
   * Adds the additional Map Entry Info.
   * 
   * @param additionalMapEntryInfo the additional Map Entry Info
   */
  public void addAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo);

  /**
   * Removes the additional Map Entry Info.
   * 
   * @param additionalMapEntryInfo the additional Map Entry Info
   */
  public void removeAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo);

  /**
   * Sets the additional Map Entry Infos.
   * 
   * @param additionalMapEntryInfos the new additional Map Entry Infos
   */
  public void setAdditionalMapEntryInfos(List<AdditionalMapEntryInfo> additionalMapEntryInfos);

  /**
   * Gets the additional Map Entry Infos.
   * 
   * @return the additional Map Entry Infos
   */
  public List<AdditionalMapEntryInfo> getAdditionalMapEntryInfos();

}
