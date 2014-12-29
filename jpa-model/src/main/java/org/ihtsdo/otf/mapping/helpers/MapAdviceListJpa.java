package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * JAXB enabled implementation of {@link MapAdviceList}.
 */
@XmlRootElement(name = "mapAdviceList")
public class MapAdviceListJpa extends AbstractResultList<MapAdvice> implements
    MapAdviceList {

  /** The map projects. */
  private List<MapAdvice> mapAdvices = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public MapAdviceListJpa() {
    // do nothing
  }

  /**
   * Adds the map project.
   * 
   * @param mapAdvice the map project
   */
  @Override
  public void addMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.add(mapAdvice);
  }

  /**
   * Removes the map project.
   * 
   * @param mapAdvice the map project
   */
  @Override
  public void removeMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.remove(mapAdvice);
  }

  /**
   * Sets the map projects.
   * 
   * @param mapAdvices the new map projects
   */
  @Override
  public void setMapAdvices(List<MapAdvice> mapAdvices) {
    this.mapAdvices = new ArrayList<>();
    this.mapAdvices.addAll(mapAdvices);

  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  @Override
  @XmlElement(type = MapAdviceJpa.class, name = "mapAdvice")
  public List<MapAdvice> getMapAdvices() {
    return mapAdvices;
  }

  /**
   * Return the count as an xml element.
   * 
   * @return the number of objects in the list
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapAdvices.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapAdvice> comparator) {
    Collections.sort(mapAdvices, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapAdvice element) {
    return mapAdvices.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapAdvice> getIterable() {
    return mapAdvices;
  }

}
