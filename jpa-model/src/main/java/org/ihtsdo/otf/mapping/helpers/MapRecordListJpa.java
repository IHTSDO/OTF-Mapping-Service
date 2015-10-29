package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * JAXB enabled implementation of {@link MapRecordList}
 */
@XmlRootElement(name = "mapRecordList")
public class MapRecordListJpa extends AbstractResultList<MapRecord> implements
    MapRecordList {

  /** The map records. */
  private List<MapRecord> mapRecords = new ArrayList<>();

  /**
   * Instantiates a new map record list.
   */
  public MapRecordListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRecordList#addMapRecord(org.ihtsdo.otf
   * .mapping.model.MapRecord)
   */
  @Override
  public void addMapRecord(MapRecord mapRecord) {
    mapRecords.add(mapRecord);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRecordList#removeMapRecord(org.ihtsdo
   * .otf.mapping.model.MapRecord)
   */
  @Override
  public void removeMapRecord(MapRecord mapRecord) {
    mapRecords.remove(mapRecord);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapRecordList#setMapRecords(java.util.List)
   */
  @Override
  public void setMapRecords(List<MapRecord> mapRecords) {
    this.mapRecords = new ArrayList<>();
    if (mapRecords != null) {
      this.mapRecords.addAll(mapRecords);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapRecordList#getMapRecords()
   */
  @Override
  @XmlElement(type = MapRecordJpa.class, name = "mapRecord")
  public List<MapRecord> getMapRecords() {
    return mapRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapRecords.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapRecord> comparator) {
    Collections.sort(mapRecords, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapRecord element) {
    return mapRecords.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapRecord> getIterable() {
    return mapRecords;
  }

}
