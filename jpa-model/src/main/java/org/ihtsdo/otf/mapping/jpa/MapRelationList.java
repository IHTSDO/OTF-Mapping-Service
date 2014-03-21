package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Container for map relations.
 */
@XmlRootElement(name = "mapRelationList")
public class MapRelationList {

	/** The map relations. */
	private List<MapRelation> mapRelations = new ArrayList<MapRelation>();

	/**
	 * Instantiates a new map relation list.
	 */
	public MapRelationList() {
		// do nothing
	}

	/**
	 * Adds the map relation.
	 * 
	 * @param mapRelation
	 *            the map relation
	 */
	public void addMapRelation(MapRelation mapRelation) {
		mapRelations.add(mapRelation);
	}

	/**
	 * Removes the map relation.
	 * 
	 * @param mapRelation
	 *            the map relation
	 */
	public void removeMapRelation(MapRelation mapRelation) {
		mapRelations.remove(mapRelation);
	}

	/**
	 * Sets the map relations.
	 * 
	 * @param mapRelations
	 *            the new map relations
	 */
	public void setMapRelations(List<MapRelation> mapRelations) {
		this.mapRelations = new ArrayList<MapRelation>();
		this.mapRelations.addAll(mapRelations);
		
		
	}
	
	/**
	 * Sorts the map relations alphabetically by name
	 */
	public void sortMapRelations() {
	
		Collections.sort(this.mapRelations,
			new Comparator<MapRelation>() {
				@Override
				public int compare(MapRelation o1, MapRelation o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
}

	/**
	 * Gets the map relations.
	 * 
	 * @return the map relations
	 */
	@XmlElement(type=MapRelationJpa.class, name="mapRelation")
	public List<MapRelation> getMapRelations() {
		return mapRelations;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapRelations.size();
	}

}
