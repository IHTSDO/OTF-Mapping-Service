package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * The interface for the content service
 *
 */
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
	 * @throws Exception if anything goes wrong
	 */
	public List<Concept> getConcepts(String searchString) throws Exception;
}
