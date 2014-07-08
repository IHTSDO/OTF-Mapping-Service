package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;
import org.hibernate.CacheMode;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
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
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Default workflow service implementation.
 */
public class WorkflowServiceJpa extends RootServiceJpa implements
		WorkflowService {

	/** The manager. */
	private EntityManager manager;

	/** The transaction per operation. */
	private boolean transactionPerOperation = true;

	/** The transaction entity. */
	private EntityTransaction tx;

	/**
	 * Instantiates an empty {@link WorkflowServiceJpa}.
	 * 
	 * @throws Exception
	 */

	public WorkflowServiceJpa() throws Exception {
		super();

		// created on each instantiation
		manager = factory.createEntityManager();

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
			TrackingRecord ma = manager.find(TrackingRecordJpa.class,
					trackingRecordId);

			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			TrackingRecord ma = manager.find(TrackingRecordJpa.class,
					trackingRecordId);
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
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#getTrackingRecords
	 * ()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecords() throws Exception {

		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();

		trackingRecordList.setTrackingRecords(manager.createQuery(
				"select tr from TrackingRecordJpa tr").getResultList());

		return trackingRecordList;
	}

	@Override
	public TrackingRecord getTrackingRecordForMapProjectAndConcept(
			MapProject mapProject, Concept concept) {

		try {
			return (TrackingRecord) manager
					.createQuery(
							"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminology = :terminology and terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
					.setParameter("mapProjectId", mapProject.getId())
					.setParameter("terminology", concept.getTerminology())
					.setParameter("terminologyVersion",
							concept.getTerminologyVersion())
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
	 * getTrackingRecordsForMapProject (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TrackingRecordList getTrackingRecordsForMapProject(
			MapProject mapProject) throws Exception {

		System.out.println("Getting tracking records at "
				+ (System.currentTimeMillis() / 1000 - Math.floor(System
						.currentTimeMillis() / 100000) * 100));

		TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
		trackingRecordList
				.setTrackingRecords(manager
						.createQuery(
								"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId")
						.setParameter("mapProjectId", mapProject.getId())
						.getResultList());

		System.out.println("Finished getting tracking records at "
				+ (System.currentTimeMillis() / 1000 - Math.floor(System
						.currentTimeMillis() / 100000) * 100));

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
	public TrackingRecord getTrackingRecord(MapProject mapProject,
			Concept concept) throws Exception {

		javax.persistence.Query query = manager
				.createQuery(
						"select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId and terminologyId = :terminologyId")
				.setParameter("mapProjectId", mapProject.getId())
				.setParameter("terminologyId", concept.getTerminologyId());

		return (TrackingRecord) query.getSingleResult();
	}

	private static String constructTrackingRecordForMapProjectIdQuery(
			Long mapProjectId, String query) {

		String full_query;

		// if no filter supplied, return query based on map project id only
		if (query == null || query.equals("") || query.equals("null")) {
			full_query = "mapProjectId:" + mapProjectId;
			return full_query;
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

		String queryStr_mod = query;
		queryStr_mod = queryStr_mod.replace("(", " ( ");
		queryStr_mod = queryStr_mod.replace(")", " ) ");
		queryStr_mod = queryStr_mod.replace("\"", " \" ");
		queryStr_mod = queryStr_mod.replace("+", " + ");
		queryStr_mod = queryStr_mod.replace("-", " - ");

		// remove any leading or trailing whitespace (otherwise first/last null
		// term
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

					// special case check: fielded term. Impossible for first
					// term to be
					// fielded.
					if (parsedTerms.size() == 0) {
						parsedTerms.add("\"" + currentTerm + "\"");
					} else {
						String lastParsedTerm = parsedTerms.get(parsedTerms
								.size() - 1);

						// if last parsed term ended with a colon, append this
						// term to the
						// last parsed term
						if (lastParsedTerm.endsWith(":") == true) {
							parsedTerms.set(parsedTerms.size() - 1,
									lastParsedTerm + "\"" + currentTerm + "\"");
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
					currentTerm = currentTerm == "" ? terms[i] : currentTerm
							+ " " + terms[i];

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
			 * full_query += (i == 0 ? // check for first term "" : // -> if
			 * first character, add nothing
			 * parsedTerms.get(i-1).matches(escapeTerms) ? // check if last term
			 * was an escape character "": // -> if last term was an escape
			 * character, add nothing " "); // -> otherwise, add a separating
			 * space
			 */

			// if an escape character/sequence, add this term unmodified
			if (parsedTerms.get(i).matches(escapeTerms)) {

				full_query += parsedTerms.get(i);

				// else if a boolean character, add this term in upper-case form
				// (i.e.
				// lucene format)
			} else if (parsedTerms.get(i).matches(booleanTerms)) {

				full_query += parsedTerms.get(i).toUpperCase();

				// else if already a field-specific query term, add this term
				// unmodified
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
		full_query = "(" + full_query + ")" + " AND mapProjectId:"
				+ mapProjectId;

		Logger.getLogger(MappingServiceJpa.class).debug(
				"Full query: " + full_query);

		return full_query;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableWork(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAvailableWork with query: '"
				+ query + "'");

		SearchResultList availableWork = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAvailableWork
		// - must be NON_LEGACY PATH
		// - any tracking record with no assigned users is by definition
		// available
		// - any tracking record with one assigned user on NON_LEGACY_PATH with
		// workflowstatus NEW, EDITING_IN_PROGRESS, or EDITING_DONE. Assigned
		// user must not be this user
		full_query += " AND workflowPath:NON_LEGACY_PATH";
		full_query += " AND (assignedUserCount:0 OR "
				+ "(assignedUserCount:1 AND NOT assignedUserNames:" + mapUser.getUserName() + "))";
			

		System.out.println("FindAvailableWork query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableConflicts
	 * (org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableConflicts(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAvailableWork with query: '"
				+ query + "'");

		SearchResultList availableConflicts = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAvailableConflicts
		// - user and workflowStatus pair of CONFLICT_DETECTED~userName exists
		// - user and workflowStatus pairs of CONFLICT_NEW/CONFLICT_IN_PROGRESS~userName does not exist
		full_query += " AND userAndWorkflowStatusPairs:CONFLICT_DETECTED_*";
		full_query += " AND NOT (userAndWorkflowStatusPairs:CONFLICT_NEW_* OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_*)";

		System.out.println("FindAvailableConflicts query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
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
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAvailableReviewWork
	 * (org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAvailableReviewWork(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAvailableReviewWork with query: '"
				+ query + "'");

		SearchResultList availableReviewWork = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAvailableReviewWork
		// - a user (any) and workflowStatus pair of REVIEW_NEEDED~userName exists
		// - the REVIEW_NEEDED pair is not for this user (i.e. user can't review their own work)
		// - user and workflowStatus pairs of CONFLICT_NEW/CONFLICT_IN_PROGRESS~userName does not exist
		full_query += " AND userAndWorkflowStatusPairs:REVIEW_NEEDED_*"
					+ " AND NOT userAndWorkflowStatusPairs:REVIEW_NEEDED_" + mapUser.getUserName()
				   	+ " AND NOT (userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName() 
				   	+ " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName() + ")";
		
		System.out.println("FindAvailableReviewWork query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
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

	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedWork(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAssignedWork with query: '" + query
				+ "'");

		SearchResultList assignedWork = new SearchResultListJpa();

		// create a blank pfs parameter object if one not passed in
		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();
		
		// create a blank query restriction if none provided
		if (pfsParameter.getQueryRestriction() == null)
			pfsParameter.setQueryRestriction("");

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAssignedWork
		// - user and workflowStatus must exist in a pair of form:
		//	 workflowStatus~userName, e.g. NEW~dmo or EDITING_IN_PROGRESS~kli
		// - modify search term based on pfs parameter query restriction field
		//   * default: NEW, EDITING_IN_PROGRESS, EDITING_DONE
		//   * NEW:  NEW
		//   * EDITED:  EDITING_IN_PROGRESS, EDITING_DONE
		
		// add terms based on query restriction
		switch (pfsParameter.getQueryRestriction()) {
		case "NEW":
			full_query += " AND userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName();
			break;
		case "EDITING_IN_PROGRESS":
			full_query += " AND userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName();
			break;
		case "EDITING_DONE":
			full_query +=" AND userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName();
			break;
		default:
			full_query += " AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
			+ " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName()
			+ " OR userAndWorkflowStatusPairs:EDITING_DONE_" + mapUser.getUserName() + ")";
			break;
		}

		
		/*
		
		full_query += " AND (userAndWorkflowStatusPairs:NEW_" + mapUser.getUserName()
				+ " OR userAndWorkflowStatusPairs:EDITING_IN_PROGRESS_" + mapUser.getUserName() + ")";
				*/

		System.out.println("FindAssignedWork query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

		assignedWork.setTotalCount(ftquery.getResultSize());

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
			} else {
				throw new Exception(
						"Concept query specified a field that does not exist or is not a string");
			}
		}

		List<TrackingRecord> results = ftquery.getResultList();
		MappingService mappingService = new MappingServiceJpa();
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (Long mapRecordId : tr.getMapRecordIds()) {

				MapRecord mr = mappingService.getMapRecord(mapRecordId);
				if (mr.getOwner().equals(mapUser))
					mapRecord = mr;
			}

			if (mapRecord == null) {
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
			assignedWork.addSearchResult(result);
		}
		mappingService.close();
		return assignedWork;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getAssignedConflicts(
	 * org.ihtsdo.otf.mapping.model.MapProject,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedConflicts(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAssignedConflicts with query: '"
				+ query + "'");

		SearchResultList assignedConflicts = new SearchResultListJpa();

		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();
		
		// create a blank query restriction if none provided
				if (pfsParameter.getQueryRestriction() == null)
					pfsParameter.setQueryRestriction("");

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAssignedConflicts
		// - workflow status CONFLICT_NEW or CONFLICT_IN_PROGRESS with this user name in pair

		// add terms based on query restriction
		switch (pfsParameter.getQueryRestriction()) {
		case "CONFLICT_NEW":
			full_query += " AND userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName();
			break;
		case "CONFLICT_IN_PROGRESS":
			full_query += " AND userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName();
			break;
		default:
			full_query += " AND (userAndWorkflowStatusPairs:CONFLICT_NEW_" + mapUser.getUserName()
			+ " OR userAndWorkflowStatusPairs:CONFLICT_IN_PROGRESS_" + mapUser.getUserName() + ")";
			break;
		}
		System.out.println("FindAssignedConflict query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

		assignedConflicts.setTotalCount(ftquery.getResultSize());

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
			} else {
				throw new Exception(
						"Concept query specified a field that does not exist or is not a string");
			}
		}

		List<TrackingRecord> results = ftquery.getResultList();
		MappingService mappingService = new MappingServiceJpa();
		for (TrackingRecord tr : results) {
			System.out.println("Assigned conflict: " + tr.toString());
			SearchResult result = new SearchResultJpa();

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (Long mapRecordId : tr.getMapRecordIds()) {

				MapRecord mr = mappingService.getMapRecord(mapRecordId);
				if (mr.getOwner().equals(mapUser))
					mapRecord = mr;
			}

			if (mapRecord == null) {
				throw new Exception(
						"Failed to retrieve assigned conflicts:  no map record found for user "
								+ mapUser.getUserName() + " and concept "
								+ tr.getTerminologyId());
			}
			result.setTerminologyId(mapRecord.getConceptId());
			result.setValue(mapRecord.getConceptName());
			result.setTerminology(mapRecord.getLastModified().toString());
			result.setTerminologyVersion(mapRecord.getWorkflowStatus().toString());
			result.setId(mapRecord.getId());
			assignedConflicts.addSearchResult(result);
		}
		mappingService.close();
		return assignedConflicts;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findAssignedReviewWork(MapProject mapProject,
			MapUser mapUser, String query, PfsParameter pfsParameter)
			throws Exception {

		System.out.println("Testing new findAssignedReviewWork with query: '" + query
				+ "'");

		SearchResultList assignedReviewWork = new SearchResultListJpa();

		// create a blank pfs parameter object if one not passed in
		if (pfsParameter == null)
			pfsParameter = new PfsParameterJpa();
		
		// create a blank query restriction if none provided
		if (pfsParameter.getQueryRestriction() == null)
			pfsParameter.setQueryRestriction("");

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct basic query
		String full_query = constructTrackingRecordForMapProjectIdQuery(
				mapProject.getId(), query);

		// add the query terms specific to findAssignedReviewWork
		// - user and workflow status must exist in the form REVIEW_NEW_userName or REVIEW_IN_PROGRESS_userName
		
		// add terms based on query restriction
		switch (pfsParameter.getQueryRestriction()) {
		case "NEW":
			full_query += " AND userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName();
					
			break;
		case "EDITING_IN_PROGRESS":
			full_query += " AND userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName();
			break;
		default:
			full_query += " AND (userAndWorkflowStatusPairs:REVIEW_NEW_" + mapUser.getUserName()
			+ " OR userAndWorkflowStatusPairs:REVIEW_IN_PROGRESS_" + mapUser.getUserName() + ")";
			break;
		}

		System.out.println("FindAssignedReviewWork query: " + full_query);

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(TrackingRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, TrackingRecordJpa.class);

		assignedReviewWork.setTotalCount(ftquery.getResultSize());

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
			if (TrackingRecordJpa.class
					.getDeclaredField(pfsParameter.getSortField()).getType()
					.equals(String.class)) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
			} else {
				throw new Exception(
						"Concept query specified a field that does not exist or is not a string");
			}
		}

		List<TrackingRecord> results = ftquery.getResultList();
		MappingService mappingService = new MappingServiceJpa();
		for (TrackingRecord tr : results) {
			SearchResult result = new SearchResultJpa();

			// get the map record assigned to this user
			MapRecord mapRecord = null;
			for (Long mapRecordId : tr.getMapRecordIds()) {

				MapRecord mr = mappingService.getMapRecord(mapRecordId);
				if (mr.getOwner().equals(mapUser))
					mapRecord = mr;
			}

			if (mapRecord == null) {
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

	/**
	 * Perform workflow actions based on a specified action.
	 * ASSIGN_FROM_INITIAL_RECORD is the only routine that requires a map record
	 * to be passed in All other cases that all required mapping information
	 * (e.g. map records) be current in the database (i.e. updateMapRecord has
	 * been called)
	 * 
	 * @param mapProject
	 *            the map project
	 * @param concept
	 *            the concept
	 * @param mapUser
	 *            the map user
	 * @param mapRecord
	 *            the map record
	 * @param workflowAction
	 *            the workflow action
	 * @throws Exception
	 *             the exception
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
		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(mapProject.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
		algorithmHandler.setMapProject(mapProject);

		// locate any existing workflow tracking records for this project and
		// concept
		// NOTE:  Exception handling deliberate, since tracking record may or may not exist
		//		  depending on workflow path
		TrackingRecord trackingRecord;
		try {
			trackingRecord = getTrackingRecord(mapProject, concept);
		} catch (NoResultException e) {
			trackingRecord = null;
		}
		Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

		// if the record passed in updates an existing record, replace it in the
		// set
		if (mapRecord != null && mapRecord.getId() != null) {
			for (MapRecord mr : mapRecords) {
				if (mr.getId().equals(mapRecord.getId())) {
					mapRecords.remove(mr);
					mapRecords.add(mapRecord);
					break;
				}
			}
		}

		// switch on workflow action
		switch (workflowAction) {
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
				trackingRecord.setTerminologyVersion(concept
						.getTerminologyVersion());
				trackingRecord.setTerminologyId(concept.getTerminologyId());
				trackingRecord.setDefaultPreferredName(concept
						.getDefaultPreferredName());
				trackingRecord.addMapRecordId(mapRecord.getId());

				// get the tree positions for this concept and set the sort key
				// to
				// the first retrieved
				ContentService contentService = new ContentServiceJpa();
				TreePositionList treePositionsList = contentService
						.getTreePositionsWithDescendants(
								concept.getTerminologyId(),
								concept.getTerminology(),
								concept.getTerminologyVersion());

				// handle inactive concepts - which don't have tree positions
				if (treePositionsList.getCount() == 0) {
					trackingRecord.setSortKey("");
				} else {
					trackingRecord.setSortKey(treePositionsList
							.getTreePositions().get(0).getAncestorPath());
				}

				// only FIX_ERROR_PATH valid, QA_PATH in Phase 2
				trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);

				// perform the assign action via the algorithm handler
				mapRecords = algorithmHandler.assignFromInitialRecord(
						trackingRecord, mapRecords, mapRecord, mapUser);
			} else {
				
				throw new LocalException("Assignment from published record failed -- concept already in workflow");
			
			}

			break;

		case ASSIGN_FROM_SCRATCH:

			// Logger.getLogger(WorkflowServiceJpa.class).info(
			// "ASSIGN_FROM_SCRATCH");

			// expect existing (pre-computed) workflow tracking record
			if (trackingRecord == null) {
				throw new Exception(
						"Could not find tracking record for assignment.");
			}

			// perform the assignment via the algorithm handler
			mapRecords = algorithmHandler.assignFromScratch(trackingRecord,
					mapRecords, concept, mapUser);

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
			mapRecords = algorithmHandler.unassign(trackingRecord, mapRecords,
					mapUser);

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
						"SAVE_FOR_LATER - User not assigned to record");

			// Logger.getLogger(WorkflowServiceJpa.class).info(
			// "Performing action...");

			mapRecords = algorithmHandler.saveForLater(trackingRecord,
					mapRecords, mapUser);

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
						"User not assigned to record for finishing request");
			//
			// Logger.getLogger(WorkflowServiceJpa.class).info(
			// "Performing action...");
			//
			// // perform the action
			mapRecords = algorithmHandler.finishEditing(trackingRecord,
					mapRecords, mapUser);

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
						"User not assigned to record for cancel request");

			mapRecords = algorithmHandler.cancelWork(trackingRecord,
					mapRecords, mapUser);

			break;
		default:
			throw new Exception("Unknown action requested.");
		}


		Logger.getLogger(WorkflowServiceJpa.class).info("Synchronizing...");

		// synchronize the map records via helper function
		Set<MapRecord> syncedRecords = synchronizeMapRecords(trackingRecord,
				mapRecords);


		// clear the pointer fields (i.e. ids and names of mapping
		// objects)
		trackingRecord.setMapRecordIds(null);
		trackingRecord.setAssignedUserNames(null);
		trackingRecord.setUserAndWorkflowStatusPairs(null);

		// recalculate the pointer fields
		for (MapRecord mr : syncedRecords) {
			trackingRecord.addMapRecordId(mr.getId());
			trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
			trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner()
					.getUserName(), mr.getWorkflowStatus().toString());
		}

		// if the tracking record is ready for removal, delete it
		if ((getWorkflowStatusForTrackingRecord(trackingRecord).equals(
				WorkflowStatus.READY_FOR_PUBLICATION) || getWorkflowStatusForTrackingRecord(trackingRecord).equals(
						WorkflowStatus.PUBLISHED))
				&& trackingRecord.getMapRecordIds().size() == 1) {

			removeTrackingRecord(trackingRecord.getId());

			// else add the tracking record if new
		} else if (trackingRecord.getId() == null) {
			addTrackingRecord(trackingRecord);

			// otherwise update the tracking record
		} else {

			updateTrackingRecord(trackingRecord);
		}

	}

	/**
	 * Algorithm has gotten needlessly complex due to conflicting service
	 * changes and algorithm handler changes. However, the basic process is
	 * this:
	 * 
	 * 1) Function takes a set of map records returned from the algorithm
	 * handler These map records may have a hibernate id (updated/unchanged) or
	 * not (added) 2) The passed map records are detached from the persistence
	 * environment. 3) The existing (in database) records are re-retrieved from
	 * the database. Note that this is why the passed map records are detached
	 * -- otherwise they are overwritten. 4) Each record in the detached set is
	 * checked against the 'refreshed' database record set - if the detached
	 * record is not in the set, then it has been added - if the detached record
	 * is in the set, check it for updates - if it has been changed, update it -
	 * if no change, disregard 5) Each record in the 'refreshed' databased
	 * record set is checked against the new set - if the refreshed record is
	 * not in the new set, delete it from the database 6) Return the detached
	 * set as re-synchronized with the database
	 * 
	 * Note on naming conventions used in this method: - mapRecords: the set of
	 * records passed in as argument - newRecords: The set of records to be
	 * returned after synchronization - oldRecords: The set of records retrieved
	 * by id from the database for comparison - syncedRecords: The synchronized
	 * set of records for return from this routine
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapRecords
	 *            the map records
	 * @return the sets the
	 * @throws Exception
	 *             the exception
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
				MapRecord newRecord = new MapRecordJpa(mr, true);
				
				 Logger.getLogger(WorkflowServiceJpa.class).info(
				 "Adding record: " + newRecord.toString());
				 
				// add the record to the database
				
				mappingService.addMapRecord(mr);

				// add the record to the return list
				syncedRecords.add(newRecord);
			}

			// otherwise, check for update
			else {
				// if the old map record is changed, update it
				Logger.getLogger(WorkflowServiceJpa.class).info("New record: " + mr.toString());
				Logger.getLogger(WorkflowServiceJpa.class).info("Old record: " + getMapRecordInSet(oldRecords, mr.getId()).toString());
				
				if (!mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
					Logger.getLogger(WorkflowServiceJpa.class).info(
							 "Updating record: " + mr.getId().toString() + " with " + mr.getMapEntries().get(0) + " advices");
					mappingService.updateMapRecord(mr);
				} else {
					Logger.getLogger(WorkflowServiceJpa.class).info(
							 "Record " + mr.getId().toString() + " has not changed, not updating");
				}
				
				

				syncedRecords.add(mr);
			}
		}

		// cycle over old records to check for deletions
		for (MapRecord mr : oldRecords) {

			// if old record is not in the new record set, delete it
			if (getMapRecordInSet(syncedRecords, mr.getId()) == null) {
				mappingService.removeMapRecord(mr.getId());
			}
		}

		// close the service
		mappingService.close();

		return newRecords;

	}

	/**
	 * Gets the map record in set.
	 * 
	 * @param mapRecords
	 *            the map records
	 * @param mapRecordId
	 *            the map record id
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
		SearchResultList conceptsInScope = mappingService.findConceptsInScope(
				mapProject.getId(), null);

		// construct a hashset of concepts in scope
		Set<String> conceptIds = new HashSet<>();
		for (SearchResult sr : conceptsInScope.getIterable()) {
			conceptIds.add(sr.getTerminologyId());
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Concept ids put into hash set: " + conceptIds.size());

		// get the current records
		MapRecordList mapRecords = mappingService
				.getMapRecordsForMapProject(mapProject.getId());

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Processing existing records (" + mapRecords.getCount()
						+ " found)");

		// instantiate a mapped set of non-published records
		Map<String, List<MapRecord>> unpublishedRecords = new HashMap<>();

		// cycle over the map records, and remove concept ids if a map record is
		// publication-ready
		for (MapRecord mapRecord : mapRecords.getIterable()) {
			if (mapRecord.getWorkflowStatus().equals(
					WorkflowStatus.READY_FOR_PUBLICATION)
					|| mapRecord.getWorkflowStatus().equals(
							WorkflowStatus.PUBLISHED)) {

				conceptIds.remove(mapRecord.getConceptId());
			} else {

				List<MapRecord> originIds;

				// if this key does not yet have a constructed list, make one,
				// otherwise get the existing list
				if (unpublishedRecords.containsKey(mapRecord.getConceptId())) {
					originIds = unpublishedRecords
							.get(mapRecord.getConceptId());
				} else {
					originIds = new ArrayList<>();
				}

				originIds.add(mapRecord);
				unpublishedRecords.put(mapRecord.getConceptId(), originIds);
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Concepts with no publication-ready map record: "
						+ conceptIds.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Concepts with unpublished map record content:  "
						+ unpublishedRecords.size());

		beginTransaction();

		// construct the tracking records for unmapped concepts
		for (String terminologyId : conceptIds) {

			// retrieve the concept for this result
			Concept concept = contentService.getConcept(terminologyId,
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			// create a workflow tracking record for this concept
			TrackingRecord trackingRecord = new TrackingRecordJpa();

			// populate the fields from project and concept
			trackingRecord.setMapProjectId(mapProject.getId());
			trackingRecord.setTerminology(concept.getTerminology());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminologyVersion(concept
					.getTerminologyVersion());
			trackingRecord.setDefaultPreferredName(concept
					.getDefaultPreferredName());
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

			// get the tree positions for this concept and set the sort key to
			// the first retrieved
			TreePositionList treePositionsList = contentService
					.getTreePositions(concept.getTerminologyId(),
							concept.getTerminology(),
							concept.getTerminologyVersion());

			// handle inactive concepts - which don't have tree positions
			if (treePositionsList.getCount() == 0) {
				trackingRecord.setSortKey("");
			} else {
				trackingRecord.setSortKey(treePositionsList.getTreePositions()
						.get(0).getAncestorPath());
			}

			// add any existing map records to this tracking record
			if (unpublishedRecords.containsKey(trackingRecord
					.getTerminologyId())) {
				for (MapRecord mr : unpublishedRecords.get(trackingRecord
						.getTerminologyId())) {
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"    Adding existing map record " + mr.getId()
									+ ", owned by "
									+ mr.getOwner().getUserName()
									+ " to tracking record for "
									+ trackingRecord.getTerminologyId());

					trackingRecord.addMapRecordId(mr.getId());
					trackingRecord.addAssignedUserName(mr.getOwner()
							.getUserName());
					trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner()
							.getUserName(), mr.getWorkflowStatus().toString());
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
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);
		fullTextEntityManager.setProperty("Version", Version.LUCENE_36);

		// create the indexes
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Creating indexes for TrackingRecordJpa");
		fullTextEntityManager.purgeAll(TrackingRecordJpa.class);
		fullTextEntityManager.flushToIndexes();
		fullTextEntityManager.createIndexer(TrackingRecordJpa.class)
				.batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
				.threadsToLoadObjects(4).threadsForSubsequentFetching(8)
				.startAndWait();
		
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

	/**
	 * Generates up to a desired number of conflicts for a map project.
	 * 
	 * Clears any map records with status != PUBLISHED.
	 * 
	 * @param mapProject
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void generateRandomConflictData(MapProject mapProject,
			int numDesiredConflicts) throws Exception {

		// instantiate the random number generator
		Random rand = new Random();

		// instantiate the services and algorithm handler
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
				.getProjectSpecificAlgorithmHandler(mapProject);


		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Generating random conflicts -- number desired is "
						+ numDesiredConflicts);

		// the tracking records associated with this project
		List<TrackingRecord> trackingRecords = getTrackingRecordsForMapProject(
				mapProject).getTrackingRecords();

		// the list of tracking records available to specialist assignment
		List<TrackingRecord> specialistTrackingRecords = new ArrayList<>();

		// the list of CONFLICT_DETECTED tracking records
		List<TrackingRecord> leadTrackingRecords = new ArrayList<>();

		// the list of CONFLICT_NEW tracking records
		List<TrackingRecord> conflictTrackingRecords = new ArrayList<>();

		// the list of specialists and leads on this project (for convenience)
		List<MapUser> mapSpecialists = new ArrayList<>();
		List<MapUser> mapLeads = new ArrayList<>();

		// select only the 'real' (human) users
		Logger.getLogger(WorkflowServiceJpa.class).info(" Specialists found:");
		for (MapUser mapSpecialist : mapProject.getMapSpecialists()) {
			if (!mapSpecialist.getName().matches(
					"Loader Record|Legacy Record|Default|string")) {
				mapSpecialists.add(mapSpecialist);
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"  " + mapSpecialist.getName());
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(" Leads found:");
		for (MapUser mapLead : mapProject.getMapLeads()) {
			mapLeads.add(mapLead);
			Logger.getLogger(WorkflowServiceJpa.class).info(
					"  " + mapLead.getName());
		}

		// throw exceptions if the user set is not sufficient
		if (mapSpecialists.size() < 2) {
			throw new Exception(
					"Cannot generate random conflicts with less than two specialists attached to the project");
		}
		if (mapLeads.size() == 0) {
			throw new Exception(
					"Cannot generate random conflicts without a lead attached to the project");
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Computing available work " + trackingRecords.size()
						+ " workflow tracking records");

		// sort the currently existing workflow state into the various sets
		for (TrackingRecord trackingRecord : trackingRecords) {

			// if no records attached to this tracking record, it is "clean"
			if (trackingRecord.getMapRecordIds().size() == 0) {

				// add clean record to available for specialist list
				specialistTrackingRecords.add(trackingRecord);

				// otherwise, in-progress workflow already exists
			} else {
				switch (getWorkflowStatusForTrackingRecord(trackingRecord)) {

				// conflict available to lead
				case CONFLICT_DETECTED:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"   Available conflict: "
									+ trackingRecord.getTerminologyId());
					leadTrackingRecords.add(trackingRecord);
					break;

				// assigned conflict is added to final set
				case CONFLICT_IN_PROGRESS:
				case CONFLICT_NEW:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"   Assigned conflict: "
									+ trackingRecord.getTerminologyId());

					conflictTrackingRecords.add(trackingRecord);
					break;

				// Consensus Path ignored
				case CONSENSUS_NEEDED:
				case CONSENSUS_IN_PROGRESS:
					// do nothing
					break;

				// specialist editing in progress
				case EDITING_DONE:
				case EDITING_IN_PROGRESS:
				case NEW:

					// if only one record present, available to another
					// specialist
					if (trackingRecord.getMapRecordIds().size() == 1) {
						specialistTrackingRecords.add(trackingRecord);
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"   Available Concept: "
										+ trackingRecord.getTerminologyId());
					}

					break;

				// ignore published and review status
				case PUBLISHED:
				case READY_FOR_PUBLICATION:
				case REVISION:
					break;
				default:
					Logger.getLogger(WorkflowServiceJpa.class).info(
							"     ERROR:  invalid workflow status");
					break;
				}
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("     Concepts available:  "
						+ specialistTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Conflicts available: " + leadTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Existing conflicts:  " + conflictTrackingRecords.size());

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Generating list of valid targets.");

		int startConceptIndex = 0;
		List<Concept> targetCodes = new ArrayList<>();
		while (targetCodes.size() < 1000) {

			for (Concept concept : (List<Concept>) manager
					.createQuery(
							"select c from ConceptJpa c where terminology = :terminology")
					.setParameter("terminology",
							mapProject.getDestinationTerminology())
					.setFirstResult(startConceptIndex).setMaxResults(1000)
					.getResultList()) {

				if (algorithmHandler.isTargetCodeValid(concept
						.getTerminologyId())) {
					targetCodes.add(concept);
				}
			}
		}

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Target set has " + targetCodes.size() + " concepts.");

		int nRecordsAssignedToSpecialist = 0;
		int nRecordsSavedForLater = 0;

		// generate a set of valid target concepts
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Begin assigning random work...");

		// perform assignment loop until :
		// - the number of desired conflicts is reached OR
		// - the records available to specialists is exhausted
		while (leadTrackingRecords.size() < numDesiredConflicts
				&& specialistTrackingRecords.size() > 0) {

			// if CONFLICT_DETECTED records are available
			if (leadTrackingRecords.size() > 0) {

				// get the first available CONFLICT_DETECTED tracking record
				TrackingRecord trackingRecord = leadTrackingRecords.get(0);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"   Procesing CONFLICT_DETECTED for "
								+ trackingRecord.getTerminologyId() + ", "
								+ trackingRecord.getDefaultPreferredName());

				// get the concept for this tracking record
				Concept concept = contentService.getConcept(
						trackingRecord.getTerminologyId(),
						trackingRecord.getTerminology(),
						trackingRecord.getTerminologyVersion());

				// get the random lead
				MapUser mapLead = getAssignableLead(leadTrackingRecords.get(0),
						mapLeads);

				// if no user available, move to the next tracking record
				if (mapLead == null) {
					leadTrackingRecords.remove(trackingRecord);
					continue;
				}

				// assign the conflict
				processWorkflowAction(mapProject, concept, mapLead, null,
						WorkflowAction.ASSIGN_FROM_SCRATCH);

				// add this workflow tracking record to the conflict assigned
				// list
				conflictTrackingRecords.add(trackingRecord);

				// remove this workflow tracking record from the conflict
				// available list
				leadTrackingRecords.remove(trackingRecord);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"    Conflict assigned to " + mapLead.getName());

				// otherwise, randomly assign a specialist to a record and
				// modify the record
			} else {

				// get the next available tracking record and process it
				TrackingRecord trackingRecord = specialistTrackingRecords
						.get(0);

				Logger.getLogger(WorkflowServiceJpa.class).info(
						"   Procesing available Concept for "
								+ trackingRecord.getTerminologyId() + ", "
								+ trackingRecord.getDefaultPreferredName());

				// get the concept for this record
				Concept concept = contentService.getConcept(
						trackingRecord.getTerminologyId(),
						trackingRecord.getTerminology(),
						trackingRecord.getTerminologyVersion());
				
				// set the initial flag for whether this concept has been "finished"
				// - a conflict has arisen
				// - two specialists have been assigned, but one record has been
				//   saved for later or there were no conflicts
				boolean conceptProcessed = false;
				
				do {

					// get the available specialist for this tracking record
					MapUser mapSpecialist = getAssignableSpecialist(trackingRecord,
							mapSpecialists);
	
					// if no user available, move to the next tracking record
					if (mapSpecialist == null) {
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"     No user available for assignment, removing concept");
						specialistTrackingRecords.remove(trackingRecord);
						continue;
					}
	
					// assign the specialist to this concept
					processWorkflowAction(mapProject, concept, mapSpecialist, null,
							WorkflowAction.ASSIGN_FROM_SCRATCH);
	
					// get the record corresponding to this user
					MapRecord mapRecord = getMapRecordForTrackingRecordAndMapUser(
							trackingRecord, mapSpecialist);
	
					// make some random changes to the record
					randomizeMapRecord(mapProject, mapRecord, targetCodes);
	
					// determine whether to save for later or finish
					ValidationResult validationResult = algorithmHandler
							.validateRecord(mapRecord);
					if (validationResult.getErrors().size() > 0 // if any errors
																// reported
							|| rand.nextInt(5) == 0) { // randomly save some for
														// later anyway
	
						if (validationResult.getErrors().size() > 0) {
							Logger.getLogger(WorkflowServiceJpa.class).info(
									"    Randomized record has errors: "
											+ mapSpecialist.getName());
							for (String error : validationResult.getErrors()) {
								Logger.getLogger(WorkflowServiceJpa.class).info(
										"      " + error);
							}
						}
	
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"    Record saved for later by "
										+ mapSpecialist.getName());
	
						processWorkflowAction(mapProject, concept, mapSpecialist,
								mapRecord, WorkflowAction.SAVE_FOR_LATER);
						nRecordsSavedForLater++;
	
					} else {
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"    Finish editing by " + mapSpecialist.getName());
						processWorkflowAction(mapProject, concept, mapSpecialist,
								mapRecord, WorkflowAction.FINISH_EDITING);
	
					}
					
					// check if a conflict has arisen
					if (getWorkflowStatusForTrackingRecord(trackingRecord).equals(
							WorkflowStatus.CONFLICT_DETECTED)) {

						// add the tracking record to the available for lead list
						leadTrackingRecords.add(trackingRecord);

						// remove the tracking record from th
						specialistTrackingRecords.remove(trackingRecord);

						Logger.getLogger(WorkflowServiceJpa.class).info(
								"    New conflict detected!");
						
						conceptProcessed = true;


					// otherwise, check that this record is not 'stuck' in
					// editing
					// - workflow status is less than CONFLICT_DETECTED
					// - AND two users are assigned
					} else if (getWorkflowStatusForTrackingRecord(trackingRecord)
							.compareTo(WorkflowStatus.CONFLICT_DETECTED) < 1
							&& trackingRecord.getMapRecordIds().size() == 2) {

						Logger.getLogger(WorkflowServiceJpa.class)
								.info("    Tracking record has two users and is not in conflict, removing.");

						// remove the record from the available list
						specialistTrackingRecords.remove(trackingRecord);
						
						conceptProcessed = true;
					}
					
				} while(conceptProcessed == false);

				

				// increment the counter
				nRecordsAssignedToSpecialist++;
			}
		}
		Logger.getLogger(WorkflowServiceJpa.class).info("Generation complete.");
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Concepts still available:        "
						+ specialistTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Conflicts still available:       "
						+ leadTrackingRecords.size());
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Records assigned to specialists: "
						+ nRecordsAssignedToSpecialist);
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Records 'saved for later':       " + nRecordsSavedForLater);
		Logger.getLogger(WorkflowServiceJpa.class).info(
				"   Conflicts assigned to leads:     "
						+ conflictTrackingRecords.size());
	}

	/**
	 * Returns the assignable specialist.
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapUsers
	 *            the map users
	 * @return the assignable specialist
	 * @throws Exception
	 *             the exception
	 */
	private MapUser getAssignableSpecialist(TrackingRecord trackingRecord,
			List<MapUser> mapUsers) throws Exception {

		// discard any users already assigned to this record
		for (MapUser mapUser : getMapUsersForTrackingRecord(trackingRecord)
				.getMapUsers()) {
			mapUsers.remove(mapUser);
		}

		// if no assignable users, return null
		if (mapUsers.size() == 0)
			return null;

		// return a random user from the truncated list
		Random rand = new Random();
		return mapUsers.get(rand.nextInt(mapUsers.size()));

	}

	/**
	 * Returns the assignable lead.
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapUsers
	 *            the map users
	 * @return the assignable lead
	 * @throws Exception
	 *             the exception
	 */
	private MapUser getAssignableLead(TrackingRecord trackingRecord,
			List<MapUser> mapUsers) throws Exception {

		// discard any users already assigned to this record
		for (MapUser mapUser : getMapUsersForTrackingRecord(trackingRecord)
				.getMapUsers()) {
			mapUsers.remove(mapUser);
		}

		// if no assignable users, return null
		if (mapUsers.size() == 0)
			return null;

		// return a random user from the truncated list
		Random rand = new Random();
		return mapUsers.get(rand.nextInt(mapUsers.size()));
	}

	/**
	 * Randomize map record.
	 * 
	 * @param mapProject
	 *            the map project
	 * @param mapRecord
	 *            the map record
	 * @param targetConcepts
	 *            the target concepts
	 */
	private void randomizeMapRecord(MapProject mapProject, MapRecord mapRecord,
			List<Concept> targetConcepts) {

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"     Randomizing map record.");

		Random rand = new Random();

		// /////////////////////////
		// RULES
		// /////////////////////////
		List<String> precomputedRules = new ArrayList<>();

		// add the gender rules
		precomputedRules.add("IFA 248153007 | Male (finding) |");
		precomputedRules.add("IFA 248152002 | Female (finding) |");

		// age rule variables
		List<MapAgeRange> ageRanges;
		int nAgeRanges;

		// add a random number of Age - At Onset rules
		ageRanges = new ArrayList<>(mapProject.getPresetAgeRanges());

		// determine a random number of chronological age ranges to add
		nAgeRanges = rand.nextInt(ageRanges.size());

		for (int i = 0; i < nAgeRanges; i++) {
			// get a random age range from the dynamic list
			MapAgeRange ageRange = ageRanges
					.get(rand.nextInt(ageRanges.size()));

			// compute the rule string
			precomputedRules
					.add(computeAgeRuleString(
							"IFA 424144002 | Current chronological age (observable entity)",
							ageRange));

			// remove the age range from the list
			ageRanges.remove(ageRange);
		}

		// add a random number of Age - Chronological rules
		ageRanges = new ArrayList<>(mapProject.getPresetAgeRanges());

		// determine a random number of onset age ranges to add
		nAgeRanges = rand.nextInt(ageRanges.size());

		for (int i = 0; i < nAgeRanges; i++) {
			// get a random age range from the dynamic list
			MapAgeRange ageRange = ageRanges
					.get(rand.nextInt(ageRanges.size()));

			// compute the rule string
			precomputedRules
					.add(computeAgeRuleString(
							"IFA 445518008 | Age at onset of clinical finding (observable entity)",
							ageRange));

			// remove the age range from the list
			ageRanges.remove(ageRange);
		}

		// if no group structure, 1 group, else, between 1 and 2 groups
		int numGroups = mapProject.isGroupStructure() == true ? rand.nextInt(2) + 1
				: 1;

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"      Record will have " + numGroups + " groups");
		
		// determine whether to add a note (currently 50% chance)
		if (rand.nextInt(2) == 0) {

			// determine a random number of notes
			int nNote = rand.nextInt(2) + 1;
			for (int iNote = 0; iNote < nNote; iNote++) {

				MapNote mapNote = new MapNoteJpa();
				mapNote.setUser(mapRecord.getOwner());
				mapNote.setNote("I'm note #" + (iNote + 1) + " by "
						+ mapRecord.getOwner().getName());
				mapRecord.addMapNote(mapNote);
				Logger.getLogger(WorkflowServiceJpa.class).info(
						"         Added note: " + mapNote.getNote());
			}
		}

		// //////////////////////////////
		// ENTRIES
		// //////////////////////////////
		for (int i = 1; i <= numGroups; i++) {

			// determine the number of entries
			int numEntries = rand.nextInt(3) + 1;

			Logger.getLogger(WorkflowServiceJpa.class).info(
					"       Group " + i + " will have " + numEntries
							+ " entries");

			// generate entries
			for (int j = 1; j <= numEntries; j++) {

				// instantiate the map entry with group and priority
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setMapGroup(i);
				mapEntry.setMapPriority(j);

				// assign a target code
				Concept targetConcept = targetConcepts.get(rand
						.nextInt(targetConcepts.size()));
				mapEntry.setTargetId(targetConcept.getTerminologyId());
				mapEntry.setTargetName(targetConcept.getDefaultPreferredName());

				// if project is rule based
				if (mapProject.isRuleBased()) {

					List<String> availableRules = new ArrayList<>(
							precomputedRules);

					// if last entry in group, assign TRUE rule
					if (j == numEntries) {
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"         Setting rule: TRUE");
						mapEntry.setRule("TRUE");
					} else {
						mapEntry.setRule(availableRules.get(rand
								.nextInt(availableRules.size())));
						Logger.getLogger(WorkflowServiceJpa.class).info(
								"         Setting rule: " + mapEntry.getRule());

					}
				}

				// add the map entry
				mapRecord.addMapEntry(mapEntry);

			}
		}
	}

	/**
	 * Compute age rule string. Helper function for randomizeMapRecord.
	 * 
	 * @param initString
	 *            the init string
	 * @param ageRange
	 *            the age range
	 * @return the string
	 */

	private String computeAgeRuleString(String initString, MapAgeRange ageRange) {

		String rule = "";

		if (ageRange.hasLowerBound() == true) {
			rule += initString + " | "
					+ (ageRange.getLowerInclusive() == true ? ">=" : ">")
					+ ageRange.getLowerValue() + " " + ageRange.getLowerUnits();
		}

		if (ageRange.hasLowerBound() == true
				&& ageRange.hasUpperBound() == true) {
			rule += " AND ";
		}

		if (ageRange.hasUpperBound() == true) {
			rule += initString + " | "
					+ (ageRange.getUpperInclusive() == true ? ">=" : ">")
					+ ageRange.getUpperValue() + " " + ageRange.getUpperUnits();
		}

		return rule;
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
	public MapUserList getMapUsersForTrackingRecord(
			TrackingRecord trackingRecord) throws Exception {
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

	/**
	 * Returns the map record for workflow tracking record and map user.
	 * 
	 * @param trackingRecord
	 *            the tracking record
	 * @param mapUser
	 *            the map user
	 * @return the map record for workflow tracking record and map user
	 * @throws Exception
	 *             the exception
	 */
	private MapRecord getMapRecordForTrackingRecordAndMapUser(
			TrackingRecord trackingRecord, MapUser mapUser) throws Exception {
		for (MapRecord mapRecord : getMapRecordsForTrackingRecord(trackingRecord)) {
			if (mapRecord.getOwner().equals(mapUser))
				return mapRecord;
		}
		return null;
	}

	@Override
	public void generateMapperTestingStateKLININ(MapProject mapProject)
			throws Exception {

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Generating clean Mapping User Testing State for project "
						+ mapProject.getName());

		String[] concepts = { "28221000119103", "700189007", "295131000119103",
				"72791000119108", "295041000119108", "295051000119105",
				"700147004", "700109009", "700112007", "700150001",
				"700081008", "700075000", "700097003", "700080009",
				"700082001", "402714001", "700094005", "166631000119101",
				"700076004", "403469006", "3961000119101", "700095006",
				"700153004", "700195008", "700107006", "700111000",
				"700077008", "700079006", "700167008", "700178000",
				"700181005", "700176001", "700170007", "700164001",
				"700173009", "440419004", "700078003", "700168003",
				"700179008", "700182003", "700177005", "700171006",
				"700165000", "700174003", "700149001", "700127007", "700132008"

		};

		String[] userNames = { "kli", "nin" };

		generateMappingTestingState(mapProject, userNames, concepts);

	}

	@Override
	public void generateMapperTestingStateBHEKRE(MapProject mapProject)
			throws Exception {

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Generating clean Mapping User Testing State for project "
						+ mapProject.getName());

		String[] concepts = { "399050001", "283795009", "283796005",
				"110079005", "110248002", "110243006", "373588006",
				"109683005", "38372004", "427782005", "282756002", "283362001",
				"196439008", "2136001", "285670000", "284205006", "8954007",
				"110077007", "5529003", "3097002", "109762009", "441373001",
				"420881009", "419076005", "418176003", "419474003",
				"277209002", "294432001", "156073000", "294090001",
				"416402001", "233624006", "294386005", "156072005",
				"402267000", "230599000", "295050005", "34270000", "431043000",
				"129616004", "405538007", "294594004", "433202001",
				"399076001", "135071000119105", "699529005", "699705001",
				"699859009", "126741004", "427916006", "699942000",
				"699699005", "48601000119107", "699649006", "699588004",
				"699760008", "202857003", "699686007", "108431000119104",
				"399932006", "700049004", "698632006", "93713003", "700053002",
				"700055009", "403967000" };

		String[] userNames = { "bhe", "kre" };

		generateMappingTestingState(mapProject, userNames, concepts);

	}

	@SuppressWarnings("unchecked")
	private void generateMappingTestingState(MapProject mapProject,
			String[] userNames, String[] concepts) throws Exception {

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Total: " + concepts.length + " concepts requested");

		// combine the string arrays into a unique-value hash set
		Set<MapRecord> existingRecords = new HashSet<>();
		Set<String> uniqueIds = new HashSet<>();
		for (String terminologyId : concepts) {
			uniqueIds.add(terminologyId);
		}

		// open the services
		MappingService mappingService = new MappingServiceJpa();
		ContentService contentService = new ContentServiceJpa();

		// set the terminology and version -- shorthand
		String terminology = mapProject.getSourceTerminology();
		String terminologyVersion = mapProject.getSourceTerminologyVersion();

		// get the map users
		Set<MapUser> mapUsers = new HashSet<>();
		for (String userName : userNames) {
			mapUsers.add(mappingService.getMapUser(userName));
		}

		// retrieve the concepts matching the unique ids and assemble them in a
		// map of terminologyId -> concept
		Map<String, Concept> uniqueConcepts = new HashMap<>();
		for (String terminologyId : uniqueIds) {
			Concept concept = contentService.getConcept(terminologyId,
					terminology, terminologyVersion);
			if (concept != null) {
				uniqueConcepts.put(terminologyId, concept);
			} else {
				Logger.getLogger(WorkflowServiceJpa.class).warn(
						"  Concept " + terminologyId
								+ " not found in database.");
			}
		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"  Total: " + concepts.length + " unique concepts retrieved");

		// find any existing records for the concepts
		for (String terminologyId : uniqueIds) {
			javax.persistence.Query query = manager
					.createQuery(
							"select mr from MapRecordJpa mr where conceptId = :conceptId and mapProjectId = :mapProjectId")
					.setParameter("conceptId", terminologyId)
					.setParameter("mapProjectId", mapProject.getId());

			List<MapRecord> mapRecords = (List<MapRecord>) query
					.getResultList();
			for (MapRecord mapRecord : mapRecords) {
				existingRecords.add(mapRecord);
			}

		}
		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Removing existing map records and updating/creating tracking records for specified concepts, found "
						+ existingRecords.size());

		// remove the existing records
		for (MapRecord mapRecord : existingRecords) {
			Logger.getLogger(WorkflowServiceJpa.class).warn(
					"Removing record " + mapRecord.getId() + ", owned by "
							+ mapRecord.getOwner().getUserName());
			mappingService.removeMapRecord(mapRecord.getId());

		}

		Logger.getLogger(WorkflowServiceJpa.class)
				.info("Deleting and re-creating tracking records to ensure clean state");

		// remove the existing tracking records and create a new one
		for (Concept concept : uniqueConcepts.values()) {

			TrackingRecord trackingRecord = null;
			try {
				trackingRecord = getTrackingRecordForMapProjectAndConcept(
						mapProject,
						uniqueConcepts.get(concept.getTerminologyId()));
			} catch (Exception e) {
				// do nothing
			}
			if (trackingRecord != null)
				removeTrackingRecord(trackingRecord.getId());

			// create the new tracking record
			trackingRecord = new TrackingRecordJpa();
			trackingRecord.setDefaultPreferredName(concept
					.getDefaultPreferredName());
			trackingRecord.setMapProjectId(mapProject.getId());
			trackingRecord.setTerminologyId(concept.getTerminologyId());
			trackingRecord.setTerminology(mapProject.getSourceTerminology());
			trackingRecord.setTerminologyVersion(mapProject
					.getSourceTerminologyVersion());
			trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);

			trackingRecord.setSortKey(contentService
					.getTreePositions(trackingRecord.getTerminologyId(),
							trackingRecord.getTerminology(),
							trackingRecord.getTerminologyVersion())
					.getTreePositions().get(0).getAncestorPath());

			addTrackingRecord(trackingRecord);

		}

		Logger.getLogger(WorkflowServiceJpa.class).info(
				"Assigning concepts....");

		// cycle over map users
		for (MapUser mapUser : mapUsers) {
			// assign concept to user
			for (Concept concept : uniqueConcepts.values()) {
				if (concept != null)
					this.processWorkflowAction(mapProject, concept, mapUser,
							null, WorkflowAction.ASSIGN_FROM_SCRATCH);
			}
		}

		// close the services
		contentService.close();
		mappingService.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#close()
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
	 * org.ihtsdo.otf.mapping.services.WorkflowService#getTransactionPerOperation
	 * ()
	 */
	@Override
	public boolean getTransactionPerOperation() throws Exception {
		return transactionPerOperation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.WorkflowService#setTransactionPerOperation
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
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#beginTransaction()
	 */
	@Override
	public void beginTransaction() throws Exception {

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
	 * @see org.ihtsdo.otf.mapping.services.WorkflowService#commit()
	 */
	@Override
	public void commit() throws Exception {

		if (getTransactionPerOperation())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when using transactions per operation mode.");
		else if (tx != null && !tx.isActive())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when there "
							+ "is no active transaction");
		tx.commit();
	}

}
