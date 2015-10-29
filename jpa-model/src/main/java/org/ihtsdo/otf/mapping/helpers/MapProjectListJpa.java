package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * JAXB enabled implementation of {@link MapProjectList}
 */
@XmlRootElement(name = "mapProjectList")
public class MapProjectListJpa extends AbstractResultList<MapProject> implements
    MapProjectList {

  /** The map projects. */
  private List<MapProject> mapProjects = new ArrayList<>();

  /**
   * Instantiates a new map project list.
   */
  public MapProjectListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapProjectList#addMapProject(org.ihtsdo.
   * otf.mapping.model.MapProject)
   */
  @Override
  public void addMapProject(MapProject mapProject) {
    mapProjects.add(mapProject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapProjectList#removeMapProject(org.ihtsdo
   * .otf.mapping.model.MapProject)
   */
  @Override
  public void removeMapProject(MapProject mapProject) {
    mapProjects.remove(mapProject);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.MapProjectList#setMapProjects(java.util.
   * List)
   */
  @Override
  public void setMapProjects(List<MapProject> mapProjects) {
    this.mapProjects = new ArrayList<>();
    if (mapProjects != null) {
      this.mapProjects.addAll(mapProjects);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.MapProjectList#getMapProjects()
   */
  @Override
  @XmlElement(type = MapProjectJpa.class, name = "mapProject")
  public List<MapProject> getMapProjects() {
    return mapProjects;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return mapProjects.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<MapProject> comparator) {
    Collections.sort(mapProjects, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(MapProject element) {
    return mapProjects.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<MapProject> getIterable() {
    return mapProjects;
  }

}
