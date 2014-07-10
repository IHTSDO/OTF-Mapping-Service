package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;

/**
 * The interface for the content service.
 * 
 * @author ${author}
 */
public interface ContentService extends RootService {

	/**
	 * Closes the manager associated with service.y
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void close() throws Exception;

	/**
	 * Returns the concept.
	 * 
	 * @param conceptId
	 *            the concept id
	 * @return the concept
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Concept getConcept(Long conceptId) throws Exception;

	/**
	 * Returns the concept matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the concept id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the concept
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Concept getConcept(String terminologyId, String terminology,
			String terminologyVersion) throws Exception;

	/**
	 * Adds the concept.
	 * 
	 * @param concept
	 *            the concept
	 * @return the concept
	 * @throws Exception
	 *             the exception
	 */
	public Concept addConcept(Concept concept) throws Exception;

	/**
	 * Update concept.
	 * 
	 * @param concept
	 *            the concept
	 * @throws Exception
	 *             the exception
	 */
	public void updateConcept(Concept concept) throws Exception;

	/**
	 * Removes the concept.
	 * 
	 * @param concept
	 *            the concept
	 * @throws Exception
	 *             the exception
	 */
	public void removeConcept(Concept concept) throws Exception;

	/**
	 * Returns the description.
	 * 
	 * @param descriptionId
	 *            the description id
	 * @return the description
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Description getDescription(Long descriptionId) throws Exception;

	/**
	 * Returns the description matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the description id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the description
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Description getDescription(String terminologyId, String terminology,
			String terminologyVersion) throws Exception;

	/**
	 * Adds the description.
	 * 
	 * @param description
	 *            the description
	 * @return the description
	 * @throws Exception
	 *             the exception
	 */
	public Description addDescription(Description description) throws Exception;

	/**
	 * Update description.
	 * 
	 * @param description
	 *            the description
	 * @throws Exception
	 *             the exception
	 */
	public void updateDescription(Description description) throws Exception;

	/**
	 * Removes the description.
	 * 
	 * @param description
	 *            the description
	 * @throws Exception
	 *             the exception
	 */
	public void removeDescription(Description description) throws Exception;

	/**
	 * Returns the relationship.
	 * 
	 * @param relationshipId
	 *            the relationship id
	 * @return the relationship
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Relationship getRelationship(Long relationshipId) throws Exception;

	/**
	 * Returns the relationship matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the relationship id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the relationship
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public Relationship getRelationship(String terminologyId,
			String terminology, String terminologyVersion) throws Exception;

	/**
	 * Adds the relationship.
	 * 
	 * @param relationship
	 *            the relationship
	 * @return the relationship
	 * @throws Exception
	 *             the exception
	 */
	public Relationship addRelationship(Relationship relationship)
			throws Exception;

	/**
	 * Update relationship.
	 * 
	 * @param relationship
	 *            the relationship
	 * @throws Exception
	 *             the exception
	 */
	public void updateRelationship(Relationship relationship) throws Exception;

	/**
	 * Removes the relationship.
	 * 
	 * @param relationship
	 *            the relationship
	 * @throws Exception
	 *             the exception
	 */
	public void removeRelationship(Relationship relationship) throws Exception;

	/**
	 * Returns the languageRefSetMember.
	 * 
	 * @param languageRefSetMemberId
	 *            the languageRefSetMember id
	 * @return the languageRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public LanguageRefSetMember getLanguageRefSetMember(
			Long languageRefSetMemberId) throws Exception;

	/**
	 * Returns the languageRefSetMember matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the languageRefSetMember id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the languageRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public LanguageRefSetMember getLanguageRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception;

	/**
	 * Adds the languageRefSetMember.
	 * 
	 * @param languageRefSetMember
	 *            the languageRefSetMember
	 * @return the languageRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public LanguageRefSetMember addLanguageRefSetMember(
			LanguageRefSetMember languageRefSetMember) throws Exception;

	/**
	 * Update languageRefSetMember.
	 * 
	 * @param languageRefSetMember
	 *            the languageRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void updateLanguageRefSetMember(
			LanguageRefSetMember languageRefSetMember) throws Exception;

	/**
	 * Removes the languageRefSetMember.
	 * 
	 * @param languageRefSetMember
	 *            the languageRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void removeLanguageRefSetMember(
			LanguageRefSetMember languageRefSetMember) throws Exception;

	/**
	 * Returns the attributeValueRefSetMember.
	 * 
	 * @param attributeValueRefSetMemberId
	 *            the attributeValueRefSetMember id
	 * @return the attributeValueRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public AttributeValueRefSetMember getAttributeValueRefSetMember(
			Long attributeValueRefSetMemberId) throws Exception;

	/**
	 * Returns the attributeValueRefSetMember matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the attributeValueRefSetMember id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the attributeValueRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public AttributeValueRefSetMember getAttributeValueRefSetMember(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception;

	/**
	 * Adds the attributeValueRefSetMember.
	 * 
	 * @param attributeValueRefSetMember
	 *            the attributeValueRefSetMember
	 * @return the attributeValueRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public AttributeValueRefSetMember addAttributeValueRefSetMember(
			AttributeValueRefSetMember attributeValueRefSetMember)
			throws Exception;

	/**
	 * Update attributeValueRefSetMember.
	 * 
	 * @param attributeValueRefSetMember
	 *            the attributeValueRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void updateAttributeValueRefSetMember(
			AttributeValueRefSetMember attributeValueRefSetMember)
			throws Exception;

	/**
	 * Removes the attributeValueRefSetMember.
	 * 
	 * @param attributeValueRefSetMember
	 *            the attributeValueRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void removeAttributeValueRefSetMember(
			AttributeValueRefSetMember attributeValueRefSetMember)
			throws Exception;

	/**
	 * Returns the complexMapRefSetMember.
	 * 
	 * @param complexMapRefSetMemberId
	 *            the complexMapRefSetMember id
	 * @return the complexMapRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public ComplexMapRefSetMember getComplexMapRefSetMember(
			Long complexMapRefSetMemberId) throws Exception;

	/**
	 * Returns the complexMapRefSetMember matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the complexMapRefSetMember id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the complexMapRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public ComplexMapRefSetMember getComplexMapRefSetMember(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception;

	/**
	 * Adds the complexMapRefSetMember.
	 * 
	 * @param complexMapRefSetMember
	 *            the complexMapRefSetMember
	 * @return the complexMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public ComplexMapRefSetMember addComplexMapRefSetMember(
			ComplexMapRefSetMember complexMapRefSetMember) throws Exception;

	/**
	 * Update complexMapRefSetMember.
	 * 
	 * @param complexMapRefSetMember
	 *            the complexMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void updateComplexMapRefSetMember(
			ComplexMapRefSetMember complexMapRefSetMember) throws Exception;

	/**
	 * Removes the complexMapRefSetMember.
	 * 
	 * @param complexMapRefSetMember
	 *            the complexMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void removeComplexMapRefSetMember(
			ComplexMapRefSetMember complexMapRefSetMember) throws Exception;

	/**
	 * Returns the simpleMapRefSetMember.
	 * 
	 * @param simpleMapRefSetMemberId
	 *            the simpleMapRefSetMember id
	 * @return the simpleMapRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public SimpleMapRefSetMember getSimpleMapRefSetMember(
			Long simpleMapRefSetMemberId) throws Exception;

	/**
	 * Returns the simpleMapRefSetMember matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the simpleMapRefSetMember id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the simpleMapRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public SimpleMapRefSetMember getSimpleMapRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception;

	/**
	 * Adds the simpleMapRefSetMember.
	 * 
	 * @param simpleMapRefSetMember
	 *            the simpleMapRefSetMember
	 * @return the simpleMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public SimpleMapRefSetMember addSimpleMapRefSetMember(
			SimpleMapRefSetMember simpleMapRefSetMember) throws Exception;

	/**
	 * Update simpleMapRefSetMember.
	 * 
	 * @param simpleMapRefSetMember
	 *            the simpleMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void updateSimpleMapRefSetMember(
			SimpleMapRefSetMember simpleMapRefSetMember) throws Exception;

	/**
	 * Removes the simpleMapRefSetMember.
	 * 
	 * @param simpleMapRefSetMember
	 *            the simpleMapRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void removeSimpleMapRefSetMember(
			SimpleMapRefSetMember simpleMapRefSetMember) throws Exception;

	/**
	 * Returns the simpleRefSetMember.
	 * 
	 * @param simpleRefSetMemberId
	 *            the simpleRefSetMember id
	 * @return the simpleRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public SimpleRefSetMember getSimpleRefSetMember(Long simpleRefSetMemberId)
			throws Exception;

	/**
	 * Returns the simpleRefSetMember matching the specified parameters.
	 * 
	 * @param terminologyId
	 *            the simpleRefSetMember id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminologyVersion
	 * @return the simpleRefSetMember
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public SimpleRefSetMember getSimpleRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception;

	/**
	 * Adds the simpleRefSetMember.
	 * 
	 * @param simpleRefSetMember
	 *            the simpleRefSetMember
	 * @return the simpleRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public SimpleRefSetMember addSimpleRefSetMember(
			SimpleRefSetMember simpleRefSetMember) throws Exception;

	/**
	 * Update simpleRefSetMember.
	 * 
	 * @param simpleRefSetMember
	 *            the simpleRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void updateSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember)
			throws Exception;

	/**
	 * Removes the simpleRefSetMember.
	 * 
	 * @param simpleRefSetMember
	 *            the simpleRefSetMember
	 * @throws Exception
	 *             the exception
	 */
	public void removeSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember)
			throws Exception;

	/**
	 * Returns the concept search results matching the query. Results can be
	 * paged, filtered, and sorted.
	 * 
	 * @param query
	 *            the search string
	 * @param pfsParameter
	 *            the paging, filtering, sorting parameter
	 * @return the search results for the search string
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public SearchResultList findConceptsForQuery(String query,
			PfsParameter pfsParameter) throws Exception;

	/**
	 * Finds the descendants of a concept, subject to max results limitation in
	 * PFS parameters object
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param pfsParameter
	 *            the pfs parameter containing the max results restriction
	 * @return the set of concepts
	 * @throws Exception
	 *             the exception
	 */
	public SearchResultList findDescendantConcepts(String terminologyId,
			String terminology, String terminologyVersion,
			PfsParameter pfsParameter) throws Exception;

	/**
	 * Clear tree positions.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @throws Exception
	 *             the exception
	 */
	public void clearTreePositions(String terminology, String terminologyVersion)
			throws Exception;

	/**
	 * Compute tree positions.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param typeId
	 *            the type id
	 * @param rootId
	 *            the root id
	 * @throws Exception
	 *             the exception
	 */
	public void computeTreePositions(String terminology,
			String terminologyVersion, String typeId, String rootId)
			throws Exception;

	/**
	 * Gets the transaction per operation.
	 * 
	 * @return the transaction per operation
	 * @throws Exception
	 *             the exception
	 */
	public boolean getTransactionPerOperation() throws Exception;

	/**
	 * Sets the transaction per operation.
	 * 
	 * @param transactionPerOperation
	 *            the transaction per operation
	 * @throws Exception
	 *             the exception
	 */
	public void setTransactionPerOperation(boolean transactionPerOperation)
			throws Exception;

	/**
	 * Gets the tree positions with all descendants fully rendered
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @return the local trees
	 * @throws Exception
	 */
	public TreePositionList getTreePositionsWithDescendants(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception;

	/**
	 * Gets the root tree positions for a given terminology.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @return the root tree positions for terminology
	 * @throws Exception
	 */
	public TreePositionList getRootTreePositions(String terminology,
			String terminologyVersion) throws Exception;

	/**
	 * Returns the tree positions for concept query. This returns a full tree
	 * position graph all the way up to the root node.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param query
	 *            the query
	 * @return the tree positions for concept query
	 * @throws Exception
	 *             the exception
	 */
	public TreePositionList getTreePositionGraphForQuery(String terminology,
			String terminologyVersion, String query) throws Exception;

	/**
	 * Gets the tree positions
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @return the tree positions with children
	 * @throws Exception
	 *             the exception
	 */
	public TreePositionList getTreePositions(String terminologyId,
			String terminology, String terminologyVersion) throws Exception;

}
