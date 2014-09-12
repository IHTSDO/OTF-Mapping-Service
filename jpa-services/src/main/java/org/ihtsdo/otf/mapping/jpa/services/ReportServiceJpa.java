package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.ihtsdo.otf.mapping.helpers.FeedbackList;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput;
import org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutputJpa;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

public class ReportServiceJpa extends RootServiceJpa implements ReportService {
	
	public ReportServiceJpa() throws Exception {
		super();
	}

	@Override
	public MapReportSpecialistOutput getUserReportSpecialistOutput(Long id) {
		MapReportSpecialistOutput m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from MapReportSpecialistOutputJpa m where id = :id");
		query.setParameter("id", id);
		m = (MapReportSpecialistOutput) query.getSingleResult();

		return m;
	}

	@Override
	public MapReportSpecialistOutput addUserReportSpecialistOutput(
			MapReportSpecialistOutput specialistOutput) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(specialistOutput);
			tx.commit();

			return specialistOutput;
		} else {
			if (!tx.isActive()) {
				throw new IllegalStateException(
						"Error attempting to change data without an active transaction");
			}
			manager.persist(specialistOutput);
			return specialistOutput;
		}

	}

	@Override
	public void updateUserReportSpecialistOutput(
			MapReportSpecialistOutput specialistOutput) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			manager.merge(specialistOutput);
			tx.commit();
		} else {
			manager.merge(specialistOutput);
		}

	}

	@Override
	public void removeUserReportSpecialistOutput(Long id) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

			// first, remove the user from this report
			tx.begin();
			MapReportSpecialistOutput sr = manager.find(
					MapReportSpecialistOutput.class, id);
			sr.setOwner(null);
			;
			tx.commit();

			// now remove the report
			tx.begin();
			if (manager.contains(sr)) {
				manager.remove(sr);
			} else {
				manager.remove(manager.merge(sr));
			}
			tx.commit();
		} else {
			MapReportSpecialistOutput sr = manager.find(
					MapReportSpecialistOutput.class, id);
			sr.setOwner(null);
			if (manager.contains(sr)) {
				manager.remove(sr);
			} else {
				manager.remove(manager.merge(sr));
			}
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public MapReportSpecialistOutput computeSpecialistOutputReport(
			MapUser mapUser, MapProject mapProject, Date startDate, Date endDate)
			throws Exception {
		
		Logger.getLogger(ReportServiceJpa.class).info("Entering computeSpecialistOutputReport");

		// construct the report object
		MapReportSpecialistOutput report = new MapReportSpecialistOutputJpa();
		report.setOwner(mapUser);
		report.setEndDate(endDate);
		report.setStartDate(startDate);

		// get the map records modified by this user in this time
		AuditReader reader = AuditReaderFactory.get(manager);

		// construct the query
		AuditQuery query = reader
				.createQuery()

				// all revisions, returned as objects, finding deleted entries
				.forRevisionsOfEntity(MapRecordJpa.class, true, true)

				// add mapProjectId and owner as constraints
				.add(AuditEntity.property("mapProjectId")
						.eq(mapProject.getId()))
						
				.add(AuditEntity.relatedId("owner").eq(mapUser.getId()));

				/*.add(AuditEntity.property("lastModified").between(startDate.getTime(),
						endDate.getTime()));*/

		// execute the query
		List<MapRecord> editedRecords = query.getResultList();
		
		System.out.println(editedRecords.size() + " results found");

		// counter variables
		int nTotal = 0;
		int nFinished = 0;
		int nConflict = 0;
		int nError = 0;

		// local variable for feedbacks
		FeedbackList feedbackList;

		// instantiate workflow service
		WorkflowService workflowService = new WorkflowServiceJpa();

		// cycle over the records
		for (MapRecord mr : editedRecords) {
			
			System.out.println("Processing record " + mr.toString());

			// add record id
			report.addMapRecordId(mr.getId());

			switch (mapProject.getWorkflowType()) {
			case CONFLICT_PROJECT:

				// increment total counter
				nTotal++;

				// switch on workflow status
				switch (mr.getWorkflowStatus()) {

				// if at review needed or conflict detected stage
				case REVIEW_NEEDED:
					nFinished++;

					// find any feedback with errors
					feedbackList = workflowService
							.getFeedbackErrorsForRecord(mr);

					// increment error counter
					nError += feedbackList.getCount();

					break;
				case CONFLICT_DETECTED:
					// increment finished counter
					nFinished++;

					// increment conflict counter
					nConflict++;

					// find any feedback with errors
					feedbackList = workflowService
							.getFeedbackErrorsForRecord(mr);

					// increment error counter
					nError += feedbackList.getCount();

					break;
				case EDITING_DONE:
					nFinished++;
					break;
				case EDITING_IN_PROGRESS:
				case NEW:
					// do nothing
					break;

				default:
					Logger.getLogger(ReportServiceJpa.class).warn(
							"Invalid workflow status " + mr.getWorkflowStatus()
									+ " on record " + mr.getId());
					break;

				}

				break;
			case REVIEW_PROJECT:

				// increment total counter
				nTotal++;

				// switch on workflow status
				switch (mr.getWorkflowStatus()) {

				case REVIEW_NEEDED:

					nFinished++;

					// find any feedback with errors
					feedbackList = workflowService
							.getFeedbackErrorsForRecord(mr);

					// increment error counter
					nError += feedbackList.getCount();
					break;
				case EDITING_DONE:
					nFinished++;
					break;
				case EDITING_IN_PROGRESS:
				case NEW:
					// do nothing
					break;

				default:
					Logger.getLogger(ReportServiceJpa.class).warn(
							"Invalid workflow status " + mr.getWorkflowStatus()
									+ " on record " + mr.getId());
					break;

				}
			}

		}
		
		report.setTotalCount(nTotal);
		report.setFinishedCount(nFinished);
		report.setConflictCount(nConflict);
		report.setErrorCount(nError);

		return report;

	}

}
