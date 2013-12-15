package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
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
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

import com.fasterxml.jackson.annotation.JsonManagedReference;


/**
 * The Class MapProjectJpa.
 *
 */
@Entity
@Table(name = "map_projects")
@Audited
@Indexed
@XmlRootElement
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

	/** The allowable map advices for this MapProject. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, fetch=FetchType.EAGER)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();

	/** The ref set id. */
	private Long refSetId;
	
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
	
	/** The map leads. */
	@ManyToMany(targetEntity=MapLeadJpa.class, fetch=FetchType.EAGER)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapLeadJpa.class)
	private Set<MapLead> mapLeads = new HashSet<MapLead>();
	
	/** The map specialists. */
	@ManyToMany(targetEntity=MapSpecialistJpa.class, fetch=FetchType.EAGER)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapSpecialistJpa.class)
	private Set<MapSpecialist> mapSpecialists = new HashSet<MapSpecialist>();
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapProject#getMapLeads()
	 */
	@Override
	@XmlElement(type=MapLeadJpa.class)
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
	@XmlElement(type=MapSpecialistJpa.class)
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
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public Long getRefSetId() {
		return refSetId;
	}

	@Override
	public void setRefSetId(Long refSetId) {
		this.refSetId = refSetId;
	}

	@Override
	@XmlElement(type=MapAdviceJpa.class)
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		 
		 return this.getId() + "," + this.getName() + "," +
		     this.getRefSetId() + "," +
				 this.getSourceTerminology() + "," +
				 this.getSourceTerminologyVersion() + "," +
				 this.getDestinationTerminology() + "," +
				 this.getDestinationTerminologyVersion() + "," +
				 this.isBlockStructure() + "," +
				 this.isGroupStructure() + "," +
				 this.isPublished() + "," + 
				 this.getMapAdvices() == null ? "" : this.getMapAdvices().toString() + "," +
				 this.getMapLeads() == null ? "" : this.getMapLeads().toString() + "," +
				 this.getMapSpecialists() == null ? "" : this.getMapSpecialists().toString();
				 
	 }


}
