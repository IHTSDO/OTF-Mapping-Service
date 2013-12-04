package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.model.Concept;

public interface ContentService {
	
	/**
	 * Returns the concept.
	 *
	 * @param conceptId the concept id
	 * @return the concept
	 */
	public Concept getConcept(Long conceptId);
	
	/**
	 * Returns the concept.
	 *
	 * @param searchString the search string
	 * @return the concept
	 */
	public List<String> getConcepts(String searchString);
}
