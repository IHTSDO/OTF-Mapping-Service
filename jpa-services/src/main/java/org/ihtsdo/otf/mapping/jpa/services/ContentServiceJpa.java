package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.util.Version;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.ihtsdo.otf.mapping.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;


public class ContentServiceJpa implements ContentService {
		
	/** The factory. */
	private EntityManagerFactory factory;
	
	/** The field names. */
	private static Set<String> fieldNames;
	
	private EntityManager manager;

	/**
	 * Instantiates an empty {@link ContentServiceJpa}.
	 */
	public ContentServiceJpa() {
		factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.ContentService#getConcept(java.lang.Long)
	 */
	@Override
	public Concept getConcept(Long conceptId) {
		manager = factory.createEntityManager();
		Concept cpt = manager.find(ConceptJpa.class, conceptId);
		manager.detach(cpt);
		manager.close();
		return cpt;
	}
	
	@Override
	public List<Concept> getConcepts(String searchString) {
		manager = factory.createEntityManager();
		/**FullTextQuery fullTextQuery = buildFullTextQuery(manager, searchString,
				new KeywordAnalyzer());
		List<Concept> resultList = fullTextQuery.getResultList();
		if (resultList.isEmpty()) {
			fullTextQuery = buildFullTextQuery(manager, searchString,
					new StandardAnalyzer(Version.LUCENE_30));
			return fullTextQuery.getResultList();
		}*/
		return test(searchString);
	}

  public List<Concept> test(String searchString) {
	FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
  try {
      // This will ensure that index for already inserted data is created.
      fullTextEntityManager.createIndexer().startAndWait();
      // Add some more record, lucene will index every new object inserted, removed or updated.
      // addMoreRecords(entityManager);
      // Search for Book
      QueryBuilder qb = fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(ConceptJpa.class).get();
      org.apache.lucene.search.Query query = qb.keyword().onFields("defaultPreferredName").matching(searchString).createQuery();
      Query jpaQuery = fullTextEntityManager.createFullTextQuery(query, ConceptJpa.class);
      System.out.println("query " + query.toString());
      
      // execute search
      List<Concept> bookResult = jpaQuery.getResultList();
       
      if (bookResult != null) {
          for (Concept mhsBookEntityBean : bookResult) {
              System.out.println("Book found = " + mhsBookEntityBean);
          }
      }
      return bookResult;
       
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

	/**
	 * Builds the full text query from the specified search options.
	 *
	 * @param em the entity manager
	 * @param searchOption the search options
	 * @param analyzer the analyzer
	 * @return the full text query
	
	private FullTextQuery buildFullTextQuery(EntityManager em,
			String searchString, Analyzer analyzer) {
		FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search
				.getFullTextEntityManager(em);
		org.apache.lucene.search.Query luceneQuery;

		if (fieldNames.isEmpty())
			init();
		List<String> fields = new LinkedList<String>(fieldNames);

		luceneQuery = buildLuceneQuery(fullTextEntityManager,
				searchString, fields.toArray(new String[0]),
				analyzer);
		FullTextQuery fullTextQuery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, ConceptJpa.class);

		System.out.println(luceneQuery.toString());

		return fullTextQuery;

	}  */
	
	/**
	 * Builds the Lucene query.
	 *
	 * @param fullTextEntityManager the full text entity manager
	 * @param words the words
	 * @param fields the fields
	 * @param analyzer the analyzer
	 * @return the org.apache.lucene.search. query
	
	@SuppressWarnings({
			"static-method"
	})
	private org.apache.lucene.search.Query buildLuceneQuery(
			FullTextEntityManager fullTextEntityManager, String words,
			String[] fields, Analyzer analyzer) {
		if (words == null || words.isEmpty())
			return new MatchAllDocsQuery();
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			if (words.indexOf(':') == -1) {
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
						Version.LUCENE_30, fields, analyzer);
				queryParser.setAllowLeadingWildcard(false);
				luceneQuery = queryParser.parse(words);
			} else {
				QueryParser queryParser = new QueryParser(Version.LUCENE_30,
						"summary", analyzer);
				luceneQuery = queryParser.parse(words);
			}
		} catch (org.apache.lucene.queryParser.ParseException e) {
			throw new IllegalArgumentException(
					e.getMessage(), e);
		}
		return luceneQuery;
	}   */
	
	/**
	 * Initializes the registry.

	@PostConstruct
	public void init() {
		fieldNames = new HashSet<String>();
		
		FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search
				.getFullTextEntityManager(manager);
		ReaderProvider readerProvider = fullTextEntityManager
				.getSearchFactory().getReaderProvider();
		for (Class<?> entity : ((SearchFactoryImplementor) fullTextEntityManager
				.getSearchFactory()).getDocumentBuildersIndexedEntities()
				.keySet()) {
			DirectoryProvider<?>[] directoryProviders = fullTextEntityManager
					.getSearchFactory().getDirectoryProviders(entity);
			IndexReader indexReader = readerProvider
					.openReader(directoryProviders);
			try {
				fieldNames.addAll(indexReader
						.getFieldNames(FieldOption.INDEXED));
			} finally {
				readerProvider.closeReader(indexReader);
			}
		}
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
		  currentUsername = ((UserDetails)principal).getUsername();
		} else {
		  currentUsername = principal.toString();
		}
		currentUsername = currentUsername.toLowerCase();
	}	 */
}
