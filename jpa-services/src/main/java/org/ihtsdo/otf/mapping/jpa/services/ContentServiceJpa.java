package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * The Content Services for the Jpa model
 */
public class ContentServiceJpa implements ContentService {

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The manager. */
	private EntityManager manager;

	/** The indexed field names. */
	private Set<String> fieldNames;
	
	private FullTextEntityManager fullTextEntityManager;

	/**
	 * Instantiates an empty {@link ContentServiceJpa}.
	 */
	public ContentServiceJpa() {
		
		// created once
		if (factory == null) {
			factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		}
		
		// create on each instantiation
		manager = factory.createEntityManager();
		
		if (fieldNames == null) {
			
			fieldNames = new HashSet<String>();
	
			fullTextEntityManager =
					org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
			IndexReaderAccessor indexReaderAccessor =
					fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
			Set<String> indexedClassNames =
					fullTextEntityManager.getSearchFactory().getStatistics()
							.getIndexedClassNames();
			for (String indexClass : indexedClassNames) {
				IndexReader indexReader = indexReaderAccessor.open(indexClass);
				try {
					for (FieldInfo info : ReaderUtil.getMergedFieldInfos(indexReader)) {
						fieldNames.add(info.name);
					}
				} finally {
					indexReaderAccessor.close(indexReader);
				}
			}

		}
	}
	
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) { manager.close(); }
	}

		
	@Override
	public Concept getConcept(Long conceptId) {
		
		Concept c = manager.find(ConceptJpa.class, conceptId);
		if (manager.isOpen()) { manager.close(); }
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concept getConcept(String terminologyId, String terminology, String terminologyVersion) {
		
		javax.persistence.Query query =
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

			Concept c = (Concept) query.getSingleResult();

			System.out.println("Returning cid... "
					+ ((c != null) ? c.getTerminologyId().toString() : "null"));
		
			//if (manager.isOpen()) { manager.close(); }
			return c;

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Concept query for terminologyId = " + terminologyId + ", terminology = "
							+ terminology + ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.ContentService#findConcepts(java.lang.
	 * String )
	 */
	@Override
	public SearchResultList findConcepts(String searchString) throws Exception {
		
		SearchResultList results = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);
		try {
			SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
			Query luceneQuery;
			// if the field is not indicated in the URL
			if (searchString.indexOf(':') == -1) {
				MultiFieldQueryParser queryParser =
						new MultiFieldQueryParser(Version.LUCENE_36,
								fieldNames.toArray(new String[0]),
								searchFactory.getAnalyzer(ConceptJpa.class));
				queryParser.setAllowLeadingWildcard(false);
				luceneQuery = queryParser.parse(searchString);
				// index field is indicated in the URL with a ':' separating
				// field and value
			} else {
				QueryParser queryParser =
						new QueryParser(Version.LUCENE_36, "summary",
								searchFactory.getAnalyzer(ConceptJpa.class));
				luceneQuery = queryParser.parse(searchString);
			}

			FullTextQuery fullTextQuery =
					fullTextEntityManager.createFullTextQuery(luceneQuery, ConceptJpa.class);
			
			List<Concept> concepts = fullTextQuery.getResultList();
			
			System.out.println("Found " + Integer.toString(concepts.size()) + " concepts for query");
			
			for (Concept c : concepts) {
				results.addSearchResult(new SearchResultJpa(c.getId(), c.getTerminologyId(), c.getDefaultPreferredName()));
			}
			
			results.sortSearchResultsById();
			if (manager.isOpen()) { manager.close(); }

			return results;
		} catch (Exception e) {
			
			throw e;
		} finally {
			if (manager.isOpen()) { manager.close(); }
			if (fullTextEntityManager != null) {
				fullTextEntityManager.close();
			}
			fullTextEntityManager = null;
		}
	}
	
	/**
	 * Find descendant concepts through inverse relationships given a concept and typeId
	 * @param terminologyId
	 * @param terminology
	 * @param terminologyVersion
	 * @param typeId
	 * @return the set of concepts
	 */

	public Set<Concept> getDescendants(String terminologyId, String terminology, String terminologyVersion, Long typeId) {
		
		
		Queue<Concept> concept_queue = new LinkedList<Concept>();
		Set<Concept> concept_set = new HashSet<Concept>();
		
		// get the concept and add it as first element of concept list
		Concept rootConcept = getConcept(terminologyId, terminology, terminologyVersion);
		
		// if non-null result, seed the queue with this concept
		if (rootConcept != null) {
			concept_queue.add(rootConcept);
		}
		
		// while concepts remain to be checked
		while (!concept_queue.isEmpty()) {
			
			// retrieve this concept
			Concept c = concept_queue.poll();
			
			// if concept is active
			if (c.isActive()) {
				
				// if concept is already in set, it has already been processed
				if (!concept_set.contains(c)) {
				
					// relationship set and iterator
					Set<Relationship> inv_relationships = c.getInverseRelationships();
					Iterator<Relationship> it_inv_rel = inv_relationships.iterator();
						
					// iterate over inverse relationships
					while (it_inv_rel.hasNext()) {
						
						// get relationship
						Relationship rel = it_inv_rel.next();
					
						// if relationship is active, typeId equals the provided typeId, and the source concept is active
						if (rel.isActive() && rel.getTypeId().equals(typeId) && rel.getSourceConcept().isActive()) {
							
							// get source concept from inverse relationship (i.e. child of concept)
							Concept c_rel = rel.getSourceConcept();					
							
							// if set does not contain the source concept, add it to set and queue
							if (!concept_set.contains(c_rel)) {
								concept_set.add(c_rel);
								concept_queue.add(c_rel);
							}
						}
					}
				}
			}
		}
		
		return concept_set;
	}
	
	public List<Concept> getConceptsForRefSetId(Long refSetId, String terminology, String terminologyVersion) {
		
		
		javax.persistence.Query query =
				manager
						.createQuery("select c from ConceptJpa c where refSetId = :refSetId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */

		try {

			query.setParameter("refSetId", refSetId.toString());
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			List<Concept> cids = query.getResultList();
			
			if (manager.isOpen()) { manager.close(); }
			
			return cids;
			

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Concept query for refSetId = " + refSetId + ", terminology = "
							+ terminology + ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}
		
	}

}
