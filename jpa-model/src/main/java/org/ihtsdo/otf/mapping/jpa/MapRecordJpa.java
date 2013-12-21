package org.ihtsdo.otf.mapping.jpa;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapNote;
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
	
	@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity=MapBlockJpa.class)
	@JsonManagedReference
	private List<MapBlock> mapBlocks;
	
	@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity=MapGroupJpa.class)
	@JsonManagedReference
	private List<MapGroup> mapGroups;
	
	@OneToMany(mappedBy = "mapRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, targetEntity=MapEntryJpa.class)
	@JsonManagedReference
	private List<MapEntry> mapEntries;
	
	@ManyToMany(targetEntity=MapNoteJpa.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	@IndexedEmbedded(targetElement=MapNoteJpa.class)
	private List<MapNote> notes;
	

	@Override
	public Long getId() {
		return id;
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

	@Override
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
	}

	@Override
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
	}

	@Override
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mapBlocks == null) ? 0 : mapBlocks.hashCode());
		result =
				prime * result + ((mapEntries == null) ? 0 : mapEntries.hashCode());
		result = prime * result + ((mapGroups == null) ? 0 : mapGroups.hashCode());
		result = prime * result + ((notes == null) ? 0 : notes.hashCode());
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
		MapRecordJpa other = (MapRecordJpa) obj;
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mapBlocks == null) {
			if (other.mapBlocks != null)
				return false;
		} else if (!mapBlocks.equals(other.mapBlocks))
			return false;
		if (mapEntries == null) {
			if (other.mapEntries != null)
				return false;
		} else if (!mapEntries.equals(other.mapEntries))
			return false;
		if (mapGroups == null) {
			if (other.mapGroups != null)
				return false;
		} else if (!mapGroups.equals(other.mapGroups))
			return false;
		if (notes == null) {
			if (other.notes != null)
				return false;
		} else if (!notes.equals(other.notes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MapRecordImpl [id=" + id + ", conceptId=" + conceptId
				+ ", mapBlocks=" + mapBlocks + ", mapGroups=" + mapGroups
				+ ", mapNotes=" + notes + ", mapEntries=" + mapEntries + "]";
	}

}
