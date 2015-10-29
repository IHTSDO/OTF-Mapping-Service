package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * JAXB enabled implementation of {@link MapRelationList}.
 */
@XmlRootElement(name = "mapRelationList")
public class MapRelationListJpa extends AbstractResultList<MapRelation>
    implements MapRelationList {

  /** The map relations. */
  private List<MapRelation> mapRelations = new ArrayList<>();

  /**
   * Instantiates a new map relation list.
   */
  public MapRelationListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRelationList#addMapRelation(org.ihtsdo
   * .otf.mapping.model.MapRelation)
   */
  @Override
  public void addMapRelation(MapRelation mapRelation) {
    mapRelations.add(mapRelation);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRelationList#removeMapRelation(org.ihtsdo
   * .otf.mapping.model.MapRelation)
   */
  @Override
  public void removeMapRelation(MapRelation mapRelation) {
    mapRelations.remove(mapRelation);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRelationList#setMapRelations(java.util
   * .List)
   */
  @Override
  public void setMapRelations(List<MapRelation> mapRelations) {
    this.mapRelations = new ArrayList<>();
    this.mapRelations.addAll(mapRelations);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapRelationList#getMapRelations()
   */
  @Override
  @XmlElement(type = MapRelationJpa.class, name = "mapRelation")
  public List<MapRelation> getMapRelations() {
    return mapRelations;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapRelations.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapRelation> comparator) {
    Collections.sort(mapRelations, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapRelation element) {
    return mapRelations.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapRelation> getIterable() {
    return mapRelations;
  }

}
