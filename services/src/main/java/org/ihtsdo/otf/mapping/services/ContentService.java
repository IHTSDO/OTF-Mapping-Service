package org.ihtsdo.otf.mapping.services;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.DescriptionList;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.RelationshipList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Generically represents a service for accessing terminology content.
 */
public interface ContentService extends RootService {

  /**
   * Closes the manager associated with service.y
   * 
   * @throws Exception the exception
   */
  @Override
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
   * Adds the concept.
   * 
   * @param concept the concept
   * @return the concept
   * @throws Exception the exception
   */
  public Concept addConcept(Concept concept) throws Exception;

  /**
   * Update concept.
   * 
   * @param concept the concept
   * @throws Exception the exception
   */
  public void updateConcept(Concept concept) throws Exception;

  /**
   * Removes the concept.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeConcept(Long id) throws Exception;

  /**
   * Returns the description.
   * 
   * @param id the id
   * @return the description
   * @throws Exception if anything goes wrong
   */
  public Description getDescription(Long id) throws Exception;

  /**
   * Returns the description matching the specified parameters.
   * 
   * @param terminologyId the description id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the description
   * @throws Exception if anything goes wrong
   */
  public Description getDescription(String terminologyId, String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Adds the description.
   * 
   * @param description the description
   * @return the description
   * @throws Exception the exception
   */
  public Description addDescription(Description description) throws Exception;

  /**
   * Update description.
   * 
   * @param description the description
   * @throws Exception the exception
   */
  public void updateDescription(Description description) throws Exception;

  /**
   * Removes the description.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeDescription(Long id) throws Exception;

  /**
   * Returns the relationship.
   * 
   * @param relationshipId the relationship id
   * @return the relationship
   * @throws Exception if anything goes wrong
   */
  public Relationship getRelationship(Long relationshipId) throws Exception;

  /**
   * Returns the relationship matching the specified parameters.
   * 
   * @param terminologyId the relationship id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the relationship
   * @throws Exception if anything goes wrong
   */
  public Relationship getRelationship(String terminologyId, String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Adds the relationship.
   * 
   * @param relationship the relationship
   * @return the relationship
   * @throws Exception the exception
   */
  public Relationship addRelationship(Relationship relationship)
    throws Exception;

  /**
   * Update relationship.
   * 
   * @param relationship the relationship
   * @throws Exception the exception
   */
  public void updateRelationship(Relationship relationship) throws Exception;

  /**
   * Removes the relationship.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeRelationship(Long id) throws Exception;

  /**
   * Returns the languageRefSetMember.
   * 
   * @param languageRefSetMemberId the languageRefSetMember id
   * @return the languageRefSetMember
   * @throws Exception if anything goes wrong
   */
  public LanguageRefSetMember getLanguageRefSetMember(
    Long languageRefSetMemberId) throws Exception;

  /**
   * Returns the languageRefSetMember matching the specified parameters.
   * 
   * @param terminologyId the languageRefSetMember id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the languageRefSetMember
   * @throws Exception if anything goes wrong
   */
  public LanguageRefSetMember getLanguageRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Adds the languageRefSetMember.
   * 
   * @param languageRefSetMember the languageRefSetMember
   * @return the languageRefSetMember
   * @throws Exception the exception
   */
  public LanguageRefSetMember addLanguageRefSetMember(
    LanguageRefSetMember languageRefSetMember) throws Exception;

  /**
   * Update languageRefSetMember.
   * 
   * @param languageRefSetMember the languageRefSetMember
   * @throws Exception the exception
   */
  public void updateLanguageRefSetMember(
    LanguageRefSetMember languageRefSetMember) throws Exception;

  /**
   * Removes the languageRefSetMember.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeLanguageRefSetMember(Long id) throws Exception;

  /**
   * Returns the attributeValueRefSetMember.
   * 
   * @param attributeValueRefSetMemberId the attributeValueRefSetMember id
   * @return the attributeValueRefSetMember
   * @throws Exception if anything goes wrong
   */
  public AttributeValueRefSetMember getAttributeValueRefSetMember(
    Long attributeValueRefSetMemberId) throws Exception;

  /**
   * Returns the attributeValueRefSetMember matching the specified parameters.
   * 
   * @param terminologyId the attributeValueRefSetMember id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the attributeValueRefSetMember
   * @throws Exception if anything goes wrong
   */
  public AttributeValueRefSetMember getAttributeValueRefSetMember(
    String terminologyId, String terminology, String terminologyVersion)
    throws Exception;

  /**
   * Adds the attributeValueRefSetMember.
   * 
   * @param attributeValueRefSetMember the attributeValueRefSetMember
   * @return the attributeValueRefSetMember
   * @throws Exception the exception
   */
  public AttributeValueRefSetMember addAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) throws Exception;

  /**
   * Update attributeValueRefSetMember.
   * 
   * @param attributeValueRefSetMember the attributeValueRefSetMember
   * @throws Exception the exception
   */
  public void updateAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) throws Exception;

  /**
   * Removes the attributeValueRefSetMember.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeAttributeValueRefSetMember(Long id) throws Exception;

  /**
   * Returns the complexMapRefSetMember.
   * 
   * @param complexMapRefSetMemberId the complexMapRefSetMember id
   * @return the complexMapRefSetMember
   * @throws Exception if anything goes wrong
   */
  public ComplexMapRefSetMember getComplexMapRefSetMember(
    Long complexMapRefSetMemberId) throws Exception;

  /**
   * Returns the complexMapRefSetMember matching the specified parameters.
   * 
   * @param terminologyId the complexMapRefSetMember id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the complexMapRefSetMember
   * @throws Exception if anything goes wrong
   */
  public ComplexMapRefSetMember getComplexMapRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Adds the complexMapRefSetMember.
   * 
   * @param complexMapRefSetMember the complexMapRefSetMember
   * @return the complexMapRefSetMember
   * @throws Exception the exception
   */
  public ComplexMapRefSetMember addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) throws Exception;

  /**
   * Update complexMapRefSetMember.
   * 
   * @param complexMapRefSetMember the complexMapRefSetMember
   * @throws Exception the exception
   */
  public void updateComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) throws Exception;

  /**
   * Removes the complexMapRefSetMember.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeComplexMapRefSetMember(Long id) throws Exception;

  /**
   * Returns the simpleMapRefSetMember.
   * 
   * @param simpleMapRefSetMemberId the simpleMapRefSetMember id
   * @return the simpleMapRefSetMember
   * @throws Exception if anything goes wrong
   */
  public SimpleMapRefSetMember getSimpleMapRefSetMember(
    Long simpleMapRefSetMemberId) throws Exception;

  /**
   * Returns the simpleMapRefSetMember matching the specified parameters.
   * 
   * @param terminologyId the simpleMapRefSetMember id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the simpleMapRefSetMember
   * @throws Exception if anything goes wrong
   */
  public SimpleMapRefSetMember getSimpleMapRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Adds the simpleMapRefSetMember.
   * 
   * @param simpleMapRefSetMember the simpleMapRefSetMember
   * @return the simpleMapRefSetMember
   * @throws Exception the exception
   */
  public SimpleMapRefSetMember addSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) throws Exception;

  /**
   * Update simpleMapRefSetMember.
   * 
   * @param simpleMapRefSetMember the simpleMapRefSetMember
   * @throws Exception the exception
   */
  public void updateSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) throws Exception;

  /**
   * Removes the simpleMapRefSetMember.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeSimpleMapRefSetMember(Long id) throws Exception;

  /**
   * Returns the simpleRefSetMember.
   * 
   * @param simpleRefSetMemberId the simpleRefSetMember id
   * @return the simpleRefSetMember
   * @throws Exception if anything goes wrong
   */
  public SimpleRefSetMember getSimpleRefSetMember(Long simpleRefSetMemberId)
    throws Exception;

  /**
   * Returns the simpleRefSetMember matching the specified parameters.
   * 
   * @param terminologyId the simpleRefSetMember id
   * @param terminology the terminology
   * @param terminologyVersion the terminologyVersion
   * @return the simpleRefSetMember
   * @throws Exception if anything goes wrong
   */
  public SimpleRefSetMember getSimpleRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Adds the simpleRefSetMember.
   * 
   * @param simpleRefSetMember the simpleRefSetMember
   * @return the simpleRefSetMember
   * @throws Exception the exception
   */
  public SimpleRefSetMember addSimpleRefSetMember(
    SimpleRefSetMember simpleRefSetMember) throws Exception;

  /**
   * Update simpleRefSetMember.
   * 
   * @param simpleRefSetMember the simpleRefSetMember
   * @throws Exception the exception
   */
  public void updateSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember)
    throws Exception;

  /**
   * Removes the simpleRefSetMember.
   * 
   * @param id the id
   * @throws Exception the exception
   */
  public void removeSimpleRefSetMember(Long id) throws Exception;

  /**
   * Returns the concept search results matching the query. Results can be
   * paged, filtered, and sorted.
   * 
   * @param query the search string
   * @param pfsParameter the paging, filtering, sorting parameter
   * @return the search results for the search string
   * @throws Exception if anything goes wrong
   */
  public SearchResultList findConceptsForQuery(String query,
    PfsParameter pfsParameter) throws Exception;

  /**
   * Finds the descendants of a concept, subject to max results limitation in
   * PFS parameters object.
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
    String terminology, String terminologyVersion, PfsParameter pfsParameter)
    throws Exception;

  /**
   * Returns the descendant concepts count.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the descendant concepts count
   * @throws Exception the exception
   */
  public int getDescendantConceptsCount(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

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
   * @return the validation result containing any errors/warnings/messages
   * @throws Exception the exception
   */
  public ValidationResult computeTreePositions(String terminology,
    String terminologyVersion, String typeId, String rootId) throws Exception;

  /**
   * Cycle check.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param typeId the type id
   * @param rootId the root id
   * @throws Exception the exception
   */
  public void cycleCheck(String terminology, String terminologyVersion,
    String typeId, String rootId) throws Exception;

  /**
   * Gets the transaction per operation.
   * 
   * @return the transaction per operation
   * @throws Exception the exception
   */
  @Override
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   * 
   * @param transactionPerOperation the transaction per operation
   * @throws Exception the exception
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Gets the tree positions with all descendants fully rendered.
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the local trees
   * @throws Exception the exception
   */
  public TreePositionList getTreePositionsWithDescendants(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Returns the any tree position with descendants.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the any tree position with descendants
   * @throws Exception the exception
   */
  public TreePosition getAnyTreePositionWithDescendants(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Gets the root tree positions for a given terminology.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the root tree positions for terminology
   * @throws Exception the exception
   */
  public TreePositionList getRootTreePositions(String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Returns the tree positions for concept query. This returns a full tree
   * position graph all the way up to the root node.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param query the query
   * @param pfs the pfs
   * @return the tree positions for concept query
   * @throws Exception the exception
   */
  public TreePositionList getTreePositionGraphForQuery(String terminology,
    String terminologyVersion, String query, PfsParameter pfs) throws Exception;

  /**
   * Gets the tree positions.
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the tree positions with children
   * @throws Exception the exception
   */
  public TreePositionList getTreePositions(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Is descendant of.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param version the version
   * @param ancestorId the ancestor id
   * @return the tree position list
   * @throws Exception the exception
   */
  public boolean isDescendantOf(String terminologyId, String terminology,
    String version, String ancestorId) throws Exception;

  /**
   * Indicates whether or not descendant of is the case.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param version the version
   * @param ancestorIds the ancestor ids
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDescendantOf(String terminologyId, String terminology,
    String version, List<String> ancestorIds) throws Exception;

  /**
   * Find concepts modified since date.
   *
   * @param terminology the terminology
   * @param date the date
   * @param pfsParameter the pfs parameter
   * @return the search result list
   * @throws Exception the exception
   */
  public ConceptList getConceptsModifiedSinceDate(String terminology,
    Date date, PfsParameter pfsParameter) throws Exception;

  /**
   * Find descriptions modified since date.
   * 
   * @param terminology the terminology
   * @param date the date
   * @return the search result list
   */
  public DescriptionList getDescriptionsModifiedSinceDate(String terminology,
    Date date);

  /**
   * Find relationships modified since date.
   * 
   * @param terminology the terminology
   * @param date the date
   * @return the search result list
   */
  public RelationshipList getRelationshipsModifiedSinceDate(String terminology,
    Date date);

  /**
   * Find language ref set members modified since date.
   * 
   * @param terminology the terminology
   * @param date the date
   * @return the search result list
   */
  public LanguageRefSetMemberList getLanguageRefSetMembersModifiedSinceDate(
    String terminology, Date date);

  /**
   * Compute tree position concept information.
   *
   * @param tpList the tp list
   * @param descTypes the desc types
   * @param relTypes the rel types
   * @throws Exception the exception
   */
  public void computeTreePositionInformation(TreePositionList tpList,
    Map<String, String> descTypes, Map<String, String> relTypes)
    throws Exception;

  /**
   * Gets the relationship id.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the relationship id
   * @throws Exception the exception
   */
  public Long getRelationshipId(String terminologyId, String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Gets the all concepts.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the all concepts
   */
  public ConceptList getAllConcepts(String terminology,
    String terminologyVersion);

  /**
   * Gets all relationships.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the relationships
   * @throws Exception the exception
   */
  public RelationshipList getAllActiveRelationships(String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Gets all descriptions.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the descriptions
   * @throws Exception the exception
   */
  public DescriptionList getAllActiveDescriptions(String terminology,
    String terminologyVersion) throws Exception;

  /**
   * Gets all concepts.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the concepts
   * @throws Exception the exception
   */
  public LanguageRefSetMemberList getAllActiveLanguageRefSetMembers(
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Gets the all relationship terminology ids.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the all relationship terminology ids
   */
  public Set<String> getAllRelationshipTerminologyIds(String terminology,
    String terminologyVersion);

  /**
   * Gets the all description terminology ids.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the all description terminology ids
   */
  public Set<String> getAllDescriptionTerminologyIds(String terminology,
    String terminologyVersion);

  /**
   * Gets the all language ref set member terminology ids.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the all language ref set member terminology ids
   */
  public Set<String> getAllLanguageRefSetMemberTerminologyIds(
    String terminology, String terminologyVersion);

  /**
   * Removes the tree position.
   *
   * @param id the id
   * @throws Exception the exception
   */
  public void removeTreePosition(Long id) throws Exception;

  /**
   * Gets all concepts.
   *
   * @return the concepts
   * @throws Exception the exception
   */
  public ConceptList getConcepts() throws Exception;

  /**
   * Gets the tree position with descendants.
   *
   * @param tp the tp
   * @return the tree position with descendants
   * @throws Exception the exception
   */
  public TreePosition getTreePositionWithDescendants(TreePosition tp)
    throws Exception;

  /**
   * Returns the tree positions with children.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the tree positions with children
   * @throws Exception the exception
   */
  public TreePositionList getTreePositionsWithChildren(String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

  /**
   * Returns all complex map ref set members for a given refset id.
   *
   * @param refSetId the ref set id
   * @return the complex map ref set members for ref set id
   * @throws Exception the exception
   */
  public ComplexMapRefSetMemberList getComplexMapRefSetMembersForRefSetId(
    String refSetId) throws Exception;

  /**
   * Indicates whether or not concept is descendant along a hierarchical path
   *
   * @param ancestorPath the ancestor path
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public boolean isDescendantOfPath(String ancestorPath, String terminologyId,
    String terminology, String terminologyVersion) throws Exception;

}
