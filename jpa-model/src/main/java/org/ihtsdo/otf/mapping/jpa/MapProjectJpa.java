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
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
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
@XmlRootElement(name="mapProject")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapProjectJpa implements MapProject {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The name. */
	@Column(nullable = false)
	private String name;

	/** Indicates whether there is block structure for map records of this project. */
	@Column(unique = false, nullable = false)
	private boolean blockStructure = false;

	/** Indicates whether there is group structure for map records of this project. */
	@Column(unique = false, nullable = false)
	private boolean groupStructure = false;

	/** Indicates if the map project has been published. */
	@Column(unique = false, nullable = false)
	private boolean published = false;

	/** The ref set id. */
	private String refSetId;
	
	/**  The ref set name. */
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
	
	/**  The RF2 refset pattern for this map project. */
	@Column(nullable = true)
	private String mapRefsetPattern;
	
	/**  The relation behavior. */
	@Column(nullable = true)
	private String mapRelationStyle;
	
	/**  The name of the mapping principle document. */
	@Column(nullable = true)
	private String mapPrincipleSourceDocument;
	
	/**  Flag for whether this project is rule based. */
	@Column(nullable = false)
	private boolean ruleBased;

	/** The map leads. */
	@ManyToMany(targetEntity=MapUserJpa.class, fetch=FetchType.EAGER)
	@JoinTable(name="map_projects_map_leads",
	   joinColumns=@JoinColumn(name="map_projects_id"),
	   inverseJoinColumns=@JoinColumn(name="map_users_id"))
	@IndexedEmbedded(targetElement=MapUserJpa.class)
	private Set<MapUser> mapLeads = new HashSet<MapUser>();
	
	/** The map specialists. */
	@ManyToMany(targetEntity=MapUserJpa.class, fetch=FetchType.EAGER)
	@JoinTable(name="map_projects_map_specialists",
			   joinColumns=@JoinColumn(name="map_projects_id"),
			   inverseJoinColumns=@JoinColumn(name="map_users_id"))
	@IndexedEmbedded(targetElement=MapUserJpa.class)
	private Set<MapUser> mapSpecialists = new HashSet<MapUser>();
	
	/** The allowable map principles for this MapProject. */
	@ManyToMany(targetEntity=MapPrincipleJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapPrincipleJpa.class)
	private Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();

	/** The allowable map advices for this MapProject. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	/**  The preset age ranges for rule generation (Age, Age at onset). */
	@ElementCollection(fetch=FetchType.EAGER)
	@CollectionTable(name="map_projects_rule_preset_age_ranges", joinColumns=@JoinColumn(name="id"))
	@Column(nullable = true)
	private Set<String> rulePresetAgeRanges = new HashSet<String>();
	
	
  /**  The concepts in scope for this project. */
	@ElementCollection(fetch=FetchType.EAGER)
	@CollectionTable(name="map_projects_scope_concepts", joinColumns=@JoinColumn(name="id"))
	@Column(nullable = true)
	private Set<String> scopeConcepts = new HashSet<String>();
	
	/**  The concepts excluded from scope of this project. */
	@ElementCollection(fetch=FetchType.EAGER)
	@CollectionTable(name="map_projects_scope_excluded_concepts", joinColumns=@JoinColumn(name="id"))
	@Column(nullable = true)
	private Set<String> scopeExcludedConcepts = new HashSet<String>();
	
	/**  Indicates if descendants of the scope are included in the scope. */
	@Column(unique = false, nullable = false)
	private boolean scopeDescendantsFlag = false;
	
	/**  Indicates if descendants of the excluded scope are excluded from the scope. */
	@Column(unique = false, nullable = false)
	private boolean scopeExcludedDescendantsFlag = false;
	
	/**
	 *  Default constructor.
	 */
	public MapProjectJpa() {
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
	 */
	@Override
	@XmlElement(type=MapUserJpa.class, name="mapLead")
	public Set<MapUser> getMapLeads() {
		return mapLeads;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
	 */
	@Override
	public void setMapLeads(Set<MapUser> mapLeads) {
		this.mapLeads = mapLeads;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapLead(org.ihtsdo.otf.mapping.model.MapLead)
	 */
	@Override
	public void addMapLead(MapUser mapLead) {
		mapLeads.add(mapLead);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapLead(org.ihtsdo.otf.mapping.model.MapLead)
	 */
	@Override
	public void removeMapLead(MapUser mapLead) {
		mapLeads.remove(mapLead);
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
	 */
	@Override
	@XmlElement(type=MapUserJpa.class, name="mapSpecialist")
	public Set<MapUser> getMapSpecialists() {
		return mapSpecialists;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapSpecialists(java.util.Set)
	 */
	@Override
	public void setMapSpecialists(Set<MapUser> mapSpecialists) {
		this.mapSpecialists = mapSpecialists;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapSpecialist(org.ihtsdo.otf.mapping.model.MapSpecialist)
	 */
	@Override
	public void addMapSpecialist(MapUser mapSpecialist) {
		mapSpecialists.add(mapSpecialist);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapSpecialist(org.ihtsdo.otf.mapping.model.MapSpecialist)
	 */
	@Override
	public void removeMapSpecialist(MapUser mapSpecialist) {
		mapSpecialists.remove(mapSpecialist);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminology()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getSourceTerminology() {
		return sourceTerminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminology(java.lang.String)
	 */
	@Override
	public void setSourceTerminology(String sourceTerminology) {
		this.sourceTerminology = sourceTerminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminology()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getDestinationTerminology() {
		return destinationTerminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminology(java.lang.String)
	 */
	@Override
	public void setDestinationTerminology(String destinationTerminology) {
		this.destinationTerminology = destinationTerminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminologyVersion()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getSourceTerminologyVersion() {
		return sourceTerminologyVersion;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setSourceTerminologyVersion(java.lang.String)
	 */
	@Override
	public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
		this.sourceTerminologyVersion = sourceTerminologyVersion;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getDestinationTerminologyVersion()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getDestinationTerminologyVersion() {
		return destinationTerminologyVersion;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setDestinationTerminologyVersion(java.lang.String)
	 */
	@Override
	public void setDestinationTerminologyVersion(
		String destinationTerminologyVersion) {
		this.destinationTerminologyVersion = destinationTerminologyVersion;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getName()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)		
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isBlockStructure()
	 */
	@Override
	public boolean isBlockStructure() {
		return blockStructure;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setBlockStructure(boolean)
	 */
	@Override
	public void setBlockStructure(boolean blockStructure) {
		this.blockStructure = blockStructure;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isGroupStructure()
	 */
	@Override
	public boolean isGroupStructure() {
		return groupStructure;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setGroupStructure(boolean)
	 */
	@Override
	public void setGroupStructure(boolean groupStructure) {
		this.groupStructure = groupStructure;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isPublished()
	 */
	@Override
	public boolean isPublished() {
		return published;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setPublished(boolean)
	 */
	@Override
	public void setPublished(boolean published) {
		this.published = published;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetName()
	 */
	@Override
	public String getRefSetName() {
		return this.refSetName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetName(java.lang.String)
	 */
	@Override
	public void setRefSetName(String refSetName) {
		this.refSetName = refSetName;
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetId()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getRefSetId() {
		return refSetId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetId(java.lang.String)
	 */
	@Override
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRelationStyle()
	 */
	@Override
	public String getMapRelationStyle() {
		return mapRelationStyle;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrincipleSourceDocument()
	 */
	@Override
	public String getMapPrincipleSourceDocument() {
		return mapPrincipleSourceDocument;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapPrincipleSourceDocument(java.lang.String)
	 */
	@Override
	public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument) {
		this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapRelationStyle(java.lang.String)
	 */
	@Override
	public void setMapRelationStyle(String mapRelationStyle) {
		this.mapRelationStyle = mapRelationStyle;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isRuleBased()
	 */
	@Override
	public boolean isRuleBased() {
		return ruleBased;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRuleBased(boolean)
	 */
	@Override
	public void setRuleBased(boolean ruleBased) {
		this.ruleBased = ruleBased;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapRefsetPattern()
	 */
	@Override
	public String getMapRefsetPattern() {
		return mapRefsetPattern;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapRefsetPattern(java.lang.String)
	 */
	@Override
	public void setMapRefsetPattern(String mapRefsetPattern) {
		this.mapRefsetPattern = mapRefsetPattern;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRulePresetAgeRanges()
	 */
	@Override
	public Set<String> getRulePresetAgeRanges() {
		return rulePresetAgeRanges;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRulePresetAgeRanges(java.util.Set)
	 */
	@Override
	public void setRulePresetAgeRanges(Set<String> rulePresetAgeRanges) {
		this.rulePresetAgeRanges = rulePresetAgeRanges;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdvices()
	 */
	@Override
	@XmlElement(type=MapAdviceJpa.class, name="mapAdvice")
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapAdvices(java.util.Set)
	 */
	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapPrinciples()
	 */
	@Override
	@XmlElement(type=MapPrincipleJpa.class, name="mapPrinciple")
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapPrinciples(java.util.Set)
	 */
	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapPrinciple(org.ihtsdo.otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapPrinciple(org.ihtsdo.otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeConcepts()
	 */
	@Override
	public Set<String> getScopeConcepts() {
		return scopeConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setScopeConcepts(java.util.Set)
	 */
	@Override
	public void setScopeConcepts(Set<String> scopeConcepts) {
		this.scopeConcepts = scopeConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isScopeDescendantsFlag()
	 */
	@Override
	public boolean isScopeDescendantsFlag() {
		return scopeDescendantsFlag;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setScopeDescendantsFlag(boolean)
	 */
	@Override
	public void setScopeDescendantsFlag(boolean flag) {
		scopeDescendantsFlag = flag;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getScopeExcludedConcepts()
	 */
	@Override
	public Set<String> getScopeExcludedConcepts() {
		return scopeExcludedConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedConcepts(java.util.Set)
	 */
	@Override
	public void setScopeExcludedConcepts(Set<String> scopeExcludedConcepts) {
		this.scopeExcludedConcepts = scopeExcludedConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#isScopeExcludedDescendantsFlag()
	 */
	@Override
	public boolean isScopeExcludedDescendantsFlag() {
		return scopeExcludedDescendantsFlag;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setScopeExcludedDescendantsFlag(boolean)
	 */
	@Override
	public void setScopeExcludedDescendantsFlag(boolean flag) {
		scopeExcludedDescendantsFlag = flag;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapProjectJpa [name=" + name + ", blockStructure="
				+ blockStructure + ", groupStructure=" + groupStructure
				+ ", published=" + published + ", refSetId=" + refSetId
				+ ", refSetName=" + refSetName + ", sourceTerminology="
				+ sourceTerminology + ", sourceTerminologyVersion="
				+ sourceTerminologyVersion + ", destinationTerminology="
				+ destinationTerminology + ", destinationTerminologyVersion="
				+ destinationTerminologyVersion + ", mapRefsetPattern="
				+ mapRefsetPattern + ", mapRelationStyle=" + mapRelationStyle
				+ ", mapPrincipleSourceDocument=" + mapPrincipleSourceDocument
				+ ", ruleBased=" + ruleBased + ", mapLeads=" + mapLeads
				+ ", mapSpecialists=" + mapSpecialists + ", mapPrinciples="
				+ mapPrinciples + ", mapAdvices=" + mapAdvices
				+ ", rulePresetAgeRanges=" + rulePresetAgeRanges + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (blockStructure ? 1231 : 1237);
		result = prime
				* result
				+ ((destinationTerminology == null) ? 0
						: destinationTerminology.hashCode());
		result = prime
				* result
				+ ((destinationTerminologyVersion == null) ? 0
						: destinationTerminologyVersion.hashCode());
		result = prime * result + (groupStructure ? 1231 : 1237);
		result = prime * result
				+ ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result
				+ ((mapLeads == null) ? 0 : mapLeads.hashCode());
		result = prime
				* result
				+ ((mapPrincipleSourceDocument == null) ? 0
						: mapPrincipleSourceDocument.hashCode());
		result = prime * result
				+ ((mapPrinciples == null) ? 0 : mapPrinciples.hashCode());
		result = prime
				* result
				+ ((mapRefsetPattern == null) ? 0 : mapRefsetPattern.hashCode());
		result = prime
				* result
				+ ((mapRelationStyle == null) ? 0 : mapRelationStyle.hashCode());
		result = prime * result
				+ ((mapSpecialists == null) ? 0 : mapSpecialists.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (published ? 1231 : 1237);
		result = prime * result
				+ ((refSetId == null) ? 0 : refSetId.hashCode());
		result = prime * result
				+ ((refSetName == null) ? 0 : refSetName.hashCode());
		result = prime * result + (ruleBased ? 1231 : 1237);
		result = prime
				* result
				+ ((rulePresetAgeRanges == null) ? 0 : rulePresetAgeRanges
						.hashCode());
		result = prime
				* result
				+ ((sourceTerminology == null) ? 0 : sourceTerminology
						.hashCode());
		result = prime
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
		if (mapAdvices == null) {
			if (other.mapAdvices != null)
				return false;
		} else if (!mapAdvices.equals(other.mapAdvices))
			return false;
		if (mapLeads == null) {
			if (other.mapLeads != null)
				return false;
		} else if (!mapLeads.equals(other.mapLeads))
			return false;
		if (mapPrincipleSourceDocument == null) {
			if (other.mapPrincipleSourceDocument != null)
				return false;
		} else if (!mapPrincipleSourceDocument
				.equals(other.mapPrincipleSourceDocument))
			return false;
		if (mapPrinciples == null) {
			if (other.mapPrinciples != null)
				return false;
		} else if (!mapPrinciples.equals(other.mapPrinciples))
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
		if (mapSpecialists == null) {
			if (other.mapSpecialists != null)
				return false;
		} else if (!mapSpecialists.equals(other.mapSpecialists))
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
		if (rulePresetAgeRanges == null) {
			if (other.rulePresetAgeRanges != null)
				return false;
		} else if (!rulePresetAgeRanges.equals(other.rulePresetAgeRanges))
			return false;
		if (sourceTerminology == null) {
			if (other.sourceTerminology != null)
				return false;
		} else if (!sourceTerminology.equals(other.sourceTerminology))
			return false;
		if (sourceTerminologyVersion == null) {
			if (other.sourceTerminologyVersion != null)
				return false;
		} else if (!sourceTerminologyVersion
				.equals(other.sourceTerminologyVersion))
			return false;
		return true;
	}


}
