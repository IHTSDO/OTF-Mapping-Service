package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * The Content Services for the Jpa model.
 */
public class ContentServiceJpa extends RootServiceJpa implements ContentService {

  /** The manager. */
  private EntityManager manager;

  /** The transaction per operation. */
  private boolean transactionPerOperation = true;

  /**
   * Instantiates an empty {@link ContentServiceJpa}.
   * @throws Exception
   */
  public ContentServiceJpa() throws Exception {
    super();

    // create on each instantiation
    manager = factory.createEntityManager();
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
  public Concept getConcept(Long terminologyId) throws Exception {
    Concept c = manager.find(ConceptJpa.class, terminologyId);
    return c;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Concept getConcept(String terminologyId, String terminology,
    String terminologyVersion) throws Exception {
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
      Logger.getLogger(this.getClass()).debug(
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
  public SearchResultList findConceptsByQuery(String searchString,
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

      // Sort by ID
      results.sortBy(new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          return o1.getId().compareTo(o2.getId());
        }
      });

      // closing fullTextEntityManager closes manager as well, recreate
      manager = factory.createEntityManager();

      return results;
    } catch (Exception e) {

      throw e;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getDescendants(java.lang
   * .String, java.lang.String, java.lang.String, java.lang.Long)
   */
  @Override
  public SearchResultList findDescendantConcepts(String terminologyId,
    String terminology, String terminologyVersion, PfsParameter pfsParameter) throws Exception {

    // TODO: make this respect the PFS parameter
    Map<String, String> terminologyIdToDefaultPns = new HashMap<>();
    Map<String, Long> terminologyIdToHibernateIds = new HashMap<>();
    SearchResultList searchResultList = new SearchResultListJpa();
    javax.persistence.Query query =
        manager
            .createQuery("select tp.ancestorPath from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    query.setParameter("terminologyId", terminologyId);
    for (Object result : query.getResultList()) {

      String ancestorPath = result.toString() + "~" + terminologyId;
      javax.persistence.Query query2 =
          manager
              .createQuery("select distinct b.id, a.terminologyId, b.defaultPreferredName "
                  + "From TreePositionJpa a, ConceptJpa b "
                  + "where a.terminologyId = b.terminologyId and a.terminology = b.terminology and"
                  + " a.terminologyVersion = b.terminologyVersion and "
                  + " a.ancestorPath like '" + ancestorPath + "%'");
      for (Object result2 : query2.getResultList()) {
        Object[] values2 = (Object[]) result2;
        terminologyIdToDefaultPns.put(values2[1].toString(),
            values2[2].toString());
        terminologyIdToHibernateIds.put(values2[1].toString(),
            Long.parseLong(values2[0].toString()));
      }
    }
    for (Map.Entry<String, String> entry : terminologyIdToDefaultPns.entrySet()) {
      String cid = entry.getKey();
      String pn = entry.getValue();
      SearchResult searchResult = new SearchResultJpa();
      searchResult.setId(terminologyIdToHibernateIds.get(cid));
      searchResult.setTerminologyId(cid);
      searchResult.setTerminology(terminology);
      searchResult.setTerminologyVersion(terminologyVersion);
      searchResult.setValue(pn);
      searchResultList.addSearchResult(searchResult);
    }

    searchResultList.setTotalCount(searchResultList.getCount());
    return searchResultList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#findTreePositionsForConcept
   * (java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public SearchResultList findTreePositions(String terminologyId,
    String terminology, String terminologyVersion) throws Exception {
    javax.persistence.Query query =
        manager
            .createQuery("select tp.id, tp.ancestorPath from TreePositionJpa tp where terminologyVersion = :terminologyVersion and terminology = :terminology and terminologyId = :terminologyId");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);
    query.setParameter("terminologyId", terminologyId);
    SearchResultList searchResultList = new SearchResultListJpa();
    for (Object result : query.getResultList()) {
      Object[] values = (Object[]) result;
      SearchResult searchResult = new SearchResultJpa();
      searchResult.setId(Long.parseLong(values[0].toString()));
      searchResult.setTerminologyId(terminologyId);
      searchResult.setTerminology(terminology);
      searchResult.setTerminologyVersion(terminologyVersion);
      searchResult.setValue(values[1].toString());
      searchResultList.addSearchResult(searchResult);
    }
    searchResultList.setTotalCount(searchResultList.getCount());
    return searchResultList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#clearTreePositions(java.
   * lang.String, java.lang.String)
   */
  @Override
  public void clearTreePositions(String terminology, String terminologyVersion)
    throws Exception {

    javax.persistence.Query query =
        manager
            .createQuery("DELETE From TreePositionJpa tp where terminology = :terminology and terminologyVersion = :terminologyVersion");
    query.setParameter("terminology", terminology);
    query.setParameter("terminologyVersion", terminologyVersion);

    int results = 0;
    if (getTransactionPerOperation()) {
      EntityTransaction tx = manager.getTransaction();
      tx.begin();
      results = query.executeUpdate();
      tx.commit();
    } else {
      results = query.executeUpdate();
    }
    Logger.getLogger(this.getClass()).info("  deleted " + results + " entries");

  }

  // instantiate map of terminology to child count
  int computeTreePositionGlobalCount;

  EntityTransaction computeTreePositionTransaction;

  Long computeTreePositionMaxMemoryUsage;

  Long computeTreePositionLastTime;

  int computeTreePositionCommitCt;

  @Override
  public void computeTreePositions(String terminology,
    String terminologyVersion, String typeId, String rootId) throws Exception {
    Logger.getLogger(this.getClass()).info(
        "Starting computeTreePositions - " + rootId + ", " + terminology);

    // initialize global variables
    computeTreePositionGlobalCount = 0;
    computeTreePositionTransaction = manager.getTransaction();
    computeTreePositionMaxMemoryUsage = 0L;
    computeTreePositionLastTime = System.currentTimeMillis();
    computeTreePositionCommitCt = 5000;

    // System.setOut(new PrintStream(new
    // FileOutputStream("C:/Users/Patrick/Documents/WCI/Working Notes/TreePositionRuns/computeTreePositions_"
    // + System.currentTimeMillis() + ".txt")));
    // System.out.println("ComputeTreePositions run for " +(new
    // Date()).toString());

    // get the root concept
    Concept rootConcept = getConcept(rootId, terminology, terminologyVersion);

    // begin the transaction
    computeTreePositionTransaction.begin();

    // begin the recursive computation
    computeTreePositionsHelper(rootConcept, typeId, ""); // initially empty
                                                         // ancestor path

    // commit any remaining tree positions
    computeTreePositionTransaction.commit();

    Runtime runtime = Runtime.getRuntime();
    Logger.getLogger(this.getClass()).info(
        "Final Tree Positions: " + computeTreePositionGlobalCount
            + ", MEMORY USAGE: " + runtime.totalMemory());

  }

  /**
   * Recursive function to create tree positions and compute children/descendant
   * count at each tree position.
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
  private Set<Long> computeTreePositionsHelper(Concept concept, String typeId,
    String ancestorPath) throws Exception {

    Set<Long> descConceptIds = new HashSet<>();

    // if concept is active
    if (concept.isActive()) {

      // instantiate the tree position
      TreePosition tp = new TreePositionJpa();

      // logging information
      int ancestorCount =
          ancestorPath.length() - ancestorPath.replaceAll("~", "").length();
      @SuppressWarnings("unused")
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

      // System.out.println(loggerPrefix +
      // "Creating tree position " + tp.toString());

      // construct the ancestor path terminating at this concept
      String conceptPath =
          (ancestorPath.equals("") ? concept.getTerminologyId() : ancestorPath
              + "~" + concept.getTerminologyId());

      // construct the list of terminology ids representing valid children
      Set<Long> childrenConceptIds = new HashSet<>();

      // cycle over all relationships
      for (Relationship rel : concept.getInverseRelationships()) {

        // if relationship is active, typeId equals the provided typeId, and
        // the source concept is active
        if (rel.isActive() && rel.getTypeId().toString().equals(typeId)
            && rel.getSourceConcept().isActive()) {

          // get the child concept
          Concept childConcept = rel.getSourceConcept();

          // add this terminology id to the set of children
          childrenConceptIds.add(childConcept.getId());

          // add this terminology id to the set of descendants
          descConceptIds.add(childConcept.getId());
        }
      }

      // iterate over the child terminology ids
      // this iteration is entirely local and depends on no managed objects
      for (Long childConceptId : childrenConceptIds) {

        // call helper function on child concept
        // add the results to the local descendant set
        descConceptIds.addAll(computeTreePositionsHelper(
            getConcept(childConceptId), typeId, conceptPath));

      }

      /*
       * System.out.println(loggerPrefix + " Calculation complete (" +
       * computeTreePositionGlobalCount + "): " + tp.toString());
       * System.out.println(loggerPrefix + " " + childrenTerminologyIds.size() +
       * " children, " + descendantConcepts.size() + " descendants");
       */
      // set the children count
      tp.setChildrenCount(childrenConceptIds.size());

      // set the descendant count
      tp.setDescendantCount(descConceptIds.size());

      // routinely commit and force clear the manager
      // any existing recursive threads are entirely dependent on local
      // variables
      if (++computeTreePositionGlobalCount % computeTreePositionCommitCt == 0) {

        // commit the transaction
        computeTreePositionTransaction.commit();

        // clear the manager
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
                + computeTreePositionGlobalCount + "\t"
                + Math.floor(runtime.totalMemory() / 1024 / 1024) + "\t"
                + Double.toString(computeTreePositionCommitCt / elapsedTime));

        /*
         * System.out.println( "*** Tree Positions: " +
         * computeTreePositionGlobalCount + ", Current memory usage: " +
         * Math.floor(runtime.totalMemory() / 1024 / 1024) +
         * "MB, Commit interval: " + "s, Average speed: " +
         * Double.toString(computeTreePositionCommitCt / elapsedTime) +
         * " tree positisions / s");
         */

      }
    }

    // return the descendant concept set
    // note that the local child and descendant set will be garbage collected
    return descConceptIds;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getTransactionPerOperation()
   */
  @Override
  public boolean getTransactionPerOperation() throws Exception {
    return transactionPerOperation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#setTransactionPerOperation
   * (boolean)
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception {
    this.transactionPerOperation = transactionPerOperation;
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
    List<TreePosition> treePositions =
        manager
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
    List<TreePosition> treePositions =
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
  public TreePositionList getTreePositions(String terminologyId,
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

    // for each tree position
    for (TreePosition treePosition : treePositionList.getTreePositions()) {

      // if this tree position has children
      if (treePosition.getChildrenCount() > 0) {

        // retrieve the children
        treePosition.setChildren(getChildTreePositions(treePosition)
            .getTreePositions());
      }
    }
    treePositionList.setTotalCount(treePositionList.getTreePositions().size());
    return treePositionList;
  }

  /**
   * Given a local tree position, returns the root tree with this tree position
   * as terminal child
   * 
   * 
   * @param treePosition the tree position
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

    // for each ancestor, get the tree position corresponding to the original
    // tree position's path
    for (int i = ancestors.length - 1; i > -1; i--) {

      // flag to ensure ancestor exists
      boolean ancestorFound = false;

      // cycle over the tree positions for this ancestor
      for (TreePosition tp : getTreePositions(ancestors[i],
          treePosition.getTerminology(), treePosition.getTerminologyVersion())
          .getTreePositions()) {

        // check if this ancestor path matches the beginning of the original
        // tree position's ancestor path
        if (treePosition.getAncestorPath().startsWith(tp.getAncestorPath())) {
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

    // if all tree positions to root have been found, construct the final tree
    // position
    for (TreePosition tp : ancestorTreePositions) {

      // if this persisted tree position does not have this id as a child, add
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
  public TreePositionList getTreePositionGraphByQuery(String terminology,
    String terminologyVersion, String query) throws Exception {

    // construct the query
    String full_query =
        constructTreePositionQuery(terminology, terminologyVersion, query);

    // execute the full text query
    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TreePositionJpa.class));
    luceneQuery = queryParser.parse(full_query);

    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TreePositionJpa.class);

    // retrieve the query results
    List<TreePosition> queriedTreePositions = ftquery.getResultList();

    // initialize the result set
    List<TreePosition> fullTreePositions = new ArrayList<>();

    Logger.getLogger(ContentServiceJpa.class).info(
        "Found " + queriedTreePositions.size() + " results:");
    for (TreePosition queriedTreePosition : queriedTreePositions) {
      Logger.getLogger(ContentServiceJpa.class).info(
          queriedTreePosition.getTerminologyId());
    }

    // for each query result, construct the full tree (i.e. up to root)
    for (TreePosition queriedTreePosition : queriedTreePositions) {

      TreePosition fullTreePosition =
          constructRootTreePosition(queriedTreePosition);

      Logger.getLogger(ContentServiceJpa.class).info(
          "Checking root " + fullTreePosition.getTerminologyId());

      // if this root is already present in the final list, add this position's
      // children to existing root
      if (fullTreePositions.contains(fullTreePosition)) {

        TreePosition existingTreePosition =
            fullTreePositions.get(fullTreePositions.indexOf(fullTreePosition));

        Logger.getLogger(ContentServiceJpa.class).info(
            "Found existing root at position "
                + fullTreePositions.indexOf(fullTreePosition) + " with "
                + existingTreePosition.getChildren().size());

        existingTreePosition.addChildren(fullTreePosition.getChildren());

        Logger.getLogger(ContentServiceJpa.class).info(
            "  Added " + fullTreePosition.getChildren().size() + " children:");
        for (TreePosition tp : fullTreePosition.getChildren()) {
          Logger.getLogger(ContentServiceJpa.class).info(tp.getTerminologyId());
        }

        fullTreePositions.set(fullTreePositions.indexOf(fullTreePosition),
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

  /**
   * Helper function for map record query construction using both fielded terms
   * and unfielded terms.
   * 
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param query the query
   * @return the full lucene query text
   */
  private static String constructTreePositionQuery(String terminology,
    String terminologyVersion, String query) throws Exception {

    String full_query;

    // if no filter supplied, return query based on map project id only
    if (query == null || query.equals("")) {
      full_query =
          "terminology:" + terminology + " AND terminologyVersion:"
              + terminologyVersion;
      return full_query;
    }

    // Pre-treatment: Find any lower-case boolean operators and set to uppercase

    // //////////////////
    // Basic algorithm:
    //
    // 1) add whitespace breaks to operators
    // 2) split query on whitespace
    // 3) cycle over terms in split query to find quoted material, add each
    // term/quoted term to parsed terms\
    // a) special case: quoted term after a :
    // 3) cycle over terms in parsed terms
    // a) if an operator/parantheses, pass through unchanged (send to upper case
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

    String queryStr_mod = queryStr;
    queryStr_mod = queryStr_mod.replace("(", " ( ");
    queryStr_mod = queryStr_mod.replace(")", " ) ");
    queryStr_mod = queryStr_mod.replace("\"", " \" ");
    queryStr_mod = queryStr_mod.replace("+", " + ");
    queryStr_mod = queryStr_mod.replace("-", " - ");

    // remove any leading or trailing whitespace (otherwise first/last null term
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

          // special case check: fielded term. Impossible for first term to be
          // fielded.
          if (parsedTerms.size() == 0) {
            parsedTerms.add("\"" + currentTerm + "\"");
          } else {
            String lastParsedTerm = parsedTerms.get(parsedTerms.size() - 1);

            // if last parsed term ended with a colon, append this term to the
            // last parsed term
            if (lastParsedTerm.endsWith(":") == true) {
              parsedTerms.set(parsedTerms.size() - 1, lastParsedTerm + "\""
                  + currentTerm + "\"");
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
          currentTerm =
              currentTerm == "" ? terms[i] : currentTerm + " " + terms[i];

          // otherwise, add to parsed list
        } else {
          parsedTerms.add(terms[i]);
        }
      }
    }

    for (String s : parsedTerms) {
      Logger.getLogger(MappingServiceJpa.class).debug("  " + s);
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
       * full_query += (i == 0 ? // check for first term "" : // -> if first
       * character, add nothing parsedTerms.get(i-1).matches(escapeTerms) ? //
       * check if last term was an escape character "": // -> if last term was
       * an escape character, add nothing " "); // -> otherwise, add a
       * separating space
       */

      // if an escape character/sequence, add this term unmodified
      if (parsedTerms.get(i).matches(escapeTerms)) {

        full_query += parsedTerms.get(i);

        // else if a boolean character, add this term in upper-case form (i.e.
        // lucene format)
      } else if (parsedTerms.get(i).matches(booleanTerms)) {

        full_query += parsedTerms.get(i).toUpperCase();

        // else if already a field-specific query term, add this term unmodified
      } else if (parsedTerms.get(i).contains(":")) {

        full_query += parsedTerms.get(i);

        // otherwise, treat as unfielded query term
      } else {

        // open parenthetical term
        full_query += "(";

        // add fielded query for each indexed term, separated by OR
        Iterator<String> names_iter = fieldNames.iterator();
        while (names_iter.hasNext()) {
          full_query += names_iter.next() + ":" + parsedTerms.get(i);
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
    full_query =
        "(" + full_query + ")" + " AND terminology:" + terminology
            + " AND terminologyVersion:" + terminologyVersion;

    Logger.getLogger(MappingServiceJpa.class)
        .debug("Full query: " + full_query);

    return full_query;

  }

}
