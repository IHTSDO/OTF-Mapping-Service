package org.ihtsdo.otf.mapping.jpa.services;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
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
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

// TODO: Auto-generated Javadoc
/**
 * The Class ReportServiceJpa.
 */
public class ReportServiceJpa extends RootServiceJpa implements ReportService {

	/**
	 * Instantiates a new report service jpa.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public ReportServiceJpa() throws Exception {
		super();
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
		javax.persistence.Query query = manager
				.createQuery("select m from ReportJpa m");

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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#findReportsForQuery(java
	 * .lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameter)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findReportsForQuery(String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		try {
			// construct luceneQuery based on URL format
			if (query.indexOf(':') == -1) { // no fields indicated
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
						Version.LUCENE_36, fieldNames.toArray(new String[0]),
						searchFactory.getAnalyzer(ReportJpa.class));
				queryParser.setAllowLeadingWildcard(false);
				luceneQuery = queryParser.parse(query);

			} else { // field:value
				QueryParser queryParser = new QueryParser(Version.LUCENE_36,
						"summary", searchFactory.getAnalyzer(ReportJpa.class));
				luceneQuery = queryParser.parse(query);
			}
		} catch (ParseException e) {
			throw new LocalException(
					"The specified search terms cannot be parsed.  Please check syntax and try again.");
		}

		List<Report> reports;

		reports = fullTextEntityManager.createFullTextQuery(luceneQuery,
				ReportJpa.class).getResultList();
		// if a parse exception, throw a local exception

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(reports.size()) + " reports retrieved");

		for (Report report : reports) {
			s.addSearchResult(new SearchResultJpa(report.getId(), null, report
					.getName()));
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

		javax.persistence.Query query = manager
				.createQuery("select r from ReportJpa r where id = :id");
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#addReport(org.ihtsdo.otf
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#updateReport(org.ihtsdo
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#removeReport(java.lang.
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

		javax.persistence.Query query = manager
				.createQuery("select r from ReportResultJpa r where id = :id");
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#updateReportResult(org.
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#removeReportResult(java
	 * .lang.Long)
	 */
	@Override
	public void removeReportResult(Long reportResultId) {

		ReportResult reportResult = manager.find(ReportResultJpa.class,
				reportResultId);

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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#getReportResultItem(java
	 * .lang.Long)
	 */
	@Override
	public ReportResultItem getReportResultItem(Long reportResultItemId) {
		ReportResultItem r = null;

		javax.persistence.Query query = manager
				.createQuery("select r from ReportResultItemJpa r where id = :id");
		query.setParameter("id", reportResultItemId);

		r = (ReportResultItem) query.getSingleResult();

		return r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#addReportResultItem(org
	 * .ihtsdo.otf.mapping.reports.ReportResultItem)
	 */
	@Override
	public ReportResultItem addReportResultItem(
			ReportResultItem reportResultItem) {
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#updateReportResultItem(
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#removeReportResultItem(
	 * java.lang.Long)
	 */
	@Override
	public void removeReportResultItem(Long reportResultItemId) {

		ReportResultItem reportResultItem = manager.find(
				ReportResultItemJpa.class, reportResultItemId);

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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#getReportNote(java.lang
	 * .Long)
	 */
	@Override
	public ReportNote getReportNote(Long reportNoteId) {
		ReportNote r = null;

		javax.persistence.Query query = manager
				.createQuery("select r from ReportNoteJpa r where id = :id");
		query.setParameter("id", reportNoteId);

		r = (ReportNote) query.getSingleResult();

		return r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#addReportNote(org.ihtsdo
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
	 * @param report
	 *            the report
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
		List<ReportDefinition> definitions = manager.createQuery(
				"select d from ReportDefinitionJpa d").getResultList();

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
		javax.persistence.Query query = manager
				.createQuery("select m from ReportDefinitionJpa m");

		ReportDefinitions = query.getResultList();

		ReportDefinitionListJpa ReportDefinitionList = new ReportDefinitionListJpa();
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

		javax.persistence.Query query = manager
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
	public ReportDefinition addReportDefinition(
			ReportDefinition reportDefinition) {
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
	public void removeReportDefinition(Long reportDefinitionId)
			throws Exception {

		ReportDefinition reportDefinition = manager.find(
				ReportDefinitionJpa.class, reportDefinitionId);

		// check if this definition is used by map projects
		MappingService mappingService = new MappingServiceJpa();
		MapProjectList mapProjects = mappingService.getMapProjects();
		mappingService.close();

		for (MapProject mapProject : mapProjects.getIterable()) {
			if (mapProject.getReportDefinitions().contains(reportDefinition)) {
				throw new LocalException(
						"Report definition is currently in use by project "
								+ mapProject.getName()
								+ " and cannot be deleted");
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#getReportsForMapProject
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

		Logger.getLogger(ReportServiceJpa.class).info(
				"Retrieving reports for project "
						+ mapProject.getName()
						+ (reportDefinition == null ? "" : " of type "
								+ reportDefinition.getName()));

		// instantiate empty paging/filtering/sorting object if null
		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();

		ReportList reportList = new ReportListJpa();

		// TODO: This is temporary, move this to a QueryBuilder structure
		// instead of using direct queries
		javax.persistence.Query query;

		// construct query based on whether reportType is specified
		// note that results are ordered by descending timestamp
		if (reportDefinition != null) {

			query = manager
					.createQuery(
							"select r from ReportJpa r where mapProjectId = :mapProjectId and reportDefinition_id = :reportDefinition_id order by timestamp desc")
					.setParameter("reportDefinition_id",
							reportDefinition.getId());
			;
		} else {
			query = manager
					.createQuery("select r from ReportJpa r where mapProjectId = :mapProjectId order by timestamp desc");
		}

		// add project parameter for both cases
		query.setParameter("mapProjectId", mapProject.getId());

		// execute the query to get the number of reports
		// TODO This will be folded into lucene query builder later
		reportList.setTotalCount(query.getResultList().size());

		// if paging requested, set result parameters
		if (pfsParameter.getStartIndex() != -1
				&& pfsParameter.getMaxResults() != -1) {
			query.setFirstResult(pfsParameter.getStartIndex());
			query.setMaxResults(pfsParameter.getMaxResults());
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
		for (ReportDefinition reportDefinition : reportDefinitions
				.getIterable()) {
			if (reportDefinition.isDiffReport() == true)
				diffReportDefinitions.add(reportDefinition);
			else
				dailyReportDefinitions.add(reportDefinition);
		}

		// cycle over dates until end date is passed
		while (startDate.compareTo(endDate) <= 0) {

			cal.setTime(startDate);

			Logger.getLogger(ReportServiceJpa.class).info(
					"  Generating reports for " + startDate.toString());

			// first, do the non-diff reports
			// note that these must be calculated prior to diff reports
			for (ReportDefinition reportDefinition : dailyReportDefinitions) {

				if (isDateToRunReport(reportDefinition, startDate)) {
					Logger.getLogger(ReportServiceJpa.class).info(
							"    Generating report "
									+ reportDefinition.getName());

					Report report = this.generateReport(mapProject, mapUser,
							reportDefinition.getName(), reportDefinition,
							startDate, true);

					Logger.getLogger(ReportServiceJpa.class).info(
							"     Persisting report.");

					// persist the report
					report = this.addReport(report);
				}
			}

			// second, do the diff reports
			// note that this requires the current daily report to be present
			// (i.e. calculated above)
			for (ReportDefinition reportDefinition : diffReportDefinitions) {

				if (isDateToRunReport(reportDefinition, startDate) == true) {

					Logger.getLogger(ReportServiceJpa.class).info(
							"    Generating report "
									+ reportDefinition.getName());

					Report report = this.generateReport(mapProject, mapUser,
							reportDefinition.getName(), reportDefinition,
							startDate, true);

					if (report != null) {
						Logger.getLogger(ReportServiceJpa.class).info(
								"     Persisting report " + report.toString());

						// persist the report
						report = this.addReport(report);
					} else {
						Logger.getLogger(ReportService.class).warn(
								"    Skipping report");
					}

				}

			}

			// increment startDate by 1 day
			cal.setTime(startDate);
			cal.add(Calendar.DATE, 1);
			startDate = cal.getTime();
		}

	}

	/**
	 * Helper function Given a report definition and a date, assesses whether
	 * this report should be run, based on definition frequency
	 * 
	 * @param reportDefinition
	 * @param date
	 * @return
	 * @throws Exception
	 */
	private boolean isDateToRunReport(ReportDefinition reportDefinition,
			Date date) throws Exception {

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		;

		switch (reportDefinition.getFrequency()) {
		case ANNUALLY:
			if (cal.get(Calendar.DAY_OF_YEAR) == 1)
				return true;
			break;
		case DAILY:
			return true;

		case MONTHLY:
			if (cal.get(Calendar.DAY_OF_MONTH) == 1)
				return true;
			break;
		case WEEKLY:
			if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
				return true;
			break;
		default:
			throw new Exception(
					"Report definition found with invalid time period.");
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.ReportService#generateDailyReports(org
	 * .ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
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

		Logger.getLogger(ReportServiceJpa.class).info(
				"Generating report " + name + " for report definition "
						+ reportDefinition.getName() + " for date "
						+ date.toString());

		// get the query to replace parameterized values
		String query = reportDefinition.getQuery();

		// instantiate the calendar object
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		query = query.replaceAll(":MAP_PROJECT_ID:", mapProject.getId()
				.toString());
		query = query.replaceAll(":TIMESTAMP:", Long.toString(date.getTime()));

		// if a diff report, need to construct a second timestamp corresponding
		// to earlier report
		if (reportDefinition.isDiffReport() == true) {

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
			query = query.replaceAll(":TIMESTAMP2:",
					Long.toString(cal.getTimeInMillis()));
		}

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
		report.setTimestamp(date.getTime());

		// execute the query
		ResultSet resultSet = null;
		switch (reportDefinition.getQueryType()) {
		case HQL:
			break;
		case LUCENE:
			break;
		case SQL:
			resultSet = this.executeSqlQuery(report.getQuery());
			break;
		default:
			break;

		}

		if (resultSet == null)
			throw new Exception("Failed to retrieve results for query");

		Map<String, ReportResult> valueMap = new HashMap<>();

		// if a difference report
		if (reportDefinition.isDiffReport() == true) {

			// get the ids corresponding to reports to be diffed
			try {
				resultSet.next();
				report.setReport1Id(new Long(resultSet.getString("itemId")));
				resultSet.next();
				report.setReport2Id(new Long(resultSet.getString("itemId")));
			} catch (SQLException e) {
				Logger.getLogger(ReportServiceJpa.class).warn(
						"Could not retrieve report ids for diff report");
				return null;
			}

			// if either report id is null, cannot construct report, return null
			if (report.getReport1Id() == null || report.getReport2Id() == null)
				return null;

			// get the two reports
			Report report1 = getReport(report.getReport1Id());
			Report report2 = getReport(report.getReport2Id());

			// cycle over first result and find matching values in the second
			// report
			for (ReportResult result1 : report1.getResults()) {

				ReportResult resultDiff = new ReportResultJpa();
				resultDiff.setReport(report);
				resultDiff.setValue(result1.getValue());

				boolean matchingValueFound = false;

				// cycle over second report to find the corresponding value (if
				// it exists)
				for (ReportResult result2 : report2.getResults()) {
					if (result1.getValue().equals(result2.getValue())) {
						resultDiff.setCt(result1.getCt() - result2.getCt());
						matchingValueFound = true;
					}
				}

				// if matching value was not found, this is a new item (i.e. no
				// diff)
				if (matchingValueFound == false) {
					resultDiff.setValue(result1.getValue());
					resultDiff.setCt(result1.getCt());
				}

				report.addResult(resultDiff);

			}

			// cycle over the second result and find any non-matching values
			// (i.e. removed values)
			for (ReportResult result2 : report2.getResults()) {

				boolean matchingValueFound = false;
				for (ReportResult result1 : report1.getResults()) {
					if (result1.getValue().equals(result2.getValue()))
						matchingValueFound = true;
				}

				if (matchingValueFound == false) {

					ReportResult resultDiff = new ReportResultJpa();
					resultDiff.setReport(report);
					resultDiff.setValue("Items removed since previous report");
					resultDiff.setCt(-result2.getCt());

					report.addResult(resultDiff);
				}
			}

			// if a data-point report
		} else {

			while (resultSet.next()) {
				String value = resultSet.getString("value");
				String itemName = resultSet.getString("itemName");
				String itemId = resultSet.getString("itemId");

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
				reportResult.setCt(reportResult.getReportResultItems().size());

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
	 * @param report
	 *            the report
	 * @param value
	 *            the value
	 * @return the report result for value
	 */
	public ReportResult getReportResultForValue(Report report, String value) {
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
	 * @param result1
	 *            the result1
	 * @param result2
	 *            the result2
	 * @return the report result items added
	 */
	public ReportResultItemList getReportResultItemsAdded(ReportResult result1,
			ReportResult result2) {

		ReportResultItemList newItems = new ReportResultItemListJpa();

		for (ReportResultItem item1 : result1.getReportResultItems()) {
			if (result2 == null
					|| !result2.getReportResultItems().contains(item1)) {
				ReportResultItem newItem = new ReportResultItemJpa();
				newItem.setItemId(item1.getItemId());
				newItem.setItemName(item1.getItemName());
				newItem.setResultType(item1.getResultType());
				newItems.addReportResultItem(newItem);
			}
		}

		return newItems;
	}

	// /////////////////////////////////////////////////////
	// Query Handlers
	// /////////////////////////////////////////////////////
	/**
	 * Execute sql query.
	 * 
	 * @param query
	 *            the query
	 * @return the result set
	 * @throws Exception
	 *             the exception
	 */
	public ResultSet executeSqlQuery(String query) throws Exception {

		// System.out.println("Executing query: ");
		// System.out.println(query);

		// get the database parameters
		Properties config = ConfigUtility.getConfigProperties();

		Properties connectionProps = new Properties();
		connectionProps.put("user",
				config.getProperty("javax.persistence.jdbc.user"));
		connectionProps.put("password",
				config.getProperty("javax.persistence.jdbc.password"));

		// open the JDBC connection
		java.sql.Connection conn = DriverManager.getConnection(
				config.getProperty("javax.persistence.jdbc.url"),
				connectionProps);

		java.sql.Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);

		return rs;
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

		Logger.getLogger(ReportServiceJpa.class).info(
				"Retrieving reports for project " + mapProject.getName()
						+ "...");

		ReportList reports = getReportsForMapProject(mapProject, null);

		Logger.getLogger(ReportServiceJpa.class).info(
				"Removing " + reports.getCount() + " reports.");

		for (Report report : reports.getReports()) {

			this.removeReport(report.getId());
		}

		/*
		 * Logger.getLogger(ReportServiceJpa.class).info(
		 * "  Removing report with id " + report.getId());
		 * 
		 * // remove all result items for (ReportResult result :
		 * report.getResults()) {
		 * 
		 * Logger.getLogger(ReportServiceJpa.class).info(
		 * "    Removing report result with id " + result.getId());
		 * 
		 * // remove all report result items for (ReportResultItem item :
		 * result.getReportResultItems()) {
		 * 
		 * Logger.getLogger(ReportServiceJpa.class).info(
		 * "      Removing report result item with id " + item.getId());
		 * removeReportResultItem(item.getId()); }
		 * 
		 * this.commit(); this.beginTransaction();
		 * result.setReportResultItems(null); updateReportResult(result);
		 * removeReportResult(result.getId()); }
		 * 
		 * // remove all notes for (ReportNote note : report.getNotes()) {
		 * 
		 * Logger.getLogger(ReportServiceJpa.class).info(
		 * "    Removing report note with id " + note.getId());
		 * removeReportNote(note.getId()); } report.setResults(null);
		 * report.setNotes(null); //removeReport(report.getId());
		 * 
		 * }
		 * 
		 * this.commit();
		 */
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

		javax.persistence.Query query = manager
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

}
