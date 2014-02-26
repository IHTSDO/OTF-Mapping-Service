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
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * The Class MapProjectJpa.
 *
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
	
	/** The ref set name */
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
	
	/** The mapping behavior */
	@Column(nullable = true)
	private String mapType;
	
	/** The relation behavior */
	@Column(nullable = true)
	private String mapRelationStyle;
	
	/** The name of the mapping principle document */
	@Column(nullable = true)
	private String mapPrincipleSourceDocument;
	
	/** Flag for whether this project is rule based */
	@Column(nullable = false)
	private boolean ruleBased;

	/** The map leads. */
	@ManyToMany(targetEntity=MapLeadJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapLeadJpa.class)
	private Set<MapLead> mapLeads = new HashSet<MapLead>();
	
	/** The map specialists. */
	@ManyToMany(targetEntity=MapSpecialistJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapSpecialistJpa.class)
	private Set<MapSpecialist> mapSpecialists = new HashSet<MapSpecialist>();
	
	/** The allowable map principles for this MapProject. */
	@ManyToMany(targetEntity=MapPrincipleJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapPrincipleJpa.class)
	private Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();

	/** The allowable map advices for this MapProject. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	/** The preset age ranges for rule generation (Age, Age at onset) */
	@ElementCollection(fetch=FetchType.EAGER)
	@CollectionTable(name="map_projects_rule_preset_age_ranges", joinColumns=@JoinColumn(name="id"))
	@Column(nullable = true)
	private Set<String> rulePresetAgeRanges = new HashSet<String>();
	
	/** Default constructor */
	public MapProjectJpa() {
	}

	/**
	 * Full constructor
	 * @param id the id
	 * @param name the project name
	 * @param blockStructure the blockstructure (boolean)
	 * @param groupStructure the group structure (boolean)
	 * @param published is published (boolean)
	 * @param mapAdvices the map advices
	 * @param refSetId the ref set id
	 * @param sourceTerminology the source terminology
	 * @param sourceTerminologyVersion the source terminology version
	 * @param destinationTerminology the destination terminology
	 * @param destinationTerminologyVersion the destination terminology vresion
	 * @param mapLeads the map leads
	 * @param mapSpecialists the map specialists
	 */
	public MapProjectJpa(Long id, String name, boolean blockStructure,
			boolean groupStructure, boolean published, Set<MapAdvice> mapAdvices,
			String refSetId, String sourceTerminology, String sourceTerminologyVersion,
			String destinationTerminology, String destinationTerminologyVersion,
			Set<MapLead> mapLeads, Set<MapSpecialist> mapSpecialists) {
		super();
		this.id = id;
		this.name = name;
		this.blockStructure = blockStructure;
		this.groupStructure = groupStructure;
		this.published = published;
		this.mapAdvices = mapAdvices;
		this.refSetId = refSetId;
		this.sourceTerminology = sourceTerminology;
		this.sourceTerminologyVersion = sourceTerminologyVersion;
		this.destinationTerminology = destinationTerminology;
		this.destinationTerminologyVersion = destinationTerminologyVersion;
		this.mapLeads = mapLeads;
		this.mapSpecialists = mapSpecialists;
	}

	/**
	 * Return the id
	 * @return the id
	 */
	@Override
	public Long getId() {
		return this.id;
	}
	
	/**
	 * Set the id
	 * @param id the id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;		
	}
	
	/**
	 * Returns the id in string form
	 * @return the id in string form
	 */
	@XmlID
	@Override
	public String getObjectId() {
		return id.toString();
	}
	
	/**
	 * Required for 
	 * @param objectId
	 */
	public void setObjectId(String objectId) {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
	 */
	@Override
	@XmlElement(type=MapLeadJpa.class, name="mapLead")
	public Set<MapLead> getMapLeads() {
		return mapLeads;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
	 */
	@Override
	public void setMapLeads(Set<MapLead> mapLeads) {
		this.mapLeads = mapLeads;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapLead(org.ihtsdo.otf.mapping.model.MapLead)
	 */
	@Override
	public void addMapLead(MapLead mapLead) {
		mapLeads.add(mapLead);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapLead(org.ihtsdo.otf.mapping.model.MapLead)
	 */
	@Override
	public void removeMapLead(MapLead mapLead) {
		mapLeads.remove(mapLead);
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
	 */
	@Override
	@XmlElement(type=MapSpecialistJpa.class, name="mapSpecialist")
	public Set<MapSpecialist> getMapSpecialists() {
		return mapSpecialists;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapSpecialists(java.util.Set)
	 */
	@Override
	public void setMapSpecialists(Set<MapSpecialist> mapSpecialists) {
		this.mapSpecialists = mapSpecialists;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#addMapSpecialist(org.ihtsdo.otf.mapping.model.MapSpecialist)
	 */
	@Override
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
		mapSpecialists.add(mapSpecialist);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#removeMapSpecialist(org.ihtsdo.otf.mapping.model.MapSpecialist)
	 */
	@Override
	public void removeMapSpecialist(MapSpecialist mapSpecialist) {
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


	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)		
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isBlockStructure() {
		return blockStructure;
	}

	@Override
	public void setBlockStructure(boolean blockStructure) {
		this.blockStructure = blockStructure;
	}

	@Override
	public boolean isGroupStructure() {
		return groupStructure;
	}

	@Override
	public void setGroupStructure(boolean groupStructure) {
		this.groupStructure = groupStructure;
	}

	@Override
	public boolean isPublished() {
		return published;
	}

	@Override
	public void setPublished(boolean published) {
		this.published = published;
	}

	@Override
	public String getRefSetName() {
		return this.refSetName;
	}

	@Override
	public void setRefSetName(String refSetName) {
		this.refSetName = refSetName;
		
	}

	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getRefSetId() {
		return refSetId;
	}

	@Override
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
	}

	@Override
	public String getMapRelationStyle() {
		return mapRelationStyle;
	}
	
	@Override
	public String getMapPrincipleSourceDocument() {
		return mapPrincipleSourceDocument;
	}

	@Override
	public void setMapPrincipleSourceDocument(String mapPrincipleSourceDocument) {
		this.mapPrincipleSourceDocument = mapPrincipleSourceDocument;
	}

	@Override
	public void setMapRelationStyle(String mapRelationStyle) {
		this.mapRelationStyle = mapRelationStyle;
	}
	
	@Override
	public boolean isRuleBased() {
		return ruleBased;
	}

	@Override
	public void setRuleBased(boolean ruleBased) {
		this.ruleBased = ruleBased;
	}
	
	@Override
	public String getMapType() {
		return mapType;
	}

	@Override
	public void setMapType(String mapType) {
		this.mapType = mapType;
	}

	@Override
	public Set<String> getRulePresetAgeRanges() {
		return rulePresetAgeRanges;
	}

	@Override
	public void setRulePresetAgeRanges(Set<String> rulePresetAgeRanges) {
		this.rulePresetAgeRanges = rulePresetAgeRanges;
	}

	@Override
	@XmlElement(type=MapAdviceJpa.class, name="mapAdvice")
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}
	
	@Override
	@XmlElement(type=MapPrincipleJpa.class, name="mapPrinciple")
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	@Override
	public String toString() {
		return "MapProjectJpa [name=" + name + ", blockStructure="
				+ blockStructure + ", groupStructure=" + groupStructure
				+ ", published=" + published + ", refSetId=" + refSetId
				+ ", refSetName=" + refSetName + ", sourceTerminology="
				+ sourceTerminology + ", sourceTerminologyVersion="
				+ sourceTerminologyVersion + ", destinationTerminology="
				+ destinationTerminology + ", destinationTerminologyVersion="
				+ destinationTerminologyVersion + ", mapType=" + mapType
				+ ", mapRelationStyle=" + mapRelationStyle
				+ ", mapPrincipleSourceDocument=" + mapPrincipleSourceDocument
				+ ", ruleBased=" + ruleBased + ", mapLeads=" + mapLeads
				+ ", mapSpecialists=" + mapSpecialists + ", mapPrinciples="
				+ mapPrinciples + ", mapAdvices=" + mapAdvices
				+ ", rulePresetAgeRanges=" + rulePresetAgeRanges + "]";
	}

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
				+ ((mapRelationStyle == null) ? 0 : mapRelationStyle.hashCode());
		result = prime * result
				+ ((mapSpecialists == null) ? 0 : mapSpecialists.hashCode());
		result = prime * result + ((mapType == null) ? 0 : mapType.hashCode());
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
		if (mapType == null) {
			if (other.mapType != null)
				return false;
		} else if (!mapType.equals(other.mapType))
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
