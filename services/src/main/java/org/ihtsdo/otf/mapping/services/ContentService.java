package org.ihtsdo.otf.mapping.services;

import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * The interface for the content service.
 * 
 * @author ${author}
 */
public interface ContentService extends RootService {

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
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the search results for the search string
	 * @throws Exception if anything goes wrong
	 */
	public SearchResultList findAllConcepts(String terminology, String terminologyVersion)
		throws Exception;

	/**
	 * Gets the descendants of a concept.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param typeId the type id
	 * @return the set of concepts
	 * @throws Exception the exception
	 */
	public SearchResultList findDescendants(String terminologyId, String terminology,
			String terminologyVersion, String typeId) throws Exception;

	/**
	 * Returns the descendants.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param typeId the type id
	 * @return the descendants
	 * @throws Exception the exception
	 */
	public Set<Concept> getDescendants(String terminologyId, String terminology,
			String terminologyVersion, String typeId) throws Exception;

	/**
	 * Find children.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param typeId the type id
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findChildren(String terminologyId, String terminology,
			String terminologyVersion, Long typeId) throws Exception;
	
	/**
	 * Returns the tree positions for concept.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the tree positions for concept
	 * @throws Exception the exception
	 */
	public SearchResultList findTreePositionsForConcept(String terminologyId, String terminology,
		String terminologyVersion) throws Exception;
	

	/**
	 * Clear tree positions.
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @throws Exception the exception
	 */
	public void clearTreePositions(String terminology, String terminologyVersion) throws Exception;
	
	/**
	 * Compute tree positions.
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param typeId the type id
	 * @param rootId the root id
	 * @throws Exception the exception
	 */
	public void computeTreePositions(String terminology, String terminologyVersion, String typeId, String rootId) throws Exception; 

	/**
	 * Gets the transaction per operation.
	 *
	 * @return the transaction per operation
	 * @throws Exception the exception
	 */
	public boolean getTransactionPerOperation() throws Exception;
	

	/**
	 * Sets the transaction per operation.
	 *
	 * @param transactionPerOperation the transaction per operation
	 * @throws Exception the exception
	 */
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;
	
	/**
	 * Find descendants from tree postions.
	 *
	 * @param conceptId the concept id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the search result list
	 * @throws Exception the exception
	 */
	public SearchResultList findDescendantsFromTreePostions(String conceptId,
		String terminology, String terminologyVersion ) throws Exception;



	/**
	 * Gets the local trees.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the local trees
	 */
	public TreePositionList getLocalTrees(String terminologyId, String terminology,
			String terminologyVersion);

	/**
	 * Gets the tree positions for concept.
	 *
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the tree positions for concept
	 */
	public TreePositionList getTreePositionsForConcept(String terminologyId,
			String terminology, String terminologyVersion);

	/**
	 * Gets the tree position children.
	 *
	 * @param treePosition the tree position
	 * @return the tree position children
	 */
	public TreePositionList getTreePositionChildren(TreePosition treePosition);

	/**
	 * Gets the root tree positions for a given terminology
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the root tree positions for terminology
	 */
	public TreePositionList getRootTreePositionsForTerminology(String terminology,
			String terminologyVersion);

	/**
	 * Returns the tree positions for concept query.
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param query the query
	 * @return the tree positions for concept query
	 * @throws Exception the exception
	 */
	public TreePositionList getTreePositionsForConceptQuery(
			String terminology, String terminologyVersion, String query) throws Exception;

	/**
	 * Returns the concept tree roots.
	 *
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the concept tree roots
	 * @throws Exception the exception
	 */
	public ConceptList getConceptTreeRoots(String terminology, String terminologyVersion) throws Exception;
}
