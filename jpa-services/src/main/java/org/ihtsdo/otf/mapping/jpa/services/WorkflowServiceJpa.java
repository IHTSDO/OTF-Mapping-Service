/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.CacheMode;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationList;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationListJpa;
import org.ihtsdo.otf.mapping.helpers.FeedbackList;
import org.ihtsdo.otf.mapping.helpers.FeedbackListJpa;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.AbstractWorkflowPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowFixErrorPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowNonLegacyPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowReviewProjectPathHandler;
import org.ihtsdo.otf.mapping.model.Feedback;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.OtfEmailHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

/**
 * Default workflow service implementation.
 */
public class WorkflowServiceJpa extends RootServiceJpa implements
    WorkflowService {

  /** The map record indexed field names. */
  protected static Set<String> trackingRecordFieldNames;

  /**
   * Instantiates an empty {@link WorkflowServiceJpa}.
   *
   * @throws Exception the exception
   */
  public WorkflowServiceJpa() throws Exception {
    super();
    if (trackingRecordFieldNames == null) {
      initializeFieldNames();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#initializeFieldNames()
   */
  @Override
  public synchronized void initializeFieldNames() throws Exception {
    trackingRecordFieldNames = new HashSet<>();
    EntityManager manager = factory.createEntityManager();
    FullTextEntityManager fullTextEntityManager =
        org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
    IndexReaderAccessor indexReaderAccessor =
        fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
    Set<String> indexedClassNames =
        fullTextEntityManager.getSearchFactory().getStatistics()
            .getIndexedClassNames();
    for (String indexClass : indexedClassNames) {
      if (indexClass.indexOf("TrackingRecordJpa") != -1) {
        Logger.getLogger(ContentServiceJpa.class).info(
            "FOUND TrackingRecordJpa index");
        IndexReader indexReader = indexReaderAccessor.open(indexClass);
        try {
          for (FieldInfo info : ReaderUtil.getMergedFieldInfos(indexReader)) {
            trackingRecordFieldNames.add(info.name);
          }
        } finally {
          indexReaderAccessor.close(indexReader);
        }
      }
    }
    fullTextEntityManager.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#addTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public TrackingRecord addTrackingRecord(TrackingRecord trackingRecord)
    throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(trackingRecord);
      tx.commit();
    } else {
      manager.persist(trackingRecord);
    }

    return trackingRecord;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#removeTrackingRecord
   * (java.lang.Long)
   */
  @Override
  public void removeTrackingRecord(Long trackingRecordId) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      TrackingRecord ma =
          manager.find(TrackingRecordJpa.class, trackingRecordId);

      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
      tx.commit();
    } else {
      TrackingRecord ma =
          manager.find(TrackingRecordJpa.class, trackingRecordId);
      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#updateTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public void updateTrackingRecord(TrackingRecord record) throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(record);
      tx.commit();
    } else {
      manager.merge(record);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getTrackingRecords ()
   */
  @SuppressWarnings("unchecked")
  @Override
  public TrackingRecordList getTrackingRecords() throws Exception {

    TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();

    trackingRecordList.setTrackingRecords(manager.createQuery(
        "select tr from TrackingRecordJpa tr").getResultList());

    return trackingRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getTrackingRecordForMapProjectAndConcept
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.rf2.Concept)
   */
  @Override
  public TrackingRecord getTrackingRecordForMapProjectAndConcept(
    MapProject mapProject, Concept concept) {

    try {
      return (TrackingRecord) manager
          .createQuery(
              "select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
          .setParameter("mapProjectId", mapProject.getId())
          .setParameter("terminology", concept.getTerminology())
          .setParameter("terminologyVersion", concept.getTerminologyVersion())
          .setParameter("terminologyId", concept.getTerminologyId())
          .getSingleResult();
    } catch (Exception e) {
      return null;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getTrackingRecordForMapProjectAndConcept
   * (org.ihtsdo.otf.mapping.model.MapProject, java.lang.String)
   */
  @Override
  public TrackingRecord getTrackingRecordForMapProjectAndConcept(
    MapProject mapProject, String terminologyId) {

    try {
      return (TrackingRecord) manager
          .createQuery(
              "select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
          .setParameter("mapProjectId", mapProject.getId())
          .setParameter("terminology", mapProject.getSourceTerminology())
          .setParameter("terminologyVersion",
              mapProject.getSourceTerminologyVersion())
          .setParameter("terminologyId", terminologyId).getSingleResult();
    } catch (Exception e) {
      return null;
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getTrackingRecordsForMapProject (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @SuppressWarnings("unchecked")
  @Override
  public TrackingRecordList getTrackingRecordsForMapProject(
    MapProject mapProject) throws Exception {

    TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
    javax.persistence.Query query =
        manager
            .createQuery(
                "select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId")
            .setParameter("mapProjectId", mapProject.getId());

    trackingRecordList.setTrackingRecords(query.getResultList());

    return trackingRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getTrackingRecord
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.rf2.Concept)
   */
  @Override
  public TrackingRecord getTrackingRecord(MapProject mapProject, Concept concept)
    throws Exception {

    javax.persistence.Query query =
        manager
            .createQuery(
                "select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminologyId = :terminologyId")
            .setParameter("mapProjectId", mapProject.getId())
            .setParameter("terminologyId", concept.getTerminologyId());

    return (TrackingRecord) query.getSingleResult();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#addWorkflowException
   * (org.ihtsdo.otf.mapping.workflow.WorkflowException)
   */
  @Override
  public WorkflowException addWorkflowException(WorkflowException trackingRecord)
    throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(trackingRecord);
      tx.commit();
    } else {
      manager.persist(trackingRecord);
    }

    return trackingRecord;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#removeWorkflowException
   * (java.lang.Long)
   */
  @Override
  public void removeWorkflowException(Long trackingRecordId) throws Exception {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      WorkflowException ma =
          manager.find(WorkflowExceptionJpa.class, trackingRecordId);

      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
      tx.commit();
    } else {
      WorkflowException ma =
          manager.find(WorkflowExceptionJpa.class, trackingRecordId);
      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#updateWorkflowException
   * (org.ihtsdo.otf.mapping.workflow.WorkflowException)
   */
  @Override
  public void updateWorkflowException(WorkflowException workflowException)
    throws Exception {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(workflowException);
      tx.commit();
    } else {
      manager.merge(workflowException);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#getWorkflowException(org
   * .ihtsdo.otf.mapping.model.MapProject, java.lang.String)
   */
  @Override
  public WorkflowException getWorkflowException(MapProject mapProject,
    String terminologyId) {

    javax.persistence.Query query =
        manager
            .createQuery(
                "select we from WorkflowExceptionJpa we where mapProjectId = :mapProjectId"
                    + " and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
            .setParameter("mapProjectId", mapProject.getId())
            .setParameter("terminology", mapProject.getSourceTerminology())
            .setParameter("terminologyVersion",
                mapProject.getSourceTerminologyVersion())
            .setParameter("terminologyId", terminologyId);

    // try to get the expected single result
    try {
      return (WorkflowException) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Construct map project id query.
   *
   * @param mapProjectId the map project id
   * @param query the query
   * @return the string
   */
  private static String constructMapProjectIdQuery(Long mapProjectId,
    String query) {

    String fullQuery;

    // if no filter supplied, return query based on map project id only
    if (query == null || query.equals("") || query.equals("null")
        || query.equals("undefined")) {
      fullQuery = "mapProjectId:" + mapProjectId;
      return fullQuery;
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

    String queryStrMod = query;
    queryStrMod = queryStrMod.replace("(", " ( ");
    queryStrMod = queryStrMod.replace(")", " ) ");
    queryStrMod = queryStrMod.replace("\"", " \" ");
    queryStrMod = queryStrMod.replace("+", " + ");
    queryStrMod = queryStrMod.replace("-", " - ");

    // remove any leading or trailing whitespace (otherwise first/last null
    // term
    // bug)
    queryStrMod = queryStrMod.trim();

    // split the string by white space and single-character operators
    String[] terms = queryStrMod.split("\\s+");

    // merge items between quotation marks
    boolean exprInQuotes = false;
    List<String> parsedTerms = new ArrayList<>();
    String currentTerm = "";

    // cycle over terms to identify quoted (i.e. non-parsed) terms
    for (int i = 0; i < terms.length; i++) {

      // if an open quote is detected
      if (terms[i].equals("\"")) {

        if (exprInQuotes) {

          // special case check: fielded term. Impossible for first
          // term to be
          // fielded.
          if (parsedTerms.size() == 0) {
            parsedTerms.add("\"" + currentTerm + "\"");
          } else {
            String lastParsedTerm = parsedTerms.get(parsedTerms.size() - 1);

            // if last parsed term ended with a colon, append this
            // term to the
            // last parsed term
            if (lastParsedTerm.endsWith(":")) {
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
        if (exprInQuotes) {
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
    fullQuery = "";

    for (int i = 0; i < parsedTerms.size(); i++) {

      // if not the first term AND the last term was not an escape term
      // add whitespace separator
      if (i != 0 && !parsedTerms.get(i - 1).matches(escapeTerms)) {

        fullQuery += " ";
      }
      /*
       * fullQuery += (i == 0 ? // check for first term "" : // -> if first
       * character, add nothing parsedTerms.get(i-1).matches(escapeTerms) ? //
       * check if last term was an escape character "": // -> if last term was
       * an escape character, add nothing " "); // -> otherwise, add a
       * separating space
       */

      // if an escape character/sequence, add this term unmodified
      if (parsedTerms.get(i).matches(escapeTerms)) {

        fullQuery += parsedTerms.get(i);

        // else if a boolean character, add this term in upper-case form
        // (i.e.
        // lucene format)
      } else if (parsedTerms.get(i).matches(booleanTerms)) {

        fullQuery += parsedTerms.get(i).toUpperCase();

        // else if already a field-specific query term, add this term
        // unmodified
      } else if (parsedTerms.get(i).contains(":")) {

        fullQuery += parsedTerms.get(i);

        // otherwise, treat as unfielded query term
      } else {

        // open parenthetical term
        fullQuery += "(";

        // add fielded query for each indexed term, separated by OR
        Iterator<String> namesIter = trackingRecordFieldNames.iterator();
        while (namesIter.hasNext()) {
          fullQuery += namesIter.next() + ":" + parsedTerms.get(i);
          if (namesIter.hasNext())
            fullQuery += " OR ";
        }

        // close parenthetical term
        fullQuery += ")";
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

          fullQuery += " OR";
        }
      }
    }

    // add parantheses and map project constraint
    fullQuery = "(" + fullQuery + ")" + " AND mapProjectId:" + mapProjectId;

    Logger.getLogger(MappingServiceJpa.class).debug("Full query: " + fullQuery);

    return fullQuery;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#findAvailableWork(org.ihtsdo
   * .otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser,
   * java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {
    SearchResultList availableWork = new SearchResultListJpa();

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAvailableWork
    // - must be NON_LEGACY PATH
    // - any tracking record with no assigned users is by definition
    // available
    // - any tracking record with one assigned user on NON_LEGACY_PATH with
    // workflowstatus NEW, EDITING_IN_PROGRESS, or EDITING_DONE. Assigned
    // user must not be this user

    switch (mapProject.getWorkflowType()) {
      case CONFLICT_PROJECT:
        fullQuery += " AND workflowPath:NON_LEGACY_PATH";
        // Handle "team" based assignment
        if (mapProject.isTeamBased() && mapUser.getTeam() != null
            && !mapUser.getTeam().isEmpty()) {
          // Use "AND NOT" clauses for all members matching my user's team.
          MappingService service = new MappingServiceJpa();
          fullQuery += " AND (assignedUserCount:0 OR (assignedUserCount:1 ";
          for (MapUser user : service.getMapUsersForTeam(mapUser.getTeam())
              .getMapUsers()) {
            fullQuery += " AND NOT assignedUserNames:" + user.getUserName();
          }
          fullQuery += ") )";
          service.close();
        } else {
          fullQuery +=
              " AND (assignedUserCount:0 OR "
                  + "(assignedUserCount:1 AND NOT assignedUserNames:"
                  + mapUser.getUserName() + "))";
        }
        break;
      case REVIEW_PROJECT:
        fullQuery += " AND workflowPath:REVIEW_PROJECT_PATH";
        fullQuery += " AND assignedUserCount:0";
        break;
      default:
        throw new Exception("Invalid workflow type specified for project "
            + mapProject.getWorkflowType());

    }

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    availableWork.setTotalCount(ftquery.getResultSize());

    if (pfsParameter.getStartIndex() != -1
        && pfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(pfsParameter.getStartIndex());
      ftquery.setMaxResults(pfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (pfsParameter.getSortField() != null
        && !pfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField())
          .getType().equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
            SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
    }
    List<TrackingRecord> results = ftquery.getResultList();

    for (TrackingRecord tr : results) {
      SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      availableWork.addSearchResult(result);
    }
    return availableWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableConflicts
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableConflicts(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList availableConflicts = new SearchResultListJpa();

    // if not a conflict project, return empty list
    if (!mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT)) {
      return availableConflicts;
    }

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAvailableConflicts
    // - user and workflowStatus pair of CONFLICT_DETECTED_userName exists
    // - user and workflowStatus pairs of
    // CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist
    fullQuery += " AND userAndWorkflowStatusPairs:CONFLICT_DETECTED_*";
    fullQuery +=
        " AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_* OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_* OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*)";

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    availableConflicts.setTotalCount(ftquery.getResultSize());

    if (pfsParameter.getStartIndex() != -1
        && pfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(pfsParameter.getStartIndex());
      ftquery.setMaxResults(pfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (pfsParameter.getSortField() != null
        && !pfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField())
          .getType().equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
            SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
    }
    List<TrackingRecord> results = ftquery.getResultList();

    for (TrackingRecord tr : results) {
      SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      availableConflicts.addSearchResult(result);
    }
    return availableConflicts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#findAvailableQAWork(org
   * .ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser,
   * java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableQAWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList availableQAWork = new SearchResultListJpa();

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = "mapProjectId:" + mapProject.getId();

    // add the query terms specific to findAvailableReviewWork
    // - a user (any) and workflowStatus pair of QA_NEEDED_userName
    // exists
    // - the QA_NEEDED pair is not for this user (i.e. user can't review
    // their own work, UNLESS there is only one lead on the project
    // - user and workflowStatus pairs of
    // CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

    // must have a QA_NEEDED tag with any user
    fullQuery += " AND userAndWorkflowStatusPairs:QA_NEEDED_*";

    fullQuery += " AND workflowPath:QA_PATH";

    // there must not be an already claimed review record
    fullQuery +=
        " AND NOT (userAndWorkflowStatusPairs:QA_NEW_*"
            + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:QA_RESOLVED_*" + ")";

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    List<TrackingRecord> allResults = ftquery.getResultList();
    List<TrackingRecord> results = new ArrayList<>();

    if (query == null || query.equals("") || query.equals("null")
        || query.equals("undefined")) {
      results = allResults;
    } else {
      // remove tracking records that don't have a map record with a label
      // matching the query
      for (TrackingRecord tr : allResults) {
        boolean labelFound = false;

        // OPTIMIZATION: Bypass the "get map records for tr" mechanism to avoid
        // full lazy initialization of the record
        Set<MapRecord> mapRecords = new HashSet<>();
        if (tr != null && tr.getMapRecordIds() != null) {
          for (Long id : tr.getMapRecordIds()) {
            // go directly, we just need record label info
            mapRecords.add(manager.find(MapRecordJpa.class, id));
          }
        }
        for (MapRecord record : mapRecords) {
          for (String label : record.getLabels()) {
            if (label.equals(query)) {
              labelFound = true;
            }
          }
        }
        if (labelFound) {
          results.add(tr);
        }
      }
    }

    // set the total count matching this label
    availableQAWork.setTotalCount(results.size());

    // apply paging, and sorting if appropriate
    if (pfsParameter != null
        && (pfsParameter.getSortField() != null && !pfsParameter.getSortField()
            .isEmpty())) {
      // check that specified sort field exists on Concept and is
      // a string
      final Field sortField =
          TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField());
      if (!sortField.getType().equals(String.class)) {

        throw new Exception(
            "findAvailableQAWork error:  Referenced sort field is not of type String");
      }

      // allow the field to access the Concept values
      sortField.setAccessible(true);

      // sort the list - UNTESTED
      Collections.sort(results, new Comparator<TrackingRecord>() {
        @Override
        public int compare(TrackingRecord c1, TrackingRecord c2) {

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
    int toIndex = results.size();
    if (pfsParameter != null) {
      startIndex =
          pfsParameter.getStartIndex() == -1 ? 0 : Math.min(results.size(),
              pfsParameter.getStartIndex());

      // ensure start index not negative
      if (startIndex < 0)
        startIndex = 0;

      toIndex =
          pfsParameter.getMaxResults() == -1 ? results.size() : Math.min(
              results.size(), startIndex + pfsParameter.getMaxResults());
    }

    for (TrackingRecord tr : results.subList(startIndex, toIndex)) {
      SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setId(tr.getId());
      result.setValue(tr.getDefaultPreferredName());

      StringBuffer labelBuffer = new StringBuffer();
      for (MapRecord record : getMapRecordsForTrackingRecord(tr)) {
        for (String label : record.getLabels()) {
          if (labelBuffer.indexOf(label) == -1)
            labelBuffer.append(";").append(label);
        }
      }
      result.setValue2(labelBuffer.toString());
      availableQAWork.addSearchResult(result);
    }
    return availableQAWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableReviewWork
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAvailableReviewWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList availableReviewWork = new SearchResultListJpa();

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAvailableReviewWork
    // - a user (any) and workflowStatus pair of REVIEW_NEEDED_userName
    // exists
    // - the REVIEW_NEEDED pair is not for this user (i.e. user can't review
    // their own work, UNLESS there is only one lead on the project
    // - user and workflowStatus pairs of
    // CONFLICT_NEW/CONFLICT_IN_PROGRESS_userName does not exist

    // must have a REVIEW_NEEDED tag with any user
    fullQuery += " AND userAndWorkflowStatusPairs:REVIEW_NEEDED_*";

    // there must not be an already claimed review record
    fullQuery +=
        " AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*" + ")";

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    availableReviewWork.setTotalCount(ftquery.getResultSize());

    if (pfsParameter.getStartIndex() != -1
        && pfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(pfsParameter.getStartIndex());
      ftquery.setMaxResults(pfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (pfsParameter.getSortField() != null
        && !pfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField())
          .getType().equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
            SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
    }
    List<TrackingRecord> results = ftquery.getResultList();

    for (TrackingRecord tr : results) {
      SearchResult result = new SearchResultJpa();
      result.setTerminologyId(tr.getTerminologyId());
      result.setValue(tr.getDefaultPreferredName());
      result.setId(tr.getId());
      availableReviewWork.addSearchResult(result);
    }
    return availableReviewWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#findAssignedWork(org.ihtsdo
   * .otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser,
   * java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    PfsParameter localPfsParameter = pfsParameter;

    SearchResultList assignedWork = new SearchResultListJpa();

    // create a blank pfs parameter object if one not passed in
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // create a blank query restriction if none provided
    if (localPfsParameter.getQueryRestriction() == null)
      localPfsParameter.setQueryRestriction("");

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAssignedWork
    // - user and workflowStatus must exist in a pair of form:
    // workflowStatus_userName, e.g. NEW_dmo or EDITING_IN_PROGRESS_kli
    // - modify search term based on pfs parameter query restriction field
    // * default: NEW, EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED
    // * NEW: NEW
    // * EDITED: EDITING_IN_PROGRESS, EDITING_DONE/CONFLICT_DETECTED

    // add terms based on query restriction
    switch (localPfsParameter.getQueryRestriction()) {
      case "NEW":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName();
        break;
      case "EDITING_IN_PROGRESS":
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_"
                + mapUser.getUserName() + ")";
        break;
      case "EDITING_DONE":
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:EDITING_DONE_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_"
                + mapUser.getUserName() + ")";

        break;
      default:
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:EDITING_DONE_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_DETECTED_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_"
                + mapUser.getUserName() + ")";
        break;
    }

    // add terms to exclude concepts that a lead has claimed
    fullQuery +=
        " AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*"
            + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_NEW_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_NEEDED_*"
            + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_*)";

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    assignedWork.setTotalCount(ftquery.getResultSize());

    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(localPfsParameter.getStartIndex());
      ftquery.setMaxResults(localPfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (localPfsParameter.getSortField() != null
        && !localPfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class
          .getDeclaredField(localPfsParameter.getSortField()).getType()
          .equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(
            localPfsParameter.getSortField(), SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
      // otherwise, sort by ancestor path
    } else {
      ftquery.setSort(new Sort(new SortField("sortKey", SortField.STRING)));
    }

    List<TrackingRecord> results = ftquery.getResultList();
    MappingService mappingService = new MappingServiceJpa();

    for (TrackingRecord tr : results) {

      // instantiate the result list
      SearchResult result = new SearchResultJpa();

      // get the map records associated with this tracking record
      Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

      // get the map record assigned to this user
      MapRecord mapRecord = null;

      // SEE BELOW/MAP-617
      WorkflowStatus mapLeadAlternateRecordStatus = null;

      for (MapRecord mr : mapRecords) {

        if (mr.getOwner().equals(mapUser)) {

          // if this lead has review or conflict work, set the flag
          if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
              || mr.getWorkflowStatus().equals(
                  WorkflowStatus.CONFLICT_IN_PROGRESS)
              || mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
              || mr.getWorkflowStatus().equals(
                  WorkflowStatus.REVIEW_IN_PROGRESS)) {

            mapLeadAlternateRecordStatus = mr.getWorkflowStatus();

            // added to prevent user from getting REVISION record
            // back on FIX_ERROR_PATH
            // yet another problem related to leads being able to
            // serve as dual roles
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            // do nothing

            // otherwise, this is the specialist/concept-level work
          } else {
            mapRecord = mr;
          }
        }
      }

      // if no record and no review or conflict work was found, throw
      // error
      if (mapRecord == null) {
        throw new Exception(
            "Failed to retrieve assigned work:  no map record found for user "
                + mapUser.getUserName() + " and concept "
                + tr.getTerminologyId());

      } else {

        // alter the workflow status if a higher-level record exists for
        // this user
        if (mapLeadAlternateRecordStatus != null) {

          Logger.getLogger(WorkflowServiceJpa.class).info(
              "Setting alternate record status: "
                  + mapLeadAlternateRecordStatus);
          mapRecord.setWorkflowStatus(mapLeadAlternateRecordStatus);
        }
        // create the search result
        result.setTerminologyId(mapRecord.getConceptId());
        result.setValue(mapRecord.getConceptName());
        result.setTerminology(mapRecord.getLastModified().toString());
        result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
        result.setId(mapRecord.getId());
        assignedWork.addSearchResult(result);
      }
    }
    mappingService.close();
    return assignedWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getAssignedConflicts(
   * org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedConflicts(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList assignedConflicts = new SearchResultListJpa();

    if (mapProject.getWorkflowType().toString().equals("REVIEW_PROJECT_PATH"))
      return assignedConflicts;

    PfsParameter localPfsParameter = pfsParameter;

    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // create a blank query restriction if none provided
    if (localPfsParameter.getQueryRestriction() == null)
      localPfsParameter.setQueryRestriction("");

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAssignedConflicts
    // - workflow status CONFLICT_NEW or CONFLICT_IN_PROGRESS with this user
    // name in pair

    // add terms based on query restriction
    switch (localPfsParameter.getQueryRestriction()) {
      case "CONFLICT_NEW":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:CONFLICT_NEW_"
                + mapUser.getUserName();
        break;
      case "CONFLICT_IN_PROGRESS":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_"
                + mapUser.getUserName();
        break;
      case "CONFLICT_RESOLVED":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:CONFLICT_RESOLVED_"
                + mapUser.getUserName();
        break;
      default:
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:CONFLICT_NEW_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:CONFLICT_RESOLVED_"
                + mapUser.getUserName() + ")";
        break;
    }

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    assignedConflicts.setTotalCount(ftquery.getResultSize());

    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(localPfsParameter.getStartIndex());
      ftquery.setMaxResults(localPfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (localPfsParameter.getSortField() != null
        && !localPfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class
          .getDeclaredField(localPfsParameter.getSortField()).getType()
          .equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(
            localPfsParameter.getSortField(), SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
    }

    List<TrackingRecord> results = ftquery.getResultList();
    MappingService mappingService = new MappingServiceJpa();
    for (TrackingRecord tr : results) {
      SearchResult result = new SearchResultJpa();

      Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

      // get the map record assigned to this user
      MapRecord mapRecord = null;
      for (MapRecord mr : mapRecords) {

        try {
          if (mr.getOwner().equals(mapUser))

            // SEE MAP-617:
            // Lower level record may exist with same owner, only
            // add if actually a conflict

            if (mr.getWorkflowStatus().compareTo(
                WorkflowStatus.CONFLICT_DETECTED) < 0) {
              // do nothing, this is the specialist level work
            } else {
              mapRecord = mr;
            }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (mapRecord == null) {

        throw new Exception(
            "Failed to retrieve assigned conflicts:  no map record found for user "
                + mapUser.getUserName() + " and concept "
                + tr.getTerminologyId());
      } else {
        result.setTerminologyId(mapRecord.getConceptId());
        result.setValue(mapRecord.getConceptName());
        result.setTerminology(mapRecord.getLastModified().toString());
        result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
        result.setId(mapRecord.getId());
        assignedConflicts.addSearchResult(result);
      }
    }
    mappingService.close();
    return assignedConflicts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#findAssignedReviewWork(
   * org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.model.MapUser, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedReviewWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList assignedReviewWork = new SearchResultListJpa();
    PfsParameter localPfsParameter = pfsParameter;

    // create a blank pfs parameter object if one not passed in
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // create a blank query restriction if none provided
    if (localPfsParameter.getQueryRestriction() == null)
      localPfsParameter.setQueryRestriction("");

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = constructMapProjectIdQuery(mapProject.getId(), query);

    // add the query terms specific to findAssignedReviewWork
    // - user and workflow status must exist in the form REVIEW_NEW_userName
    // or REVIEW_IN_PROGRESS_userName

    // add terms based on query restriction
    switch (localPfsParameter.getQueryRestriction()) {
      case "REVIEW_NEW":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:REVIEW_NEW_"
                + mapUser.getUserName();

        break;
      case "REVIEW_IN_PROGRESS":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_"
                + mapUser.getUserName();
        break;
      case "REVIEW_RESOLVED":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:REVIEW_RESOLVED_"
                + mapUser.getUserName();
        break;
      default:
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:REVIEW_NEW_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:REVIEW_RESOLVED_"
                + mapUser.getUserName() + ")";
        break;
    }

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    assignedReviewWork.setTotalCount(ftquery.getResultSize());

    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      ftquery.setFirstResult(localPfsParameter.getStartIndex());
      ftquery.setMaxResults(localPfsParameter.getMaxResults());

    }

    // if sort field is specified, set sort key
    if (localPfsParameter.getSortField() != null
        && !localPfsParameter.getSortField().isEmpty()) {

      // check that specified sort field exists on Concept and is
      // a string
      if (TrackingRecordJpa.class
          .getDeclaredField(localPfsParameter.getSortField()).getType()
          .equals(String.class)) {
        ftquery.setSort(new Sort(new SortField(
            localPfsParameter.getSortField(), SortField.STRING)));
      } else {
        throw new Exception(
            "Concept query specified a field that does not exist or is not a string");
      }
    }

    List<TrackingRecord> results = ftquery.getResultList();
    MappingService mappingService = new MappingServiceJpa();
    for (TrackingRecord tr : results) {
      SearchResult result = new SearchResultJpa();

      Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

      // get the map record assigned to this user
      MapRecord mapRecord = null;
      for (MapRecord mr : mapRecords) {

        if (mr.getOwner().equals(mapUser)) {

          // check for the case where REVIEW work is both specialist
          // and
          // lead level for same user
          if (mr.getWorkflowStatus().compareTo(WorkflowStatus.REVIEW_NEW) < 0) {
            // do nothing, this is the specialist level work

          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            // do nothing

          } else {
            // add the record
            mapRecord = mr;
          }
        }
      }

      if (mapRecord == null) {
        mappingService.close();
        throw new Exception(
            "Failed to retrieve assigned work:  no map record found for user "
                + mapUser.getUserName() + " and concept "
                + tr.getTerminologyId());
      }
      result.setTerminologyId(mapRecord.getConceptId());
      result.setValue(mapRecord.getConceptName());
      result.setTerminology(mapRecord.getLastModified().toString());
      result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
      result.setId(mapRecord.getId());
      assignedReviewWork.addSearchResult(result);
    }
    mappingService.close();
    return assignedReviewWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#findAssignedQAWork(org.
   * ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser,
   * java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public SearchResultList findAssignedQAWork(MapProject mapProject,
    MapUser mapUser, String query, PfsParameter pfsParameter) throws Exception {

    SearchResultList assignedReviewWork = new SearchResultListJpa();
    PfsParameter localPfsParameter = pfsParameter;

    // create a blank pfs parameter object if one not passed in
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // create a blank query restriction if none provided
    if (localPfsParameter.getQueryRestriction() == null)
      localPfsParameter.setQueryRestriction("");

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct basic query
    String fullQuery = "mapProjectId:" + mapProject.getId();

    // add the query terms specific to findAssignedReviewWork
    // - user and workflow status must exist in the form QA_NEW_userName
    // or QA_IN_PROGRESS_userName

    // add terms based on query restriction
    switch (localPfsParameter.getQueryRestriction()) {
      case "QA_NEW":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName();

        break;
      case "QA_IN_PROGRESS":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:QA_IN_PROGRESS_"
                + mapUser.getUserName();
        break;
      case "QA_RESOLVED":
        fullQuery +=
            " AND userAndWorkflowStatusPairs:QA_RESOLVED_"
                + mapUser.getUserName();
        break;
      default:
        fullQuery +=
            " AND (userAndWorkflowStatusPairs:QA_NEW_" + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:QA_IN_PROGRESS_"
                + mapUser.getUserName()
                + " OR userAndWorkflowStatusPairs:QA_RESOLVED_"
                + mapUser.getUserName() + ")";
        break;
    }

    // don't get path
    fullQuery += " AND workflowPath:QA_PATH";

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(TrackingRecordJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            TrackingRecordJpa.class);

    List<TrackingRecord> allResults = ftquery.getResultList();
    List<TrackingRecord> results = new ArrayList<>();

    if (query == null || query.equals("") || query.equals("null")
        || query.equals("undefined")) {
      results = allResults;
    } else {
      // remove tracking records that don't have a map record with a label
      // matching the query
      for (TrackingRecord tr : allResults) {
        boolean labelFound = false;
        for (MapRecord record : getMapRecordsForTrackingRecord(tr)) {
          for (String label : record.getLabels()) {
            if (label.equals(query)) {
              labelFound = true;
            }
          }
        }
        if (labelFound) {
          results.add(tr);
        }
      }
    }

    assignedReviewWork.setTotalCount(results.size());

    // apply paging, and sorting if appropriate
    if (pfsParameter != null
        && (pfsParameter.getSortField() != null && !pfsParameter.getSortField()
            .isEmpty())) {
      // check that specified sort field exists on Concept and is
      // a string
      final Field sortField =
          TrackingRecordJpa.class.getDeclaredField(pfsParameter.getSortField());
      if (!sortField.getType().equals(String.class)) {

        throw new Exception(
            "findAssignedQAWork error:  Referenced sort field is not of type String");
      }

      // allow the field to access the Concept values
      sortField.setAccessible(true);

      // sort the list - UNTESTED
      Collections.sort(results, new Comparator<TrackingRecord>() {
        @Override
        public int compare(TrackingRecord c1, TrackingRecord c2) {

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
    int toIndex = results.size();
    if (pfsParameter != null) {
      if (pfsParameter.getStartIndex() != -1) {
        // ensure that start index is within array boundaries
        startIndex = Math.min(results.size(), pfsParameter.getStartIndex());

        // ensure startIndex not less than zero
        if (startIndex < 0)
          startIndex = 0;
      }

      if (pfsParameter.getMaxResults() != -1) {
        toIndex =
            Math.min(results.size(), startIndex + pfsParameter.getMaxResults());
      }
    }

    for (TrackingRecord tr : results.subList(startIndex, toIndex)) {

      SearchResult result = new SearchResultJpa();

      Set<MapRecord> mapRecords = this.getMapRecordsForTrackingRecord(tr);

      // get the map record assigned to this user
      MapRecord mapRecord = null;
      for (MapRecord mr : mapRecords) {

        if (mr.getOwner().equals(mapUser)) {

          if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
            // do nothing, this is the specialist level QA work

          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            // do nothing

          } else {
            // this is the user's record
            mapRecord = mr;
          }
        }
      }

      if (mapRecord == null) {
        throw new Exception(
            "Failed to retrieve assigned work:  no map record found for user "
                + mapUser.getUserName() + " and concept "
                + tr.getTerminologyId());
      }
      result.setTerminologyId(mapRecord.getConceptId());
      result.setValue(mapRecord.getConceptName());
      StringBuffer labelBuffer = new StringBuffer();
      for (MapRecord record : getMapRecordsForTrackingRecord(tr)) {
        for (String label : record.getLabels()) {
          if (labelBuffer.indexOf(label) == -1)
            labelBuffer.append(";").append(label);
        }
      }
      result.setValue2(labelBuffer.toString());
      result.setTerminology(mapRecord.getLastModified().toString());
      result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
      result.setId(mapRecord.getId());
      assignedReviewWork.addSearchResult(result);
    }
    return assignedReviewWork;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#createQAWork(org.ihtsdo
   * .otf.mapping.reports.Report)
   */
  @Override
  public void createQAWork(Report report) throws Exception {

    if (report.getResults() == null || report.getResults().size() != 1) {
      throw new Exception("Failed to provide a report with one result set "
          + report.getId());
    }

    Set<String> conceptIds = new HashSet<>();
    for (ReportResultItem resultItem : report.getResults().get(0)
        .getReportResultItems()) {
      conceptIds.add(resultItem.getItemId());
    }

    // open the services
    ContentService contentService = new ContentServiceJpa();
    MappingService mappingService = new MappingServiceJpa();

    // get the map project and concept
    MapProject mapProject =
        mappingService.getMapProject(report.getMapProjectId());

    // find the qa user
    MapUser mapUser = null;
    for (MapUser user : mappingService.getMapUsers().getMapUsers()) {
      if (user.getUserName().equals("qa"))
        mapUser = user;
    }
    
    for (String conceptId : conceptIds) {

      Concept concept =
          contentService.getConcept(conceptId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      MapRecordList recordList =
          mappingService.getMapRecordsForProjectAndConcept(mapProject.getId(),
              conceptId);

      for (MapRecord mapRecord : recordList.getMapRecords()) {
        // set the label on the record
        mapRecord.addLabel(report.getReportDefinition().getName());

        // process the workflow action
        processWorkflowAction(mapProject, concept, mapUser, mapRecord,
            WorkflowAction.CREATE_QA_RECORD);
      }
    }

    mappingService.close();
    contentService.close();
  }

  /**
   * Perform workflow actions based on a specified action.
   * ASSIGN_FROM_INITIAL_RECORD is the only routine that requires a map record
   * to be passed in All other cases that all required mapping information (e.g.
   * map records) be current in the database (i.e. updateMapRecord has been
   * called)
   * 
   * @param mapProject the map project
   * @param concept the concept
   * @param mapUser the map user
   * @param mapRecord the map record
   * @param workflowAction the workflow action
   * @throws Exception the exception
   */
  @Override
  public void processWorkflowAction(MapProject mapProject, Concept concept,
    MapUser mapUser, MapRecord mapRecord, WorkflowAction workflowAction)
    throws Exception {

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Processing workflow action by " + mapUser.getName() + ":  "
            + workflowAction.toString());
    if (mapRecord != null) {
      Logger.getLogger(WorkflowServiceJpa.class).info(
          "  Record attached: " + mapRecord.toString());
    }

    setTransactionPerOperation(true);

    // instantiate the algorithm handler for this project\
    ProjectSpecificAlgorithmHandler algorithmHandler =
        (ProjectSpecificAlgorithmHandler) Class.forName(
            mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();
    algorithmHandler.setMapProject(mapProject);

    // locate any existing workflow tracking records for this project and
    // concept
    // NOTE: Exception handling deliberate, since tracking record may or may
    // not exist
    // depending on workflow path
    TrackingRecord trackingRecord = null;
    try {
      trackingRecord = getTrackingRecord(mapProject, concept);
    } catch (NoResultException e) {
      trackingRecord = null;
    }

    // Validate tracking record
    if (trackingRecord != null) {

      // instantiate the handler based on tracking record workflow type
      AbstractWorkflowPathHandler handler = null;
      switch (trackingRecord.getWorkflowPath()) {
        case CONSENSUS_PATH:
          break;
        case DRIP_FEED_REVIEW_PATH:
          break;
        case FIX_ERROR_PATH:
          handler = new WorkflowFixErrorPathHandler();
          break;
        case LEGACY_PATH:
          break;
        case NON_LEGACY_PATH:
          handler = new WorkflowNonLegacyPathHandler();
          break;
        case QA_PATH:
          handler = new WorkflowQaPathHandler();
          break;
        case REVIEW_PROJECT_PATH:
          handler = new WorkflowReviewProjectPathHandler();
          break;
        default:
          throw new Exception(
              "Could not determine workflow handler from tracking record for path: "
                  + trackingRecord.getWorkflowPath().toString());
      }

      ValidationResult result =
          handler.validateTrackingRecordForActionAndUser(trackingRecord,
              workflowAction, mapUser);

      if (!result.isValid()) {

        Logger.getLogger(WorkflowServiceJpa.class).info(result.toString());

        StringBuffer message = new StringBuffer();

        message.append("Errors were detected in the workflow for:\n");
        message.append("  Project\t: " + mapProject.getName() + "\n");
        message.append("  Concept\t: " + concept.getTerminologyId() + "\n");
        message.append("  Path:\t "
            + trackingRecord.getWorkflowPath().toString() + "\n");
        message.append("  User\t: " + mapUser.getUserName() + "\n");
        message.append("  Action\t: " + workflowAction.toString() + "\n");

        message.append("\n");

        // record information
        message.append("Records involved:\n");
        message.append("  " + "id\tUser\tWorkflowStatus\n");

        for (MapRecord mr : getMapRecordsForTrackingRecord(trackingRecord)) {
          message.append("  " + mr.getId().toString() + "\t"
              + mr.getOwner().getUserName() + "\t"
              + mr.getWorkflowStatus().toString() + "\n");
        }
        message.append("\n");

        message.append("Errors reported:\n");

        for (String error : result.getErrors()) {
          message.append("  " + error + "\n");
        }

        message.append("\n");

        // log the message
        Logger.getLogger(WorkflowServiceJpa.class).error(
            "Workflow error detected\n" + message.toString());

        // send email if indicated
        Properties config = ConfigUtility.getConfigProperties();

        String notificationRecipients =
            config.getProperty("send.notification.recipients");
        if (!notificationRecipients.isEmpty()) {
          OtfEmailHandler emailHandler = new OtfEmailHandler();
          emailHandler.sendSimpleEmail(notificationRecipients,
              config.getProperty("mail.smtp.user"),
              mapProject.getName() + " Workflow Error Alert, Concept "
                  + concept.getTerminologyId(), message.toString());
        }

        throw new LocalException("Workflow action " + workflowAction.toString()
            + " could not be performed on concept "
            + trackingRecord.getTerminologyId());
      }
    }

    Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

    // if the record passed in updates an existing record, replace it in the
    // set
    if (mapRecord != null && mapRecord.getId() != null) {
      for (MapRecord mr : mapRecords) {
        if (mr.getId().equals(mapRecord.getId())) {

          Logger.getLogger(WorkflowService.class).info(
              "Replacing record " + mr.toString() + "\n  with"
                  + mapRecord.toString());

          mapRecords.remove(mr);
          mapRecords.add(mapRecord);
          break;
        }
      }
    }

    // switch on workflow action
    switch (workflowAction) {
      case CREATE_QA_RECORD:

        Logger.getLogger(WorkflowServiceJpa.class).info("CREATE_QA_RECORD");

        // if a tracking record is found, perform no action (this record is
        // already assigned)
        if (trackingRecord == null) {

          // expect a map record to be passed in
          if (mapRecord == null) {
            throw new Exception(
                "ProcessWorkflowAction: CREATE_QA_RECORD - Call to assign from intial record must include an existing map record");
          }

          // create a new tracking record for QA_PATH
          trackingRecord = new TrackingRecordJpa();
          trackingRecord.setMapProjectId(mapProject.getId());
          trackingRecord.setTerminology(concept.getTerminology());
          trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
          trackingRecord.setTerminologyId(concept.getTerminologyId());
          trackingRecord.setDefaultPreferredName(concept
              .getDefaultPreferredName());
          trackingRecord.addMapRecordId(mapRecord.getId());

          // get the tree positions for this concept and set the sort key
          // to
          // the first retrieved
          ContentService contentService = new ContentServiceJpa();
          TreePositionList treePositionsList =
              contentService.getTreePositionsWithDescendants(
                  concept.getTerminologyId(), concept.getTerminology(),
                  concept.getTerminologyVersion());

          // handle inactive concepts - which don't have tree positions
          if (treePositionsList.getCount() == 0) {
            trackingRecord.setSortKey("");
          } else {
            trackingRecord.setSortKey(treePositionsList.getTreePositions()
                .get(0).getAncestorPath());
          }

          trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);

          // perform the assign action via the algorithm handler
          mapRecords =
              algorithmHandler.assignFromInitialRecord(trackingRecord,
                  mapRecords, mapRecord, mapUser);

          contentService.close();
          // otherwise, this concept is already in the workflow, do nothing
        } else {

          // do nothing (label will be added in synchronize)
        }

        break;
      case ASSIGN_FROM_INITIAL_RECORD:

        Logger.getLogger(WorkflowServiceJpa.class).info(
            "ASSIGN_FROM_INITIAL_RECORD");

        // if a tracking record is found, perform no action (this record is
        // already assigned)
        if (trackingRecord == null) {

          // expect a map record to be passed in
          if (mapRecord == null) {
            throw new Exception(
                "ProcessWorkflowAction: ASSIGN_FROM_INITIAL_RECORD - Call to assign from intial record must include an existing map record");
          }

          // create a new tracking record for FIX_ERROR_PATH or QA_PATH
          trackingRecord = new TrackingRecordJpa();
          trackingRecord.setMapProjectId(mapProject.getId());
          trackingRecord.setTerminology(concept.getTerminology());
          trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
          trackingRecord.setTerminologyId(concept.getTerminologyId());
          trackingRecord.setDefaultPreferredName(concept
              .getDefaultPreferredName());
          trackingRecord.addMapRecordId(mapRecord.getId());

          // get the tree positions for this concept and set the sort key
          // to
          // the first retrieved
          ContentService contentService = new ContentServiceJpa();
          TreePositionList treePositionsList =
              contentService.getTreePositionsWithDescendants(
                  concept.getTerminologyId(), concept.getTerminology(),
                  concept.getTerminologyVersion());

          // handle inactive concepts - which don't have tree positions
          if (treePositionsList.getCount() == 0) {
            trackingRecord.setSortKey("");
          } else {
            trackingRecord.setSortKey(treePositionsList.getTreePositions()
                .get(0).getAncestorPath());
          }

          trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

          // perform the assign action via the algorithm handler
          mapRecords =
              algorithmHandler.assignFromInitialRecord(trackingRecord,
                  mapRecords, mapRecord, mapUser);
          contentService.close();
        } else {

          throw new LocalException(
              "Assignment from published record failed -- concept already in workflow");

        }

        break;

      case ASSIGN_FROM_SCRATCH:

        // Logger.getLogger(WorkflowServiceJpa.class).info(
        // "ASSIGN_FROM_SCRATCH");

        // expect existing (pre-computed) workflow tracking record
        if (trackingRecord == null) {
          throw new Exception("Could not find tracking record for assignment.");
        }

        // Team based assignment only matters on NON_LEGACY_PATH and not for
        // conflict cases
        // If "concepts" assignment is being done on NON_LEGACY PATH and another
        // team
        // member claimed the other role, then leave alone
        if (trackingRecord.getWorkflowPath() == WorkflowPath.NON_LEGACY_PATH
            && mapProject.isTeamBased()
            && trackingRecord.getAssignedUserCount() > 0
            && !trackingRecord.getUserAndWorkflowStatusPairs().contains(
                WorkflowStatus.CONFLICT_DETECTED.toString())) {
          MappingService service = new MappingServiceJpa();
          for (MapUser user : service.getMapUsersForTeam(mapUser.getTeam())
              .getMapUsers()) {
            if (trackingRecord.getAssignedUserNames().contains(
                user.getUserName())) {
              service.close();
              throw new LocalException(
                  "This concept is already assigned to another member of "
                      + "the same team.  Reload the dashboard and try again");
            }
          }
          service.close();
        }

        // perform the assignment via the algorithm handler
        mapRecords =
            algorithmHandler.assignFromScratch(trackingRecord, mapRecords,
                concept, mapUser);

        break;

      case UNASSIGN:

        Logger.getLogger(WorkflowServiceJpa.class).info("UNASSIGN");

        // expect existing (pre-computed) workflow tracking record to exist
        // with this user assigned
        if (trackingRecord == null)
          throw new Exception(
              "ProcessWorkflowAction: UNASSIGN - Could not find tracking record for unassignment.");

        // expect this user to be assigned to a map record in this tracking
        // record
        if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
          throw new Exception(
              "ProcessWorkflowAction: UNASSIGN - User not assigned to record for unassignment request");

        // perform the unassign action via the algorithm handler
        mapRecords =
            algorithmHandler.unassign(trackingRecord, mapRecords, mapUser);

        break;

      case SAVE_FOR_LATER:

        // Logger.getLogger(WorkflowServiceJpa.class).info("SAVE_FOR_LATER");

        // expect existing (pre-computed) workflow tracking record to exist
        // with this user assigned
        if (trackingRecord == null)
          throw new Exception(
              "ProcessWorkflowAction: SAVE_FOR_LATER - Could not find tracking record.");

        // expect this user to be assigned to a map record in this tracking
        // record
        if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
          throw new Exception(
              "ProcessWorkflowAction: SAVE_FOR_LATER - SAVE_FOR_LATER - User not assigned to record");

        // Logger.getLogger(WorkflowServiceJpa.class).info(
        // "Performing action...");

        mapRecords =
            algorithmHandler.saveForLater(trackingRecord, mapRecords, mapUser);

        break;

      case FINISH_EDITING:

        // Logger.getLogger(WorkflowServiceJpa.class).info("FINISH_EDITING");

        // expect existing (pre-computed) workflow tracking record to exist
        // with this user assigned
        if (trackingRecord == null)
          throw new Exception(
              "ProcessWorkflowAction: FINISH_EDITING - Could not find tracking record to be finished.");

        // expect this user to be assigned to a map record in this tracking
        // record
        if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
          throw new Exception(
              "ProcessWorkflowAction: FINISH_EDITING - User not assigned to record for finishing request");
        //
        // Logger.getLogger(WorkflowServiceJpa.class).info(
        // "Performing action...");
        //
        // // perform the action
        mapRecords =
            algorithmHandler.finishEditing(trackingRecord, mapRecords, mapUser);

        break;

      case PUBLISH:

        // Logger.getLogger(WorkflowServiceJpa.class).info("FINISH_EDITING");

        // expect existing (pre-computed) workflow tracking record to exist
        // with this user assigned
        if (trackingRecord == null)
          throw new Exception(
              "ProcessWorkflowAction: PUBLISH - Could not find tracking record to be published.");

        // expect this user to be assigned to a map record in this tracking
        // record
        if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
          throw new Exception(
              "ProcessWorkflowAction: PUBLISH - User not assigned to tracking record for publish request");
        //
        // Logger.getLogger(WorkflowServiceJpa.class).info(
        // "Performing action...");
        //
        // // perform the action
        mapRecords =
            algorithmHandler.publish(trackingRecord, mapRecords, mapUser);

        break;

      case CANCEL:

        // expect existing (pre-computed) workflow tracking record to exist
        // with this user assigned
        if (trackingRecord == null)
          throw new Exception(
              "ProcessWorkflowAction: CANCEL - Could not find tracking record to be finished.");

        // expect this user to be assigned to a map record in this tracking
        // record
        if (!getMapUsersForTrackingRecord(trackingRecord).contains(mapUser))
          throw new Exception(
              "ProcessWorkflowAction: CANCEL - User not assigned to record for cancel request");

        mapRecords =
            algorithmHandler.cancelWork(trackingRecord, mapRecords, mapUser);

        break;
      default:
        throw new Exception("Unknown action requested.");
    }

    Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");

    // synchronize the map records via helper function
    Set<MapRecord> syncedRecords =
        synchronizeMapRecords(trackingRecord, mapRecords);

    // clear the pointer fields (i.e. ids and names of mapping
    // objects)
    trackingRecord.setMapRecordIds(null);
    trackingRecord.setAssignedUserNames(null);
    trackingRecord.setUserAndWorkflowStatusPairs(null);

    // recalculate the pointer fields
    for (MapRecord mr : syncedRecords) {
      trackingRecord.addMapRecordId(mr.getId());
      trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
      trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner().getUserName(),
          mr.getWorkflowStatus().toString());
    }

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Revised tracking record: " + trackingRecord.toString());

    // if the tracking record is ready for removal, delete it
    if ((getWorkflowStatusForTrackingRecord(trackingRecord).equals(
        WorkflowStatus.READY_FOR_PUBLICATION) || getWorkflowStatusForTrackingRecord(
        trackingRecord).equals(WorkflowStatus.PUBLISHED))
        && trackingRecord.getMapRecordIds().size() == 1) {

      Logger.getLogger(WorkflowServiceJpa.class).info(
          "  Publication ready, removing tracking record.");
      removeTrackingRecord(trackingRecord.getId());

      // else add the tracking record if new
    } else if (trackingRecord.getId() == null) {
      Logger.getLogger(WorkflowServiceJpa.class).info(
          "  New workflow concept, adding tracking record.");
      addTrackingRecord(trackingRecord);

      // otherwise update the tracking record
    } else {
      Logger.getLogger(WorkflowServiceJpa.class).info(
          "  Still in workflow, updating tracking record.");
      updateTrackingRecord(trackingRecord);
    }

  }

  /**
   * Algorithm has gotten needlessly complex due to conflicting service changes
   * and algorithm handler changes. However, the basic process is this:
   * 
   * 1) Function takes a set of map records returned from the algorithm handler
   * These map records may have a hibernate id (updated/unchanged) or not
   * (added) 2) The passed map records are detached from the persistence
   * environment. 3) The existing (in database) records are re-retrieved from
   * the database. Note that this is why the passed map records are detached --
   * otherwise they are overwritten. 4) Each record in the detached set is
   * checked against the 'refreshed' database record set - if the detached
   * record is not in the set, then it has been added - if the detached record
   * is in the set, check it for updates - if it has been changed, update it -
   * if no change, disregard 5) Each record in the 'refreshed' databased record
   * set is checked against the new set - if the refreshed record is not in the
   * new set, delete it from the database 6) Return the detached set as
   * re-synchronized with the database
   * 
   * Note on naming conventions used in this method: - mapRecords: the set of
   * records passed in as argument - newRecords: The set of records to be
   * returned after synchronization - oldRecords: The set of records retrieved
   * by id from the database for comparison - syncedRecords: The synchronized
   * set of records for return from this routine
   * 
   * @param trackingRecord the tracking record
   * @param mapRecords the map records
   * @return the sets the
   * @throws Exception the exception
   */
  @Override
  public Set<MapRecord> synchronizeMapRecords(TrackingRecord trackingRecord,
    Set<MapRecord> mapRecords) throws Exception {

    Set<MapRecord> newRecords = new HashSet<>();
    Set<MapRecord> oldRecords = new HashSet<>();
    Set<MapRecord> syncedRecords = new HashSet<>();

    // detach the currently persisted map records from the workflow service
    // to avoid overwrite by retrieval of existing records
    for (MapRecord mr : mapRecords) {
      manager.detach(mr);
      newRecords.add(mr);
    }

    // Instantiate the mapping service
    MappingService mappingService = new MappingServiceJpa();

    // retrieve the old (existing) records
    if (trackingRecord.getMapRecordIds() != null) {
      for (Long id : trackingRecord.getMapRecordIds()) {
        oldRecords.add(mappingService.getMapRecord(id));
      }
    }

    // cycle over new records to check for additions or updates
    for (MapRecord mr : newRecords) {
      if (getMapRecordInSet(oldRecords, mr.getId()) == null) {

        // deep copy the detached record into a new
        // persistence-environment record
        // this routine also duplicates child collections to avoid
        // detached object errors
        MapRecord newRecord = new MapRecordJpa(mr, false);

        /*
         * Logger.getLogger(WorkflowServiceJpa.class).info( "Adding record: " +
         * newRecord.toString());
         */
        // add the record to the database

        mappingService.addMapRecord(newRecord);

        // add the record to the return list
        syncedRecords.add(newRecord);
      }

      // otherwise, check for update
      else {
        // if the old map record is changed, update it
        /*
         * Logger.getLogger(WorkflowServiceJpa.class).info( "New record: " +
         * mr.toString()); Logger.getLogger(WorkflowServiceJpa.class).info(
         * "Old record: " + getMapRecordInSet(oldRecords,
         * mr.getId()).toString());
         */

        if (!mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
          Logger.getLogger(WorkflowServiceJpa.class)
              .info("  Changed: UPDATING");
          mappingService.updateMapRecord(mr);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class).info(
              "  No change: NOT UPDATING");
        }

        syncedRecords.add(mr);
      }
    }

    // cycle over old records to check for deletions
    for (MapRecord mr : oldRecords) {

      // if old record is not in the new record set, delete it
      if (getMapRecordInSet(syncedRecords, mr.getId()) == null) {

        Logger.getLogger(WorkflowServiceJpa.class).info(
            "Deleting record " + mr.getId());
        mappingService.removeMapRecord(mr.getId());
      }
    }

    // close the service
    mappingService.close();

    return syncedRecords;

  }

  /**
   * Gets the map record in set.
   * 
   * @param mapRecords the map records
   * @param mapRecordId the map record id
   * @return the map record in set
   */

  private MapRecord getMapRecordInSet(Set<MapRecord> mapRecords,
    Long mapRecordId) {
    if (mapRecordId == null)
      return null;

    for (MapRecord mr : mapRecords) {
      if (mapRecordId.equals(mr.getId()))
        return mr;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#computeWorkflow(org.ihtsdo
   * .otf.mapping.model.MapProject)
   */
  @Override
  public void computeWorkflow(MapProject mapProject) throws Exception {

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Start computing workflow for " + mapProject.getName());

    // set the transaction parameter and tracking variables
    setTransactionPerOperation(false);
    int commitCt = 1000;
    int trackingRecordCt = 0;

    // Clear the workflow for this project
    Logger.getLogger(WorkflowServiceJpa.class).info("  Clear old workflow");
    clearWorkflowForMapProject(mapProject);

    // open the services
    ContentService contentService = new ContentServiceJpa();
    MappingService mappingService = new MappingServiceJpa();

    // get the concepts in scope
    SearchResultList conceptsInScope =
        mappingService.findConceptsInScope(mapProject.getId(), null);

    // construct a hashset of concepts in scope
    Set<String> conceptIds = new HashSet<>();
    for (SearchResult sr : conceptsInScope.getIterable()) {
      conceptIds.add(sr.getTerminologyId());
    }

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "  Concept ids put into hash set: " + conceptIds.size());

    // get the current records
    MapRecordList mapRecords =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Processing existing records (" + mapRecords.getCount() + " found)");

    // instantiate a mapped set of non-published records
    Map<String, List<MapRecord>> unpublishedRecords = new HashMap<>();

    // cycle over the map records, and remove concept ids if a map record is
    // publication-ready
    for (MapRecord mapRecord : mapRecords.getIterable()) {

      // if this map record is published, skip and remove this concept
      if (mapRecord.getWorkflowStatus().equals(
          WorkflowStatus.READY_FOR_PUBLICATION)
          || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {

        conceptIds.remove(mapRecord.getConceptId());
      }

      // if this concept is in scope, add to workflow
      else if (conceptIds.contains(mapRecord.getConceptId())) {

        List<MapRecord> originIds;

        // if this key does not yet have a constructed list, make one,
        // otherwise get the existing list
        if (unpublishedRecords.containsKey(mapRecord.getConceptId())) {
          originIds = unpublishedRecords.get(mapRecord.getConceptId());
        } else {
          originIds = new ArrayList<>();
        }

        originIds.add(mapRecord);
        unpublishedRecords.put(mapRecord.getConceptId(), originIds);
      }
    }

    Logger.getLogger(WorkflowServiceJpa.class)
        .info(
            "  Concepts with no publication-ready map record: "
                + conceptIds.size());
    Logger.getLogger(WorkflowServiceJpa.class).info(
        "  Concepts with unpublished map record content:  "
            + unpublishedRecords.size());

    beginTransaction();

    // construct the tracking records for unmapped concepts
    for (String terminologyId : conceptIds) {

      // retrieve the concept for this result
      Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // if concept could not be retrieved, throw exception
      if (concept == null) {
        throw new Exception("Failed to retrieve concept " + terminologyId);
      }

      // skip inactive concepts
      if (!concept.isActive()) {
        Logger.getLogger(WorkflowServiceJpa.class).warn(
            "Skipped inactive concept " + terminologyId);
        continue;
      }

      // get the tree positions for this concept and set the sort key to
      // the first retrieved
      TreePositionList treePositionsList =
          contentService.getTreePositions(concept.getTerminologyId(),
              concept.getTerminology(), concept.getTerminologyVersion());

      // if no tree position, throw exception
      if (treePositionsList.getCount() == 0) {
        throw new Exception("Active concept " + terminologyId
            + " has no tree positions");
      }

      // create a workflow tracking record for this concept
      TrackingRecord trackingRecord = new TrackingRecordJpa();

      // populate the fields from project and concept
      trackingRecord.setMapProjectId(mapProject.getId());
      trackingRecord.setTerminology(concept.getTerminology());
      trackingRecord.setTerminologyId(concept.getTerminologyId());
      trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
      trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
      trackingRecord.setSortKey(treePositionsList.getTreePositions().get(0)
          .getAncestorPath());

      // add any existing map records to this tracking record
      Set<MapRecord> mapRecordsForTrackingRecord = new HashSet<>();
      if (unpublishedRecords.containsKey(trackingRecord.getTerminologyId())) {
        for (MapRecord mr : unpublishedRecords.get(trackingRecord
            .getTerminologyId())) {
          Logger.getLogger(WorkflowServiceJpa.class).info(
              "    Adding existing map record " + mr.getId() + ", owned by "
                  + mr.getOwner().getUserName() + " to tracking record for "
                  + trackingRecord.getTerminologyId());

          trackingRecord.addMapRecordId(mr.getId());
          trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
          trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner()
              .getUserName(), mr.getWorkflowStatus().toString());

          // add to the local set for workflow calculation
          mapRecordsForTrackingRecord.add(mr);
        }
      }

      // check if REVISION record is present
      // check if QA record is present
      boolean revisionRecordPresent = false;
      boolean qaRecordPresent = false;
      for (MapRecord mr : mapRecordsForTrackingRecord) {
        if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION))
          revisionRecordPresent = true;
        if (mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)
            || mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
            || mr.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
            || mr.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)) {
          qaRecordPresent = true;
        }
      }

      // if REVISION found and no qa records, set to FIX_ERROR_PATH
      if (revisionRecordPresent && !qaRecordPresent) {
        trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
        // if REVISION found and qa records, set to QA_PATH
      } else if (revisionRecordPresent && qaRecordPresent) {
        trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);
        // otherwise, set to the WorkflowPath corresponding to the
        // project WorkflowType
      } else {
        if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT))
          trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);
        else if (mapProject.getWorkflowType().equals(
            WorkflowType.REVIEW_PROJECT))
          trackingRecord.setWorkflowPath(WorkflowPath.REVIEW_PROJECT_PATH);
        else {
          throw new Exception("Could not set workflow path from workflow type "
              + mapProject.getWorkflowType() + " for records "
              + trackingRecord.getMapRecordIds().toString());
        }
      }

      addTrackingRecord(trackingRecord);

      if (++trackingRecordCt % commitCt == 0) {
        Logger.getLogger(WorkflowServiceJpa.class).info(
            "  " + trackingRecordCt + " tracking records created");
        commit();
        beginTransaction();

        // close and re-instantiate the content service to prevent
        // memory buildup from Concept and TreePosition objects
        contentService.close();
        contentService = new ContentServiceJpa();
      }
    }

    // commit any remaining transactions
    commit();

    // instantiate the full text eneity manager and set version
    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);
    fullTextEntityManager.setProperty("Version", Version.LUCENE_36);

    // create the indexes
    Logger.getLogger(WorkflowServiceJpa.class).info(
        "  Creating indexes for TrackingRecordJpa");
    fullTextEntityManager.purgeAll(TrackingRecordJpa.class);
    fullTextEntityManager.flushToIndexes();
    fullTextEntityManager.createIndexer(TrackingRecordJpa.class)
        .batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
        .threadsToLoadObjects(4).threadsForSubsequentFetching(8).startAndWait();

    Logger.getLogger(WorkflowServiceJpa.class).info("Done.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#clearWorkflowForMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void clearWorkflowForMapProject(MapProject mapProject)
    throws Exception {

    int commitCt = 0;
    int commitInterval = 1000;

    // begin transaction not in transaction-per-operation mode
    if (!getTransactionPerOperation()) {
      beginTransaction();
    }

    for (TrackingRecord tr : getTrackingRecordsForMapProject(mapProject)
        .getTrackingRecords()) {

      removeTrackingRecord(tr.getId());

      if (++commitCt % commitInterval == 0) {

        // if not a transaction for every operation, commit at intervals
        if (!getTransactionPerOperation()) {
          commit();
          beginTransaction();
        }

        Logger.getLogger(WorkflowServiceJpa.class).info(
            "  Removed " + commitCt + " tracking records");
      }
    }

    // commit any last deletions if not in transaction-per-operation mode
    if (!getTransactionPerOperation()) {
      commit();
    }

  }

  // //////////////////////////
  // Utility functions
  // //////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getMapRecordsForTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public Set<MapRecord> getMapRecordsForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    Set<MapRecord> mapRecords = new HashSet<>();
    MappingService mappingService = new MappingServiceJpa();
    if (trackingRecord != null && trackingRecord.getMapRecordIds() != null) {
      for (Long id : trackingRecord.getMapRecordIds()) {
        mapRecords.add(mappingService.getMapRecord(id));
      }
    }
    mappingService.close();
    return mapRecords;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getMapUsersForTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public MapUserList getMapUsersForTrackingRecord(TrackingRecord trackingRecord)
    throws Exception {
    return getMapUsersFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getWorkflowStatusForTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public WorkflowStatus getWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    return getWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getLowestWorkflowStatusForTrackingRecord
   * (org.ihtsdo.otf.mapping.workflow.TrackingRecord)
   */
  @Override
  public WorkflowStatus getLowestWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    return getLowestWorkflowStatusFromMapRecords(getMapRecordsForTrackingRecord(trackingRecord));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#getMapUsersFromMapRecords
   * (java.util.Set)
   */
  @Override
  public MapUserList getMapUsersFromMapRecords(Set<MapRecord> mapRecords) {
    MapUserList mapUserList = new MapUserListJpa();
    for (MapRecord mr : mapRecords) {
      mapUserList.addMapUser(mr.getOwner());
    }
    return mapUserList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getWorkflowStatusFromMapRecords(java.util.Set)
   */
  @Override
  public WorkflowStatus getWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.NEW;
    for (MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getLowestWorkflowStatusFromMapRecords(java.util.Set)
   */
  @Override
  public WorkflowStatus getLowestWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.REVISION;
    for (MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#computeWorkflowStatusErrors
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public List<String> computeWorkflowStatusErrors(MapProject mapProject)
    throws Exception {

    List<String> results = new ArrayList<>();

    // instantiate the mapping service
    MappingService mappingService = new MappingServiceJpa();

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Retrieving tracking records for project " + mapProject.getId() + ", "
            + mapProject.getName());

    // instantiate a copy of all workflow handlers
    WorkflowNonLegacyPathHandler nonLegacyHandler =
        new WorkflowNonLegacyPathHandler();
    WorkflowFixErrorPathHandler fixErrorHandler =
        new WorkflowFixErrorPathHandler();
    WorkflowQaPathHandler qaHandler = new WorkflowQaPathHandler();
    WorkflowReviewProjectPathHandler reviewHandler =
        new WorkflowReviewProjectPathHandler();

    // get all the tracking records for this project
    TrackingRecordList trackingRecords =
        this.getTrackingRecordsForMapProject(mapProject);

    // construct a set of terminology ids for which a tracking record exists
    Set<String> terminologyIdsWithTrackingRecord = new HashSet<>();

    for (TrackingRecord trackingRecord : trackingRecords.getTrackingRecords()) {

      terminologyIdsWithTrackingRecord.add(trackingRecord.getTerminologyId());

      // instantiate the handler based on tracking record workflow type
      AbstractWorkflowPathHandler handler = null;
      switch (trackingRecord.getWorkflowPath()) {
        case CONSENSUS_PATH:
          break;
        case DRIP_FEED_REVIEW_PATH:
          break;
        case FIX_ERROR_PATH:
          handler = fixErrorHandler;
          break;
        case LEGACY_PATH:
          break;
        case NON_LEGACY_PATH:
          handler = nonLegacyHandler;
          break;
        case QA_PATH:
          handler = qaHandler;
          break;
        case REVIEW_PROJECT_PATH:
          handler = reviewHandler;
          break;
        default:
          results
              .add("ERROR: Could not determine workflow handler from tracking record for concept "
                  + trackingRecord.getTerminologyId()
                  + " for path: "
                  + trackingRecord.getWorkflowPath().toString());
      }

      ValidationResult result = handler.validateTrackingRecord(trackingRecord);

      if (!result.isValid()) {
        results
            .add(constructErrorMessageStringForTrackingRecordAndValidationResult(
                trackingRecord, result));
      }
    }
    Logger.getLogger(WorkflowServiceJpa.class).info(
        "  Checking map records for " + mapProject.getId() + ", "
            + mapProject.getName());

    // second, check all records for non-publication ready content without
    // tracking record, skip inactive concepts
    ContentService contentService = new ContentServiceJpa();
    for (MapRecord mapRecord : mappingService.getMapRecordsForMapProject(
        mapProject.getId()).getMapRecords()) {

      // if not publication ready
      if (!mapRecord.getWorkflowStatus().equals(
          WorkflowStatus.READY_FOR_PUBLICATION)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {

        Concept concept =
            contentService.getConcept(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion());
        // if no tracking record found for this concept
        // and the concept is active, then report an error
        if (!terminologyIdsWithTrackingRecord
            .contains(mapRecord.getConceptId())
            && concept != null
            && concept.isActive()) {
          results.add("Map Record " + mapRecord.getId() + ": "
              + mapRecord.getWorkflowStatus()
              + " but no tracking record exists (Concept "
              + mapRecord.getConceptId() + " " + mapRecord.getConceptName());
        }
      }
    }
    mappingService.close();
    contentService.close();
    return results;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#computeUntrackedMapRecords
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void computeUntrackedMapRecords(MapProject mapProject)
    throws Exception {

    MappingService mappingService = new MappingServiceJpa();

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Retrieving map records for project " + mapProject.getId() + ", "
            + mapProject.getName());

    MapRecordList mapRecordsInProject =
        mappingService.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "  " + mapRecordsInProject.getCount() + " retrieved");

    // set the reporting interval based on number of tracking records
    int nObjects = 0;
    int nMessageInterval =
        (int) Math.floor(mapRecordsInProject.getCount() / 10);

    Set<MapRecord> recordsUntracked = new HashSet<>();

    for (MapRecord mr : mapRecordsInProject.getIterable()) {

      TrackingRecord tr =
          this.getTrackingRecordForMapProjectAndConcept(mapProject,
              mr.getConceptId());

      // if no tracking record, check that this is a publication ready map
      // record
      if (tr == null) {
        if (!mr.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            && !mr.getWorkflowStatus().equals(
                WorkflowStatus.READY_FOR_PUBLICATION)) {
          recordsUntracked.add(mr);
        }

      }

      if (++nObjects % nMessageInterval == 0) {
        Logger.getLogger(WorkflowServiceJpa.class).info(
            "  " + nObjects + " records processed, " + recordsUntracked.size()
                + " unpublished map records without tracking record");
      }
    }

    mappingService.close();

  }

  // /////////////////////////////////////
  // FEEDBACK FUNCTIONS
  // /////////////////////////////////////

  /**
   * Adds the feedback.
   * 
   * @param feedback the feedback
   * @return the feedback
   */
  @Override
  public Feedback addFeedback(Feedback feedback) {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(feedback);
      tx.commit();
    } else {
      manager.persist(feedback);
    }

    return feedback;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#getFeedbacks()
   */
  @Override
  @SuppressWarnings("unchecked")
  public FeedbackList getFeedbacks() {
    List<Feedback> feedbacks = null;
    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from FeedbackJpa m");
    // Try query
    feedbacks = query.getResultList();
    FeedbackListJpa feedbackList = new FeedbackListJpa();
    feedbackList.setFeedbacks(feedbacks);
    feedbackList.setTotalCount(feedbacks.size());

    return feedbackList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#addFeedbackConversation
   * (org.ihtsdo.otf.mapping.model.FeedbackConversation)
   */
  @Override
  public FeedbackConversation addFeedbackConversation(
    FeedbackConversation conversation) {

    // set the conversation of all elements of this conversation
    conversation.assignToChildren();

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(conversation);
      tx.commit();
    } else {
      manager.persist(conversation);
    }

    return conversation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#updateFeedbackConversation
   * (org.ihtsdo.otf.mapping.model.FeedbackConversation)
   */
  @Override
  public void updateFeedbackConversation(FeedbackConversation conversation) {

    // set the conversation of all elements of this conversation
    conversation.assignToChildren();

    if (getTransactionPerOperation()) {

      tx = manager.getTransaction();

      tx.begin();
      manager.merge(conversation);
      tx.commit();
      // manager.close();
    } else {
      manager.merge(conversation);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#getFeedbackConversation
   * (java.lang.Long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversation getFeedbackConversation(Long id) throws Exception {

    // construct query
    javax.persistence.Query query =
        manager
            .createQuery("select m from FeedbackConversationJpa m where mapRecordId = :recordId");

    // Try query
    query.setParameter("recordId", id);
    List<FeedbackConversation> feedbackConversations = query.getResultList();

    if (feedbackConversations != null && feedbackConversations.size() > 0)
      handleFeedbackConversationLazyInitialization(feedbackConversations.get(0));

    Logger.getLogger(this.getClass()).debug(
        "Returning feedback conversation id... "
            + ((feedbackConversations != null) ? id.toString() : "null"));

    return feedbackConversations != null && feedbackConversations.size() > 0
        ? feedbackConversations.get(0) : null;
  }

  /**
   * Handle feedback conversation lazy initialization.
   *
   * @param feedbackConversation the feedback conversation
   */
  private void handleFeedbackConversationLazyInitialization(
    FeedbackConversation feedbackConversation) {
    // handle all lazy initializations
    for (Feedback feedback : feedbackConversation.getFeedbacks()) {
      feedback.getSender().getName();
      for (MapUser recipient : feedback.getRecipients())
        recipient.getName();
      for (MapUser viewedBy : feedback.getViewedBy())
        viewedBy.getName();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getFeedbackConversationsForConcept(java.lang.Long, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversationList getFeedbackConversationsForConcept(
    Long mapProjectId, String terminologyId) throws Exception {

    MappingService mappingService = new MappingServiceJpa();
    MapProject mapProject = mappingService.getMapProject(mapProjectId);
    mappingService.close();

    javax.persistence.Query query =
        manager
            .createQuery(
                "select m from FeedbackConversationJpa m where terminology = :terminology and"
                    + " terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
            .setParameter("terminology", mapProject.getSourceTerminology())
            .setParameter("terminologyVersion",
                mapProject.getSourceTerminologyVersion())
            .setParameter("terminologyId", terminologyId);

    List<FeedbackConversation> feedbackConversations = query.getResultList();
    for (FeedbackConversation feedbackConversation : feedbackConversations) {
      handleFeedbackConversationLazyInitialization(feedbackConversation);
    }

    // set the total count
    FeedbackConversationListJpa feedbackConversationList =
        new FeedbackConversationListJpa();
    feedbackConversationList.setTotalCount(feedbackConversations.size());

    // extract the required sublist of feedback conversations
    feedbackConversationList.setFeedbackConversations(feedbackConversations);

    return feedbackConversationList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * findFeedbackConversationsForProject(java.lang.Long, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings({
    "unchecked"
  })
  @Override
  public FeedbackConversationList findFeedbackConversationsForProject(
    Long mapProjectId, String userName, String query, PfsParameter pfsParameter)
    throws Exception {

    MappingService mappingService = new MappingServiceJpa();
    MapProject mapProject = mappingService.getMapProject(mapProjectId);
    mappingService.close();

    // remove from the query the viewed parameter, if it exists
    // viewed will be handled later because it is on the Feedback object,
    // not the FeedbackConversation object
    String modifiedQuery = "";
    if (query.contains(" AND viewed:false"))
      modifiedQuery = query.replace(" AND viewed:false", "");
    else if (query.contains(" AND viewed:true"))
      modifiedQuery = query.replace(" AND viewed:true", "");
    else
      modifiedQuery = query;

    // construct basic query
    String fullQuery =
        constructMapProjectIdQuery(mapProject.getId(), modifiedQuery);

    fullQuery +=
        " AND terminology:" + mapProject.getSourceTerminology()
            + " AND terminologyVersion:"
            + mapProject.getSourceTerminologyVersion() + " AND "
            + "( feedbacks.sender.userName:" + userName + " OR "
            + "feedbacks.recipients.userName:" + userName + ")";

    Logger.getLogger(MappingServiceJpa.class).info(fullQuery);

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct luceneQuery based on URL format

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(FeedbackConversationJpa.class));
    try {
      luceneQuery = queryParser.parse(fullQuery);
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
    org.hibernate.search.jpa.FullTextQuery ftquery =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            FeedbackConversationJpa.class);

    // Sort Options -- in order of priority
    // (1) if a sort field is specified by pfs parameter, use it
    // (2) if a query has been specified, use nothing (lucene relevance
    // default)
    // (3) if a query has not been specified, sort by conceptId

    String sortField = "lastModified";
    if (pfsParameter != null && pfsParameter.getSortField() != null
        && !pfsParameter.getSortField().isEmpty()) {
      ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
          SortField.STRING, true)));
    } else if (pfsParameter != null
        && pfsParameter.getQueryRestriction() != null
        && !pfsParameter.getQueryRestriction().isEmpty()) {
      // do nothing
    } else {
      ftquery
          .setSort(new Sort(new SortField(sortField, SortField.STRING, true)));
    }

    // get the results
    int totalCount = ftquery.getResultSize();

    if (pfsParameter != null && !query.contains("viewed")) {
      ftquery.setFirstResult(pfsParameter.getStartIndex());
      ftquery.setMaxResults(pfsParameter.getMaxResults());
    }

    List<FeedbackConversation> feedbackConversations = ftquery.getResultList();

    if (pfsParameter != null && query.contains("viewed")) {
      List<FeedbackConversation> conversationsToKeep = new ArrayList<>();
      for (FeedbackConversation fc : feedbackConversations) {
        if (query.contains("viewed:false")) {
          for (Feedback feedback : fc.getFeedbacks()) {
            Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
            boolean found = false;
            for (MapUser user : alreadyViewedBy) {
              if (user.getUserName().equals(userName))
                found = true;
            }
            if (!found)
              conversationsToKeep.add(fc);
          }
        }
        if (query.contains("viewed:true")) {
          boolean found = false;
          for (Feedback feedback : fc.getFeedbacks()) {
            Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
            for (MapUser user : alreadyViewedBy) {
              if (user.getUserName().equals(userName)) {
                found = true;
                break;
              }
            }
            if (!found)
              break;
          }
          if (found)
            conversationsToKeep.add(fc);
        }
      }
      totalCount = conversationsToKeep.size();
      feedbackConversations.clear();
      for (int i = pfsParameter.getStartIndex(); i < pfsParameter
          .getStartIndex() + pfsParameter.getMaxResults()
          && i < conversationsToKeep.size(); i++) {
        feedbackConversations.add(conversationsToKeep.get(i));
      }

    }

    Logger.getLogger(this.getClass()).debug(
        Integer.toString(feedbackConversations.size())
            + " feedbackConversations retrieved");

    for (FeedbackConversation feedbackConversation : feedbackConversations) {
      handleFeedbackConversationLazyInitialization(feedbackConversation);
    }

    // set the total count
    FeedbackConversationListJpa feedbackConversationList =
        new FeedbackConversationListJpa();
    feedbackConversationList.setTotalCount(totalCount);

    // extract the required sublist of feedback conversations
    feedbackConversationList.setFeedbackConversations(feedbackConversations);

    return feedbackConversationList;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.WorkflowService#
   * getFeedbackConversationsForRecord(java.lang.Long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversationList getFeedbackConversationsForRecord(
    Long mapRecordId) throws Exception {

    javax.persistence.Query query =
        manager
            .createQuery(
                "select m from FeedbackConversationJpa m where mapRecordId=:mapRecordId")
            .setParameter("mapRecordId", mapRecordId);

    List<FeedbackConversation> feedbackConversations = query.getResultList();
    for (FeedbackConversation feedbackConversation : feedbackConversations) {
      handleFeedbackConversationLazyInitialization(feedbackConversation);
    }

    // set the total count
    FeedbackConversationListJpa feedbackConversationList =
        new FeedbackConversationListJpa();
    feedbackConversationList.setTotalCount(feedbackConversations.size());

    // extract the required sublist of feedback conversations
    feedbackConversationList.setFeedbackConversations(feedbackConversations);

    return feedbackConversationList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#getFeedbackErrorsForRecord
   * (org.ihtsdo.otf.mapping.model.MapRecord)
   */
  @Override
  public FeedbackList getFeedbackErrorsForRecord(MapRecord mapRecord)
    throws Exception {

    List<Feedback> feedbacksWithError = new ArrayList<>();

    // find any feedback conersations for this record
    FeedbackConversationList conversations =
        this.getFeedbackConversationsForRecord(mapRecord.getId());

    // cycle over feedbacks
    for (FeedbackConversation conversation : conversations.getIterable()) {
      for (Feedback feedback : conversation.getFeedbacks()) {
        if (feedback.getIsError()) {
          feedbacksWithError.add(feedback);

        }

      }
    }
    FeedbackList feedbackList = new FeedbackListJpa();
    feedbackList.setFeedbacks(feedbacksWithError);
    return feedbackList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.WorkflowService#sendFeedbackEmail(java.
   * lang.String, java.lang.String, java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void sendFeedbackEmail(String name, String email, String conceptId,
    String conceptName, String refSetId, String message) throws Exception {
    OtfEmailHandler emailHandler = new OtfEmailHandler();
    // get to address from config.properties
    Properties config = ConfigUtility.getConfigProperties();
    String feedbackUserRecipient =
        config.getProperty("mail.smtp.to.feedback.user");
    String baseUrlWebapp = config.getProperty("base.url.webapp");
    String conceptUrl =
        baseUrlWebapp + "/#/record/conceptId/" + conceptId
            + "/autologin?refSetId=" + refSetId;

    emailHandler.sendSimpleEmail(feedbackUserRecipient, email,
        "Mapping Tool User Feedback: " + conceptId + "-" + conceptName,
        "User: " + name + "<br>" + "Email: " + email + "<br>"
            + "Concept: <a href=" + conceptUrl + ">" + conceptId + "- "
            + conceptName + "</a><br><br>" + message);

  }

  /**
   * Construct error message string for tracking record and validation result.
   *
   * @param trackingRecord the tracking record
   * @param result the result
   * @return the string
   * @throws Exception the exception
   */
  private String constructErrorMessageStringForTrackingRecordAndValidationResult(
    TrackingRecord trackingRecord, ValidationResult result) throws Exception {

    StringBuffer message = new StringBuffer();

    message.append("ERROR for Concept " + trackingRecord.getTerminologyId()
        + ", Path " + trackingRecord.getWorkflowPath().toString() + "\n");

    // record information
    message.append("  Records involved:\n");
    message.append("    " + "id\tUser\tWorkflowStatus\n");

    for (MapRecord mr : getMapRecordsForTrackingRecord(trackingRecord)) {
      message.append("    " + mr.getId().toString() + "\t"
          + mr.getOwner().getUserName() + "\t"
          + mr.getWorkflowStatus().toString() + "\n");
    }

    message.append("  Errors reported:\n");

    for (String error : result.getErrors()) {
      message.append("    " + error + "\n");
    }

    return message.toString();
  }
}
