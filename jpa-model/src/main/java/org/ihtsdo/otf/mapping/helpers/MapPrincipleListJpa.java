package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.model.MapPrinciple;

/**
 * JAXB enabled implementation of {@link MapPrincipleList}.
 */
@XmlRootElement(name = "mapPrincipleList")
public class MapPrincipleListJpa extends AbstractResultList<MapPrinciple>
    implements MapPrincipleList {

  /** The map principles. */
  private List<MapPrinciple> mapPrinciples = new ArrayList<>();

  /**
   * Default Constructor
   */
  public MapPrincipleListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapPrincipleList#addMapPrinciple(org.ihtsdo
   * .otf.mapping.model.MapPrinciple)
   */
  @Override
  public void addMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.add(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapPrincipleList#removeMapPrinciple(org.
   * ihtsdo.otf.mapping.model.MapPrinciple)
   */
  @Override
  public void removeMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.remove(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapPrincipleList#setmapPrinciples(java.util
   * .List)
   */
  @Override
  public void setMapPrinciples(List<MapPrinciple> mapPrinciples) {
    this.mapPrinciples = new ArrayList<>();
    this.mapPrinciples.addAll(mapPrinciples);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapPrincipleList#getmapPrinciples()
   */
  @Override
  @XmlElement(type = MapPrincipleJpa.class, name = "mapPrinciple")
  public List<MapPrinciple> getMapPrinciples() {
    return mapPrinciples;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapPrinciples.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapPrinciple> comparator) {
    Collections.sort(mapPrinciples, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapPrinciple element) {
    return mapPrinciples.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapPrinciple> getIterable() {
    return mapPrinciples;
  }

}
