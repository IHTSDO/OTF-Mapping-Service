package org.ihtsdo.otf.mapping.jpa.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.ConceptListJpa;
import org.ihtsdo.otf.mapping.helpers.DescriptionList;
import org.ihtsdo.otf.mapping.helpers.DescriptionListJpa;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberList;
import org.ihtsdo.otf.mapping.helpers.LanguageRefSetMemberListJpa;
import org.ihtsdo.otf.mapping.helpers.LocalException;
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
import org.ihtsdo.otf.mapping.services.MetadataService;

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

	/**
	 * Instantiates an empty {@link ContentServiceJpa}.
	 * 
	 * @throws Exception
	 */
	public ContentServiceJpa() throws Exception {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.ContentService#close()
	 */

	@SuppressWarnings("unchecked")
	@Override
	public ConceptList getConcepts() throws Exception {
		List<Concept> m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from ConceptJpa m");

		m = query.getResultList();
		ConceptListJpa ConceptList = new ConceptListJpa();
		ConceptList.setConcepts(m);
		ConceptList.setTotalCount(m.size());
		return ConceptList;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getConcept(java.lang.Long)
	 */
	@Override
	public Concept getConcept(Long id) throws Exception {
		Concept c = manager.find(ConceptJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concept getConcept(String terminologyId, String terminology,
			String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			Concept c = (Concept) query.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException( "Concept query for terminologyId = " +
			 * terminologyId + ", terminology = " + terminology +
			 * ", terminologyVersion = " + terminologyVersion +
			 * " returned no results!", e);
			 */
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ConceptList getAllConcepts(String terminology,
			String terminologyVersion) {
		javax.persistence.Query query = manager
				.createQuery("select c from ConceptJpa c where terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			List<Concept> concepts = (List<Concept>) query.getResultList();
			ConceptList conceptList = new ConceptListJpa();
			conceptList.setConcepts(concepts);
			return conceptList;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException( "Concept query for terminologyId = " +
			 * terminologyId + ", terminology = " + terminology +
			 * ", terminologyVersion = " + terminologyVersion +
			 * " returned no results!", e);
			 */
		}
	}

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

	@Override
	public void removeTreePosition(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this concept
		TreePosition tp = manager.find(TreePositionJpa.class, id);

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

	@Override
	public void removeConcept(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this concept
		Concept mu = manager.find(ConceptJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getDescription(java.lang
	 * .Long)
	 */
	@Override
	public Description getDescription(Long id) throws Exception {
		Description c = manager.find(DescriptionJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Description getDescription(String terminologyId, String terminology,
			String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from DescriptionJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {

			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			Description c = (Description) query.getSingleResult();
			return c;
		} catch (NoResultException e) {
			Logger.getLogger(ContentServiceJpa.class).warn(
					"Could not retrieve description " + terminologyId
							+ ", terminology = " + terminology
							+ ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			return null;

		}
	}

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

	@Override
	public void removeDescription(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this description
		Description mu = manager.find(DescriptionJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getRelationship(java.lang
	 * .Long)
	 */
	@Override
	public Relationship getRelationship(Long id) throws Exception {
		Relationship c = manager.find(RelationshipJpa.class, id);
		return c;
	}

	@Override
	public Long getRelationshipId(String terminologyId, String terminology,
			String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery(
						"select r.id from RelationshipJpa r where terminologyId=:terminologyId and terminology=:terminology and terminologyVersion=:terminologyVersion")
				.setParameter("terminologyId", terminologyId)
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion);

		try {
			Long relationshipId = (Long) query.getSingleResult();
			return relationshipId;
		} catch (NoResultException e) {
			Logger.getLogger(ContentServiceJpa.class).info(
					"Could not find relationship id for" + terminologyId
							+ " for terminology " + terminology
							+ " and version " + terminologyVersion);
			return null;
		} catch (Exception e) {
			Logger.getLogger(ContentServiceJpa.class).info(
					"Unexpected exception retrieving relationship id for"
							+ terminologyId + " for terminology " + terminology
							+ " and version " + terminologyVersion);
			return null;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Relationship getRelationship(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from RelationshipJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			Relationship c = (Relationship) query.getSingleResult();
			return c;
		} catch (Exception e) {

			Logger.getLogger(ContentServiceJpa.class).info(
					"Relationship query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology
							+ ", terminologyVersion = " + terminologyVersion
							+ " threw an exception!");
			return null;
		}
	}

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

	@Override
	public void removeRelationship(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this relationship
		Relationship mu = manager.find(RelationshipJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getAttributeValueRefSetMember
	 * (java.lang.Long)
	 */
	@Override
	public AttributeValueRefSetMember getAttributeValueRefSetMember(Long id)
			throws Exception {
		AttributeValueRefSetMember c = manager.find(
				AttributeValueRefSetMemberJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AttributeValueRefSetMember getAttributeValueRefSetMember(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from AttributeValueRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			AttributeValueRefSetMember c = (AttributeValueRefSetMember) query
					.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException(
			 * "AttributeValueRefSetMember query for terminologyId = " +
			 * terminologyId + ", terminology = " + terminology +
			 * ", terminologyVersion = " + terminologyVersion +
			 * " returned no results!", e);
			 */
		}
	}

	@Override
	public AttributeValueRefSetMember addAttributeValueRefSetMember(
			AttributeValueRefSetMember attributeValueRefSetMember)
			throws Exception {
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

	@Override
	public void updateAttributeValueRefSetMember(
			AttributeValueRefSetMember attributeValueRefSetMember)
			throws Exception {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(attributeValueRefSetMember);
			tx.commit();
		} else {
			manager.merge(attributeValueRefSetMember);
		}

	}

	@Override
	public void removeAttributeValueRefSetMember(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this map specialist
		AttributeValueRefSetMember mu = manager.find(
				AttributeValueRefSetMemberJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getComplexMapRefSetMember
	 * (java.lang.Long)
	 */
	@Override
	public ComplexMapRefSetMember getComplexMapRefSetMember(Long id)
			throws Exception {
		ComplexMapRefSetMember c = manager.find(
				ComplexMapRefSetMemberJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ComplexMapRefSetMember getComplexMapRefSetMember(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from ComplexMapRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			ComplexMapRefSetMember c = (ComplexMapRefSetMember) query
					.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException(
			 * "ComplexMapRefSetMember query for terminologyId = " +
			 * terminologyId + ", terminology = " + terminology +
			 * ", terminologyVersion = " + terminologyVersion +
			 * " returned no results!", e);
			 */
		}
	}

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

	@Override
	public void removeComplexMapRefSetMember(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this complex map ref set member
		ComplexMapRefSetMember mu = manager.find(
				ComplexMapRefSetMemberJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getLanguageRefSetMember
	 * (java.lang.Long)
	 */
	@Override
	public LanguageRefSetMember getLanguageRefSetMember(Long id)
			throws Exception {
		LanguageRefSetMember c = manager
				.find(LanguageRefSetMemberJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LanguageRefSetMember getLanguageRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from LanguageRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			LanguageRefSetMember c = (LanguageRefSetMember) query
					.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException(
			 * "LanguageRefSetMember query for terminologyId = " + terminologyId
			 * + ", terminology = " + terminology + ", terminologyVersion = " +
			 * terminologyVersion + " returned no results!", e);
			 */
		}
	}

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

	@Override
	public void removeLanguageRefSetMember(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this language ref set member
		LanguageRefSetMember mu = manager.find(LanguageRefSetMemberJpa.class,
				id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getSimpleMapRefSetMember
	 * (java.lang.Long)
	 */
	@Override
	public SimpleMapRefSetMember getSimpleMapRefSetMember(Long id)
			throws Exception {
		SimpleMapRefSetMember c = manager.find(SimpleMapRefSetMemberJpa.class,
				id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SimpleMapRefSetMember getSimpleMapRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from SimpleMapRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			SimpleMapRefSetMember c = (SimpleMapRefSetMember) query
					.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException(
			 * "SimpleMapRefSetMember query for terminologyId = " +
			 * terminologyId + ", terminology = " + terminology +
			 * ", terminologyVersion = " + terminologyVersion +
			 * " returned no results!", e);
			 */
		}
	}

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

	@Override
	public void removeSimpleMapRefSetMember(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this simple map ref set member
		SimpleMapRefSetMember mu = manager.find(SimpleMapRefSetMemberJpa.class,
				id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getSimpleRefSetMember(
	 * java.lang.Long)
	 */
	@Override
	public SimpleRefSetMember getSimpleRefSetMember(Long id) throws Exception {
		SimpleRefSetMember c = manager.find(SimpleRefSetMemberJpa.class, id);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SimpleRefSetMember getSimpleRefSetMember(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {
		javax.persistence.Query query = manager
				.createQuery("select c from SimpleRefSetMemberJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);
			SimpleRefSetMember c = (SimpleRefSetMember) query.getSingleResult();
			return c;
		} catch (NoResultException e) {
			return null;
			/*
			 * throw new LocalException(
			 * "SimpleRefSetMember query for terminologyId = " + terminologyId +
			 * ", terminology = " + terminology + ", terminologyVersion = " +
			 * terminologyVersion + " returned no results!", e);
			 */
		}
	}

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

	@Override
	public void removeSimpleRefSetMember(Long id) throws Exception {

		tx = manager.getTransaction();

		// retrieve this simple ref set member
		SimpleRefSetMember mu = manager.find(SimpleRefSetMemberJpa.class, id);

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#findConcepts(java.lang.
	 * String )
	 */
	@Override
	public SearchResultList findConceptsForQuery(String searchString,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList results = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);
		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		try {

			// if the field is not indicated in the URL
			if (searchString.indexOf(':') == -1) {
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
						Version.LUCENE_36, fieldNames.toArray(new String[0]),
						searchFactory.getAnalyzer(ConceptJpa.class));
				queryParser.setAllowLeadingWildcard(false);
				luceneQuery = queryParser.parse(searchString);
				// index field is indicated in the URL with a ':' separating
				// field and value
			} else {
				QueryParser queryParser = new QueryParser(Version.LUCENE_36,
						"summary", searchFactory.getAnalyzer(ConceptJpa.class));
				luceneQuery = queryParser.parse(searchString);
			}
		} catch (ParseException e) {
			throw new LocalException(
					"The specified search terms cannot be parsed.  Please check syntax and try again.");
		}

		FullTextQuery fullTextQuery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, ConceptJpa.class);

		// set paging/filtering/sorting if indicated
		if (pfsParameter != null) {

			// if start index and max results are set, set paging
			if (pfsParameter.getStartIndex() != -1
					&& pfsParameter.getMaxResults() != -1) {
				fullTextQuery.setFirstResult(pfsParameter.getStartIndex());
				fullTextQuery.setMaxResults(pfsParameter.getMaxResults());
			}

			// if sort field is specified, set sort key
			if (pfsParameter.getSortField() != null
					&& !pfsParameter.getSortField().isEmpty()) {

				// check that specified sort field exists on Concept and is
				// a string
				if (Concept.class.getDeclaredField(pfsParameter.getSortField())
						.getType().equals(String.class)) {
					fullTextQuery.setSort(new Sort(new SortField(pfsParameter
							.getSortField(), SortField.STRING)));

				} else {
					throw new Exception(
							"Concept query specified a field that does not exist or is not a string");
				}

			}

		}

		// execute the query
		@SuppressWarnings("unchecked")
		List<Concept> concepts = fullTextQuery.getResultList();

		// construct the search results
		for (Concept c : concepts) {
			SearchResult sr = new SearchResultJpa();
			sr.setId(c.getId());
			sr.setTerminologyId(c.getTerminologyId());
			sr.setTerminology(c.getTerminology());
			sr.setTerminologyVersion(c.getTerminologyVersion());
			sr.setValue(c.getDefaultPreferredName());
			results.addSearchResult(sr);
		}

		fullTextEntityManager.close();

		// closing fullTextEntityManager closes manager as well, recreate
		manager = factory.createEntityManager();

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(java.lang
	 * .String, java.lang.String, java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findDescendantConcepts(String terminologyId,
			String terminology, String terminologyVersion,
			PfsParameter pfsParameter) throws Exception {

		Logger.getLogger(ContentServiceJpa.class).info(
				"findDescendantConcepts called: " + terminologyId + ", "
						+ terminology + ", " + terminologyVersion);

		SearchResultList searchResultList = new SearchResultListJpa();

		javax.persistence.Query query = manager
				.createQuery("select tp.ancestorPath from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId");
		query.setParameter("terminology", terminology);
		query.setParameter("terminologyVersion", terminologyVersion);
		query.setParameter("terminologyId", terminologyId);

		// get the first tree position
		List<String> ancestorPaths = query.getResultList();

		// skip construction if no ancestor path was found
		if (ancestorPaths.size() != 0) {

			String ancestorPath = ancestorPaths.get(0);

			// insert string to actually add this concept to the ancestor path
			ancestorPath += "~" + terminologyId;

			// construct query for descendants
			query = manager.createQuery("select distinct c "
					+ "from TreePositionJpa tp, ConceptJpa c "
					+ "where tp.terminologyId = c.terminologyId "
					+ "and tp.terminology = c.terminology "
					+ "and tp.terminologyVersion = c.terminologyVersion "
					+ "and tp.ancestorPath like '" + ancestorPath + "%'");

			List<Concept> concepts = query.getResultList();

			// set the total count of descendant concepts
			searchResultList.setTotalCount(concepts.size());

			// apply paging, and sorting if appropriate
			if (pfsParameter != null
					&& (pfsParameter.getSortField() != null && !pfsParameter
							.getSortField().isEmpty())) {
				// check that specified sort field exists on Concept and is
				// a string
				final Field sortField = Concept.class
						.getDeclaredField(pfsParameter.getSortField());
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
							return ((String) sortField.get(c1))
									.compareTo((String) sortField.get(c2));
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
				toIndex = Math.min(concepts.size(),
						startIndex + pfsParameter.getMaxResults());
			}

			// construct the search results
			for (Concept c : concepts.subList(startIndex, toIndex)) {
				SearchResult searchResult = new SearchResultJpa();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#clearTreePositions(java.
	 * lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void clearTreePositions(String terminology, String terminologyVersion)
			throws Exception {

		Logger.getLogger(this.getClass()).info(
				"Removing tree positions via object-management for "
						+ terminology + ", " + terminologyVersion);

		// if currently in transaction-per-operation mode, temporarily set to
		// false
		boolean currentTransactionStrategy = getTransactionPerOperation();
		if (getTransactionPerOperation()) {
			this.setTransactionPerOperation(false);
		}

		int results = 0; // progress tracker
		int commitSize = 5000; // retrieval/delete batch size

		javax.persistence.Query query = manager
				.createQuery("select tp from TreePositionJpa tp where terminology = :terminology and terminologyVersion = :terminologyVersion");
		query.setParameter("terminology", terminology);
		query.setParameter("terminologyVersion", terminologyVersion);

		query.setFirstResult(0);
		query.setMaxResults(commitSize);

		boolean resultsFound = true;

		while (resultsFound) {

			List<TreePosition> treePositions = query.getResultList();

			if (treePositions.size() == 0)
				resultsFound = false;

			this.beginTransaction();
			for (TreePosition tp : treePositions) {
				this.removeTreePosition(tp.getId());
			}
			this.commit();

			results += commitSize;

			Logger.getLogger(this.getClass()).info(
					"  " + results + " tree positions deleted");
		}
		;

		Logger.getLogger(this.getClass()).info(
				"Finished:  deleted " + results + " tree positions");

		// set the transaction strategy based on status starting this routine
		setTransactionPerOperation(currentTransactionStrategy);

	}

	@Override
	public void computeTreePositions(String terminology,
			String terminologyVersion, String typeId, String rootId)
			throws Exception {
		Logger.getLogger(this.getClass()).info(
				"Starting computeTreePositions - " + rootId + ", "
						+ terminology + ", isaRelTypeId = " + typeId);

		// initialize global variables
		EntityTransaction computeTreePositionTransaction = manager
				.getTransaction();
		int computeTreePositionCommitCt = 5000;
		computeTreePositionTotalCount = 0;
		computeTreePositionMaxMemoryUsage = 0L;
		computeTreePositionLastTime = System.currentTimeMillis();

		// System.setOut(new PrintStream(new
		// FileOutputStream("C:/Users/Patrick/Documents/WCI/Working Notes/TreePositionRuns/computeTreePositions_"
		// + System.currentTimeMillis() + ".txt")));
		// // System.out.println("ComputeTreePositions run for " +(new
		// Date()).toString());

		// get the root concept
		Concept rootConcept = getConcept(rootId, terminology,
				terminologyVersion);

		// begin the transaction
		computeTreePositionTransaction.begin();

		// begin the recursive computation
		computeTreePositionsHelper(rootConcept, typeId, "",
				computeTreePositionCommitCt, computeTreePositionTransaction);

		// commit any remaining tree positions
		computeTreePositionTransaction.commit();

		Runtime runtime = Runtime.getRuntime();
		Logger.getLogger(this.getClass()).info(
				"Final Tree Positions: " + computeTreePositionTotalCount
						+ ", MEMORY USAGE: " + runtime.totalMemory());

	}

	/**
	 * Recursive function to create tree positions and compute
	 * children/descendant count at each tree position.
	 * 
	 * NOTE: This function is designed to keep as little Concept information in
	 * storage as possible. See inline notes.
	 * 
	 * @param terminologyId
	 * @param terminology
	 * @param terminologyVersion
	 * @param typeId
	 * @param ancestorPath
	 * @return tree positions at this level
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private Set<Long> computeTreePositionsHelper(Concept concept,
			String typeId, String ancestorPath,
			int computeTreePositionCommitCt,
			EntityTransaction computeTreePositionTransaction) throws Exception {

		Set<Long> descConceptIds = new HashSet<>();

		// if concept is active
		if (concept.isActive()) {

			// instantiate the tree position
			TreePosition tp = new TreePositionJpa();

			// logging information
			int ancestorCount = ancestorPath.length()
					- ancestorPath.replaceAll("~", "").length();
			String loggerPrefix = "";
			for (int i = 0; i < ancestorCount; i++)
				loggerPrefix += "  ";

			// Logger.get// Logger(ContentServiceJpa.class).info(loggerPrefix +
			// "Computing position for concept " + concept.getTerminologyId() +
			// ", " + concept.getDefaultPreferredName());;

			tp.setAncestorPath(ancestorPath);
			tp.setTerminology(concept.getTerminology());
			tp.setTerminologyVersion(concept.getTerminologyVersion());
			tp.setTerminologyId(concept.getTerminologyId());
			tp.setDefaultPreferredName(concept.getDefaultPreferredName());

			// persist the tree position
			manager.persist(tp);

			// construct the ancestor path terminating at this concept
			String conceptPath = (ancestorPath.equals("") ? concept
					.getTerminologyId() : ancestorPath + "~"
					+ concept.getTerminologyId());

			// construct the list of terminology ids representing valid children
			Set<Long> childrenConceptIds = new HashSet<>();

			// cycle over all relationships
			for (Relationship rel : concept.getInverseRelationships()) {

				// if relationship is active, typeId equals the provided typeId,
				// and
				// the source concept is active
				if (rel.isActive() && rel.getTypeId().toString().equals(typeId)
						&& rel.getSourceConcept().isActive()) {

					// Logger.get//
					// Logger(ContentServiceJpa.class).info(loggerPrefix +
					// "  Relationship " + rel.getTerminologyId() +
					// " active, matches typeId, source concept active");

					// get the child concept
					Concept childConcept = rel.getSourceConcept();

					// add this terminology id to the set of children
					childrenConceptIds.add(childConcept.getId());

					// add this terminology id to the set of descendants
					descConceptIds.add(childConcept.getId());
				}
			}

			// Logger.get// Logger(ContentServiceJpa.class).info(loggerPrefix +
			// " " + childrenConceptIds.size() + " children");

			// iterate over the child terminology ids
			// this iteration is entirely local and depends on no managed
			// objects
			for (Long childConceptId : childrenConceptIds) {

				// call helper function on child concept
				// add the results to the local descendant set
				descConceptIds.addAll(computeTreePositionsHelper(
						getConcept(childConceptId), typeId, conceptPath,
						computeTreePositionCommitCt,
						computeTreePositionTransaction));

			}

			// set the children count
			tp.setChildrenCount(childrenConceptIds.size());

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
				float elapsedTime = System.currentTimeMillis()
						- computeTreePositionLastTime;
				elapsedTime = elapsedTime / 1000;
				computeTreePositionLastTime = System.currentTimeMillis();

				if (runtime.totalMemory() > computeTreePositionMaxMemoryUsage)
					computeTreePositionMaxMemoryUsage = runtime.totalMemory();

				Logger.getLogger(ContentServiceJpa.class)
						.info("\t"
								+ System.currentTimeMillis()
								/ 1000
								+ "\t"
								+ computeTreePositionTotalCount
								+ "\t"
								+ Math.floor(runtime.totalMemory() / 1024 / 1024)
								+ "\t"
								+ Double.toString(computeTreePositionCommitCt
										/ elapsedTime));

				/*
				 * // System.out.println( "*** Tree Positions: " +
				 * computeTreePositionGlobalCount + ", Current memory usage: " +
				 * Math.floor(runtime.totalMemory() / 1024 / 1024) +
				 * "MB, Commit interval: " + "s, Average speed: " +
				 * Double.toString(computeTreePositionCommitCt / elapsedTime) +
				 * " tree positisions / s");
				 */

			}
		}

		// return the descendant concept set
		// note that the local child and descendant set will be garbage
		// collected
		return descConceptIds;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.ContentService#
	 * getRootTreePositionsForTerminology(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TreePositionList getRootTreePositions(String terminology,
			String terminologyVersion) throws Exception {
		List<TreePosition> treePositions = manager
				.createQuery(
						"select tp from TreePositionJpa tp where ancestorPath = '' and terminologyVersion = :terminologyVersion and terminology = :terminology")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion)
				.getResultList();
		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(treePositions);
		treePositionList.setTotalCount(treePositions.size());
		return treePositionList;
	}

	@SuppressWarnings("unchecked")
	private TreePositionList getChildTreePositions(TreePosition treePosition)
			throws Exception {
		List<TreePosition> treePositions = manager
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
		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(treePositions);
		treePositionList.setTotalCount(treePositions.size());
		return treePositionList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getTreePositions(java.
	 * lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public TreePositionList getTreePositions(String terminologyId,
			String terminology, String terminologyVersion) throws Exception {

		@SuppressWarnings("unchecked")
		List<TreePosition> treePositions = manager
				.createQuery(
						"select tp from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion)
				.setParameter("terminologyId", terminologyId).getResultList();
		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(treePositions);
		treePositionList.setTotalCount(treePositions.size());

		return treePositionList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getLocalTrees(java.lang.
	 * String, java.lang.String, java.lang.String)
	 */
	@Override
	public TreePositionList getTreePositionsWithDescendants(
			String terminologyId, String terminology, String terminologyVersion)
			throws Exception {
		// get tree positions for concept (may be multiple)
		@SuppressWarnings("unchecked")
		List<TreePosition> treePositions = manager
				.createQuery(
						"select tp from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion)
				.setParameter("terminologyId", terminologyId).getResultList();
		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(treePositions);
		treePositionList.setTotalCount(treePositions.size());

		// for each tree position
		for (TreePosition treePosition : treePositionList.getTreePositions()) {

			// if this tree position has children
			if (treePosition.getChildrenCount() > 0) {

				// retrieve the children
				treePosition.setChildren(getChildTreePositions(treePosition)
						.getTreePositions());
			}
		}
		treePositionList.setTotalCount(treePositionList.getTreePositions()
				.size());
		return treePositionList;
	}

	/**
	 * Given a local tree position, returns the root tree with this tree
	 * position as terminal child
	 * 
	 * 
	 * @param treePosition
	 *            the tree position
	 * @return the root tree position for concept
	 * @throws Exception
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
			for (TreePosition tp : getTreePositions(ancestors[i],
					treePosition.getTerminology(),
					treePosition.getTerminologyVersion()).getTreePositions()) {

				// check if this ancestor path matches the beginning of the
				// original
				// tree position's ancestor path
				if (treePosition.getAncestorPath().startsWith(
						tp.getAncestorPath())) {
					ancestorTreePositions.add(tp);
					ancestorFound = true;
				}
			}

			if (ancestors[i].length() != 0 && ancestorFound == false) {
				throw new Exception("Ancestor tree position " + ancestors[i]
						+ " not found!");
			}
		}

		// the returned full (root) tree position
		TreePosition rootTreePosition = treePosition;

		// if all tree positions to root have been found, construct the final
		// tree
		// position
		for (TreePosition tp : ancestorTreePositions) {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.ContentService#getTreePositionsQuery
	 * (java.lang.String, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TreePositionList getTreePositionGraphForQuery(String terminology,
			String terminologyVersion, String query) throws Exception {

		// construct the query
		String full_query = constructTreePositionQuery(terminology,
				terminologyVersion, query);

		Logger.getLogger(ContentServiceJpa.class).info(
				"Full query: " + full_query);

		// execute the full text query
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		try {

			QueryParser queryParser = new QueryParser(Version.LUCENE_36,
					"summary", searchFactory.getAnalyzer(TreePositionJpa.class));
			luceneQuery = queryParser.parse(full_query);

		} catch (ParseException e) {
			throw new LocalException(
					"The specified search terms cannot be parsed.  Please check syntax and try again.");
		}

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TreePositionJpa.class);

		// retrieve the query results
		List<TreePosition> queriedTreePositions = ftquery.getResultList();

		// initialize the result set
		List<TreePosition> fullTreePositions = new ArrayList<>();

		Logger.getLogger(ContentServiceJpa.class).info(
				"Found " + queriedTreePositions.size() + " results:");
		for (TreePosition queriedTreePosition : queriedTreePositions) {
			Logger.getLogger(ContentServiceJpa.class).info(
					queriedTreePosition.toString());
		}

		// for each query result, construct the full tree (i.e. up to root, and
		// including children if exact match)
		for (TreePosition queriedTreePosition : queriedTreePositions) {

			// if the query is an exact match for the terminology id of this
			// tree position, attach children
			if (queriedTreePosition.getTerminologyId().toUpperCase()
					.equals(query.toUpperCase())) {
				queriedTreePosition.setChildren(getChildTreePositions(
						queriedTreePosition).getTreePositions());

			}

			TreePosition fullTreePosition = constructRootTreePosition(queriedTreePosition);

			Logger.getLogger(ContentServiceJpa.class).info(
					"Checking root " + fullTreePosition.getTerminologyId());

			// if this root is already present in the final list, add this
			// position's
			// children to existing root
			if (fullTreePositions.contains(fullTreePosition)) {

				TreePosition existingTreePosition = fullTreePositions
						.get(fullTreePositions.indexOf(fullTreePosition));

				Logger.getLogger(ContentServiceJpa.class).info(
						"Found existing root at position "
								+ fullTreePositions.indexOf(fullTreePosition)
								+ " with "
								+ existingTreePosition.getChildren().size());

				/*
				 * existingTreePosition
				 * .addChildren(fullTreePosition.getChildren());
				 * 
				 * Logger.getLogger(ContentServiceJpa.class).info( "  Added " +
				 * fullTreePosition.getChildren().size() + " children:");
				 */
				for (TreePosition tp : fullTreePosition.getChildren()) {
					Logger.getLogger(ContentServiceJpa.class).info(
							tp.getTerminologyId());
				}

				fullTreePositions.set(
						fullTreePositions.indexOf(fullTreePosition),
						existingTreePosition);

				// otherwise, add this root
			} else {
				fullTreePositions.add(fullTreePosition);
			}
		}

		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(fullTreePositions);
		treePositionList.setTotalCount(fullTreePositions.size());
		return treePositionList;
	}

	@Override
	public void computeTreePositionInformation(TreePositionList tpList)
			throws Exception {

		// if results are found, retrieve metadata and compute information
		if (tpList.getCount() > 0) {

			String terminology = tpList.getTreePositions().get(0)
					.getTerminology();
			String terminologyVersion = tpList.getTreePositions().get(0)
					.getTerminologyVersion();

			MetadataService metadataService = new MetadataServiceJpa();
			Map<String, String> descTypes = metadataService
					.getDescriptionTypes(terminology, terminologyVersion);
			Map<String, String> relTypes = metadataService
					.getRelationshipTypes(terminology, terminologyVersion);

			for (TreePosition tp : tpList.getTreePositions())
				computeTreePositionInformationHelper(tp, descTypes, relTypes);
		}

	}

	/**
	 * Helper function to recursively calculate the displayed information for a
	 * tree position graph - description groups (e.g. inclusions, exclusions,
	 * etc) - concept labels (e.g. code ranges) attached to
	 * inclusions/exclusions
	 * 
	 * Algorithm: (1) get Concept for tree position (2) Cycle over all
	 * Descriptions (3) For each description, cycle over all relationships (4)
	 * If relationship terminology id starts with the description terminology
	 * id, this is something to render (5) ... update this later
	 * 
	 * Data Structure: TreePosition->DescriptionGroups: each description group
	 * is a description type, e.g. Inclusion, Exclusion, etc.
	 * DescriptionGroups->Description: each description is a concept preferred
	 * name and a set of referenced concepts ReferencedConcept: each referenced
	 * concept is a display name and the terminology id of an existing concept
	 * to link to
	 * 
	 * @param treePosition
	 * @return
	 * @throws Exception
	 */
	private TreePosition computeTreePositionInformationHelper(
			TreePosition treePosition, Map<String, String> descTypes,
			Map<String, String> relTypes) throws Exception {

		// System.out.println("");
		// System.out.println("***************************");
		// System.out.println("Computing information for tree position, concept: "
		// + treePosition.getTerminologyId());
		// System.out.println("***************************");
		// get the concept for this tree position
		Concept concept = getConcept(treePosition.getTerminologyId(),
				treePosition.getTerminology(),
				treePosition.getTerminologyVersion());

		// map of Type -> Description Groups
		// e.g. there "Inclusion" -> all inclusion description groups
		Map<String, TreePositionDescriptionGroup> descGroups = new HashMap<>();

		// cycle over all descriptions
		for (Description desc : concept.getDescriptions()) {

			// System.out.println("  Checking description: "
			// + desc.getTerminologyId() + ", " + desc.getTypeId() + ", "
			// + desc.getTerm());

			String descType = desc.getTypeId().toString();

			// get or create the description group for this description type
			TreePositionDescriptionGroup descGroup = null;
			if (descGroups.get(descType) != null)
				descGroup = descGroups.get(descType);
			else {
				// System.out.println("    Creating descGroup:  "
				// + descTypes.get(descType));
				descGroup = new TreePositionDescriptionGroupJpa();
				descGroup.setName(descTypes.get(descType));
				descGroup.setTypeId(descType);
			}

			// get or create the tree position description for this description
			// term
			TreePositionDescription tpDesc = null;
			for (TreePositionDescription tpd : descGroup
					.getTreePositionDescriptions()) {
				if (tpd.getName().equals(desc.getTerm()))
					tpDesc = tpd;
			}

			// if no description found, create a new one
			if (tpDesc == null) {
				// System.out.println("    Creating tpDesc:  " +
				// desc.getTerm());
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
			for (Relationship rel : concept.getRelationships()) {

				// System.out.println("  Checking relationship "
				// + rel.getTerminologyId() + ", " + rel.getTypeId());

				if (rel.getTerminologyId().startsWith(
						desc.getTerminologyId() + "~")) {

					// System.out.println("     Matches!");

					// Non-persisted objects, so remove this description from
					// list, modify it, and re-add it
					descGroup.removeTreePositionDescription(tpDesc);

					// create the referenced concept object
					TreePositionReferencedConcept referencedConcept = new TreePositionReferencedConceptJpa();
					referencedConcept.setTerminologyId(rel
							.getDestinationConcept().getTerminologyId());

					String displayName = (rel.getLabel() == null ? rel
							.getDestinationConcept().getTerminologyId() : // if
																			// no
																			// label,
																			// just
																			// use
																			// terminology
																			// id
							rel.getLabel()); // if label present, use label as
												// display name

					// System.out.println("      Destination Concept: "
					// + rel.getDestinationConcept().getTerminologyId()
					// + " with label "
					// + rel.getDestinationConcept().getLabel());

					// switch on relationship type to add any additional
					// information
					String relType = relTypes.get(rel.getTypeId().toString());

					// System.out.println("      Relationship type: "
					// + rel.getTypeId().toString() + ", "
					// + relTypes.get(rel.getTypeId().toString()));

					// if asterisk-to-dagger, add †
					if (relType.indexOf("Asterisk") == 0) {
						// System.out.println("           ASTERISK");
						displayName += " *";
					} else if (relType.indexOf("Dagger") == 0) {
						// System.out.println("           DAGGER");
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

		treePosition.setDescGroups(new ArrayList<TreePositionDescriptionGroup>(
				descGroups.values()));

		// calculate information for all children
		if (treePosition.getChildrenCount() > 0) {
			for (TreePosition tp : treePosition.getChildren()) {
				computeTreePositionInformationHelper(tp, descTypes, relTypes);
			}
		}

		return treePosition;
	}

	/**
	 * Helper function for map record query construction using both fielded
	 * terms and unfielded terms.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param query
	 *            the query
	 * @return the full lucene query text
	 */
	private static String constructTreePositionQuery(String terminology,
			String terminologyVersion, String query) throws Exception {

		String full_query;

		// if no filter supplied, return query based on map project id only
		if (query == null || query.equals("")) {
			full_query = "terminology:" + terminology
					+ " AND terminologyVersion:" + terminologyVersion;
			return full_query;
		}

		// Pre-treatment: Find any lower-case boolean operators and set to
		// uppercase

		// //////////////////
		// Basic algorithm:
		//
		// 1) add whitespace breaks to operators
		// 2) split query on whitespace
		// 3) cycle over terms in split query to find quoted material, add each
		// term/quoted term to parsed terms\
		// a) special case: quoted term after a :
		// 3) cycle over terms in parsed terms
		// a) if an operator/parantheses, pass through unchanged (send to upper
		// case
		// for boolean)
		// b) if a fielded query (i.e. field:value), pass through unchanged
		// c) if not, construct query on all fields with this term

		// list of escape terms (i.e. quotes, operators) to be fed into query
		// untouched
		String escapeTerms = "\\+|\\-|\"|\\(|\\)";
		String booleanTerms = "and|AND|or|OR|not|NOT";

		// first cycle over the string to add artificial breaks before and after
		// control characters
		final String queryStr = query;

		// pad the beginning to ensure capture of dash character
		// terminology ids may contain terms like D55-D59, which should be
		// preserved whole
		// but we still want to capture lucene negation term, e.g. -D55
		String queryStr_mod = queryStr;

		queryStr_mod = queryStr_mod.replace("(", " ( ");
		queryStr_mod = queryStr_mod.replace(")", " ) ");
		queryStr_mod = queryStr_mod.replace("\"", " \" ");
		queryStr_mod = queryStr_mod.replace("+", " + ");
		queryStr_mod = queryStr_mod.replace(" -", " - "); // note extra space on
															// this term, see
															// above

		// remove any leading or trailing whitespace (otherwise first/last null
		// term
		// bug)
		queryStr_mod = queryStr_mod.trim();

		// split the string by white space and single-character operators
		String[] terms = queryStr_mod.split("\\s+");

		// merge items between quotation marks
		boolean exprInQuotes = false;
		List<String> parsedTerms = new ArrayList<>();
		// List<String> parsedTerms_temp = new ArrayList<String>();
		String currentTerm = "";

		// cycle over terms to identify quoted (i.e. non-parsed) terms
		for (int i = 0; i < terms.length; i++) {

			// if an open quote is detected
			if (terms[i].equals("\"")) {

				if (exprInQuotes == true) {

					// special case check: fielded term. Impossible for first
					// term to be
					// fielded.
					if (parsedTerms.size() == 0) {
						parsedTerms.add("\"" + currentTerm + "\"");
					} else {
						String lastParsedTerm = parsedTerms.get(parsedTerms
								.size() - 1);

						// if last parsed term ended with a colon, append this
						// term to the
						// last parsed term
						if (lastParsedTerm.endsWith(":") == true) {
							parsedTerms.set(parsedTerms.size() - 1,
									lastParsedTerm + "\"" + currentTerm + "\"");
						} else {
							parsedTerms.add("\"" + currentTerm + "\"");
						}
					}

					// reset current term
					currentTerm = "";
					exprInQuotes = false;

				} else {
					exprInQuotes = true;
				}

				// if no quote detected
			} else {

				// if inside quotes, continue building term
				if (exprInQuotes == true) {
					currentTerm = currentTerm == "" ? terms[i] : currentTerm
							+ " " + terms[i];

					// otherwise, add to parsed list
				} else {
					parsedTerms.add(terms[i]);
				}
			}
		}

		for (String s : parsedTerms) {
			Logger.getLogger(ContentServiceJpa.class).debug("  " + s);
		}

		// cycle over terms to construct query
		full_query = "";

		for (int i = 0; i < parsedTerms.size(); i++) {

			// if not the first term AND the last term was not an escape term
			// add whitespace separator
			if (i != 0 && !parsedTerms.get(i - 1).matches(escapeTerms)) {

				full_query += " ";
			}
			/*
			 * full_query += (i == 0 ? // check for first term "" : // -> if
			 * first character, add nothing
			 * parsedTerms.get(i-1).matches(escapeTerms) ? // check if last term
			 * was an escape character "": // -> if last term was an escape
			 * character, add nothing " "); // -> otherwise, add a separating
			 * space
			 */

			// if an escape character/sequence, add this term unmodified
			if (parsedTerms.get(i).matches(escapeTerms)) {

				full_query += parsedTerms.get(i);

				// else if a boolean character, add this term in upper-case form
				// (i.e.
				// lucene format)
			} else if (parsedTerms.get(i).matches(booleanTerms)) {

				full_query += parsedTerms.get(i).toUpperCase();

				// else if already a field-specific query term, add this term
				// unmodified
			} else if (parsedTerms.get(i).contains(":")) {

				full_query += parsedTerms.get(i);

				// otherwise, treat as unfielded query term
			} else {

				// open parenthetical term
				full_query += "(";

				// add fielded query for each indexed term, separated by OR
				Iterator<String> names_iter = treePositionFieldNames.iterator();
				while (names_iter.hasNext()) {

					String fieldName = names_iter.next();
					Logger.getLogger(ContentServiceJpa.class).info(
							"  field name: " + fieldName);

					full_query += fieldName + ":" + parsedTerms.get(i);
					if (names_iter.hasNext())
						full_query += " OR ";
				}

				// close parenthetical term
				full_query += ")";
			}

			// if further terms remain in the sequence
			if (!(i == parsedTerms.size() - 1)) {

				// Add a separating OR iff:
				// - this term is not an escape character
				// - this term is not a boolean term
				// - next term is not a boolean term
				if (!parsedTerms.get(i).matches(escapeTerms)
						&& !parsedTerms.get(i).matches(booleanTerms)
						&& !parsedTerms.get(i + 1).matches(booleanTerms)) {

					full_query += " OR";
				}
			}
		}

		// add parantheses and map project constraint
		full_query = "(" + full_query + ")" + " AND terminology:" + terminology
				+ " AND terminologyVersion:" + terminologyVersion;

		Logger.getLogger(ContentServiceJpa.class).debug(
				"Full query: " + full_query);

		return full_query;

	}

	@SuppressWarnings("unchecked")
	@Override
	public ConceptList getConceptsModifiedSinceDate(String terminology,
			Date date, PfsParameter pfsParameter) throws Exception {
		ConceptList results = new ConceptListJpa();

		javax.persistence.Query query;

		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();

		// if no date provided, get the latest modified concepts
		query = manager.createQuery(
				"select max(c.effectiveTime) from ConceptJpa c"
						+ " where terminology = :terminology").setParameter(
				"terminology", terminology);

		Date tempDate = (Date) query.getSingleResult();
		System.out.println("Max date   = " + tempDate.toString());

		if (date == null) {
			date = tempDate;

		} else {
			System.out.println("Date input = " + date.toString());
		}

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

		QueryBuilder qb = searchFactory.buildQueryBuilder()
				.forEntity(ConceptJpa.class).get();

		Query luceneQuery;
		if (pfsParameter.getQueryRestriction() == null) {
			luceneQuery = qb
					.bool()
					.must(qb.keyword().onField("effectiveTime").matching(date)
							.createQuery())
					.must(qb.keyword().onField("terminology")
							.matching(terminology).createQuery()).createQuery();
		} else {
			luceneQuery = qb
					.bool()
					.must(qb.keyword().onField("effectiveTime").matching(date)
							.createQuery())
					.must(qb.keyword().onField("terminology")
							.matching(terminology).createQuery())
					.must(qb.keyword()
							.onFields("terminologyId", "defaultPreferredName")
							.matching(pfsParameter.getQueryRestriction())
							.createQuery()).createQuery();

		}
		Logger.getLogger(ContentServiceJpa.class).info(
				"Query text: " + luceneQuery.toString());

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, ConceptJpa.class);

		if (pfsParameter.getStartIndex() != -1
				&& pfsParameter.getMaxResults() != -1) {
			ftquery.setFirstResult(pfsParameter.getStartIndex());
			ftquery.setMaxResults(pfsParameter.getMaxResults());

		}
		results.setTotalCount(ftquery.getResultSize());
		results.setConcepts(ftquery.getResultList());
		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DescriptionList getDescriptionsModifiedSinceDate(String terminology,
			Date date) {
		DescriptionList results = new DescriptionListJpa();

		javax.persistence.Query query = manager
				.createQuery(
						"select d from DescriptionJpa d"
								+ " where effectiveTime >= :releaseDate"
								+ " and terminology = :terminology")
				.setParameter("releaseDate", date)
				.setParameter("terminology", terminology);

		results.setDescriptions(query.getResultList());
		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RelationshipList getRelationshipsModifiedSinceDate(
			String terminology, Date date) {
		RelationshipList results = new RelationshipListJpa();

		javax.persistence.Query query = manager
				.createQuery(
						"select r from RelationshipJpa r"
								+ " where effectiveTime >= :releaseDate"
								+ " and terminology = :terminology")
				.setParameter("releaseDate", date)
				.setParameter("terminology", terminology);

		results.setRelationships(query.getResultList());

		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LanguageRefSetMemberList getLanguageRefSetMembersModifiedSinceDate(
			String terminology, Date date) {
		LanguageRefSetMemberList results = new LanguageRefSetMemberListJpa();

		javax.persistence.Query query = manager
				.createQuery(
						"select l from LanguageRefSetMemberJpa l"
								+ " where effectiveTime >= :releaseDate"
								+ " and terminology = :terminology")
				.setParameter("releaseDate", date)
				.setParameter("terminology", terminology);

		results.setLanguageRefSetMembers(query.getResultList());

		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getAllRelationshipTerminologyIds(String terminology,
			String terminologyVersion) {
		javax.persistence.Query query = manager
				.createQuery(
						"select c.terminologyId from RelationshipJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion);

		List<String> terminologyIds = query.getResultList();
		Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
		return terminologyIdSet;

	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getAllDescriptionTerminologyIds(String terminology,
			String terminologyVersion) {
		javax.persistence.Query query = manager
				.createQuery(
						"select c.terminologyId from DescriptionJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion);

		List<String> terminologyIds = query.getResultList();
		Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
		return terminologyIdSet;

	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> getAllLanguageRefSetMemberTerminologyIds(
			String terminology, String terminologyVersion) {
		javax.persistence.Query query = manager
				.createQuery(
						"select c.terminologyId from LanguageRefSetMemberJpa c where terminology=:terminology and terminologyVersion=:terminologyVersion")
				.setParameter("terminology", terminology)
				.setParameter("terminologyVersion", terminologyVersion);

		List<String> terminologyIds = query.getResultList();
		Set<String> terminologyIdSet = new HashSet<>(terminologyIds);
		return terminologyIdSet;

	}

}
