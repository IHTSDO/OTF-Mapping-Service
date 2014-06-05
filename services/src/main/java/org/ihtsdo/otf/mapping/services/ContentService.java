package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * The interface for the content service.
 * 
 * @author ${author}
 */
public interface ContentService extends RootService {

  /**
   * Closes the manager associated with service.y
   * 
   * @throws Exception the exception
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
  public SearchResultList findConceptsByQuery(String query,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Finds the descendants of a concept, subject to max results limitation in
   * PFS parameters object
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param pfsParameter the pfs parameter containing the max results
   *          restriction
   * @return the set of concepts
   * @throws Exception the exception
   */
  public SearchResultList findDescendantConcepts(String terminologyId,
    String terminology, String terminologyVersion,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Clear tree positions.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @throws Exception the exception
   */
  public void clearTreePositions(String terminology, String terminologyVersion)
    throws Exception;

  /**
   * Compute tree positions.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param typeId the type id
   * @param rootId the root id
   * @throws Exception the exception
   */
  public void computeTreePositions(String terminology,
    String terminologyVersion, String typeId, String rootId) throws Exception;

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
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Gets the tree positions with all descendants fully rendered
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the local trees
   * @throws Exception 
   */
  public TreePositionList getTreePositionsWithDescendants(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Gets the root tree positions for a given terminology.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the root tree positions for terminology
   * @throws Exception 
   */
  public TreePositionList getRootTreePositions(
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Returns the tree positions for concept query. This returns a full tree
   * position graph all the way up to the root node.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param query the query
   * @return the tree positions for concept query
   * @throws Exception the exception
   */
  public TreePositionList getTreePositionGraphByQuery(String terminology,
    String terminologyVersion, String query) throws Exception;

  /**
   * Gets the tree positions
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the tree positions with children
   * @throws Exception the exception
   */
  public TreePositionList getTreePositions(String terminologyId,
		String terminology, String terminologyVersion) throws Exception;

}
