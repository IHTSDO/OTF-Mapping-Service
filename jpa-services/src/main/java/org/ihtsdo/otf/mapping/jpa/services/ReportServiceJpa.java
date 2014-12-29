package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemListJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.reports.ReportNote;
import org.ihtsdo.otf.mapping.reports.ReportNoteJpa;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;
import org.ihtsdo.otf.mapping.reports.ReportResultItemJpa;
import org.ihtsdo.otf.mapping.reports.ReportResultJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * JPA enabled implementation of {@link ReportService}.
 */
public class ReportServiceJpa extends RootServiceJpa implements ReportService {

  /** The map record indexed field names. */
  protected static Set<String> reportFieldNames;

  /**
   * Instantiates a new report service jpa.
   * 
   * @throws Exception the exception
   */
  public ReportServiceJpa() throws Exception {
    super();
    if (reportFieldNames == null) {
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
    reportFieldNames = new HashSet<>();
    EntityManager manager = factory.createEntityManager();
    FullTextEntityManager fullTextEntityManager =
        org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
    IndexReaderAccessor indexReaderAccessor =
        fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
    Set<String> indexedClassNames =
        fullTextEntityManager.getSearchFactory().getStatistics()
            .getIndexedClassNames();
    for (String indexClass : indexedClassNames) {
      if (indexClass.indexOf("ReportJpa") != 0) {
        IndexReader indexReader = indexReaderAccessor.open(indexClass);
        try {
          for (FieldInfo info : ReaderUtil.getMergedFieldInfos(indexReader)) {
            reportFieldNames.add(info.name);
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
   * @see org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa#close()
   */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
    }
  }

  // /////////////////////////////////////////////////////
  // CRUD Services for ReportJpa
  // /////////////////////////////////////////////////////
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getReports()
   */
  @Override
  @SuppressWarnings("unchecked")
  public ReportList getReports() {

    List<Report> Reports = null;

    // construct query
    javax.persistence.Query query =
        manager.createQuery("select m from ReportJpa m");

    Reports = query.getResultList();

    // force instantiation of lazy collections
    for (Report Report : Reports) {
      handleReportLazyInitialization(Report);
    }

    ReportListJpa ReportList = new ReportListJpa();
    ReportList.setReports(Reports);
    ReportList.setTotalCount(Reports.size());
    return ReportList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#findReportsForQuery(java
   * .lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @Override
  @SuppressWarnings("unchecked")
  public SearchResultList findReportsForQuery(String query,
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
                reportFieldNames.toArray(new String[0]),
                searchFactory.getAnalyzer(ReportJpa.class));
        queryParser.setAllowLeadingWildcard(false);
        luceneQuery = queryParser.parse(query);

      } else { // field:value
        QueryParser queryParser =
            new QueryParser(Version.LUCENE_36, "summary",
                searchFactory.getAnalyzer(ReportJpa.class));
        luceneQuery = queryParser.parse(query);
      }
    } catch (ParseException e) {
      throw new LocalException(
          "The specified search terms cannot be parsed.  Please check syntax and try again.");
    }

    List<Report> reports;

    reports =
        fullTextEntityManager.createFullTextQuery(luceneQuery, ReportJpa.class)
            .getResultList();
    // if a parse exception, throw a local exception

    Logger.getLogger(this.getClass()).debug(
        Integer.toString(reports.size()) + " reports retrieved");

    for (Report report : reports) {
      s.addSearchResult(new SearchResultJpa(report.getId(), null, report
          .getName(), ""));

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

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#getReport(java.lang.Long)
   */
  @Override
  public Report getReport(Long reportId) {
    Report r = null;

    javax.persistence.Query query =
        manager.createQuery("select r from ReportJpa r where id = :id");
    query.setParameter("id", reportId);

    r = (Report) query.getSingleResult();

    // handle lazy initializations
    r.getResults().size();
    r.getNotes().size();

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#addReport(org.ihtsdo.otf
   * .mapping.reports.Report)
   */
  @Override
  public Report addReport(Report report) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(report);
      tx.commit();

      return report;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(report);
      return report;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#updateReport(org.ihtsdo
   * .otf.mapping.reports.Report)
   */
  @Override
  public void updateReport(Report report) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(report);
      tx.commit();
    } else {
      manager.merge(report);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#removeReport(java.lang.
   * Long)
   */
  @Override
  public void removeReport(Long reportId) {

    Report report = manager.find(ReportJpa.class, reportId);

    // now remove the entry
    tx.begin();
    if (manager.contains(report)) {
      manager.remove(report);
    } else {
      manager.remove(manager.merge(report));
    }
    tx.commit();

  }

  // /////////////////////////////////////////////////////
  // CRUD Services for ReportResultJpa
  // /////////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#getReportResult(java.lang
   * .Long)
   */
  @Override
  public ReportResult getReportResult(Long reportResultId) {
    ReportResult r = null;

    javax.persistence.Query query =
        manager.createQuery("select r from ReportResultJpa r where id = :id");
    query.setParameter("id", reportResultId);

    r = (ReportResult) query.getSingleResult();

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#addReportResult(org.ihtsdo
   * .otf.mapping.reports.ReportResult)
   */
  @Override
  public ReportResult addReportResult(ReportResult reportResult) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(reportResult);
      tx.commit();

      return reportResult;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(reportResult);
      return reportResult;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#updateReportResult(org.
   * ihtsdo.otf.mapping.reports.ReportResult)
   */
  @Override
  public void updateReportResult(ReportResult reportResult) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(reportResult);
      tx.commit();
    } else {
      manager.merge(reportResult);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#removeReportResult(java
   * .lang.Long)
   */
  @Override
  public void removeReportResult(Long reportResultId) {

    ReportResult reportResult =
        manager.find(ReportResultJpa.class, reportResultId);

    // now remove the entry
    tx.begin();
    if (manager.contains(reportResult)) {
      manager.remove(reportResult);
    } else {
      manager.remove(manager.merge(reportResult));
    }
    tx.commit();

  }

  // /////////////////////////////////////////////////////
  // CRUD Services for ReportResultItemJpa
  // /////////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getReportResultItem(java
   * .lang.Long)
   */
  @Override
  public ReportResultItem getReportResultItem(Long reportResultItemId) {
    ReportResultItem r = null;

    javax.persistence.Query query =
        manager
            .createQuery("select r from ReportResultItemJpa r where id = :id");
    query.setParameter("id", reportResultItemId);

    r = (ReportResultItem) query.getSingleResult();

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#addReportResultItem(org
   * .ihtsdo.otf.mapping.reports.ReportResultItem)
   */
  @Override
  public ReportResultItem addReportResultItem(ReportResultItem reportResultItem) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(reportResultItem);
      tx.commit();

      return reportResultItem;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(reportResultItem);
      return reportResultItem;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#updateReportResultItem(
   * org.ihtsdo.otf.mapping.reports.ReportResultItem)
   */
  @Override
  public void updateReportResultItem(ReportResultItem reportResultItem) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(reportResultItem);
      tx.commit();
    } else {
      manager.merge(reportResultItem);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#removeReportResultItem(
   * java.lang.Long)
   */
  @Override
  public void removeReportResultItem(Long reportResultItemId) {

    ReportResultItem reportResultItem =
        manager.find(ReportResultItemJpa.class, reportResultItemId);

    // now remove the entry
    tx.begin();
    if (manager.contains(reportResultItem)) {
      manager.remove(reportResultItem);
    } else {
      manager.remove(manager.merge(reportResultItem));
    }
    tx.commit();

  }

  // /////////////////////////////////////////////////////
  // CRUD Services for ReportNoteJpa
  // /////////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getReportNote(java.lang
   * .Long)
   */
  @Override
  public ReportNote getReportNote(Long reportNoteId) {
    ReportNote r = null;

    javax.persistence.Query query =
        manager.createQuery("select r from ReportNoteJpa r where id = :id");
    query.setParameter("id", reportNoteId);

    r = (ReportNote) query.getSingleResult();

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#addReportNote(org.ihtsdo
   * .otf.mapping.reports.ReportNote)
   */
  @Override
  public ReportNote addReportNote(ReportNote reportNote) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(reportNote);
      tx.commit();

      return reportNote;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(reportNote);
      return reportNote;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#updateReportNote(org.ihtsdo
   * .otf.mapping.reports.ReportNote)
   */
  @Override
  public void updateReportNote(ReportNote reportNote) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(reportNote);
      tx.commit();
    } else {
      manager.merge(reportNote);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#removeReportNote(java.lang
   * .Long)
   */
  @Override
  public void removeReportNote(Long reportNoteId) {

    ReportNote reportNote = manager.find(ReportNoteJpa.class, reportNoteId);

    // now remove the entry
    tx.begin();
    if (manager.contains(reportNote)) {
      manager.remove(reportNote);
    } else {
      manager.remove(manager.merge(reportNote));
    }
    tx.commit();

  }

  /**
   * Handle report lazy initialization.
   * 
   * @param report the report
   */
  private void handleReportLazyInitialization(Report report) {
    report.getNotes().size();
    report.getResults().size();

    for (ReportResult result : report.getResults()) {
      result.getReportResultItems().size();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#getReportDefinitionsForRole
   * (org.ihtsdo.otf.mapping.helpers.MapUserRole)
   */
  @SuppressWarnings("unchecked")
  @Override
  public ReportDefinitionList getReportDefinitionsForRole(MapUserRole role) {
    ReportDefinitionList definitionList = new ReportDefinitionListJpa();

    // System.out.println("Getting report definitions for role: "
    // + role.toString());

    // get all definitions
    List<ReportDefinition> definitions =
        manager.createQuery(
            "select d from ReportDefinitionJpa d where isQACheck = false")
            .getResultList();

    // cycle over definitions and check if role has required privileges
    for (ReportDefinition definition : definitions) {
      if (role.hasPrivilegesOf(definition.getRoleRequired()))
        definitionList.addReportDefinition(definition);
    }

    // return the definition list
    return definitionList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getReportDefinitions()
   */
  @Override
  @SuppressWarnings("unchecked")
  public ReportDefinitionList getReportDefinitions() {

    List<ReportDefinition> ReportDefinitions = null;

    // construct query
    javax.persistence.Query query =
        manager
            .createQuery("select m from ReportDefinitionJpa m where isQACheck = false");

    ReportDefinitions = query.getResultList();

    ReportDefinitionListJpa ReportDefinitionList =
        new ReportDefinitionListJpa();
    ReportDefinitionList.setReportDefinitions(ReportDefinitions);
    ReportDefinitionList.setTotalCount(ReportDefinitions.size());
    return ReportDefinitionList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportDefinitionService#getReportDefinition
   * (java.lang.Long)
   */
  @Override
  public ReportDefinition getReportDefinition(Long reportDefinitionId) {
    ReportDefinition r = null;

    javax.persistence.Query query =
        manager
            .createQuery("select r from ReportDefinitionJpa r where id = :id");
    query.setParameter("id", reportDefinitionId);

    r = (ReportDefinition) query.getSingleResult();

    return r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportDefinitionService#addReportDefinition
   * (org.ihtsdo.otf .mapping.reportDefinitions.ReportDefinition)
   */
  @Override
  public ReportDefinition addReportDefinition(ReportDefinition reportDefinition) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.persist(reportDefinition);
      tx.commit();

      return reportDefinition;
    } else {
      if (!tx.isActive()) {
        throw new IllegalStateException(
            "Error attempting to change data without an active transaction");
      }
      manager.persist(reportDefinition);
      return reportDefinition;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportDefinitionService#
   * updateReportDefinition(org.ihtsdo
   * .otf.mapping.reportDefinitions.ReportDefinition)
   */
  @Override
  public void updateReportDefinition(ReportDefinition reportDefinition) {
    if (getTransactionPerOperation()) {
      tx = manager.getTransaction();
      tx.begin();
      manager.merge(reportDefinition);
      tx.commit();
    } else {
      manager.merge(reportDefinition);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportDefinitionService#
   * removeReportDefinition(java.lang. Long)
   */
  @Override
  public void removeReportDefinition(Long reportDefinitionId) throws Exception {

    ReportDefinition reportDefinition =
        manager.find(ReportDefinitionJpa.class, reportDefinitionId);

    // check if this definition is used by map projects
    MappingService mappingService = new MappingServiceJpa();
    MapProjectList mapProjects = mappingService.getMapProjects();
    mappingService.close();

    for (MapProject mapProject : mapProjects.getIterable()) {
      if (mapProject.getReportDefinitions().contains(reportDefinition)) {
        throw new LocalException(
            "Report definition is currently in use by project "
                + mapProject.getName() + " and cannot be deleted");
      }
    }

    // now remove the entry
    tx.begin();
    if (manager.contains(reportDefinition)) {
      manager.remove(reportDefinition);
    } else {
      manager.remove(manager.merge(reportDefinition));
    }
    tx.commit();

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getReportsForMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @Override
  public ReportList getReportsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter) {

    return getReportsForMapProjectAndReportDefinition(mapProject, null,
        pfsParameter);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#
   * getReportsForMapProjectAndReportDefinition
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.reports.ReportDefinition,
   * org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public ReportList getReportsForMapProjectAndReportDefinition(
    MapProject mapProject, ReportDefinition reportDefinition,
    PfsParameter pfsParameter) {

    Logger.getLogger(getClass()).info(
        "Retrieving reports for project "
            + mapProject.getName()
            + (reportDefinition == null ? "" : " of type "
                + reportDefinition.getName()));

    // instantiate empty paging/filtering/sorting object if null
    PfsParameter localPfsParameter = pfsParameter;
    if (localPfsParameter == null)
      localPfsParameter = new PfsParameterJpa();

    ReportList reportList = new ReportListJpa();

    // TODO: This is temporary, move this to a QueryBuilder structure
    // instead of using direct queries
    javax.persistence.Query query;

    // if definition supplied, return only results for that definition
    if (reportDefinition != null) {

      query =
          manager.createQuery(
              "select r from ReportJpa r "
                  + "where mapProjectId = :mapProjectId "
                  + "and reportDefinition_id = :reportDefinition_id "
                  + "order by timestamp desc").setParameter(
              "reportDefinition_id", reportDefinition.getId());

      // if no definition supplied, return all results, excluding QA
      // checks
    } else {
      query =
          manager
              .createQuery("select r from ReportJpa r, ReportDefinitionJpa d "
                  + "where mapProjectId = :mapProjectId "
                  + "and d.id = reportDefinition_id "
                  + "and d.isQACheck = false " + "order by timestamp desc");
    }

    // add project parameter for both cases
    query.setParameter("mapProjectId", mapProject.getId());

    // execute the query to get the number of reports
    // TODO This will be folded into lucene query builder later
    reportList.setTotalCount(query.getResultList().size());

    // if paging requested, set result parameters
    if (localPfsParameter.getStartIndex() != -1
        && localPfsParameter.getMaxResults() != -1) {
      query.setFirstResult(localPfsParameter.getStartIndex());
      query.setMaxResults(localPfsParameter.getMaxResults());
    }

    // reports are ALWAYS sorted in reverse order of date
    List<Report> reports = query.getResultList();

    for (Report report : reports)
      this.handleReportLazyInitialization(report);

    reportList.setReports(reports);

    return reportList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#generateReportsForDateRange
   * (org.ihtsdo.otf.mapping.model.MapProject,
   * org.ihtsdo.otf.mapping.model.MapUser, java.util.Date, java.util.Date)
   */
  @Override
  public void generateReportsForDateRange(MapProject mapProject,
    MapUser mapUser, Date startDate, Date endDate) throws Exception {

    Calendar cal = Calendar.getInstance();

    // get all report definitions
    ReportDefinitionList reportDefinitions = this.getReportDefinitions();

    // separate report definitions into daily and diff sets
    // note that this is necessary as diff reports require
    // a daily report to be present prior to calculation
    Set<ReportDefinition> dailyReportDefinitions = new HashSet<>();
    Set<ReportDefinition> diffReportDefinitions = new HashSet<>();

    // sort the report definitions into daily and diff sets
    for (ReportDefinition reportDefinition : reportDefinitions.getIterable()) {
      if (reportDefinition.isDiffReport() == true)
        diffReportDefinitions.add(reportDefinition);
      else
        dailyReportDefinitions.add(reportDefinition);
    }

    // cycle over dates until end date is passed
    Date localStartDate = startDate;
    while (localStartDate.compareTo(endDate) <= 0) {

      cal.setTime(localStartDate);

      Logger.getLogger(getClass()).info(
          "  Generating reports for " + localStartDate);

      // first, do the non-diff reports
      // note that these must be calculated prior to diff reports
      for (ReportDefinition reportDefinition : dailyReportDefinitions) {

        if (isDateToRunReport(reportDefinition, localStartDate)) {
          Logger.getLogger(getClass()).info(
              "    Generating report " + reportDefinition.getName());

          // Only generate reports for those that have queries
          if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
            Report report =
                generateReport(mapProject, mapUser,
                    reportDefinition.getName(), reportDefinition,
                    localStartDate, true);
            //  If report not generated (e.g. a diff report)
            if (report != null) {
              addReport(report);
            }
            Logger.getLogger(getClass()).info(
                "     Persisting report.");

            // persist the report
            report = this.addReport(report);
          }
        }
      }

      // second, do the diff reports
      // note that this requires the current daily report to be present
      // (i.e. calculated above)
      for (ReportDefinition reportDefinition : diffReportDefinitions) {

        if (isDateToRunReport(reportDefinition, localStartDate) == true) {

          Logger.getLogger(getClass()).info(
              "    Generating report " + reportDefinition.getName());
          Report report;
          try {

            report =
                this.generateReport(mapProject, mapUser,
                    reportDefinition.getName(), reportDefinition,
                    localStartDate, true);

            if (report != null) {
              Logger.getLogger(getClass()).info(
                  "     Persisting report " + report.toString());

              // persist the report
              report = this.addReport(report);
            } else {
              Logger.getLogger(ReportService.class).warn("    Skipping report");
            }
          } catch (LocalException e) {
            Logger.getLogger(getClass()).warn(e.getMessage());
          }

        }

      }

      // increment startDate by 1 day
      cal.setTime(localStartDate);
      cal.add(Calendar.DATE, 1);
      localStartDate = cal.getTime();
    }

  }

  /**
   * Helper function Given a report definition and a date, assesses whether this
   * report should be run, based on definition frequency
   * 
   * @param reportDefinition
   * @param date
   * @return
   * @throws Exception
   */
  private boolean isDateToRunReport(ReportDefinition reportDefinition, Date date)
    throws Exception {

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    switch (reportDefinition.getFrequency()) {
      case DAILY:
        return true;
      case MONDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY);
      case TUESDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY);
      case WEDNESDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY);
      case THURSDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY);
      case FRIDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
      case SATURDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
      case SUNDAY:
        return (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
      case FIRST_OF_MONTH:
        return cal.get(Calendar.DAY_OF_MONTH) == 1;
      case MID_MONTH:
        return cal.get(Calendar.DAY_OF_MONTH) == 15;
      case LAST_OF_MONTH:
        return cal.get(Calendar.DAY_OF_MONTH) == cal
            .getActualMaximum(Calendar.DAY_OF_MONTH);

      default:
        throw new Exception("Report definition found with invalid time period.");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#generateDailyReports(org
   * .ihtsdo.otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  public void generateDailyReports(MapProject mapProject, MapUser mapUser)
    throws Exception {

    // get today's date
    Date date = new Date();

    // call date range report generation with start and end date as today
    this.generateReportsForDateRange(mapProject, mapUser, date, date);

  }

  // /////////////////////////////////////////////////////
  // Report Generation Service
  // /////////////////////////////////////////////////////
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#generateReport(org.ihtsdo
   * .otf.mapping.model.MapProject, org.ihtsdo.otf.mapping.model.MapUser,
   * java.lang.String, org.ihtsdo.otf.mapping.reports.ReportDefinition,
   * java.util.Date, boolean)
   */
  @Override
  public Report generateReport(MapProject mapProject, MapUser owner,
    String name, ReportDefinition reportDefinition, Date date,
    boolean autoGenerated) throws Exception {

    Logger.getLogger(getClass()).info(
        "Generating report " + name + " for report definition "
            + reportDefinition.getName() + " for date " + date.toString());

    // instantiate the calendar object
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    // get the query to replace parameterized values
    String query = reportDefinition.getQuery();

    // if a diff report, need to construct a query based on specified report
    // definition
    if (reportDefinition.isDiffReport() == true) {

      query =
          "select 'Report' value, name itemName, id itemId "
              + "from reports where name = '"
              + reportDefinition.getDiffReportDefinitionName()
              + "' "
              + "and (timestamp = "
              + "(select max(timestamp) from reports "
              + "where timestamp <= :TIMESTAMP:) OR timestamp = "
              + "(select max(timestamp) from reports where timestamp <= :TIMESTAMP2:)) "
              + "order by timestamp desc;";

      // modify date by appropriate increment
      switch (reportDefinition.getTimePeriod()) {
        case ANNUALLY:
          cal.add(Calendar.YEAR, -1);
          break;
        case DAILY:
          cal.add(Calendar.DAY_OF_MONTH, -1);
          break;
        case MONTHLY:
          cal.add(Calendar.MONTH, -1);
          break;
        case WEEKLY:
          cal.add(Calendar.DAY_OF_MONTH, -7);
          break;
        default:
          break;
      }

      // replace the second timestamp with the modified date
      query =
          query
              .replaceAll(":TIMESTAMP2:", Long.toString(cal.getTimeInMillis()));
    }

    // replace the map project id and timestamp parameters
    query = query.replaceAll(":MAP_PROJECT_ID:", mapProject.getId().toString());
    query = query.replaceAll(":TIMESTAMP:", Long.toString(date.getTime()));

    // instantiate the report
    Report report = new ReportJpa();
    report.setReportDefinition(reportDefinition);
    report.setActive(true);

    report.setAutoGenerated(autoGenerated);
    report.setMapProjectId(mapProject.getId());
    report.setName(name);
    report.setOwner(owner);
    report.setQuery(query); // use the modified query (i.e. the actually
    // executed query)
    report.setQueryType(reportDefinition.getQueryType());
    report.setResultType(reportDefinition.getResultType());
    report.setTimestamp(cal.getTimeInMillis());

    // execute the query
    List<Object[]> results = null;
    switch (reportDefinition.getQueryType()) {
      case HQL:
        results = executeQuery(report.getQuery(), false);
        break;
      case LUCENE:
        // query map records index which returns map objects
        // value = "", itemId = mapRecord.getId(),
        // itemName=mapRecord.getConceptName()
        break;
      case SQL:
        results = executeQuery(report.getQuery(), true);
        break;
      case NONE:
        return null;
      default:
        break;

    }

    if (results == null)
      throw new Exception("Failed to retrieve results for query");

    Map<String, ReportResult> valueMap = new HashMap<>();

    // if a difference report
    if (reportDefinition.isDiffReport()) {

      if (results.size() != 2) {
        throw new Exception("Diff reqport query has unexpected number of results");
      }

      // get the ids corresponding to reports to be diffed
      report.setReport1Id(new Long(results.get(0)[2].toString()));
      report.setReport2Id(new Long(results.get(1)[2].toString()));
      Logger.getLogger(getClass()).info(
          "    report id 1 = " + report.getReport1Id());  
      Logger.getLogger(getClass()).info(
          "    report id 2 = " + report.getReport2Id());  

      // if either report id is null, cannot construct report, return null
      if (report.getReport1Id() == null || report.getReport2Id() == null) {
        return null;
      }

      // get the two reports
      Report report1;
      try {
        report1 = getReport(report.getReport1Id());
      } catch (Exception e) {
        throw new LocalException(
            "Could not retrieve first report for diff report", e);
      }

      Report report2;
      try {
        report2 = getReport(report.getReport2Id());
      } catch (Exception e) {
        throw new LocalException(
            "Could not retrieve second report for diff report", e);
      }

      // cycle over results in first report
      for (ReportResult result1 : report1.getResults()) {

        // check if an result with this value exists
        ReportResult result2 =
            this.getReportResultForValue(report2, result1.getValue());

        // find items in first not in second -- these are NEW
        ReportResult resultNew =
            this.getReportResultItemsNotInResult(result1, result2);

        resultNew.setDateValue("");
        resultNew.setName(result1.getName());
        resultNew.setProjectName(result1.getProjectName());
        resultNew.setValue(result1.getValue());
        resultNew.setReport(report);
        report.addResult(resultNew);

        // find items in second not in first -- these are REMOVED
        ReportResult resultRemoved =
            this.getReportResultItemsNotInResult(result2, result1);

        if (resultRemoved.getCt() > 0) {
          resultRemoved.setDateValue("");
          resultRemoved.setName(result1.getName());
          resultRemoved.setProjectName(result1.getProjectName());
          resultRemoved.setValue(result1.getValue() + " (Removed)");
          resultRemoved.setReport(report);
          report.addResult(resultRemoved);
        }
      }

      // if a data-point report
    } else {

      for (Object[] result : results) {
        String value = result[0].toString();
        String itemId = result[1].toString();
        String itemName = result[2].toString();

        // get report result (create if necessary)
        ReportResult reportResult = valueMap.get(value);
        if (reportResult == null) {
          reportResult = new ReportResultJpa();
          reportResult.setDateValue("");
          reportResult.setValue(value);
          reportResult.setName(reportDefinition.getName());
          reportResult.setProjectName(mapProject.getName());
          reportResult.setReport(report);
        }
        // construct the ReportResultItem
        ReportResultItem item = new ReportResultItemJpa();
        item.setResultType(report.getResultType());
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setReportResult(reportResult);

        // add the item to the list and update count
        reportResult.addReportResultItem(item);

        // update the value map
        valueMap.put(value, reportResult);
      }
    }

    // add each report result to the report
    for (ReportResult reportResult : valueMap.values()) {
      report.addResult(reportResult);
    }
    return report;

  }

  /**
   * Gets the report result for value.
   * 
   * @param report the report
   * @param value the value
   * @return the report result for value
   */
  private ReportResult getReportResultForValue(Report report, String value) {
    for (ReportResult result : report.getResults()) {
      if (result.getValue().equals(value))
        return result;
    }
    return null;
  }

  /**
   * Helper function Returns the result items in the first result that are not
   * present in the second result.
   * 
   * @param result1 the set to be checked for new items
   * @param result2 the older set against which items will be compared
   * @return the report result items added
   */
  private ReportResult getReportResultItemsNotInResult(ReportResult result1,
    ReportResult result2) {

    ReportResult result = new ReportResultJpa();

    // cycle over all result items in the first report
    if (result1 != null) {
      for (ReportResultItem item1 : result1.getReportResultItems()) {

        // if second result set does not contain this item, add it to result
        // list
        if (result2 != null && !result2.getReportResultItems().contains(item1)) {

          // construct a new item to ensure clean data structure
          ReportResultItem newItem = new ReportResultItemJpa();
          newItem.setItemId(item1.getItemId());
          newItem.setItemName(item1.getItemName());
          newItem.setReportResult(result);
          newItem.setResultType(item1.getResultType());
          result.addReportResultItem(newItem);
        }
      }
    }

    return result;
  }

  // /////////////////////////////////////////////////////
  // Query Handlers
  // /////////////////////////////////////////////////////
  /**
   * Execute sql query.
   *
   * @param query the query
   * @param nativeFlag the native flag
   * @return the result set
   * @throws Exception the exception
   */
  @SuppressWarnings({
    "unchecked"
  })
  private List<Object[]> executeQuery(String query, boolean nativeFlag)
    throws Exception {

    // check for sql query errors -- throw as local exception
    // this is used to propagate errors back to user when testing queries

    // ensure that query begins with SELECT (i.e. prevent injection
    // problems)
    if (!query.toUpperCase().startsWith("SELECT")) {
      throw new LocalException(
          "SQL Query has bad format:  does not begin with SELECT");
    }

    // check for multiple commands (i.e. multiple semi-colons)
    if (query.indexOf(";") != query.length() - 1 && query.endsWith(";")) {
      throw new LocalException(
          "SQL Query has bad format:  multiple commands detected");
    }

    // crude check: check for data manipulation commands
    if (query.toUpperCase().matches(
        "ALTER |CREATE |DROP |DELETE |INSERT |TRUNCATE |UPDATE ")) {
      throw new LocalException(
          "SQL Query has bad format:  data manipulation request detected");
    }

    // check for proper format for insertion into reports
    String selectSubStr =
        query.substring(0, query.toUpperCase().indexOf("FROM"));

    if (!selectSubStr.contains("itemId"))
      throw new LocalException(
          "Report query must return column result with name of 'itemId'");

    if (!selectSubStr.contains("itemName"))
      throw new LocalException(
          "Report query must return column result with name of 'itemName'");

    if (!selectSubStr.contains("value"))
      throw new LocalException(
          "Report query must return column result with name of 'value'");

    javax.persistence.Query jpaQuery = null;
    if (nativeFlag) {
      jpaQuery = manager.createNativeQuery(query);
    } else {
      jpaQuery = manager.createQuery(query);
    }
    return jpaQuery.getResultList();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#removeReportsForMapProject
   * (org.ihtsdo.otf.mapping.model.MapProject)
   */
  @Override
  public void removeReportsForMapProject(MapProject mapProject) {

    Logger.getLogger(getClass()).info(
        "Retrieving reports for project " + mapProject.getName() + "...");

    ReportList reports = getReportsForMapProject(mapProject, null);

    Logger.getLogger(getClass()).info(
        "Removing " + reports.getCount() + " reports.");

    for (Report report : reports.getReports()) {

      this.removeReport(report.getId());
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#
   * getReportResultItemsForReportResult(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameter)
   */
  @SuppressWarnings("unchecked")
  @Override
  public ReportResultItemList getReportResultItemsForReportResult(
    Long reportResultId, PfsParameter pfsParameter) {

    ReportResultItemList reportResultItemList = new ReportResultItemListJpa();

    javax.persistence.Query query =
        manager
            .createQuery(
                "select r from ReportResultItemJpa r where reportResult_id = :reportResultId")
            .setParameter("reportResultId", reportResultId);

    if (pfsParameter.getStartIndex() != -1
        && pfsParameter.getMaxResults() != -1) {
      query.setFirstResult(pfsParameter.getStartIndex());
      query.setMaxResults(pfsParameter.getMaxResults());
    }

    reportResultItemList.setReportResultItems(query.getResultList());

    return reportResultItemList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ReportService#getQACheckDefinitionsForRole
   * (org.ihtsdo.otf.mapping.helpers.MapUserRole)
   */
  @SuppressWarnings("unchecked")
  @Override
  public ReportDefinitionList getQACheckDefinitionsForRole(MapUserRole role) {
    ReportDefinitionList definitionList = new ReportDefinitionListJpa();

    // System.out.println("Getting report definitions for role: "
    // + role.toString());

    // get all definitions
    List<ReportDefinition> definitions =
        manager.createQuery(
            "select d from ReportDefinitionJpa d where isQACheck = true")
            .getResultList();

    // cycle over definitions and check if role has required privileges
    for (ReportDefinition definition : definitions) {
      if (role.hasPrivilegesOf(definition.getRoleRequired()))
        definitionList.addReportDefinition(definition);
    }

    // return the definition list
    return definitionList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getQACheckDefinitions()
   */
  @Override
  @SuppressWarnings("unchecked")
  public ReportDefinitionList getQACheckDefinitions() {

    List<ReportDefinition> qaCheckDefinitions = null;

    // construct query
    javax.persistence.Query query =
        manager
            .createQuery("select m from ReportDefinitionJpa m where isQACheck = true");

    qaCheckDefinitions = query.getResultList();

    ReportDefinitionListJpa qaCheckDefinitionList =
        new ReportDefinitionListJpa();
    qaCheckDefinitionList.setReportDefinitions(qaCheckDefinitions);
    qaCheckDefinitionList.setTotalCount(qaCheckDefinitions.size());
    return qaCheckDefinitionList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.ReportService#getQALabels()
   */
  @Override
  public SearchResultList getQALabels() {

    ReportDefinitionList definitions = getQACheckDefinitions();

    SearchResultList searchResultList = new SearchResultListJpa();
    for (ReportDefinition def : definitions.getReportDefinitions()) {
      searchResultList.addSearchResult(new SearchResultJpa(def.getId(), null,
          def.getName(), ""));
    }
    return searchResultList;

  }

}
