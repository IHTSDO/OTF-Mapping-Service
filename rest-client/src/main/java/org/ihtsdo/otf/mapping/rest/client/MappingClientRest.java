package org.ihtsdo.otf.mapping.rest.client;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.MappingServiceRest;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;

public class MappingClientRest extends RootClientRest
    implements MappingServiceRest {

  /** The config. */
  private Properties config = null;

  /**
   * Instantiates a {@link MappingClientRest} from the specified parameters.
   *
   * @param config the config
   */
  public MappingClientRest(Properties config) {
    this.config = config;
  }

  @Override
  public MapProjectListJpa getMapProjects(String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapProject getMapProject(Long mapProjectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapProject addMapProject(MapProjectJpa mapProject, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapProject(MapProjectJpa mapProject, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapProject(MapProjectJpa mapProject, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapProject cloneMapProject(MapProjectJpa mapProject, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public SearchResultList findMapProjectsForQuery(String query,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapProjectListJpa getMapProjectsForUser(String mapUserName,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapUserListJpa getMapUsers(String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public SearchResultList getScopeConceptsForMapProject(Long projectId,
    PfsParameterJpa pfsParameter, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult addScopeConceptsToMapProject(
    List<String> terminologyIds, Long projectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult removeScopeConceptFromMapProject(String terminologyId,
    Long projectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult removeScopeConceptsFromMapProject(
    List<String> terminologyIds, Long projectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public SearchResultList getScopeExcludedConceptsForMapProject(Long projectId,
    PfsParameterJpa pfsParameter, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult addScopeExcludedConceptsToMapProject(
    List<String> terminologyIds, Long projectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult removeScopeExcludedConceptFromMapProject(
    String terminologyId, Long projectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult removeScopeExcludedConceptsFromMapProject(
    List<String> terminologyIds, Long projectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapUser getMapUser(String mapUserName, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapUser addMapUser(MapUserJpa mapUser, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapUser(MapUserJpa mapUser, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapUser(MapUserJpa mapUser, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapAdviceListJpa getMapAdvices(String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapAdvice addMapAdvice(MapAdviceJpa mapAdvice, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapAdvice(MapAdviceJpa mapAdvice, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapAdvice(MapAdviceJpa mapAdvice, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapAgeRangeListJpa getMapAgeRanges(String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapAgeRange addMapAgeRange(MapAgeRangeJpa mapAgeRange,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapAgeRange(MapAgeRangeJpa mapAgeRange, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapAgeRange(MapAgeRangeJpa mapAgeRange, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapRelationListJpa getMapRelations(String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRelation addMapRelation(MapRelationJpa mapRelation,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapRelation(MapRelationJpa mapRelation, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapRelation(MapRelationJpa mapRelation, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapPrincipleListJpa getMapPrinciples(String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapPrinciple getMapPrinciple(Long mapPrincipleId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapPrinciple addMapPrinciple(MapPrincipleJpa mapPrinciple,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapPrinciple(MapPrincipleJpa mapPrinciple, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public void removeMapPrinciple(MapPrincipleJpa principle, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public MapUserPreferences getMapUserPreferences(String username,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapUserPreferences addMapUserPreferences(
    MapUserPreferencesJpa mapUserPreferences, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapUserPreferences(MapUserPreferencesJpa mapUserPreferences,
    String authToken) throws Exception {
    // N/A

  }

  @Override
  public void removeMapUserPreferences(MapUserPreferencesJpa mapUserPreferences,
    String authToken) throws Exception {
    // N/A

  }

  @Override
  public MapRecord getMapRecord(Long mapRecordId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecord addMapRecord(MapRecordJpa mapRecord, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void updateMapRecord(MapRecordJpa mapRecord, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public Response removeMapRecord(MapRecordJpa mapRecord, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(
    List<String> terminologyIds, Long projectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordListJpa getMapRecordsForConceptId(String conceptId,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordListJpa getMapRecordsForConceptIdHistorical(String conceptId,
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }
  
  @Override
  public MapUserListJpa getMapRecordsForConceptIdHistoricalMapUsers(String conceptId,
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordListJpa getMapRecordsForMapProjectAndQuery(Long mapProjectId,
    PfsParameterJpa pfsParameter, String ancestorId, String relationshipName, String relationshipValue, 
    boolean excludeDescendants, String query, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordListJpa getPublishedMapRecordsForMapProject(Long mapProjectId,
    PfsParameterJpa pfsParameter, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordList getMapRecordRevisions(Long mapRecordId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecord getMapRecordHistorical(Long mapRecordId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRelation computeMapRelation(MapRecordJpa mapRecord,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapAdviceList computeMapAdvice(Integer entryIndex,
    MapRecordJpa mapRecord, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapUserRole getMapUserRoleForMapProject(String username,
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public SearchResultList getUnmappedDescendantsForConcept(String terminologyId,
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public TreePositionList getTreePositionWithDescendantsForConceptAndMapProject(
    String terminologyId, Long mapProjectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }
  
  @Override
  public TreePositionList getSourceTreePositionWithDescendantsForConceptAndMapProject(
    String terminologyId, Long mapProjectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public TreePositionList getDestinationRootTreePositionsForMapProject(
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public TreePositionList getSourceRootTreePositionsForMapProject(
    Long mapProjectId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public TreePositionList getTreePositionGraphsForQueryAndMapProject(
    String query, Long mapProjectId, PfsParameterJpa pfsParameter,
    String authToken) throws Exception {
    // N/A
    return null;
  }
  
  public TreePositionList getSourceTreePositionGraphsForQueryAndMapProject(
	String query, Long mapProjectId, PfsParameterJpa pfsParameter,
	String authToken) throws Exception {
	// N/A
	return null;
  }

  @Override
  public MapRecordListJpa getMapRecordsEditedByMapUser(Long mapProjectId,
    String username, PfsParameterJpa pfsParameter, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public MapRecordList getOriginMapRecordsForConflict(Long mapRecordId,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult validateMapRecord(MapRecordJpa mapRecord,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public ValidationResult compareMapRecords(Long mapRecordId1,
    Long mapRecordId2, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public Concept isTargetCodeValid(Long mapProjectId, String terminologyId,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public String uploadMappingHandbookFile(InputStream fileInputStream,
    FormDataContentDisposition contentDispositionHeader, Long mapProjectId,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public KeyValuePairLists getMapProjectMetadata(String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public KeyValuePairList getAllTerminologyNotes(Long mapProjectId,
    String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public void computeDefaultPreferredNames(Long mapProjectId, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public String beginReleaseForMapProject(String effectiveTime, Long mapProjectId,
    String authToken) throws Exception {
    // N/A
    return null;
  }


  @Override
  public String finishReleaseForMapProject(boolean testModeFlag,
    Long mapProjectId, String effectiveTime, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public void startEditingCycleForMapProject(Long mapProjectId,
    String authToken) throws Exception {
    // N/A

  }

  @Override
  public void createJiraIssue(String conceptId, String conceptAuthor,
    String messageText, MapRecordJpa mapRecord, String authToken)
    throws Exception {
    // N/A

  }

  @Override
  public String getReleaseFileNames(Long mapProjectId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public String getLog(String projectId, List<String> logTypes, String query,
    String authToken) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public SearchResultList getConceptAuthors(String conceptId, String authToken)
    throws Exception {
    // N/A
    return null;
  }

  @Override
  public SearchResultList getConceptAuthoringChanges(String projectId,
    String conceptId, String authToken) throws Exception {
    // N/A
    return null;
  }

  @Override
  public String processReleaseForMapProject(String moduleId,
          String effectiveTime, Long mapProjectId, boolean writeDelta,
          String authToken) throws Exception {
      // TODO Auto-generated method stub
    return null;
      
  }

  @Override
  public SearchResultList getReleaseReportList(Long mapProjectId,
          String authToken) throws Exception {
      // TODO Auto-generated method stub
      return null;
  }

  @Override
  public SearchResultList getFileListFromAmazonS3(Long mapProjectId,
          String authToken) throws Exception {
      // TODO Auto-generated method stub
      return null;
  }

  @Override
  public SearchResultList getCurrentReleaseFileName(Long mapProjectId,
          String authToken) throws Exception {
      // TODO Auto-generated method stub
      return null;
  }

  @Override
  public InputStream downloadFileFromAmazonS3(String filePath, String authToken)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream downloadCurrentReleaseFile(String fileName,
    Long mapProjectId, String authToken) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MapUserRole getMapUserRoleForApplication(String username,
    String authToken) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }
  
}
