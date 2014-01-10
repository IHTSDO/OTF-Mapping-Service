package org.ihtsdo.otf.mapping.services;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.rf2.Concept;

// TODO: Auto-generated Javadoc
/**
 * The interface for the content service.
 *
 * @author ${author}
 */
public interface ContentService {
	
	/**
	 * Closes the manager associated with service
	 * @exception Exception the exception
	 */
	public void close() throws Exception;
	
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
	public Concept getConcept(String terminologyId, String terminology, String terminologyVersion);
	
	/**
	 * Returns the concept.
	 *
	 * @param searchString the search string
	 * @return the search results for the search string
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findConcepts(String searchString, PfsParameter pfsParameter) throws Exception;
	
	/**
	 * Gets the descendants of a concept
	 * @param terminologyId
	 * @param terminology
	 * @param terminologyVersion
	 * @param typeId
	 * @return the set of concepts
	 */
	public Set<Concept> getDescendants(String terminologyId, String terminology, String terminologyVersion, Long typeId);
	
}
