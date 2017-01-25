package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
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
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * JPA enabled implementation of {@link ReportService}.
 */
public class ReportServiceJpa extends RootServiceJpa implements ReportService {

  /**
   * Instantiates a new report service jpa.
   * 
   * @throws Exception the exception
   */
  public ReportServiceJpa() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
    }
  }

  // /////////////////////////////////////////////////////
  // CRUD Services for ReportJpa
  // /////////////////////////////////////////////////////

  /* see superclass */
  @Override
  @SuppressWarnings("unchecked")
  public ReportList getReports() {

    List<Report> Reports = null;

    // construct query
    final javax.persistence.Query query =
        manager.createQuery("select m from ReportJpa m");

    Reports = query.getResultList();

    // force instantiation of lazy collections
    for (final Report Report : Reports) {
      handleReportLazyInitialization(Report);
    }

    final ReportListJpa ReportList = new ReportListJpa();
    ReportList.setReports(Reports);
    ReportList.setTotalCount(Reports.size());
    return ReportList;
  }

  /* see superclass */
  @Override
  @SuppressWarnings("unchecked")
  public SearchResultList findReportsForQuery(String query,
    PfsParameter pfsParameter) throws Exception {

    final SearchResultList list = new SearchResultListJpa();

    final int[] totalCt = new int[1];
    final List<Report> reports =
        (List<Report>) this.getQueryResults(query, ReportJpa.class,
            ReportJpa.class, pfsParameter, totalCt);
    list.setTotalCount(totalCt[0]);

    for (final Report report : reports) {
      list.addSearchResult(new SearchResultJpa(report.getId(), null, report
          .getName(), ""));
    }

    return list;
  }

  /* see superclass */
  @Override
  public Report getReport(Long reportId) {
    Report r = null;

    final javax.persistence.Query query =
        manager.createQuery("select r from ReportJpa r where id = :id");
    query.setParameter("id", reportId);

    r = (Report) query.getSingleResult();

    // handle lazy initializations
    handleReportLazyInitialization(r);

    return r;
  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public ReportResult getReportResult(Long reportResultId) {
    ReportResult r = null;

    final javax.persistence.Query query =
        manager.createQuery("select r from ReportResultJpa r where id = :id");
    query.setParameter("id", reportResultId);

    r = (ReportResult) query.getSingleResult();

    return r;
  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public void removeReportResult(Long reportResultId) {

    final ReportResult reportResult =
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public ReportNote getReportNote(Long reportNoteId) {
    ReportNote r = null;

    final javax.persistence.Query query =
        manager.createQuery("select r from ReportNoteJpa r where id = :id");
    query.setParameter("id", reportNoteId);

    r = (ReportNote) query.getSingleResult();

    return r;
  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public void removeReportNote(Long reportNoteId) {

    final ReportNote reportNote =
        manager.find(ReportNoteJpa.class, reportNoteId);

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
  @SuppressWarnings("static-method")
  private void handleReportLazyInitialization(Report report) {
    report.getNotes().size();
    report.getResults().size();

    for (final ReportResult result : report.getResults()) {
      result.getReportResultItems().size();
    }

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ReportDefinitionList getReportDefinitionsForRole(MapUserRole role) {
    ReportDefinitionList definitionList = new ReportDefinitionListJpa();

    // get all definitions
    final List<ReportDefinition> definitions =
        manager.createQuery(
            "select d from ReportDefinitionJpa d where isQACheck = false")
            .getResultList();

    // cycle over definitions and check if role has required privileges
    for (final ReportDefinition definition : definitions) {
      if (role.hasPrivilegesOf(definition.getRoleRequired()))
        definitionList.addReportDefinition(definition);
    }

    // return the definition list
    return definitionList;
  }

  /* see superclass */
  @Override
  @SuppressWarnings("unchecked")
  public ReportDefinitionList getReportDefinitions() {

    List<ReportDefinition> reportDefinitions = null;

    // construct query
    final javax.persistence.Query query =
        manager
            .createQuery("select m from ReportDefinitionJpa m where isQACheck = false");

    reportDefinitions = query.getResultList();

    final ReportDefinitionListJpa ReportDefinitionList =
        new ReportDefinitionListJpa();
    ReportDefinitionList.setReportDefinitions(reportDefinitions);
    ReportDefinitionList.setTotalCount(reportDefinitions.size());
    return ReportDefinitionList;
  }

  /* see superclass */
  @Override
  public ReportDefinition getReportDefinition(Long reportDefinitionId) {
    ReportDefinition r = null;

    final javax.persistence.Query query =
        manager
            .createQuery("select r from ReportDefinitionJpa r where id = :id");
    query.setParameter("id", reportDefinitionId);

    r = (ReportDefinition) query.getSingleResult();

    return r;
  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public void removeReportDefinition(Long reportDefinitionId) throws Exception {

    final ReportDefinition reportDefinition =
        manager.find(ReportDefinitionJpa.class, reportDefinitionId);

    // check if this definition is used by map projects
    final MappingService mappingService = new MappingServiceJpa();
    try {
      final MapProjectList mapProjects = mappingService.getMapProjects();

      for (final MapProject mapProject : mapProjects.getIterable()) {
        if (mapProject.getReportDefinitions().contains(reportDefinition)) {
          throw new LocalException(
              "Report definition is currently in use by project "
                  + mapProject.getName() + " and cannot be deleted");
        }
      }
    } catch (Exception e) {
      throw e;
    } finally {
      mappingService.close();
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

  /* see superclass */
  @Override
  public ReportList getReportsForMapProject(MapProject mapProject,
    PfsParameter pfsParameter) {

    return getReportsForMapProjectAndReportDefinition(mapProject, null,
        pfsParameter);
  }

  /* see superclass */
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

    final ReportList reportList = new ReportListJpa();

    // TODO: This is temporary, move this to a QueryBuilder structure
    // instead of using direct queries
    final javax.persistence.Query query;

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
                  + "and r.reportDefinition = d " + "and d.isQACheck = false "
                  + "order by timestamp desc");
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
    final List<Report> reports = query.getResultList();

    // set report results to null
    for (final Report report : reports) {
      report.setResults(null);
    }

    reportList.setReports(reports);

    Logger.getLogger(getClass()).info("  count = " + reportList.getCount());

    return reportList;
  }

  /* see superclass */
  @Override
  public void generateReportsForDateRange(MapProject mapProject,
    MapUser mapUser, Date startDate, Date endDate) throws Exception {

    Calendar cal = Calendar.getInstance();

    // get all report definitions
    final Set<ReportDefinition> reportDefinitions =
        mapProject.getReportDefinitions();

    // separate report definitions into daily and diff sets
    // note that this is necessary as diff reports require
    // a daily report to be present prior to calculation
    final List<ReportDefinition> nonDiffReportDefinitions = new ArrayList<>();
    final List<ReportDefinition> diffReportDefinitions = new ArrayList<>();

    // sort the report definitions into daily and diff sets
    for (final ReportDefinition reportDefinition : reportDefinitions) {

      // if diff report, add to diff set
      if (reportDefinition.isDiffReport()) {
        diffReportDefinitions.add(reportDefinition);
      }
      // otherwise, if not a qa check, add to the normal reports
      else if (!reportDefinition.isQACheck()) {
        nonDiffReportDefinitions.add(reportDefinition);
      }
    }

    // handle reports in sort order
    Comparator<ReportDefinition> comp = new Comparator<ReportDefinition>() {
      @Override
      public int compare(ReportDefinition o1, ReportDefinition o2) {
        return o1.getName().compareTo(o2.getName());
      }
    };
    Collections.sort(nonDiffReportDefinitions, comp);
    Collections.sort(diffReportDefinitions, comp);

    // cycle over dates until end date is passed
    Date localStartDate = startDate;
    while (localStartDate.compareTo(endDate) <= 0) {

      cal.setTime(localStartDate);

      Logger.getLogger(getClass()).info(
          "  Generating reports for " + localStartDate);

      // first, do the non-diff reports
      // note that these must be calculated prior to diff reports
      for (final ReportDefinition reportDefinition : nonDiffReportDefinitions) {

        if (isDateToRunReport(reportDefinition, localStartDate)) {

          // Only generate reports for those that have queries
          if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
            Logger.getLogger(getClass()).info(
                "    Generating report " + reportDefinition.getName());

            final Report report =
                generateReport(mapProject, mapUser, reportDefinition.getName(),
                    reportDefinition, localStartDate, true);

            // If generateReport returns a non-null result (no errors), add
            if (report != null) {
              Logger.getLogger(getClass()).info("     Persisting report.");
              addReport(report);
            }
          }
        }
      }

      // second, do the diff reports
      // note that this requires the current daily report to be present
      // (i.e. calculated above)
      for (final ReportDefinition reportDefinition : diffReportDefinitions) {

        if (isDateToRunReport(reportDefinition, localStartDate)) {

          Logger.getLogger(getClass()).info(
              "    Generating report " + reportDefinition.getName());
          Report report;
          try {

            report =
                generateReport(mapProject, mapUser, reportDefinition.getName(),
                    reportDefinition, localStartDate, true);

            if (report != null) {
              Logger.getLogger(getClass()).info(
                  "     Persisting report " + report.toString());

              // persist the report
              report = addReport(report);
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
   * report should be run, based on definition frequency.
   *
   * @param reportDefinition the report definition
   * @param date the date
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private boolean isDateToRunReport(ReportDefinition reportDefinition, Date date)
    throws Exception {

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    switch (reportDefinition.getFrequency()) {
      case DAILY:
      case ON_DEMAND:
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

  /* see superclass */
  @Override
  public void generateDailyReports(MapProject mapProject, MapUser mapUser)
    throws Exception {

    // get today's date
    Date date = new Date();

    // call date range report generation with start and end date as today
    generateReportsForDateRange(mapProject, mapUser, date, date);

  }

  // /////////////////////////////////////////////////////
  // Report Generation Service
  // /////////////////////////////////////////////////////

  /* see superclass */
  @Override
  public Report generateReport(MapProject mapProject, MapUser owner,
    String name, ReportDefinition reportDefinition, Date date,
    boolean autoGenerated) throws Exception {

    Logger.getLogger(getClass()).info(
        "Generating report " + name + " for report definition "
            + reportDefinition.getName() + " for date " + date.toString());

    // instantiate the calendar object
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);

    // get the query to replace parameterized values
    String query = reportDefinition.getQuery();

    // if a diff report, need to construct a query based on specified report
    // definition
    if (reportDefinition.isDiffReport()) {

      query =
      // most recent report before or on specified date
          "select 'Report' value, name itemName, id itemId "
              + "from reports where name = '"
              + reportDefinition.getDiffReportDefinitionName()
              + "' "
              + "and mapProjectId = :MAP_PROJECT_ID: "
              + "and timestamp = "
              + "(select max(timestamp) from reports where timestamp <= :TIMESTAMP:"
              + " and mapProjectId = :MAP_PROJECT_ID: and name = '"
              + reportDefinition.getDiffReportDefinitionName()
              + "') "
              + "limit 1 "

              // union with most recent report before or on specified date less
              // interval
              // note: surround with parantheses to correctly apply LIMIT
              + "UNION "
              + "(select 'Report' value, name itemName, id itemId "
              + "from reports where name = '"
              + reportDefinition.getDiffReportDefinitionName()
              + "' "
              + "and mapProjectId = :MAP_PROJECT_ID: "
              + "and timestamp = "
              + "(select max(timestamp) from reports where timestamp <= :TIMESTAMP2:"
              + " and mapProjectId = :MAP_PROJECT_ID: and name = '"
              + reportDefinition.getDiffReportDefinitionName() + "') "
              + "limit 1)";

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
      // only used for diff report
      query =
          query
              .replaceAll(":TIMESTAMP2:", Long.toString(cal.getTimeInMillis()));

      // Force an SQL type because we are actually constructing a query.
      reportDefinition.setQueryType(ReportQueryType.SQL);
    }

    // replace the map project id and timestamp parameters
    query = query.replaceAll(":MAP_PROJECT_ID:", mapProject.getId().toString());
    query =
        query.replaceAll(":SOURCE_TERMINOLOGY:",
            mapProject.getSourceTerminology());
    query =
        query.replaceAll(":SOURCE_TERMINOLOGY_VERSION:",
            mapProject.getSourceTerminologyVersion());
    query =
        query.replaceAll(":DESTINATION_TERMINOLOGY:",
            mapProject.getDestinationTerminology());
    query =
        query.replaceAll(":DESTINATION_TERMINOLOGY_VERSION:",
            mapProject.getDestinationTerminologyVersion());
    query =
        query.replaceAll(":EDITING_CYCLE_BEGIN_DATE:",
            Long.toString(mapProject.getEditingCycleBeginDate().getTime()));
    query =
        query.replaceAll(":LATEST_PUBLICATION_DATE:",
            Long.toString(mapProject.getLatestPublicationDate().getTime()));
    query = query.replaceAll(":TIMESTAMP:", Long.toString(date.getTime()));

    // Handle previous versions
    if (query.contains(":PREVIOUS")) {
      final MetadataService service = new MetadataServiceJpa();
      try {
        String prevSourceVersion =
            service.getPreviousVersion(mapProject.getSourceTerminology());
        if (prevSourceVersion == null) {
          prevSourceVersion = "";
        }
        String prevDestVersion =
            service.getPreviousVersion(mapProject.getDestinationTerminology());
        if (prevDestVersion == null) {
          prevDestVersion = "";
        }
        query =
            query.replaceAll(":PREVIOUS_SOURCE_TERMINOLOGY_VERSION:",
                prevSourceVersion);
        query =
            query.replaceAll(":PREVIOUS_DESTINATION_TERMINOLOGY_VERSION:",
                prevDestVersion);
      } catch (Exception e) {
        throw e;
      } finally {
        service.close();
      }
    }

    // instantiate the report
    Report report = new ReportJpa();
    report.setReportDefinition(reportDefinition);
    report.setActive(true);

    report.setAutoGenerated(autoGenerated);
    report.setDiffReport(reportDefinition.isDiffReport());
    report.setMapProjectId(mapProject.getId());
    report.setName(name);
    report.setOwner(owner);
    report.setQuery(query); // use the modified query (i.e. the actually
    // executed query)
    report.setQueryType(reportDefinition.getQueryType());
    report.setResultType(reportDefinition.getResultType());
    report.setTimestamp(date.getTime());

    // execute the query
    List<Object[]> results = null;
    switch (reportDefinition.getQueryType()) {
      case HQL:
        try {
          results = executeQuery(report.getQuery(), false);
        } catch (java.lang.IllegalArgumentException e) {
          throw new LocalException("Error executing HQL query: "
              + e.getMessage());
        }
        break;
      case LUCENE:
        // query map records index which returns map objects
        // value = "", itemId = mapRecord.getId(),
        // itemName=mapRecord.getConceptName()
        break;
      case SQL:
        try {
          results = executeQuery(report.getQuery(), true);
        } catch (javax.persistence.PersistenceException e) {
          throw new LocalException("Error executing SQL query:  "
              + e.getMessage());
        } catch (java.lang.IllegalArgumentException e) {
          throw new LocalException(
              "Error executing SQL query, possible invalid parameters (valid parameters are :MAP_PROJECT_ID:, :TIMESTAMP:):  "
                  + e.getMessage());
        }
        break;
      case NONE:
        return null;
      default:
        break;

    }

    if (results == null)
      throw new Exception("Failed to retrieve results for query");

    final Map<String, ReportResult> valueMap = new HashMap<>();

    // if a difference report
    if (reportDefinition.isDiffReport()) {

      if (results.size() != 2) {
        throw new LocalException(
            "Could not construct diff report for query, unexpected number of results (expected 2, found "
                + results.size() + ") from query " + query);
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
        throw new LocalException(
            "Could not construct diff report for query, a null report was returned from query "
                + query);
      }

      // get the two reports
      Report report1;
      try {
        report1 = getReport(report.getReport1Id());
      } catch (Exception e) {
        throw new LocalException(
            "Could not retrieve first report for diff report with id "
                + report.getReport1Id(), e);
      }

      Report report2;
      try {
        report2 = getReport(report.getReport2Id());
      } catch (Exception e) {
        throw new LocalException(
            "Could not retrieve second report for diff report with id "
                + report.getReport2Id(), e);
      }

      // cycle over results in first report
      for (final ReportResult result1 : report1.getResults()) {

        // check if an result with this value exists
        final ReportResult result2 =
            getReportResultForValue(report2, result1.getValue());

        // find items in first not in second -- these are NEW
        final ReportResult resultNew =
            getReportResultItemsNotInResult(result1, result2);

        resultNew.setDateValue("");
        resultNew.setName(result1.getName());
        resultNew.setProjectName(result1.getProjectName());
        resultNew.setValue(result1.getValue());
        resultNew.setReport(report);
        report.addResult(resultNew);

        // find items in second not in first -- these are REMOVED
        final ReportResult resultRemoved =
            getReportResultItemsNotInResult(result2, result1);

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

      for (final Object[] result : results) {
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
        final ReportResultItem item = new ReportResultItemJpa();
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
    for (final ReportResult reportResult : valueMap.values()) {
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
  @SuppressWarnings("static-method")
  private ReportResult getReportResultForValue(Report report, String value) {
    for (final ReportResult result : report.getResults()) {
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
  @SuppressWarnings("static-method")
  private ReportResult getReportResultItemsNotInResult(ReportResult result1,
    ReportResult result2) {

    ReportResult result = new ReportResultJpa();

    // cycle over all result items in the first report
    if (result1 != null) {
      for (final ReportResultItem item1 : result1.getReportResultItems()) {

        // if second result set does not contain this item, add it to result
        // list
        if (result2 != null && !result2.getReportResultItems().contains(item1)) {

          // construct a new item to ensure clean data structure
          final ReportResultItem newItem = new ReportResultItemJpa();
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

    if (query.toUpperCase().indexOf("FROM") == -1)
      throw new LocalException("Report query must contain the term FROM");

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

  /* see superclass */
  @Override
  public void removeReportsForMapProject(MapProject mapProject, Date startDate,
    Date endDate) {

    ReportList reports = getReportsForMapProject(mapProject, null);
    Date start = new Date(0);
    Date end = new Date();
    if (startDate != null) {
      start = startDate;
    }
    if (endDate != null) {
      end = endDate;
    }
    Logger.getLogger(getClass()).info(
        "Removing reports for project " + mapProject.getName() + ", "
            + mapProject.getId() + " - " + start + " to " + end);
    for (final Report report : reports.getReports()) {
      if (report.getTimestamp() >= start.getTime()
          && report.getTimestamp() <= end.getTime()) {
        Logger.getLogger(getClass())
            .info("  Remove report - " + report.getId());
        removeReport(report.getId());
      }
    }

  }

  /* see superclass */
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

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ReportDefinitionList getQACheckDefinitionsForRole(MapUserRole role) {
    ReportDefinitionList definitionList = new ReportDefinitionListJpa();

    // get all definitions
    List<ReportDefinition> definitions =
        manager.createQuery(
            "select d from ReportDefinitionJpa d where isQACheck = true")
            .getResultList();

    // cycle over definitions and check if role has required privileges
    for (final ReportDefinition definition : definitions) {
      if (role.hasPrivilegesOf(definition.getRoleRequired()))
        definitionList.addReportDefinition(definition);
    }

    // return the definition list
    return definitionList;
  }

  /* see superclass */
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

  /* see superclass */
  @Override
  public SearchResultList getQALabels(Long mapProjectId) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Reoprt Service - get QA Labels - " + mapProjectId);

    // Look up distinct label values used by QA records.
    final javax.persistence.Query query =
        manager
            .createQuery("select distinct l from MapRecordJpa m JOIN m.labels l "
                + "WHERE m.mapProjectId = :mapProjectId "
                + "AND m.workflowStatus = 'QA_NEEDED'");
    // TODO: may need to remove where not al
    try {
      query.setParameter("mapProjectId", mapProjectId);
      @SuppressWarnings("unchecked")
      final List<String> labels = query.getResultList();
      // Sort the labels
      Collections.sort(labels);
      final SearchResultList results = new SearchResultListJpa();
      for (final String label : labels) {
        final SearchResult result = new SearchResultJpa();
        result.setValue(label);
        results.addSearchResult(result);
      }

      return results;
    } catch (NoResultException e) {
      return new SearchResultListJpa();
    }

  }

}
