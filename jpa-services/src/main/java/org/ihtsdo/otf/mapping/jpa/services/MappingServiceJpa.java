/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.criterion.MatchMode;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeList;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleList;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapRelationList;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList;
import org.ihtsdo.otf.mapping.helpers.MapUserPreferencesListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * JPA implementation of the {@link MappingService}.
 */
public class MappingServiceJpa extends RootServiceJpa implements MappingService {

  /** The commit count. */
  private final static int commitCt = 2000;

  /** The map record indexed field names. */
  protected static Set<String> mapRecordFieldNames;

  /** The map record indexed field names. */
  protected static Set<String> mapProjectFieldNames;

  /**
   * Instantiates an empty {@link MappingServiceJpa}.
   * 
   * @throws Exception the exception
   */
  public MappingServiceJpa() throws Exception {
    super();
    if (mapRecordFieldNames == null) {
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
    mapRecordFieldNames = new HashSet<>();
    mapProjectFieldNames = new HashSet<>();
    Map<String, Set<String>> fieldNamesMap = new HashMap<>();
    fieldNamesMap.put("MapRecordJpa", mapRecordFieldNames);
    fieldNamesMap.put("MapProjectJpa", mapProjectFieldNames);
    EntityManager manager = factory.createEntityManager();
    FullTextEntityManager fullTextEntityManager =
        org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
    IndexReaderAccessor indexReaderAccessor =
        fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
    Set<String> indexedClassNames =
        fullTextEntityManager.getSearchFactory().getStatistics()
            .getIndexedClassNames();
    for (String indexClass : indexedClassNames) {
      Set<String> fieldNames = null;
      if (indexClass.indexOf("MapRecordJpa") != -1) {
        Logger.getLogger(ContentServiceJpa.class).info(
            "FOUND MapRecordJpa index");
        fieldNames = fieldNamesMap.get("MapRecordJpa");
      } else if (indexClass.indexOf("MapProjectJpa") != -1) {
        Logger.getLogger(ContentServiceJpa.class).info(
            "FOUND MapProjectJpa index");
        fieldNames = fieldNamesMap.get("MapProjectJpa");
      }
      if (fieldNames != null) {
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
    fullTextEntityManager.close();
  }

  /**
   * Close the manager when done with this service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
    }
  }

  // //////////////////////////////////
  // MapProject
  // - getMapProjects
  // - getMapProject(Long id)
  // - getMapProject(String name)
  // - findMapProjects(String query)
  // - addMapProject(MapProject mapProject)
  // - updateMapProject(MapProject mapProject)
  // - removeMapProject(MapProject mapProject)
  // //////////////////////////////////

  /**
   * Return map project for auto-generated id.
   * 
   * @param id the auto-generated id
   * @return the MapProject
   * @throws Exception the exception
   */
  @Override
  public MapProject getMapProject(Long id) throws Exception {

    MapProject m = null;

    javax.persistence.Query query =
        manager.createQuery("select m from MapProjectJpa m where id = :id");
    query.setParameter("id", id);

    m = (MapProject) query.getSingleResult();
    m.getScopeConcepts().size();
    m.getScopeExcludedConcepts().size();
    m.getMapAdvices().size();
    m.getMapRelations().size();
    m.getMapLeads().size();
    m.getMapSpecialists().size();
    m.getMapPrinciples().size();
    m.getPresetAgeRanges().size();
    return m;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapProjectForRefSetId
   * (java.lang.String)
   */
  @Override
  public MapProject getMapProjectForRefSetId(String refSetId) throws Exception {

    MapProject m = null;

    javax.persistence.Query query =
        manager.createQuery(
            "select m from MapProjectJpa m where refSetId = :refSetId")
            .setParameter("refSetId", refSetId);

    m = (MapProject) query.getSingleResult();
    m.getScopeConcepts().size();
    m.getScopeExcludedConcepts().size();
    m.getMapAdvices().size();
    m.getMapRelations().size();
    m.getMapLeads().size();
    m.getMapSpecialists().size();
    m.getMapPrinciples().size();
    m.getPresetAgeRanges().size();
    return m;
  }

  /**
   * Retrieve all map projects.
   * 
   * @return a List of MapProjects
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapProjectList getMapProjects() {

    List<MapProject> mapProjects = null;

    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from MapProjectJpa m");

    mapProjects = query.getResultList();

    // force instantiation of lazy collections
    for (MapProject mapProject : mapProjects) {
      handleMapProjectLazyInitialization(mapProject);
    }

    MapProjectListJpa mapProjectList = new MapProjectListJpa();
    mapProjectList.setMapProjects(mapProjects);
    mapProjectList.setTotalCount(mapProjects.size());
    return mapProjectList;
  }

  /**
   * Query for MapProjects.
   * 
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @return the list of MapProject
   * @throws Exception the exception
   */
  @Override
  @SuppressWarnings("unchecked")
  public SearchResultList findMapProjectsForQuery(String query,
    PfsParameter pfsParameter) throws Exception {

    SearchResultList s = new SearchResultListJpa();

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    try {
      // construct luceneQuery based on URL format
      if (query.indexOf(':') == -1) { // no fields indicated
        MultiFieldQueryParser queryParser =
            new MultiFieldQueryParser(Version.LUCENE_36,
                mapProjectFieldNames.toArray(new String[0]),
                searchFactory.getAnalyzer(MapProjectJpa.class));
        queryParser.setAllowLeadingWildcard(false);
        luceneQuery = queryParser.parse(query);

      } else { // field:value
        QueryParser queryParser =
            new QueryParser(Version.LUCENE_36, "summary",
                searchFactory.getAnalyzer(MapProjectJpa.class));
        luceneQuery = queryParser.parse(query);
      }
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }

    List<MapProject> m;

    m =
        fullTextEntityManager.createFullTextQuery(luceneQuery,
            MapProjectJpa.class).getResultList();
    // if a parse exception, throw a local exception

    Logger.getLogger(this.getClass()).debug(
        Integer.toString(m.size()) + " map projects retrieved");

    for (MapProject mp : m) {
      s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId()
          .toString(), mp.getName(), ""));
    }

    // Sort by ID
    s.sortBy(new Comparator<SearchResult>() {
      @Override
      public int compare(SearchResult o1, SearchResult o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });

    fullTextEntityManager.close();

    // closing fullTextEntityManager also closes manager, recreate
    manager = factory.createEntityManager();

    return s;

  }

  /**
   * Add a map project.
   * 
   * @param mapProject the map project
   * @return the map project
   * @throws Exception the exception
   */
  @Override
  public MapProject addMapProject(MapProject mapProject) throws Exception {

    // check that each user has only one role
    validateUserAndRole(mapProject);

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(mapProject);
      tx.commit();

      return mapProject;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(mapProject);
      return mapProject;
    }

  }

  /**
   * Update a map project.
   * 
   * @param mapProject the changed map project
   * @throws Exception the exception
   */
  @Override
  public void updateMapProject(MapProject mapProject) throws Exception {

    // check that each user has only one role
    validateUserAndRole(mapProject);

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(mapProject);
      tx.commit();
    } else {
      manager.merge(mapProject);
    }

  }

  /**
   * Remove (delete) a map project.
   * 
   * @param mapProjectId the map project to be removed
   */
  @Override
  public void removeMapProject(Long mapProjectId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      // first, remove the leads and specialists from this project
      tx.begin();
      MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
      mp.setMapLeads(null);
      mp.setMapSpecialists(null);
      tx.commit();

      // now remove the entry
      tx.begin();
      if (manager.contains(mp)) {
        manager.remove(mp);
      } else {
        manager.remove(manager.merge(mp));
      }
      tx.commit();
    } else {
      MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
      mp.setMapLeads(null);
      mp.setMapSpecialists(null);
      if (manager.contains(mp)) {
        manager.remove(mp);
      } else {
        manager.remove(manager.merge(mp));
      }
    }

  }

  // ///////////////////////////////////////////////////////////////
  // MapUser
  // - getMapUsers()
  // - getMapProjectsForUser(MapUser mapUser)
  // - findMapUsers(String query)
  // - addMapUser(MapUser mapUser)
  // - updateMapUser(MapUser mapUser)
  // - removeMapUser(Long id)
  // ///////////////////////////////////////////////////////////////

  /**
   * Retrieve all map users.
   * 
   * @return a List of MapUsers
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapUserList getMapUsers() {

    List<MapUser> m = null;

    javax.persistence.Query query =
        manager.createQuery("select m from MapUserJpa m");

    m = query.getResultList();
    MapUserListJpa mapUserList = new MapUserListJpa();
    mapUserList.setMapUsers(m);
    mapUserList.setTotalCount(m.size());
    return mapUserList;
  }

  /* see superclass */
  @Override
  @SuppressWarnings("unchecked")
  public MapUserList getMapUsersForTeam(String team) {

    List<MapUser> m = null;

    javax.persistence.Query query =
        manager.createQuery("select m from MapUserJpa m where team = :team");
    query.setParameter("team", team);
    m = query.getResultList();
    MapUserListJpa mapUserList = new MapUserListJpa();
    mapUserList.setMapUsers(m);
    mapUserList.setTotalCount(m.size());
    return mapUserList;
  }

  /**
   * Return map specialist for auto-generated id.
   * 
   * @param id the auto-generated id
   * @return the MapSpecialist
   * @throws Exception the exception
   */
  @Override
  public MapUser getMapUser(Long id) throws Exception {

    javax.persistence.Query query =
        manager.createQuery("select m from MapUserJpa m where id = :id");
    query.setParameter("id", id);
    return (MapUser) query.getSingleResult();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapUser(java.lang.String
   * )
   */
  @Override
  public MapUser getMapUser(String userName) throws Exception {

    javax.persistence.Query query =
        manager
            .createQuery("select m from MapUserJpa m where userName = :userName");
    query.setParameter("userName", userName);
    return (MapUser) query.getSingleResult();
  }

  /**
   * Retrieve all map projects assigned to a particular map specialist.
   * 
   * @param mapUser the map user
   * @return a List of MapProjects
   */
  @Override
  public MapProjectList getMapProjectsForMapUser(MapUser mapUser) {

    MapProjectList mpList = getMapProjects();
    List<MapProject> mpListReturn = new ArrayList<>();

    // iterate and check for presence of mapUser as specialist
    for (MapProject mp : mpList.getMapProjects()) {

      for (MapUser ms : mp.getMapSpecialists()) {
        if (ms.equals(mapUser)) {
          mpListReturn.add(mp);
        }
      }

      for (MapUser ms : mp.getMapLeads()) {
        if (ms.equals(mapUser)) {
          mpListReturn.add(mp);
        }
      }
    }

    // force instantiation of lazy collections
    for (MapProject mapProject : mpListReturn) {
      handleMapProjectLazyInitialization(mapProject);
    }

    MapProjectListJpa mapProjectList = new MapProjectListJpa();
    mapProjectList.setMapProjects(mpListReturn);
    mapProjectList.setTotalCount(mpListReturn.size());
    return mapProjectList;
  }

  /**
   * Update a map specialist.
   * 
   * @param mapUser the changed map user
   */
  @Override
  public void updateMapUser(MapUser mapUser) {

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(mapUser);
      tx.commit();
    } else {
      manager.merge(mapUser);
    }

  }

  /**
   * Remove (delete) a map specialist.
   * 
   * @param mapUserId the map user to be removed
   */
  @Override
  public void removeMapUser(Long mapUserId) {

    tx = manager.getTransaction();

    // retrieve this map specialist
    MapUser mu = manager.find(MapUserJpa.class, mapUserId);

    // retrieve all projects on which this specialist appears
    List<MapProject> projects = getMapProjectsForMapUser(mu).getMapProjects();

    if (getTransactionPerOperation()) {
      // remove specialist from all these projects
      tx.begin();
      for (MapProject mp : projects) {
        mp.removeMapLead(mu);
        mp.removeMapSpecialist(mu);
        manager.merge(mp);
      }
      tx.commit();

      // remove specialist
      tx.begin();
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
      tx.commit();

    } else {
      for (MapProject mp : projects) {
        mp.removeMapLead(mu);
        mp.removeMapSpecialist(mu);
        manager.merge(mp);
      }
      if (manager.contains(mu)) {
        manager.remove(mu);
      } else {
        manager.remove(manager.merge(mu));
      }
    }

  }

  /**
   * Add a map lead.
   * 
   * @param mapUser the map lead
   * @return the map user
   */
  @Override
  public MapUser addMapUser(MapUser mapUser) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(mapUser);
      tx.commit();
    } else {
      manager.persist(mapUser);
    }

    return mapUser;
  }

  // //////////////////////////////////
  // MapRecord
  // //////////////////////////////////

  /**
   * Retrieve all map records.
   * 
   * @return a List of MapRecords
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapRecordList getMapRecords() {
    List<MapRecord> mapRecords = null;
    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from MapRecordJpa m");
    // Try query
    mapRecords = query.getResultList();
    MapRecordListJpa mapRecordList = new MapRecordListJpa();

    for (MapRecord mr : mapRecordList.getIterable()) {
      this.handleMapRecordLazyInitialization(mr);
    }

    mapRecordList.setMapRecords(mapRecords);
    mapRecordList.setTotalCount(mapRecords.size());

    return mapRecordList;
  }

  /**
   * Retrieve map record for given id.
   * 
   * @param id the map record id
   * @return the map record
   * @throws Exception the exception
   */
  @Override
  public MapRecord getMapRecord(Long id) throws Exception {

    /*
     * Try to retrieve the single expected result If zero or more than one
     * result are returned, log error and set result to null
     */

    MapRecord mapRecord = manager.find(MapRecordJpa.class, id);

    if (mapRecord != null)
      handleMapRecordLazyInitialization(mapRecord);

    return mapRecord;
  }

  /**
   * Retrieve map records for a lucene query.
   * 
   * @param query the lucene query string
   * @param pfsParameter the pfs parameter
   * @return a list of map records
   * @throws Exception the exception
   */
  @Override
  @SuppressWarnings("unchecked")
  public SearchResultList findMapRecordsForQuery(String query,
    PfsParameter pfsParameter) throws Exception {

    SearchResultList s = new SearchResultListJpa();

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    try {

      // construct luceneQuery based on URL format
      if (query.indexOf(':') == -1) { // no fields indicated
        MultiFieldQueryParser queryParser =
            new MultiFieldQueryParser(Version.LUCENE_36,
                mapRecordFieldNames.toArray(new String[0]),
                searchFactory.getAnalyzer(MapRecordJpa.class));
        queryParser.setAllowLeadingWildcard(false);
        luceneQuery = queryParser.parse(query);

      } else { // field:value
        QueryParser queryParser =
            new QueryParser(Version.LUCENE_36, "summary",
                searchFactory.getAnalyzer(MapRecordJpa.class));
        luceneQuery = queryParser.parse(query);
      }

      List<MapRecord> mapRecords =
          fullTextEntityManager.createFullTextQuery(luceneQuery,
              MapRecordJpa.class).getResultList();

      Logger.getLogger(this.getClass()).debug(
          Integer.toString(mapRecords.size()) + " map records retrieved");

      for (MapRecord mapRecord : mapRecords) {
        s.addSearchResult(new SearchResultJpa(mapRecord.getId(), mapRecord
            .getConceptId().toString(), mapRecord.getConceptName(), ""));
      }

      // Sort by ID
      s.sortBy(new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          return o1.getId().compareTo(o2.getId());
        }
      });

      fullTextEntityManager.close();

      // closing fullTextEntityManager also closes manager, recreate
      manager = factory.createEntityManager();

      return s;

    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }
  }

  /**
   * Add a map record.
   * 
   * @param mapRecord the map record to be added
   * @return the map record
   * @throws Exception the exception
   */
  @Override
  public MapRecord addMapRecord(MapRecord mapRecord) throws Exception {

    // check if user valid
    if (mapRecord.getOwner() == null) {
      throw new Exception("Map Record requires valid user in owner field");
    }

    // check if last modified by user valid
    if (mapRecord.getLastModifiedBy() == null) {
      throw new Exception(
          "Map Record requires valid user in lastModifiedBy field");
    }

    // set the map record of all elements of this record
    mapRecord.assignToChildren();

    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapRecord);
      tx.commit();
      return mapRecord;
    } else {
      manager.persist(mapRecord);
      return mapRecord;
    }

  }

  /**
   * Update a map record.
   * 
   * @param mapRecord the map record to be updated
   */
  @Override
  public void updateMapRecord(MapRecord mapRecord) throws Exception {

    // update last modified timestamp
    mapRecord.setLastModified((new java.util.Date()).getTime());

    // first assign the map record to its children
    mapRecord.assignToChildren();

    // double check that all entries with blank targets are correctly set
    // NOTES: Do not want this to interrupt normal flow if problems occur
    // Algorithm Handler only instantiated if required
    // Warning message used to store any errors for logging/email
    ProjectSpecificAlgorithmHandler algorithmHandler = null;
    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

        // get handler if not already instantiated
        if (algorithmHandler == null) {
          algorithmHandler =
              getProjectSpecificAlgorithmHandler(getMapProject(mapRecord
                  .getMapProjectId()));
        }

        // try to set the default target name
        mapEntry.setTargetId("");
        mapEntry.setTargetName(algorithmHandler
            .getDefaultTargetNameForBlankTarget());
      }
    }

    if (getTransactionPerOperation()) {

      tx = manager.getTransaction();
      tx.begin();
      manager.merge(mapRecord);
      tx.commit();
    } else {
      manager.merge(mapRecord);
    }

  }

  /**
   * Remove (delete) a map record by id.
   * 
   * @param id the id of the map record to be removed
   */
  @Override
  public void removeMapRecord(Long id) {

    tx = manager.getTransaction();

    // find the map record
    MapRecord m = manager.find(MapRecordJpa.class, id);

    if (getTransactionPerOperation()) {
      // delete the map record
      tx.begin();
      if (manager.contains(m)) {
        manager.remove(m);
      } else {
        manager.remove(manager.merge(m));
      }
      tx.commit();

    } else {
      if (manager.contains(m)) {
        manager.remove(m);
      } else {
        manager.remove(manager.merge(m));
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRecordRevisions(
   * java.lang.Long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapRecordList getMapRecordRevisions(Long mapRecordId) {

    AuditReader reader = AuditReaderFactory.get(manager);
    List<MapRecord> revisions = reader.createQuery()

    // all revisions, returned as objects, not finding deleted entries
        .forRevisionsOfEntity(MapRecordJpa.class, true, false)

        // search by id
        .add(AuditEntity.id().eq(mapRecordId))

        // order by descending timestamp
        .addOrder(AuditEntity.property("timestamp").desc())

        // execute query
        .getResultList();

    // construct the map
    MapRecordListJpa mapRecordList = new MapRecordListJpa();
    mapRecordList.setMapRecords(revisions);
    for (MapRecord mapRecord : revisions) {
      handleMapRecordLazyInitialization(mapRecord);
    }
    mapRecordList.setTotalCount(revisions.size());
    return mapRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * getMapRecordRevisionsForConcept(java.lang.String)
   */
  @Override
  public MapRecordList getMapRecordRevisionsForConcept(String conceptId,
    Long mapProjectId) {

    AuditReader reader = AuditReaderFactory.get(manager);
    @SuppressWarnings("unchecked")
    List<MapRecord> revisions = reader.createQuery()

    // all revisions, returned as objects, not finding deleted entries
        .forRevisionsOfEntity(MapRecordJpa.class, true, false)

        // search by conceptId
        .add(AuditEntity.property("conceptId").eq(conceptId))

        // constrain by mapProjectId
        .add(AuditEntity.property("mapProjectId").eq(mapProjectId))

        // order by descending timestamp
        .addOrder(AuditEntity.property("lastModified").desc())

        // execute query
        .getResultList();

    // construct the map keeping only the most recent for each recordId
    MapRecordListJpa mapRecordList = new MapRecordListJpa();
    Set<Long> usedRecordIds = new HashSet<>();
    List<MapRecord> mostRecentHistoricalRecords = new ArrayList<>();
    for (MapRecord mapRecord : revisions) {
      if (!usedRecordIds.contains(mapRecord.getId())) {
        handleMapRecordLazyInitialization(mapRecord);
        usedRecordIds.add(mapRecord.getId());
        mostRecentHistoricalRecords.add(mapRecord);
      }
    }
    mapRecordList.setMapRecords(mostRecentHistoricalRecords);
    mapRecordList.setTotalCount(mapRecordList.getMapRecords().size());
    return mapRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getRecentlyEditedMapRecords
   * (org.ihtsdo.otf.mapping.model.MapUser)
   */
  @SuppressWarnings({
    "unchecked"
  })
  @Override
  public MapRecordList getRecentlyEditedMapRecords(Long projectId,
    String userName, PfsParameter pfsParameter) throws Exception {

    MapUser user = getMapUser(userName);

    AuditReader reader = AuditReaderFactory.get(manager);
    PfsParameter localPfsParameter = pfsParameter;

    // if no pfsParameter supplied, construct a default one
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // split the query restrictions
    if (localPfsParameter.getQueryRestriction() != null) {
      // do nothing
    }

    // construct the query
    AuditQuery query =
        reader.createQuery()

            // all revisions, returned as objects, finding deleted entries
            .forRevisionsOfEntity(MapRecordJpa.class, true, true)

            // add mapProjectId and owner as constraints
            .add(AuditEntity.property("mapProjectId").eq(projectId))
            .add(AuditEntity.relatedId("lastModifiedBy").eq(user.getId()))

            // exclude records with workflow status NEW
            .add(AuditEntity.property("workflowStatus").ne(WorkflowStatus.NEW));

    // if sort field specified
    if (localPfsParameter.getSortField() != null) {
      query.addOrder(AuditEntity.property(localPfsParameter.getSortField())
          .desc());

      // otherwise, sort by last modified (descending)
    } else {
      query.addOrder(AuditEntity.property("lastModified").desc());
    }
    // if paging request supplied, set first result and max results
    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      query.setFirstResult(localPfsParameter.getStartIndex()).setMaxResults(
          localPfsParameter.getMaxResults());

    }

    // if query terms specified, add
    if (pfsParameter != null && pfsParameter.getQueryRestriction() != null) {
      String[] queryTerms = pfsParameter.getQueryRestriction().split(" ");
      query.add(AuditEntity.or(
          AuditEntity.property("conceptId").in(queryTerms),
          AuditEntity.property("conceptName").like(
              pfsParameter.getQueryRestriction(), MatchMode.ANYWHERE)));

    }

    // execute the query
    List<MapRecord> editedRecords = query.getResultList();

    // create the mapRecordList and set total size
    MapRecordListJpa mapRecordList = new MapRecordListJpa();
    // mapRecordList.setTotalCount(editedRecords.size());

    // only add one copy -- note this results in uneven page sizes
    List<MapRecord> uniqueRecords = new ArrayList<>();
    for (MapRecord mapRecord : editedRecords) {
      boolean recordExists = false;
      for (MapRecord mr : uniqueRecords) {
        if (mr.getId().equals(mapRecord.getId()))
          recordExists = true;
      }
      if (!recordExists)
        uniqueRecords.add(mapRecord);
    }

    // handle all lazy initializations
    for (MapRecord mapRecord : uniqueRecords) {
      handleMapRecordLazyInitialization(mapRecord);
    }
    mapRecordList.setMapRecords(uniqueRecords);
    return mapRecordList;
  }

  // //////////////////////////////////
  // Other query services
  // //////////////////////////////////

  // //////////////////////////////////////////////
  // Descendant services
  // //////////////////////////////////////////////

  /**
   * Retrieve map records for a given terminology id.
   * 
   * @param terminologyId the concept id
   * @return the list of map records
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapRecordList getMapRecordsForConcept(String terminologyId) {
    List<MapRecord> mapRecords = null;

    // construct query
    javax.persistence.Query query =
        manager
            .createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

    // Try query
    query.setParameter("conceptId", terminologyId);
    mapRecords = query.getResultList();
    for (MapRecord mapRecord : mapRecords) {
      handleMapRecordLazyInitialization(mapRecord);
    }

    MapRecordListJpa mapRecordList = new MapRecordListJpa();
    mapRecordList.setMapRecords(mapRecords);
    mapRecordList.setTotalCount(mapRecords.size());
    return mapRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject
   * (java.lang.Long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapRecordList getMapRecordsForMapProject(Long mapProjectId)
    throws Exception {

    javax.persistence.Query query =
        manager.createQuery(
            "select m from MapRecordJpa m where mapProjectId = :mapProjectId")
            .setParameter("mapProjectId", mapProjectId);
    MapRecordList mapRecordList = new MapRecordListJpa();
    mapRecordList.setMapRecords(query.getResultList());
    return mapRecordList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * getMapRecordForProjectAndConcept(java.lang.Long, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapRecordList getMapRecordsForProjectAndConcept(Long mapProjectId,
    String terminologyId) throws Exception {

    List<MapRecord> mapRecords =
        manager
            .createQuery(
                "select m from MapRecordJpa m where mapProjectId = :mapProjectId and conceptId = :conceptId")
            .setParameter("mapProjectId", mapProjectId)
            .setParameter("conceptId", terminologyId).getResultList();
    MapRecordList mapRecordList = new MapRecordListJpa();
    for (MapRecord mapRecord : mapRecords) {
      handleMapRecordLazyInitialization(mapRecord);
    }
    mapRecordList.setMapRecords(mapRecords);
    mapRecordList.setTotalCount(mapRecords.size());
    return mapRecordList;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject
   * (java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapRecordList getPublishedAndReadyForPublicationMapRecordsForMapProject(
    Long mapProjectId, PfsParameter pfsParameter) throws Exception {

    // construct basic query
    String fullQuery =
        constructMapRecordForMapProjectIdQuery(mapProjectId,
            pfsParameter == null ? new PfsParameterJpa() : pfsParameter);

    fullQuery +=
        " AND (workflowStatus:'PUBLISHED' OR workflowStatus:'READY_FOR_PUBLICATION')";

    Logger.getLogger(MappingServiceJpa.class).info(fullQuery);

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct luceneQuery based on URL format

    org.hibernate.search.jpa.FullTextQuery ftquery = null;

    try {

      QueryParser queryParser =
          new QueryParser(Version.LUCENE_36, "summary",
              searchFactory.getAnalyzer(MapRecordJpa.class));
      luceneQuery = queryParser.parse(fullQuery);

      ftquery =
          fullTextEntityManager.createFullTextQuery(luceneQuery,
              MapRecordJpa.class);

      // if a parse exception, throw a local exception
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }

    // Sort Options -- in order of priority
    // (1) if a sort field is specified by pfs parameter, use it
    // (2) if a query has been specified, use nothing (lucene relevance
    // default)
    // (3) if a query has not been specified, sort by conceptId

    String sortField = "conceptId";
    if (pfsParameter != null && pfsParameter.getSortField() != null
        && !pfsParameter.getSortField().isEmpty()) {
      ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
          SortField.STRING)));
    } else if (pfsParameter != null
        && pfsParameter.getQueryRestriction() != null
        && !pfsParameter.getQueryRestriction().isEmpty()) {
      // do nothing
    } else {
      ftquery.setSort(new Sort(new SortField(sortField, SortField.STRING)));
    }

    // get the results
    int totalCount;
    List<MapRecord> mapRecords = new ArrayList<>();

    totalCount = ftquery.getResultSize();

    if (pfsParameter != null) {
      ftquery.setFirstResult(pfsParameter.getStartIndex());
      ftquery.setMaxResults(pfsParameter.getMaxResults());
    }
    mapRecords = ftquery.getResultList();

    Logger.getLogger(this.getClass()).debug(
        Integer.toString(mapRecords.size()) + " records retrieved");

    for (MapRecord mapRecord : mapRecords) {
      handleMapRecordLazyInitialization(mapRecord);
    }

    // set the total count
    MapRecordListJpa mapRecordList = new MapRecordListJpa();
    mapRecordList.setTotalCount(totalCount);

    // extract the required sublist of map records
    mapRecordList.setMapRecords(mapRecords);

    return mapRecordList;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject
   * (java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapRecordList getPublishedMapRecordsForMapProject(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception {

    // construct basic query
    String fullQuery =
        constructMapRecordForMapProjectIdQuery(mapProjectId,
            pfsParameter == null ? new PfsParameterJpa() : pfsParameter);

    fullQuery += " AND workflowStatus:'PUBLISHED'";

    Logger.getLogger(MappingServiceJpa.class).info(fullQuery);

    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
    Query luceneQuery;

    // construct luceneQuery based on URL format

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(MapRecordJpa.class));

    try {
      luceneQuery = queryParser.parse(fullQuery);

      org.hibernate.search.jpa.FullTextQuery ftquery =
          fullTextEntityManager.createFullTextQuery(luceneQuery,
              MapRecordJpa.class);

      // Sort Options -- in order of priority
      // (1) if a sort field is specified by pfs parameter, use it
      // (2) if a query has been specified, use nothing (lucene relevance
      // default)
      // (3) if a query has not been specified, sort by conceptId

      String sortField = "conceptId";
      if (pfsParameter != null && pfsParameter.getSortField() != null
          && !pfsParameter.getSortField().isEmpty()) {
        ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
            SortField.STRING)));
      } else if (pfsParameter != null
          && pfsParameter.getQueryRestriction() != null
          && !pfsParameter.getQueryRestriction().isEmpty()) {
        // do nothing
      } else {
        ftquery.setSort(new Sort(new SortField(sortField, SortField.STRING)));
      }

      // get the results
      int totalCount = ftquery.getResultSize();

      if (pfsParameter != null) {
        ftquery.setFirstResult(pfsParameter.getStartIndex());
        ftquery.setMaxResults(pfsParameter.getMaxResults());
      }
      List<MapRecord> mapRecords = ftquery.getResultList();

      Logger.getLogger(this.getClass()).debug(
          Integer.toString(mapRecords.size()) + " records retrieved");

      for (MapRecord mapRecord : mapRecords) {
        handleMapRecordLazyInitialization(mapRecord);
      }

      // set the total count
      MapRecordListJpa mapRecordList = new MapRecordListJpa();
      mapRecordList.setTotalCount(totalCount);

      // extract the required sublist of map records
      mapRecordList.setMapRecords(mapRecords);

      return mapRecordList;
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }

  }

  /**
   * Helper function for map record query construction using both fielded terms
   * and unfielded terms.
   * 
   * @param mapProjectId the map project id for which queries are retrieved
   * @param pfsParameter the pfs parameter
   * @return the full lucene query text
   */
  private static String constructMapRecordForMapProjectIdQuery(
    Long mapProjectId, PfsParameter pfsParameter) {

    String fullQuery;

    // if no filter supplied, return query based on map project id only
    if (pfsParameter != null
        && (pfsParameter.getQueryRestriction() == null
            || pfsParameter.getQueryRestriction().equals("") || pfsParameter
            .getQueryRestriction().equals("undefined"))) {
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
    final String queryStr =
        (pfsParameter == null ? "" : pfsParameter.getQueryRestriction());

    String queryStrMod = queryStr;
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
        Iterator<String> namesIter = mapRecordFieldNames.iterator();
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
   * org.ihtsdo.otf.mapping.services.MappingService#findConceptsInScope(org.
   * ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public SearchResultList findConceptsInScope(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception {
    Logger.getLogger(this.getClass()).info(
        "Find concepts in scope for " + mapProjectId);

    MapProject project = getMapProject(mapProjectId);
    SearchResultList conceptsInScope = new SearchResultListJpa();

    ContentService contentService = new ContentServiceJpa();

    String terminology = project.getSourceTerminology();
    String terminologyVersion = project.getSourceTerminologyVersion();

    // Avoid including the scope concepts themselves in the definition
    // if we are looking for descendants
    // e.g. "Clinical Finding" does not need to be mapped for SNOMED->ICD10
    if (!project.isScopeDescendantsFlag()) {
      Logger.getLogger(this.getClass()).info(
          "  Project not using scope descendants flag - "
              + project.getScopeConcepts());
      for (String conceptId : project.getScopeConcepts()) {
        Concept c =
            contentService.getConcept(conceptId, terminology,
                terminologyVersion);
        if (c == null) {
          contentService.close();
          throw new Exception("Scope concept " + conceptId + " does not exist.");
        }
        // Only keep active concepts
        if (c.isActive()) {
          SearchResult sr = new SearchResultJpa();
          sr.setId(c.getId());
          sr.setTerminologyId(c.getTerminologyId());
          sr.setTerminology(c.getTerminology());
          sr.setTerminologyVersion(c.getTerminologyVersion());
          sr.setValue(c.getDefaultPreferredName());
          conceptsInScope.addSearchResult(sr);
        }
      }
    }

    // Include descendants in scope.
    if (project.isScopeDescendantsFlag()) {
      Logger.getLogger(this.getClass()).info(
          "  Project using scope descendants flag");
      // for each scope concept, get descendants
      for (String terminologyId : project.getScopeConcepts()) {
        SearchResultList descendants =
            contentService.findDescendantConcepts(terminologyId, terminology,
                terminologyVersion, pfsParameter);

        Logger.getLogger(this.getClass()).info(
            "    Concept " + terminologyId + " has "
                + descendants.getTotalCount() + " descendants ("
                + descendants.getCount() + " from getCount)");
        // cycle over descendants
        for (SearchResult sr : descendants.getSearchResults()) {
          conceptsInScope.addSearchResult(sr);
        }
      }
    }

    contentService.close();

    // get those excluded from scope
    // Get as set so next step is easily run.
    Set<SearchResult> excludedResultList =
        new HashSet<>(findConceptsExcludedFromScope(mapProjectId, pfsParameter)
            .getSearchResults());

    // remove those excluded from scope
    SearchResultList finalConceptsInScope = new SearchResultListJpa();
    for (SearchResult sr : conceptsInScope.getSearchResults()) {
      if (!excludedResultList.contains(sr)) {
        finalConceptsInScope.addSearchResult(sr);
      }
    }

    finalConceptsInScope.setTotalCount(finalConceptsInScope.getCount());
    Logger.getLogger(this.getClass()).info(
        "Finished getting scope concepts - "
            + finalConceptsInScope.getTotalCount());

    /**
     * PrintWriter writer = new PrintWriter("C:/data/inScopeConcepts.txt",
     * "UTF-8"); for(SearchResult sr : finalConceptsInScope.getSearchResults())
     * { writer.println(sr.getTerminologyId()); } writer.close();
     */

    return finalConceptsInScope;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#findUnmappedConceptsInScope
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public SearchResultList findUnmappedConceptsInScope(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception {
    Logger.getLogger(this.getClass()).info(
        "Find unmapped concepts in scope for " + mapProjectId);
    // Get in scope concepts
    SearchResultList conceptsInScope =
        findConceptsInScope(mapProjectId, pfsParameter);
    Logger.getLogger(this.getClass()).info(
        "  Project has " + conceptsInScope.getTotalCount()
            + " concepts in scope");

    //
    // Look for concept ids that have publication records in the current
    // project
    //
    Set<String> mappedConcepts = new HashSet<>();
    for (MapRecord mapRecord : getMapRecordsForMapProject(mapProjectId)
        .getIterable()) {
      if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
          || mapRecord.getWorkflowStatus().equals(
              WorkflowStatus.READY_FOR_PUBLICATION)) {
        mappedConcepts.add(mapRecord.getConceptId());
      }
    }

    //
    // Keep any search results that do not have mapped concepts
    //
    ContentService contentService = new ContentServiceJpa();
    SearchResultList unmappedConceptsInScope = new SearchResultListJpa();
    for (SearchResult sr : conceptsInScope.getSearchResults()) {

      if (!mappedConcepts.contains(sr.getTerminologyId())) {
        unmappedConceptsInScope.addSearchResult(sr);
      }

    }
    unmappedConceptsInScope.setTotalCount(unmappedConceptsInScope.getCount());
    contentService.close();

    Logger.getLogger(this.getClass()).info(
        "  Project has " + unmappedConceptsInScope.getTotalCount()
            + " unmapped concepts in scope");

    return unmappedConceptsInScope;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * findMappedConceptsOutOfScopeBounds(java.lang.Long)
   */
  @Override
  public SearchResultList findMappedConceptsOutOfScopeBounds(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception {
    SearchResultList mappedConceptsOutOfBounds = new SearchResultListJpa();
    MapProject project = getMapProject(mapProjectId);
    List<MapRecord> mapRecordList =
        getMapRecordsForMapProject(mapProjectId).getMapRecords();
    ContentService contentService = new ContentServiceJpa();

    for (MapRecord record : mapRecordList) {
      Concept c =
          contentService.getConcept(record.getConceptId(),
              project.getSourceTerminology(),
              project.getSourceTerminologyVersion());
      if (isConceptOutOfScopeBounds(c.getTerminologyId(), mapProjectId)) {
        SearchResult sr = new SearchResultJpa();
        sr.setId(c.getId());
        sr.setTerminologyId(c.getTerminologyId());
        sr.setTerminology(c.getTerminology());
        sr.setTerminologyVersion(c.getTerminologyVersion());
        sr.setValue(c.getDefaultPreferredName());
        mappedConceptsOutOfBounds.addSearchResult(sr);
      }

    }
    contentService.close();
    return mappedConceptsOutOfBounds;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#findConceptsExcludedFromScope
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public SearchResultList findConceptsExcludedFromScope(Long mapProjectId,
    PfsParameter pfsParameter) throws Exception {
    SearchResultList conceptsExcludedFromScope = new SearchResultListJpa();

    ContentService contentService = new ContentServiceJpa();
    MapProject project = getMapProject(mapProjectId);
    String terminology = project.getSourceTerminology();
    String terminologyVersion = project.getSourceTerminologyVersion();

    // add specified excluded concepts
    for (String conceptId : project.getScopeExcludedConcepts()) {
      Concept c =
          contentService.getConcept(conceptId, terminology, terminologyVersion);
      if (c != null) {
        SearchResult sr = new SearchResultJpa();
        sr.setId(c.getId());
        sr.setTerminologyId(c.getTerminologyId());
        sr.setTerminology(c.getTerminology());
        sr.setTerminologyVersion(c.getTerminologyVersion());
        sr.setValue(c.getDefaultPreferredName());
        conceptsExcludedFromScope.addSearchResult(sr);
      }
    }

    // add descendant excluded concepts if indicated
    if (project.isScopeExcludedDescendantsFlag()) {

      // for each excluded scope concept, get descendants
      for (String terminologyId : project.getScopeExcludedConcepts()) {
        SearchResultList descendants =
            contentService.findDescendantConcepts(terminologyId, terminology,
                terminologyVersion, null);

        // cycle over descendants
        for (SearchResult sr : descendants.getSearchResults()) {
          conceptsExcludedFromScope.addSearchResult(sr);
        }
      }

    }

    contentService.close();
    conceptsExcludedFromScope.setTotalCount(conceptsExcludedFromScope
        .getCount());
    Logger.getLogger(this.getClass()).info(
        "Concepts excluded from scope "
            + +conceptsExcludedFromScope.getTotalCount());
    return conceptsExcludedFromScope;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#isConceptInScope(org.ihtsdo
   * .otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public boolean isConceptInScope(String conceptId, Long mapProjectId)
    throws Exception {
    MapProject project = getMapProject(mapProjectId);
    // if directly matches preset scope concept return true
    for (String c : project.getScopeConcepts()) {
      if (c.equals(conceptId))
        return true;
    }
    // don't make contentService if no chance descendants meet conditions
    if (!project.isScopeDescendantsFlag() && !project.isScopeDescendantsFlag())
      return false;

    ContentService contentService = new ContentServiceJpa();
    for (TreePosition tp : contentService.getTreePositionsWithDescendants(
        conceptId, project.getSourceTerminology(),
        project.getSourceTerminologyVersion()).getIterable()) {
      String ancestorPath = tp.getAncestorPath();
      if (project.isScopeExcludedDescendantsFlag()
          && ancestorPath.contains(conceptId)) {
        continue;
      }
      if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
        contentService.close();
        return true;
      }
    }
    contentService.close();
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#isConceptExcludedFromScope
   * (org.ihtsdo.otf.mapping.rf2.Concept,
   * org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public boolean isConceptExcludedFromScope(String conceptId, Long mapProjectId)
    throws Exception {
    MapProject project = getMapProject(mapProjectId);
    // if directly matches preset scope concept return true
    for (String c : project.getScopeExcludedConcepts()) {
      if (c.equals(conceptId))
        return true;
    }
    // don't make contentService if no chance descendants meet conditions
    if (!project.isScopeDescendantsFlag() && !project.isScopeDescendantsFlag())
      return false;

    ContentService contentService = new ContentServiceJpa();
    for (TreePosition tp : contentService.getTreePositionsWithDescendants(
        conceptId, project.getSourceTerminology(),
        project.getSourceTerminologyVersion()).getIterable()) {
      String ancestorPath = tp.getAncestorPath();
      if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
        continue;
      }
      if (project.isScopeExcludedDescendantsFlag()
          && ancestorPath.contains(conceptId)) {
        contentService.close();
        return true;
      }
    }
    contentService.close();
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#isConceptOutOfScopeBounds
   * (java.lang.String, java.lang.Long)
   */
  @Override
  public boolean isConceptOutOfScopeBounds(String conceptId, Long mapProjectId)
    throws Exception {
    MapProject project = getMapProject(mapProjectId);
    // if directly matches preset scope concept return false
    for (String c : project.getScopeConcepts()) {
      if (c.equals(conceptId))
        return false;
    }

    ContentService contentService = new ContentServiceJpa();
    for (TreePosition tp : contentService.getTreePositionsWithDescendants(
        conceptId, project.getSourceTerminology(),
        project.getSourceTerminologyVersion()).getIterable()) {
      String ancestorPath = tp.getAncestorPath();
      if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
        return false;
      }
    }
    contentService.close();
    return true;
  }

  /**
   * Given a concept, returns a list of descendant concepts that have no
   * associated map record.
   * 
   * @param terminologyId the terminology id
   * @param mapProjectId the map project id
   * @param pfsParameter the pfs parameter
   * @return the list of unmapped descendant concepts
   * @throws Exception the exception
   */
  @Override
  public SearchResultList findUnmappedDescendantsForConcept(
    String terminologyId, Long mapProjectId, PfsParameter pfsParameter)
    throws Exception {

    MapProject project = getMapProject(mapProjectId);
    SearchResultList unmappedDescendants = new SearchResultListJpa();

    // get hierarchical rel
    MetadataService metadataService = new MetadataServiceJpa();
    Map<String, String> hierarchicalRelationshipTypeMap =
        metadataService.getHierarchicalRelationshipTypes(
            project.getSourceTerminology(),
            project.getSourceTerminologyVersion());
    if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
      throw new IllegalStateException(
          "Map project source terminology has too many hierarchical relationship types - "
              + project.getSourceTerminology());
    }
    if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
      throw new IllegalStateException(
          "Map project source terminology has too few hierarchical relationship types - "
              + project.getSourceTerminology());
    }

    // get descendants -- no pfsParameter, want all results
    ContentService contentService = new ContentServiceJpa();
    SearchResultList descendants =
        contentService.findDescendantConcepts(terminologyId,
            project.getSourceTerminology(),
            project.getSourceTerminologyVersion(), null);

    // if number of descendants <= low-level concept threshold, treat as
    // high-level concept and report no unmapped
    if (descendants.getCount() < project.getPropagationDescendantThreshold()) {

      // cycle over descendants
      for (SearchResult sr : descendants.getSearchResults()) {

        // if descendant has no associated map records, add to list
        if (getMapRecordsForConcept(sr.getTerminologyId()).getTotalCount() == 0) {
          unmappedDescendants.addSearchResult(sr);
        }
      }
    }

    // close managers
    contentService.close();
    metadataService.close();

    return unmappedDescendants;

  }

  // //////////////////////////////
  // Services to be implemented //
  // //////////////////////////////

  // //////////////////////////
  // Addition services ///
  // //////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#addMapPrinciple(org.ihtsdo
   * .otf.mapping.model.MapPrinciple)
   */
  @Override
  public MapPrinciple addMapPrinciple(MapPrinciple mapPrinciple) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapPrinciple);
      tx.commit();
    } else {
      manager.persist(mapPrinciple);
    }
    return mapPrinciple;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#addMapAdvice(org.ihtsdo.
   * otf.mapping.model.MapAdvice)
   */
  @Override
  public MapAdvice addMapAdvice(MapAdvice mapAdvice) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapAdvice);
      tx.commit();
    } else {
      manager.persist(mapAdvice);
    }

    return mapAdvice;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#addMapRelation(org.ihtsdo.
   * otf.mapping.model.MapRelation)
   */
  @Override
  public MapRelation addMapRelation(MapRelation mapRelation) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapRelation);
      tx.commit();
    } else {
      manager.persist(mapRelation);
    }
    return mapRelation;
  }

  // //////////////////////////
  // Update services ///
  // //////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#updateMapPrinciple(org
   * .ihtsdo .otf.mapping.model.MapPrinciple)
   */
  @Override
  public void updateMapPrinciple(MapPrinciple mapPrinciple) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.merge(mapPrinciple);
      tx.commit();
      // manager.close();
    } else {
      manager.merge(mapPrinciple);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#updateMapAdvice(org.ihtsdo
   * .otf.mapping.model.MapAdvice)
   */
  @Override
  public void updateMapAdvice(MapAdvice mapAdvice) {
    if (getTransactionPerOperation()) {

      tx = manager.getTransaction();

      tx.begin();
      manager.merge(mapAdvice);
      tx.commit();
      // manager.close();
    } else {
      manager.merge(mapAdvice);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#updateMapRelation(org.
   * ihtsdo .otf.mapping.model.MapRelation)
   */
  @Override
  public void updateMapRelation(MapRelation mapRelation) {
    if (getTransactionPerOperation()) {

      tx = manager.getTransaction();

      tx.begin();
      manager.merge(mapRelation);
      tx.commit();
      // manager.close();
    } else {
      manager.merge(mapRelation);
    }
  }

  // //////////////////////////
  // Removal services ///
  // //////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapPrinciple(java
   * .lang.Long)
   */
  @Override
  public void removeMapPrinciple(Long mapPrincipleId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      MapPrinciple mp = manager.find(MapPrincipleJpa.class, mapPrincipleId);
      if (manager.contains(mp)) {
        manager.remove(mp);
      } else {
        manager.remove(manager.merge(mp));
      }
      tx.commit();
    } else {
      MapPrinciple mp = manager.find(MapPrincipleJpa.class, mapPrincipleId);
      if (manager.contains(mp)) {
        manager.remove(mp);
      } else {
        manager.remove(manager.merge(mp));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#removeMapAdvice(java.lang
   * .Long)
   */
  @Override
  public void removeMapAdvice(Long mapAdviceId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      MapAdvice ma = manager.find(MapAdviceJpa.class, mapAdviceId);
      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
      tx.commit();
    } else {
      MapAdvice ma = manager.find(MapAdviceJpa.class, mapAdviceId);
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
   * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapRelation(java
   * .lang.Long)
   */
  @Override
  public void removeMapRelation(Long mapRelationId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      MapRelation ma = manager.find(MapRelationJpa.class, mapRelationId);
      if (manager.contains(ma)) {
        manager.remove(ma);
      } else {
        manager.remove(manager.merge(ma));
      }
      tx.commit();
    } else {
      MapRelation ma = manager.find(MapRelationJpa.class, mapRelationId);
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
   * org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciple(java.lang
   * .Long)
   */
  @Override
  public MapPrinciple getMapPrinciple(Long id) throws Exception {

    javax.persistence.Query query =
        manager.createQuery("select m from MapPrincipleJpa m where id = :id");
    query.setParameter("id", id);
    return (MapPrinciple) query.getSingleResult();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciples()
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapPrincipleList getMapPrinciples() {
    List<MapPrinciple> mapPrinciples = new ArrayList<>();

    javax.persistence.Query query =
        manager.createQuery("select m from MapPrincipleJpa m");

    // Try query
    mapPrinciples = query.getResultList();

    MapPrincipleListJpa mapPrincipleList = new MapPrincipleListJpa();
    mapPrincipleList.setMapPrinciples(mapPrinciples);
    mapPrincipleList.setTotalCount(mapPrinciples.size());
    return mapPrincipleList;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapAdvices()
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapAdviceList getMapAdvices() {
    List<MapAdvice> mapAdvices = new ArrayList<>();

    javax.persistence.Query query =
        manager.createQuery("select m from MapAdviceJpa m");

    // Try query
    mapAdvices = query.getResultList();
    MapAdviceListJpa mapAdviceList = new MapAdviceListJpa();
    mapAdviceList.setMapAdvices(mapAdvices);
    mapAdviceList.setTotalCount(mapAdvices.size());
    return mapAdviceList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRelations()
   */
  @SuppressWarnings("unchecked")
  @Override
  public MapRelationList getMapRelations() {
    List<MapRelation> mapRelations = new ArrayList<>();

    javax.persistence.Query query =
        manager.createQuery("select m from MapRelationJpa m");

    // Try query
    mapRelations = query.getResultList();
    mapRelations = query.getResultList();
    MapRelationListJpa mapRelationList = new MapRelationListJpa();
    mapRelationList.setMapRelations(mapRelations);
    mapRelationList.setTotalCount(mapRelations.size());

    return mapRelationList;
  }

  // ///////////////////////////////////////
  // / Services for Map Project Creation
  // ///////////////////////////////////////

  /* see superclass */
  @Override
  public void createMapRecordsForMapProject(Long mapProjectId,
    WorkflowStatus workflowStatus) throws Exception {
    MapProject mapProject = getMapProject(mapProjectId);
    Logger.getLogger(MappingServiceJpa.class).info(
        "Called createMapRecordsForMapProject for project - " + mapProjectId
            + " workflowStatus - " + workflowStatus);
    if (!getTransactionPerOperation()) {
      throw new IllegalStateException(
          "The application must let the service manage transactions for this method");
    }

    // get the loader user
    MapUser loaderUser;
    try {
      loaderUser = getMapUser("loader");
    } catch (Exception e) {
      // if loader user does not exist, add it
      loaderUser = new MapUserJpa();
      loaderUser.setApplicationRole(MapUserRole.VIEWER);
      loaderUser.setUserName("loader");
      loaderUser.setName("Loader Record");
      loaderUser.setEmail("none");
      loaderUser = addMapUser(loaderUser);
    }
    List<ComplexMapRefSetMember> members = new ArrayList<>();

    // IF map project uses "simple", then construct a query from a simple map
    if (mapProject.getMapRefsetPattern() == MapRefsetPattern.SimpleMap) {
      // else retrieve all complex map ref set members for mapProject
      javax.persistence.Query query =
          manager.createQuery("select r from SimpleMapRefSetMemberJpa r "
              + "where r.refSetId = :refSetId");
      query.setParameter("refSetId", mapProject.getRefSetId());
      for (Object member : query.getResultList()) {
        final SimpleMapRefSetMember simpleMember =
            (SimpleMapRefSetMember) member;
        final ComplexMapRefSetMember complexMember =
            new ComplexMapRefSetMemberJpa(simpleMember);
        members.add(complexMember);
      }
    }

    // else retrieve all complex map ref set members for mapProject
    else {
      javax.persistence.Query query =
          manager.createQuery("select r from ComplexMapRefSetMemberJpa r "
              + "where r.refSetId = :refSetId order by r.concept.id, "
              + "r.mapBlock, r.mapGroup, r.mapPriority");
      query.setParameter("refSetId", mapProject.getRefSetId());
      for (Object member : query.getResultList()) {
        ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
        members.add(refSetMember);
      }
    }

    // Create the map records
    createMapRecordsForMapProject(mapProjectId, loaderUser, members,
        workflowStatus);
  }

  // ONLY FOR TESTING PURPOSES
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * removeMapRecordsForMapProjectId (java.lang.Long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Long removeMapRecordsForMapProject(Long mapProjectId) {

    tx = manager.getTransaction();

    int nRecords = 0;
    tx.begin();
    List<MapRecord> records =
        manager
            .createQuery(
                "select m from MapRecordJpa m where m.mapProjectId = :mapProjectId")
            .setParameter("mapProjectId", mapProjectId).getResultList();

    for (MapRecord record : records) {

      // delete notes
      for (MapNote note : record.getMapNotes()) {
        if (manager.contains(note)) {
          manager.remove(note);
        } else {
          manager.remove(manager.merge(note));
        }
      }
      record.setMapNotes(null);

      // delete entries
      for (MapEntry entry : record.getMapEntries()) {

        // remove advices
        entry.setMapAdvices(null);

        // merge entry to remove principle/advice associations
        manager.merge(entry);

        // delete entry
        manager.remove(entry);

        nRecords++;
      }

      // remove principles
      record.setMapPrinciples(null);

      // merge record to remove principle associations
      manager.merge(record);

      // delete record
      manager.remove(record);
    }

    tx.commit();

    Logger.getLogger(this.getClass()).debug(
        Integer.toString(nRecords) + " records deleted for map project id = "
            + mapProjectId);

    return new Long(nRecords);

  }

  /**
   * Helper function to call main routine with null value for sampling rate.
   * 
   * @param mapProjectId the map project id
   * @param mapUser the map user
   * @param complexMapRefSetMembers the complex map ref set members
   * @param workflowStatus the workflow status
   * @throws Exception the exception
   */
  @Override
  public void createMapRecordsForMapProject(Long mapProjectId, MapUser mapUser,
    List<ComplexMapRefSetMember> complexMapRefSetMembers,
    WorkflowStatus workflowStatus) throws Exception {
    createMapRecordsForMapProject(mapProjectId, mapUser,
        complexMapRefSetMembers, workflowStatus, -1.0f);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject
   * (java.lang.Long, java.util.List)
   */
  @Override
  public void createMapRecordsForMapProject(Long mapProjectId, MapUser mapUser,
    List<ComplexMapRefSetMember> complexMapRefSetMembers,
    WorkflowStatus workflowStatus, float samplingRate) throws Exception {
    MapProject mapProject = getMapProject(mapProjectId);
    Logger.getLogger(MappingServiceJpa.class).debug(
        "  Creating map records for map project - " + mapProject.getName()
            + ", assigning workflow status " + WorkflowStatus.PUBLISHED);

    // Verify application is letting the service manage transactions
    if (!getTransactionPerOperation()) {
      throw new IllegalStateException(
          "The application must let the service manage transactions for this method");
    }

    // Setup content service
    ContentService contentService = new ContentServiceJpa();

    // Get map relation id->name mapping
    MetadataService metadataService = new MetadataServiceJpa();

    Map<String, String> relationIdNameMap =
        metadataService.getMapRelations(mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());
    Logger.getLogger(MappingServiceJpa.class).debug(
        "    relationIdNameMap = " + relationIdNameMap);

    // use the map relation id->name mapping to construct a hash set of
    // MapRelations
    Map<String, MapRelation> mapRelationIdMap = new HashMap<>();
    for (MapRelation mapRelation : getMapRelations().getIterable()) {
      mapRelationIdMap.put(mapRelation.getTerminologyId(), mapRelation);
    }

    Map<String, String> hierarchicalRelationshipTypeMap =
        metadataService.getHierarchicalRelationshipTypes(
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());
    if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
      throw new IllegalStateException(
          "Map project source terminology has too many hierarchical relationship types - "
              + mapProject.getSourceTerminology());
    }
    if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
      throw new IllegalStateException(
          "Map project source terminology has too few hierarchical relationship types - "
              + mapProject.getSourceTerminology());
    }

    boolean prevTransactionPerOperationSetting = getTransactionPerOperation();
    setTransactionPerOperation(false);
    beginTransaction();
    MapAdviceList mapAdvices = getMapAdvices();
    int mapPriorityCt = 0;
    int prevMapGroup = 0;

    // sampling tracker variables
    int samplingRecordsCreated = 0;
    int samplingRecordsPublished = 0;
    try {
      // instantiate other local variables
      String prevConceptId = null;
      MapRecord mapRecord = null;
      int ct = 0;
      Random random = new Random();

      if (mapUser == null) {
        throw new Exception("Loader user could not be found");
      }

      for (ComplexMapRefSetMember refSetMember : complexMapRefSetMembers) {

        // Skip inactive cases
        if (!refSetMember.isActive()) {
          Logger.getLogger(MappingServiceJpa.class).debug(
              "Skipping refset member " + refSetMember.getTerminologyId());
          continue;
        }

        // Skip concept exclusion rules
        if (refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|.*\\s\\|")) {
          if (refSetMember.getMapAdvice().contains(
              "MAP IS CONTEXT DEPENDENT FOR GENDER")
              && !refSetMember.getMapRule().matches("AND IFA")) {
            // unless simple gender rule, then keep
          } else if (refSetMember
              .getMapRule()
              .matches(
                  "IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>].*AND IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]")) {
            // unless 2-part age rule, then keep
          } else if (refSetMember.getMapRule().matches(
              "IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]")
              && !refSetMember.getMapRule().matches("AND IFA")) {
            // unless simple age rule without compund clause, then keep
          } else {
            // else skip
            Logger.getLogger(MappingServiceJpa.class).debug(
                "    Skipping refset member exclusion rule "
                    + refSetMember.getTerminologyId());
            continue;
          }
        }

        // retrieve the concept
        Logger.getLogger(MappingServiceJpa.class).debug(
            "    Get refset member concept");
        Concept concept = refSetMember.getConcept();

        // if no concept for this ref set member, skip
        if (concept == null) {
          continue;
          /*
           * throw new NoResultException(
           * "    Concept is unexpectedly missing for " +
           * refSetMember.getTerminologyId());
           */
        }

        // if different concept than previous ref set member, create
        // new
        // mapRecord
        if (!concept.getTerminologyId().equals(prevConceptId)) {
          Logger.getLogger(MappingServiceJpa.class).debug(
              "    Creating map record for " + concept.getTerminologyId());

          mapPriorityCt = 0;
          prevMapGroup = 0;
          mapRecord = new MapRecordJpa();
          mapRecord.setConceptId(concept.getTerminologyId());
          mapRecord.setConceptName(concept.getDefaultPreferredName());
          mapRecord.setMapProjectId(mapProject.getId());

          // set the previous concept to this concept
          prevConceptId = refSetMember.getConcept().getTerminologyId();

          // set the owner and lastModifiedBy user fields to
          // loaderUser
          mapRecord.setOwner(mapUser);
          mapRecord.setLastModifiedBy(mapUser);

          // random determine workflow state
          // based on sampling percentage
          // NOTE: Explicit equality check for -1.0f put in to
          // avoid
          // any possible errors
          // in multiplication/division/comparison
          if (samplingRate != -1.0f
              && random.nextInt(100 + 1) / 100.0 < samplingRate) {
            samplingRecordsCreated++;
            mapRecord.setWorkflowStatus(workflowStatus);
          } else {
            samplingRecordsPublished++;
            mapRecord.setWorkflowStatus(WorkflowStatus.PUBLISHED);
          }

          addMapRecord(mapRecord);

          if (++ct % commitCt == 0) {
            Logger.getLogger(MappingServiceJpa.class).info(
                "    " + ct + " records created");
            commit();
            manager.clear();
            beginTransaction();
            // For memory management, avoid keeping cache of
            // tree
            // positions
            contentService.close();
            contentService = new ContentServiceJpa();
          }
        }

        // check if target is in desired terminology; if so, create
        // entry
        String targetName = null;

        if (!refSetMember.getMapTarget().equals("")) {
          Concept c =
              contentService.getConcept(refSetMember.getMapTarget(),
                  mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
          if (c == null) {
            targetName = "Target name could not be determined";
          } else {
            targetName = c.getDefaultPreferredName();
          }
          Logger.getLogger(this.getClass()).debug(
              "      Setting target name " + targetName);
        } else {
          targetName = "No target";
        }

        // Set map relation id as well from the cache
        String relationName = null;
        if (refSetMember.getMapRelationId() != null) {
          relationName =
              relationIdNameMap.get(refSetMember.getMapRelationId().toString());
          Logger.getLogger(this.getClass()).debug(
              "      Look up relation name = " + relationName);
        }

        Logger.getLogger(this.getClass()).debug("      Create map entry");
        MapEntry mapEntry = new MapEntryJpa();
        mapEntry.setTargetId(refSetMember.getMapTarget() == null ? ""
            : refSetMember.getMapTarget());
        mapEntry.setTargetName(targetName);
        mapEntry.setMapRecord(mapRecord);
        mapEntry.setMapRelation(mapRelationIdMap.get(refSetMember
            .getMapRelationId().toString()));
        String rule = refSetMember.getMapRule();
        if (rule.equals("OTHERWISE TRUE"))
          rule = "TRUE";
        mapEntry.setRule(rule);
        mapEntry.setMapBlock(refSetMember.getMapBlock());
        mapEntry.setMapGroup(refSetMember.getMapGroup());
        if (prevMapGroup != refSetMember.getMapGroup()) {
          mapPriorityCt = 0;
          prevMapGroup = refSetMember.getMapGroup();
        }
        // Increment map priority as we go through records
        mapEntry.setMapPriority(++mapPriorityCt);

        mapRecord.addMapEntry(mapEntry);

        // Add support for advices - and there can be multiple map
        // advice values
        // Only add advice if it is an allowable value and doesn't
        // match
        // relation name
        // This should automatically exclude IFA/ALWAYS advice
        Logger.getLogger(this.getClass()).debug("      Setting map advice");
        if (refSetMember.getMapAdvice() != null
            && !refSetMember.getMapAdvice().equals("")) {
          for (MapAdvice ma : mapAdvices.getIterable()) {
            if (refSetMember.getMapAdvice().indexOf(ma.getName()) != -1
                && !ma.getName().equals(relationName)) {
              mapEntry.addMapAdvice(ma);
              Logger.getLogger(this.getClass()).debug("    " + ma.getName());
            }
          }
        }
      }

      Logger.getLogger(MappingServiceJpa.class).info(
          "    " + ct + " records created");
      if (samplingRate != -1.0f) {
        Logger.getLogger(MappingServiceJpa.class).info(
            "    " + samplingRecordsCreated + " records set to "
                + workflowStatus);
        Logger.getLogger(MappingServiceJpa.class).info(
            "    " + samplingRecordsPublished + " records set to PUBLISHED");
      }

      commit();
      contentService.close();
      metadataService.close();
    } catch (Exception e) {
      setTransactionPerOperation(prevTransactionPerOperationSetting);
      throw e;
    }
    setTransactionPerOperation(prevTransactionPerOperationSetting);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getTransactionPerOperation
   * ()
   */
  @Override
  public boolean getTransactionPerOperation() {
    return transactionPerOperation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#setTransactionPerOperation
   * (boolean)
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation) {
    this.transactionPerOperation = transactionPerOperation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#beginTransaction()
   */
  @Override
  public void beginTransaction() {

    if (getTransactionPerOperation())
      throw new IllegalStateException(
          "Error attempting to begin a transaction when using transactions per operation mode.");
    else if (tx != null && tx.isActive())
      throw new IllegalStateException(
          "Error attempting to begin a transaction when there "
              + "is already an active transaction");
    tx = manager.getTransaction();
    tx.begin();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#commit()
   */
  @Override
  public void commit() {

    if (getTransactionPerOperation()) {
      throw new IllegalStateException(
          "Error attempting to commit a transaction when using transactions per operation mode.");
    } else if (tx == null || (tx != null && !tx.isActive())) {
      throw new IllegalStateException(
          "Error attempting to commit a transaction when there "
              + "is no active transaction");
    }
    tx.commit();
  }

  // ////////////////////////
  // AGE RANGE FUNCTIONS
  // ////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapAgeRanges()
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapAgeRangeList getMapAgeRanges() {

    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from MapAgeRangeJpa m");

    MapAgeRangeList m = new MapAgeRangeListJpa();
    m.setMapAgeRanges(query.getResultList());

    return m;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#addMapAgeRange(org.ihtsdo
   * .otf.mapping.model.MapAgeRange)
   */
  @Override
  public MapAgeRange addMapAgeRange(MapAgeRange mapAgeRange) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapAgeRange);
      tx.commit();

    } else {
      manager.persist(mapAgeRange);
    }

    return mapAgeRange;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapAgeRange(java
   * .lang.Long)
   */
  @Override
  public void removeMapAgeRange(Long mapAgeRangeId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      MapAgeRange mar = manager.find(MapAgeRangeJpa.class, mapAgeRangeId);
      if (manager.contains(mar)) {
        manager.remove(mar);
      } else {
        manager.remove(manager.merge(mar));
      }
      tx.commit();
    } else {
      MapAgeRange mar = manager.find(MapAgeRangeJpa.class, mapAgeRangeId);
      if (manager.contains(mar)) {
        manager.remove(mar);
      } else {
        manager.remove(manager.merge(mar));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#updateMapAgeRange(org.
   * ihtsdo .otf.mapping.model.MapAgeRange)
   */
  @Override
  public void updateMapAgeRange(MapAgeRange mapAgeRange) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.merge(mapAgeRange);
      tx.commit();
    } else {
      manager.merge(mapAgeRange);
    }
  }

  // /////////////////////////////////////
  // MAP USER PREFERENCES FUNCTIONS
  // /////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapUserPreferences(
   * java.lang.String)
   */
  @Override
  public MapUserPreferences getMapUserPreferences(String userName)
    throws Exception {

    Logger.getLogger(MappingServiceJpa.class).info("Finding user " + userName);
    MapUser mapUser = getMapUser(userName);
    javax.persistence.Query query =
        manager
            .createQuery(
                "select m from MapUserPreferencesJpa m where mapUser_id = :mapUser_id")
            .setParameter("mapUser_id", mapUser.getId());

    MapUserPreferences m;
    try {
      m = (MapUserPreferences) query.getSingleResult();
    }

    // catch no result exception and create default user preferences
    catch (NoResultException e) {

      // create object
      m = new MapUserPreferencesJpa();
      m.setMapUser(mapUser); // set the map user
      MapProjectList mapProjects = getMapProjects();
      m.setLastMapProjectId(mapProjects.getIterable().iterator().next().getId()); // set
                                                                                  // a
                                                                                  // default
                                                                                  // project
                                                                                  // to
                                                                                  // 1st
                                                                                  // project
                                                                                  // found

      // add object
      addMapUserPreferences(m);
    }

    // return preferences
    return m;
  }

  /**
   * Retrieve all map user preferences.
   * 
   * @return a List of MapUserPreferencess
   */
  @Override
  @SuppressWarnings("unchecked")
  public MapUserPreferencesList getMapUserPreferences() {

    List<MapUserPreferences> m = null;

    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from MapUserPreferencesJpa m");

    // Try query

    m = query.getResultList();
    MapUserPreferencesListJpa mapUserPreferencesList =
        new MapUserPreferencesListJpa();
    mapUserPreferencesList.setMapUserPreferences(m);
    mapUserPreferencesList.setTotalCount(m.size());
    return mapUserPreferencesList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#addMapUserPreferences(
   * org.ihtsdo.otf.mapping.model.MapUserPreferences)
   */
  @Override
  public MapUserPreferences addMapUserPreferences(
    MapUserPreferences mapUserPreferences) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.persist(mapUserPreferences);
      tx.commit();

    } else {
      manager.persist(mapUserPreferences);
    }

    return mapUserPreferences;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#removeMapUserPreferences
   * (java.lang.Long)
   */
  @Override
  public void removeMapUserPreferences(Long mapUserPreferencesId) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      MapUserPreferences mar =
          manager.find(MapUserPreferencesJpa.class, mapUserPreferencesId);
      if (manager.contains(mar)) {
        manager.remove(mar);
      } else {
        manager.remove(manager.merge(mar));
      }
      tx.commit();
    } else {
      MapUserPreferences mar =
          manager.find(MapUserPreferencesJpa.class, mapUserPreferencesId);
      if (manager.contains(mar)) {
        manager.remove(mar);
      } else {
        manager.remove(manager.merge(mar));
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#updateMapUserPreferences
   * (org.ihtsdo .otf.mapping.model.MapUserPreferences)
   */
  @Override
  public void updateMapUserPreferences(MapUserPreferences mapUserPreferences) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();

      tx.begin();
      manager.merge(mapUserPreferences);
      tx.commit();
    } else {
      manager.merge(mapUserPreferences);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getMapUserRole(java.lang
   * .String, java.lang.Long)
   */
  @Override
  public MapUserRole getMapUserRoleForMapProject(String userName,
    Long mapProjectId) throws Exception {

    Logger.getLogger(MappingServiceJpa.class).info(
        "Finding user's role " + userName + " " + mapProjectId);

    // get the user and map project for parameters
    MapUser mapUser = getMapUser(userName);
    MapProject mapProject = getMapProject(mapProjectId);

    // check which collection this user belongs to for this project
    if (mapProject.getMapLeads().contains(mapUser)) {
      return MapUserRole.LEAD;
    } else if (mapProject.getMapSpecialists().contains(mapUser)) {
      return MapUserRole.SPECIALIST;
    }

    // check for application administrator
    if (mapUser.getApplicationRole().equals(MapUserRole.ADMINISTRATOR)) {
      return MapUserRole.ADMINISTRATOR;
    }

    // return role NONE if user is not on role lists and project is private
    if (!mapProject.isPublic())
      return MapUserRole.NONE;

    // default role is Viewer
    return MapUserRole.VIEWER;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * getProjectSpecificAlgorithmHandler
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  @XmlTransient
  public ProjectSpecificAlgorithmHandler getProjectSpecificAlgorithmHandler(
    MapProject mapProject) throws InstantiationException,
    IllegalAccessException, ClassNotFoundException {

    ProjectSpecificAlgorithmHandler algorithmHandler =
        (ProjectSpecificAlgorithmHandler) Class.forName(
            mapProject.getProjectSpecificAlgorithmHandlerClass()).newInstance();

    algorithmHandler.setMapProject(mapProject);
    return algorithmHandler;

  }

  /**
   * Sets the valid field for tree positions, given a map project id.
   * 
   * @param treePositions the tree positions
   * @param mapProjectId the map project id
   * @return the revised list of tree positions
   * @throws Exception the exception
   */
  @Override
  public TreePositionList setTreePositionValidCodes(
    List<TreePosition> treePositions, Long mapProjectId) throws Exception {

    Logger.getLogger(MappingServiceJpa.class).info(
        "Setting tree position valid codes");

    // get the map project and its algorithm handler
    MapProject mapProject = getMapProject(mapProjectId);
    ProjectSpecificAlgorithmHandler algorithmHandler =
        getProjectSpecificAlgorithmHandler(mapProject);

    setTreePositionValidCodesHelper(treePositions, algorithmHandler);

    TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());
    return treePositionList;
  }

  /**
   * Helper function to recursively cycle over nodes and their children.
   * Instantiated to prevent necessity for retrieving algorithm handler at each
   * level. Note: Not necessary to return objects, tree positions are persisted
   * objects
   * 
   * @param treePositions the tree positions
   * @param algorithmHandler the algorithm handler
   * @throws Exception the exception
   */
  public void setTreePositionValidCodesHelper(List<TreePosition> treePositions,
    ProjectSpecificAlgorithmHandler algorithmHandler) throws Exception {

    // cycle over all tree positions and check target code, recursively
    // cycle over children
    for (TreePosition tp : treePositions) {

      tp.setValid(algorithmHandler.isTargetCodeValid(tp.getTerminologyId()));

      setTreePositionValidCodesHelper(tp.getChildren(), algorithmHandler);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#
   * setTreePositionTerminologyNotes(java.util.List, java.lang.Long)
   */
  @Override
  public TreePositionList setTreePositionTerminologyNotes(
    List<TreePosition> treePositions, Long mapProjectId) throws Exception {

    Logger.getLogger(MappingServiceJpa.class).info(
        "Setting tree position terminology notes");

    // get the map project and its algorithm handler
    MapProject mapProject = getMapProject(mapProjectId);
    ProjectSpecificAlgorithmHandler algorithmHandler =
        getProjectSpecificAlgorithmHandler(mapProject);

    // construct the tree position list
    TreePositionListJpa treePositionList = new TreePositionListJpa();
    treePositionList.setTreePositions(treePositions);
    treePositionList.setTotalCount(treePositions.size());

    // compute the target terminology notes
    algorithmHandler.computeTargetTerminologyNotes(treePositionList);

    return treePositionList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getOriginMapRecordsForConflict
   * (java.lang.Long)
   */
  @SuppressWarnings("unused")
  @Override
  public MapRecordList getOriginMapRecordsForConflict(Long mapRecordId)
    throws Exception {

    Logger.getLogger(MappingServiceJpa.class).info(
        "getRecordsInConflict with record id = " + mapRecordId.toString());

    MapRecordList conflictRecords = new MapRecordListJpa();

    MapRecord mapRecord = getMapRecord(mapRecordId);
    if (mapRecord == null) {
      throw new Exception(
          "getRecordsInConflict: Could not find map record with id = "
              + mapRecordId.toString() + "!");
    }

    MapProject mapProject = getMapProject(mapRecord.getMapProjectId());

    /*
     * Three cases where this is called CONFLICT_PROJECT: Two specialists in
     * conflict Requires two CONFLICT_DETECTED records in database Record must
     * be CONFLICT_NEW, CONFLICT_IN_PROGRESS, CONFLICT_RESOLVED FIX_ERROR_PATH:
     * For any project type Requires one REVIEW_NEEDED record in database
     * Requires one REVISION record in database Record must be REVIEW_NEW,
     * REVIEW_IN_PROGRESS, REVIEW_RESOLVED REVIEW_PROJECT: For either normal
     * workflow or FIX_ERROR_PATH Requires one REVIEW_NEEDED record in database
     * Record must be REVIEW_NEW, REVIEW_IN_PROGRESS, REVIEW_RESOLVED record
     */

    // if a conflict project and two specialist records in conflict,
    // retrieve the CONFLICT_DETECTED
    // records
    if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT)
        && (mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
            || mapRecord.getWorkflowStatus().equals(
                WorkflowStatus.CONFLICT_IN_PROGRESS) || mapRecord
            .getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED))) {

      // As with review record below, this try/catch block is a
      // method to handle situations where originId set has more than
      // two elements (i.e. is unordered)
      // and where records retrieved are no longer in the database (e.g.
      // in
      // audit history)
      try {
        for (Long originId : mapRecord.getOriginIds()) {
          MapRecord mr = getMapRecord(originId);
          if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
            conflictRecords.addMapRecord(getMapRecord(originId));
          }
        }

        if (conflictRecords.getCount() == 2) {
          conflictRecords.setTotalCount(conflictRecords.getCount());
          return conflictRecords;
        } else {
          throw new Exception(
              "Could not retrieve two CONFLICT_DETECTED records for conflict");
        }
      } catch (Exception e) {
        // do nothing
      }

    } else if ((mapProject.getWorkflowType().equals(
        WorkflowType.CONFLICT_PROJECT) && (mapRecord.getWorkflowStatus()
        .equals(WorkflowStatus.REVIEW_NEW)
        || mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_IN_PROGRESS)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS) || mapRecord
        .getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)))

        ||

        (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT) && mapRecord
            .getOriginIds().size() > 2)) {

      boolean foundReviewRecord = false; // the specialist's completed
      // work
      boolean foundRevisionRecord = false; // the original published work

      for (Long originId : mapRecord.getOriginIds()) {
        System.out.println("Getting origin id:  " + originId);
        MapRecord mr = getMapRecord(originId);

        // This try/catch block is here to prevent problems
        // with the origin ids being an unordered set
        // Only records currently in the database are returned
        try {

          if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)
              || mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
            conflictRecords.addMapRecord(getMapRecord(originId));
            foundReviewRecord = true;
          } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
            conflictRecords.addMapRecord(getMapRecord(originId));
            foundRevisionRecord = true;
          }

        } catch (Exception e) {
          // do nothing
        }

        // once records are found, stop processing origin ids
        if (foundReviewRecord && foundRevisionRecord) {
          conflictRecords.setTotalCount(conflictRecords.getCount());
          return conflictRecords;
        }

      }

      // this is the case on the QA_PATH
      if (foundReviewRecord || foundRevisionRecord) {
        conflictRecords.setTotalCount(conflictRecords.getCount());
        return conflictRecords;
      }

    } else if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT)
        && mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEW)
        || mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.REVIEW_IN_PROGRESS)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.REVIEW_RESOLVED)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_NEW)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_IN_PROGRESS)
        || mapRecord.getWorkflowStatus().equals(WorkflowStatus.QA_RESOLVED)) {

      System.out.println("Getting origin id for REVIEW_PROJECT record");

      WorkflowService workflowService = new WorkflowServiceJpa();

      TrackingRecord tr =
          workflowService.getTrackingRecordForMapProjectAndConcept(mapProject,
              mapRecord.getConceptId());

      if (tr.getWorkflowPath().equals(WorkflowPath.REVIEW_PROJECT_PATH)
          || tr.getWorkflowPath().equals(WorkflowPath.QA_PATH)) {

        for (Long originId : mapRecord.getOriginIds()) {

          try {
            MapRecord mr =
                getMapRecord(mapRecord.getOriginIds().iterator().next());

            // check assumption
            if (!mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)
                && !mr.getWorkflowStatus().equals(WorkflowStatus.QA_NEEDED)) {
              workflowService.close();
              throw new Exception(
                  "Single origin record found for review, but was not REVIEW_NEEDED or QA_NEEDED");
            }

            // add and return this record
            conflictRecords.addMapRecord(mr);
            conflictRecords.setTotalCount(conflictRecords.getCount());
            workflowService.close();

            return conflictRecords;

          } catch (Exception e) {
            // do nothing
          }

        }

      } else if (tr.getWorkflowPath().equals(WorkflowPath.FIX_ERROR_PATH)) {

        boolean foundReviewRecord = false; // the specialist's completed
        // work
        boolean foundRevisionRecord = false; // the original published
        // work

        for (Long originId : mapRecord.getOriginIds()) {
          System.out.println("Getting origin id:  " + originId);
          MapRecord mr = getMapRecord(originId);

          // As with other try blocks in this section, this
          // try/catch block is implemented to handle situations
          // where the unordered originIds set leads to attempts
          // to retrieve records that no longer exist
          try {

            if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
              conflictRecords.addMapRecord(getMapRecord(originId));
              foundReviewRecord = true;
            } else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
              conflictRecords.addMapRecord(getMapRecord(originId));
              foundRevisionRecord = true;
            }

          } catch (Exception e) {
            // do nothing, attempted to find a record that no longer
            // exists
          }

          // once records are found, stop processing origin ids
          if (foundReviewRecord && foundRevisionRecord) {
            conflictRecords.setTotalCount(conflictRecords.getCount());
            workflowService.close();
            return conflictRecords;
          }

        }
      } else {
        throw new Exception(
            "Could not retrieve exactly one origin id for REVIEW_PROJECT path");
      }

    } else {
      throw new Exception(
          "Invalid map record passed to conflict origins routine");
    }

    return conflictRecords;
  }

  /**
   * Validate that a single user cannot have more than one role on a particular
   * map project.
   * 
   * @param mapProject the map project
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private void validateUserAndRole(MapProject mapProject) throws Exception {
    Map<MapUser, String> userToRoleMap = new HashMap<>();
    for (MapUser user : mapProject.getMapLeads()) {
      // if user is already in map, throw exception
      if (userToRoleMap.containsKey(user))
        throw new IllegalStateException("Error: User " + user.getName()
            + " has more than one role.");
      else
        userToRoleMap.put(user, "lead");
    }
    for (MapUser user : mapProject.getMapSpecialists()) {
      // if user is already in map, throw exception
      if (userToRoleMap.containsKey(user))
        throw new IllegalStateException("Error: User " + user.getName()
            + " has more than one role.");
      else
        userToRoleMap.put(user, "specialist");
    }

  }

  /**
   * Handle map record lazy initialization.
   * 
   * @param mapRecord the map record
   */
  @SuppressWarnings("static-method")
  private void handleMapRecordLazyInitialization(MapRecord mapRecord) {
    // handle all lazy initializations
    mapRecord.getOwner().getEmail();
    mapRecord.getLastModifiedBy().getEmail();
    mapRecord.getMapNotes().size();
    mapRecord.getMapPrinciples().size();
    mapRecord.getOriginIds().size();
    mapRecord.getLabels().size();
    mapRecord.getReasonsForConflict().size();
    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      if (mapEntry.getMapRelation() != null)
        mapEntry.getMapRelation().getName();
      mapEntry.getMapAdvices().size();
    }

  }

  /**
   * Handle map project lazy initialization.
   * 
   * @param mapProject the map project
   */
  @SuppressWarnings("static-method")
  private void handleMapProjectLazyInitialization(MapProject mapProject) {
    // handle all lazy initializations
    mapProject.getMapAdvices().size();
    mapProject.getMapRelations().size();
    mapProject.getMapLeads().size();
    mapProject.getMapSpecialists().size();
    mapProject.getMapPrinciples().size();
    mapProject.getPresetAgeRanges().size();
    mapProject.getErrorMessages().size();
    mapProject.getReportDefinitions().size();
    mapProject.getScopeConcepts().size();
    mapProject.getScopeExcludedConcepts().size();
    mapProject.getErrorMessages().size();

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#checkMapGroupsForMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void checkMapGroupsForMapProject(MapProject mapProject,
    boolean updateRecords) throws Exception {

    if (updateRecords) {
      this.setTransactionPerOperation(false);
      this.beginTransaction();
    }
    Logger.getLogger(MappingServiceJpa.class).info(
        "Checking map group numbering and empty groups for project "
            + mapProject.getName());
    Logger.getLogger(MappingServiceJpa.class).info(
        "  Mode: " + (updateRecords ? "Update" : "Check"));

    MapRecordList mapRecordsInProject =
        this.getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(MappingServiceJpa.class).info(
        "Checking " + mapRecordsInProject.getCount() + " map records.");

    // logging variables
    int nRecordsChecked = 0;
    int nRecordsRemapped = 0;
    int nMessageInterval =
        (int) Math.floor(mapRecordsInProject.getCount() / 10);

    // /////////////////////////
    // Empty Group Checking ///
    // /////////////////////////

    // tracking/logging variables
    int nMapRecordsAltered = 0;
    int nMapGroupsRemoved = 0;
    int nMapEntriesRemoved = 0;

    // cycle over all map records for this project
    for (MapRecord mr : mapRecordsInProject.getIterable()) {
      Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

      boolean mapRecordAltered = false;

      // sort entries by group
      for (MapEntry me : mr.getMapEntries()) {
        // get the cached entries
        List<MapEntry> entries = entriesByGroup.get(me.getMapGroup());

        // if this group not encountered, instantiate new list
        if (entries == null)
          entries = new ArrayList<>();

        // add entry to list
        entries.add(me);

        // replace cached list
        entriesByGroup.put(me.getMapGroup(), entries);
      }

      // NOTE: This duplicates DefaultProjectSpecificAlgorithmHandler
      // Consider changing return content of Validation Result
      // cycle over each group
      for (int group : entriesByGroup.keySet()) {

        // skip first group
        if (group != 1) {
          List<MapEntry> entries = entriesByGroup.get(group);

          // cycle over entries in this group
          boolean isValidGroup = false;
          for (MapEntry entry : entries) {
            if (entry.getTargetId() != null && !entry.getTargetId().equals(""))
              isValidGroup = true;
          }

          // if not a valid group, remove all entries
          if (!isValidGroup) {

            mapRecordAltered = true;

            for (MapEntry entry : entries) {
              mr.removeMapEntry(entry);
              nMapEntriesRemoved++;
            }

            nMapGroupsRemoved++;
          }
        }
      }

      // if record latered and update flag set, updat ethe record
      if (mapRecordAltered && updateRecords) {
        this.handleMapRecordLazyInitialization(mr);
        this.updateMapRecord(mr);
      }
      // update the counter
      if (mapRecordAltered) {
        nMapRecordsAltered++;
      }
    }

    if (updateRecords) {
      this.commit();
      this.beginTransaction();
    }

    Logger.getLogger(MappingServiceJpa.class).info(
        "Records modified: " + nMapRecordsAltered);
    Logger.getLogger(MappingServiceJpa.class).info(
        "Groups removed  : " + nMapGroupsRemoved);
    Logger.getLogger(MappingServiceJpa.class).info(
        "Entries removed : " + nMapEntriesRemoved);

    // ////////////////////////////////////////////
    // Group Number Checking //
    // MUST come after high-level group checking //
    // ////////////////////////////////////////////

    // cycle over all records
    for (MapRecord mapRecord : mapRecordsInProject.getIterable()) {

      // create a map representing oldGroup -> newGroup
      List<Integer> mapGroupsFound = new ArrayList<>();

      // map of remappings
      Map<Integer, Integer> mapGroupRemapping = new HashMap<>();

      // find the existing groups
      for (MapEntry mapEntry : mapRecord.getMapEntries()) {

        // if this group not already present, add to list
        if (!mapGroupsFound.contains(mapEntry.getMapGroup()))
          mapGroupsFound.add(mapEntry.getMapGroup());
      }

      // sort the groups found
      Collections.sort(mapGroupsFound);

      // get the total number of groups present
      int nMapGroups = mapGroupsFound.size();

      // if no groups at all, skip this record
      if (nMapGroups > 0) {

        // flag for whether map record needs to be modified
        boolean mapGroupsRemapped = false;

        // shorthand the min/max values
        int minGroup = Collections.min(mapGroupsFound);
        int maxGroup = Collections.max(mapGroupsFound);

        // if the max group is not equal to the number of groups
        // or the min group is not equal to 1
        if (maxGroup != nMapGroups || minGroup != 1) {

          mapGroupsRemapped = true;

          // counter for groups
          int cumMissingGroups = 0;

          // cycle over all group values from 0 to max group
          for (int i = 0; i <= maxGroup; i++) {

            // if this group present,
            // - remove the group from set
            // - subtract current value by the cumulative number of
            // missed groups found
            // - add 1 and subtract the value of the min group
            // - re-add the new remapped group
            // otherwise
            // - increment the missing group counter
            //
            // e.g. (0, 3, 5) goes through the following steps:
            // 0 -> 0 - 0 + 1 = 1 -> map as (0, 1)
            // 1 -> not present, increment offset
            // 2 -> not present, increment offset
            // 3 -> 3 - 2 + 1 = 2 -> map as (3, 2)
            // 4 -> not present, increment offset
            // 5 -> 5 - 3 + 1 = 3 -> map as (5, 3)
            //
            // Note that for this algorithm, zero is considered a
            // "missing group" if not present
            // 0 -> not present, increment offset
            // 1 -> 1 - 1 + 1 = 1 -> map as (1, 1)
            if (mapGroupsFound.contains(i)) {
              mapGroupRemapping.put(i, i - cumMissingGroups + 1);
            } else {
              cumMissingGroups++;
            }
          }

        }

        // if errors detected, log
        if (mapGroupsRemapped) {

          nRecordsRemapped++;

          Logger.getLogger(MappingServiceJpa.class).info(
              "Record requires remapping:  " + mapRecord.getId() + ": "
                  + mapRecord.getConceptId() + ", "
                  + mapRecord.getConceptName());

          String mapLogStr = "";
          for (Integer i : mapGroupRemapping.keySet()) {
            mapLogStr += " " + i + "->" + mapGroupRemapping.get(i);
          }

          Logger.getLogger(MappingServiceJpa.class).info(
              "  Groups to remap: " + mapLogStr);
        }

        // if errors detected and update mode specified, update
        if (mapGroupsRemapped && updateRecords) {

          this.handleMapRecordLazyInitialization(mapRecord);

          for (MapEntry me : mapRecord.getMapEntries()) {
            if (mapGroupRemapping.containsKey(me.getMapGroup())) {
              me.setMapGroup(mapGroupRemapping.get(me.getMapGroup()));
            }
          }

          Logger.getLogger(MappingServiceJpa.class).info("  Updating record.");
          this.updateMapRecord(mapRecord);

        }

        // output logging information
        if (++nRecordsChecked % nMessageInterval == 0) {
          Logger.getLogger(MappingServiceJpa.class).info(
              "  " + nRecordsChecked + " records processed ("
                  + (nRecordsChecked / nMessageInterval * 10) + "%), "
                  + nRecordsRemapped + " with group errors");
        }
      }
    }

    if (updateRecords) {
      this.commit();
    }

    Logger.getLogger(MappingServiceJpa.class).info(
        "  " + nRecordsChecked + " total records processed ("
            + nRecordsRemapped + " with group errors");
  }

  @Override
  public void removeMapAdviceFromEnvironment(MapAdvice mapAdvice)
    throws Exception {

    // commit changes after each object type
    setTransactionPerOperation(false);

    // flag used to only update objects where advice was removed
    boolean adviceRemoved;

    // counter for number of objects advice removed from
    int nAdviceRemoved = 0;

    Logger.getLogger(MappingServiceJpa.class).info(
        "Removing map advice from map records...");

    // remove map advice from all map entries, found via map records
    beginTransaction();

    for (MapRecord mr : getMapRecords().getIterable()) {

      adviceRemoved = false;

      // cycle over entries
      for (MapEntry me : mr.getMapEntries()) {

        if (me.getMapAdvices().contains(mapAdvice)) {
          me.removeMapAdvice(mapAdvice);
          adviceRemoved = true;
          nAdviceRemoved++;
        }
      }

      if (adviceRemoved)
        updateMapRecord(mr);

    }

    Logger.getLogger(MappingServiceJpa.class).info(
        "  " + nAdviceRemoved + " instances removed from map entries");
    commit();
    Logger.getLogger(MappingServiceJpa.class).info("  " + "Changes committed");

    // remove map advice from all project allowable sets
    Logger.getLogger(MappingServiceJpa.class).info(
        "Removing map advice from map projects...");
    beginTransaction();
    nAdviceRemoved = 0;

    for (MapProject mp : getMapProjects().getIterable()) {

      adviceRemoved = false;

      if (mp.getMapAdvices().contains(mapAdvice)) {
        mp.removeMapAdvice(mapAdvice);
        adviceRemoved = true;
      }

      if (adviceRemoved)
        updateMapProject(mp);
    }

    removeMapAdvice(mapAdvice.getId());

    Logger.getLogger(MappingServiceJpa.class).info(
        "  " + nAdviceRemoved + " instances removed from map projects");
    commit();
    Logger.getLogger(MappingServiceJpa.class).info("  " + "Changes committed");

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#getMapProjectMetadata()
   */
  @Override
  public Map<String, Map<String, String>> getMapProjectMetadata()
    throws Exception {
    Map<String, Map<String, String>> idNameMapList = new HashMap<>();

    Map<String, String> workflowNameMap = new HashMap<>();
    for (WorkflowType type : WorkflowType.values()) {
      workflowNameMap.put(type.name(), type.getDisplayName());
    }
    if (workflowNameMap.size() > 0) {
      idNameMapList.put("Workflow Types", workflowNameMap);
    }

    Map<String, String> relationStyleNameMap = new HashMap<>();
    for (RelationStyle type : RelationStyle.values()) {
      relationStyleNameMap.put(type.name(), type.getDisplayName());
    }
    if (relationStyleNameMap.size() > 0) {
      idNameMapList.put("Relation Styles", relationStyleNameMap);
    }

    Map<String, String> mapRefsetPatternNameMap = new HashMap<>();
    for (MapRefsetPattern type : MapRefsetPattern.values()) {
      mapRefsetPatternNameMap.put(type.name(), type.getDisplayName());
    }
    if (mapRefsetPatternNameMap.size() > 0) {
      idNameMapList.put("Map Refset Patterns", mapRefsetPatternNameMap);
    }

    // return a default project specific algorithm handler
    Map<String, String> projectSpecificAlgorithmHandlers = new HashMap<>();

    // add the Default Specific Project Algorithm Handler
    // Don't want any of the other specific ones, only want the class name of
    // the default
    projectSpecificAlgorithmHandlers
        .put("default",
            "org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler");

    idNameMapList.put("Project Specific Handlers",
        projectSpecificAlgorithmHandlers);

    return idNameMapList;
  }

  @Override
  public SearchResultList getScopeConceptsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter) throws Exception {

    SearchResultList searchResultList = new SearchResultListJpa();

    // if pfsParamter is null, construct a blank pfs parameter object
    PfsParameter localPfsParameter = pfsParameter;
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // convert set to list
    List<String> scopeConcepts = new ArrayList<>(mapProject.getScopeConcepts());

    // sort lexically
    Collections.sort(scopeConcepts);

    // calculate the start and end indices
    int startIndex =
        localPfsParameter.getStartIndex() == -1 ? 0 : Math.min(
            localPfsParameter.getStartIndex(), scopeConcepts.size());
    int endIndex =
        localPfsParameter.getMaxResults() == -1 ? scopeConcepts.size() : Math
            .min(startIndex + localPfsParameter.getMaxResults(),
                scopeConcepts.size());

    // set the total count
    searchResultList.setTotalCount(scopeConcepts.size());

    // get the sublist
    scopeConcepts = scopeConcepts.subList(startIndex, endIndex);

    // for each concept id in sublist, get the
    ContentService contentService = new ContentServiceJpa();
    for (String conceptId : scopeConcepts) {

      SearchResult searchResult = new SearchResultJpa();
      searchResult.setTerminology(mapProject.getSourceTerminology());
      searchResult.setTerminologyVersion(mapProject
          .getSourceTerminologyVersion());
      searchResult.setTerminologyId(conceptId);

      // get the concept
      Concept concept =
          contentService.getConcept(conceptId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // if concept found, set search result parameters from concept,
      // otherwise, set error text
      searchResult.setValue(concept == null ? "Concept could not be retrieved"
          : concept.getDefaultPreferredName());

      searchResultList.addSearchResult(searchResult);
    }
    contentService.close();

    return searchResultList;
  }

  @Override
  public SearchResultList getScopeExcludedConceptsForMapProject(
    MapProject mapProject, PfsParameter pfsParameter) throws Exception {

    SearchResultList searchResultList = new SearchResultListJpa();

    PfsParameter localPfsParameter = pfsParameter;

    // if pfsParamter is null, construct a blank pfs parameter object
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    // convert set to list
    List<String> scopeExcludedConcepts =
        new ArrayList<>(mapProject.getScopeExcludedConcepts());

    // sort lexically
    Collections.sort(scopeExcludedConcepts);

    // calculate the start and end indices
    int startIndex =
        localPfsParameter.getStartIndex() == -1 ? 0 : Math.min(
            localPfsParameter.getStartIndex(), scopeExcludedConcepts.size());
    int endIndex =
        localPfsParameter.getMaxResults() == -1 ? scopeExcludedConcepts.size()
            : Math.min(startIndex + localPfsParameter.getMaxResults(),
                scopeExcludedConcepts.size());

    // set the total count
    searchResultList.setTotalCount(scopeExcludedConcepts.size());

    // get the sublist
    scopeExcludedConcepts = scopeExcludedConcepts.subList(startIndex, endIndex);

    // for each concept id in sublist, get the concept name
    ContentService contentService = new ContentServiceJpa();
    for (String conceptId : scopeExcludedConcepts) {

      SearchResult searchResult = new SearchResultJpa();
      searchResult.setTerminology(mapProject.getSourceTerminology());
      searchResult.setTerminologyVersion(mapProject
          .getSourceTerminologyVersion());
      searchResult.setTerminologyId(conceptId);

      // get the concept
      Concept concept =
          contentService.getConcept(conceptId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // if concept found, set search result parameters from concept,
      // otherwise, set error text
      searchResult.setValue(concept == null ? "Concept could not be retrieved"
          : concept.getDefaultPreferredName());

      searchResultList.addSearchResult(searchResult);
    }
    contentService.close();

    return searchResultList;
  }

}
