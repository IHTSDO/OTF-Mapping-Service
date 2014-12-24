package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;

/**
 * JAXB enabled implementation of {@link MapEntryList}
 */
@XmlRootElement(name = "mapEntryList")
public class MapEntryListJpa extends AbstractResultList<MapEntry> implements
    MapEntryList {

  /** The map entrys. */
  private List<MapEntry> mapEntrys = new ArrayList<>();

  /**
   * Instantiates a new map entry list.
   */
  public MapEntryListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapEntryList#addMapEntry(org.ihtsdo.otf.
   * mapping.model.MapEntry)
   */
  @Override
  public void addMapEntry(MapEntry MapEntry) {
    mapEntrys.add(MapEntry);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapEntryList#removeMapEntry(org.ihtsdo.otf
   * .mapping.model.MapEntry)
   */
  @Override
  public void removeMapEntry(MapEntry MapEntry) {
    mapEntrys.remove(MapEntry);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapEntryList#setmapEntrys(java.util.List)
   */
  @Override
  public void setMapEntrys(List<MapEntry> mapEntrys) {
    this.mapEntrys = new ArrayList<>();
    this.mapEntrys.addAll(mapEntrys);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapEntryList#getmapEntrys()
   */
  @Override
  @XmlElement(type = MapEntryJpa.class, name = "mapEntry")
  public List<MapEntry> getmapEntrys() {
    return mapEntrys;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapEntrys.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapEntry> comparator) {
    Collections.sort(mapEntrys, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapEntry element) {
    return mapEntrys.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapEntry> getIterable() {
    return mapEntrys;
  }

}
