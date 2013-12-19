package org.ihtsdo.otf.mapping.pojo;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapGroup;
import org.ihtsdo.otf.mapping.model.MapRecord;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapGroup}.
 *
 * @author ${author}
 */
public class MapGroupImpl implements MapGroup {

	/** The id. */
	private Long id;
	
	/** The index. */
	private int index;
	
	/** The map entries included in this MapGroup. */
	private List<MapEntry> mapEntries;
	
	/** The map record. */
	private MapRecord mapRecord;
	
	/** The map block. */
	private MapBlock mapBlock;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getIndex()
	 */
	@Override
	public int getIndex() {
		return index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setIndex(int)
	 */
	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapEntries()
	 */
	@Override
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapEntries(java.util.List)
	 */
	@Override
	public void setMapEntries(List<MapEntry> mapEntries) {
		this.mapEntries = mapEntries;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#addMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void addMapEntry(MapEntry mapEntry) {
		mapEntries.add(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#removeMapEntry(org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	public void removeMapEntry(MapEntry mapEntry) {
		mapEntries.remove(mapEntry);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapRecord()
	 */
	@Override
	public MapRecord getMapRecord() {
		return mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void setMapRecord(MapRecord mapRecord) {
		this.mapRecord = mapRecord;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#getMapBlock()
	 */
	@Override
	public MapBlock getMapBlock() {
		return mapBlock;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapGroup#setMapBlock(org.ihtsdo.otf.mapping.model.MapBlock)
	 */
	@Override
	public void setMapBlock(MapBlock mapBlock) {
		this.mapBlock = mapBlock;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + index;
		result = prime * result + ((mapBlock == null) ? 0 : mapBlock.hashCode());
		result =
				prime * result + ((mapEntries == null) ? 0 : mapEntries.hashCode());
		result = prime * result + ((mapRecord == null) ? 0 : mapRecord.hashCode());
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
		MapGroupImpl other = (MapGroupImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (index != other.index)
			return false;
		if (mapBlock == null) {
			if (other.mapBlock != null)
				return false;
		} else if (!mapBlock.equals(other.mapBlock))
			return false;
		if (mapEntries == null) {
			if (other.mapEntries != null)
				return false;
		} else if (!mapEntries.equals(other.mapEntries))
			return false;
		if (mapRecord == null) {
			if (other.mapRecord != null)
				return false;
		} else if (!mapRecord.equals(other.mapRecord))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapGroupImpl [id=" + id + ", index=" + index + ", mapEntries="
				+ mapEntries + ", mapRecord=" + mapRecord + ", mapBlock=" + mapBlock
				+ "]";
	}

}
