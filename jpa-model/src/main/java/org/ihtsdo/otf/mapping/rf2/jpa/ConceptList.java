package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * Container for map concepts.
 */
@XmlRootElement(name = "conceptList")
public class ConceptList {

	/** The map concepts. */
	private List<Concept> Concepts = new ArrayList<Concept>();

	/**
	 * Instantiates a new map concept list.
	 */
	public ConceptList() {
		// do nothing
	}

	/**
	 * Adds the map concept.
	 * 
	 * @param Concept
	 *            the map concept
	 */
	public void addConcept(Concept Concept) {
		Concepts.add(Concept);
	}

	/**
	 * Removes the map concept.
	 * 
	 * @param Concept
	 *            the map concept
	 */
	public void removeConcept(Concept Concept) {
		Concepts.remove(Concept);
	}

	/**
	 * Sets the map concepts.
	 * 
	 * @param Concepts
	 *            the new map concepts
	 */
	public void setConcepts(List<Concept> Concepts) {
		this.Concepts = new ArrayList<Concept>();
		if (Concepts != null) {
			this.Concepts.addAll(Concepts);
		}
		
		
	}
	
	/**
	 * Sorts the map concepts alphabetically by name
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
	 * Gets the map concepts.
	 * 
	 * @return the map concepts
	 */
	@XmlElement(type=ConceptJpa.class, name="concept")
	public List<Concept> getConcepts() {
		return Concepts;
	}

}
