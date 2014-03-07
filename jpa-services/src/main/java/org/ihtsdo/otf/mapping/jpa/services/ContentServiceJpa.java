package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
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
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * The Content Services for the Jpa model.
 */
public class ContentServiceJpa implements ContentService {

  /** The factory. */
  private static EntityManagerFactory factory;

  /** The manager. */
  private EntityManager manager;

  /** The indexed field names. */
  private static Set<String> fieldNames;
  
	/** The transaction per operation. */
	private boolean transactionPerOperation = true;

	/** The transaction entity. */
	private EntityTransaction tx;

  /**
   * Instantiates an empty {@link ContentServiceJpa}.
   */
  public ContentServiceJpa() {

    // created once or if the factory has closed
    if (factory == null || !factory.isOpen()) {
      Logger.getLogger(this.getClass()).info(
          "Setting content service entity manager factory.");
      factory = Persistence.createEntityManagerFactory("MappingServiceDS");
    }

    // create on each instantiation
    manager = factory.createEntityManager();

    if (fieldNames == null) {
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

      fullTextEntityManager.close();

      // closing fullTextEntityManager closes manager as well, recreate
      manager = factory.createEntityManager();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ContentService#close()
   */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
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
    Concept c = manager.find(ConceptJpa.class, conceptId);
    return c;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Concept getConcept(String terminologyId, String terminology,
    String terminologyVersion) {
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
      return c;
    } catch (NoResultException e) {
      // log result and return null
      Logger.getLogger(this.getClass()).warn(
          "ContentService.getConcept(): Concept query for terminologyId = "
              + terminologyId + ", terminology = " + terminology
              + ", terminologyVersion = " + terminologyVersion
              + " returned no results!");
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
  public SearchResultList findConcepts(String searchString,
    PfsParameter pfsParameter) throws Exception {

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
          fullTextEntityManager.createFullTextQuery(luceneQuery,
              ConceptJpa.class);

      @SuppressWarnings("unchecked")
      List<Concept> concepts = fullTextQuery.getResultList();

      System.out.println("Found " + Integer.toString(concepts.size())
          + " concepts for query");

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

      results.sortSearchResultsById();
      
      // closing fullTextEntityManager closes manager as well, recreate
      manager = factory.createEntityManager();
      
      return results;
    } catch (Exception e) {

      throw e;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SearchResultList findAllConcepts(String terminology,
    String terminologyVersion) {
    javax.persistence.Query query =
        manager
            .createQuery("select c.id, c.terminologyId, c.defaultPreferredName from ConceptJpa c where terminologyVersion = :terminologyVersion and terminology = :terminology");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    SearchResultList searchResultList = new SearchResultListJpa();
    for (Object result : query.getResultList()) {
      Object[] values = (Object[]) result;
      SearchResult searchResult = new SearchResultJpa();
      searchResult.setId(Long.parseLong(values[0].toString()));
      searchResult.setTerminologyId(values[1].toString());
      searchResult.setTerminology(terminology);
      searchResult.setTerminologyVersion(terminologyVersion);
      searchResult.setValue(values[2].toString());
      searchResultList.addSearchResult(searchResult);
    }
    return searchResultList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.Long)
   */
  @Override
  public SearchResultList findDescendants(String terminologyId,
    String terminology, String terminologyVersion, Long typeId) {

    // convert concept set to search results
    SearchResultList results = new SearchResultListJpa();
    Iterator<Concept> conceptSet_iter =
        getDescendants(terminologyId, terminology, terminologyVersion, typeId)
            .iterator();

    while (conceptSet_iter.hasNext()) {

      Concept c = conceptSet_iter.next();
      SearchResult s = new SearchResultJpa();

      s.setId(c.getId());
      s.setTerminology(c.getTerminology());
      s.setTerminologyVersion(c.getTerminologyVersion());
      s.setTerminologyId(c.getTerminologyId());
      s.setValue(c.getDefaultPreferredName());

      results.addSearchResult(s);
    }

    return results;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.Long)
   */
  @Override
  public Set<Concept> getDescendants(String terminologyId, String terminology,
    String terminologyVersion, Long typeId) {

    Queue<Concept> conceptQueue = new LinkedList<Concept>();
    Set<Concept> conceptSet = new HashSet<Concept>();

    // get the concept and add it as first element of concept list
    Concept rootConcept =
        getConcept(terminologyId, terminology, terminologyVersion);

    // if non-null result, seed the queue with this concept
    if (rootConcept != null) {
      conceptQueue.add(rootConcept);
    }

    // while concepts remain to be checked
    while (!conceptQueue.isEmpty()) {

      // retrieve this concept
      Concept c = conceptQueue.poll();
			

      // if concept is active
      if (c.isActive()) {

				// if concept is already in set, it has already been processed
				//if (!conceptSet.contains(c)) {

          // relationship set and iterator
          Set<Relationship> inv_relationships = c.getInverseRelationships();
          Iterator<Relationship> it_inv_rel = inv_relationships.iterator();
		

          // iterate over inverse relationships
          while (it_inv_rel.hasNext()) {

            // get relationship
            Relationship rel = it_inv_rel.next();

            // if relationship is active, typeId equals the provided typeId, and
            // the source concept is active
            if (rel.isActive() && rel.getTypeId().equals(typeId)
                && rel.getSourceConcept().isActive()) {

              // get source concept from inverse relationship (i.e. child of
              // concept)
              Concept c_rel = rel.getSourceConcept();

							// if set does not contain the source concept, add it to set and
							// queue
							if (!conceptSet.contains(c_rel)) {
								conceptSet.add(c_rel);
								conceptQueue.add(c_rel);
							}
						} 
					}
				//} 
			} 
		}

    return conceptSet;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.Long)
   */
  @Override
  public SearchResultList findChildren(String terminologyId,
    String terminology, String terminologyVersion, Long typeId) {

    SearchResultList children = new SearchResultListJpa();

    // get the concept and add it as first element of concept list
    Concept concept =
        getConcept(terminologyId, terminology, terminologyVersion);

    // if no concept, return empty list
    if (concept == null) {
      return children;
    }

    // cycle over relationships
    for (Relationship rel : concept.getInverseRelationships()) {

      if (rel.isActive() && rel.getTypeId().equals(typeId)
          && rel.getSourceConcept().isActive()) {

        Concept c = rel.getSourceConcept();

        SearchResult sr = new SearchResultJpa();
        sr.setId(c.getId());
        sr.setTerminologyId(c.getTerminologyId());
        sr.setTerminology(c.getTerminology());
        sr.setTerminologyVersion(c.getTerminologyVersion());
        sr.setValue(c.getDefaultPreferredName());

        // add search result to list
        children.addSearchResult(sr);
      }
    }

    return children;
  }

	@Override
	public SearchResultList getTreePositionsForConcept(String conceptId,
		String terminology, String terminologyVersion) {
	    javax.persistence.Query query =
	        manager
	            .createQuery("select tp.id, tp.ancestorPath from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and conceptId = :conceptId");
	    query.setParameter("terminology", terminology);
	    query.setParameter("terminologyVersion", terminologyVersion);
	    query.setParameter("conceptId", conceptId);
	    SearchResultList searchResultList = new SearchResultListJpa();
	    for (Object result : query.getResultList()) {
	      Object[] values = (Object[]) result;
	      SearchResult searchResult = new SearchResultJpa();
	      searchResult.setId(Long.parseLong(values[0].toString()));
	      searchResult.setTerminologyId(conceptId);
	      searchResult.setTerminology(terminology);
	      searchResult.setTerminologyVersion(terminologyVersion);
	      searchResult.setValue(values[1].toString());
	      searchResultList.addSearchResult(searchResult);
	    }
	    return searchResultList;
	}

	@Override
	public SearchResultList getDescendantTreePositionsForConcept(
		String terminologyId, String terminology, String terminologyVersion) {
    javax.persistence.Query query =
        manager
            .createQuery("select tp.id, tp.ancestorPath, tp.conceptId from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    SearchResultList searchResultList = new SearchResultListJpa();
    for (Object result : query.getResultList()) {
      Object[] values = (Object[]) result;
    	String ancestorPath = values[1].toString();
    	if (ancestorPath.contains(terminologyId)) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setId(Long.parseLong(values[0].toString()));
        searchResult.setTerminologyId(values[2].toString());
        searchResult.setTerminology(terminology);
        searchResult.setTerminologyVersion(terminologyVersion);
        searchResult.setValue(values[1].toString());
        searchResultList.addSearchResult(searchResult);
    	}
    }
    return searchResultList;
	}

	@Override
	public void clearTreePositions(String terminology, String terminologyVersion) {
		
		javax.persistence.Query query =
				manager
						.createQuery("DELETE From TreePositionJpa tp where terminology = :terminology and terminologyVersion = :terminologyVersion");
		query.setParameter("terminology", terminology);
		query.setParameter("terminologyVersion", terminologyVersion);
		
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			query.executeUpdate();
			tx.commit();
		} else {
			query.executeUpdate();
		}

		
	}

  
	@Override
	public Set<TreePosition> computeTreePositions(String terminology,
		String terminologyVersion, String typeId, String rootId) {
		
    Queue<Map<Concept, TreePosition>> conceptQueue = new LinkedList<Map<Concept, TreePosition>>();
    Set<Map<Concept, TreePosition>> conceptSet = new HashSet<Map<Concept, TreePosition>>();
    Set<TreePosition> tpSet = new HashSet<TreePosition>();

    // get the concept and add it as first element of concept list
    Concept rootConcept =
        getConcept(rootId, terminology, terminologyVersion);

    // if non-null result, seed the queue with this concept
    if (rootConcept != null) {
    	Map<Concept, TreePosition> hm = new HashMap<Concept, TreePosition>();
    	TreePosition rootTp = new TreePositionJpa("");
    	rootTp.setTerminology(terminology);
    	rootTp.setTerminologyVersion(terminologyVersion);
    	rootTp.setConceptId(rootConcept.getTerminologyId());
    	tpSet.add(rootTp);
    	hm.put(rootConcept, rootTp);
      conceptQueue.add(hm);
    }

    // while concepts remain to be checked
    while (!conceptQueue.isEmpty()) {

      // retrieve this concept
    	Map<Concept, TreePosition> currentMap = conceptQueue.poll();
      Concept currentConcept = currentMap.keySet().iterator().next();
      TreePosition currentTp = currentMap.get(currentConcept);
			

      // if concept is active
      if (currentConcept.isActive()) {

          // relationship set and iterator
          Set<Relationship> inv_relationships = currentConcept.getInverseRelationships();
          Iterator<Relationship> it_inv_rel = inv_relationships.iterator();
		
          // iterate over inverse relationships (for each child)
          while (it_inv_rel.hasNext()) {

            // get relationship
            Relationship rel = it_inv_rel.next();

            // if relationship is active, typeId equals the provided typeId, and
            // the source concept is active
            if (rel.isActive() && rel.getTypeId().toString().equals(typeId)
                && rel.getSourceConcept().isActive()) {

              // get source concept from inverse relationship (i.e. child of
              // concept)
              Concept c_rel = rel.getSourceConcept();

              TreePosition tp = new TreePositionJpa();
              if (currentTp.getAncestorPath().equals(""))
              	tp.setAncestorPath(currentTp.getConceptId());
              else
                tp.setAncestorPath(currentTp.getAncestorPath() + "~" + currentTp.getConceptId());
              tp.setTerminology(terminology);
              tp.setTerminologyVersion(terminologyVersion);
              tp.setConceptId(c_rel.getTerminologyId());
              tpSet.add(tp);
              
							// if set does not contain the source concept, add it to set and
							// queue
              boolean setContainsChild = false;
              for (Map<Concept, TreePosition> map : conceptSet) {
              	if (map.containsKey(c_rel)) {
              		setContainsChild = true;
              		break;
              	}
              }
							if (!setContainsChild) {
								Map<Concept, TreePosition> lhm = new HashMap<Concept, TreePosition>();
								lhm.put(c_rel, tp);
								conceptSet.add(lhm);
								conceptQueue.add(lhm);
							}
						} 
					} // after iterating over children
			} 
      
    }
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			for (TreePosition pos : tpSet) {
			  manager.persist(pos);
			}
			tx.commit();
		} else {
			for (TreePosition pos : tpSet) {
			  manager.persist(pos);
			}
		}
    return tpSet;
		
	}

	@Override
	public boolean getTransactionPerOperation() {
		return transactionPerOperation;
	}
}
