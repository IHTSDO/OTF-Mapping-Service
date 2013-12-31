package org.ihtsdo.otf.mapping.services;

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
	 * @param conceptId the concept id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminologyVersion
	 * @return the concept
	 */
	public Concept getConcept(Long conceptId, String terminology, String terminologyVersion);
	
	/**
	 * Returns the concept.
	 *
	 * @param searchString the search string
	 * @return the search results for the search string
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findConcepts(String searchString) throws Exception;
}
