package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.model.Concept;

public interface ContentService {
	
	/**
	 * Returns the concept.
	 *
	 * @param conceptId the concept id
	 * @return the concept
	 */
	public Concept getConcept(Long conceptId);
}
