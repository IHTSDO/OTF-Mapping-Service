package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
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
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.SearchResult;
import org.ihtsdo.otf.mapping.services.SearchResultList;

/**
 * The Content Services for the Jpa model
 */
public class ContentServiceJpa implements ContentService {

	/** The factory. */
	private EntityManagerFactory factory;

	/** The manager. */
	private EntityManager manager;

	/** The indexed field names. */
	private Set<String> fieldNames;

	/**
	 * Instantiates an empty {@link ContentServiceJpa}.
	 */
	public ContentServiceJpa() {
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		fieldNames = new HashSet<String>();

		FullTextEntityManager fullTextEntityManager =
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
		System.out.println("ended init " + fieldNames.toString());
	}

	/**
	 * Retrieves a limited number of concepts FOR TESTING PURPOSES ONLY
	 * @param n_concepts the number of concepts
	 * @return the list of concepts
	 */
	public List<Concept> getConceptsLimited(int n_concepts) {
		manager = factory.createEntityManager();
		javax.persistence.Query query =
				manager.createQuery("select terminologyId from ConceptJpa c");

		query.setMaxResults(n_concepts);

		List<String> concept_ids = (List<String>) query.getResultList();

		List<Concept> concepts = new ArrayList<Concept>();

		for (String concept_id : concept_ids) {
			Concept c = new ConceptJpa();
			c.setTerminologyId(concept_id);
			concepts.add(c);
		}

		System.out.println("Returning " + Integer.toString(concept_ids.size())
				+ " concept ids");
		return concepts;

	}

	@Override
	public Concept getConcept(Long conceptId) {
		manager = factory.createEntityManager();
		return manager.find(ConceptJpa.class, conceptId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Concept getConcept(Long conceptId, String terminology,
		String terminologyVersion) {
		manager = factory.createEntityManager();
		javax.persistence.Query query =
				manager
						.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */

		try {

			query.setParameter("terminologyId", conceptId.toString());
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			System.out.println("Returning cid... "
					+ ((c != null) ? c.getTerminologyId().toString() : "null"));
			return c;

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Concept query for terminologyId = " + conceptId + ", terminology = "
							+ terminology + ", terminologyVersion = " + terminologyVersion
							+ " returned no results!");
			return null;
		}
	}

	public void getDescendants(Long conceptId, String terminology,
		String terminologyVersion, Set<Concept> descendantResultSet) {
		ContentService contentService = new ContentServiceJpa();
	  Concept concept = contentService.getConcept(conceptId, terminology, terminologyVersion);
	  getDescendants(concept, terminology, terminologyVersion, descendantResultSet);		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(org.ihtsdo
	 * .otf.mapping.rf2.Concept, java.lang.String, java.lang.String,
	 * java.util.Set)
	 */
	@Override
	public void getDescendants(Concept concept, String terminology,
		String terminologyVersion, Set<Concept> descendantResultSet) {
		manager = factory.createEntityManager();
		Set<Concept> children = new HashSet<Concept>();

		for (Relationship rel : concept.getInverseRelationships()) {
			System.out.println("Child of " + concept.getTerminologyId() + " is "
					+ rel.getSourceConcept().getTerminologyId());
			if (rel.getTypeId().toString().equals("116680003") && rel.isActive() && rel.getSourceConcept().isActive()) {
			  children.add(rel.getSourceConcept());
			}
		}
		for (Concept child : children) {
			if (child.getTerminology().equals(terminology)
					&& child.getTerminologyVersion().equals(terminologyVersion)) {
				descendantResultSet.add(child);
				getDescendants(child, terminology, terminologyVersion,
						descendantResultSet);
			}
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

		manager = factory.createEntityManager();
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
					fullTextEntityManager.createFullTextQuery(luceneQuery,
							ConceptJpa.class);

			List<Concept> concepts = fullTextQuery.getResultList();

			System.out.println("Found " + Integer.toString(concepts.size())
					+ " concepts for query");

			for (Concept c : concepts) {
				results.addSearchResult(new SearchResultJpa(c.getId(), c
						.getTerminologyId(), c.getDefaultPreferredName()));
			}
			
			results.sortSearchResultsById();

			return results;
		} catch (Exception e) {
			throw e;
		} finally {
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
	 * @return
	 */
	public SearchResultList getDescendants(Long terminologyId, String terminology, String terminologyVersion, Long typeId) {
		
		
		Queue<Concept> concepts = new LinkedList<Concept>();
		SearchResultList results= new SearchResultListJpa();
		
		// get the concept and add it as first element of concept list
		concepts.add(getConcept(terminologyId, terminology, terminologyVersion));
		
		while (!concepts.isEmpty()) {
			
			// retrieve this concept
			Concept c = concepts.poll();
			
				if (c.isActive()) {
				
				// relationship sets
				Set<Relationship> relationships = c.getRelationships();
				Set<Relationship> inv_relationships = c.getInverseRelationships();
				
				// iterators for relationship sets
				Iterator<Relationship> it_inv_rel = inv_relationships.iterator();
				
				// iterate over inverse relationships
				while (it_inv_rel.hasNext()) {
					
					// get relationship
					Relationship rel = it_inv_rel.next();
					
					if (rel.isActive() && rel.getTypeId().equals(typeId)) {
						
						// get destination concept
						Concept c_rel = rel.getSourceConcept();
						
						// construct search result
						SearchResult searchResult= new SearchResultJpa(c_rel.getId(), c_rel.getTerminologyId(), c_rel.getDefaultPreferredName());					
						
						// if concept list does not contain this concept, add result to list and concept to queue
						if (!results.contains(searchResult)) {
							results.addSearchResult(searchResult);
							concepts.add(c_rel);
						}
					}
				}
			}
		}
		
		results.sortSearchResultsById();
		
		return results;
	}
	
	//////////////////////////////////////////
	// Other Services
	//////////////////////////////////////////
	
	public SearchResultList findUnmappedDescendants(MapProject mapProject) {
		
		SearchResultList results = new SearchResultListJpa();
	
	return results;
	}


}
