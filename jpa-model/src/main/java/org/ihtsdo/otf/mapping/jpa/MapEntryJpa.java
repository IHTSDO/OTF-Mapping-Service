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

/**
 * The Map Entry Jpa object
 */
@Entity
@Table(name = "map_entries")
@Audited
@XmlRootElement(name="mapEntry")
public class MapEntryJpa implements MapEntry {
	
	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	@ManyToOne(targetEntity=MapRecordJpa.class, optional=false)
	@ContainedIn
	private MapRecord mapRecord;

	/** The map notes. */
	@ManyToMany(targetEntity=MapNoteJpa.class, cascade = CascadeType.ALL, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapNoteJpa.class)
	private Set<MapNote> mapNotes = new HashSet<MapNote>();

	/** The map advices. */
	@ManyToMany(targetEntity=MapAdviceJpa.class, cascade = CascadeType.ALL, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapAdviceJpa.class)
	private Set<MapAdvice> mapAdvices = new HashSet<MapAdvice>();
	
	/** The map principles. */
	@ManyToMany(targetEntity=MapPrincipleJpa.class, cascade = CascadeType.ALL, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapPrincipleJpa.class)
	private Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();

	/** The target. */
	@Column(nullable = false)
	private String target;
	
	/** The rule. */
	@Column(nullable = true, length = 4000)
	private String rule;

	/** The index (map priority). */
	@Column(nullable = false)
	private int indexMapPriority;

	/** The relation id. */
	@Column(nullable = false, length = 25)
	private String relationId;

	/** default constructor */
	public MapEntryJpa() {
		// empty
	}

	/** Constructor 
	 * @param id the id
	 * @param mapRecord the map record
	 * @param mapNotes the map notes
	 * @param target the target
	 * @param mapAdvices the map advices
	 * @param rule the rule
	 * @param indexMapPriority the index map priority
	 * @param relationId the relation id
	 */
	public MapEntryJpa(Long id, MapRecord mapRecord, Set<MapNote> mapNotes,
			String target, Set<MapAdvice> mapAdvices, String rule,
			int indexMapPriority, String relationId) {
		super();
		this.id = id;
		this.mapRecord = mapRecord;
		this.mapNotes = mapNotes;
		this.target = target;
		this.mapAdvices = mapAdvices;
		this.rule = rule;
		this.indexMapPriority = indexMapPriority;
		this.relationId = relationId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getId()
	 */
	@Override
	@XmlTransient
	public Long getId() {
		return id;
	}
	
	/**
	 * Returns the id in string form
	 * @return the id in string form
	 */
	@XmlID
	public String getObjectId() {
		return id.toString();
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getNotes()
	 */
	@Override
	@XmlElement(type=MapNoteJpa.class, name="mapNote")
	public Set<MapNote> getNotes() {
		return mapNotes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setNotes(java.util.List)
	 */
	@Override
	public void setNotes(Set<MapNote> notes) {
		this.mapNotes = notes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void addNote(MapNote note) {
		mapNotes.add(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void removeNote(MapNote note) {
		mapNotes.remove(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getTarget()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getTarget() {
		return target;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setTarget(java.lang.String)
	 */
	@Override
	public void setTarget(String target) {
		this.target = target;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#getAdvices()
	 */
	@Override
	@XmlElement(type=MapAdviceJpa.class, name="mapAdvice")
	public Set<MapAdvice> getAdvices() {
		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setAdvices(java.util.Set)
	 */
	@Override
	public void setAdvices(Set<MapAdvice> mapAdvices) {
		this.mapAdvices = mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#addAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addAdvice(MapAdvice mapAdvice) {
		mapAdvices.add(mapAdvice);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#removeAdvice(org.ihtsdo.otf.mapping.model.MapAdvice)
	 */
	@Override
	public void removeAdvice(MapAdvice mapAdvice) {
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
	public int getIndexMapPriority() {
		return indexMapPriority;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapEntry#setIndex(java.lang.String)
	 */
	@Override
	public void setIndexMapPriority(int index) {
		this.indexMapPriority = index;
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

	@XmlTransient
	@Override
	public MapRecord getMapRecord() {
		return this.mapRecord;
	}

	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}
	
	/**
	 * Return the map record id in string form
	 * @return the map record id in string form
	 */
	@XmlElement(name = "mapRecordId")
	public String getMapRecordId() {
		return mapRecord != null ? mapRecord.getObjectId() : null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + indexMapPriority;
		result =
				prime * result + ((mapAdvices == null) ? 0 : mapAdvices.hashCode());
		result = prime * result + ((mapNotes == null) ? 0 : mapNotes.hashCode());
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
		result =
				prime * result + ((relationId == null) ? 0 : relationId.hashCode());
		result = prime * result + ((rule == null) ? 0 : rule.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		MapEntryJpa other = (MapEntryJpa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (indexMapPriority != other.indexMapPriority)
			return false;
		if (mapAdvices == null) {
			if (other.mapAdvices != null)
				return false;
		} else if (!mapAdvices.equals(other.mapAdvices))
			return false;
		if (mapNotes == null) {
			if (other.mapNotes != null)
				return false;
		} else if (!mapNotes.equals(other.mapNotes))
			return false;
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
		if (rule == null) {
			if (other.rule != null)
				return false;
		} else if (!rule.equals(other.rule))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ""; /*MapEntryJpa [id=" + id + ", mapRecord=" + mapRecord + ", mapNotes="
				+ mapNotes + ", target=" + target + ", mapAdvices=" + mapAdvices
				+ ", rule=" + rule + ", indexMapPriority=" + indexMapPriority
				+ ", relationId=" + relationId + "]";*/ // TODO Changed this due to stack overflow error, testing output
	}
	
	
}
