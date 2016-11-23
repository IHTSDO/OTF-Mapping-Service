/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.apache.lucene.util.Version;
import org.hibernate.CacheMode;
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
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
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
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

/**
 * Default workflow service implementation.
 */
public class WorkflowServiceJpa extends MappingServiceJpa
    implements WorkflowService {

  /**
   * Instantiates an empty {@link WorkflowServiceJpa}.
   *
   * @throws Exception the exception
   */
  public WorkflowServiceJpa() throws Exception {
    super();

  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public TrackingRecordList getTrackingRecords() throws Exception {

    TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();

    trackingRecordList.setTrackingRecords(manager
        .createQuery("select tr from TrackingRecordJpa tr").getResultList());

    return trackingRecordList;
  }

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public TrackingRecordList getTrackingRecordsForMapProject(
    MapProject mapProject) throws Exception {

    TrackingRecordListJpa trackingRecordList = new TrackingRecordListJpa();
    javax.persistence.Query query = manager
        .createQuery(
            "select tr from TrackingRecordJpa tr where mapProjectId = :mapProjectId")
        .setParameter("mapProjectId", mapProject.getId());

    trackingRecordList.setTrackingRecords(query.getResultList());

    return trackingRecordList;
  }

  /* see superclass */
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

  /* see superclass */
  @Override
  public WorkflowException addWorkflowException(
    WorkflowException trackingRecord) throws Exception {

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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @Override
  public WorkflowException getWorkflowException(MapProject mapProject,
    String terminologyId) {

    final javax.persistence.Query query = manager
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

  /* see superclass */
  @Override
  public void createQAWork(Report report) throws Exception {

    if (report.getResults() == null || report.getResults().size() != 1) {
      throw new Exception(
          "Failed to provide a report with one result set " + report.getId());
    }

    Set<String> conceptIds = new HashSet<>();
    for (final ReportResultItem resultItem : report.getResults().get(0)
        .getReportResultItems()) {
      conceptIds.add(resultItem.getItemId());
    }

    // open the services
    ContentService contentService = new ContentServiceJpa();

    // get the map project and concept
    MapProject mapProject = getMapProject(report.getMapProjectId());

    // find the qa user
    MapUser mapUser = null;
    for (final MapUser user : getMapUsers().getMapUsers()) {
      if (user.getUserName().equals("qa"))
        mapUser = user;
    }

    for (final String conceptId : conceptIds) {

      final Concept concept = contentService.getConcept(conceptId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      final MapRecordList recordList =
          getMapRecordsForProjectAndConcept(mapProject.getId(), conceptId);
      // lazy initialize
      recordList.getMapRecords().size();

      for (final MapRecord mapRecord : recordList.getMapRecords()) {
        // set the label on the record
        mapRecord.addLabel(report.getReportDefinition().getName());

        // process the workflow action - only if published/READY
        if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
            || mapRecord.getWorkflowStatus()
                .equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
          processWorkflowAction(mapProject, concept, mapUser, mapRecord,
              WorkflowAction.CREATE_QA_RECORD);
        }
      }
    }

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

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("Processing workflow action by " + mapUser.getName() + ":  "
            + workflowAction.toString());
    if (mapRecord != null) {
      Logger.getLogger(WorkflowServiceJpa.class)
          .info("  Record attached: " + mapRecord.toString());
    }

    setTransactionPerOperation(true);

    // instantiate the algorithm handler for this project
    ProjectSpecificAlgorithmHandler algorithmHandler =
        (ProjectSpecificAlgorithmHandler) Class
            .forName(mapProject.getProjectSpecificAlgorithmHandlerClass())
            .newInstance();
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
      // do nothing (leave trackingRecord null)
    }

    // declare the validation result for existing tracking records
    ValidationResult result = null;

    // declare the workflow handler
    AbstractWorkflowPathHandler handler = null;

    // if no tracking record, create one (NOTE: Must be FIX_ERROR or QA
    // path)
    if (trackingRecord == null) {

      Logger.getLogger(this.getClass()).debug("Creating new tracking record");

      // create a tracking record for this concept with no records or
      // users
      trackingRecord = new TrackingRecordJpa();
      trackingRecord.setMapProjectId(mapProject.getId());
      trackingRecord.setTerminology(concept.getTerminology());
      trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
      trackingRecord.setTerminologyId(concept.getTerminologyId());
      trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());

      // get the tree positions for this concept and set the sort key //
      // to
      // the first retrieved
      final ContentService contentService = new ContentServiceJpa();
      try {
        TreePositionList treePositionsList = contentService
            .getTreePositionsWithDescendants(concept.getTerminologyId(),
                concept.getTerminology(), concept.getTerminologyVersion());

        // handle inactive concepts - which don't have tree positions
        if (treePositionsList.getCount() == 0) {
          trackingRecord.setSortKey("");
        } else {
          trackingRecord.setSortKey(
              treePositionsList.getTreePositions().get(0).getAncestorPath());
        }
      } catch (Exception e) {
        throw e;
      } finally {
        contentService.close();
      }

      // if Qa Path, instantiate Qa Path handler
      if (workflowAction.equals(WorkflowAction.CREATE_QA_RECORD)) {
        handler = (AbstractWorkflowPathHandler) Class
            .forName(ConfigUtility.getConfigProperties()
                .getProperty("workflow.path.handler.QA_PATH.class"))
            .newInstance();
        trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);
      }

      // otherwise, use Fix Error Path
      else {
        handler = (AbstractWorkflowPathHandler) Class
            .forName(ConfigUtility.getConfigProperties()
                .getProperty("workflow.path.handler.FIX_ERROR_PATH.class"))
            .newInstance();
        trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
      }

    }
    // otherwise, instantiate based on tracking record
    else {

      Logger.getLogger(WorkflowServiceJpa.class)
          .debug("  Tracking Record: " + trackingRecord.toString());

      handler =
          (AbstractWorkflowPathHandler) Class
              .forName(ConfigUtility.getConfigProperties()
                  .getProperty("workflow.path.handler."
                      + trackingRecord.getWorkflowPath() + ".class"))
              .newInstance();

    }

    if (handler == null) {
      throw new Exception("Could not determine workflow handler");
    }

    // validate the tracking record by its handler
    result = handler.validateTrackingRecordForActionAndUser(trackingRecord,
        workflowAction, mapUser);

    // validation only run on retrieved tracking records (not constructed
    // ones)
    if (result != null && !result.isValid()) {

      Logger.getLogger(WorkflowServiceJpa.class).info(result.toString());

      StringBuffer message = new StringBuffer();

      message.append("Errors were detected in the workflow for:\n");
      message.append("  Project\t: " + mapProject.getName() + "\n");
      message.append("  Concept\t: " + concept.getTerminologyId() + "\n");
      message.append(
          "  Path:\t " + trackingRecord.getWorkflowPath().toString() + "\n");
      message.append("  User\t: " + mapUser.getUserName() + "\n");
      message.append("  Action\t: " + workflowAction.toString() + "\n");

      message.append("\n");

      // record information
      message.append("Records involved:\n");
      message.append("  " + "id\tUser\tWorkflowStatus\n");

      for (final MapRecord mr : getMapRecordsForTrackingRecord(
          trackingRecord)) {
        message.append(
            "  " + mr.getId().toString() + "\t" + mr.getOwner().getUserName()
                + "\t" + mr.getWorkflowStatus().toString() + "\n");
      }
      message.append("\n");

      message.append("Errors reported:\n");

      for (final String error : result.getErrors()) {
        message.append("  " + error + "\n");
      }

      message.append("\n");

      // log the message
      Logger.getLogger(WorkflowServiceJpa.class)
          .error("Workflow error detected\n" + message.toString());

      // send email if indicated
      Properties config = ConfigUtility.getConfigProperties();

      String notificationRecipients =
          config.getProperty("send.notification.recipients");
      if (!notificationRecipients.isEmpty()) {

        String from;
        if (config.containsKey("mail.smtp.from")) {
          from = config.getProperty("mail.smtp.from");
        } else {
          from = config.getProperty("mail.smtp.user");
        }
        Properties props = new Properties();
        props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
        props.put("mail.smtp.password",
            config.getProperty("mail.smtp.password"));
        props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
        props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
        props.put("mail.smtp.starttls.enable",
            config.getProperty("mail.smtp.starttls.enable"));
        props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
        ConfigUtility.sendEmail(
            mapProject.getName() + " Workflow Error Alert, Concept "
                + concept.getTerminologyId(),
            from, notificationRecipients, message.toString(), props,
            "true".equals(config.getProperty("mail.smtp.auth")));
      }

      throw new LocalException("Workflow action " + workflowAction.toString()
          + " could not be performed on concept "
          + trackingRecord.getTerminologyId());
    }

    Set<MapRecord> mapRecords = getMapRecordsForTrackingRecord(trackingRecord);

    // if the record passed in updates an existing record, replace it in the
    // set
    if (mapRecord != null && mapRecord.getId() != null) {
      for (final MapRecord mr : mapRecords) {
        if (mr.getId().equals(mapRecord.getId())) {
          mapRecords.remove(mr);
          mapRecords.add(mapRecord);
          break;
        }
      }
    }

    // process the workflow action
    mapRecords = handler.processWorkflowAction(trackingRecord, workflowAction,
        mapProject, mapUser, mapRecords, mapRecord);

    Logger.getLogger(WorkflowServiceJpa.class).debug("Synchronizing...");

    // synchronize the map records via helper function
    Set<MapRecord> syncedRecords =
        synchronizeMapRecords(trackingRecord, mapRecords);

    // clear the pointer fields (i.e. ids and names of mapping
    // objects)
    trackingRecord.setMapRecordIds(new HashSet<Long>());
    trackingRecord.setAssignedUserNames(null);
    trackingRecord.setUserAndWorkflowStatusPairs(null);

    // recalculate the pointer fields
    for (MapRecord mr : syncedRecords) {
      trackingRecord.addMapRecordId(mr.getId());
      trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
      trackingRecord.addUserAndWorkflowStatusPair(mr.getOwner().getUserName(),
          mr.getWorkflowStatus().toString());
    }

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("Revised tracking record: " + trackingRecord.toString());

    // if the tracking record is ready for removal, delete it
    if ((getWorkflowStatusForTrackingRecord(trackingRecord)
        .equals(WorkflowStatus.READY_FOR_PUBLICATION)
        || getWorkflowStatusForTrackingRecord(trackingRecord)
            .equals(WorkflowStatus.PUBLISHED))
        && trackingRecord.getMapRecordIds().size() == 1) {

      Logger.getLogger(WorkflowServiceJpa.class)
          .debug("  Publication ready, removing tracking record.");
      removeTrackingRecord(trackingRecord.getId());

      // else add the tracking record if new
    } else if (trackingRecord.getId() == null) {
      Logger.getLogger(WorkflowServiceJpa.class)
          .debug("  New workflow concept, adding tracking record.");
      addTrackingRecord(trackingRecord);

      // otherwise update the tracking record
    } else {
      Logger.getLogger(WorkflowServiceJpa.class)
          .debug("  Still in workflow, updating tracking record.");
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
    for (final MapRecord mr : mapRecords) {
      Logger.getLogger(this.getClass()).debug("  Map record attached: "
          + mr.getId() + ", " + mr.getWorkflowStatus());

      manager.detach(mr);
      newRecords.add(mr);

      // ensure that all map records with ids are on the tracking record
      // NOTE: This was added after workflow refactoring to ensure that
      // FIX_ERROR_PATH and QA_PATH records were properly retrieved from
      // the database, despite the tracking record not yet
      // "containing" these records
      if (mr.getId() != null) {
        // map record ids is a set, simply add (no worry about
        // duplicates)
        trackingRecord.addMapRecordId(mr.getId());
      }
    }

    // retrieve the old (existing) records
    if (trackingRecord.getMapRecordIds() != null) {
      for (final Long id : trackingRecord.getMapRecordIds()) {
        MapRecord oldRecord = getMapRecord(id);
        oldRecords.add(oldRecord);
        Logger.getLogger(this.getClass())
            .debug("  Existing record retrieved: " + oldRecord.getId());
      }
    }

    // cycle over new records to check for additions or updates
    for (final MapRecord mr : newRecords) {

      Logger.getLogger(WorkflowServiceJpa.class)
          .debug("  Checking attached record: " + mr.getId());
      if (mr.getId() == null) {

        Logger.getLogger(WorkflowServiceJpa.class).debug("    Add record");
        // deep copy the detached record into a new
        // persistence-environment record
        // this routine also duplicates child collections to avoid
        // detached object errors
        MapRecord newRecord = new MapRecordJpa(mr, false);

        // add the record to the database
        addMapRecord(newRecord);

        // add the record to the return list
        syncedRecords.add(newRecord);

      }

      // otherwise, check for update
      else {
        // if the old map record is changed, update it

        if (!mr.isEquivalent(getMapRecordInSet(oldRecords, mr.getId()))) {
          Logger.getLogger(WorkflowServiceJpa.class).debug("    Update record");
          updateMapRecord(mr);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class)
              .debug("    Record unchanged");
        }

        syncedRecords.add(mr);
      }
    }

    // cycle over old records to check for deletions
    for (final MapRecord mr : oldRecords) {

      Logger.getLogger(this.getClass())
          .debug("  Checking for deleted records: " + mr.getId());
      // if old record is not in the new record set, delete it
      if (getMapRecordInSet(syncedRecords, mr.getId()) == null) {

        Logger.getLogger(WorkflowServiceJpa.class).debug("    Delete record");
        removeMapRecord(mr.getId());
      } else {
        Logger.getLogger(WorkflowServiceJpa.class).debug("    Record exists");
      }
    }

    return syncedRecords;

  }

  /**
   * Gets the map record in set.
   * 
   * @param mapRecords the map records
   * @param mapRecordId the map record id
   * @return the map record in set
   */
  @SuppressWarnings("static-method")
  private MapRecord getMapRecordInSet(Set<MapRecord> mapRecords,
    Long mapRecordId) {
    if (mapRecordId == null)
      return null;

    for (final MapRecord mr : mapRecords) {
      if (mapRecordId.equals(mr.getId()))
        return mr;
    }
    return null;
  }

  /* see superclass */
  @Override
  public void computeWorkflow(MapProject mapProject) throws Exception {

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("Start computing workflow for " + mapProject.getName());

    // set the transaction parameter and tracking variables
    setTransactionPerOperation(false);
    int commitCt = 1000;
    int trackingRecordCt = 0;

    // Clear the workflow for this project
    Logger.getLogger(WorkflowServiceJpa.class).info("  Clear old workflow");
    clearWorkflowForMapProject(mapProject);

    // open the services
    ContentService contentService = new ContentServiceJpa();

    String workflowPath;
    switch (mapProject.getWorkflowType()) {
      case CONFLICT_PROJECT:
        workflowPath = "NON_LEGACY_PATH";
        break;

      case REVIEW_PROJECT:
        workflowPath = "REVIEW_PROJECT_PATH";
        break;

      default:
        workflowPath = mapProject.getWorkflowType().toString();
        break;

    }

    // get the workflow handler for this project
    WorkflowPathHandler workflowHandler =
        this.getWorkflowPathHandler(workflowPath);

    // get the concepts in scope
    SearchResultList conceptsInScope =
        findConceptsInScope(mapProject.getId(), null);

    // construct a hashset of concepts in scope
    Set<String> conceptIds = new HashSet<>();
    for (final SearchResult sr : conceptsInScope.getIterable()) {
      conceptIds.add(sr.getTerminologyId());
    }

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("  Concept ids put into hash set: " + conceptIds.size());

    // get the current records
    MapRecordList mapRecords = getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(WorkflowServiceJpa.class).info(
        "Processing existing records (" + mapRecords.getCount() + " found)");

    // instantiate a mapped set of non-published records
    Map<String, List<MapRecord>> unpublishedRecords = new HashMap<>();

    // cycle over the map records, and remove concept ids if a map record is
    // publication-ready
    for (final MapRecord mapRecord : mapRecords.getIterable()) {

      // if this map record is not eligible for insertion into workflow
      // skip it and remove the`concept id from the set
      if (!workflowHandler.isMapRecordInWorkflow(mapRecord)) {
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
        .info("  Concepts with no publication-ready map record: "
            + conceptIds.size());
    Logger.getLogger(WorkflowServiceJpa.class)
        .info("  Concepts with unpublished map record content:  "
            + unpublishedRecords.size());

    beginTransaction();

    // if (workflowHandler.isEmptyWorkflowAllowed()) {
    //
    // }

    // construct the tracking records for unmapped concepts
    for (final String terminologyId : conceptIds) {

      // empty workflow check: no records for this id and empty workflow not
      // allowed
      if (!unpublishedRecords.containsKey(terminologyId)
          && !workflowHandler.isEmptyWorkflowAllowed()) {
        continue;
      }

      // retrieve the concept for this result
      Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // if concept could not be retrieved, throw exception
      if (concept == null) {
        throw new Exception("Failed to retrieve concept " + terminologyId);
      }

      // skip inactive concepts
      if (!concept.isActive()) {
        Logger.getLogger(WorkflowServiceJpa.class)
            .warn("Skipped inactive concept " + terminologyId);
        continue;
      }

      // bypass for integration tests
      String sortKey = "";
      if (concept.getLabel() == null
          || !concept.getLabel().equals("integration-test")) {
        // get the tree positions for this concept and set the sort key
        // to
        // the first retrieved
        TreePositionList treePositionsList =
            contentService.getTreePositions(concept.getTerminologyId(),
                concept.getTerminology(), concept.getTerminologyVersion());

        // if no tree position, throw exception
        if (treePositionsList.getCount() == 0) {
          throw new Exception(
              "Active concept " + terminologyId + " has no tree positions");
        }

        sortKey = treePositionsList.getTreePositions().get(0).getAncestorPath();
      }
      // create a workflow tracking record for this concept
      TrackingRecord trackingRecord = new TrackingRecordJpa();

      // populate the fields from project and concept
      trackingRecord.setMapProjectId(mapProject.getId());
      trackingRecord.setTerminology(concept.getTerminology());
      trackingRecord.setTerminologyId(concept.getTerminologyId());
      trackingRecord.setTerminologyVersion(concept.getTerminologyVersion());
      trackingRecord.setDefaultPreferredName(concept.getDefaultPreferredName());
      trackingRecord.setSortKey(sortKey);

      // add any existing map records to this tracking record
      Set<MapRecord> mapRecordsForTrackingRecord = new HashSet<>();
      if (unpublishedRecords.containsKey(trackingRecord.getTerminologyId())) {
        for (final MapRecord mr : unpublishedRecords
            .get(trackingRecord.getTerminologyId())) {
          Logger.getLogger(WorkflowServiceJpa.class).info(
              "    Adding existing map record " + mr.getId() + ", owned by "
                  + mr.getOwner().getUserName() + " to tracking record for "
                  + trackingRecord.getTerminologyId());

          trackingRecord.addMapRecordId(mr.getId());
          trackingRecord.addAssignedUserName(mr.getOwner().getUserName());
          trackingRecord.addUserAndWorkflowStatusPair(
              mr.getOwner().getUserName(), mr.getWorkflowStatus().toString());

          // add to the local set for workflow calculation
          mapRecordsForTrackingRecord.add(mr);
        }
      }

      // check against current workflow and universal workflows (currently Fix
      // Error and QA Paths)
      boolean isProjectPath =
          workflowHandler.isTrackingRecordInWorkflow(trackingRecord);
      boolean isFixErrorPath = (new WorkflowFixErrorPathHandler())
          .isTrackingRecordInWorkflow(trackingRecord);
      boolean isQaPath = (new WorkflowQaPathHandler())
          .isTrackingRecordInWorkflow(trackingRecord);

      // if is project path
      if (isProjectPath) {

        // check fix error not detected
        if (isFixErrorPath == true) {
          Logger.getLogger(getClass())
              .error("Skipping concept -- Workflow combination for concept "
                  + trackingRecord.getTerminologyId() + " matches both "
                  + workflowHandler.getName() + " and FIX_ERROR_PATH: "
                  + trackingRecord.getUserAndWorkflowStatusPairs());
          continue;
        }

        // check fix error and qa not detected
        else if (isQaPath == true) {
          Logger.getLogger(getClass())
              .error("Skipping concept -- Workflow combination for concept "
                  + trackingRecord.getTerminologyId() + " matches both "
                  + workflowHandler.getName() + " and QA_PATH: "
                  + trackingRecord.getUserAndWorkflowStatusPairs());
          continue;
        }

        // otherwise, set workflow type
        if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT))
          trackingRecord.setWorkflowPath(WorkflowPath.NON_LEGACY_PATH);
        else if (mapProject.getWorkflowType()
            .equals(WorkflowType.REVIEW_PROJECT))
          trackingRecord.setWorkflowPath(WorkflowPath.REVIEW_PROJECT_PATH);
        else {
          trackingRecord.setWorkflowPath(
              WorkflowPath.valueOf(mapProject.getWorkflowType().toString()));
        }
      }

      // otherwise, if Fix Error Path
      else if (isFixErrorPath == true) {
        trackingRecord.setWorkflowPath(WorkflowPath.FIX_ERROR_PATH);
      }

      // otherwise, if QA Path
      else if (isQaPath == true) {
        trackingRecord.setWorkflowPath(WorkflowPath.QA_PATH);
      }

      // otherwise, workflow combination not valid, skip and log
      else {
        Logger.getLogger(getClass())
            .error("Skipping concept -- Workflow combination for concept "
                + trackingRecord.getTerminologyId() + " not valid for any of"
                + workflowHandler.getName() + ", FIX_ERROR_PATH, or QA_PATH: "
                + trackingRecord.getUserAndWorkflowStatusPairs());
        continue;
      }

      addTrackingRecord(trackingRecord);

      if (++trackingRecordCt % commitCt == 0) {
        Logger.getLogger(WorkflowServiceJpa.class)
            .info("  " + trackingRecordCt + " tracking records created");
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
    Logger.getLogger(WorkflowServiceJpa.class)
        .info("  Creating indexes for TrackingRecordJpa");
    fullTextEntityManager.purgeAll(TrackingRecordJpa.class);
    fullTextEntityManager.flushToIndexes();
    fullTextEntityManager.createIndexer(TrackingRecordJpa.class)
        .batchSizeToLoadObjects(100).cacheMode(CacheMode.NORMAL)
        .threadsToLoadObjects(4).threadsForSubsequentFetching(8).startAndWait();

    Logger.getLogger(WorkflowServiceJpa.class).info("Done.");
  }

  /* see superclass */
  @Override
  public void clearWorkflowForMapProject(MapProject mapProject)
    throws Exception {

    int commitCt = 0;
    int commitInterval = 1000;

    // begin transaction not in transaction-per-operation mode
    if (!getTransactionPerOperation()) {
      beginTransaction();
    }

    for (final TrackingRecord tr : getTrackingRecordsForMapProject(mapProject)
        .getTrackingRecords()) {

      removeTrackingRecord(tr.getId());

      if (++commitCt % commitInterval == 0) {

        // if not a transaction for every operation, commit at intervals
        if (!getTransactionPerOperation()) {
          commit();
          beginTransaction();
        }

        Logger.getLogger(WorkflowServiceJpa.class)
            .info("  Removed " + commitCt + " tracking records");
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

  /* see superclass */
  @Override
  public Set<MapRecord> getMapRecordsForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    Set<MapRecord> mapRecords = new HashSet<>();
    if (trackingRecord != null && trackingRecord.getMapRecordIds() != null) {
      for (final Long id : trackingRecord.getMapRecordIds()) {
        mapRecords.add(getMapRecord(id));
      }
    }

    return mapRecords;
  }

  /* see superclass */
  @Override
  public MapUserList getMapUsersForTrackingRecord(TrackingRecord trackingRecord)
    throws Exception {
    return getMapUsersFromMapRecords(
        getMapRecordsForTrackingRecord(trackingRecord));
  }

  /* see superclass */
  @Override
  public WorkflowStatus getWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    return getWorkflowStatusFromMapRecords(
        getMapRecordsForTrackingRecord(trackingRecord));
  }

  /* see superclass */
  @Override
  public WorkflowStatus getLowestWorkflowStatusForTrackingRecord(
    TrackingRecord trackingRecord) throws Exception {
    return getLowestWorkflowStatusFromMapRecords(
        getMapRecordsForTrackingRecord(trackingRecord));
  }

  /* see superclass */
  @Override
  public MapUserList getMapUsersFromMapRecords(Set<MapRecord> mapRecords) {
    MapUserList mapUserList = new MapUserListJpa();
    for (final MapRecord mr : mapRecords) {
      mapUserList.addMapUser(mr.getOwner());
    }
    return mapUserList;
  }

  /* see superclass */
  @Override
  public WorkflowStatus getWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.NEW;
    for (final MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /* see superclass */
  @Override
  public WorkflowStatus getLowestWorkflowStatusFromMapRecords(
    Set<MapRecord> mapRecords) {
    WorkflowStatus workflowStatus = WorkflowStatus.REVISION;
    for (final MapRecord mr : mapRecords) {
      if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
        workflowStatus = mr.getWorkflowStatus();
    }
    return workflowStatus;
  }

  /* see superclass */
  @Override
  public List<String> computeWorkflowStatusErrors(MapProject mapProject)
    throws Exception {

    List<String> results = new ArrayList<>();

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("Retrieving tracking records for project " + mapProject.getId()
            + ", " + mapProject.getName());

    // get all the tracking records for this project
    TrackingRecordList trackingRecords =
        this.getTrackingRecordsForMapProject(mapProject);

    // construct a set of terminology ids for which a tracking record exists
    Set<String> terminologyIdsWithTrackingRecord = new HashSet<>();

    WorkflowPathHandler handler =
        getWorkflowPathHandlerForMapProject(mapProject);

    WorkflowPathHandler fixErrorHandler =
        this.getWorkflowPathHandler(WorkflowPath.FIX_ERROR_PATH.toString());

    WorkflowPathHandler qaPathHandler =
        this.getWorkflowPathHandler(WorkflowPath.QA_PATH.toString());

    for (final TrackingRecord trackingRecord : trackingRecords
        .getTrackingRecords()) {

      terminologyIdsWithTrackingRecord.add(trackingRecord.getTerminologyId());

      ValidationResult result = null;

      // check fix error path
      if (trackingRecord.getWorkflowPath()
          .equals(WorkflowPath.FIX_ERROR_PATH)) {
        result = fixErrorHandler.validateTrackingRecord(trackingRecord);
      }

      // check qa path
      else if (trackingRecord.getWorkflowPath().equals(WorkflowPath.QA_PATH)) {
        result = qaPathHandler.validateTrackingRecord(trackingRecord);
      }

      // otherwise, check against project handler
      else {
        result = handler.validateTrackingRecord(trackingRecord);
      }

      if (result == null) {
        result = new ValidationResultJpa();
        result.addError("Unexpected error -- validation result came back null");
      }

      if (!result.isValid()) {
        results.add(
            constructErrorMessageStringForTrackingRecordAndValidationResult(
                trackingRecord, result));
      }
    }
    Logger.getLogger(WorkflowServiceJpa.class)
        .info("  Checking map records for " + mapProject.getId() + ", "
            + mapProject.getName());

    // second, check all records for non-publication ready content without
    // tracking record, skip inactive concepts
    final ContentService contentService = new ContentServiceJpa();
    for (final MapRecord mapRecord : getMapRecordsForMapProject(
        mapProject.getId()).getMapRecords()) {

      // if eligible for workflow
      if (handler.isMapRecordInWorkflow(mapRecord)) {

        final Concept concept = contentService.getConcept(
            mapRecord.getConceptId(), mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());
        // if no tracking record found for this concept
        // and the concept is active, then report an error
        if (!terminologyIdsWithTrackingRecord.contains(mapRecord.getConceptId())
            && concept != null && concept.isActive()) {
          results.add("Map Record " + mapRecord.getId() + ": "
              + mapRecord.getWorkflowStatus()
              + " but no tracking record exists (Concept "
              + mapRecord.getConceptId() + " " + mapRecord.getConceptName());
        }
      }
    }
    contentService.close();
    return results;

  }

  /* see superclass */
  @Override
  public void computeUntrackedMapRecords(MapProject mapProject)
    throws Exception {

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("Retrieving map records for project " + mapProject.getId() + ", "
            + mapProject.getName());

    final MapRecordList mapRecordsInProject =
        getMapRecordsForMapProject(mapProject.getId());

    Logger.getLogger(WorkflowServiceJpa.class)
        .info("  " + mapRecordsInProject.getCount() + " retrieved");

    WorkflowPathHandler handler =
        getWorkflowPathHandler(mapProject.getWorkflowType().toString());

    // set the reporting interval based on number of tracking records
    int nObjects = 0;
    int nMessageInterval =
        (int) Math.floor(mapRecordsInProject.getCount() / 10);

    final Set<MapRecord> recordsUntracked = new HashSet<>();

    for (final MapRecord mr : mapRecordsInProject.getIterable()) {

      final TrackingRecord tr = this.getTrackingRecordForMapProjectAndConcept(
          mapProject, mr.getConceptId());

      // if no tracking record, check that this is a publication ready map
      // record
      if (tr == null) {
        if (handler.isMapRecordInWorkflow(mr)) {
          recordsUntracked.add(mr);
        }

      }

      if (++nObjects % nMessageInterval == 0) {
        Logger.getLogger(WorkflowServiceJpa.class)
            .info("  " + nObjects + " records processed, "
                + recordsUntracked.size()
                + " unpublished map records without tracking record");
      }
    }
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
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

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversation getFeedbackConversation(Long id)
    throws Exception {

    // construct query
    javax.persistence.Query query = manager.createQuery(
        "select m from FeedbackConversationJpa m where mapRecordId = :recordId");

    // Try query
    query.setParameter("recordId", id);
    List<FeedbackConversation> feedbackConversations = query.getResultList();

    if (feedbackConversations != null && feedbackConversations.size() > 0)
      handleFeedbackConversationLazyInitialization(
          feedbackConversations.get(0));

    Logger.getLogger(this.getClass())
        .debug("Returning feedback conversation id... "
            + ((feedbackConversations != null) ? id.toString() : "null"));

    return feedbackConversations != null && feedbackConversations.size() > 0
        ? feedbackConversations.get(0) : null;
  }

  /**
   * Handle feedback conversation lazy initialization.
   *
   * @param feedbackConversation the feedback conversation
   */
  @SuppressWarnings("static-method")
  private void handleFeedbackConversationLazyInitialization(
    FeedbackConversation feedbackConversation) {
    // handle all lazy initializations
    for (final Feedback feedback : feedbackConversation.getFeedbacks()) {
      feedback.getSender().getName();
      for (final MapUser recipient : feedback.getRecipients())
        recipient.getName();
      for (final MapUser viewedBy : feedback.getViewedBy())
        viewedBy.getName();
    }

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversationList getFeedbackConversationsForConcept(
    Long mapProjectId, String terminologyId) throws Exception {

    final MapProject mapProject = getMapProject(mapProjectId);
    final javax.persistence.Query query = manager
        .createQuery(
            "select m from FeedbackConversationJpa m where terminology = :terminology and"
                + " terminologyVersion = :terminologyVersion and terminologyId = :terminologyId")
        .setParameter("terminology", mapProject.getSourceTerminology())
        .setParameter("terminologyVersion",
            mapProject.getSourceTerminologyVersion())
        .setParameter("terminologyId", terminologyId);

    List<FeedbackConversation> feedbackConversations = query.getResultList();
    for (final FeedbackConversation feedbackConversation : feedbackConversations) {
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

  /* see superclass */
  @SuppressWarnings({
      "unchecked"
  })
  @Override
  public FeedbackConversationList findFeedbackConversationsForProject(
    Long mapProjectId, String userName, String query, PfsParameter pfsParameter)
    throws Exception {

    MapProject mapProject = null;
    mapProject = getMapProject(mapProjectId);

    String modifiedQuery = "";
    if (query.contains(" AND viewed:false"))
      modifiedQuery = query.replace(" AND viewed:false", "");
    else if (query.contains(" AND viewed:true"))
      modifiedQuery = query.replace(" AND viewed:true", "");
    else
      modifiedQuery = query;

    final StringBuilder sb = new StringBuilder();
    sb.append(modifiedQuery).append(" AND ");
    sb.append("mapProjectId:" + mapProject.getId());

    // remove from the query the viewed parameter, if it exists
    // viewed will be handled later because it is on the Feedback object,
    // not the FeedbackConversation object
    sb.append(" AND terminology:" + mapProject.getSourceTerminology()
        + " AND terminologyVersion:" + mapProject.getSourceTerminologyVersion()
        + " AND " + "( feedbacks.sender.userName:" + userName + " OR "
        + "feedbacks.recipients.userName:" + userName + ")");

    Logger.getLogger(getClass()).info("  query = " + sb.toString());

    final PfsParameter pfs = new PfsParameterJpa(pfsParameter);
    if (pfs.getSortField() == null || pfs.getSortField().isEmpty()) {
      pfs.setSortField("lastModified");
    }
    // Get all results if viewed is specified
    if (query.contains("viewed")) {
      pfs.setStartIndex(-1);
    }

    int[] totalCt = new int[1];
    final List<FeedbackConversation> feedbackConversations =
        (List<FeedbackConversation>) getQueryResults(sb.toString(),
            FeedbackConversationJpa.class, FeedbackConversationJpa.class, pfs,
            totalCt);

    if (pfsParameter != null && query.contains("viewed")) {

      // Handle viewed flag
      final List<FeedbackConversation> conversationsToKeep = new ArrayList<>();
      for (final FeedbackConversation fc : feedbackConversations) {
        if (query.contains("viewed:false")) {
          for (final Feedback feedback : fc.getFeedbacks()) {
            final Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
            boolean found = false;
            for (final MapUser user : alreadyViewedBy) {
              if (user.getUserName().equals(userName)) {
                found = true;
                break;
              }
            }
            // add if not found
            if (!found)
              conversationsToKeep.add(fc);
          }
        }
        if (query.contains("viewed:true")) {
          boolean found = false;
          for (final Feedback feedback : fc.getFeedbacks()) {
            Set<MapUser> alreadyViewedBy = feedback.getViewedBy();
            for (final MapUser user : alreadyViewedBy) {
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
      totalCt[0] = conversationsToKeep.size();
      feedbackConversations.clear();
      if (pfsParameter.getStartIndex() != -1) {
        for (int i =
            pfsParameter.getStartIndex(); i < pfsParameter.getStartIndex()
                + pfsParameter.getMaxResults()
                && i < conversationsToKeep.size(); i++) {
          feedbackConversations.add(conversationsToKeep.get(i));
        }
      } else {
        feedbackConversations.addAll(conversationsToKeep);
      }

    }

    Logger.getLogger(this.getClass())
        .debug(Integer.toString(feedbackConversations.size())
            + " feedbackConversations retrieved");

    for (final FeedbackConversation feedbackConversation : feedbackConversations) {
      handleFeedbackConversationLazyInitialization(feedbackConversation);
    }

    // set the total count
    FeedbackConversationListJpa feedbackConversationList =
        new FeedbackConversationListJpa();
    feedbackConversationList.setTotalCount(totalCt[0]);

    // extract the required sublist of feedback conversations
    feedbackConversationList.setFeedbackConversations(feedbackConversations);

    return feedbackConversationList;

  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public FeedbackConversationList getFeedbackConversationsForRecord(
    Long mapRecordId) throws Exception {

    javax.persistence.Query query = manager
        .createQuery(
            "select m from FeedbackConversationJpa m where mapRecordId=:mapRecordId")
        .setParameter("mapRecordId", mapRecordId);

    List<FeedbackConversation> feedbackConversations = query.getResultList();
    for (final FeedbackConversation feedbackConversation : feedbackConversations) {
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

  /* see superclass */
  @Override
  public FeedbackList getFeedbackErrorsForRecord(MapRecord mapRecord)
    throws Exception {

    List<Feedback> feedbacksWithError = new ArrayList<>();

    // find any feedback conersations for this record
    FeedbackConversationList conversations =
        this.getFeedbackConversationsForRecord(mapRecord.getId());

    // cycle over feedbacks
    for (final FeedbackConversation conversation : conversations
        .getIterable()) {
      for (final Feedback feedback : conversation.getFeedbacks()) {
        if (feedback.getIsError()) {
          feedbacksWithError.add(feedback);

        }

      }
    }
    FeedbackList feedbackList = new FeedbackListJpa();
    feedbackList.setFeedbacks(feedbacksWithError);
    return feedbackList;
  }

  /* see superclass */
  @Override
  public void sendFeedbackEmail(String name, String email, String conceptId,
    String conceptName, String refSetId, String message) throws Exception {
    // get to address from config.properties
    Properties config = ConfigUtility.getConfigProperties();
    String feedbackUserRecipient =
        config.getProperty("mail.smtp.to.feedback.user");
    String baseUrlWebapp = config.getProperty("base.url.webapp");
    String conceptUrl = baseUrlWebapp + "/#/record/conceptId/" + conceptId
        + "/autologin?refSetId=" + refSetId;

    String from;
    if (config.containsKey("mail.smtp.from")) {
      from = config.getProperty("mail.smtp.from");
    } else {
      from = config.getProperty("mail.smtp.user");
    }
    Properties props = new Properties();
    props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    props.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
    ConfigUtility.sendEmail(
        "Mapping Tool User Feedback: " + conceptId + "-" + conceptName, from,
        feedbackUserRecipient,
        "<html>User: " + name + "<br>" + "Email: " + email + "<br>"
            + "Concept: <a href=" + conceptUrl + ">" + conceptId + "- "
            + conceptName + "</a><br><br>" + message + "</html>",
        props, "true".equals(config.getProperty("mail.smtp.auth")));

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

    for (final MapRecord mr : getMapRecordsForTrackingRecord(trackingRecord)) {
      message.append(
          "    " + mr.getId().toString() + "\t" + mr.getOwner().getUserName()
              + "\t" + mr.getWorkflowStatus().toString() + "\n");
    }

    message.append("  Errors reported:\n");

    for (final String error : result.getErrors()) {
      message.append("    " + error + "\n");
    }

    return message.toString();
  }

  @Override
  public SearchResultList findAvailableWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter) throws Exception {

    WorkflowPathHandler handler = null;
    switch (mapProject.getWorkflowType()) {
      case CONFLICT_PROJECT:
        handler = new WorkflowNonLegacyPathHandler();
        break;
      case REVIEW_PROJECT:
        handler = new WorkflowReviewProjectPathHandler();
        break;
      default:
        handler = this
            .getWorkflowPathHandler(mapProject.getWorkflowType().toString());
        break;
    }

    if (handler == null) {
      throw new Exception(
          "Could not retrieve workflow handler for workflow type "
              + mapProject.getWorkflowType());
    }
    return handler.findAvailableWork(mapProject, mapUser, userRole, query,
        pfsParameter, this);
  }

  @Override
  public SearchResultList findAssignedWork(MapProject mapProject,
    MapUser mapUser, MapUserRole userRole, String query,
    PfsParameter pfsParameter) throws Exception {

    WorkflowPathHandler handler = null;

    // TODO Get rid of switch cases once workflow type and workflow path are
    // aligned
    switch (mapProject.getWorkflowType()) {
      case CONFLICT_PROJECT:
        handler = new WorkflowNonLegacyPathHandler();
        break;
      case REVIEW_PROJECT:
        handler = new WorkflowReviewProjectPathHandler();
        break;
      default:
        handler = this
            .getWorkflowPathHandler(mapProject.getWorkflowType().toString());
        break;
    }

    if (handler == null) {
      throw new Exception(
          "Could not retrieve workflow handler for workflow type "
              + mapProject.getWorkflowType());
    }
    return handler.findAssignedWork(mapProject, mapUser, userRole, query,
        pfsParameter, this);
  }

  /* see superclass */
  @Override
  public MapRecord getPreviouslyPublishedVersionOfMapRecord(MapRecord mapRecord)
    throws Exception {

    // get the record revisions
    final List<MapRecord> revisions =
        getMapRecordRevisions(mapRecord.getId()).getMapRecords();

    // ensure revisions are sorted by descending timestamp
    Collections.sort(revisions, new Comparator<MapRecord>() {
      @Override
      public int compare(MapRecord mr1, MapRecord mr2) {
        return mr2.getLastModified().compareTo(mr1.getLastModified());
      }
    });

    // check assumption: last revision exists, at least two records must be
    // present
    if (revisions.size() < 2) {
      throw new Exception(
          "Attempted to get the previously published version of map record with id "
              + mapRecord.getId() + ", " + mapRecord.getOwner().getName()
              + ", and concept id " + mapRecord.getConceptId()
              + ", but no previous revisions exist.");
    }

    // cycle over records until the previously
    // published/ready-for-publication
    // state record is found
    for (final MapRecord revision : revisions) {
      if (revision.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
          || revision.getWorkflowStatus()
              .equals(WorkflowStatus.READY_FOR_PUBLICATION)) {

        return revision;
      }
    }

    throw new Exception(
        "Could not retrieve previously published state of map record for concept "
            + mapRecord.getConceptId() + ", " + mapRecord.getConceptName());

  }

  @Override
  public WorkflowPathHandler getWorkflowPathHandler(String name)
    throws Exception {
    String handlerClass = ConfigUtility.getConfigProperties()
        .getProperty("workflow.path.handler." + name + ".class");

    return (WorkflowPathHandler) Class.forName(handlerClass).newInstance();
  }

  @Override
  public WorkflowPathHandler getWorkflowPathHandlerForMapProject(
    MapProject mapProject) throws Exception {
    // TODO Remove the switch statement once conflict/review project types are
    // normalized with workflow
    switch (mapProject.getWorkflowType()) {
      case CONFLICT_PROJECT:
        return getWorkflowPathHandler("NON_LEGACY_PATH");
      case REVIEW_PROJECT:
        return getWorkflowPathHandler("REVIEW_PROJECT_PATH");
      default:
        return getWorkflowPathHandler(mapProject.getWorkflowType().toString());

    }
  }
}
