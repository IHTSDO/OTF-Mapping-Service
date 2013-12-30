package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "mapAdviceList")
public class MapAdviceList {

	/** The map projects. */
	private List<MapAdvice> mapAdvices = new ArrayList<MapAdvice>();

	/**
	 * Instantiates a new map project list.
	 */
	public MapAdviceList() {
		// do nothing
	}

	/**
	 * Adds the map project.
	 * 
	 * @param mapAdvice
	 *            the map project
	 */
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/**
	 * Removes the map project.
	 * 
	 * @param mapAdvice
	 *            the map project
	 */
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}

	/**
	 * Sets the map projects.
	 * 
	 * @param mapAdvices
	 *            the new map projects
	 */
	public void setMapAdvices(List<MapAdvice> mapAdvices) {
		this.mapAdvices = new ArrayList<MapAdvice>();
		this.mapAdvices.addAll(mapAdvices);
		
		
	}
	
	/**
	 * Sorts the map projects alphabetically by name
	 */
	public void sortMapAdvices() {
	
		Collections.sort(this.mapAdvices,
			new Comparator<MapAdvice>() {
				@Override
				public int compare(MapAdvice o1, MapAdvice o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=MapAdviceJpa.class, name="mapAdvice")
	public List<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

}
