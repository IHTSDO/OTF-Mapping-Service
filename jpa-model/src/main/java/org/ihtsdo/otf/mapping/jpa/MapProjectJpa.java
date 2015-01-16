package org.ihtsdo.otf.mapping.jpa;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JPA enabled implementation of {@link MapProject}.
 */
@Entity
@Table(name = "map_projects", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
      "name"
    })
  })
@Audited
@Indexed
@XmlRootElement(name = "mapProject")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapProjectJpa implements MapProject {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** Whether this project is viewable by public roles. */
  @Column(unique = false, nullable = false)
  private boolean isPublic = false;

  /**
   * Indicates whether there is group structure for map records of this project.
   */
  @Column(unique = false, nullable = false)
  private boolean groupStructure = false;

  /** Indicates if the map project has been published. */
  @Column(unique = false, nullable = false)
  private boolean published = false;

  /**
   * Indicates what type of workflow to use for this project, defaults to
   * conflict review.
   */
  @Enumerated(EnumType.STRING)
  private WorkflowType workflowType = null;

  /** The ref set id. */
  private String refSetId;

  /** The ref set name. */
  private String refSetName;

  /**  The editing cycle begin date. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = true)
  private Date editingCycleBeginDate;

  /**  The latest publication date. */
  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = true)
  private Date latestPublicationDate;

  /** The source terminology. */
  @Column(nullable = false)
  private String sourceTerminology;

  /** The source terminology version. */
  @Column(nullable = false)
  private String sourceTerminologyVersion;

  /** The destination terminology. */
  @Column(nullable = false)
  private String destinationTerminology;

  /** The destination terminology version. */
  @Column(nullable = false)
  private String destinationTerminologyVersion;

  /** The RF2 refset pattern for this map project. */
  @Enumerated(EnumType.STRING)
  private MapRefsetPattern mapRefsetPattern = null;

  /** The relation behavior. */
  @Enumerated(EnumType.STRING)
  private RelationStyle mapRelationStyle = null;

  /** The mapping principle document name. */
  @Column(nullable = true)
  private String mapPrincipleSourceDocumentName;

  /** The mapping principle document. */
  @Column(nullable = true)
  private String mapPrincipleSourceDocument;

  /** Flag for whether this project is rule based. */
  @Column(nullable = false)
  private boolean ruleBased;

  /** Name of the handler for project-specific algorithms. */
  @Column(nullable = true)
  private String projectSpecificAlgorithmHandlerClass;

  /** The algorithm handler. */
  @Transient
  private ProjectSpecificAlgorithmHandler algorithmHandler;

  /** The preset age ranges. */
  @ManyToMany(targetEntity = MapAgeRangeJpa.class, fetch = FetchType.LAZY)
  private Set<MapAgeRange> presetAgeRanges = new HashSet<>();

  /** The map leads. */
  @ManyToMany(targetEntity = MapUserJpa.class, fetch = FetchType.LAZY)
  @JoinTable(name = "map_projects_map_leads", joinColumns = @JoinColumn(name = "map_projects_id"), inverseJoinColumns = @JoinColumn(name = "map_users_id"))
  @IndexedEmbedded(targetElement = MapUserJpa.class)
  private Set<MapUser> mapLeads = new HashSet<>();

  /** The map specialists. */
  @ManyToMany(targetEntity = MapUserJpa.class, fetch = FetchType.LAZY)
  @JoinTable(name = "map_projects_map_specialists", joinColumns = @JoinColumn(name = "map_projects_id"), inverseJoinColumns = @JoinColumn(name = "map_users_id"))
  @IndexedEmbedded(targetElement = MapUserJpa.class)
  private Set<MapUser> mapSpecialists = new HashSet<>();

  /** The allowable map principles for this MapProject. */
  @ManyToMany(targetEntity = MapPrincipleJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = MapPrincipleJpa.class)
  private Set<MapPrinciple> mapPrinciples = new HashSet<>();

  /** The allowable map advices for this MapProject. */
  @ManyToMany(targetEntity = MapAdviceJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = MapAdviceJpa.class)
  private Set<MapAdvice> mapAdvices = new HashSet<>();

  /** The allowable map relations for this MapProject. */
  @ManyToMany(targetEntity = MapRelationJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = MapRelationJpa.class)
  private Set<MapRelation> mapRelations = new HashSet<>();

  /** The allowable report definitions for this MapProject. */
  @ManyToMany(targetEntity = ReportDefinitionJpa.class, fetch = FetchType.LAZY)
  @IndexedEmbedded(targetElement = ReportDefinitionJpa.class)
  private Set<ReportDefinition> reportDefinitions = new HashSet<>();

  /** The concepts in scope for this project. */
  @ElementCollection
  @CollectionTable(name = "map_projects_scope_concepts", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Set<String> scopeConcepts = new HashSet<>();

  /** The concepts excluded from scope of this project. */
  @ElementCollection
  @CollectionTable(name = "map_projects_scope_excluded_concepts", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Set<String> scopeExcludedConcepts = new HashSet<>();

  /** Indicates if descendants of the scope are included in the scope. */
  @Column(unique = false, nullable = false)
  private boolean scopeDescendantsFlag = false;

  /**
   * Indicates if descendants of the excluded scope are excluded from the scope.
   */
  @Column(unique = false, nullable = false)
  private boolean scopeExcludedDescendantsFlag = false;

  /** The error messages allowed for this project. */
  @ElementCollection
  @CollectionTable(name = "map_projects_error_messages", joinColumns = @JoinColumn(name = "id"))
  @Column(nullable = true)
  private Set<String> errorMessages = new HashSet<>();

  /** The propagated flag. */
  @Column(unique = false, nullable = false)
  private boolean propagatedFlag = false;

  /** The propagation descendant threshold. */
  @Column(nullable = true)
  private Integer propagationDescendantThreshold;

  /**
   * Default constructor.
   */
  public MapProjectJpa() {
  }

  

  /**
   * Instantiates a {@link MapProjectJpa} from the specified parameters.
   *
   * @param project the project
   */
  public MapProjectJpa(MapProject project) {
    super();
    this.id = project.getId();
    this.name = project.getName();
    this.isPublic = project.isPublic();
    this.groupStructure = project.isGroupStructure();
    this.published = project.isPublished();
    this.refSetId = project.getRefSetId();
    this.refSetName = project.getRefSetName();
    this.sourceTerminology = project.getSourceTerminology();
    this.sourceTerminologyVersion = project.getSourceTerminologyVersion();
    this.destinationTerminology = project.getDestinationTerminology();
    this.destinationTerminologyVersion =
        project.getDestinationTerminologyVersion();
    this.mapRefsetPattern = project.getMapRefsetPattern();
    this.mapRelationStyle = project.getMapRelationStyle();
    this.mapPrincipleSourceDocument = project.getMapPrincipleSourceDocument();
    this.mapPrincipleSourceDocumentName =
        project.getMapPrincipleSourceDocumentName();
    this.ruleBased = project.isRuleBased();
    this.projectSpecificAlgorithmHandlerClass =
        project.getProjectSpecificAlgorithmHandlerClass();
    // this.algorithmHandler = project.algorithmHandler;
    this.presetAgeRanges = project.getPresetAgeRanges();
    this.mapLeads = project.getMapLeads();
    this.mapSpecialists = project.getMapSpecialists();
    this.mapPrinciples = project.getMapPrinciples();
    this.mapAdvices = project.getMapAdvices();
    this.mapRelations = project.getMapRelations();
    this.scopeConcepts = project.getScopeConcepts();
    this.scopeExcludedConcepts = project.getScopeExcludedConcepts();
    this.scopeDescendantsFlag = project.isScopeDescendantsFlag();
    this.scopeExcludedDescendantsFlag =
        project.isScopeExcludedDescendantsFlag();
    this.errorMessages = project.getErrorMessages();
    this.propagatedFlag = project.isPropagatedFlag();
    this.propagationDescendantThreshold =
        project.getPropagationDescendantThreshold();
    this.workflowType = project.getWorkflowType();
    this.latestPublicationDate = project.getLatestPublicationDate();
    this.editingCycleBeginDate = project.getEditingCycleBeginDate();
  }

  /**
   * Return the id.
   * 
   * @return the id
   */
  @Override
  public Long getId() {
    return this.id;
  }

  /**
   * Set the id.
   * 
   * @param id the id
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the id in string form.
   * 
   * @return the id in string form
   */
  @XmlID
  @Override
  public String getObjectId() {
    return id.toString();
  }

  /**
   * Required for .
   * 
   * @param objectId the object id
   */
  public void setObjectId(String objectId) {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "mapLead")
  public Set<MapUser> getMapLeads() {
    return mapLeads;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
   */
  @Override
  public void setMapLeads(Set<MapUser> mapLeads) {
    this.mapLeads = mapLeads;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addMapLead(org.ihtsdo.otf.mapping
   * .model.MapLead)
   */
  @Override
  public void addMapLead(MapUser mapLead) {
    mapLeads.add(mapLead);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeMapLead(org.ihtsdo.otf.mapping
   * .model.MapLead)
   */
  @Override
  public void removeMapLead(MapUser mapLead) {
    mapLeads.remove(mapLead);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
   */
  @Override
  @XmlElement(type = MapUserJpa.class, name = "mapSpecialist")
  public Set<MapUser> getMapSpecialists() {
    return mapSpecialists;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setMapSpecialists(java.util.Set)
   */
  @Override
  public void setMapSpecialists(Set<MapUser> mapSpecialists) {
    this.mapSpecialists = mapSpecialists;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addMapSpecialist(org.ihtsdo.otf
   * .mapping.model.MapSpecialist)
   */
  @Override
  public void addMapSpecialist(MapUser mapSpecialist) {
    mapSpecialists.add(mapSpecialist);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeMapSpecialist(org.ihtsdo.
   * otf.mapping.model.MapSpecialist)
   */
  @Override
  public void removeMapSpecialist(MapUser mapSpecialist) {
    mapSpecialists.remove(mapSpecialist);
  }


  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminology()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getSourceTerminology() {
    return sourceTerminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminology(java.lang.
   * String)
   */
  @Override
  public void setSourceTerminology(String sourceTerminology) {
    this.sourceTerminology = sourceTerminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminology()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getDestinationTerminology() {
    return destinationTerminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminology(java.
   * lang.String)
   */
  @Override
  public void setDestinationTerminology(String destinationTerminology) {
    this.destinationTerminology = destinationTerminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminologyVersion()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getSourceTerminologyVersion() {
    return sourceTerminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminologyVersion(java
   * .lang.String)
   */
  @Override
  public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
    this.sourceTerminologyVersion = sourceTerminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminologyVersion()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getDestinationTerminologyVersion() {
    return destinationTerminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminologyVersion
   * (java.lang.String)
   */
  @Override
  public void setDestinationTerminologyVersion(
    String destinationTerminologyVersion) {
    this.destinationTerminologyVersion = destinationTerminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getName()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#isPublic()
   */
  @Override
  public boolean isPublic() {
    return isPublic;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setPublic(boolean)
   */
  @Override
  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#isGroupStructure()
   */
  @Override
  public boolean isGroupStructure() {
    return groupStructure;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setGroupStructure(boolean)
   */
  @Override
  public void setGroupStructure(boolean groupStructure) {
    this.groupStructure = groupStructure;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#isPublished()
   */
  @Override
  public boolean isPublished() {
    return published;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setPublished(boolean)
   */
  @Override
  public void setPublished(boolean published) {
    this.published = published;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getWorkflowType()
   */
  @Override
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setWorkflowType(org.ihtsdo.otf.
   * mapping.helpers.WorkflowType)
   */
  @Override
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetName()
   */
  @Override
  public String getRefSetName() {
    return this.refSetName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setRefSetName(java.lang.String)
   */
  @Override
  public void setRefSetName(String refSetName) {
    this.refSetName = refSetName;

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapProject#getEditingCycleBeginDate()
   */
  @Override
  public Date getEditingCycleBeginDate() {
    if (editingCycleBeginDate == null) {
      return new Date(0);
    }
    return editingCycleBeginDate;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapProject#setEditingCycleBeginDate(java.util.Date)
   */
  @Override
  public void setEditingCycleBeginDate(Date editingCycleBeginDate) {
    this.editingCycleBeginDate = editingCycleBeginDate;
  }
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapProject#getLatestPublicationDate()
   */
  @Override
  public Date getLatestPublicationDate() {
    if (latestPublicationDate == null) {
      return new Date(0);
    }
    return latestPublicationDate;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.model.MapProject#setLatestPublicationDate(java.util.Date)
   */
  @Override
  public void setLatestPublicationDate(Date latestPublicationDate) {
    this.latestPublicationDate = latestPublicationDate;
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetId()
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getRefSetId() {
    return refSetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetId(java.lang.String)
   */
  @Override
  public void setRefSetId(String refSetId) {
    this.refSetId = refSetId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelationStyle()
   */
  @Override
  public RelationStyle getMapRelationStyle() {
    return mapRelationStyle;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#getmapPrincipleSourceDocumentName()
   */
  @Override
  public String getMapPrincipleSourceDocumentName() {
    return mapPrincipleSourceDocumentName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setmapPrincipleSourceDocumentName
   * (java .lang.String)
   */
  @Override
  public void setMapPrincipleSourceDocumentName(
    String mapPrincipleSourceDocumentName) {
    this.mapPrincipleSourceDocumentName = mapPrincipleSourceDocumentName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setMapRelationStyle(java.lang.String
   * )
   */
  @Override
  public void setMapRelationStyle(RelationStyle mapRelationStyle) {
    this.mapRelationStyle = mapRelationStyle;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#isRuleBased()
   */
  @Override
  public boolean isRuleBased() {
    return ruleBased;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setRuleBased(boolean)
   */
  @Override
  public void setRuleBased(boolean ruleBased) {
    this.ruleBased = ruleBased;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRefsetPattern()
   */
  @Override
  public MapRefsetPattern getMapRefsetPattern() {
    return mapRefsetPattern;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setMapRefsetPattern(java.lang.String
   * )
   */
  @Override
  public void setMapRefsetPattern(MapRefsetPattern mapRefsetPattern) {
    this.mapRefsetPattern = mapRefsetPattern;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdvices()
   */
  @Override
  @XmlElement(type = MapAdviceJpa.class, name = "mapAdvice")
  public Set<MapAdvice> getMapAdvices() {
    return mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setMapAdvices(java.util.Set)
   */
  @Override
  public void setMapAdvices(Set<MapAdvice> mapAdvices) {
    this.mapAdvices = mapAdvices;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addMapAdvice(org.ihtsdo.otf.mapping
   * .model.MapAdvice)
   */
  @Override
  public void addMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.add(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeMapAdvice(org.ihtsdo.otf.
   * mapping.model.MapAdvice)
   */
  @Override
  public void removeMapAdvice(MapAdvice mapAdvice) {
    mapAdvices.remove(mapAdvice);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrinciples()
   */
  @Override
  @XmlElement(type = MapPrincipleJpa.class, name = "mapPrinciple")
  public Set<MapPrinciple> getMapPrinciples() {
    return mapPrinciples;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setMapPrinciples(java.util.Set)
   */
  @Override
  public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
    this.mapPrinciples = mapPrinciples;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addMapPrinciple(org.ihtsdo.otf.
   * mapping.model.MapPrinciple)
   */
  @Override
  public void addMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.add(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeMapPrinciple(org.ihtsdo.otf
   * .mapping.model.MapPrinciple)
   */
  @Override
  public void removeMapPrinciple(MapPrinciple mapPrinciple) {
    mapPrinciples.remove(mapPrinciple);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeConcepts()
   */
  @Override
  public Set<String> getScopeConcepts() {
    return scopeConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setScopeConcepts(java.util.Set)
   */
  @Override
  public void setScopeConcepts(Set<String> scopeConcepts) {
    this.scopeConcepts = scopeConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addScopeConcept(java.lang.String)
   */
  @Override
  public void addScopeConcept(String terminologyId) {
    this.scopeConcepts.add(terminologyId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeScopeConcept(java.lang.String
   * )
   */
  @Override
  public void removeScopeConcept(String terminologyId) {
    this.scopeConcepts.remove(terminologyId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#isScopeDescendantsFlag()
   */
  @Override
  public boolean isScopeDescendantsFlag() {
    return scopeDescendantsFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setScopeDescendantsFlag(boolean)
   */
  @Override
  public void setScopeDescendantsFlag(boolean flag) {
    scopeDescendantsFlag = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeExcludedConcepts()
   */
  @Override
  public Set<String> getScopeExcludedConcepts() {
    return scopeExcludedConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedConcepts(java.util
   * .Set)
   */
  @Override
  public void setScopeExcludedConcepts(Set<String> scopeExcludedConcepts) {
    this.scopeExcludedConcepts = scopeExcludedConcepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addScopeExcludedConcept(java.lang
   * .String)
   */
  @Override
  public void addScopeExcludedConcept(String terminologyId) {
    this.scopeExcludedConcepts.add(terminologyId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeScopeExcludedConcept(java
   * .lang.String)
   */
  @Override
  public void removeScopeExcludedConcept(String terminologyId) {
    this.scopeExcludedConcepts.remove(terminologyId);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#isScopeExcludedDescendantsFlag()
   */
  @Override
  public boolean isScopeExcludedDescendantsFlag() {
    return scopeExcludedDescendantsFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedDescendantsFlag
   * (boolean)
   */
  @Override
  public void setScopeExcludedDescendantsFlag(boolean flag) {
    scopeExcludedDescendantsFlag = flag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getPresetAgeRanges()
   */
  @Override
  @XmlElement(type = MapAgeRangeJpa.class, name = "mapAgeRange")
  public Set<MapAgeRange> getPresetAgeRanges() {
    return this.presetAgeRanges;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setPresetAgeRanges(java.util.Set)
   */
  @Override
  public void setPresetAgeRanges(Set<MapAgeRange> ageRanges) {
    this.presetAgeRanges = ageRanges;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addPresetAgeRange(org.ihtsdo.otf
   * .mapping.model.MapAgeRange)
   */
  @Override
  public void addPresetAgeRange(MapAgeRange ageRange) {
    this.presetAgeRanges.add(ageRange);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removePresetAgeRange(org.ihtsdo
   * .otf.mapping.model.MapAgeRange)
   */
  @Override
  public void removePresetAgeRange(MapAgeRange ageRange) {
    this.presetAgeRanges.remove(ageRange);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelations()
   */
  @Override
  @XmlElement(type = MapRelationJpa.class, name = "mapRelation")
  public Set<MapRelation> getMapRelations() {
    return mapRelations;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#setMapRelations(java.util.Set)
   */
  @Override
  public void setMapRelations(Set<MapRelation> mapRelations) {
    this.mapRelations = mapRelations;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addMapRelation(org.ihtsdo.otf.mapping
   * .model.MapRelation)
   */
  @Override
  public void addMapRelation(MapRelation mr) {
    this.mapRelations.add(mr);

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeMapRelation(org.ihtsdo.otf
   * .mapping.model.MapRelation)
   */
  @Override
  public void removeMapRelation(MapRelation mr) {
    this.mapRelations.remove(mr);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getErrorMessages()
   */
  @Override
  public Set<String> getErrorMessages() {
    return errorMessages;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setErrorMessages(java.util.Set)
   */
  @Override
  public void setErrorMessages(Set<String> errorMessages) {
    this.errorMessages = errorMessages;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#getProjectSpecificAlgorithmHandlerClass
   * ()
   */
  @Override
  public String getProjectSpecificAlgorithmHandlerClass() {
    return projectSpecificAlgorithmHandlerClass;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setProjectSpecificAlgorithmHandlerClass
   * (java.lang.String)
   */
  @Override
  public void setProjectSpecificAlgorithmHandlerClass(
    String projectSpecificAlgorithmHandlerClass) {
    this.projectSpecificAlgorithmHandlerClass =
        projectSpecificAlgorithmHandlerClass;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "MapProjectJpa [id=" + id + ", name=" + name + ", isPublic="
        + isPublic + ", groupStructure=" + groupStructure + ", published="
        + published + ", workflowType=" + workflowType + ", refSetId="
        + refSetId + ", refSetName=" + refSetName + ", sourceTerminology="
        + sourceTerminology + ", sourceTerminologyVersion="
        + sourceTerminologyVersion + ", destinationTerminology="
        + destinationTerminology + ", destinationTerminologyVersion="
        + destinationTerminologyVersion + ", mapRefsetPattern="
        + mapRefsetPattern + ", mapRelationStyle=" + mapRelationStyle
        + ", mapPrincipleSourceDocumentName=" + mapPrincipleSourceDocumentName
        + ", mapPrincipleSourceDocument=" + mapPrincipleSourceDocument
        + ", ruleBased=" + ruleBased
        + ", projectSpecificAlgorithmHandlerClass="
        + projectSpecificAlgorithmHandlerClass + ", algorithmHandler="
        + algorithmHandler + ", presetAgeRanges=" + presetAgeRanges
        + ", mapLeads=" + mapLeads + ", mapSpecialists=" + mapSpecialists
        + ", mapPrinciples=" + mapPrinciples + ", mapAdvices=" + mapAdvices
        + ", mapRelations=" + mapRelations + ", reportDefinitions="
        + reportDefinitions + ", scopeConcepts=" + scopeConcepts
        + ", scopeExcludedConcepts=" + scopeExcludedConcepts
        + ", scopeDescendantsFlag=" + scopeDescendantsFlag
        + ", scopeExcludedDescendantsFlag=" + scopeExcludedDescendantsFlag
        + ", errorMessages=" + errorMessages + ", propagatedFlag="
        + propagatedFlag + ", propagationDescendantThreshold="
        + propagationDescendantThreshold + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result
            + ((destinationTerminology == null) ? 0 : destinationTerminology
                .hashCode());
    result =
        prime
            * result
            + ((destinationTerminologyVersion == null) ? 0
                : destinationTerminologyVersion.hashCode());
    result = prime * result + (isPublic ? 1231 : 1237);
    result = prime * result + ((refSetId == null) ? 0 : refSetId.hashCode());
    result =
        prime * result
            + ((scopeConcepts == null) ? 0 : scopeConcepts.hashCode());
    result = prime * result + (scopeDescendantsFlag ? 1231 : 1237);
    result =
        prime
            * result
            + ((scopeExcludedConcepts == null) ? 0 : scopeExcludedConcepts
                .hashCode());
    result = prime * result + (scopeExcludedDescendantsFlag ? 1231 : 1237);
    result =
        prime * result
            + ((sourceTerminology == null) ? 0 : sourceTerminology.hashCode());
    result =
        prime
            * result
            + ((sourceTerminologyVersion == null) ? 0
                : sourceTerminologyVersion.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MapProjectJpa other = (MapProjectJpa) obj;
    if (destinationTerminology == null) {
      if (other.destinationTerminology != null)
        return false;
    } else if (!destinationTerminology.equals(other.destinationTerminology))
      return false;
    if (destinationTerminologyVersion == null) {
      if (other.destinationTerminologyVersion != null)
        return false;
    } else if (!destinationTerminologyVersion
        .equals(other.destinationTerminologyVersion))
      return false;
    if (isPublic != other.isPublic)
      return false;
    if (refSetId == null) {
      if (other.refSetId != null)
        return false;
    } else if (!refSetId.equals(other.refSetId))
      return false;
    if (scopeConcepts == null) {
      if (other.scopeConcepts != null)
        return false;
    } else if (!scopeConcepts.equals(other.scopeConcepts))
      return false;
    if (scopeDescendantsFlag != other.scopeDescendantsFlag)
      return false;
    if (scopeExcludedConcepts == null) {
      if (other.scopeExcludedConcepts != null)
        return false;
    } else if (!scopeExcludedConcepts.equals(other.scopeExcludedConcepts))
      return false;
    if (scopeExcludedDescendantsFlag != other.scopeExcludedDescendantsFlag)
      return false;
    if (sourceTerminology == null) {
      if (other.sourceTerminology != null)
        return false;
    } else if (!sourceTerminology.equals(other.sourceTerminology))
      return false;
    if (sourceTerminologyVersion == null) {
      if (other.sourceTerminologyVersion != null)
        return false;
    } else if (!sourceTerminologyVersion.equals(other.sourceTerminologyVersion))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setMapPrincipleSourceDocument(java
   * .lang.String)
   */
  @Override
  public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument) {
    this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#getMapPrincipleSourceDocument()
   */
  @Override
  public String getMapPrincipleSourceDocument() {
    return mapPrincipleSourceDocument;
  }

  /**
   * Returns the propagation descendant threshold.
   *
   * @return the propagation descendant threshold
   */
  @Override
  public Integer getPropagationDescendantThreshold() {
    return propagationDescendantThreshold;
  }

  /**
   * Sets the propagation descendant threshold.
   *
   * @param propagationDescendantThreshold the propagation descendant threshold
   */
  @Override
  public void setPropagationDescendantThreshold(
    Integer propagationDescendantThreshold) {
    this.propagationDescendantThreshold = propagationDescendantThreshold;
  }

  /**
   * Indicates whether or not propagated flag is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @Override
  public boolean isPropagatedFlag() {
    return propagatedFlag;
  }

  /**
   * Sets the propagated flag.
   *
   * @param propagatedFlag the propagated flag
   */
  @Override
  public void setPropagatedFlag(boolean propagatedFlag) {
    this.propagatedFlag = propagatedFlag;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapProject#getReportDefinitions()
   */
  @Override
  @XmlElement(type = ReportDefinitionJpa.class, name = "reportDefinition")
  public Set<ReportDefinition> getReportDefinitions() {
    return reportDefinitions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#setReportDefinitions(java.util.Set)
   */
  @Override
  public void setReportDefinitions(Set<ReportDefinition> reportDefinitions) {
    this.reportDefinitions = reportDefinitions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#addReportDefinition(org.ihtsdo.
   * otf.mapping.reports.ReportDefinition)
   */
  @Override
  public void addReportDefinition(ReportDefinition reportDefinition) {
    reportDefinitions.add(reportDefinition);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapProject#removeReportDefinition(org.ihtsdo
   * .otf.mapping.reports.ReportDefinition)
   */
  @Override
  public void removeReportDefinition(ReportDefinition reportDefinition) {
    reportDefinitions.remove(reportDefinition);
  }

}
