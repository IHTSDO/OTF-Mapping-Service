package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Comparator;
import java.util.List;

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
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportListJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.reports.Report;
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

	}

}
