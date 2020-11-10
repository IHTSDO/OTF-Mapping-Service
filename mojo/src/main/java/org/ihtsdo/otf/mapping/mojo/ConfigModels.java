package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Nuno Marques
 *
 */
public class ConfigModels {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MapProjectConfiguration {
    private String destinationTerminology;
    private String destinationTerminologyVersion;
    private Boolean groupStructure;
    private String mapRefsetPattern;
    private String name;
    private String projectSpecificAlgorithmHandlerClass;
    private Boolean propagatedFlag;
    private Boolean isPublic;
    private Boolean isTeamBased;
    private String refSetId;
    private String refSetName;
    private String sourceTerminology;
    private String sourceTerminologyVersion;
    private String workflowType;
    private String mapRelationStyle;
    private Boolean scopeDescendantsFlag;
    private String dateFormat;
    private String editingCycleBeginDate;
    private Set<String> includeScopeConcepts = new HashSet<>();
    private Set<String> excludeScopeConcepts = new HashSet<>();
    private Set<String> reports = new HashSet<>();
    private Set<String> leads = new HashSet<>();
    private Set<String> specialists = new HashSet<>();
    private Set<String> errorMessages = new HashSet<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MapProjectConfiguration() {
    }

    /**
     * 
     * @param dateFormat
     * @param destinationTerminology
     * @param destinationTerminologyVersion
     * @param editingCycleBeginDate
     * @param groupStructure
     * @param mapRefsetPattern
     * @param name
     * @param projectSpecificAlgorithmHandlerClass
     * @param propagatedFlag
     * @param isPublic
     * @param isTeamBased
     * @param refSetId
     * @param refSetName
     * @param sourceTerminology
     * @param sourceTerminologyVersion
     * @param workflowType
     * @param mapRelationStyle
     * @param scopeDescendantsFlag
     * @param scopeConcepts
     * @param reports
     * @param leads
     * @param specialists
     */
    @JsonCreator
    public MapProjectConfiguration(
        @JsonProperty("dateFormat") String dateFormat,
        @JsonProperty("destinationTerminology") String destinationTerminology,
        @JsonProperty("destinationTerminologyVersion") String destinationTerminologyVersion,
        @JsonProperty("editingCycleBeginDate") String editingCycleBeginDate,
        @JsonProperty("groupStructure") Boolean groupStructure,
        @JsonProperty("mapRefsetPattern") String mapRefsetPattern,
        @JsonProperty("name") String name,
        @JsonProperty("projectSpecificAlgorithmHandlerClass") String projectSpecificAlgorithmHandlerClass,
        @JsonProperty("propagatedFlag") Boolean propagatedFlag,
        @JsonProperty("isPublic") Boolean isPublic,
        @JsonProperty("isTeamBased") Boolean isTeamBased,
        @JsonProperty("refSetId") String refSetId,
        @JsonProperty("refSetName") String refSetName,
        @JsonProperty("sourceTerminology") String sourceTerminology,
        @JsonProperty("sourceTerminologyVersion") String sourceTerminologyVersion,
        @JsonProperty("workflowType") String workflowType,
        @JsonProperty("mapRelationStyle") String mapRelationStyle,
        @JsonProperty("scopeDescendantsFlag") Boolean scopeDescendantsFlag,
        @JsonProperty("includeScopeConcepts") Set<String> includeScopeConcepts,
        @JsonProperty("excludeScopeConcepts") Set<String> excludeScopeConcepts,
        @JsonProperty("reports") Set<String> reports,
        @JsonProperty("leads") Set<String> leads,
        @JsonProperty("specialists") Set<String> specialists,
        @JsonProperty("errorMessages") Set<String> errorMessages) {
      super();
      this.dateFormat = dateFormat;
      this.destinationTerminology = destinationTerminology;
      this.destinationTerminologyVersion = destinationTerminologyVersion;
      this.editingCycleBeginDate = editingCycleBeginDate;
      this.groupStructure = groupStructure;
      this.mapRefsetPattern = mapRefsetPattern;
      this.name = name;
      this.projectSpecificAlgorithmHandlerClass = projectSpecificAlgorithmHandlerClass;
      this.propagatedFlag = propagatedFlag;
      this.isPublic = isPublic;
      this.isTeamBased = isTeamBased;
      this.refSetId = refSetId;
      this.refSetName = refSetName;
      this.sourceTerminology = sourceTerminology;
      this.sourceTerminologyVersion = sourceTerminologyVersion;
      this.workflowType = workflowType;
      this.mapRelationStyle = mapRelationStyle;
      this.scopeDescendantsFlag = scopeDescendantsFlag;
      this.includeScopeConcepts = includeScopeConcepts;
      this.excludeScopeConcepts = excludeScopeConcepts;
      this.reports = reports;
      this.leads = leads;
      this.specialists = specialists;
      this.errorMessages = errorMessages;
    }

    public String getDateFormat() {
      return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
    }

    public String getDestinationTerminology() {
      return destinationTerminology;
    }

    public void setDestinationTerminology(String destinationTerminology) {
      this.destinationTerminology = destinationTerminology;
    }

    public String getDestinationTerminologyVersion() {
      return destinationTerminologyVersion;
    }

    public void setDestinationTerminologyVersion(
        String destinationTerminologyVersion) {
      this.destinationTerminologyVersion = destinationTerminologyVersion;
    }

    public String getEditingCycleBeginDate() {
      return editingCycleBeginDate;
    }

    public void setEditingCycleBeginDate(String editingCycleBeginDate) {
      this.editingCycleBeginDate = editingCycleBeginDate;
    }

    public Boolean getGroupStructure() {
      return groupStructure;
    }

    public void setGroupStructure(Boolean groupStructure) {
      this.groupStructure = groupStructure;
    }

    public String getMapRefsetPattern() {
      return mapRefsetPattern;
    }

    public void setMapRefsetPattern(String mapRefsetPattern) {
      this.mapRefsetPattern = mapRefsetPattern;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getProjectSpecificAlgorithmHandlerClass() {
      return projectSpecificAlgorithmHandlerClass;
    }

    public void setProjectSpecificAlgorithmHandlerClass(
        String projectSpecificAlgorithmHandlerClass) {
      this.projectSpecificAlgorithmHandlerClass = projectSpecificAlgorithmHandlerClass;
    }

    public Boolean getPropagatedFlag() {
      return propagatedFlag;
    }

    public void setPropagatedFlag(Boolean propagatedFlag) {
      this.propagatedFlag = propagatedFlag;
    }

    public Boolean getIsPublic() {
      return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
      this.isPublic = isPublic;
    }

    public Boolean getIsTeamBased() {
      return isTeamBased;
    }

    public void setIsTeamBased(Boolean isTeamBased) {
      this.isTeamBased = isTeamBased;
    }

    public String getRefSetId() {
      return refSetId;
    }

    public void setRefSetId(String refSetId) {
      this.refSetId = refSetId;
    }

    public String getRefSetName() {
      return refSetName;
    }

    public void setRefSetName(String refSetName) {
      this.refSetName = refSetName;
    }

    public String getSourceTerminology() {
      return sourceTerminology;
    }

    public void setSourceTerminology(String sourceTerminology) {
      this.sourceTerminology = sourceTerminology;
    }

    public String getSourceTerminologyVersion() {
      return sourceTerminologyVersion;
    }

    public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
      this.sourceTerminologyVersion = sourceTerminologyVersion;
    }

    public String getWorkflowType() {
      return workflowType;
    }

    public void setWorkflowType(String workflowType) {
      this.workflowType = workflowType;
    }

    public String getMapRelationStyle() {
      return mapRelationStyle;
    }

    public void setMapRelationStyle(String mapRelationStyle) {
      this.mapRelationStyle = mapRelationStyle;
    }

    public Boolean getScopeDescendantsFlag() {
      return scopeDescendantsFlag;
    }

    public void setScopeDescendantsFlag(Boolean scopeDescendantsFlag) {
      this.scopeDescendantsFlag = scopeDescendantsFlag;
    }

    public Set<String> getIncludeScopeConcepts() {
      return includeScopeConcepts;
    }

    public void setIncludeScopeConcepts(Set<String> includeScopeConcepts) {
      this.includeScopeConcepts = includeScopeConcepts;
    }

    public Set<String> getExcludeScopeConcepts() {
      return excludeScopeConcepts;
    }

    public void setExcludeScopeConcepts(Set<String> excludeScopeConcepts) {
      this.excludeScopeConcepts = excludeScopeConcepts;
    }

    public Set<String> getReports() {
      return reports;
    }

    public void setReports(Set<String> reports) {
      this.reports = reports;
    }

    public Set<String> getLeads() {
      return leads;
    }

    public void setLeads(Set<String> leads) {
      this.leads = leads;
    }

    public Set<String> getSpecialists() {
      return specialists;
    }

    public void setSpecialists(Set<String> specialists) {
      this.specialists = specialists;
    }

    public Set<String> getErrorMessages() {
      return errorMessages;
    }

    public void setErrorMessages(Set<String> errorMessages) {
      this.errorMessages = errorMessages;
    }

  }

}
