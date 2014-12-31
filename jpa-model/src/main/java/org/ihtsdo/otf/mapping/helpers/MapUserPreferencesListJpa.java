package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;

/**
 * JAXB enabled implementation of {@link MapUserPreferencesList}.
 */
@XmlRootElement(name = "mapUserPreferencesList")
public class MapUserPreferencesListJpa extends
    AbstractResultList<MapUserPreferences> implements MapUserPreferencesList {

  /** The map entrys. */
  private List<MapUserPreferences> mapUserPreferences = new ArrayList<>();

  /**
   * Instantiates a new map entry list.
   */
  public MapUserPreferencesListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList#addMapUserPreferences
   * (org.ihtsdo.otf.mapping.model.MapUserPreferences)
   */
  @Override
  public void addMapUserPreferences(MapUserPreferences MapUserPreferences) {
    mapUserPreferences.add(MapUserPreferences);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList#removeMapUserPreferences
   * (org.ihtsdo.otf.mapping.model.MapUserPreferences)
   */
  @Override
  public void removeMapUserPreferences(MapUserPreferences MapUserPreferences) {
    mapUserPreferences.remove(MapUserPreferences);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList#setMapUserPreferences
   * (java.util.List)
   */
  @Override
  public void setMapUserPreferences(List<MapUserPreferences> mapUserPreferences) {
    this.mapUserPreferences = new ArrayList<>();
    this.mapUserPreferences.addAll(mapUserPreferences);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList#getMapUserPreferences
   * ()
   */
  @Override
  @XmlElement(type = MapUserPreferencesJpa.class, name = "mapUserPreferences")
  public List<MapUserPreferences> getMapUserPreferences() {
    return mapUserPreferences;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapUserPreferences.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapUserPreferences> comparator) {
    Collections.sort(mapUserPreferences, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapUserPreferences element) {
    return mapUserPreferences.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapUserPreferences> getIterable() {
    return mapUserPreferences;
  }

}
