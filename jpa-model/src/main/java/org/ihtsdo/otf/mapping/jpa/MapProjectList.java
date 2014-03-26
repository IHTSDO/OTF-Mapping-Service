package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "mapProjectList")
public class MapProjectList {

	/** The map projects. */
	private List<MapProject> mapProjects = new ArrayList<>();

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
		this.mapProjects = new ArrayList<>();
		if (mapProjects != null) {
			this.mapProjects.addAll(mapProjects);
		}
		
		
	}
	
	/**
	 * Sorts the map projects alphabetically by name
	 */
	public void sortMapProjects() {
	
		Collections.sort(this.mapProjects,
			new Comparator<MapProject>() {
				@Override
				public int compare(MapProject o1, MapProject o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=MapProjectJpa.class, name="mapProject")
	public List<MapProject> getMapProjects() {
		return mapProjects;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapProjects.size();
	}

}
