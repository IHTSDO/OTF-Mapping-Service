package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
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
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Class MapProjectJpa.
 * 
 * @author ${author}
 */
@Entity
@Table(name = "map_projects")
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
	 * Indicates whether there is block structure for map records of this project.
	 */
	@Column(unique = false, nullable = false)
	private boolean blockStructure = false;

	/**
	 * Indicates whether there is group structure for map records of this project.
	 */
	@Column(unique = false, nullable = false)
	private boolean groupStructure = false;

	/** Indicates if the map project has been published. */
	@Column(unique = false, nullable = false)
	private boolean published = false;
	
	/** Indicates what type of workflow to use for this project, defaults to conflict review */
	@Column(unique = false, nullable = false)
	private String workflowType = "";

	/** The ref set id. */
	private String refSetId;

	/** The ref set name. */
	private String refSetName;

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
	@Column(nullable = false)
	private String mapRefsetPattern;

	/** The relation behavior. */
	@Column(nullable = false)
	private String mapRelationStyle;

	/** The mapping principle document. */
	@Column(nullable = true)
	private String mapPrincipleSourceDocument;
	
	/** The name of the mapping principle document. */
	@Column(nullable = true)
	private String mapPrincipleSourceDocumentName;

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

	/** The map administrators. */
	@ManyToMany(targetEntity = MapUserJpa.class, fetch = FetchType.EAGER)
	@JoinTable(name = "map_projects_map_administrators", joinColumns = @JoinColumn(name = "map_projects_id"), inverseJoinColumns = @JoinColumn(name = "map_users_id"))
	@IndexedEmbedded(targetElement = MapUserJpa.class)
	private Set<MapUser> mapAdministrators = new HashSet<>();
	
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

	/**
	 * Default constructor.
	 */
	public MapProjectJpa() {
	}
	
	

	/**
	 * Instantiates a new map project jpa.
	 *
	 * @param id the id
	 * @param name the name
	 * @param isPublic the is public
	 * @param blockStructure the block structure
	 * @param groupStructure the group structure
	 * @param published the published
	 * @param refSetId the ref set id
	 * @param refSetName the ref set name
	 * @param sourceTerminology the source terminology
	 * @param sourceTerminologyVersion the source terminology version
	 * @param destinationTerminology the destination terminology
	 * @param destinationTerminologyVersion the destination terminology version
	 * @param mapRefsetPattern the map refset pattern
	 * @param mapRelationStyle the map relation style
	 * @param mapPrincipleSourceDocument the map principle source document
	 * @param ruleBased the rule based
	 * @param projectSpecificAlgorithmHandlerClass the project specific algorithm handler class
	 * @param algorithmHandler the algorithm handler
	 * @param presetAgeRanges the preset age ranges
	 * @param mapLeads the map leads
	 * @param mapSpecialists the map specialists
	 * @param mapAdministrators the map administrators
	 * @param mapPrinciples the map principles
	 * @param mapAdvices the map advices
	 * @param mapRelations the map relations
	 * @param scopeConcepts the scope concepts
	 * @param scopeExcludedConcepts the scope excluded concepts
	 * @param scopeDescendantsFlag the scope descendants flag
	 * @param scopeExcludedDescendantsFlag the scope excluded descendants flag
	 */
	public MapProjectJpa(Long id, String name, boolean isPublic,
			boolean blockStructure, boolean groupStructure, boolean published,
			String refSetId, String refSetName, String sourceTerminology,
			String sourceTerminologyVersion, String destinationTerminology,
			String destinationTerminologyVersion, String mapRefsetPattern,
			String mapRelationStyle, String mapPrincipleSourceDocument,
			boolean ruleBased, String projectSpecificAlgorithmHandlerClass,
			ProjectSpecificAlgorithmHandler algorithmHandler,
			Set<MapAgeRange> presetAgeRanges, Set<MapUser> mapLeads,
			Set<MapUser> mapSpecialists, Set<MapUser> mapAdministrators, Set<MapPrinciple> mapPrinciples,
			Set<MapAdvice> mapAdvices, Set<MapRelation> mapRelations,
			Set<String> scopeConcepts, Set<String> scopeExcludedConcepts,
			boolean scopeDescendantsFlag, boolean scopeExcludedDescendantsFlag) {
		super();
		this.id = id;
		this.name = name;
		this.isPublic = isPublic;
		this.blockStructure = blockStructure;
		this.groupStructure = groupStructure;
		this.published = published;
		this.refSetId = refSetId;
		this.refSetName = refSetName;
		this.sourceTerminology = sourceTerminology;
		this.sourceTerminologyVersion = sourceTerminologyVersion;
		this.destinationTerminology = destinationTerminology;
		this.destinationTerminologyVersion = destinationTerminologyVersion;
		this.mapRefsetPattern = mapRefsetPattern;
		this.mapRelationStyle = mapRelationStyle;
		this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
		this.ruleBased = ruleBased;
		this.projectSpecificAlgorithmHandlerClass = projectSpecificAlgorithmHandlerClass;
		this.algorithmHandler = algorithmHandler;
		this.presetAgeRanges = presetAgeRanges;
		this.mapLeads = mapLeads;
		this.mapSpecialists = mapSpecialists;
		this.mapAdministrators = mapAdministrators;
		this.mapPrinciples = mapPrinciples;
		this.mapAdvices = mapAdvices;
		this.mapRelations = mapRelations;
		this.scopeConcepts = scopeConcepts;
		this.scopeExcludedConcepts = scopeExcludedConcepts;
		this.scopeDescendantsFlag = scopeDescendantsFlag;
		this.scopeExcludedDescendantsFlag = scopeExcludedDescendantsFlag;
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdministrators()
	 */
	@Override
	@XmlElement(type = MapUserJpa.class, name = "mapAdministrator")
	public Set<MapUser> getMapAdministrators() {
		return mapAdministrators;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapAdministrators(java.util.Set)
	 */
	@Override
	public void setMapAdministrators(Set<MapUser> mapAdministrators) {
		this.mapAdministrators = mapAdministrators;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapAdministrator(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void addMapAdministrator(MapUser mapAdministrator) {
		mapAdministrators.add(mapAdministrator);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapAdministrator(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void removeMapAdministrator(MapUser mapAdministrator) {
		mapAdministrators.remove(mapAdministrator);
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPublic()
	 */
	@Override
	public boolean isPublic() {
		return isPublic;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPublic(boolean)
	 */
	@Override
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isBlockStructure()
	 */
	@Override
	public boolean isBlockStructure() {
		return blockStructure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setBlockStructure(boolean)
	 */
	@Override
	public void setBlockStructure(boolean blockStructure) {
		this.blockStructure = blockStructure;
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

	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getWorkflowType()
	 */
	@Override
	public String getWorkflowType() {
		return workflowType;
	}


    /* (non-Javadoc)
     * @see org.ihtsdo.otf.mapping.model.MapProject#setWorkflowType(org.ihtsdo.otf.mapping.helpers.WorkflowType)
     */
    @Override
	public void setWorkflowType(String workflowType) {
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
	public String getMapRelationStyle() {
		return mapRelationStyle;
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
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapRelationStyle(java.lang.String
	 * )
	 */
	@Override
	public void setMapRelationStyle(String mapRelationStyle) {
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
	public String getMapRefsetPattern() {
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
	public void setMapRefsetPattern(String mapRefsetPattern) {
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getPresetAgeRanges()
	 */
	@Override
	@XmlElement(type = MapAgeRangeJpa.class, name = "mapAgeRange")
	public Set<MapAgeRange> getPresetAgeRanges() {
		return this.presetAgeRanges;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPresetAgeRanges(java.util.Set)
	 */
	@Override
	public void setPresetAgeRanges(Set<MapAgeRange> ageRanges) {
		this.presetAgeRanges = ageRanges;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addPresetAgeRange(org.ihtsdo.otf.mapping.model.MapAgeRange)
	 */
	@Override
	public void addPresetAgeRange(MapAgeRange ageRange) {
		this.presetAgeRanges.add(ageRange);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removePresetAgeRange(org.ihtsdo.otf.mapping.model.MapAgeRange)
	 */
	@Override
	public void removePresetAgeRange(MapAgeRange ageRange) {
		this.presetAgeRanges.remove(ageRange);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelations()
	 */
	@Override
	@XmlElement(type = MapRelationJpa.class, name = "mapRelation")
	public Set<MapRelation> getMapRelations() {
		return mapRelations;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapRelations(java.util.Set)
	 */
	@Override
	public void setMapRelations(Set<MapRelation> mapRelations) {
		this.mapRelations = mapRelations;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapRelation(org.ihtsdo.otf.mapping.model.MapRelation)
	 */
	@Override
	public void addMapRelation(MapRelation mr) {
		this.mapRelations.add(mr);

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapRelation(org.ihtsdo.otf.mapping.model.MapRelation)
	 */
	@Override
	public void removeMapRelation(MapRelation mr) {
		this.mapRelations.remove(mr);

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getProjectSpecificAlgorithmHandlerClass()
	 */
	@Override
	public String getProjectSpecificAlgorithmHandlerClass() {
		return projectSpecificAlgorithmHandlerClass;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setProjectSpecificAlgorithmHandlerClass(java.lang.String)
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
		return "MapProjectJpa [name=" + name + ", blockStructure=" + blockStructure
				+ ", groupStructure=" + groupStructure + ", published=" + published
				+ ", refSetId=" + refSetId + ", refSetName=" + refSetName
				+ ", sourceTerminology=" + sourceTerminology
				+ ", sourceTerminologyVersion=" + sourceTerminologyVersion
				+ ", destinationTerminology=" + destinationTerminology
				+ ", destinationTerminologyVersion=" + destinationTerminologyVersion
				+ ", mapRefsetPattern=" + mapRefsetPattern + ", mapRelationStyle="
				+ mapRelationStyle + ", mapPrincipleSourceDocument="
				+ mapPrincipleSourceDocument + ", ruleBased=" + ruleBased
				+ ", presetAgeRanges=" + presetAgeRanges + ", mapLeads=" + mapLeads
				+ ", mapSpecialists=" + mapSpecialists + ", mapAdministrators=" + mapAdministrators + ", mapPrinciples="
				+ mapPrinciples + ", mapAdvices=" + mapAdvices + ", mapRelations="
				+ mapRelations + ", scopeConcepts=" + scopeConcepts
				+ ", scopeExcludedConcepts=" + scopeExcludedConcepts
				+ ", scopeDescendantsFlag=" + scopeDescendantsFlag
				+ ", scopeExcludedDescendantsFlag=" + scopeExcludedDescendantsFlag
				+ "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (blockStructure ? 1231 : 1237);
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
		result = prime * result + (groupStructure ? 1231 : 1237);
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapLeads == null) ? 0 : mapLeads.hashCode());
		result = prime * result + ((mapAdministrators == null) ? 0 : mapAdministrators.hashCode());
		result =
				prime
				* result
				+ ((mapPrincipleSourceDocument == null) ? 0
						: mapPrincipleSourceDocument.hashCode());
		result =
				prime * result
				+ ((mapPrinciples == null) ? 0 : mapPrinciples.hashCode());
		result =
				prime * result
				+ ((mapRefsetPattern == null) ? 0 : mapRefsetPattern.hashCode());
		result =
				prime * result
				+ ((mapRelationStyle == null) ? 0 : mapRelationStyle.hashCode());
		result =
				prime * result + ((mapRelations == null) ? 0 : mapRelations.hashCode());
		result =
				prime * result
				+ ((mapSpecialists == null) ? 0 : mapSpecialists.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (published ? 1231 : 1237);
		result = prime * result + ((refSetId == null) ? 0 : refSetId.hashCode());
		result =
				prime * result + ((refSetName == null) ? 0 : refSetName.hashCode());
		result = prime * result + (ruleBased ? 1231 : 1237);
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
		if (blockStructure != other.blockStructure)
			return false;
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
		if (groupStructure != other.groupStructure)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mapRefsetPattern == null) {
			if (other.mapRefsetPattern != null)
				return false;
		} else if (!mapRefsetPattern.equals(other.mapRefsetPattern))
			return false;
		if (mapRelationStyle == null) {
			if (other.mapRelationStyle != null)
				return false;
		} else if (!mapRelationStyle.equals(other.mapRelationStyle))
			return false;
		if (mapRelations == null) {
			if (other.mapRelations != null)
				return false;
		} else if (!mapRelations.equals(other.mapRelations))
			return false;
		if (mapSpecialists == null) {
			if (other.mapSpecialists != null)
				return false;
		} else if (!mapSpecialists.equals(other.mapSpecialists))
			return false;
		if (mapAdministrators == null) {
			if (other.mapAdministrators != null)
				return false;
		} else if (!mapAdministrators.equals(other.mapAdministrators))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (published != other.published)
			return false;
		if (refSetId == null) {
			if (other.refSetId != null)
				return false;
		} else if (!refSetId.equals(other.refSetId))
			return false;
		if (refSetName == null) {
			if (other.refSetName != null)
				return false;
		} else if (!refSetName.equals(other.refSetName))
			return false;
		if (ruleBased != other.ruleBased)
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



	@Override
	public void setMapPrincipleSourceDocumentName(
		String mapPrincipleSourceDocumentName) {
		this.mapPrincipleSourceDocumentName = mapPrincipleSourceDocumentName;
	}



	@Override
	public String getMapPrincipleSourceDocumentName() {
		return mapPrincipleSourceDocumentName;
	}




}
