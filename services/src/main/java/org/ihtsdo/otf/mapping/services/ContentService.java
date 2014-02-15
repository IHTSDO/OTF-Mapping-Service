package org.ihtsdo.otf.mapping.services;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * The interface for the content service.
 * 
 * @author ${author}
 */
public interface ContentService {

	/**
	 * Closes the manager associated with service.y
	 * 
	 * @exception Exception the exception
	 */
	public void close() throws Exception;

	/**
	 * Returns the concept.
	 * 
	 * @param conceptId the concept id
	 * @return the concept
	 * @throws Exception if anything goes wrong
	 */
	public Concept getConcept(Long conceptId) throws Exception;

	/**
	 * Returns the concept matching the specified parameters.
	 * 
	 * @param terminologyId the concept id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminologyVersion
	 * @return the concept
	 * @throws Exception if anything goes wrong
	 */
	public Concept getConcept(String terminologyId, String terminology,
		String terminologyVersion) throws Exception;

	/**
	 * Returns the concept search results matching the query. Results can be
	 * paged, filtered, and sorted.
	 * 
	 * @param query the search string
	 * @param pfsParameter the paging, filtering, sorting parameter
	 * @return the search results for the search string
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findConcepts(String query, PfsParameter pfsParameter)
		throws Exception;

	/**
	 * Returns {@link SearchResultList} for all concepts of the specified terminology.
	 * @param terminology 
	 * @param terminologyVersion 
	 * 
	 * @return the search results for the search string
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findAllConcepts(String terminology, String terminologyVersion)
		throws Exception;

	/**
	 * Gets the descendants of a concept
	 * @param terminologyId
	 * @param terminology
	 * @param terminologyVersion
	 * @param typeId
	 * @return the set of concepts
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findDescendants(String terminologyId, String terminology,
			String terminologyVersion, Long typeId);

	Set<Concept> getDescendants(String terminologyId, String terminology,
			String terminologyVersion, Long typeId);

	SearchResultList findChildren(String terminologyId, String terminology,
			String terminologyVersion, Long typeId);

}
