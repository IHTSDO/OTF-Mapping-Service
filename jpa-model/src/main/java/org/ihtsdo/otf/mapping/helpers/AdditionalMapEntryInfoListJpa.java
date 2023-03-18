package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.AdditionalMapEntryInfoJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;

/**
 * JAXB enabled implementation of {@link AdditionalMapEntryInfoList}.
 */
@XmlRootElement(name = "additionalMapEntryInfoList")
public class AdditionalMapEntryInfoListJpa extends AbstractResultList<AdditionalMapEntryInfo> implements
AdditionalMapEntryInfoList {

  /** The additional Map Entry Infos. */
  private List<AdditionalMapEntryInfo> additionalMapEntryInfos = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public AdditionalMapEntryInfoListJpa() {
    // do nothing
  }

  /**
   * Adds the map project.
   * 
   * @param additionalMapEntryInfo the map project
   */
  @Override
  public void addAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo) {
    additionalMapEntryInfos.add(additionalMapEntryInfo);
  }

  /**
   * Removes the map project.
   * 
   * @param additionalMapEntryInfo the map project
   */
  @Override
  public void removeAdditionalMapEntryInfo(AdditionalMapEntryInfo additionalMapEntryInfo) {
    additionalMapEntryInfos.remove(additionalMapEntryInfo);
  }

  /**
   * Sets the map projects.
   * 
   * @param additionalMapEntryInfos the new map projects
   */
  @Override
  public void setAdditionalMapEntryInfos(List<AdditionalMapEntryInfo> additionalMapEntryInfos) {
    this.additionalMapEntryInfos = new ArrayList<>();
    this.additionalMapEntryInfos.addAll(additionalMapEntryInfos);

  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  @Override
  @XmlElement(type = AdditionalMapEntryInfoJpa.class, name = "additionalMapEntryInfo")
  public List<AdditionalMapEntryInfo> getAdditionalMapEntryInfos() {
    return additionalMapEntryInfos;
  }

  /**
   * Return the count as an xml element.
   * 
   * @return the number of objects in the list
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return additionalMapEntryInfos.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<AdditionalMapEntryInfo> comparator) {
    Collections.sort(additionalMapEntryInfos, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(AdditionalMapEntryInfo element) {
    return additionalMapEntryInfos.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<AdditionalMapEntryInfo> getIterable() {
    return additionalMapEntryInfos;
  }

}
