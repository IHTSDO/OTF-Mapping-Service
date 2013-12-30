package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;

import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * The Map Record Jpa object
 */
@Entity
@Table(name = "map_records")
@Audited
@Indexed
@XmlRootElement(name="mapRecord")
public class MapRecordJpa implements MapRecord {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	@Column(nullable = false)
	private String conceptId;
	
	/*@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity=MapBlockJpa.class)
	@JsonManagedReference
	private List<MapBlock> mapBlocks = new ArrayList<MapBlock>();
	
	@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity=MapGroupJpa.class)
	@JsonManagedReference
	private List<MapGroup> mapGroups = new ArrayList<MapGroup>();
	*/
	
	@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=MapEntryJpa.class)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapEntryJpa.class)
	private List<MapEntry> mapEntries = new ArrayList<MapEntry>();
	
	/*@ManyToMany(targetEntity=MapNoteJpa.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapNoteJpa.class)
	private List<MapNote> notes = new ArrayList<MapNote>();
	*/

	public MapRecordJpa() {
	}

	public MapRecordJpa(Long id, String conceptId, List<MapEntry> mapEntries) { // , List<MapNote> notes) {
		super();
		this.id = id;
		this.conceptId = conceptId;
		//this.mapBlocks = mapBlocks;
		//this.mapGroups = mapGroups;
		this.mapEntries = mapEntries;
		//this.notes = notes;
	}

	@Override
	@XmlTransient
	public Long getId() {
		return id;
	}
	
	@XmlID
	public String getID() {
		return id.toString();
	}

	
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	/*@Override
	@XmlElement(type=MapBlockJpa.class)
	public List<MapBlock> getMapBlocks() {
		return mapBlocks;
	}

	@Override
	public void setMapBlocks(List<MapBlock> mapBlocks) {
		this.mapBlocks = mapBlocks;
	}

	@Override
	public void addMapBlock(MapBlock mapBlock) {
		mapBlocks.add(mapBlock);
	}

	@Override
	public void removeMapBlock(MapBlock mapBlock) {
		mapBlocks.remove(mapBlock);
	}

	@Override
	@XmlElement(type=MapGroupJpa.class)
	public List<MapGroup> getMapGroups() {
		return mapGroups;
	}

	@Override
	public void setMapGroups(List<MapGroup> mapGroups) {
		this.mapGroups = mapGroups;
	}

	@Override
	public void addMapGroup(MapGroup mapGroup) {
		mapGroups.add(mapGroup);
	}

	@Override
	public void removeMapGroup(MapGroup mapGroup) {
		mapGroups.remove(mapGroup);
	}*/

	/*@Override
	@XmlElement(type=MapNoteJpa.class)
	public List<MapNote> getNotes() {
		return notes;
	}

	@Override
	public void setNotes(List<MapNote> notes) {
		this.notes = notes;
	}

	@Override
	public void addNote(MapNote note) {
		notes.add(note);
	}

	@Override
	public void removeNote(MapNote note) {
		notes.remove(note);
	}*/

	@Override
	@XmlElement(type=MapEntryJpa.class, name="mapEntry")
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	@Override
	public void setMapEntries(List<MapEntry> mapEntries) {
		this.mapEntries = mapEntries;
	}

	@Override
	public void addMapEntry(MapEntry mapEntry) {
		mapEntries.add(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void removeMapEntry(MapEntry mapEntry) {
		mapEntries.remove(mapEntry);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((mapEntries == null) ? 0 : mapEntries.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MapRecordJpa other = (MapRecordJpa) obj;
		if (conceptId == null) {
			if (other.conceptId != null) {
				return false;
			}
		} else if (!conceptId.equals(other.conceptId)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (mapEntries == null) {
			if (other.mapEntries != null) {
				return false;
			}
		} else if (!mapEntries.equals(other.mapEntries)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapRecordJpa [id=" + id + ", conceptId=" + conceptId
				+ ", mapEntries=" + mapEntries + "]";
	}

}
