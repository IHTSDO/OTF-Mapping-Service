package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Map Entry Jpa object.
 *
 */
@Entity
@Table(name = "map_entries")
@Audited
@XmlRootElement(name="mapEntry")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapEntryJpa implements MapEntry {
	
	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The map record. */
	@ManyToOne(targetEntity=MapRecordJpa.class, optional=false)
	@ContainedIn
	private MapRecord mapRecord;

	/** The map notes. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=MapNoteJpa.class)
	@IndexedEmbedded(targetElement=MapNoteJpa.class)
	private Set<MapNote> mapNotes = new HashSet<MapNote>();

	/** The map advices. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	/** The map principles. */
	@ManyToMany(targetEntity=MapPrincipleJpa.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapPrincipleJpa.class)
	private Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();

	/** The target. */
	@Column(nullable = true)
	private String targetId;
	
	/** The target name. */
	@Column(nullable = true) // TODO Change to false once other terminologies are in
	private String targetName;
	
	/** The rule. */
	@Column(nullable = true, length = 4000)
	private String rule;

	/** The map priority. */
	@Column(nullable = false)
	private int mapPriority;

	/** The relation id. */
	@Column(nullable = false, length = 25)
	private String relationId;
	
	/** The index (map priority). */
	@Column(nullable = false)
	private int mapBlock;
	
	/** The index (map priority). */
	@Column(nullable = false)
	private int mapGroup;
	/**
	 * default constructor.
	 */
	public MapEntryJpa() {
		// empty
	}

	/**
	 * Constructor.
	 *
	 * @param id the id
	 * @param mapRecord the map record
	 * @param mapNotes the map notes
	 * @param targetId the targetId
	 * @param mapAdvices the map advices
	 * @param rule the rule
	 * @param mapPriority the index map priority
	 * @param relationId the relation id
	 */
	public MapEntryJpa(Long id, MapRecord mapRecord, Set<MapNote> mapNotes,
			String targetId, Set<MapAdvice> mapAdvices, String rule,
			int mapPriority, String relationId) {
		super();
		this.id = id;
		this.mapRecord = mapRecord;
		this.mapNotes = mapNotes;
		this.targetId = targetId;
		this.mapAdvices = mapAdvices;
		this.rule = rule;
		this.mapPriority = mapPriority;
		this.relationId = relationId;
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
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapNotes()
	 */
	@Override
	@XmlElement(type=MapNoteJpa.class, name="mapNote")
	public Set<MapNote> getMapNotes() {
		return mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapNotes(java.util.Set)
	 */
	@Override
	public void setMapNotes(Set<MapNote> notes) {
		this.mapNotes = notes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addMapNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void addMapNote(MapNote note) {
		mapNotes.add(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeMapNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void removeMapNote(MapNote note) {
		mapNotes.remove(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getTargetId() {
		return targetId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setTarget(java.lang.String)
	 */
	@Override
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	@Override
	public String getTargetName() {
		return this.targetName;
	}

	@Override
	public void setTargetName(String targetName) {
		this.targetName = targetName;
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapAdvices()
	 */
	@XmlElement(type=MapAdviceJpa.class, name="mapAdvice")
	@Override
	public Set<MapAdvice> getMapAdvices() {
		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapAdvices(java.util.Set)
	 */
	@Override
	public void setMapAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeMapAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void removeMapAdvice(MapAdvice mapAdvice) {
		mapAdvices.remove(mapAdvice);
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapPrinciples()
	 */
	@XmlElement(type=MapPrincipleJpa.class, name="mapPrinciple")
	@Override
	public Set<MapPrinciple> getMapPrinciples() {
		return mapPrinciples;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapPrinciples(java.util.Set)
	 */
	@Override
	public void setMapPrinciples(Set<MapPrinciple> mapPrinciples) {
		this.mapPrinciples = mapPrinciples;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addMapPrinciple(org.ihtsdo.otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.add(mapPrinciple);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeMapPrinciple(org.ihtsdo.otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void removeMapPrinciple(MapPrinciple mapPrinciple) {
		mapPrinciples.remove(mapPrinciple);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getRule()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getRule() {
		return rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setRule(java.lang.String)
	 */
	@Override
	public void setRule(String rule) {
		this.rule = rule;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getIndex()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public int getMapPriority() {
		return mapPriority;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setIndex(java.lang.String)
	 */
	@Override
	public void setMapPriority(int index) {
		this.mapPriority = index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getRelationId()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getRelationId() {
		return relationId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setRelationId(java.lang.String)
	 */
	@Override
	public void setRelationId(String relationId) {
		this.relationId = relationId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getMapRecord()
	 */
	@XmlTransient
	@Override
	public MapRecord getMapRecord() {
		return this.mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}
	
	/**
	 * Returns the map record id.
	 *
	 * @return the map record id
	 */
	@XmlElement
	public String getMapRecordId() {
		return mapRecord != null ? mapRecord.getObjectId() : null;
	}
	
	@Override
	public int getMapGroup() {
		return this.mapGroup;
	}

	@Override
	public void setMapGroup(int mapGroup) {
		this.mapGroup = mapGroup;
		
	}

	@Override
	public int getMapBlock() {
		return this.mapBlock;
	}

	@Override
	public void setMapBlock(int mapBlock) {
		this.mapBlock = mapBlock;
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
		result =
				prime * result + ((relationId == null) ? 0 : relationId.hashCode());
		result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
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
		MapEntryJpa other = (MapEntryJpa) obj;
		if (mapRecord == null) {
			if (other.mapRecord != null)
				return false;
		} else if (!mapRecord.equals(other.mapRecord))
			return false;
		if (relationId == null) {
			if (other.relationId != null)
				return false;
		} else if (!relationId.equals(other.relationId))
			return false;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MapEntryJpa [id=" + id + ", mapRecord=" + mapRecord
				+ ", mapNotes=" + mapNotes + ", mapAdvices=" + mapAdvices
				+ ", mapPrinciples=" + mapPrinciples + ", targetId=" + targetId
				+ ", rule=" + rule + ", mapPriority=" + mapPriority
				+ ", relationId=" + relationId + ", mapBlock=" + mapBlock
				+ ", mapGroup=" + mapGroup + "]";
	}
	
	
}
