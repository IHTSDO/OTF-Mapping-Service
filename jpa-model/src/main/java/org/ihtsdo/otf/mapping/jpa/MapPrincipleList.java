package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapPrinciple;

/**
 * Container for map principles.
 */
@XmlRootElement(name = "mapPrincipleList")
public class MapPrincipleList {

	/** The map principles. */
	private List<MapPrinciple> mapPrinciples = new ArrayList<MapPrinciple>();

	/**
	 * Default Constructor
	 */
	public MapPrincipleList() {
		// do nothing
	}

	/**
	 * Adds the map principle.
	 * 
	 * @param mapPrinciple
	 *            the map principle
	 */
	public void addmapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	/**
	 * Removes the map principle.
	 * 
	 * @param mapPrinciple
	 *            the map principle
	 */
	public void removemapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/**
	 * Sets the map principles.
	 * 
	 * @param mapPrinciples
	 *            the new map principles
	 */
	public void setmapPrinciples(List<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = new ArrayList<MapPrinciple>();
		this.mapPrinciples.addAll(mapPrinciples);
		
		
	}
	
	/**
	 * Sorts the map principles alphabetically by name
	 */
	public void sortmapPrinciples() {
	
		Collections.sort(this.mapPrinciples,
			new Comparator<MapPrinciple>() {
				@Override
				public int compare(MapPrinciple o1, MapPrinciple o2) {
					return o1.getId().compareTo(o2.getId());
				}

			});
}

	/**
	 * Gets the map principles.
	 * 
	 * @return the map principles
	 */
	@XmlElement(type=MapPrincipleJpa.class, name="mapPrinciple")
	public List<MapPrinciple> getmapPrinciples() {
		return mapPrinciples;
	}

	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapPrinciples.size();
	}
}
