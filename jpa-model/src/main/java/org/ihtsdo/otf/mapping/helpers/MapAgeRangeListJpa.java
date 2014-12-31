package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.model.MapAgeRange;

/**
 * JAXB enabled implementation of {@link MapAgeRangeList}.
 */
@XmlRootElement(name = "mapAgeRangeList")
public class MapAgeRangeListJpa extends AbstractResultList<MapAgeRange>
    implements MapAgeRangeList {

  /** The map projects. */
  private List<MapAgeRange> mapAgeRanges = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public MapAgeRangeListJpa() {
    // do nothing
  }

  /**
   * Adds the map project.
   * 
   * @param mapAgeRange the map project
   */
  @Override
  public void addMapAgeRange(MapAgeRange mapAgeRange) {
    mapAgeRanges.add(mapAgeRange);
  }

  /**
   * Removes the map project.
   * 
   * @param mapAgeRange the map project
   */
  @Override
  public void removeMapAgeRange(MapAgeRange mapAgeRange) {
    mapAgeRanges.remove(mapAgeRange);
  }

  /**
   * Sets the map projects.
   * 
   * @param mapAgeRanges the new map projects
   */
  @Override
  public void setMapAgeRanges(List<MapAgeRange> mapAgeRanges) {
    this.mapAgeRanges = new ArrayList<>();
    this.mapAgeRanges.addAll(mapAgeRanges);

  }

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  @Override
  @XmlElement(type = MapAgeRangeJpa.class, name = "mapAgeRange")
  public List<MapAgeRange> getMapAgeRanges() {
    return mapAgeRanges;
  }

  /**
   * Return the count as an xml element.
   * 
   * @return the number of objects in the list
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapAgeRanges.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapAgeRange> comparator) {
    Collections.sort(mapAgeRanges, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapAgeRange element) {
    return mapAgeRanges.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapAgeRange> getIterable() {
    return mapAgeRanges;
  }

}
