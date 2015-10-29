package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * JAXB enabled implementation of {@link MapUserList}.
 */
@XmlRootElement(name = "mapUserList")
public class MapUserListJpa extends AbstractResultList<MapUser> implements
    MapUserList {

  /** The map users. */
  private List<MapUser> mapUsers = new ArrayList<>();

  /**
   * Instantiates a new map user list.
   */
  public MapUserListJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link MapUserListJpa} from the specified parameters.
   *
   * @param mapUsers the map users
   */
  public MapUserListJpa(List<MapUser> mapUsers) {
    this.mapUsers = mapUsers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserList#addMapUser(org.ihtsdo.otf.mapping
   * .model.MapUser)
   */
  @Override
  public void addMapUser(MapUser MapUser) {
    mapUsers.add(MapUser);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapUserList#removeMapUser(org.ihtsdo.otf
   * .mapping.model.MapUser)
   */
  @Override
  public void removeMapUser(MapUser MapUser) {
    mapUsers.remove(MapUser);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapUserList#setMapUsers(java.util.List)
   */
  @Override
  public void setMapUsers(List<MapUser> mapUsers) {
    this.mapUsers = new ArrayList<>();
    this.mapUsers.addAll(mapUsers);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapUserList#getMapUsers()
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "mapUser")
  public List<MapUser> getMapUsers() {
    return mapUsers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapUsers.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapUser> comparator) {
    Collections.sort(mapUsers, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapUser element) {
    return mapUsers.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapUser> getIterable() {
    return mapUsers;
  }

}
