package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;

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
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.AbstractComponent;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

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
	 * Returns the concept for id.
	 * 
	 * @param id the id
	 * @return the concept for id
	 */
	public Concept getConceptForId(Long id) {
		return getConcept(id);

	}
	
	public List<Concept> getConceptsLimited(int n_concepts) {
		manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select terminologyId from ConceptJpa c");
	
	
		try {
			query.setMaxResults(n_concepts);
			
			List<String> concept_ids = (List<String>) query.getResultList();
			
			List<Concept> concepts = new ArrayList<Concept>();
			
			for (String concept_id : concept_ids) {
				Concept c = new ConceptJpa();
				c.setTerminologyId(concept_id);
				concepts.add(c);
			}
			
			
			System.out.println("Returning " + Integer.toString(concept_ids.size()) + " concept ids");
			return concepts;
			
		} catch (Exception e) {
			System.out.println("Could not retrieve limited number of concepts");
			return null;
			// TODO Auto-generated stub
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getConcept(java.lang.Long)
	 */
	@Override
	public Concept getConcept(Long conceptId) {
		manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		
		/*
		 * Try to retrieve the single expected result
		 * If zero or more than one result are returned, log error and set result to null
		 */

		String terminology = "SNOMEDCT";
		String terminologyVersion = "20130131";
		try {
			
			query.setParameter("terminologyId", conceptId.toString());
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			System.out.println("Returning cid... " + ((c != null) ? c.getTerminologyId().toString() : "null"));
			return c;
			
		} catch (NoResultException e) {
			System.out.println("Concept query for terminologyId = " + conceptId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned no results!");
			return null;		
		} catch (NonUniqueResultException e) {
			System.out.println("Concept query for terminologyId = " + conceptId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned multiple results!");
			return null;
		}	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ContentService#getConcepts(java.lang.String
	 * )
	 */
	@Override
	public List<Concept> getConcepts(String searchString) {
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
			// index field is indicated in the URL with a ':' separating field and value
			} else {
				QueryParser queryParser =
						new QueryParser(Version.LUCENE_36, "summary",
								searchFactory.getAnalyzer(ConceptJpa.class));
				luceneQuery = queryParser.parse(searchString);
			}

				FullTextQuery fullTextQuery =
						fullTextEntityManager.createFullTextQuery(luceneQuery);
				List<AbstractComponent> results = fullTextQuery.getResultList();
				List<Concept> components = new ArrayList<Concept>();
				for (AbstractComponent s : results) {
					if (s instanceof ConceptJpa) {
						//components.add(new SearchResultJpa(((ConceptJpa) s).getId(), ((ConceptJpa) s).getDefaultPreferredName()));
					} else if (s instanceof DescriptionJpa) {
						//components.add(new SearchResultJpa(((DescriptionJpa) s).getId(), ((DescriptionJpa) s).getTerm()));
					}
				}
				
				return components;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fullTextEntityManager != null) {
				fullTextEntityManager.close();
			}
			fullTextEntityManager = null;
		}
		return null;
	}

	
}
