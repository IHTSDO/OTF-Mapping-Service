package org.ihtsdo.otf.mapping.jpa.services.rest;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.rf2.Concept;

public interface ContentServiceRest {

	/**
	   * Returns the concept for id, terminology, and terminology version.
	   *
	   * @param terminologyId the terminology id
	   * @param terminology the concept terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the concept
	   * @throws Exception the exception
	   */
	Concept getConcept(String terminologyId, String terminology, String terminologyVersion, String authToken)
			throws Exception;

	/**
	   * Returns the concept for id, terminology. Looks in the latest version of the
	   * terminology.
	   *
	   * @param terminologyId the id
	   * @param terminology the concept terminology
	   * @param authToken the auth token
	   * @return the concept
	   * @throws Exception the exception
	   */
	Concept getConcept(String terminologyId, String terminology, String authToken) throws Exception;

	/**
	   * Returns the concept for search string.
	   *
	   * @param query the lucene search string
	   * @param authToken the auth token
	   * @return the concept for id
	   * @throws Exception the exception
	   */
	SearchResultList findConceptsForQuery(String query, String authToken) throws Exception;

	/**
	   * Returns the descendants of a concept as mapped by relationships and inverse
	   * relationships.
	   *
	   * @param terminologyId the terminology id
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findDescendantConcepts(String terminologyId, String terminology, String terminologyVersion,
			String authToken) throws Exception;

	/**
	   * Returns the immediate children of a concept given terminology information.
	   *
	   * @param id the terminology id
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findChildConcepts(String id, String terminology, String terminologyVersion, String authToken)
			throws Exception;

	/**
	   * Find delta concepts for terminology.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @param pfsParameter the pfs parameter
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findDeltaConceptsForTerminology(String terminology, String terminologyVersion, String authToken,
			PfsParameterJpa pfsParameter) throws Exception;

	/**
	   * Returns the index viewer indexes.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the index viewer indexes
	   * @throws Exception the exception
	   */
	SearchResultList getIndexDomains(String terminology, String terminologyVersion, String authToken) throws Exception;

	/**
	   * Returns the index viewer pages for index.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param index the index
	   * @param authToken the auth token
	   * @return the index viewer pages for index
	   * @throws Exception the exception
	   */
	SearchResultList getIndexViewerPagesForIndex(String terminology, String terminologyVersion, String index,
			String authToken) throws Exception;

	/**
	   * Returns the index viewer details for link.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param domain the domain
	   * @param link the link
	   * @param authToken the auth token
	   * @return the index viewer details for link
	   * @throws Exception the exception
	   */
	String getIndexViewerDetailsForLink(String terminology, String terminologyVersion, String domain, String link,
			String authToken) throws Exception;

	/**
	   * Find index viewer search result entries.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param domain the domain
	   * @param searchField the search field
	   * @param subSearchField the sub search field
	   * @param subSubSearchField the sub sub search field
	   * @param allFlag the all flag
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findIndexViewerEntries(String terminology, String terminologyVersion, String domain,
			String searchField, String subSearchField, String subSubSearchField, boolean allFlag, String authToken)
			throws Exception;

}