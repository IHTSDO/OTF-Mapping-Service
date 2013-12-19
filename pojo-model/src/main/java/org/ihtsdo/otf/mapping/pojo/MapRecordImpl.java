package org.ihtsdo.otf.mapping.pojo;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapRecord}.
 * Includes hibernate tags for MEME database.
 *
 * @author ${author}
 */
public class MapRecordImpl implements MapRecord {
	
	/** The id. */
	private Long id;
	
	/** The concept id. */
	private String conceptId;
	
	/** The map blocks. */
	private List<MapBlock> mapBlocks;
	
	/** The map groups. */
	private List<MapGroup> mapGroups;
	
	/** The notes. */
	private List<MapNote> notes;
	
	/** The map entries. */
	private List<MapEntry> mapEntries;

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getConceptId()
	 */
	@Override
	public String getConceptId() {
		return conceptId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setConceptId(java.lang.String)
	 */
	@Override
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapBlocks()
	 */
	@Override
	public List<MapBlock> getMapBlocks() {
		return mapBlocks;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapBlocks(java.util.List)
	 */
	@Override
	public void setMapBlocks(List<MapBlock> mapBlocks) {
		this.mapBlocks = mapBlocks;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addMapBlock(org.ihtsdo.otf.mapping.model.MapBlock)
	 */
	@Override
	public void addMapBlock(MapBlock mapBlock) {
		mapBlocks.add(mapBlock);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeMapBlock(org.ihtsdo.otf.mapping.model.MapBlock)
	 */
	@Override
	public void removeMapBlock(MapBlock mapBlock) {
		mapBlocks.remove(mapBlock);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapGroups()
	 */
	@Override
	public List<MapGroup> getMapGroups() {
		return mapGroups;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapGroups(java.util.List)
	 */
	@Override
	public void setMapGroups(List<MapGroup> mapGroups) {
		this.mapGroups = mapGroups;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addMapGroup(org.ihtsdo.otf.mapping.model.MapGroup)
	 */
	@Override
	public void addMapGroup(MapGroup mapGroup) {
		mapGroups.add(mapGroup);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeMapGroup(org.ihtsdo.otf.mapping.model.MapGroup)
	 */
	@Override
	public void removeMapGroup(MapGroup mapGroup) {
		mapGroups.remove(mapGroup);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getNotes()
	 */
	@Override
	public List<MapNote> getNotes() {
		return notes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setNotes(java.util.List)
	 */
	@Override
	public void setNotes(List<MapNote> notes) {
		this.notes = notes;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void addNote(MapNote note) {
		notes.add(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#removeNote(org.ihtsdo.otf.mapping.model.MapNote)
	 */
	@Override
	public void removeNote(MapNote note) {
		notes.remove(note);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#getMapEntries()
	 */
	@Override
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#setMapEntries(java.util.List)
	 */
	@Override
	public void setMapEntries(List<MapEntry> mapEntries) {
		this.mapEntries = mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapRecord#addMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
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
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mapBlocks == null) ? 0 : mapBlocks.hashCode());
		result =
				prime * result + ((mapEntries == null) ? 0 : mapEntries.hashCode());
		result = prime * result + ((mapGroups == null) ? 0 : mapGroups.hashCode());
		result = prime * result + ((notes == null) ? 0 : notes.hashCode());
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
		MapRecordImpl other = (MapRecordImpl) obj;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapRecordImpl [id=" + id + ", conceptId=" + conceptId
				+ ", mapBlocks=" + mapBlocks + ", mapGroups=" + mapGroups
				+ ", mapNotes=" + notes + ", mapEntries=" + mapEntries + "]";
	}

}
