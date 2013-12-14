package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "MapProjectList")
public class MapProjectList {

	/** The map projects. */
	private List<MapProject> mapProjects = new ArrayList<MapProject>();

	/**
	 * Instantiates a new map project list.
	 */
	public MapProjectList() {
		// do nothing
	}

	/**
	 * Adds the map project.
	 * 
	 * @param mapProject
	 *            the map project
	 */
	public void addMapProject(MapProject mapProject) {
		mapProjects.add(mapProject);
	}

	/**
	 * Removes the map project.
	 * 
	 * @param mapProject
	 *            the map project
	 */
	public void removeMapProject(MapProject mapProject) {
		mapProjects.remove(mapProject);
	}

	/**
	 * Sets the map projects.
	 * 
	 * @param mapProjects
	 *            the new map projects
	 */
	public void setMapProjects(List<MapProject> mapProjects) {
		this.mapProjects = new ArrayList<MapProject>();
		this.mapProjects.addAll(mapProjects);
	}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=MapProjectJpa.class)
	public List<MapProject> getMapProjects() {
		return mapProjects;
	}

}
