/**
 * 
 */
package org.ihtsdo.otf.mapping.rest.client;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.rest.ContentServiceRest;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * @author Nuno Marques
 *
 */
public class ContentClientRest extends RootClientRest implements ContentServiceRest {

	@Override
	public Concept getConcept(String terminologyId, String terminology, String terminologyVersion, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Concept getConcept(String terminologyId, String terminology, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findConceptsForQuery(String query, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findDescendantConcepts(String terminologyId, String terminology, String terminologyVersion,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findChildConcepts(String id, String terminology, String terminologyVersion,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findDeltaConceptsForTerminology(String terminology, String terminologyVersion,
			String authToken, PfsParameterJpa pfsParameter) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList getIndexDomains(String terminology, String terminologyVersion, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList getIndexViewerPagesForIndex(String terminology, String terminologyVersion, String index,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIndexViewerDetailsForLink(String terminology, String terminologyVersion, String domain,
			String link, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList findIndexViewerEntries(String terminology, String terminologyVersion, String domain,
			String searchField, String subSearchField, String subSubSearchField, boolean allFlag, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
