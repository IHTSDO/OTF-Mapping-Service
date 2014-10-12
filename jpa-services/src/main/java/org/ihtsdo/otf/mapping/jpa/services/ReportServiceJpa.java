package org.ihtsdo.otf.mapping.jpa.services;

import java.io.File;
import java.io.FileReader;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.NoResultException;

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
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportType;
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
import org.ihtsdo.otf.mapping.services.ReportService;

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

	@SuppressWarnings("unchecked")
	@Override
	public ReportDefinitionList getReportDefinitionsForRole(MapUserRole role) {
		ReportDefinitionList definitionList = new ReportDefinitionListJpa();

		System.out.println("Getting report definitions for role: "
				+ role.toString());

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

	@Override
	public ReportDefinition getReportDefinition(ReportType reportType) {
		ReportDefinition r = null;

		System.out.println(reportType.toString() + " " + reportType);

		javax.persistence.Query query = manager
				.createQuery("select r from ReportDefinitionJpa r where reportType = :reportType");
		query.setParameter("reportType", reportType);

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
	public void removeReportDefinition(Long reportDefinitionId) {

		ReportDefinition reportDefinition = manager.find(
				ReportDefinitionJpa.class, reportDefinitionId);

		// now remove the entry
		tx.begin();
		if (manager.contains(reportDefinition)) {
			manager.remove(reportDefinition);
		} else {
			manager.remove(manager.merge(reportDefinition));
		}
		tx.commit();

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ReportList getReportsForMapProject(MapProject mapProject,
			PfsParameter pfsParameter) {
		
		return getReportsForMapProjectAndReportType(mapProject, null, pfsParameter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ReportList getReportsForMapProjectAndReportType(MapProject mapProject,
			String reportType, PfsParameter pfsParameter) {
		
		// instantiate empty paging/filtering/sorting object if null
		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();

		ReportList reportList = new ReportListJpa();
		
		// TODO: This is temporary, move this to a QueryBuilder structure instead of using direct queries
		javax.persistence.Query query;
		
		// construct query based on whether reportType is specified
		// note that results are ordered by descending timestamp
		if (reportType != null) {
		
		query = manager
				.createQuery(
						"select r from ReportJpa r where mapProjectId = :mapProjectId and reportType = :reportType order by timestamp desc")
						.setParameter("reportType", ReportType.valueOf(reportType));
				
		} else {
			query = manager
					.createQuery(
							"select r from ReportJpa r where mapProjectId = :mapProjectId order by timestamp desc");
		}
		
		// add project parameter for both cases
		query.setParameter("mapProjectId", mapProject.getId());
		
		// execute the query to get the number of reports
		// TODO This will be folded into lucene query builder later
		reportList.setTotalCount(query.getResultList().size());
		
		// if paging requested, set result parameters
		if (pfsParameter.getStartIndex() != -1 && pfsParameter.getMaxResults() != -1) {
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

	@SuppressWarnings("unchecked")
	@Override
	public Report getLastReportForReportType(MapProject mapProject,
			String reportType) {

		Logger.getLogger(ReportServiceJpa.class).info(
				"Getting last report of type " + reportType
						+ " for map project " + mapProject.getName());

		try {

			List<Report> reports = manager
					.createQuery(
							"select r from ReportJpa r where mapProjectId = :mapProjectId and reportType = :reportType order by timestamp desc")
					.setParameter("reportType", ReportType.valueOf(reportType))
					.setParameter("mapProjectId", mapProject.getId())
					.getResultList();

			this.handleReportLazyInitialization(reports.get(0));

			return reports.get(0);
		} catch (NoResultException e) {
			return null;
		}

	}

	@Override
	public void generateReportsForDateRange(MapProject mapProject,
			MapUser mapUser, Date startDate, Date endDate) throws Exception {

		Calendar cal = Calendar.getInstance();

		// get all report definitions
		ReportDefinitionList reportDefinitions = this.getReportDefinitions();

		// cycle over dates until end date is passed
		while (startDate.compareTo(endDate) <= 0) {

			Logger.getLogger(ReportServiceJpa.class).info(
					"  Generating reports for " + startDate.toString());

			for (ReportDefinition reportDefinition : reportDefinitions
					.getIterable()) {

				this.generateReport(mapProject, mapUser,
				reportDefinition.getReportName(), reportDefinition,
				startDate, true);
			}

			// increment startDate by 1 day
			cal.setTime(startDate);
			cal.add(Calendar.DATE, 1);
			startDate = cal.getTime();
		}

	}

	@Override
	public void generateDailyReports(MapProject mapProject, MapUser mapUser,
			Date date) {

	}

	// /////////////////////////////////////////////////////
	// Report Generation Service
	// /////////////////////////////////////////////////////
	@Override
	public Report generateReport(MapProject mapProject, MapUser owner,
			String name, ReportDefinition reportDefinition, Date date,
			boolean autoGenerated) throws Exception {

		// for constructing older queries, unused for now
		if (date == null)
			date = new Date();

		// replace parameters in the query
		String query = reportDefinition.getQuery();

		query = query.replaceAll(":MAP_PROJECT_ID:", mapProject.getId()
				.toString());
		query = query.replaceAll(":TIMESTAMP:", Long.toString(date.getTime()));
		/*
		 * query = query.replaceAll("$\\{MAP_PROJECT_ID\\}", mapProject.getId()
		 * .toString()); query = query.replaceAll("$\\{TIMESTAMP\\}",
		 * Long.toString(date.getTime()));
		 */
		// instantiate the report
		Report report = new ReportJpa();
		report.setActive(true);

		report.setAutoGenerated(autoGenerated);
		report.setMapProjectId(mapProject.getId());
		report.setName(name);
		report.setOwner(owner);
		report.setQuery(query); // use the modified query (i.e. the actually
								// executed query)
		report.setQueryType(reportDefinition.getQueryType());
		report.setReportType(reportDefinition.getReportType());
		report.setResultType(reportDefinition.getResultType());

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

		while (resultSet.next()) {
			String value = resultSet.getString("value");
			String itemName = resultSet.getString("itemName");
			String itemId = resultSet.getString("itemId");

			// get report result (create if nessary)
			ReportResult reportResult = valueMap.get(value);
			if (reportResult == null) {
				reportResult = new ReportResultJpa();
				reportResult.setDateValue("");
				reportResult.setValue(value);
				reportResult.setName(reportDefinition.getReportName());
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

		// add each report result to the report
		for (ReportResult reportResult : valueMap.values()) {
			report.addResult(reportResult);
		}

		// persist the report
		report = this.addReport(report);

		return report;

	}

	// /////////////////////////////////////////////////////
	// Query Handlers
	// /////////////////////////////////////////////////////
	public ResultSet executeSqlQuery(String query) throws Exception {

		System.out.println("Executing query: ");
		System.out.println(query);

		// get the database parameters
		String configFileName = System.getProperty("run.config");
		Properties config = new Properties();
		FileReader in = new FileReader(new File(configFileName));
		config.load(in);
		in.close();

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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.ReportService#removeReportsForMapProject(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void removeReportsForMapProject(MapProject mapProject) {
		
		this.setTransactionPerOperation(true);
		
		Logger.getLogger(ReportServiceJpa.class).info("Retrieving reports for project " + mapProject.getName() + "...");
		
		ReportList reports = getReportsForMapProject(mapProject, null);
		
		Logger.getLogger(ReportServiceJpa.class).info("Removing " + reports.getCount() + " reports.");
		
		for (Report report : reports.getReports()) {
			
			Logger.getLogger(ReportServiceJpa.class).info("  Removing report with id " + report.getId());
			
			// remove all result items
			for (ReportResult result : report.getResults()) {
				
				Logger.getLogger(ReportServiceJpa.class).info("    Removing report result with id " + result.getId());
				
				// remove all report result items
				for (ReportResultItem item : result.getReportResultItems()) {
					
					Logger.getLogger(ReportServiceJpa.class).info("      Removing report result item with id " + item.getId());
					removeReportResultItem(item.getId());
				}
				removeReportResult(result.getId());
			}
			
			// remove all notes
			for (ReportNote note : report.getNotes()) {
				
				Logger.getLogger(ReportServiceJpa.class).info("    Removing report note with id " + note.getId());
				removeReportNote(note.getId());
			}
			// remove the report
			removeReport(report.getId());
			
		}
		
	}

}
