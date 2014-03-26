package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.rf2.Relationship;

/**
 * Container for map Relationships.
 */
@XmlRootElement(name = "relationshipList")
public class RelationshipList {

	/** The map relationships. */
	private List<Relationship> relationships = new ArrayList<>();

	/**
	 * Instantiates a new map relationship list.
	 */
	public RelationshipList() {
		// do nothing
	}

	/**
	 * Adds the map relationship.
	 * @param relationship 
	 */
	public void addRelationship(Relationship relationship) {
		relationships.add(relationship);
	}

	/**
	 * Removes the map relationship.
	 * @param relationship 
	 */
	public void removeRelationship(Relationship relationship) {
		relationships.remove(relationship);
	}

	/**
	 * Sets the map Relationships given a List of relationships
	 * @param relationships 
	 */
	public void setRelationships(List<Relationship> relationships) {
		this.relationships = new ArrayList<>();
		if (relationships != null) {
			this.relationships.addAll(relationships);	
		}	
	}
	
	/**
	 * Sets the map Relationships given a Set of relationships
	 * @param relationships the set of relationships
	 */
	public void setRelationships(Set<Relationship> relationships) {
		Iterator<Relationship> iter = relationships.iterator();
		while(iter.hasNext()) {
			this.relationships.add(iter.next());
		}
	}
	
	/**
	 * Sorts the map Relationships alphabetically by name
	 */
	public void sortRelationships() {
	
		Collections.sort(this.relationships,
			new Comparator<Relationship>() {
				@Override
				public int compare(Relationship o1, Relationship o2) {
					return o1.getTerminologyId().compareTo(o2.getTerminologyId());
				}

			});
}

	/**
	 * Gets the map Relationships.
	 * 
	 * @return the map Relationships
	 */
	@XmlElement(type=RelationshipJpa.class, name="relationship")
	public List<Relationship> getRelationships() {
		return relationships;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return relationships.size();
	}

}
