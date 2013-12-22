package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * Container for map projects.
 */
@XmlRootElement(name = "ConceptList")
public class ConceptList {

	/** The map projects. */
	private List<Concept> Concepts = new ArrayList<Concept>();

	/**
	 * Instantiates a new map project list.
	 */
	public ConceptList() {
		// do nothing
	}

	/**
	 * Adds the map project.
	 * 
	 * @param Concept
	 *            the map project
	 */
	public void addConcept(Concept Concept) {
		Concepts.add(Concept);
	}

	/**
	 * Removes the map project.
	 * 
	 * @param Concept
	 *            the map project
	 */
	public void removeConcept(Concept Concept) {
		Concepts.remove(Concept);
	}

	/**
	 * Sets the map projects.
	 * 
	 * @param Concepts
	 *            the new map projects
	 */
	public void setConcepts(List<Concept> Concepts) {
		this.Concepts = new ArrayList<Concept>();
		if (Concepts != null) {
			this.Concepts.addAll(Concepts);
		}
		
		
	}
	
	/**
	 * Sorts the map projects alphabetically by name
	 */
	public void sortConcepts() {
	
		Collections.sort(this.Concepts,
			new Comparator<Concept>() {
				@Override
				public int compare(Concept o1, Concept o2) {
					return o1.getTerminologyId().compareTo(o2.getTerminologyId());
				}

			});
}

	/**
	 * Gets the map projects.
	 * 
	 * @return the map projects
	 */
	@XmlElement(type=ConceptJpa.class)
	public List<Concept> getConcepts() {
		return Concepts;
	}

}
