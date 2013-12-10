package org.ihtsdo.otf.mapping.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
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

	/** The ref set id. */
	private Long refSetId;

	/** The source terminology. */
	private String sourceTerminology;

	/** The destination terminology. */
	private String destinationTerminology;

	/** The source terminology version. */
	private String sourceTerminologyVersion;

	/** The destination terminology version. */
	private String destinationTerminologyVersion;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
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
	public Long getRefSetId() {
		return refSetId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setRefSetId(java.lang.Long)
	 */
	@Override
	public void setRefSetId(Long refSetId) {
		this.refSetId = refSetId;
	}

}
