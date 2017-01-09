package org.ihtsdo.otf.mapping.jpa.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.ComplexMapRefSetMemberListJpa;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.ConceptListJpa;
import org.ihtsdo.otf.mapping.helpers.DescriptionList;
import org.ihtsdo.otf.mapping.helpers.DescriptionListJpa;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.RelationshipList;
import org.ihtsdo.otf.mapping.helpers.RelationshipListJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionDescription;
import org.ihtsdo.otf.mapping.helpers.TreePositionDescriptionGroup;
import org.ihtsdo.otf.mapping.helpers.TreePositionDescriptionGroupJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionDescriptionJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConcept;
import org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConceptJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.IndexUtility;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.AttributeValueRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.LanguageRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * The Content Services for the Jpa model.
 */
public class ContentServiceJpa extends RootServiceJpa implements ContentService {

  /** The compute tree position total count. */
  int computeTreePositionTotalCount;

  /** The compute tree position max memory usage. */
  Long computeTreePositionMaxMemoryUsage;

  /** The compute tree position last time. */
  Long computeTreePositionLastTime;

  /** The compute tree position validation result. */
  ValidationResult computeTreePositionValidationResult;

  /**
   * Instantiates an empty {@link ContentServiceJpa}.
   * 
   * @throws Exception the exception
   */
  public ContentServiceJpa() throws Exception {
    super();

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ConceptList getConcepts() throws Exception {
    List<Concept> m = null;

    final javax.persistence.Query query =
        manager.createQuery("select c from ConceptJpa c ");

    m = query.getResultList();
    final ConceptListJpa ConceptList = new ConceptListJpa();
    ConceptList.setConcepts(m);
    ConceptList.setTotalCount(m.size());
    return ConceptList;
  }

  /* see superclass */
  @Override
  public Concept getConcept(Long id) throws Exception {
    return manager.find(ConceptJpa.class, id);
  }

  /* see superclass */
  @Override
  public Concept getConcept(String terminologyId, String terminology,
    String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (Concept) query.getSingleResult();
    } catch (NoResultException e) {
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Concept query for terminologyId = " + terminologyId
              + ", terminology = " + terminology + ", terminologyVersion = "
              + terminologyVersion + " returned no results!");
      return null;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ConceptList getAllConcepts(String terminology,
    String terminologyVersion) {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from ConceptJpa c where terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      final List<Concept> concepts = query.getResultList();
      final ConceptList conceptList = new ConceptListJpa();
      conceptList.setConcepts(concepts);
      return conceptList;
    } catch (NoResultException e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Concept query terminology = " + terminology
              + ", terminologyVersion = " + terminologyVersion
              + " returned no results!");
      return null;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public RelationshipList getAllActiveRelationships(String terminology,
    String terminologyVersion) {
    final javax.persistence.Query query =
        manager
            .createQuery("select r from RelationshipJpa r where active = 1 "
                + " and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      final List<Relationship> relationships = query.getResultList();
      final RelationshipList relationshipList = new RelationshipListJpa();
      relationshipList.setRelationships(relationships);
      return relationshipList;
    } catch (NoResultException e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Relationship query terminology = " + terminology
              + ", terminologyVersion = " + terminologyVersion
              + " returned no results!");
      return null;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public DescriptionList getAllActiveDescriptions(String terminology,
    String terminologyVersion) {
    final javax.persistence.Query query =
        manager
            .createQuery("select d from DescriptionJpa d where active = 1 "
                + " and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      final List<Description> descriptions = query.getResultList();
      final DescriptionList descriptionList = new DescriptionListJpa();
      descriptionList.setDescriptions(descriptions);
      return descriptionList;
    } catch (NoResultException e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Concept query terminology = " + terminology
              + ", terminologyVersion = " + terminologyVersion
              + " returned no results!");
      return null;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public LanguageRefSetMemberList getAllActiveLanguageRefSetMembers(
    String terminology, String terminologyVersion) {
    final javax.persistence.Query query =
        manager
            .createQuery("select l from LanguageRefSetMemberJpa l where active = 1"
                + " and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      final List<LanguageRefSetMember> languageRefSetMembers =
          query.getResultList();
      final LanguageRefSetMemberList languageRefSetMemberList =
          new LanguageRefSetMemberListJpa();
      languageRefSetMemberList.setLanguageRefSetMembers(languageRefSetMembers);
      return languageRefSetMemberList;
    } catch (NoResultException e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Concept query terminology = " + terminology
              + ", terminologyVersion = " + terminologyVersion
              + " returned no results!");
      return null;
    }
  }

  /* see superclass */
  @Override
  public Concept addConcept(Concept concept) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(concept);
      tx.commit();
    } else {
      manager.persist(concept);
    }

    return concept;
  }

  /* see superclass */
  @Override
  public void updateConcept(Concept concept) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(concept);
      tx.commit();
    } else {
      manager.merge(concept);
    }

  }

  /* see superclass */
  @Override
  public void removeTreePosition(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this concept
    final TreePosition tp = manager.find(TreePositionJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove specialist
      tx.begin();
      if (manager.contains(tp)) {
        manager.remove(tp);
      } else {
        manager.remove(manager.merge(tp));
      }
      tx.commit();

    } else {
      if (manager.contains(tp)) {
        manager.remove(tp);
      } else {
        manager.remove(manager.merge(tp));
      }
    }

  }

  /* see superclass */
  @Override
  public void removeConcept(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this concept
    final Concept mu = manager.find(ConceptJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove specialist
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public Description getDescription(Long id) throws Exception {
    return manager.find(DescriptionJpa.class, id);
  }

  /* see superclass */
  @Override
  public Description getDescription(String terminologyId, String terminology,
    String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from DescriptionJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      final Description description = (Description) query.getSingleResult();
      return description;
    } catch (NoResultException e) {
      Logger.getLogger(ContentServiceJpa.class).warn(
          "Could not retrieve description " + terminologyId
              + ", terminology = " + terminology + ", terminologyVersion = "
              + terminologyVersion + " returned no results!");
      return null;

    }
  }

  /* see superclass */
  @Override
  public Description addDescription(Description description) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(description);
      tx.commit();
    } else {
      manager.persist(description);
    }

    return description;
  }

  /* see superclass */
  @Override
  public void updateDescription(Description description) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(description);
      tx.commit();
    } else {
      manager.merge(description);
    }

  }

  /* see superclass */
  @Override
  public void removeDescription(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this description
    final Description mu = manager.find(DescriptionJpa.class, id);
    if (getTransactionPerOperation()) {

      // remove description
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public Relationship getRelationship(Long id) throws Exception {
    Relationship c = manager.find(RelationshipJpa.class, id);
    return c;
  }

  /* see superclass */
  @Override
  public Long getRelationshipId(String terminologyId, String terminology,
    String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery(
                "select r.id from RelationshipJpa r where terminologyId=:terminologyId and terminology=:terminology and terminologyVersion=:terminologyVersion")
            .setParameter("terminologyId", terminologyId)
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion);

    try {
      return (Long) query.getSingleResult();
    } catch (NoResultException e) {
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Could not find relationship id for" + terminologyId
              + " for terminology " + terminology + " and version "
              + terminologyVersion);
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Unexpected exception retrieving relationship id for" + terminologyId
              + " for terminology " + terminology + " and version "
              + terminologyVersion);
      return null;
    }

  }

  /* see superclass */
  @Override
  public Relationship getRelationship(String terminologyId, String terminology,
    String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from RelationshipJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (Relationship) query.getSingleResult();
    } catch (Exception e) {
      e.printStackTrace();
      Logger.getLogger(ContentServiceJpa.class).debug(
          "Relationship query for terminologyId = " + terminologyId
              + ", terminology = " + terminology + ", terminologyVersion = "
              + terminologyVersion + " threw an exception!");
      return null;
    }
  }

  /* see superclass */
  @Override
  public Relationship addRelationship(Relationship relationship)
    throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(relationship);
      tx.commit();
    } else {
      manager.persist(relationship);
    }

    return relationship;
  }

  /* see superclass */
  @Override
  public void updateRelationship(Relationship relationship) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(relationship);
      tx.commit();
    } else {
      manager.merge(relationship);
    }

  }

  /* see superclass */
  @Override
  public void removeRelationship(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this relationship
    final Relationship mu = manager.find(RelationshipJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove relationship
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public AttributeValueRefSetMember getAttributeValueRefSetMember(Long id)
    throws Exception {
    return manager.find(AttributeValueRefSetMemberJpa.class, id);
  }

  /* see superclass */
  @Override
  public AttributeValueRefSetMember getAttributeValueRefSetMember(
    String terminologyId, String terminology, String terminologyVersion)
    throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from AttributeValueRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (AttributeValueRefSetMember) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
      /*
       * throw new LocalException(
       * "AttributeValueRefSetMember query for terminologyId = " + terminologyId
       * + ", terminology = " + terminology + ", terminologyVersion = " +
       * terminologyVersion + " returned no results!", e);
       */
    }
  }

  /* see superclass */
  @Override
  public AttributeValueRefSetMember addAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(attributeValueRefSetMember);
      tx.commit();
    } else {
      manager.persist(attributeValueRefSetMember);
    }

    return attributeValueRefSetMember;
  }

  /* see superclass */
  @Override
  public void updateAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(attributeValueRefSetMember);
      tx.commit();
    } else {
      manager.merge(attributeValueRefSetMember);
    }

  }

  /* see superclass */
  @Override
  public void removeAttributeValueRefSetMember(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this map specialist
    final AttributeValueRefSetMember mu =
        manager.find(AttributeValueRefSetMemberJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove specialist
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public ComplexMapRefSetMember getComplexMapRefSetMember(Long id)
    throws Exception {
    return manager.find(ComplexMapRefSetMemberJpa.class, id);
  }

  /* see superclass */
  @Override
  public ComplexMapRefSetMember getComplexMapRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from ComplexMapRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (ComplexMapRefSetMember) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
      /*
       * throw new LocalException(
       * "ComplexMapRefSetMember query for terminologyId = " + terminologyId +
       * ", terminology = " + terminology + ", terminologyVersion = " +
       * terminologyVersion + " returned no results!", e);
       */
    }
  }

  /* see superclass */
  @Override
  public ComplexMapRefSetMember addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(complexMapRefSetMember);
      tx.commit();
    } else {
      manager.persist(complexMapRefSetMember);
    }

    return complexMapRefSetMember;
  }

  /* see superclass */
  @Override
  public void updateComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(complexMapRefSetMember);
      tx.commit();
    } else {
      manager.merge(complexMapRefSetMember);
    }

  }

  /* see superclass */
  @Override
  public void removeComplexMapRefSetMember(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this complex map ref set member
    final ComplexMapRefSetMember mu =
        manager.find(ComplexMapRefSetMemberJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove complex map ref set member
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public LanguageRefSetMember getLanguageRefSetMember(Long id) throws Exception {
    LanguageRefSetMember c = manager.find(LanguageRefSetMemberJpa.class, id);
    return c;
  }

  /* see superclass */
  @Override
  public LanguageRefSetMember getLanguageRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from LanguageRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (LanguageRefSetMember) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
      /*
       * throw new LocalException(
       * "LanguageRefSetMember query for terminologyId = " + terminologyId +
       * ", terminology = " + terminology + ", terminologyVersion = " +
       * terminologyVersion + " returned no results!", e);
       */
    }
  }

  /* see superclass */
  @Override
  public LanguageRefSetMember addLanguageRefSetMember(
    LanguageRefSetMember languageRefSetMember) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(languageRefSetMember);
      tx.commit();
    } else {
      manager.persist(languageRefSetMember);
    }

    return languageRefSetMember;
  }

  /* see superclass */
  @Override
  public void updateLanguageRefSetMember(
    LanguageRefSetMember languageRefSetMember) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(languageRefSetMember);
      tx.commit();
    } else {
      manager.merge(languageRefSetMember);
    }

  }

  /* see superclass */
  @Override
  public void removeLanguageRefSetMember(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this language ref set member
    final LanguageRefSetMember mu =
        manager.find(LanguageRefSetMemberJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove language ref set member
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public SimpleMapRefSetMember getSimpleMapRefSetMember(Long id)
    throws Exception {
    return manager.find(SimpleMapRefSetMemberJpa.class, id);
  }

  /* see superclass */
  @Override
  public SimpleMapRefSetMember getSimpleMapRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from SimpleMapRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (SimpleMapRefSetMember) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /* see superclass */
  @Override
  public SimpleMapRefSetMember addSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(simpleMapRefSetMember);
      tx.commit();
    } else {
      manager.persist(simpleMapRefSetMember);
    }

    return simpleMapRefSetMember;
  }

  /* see superclass */
  @Override
  public void updateSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(simpleMapRefSetMember);
      tx.commit();
    } else {
      manager.merge(simpleMapRefSetMember);
    }

  }

  /* see superclass */
  @Override
  public void removeSimpleMapRefSetMember(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this simple map ref set member
    final SimpleMapRefSetMember mu =
        manager.find(SimpleMapRefSetMemberJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove simple map ref set member
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public SimpleRefSetMember getSimpleRefSetMember(Long id) throws Exception {
    return manager.find(SimpleRefSetMemberJpa.class, id);
  }

  /* see superclass */
  @Override
  public SimpleRefSetMember getSimpleRefSetMember(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select c from SimpleRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */
    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      return (SimpleRefSetMember) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /* see superclass */
  @Override
  public SimpleRefSetMember addSimpleRefSetMember(
    SimpleRefSetMember simpleRefSetMember) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(simpleRefSetMember);
      tx.commit();
    } else {
      manager.persist(simpleRefSetMember);
    }

    return simpleRefSetMember;
  }

  /* see superclass */
  @Override
  public void updateSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember)
    throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(simpleRefSetMember);
      tx.commit();
    } else {
      manager.merge(simpleRefSetMember);
    }

  }

  /* see superclass */
  @Override
  public void removeSimpleRefSetMember(Long id) throws Exception {

    tx = manager.getTransaction();

    // retrieve this simple ref set member
    final SimpleRefSetMember mu = manager.find(SimpleRefSetMemberJpa.class, id);

    if (getTransactionPerOperation()) {

      // remove simple ref set member
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /* see superclass */
  @Override
  public SearchResultList findConceptsForQuery(String searchString,
    PfsParameter pfsParameter) throws Exception {

    final SearchResultList results = new SearchResultListJpa();

    int[] totalCt = new int[1];
    @SuppressWarnings("unchecked")
    List<Concept> concepts =
        (List<Concept>) getQueryResults(searchString == null ? ""
            : searchString, ConceptJpa.class, ConceptJpa.class, pfsParameter,
            totalCt);

    // construct the search results
    for (final Concept c : concepts) {
      final SearchResult sr = new SearchResultJpa();
      sr.setId(c.getId());
      sr.setTerminologyId(c.getTerminologyId());
      sr.setTerminology(c.getTerminology());
      sr.setTerminologyVersion(c.getTerminologyVersion());
      sr.setValue(c.getDefaultPreferredName());
      results.addSearchResult(sr);
    }
    results.setTotalCount(totalCt[0]);
    return results;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findDescendantConcepts(String terminologyId,
    String terminology, String terminologyVersion, PfsParameter pfsParameter)
    throws Exception {

    Logger.getLogger(ContentServiceJpa.class).info(
        "findDescendantConcepts called: " + terminologyId + ", " + terminology
            + ", " + terminologyVersion);

    final SearchResultList searchResultList = new SearchResultListJpa();

    javax.persistence.Query query =
        manager
            .createQuery("select tp.ancestorPath from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    query.setParameter("terminologyId", terminologyId);

    // get the first tree position
    query.setMaxResults(1);
    final List<String> ancestorPaths = query.getResultList();

    // skip construction if no ancestor path was found
    if (ancestorPaths.size() != 0) {

      String ancestorPath = ancestorPaths.get(0);

      // insert string to actually add this concept to the ancestor path
      if (!ancestorPath.isEmpty()) {
        ancestorPath += "~";
      }
      ancestorPath += terminologyId;

      // construct query for descendants
      query =
          manager.createQuery("select distinct c "
              + "from TreePositionJpa tp, ConceptJpa c "
              + "where tp.terminologyId = c.terminologyId "
              + "and tp.terminology = c.terminology "
              + "and tp.terminologyVersion = c.terminologyVersion "
              + "and tp.terminology = :terminology "
              + "and tp.terminologyVersion = :terminologyVersion "
              + "and tp.ancestorPath like '" + ancestorPath + "%'");
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);

      final List<Concept> concepts = query.getResultList();

      // set the total count of descendant concepts
      searchResultList.setTotalCount(concepts.size());

      // apply paging, and sorting if appropriate
      if (pfsParameter != null
          && (pfsParameter.getSortField() != null && !pfsParameter
              .getSortField().isEmpty())) {
        // check that specified sort field exists on Concept and is
        // a string
        final Field sortField =
            Concept.class.getDeclaredField(pfsParameter.getSortField());
        if (!sortField.getType().equals(String.class)) {

          throw new Exception(
              "findDescendantConcepts error:  Referenced sort field is not of type String");
        }

        // allow the field to access the Concept values
        sortField.setAccessible(true);

        // sort the list - UNTESTED
        Collections.sort(concepts, new Comparator<Concept>() {
          @Override
          public int compare(Concept c1, Concept c2) {

            // if an exception is returned, simply pass equality
            try {
              return ((String) sortField.get(c1)).compareTo((String) sortField
                  .get(c2));
            } catch (Exception e) {
              return 0;
            }
          }
        });
      }

      // get the start and end indexes based on paging parameters
      int startIndex = 0;
      int toIndex = concepts.size();
      if (pfsParameter != null) {
        startIndex = pfsParameter.getStartIndex();
        toIndex =
            Math.min(concepts.size(), startIndex + pfsParameter.getMaxResults());
      }

      // construct the search results
      for (final Concept c : concepts.subList(startIndex, toIndex)) {
        final SearchResult searchResult = new SearchResultJpa();
        searchResult.setId(c.getId());
        searchResult.setTerminologyId(c.getTerminologyId());
        searchResult.setTerminology(c.getTerminology());
        searchResult.setTerminologyVersion(c.getTerminologyVersion());
        searchResult.setValue(c.getDefaultPreferredName());
        searchResultList.addSearchResult(searchResult);
      }
    }

    // return the search result list
    return searchResultList;
  }

  /* see superclass */
  @Override
  public int getDescendantConceptsCount(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {

    Logger.getLogger(ContentServiceJpa.class).debug(
        "getDescendantConceptsCount called: " + terminologyId + ", "
            + terminology + ", " + terminologyVersion);

    final javax.persistence.Query query =
        manager
            .createQuery("select tp.descendantCount from TreePositionJpa tp "
                + "where tp.terminologyId = :terminologyId "
                + "and tp.terminology = :terminology "
                + "and tp.terminologyVersion = :terminologyVersion");
    query.setParameter("terminologyId", terminologyId);
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    query.setMaxResults(1);

    @SuppressWarnings("unchecked")
    final List<Object> results = query.getResultList();
    if (results.size() > 0) {
      return Integer.parseInt(results.get(0).toString());
    } else {
      return 0;
    }

  }

  /* see superclass */
  @Override
  public void clearTreePositions(String terminology, String terminologyVersion)
    throws Exception {

    if (getTransactionPerOperation()) {
      tx.begin();

      final javax.persistence.Query query =
          manager
              .createQuery("DELETE From TreePositionJpa c where terminology = :terminology and terminologyVersion = :terminologyVersion");
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      int deleteRecords = query.executeUpdate();
      Logger.getLogger(getClass()).info(
          "    treepos records deleted: " + deleteRecords);
      tx.commit();

    } else {
      final javax.persistence.Query query =
          manager
              .createQuery("DELETE From TreePositionJpa c where terminology = :terminology and terminologyVersion = :terminologyVersion");
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);
      int deleteRecords = query.executeUpdate();
      Logger.getLogger(getClass()).info(
          "    treepos records deleted: " + deleteRecords);
    }

  }

  /* see superclass */
  @Override
  public ValidationResult computeTreePositions(String terminology,
    String terminologyVersion, String typeId, String rootId) throws Exception {
    Logger.getLogger(this.getClass()).info(
        "Starting computeTreePositions - " + rootId + ", " + terminology
            + ", isaRelTypeId = " + typeId);

    // initialize global variables
    final EntityTransaction computeTreePositionTransaction =
        manager.getTransaction();
    int computeTreePositionCommitCt = 5000;
    computeTreePositionTotalCount = 0;
    computeTreePositionMaxMemoryUsage = 0L;
    computeTreePositionLastTime = System.currentTimeMillis();
    computeTreePositionValidationResult = new ValidationResultJpa();

    // get the root concept
    final Concept rootConcept =
        getConcept(rootId, terminology, terminologyVersion);

    // begin the transaction
    computeTreePositionTransaction.begin();

    // begin the recursive computation
    computeTreePositionsHelper(null, rootConcept, Long.valueOf(typeId), "",
        computeTreePositionCommitCt, computeTreePositionTransaction, false);

    // commit any remaining tree positions
    computeTreePositionTransaction.commit();

    Runtime runtime = Runtime.getRuntime();
    Logger.getLogger(this.getClass()).info(
        "Final Tree Positions: " + computeTreePositionTotalCount
            + ", MEMORY USAGE: " + runtime.totalMemory());

    return computeTreePositionValidationResult;

  }

  /**
   * Recursive function to create tree positions and compute children/descendant
   * count at each tree position.
   * 
   * NOTE: This function is designed to keep as little Concept information in
   * storage as possible. See inline notes.
   *
   * @param parChd the par chd
   * @param concept the concept
   * @param typeId the type id
   * @param ancestorPath the ancestor path
   * @param computeTreePositionCommitCt the compute tree position commit ct
   * @param computeTreePositionTransaction the compute tree position transaction
   * @param cycleCheckOnly the cycle check only
   * @return tree positions at this level
   * @throws Exception the exception
   */
  @SuppressWarnings("unused")
  private Set<Long> computeTreePositionsHelper(Map<Long, Set<Long>> parChd,
    Concept concept, Long typeId, String ancestorPath,
    int computeTreePositionCommitCt,
    EntityTransaction computeTreePositionTransaction, boolean cycleCheckOnly)
    throws Exception {

    // Get all relationships
    Map<Long, Set<Long>> localParChd = parChd;
    if (localParChd == null) {
      Logger.getLogger(this.getClass()).info(
          "  Loading relationships " + typeId);
      localParChd = new HashMap<>();
      @SuppressWarnings("unchecked")
      final List<Relationship> relationships =
          manager
              .createQuery(
                  "select r from RelationshipJpa r where "
                      + "terminologyVersion = :terminologyVersion and terminology = :terminology "
                      + "and typeId = :typeId and active = 1 "
                      + "and sourceConcept in (select sc from ConceptJpa sc where active = 1)")
              .setParameter("typeId", typeId)
              .setParameter("terminology", concept.getTerminology())
              .setParameter("terminologyVersion",
                  concept.getTerminologyVersion()).getResultList();
      int ct = 0;
      for (final Relationship r : relationships) {
        ct++;
        final Concept sourceConcept = r.getSourceConcept();
        final Concept destinationConcept = r.getDestinationConcept();
        if (!localParChd.containsKey(destinationConcept.getId())) {
          localParChd.put(destinationConcept.getId(), new HashSet<Long>());
        }
        final Set<Long> children = localParChd.get(destinationConcept.getId());
        children.add(sourceConcept.getId());
      }
      Logger.getLogger(this.getClass()).info("    count = " + ct);

    }

    final Set<Long> descConceptIds = new HashSet<>();

    // if concept is active
    if (concept.isActive()) {

      // extract the ancestor terminology ids
      final Set<String> ancestors = new HashSet<>();
      for (final String ancestor : ancestorPath.split("~"))
        ancestors.add(ancestor);

      // if ancestor path contains this terminology id, a child/ancestor cycle
      // exists
      if (ancestors.contains(concept.getTerminologyId())) {

        // add error to validation result
        computeTreePositionValidationResult
            .addError("Cycle detected for concept "
                + concept.getTerminologyId() + ", ancestor path is "
                + ancestorPath);

        // return empty set of descendants to truncate calculation on this path
        return descConceptIds;
      }

      // instantiate the tree position
      final TreePosition tp = new TreePositionJpa();

      if (!cycleCheckOnly) {

        // logging information
        int ancestorCount =
            ancestorPath.length() - ancestorPath.replaceAll("~", "").length();
        String loggerPrefix = "";
        for (int i = 0; i < ancestorCount; i++)
          loggerPrefix += "  ";

        tp.setAncestorPath(ancestorPath);
        tp.setTerminology(concept.getTerminology());
        tp.setTerminologyVersion(concept.getTerminologyVersion());
        tp.setTerminologyId(concept.getTerminologyId());
        tp.setDefaultPreferredName(concept.getDefaultPreferredName());

        // persist the tree position
        manager.persist(tp);
      }
      // construct the ancestor path terminating at this concept
      final String conceptPath =
          (ancestorPath.equals("") ? concept.getTerminologyId() : ancestorPath
              + "~" + concept.getTerminologyId());

      // Gather descendants if this is not a leaf node
      if (localParChd.containsKey(concept.getId())) {

        descConceptIds.addAll(localParChd.get(concept.getId()));

        // iterate over the child terminology ids
        // this iteration is entirely local and depends on no managed
        // objects
        for (final Long childConceptId : localParChd.get(concept.getId())) {

          // call helper function on child concept
          // add the results to the local descendant set
          final Set<Long> desc =
              computeTreePositionsHelper(localParChd,
                  getConcept(childConceptId), typeId, conceptPath,
                  computeTreePositionCommitCt, computeTreePositionTransaction,
                  cycleCheckOnly);
          if (!cycleCheckOnly) {
            descConceptIds.addAll(desc);
          }

        }

      }

      if (!cycleCheckOnly) {

        // set the children count
        tp.setChildrenCount(localParChd.containsKey(concept.getId())
            ? localParChd.get(concept.getId()).size() : 0);

        // set the descendant count
        tp.setDescendantCount(descConceptIds.size());

        // In case manager was cleared here, get it back onto changed list
        manager.merge(tp);

        // routinely commit and force clear the manager
        // any existing recursive threads are entirely dependent on local
        // variables
        if (++computeTreePositionTotalCount % computeTreePositionCommitCt == 0) {

          // commit the transaction
          computeTreePositionTransaction.commit();

          // Clear manager for memory management
          manager.clear();

          // begin a new transaction
          computeTreePositionTransaction.begin();

          // report progress and memory usage
          Runtime runtime = Runtime.getRuntime();
          float elapsedTime =
              System.currentTimeMillis() - computeTreePositionLastTime;
          elapsedTime = elapsedTime / 1000;
          computeTreePositionLastTime = System.currentTimeMillis();

          if (runtime.totalMemory() > computeTreePositionMaxMemoryUsage)
            computeTreePositionMaxMemoryUsage = runtime.totalMemory();

          Logger.getLogger(ContentServiceJpa.class).info(
              "\t" + System.currentTimeMillis() / 1000 + "\t"
                  + computeTreePositionTotalCount + "\t"
                  + Math.floor(runtime.totalMemory() / 1024 / 1024) + "\t"
                  + Double.toString(computeTreePositionCommitCt / elapsedTime));

        }
      }
    }

    // Check that this concept does not reference itself as a child
    if (descConceptIds.contains(concept.getTerminologyId())) {

      // add error to validation result
      computeTreePositionValidationResult.addError("Concept "
          + concept.getTerminologyId() + " claims itself as a child");

      // remove this terminology id to prevent infinite loop
      descConceptIds.remove(concept.getTerminologyId());
    }

    // return the descendant concept set
    // note that the local child and descendant set will be garbage
    // collected
    return descConceptIds;

  }

  /* see superclass */
  @Override
  public void cycleCheck(String terminology, String terminologyVersion,
    String typeId, String rootId) throws Exception {
    Logger.getLogger(this.getClass()).info(
        "Starting cycle check - " + rootId + ", " + terminology
            + ", isaRelTypeId = " + typeId);

    // get the root concept
    final Concept rootConcept =
        getConcept(rootId, terminology, terminologyVersion);

    // begin the recursive computation
    computeTreePositionsHelper(null, rootConcept, Long.valueOf(typeId), "", 0,
        null, true);

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public TreePositionList getRootTreePositions(String terminology,
    String terminologyVersion) throws Exception {
    List<TreePosition> treePositions =
        manager
            .createQuery(
                "select tp from TreePositionJpa tp where ancestorPath = '' and terminologyVersion = :terminologyVersion and terminology = :terminology")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .getResultList();

    // if only one result (single root), use the children of that concept
    // instead
    if (treePositions.size() == 1) {
      treePositions =
          manager
              .createQuery(
                  "select tp from TreePositionJpa tp where ancestorPath = '"
                      + treePositions.iterator().next().getTerminologyId()
                      + "' and terminologyVersion = :terminologyVersion and terminology = :terminology")
              .setParameter("terminology", terminology)
              .setParameter("terminologyVersion", terminologyVersion)
              .getResultList();
    }
  
    final TreePositionListJpa treePositionList = new TreePositionListJpa();
    sortTreePositions(treePositions);
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());

    return treePositionList;
  }

  /**
   * Gets the child tree positions.
   * 
   * @param treePosition the tree position
   * @return the child tree positions
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private TreePositionList getChildTreePositions(TreePosition treePosition)
    throws Exception {
    final List<TreePosition> treePositions =
        manager
            .createQuery(
                "select tp from TreePositionJpa tp where ancestorPath = :ancestorPath and terminology = :terminology and terminologyVersion = :terminologyVersion")
            .setParameter(
                "ancestorPath",
                (treePosition.getAncestorPath().length() == 0 ? ""
                    : treePosition.getAncestorPath() + "~")
                    + treePosition.getTerminologyId())
            .setParameter("terminology", treePosition.getTerminology())
            .setParameter("terminologyVersion",
                treePosition.getTerminologyVersion()).getResultList();
    final TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());
    return treePositionList;
  }

  /* see superclass */
  @Override
  public TreePositionList getTreePositions(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {

    @SuppressWarnings("unchecked")
    final List<TreePosition> treePositions =
        manager
            .createQuery(
                "select tp from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .setParameter("terminologyId", terminologyId).getResultList();
    final TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());

    return treePositionList;
  }

  /* see superclass */
  @Override
  public boolean isDescendantOf(String terminologyId, String terminology,
    String terminologyVersion, String ancestorId) throws Exception {
    final long ct =
        (long) manager
            .createQuery(
                "select count(tp) from TreePositionJpa tp "
                    + "where terminologyVersion = :terminologyVersion "
                    + "and terminology = :terminology "
                    + "and terminologyId = :terminologyId "
                    + "and ancestorPath like :path")
            .setParameter("path", "%~" + ancestorId + "~%")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .setParameter("terminologyId", terminologyId).getSingleResult();
    return ct > 0;
  }

  /* see superclass */
  @Override
  public boolean isDescendantOf(String terminologyId, String terminology,
    String terminologyVersion, List<String> ancestorIds) throws Exception {

    // Build clauses - this will probably fail with too many ids
    StringBuilder sb = new StringBuilder();
    sb.append("and (");
    boolean seen = false;
    for (int i = 1; i <= ancestorIds.size(); i++) {
      if (seen) {
        sb.append(" or ");
      }
      seen = true;
      sb.append("ancestorPath like :path" + i);
    }
    sb.append(")");

    final javax.persistence.Query query =
        manager.createQuery("select count(tp) from TreePositionJpa tp "
            + "where terminologyVersion = :terminologyVersion "
            + "and terminology = :terminology "
            + "and terminologyId = :terminologyId " + sb.toString());

    // Build parameters
    for (int i = 1; i <= ancestorIds.size(); i++) {
      query.setParameter("path" + i, "%~" + ancestorIds.get(i - 1) + "~%");
    }
    final long ct =
        (long) query.setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .setParameter("terminologyId", terminologyId).getSingleResult();
    return ct > 0;
  }

  /* see superclass */
  @Override
  public TreePositionList getTreePositionsWithDescendants(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    // get tree positions for concept (may be multiple)
    @SuppressWarnings("unchecked")
    List<TreePosition> treePositions =
        manager
            .createQuery(
                "select tp from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .setParameter("terminologyId", terminologyId).getResultList();
    TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());

    TreePositionListJpa treePositionsWithDescendants =
        new TreePositionListJpa();

    // for each tree position
    for (final TreePosition treePosition : treePositionList.getTreePositions()) {

      treePositionsWithDescendants
          .addTreePosition(getTreePositionWithDescendants(treePosition));
    }
    treePositionsWithDescendants.setTotalCount(treePositionsWithDescendants
        .getTreePositions().size());
    return treePositionsWithDescendants;
  }

  /**
   * Returns the any tree positions with descendants.
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the any tree positions with descendants
   * @throws Exception the exception
   */
  @Override
  public TreePosition getAnyTreePositionWithDescendants(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    // get tree positions for concept (may be multiple)
    @SuppressWarnings("unchecked")
    List<TreePosition> treePositions =
        manager
            .createQuery(
                "select tp from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion)
            .setParameter("terminologyId", terminologyId).setMaxResults(1)
            .getResultList();
    // for each tree position
    for (final TreePosition treePosition : treePositions) {
      return getTreePositionWithDescendants(treePosition);
    }
    return null;
  }

  /* see superclass */
  @Override
  public TreePosition getTreePositionWithDescendants(TreePosition tp)
    throws Exception {

    if (tp.getChildrenCount() > 0) {

      TreePositionList tpChildren = getChildTreePositions(tp);

      for (final TreePosition tpChild : tpChildren.getTreePositions()) {
        tp.addChild(getTreePositionWithDescendants(tpChild));
      }
    }

    return tp;
  }

  /**
   * Given a local tree position, returns the root tree with this tree position
   * as terminal child.
   * 
   * @param treePosition the tree position
   * @return the root tree position for concept
   * @throws Exception the exception
   */
  public TreePosition constructRootTreePosition(TreePosition treePosition)
    throws Exception {

    // array of terminology ids from ancestor path
    String ancestors[] = treePosition.getAncestorPath().split("~");

    // list of ancestral tree positions, will be ordered root->immediate
    // ancestor
    List<TreePosition> ancestorTreePositions = new ArrayList<>();

    // for each ancestor, get the tree position corresponding to the
    // original
    // tree position's path
    for (int i = ancestors.length - 1; i > -1; i--) {

      // flag to ensure ancestor exists
      boolean ancestorFound = false;

      // cycle over the tree positions for this ancestor
      for (final TreePosition tp : getTreePositions(ancestors[i],
          treePosition.getTerminology(), treePosition.getTerminologyVersion())
          .getTreePositions()) {

        // check if this ancestor path matches the beginning of the
        // original
        // tree position's ancestor path
        if (treePosition.getAncestorPath().startsWith(tp.getAncestorPath())) {
          ancestorTreePositions.add(tp);
          ancestorFound = true;
        }
      }

      if (ancestors[i].length() != 0 && !ancestorFound) {
        throw new Exception("Ancestor tree position " + ancestors[i]
            + " not found!");
      }
    }

    // the returned full (root) tree position
    TreePosition rootTreePosition = treePosition;

    // if all tree positions to root have been found, construct the final
    // tree
    // position
    for (final TreePosition tp : ancestorTreePositions) {

      // if this persisted tree position does not have this id as a child,
      // add
      // currently constructed root tree position as a child
      if (!tp.getChildren().contains(rootTreePosition)) {
        tp.addChild(rootTreePosition);
      }

      // set the new root tree position
      rootTreePosition = tp;
    }

    return rootTreePosition;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public TreePositionList getTreePositionGraphForQuery(String terminology,
    String terminologyVersion, String query, PfsParameter pfs) throws Exception {

    final PfsParameter localPfs =
        pfs == null ? new PfsParameterJpa() : new PfsParameterJpa(pfs);

        // construct the query
    final StringBuilder sb = new StringBuilder();
    if (query != null && !query.isEmpty() && !query.equals("null")) {
      sb.append(query).append(" AND ");
    }
    sb.append("terminology:" + terminology + " AND terminologyVersion:"
        + terminologyVersion);

    // retrieve the query results
    final int[] totalCt = new int[1];
    final List<TreePosition> queriedTreePositions =
        (List<TreePosition>) getQueryResults(sb.toString(),
            TreePositionJpa.class, TreePositionJpa.class,
            localPfs, totalCt);

    // initialize the result set
    final List<TreePosition> fullTreePositions = new ArrayList<>();

    // for each query result, construct the full tree (i.e. up to root, and
    // including children if exact match)
    for (final TreePosition queriedTreePosition : queriedTreePositions) {

      // if the query is an exact match for the terminology id of this
      // tree position, attach children
      if (queriedTreePosition.getTerminologyId().toUpperCase()
          .equals(query.toUpperCase())) {
        queriedTreePosition.setChildren(getChildTreePositions(
            queriedTreePosition).getTreePositions());

      }

      final TreePosition fullTreePosition =
          constructRootTreePosition(queriedTreePosition);

      // if this root is already present in the final list, add this
      // position's children to existing root
      if (fullTreePositions.contains(fullTreePosition)) {

        final TreePosition existingTreePosition =
            fullTreePositions.get(fullTreePositions.indexOf(fullTreePosition));

        /*
         * existingTreePosition .addChildren(fullTreePosition.getChildren());
         */

        fullTreePositions.set(fullTreePositions.indexOf(fullTreePosition),
            existingTreePosition);

        // otherwise, add this root
      } else {
        fullTreePositions.add(fullTreePosition);
      }
    }

    final TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(fullTreePositions);
    treePositionList.setTotalCount(totalCt[0]);
    return treePositionList;
  }

  /* see superclass */
  @Override
  public void computeTreePositionInformation(TreePositionList tpList,
    Map<String, String> descTypes, Map<String, String> relTypes)
    throws Exception {
    // if results are found, retrieve metadata and compute information
    if (tpList.getCount() > 0) {

      for (final TreePosition tp : tpList.getTreePositions())
        computeTreePositionInformationHelper(tp, descTypes, relTypes,
            new HashMap<String, List<TreePositionDescriptionGroup>>());
    }

  }

  /**
   * Helper function to recursively calculate the displayed information for a
   * tree position graph - description groups (e.g. inclusions, exclusions, etc)
   * - concept labels (e.g. code ranges) attached to inclusions/exclusions
   * 
   * Algorithm: (1) get Concept for tree position (2) Cycle over all
   * Descriptions (3) For each description, cycle over all relationships (4) If
   * relationship terminology id starts with the description terminology id,
   * this is something to render (5) ... update this later
   * 
   * Data Structure: TreePosition to DescriptionGroups: each description group
   * is a description type, e.g. Inclusion, Exclusion, etc. DescriptionGroups to
   * Description: each description is a concept preferred name and a set of
   * referenced concepts ReferencedConcept: each referenced concept is a display
   * name and the terminology id of an existing concept to link to
   *
   * @param treePosition the tree position
   * @param descTypes the desc types
   * @param relTypes the rel types
   * @param groupMap the concept map
   * @throws Exception the exception
   */
  private void computeTreePositionInformationHelper(TreePosition treePosition,
    Map<String, String> descTypes, Map<String, String> relTypes,
    Map<String, List<TreePositionDescriptionGroup>> groupMap) throws Exception {

	  // if already computed, attach desc groups and cycle through children
    if (groupMap.containsKey(treePosition.getTerminologyId())) {
      treePosition.setDescGroups(groupMap.get(treePosition.getTerminologyId()));
      if (treePosition.getChildrenCount() > 0) {
        for (final TreePosition tp : treePosition.getChildren()) {
          computeTreePositionInformationHelper(tp, descTypes, relTypes, groupMap);
        }
      }
      return;
    }

    // get the concept for this tree position
    final Concept concept =
        getConcept(treePosition.getTerminologyId(),
            treePosition.getTerminology(), treePosition.getTerminologyVersion());

    // map of Type -> Description type Groups
    // e.g. there "Inclusion" -> all inclusion description groups
    final Map<String, TreePositionDescriptionGroup> descGroups =
        new HashMap<>();

    // cycle over all descriptions
    for (final Description desc : concept.getDescriptions()) {
      final String descType = desc.getTypeId().toString();

      // get or create the description group for this description type
      TreePositionDescriptionGroup descGroup = null;
      if (descGroups.containsKey(descType)) {
        descGroup = descGroups.get(descType);
      } else {

        descGroup = new TreePositionDescriptionGroupJpa();
        descGroup.setName(descTypes.get(descType));
        descGroup.setTypeId(descType);
      }

      // get or create the tree position description for this description
      // term
      TreePositionDescription tpDesc = null;
      for (final TreePositionDescription tpd : descGroup
          .getTreePositionDescriptions()) {
        if (tpd.getName().equals(desc.getTerm()))
          tpDesc = tpd;
      }

      // if no description found, create a new one
      if (tpDesc == null) {
        tpDesc = new TreePositionDescriptionJpa();
        tpDesc.setName(desc.getTerm());
      }

      // add to group
      descGroup.addTreePositionDescription(tpDesc);

      // put it in the set
      descGroups.put(descGroup.getTypeId(), descGroup);

      // check for references
      // find any relationship where terminology id starts with the
      // description's terminology id
      // TODO: make this part of project specific algorithm handler
      // only ICD10 cares about dagger/asterisk and "references" relationships
      if (treePosition.getTerminology().equals("ICD10")) {
        for (final Relationship rel : concept.getRelationships()) {
          if (rel.getTerminologyId().startsWith(desc.getTerminologyId() + "~")) {

            // Non-persisted objects, so remove this description from
            // list, modify it, and re-add it
            descGroup.removeTreePositionDescription(tpDesc);

            // create the referenced concept object
            final TreePositionReferencedConcept referencedConcept =
                new TreePositionReferencedConceptJpa();
            referencedConcept.setTerminologyId(rel.getDestinationConcept()
                .getTerminologyId());

            // if no label, just use terminology id
            // if label present, use label as display name
            String displayName =
                (rel.getLabel() == null ? rel.getDestinationConcept()
                    .getTerminologyId() : rel.getLabel());

            // switch on relationship type to add any additional information
            final String relType = relTypes.get(rel.getTypeId().toString());

            // if asterisk-to-dagger, add †
            if (relType.indexOf("Asterisk") == 0) {
              displayName += " *";
            } else if (relType.indexOf("Dagger") == 0) {
              displayName += " \u2020";
            }

            referencedConcept.setDisplayName(displayName);

            tpDesc.addReferencedConcept(referencedConcept);

            // add or re-add the tree position description (was removed
            // earlier if existed)
            descGroup.addTreePositionDescription(tpDesc);

            // replace the existing desc group
            descGroups.put(descGroup.getTypeId(), descGroup);

          }
        }
      }
    }

    treePosition.setDescGroups(new ArrayList<>(descGroups.values()));
    groupMap.put(treePosition.getTerminologyId(), treePosition.getDescGroups());

    // calculate information for all children
    if (treePosition.getChildrenCount() > 0) {
      for (final TreePosition tp : treePosition.getChildren()) {
        computeTreePositionInformationHelper(tp, descTypes, relTypes, groupMap);
      }
    }

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ConceptList getConceptsModifiedSinceDate(String terminology,
    Date date, PfsParameter pfsParameter) throws Exception {
    ConceptList results = new ConceptListJpa();

    javax.persistence.Query query;

    PfsParameter localPfsParameter = pfsParameter;
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // if no date provided, get the latest modified concepts
    query =
        manager.createQuery(
            "select max(c.effectiveTime) from ConceptJpa c"
                + " where terminology = :terminology").setParameter(
            "terminology", terminology);

    Date tempDate = (Date) query.getSingleResult();
    Logger.getLogger(ContentServiceJpa.class).info(
        "Max date   = " + tempDate.toString());

    Date localDate = date;
    if (localDate == null) {
      localDate = tempDate;
    }

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

    QueryBuilder qb =
        searchFactory.buildQueryBuilder().forEntity(ConceptJpa.class).get();

    Query luceneQuery;
    if (localPfsParameter.getQueryRestriction() == null) {
      luceneQuery =
          qb.bool()
              .must(
                  qb.keyword().onField("effectiveTime").matching(localDate)
                      .createQuery())
              .must(
                  qb.keyword().onField("terminology").matching(terminology)
                      .createQuery()).createQuery();
    } else {
      luceneQuery =
          qb.bool()
              .must(
                  qb.keyword().onField("effectiveTime").matching(localDate)
                      .createQuery())
              .must(
                  qb.keyword().onField("terminology").matching(terminology)
                      .createQuery())
              .must(
                  qb.keyword()
                      .onFields("terminologyId", "defaultPreferredName")
                      .matching(localPfsParameter.getQueryRestriction())
                      .createQuery()).createQuery();

    }
    Logger.getLogger(ContentServiceJpa.class).info(
        "Query text: " + luceneQuery.toString());

    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager
            .createFullTextQuery(luceneQuery, ConceptJpa.class);

    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(localPfsParameter.getStartIndex());
      ftquery.setMaxResults(localPfsParameter.getMaxResults());

    }
    results.setTotalCount(ftquery.getResultSize());
    results.setConcepts(ftquery.getResultList());
    return results;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public DescriptionList getDescriptionsModifiedSinceDate(String terminology,
    Date date) {
    DescriptionList results = new DescriptionListJpa();

    javax.persistence.Query query =
        manager
            .createQuery(
                "select d from DescriptionJpa d"
                    + " where effectiveTime >= :releaseDate"
                    + " and terminology = :terminology")
            .setParameter("releaseDate", date)
            .setParameter("terminology", terminology);

    results.setDescriptions(query.getResultList());
    return results;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public RelationshipList getRelationshipsModifiedSinceDate(String terminology,
    Date date) {
    RelationshipList results = new RelationshipListJpa();

    javax.persistence.Query query =
        manager
            .createQuery(
                "select r from RelationshipJpa r"
                    + " where effectiveTime >= :releaseDate"
                    + " and terminology = :terminology")
            .setParameter("releaseDate", date)
            .setParameter("terminology", terminology);

    results.setRelationships(query.getResultList());

    return results;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public LanguageRefSetMemberList getLanguageRefSetMembersModifiedSinceDate(
    String terminology, Date date) {
    LanguageRefSetMemberList results = new LanguageRefSetMemberListJpa();

    javax.persistence.Query query =
        manager
            .createQuery(
                "select l from LanguageRefSetMemberJpa l"
                    + " where effectiveTime >= :releaseDate"
                    + " and terminology = :terminology")
            .setParameter("releaseDate", date)
            .setParameter("terminology", terminology);

    results.setLanguageRefSetMembers(query.getResultList());

    return results;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getAllRelationshipTerminologyIds(String terminology,
    String terminologyVersion) {
    javax.persistence.Query query =
        manager
            .createQuery(
                "select c.terminologyId from RelationshipJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion);

    List<String> terminologyIds = query.getResultList();
    Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
    return terminologyIdSet;

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getAllDescriptionTerminologyIds(String terminology,
    String terminologyVersion) {
    javax.persistence.Query query =
        manager
            .createQuery(
                "select c.terminologyId from DescriptionJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion);

    List<String> terminologyIds = query.getResultList();
    Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
    return terminologyIdSet;

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getAllLanguageRefSetMemberTerminologyIds(
    String terminology, String terminologyVersion) {
    javax.persistence.Query query =
        manager
            .createQuery(
                "select c.terminologyId from LanguageRefSetMemberJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
            .setParameter("terminology", terminology)
            .setParameter("terminologyVersion", terminologyVersion);

    List<String> terminologyIds = query.getResultList();
    Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
    return terminologyIdSet;

  }

  /* see superclass */
  @Override
  public TreePositionList getTreePositionsWithChildren(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    TreePositionList treePositionList =
        this.getTreePositions(terminologyId, terminology, terminologyVersion);

    for (final TreePosition tp : treePositionList.getTreePositions()) {
    	List<TreePosition> children = getChildTreePositions(tp).getTreePositions();
    	sortTreePositions(children);
    	tp.setChildren(children);
    }

    return treePositionList;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ComplexMapRefSetMemberList getComplexMapRefSetMembersForRefSetId(
    String refSetId) throws Exception {
    // Attempt to get complex members
    final List<ComplexMapRefSetMember> members =
        manager
            .createQuery(
                "select c from ComplexMapRefSetMemberJpa c where refSetId = :refSetId")
            .setParameter("refSetId", refSetId).getResultList();

    // If not found, try for simple
    if (members.size() == 0) {
      final List<SimpleMapRefSetMember> simpleMembers =
          manager
              .createQuery(
                  "select c from SimpleMapRefSetMemberJpa c where refSetId = :refSetId")
              .setParameter("refSetId", refSetId).getResultList();
      // Convert to complex map refset members
      for (final SimpleMapRefSetMember simpleMember : simpleMembers) {
        members.add(new ComplexMapRefSetMemberJpa(simpleMember));
      }
    }
    final ComplexMapRefSetMemberList list = new ComplexMapRefSetMemberListJpa();
    list.setComplexMapRefSetMembers(members);
    list.setTotalCount(list.getCount());
    return list;
  }

  /* see superclass */
  @Override
  public boolean isDescendantOfPath(String ancestorPath, String terminologyId,
    String terminology, String terminologyVersion) throws Exception {

    StringBuilder sb = new StringBuilder();
    sb.append("ancestorPath:" + QueryParser.escape(ancestorPath) + "*");
    sb.append(" AND terminologyId:" + terminologyId);
    sb.append(" AND terminology:" + terminology);
    sb.append(" AND terminologyVersion:" + terminologyVersion);

    PfsParameter pfs = new PfsParameterJpa();
    pfs.setStartIndex(0);
    pfs.setMaxResults(1);

    // get the full text query from index utility (note must be escaped due to ~
    // characters)
    FullTextQuery fullTextQuery =
        IndexUtility.applyPfsToLuceneQuery(TreePositionJpa.class,
            TreePositionJpa.class, sb.toString(), pfs, manager, true);

    int results = fullTextQuery.getResultSize();

    return results > 0;

  }
  
  private void sortTreePositions(List<TreePosition> treePositions) {
	  
	  if (treePositions != null && treePositions.size() > 0) {
		  
		final String terminology = treePositions.get(0).getTerminology();

	    // check for roman numerals and sort if found
		boolean nonRomanFound = false;
	    for (final TreePosition treepos : treePositions) {
	      if (!ConfigUtility
	          .isRomanNumeral(treepos.getTerminologyId())) {
	        nonRomanFound = true;
	        break;
	      }
	    }
	    if (!nonRomanFound) {
	      Collections.sort(treePositions, new Comparator<TreePosition>() {
	        @Override
	        public int compare(TreePosition o1, TreePosition o2) {
	          try {
	            return ConfigUtility.toArabic(o1.getTerminologyId())
	                - ConfigUtility.toArabic(o2.getTerminologyId());
	          } catch (Exception e) {
	            // just return zero, don't worry about handling the error
	            e.printStackTrace();
	            return 0;
	          }
	        }

	      });
	    } else {
	    	
	    	// explicit check for ICD terminologies -- sort by terminology id
	    	if (terminology.toLowerCase().startsWith("icd")) {
	    		Collections.sort(treePositions, new Comparator<TreePosition>() {
					@Override
					public int compare(TreePosition o1, TreePosition o2) {
						return o1.getTerminologyId().compareTo(o2.getTerminologyId());
					}
			
				});
	    	} 
	    	
	    	// otherwise, sort by name
	    	else {
		    	
				Collections.sort(treePositions, new Comparator<TreePosition>() {
					@Override
					public int compare(TreePosition o1, TreePosition o2) {
						return o1.getDefaultPreferredName().compareTo(o2.getDefaultPreferredName());
					}
			
				});
	    	}
	        
	    }
	  }
  }

}
