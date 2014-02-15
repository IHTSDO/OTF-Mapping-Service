package org.ihtsdo.otf.mapping.pojo;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlID;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapProject}.
 *
 * @author ${author}
 */
public class MapProjectImpl implements MapProject {

	/** The id. */
	private Long id;

	/** The name. */
	private String name;

	/** Indicates whether there is block structure for map records of this project. */
	private boolean blockStructure = false;

	/** Indicates whether there is group structure for map records of this project. */
	private boolean groupStructure = false;

	/** Indicates if the map project has been published. */
	private boolean published = false;

	/** The map leads working on this MapProject. */
	private Set<MapLead> mapLeads = new HashSet<MapLead>();

	/** The map specialists working on this MapProject. */
	private Set<MapSpecialist> mapSpecialists = new HashSet<MapSpecialist>();

	/** The allowable map advices for this MapProject. */
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	/** The allowable map principles for this MapProject */
	private Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();

	/** The ref set id. */
	private String refSetId;
	
	/** The ref set name. */
	private String refSetName;

	/** The source terminology. */
	private String sourceTerminology;

	/** The destination terminology. */
	private String destinationTerminology;

	/** The source terminology version. */
	private String sourceTerminologyVersion;

	/** The destination terminology version. */
	private String destinationTerminologyVersion;
	
	/** The relation style */
	private String mapRelationStyle;
	
	/** The name of the document containing the map principles */
	private String mapPrincipleSourceDocument;
	
	/** Flag for whether the project is rule based */
	private boolean ruleBased;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
	 */
	@Override
	public Set<MapLead> getMapLeads() {
		return mapLeads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setMapLeads(java.util.Set)
	 */
	@Override
	public void setMapLeads(Set<MapLead> mapLeads) {
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
	public void addMapLead(MapLead mapLead) {
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
	public void removeMapLead(MapLead mapLead) {
		mapLeads.remove(mapLead);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapSpecialists()
	 */
	@Override
	public Set<MapSpecialist> getMapSpecialists() {
		return mapSpecialists;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.model.MapProject#setMapSpecialists(java.util.Set)
	 */
	@Override
	public void setMapSpecialists(Set<MapSpecialist> mapSpecialists) {
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
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
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
	public void removeMapSpecialist(MapSpecialist mapSpecialist) {
		mapSpecialists.remove(mapSpecialist);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getSourceTerminology()
	 */
	@Override
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
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapAdvices()
	 */
	@Override
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
		this.mapAdvices.add(mapAdvice);
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
		this.mapAdvices.remove(mapAdvice);
	}
	
	@Override
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
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getRefSetId()
	 */
	@Override
	public String getRefSetId() {
		return refSetId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetId(java.lang.Long)
	 */
	@Override
	public void setRefSetId(String refSetId) {
		this.refSetId = refSetId;
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
	public String getMapRelationStyle() {
		return mapRelationStyle;
	}

	@Override
	public void setMapRelationStyle(String mapRelationStyle) {
		this.mapRelationStyle = mapRelationStyle;
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
	public boolean isRuleBased() {
		return ruleBased;
	}

	@Override
	public void setRuleBased(boolean ruleBased) {
		this.ruleBased = ruleBased;
	}

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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapLeads == null) ? 0 : mapLeads.hashCode());
		result =
				prime * result
						+ ((mapSpecialists == null) ? 0 : mapSpecialists.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (published ? 1231 : 1237);
		result = prime * result + ((refSetId == null) ? 0 : refSetId.hashCode());
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapProjectImpl other = (MapProjectImpl) obj;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
}
